import re

path = "backend/src/main/java/com/example/fairtripdistribution/service/AllocationService.java"
with open(path, "r", encoding="utf-8") as f:
    content = f.read()

# Add logger field after class declaration
if "private static final Logger log" not in content:
    content = content.replace(
        "import org.springframework.transaction.annotation.Transactional;",
        "import org.slf4j.Logger;\nimport org.slf4j.LoggerFactory;\nimport org.springframework.transaction.annotation.Transactional;"
    )
    content = content.replace(
        "public class AllocationService {",
        "public class AllocationService {\n\n    private static final Logger log = LoggerFactory.getLogger(AllocationService.class);\n"
    )

# Log on allocation success - after "return mapToResponse(trip, allocation);"
content = content.replace(
    "        return mapToResponse(trip, allocation);\n    }\n\n    @Transactional\n    @CacheEvict(value = {\"dailyReports\", \"monthlyReports\"}",
    "        log.info(\"Trip allocated: externalTripId={} vendor={} zone={} type={}\",\n                trip.getExternalTripId(), selectedVendor.getCode(), zone.getCode(), request.tripType);\n        return mapToResponse(trip, allocation);\n    }\n\n    @Transactional\n    @CacheEvict(value = {\"dailyReports\", \"monthlyReports\"}"
)

# Log no eligible vendor
content = content.replace(
    '            throw new BusinessValidationException("No eligible vendor found for allocation");',
    '            log.warn("No eligible vendor for allocation: externalTripId={} zone={} type={}", request.externalTripId, zone.getCode(), request.tripType);\n            throw new BusinessValidationException("No eligible vendor found for allocation");'
)

# Log on rejection
content = content.replace(
    "        // 5. Re-run allocation",
    '        log.info("Trip rejected: externalTripId={} vendor={} reason={}", request.externalTripId, vendor.getCode(), request.reason);\n        // 5. Re-run allocation'
)

with open(path, "w", encoding="utf-8") as f:
    f.write(content)
print("AllocationService logging added")
