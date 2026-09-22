package com.example.fairtripdistribution.repository;
import com.example.fairtripdistribution.model.entity.VendorZoneShare;
import com.example.fairtripdistribution.model.entity.enums.TripType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import java.util.List;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.CacheEvict;
public interface VendorZoneShareRepository extends JpaRepository<VendorZoneShare, Long> {
    @Cacheable(value = "vendorZoneShares", key = "#zoneId + '-' + #tripType")
    List<VendorZoneShare> findByZoneIdAndTripType(Long zoneId, TripType tripType);
    void deleteByZoneIdAndTripType(Long zoneId, TripType tripType);
    @Query("SELECT s FROM VendorZoneShare s JOIN FETCH s.vendor JOIN FETCH s.zone")
    List<VendorZoneShare> findAllWithVendorAndZone();
}
