package com.example.fairtripdistribution.repository;

import com.example.fairtripdistribution.model.entity.AllocationBucket;
import com.example.fairtripdistribution.model.entity.enums.TripType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import jakarta.persistence.LockModeType;
import java.util.List;
import java.util.Optional;

@Repository
public interface AllocationBucketRepository extends JpaRepository<AllocationBucket, Long> {

    // Used by existing Phase 3/4 tests
    Optional<AllocationBucket> findByZoneIdAndTripType(Long zoneId, TripType tripType);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT b FROM AllocationBucket b WHERE b.zone.id = :zoneId AND b.tripType = :tripType")
    Optional<AllocationBucket> findByZoneIdAndTripTypeForUpdate(@Param("zoneId") Long zoneId, @Param("tripType") TripType tripType);

    // Used by ReportService - eagerly loads zone to avoid LazyInitializationException
    @Query("SELECT b FROM AllocationBucket b JOIN FETCH b.zone")
    List<AllocationBucket> findAllWithZone();
}
