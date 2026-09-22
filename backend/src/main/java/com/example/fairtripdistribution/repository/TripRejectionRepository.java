package com.example.fairtripdistribution.repository;

import com.example.fairtripdistribution.model.entity.TripRejection;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface TripRejectionRepository extends JpaRepository<TripRejection, Long> {

    java.util.List<TripRejection> findByTripId(Long tripId);
    java.util.Optional<TripRejection> findByTripIdAndVendorId(Long tripId, Long vendorId);

}
