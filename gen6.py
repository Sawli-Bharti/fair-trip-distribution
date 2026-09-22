import os
base_dir = 'backend/src/test/java/com/example/fairtripdistribution/service'

def write_file(subpath, content):
    path = os.path.join(base_dir, subpath)
    os.makedirs(os.path.dirname(path), exist_ok=True)
    with open(path, 'w', encoding='utf-8') as f:
        f.write(content)

write_file('ConfigurationRulesTest.java', '''package com.example.fairtripdistribution.service;

import com.example.fairtripdistribution.exception.BusinessValidationException;
import com.example.fairtripdistribution.model.dto.*;
import com.example.fairtripdistribution.model.entity.*;
import com.example.fairtripdistribution.model.entity.enums.TripType;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.Arrays;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

@SpringBootTest
@Transactional
public class ConfigurationRulesTest {

    @Autowired private VendorService vendorService;
    @Autowired private ZoneService zoneService;
    @Autowired private ConfigurationService configurationService;

    @Test
    public void testVendorManagement() {
        // 1. Create valid vendor
        VendorDto v1 = new VendorDto();
        v1.code = "V_TEST1";
        v1.name = "Test Vendor 1";
        v1.isActive = true;
        Vendor savedV1 = vendorService.createVendor(v1);
        assertThat(savedV1.getId()).isNotNull();

        // 2. Reject duplicate vendor code
        VendorDto v2 = new VendorDto();
        v2.code = "V_TEST1"; // Duplicate
        v2.name = "Test Vendor 2";
        assertThrows(BusinessValidationException.class, () -> {
            vendorService.createVendor(v2);
        });
    }

    @Test
    public void testZoneManagement() {
        // 3. Create valid zone (NEAR)
        ZoneDto z1 = new ZoneDto();
        z1.code = "Z_NEAR";
        z1.name = "Near Zone";
        z1.minDistance = new BigDecimal("0");
        z1.maxDistance = new BigDecimal("15");
        z1.isActive = true;
        Zone savedZ1 = zoneService.createZone(z1);
        assertThat(savedZ1.getId()).isNotNull();

        // 4. Reject overlapping/invalid zone range
        ZoneDto invalidZone = new ZoneDto();
        invalidZone.code = "Z_OVERLAP";
        invalidZone.name = "Overlap Zone";
        invalidZone.minDistance = new BigDecimal("10"); // Overlaps with 0-15
        invalidZone.maxDistance = new BigDecimal("20");
        assertThrows(BusinessValidationException.class, () -> {
            zoneService.createZone(invalidZone);
        });
        
        ZoneDto invalidRange = new ZoneDto();
        invalidRange.code = "Z_INV";
        invalidRange.name = "Invalid Range Zone";
        invalidRange.minDistance = new BigDecimal("30");
        invalidRange.maxDistance = new BigDecimal("20");
        assertThrows(BusinessValidationException.class, () -> {
            zoneService.createZone(invalidRange);
        });
    }

    @Test
    public void testVendorZoneShares() {
        // Setup base data
        VendorDto v1 = new VendorDto(); v1.code = "V_SHARE1"; v1.name = "V1";
        Vendor vendor1 = vendorService.createVendor(v1);
        
        VendorDto v2 = new VendorDto(); v2.code = "V_SHARE2"; v2.name = "V2";
        Vendor vendor2 = vendorService.createVendor(v2);

        ZoneDto z1 = new ZoneDto(); z1.code = "Z_SHARE"; z1.name = "Z1";
        z1.minDistance = new BigDecimal("100"); z1.maxDistance = new BigDecimal("200");
        Zone zone = zoneService.createZone(z1);

        ZoneShareConfigDto config = new ZoneShareConfigDto();
        config.tripType = TripType.NORMAL;

        // 5. Create valid vendor share configuration
        VendorShareDto s1 = new VendorShareDto(); s1.vendorId = vendor1.getId(); s1.targetBasisPoints = 6000;
        VendorShareDto s2 = new VendorShareDto(); s2.vendorId = vendor2.getId(); s2.targetBasisPoints = 4000;
        config.vendorShares = Arrays.asList(s1, s2);
        
        // This should pass
        configurationService.configureZoneShares(zone.getId(), config);

        // 6. Reject target basis points below 0
        VendorShareDto s3 = new VendorShareDto(); s3.vendorId = vendor1.getId(); s3.targetBasisPoints = -100;
        VendorShareDto s4 = new VendorShareDto(); s4.vendorId = vendor2.getId(); s4.targetBasisPoints = 10100;
        config.vendorShares = Arrays.asList(s3, s4);
        assertThrows(BusinessValidationException.class, () -> {
            configurationService.configureZoneShares(zone.getId(), config);
        });

        // 7. Reject target basis points above 10000
        VendorShareDto s5 = new VendorShareDto(); s5.vendorId = vendor1.getId(); s5.targetBasisPoints = 15000;
        VendorShareDto s6 = new VendorShareDto(); s6.vendorId = vendor2.getId(); s6.targetBasisPoints = -5000;
        config.vendorShares = Arrays.asList(s5, s6);
        assertThrows(BusinessValidationException.class, () -> {
            configurationService.configureZoneShares(zone.getId(), config);
        });

        // 8. Reject Zone + TripType configuration whose total is not exactly 10000
        VendorShareDto s7 = new VendorShareDto(); s7.vendorId = vendor1.getId(); s7.targetBasisPoints = 5000;
        VendorShareDto s8 = new VendorShareDto(); s8.vendorId = vendor2.getId(); s8.targetBasisPoints = 4000; // 9000 total
        config.vendorShares = Arrays.asList(s7, s8);
        assertThrows(BusinessValidationException.class, () -> {
            configurationService.configureZoneShares(zone.getId(), config);
        });

        // 9. Reject duplicate Vendor + Zone + TripType configuration
        VendorShareDto s9 = new VendorShareDto(); s9.vendorId = vendor1.getId(); s9.targetBasisPoints = 5000;
        VendorShareDto s10 = new VendorShareDto(); s10.vendorId = vendor1.getId(); s10.targetBasisPoints = 5000; // Duplicate vendor
        config.vendorShares = Arrays.asList(s9, s10);
        assertThrows(BusinessValidationException.class, () -> {
            configurationService.configureZoneShares(zone.getId(), config);
        });
    }

    @Test
    public void testVendorCapacity() {
        VendorDto v1 = new VendorDto(); v1.code = "V_CAP1"; v1.name = "V1";
        Vendor vendor1 = vendorService.createVendor(v1);

        // 10. Create valid capacity
        CapacityDto validCap = new CapacityDto();
        validCap.totalCapacity = 10;
        validCap.availableCapacity = 5;
        VendorCapacity savedCap = configurationService.setCapacity(vendor1.getId(), validCap);
        assertThat(savedCap.getAvailableCapacity()).isEqualTo(5);

        // 11. Reject negative capacity
        CapacityDto negativeCap = new CapacityDto();
        negativeCap.totalCapacity = -5;
        negativeCap.availableCapacity = -1;
        assertThrows(BusinessValidationException.class, () -> {
            configurationService.setCapacity(vendor1.getId(), negativeCap);
        });

        // 12. Reject available capacity greater than total capacity
        CapacityDto overCap = new CapacityDto();
        overCap.totalCapacity = 5;
        overCap.availableCapacity = 10;
        assertThrows(BusinessValidationException.class, () -> {
            configurationService.setCapacity(vendor1.getId(), overCap);
        });
    }
}
''')

print('Tests created')
