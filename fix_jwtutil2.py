import os

path = 'backend/src/main/java/com/example/fairtripdistribution/security/JwtUtil.java'
with open(path, 'r', encoding='utf-8') as f:
    content = f.read()

content = content.replace('@Value("")', '@Value("")', 1)
content = content.replace('@Value("")', '@Value("")', 1)

with open(path, 'w', encoding='utf-8') as f:
    f.write(content)
print("JwtUtil fixed")
