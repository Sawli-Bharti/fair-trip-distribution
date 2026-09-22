package com.example.fairtripdistribution.service;

import com.example.fairtripdistribution.exception.BusinessValidationException;
import com.example.fairtripdistribution.exception.ResourceNotFoundException;
import com.example.fairtripdistribution.model.dto.VendorDto;
import com.example.fairtripdistribution.model.entity.Vendor;
import com.example.fairtripdistribution.repository.VendorRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class VendorService {
    private final VendorRepository vendorRepository;
    
    public VendorService(VendorRepository vendorRepository) {
        this.vendorRepository = vendorRepository;
    }
    
    @Transactional
    public Vendor createVendor(VendorDto dto) {
        if (vendorRepository.existsByCode(dto.code)) {
            throw new BusinessValidationException("Vendor code must be unique");
        }
        Vendor vendor = new Vendor();
        vendor.setCode(dto.code);
        vendor.setName(dto.name);
        vendor.setActive(dto.isActive);
        return vendorRepository.save(vendor);
    }
    
    @Transactional
    public Vendor updateVendor(Long id, VendorDto dto) {
        Vendor vendor = getVendor(id);
        if (!vendor.getCode().equals(dto.code) && vendorRepository.existsByCode(dto.code)) {
            throw new BusinessValidationException("Vendor code must be unique");
        }
        vendor.setCode(dto.code);
        vendor.setName(dto.name);
        vendor.setActive(dto.isActive);
        return vendorRepository.save(vendor);
    }
    
    @Transactional
    public Vendor toggleActive(Long id, boolean active) {
        Vendor vendor = getVendor(id);
        vendor.setActive(active);
        return vendorRepository.save(vendor);
    }
    
    public Vendor getVendor(Long id) {
        return vendorRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Vendor not found"));
    }
    
    public List<Vendor> listVendors() {
        return vendorRepository.findAll();
    }
}
