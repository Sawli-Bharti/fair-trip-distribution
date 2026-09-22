package com.example.fairtripdistribution.model.entity;

import jakarta.persistence.*;

@Entity
@Table(name = "vendor_allocation_state", uniqueConstraints = {
    @UniqueConstraint(columnNames = {"bucket_id", "vendor_id"})
})
public class VendorAllocationState {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "bucket_id", nullable = false)
    private AllocationBucket bucket;
    
    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "vendor_id", nullable = false)
    private Vendor vendor;
    
    @Column(nullable = false)
    private long allocatedTrips = 0;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public AllocationBucket getBucket() { return bucket; }
    public void setBucket(AllocationBucket bucket) { this.bucket = bucket; }
    public Vendor getVendor() { return vendor; }
    public void setVendor(Vendor vendor) { this.vendor = vendor; }
    public long getAllocatedTrips() { return allocatedTrips; }
    public void setAllocatedTrips(long allocatedTrips) { this.allocatedTrips = allocatedTrips; }
}
