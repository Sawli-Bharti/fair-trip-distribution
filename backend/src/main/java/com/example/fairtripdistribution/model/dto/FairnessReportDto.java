package com.example.fairtripdistribution.model.dto;

import java.io.Serializable;

public class FairnessReportDto implements Serializable {
    public String vendorCode;
    public String vendorName;
    public String zoneCode;
    public String tripType;
    public double promisedPercentage;
    public long actualTrips;
    public double actualPercentage;
    public double expectedTrips;
    public long runningShortfall;
    public Integer currentCapacity;
}
