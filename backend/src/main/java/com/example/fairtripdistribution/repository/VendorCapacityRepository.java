package com.example.fairtripdistribution.repository;

import com.example.fairtripdistribution.model.entity.VendorCapacity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface VendorCapacityRepository extends JpaRepository<VendorCapacity, Long> {

    java.util.Optional<VendorCapacity> findByVendorId(Long vendorId);

}
