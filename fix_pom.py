import sys

path = 'backend/pom.xml'
with open(path, 'r', encoding='utf-8') as f:
    content = f.read()

dep = """        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-cache</artifactId>
        </dependency>"""

if 'spring-boot-starter-cache' not in content:
    content = content.replace('<dependencies>', '<dependencies>\n' + dep)
    with open(path, 'w', encoding='utf-8') as f:
        f.write(content)
    print("Added cache dependency")
else:
    print("Cache dependency already exists")
