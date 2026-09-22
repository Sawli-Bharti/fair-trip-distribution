package com.example.fairtripdistribution.service;

import com.example.fairtripdistribution.exception.BusinessValidationException;
import com.example.fairtripdistribution.model.dto.*;
import com.example.fairtripdistribution.model.entity.*;
import com.example.fairtripdistribution.model.entity.enums.*;
import com.example.fairtripdistribution.repository.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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

    public AllocationService(TripRepository tripRepository, TripAllocationRepository tripAllocationRepository,
                             ZoneRepository zoneRepository, AllocationBucketRepository bucketRepository,
                             VendorAllocationStateRepository stateRepository, VendorZoneShareRepository shareRepository,
                             VendorCapacityRepository capacityRepository) {
        this.tripRepository = tripRepository;
        this.tripAllocationRepository = tripAllocationRepository;
        this.zoneRepository = zoneRepository;
        this.bucketRepository = bucketRepository;
        this.stateRepository = stateRepository;
        this.shareRepository = shareRepository;
        this.capacityRepository = capacityRepository;
    }

    @Transactional
    public TripAllocateResponseDto allocateTrip(TripAllocateRequestDto request) {
        // Idempotency check
        Optional<Trip> existingTripOpt = tripRepository.findByExternalTripId(request.externalTripId);
        if (existingTripOpt.isPresent()) {
            Trip existingTrip = existingTripOpt.get();
            Optional<TripAllocation> allocOpt = tripAllocationRepository.findByTripId(existingTrip.getId());
            if (allocOpt.isPresent() && allocOpt.get().getStatus() == AllocationStatus.SUCCESS) {
                return mapToResponse(existingTrip, allocOpt.get());
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

        // Lock all states for this bucket
        List<VendorAllocationState> states = stateRepository.findByBucketIdForUpdate(bucket.getId());
        Map<Long, VendorAllocationState> stateMap = states.stream()
            .collect(Collectors.toMap(s -> s.getVendor().getId(), s -> s));

        // Evaluate eligible vendors
        Vendor selectedVendor = null;
        long maxShortfall = Long.MIN_VALUE;
        int bestPriority = Integer.MAX_VALUE;
        long bestVendorId = Long.MAX_VALUE;

        boolean foundEligible = false;

        for (VendorZoneShare share : shares) {
            Vendor vendor = share.getVendor();
            if (!vendor.isActive()) continue;

            // Optional capacity check based on currently implemented rules (ignoring advanced consumption)
            // If the vendor has a capacity record, available must be > 0. If no record, we assume no restriction.
            Optional<VendorCapacity> capOpt = capacityRepository.findByVendorId(vendor.getId());
            if (capOpt.isPresent() && capOpt.get().getAvailableCapacity() <= 0) {
                continue; // Cannot receive trip
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

        // Save Trip & Allocation
        Trip trip = new Trip();
        trip.setExternalTripId(request.externalTripId);
        trip.setZone(zone);
        trip.setTripType(request.tripType);
        trip.setStatus(TripStatus.ALLOCATED);
        trip = tripRepository.save(trip);

        TripAllocation allocation = new TripAllocation();
        allocation.setTrip(trip);
        allocation.setVendor(selectedVendor);
        allocation.setStatus(AllocationStatus.SUCCESS);
        allocation = tripAllocationRepository.save(allocation);

        return mapToResponse(trip, allocation);
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
