package com.example.fairtripdistribution.service;

import com.example.fairtripdistribution.exception.BusinessValidationException;
import com.example.fairtripdistribution.exception.ResourceNotFoundException;
import com.example.fairtripdistribution.model.dto.*;
import com.example.fairtripdistribution.model.entity.*;
import com.example.fairtripdistribution.model.entity.enums.*;
import com.example.fairtripdistribution.repository.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class AllocationService {
    private final TripRepository tripRepository;
    private final TripAllocationRepository tripAllocationRepository;
    private final ZoneRepository zoneRepository;
    private final AllocationBucketRepository bucketRepository;
    private final VendorAllocationStateRepository stateRepository;
    private final VendorZoneShareRepository shareRepository;
    private final VendorCapacityRepository capacityRepository;
    private final TripRejectionRepository tripRejectionRepository;

    public AllocationService(TripRepository tripRepository, TripAllocationRepository tripAllocationRepository,
                             ZoneRepository zoneRepository, AllocationBucketRepository bucketRepository,
                             VendorAllocationStateRepository stateRepository, VendorZoneShareRepository shareRepository,
                             VendorCapacityRepository capacityRepository, TripRejectionRepository tripRejectionRepository) {
        this.tripRepository = tripRepository;
        this.tripAllocationRepository = tripAllocationRepository;
        this.zoneRepository = zoneRepository;
        this.bucketRepository = bucketRepository;
        this.stateRepository = stateRepository;
        this.shareRepository = shareRepository;
        this.capacityRepository = capacityRepository;
        this.tripRejectionRepository = tripRejectionRepository;
    }

    @Transactional
    public TripAllocateResponseDto allocateTrip(TripAllocateRequestDto request) {
        // Idempotency check
        Optional<Trip> existingTripOpt = tripRepository.findByExternalTripId(request.externalTripId);
        Trip trip = null;
        if (existingTripOpt.isPresent()) {
            trip = existingTripOpt.get();
            Optional<TripAllocation> allocOpt = tripAllocationRepository.findByTripId(trip.getId());
            if (allocOpt.isPresent() && allocOpt.get().getStatus() == AllocationStatus.SUCCESS) {
                return mapToResponse(trip, allocOpt.get());
            }
        }

        // Determine Zone
        Zone zone = zoneRepository.findByIsActiveTrue().stream()
            .filter(z -> {
                boolean minOk = request.distance.compareTo(z.getMinDistance()) >= 0;
                boolean maxOk = z.getMaxDistance() == null || request.distance.compareTo(z.getMaxDistance()) < 0;
                return minOk && maxOk;
            })
            .findFirst()
            .orElseThrow(() -> new BusinessValidationException("No matching active zone found for distance"));

        // Get or Create Bucket (Locked)
        AllocationBucket bucket = bucketRepository.findByZoneIdAndTripTypeForUpdate(zone.getId(), request.tripType)
            .orElseGet(() -> {
                AllocationBucket b = new AllocationBucket();
                b.setZone(zone);
                b.setTripType(request.tripType);
                b.setTotalTrips(0);
                return bucketRepository.save(b);
            });

        // Configured Shares
        List<VendorZoneShare> shares = shareRepository.findByZoneIdAndTripType(zone.getId(), request.tripType);
        if (shares.isEmpty()) {
            throw new BusinessValidationException("No configured vendor shares for this zone and trip type");
        }

        // Pre-fetch and lock capacities for deterministic and safe consumption
        List<Long> vendorIds = shares.stream().map(s -> s.getVendor().getId()).collect(Collectors.toList());
        List<VendorCapacity> lockedCapacities = capacityRepository.findByVendorIdInForUpdate(vendorIds);
        Map<Long, VendorCapacity> capacityMap = lockedCapacities.stream()
            .collect(Collectors.toMap(c -> c.getVendor().getId(), c -> c));

        // Lock all states for this bucket
        List<VendorAllocationState> states = stateRepository.findByBucketIdForUpdate(bucket.getId());
        Map<Long, VendorAllocationState> stateMap = states.stream()
            .collect(Collectors.toMap(s -> s.getVendor().getId(), s -> s));

        LocalDateTime now = LocalDateTime.now();

        // Evaluate eligible vendors
        Vendor selectedVendor = null;
        long maxShortfall = Long.MIN_VALUE;
        int bestPriority = Integer.MAX_VALUE;
        long bestVendorId = Long.MAX_VALUE;

        boolean foundEligible = false;

        for (VendorZoneShare share : shares) {
            Vendor vendor = share.getVendor();
            if (!vendor.isActive()) continue;

            // Capacity Check
            VendorCapacity cap = capacityMap.get(vendor.getId());
            if (cap != null && cap.getAvailableCapacity() <= 0) {
                continue; // Cannot receive trip
            }

            // Cooldown Check
            if (tripRejectionRepository.isVendorInCooldownForTrip(vendor.getId(), request.externalTripId, now)) {
                continue; // Cannot receive this specific trip while in cooldown
            }

            VendorAllocationState state = stateMap.computeIfAbsent(vendor.getId(), vid -> {
                VendorAllocationState s = new VendorAllocationState();
                s.setBucket(bucket);
                s.setVendor(vendor);
                s.setAllocatedTrips(0);
                return stateRepository.save(s);
            });

            // Calculate Shortfall
            long targetBasisPoints = share.getTargetBasisPoints();
            long totalTrips = bucket.getTotalTrips();
            long allocatedTrips = state.getAllocatedTrips();

            long shortfall = (totalTrips * targetBasisPoints) - (allocatedTrips * 10000L);

            boolean isBetter = false;
            if (!foundEligible) {
                isBetter = true;
                foundEligible = true;
            } else if (shortfall > maxShortfall) {
                isBetter = true;
            } else if (shortfall == maxShortfall) {
                if (vendor.getPriority() < bestPriority) {
                    isBetter = true;
                } else if (vendor.getPriority() == bestPriority) {
                    if (vendor.getId() < bestVendorId) {
                        isBetter = true;
                    }
                }
            }

            if (isBetter) {
                maxShortfall = shortfall;
                bestPriority = vendor.getPriority();
                bestVendorId = vendor.getId();
                selectedVendor = vendor;
            }
        }

        if (selectedVendor == null) {
            throw new BusinessValidationException("No eligible vendor found for allocation");
        }

        // Update State
        bucket.setTotalTrips(bucket.getTotalTrips() + 1);
        bucketRepository.save(bucket);

        VendorAllocationState selectedState = stateMap.get(selectedVendor.getId());
        selectedState.setAllocatedTrips(selectedState.getAllocatedTrips() + 1);
        stateRepository.save(selectedState);

        // Consume capacity
        VendorCapacity selectedCap = capacityMap.get(selectedVendor.getId());
        if (selectedCap != null) {
            selectedCap.setAvailableCapacity(selectedCap.getAvailableCapacity() - 1);
            capacityRepository.save(selectedCap);
        }

        // Save Trip & Allocation
        if (trip == null) {
            trip = new Trip();
            trip.setExternalTripId(request.externalTripId);
            trip.setZone(zone);
            trip.setTripType(request.tripType);
        }
        trip.setStatus(TripStatus.ALLOCATED);
        trip = tripRepository.save(trip);

        // Existing failed allocation might exist if we are re-allocating after a rejection,
        // but we create a new one to keep history, or update the old?
        // Usually, we can just insert a new SUCCESS allocation record or mark old as REJECTED and insert new.
        // It's cleaner to just insert a new one.
        TripAllocation allocation = null;
        if (trip.getId() != null) {
            allocation = tripAllocationRepository.findByTripId(trip.getId()).orElse(null);
        }
        if (allocation == null) {
            allocation = new TripAllocation();
            allocation.setTrip(trip);
        }
        allocation.setVendor(selectedVendor);
        allocation.setStatus(AllocationStatus.SUCCESS);
        allocation = tripAllocationRepository.save(allocation);

        return mapToResponse(trip, allocation);
    }

    @Transactional
    public TripAllocateResponseDto rejectTrip(TripRejectRequestDto request) {
        Trip trip = tripRepository.findByExternalTripId(request.externalTripId)
            .orElseThrow(() -> new ResourceNotFoundException("Trip not found"));
            
        // Find the active successful allocation
        TripAllocation alloc = tripAllocationRepository.findByTripId(trip.getId())
            .filter(a -> a.getStatus() == AllocationStatus.SUCCESS)
            .orElseThrow(() -> new BusinessValidationException("No successful allocation found for this trip to reject"));
            
        Vendor vendor = alloc.getVendor();
        
        // 1. Change status to REJECTED
        alloc.setStatus(AllocationStatus.REJECTED);
        tripAllocationRepository.save(alloc);
        
        // 2. Record Rejection & Cooldown (e.g. 15 minutes)
        TripRejection rejection = new TripRejection();
        rejection.setTrip(trip);
        rejection.setVendor(vendor);
        rejection.setReason(request.reason);
        rejection.setRejectedAt(LocalDateTime.now());
        rejection.setCooldownUntil(LocalDateTime.now().plusMinutes(15));
        tripRejectionRepository.save(rejection);
        
        // 3. Revert Allocation state counters
        AllocationBucket bucket = bucketRepository.findByZoneIdAndTripTypeForUpdate(trip.getZone().getId(), trip.getTripType())
            .orElseThrow(() -> new IllegalStateException("Bucket vanished"));
        bucket.setTotalTrips(bucket.getTotalTrips() - 1);
        bucketRepository.save(bucket);
        
        VendorAllocationState state = stateRepository.findByBucketIdForUpdate(bucket.getId()).stream()
            .filter(s -> s.getVendor().getId().equals(vendor.getId()))
            .findFirst().orElseThrow(() -> new IllegalStateException("State vanished"));
        state.setAllocatedTrips(state.getAllocatedTrips() - 1);
        stateRepository.save(state);
        
        // 4. Restore Capacity
        Optional<VendorCapacity> capOpt = capacityRepository.findByVendorId(vendor.getId());
        // Since we only lock the individual record during revert, it's safe (deadlock risk is minimal here, but ideally we'd lock it)
        if (capOpt.isPresent()) {
            // Re-fetch with lock
            VendorCapacity cap = capacityRepository.findByVendorIdInForUpdate(List.of(vendor.getId())).get(0);
            cap.setAvailableCapacity(cap.getAvailableCapacity() + 1);
            capacityRepository.save(cap);
        }
        
        // 5. Re-run allocation for the same trip
        TripAllocateRequestDto req = new TripAllocateRequestDto();
        req.externalTripId = trip.getExternalTripId();
        // To accurately re-run, we need the original distance. But trip doesn't store distance, only zone.
        // Wait! The user request says "Re-run the allocation process for the same trip".
        // If distance is missing, I can just use a fake distance in the middle of the zone for reallocation.
        Zone z = trip.getZone();
        if (z.getMaxDistance() == null) {
            req.distance = z.getMinDistance().add(new java.math.BigDecimal("5"));
        } else {
            req.distance = z.getMinDistance();
        }
        req.tripType = trip.getTripType();
        
        return allocateTrip(req);
    }

    private TripAllocateResponseDto mapToResponse(Trip trip, TripAllocation allocation) {
        TripAllocateResponseDto res = new TripAllocateResponseDto();
        res.tripId = trip.getId();
        res.externalTripId = trip.getExternalTripId();
        res.vendorId = allocation.getVendor().getId();
        res.zoneCode = trip.getZone().getCode();
        res.tripType = trip.getTripType();
        res.status = allocation.getStatus();
        return res;
    }
}
