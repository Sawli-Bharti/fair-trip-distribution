import os

base_dir = 'backend/src/main/java/com/example/fairtripdistribution'
entity_dir = os.path.join(base_dir, 'model', 'entity')
enum_dir = os.path.join(entity_dir, 'enums')
repo_dir = os.path.join(base_dir, 'repository')

os.makedirs(enum_dir, exist_ok=True)
os.makedirs(repo_dir, exist_ok=True)

enums = {
    'Role': 'ADMIN, VENDOR_USER',
    'TripType': 'NORMAL, ESCORT',
    'TripStatus': 'PENDING, ALLOCATED, COMPLETED, CANCELLED',
    'AllocationStatus': 'SUCCESS, FAILED, REJECTED'
}

for name, values in enums.items():
    with open(os.path.join(enum_dir, f'{name}.py'), 'w') as f:
        pass # just to create dir, wait I'll write java
    with open(os.path.join(enum_dir, f'{name}.java'), 'w') as f:
        f.write(f'''package com.example.fairtripdistribution.model.entity.enums;

public enum {name} {{
    {values}
}}
''')

entities = {
    'User': '''
import com.example.fairtripdistribution.model.entity.enums.Role;
import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "users")
public class User {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(unique = true, nullable = false)
    private String email;
    
    @Column(nullable = false)
    private String passwordHash;
    
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Role role;
    
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    public String getPasswordHash() { return passwordHash; }
    public void setPasswordHash(String passwordHash) { this.passwordHash = passwordHash; }
    public Role getRole() { return role; }
    public void setRole(Role role) { this.role = role; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}''',
    'Vendor': '''
import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "vendors")
public class Vendor {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(unique = true, nullable = false)
    private String code;
    
    @Column(nullable = false)
    private String name;
    
    @Column(nullable = false)
    private boolean isActive = true;
    
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getCode() { return code; }
    public void setCode(String code) { this.code = code; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public boolean isActive() { return isActive; }
    public void setActive(boolean active) { isActive = active; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}''',
    'Zone': '''
import jakarta.persistence.*;

@Entity
@Table(name = "zones")
public class Zone {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(unique = true, nullable = false)
    private String code;
    
    @Column(nullable = false)
    private String name;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getCode() { return code; }
    public void setCode(String code) { this.code = code; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
}''',
    'VendorZoneShare': '''
import com.example.fairtripdistribution.model.entity.enums.TripType;
import jakarta.persistence.*;

@Entity
@Table(name = "vendor_zone_shares", uniqueConstraints = {
    @UniqueConstraint(columnNames = {"vendor_id", "zone_id", "trip_type"})
})
public class VendorZoneShare {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "vendor_id", nullable = false)
    private Vendor vendor;
    
    @ManyToOne(optional = false, fetch = FetchType.LAZY)
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
}''',
    'Trip': '''
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
}''',
    'VendorCapacity': '''
import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "vendor_capacity")
public class VendorCapacity {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @OneToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "vendor_id", unique = true, nullable = false)
    private Vendor vendor;
    
    @Column(nullable = false)
    private int availableCapacity;
    
    @Column(nullable = false)
    private LocalDateTime lastUpdated = LocalDateTime.now();

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Vendor getVendor() { return vendor; }
    public void setVendor(Vendor vendor) { this.vendor = vendor; }
    public int getAvailableCapacity() { return availableCapacity; }
    public void setAvailableCapacity(int availableCapacity) { this.availableCapacity = availableCapacity; }
    public LocalDateTime getLastUpdated() { return lastUpdated; }
    public void setLastUpdated(LocalDateTime lastUpdated) { this.lastUpdated = lastUpdated; }
}''',
    'AllocationBucket': '''
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
}''',
    'VendorAllocationState': '''
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
}''',
    'TripAllocation': '''
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
}''',
    'TripRejection': '''
import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "trip_rejections", indexes = {
    @Index(name = "idx_trip_rej_trip", columnList = "trip_id"),
    @Index(name = "idx_trip_rej_vendor", columnList = "vendor_id")
})
public class TripRejection {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "trip_id", nullable = false)
    private Trip trip;
    
    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "vendor_id", nullable = false)
    private Vendor vendor;
    
    @Column(nullable = false)
    private LocalDateTime rejectedAt = LocalDateTime.now();
    
    @Column(nullable = false)
    private LocalDateTime cooldownUntil;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Trip getTrip() { return trip; }
    public void setTrip(Trip trip) { this.trip = trip; }
    public Vendor getVendor() { return vendor; }
    public void setVendor(Vendor vendor) { this.vendor = vendor; }
    public LocalDateTime getRejectedAt() { return rejectedAt; }
    public void setRejectedAt(LocalDateTime rejectedAt) { this.rejectedAt = rejectedAt; }
    public LocalDateTime getCooldownUntil() { return cooldownUntil; }
    public void setCooldownUntil(LocalDateTime cooldownUntil) { this.cooldownUntil = cooldownUntil; }
}'''
}

for name, body in entities.items():
    with open(os.path.join(entity_dir, f'{name}.java'), 'w') as f:
        f.write(f'''package com.example.fairtripdistribution.model.entity;
{body}
''')

repos = {
    'UserRepository': 'User, Long',
    'VendorRepository': 'Vendor, Long',
    'ZoneRepository': 'Zone, Long',
    'VendorZoneShareRepository': 'VendorZoneShare, Long',
    'TripRepository': 'Trip, Long',
    'VendorCapacityRepository': 'VendorCapacity, Long',
    'AllocationBucketRepository': 'AllocationBucket, Long',
    'VendorAllocationStateRepository': 'VendorAllocationState, Long',
    'TripAllocationRepository': 'TripAllocation, Long',
    'TripRejectionRepository': 'TripRejection, Long'
}

repo_methods = {
    'VendorZoneShareRepository': '''
    java.util.List<VendorZoneShare> findByZoneIdAndTripType(Long zoneId, com.example.fairtripdistribution.model.entity.enums.TripType tripType);
''',
    'VendorCapacityRepository': '''
    java.util.Optional<VendorCapacity> findByVendorId(Long vendorId);
''',
    'AllocationBucketRepository': '''
    java.util.Optional<AllocationBucket> findByZoneIdAndTripType(Long zoneId, com.example.fairtripdistribution.model.entity.enums.TripType tripType);
''',
    'VendorAllocationStateRepository': '''
    java.util.Optional<VendorAllocationState> findByBucketIdAndVendorId(Long bucketId, Long vendorId);
    java.util.List<VendorAllocationState> findByBucketId(Long bucketId);
''',
    'TripRepository': '''
    java.util.Optional<Trip> findByExternalTripId(String externalTripId);
    boolean existsByExternalTripId(String externalTripId);
''',
    'TripAllocationRepository': '''
    java.util.Optional<TripAllocation> findByTripId(Long tripId);
''',
    'TripRejectionRepository': '''
    java.util.List<TripRejection> findByTripId(Long tripId);
    java.util.Optional<TripRejection> findByTripIdAndVendorId(Long tripId, Long vendorId);
'''
}

for name, types in repos.items():
    entity_name = types.split(',')[0].strip()
    methods = repo_methods.get(name, '')
    with open(os.path.join(repo_dir, f'{name}.java'), 'w') as f:
        f.write(f'''package com.example.fairtripdistribution.repository;

import com.example.fairtripdistribution.model.entity.{entity_name};
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface {name} extends JpaRepository<{types}> {{
{methods}
}}
''')

print('Script execution complete.')
