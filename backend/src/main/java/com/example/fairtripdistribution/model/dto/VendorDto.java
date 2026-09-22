package com.example.fairtripdistribution.model.dto;
import jakarta.validation.constraints.NotBlank;
public class VendorDto {
    @NotBlank public String code;
    @NotBlank public String name;
    public boolean isActive = true;
}
