import re
path = 'backend/src/test/java/com/example/fairtripdistribution/cache/Phase6ACacheLogicTest.java'
with open(path, 'r', encoding='utf-8') as f:
    content = f.read()

content = content.strip()
if content.endswith('}'):
    content = content[:-1].strip()

with open(path, 'w', encoding='utf-8') as f:
    f.write(content)
print("Fixed braces")
