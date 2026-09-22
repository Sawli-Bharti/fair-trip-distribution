package com.example.fairtripdistribution.repository;

import com.example.fairtripdistribution.model.entity.VendorCapacity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import jakarta.persistence.LockModeType;
import java.util.List;
import java.util.Optional;

@Repository
public interface VendorCapacityRepository extends JpaRepository<VendorCapacity, Long> {
    Optional<VendorCapacity> findByVendorId(Long vendorId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT c FROM VendorCapacity c WHERE c.vendor.id IN :vendorIds")
    List<VendorCapacity> findByVendorIdInForUpdate(@Param("vendorIds") List<Long> vendorIds);

    @Query("SELECT c FROM VendorCapacity c JOIN FETCH c.vendor")
    List<VendorCapacity> findAllWithVendor();
}
