package com.example.fairtripdistribution.model.entity;

import com.example.fairtripdistribution.model.entity.enums.AllocationStatus;
import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "trip_allocations", indexes = {
    @Index(name = "idx_trip_alloc_vendor", columnList = "vendor_id")
})
public class TripAllocation {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @OneToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "trip_id", unique = true, nullable = false)
    private Trip trip;
    
    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "vendor_id", nullable = false)
    private Vendor vendor;
    
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private AllocationStatus status;
    
    @Column(nullable = false)
    private LocalDateTime allocatedAt = LocalDateTime.now();

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Trip getTrip() { return trip; }
    public void setTrip(Trip trip) { this.trip = trip; }
    public Vendor getVendor() { return vendor; }
    public void setVendor(Vendor vendor) { this.vendor = vendor; }
    public AllocationStatus getStatus() { return status; }
    public void setStatus(AllocationStatus status) { this.status = status; }
    public LocalDateTime getAllocatedAt() { return allocatedAt; }
    public void setAllocatedAt(LocalDateTime allocatedAt) { this.allocatedAt = allocatedAt; }
}
