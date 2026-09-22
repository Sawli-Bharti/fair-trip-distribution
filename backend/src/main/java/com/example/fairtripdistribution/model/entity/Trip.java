package com.example.fairtripdistribution.model.entity;

import com.example.fairtripdistribution.model.entity.enums.TripStatus;
import com.example.fairtripdistribution.model.entity.enums.TripType;
import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "trips", indexes = {
    @Index(name = "idx_trip_zone_type", columnList = "zone_id, trip_type"),
    @Index(name = "idx_trip_status", columnList = "status")
})
public class Trip {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(name = "external_trip_id", unique = true, nullable = false)
    private String externalTripId;
    
    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "zone_id", nullable = false)
    private Zone zone;
    
    @Enumerated(EnumType.STRING)
    @Column(name = "trip_type", nullable = false)
    private TripType tripType;
    
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TripStatus status;
    
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getExternalTripId() { return externalTripId; }
    public void setExternalTripId(String externalTripId) { this.externalTripId = externalTripId; }
    public Zone getZone() { return zone; }
    public void setZone(Zone zone) { this.zone = zone; }
    public TripType getTripType() { return tripType; }
    public void setTripType(TripType tripType) { this.tripType = tripType; }
    public TripStatus getStatus() { return status; }
    public void setStatus(TripStatus status) { this.status = status; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}
