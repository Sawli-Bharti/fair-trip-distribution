package com.example.fairtripdistribution.repository;

import com.example.fairtripdistribution.model.entity.VendorCapacity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface VendorCapacityRepository extends JpaRepository<VendorCapacity, Long> {

    java.util.Optional<VendorCapacity> findByVendorId(Long vendorId);
    
    @org.springframework.data.jpa.repository.Lock(jakarta.persistence.LockModeType.PESSIMISTIC_WRITE)
    @org.springframework.data.jpa.repository.Query("SELECT c FROM VendorCapacity c WHERE c.vendor.id IN :vendorIds ORDER BY c.vendor.id")
    java.util.List<VendorCapacity> findByVendorIdInForUpdate(@org.springframework.data.repository.query.Param("vendorIds") java.util.List<Long> vendorIds);
    

}
