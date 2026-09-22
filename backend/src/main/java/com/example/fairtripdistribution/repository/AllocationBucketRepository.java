package com.example.fairtripdistribution.repository;

import com.example.fairtripdistribution.model.entity.AllocationBucket;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface AllocationBucketRepository extends JpaRepository<AllocationBucket, Long> {

    java.util.Optional<AllocationBucket> findByZoneIdAndTripType(Long zoneId, com.example.fairtripdistribution.model.entity.enums.TripType tripType);

}
