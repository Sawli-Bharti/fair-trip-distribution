import os
base_dir = 'backend/src/main/java/com/example/fairtripdistribution'

def write_file(subpath, content):
    path = os.path.join(base_dir, subpath)
    os.makedirs(os.path.dirname(path), exist_ok=True)
    with open(path, 'w', encoding='utf-8') as f:
        f.write(content)

write_file('controller/VendorController.java', '''package com.example.fairtripdistribution.controller;

import com.example.fairtripdistribution.model.dto.VendorDto;
import com.example.fairtripdistribution.model.dto.CapacityDto;
import com.example.fairtripdistribution.model.entity.Vendor;
import com.example.fairtripdistribution.model.entity.VendorCapacity;
import com.example.fairtripdistribution.service.VendorService;
import com.example.fairtripdistribution.service.ConfigurationService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/vendors")
public class VendorController {
    private final VendorService vendorService;
    private final ConfigurationService configurationService;
    
    public VendorController(VendorService vendorService, ConfigurationService configurationService) {
        this.vendorService = vendorService;
        this.configurationService = configurationService;
    }
    
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public Vendor createVendor(@Valid @RequestBody VendorDto dto) {
        return vendorService.createVendor(dto);
    }
    
    @PutMapping("/{id}")
    public Vendor updateVendor(@PathVariable Long id, @Valid @RequestBody VendorDto dto) {
        return vendorService.updateVendor(id, dto);
    }
    
    @PatchMapping("/{id}/active")
    public Vendor toggleActive(@PathVariable Long id, @RequestParam boolean active) {
        return vendorService.toggleActive(id, active);
    }
    
    @GetMapping("/{id}")
    public Vendor getVendor(@PathVariable Long id) {
        return vendorService.getVendor(id);
    }
    
    @GetMapping
    public List<Vendor> listVendors() {
        return vendorService.listVendors();
    }
    
    @PutMapping("/{id}/capacity")
    public VendorCapacity setCapacity(@PathVariable Long id, @Valid @RequestBody CapacityDto dto) {
        return configurationService.setCapacity(id, dto);
    }
}
''')

write_file('controller/ZoneController.java', '''package com.example.fairtripdistribution.controller;

import com.example.fairtripdistribution.model.dto.ZoneDto;
import com.example.fairtripdistribution.model.dto.ZoneShareConfigDto;
import com.example.fairtripdistribution.model.entity.Zone;
import com.example.fairtripdistribution.service.ZoneService;
import com.example.fairtripdistribution.service.ConfigurationService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
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
''')

print('Controllers created')
