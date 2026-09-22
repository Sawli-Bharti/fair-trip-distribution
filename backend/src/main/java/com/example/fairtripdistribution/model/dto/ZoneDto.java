package com.example.fairtripdistribution.model.dto;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
public class ZoneDto {
    @NotBlank public String code;
    @NotBlank public String name;
    @NotNull public BigDecimal minDistance;
    @NotNull public BigDecimal maxDistance;
    public boolean isActive = true;
}
