package com.example.fairtripdistribution.model.entity;

import com.example.fairtripdistribution.model.entity.enums.TripType;
import jakarta.persistence.*;
import java.io.Serial;
import java.io.Serializable;

@Entity
@Table(name = "vendor_zone_shares", uniqueConstraints = {
    @UniqueConstraint(columnNames = {"vendor_id", "zone_id", "trip_type"})
})
public class VendorZoneShare implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @ManyToOne(optional = false, fetch = FetchType.EAGER)
    @JoinColumn(name = "vendor_id", nullable = false)
    private Vendor vendor;
    
    @ManyToOne(optional = false, fetch = FetchType.EAGER)
    @JoinColumn(name = "zone_id", nullable = false)
    private Zone zone;
    
    @Enumerated(EnumType.STRING)
    @Column(name = "trip_type", nullable = false)
    private TripType tripType;
    
    @Column(nullable = false)
    private int targetBasisPoints;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Vendor getVendor() { return vendor; }
    public void setVendor(Vendor vendor) { this.vendor = vendor; }
    public Zone getZone() { return zone; }
    public void setZone(Zone zone) { this.zone = zone; }
    public TripType getTripType() { return tripType; }
    public void setTripType(TripType tripType) { this.tripType = tripType; }
    public int getTargetBasisPoints() { return targetBasisPoints; }
    public void setTargetBasisPoints(int targetBasisPoints) { this.targetBasisPoints = targetBasisPoints; }
}
