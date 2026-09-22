package com.example.fairtripdistribution.controller;

import com.example.fairtripdistribution.model.dto.TripAllocateRequestDto;
import com.example.fairtripdistribution.model.dto.TripAllocateResponseDto;
import com.example.fairtripdistribution.service.AllocationService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import com.example.fairtripdistribution.model.dto.TripRejectRequestDto;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/trips")
public class TripController {
    
    private final AllocationService allocationService;
    
    public TripController(AllocationService allocationService) {
        this.allocationService = allocationService;
    }
    
    @PostMapping("/allocate")
    @ResponseStatus(HttpStatus.OK) // Since it can be idempotent (200), we return OK instead of CREATED strictly, or CREATED for new.
    public TripAllocateResponseDto allocateTrip(@Valid @RequestBody TripAllocateRequestDto request) {
        return allocationService.allocateTrip(request);
    }
    
    @PostMapping("/reject")
    @ResponseStatus(HttpStatus.OK)
    public TripAllocateResponseDto rejectTrip(@Valid @RequestBody TripRejectRequestDto request) {
        return allocationService.rejectTrip(request);
    }
}
