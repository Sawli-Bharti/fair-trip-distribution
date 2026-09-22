package com.example.fairtripdistribution.repository;

import com.example.fairtripdistribution.model.entity.Trip;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface TripRepository extends JpaRepository<Trip, Long> {

    java.util.Optional<Trip> findByExternalTripId(String externalTripId);
    
    
    boolean existsByExternalTripId(String externalTripId);

}
