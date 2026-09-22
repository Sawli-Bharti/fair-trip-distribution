package com.example.fairtripdistribution.repository;

import com.example.fairtripdistribution.model.entity.VendorAllocationState;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

@Repository
public interface VendorAllocationStateRepository extends JpaRepository<VendorAllocationState, Long> {

    java.util.Optional<VendorAllocationState> findByBucketIdAndVendorId(Long bucketId, Long vendorId);
    java.util.List<VendorAllocationState> findByBucketId(Long bucketId);

    @org.springframework.data.jpa.repository.Lock(jakarta.persistence.LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT s FROM VendorAllocationState s WHERE s.bucket.id = :bucketId")
    java.util.List<VendorAllocationState> findByBucketIdForUpdate(@org.springframework.data.repository.query.Param("bucketId") Long bucketId);

    @Query("SELECT s FROM VendorAllocationState s JOIN FETCH s.bucket b JOIN FETCH b.zone JOIN FETCH s.vendor")
    java.util.List<VendorAllocationState> findAllWithBucketAndVendor();
}
