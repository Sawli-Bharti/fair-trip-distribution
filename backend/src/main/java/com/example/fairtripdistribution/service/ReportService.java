package com.example.fairtripdistribution.service;

import com.example.fairtripdistribution.model.dto.AllocationCountProjection;
import com.example.fairtripdistribution.model.dto.FairnessReportDto;
import com.example.fairtripdistribution.model.entity.*;
import com.example.fairtripdistribution.repository.*;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class ReportService {

    private final VendorZoneShareRepository shareRepository;
    private final TripAllocationRepository tripAllocationRepository;
    private final VendorAllocationStateRepository stateRepository;
    private final VendorCapacityRepository capacityRepository;
    private final AllocationBucketRepository bucketRepository;

    public ReportService(VendorZoneShareRepository shareRepository,
                         TripAllocationRepository tripAllocationRepository,
                         VendorAllocationStateRepository stateRepository,
                         VendorCapacityRepository capacityRepository,
                         AllocationBucketRepository bucketRepository) {
        this.shareRepository = shareRepository;
        this.tripAllocationRepository = tripAllocationRepository;
        this.stateRepository = stateRepository;
        this.capacityRepository = capacityRepository;
        this.bucketRepository = bucketRepository;
    }

    @Cacheable(value = "dailyReports", key = "#date.toString()")
    @Transactional(readOnly = true)
    public List<FairnessReportDto> getDailyReport(LocalDate date) {
        LocalDateTime start = date.atStartOfDay();
        LocalDateTime end = date.plusDays(1).atStartOfDay();
        return generateReport(start, end);
    }

    @Cacheable(value = "monthlyReports", key = "#year + '-' + #month")
    @Transactional(readOnly = true)
    public List<FairnessReportDto> getMonthlyReport(int year, int month) {
        YearMonth ym = YearMonth.of(year, month);
        LocalDateTime start = ym.atDay(1).atStartOfDay();
        LocalDateTime end = ym.plusMonths(1).atDay(1).atStartOfDay();
        return generateReport(start, end);
    }

    private List<FairnessReportDto> generateReport(LocalDateTime startDate, LocalDateTime endDate) {
        // Load shares with vendor and zone eagerly via JOIN FETCH to avoid LazyInitializationException
        List<VendorZoneShare> allShares = shareRepository.findAllWithVendorAndZone();
        List<AllocationCountProjection> counts = tripAllocationRepository.countAllocationsByPeriod(startDate, endDate);
        List<VendorAllocationState> states = stateRepository.findAllWithBucketAndVendor();
        List<VendorCapacity> capacities = capacityRepository.findAllWithVendor();
        List<AllocationBucket> buckets = bucketRepository.findAllWithZone();

        Map<Long, Map<Long, Map<String, Long>>> vendorZoneTripTypeCounts = new HashMap<>();
        Map<Long, Map<String, Long>> totalBucketCounts = new HashMap<>();

        for (AllocationCountProjection p : counts) {
            vendorZoneTripTypeCounts
                .computeIfAbsent(p.getVendorId(), k -> new HashMap<>())
                .computeIfAbsent(p.getZoneId(), k -> new HashMap<>())
                .put(p.getTripType().name(), p.getTripCount());

            totalBucketCounts
                .computeIfAbsent(p.getZoneId(), k -> new HashMap<>())
                .merge(p.getTripType().name(), p.getTripCount(), Long::sum);
        }

        Map<Long, Integer> capacityMap = capacities.stream()
            .collect(Collectors.toMap(c -> c.getVendor().getId(), VendorCapacity::getAvailableCapacity));

        Map<String, AllocationBucket> bucketMap = buckets.stream()
            .collect(Collectors.toMap(b -> b.getZone().getId() + "-" + b.getTripType().name(), b -> b));

        Map<String, VendorAllocationState> stateMap = states.stream()
            .collect(Collectors.toMap(
                s -> s.getBucket().getId() + "-" + s.getVendor().getId(),
                s -> s
            ));

        List<FairnessReportDto> report = new ArrayList<>();

        for (VendorZoneShare share : allShares) {
            Vendor vendor = share.getVendor();
            Zone zone = share.getZone();
            String type = share.getTripType().name();

            long actualTrips = vendorZoneTripTypeCounts
                .getOrDefault(vendor.getId(), Collections.emptyMap())
                .getOrDefault(zone.getId(), Collections.emptyMap())
                .getOrDefault(type, 0L);

            long totalTripsInBucket = totalBucketCounts
                .getOrDefault(zone.getId(), Collections.emptyMap())
                .getOrDefault(type, 0L);

            double actualPercentage = 0.0;
            if (totalTripsInBucket > 0) {
                actualPercentage = (actualTrips * 100.0) / totalTripsInBucket;
            }

            double promisedPercentage = share.getTargetBasisPoints() / 100.0;
            double expectedTrips = (totalTripsInBucket * share.getTargetBasisPoints()) / 10000.0;

            AllocationBucket bucket = bucketMap.get(zone.getId() + "-" + type);
            long runningShortfall = 0;
            if (bucket != null) {
                VendorAllocationState state = stateMap.get(bucket.getId() + "-" + vendor.getId());
                long allocatedTrips = state != null ? state.getAllocatedTrips() : 0;
                runningShortfall = (bucket.getTotalTrips() * share.getTargetBasisPoints()) - (allocatedTrips * 10000);
            }

            FairnessReportDto dto = new FairnessReportDto();
            dto.vendorCode = vendor.getCode();
            dto.vendorName = vendor.getName();
            dto.zoneCode = zone.getCode();
            dto.tripType = type;
            dto.promisedPercentage = promisedPercentage;
            dto.actualTrips = actualTrips;
            dto.actualPercentage = actualPercentage;
            dto.expectedTrips = expectedTrips;
            dto.runningShortfall = runningShortfall;
            dto.currentCapacity = capacityMap.get(vendor.getId());

            report.add(dto);
        }

        return report;
    }
}
