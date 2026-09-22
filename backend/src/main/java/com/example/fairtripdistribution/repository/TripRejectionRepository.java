package com.example.fairtripdistribution.repository;
import com.example.fairtripdistribution.model.entity.TripRejection;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.time.LocalDateTime;

public interface TripRejectionRepository extends JpaRepository<TripRejection, Long> {
    
    // Check if the vendor is in cooldown for a specific trip
    @Query("SELECT COUNT(r) > 0 FROM TripRejection r WHERE r.vendor.id = :vendorId AND r.trip.externalTripId = :externalTripId AND r.cooldownUntil > :now")
    boolean isVendorInCooldownForTrip(@Param("vendorId") Long vendorId, @Param("externalTripId") String externalTripId, @Param("now") LocalDateTime now);
}
