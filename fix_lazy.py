import re

path = "backend/src/main/java/com/example/fairtripdistribution/service/ReportService.java"
with open(path, "r", encoding="utf-8") as f:
    content = f.read()

# Add Transactional import and readOnly annotation to generateReport
content = content.replace(
    "import org.springframework.cache.annotation.Cacheable;",
    "import org.springframework.cache.annotation.Cacheable;\nimport org.springframework.transaction.annotation.Transactional;"
)
content = content.replace(
    "    private List<FairnessReportDto> generateReport(LocalDateTime startDate, LocalDateTime endDate) {",
    "    @Transactional(readOnly = true)\n    private List<FairnessReportDto> generateReport(LocalDateTime startDate, LocalDateTime endDate) {"
)

with open(path, "w", encoding="utf-8") as f:
    f.write(content)
print("Done")
