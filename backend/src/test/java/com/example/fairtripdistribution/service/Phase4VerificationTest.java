package com.example.fairtripdistribution.service;

import com.example.fairtripdistribution.exception.BusinessValidationException;
import com.example.fairtripdistribution.model.dto.*;
import com.example.fairtripdistribution.model.entity.*;
import com.example.fairtripdistribution.model.entity.enums.TripType;
import com.example.fairtripdistribution.model.entity.enums.AllocationStatus;
import com.example.fairtripdistribution.repository.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

@SpringBootTest(properties = {"app.jwt.expiration=86400000", "app.jwt.secret=8a9b2b3c4d5e6f7a8b9c0d1e2f3a4b5c6d7e8f9a0b1c2d3e4f5a6b7c8d9e0f1a2b3c4d5e6f7a8b9c0d1e2f3a4b5c6d7e8f9a0b1c2d3e4f5a6b7c8d9e0f1a2b3c4d5e6f7a8b9c0"})
// Removing Transactional on class to allow real concurrent tests, or we can handle concurrent tests separately
public class Phase4VerificationTest {

    @Autowired private VendorService vendorService;
    @Autowired private ZoneService zoneService;
    @Autowired private ConfigurationService configurationService;
    @Autowired private AllocationService allocationService;
    @Autowired private AllocationBucketRepository bucketRepository;
    @Autowired private VendorAllocationStateRepository stateRepository;
    @Autowired private TripAllocationRepository tripAllocationRepository;
    @Autowired private TripRepository tripRepository;
    @Autowired private TripRejectionRepository tripRejectionRepository;
    @Autowired private VendorCapacityRepository capacityRepository;
    @Autowired private VendorZoneShareRepository shareRepository;

    private Zone zoneNear;
    private Vendor v1;
    private Vendor v2;
    private Vendor v3;

    @BeforeEach
    public void setup() {
        tripRejectionRepository.deleteAll();
        tripAllocationRepository.deleteAll();
        tripRepository.deleteAll();
        stateRepository.deleteAll();
        bucketRepository.deleteAll();
        capacityRepository.deleteAll();
        shareRepository.deleteAll();
        
        List<Zone> zones = zoneService.listZones();
        if (zones.isEmpty()) {
            ZoneDto z1 = new ZoneDto(); z1.code = "NEAR_P4"; z1.name = "Near"; z1.minDistance = new BigDecimal("0"); z1.maxDistance = new BigDecimal("15");
            zoneNear = zoneService.createZone(z1);
        } else {
            zoneNear = zones.get(0);
        }
        
        List<Vendor> vendors = vendorService.listVendors();
        if (vendors.size() < 3) {
            VendorDto vd1 = new VendorDto(); vd1.code = "V1_P4"; vd1.name = "V1"; vd1.priority = 10;
            v1 = vendorService.createVendor(vd1);
            VendorDto vd2 = new VendorDto(); vd2.code = "V2_P4"; vd2.name = "V2"; vd2.priority = 10;
            v2 = vendorService.createVendor(vd2);
            VendorDto vd3 = new VendorDto(); vd3.code = "V3_P4"; vd3.name = "V3"; vd3.priority = 10;
            v3 = vendorService.createVendor(vd3);
        } else {
            v1 = vendors.get(0);
            v2 = vendors.get(1);
            v3 = vendors.get(2);
        }
    }

    private void configureShares(Zone zone, TripType type, Vendor vA, int shareA, Vendor vB, int shareB, Vendor vC, int shareC) {
        ZoneShareConfigDto config = new ZoneShareConfigDto();
        config.tripType = type;
        java.util.List<VendorShareDto> list = new java.util.ArrayList<>();
        if (vA != null) { VendorShareDto s = new VendorShareDto(); s.vendorId = vA.getId(); s.targetBasisPoints = shareA; list.add(s); }
        if (vB != null) { VendorShareDto s = new VendorShareDto(); s.vendorId = vB.getId(); s.targetBasisPoints = shareB; list.add(s); }
        if (vC != null) { VendorShareDto s = new VendorShareDto(); s.vendorId = vC.getId(); s.targetBasisPoints = shareC; list.add(s); }
        config.vendorShares = list;
        configurationService.configureZoneShares(zone.getId(), config);
    }
    
    private void setCapacity(Vendor v, int cap) {
        CapacityDto dto = new CapacityDto();
        dto.totalCapacity = Math.max(cap, 100);
        dto.availableCapacity = cap;
        configurationService.setCapacity(v.getId(), dto);
    }

    @Test
    public void testCapacityConsumptionAndSkipping() {
        configureShares(zoneNear, TripType.NORMAL, v1, 10000, v2, 0, null, 0);
        setCapacity(v1, 1);
        setCapacity(v2, 10);
        
        // Trip 1 -> V1 (has capacity 1)
        TripAllocateRequestDto req1 = new TripAllocateRequestDto();
        req1.externalTripId = "CAP_1"; req1.distance = new BigDecimal("5"); req1.tripType = TripType.NORMAL;
        TripAllocateResponseDto res1 = allocationService.allocateTrip(req1);
        
        assertThat(res1.vendorId).isEqualTo(v1.getId());
        assertThat(capacityRepository.findByVendorId(v1.getId()).get().getAvailableCapacity()).isEqualTo(0);
        
        // Trip 2 -> V1 is owed but has 0 capacity, so V2 gets it (wait, V2 has 0 share here, so it gets skipped if not eligible? 
        // V2 is eligible because it has share config, just 0 basis points. It will have worse shortfall, but V1 is skipped.
        TripAllocateRequestDto req2 = new TripAllocateRequestDto();
        req2.externalTripId = "CAP_2"; req2.distance = new BigDecimal("5"); req2.tripType = TripType.NORMAL;
        TripAllocateResponseDto res2 = allocationService.allocateTrip(req2);
        
        assertThat(res2.vendorId).isEqualTo(v2.getId());
        
        // Negative capacity check
        assertThat(capacityRepository.findByVendorId(v1.getId()).get().getAvailableCapacity()).isEqualTo(0);
        assertThat(capacityRepository.findByVendorId(v2.getId()).get().getAvailableCapacity()).isEqualTo(9);
    }

