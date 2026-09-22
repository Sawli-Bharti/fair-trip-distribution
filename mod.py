import os
import re

base_dir = 'backend/src/main/java/com/example/fairtripdistribution'

def modify_file(subpath, search, replace):
    path = os.path.join(base_dir, subpath)
    with open(path, 'r', encoding='utf-8') as f:
        content = f.read()
    content = content.replace(search, replace)
    with open(path, 'w', encoding='utf-8') as f:
        f.write(content)

# 1. Update Vendor entity
modify_file('model/entity/Vendor.java', 
    'private boolean isActive = true;', 
    'private boolean isActive = true;\n    \n    @Column(nullable = false)\n    private int priority = 0;')
modify_file('model/entity/Vendor.java', 
    'public boolean isActive()', 
    'public int getPriority() { return priority; }\n    public void setPriority(int priority) { this.priority = priority; }\n    public boolean isActive()')

# 2. Update VendorDto
modify_file('model/dto/VendorDto.java', 
    'public boolean isActive = true;', 
    'public boolean isActive = true;\n    public int priority = 0;')

# 3. Update VendorService
modify_file('service/VendorService.java',
    'vendor.setActive(dto.isActive);',
    'vendor.setActive(dto.isActive);\n        vendor.setPriority(dto.priority);')

# 4. Update Zone entity to allow null maxDistance
modify_file('model/entity/Zone.java',
    '@Column(nullable = false)\n    private BigDecimal maxDistance;',
    '@Column(nullable = true)\n    private BigDecimal maxDistance;')

# 5. Update ZoneService to handle null maxDistance in overlap
modify_file('service/ZoneService.java',
    'if (dto.minDistance == null || dto.maxDistance == null || dto.minDistance.compareTo(dto.maxDistance) >= 0)',
    'if (dto.minDistance == null || (dto.maxDistance != null && dto.minDistance.compareTo(dto.maxDistance) >= 0))')
modify_file('service/ZoneService.java',
    'boolean noOverlap = dto.maxDistance.compareTo(z.getMinDistance()) <= 0 || \n                                dto.minDistance.compareTo(z.getMaxDistance()) >= 0;',
    'boolean noOverlap = false;\n            if (dto.maxDistance != null && dto.maxDistance.compareTo(z.getMinDistance()) <= 0) noOverlap = true;\n            else if (z.getMaxDistance() != null && dto.minDistance.compareTo(z.getMaxDistance()) >= 0) noOverlap = true;')

print("Entities updated successfully.")
