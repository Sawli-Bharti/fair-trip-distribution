import re

def update_file(path, replacements):
    with open(path, 'r', encoding='utf-8') as f:
        content = f.read()
    
    for old, new in replacements:
        content = content.replace(old, new)
        
    with open(path, 'w', encoding='utf-8') as f:
        f.write(content)

# 1. ZoneRepository
zr_path = 'backend/src/main/java/com/example/fairtripdistribution/repository/ZoneRepository.java'
update_file(zr_path, [
    ('import java.util.Optional;', 'import java.util.Optional;\nimport org.springframework.cache.annotation.Cacheable;'),
    ('List<Zone> findByIsActiveTrue();', '@Cacheable(value = "activeZones", sync = true)\n    List<Zone> findByIsActiveTrue();')
])

# 2. ZoneService
zs_path = 'backend/src/main/java/com/example/fairtripdistribution/service/ZoneService.java'
update_file(zs_path, [
    ('import org.springframework.transaction.annotation.Transactional;', 'import org.springframework.transaction.annotation.Transactional;\nimport org.springframework.cache.annotation.CacheEvict;'),
    ('@Transactional\n    public Zone createZone', '@Transactional\n    @CacheEvict(value = "activeZones", allEntries = true)\n    public Zone createZone'),
    ('@Transactional\n    public Zone updateZone', '@Transactional\n    @CacheEvict(value = "activeZones", allEntries = true)\n    public Zone updateZone'),
    ('@Transactional\n    public Zone toggleActive', '@Transactional\n    @CacheEvict(value = "activeZones", allEntries = true)\n    public Zone toggleActive')
])

# 3. VendorZoneShareRepository
vzr_path = 'backend/src/main/java/com/example/fairtripdistribution/repository/VendorZoneShareRepository.java'
update_file(vzr_path, [
    ('import java.util.List;', 'import java.util.List;\nimport org.springframework.cache.annotation.Cacheable;\nimport org.springframework.cache.annotation.CacheEvict;'),
    ('List<VendorZoneShare> findByZoneIdAndTripType', '@Cacheable(value = "vendorZoneShares", key = "#zoneId + \'-\' + #tripType")\n    List<VendorZoneShare> findByZoneIdAndTripType')
])

# 4. ConfigurationService
cs_path = 'backend/src/main/java/com/example/fairtripdistribution/service/ConfigurationService.java'
update_file(cs_path, [
    ('import org.springframework.transaction.annotation.Transactional;', 'import org.springframework.transaction.annotation.Transactional;\nimport org.springframework.cache.annotation.CacheEvict;'),
    ('@Transactional\n    public void configureZoneShares', '@Transactional\n    @CacheEvict(value = "vendorZoneShares", key = "#zoneId + \'-\' + #config.tripType")\n    public void configureZoneShares')
])

print("Annotations applied.")
