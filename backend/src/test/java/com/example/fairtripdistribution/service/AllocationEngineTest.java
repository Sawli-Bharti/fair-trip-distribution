package com.example.fairtripdistribution.service;

import com.example.fairtripdistribution.exception.BusinessValidationException;
import com.example.fairtripdistribution.model.dto.*;
import com.example.fairtripdistribution.model.entity.*;
import com.example.fairtripdistribution.model.entity.enums.TripType;
import com.example.fairtripdistribution.repository.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.Map;
import java.util.HashMap;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

@SpringBootTest(properties = {"app.jwt.expiration=86400000", "app.jwt.secret=8a9b2b3c4d5e6f7a8b9c0d1e2f3a4b5c6d7e8f9a0b1c2d3e4f5a6b7c8d9e0f1a2b3c4d5e6f7a8b9c0d1e2f3a4b5c6d7e8f9a0b1c2d3e4f5a6b7c8d9e0f1a2b3c4d5e6f7a8b9c0"})
@Transactional
public class AllocationEngineTest {

    @Autowired private VendorService vendorService;
    @Autowired private ZoneService zoneService;
    @Autowired private ConfigurationService configurationService;
    @Autowired private AllocationService allocationService;
    @Autowired private AllocationBucketRepository bucketRepository;
    @Autowired private VendorAllocationStateRepository stateRepository;

    private Zone zoneNear;
    private Zone zoneFar;
    
    private Vendor v1;
    private Vendor v2;
    private Vendor v3;

    @BeforeEach
    public void setup() {
        ZoneDto z1 = new ZoneDto(); z1.code = "NEAR"; z1.name = "Near"; z1.minDistance = new BigDecimal("0"); z1.maxDistance = new BigDecimal("15");
        zoneNear = zoneService.createZone(z1);
        
        ZoneDto z2 = new ZoneDto(); z2.code = "FAR"; z2.name = "Far"; z2.minDistance = new BigDecimal("25"); z2.maxDistance = null;
        zoneFar = zoneService.createZone(z2);

        VendorDto vd1 = new VendorDto(); vd1.code = "V1"; vd1.name = "V1"; vd1.priority = 10;
        v1 = vendorService.createVendor(vd1);
        
        VendorDto vd2 = new VendorDto(); vd2.code = "V2"; vd2.name = "V2"; vd2.priority = 10;
        v2 = vendorService.createVendor(vd2);
        
        VendorDto vd3 = new VendorDto(); vd3.code = "V3"; vd3.name = "V3"; vd3.priority = 10;
        v3 = vendorService.createVendor(vd3);
    }

    @Test
    public void testBasicFairness() {
        // V1 = 50%, V2 = 30%, V3 = 20%
        configureShares(zoneNear, TripType.NORMAL, v1, 5000, v2, 3000, v3, 2000);

        Map<Long, Integer> counts = new HashMap<>();
        for (int i = 0; i < 10; i++) {
            TripAllocateRequestDto req = new TripAllocateRequestDto();
            req.externalTripId = "TRIP_" + i;
            req.distance = new BigDecimal("5.0"); // NEAR
            req.tripType = TripType.NORMAL;
            
            TripAllocateResponseDto res = allocationService.allocateTrip(req);
            counts.put(res.vendorId, counts.getOrDefault(res.vendorId, 0) + 1);
        }

        assertThat(counts.get(v1.getId())).isEqualTo(5);
        assertThat(counts.get(v2.getId())).isEqualTo(3);
        assertThat(counts.get(v3.getId())).isEqualTo(2);
    }

    @Test
    public void testIdempotency() {
        configureShares(zoneNear, TripType.NORMAL, v1, 10000, null, 0, null, 0);

        TripAllocateRequestDto req = new TripAllocateRequestDto();
        req.externalTripId = "IDEMP_TRIP";
        req.distance = new BigDecimal("5.0");
        req.tripType = TripType.NORMAL;
        
        TripAllocateResponseDto res1 = allocationService.allocateTrip(req);
        TripAllocateResponseDto res2 = allocationService.allocateTrip(req); // Submit again

        assertThat(res1.tripId).isEqualTo(res2.tripId);
        assertThat(res1.vendorId).isEqualTo(res2.vendorId);
        
        AllocationBucket bucket = bucketRepository.findByZoneIdAndTripType(zoneNear.getId(), TripType.NORMAL).get();
        assertThat(bucket.getTotalTrips()).isEqualTo(1); // Should only increment once
    }

