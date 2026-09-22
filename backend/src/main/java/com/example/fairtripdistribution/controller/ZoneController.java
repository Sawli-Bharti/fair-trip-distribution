package com.example.fairtripdistribution.controller;

import com.example.fairtripdistribution.model.dto.ZoneDto;
import com.example.fairtripdistribution.model.dto.ZoneShareConfigDto;
import com.example.fairtripdistribution.model.entity.Zone;
import com.example.fairtripdistribution.service.ConfigurationService;
import com.example.fairtripdistribution.service.ZoneService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@PreAuthorize("hasRole('ADMIN')")
@RequestMapping("/api/zones")
@Tag(name = "Zone & Share Configuration", description = "ADMIN — Manage distance zones and configure vendor share allocations (basis points)")
public class ZoneController {

    private final ZoneService zoneService;
    private final ConfigurationService configurationService;

    public ZoneController(ZoneService zoneService, ConfigurationService configurationService) {
        this.zoneService = zoneService;
        this.configurationService = configurationService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Create a zone",
               description = "Defines a distance range (minDistance to maxDistance) used to match incoming trips to an allocation bucket.")
    public Zone createZone(@Valid @RequestBody ZoneDto dto) {
        return zoneService.createZone(dto);
    }

    @GetMapping
    @Operation(summary = "List all zones")
    public List<Zone> listZones() {
        return zoneService.listZones();
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get a zone by ID")
    public Zone getZone(@PathVariable Long id) {
        return zoneService.getZone(id);
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update a zone")
    public Zone updateZone(@PathVariable Long id, @Valid @RequestBody ZoneDto dto) {
        return zoneService.updateZone(id, dto);
    }

    @PatchMapping("/{id}/active")
    @Operation(summary = "Activate or deactivate a zone")
    public Zone toggleActive(@PathVariable Long id, @RequestParam boolean active) {
        return zoneService.toggleActive(id, active);
    }

    @PutMapping("/{id}/shares")
    @Operation(summary = "Configure vendor shares for a zone",
               description = """
                   Sets the target share (in basis points) for each vendor in a zone/tripType bucket.
                   10 000 bp = 100%. All shares for a bucket must sum to 10 000.
                   Replaces all existing shares for the given zone + trip type combination.
                   """,
               responses = {
                   @ApiResponse(responseCode = "200", description = "Shares configured successfully"),
                   @ApiResponse(responseCode = "400", description = "Shares do not sum to 10000 or validation error")
               })
    public ResponseEntity<Void> configureShares(@PathVariable Long id, @Valid @RequestBody ZoneShareConfigDto config) {
        configurationService.configureZoneShares(id, config);
        return ResponseEntity.ok().build();
    }
}
