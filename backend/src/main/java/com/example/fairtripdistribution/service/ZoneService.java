package com.example.fairtripdistribution.service;

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
        if (dto.minDistance == null || (dto.maxDistance != null && dto.minDistance.compareTo(dto.maxDistance) >= 0)) {
            throw new BusinessValidationException("Invalid zone range: min must be less than max");
        }
        validateOverlap(dto, excludeId);
    }

    private void validateOverlap(ZoneDto dto, Long excludeId) {
        List<Zone> activeZones = zoneRepository.findByIsActiveTrue();
        for (Zone z : activeZones) {
            if (excludeId != null && z.getId().equals(excludeId)) continue;
            boolean noOverlap = false;
            if (dto.maxDistance != null && dto.maxDistance.compareTo(z.getMinDistance()) <= 0) noOverlap = true;
            else if (z.getMaxDistance() != null && dto.minDistance.compareTo(z.getMaxDistance()) >= 0) noOverlap = true;
            if (!noOverlap) {
                throw new BusinessValidationException("Zone range overlaps with an existing active zone");
            }
        }
    }
}
