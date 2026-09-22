package com.example.fairtripdistribution.service;

import com.example.fairtripdistribution.model.dto.*;
import com.example.fairtripdistribution.model.entity.*;
import com.example.fairtripdistribution.model.entity.enums.TripType;
import com.example.fairtripdistribution.repository.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.cache.CacheManager;
import org.springframework.cache.concurrent.ConcurrentMapCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(properties = {
    "app.jwt.expiration=86400000",
    "app.jwt.secret=8a9b2b3c4d5e6f7a8b9c0d1e2f3a4b5c6d7e8f9a0b1c2d3e4f5a6b7c8d9e0f1a2b3c4d5e6f7a8b9c0d1e2f3a4b5c6d7e8f9a0b1c2d3e4f5a6b7c8d9e0f1a2b3c4d5e6f7a8b9c0",
    "spring.main.allow-bean-definition-overriding=true"
})
public class Phase6BReportTest {

    // Override the Redis CacheManager with a simple in-memory one for this test class only.
    // Redis caching behaviour is separately verified in Phase6ACacheLogicTest and
    // Phase6ARedisFallbackTest. This keeps reporting tests independent of a live Redis server.
    @TestConfiguration
    static class InMemoryCacheConfig {
        @Bean
        @Primary
        public CacheManager cacheManager() {
            return new ConcurrentMapCacheManager(
                "vendors", "activeZones", "vendorZoneShares", "dailyReports", "monthlyReports"
            );
        }
    }

    @Autowired private ReportService reportService;
    @Autowired private AllocationService allocationService;
    @Autowired private VendorRepository vendorRepository;
    @Autowired private ZoneRepository zoneRepository;
    @Autowired private VendorZoneShareRepository shareRepository;
    @Autowired private TripRepository tripRepository;
    @Autowired private TripAllocationRepository tripAllocationRepository;
    @Autowired private AllocationBucketRepository bucketRepository;
    @Autowired private VendorAllocationStateRepository stateRepository;
    @Autowired private CacheManager cacheManager;

    private Zone zone;
    private Vendor v1, v2, v3;

    @BeforeEach
    public void setup() {
        tripAllocationRepository.deleteAll();
        tripRepository.deleteAll();
        stateRepository.deleteAll();
        bucketRepository.deleteAll();
        shareRepository.deleteAll();
        vendorRepository.deleteAll();
        zoneRepository.deleteAll();

        // Safe: ConcurrentMapCacheManager never throws on clear()
        if (cacheManager.getCache("dailyReports") != null)     cacheManager.getCache("dailyReports").clear();
        if (cacheManager.getCache("monthlyReports") != null)   cacheManager.getCache("monthlyReports").clear();
        if (cacheManager.getCache("vendors") != null)          cacheManager.getCache("vendors").clear();
        if (cacheManager.getCache("activeZones") != null)      cacheManager.getCache("activeZones").clear();
        if (cacheManager.getCache("vendorZoneShares") != null) cacheManager.getCache("vendorZoneShares").clear();

        zone = new Zone();
        zone.setCode("TEST");
        zone.setName("Test Zone");
        zone.setMinDistance(BigDecimal.ZERO);
        zone.setMaxDistance(BigDecimal.valueOf(10));
        zone.setActive(true);
        zone = zoneRepository.save(zone);

        v1 = createVendor("V1");
        v2 = createVendor("V2");
        v3 = createVendor("V3");

        createShare(v1, zone, TripType.NORMAL, 5000); // 50%
        createShare(v2, zone, TripType.NORMAL, 3000); // 30%
        createShare(v3, zone, TripType.NORMAL, 2000); // 20%
    }

    private Vendor createVendor(String code) {
        Vendor v = new Vendor();
        v.setCode(code);
        v.setName(code);
        v.setActive(true);
        return vendorRepository.save(v);
    }

