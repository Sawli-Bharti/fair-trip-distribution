package com.example.fairtripdistribution.controller;

import com.example.fairtripdistribution.model.dto.TripAllocateRequestDto;
import com.example.fairtripdistribution.model.dto.TripAllocateResponseDto;
import com.example.fairtripdistribution.model.dto.TripRejectRequestDto;
import com.example.fairtripdistribution.service.AllocationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@PreAuthorize("hasAnyRole('USER', 'ADMIN')")
@RequestMapping("/api/trips")
@Tag(name = "Trip Allocation", description = "Allocate trips to vendors using the Most-Owed-First fairness algorithm, and handle vendor rejections with cooldown")
public class TripController {

    private final AllocationService allocationService;

    public TripController(AllocationService allocationService) {
        this.allocationService = allocationService;
    }

    @PostMapping("/allocate")
    @ResponseStatus(HttpStatus.OK)
    @Operation(summary = "Allocate a trip to a vendor",
               description = """
                   Determines the allocation bucket (zone + trip type) from the trip distance, then selects the
                   eligible vendor with the highest basis-point shortfall (Most-Owed-First).
                   Idempotent on `externalTripId` — re-submitting the same ID re-uses the existing trip record.
                   """,
               responses = {
                   @ApiResponse(responseCode = "200", description = "Trip allocated successfully"),
                   @ApiResponse(responseCode = "400", description = "No zone/shares found, no eligible vendor, or validation error"),
                   @ApiResponse(responseCode = "401", description = "Missing or invalid JWT")
               })
    public TripAllocateResponseDto allocateTrip(@Valid @RequestBody TripAllocateRequestDto request) {
        return allocationService.allocateTrip(request);
    }

    @PostMapping("/reject")
    @ResponseStatus(HttpStatus.OK)
    @Operation(summary = "Reject a vendor allocation and reallocate",
               description = """
                   Marks the current allocation as REJECTED, reverts the fairness counters and capacity,
                   places the vendor in a 15-minute trip-specific cooldown, then immediately re-runs allocation
                   to select the next eligible vendor.
                   """,
               responses = {
                   @ApiResponse(responseCode = "200", description = "Rejected and reallocated successfully"),
                   @ApiResponse(responseCode = "400", description = "No successful allocation found for this trip"),
                   @ApiResponse(responseCode = "404", description = "Trip not found"),
                   @ApiResponse(responseCode = "401", description = "Missing or invalid JWT")
               })
    public TripAllocateResponseDto rejectTrip(@Valid @RequestBody TripRejectRequestDto request) {
        return allocationService.rejectTrip(request);
    }
}
