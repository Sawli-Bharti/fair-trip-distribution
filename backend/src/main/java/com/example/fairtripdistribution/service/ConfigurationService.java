package com.example.fairtripdistribution.service;

import com.example.fairtripdistribution.exception.BusinessValidationException;
import com.example.fairtripdistribution.model.dto.ZoneShareConfigDto;
import com.example.fairtripdistribution.model.dto.VendorShareDto;
import com.example.fairtripdistribution.model.dto.CapacityDto;
import com.example.fairtripdistribution.model.entity.Vendor;
import com.example.fairtripdistribution.model.entity.Zone;
import com.example.fairtripdistribution.model.entity.VendorZoneShare;
import com.example.fairtripdistribution.model.entity.VendorCapacity;
import com.example.fairtripdistribution.repository.VendorZoneShareRepository;
import com.example.fairtripdistribution.repository.VendorCapacityRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.cache.annotation.CacheEvict;

import java.util.HashSet;
import java.util.Set;
import java.util.List;
import java.time.LocalDateTime;

@Service
public class ConfigurationService {
    private final VendorZoneShareRepository shareRepository;
    private final VendorCapacityRepository capacityRepository;
    private final VendorService vendorService;
    private final ZoneService zoneService;
    
    public ConfigurationService(VendorZoneShareRepository shareRepository,
                                VendorCapacityRepository capacityRepository,
                                VendorService vendorService,
                                ZoneService zoneService) {
        this.shareRepository = shareRepository;
        this.capacityRepository = capacityRepository;
        this.vendorService = vendorService;
        this.zoneService = zoneService;
    }
    
    @Transactional
    @CacheEvict(value = "vendorZoneShares", key = "#zoneId + '-' + #config.tripType")
    public void configureZoneShares(Long zoneId, ZoneShareConfigDto config) {
        Zone zone = zoneService.getZone(zoneId);
        if (!zone.isActive()) {
            throw new BusinessValidationException("Cannot configure shares for inactive zone");
        }
        
        int totalBasisPoints = 0;
        Set<Long> seenVendors = new HashSet<>();
        
        for (VendorShareDto shareDto : config.vendorShares) {
            if (shareDto.targetBasisPoints < 0 || shareDto.targetBasisPoints > 10000) {
                throw new BusinessValidationException("Share basis points must be between 0 and 10000");
            }
            if (!seenVendors.add(shareDto.vendorId)) {
                throw new BusinessValidationException("Duplicate vendor in share configuration");
            }
            Vendor vendor = vendorService.getVendor(shareDto.vendorId);
            if (!vendor.isActive()) {
                throw new BusinessValidationException("Cannot assign share to inactive vendor: " + vendor.getId());
            }
            totalBasisPoints += shareDto.targetBasisPoints;
        }
        
        if (totalBasisPoints != 10000) {
            throw new BusinessValidationException("Total zone shares must exactly equal 10000 basis points");
        }
        
        shareRepository.deleteByZoneIdAndTripType(zone.getId(), config.tripType);
        
        for (VendorShareDto shareDto : config.vendorShares) {
            Vendor vendor = vendorService.getVendor(shareDto.vendorId);
            VendorZoneShare share = new VendorZoneShare();
            share.setZone(zone);
            share.setVendor(vendor);
            share.setTripType(config.tripType);
            share.setTargetBasisPoints(shareDto.targetBasisPoints);
            shareRepository.save(share);
        }
    }
    
    @Transactional
    public VendorCapacity setCapacity(Long vendorId, CapacityDto dto) {
        Vendor vendor = vendorService.getVendor(vendorId);
        if (dto.totalCapacity < 0 || dto.availableCapacity < 0) {
            throw new BusinessValidationException("Capacity cannot be negative");
        }
        if (dto.availableCapacity > dto.totalCapacity) {
            throw new BusinessValidationException("Available capacity cannot exceed total capacity");
        }
        // Requirement: inactive vendors should not receive active capacity allocations
        if (!vendor.isActive() && dto.availableCapacity > 0) {
            throw new BusinessValidationException("Inactive vendors cannot have available capacity > 0");
        }
        
        VendorCapacity capacity = capacityRepository.findByVendorId(vendorId).orElse(new VendorCapacity());
        capacity.setVendor(vendor);
        capacity.setTotalCapacity(dto.totalCapacity);
        capacity.setAvailableCapacity(dto.availableCapacity);
        capacity.setLastUpdated(LocalDateTime.now());
        
        return capacityRepository.save(capacity);
    }
}
