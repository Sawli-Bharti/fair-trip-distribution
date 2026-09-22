package com.example.fairtripdistribution.repository;

import com.example.fairtripdistribution.model.entity.TripAllocation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface TripAllocationRepository extends JpaRepository<TripAllocation, Long> {

    java.util.Optional<TripAllocation> findByTripId(Long tripId);

}
