import re

path = 'backend/src/test/java/com/example/fairtripdistribution/cache/Phase6ACacheLogicTest.java'
with open(path, 'r', encoding='utf-8') as f:
    content = f.read()
    
# Replace the old logic
replacement = """        // Assert it is in cache
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
}"""

content = re.sub(r'        // Assert it is in cache.*?\}', replacement, content, flags=re.DOTALL)

with open(path, 'w', encoding='utf-8') as f:
    f.write(content)

print("Fixed logic test")
