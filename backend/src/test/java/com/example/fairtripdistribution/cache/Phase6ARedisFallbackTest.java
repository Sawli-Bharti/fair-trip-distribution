package com.example.fairtripdistribution.cache;

import com.example.fairtripdistribution.model.dto.VendorDto;
import com.example.fairtripdistribution.model.entity.Vendor;
import com.example.fairtripdistribution.repository.VendorRepository;
import com.example.fairtripdistribution.service.VendorService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(properties = {
    "app.jwt.expiration=86400000",
    "app.jwt.secret=8a9b2b3c4d5e6f7a8b9c0d1e2f3a4b5c6d7e8f9a0b1c2d3e4f5a6b7c8d9e0f1a2b3c4d5e6f7a8b9c0d1e2f3a4b5c6d7e8f9a0b1c2d3e4f5a6b7c8d9e0f1a2b3c4d5e6f7a8b9c0",
    "spring.data.redis.host=localhost",
    "spring.data.redis.port=6389" // definitely no redis here
})
public class Phase6ARedisFallbackTest {

    @Autowired
    private VendorService vendorService;

    @Autowired
    private VendorRepository vendorRepository;

    @BeforeEach
    public void setup() {
        vendorRepository.deleteAll();
    }

    @Test
    public void testApplicationWorksWhenRedisIsUnavailable() {
        // Application should work seamlessly even though Redis connection fails

        VendorDto dto = new VendorDto();
        dto.code = "V-FALLBACK-1";
        dto.name = "Fallback Vendor";
        dto.isActive = true;
        
        Vendor created = vendorService.createVendor(dto);
        assertThat(created).isNotNull();

        List<Vendor> vendors = vendorService.listVendors();
        assertThat(vendors).hasSize(1);
        assertThat(vendors.get(0).getCode()).isEqualTo("V-FALLBACK-1");

        dto.name = "Updated Fallback";
        vendorService.updateVendor(created.getId(), dto);

        List<Vendor> updatedVendors = vendorService.listVendors();
        assertThat(updatedVendors.get(0).getName()).isEqualTo("Updated Fallback");
    }
}
