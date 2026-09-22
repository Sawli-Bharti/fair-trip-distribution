import re

path = 'backend/src/test/java/com/example/fairtripdistribution/cache/Phase6ACacheLogicTest.java'
with open(path, 'r', encoding='utf-8') as f:
    content = f.read()
    
content = content.replace('"app.jwt.secret=8a9b2b3c4d5e6f7a8b9c0d1e2f3a4b5c6d7e8f9a0b1c2d3e4f5a6b7c8d9e0f1a2b3c4d5e6f7a8b9c0d1e2f3a4b5c6d7e8f9a0b1c2d3e4f5a6b7c8d9e0f1a2b3c4d5e6f7a8b9c0"', 
'"app.jwt.secret=8a9b2b3c4d5e6f7a8b9c0d1e2f3a4b5c6d7e8f9a0b1c2d3e4f5a6b7c8d9e0f1a2b3c4d5e6f7a8b9c0d1e2f3a4b5c6d7e8f9a0b1c2d3e4f5a6b7c8d9e0f1a2b3c4d5e6f7a8b9c0", "spring.main.allow-bean-definition-overriding=true"')

with open(path, 'w', encoding='utf-8') as f:
    f.write(content)

print("Enabled bean overriding")
