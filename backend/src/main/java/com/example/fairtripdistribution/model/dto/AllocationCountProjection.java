package com.example.fairtripdistribution.model.dto;

import com.example.fairtripdistribution.model.entity.enums.TripType;

public class AllocationCountProjection {
    private Long vendorId;
    private Long zoneId;
    private TripType tripType;
    private Long tripCount;

    public AllocationCountProjection(Long vendorId, Long zoneId, TripType tripType, Long tripCount) {
        this.vendorId = vendorId;
        this.zoneId = zoneId;
        this.tripType = tripType;
        this.tripCount = tripCount;
    }

    public Long getVendorId() { return vendorId; }
    public Long getZoneId() { return zoneId; }
    public TripType getTripType() { return tripType; }
    public Long getTripCount() { return tripCount; }
}
