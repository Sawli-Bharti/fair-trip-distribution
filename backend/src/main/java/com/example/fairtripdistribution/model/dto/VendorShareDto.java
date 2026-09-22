package com.example.fairtripdistribution.model.dto;
import jakarta.validation.constraints.NotNull;
public class VendorShareDto {
    @NotNull public Long vendorId;
    public int targetBasisPoints;
}
