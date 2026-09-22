package com.example.fairtripdistribution.model.dto;
import com.example.fairtripdistribution.model.entity.enums.TripType;
import jakarta.validation.constraints.NotNull;
import java.util.List;
public class ZoneShareConfigDto {
    @NotNull public TripType tripType;
    @NotNull public List<VendorShareDto> vendorShares;
}
