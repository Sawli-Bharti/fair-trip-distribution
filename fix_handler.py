path = "backend/src/main/java/com/example/fairtripdistribution/exception/GlobalExceptionHandler.java"
with open(path, "r", encoding="utf-8") as f:
    content = f.read()

# Add import for NoResourceFoundException and exclude it from catch-all
content = content.replace(
    "import jakarta.servlet.http.HttpServletRequest;",
    "import jakarta.servlet.http.HttpServletRequest;\nimport org.springframework.web.servlet.resource.NoResourceFoundException;"
)

content = content.replace(
    "    @ExceptionHandler(Exception.class)\n    public ResponseEntity<Map<String, Object>> handleUnexpected(",
    "    @ExceptionHandler(NoResourceFoundException.class)\n    public ResponseEntity<Map<String, Object>> handleNoResource(\n            NoResourceFoundException ex, HttpServletRequest req) {\n        return ResponseEntity.status(HttpStatus.NOT_FOUND)\n                .body(body(HttpStatus.NOT_FOUND, \"No resource found: \" + req.getRequestURI(), req.getRequestURI()));\n    }\n\n    @ExceptionHandler(Exception.class)\n    public ResponseEntity<Map<String, Object>> handleUnexpected("
)

with open(path, "w", encoding="utf-8") as f:
    f.write(content)
print("Done")
