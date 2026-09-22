package com.example.fairtripdistribution.controller;

import com.example.fairtripdistribution.model.dto.ZoneDto;
import com.example.fairtripdistribution.model.dto.ZoneShareConfigDto;
import com.example.fairtripdistribution.model.entity.Zone;
import com.example.fairtripdistribution.service.ZoneService;
import com.example.fairtripdistribution.service.ConfigurationService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.security.access.prepost.PreAuthorize;

import java.util.List;

@RestController
@PreAuthorize("hasRole('ADMIN')")
@RequestMapping("/api/zones")
public class ZoneController {
    private final ZoneService zoneService;
    private final ConfigurationService configurationService;
    
    public ZoneController(ZoneService zoneService, ConfigurationService configurationService) {
        this.zoneService = zoneService;
        this.configurationService = configurationService;
    }
    
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public Zone createZone(@Valid @RequestBody ZoneDto dto) {
        return zoneService.createZone(dto);
    }
    
    @PutMapping("/{id}")
    public Zone updateZone(@PathVariable Long id, @Valid @RequestBody ZoneDto dto) {
        return zoneService.updateZone(id, dto);
    }
    
    @PatchMapping("/{id}/active")
    public Zone toggleActive(@PathVariable Long id, @RequestParam boolean active) {
        return zoneService.toggleActive(id, active);
    }
    
    @GetMapping("/{id}")
    public Zone getZone(@PathVariable Long id) {
        return zoneService.getZone(id);
    }
    
    @GetMapping
    public List<Zone> listZones() {
        return zoneService.listZones();
    }
    
    @PutMapping("/{id}/shares")
    public ResponseEntity<Void> configureShares(@PathVariable Long id, @Valid @RequestBody ZoneShareConfigDto config) {
        configurationService.configureZoneShares(id, config);
        return ResponseEntity.ok().build();
    }
}
