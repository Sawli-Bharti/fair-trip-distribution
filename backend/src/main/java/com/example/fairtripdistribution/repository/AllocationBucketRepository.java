package com.example.fairtripdistribution.repository;

import com.example.fairtripdistribution.model.entity.AllocationBucket;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface AllocationBucketRepository extends JpaRepository<AllocationBucket, Long> {

    java.util.Optional<AllocationBucket> findByZoneIdAndTripType(Long zoneId, com.example.fairtripdistribution.model.entity.enums.TripType tripType);
    
    @org.springframework.data.jpa.repository.Lock(jakarta.persistence.LockModeType.PESSIMISTIC_WRITE)
    @org.springframework.data.jpa.repository.Query("SELECT b FROM AllocationBucket b WHERE b.zone.id = :zoneId AND b.tripType = :tripType")
    java.util.Optional<AllocationBucket> findByZoneIdAndTripTypeForUpdate(@org.springframework.data.repository.query.Param("zoneId") Long zoneId, @org.springframework.data.repository.query.Param("tripType") com.example.fairtripdistribution.model.entity.enums.TripType tripType);

}
