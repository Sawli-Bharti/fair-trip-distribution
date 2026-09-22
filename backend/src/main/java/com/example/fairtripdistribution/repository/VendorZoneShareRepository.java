package com.example.fairtripdistribution.repository;

import com.example.fairtripdistribution.model.entity.VendorZoneShare;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface VendorZoneShareRepository extends JpaRepository<VendorZoneShare, Long> {

    java.util.List<VendorZoneShare> findByZoneIdAndTripType(Long zoneId, com.example.fairtripdistribution.model.entity.enums.TripType tripType);

}
