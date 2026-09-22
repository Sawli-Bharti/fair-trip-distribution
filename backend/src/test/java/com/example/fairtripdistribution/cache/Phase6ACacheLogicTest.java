package com.example.fairtripdistribution.cache;

import com.example.fairtripdistribution.model.dto.VendorDto;
import com.example.fairtripdistribution.model.entity.Vendor;
import com.example.fairtripdistribution.repository.VendorRepository;
import com.example.fairtripdistribution.service.VendorService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cache.CacheManager;
import org.springframework.cache.concurrent.ConcurrentMapCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Primary;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(properties = {
    "app.jwt.expiration=86400000",
    "app.jwt.secret=8a9b2b3c4d5e6f7a8b9c0d1e2f3a4b5c6d7e8f9a0b1c2d3e4f5a6b7c8d9e0f1a2b3c4d5e6f7a8b9c0d1e2f3a4b5c6d7e8f9a0b1c2d3e4f5a6b7c8d9e0f1a2b3c4d5e6f7a8b9c0", "spring.main.allow-bean-definition-overriding=true"
})
public class Phase6ACacheLogicTest {

    @TestConfiguration
    static class MockCacheConfig {
        @Bean
        @Primary
        public CacheManager cacheManager() {
            return new ConcurrentMapCacheManager("vendors", "activeZones", "vendorZoneShares");
        }
    }

    @Autowired
    private VendorService vendorService;

    @Autowired
    private VendorRepository vendorRepository;

    @Autowired
    private CacheManager cacheManager;

    @BeforeEach
    public void setup() {
        vendorRepository.deleteAll();
        cacheManager.getCache("vendors").clear();
    }

    @Test
    public void testCacheableReadPathAndInvalidation() {
        VendorDto dto = new VendorDto();
        dto.code = "V-CACHE-1";
        dto.name = "Cache Vendor";
        dto.isActive = true;
        
        Vendor created = vendorService.createVendor(dto);
        assertThat(created).isNotNull();

        // Initial read -> misses cache, populates cache
        List<Vendor> vendors = vendorService.listVendors();
        assertThat(vendors).hasSize(1);
        
        // Assert it is in cache
        assertThat(cacheManager.getCache("vendors").getNativeCache()).isNotNull();

        // Update DB directly bypassing service (and cache)
        Vendor dbVendor = vendorRepository.findById(created.getId()).get();
        dbVendor.setName("Sneaky Update");
        vendorRepository.save(dbVendor);
        
        // Read again -> hits cache (should still be 'Cache Vendor', NOT 'Sneaky Update')
        List<Vendor> cachedVendors = vendorService.listVendors();
        assertThat(cachedVendors).hasSize(1);
        assertThat(cachedVendors.get(0).getName()).isEqualTo("Cache Vendor");

        // Use service method to update -> triggers eviction
        dto.name = "Updated Vendor";
        vendorService.updateVendor(created.getId(), dto);

        // Read again -> cache was evicted, hits DB (which has 'Updated Vendor')
        List<Vendor> updatedVendors = vendorService.listVendors();
        assertThat(updatedVendors).hasSize(1);
        assertThat(updatedVendors.get(0).getName()).isEqualTo("Updated Vendor");
    }
}