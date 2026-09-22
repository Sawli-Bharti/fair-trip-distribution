package com.example.fairtripdistribution.model.dto;
import com.example.fairtripdistribution.model.entity.enums.TripType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Min;
import java.math.BigDecimal;
public class TripAllocateRequestDto {
    @NotBlank public String externalTripId;
    @NotNull @Min(0) public BigDecimal distance;
    @NotNull public TripType tripType;
}
