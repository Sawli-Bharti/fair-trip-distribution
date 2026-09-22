import re

def update_file(path, replacements):
    with open(path, 'r', encoding='utf-8') as f:
        content = f.read()
    
    for old, new in replacements:
        content = content.replace(old, new)
        
    with open(path, 'w', encoding='utf-8') as f:
        f.write(content)

vs_path = 'backend/src/main/java/com/example/fairtripdistribution/service/VendorService.java'
update_file(vs_path, [
    ('@Cacheable(value = "vendors", sync = true)', '@Cacheable(value = "vendors")')
])

zr_path = 'backend/src/main/java/com/example/fairtripdistribution/repository/ZoneRepository.java'
update_file(zr_path, [
    ('@Cacheable(value = "activeZones", sync = true)', '@Cacheable(value = "activeZones")')
])

print("Removed sync=true")
