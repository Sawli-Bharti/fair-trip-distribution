import re

path = 'backend/src/test/java/com/example/fairtripdistribution/service/Phase6BReportTest.java'
with open(path, 'r', encoding='utf-8') as f:
    content = f.read()

content = content.replace('zone.setIsActive(true);', 'zone.setActive(true);')
content = content.replace('v.setIsActive(true);', 'v.setActive(true);')

with open(path, 'w', encoding='utf-8') as f:
    f.write(content)
