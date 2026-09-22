package com.example.fairtripdistribution.controller;

import com.example.fairtripdistribution.model.dto.CapacityDto;
import com.example.fairtripdistribution.model.dto.VendorDto;
import com.example.fairtripdistribution.model.entity.Vendor;
import com.example.fairtripdistribution.model.entity.VendorCapacity;
import com.example.fairtripdistribution.service.ConfigurationService;
import com.example.fairtripdistribution.service.VendorService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@PreAuthorize("hasRole('ADMIN')")
@RequestMapping("/api/vendors")
@Tag(name = "Vendor Management", description = "ADMIN — Create, update, activate/deactivate vendors and manage their trip capacity")
public class VendorController {

    private final VendorService vendorService;
    private final ConfigurationService configurationService;

    public VendorController(VendorService vendorService, ConfigurationService configurationService) {
        this.vendorService = vendorService;
        this.configurationService = configurationService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Create a new vendor", responses = {
        @ApiResponse(responseCode = "201", description = "Vendor created"),
        @ApiResponse(responseCode = "400", description = "Validation error"),
        @ApiResponse(responseCode = "403", description = "ADMIN role required")
    })
    public Vendor createVendor(@Valid @RequestBody VendorDto dto) {
        return vendorService.createVendor(dto);
    }

    @GetMapping
    @Operation(summary = "List all vendors")
    public List<Vendor> listVendors() {
        return vendorService.listVendors();
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get a vendor by ID", responses = {
        @ApiResponse(responseCode = "200", description = "Vendor found"),
        @ApiResponse(responseCode = "404", description = "Vendor not found")
    })
    public Vendor getVendor(@PathVariable Long id) {
        return vendorService.getVendor(id);
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update vendor details")
    public Vendor updateVendor(@PathVariable Long id, @Valid @RequestBody VendorDto dto) {
        return vendorService.updateVendor(id, dto);
    }

    @PatchMapping("/{id}/active")
    @Operation(summary = "Activate or deactivate a vendor",
               description = "Inactive vendors are excluded from allocation eligibility.")
    public Vendor toggleActive(@PathVariable Long id,
            @Parameter(description = "true to activate, false to deactivate") @RequestParam boolean active) {
        return vendorService.toggleActive(id, active);
    }

    @PutMapping("/{id}/capacity")
    @Operation(summary = "Set vendor capacity",
               description = "Sets total and available capacity for a vendor. Vendors with no capacity record are treated as having unlimited capacity.")
    public VendorCapacity setCapacity(@PathVariable Long id, @Valid @RequestBody CapacityDto dto) {
        return configurationService.setCapacity(id, dto);
    }
}
