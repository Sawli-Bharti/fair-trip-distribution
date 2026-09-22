package com.example.fairtripdistribution.model.dto;
import jakarta.validation.constraints.Min;
public class CapacityDto {
    @Min(0) public int totalCapacity;
    @Min(0) public int availableCapacity;
}
