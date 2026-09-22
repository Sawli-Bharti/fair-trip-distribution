import os
base_dir = 'backend/src/main/java/com/example/fairtripdistribution'

def write_file(subpath, content):
    path = os.path.join(base_dir, subpath)
    os.makedirs(os.path.dirname(path), exist_ok=True)
    with open(path, 'w', encoding='utf-8') as f:
        f.write(content)

write_file('service/VendorService.java', '''package com.example.fairtripdistribution.service;

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
''')

write_file('service/ZoneService.java', '''package com.example.fairtripdistribution.service;

import com.example.fairtripdistribution.exception.BusinessValidationException;
import com.example.fairtripdistribution.exception.ResourceNotFoundException;
import com.example.fairtripdistribution.model.dto.ZoneDto;
import com.example.fairtripdistribution.model.entity.Zone;
import com.example.fairtripdistribution.repository.ZoneRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.math.BigDecimal;

@Service
public class ZoneService {
    private final ZoneRepository zoneRepository;
    
    public ZoneService(ZoneRepository zoneRepository) {
        this.zoneRepository = zoneRepository;
    }
    
    @Transactional
    public Zone createZone(ZoneDto dto) {
        validateZoneData(dto, null);
        Zone zone = new Zone();
        zone.setCode(dto.code);
        zone.setName(dto.name);
        zone.setMinDistance(dto.minDistance);
        zone.setMaxDistance(dto.maxDistance);
        zone.setActive(dto.isActive);
        return zoneRepository.save(zone);
    }
    
    @Transactional
    public Zone updateZone(Long id, ZoneDto dto) {
        validateZoneData(dto, id);
        Zone zone = getZone(id);
        zone.setCode(dto.code);
        zone.setName(dto.name);
        zone.setMinDistance(dto.minDistance);
        zone.setMaxDistance(dto.maxDistance);
        zone.setActive(dto.isActive);
        return zoneRepository.save(zone);
    }
    
    @Transactional
    public Zone toggleActive(Long id, boolean active) {
        Zone zone = getZone(id);
        if (active) {
            // Need to validate overlap if activating
            ZoneDto dto = new ZoneDto();
            dto.minDistance = zone.getMinDistance();
            dto.maxDistance = zone.getMaxDistance();
            validateOverlap(dto, id);
        }
        zone.setActive(active);
        return zoneRepository.save(zone);
    }
    
    public Zone getZone(Long id) {
        return zoneRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Zone not found"));
    }
    
    public List<Zone> listZones() {
        return zoneRepository.findAll();
    }
    
    private void validateZoneData(ZoneDto dto, Long excludeId) {
        if (dto.minDistance == null || dto.maxDistance == null || dto.minDistance.compareTo(dto.maxDistance) >= 0) {
            throw new BusinessValidationException("Invalid zone range: min must be less than max");
        }
        validateOverlap(dto, excludeId);
    }

    private void validateOverlap(ZoneDto dto, Long excludeId) {
        List<Zone> activeZones = zoneRepository.findByIsActiveTrue();
        for (Zone z : activeZones) {
            if (excludeId != null && z.getId().equals(excludeId)) continue;
            boolean noOverlap = dto.maxDistance.compareTo(z.getMinDistance()) <= 0 || 
                                dto.minDistance.compareTo(z.getMaxDistance()) >= 0;
            if (!noOverlap) {
                throw new BusinessValidationException("Zone range overlaps with an existing active zone");
            }
        }
    }
}
''')

write_file('service/ConfigurationService.java', '''package com.example.fairtripdistribution.service;

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
''')

print('Services created')
