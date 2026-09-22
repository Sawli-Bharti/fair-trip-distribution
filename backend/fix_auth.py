path = "backend/src/main/java/com/example/fairtripdistribution/controller/AuthController.java"
with open(path, "r", encoding="utf-8") as f:
    content = f.read()

content = content.replace(
    "import com.example.fairtripdistribution.model.dto.AuthRequestDto;",
    "import com.example.fairtripdistribution.model.dto.AuthRequestDto;\nimport com.example.fairtripdistribution.model.dto.RegisterRequestDto;"
)
content = content.replace(
    "public ResponseEntity<AuthResponseDto> register(@Valid @RequestBody AuthRequestDto request)",
    "public ResponseEntity<AuthResponseDto> register(@Valid @RequestBody RegisterRequestDto request)"
)

with open(path, "w", encoding="utf-8") as f:
    f.write(content)
print("Fixed AuthController")
