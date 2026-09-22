package com.example.fairtripdistribution.repository;
import com.example.fairtripdistribution.model.entity.VendorZoneShare;
import com.example.fairtripdistribution.model.entity.enums.TripType;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
public interface VendorZoneShareRepository extends JpaRepository<VendorZoneShare, Long> {
    List<VendorZoneShare> findByZoneIdAndTripType(Long zoneId, TripType tripType);
    void deleteByZoneIdAndTripType(Long zoneId, TripType tripType);
}
