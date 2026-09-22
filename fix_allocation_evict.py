import re

path = 'backend/src/main/java/com/example/fairtripdistribution/service/AllocationService.java'
with open(path, 'r', encoding='utf-8') as f:
    content = f.read()

content = content.replace('import org.springframework.transaction.annotation.Transactional;', 'import org.springframework.transaction.annotation.Transactional;\nimport org.springframework.cache.annotation.CacheEvict;')

content = content.replace('public TripAllocateResponseDto allocateTrip(TripAllocateRequestDto request)', '@CacheEvict(value = {"dailyReports", "monthlyReports"}, allEntries = true)\n    public TripAllocateResponseDto allocateTrip(TripAllocateRequestDto request)')
content = content.replace('public TripAllocateResponseDto rejectTrip(TripRejectRequestDto request)', '@CacheEvict(value = {"dailyReports", "monthlyReports"}, allEntries = true)\n    public TripAllocateResponseDto rejectTrip(TripRejectRequestDto request)')

with open(path, 'w', encoding='utf-8') as f:
    f.write(content)

print("Added CacheEvict to AllocationService")
