package com.example.fairtripdistribution.repository;

import com.example.fairtripdistribution.model.entity.TripAllocation;
import com.example.fairtripdistribution.model.dto.AllocationCountProjection;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface TripAllocationRepository extends JpaRepository<TripAllocation, Long> {

    Optional<TripAllocation> findByTripId(Long tripId);

    @Query("SELECT new com.example.fairtripdistribution.model.dto.AllocationCountProjection(ta.vendor.id, t.zone.id, t.tripType, COUNT(ta.id)) " +
           "FROM TripAllocation ta JOIN ta.trip t " +
           "WHERE ta.status = com.example.fairtripdistribution.model.entity.enums.AllocationStatus.SUCCESS " +
           "AND ta.allocatedAt >= :startDate AND ta.allocatedAt < :endDate " +
           "GROUP BY ta.vendor.id, t.zone.id, t.tripType")
    List<AllocationCountProjection> countAllocationsByPeriod(@Param("startDate") LocalDateTime startDate, @Param("endDate") LocalDateTime endDate);
}
