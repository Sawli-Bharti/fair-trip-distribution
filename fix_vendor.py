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
    ('import org.springframework.transaction.annotation.Transactional;', 'import org.springframework.transaction.annotation.Transactional;\nimport org.springframework.cache.annotation.Cacheable;\nimport org.springframework.cache.annotation.CacheEvict;'),
    ('@Transactional\n    public Vendor createVendor', '@Transactional\n    @CacheEvict(value = "vendors", allEntries = true)\n    public Vendor createVendor'),
    ('@Transactional\n    public Vendor updateVendor', '@Transactional\n    @CacheEvict(value = "vendors", allEntries = true)\n    public Vendor updateVendor'),
    ('@Transactional\n    public Vendor toggleActive', '@Transactional\n    @CacheEvict(value = "vendors", allEntries = true)\n    public Vendor toggleActive'),
    ('public List<Vendor> listVendors()', '@Cacheable(value = "vendors", sync = true)\n    public List<Vendor> listVendors()')
])

print("VendorService updated.")
