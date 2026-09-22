import re

path = 'AI_CONTEXT.md'
with open(path, 'r', encoding='utf-8') as f:
    content = f.read()

phase_6_update = """* **Phase 6A (Completed):** Redis Caching for Configuration.
  * Added Spring Cache abstraction with Redis.
  * Redis acts purely as a performance cache. MySQL remains the source of truth.
  * Implemented CacheErrorHandler to gracefully fall back to MySQL if Redis is unavailable, ensuring high availability.
  * **Cache Entries:** ctiveZones (ZoneRepository), endorZoneShares (VendorZoneShareRepository), endors (VendorService).
  * **TTL:** 10 minutes.
  * **Eviction Rules:** Full cache eviction (@CacheEvict(allEntries=true)) triggered immediately on vendor/zone create, update, or toggle active, and on share configuration changes.
  * No caching for highly dynamic or correct-sensitive data (allocation states, capacity, etc.)."""

content = content.replace('* **Phase 6 (Current):** Caching, and Dashboards (if requested).', phase_6_update)

with open(path, 'w', encoding='utf-8') as f:
    f.write(content)

print("Updated AI_CONTEXT.md")
