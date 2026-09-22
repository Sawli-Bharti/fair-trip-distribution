path = "backend/src/main/java/com/example/fairtripdistribution/security/SecurityConfig.java"
with open(path, "r", encoding="utf-8") as f:
    content = f.read()

# Permit swagger endpoints
content = content.replace(
    '.requestMatchers("/api/auth/**").permitAll()',
    '.requestMatchers("/api/auth/**").permitAll()\n                .requestMatchers("/v3/api-docs/**", "/swagger-ui/**", "/swagger-ui.html").permitAll()'
)

with open(path, "w", encoding="utf-8") as f:
    f.write(content)
print("SecurityConfig updated for Swagger")