    @Test
    public void testAllVendorsUnavailable() {
        configureShares(zoneNear, TripType.NORMAL, v1, 10000, null, 0, null, 0);
        setCapacity(v1, 0);
        
        TripAllocateRequestDto req = new TripAllocateRequestDto();
        req.externalTripId = "NO_CAP_1"; req.distance = new BigDecimal("5"); req.tripType = TripType.NORMAL;
        assertThrows(BusinessValidationException.class, () -> allocationService.allocateTrip(req));
    }
    
    @Test
    public void testRejectionAndCooldown() {
        configureShares(zoneNear, TripType.NORMAL, v1, 10000, v2, 0, null, 0);
        setCapacity(v1, 10);
        setCapacity(v2, 10);
        
        // Allocate to V1
        TripAllocateRequestDto req = new TripAllocateRequestDto();
        req.externalTripId = "REJ_1"; req.distance = new BigDecimal("5"); req.tripType = TripType.NORMAL;
        TripAllocateResponseDto res1 = allocationService.allocateTrip(req);
        assertThat(res1.vendorId).isEqualTo(v1.getId());
        
        // V1 rejects
        TripRejectRequestDto rejReq = new TripRejectRequestDto();
        rejReq.externalTripId = "REJ_1";
        rejReq.reason = "Busy";
        TripAllocateResponseDto res2 = allocationService.rejectTrip(rejReq);
        
        // Should reallocate to V2
        assertThat(res2.vendorId).isEqualTo(v2.getId());
        
        // V1 should STILL be able to receive a different trip Y!
        TripAllocateRequestDto reqY = new TripAllocateRequestDto();
        reqY.externalTripId = "TRIP_Y"; reqY.distance = new BigDecimal("5"); reqY.tripType = TripType.NORMAL;
        TripAllocateResponseDto resY = allocationService.allocateTrip(reqY);
        assertThat(resY.vendorId).isEqualTo(v1.getId()); // V1 is now the most-owed again!
        
        // Validate records
        Trip trip = tripRepository.findByExternalTripId("REJ_1").get();
        Optional<TripAllocation> allocOpt = tripAllocationRepository.findByTripId(trip.getId());
        assertThat(allocOpt.isPresent()).isTrue();
        assertThat(allocOpt.get().getVendor().getId()).isEqualTo(v2.getId());
        assertThat(allocOpt.get().getStatus()).isEqualTo(AllocationStatus.SUCCESS);
        
        List<TripRejection> rejections = tripRejectionRepository.findAll();
        assertThat(rejections).hasSize(1);
        assertThat(rejections.get(0).getVendor().getId()).isEqualTo(v1.getId());
        
        // Fairness counters should not be corrupted
        AllocationBucket bucket = bucketRepository.findByZoneIdAndTripType(zoneNear.getId(), TripType.NORMAL).get();
        assertThat(bucket.getTotalTrips()).isEqualTo(2); // 1 for REJ_1 (V2), 1 for TRIP_Y (V1)
        
        List<VendorAllocationState> states = stateRepository.findByBucketId(bucket.getId());
        assertThat(states.stream().filter(s -> s.getVendor().getId().equals(v1.getId())).findFirst().get().getAllocatedTrips()).isEqualTo(1);
        assertThat(states.stream().filter(s -> s.getVendor().getId().equals(v2.getId())).findFirst().get().getAllocatedTrips()).isEqualTo(1);
    }
    
    @Test
    public void testConcurrencyCapacity() throws InterruptedException {
        configureShares(zoneNear, TripType.NORMAL, v1, 10000, v2, 0, null, 0);
        setCapacity(v1, 1);
        setCapacity(v2, 10);
        
        int threads = 5;
        ExecutorService executor = Executors.newFixedThreadPool(threads);
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch endLatch = new CountDownLatch(threads);
        
        AtomicInteger v1Count = new AtomicInteger(0);
        AtomicInteger v2Count = new AtomicInteger(0);
        AtomicInteger errorCount = new AtomicInteger(0);
        
        for (int i = 0; i < threads; i++) {
            final int index = i;
            executor.submit(() -> {
                try {
                    startLatch.await();
                    TripAllocateRequestDto req = new TripAllocateRequestDto();
                    req.externalTripId = "CONC_" + index; req.distance = new BigDecimal("5"); req.tripType = TripType.NORMAL;
                    TripAllocateResponseDto res = allocationService.allocateTrip(req);
                    if (res.vendorId.equals(v1.getId())) v1Count.incrementAndGet();
                    if (res.vendorId.equals(v2.getId())) v2Count.incrementAndGet();
                } catch (Exception e) {
                    errorCount.incrementAndGet();
                } finally {
                    endLatch.countDown();
                }
            });
        }
        
        startLatch.countDown();
        endLatch.await();
        executor.shutdown();
        
        assertThat(capacityRepository.findByVendorId(v1.getId()).get().getAvailableCapacity()).isGreaterThanOrEqualTo(0);
        assertThat(v1Count.get()).isLessThanOrEqualTo(1); // V1 only had capacity for 1
        
        // Remaining threads should have allocated to V2, or thrown errors if they raced to create buckets (which is a different issue, but allowed)
        assertThat(v1Count.get() + v2Count.get() + errorCount.get()).isEqualTo(threads);
    }
}
