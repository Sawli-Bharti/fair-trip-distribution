package com.example.fairtripdistribution.controller;

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
