package com.example.fairtripdistribution.repository;

import com.example.fairtripdistribution.model.entity.VendorAllocationState;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface VendorAllocationStateRepository extends JpaRepository<VendorAllocationState, Long> {

    java.util.Optional<VendorAllocationState> findByBucketIdAndVendorId(Long bucketId, Long vendorId);
    java.util.List<VendorAllocationState> findByBucketId(Long bucketId);

}
