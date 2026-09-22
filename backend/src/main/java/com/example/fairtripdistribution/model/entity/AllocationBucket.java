package com.example.fairtripdistribution.model.entity;

import com.example.fairtripdistribution.model.entity.enums.TripType;
import jakarta.persistence.*;

@Entity
@Table(name = "allocation_buckets", uniqueConstraints = {
    @UniqueConstraint(columnNames = {"zone_id", "trip_type"})
})
public class AllocationBucket {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "zone_id", nullable = false)
    private Zone zone;
    
    @Enumerated(EnumType.STRING)
    @Column(name = "trip_type", nullable = false)
    private TripType tripType;
    
    @Column(nullable = false)
    private long totalTrips = 0;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Zone getZone() { return zone; }
    public void setZone(Zone zone) { this.zone = zone; }
    public TripType getTripType() { return tripType; }
    public void setTripType(TripType tripType) { this.tripType = tripType; }
    public long getTotalTrips() { return totalTrips; }
    public void setTotalTrips(long totalTrips) { this.totalTrips = totalTrips; }
}
