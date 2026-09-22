package com.example.fairtripdistribution.model.dto;
import jakarta.validation.constraints.NotBlank;
public class TripRejectRequestDto {
    @NotBlank public String externalTripId;
    public String reason;
}