    @Test
    public void testSeparateStreams() {
        configureShares(zoneNear, TripType.NORMAL, v1, 10000, null, 0, null, 0);
        configureShares(zoneNear, TripType.ESCORT, v2, 10000, null, 0, null, 0);

        TripAllocateRequestDto req1 = new TripAllocateRequestDto();
        req1.externalTripId = "N1"; req1.distance = new BigDecimal("5.0"); req1.tripType = TripType.NORMAL;
        TripAllocateResponseDto res1 = allocationService.allocateTrip(req1);
        
        TripAllocateRequestDto req2 = new TripAllocateRequestDto();
        req2.externalTripId = "E1"; req2.distance = new BigDecimal("5.0"); req2.tripType = TripType.ESCORT;
        TripAllocateResponseDto res2 = allocationService.allocateTrip(req2);

        assertThat(res1.vendorId).isEqualTo(v1.getId());
        assertThat(res2.vendorId).isEqualTo(v2.getId());
        
        AllocationBucket b1 = bucketRepository.findByZoneIdAndTripType(zoneNear.getId(), TripType.NORMAL).get();
        AllocationBucket b2 = bucketRepository.findByZoneIdAndTripType(zoneNear.getId(), TripType.ESCORT).get();
        
        assertThat(b1.getTotalTrips()).isEqualTo(1);
        assertThat(b2.getTotalTrips()).isEqualTo(1);
    }

    @Test
    public void testDeterministicTiePriority() {
        // Equal shares and currently 0 trips allocated
        configureShares(zoneNear, TripType.NORMAL, v1, 5000, v2, 5000, null, 0);
        
        VendorDto vd2 = new VendorDto(); vd2.code = v2.getCode(); vd2.name = v2.getName(); vd2.isActive = true;
        vd2.priority = 1; // V2 has better (lower) priority
        vendorService.updateVendor(v2.getId(), vd2);

        TripAllocateRequestDto req = new TripAllocateRequestDto();
        req.externalTripId = "TIE_TRIP";
        req.distance = new BigDecimal("5.0");
        req.tripType = TripType.NORMAL;
        
        TripAllocateResponseDto res = allocationService.allocateTrip(req);
        assertThat(res.vendorId).isEqualTo(v2.getId()); // V2 should win due to priority
    }

    @Test
    public void testInvalidConfiguration() {
        TripAllocateRequestDto req = new TripAllocateRequestDto();
        req.externalTripId = "INV_TRIP";
        req.distance = new BigDecimal("5.0");
        req.tripType = TripType.NORMAL;
        
        // No shares configured yet
        assertThrows(BusinessValidationException.class, () -> {
            allocationService.allocateTrip(req);
        });
    }
    
    @Test
    public void testCarryForwardAndMostOwedFirst() {
        configureShares(zoneNear, TripType.NORMAL, v1, 5000, v2, 5000, null, 0);
        
        // Setup initial state directly to simulate carry forward
        // v1 has 10 allocated, v2 has 5 allocated.
        AllocationBucket bucket = new AllocationBucket();
        bucket.setZone(zoneNear);
        bucket.setTripType(TripType.NORMAL);
        bucket.setTotalTrips(15);
        bucket = bucketRepository.save(bucket);
        
        VendorAllocationState s1 = new VendorAllocationState();
        s1.setBucket(bucket); s1.setVendor(v1); s1.setAllocatedTrips(10);
        stateRepository.save(s1);
        
        VendorAllocationState s2 = new VendorAllocationState();
        s2.setBucket(bucket); s2.setVendor(v2); s2.setAllocatedTrips(5);
        stateRepository.save(s2);
        
        // V2 should have a much larger positive shortfall
        TripAllocateRequestDto req = new TripAllocateRequestDto();
        req.externalTripId = "MOF_TRIP";
        req.distance = new BigDecimal("5.0");
        req.tripType = TripType.NORMAL;
        
        TripAllocateResponseDto res = allocationService.allocateTrip(req);
        assertThat(res.vendorId).isEqualTo(v2.getId());
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
}