    private void createShare(Vendor v, Zone z, TripType type, int bp) {
        VendorZoneShare s = new VendorZoneShare();
        s.setVendor(v);
        s.setZone(z);
        s.setTripType(type);
        s.setTargetBasisPoints(bp);
        shareRepository.save(s);
    }

    @Test
    public void testEmptyReportHandling() {
        List<FairnessReportDto> report = reportService.getDailyReport(LocalDate.now());
        assertThat(report).hasSize(3);

        FairnessReportDto r1 = report.stream().filter(r -> r.vendorCode.equals("V1")).findFirst().get();
        assertThat(r1.actualTrips).isEqualTo(0);
        assertThat(r1.actualPercentage).isEqualTo(0.0);
        assertThat(r1.expectedTrips).isEqualTo(0.0);
        assertThat(r1.runningShortfall).isEqualTo(0);
    }

    @Test
    public void testCorrect50_30_20ReportAndShortfallCalculation() {
        for (int i = 0; i < 10; i++) {
            TripAllocateRequestDto req = new TripAllocateRequestDto();
            req.externalTripId = UUID.randomUUID().toString();
            req.distance = BigDecimal.valueOf(5);
            req.tripType = TripType.NORMAL;
            allocationService.allocateTrip(req);
        }

        List<FairnessReportDto> report = reportService.getDailyReport(LocalDate.now());
        assertThat(report).hasSize(3);

        FairnessReportDto r1 = report.stream().filter(r -> r.vendorCode.equals("V1")).findFirst().get();
        FairnessReportDto r2 = report.stream().filter(r -> r.vendorCode.equals("V2")).findFirst().get();
        FairnessReportDto r3 = report.stream().filter(r -> r.vendorCode.equals("V3")).findFirst().get();

        // 10 trips total: 50%=5, 30%=3, 20%=2
        assertThat(r1.actualTrips).isEqualTo(5);
        assertThat(r1.expectedTrips).isEqualTo(5.0);
        assertThat(r1.actualPercentage).isEqualTo(50.0);

        assertThat(r2.actualTrips).isEqualTo(3);
        assertThat(r2.expectedTrips).isEqualTo(3.0);
        assertThat(r2.actualPercentage).isEqualTo(30.0);

        assertThat(r3.actualTrips).isEqualTo(2);
        assertThat(r3.expectedTrips).isEqualTo(2.0);
        assertThat(r3.actualPercentage).isEqualTo(20.0);

        // Running shortfall = 0 when allocations exactly match promised shares
        assertThat(r1.runningShortfall).isEqualTo(0);
        assertThat(r2.runningShortfall).isEqualTo(0);
        assertThat(r3.runningShortfall).isEqualTo(0);
    }

    @Test
    public void testDailyAndMonthlyFilteringAndCacheInvalidation() {
        // Initial call populates cache with empty result
        List<FairnessReportDto> initialReport = reportService.getDailyReport(LocalDate.now());
        assertThat(initialReport).allMatch(r -> r.actualTrips == 0);

        // New allocation → @CacheEvict on AllocationService evicts dailyReports and monthlyReports
        TripAllocateRequestDto req = new TripAllocateRequestDto();
        req.externalTripId = UUID.randomUUID().toString();
        req.distance = BigDecimal.valueOf(5);
        req.tripType = TripType.NORMAL;
        allocationService.allocateTrip(req);

        // Daily cache was evicted; fresh data shows 1 trip total
        List<FairnessReportDto> updatedDaily = reportService.getDailyReport(LocalDate.now());
        long totalDaily = updatedDaily.stream().mapToLong(r -> r.actualTrips).sum();
        assertThat(totalDaily).isEqualTo(1);

        // Monthly report also reflects the allocation
        List<FairnessReportDto> updatedMonthly = reportService.getMonthlyReport(
            LocalDate.now().getYear(), LocalDate.now().getMonthValue());
        long totalMonthly = updatedMonthly.stream().mapToLong(r -> r.actualTrips).sum();
        assertThat(totalMonthly).isEqualTo(1);
    }
}
