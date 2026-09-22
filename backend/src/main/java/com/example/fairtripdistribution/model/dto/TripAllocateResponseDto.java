package com.example.fairtripdistribution.model.dto;
import com.example.fairtripdistribution.model.entity.enums.TripType;
import com.example.fairtripdistribution.model.entity.enums.AllocationStatus;
public class TripAllocateResponseDto {
    public Long tripId;
    public String externalTripId;
    public Long vendorId;
    public String zoneCode;
    public TripType tripType;
    public AllocationStatus status;
}
