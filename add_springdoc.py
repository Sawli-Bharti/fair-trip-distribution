import re

path = "backend/pom.xml"
with open(path, "r", encoding="utf-8") as f:
    content = f.read()

springdoc = """
        <!-- Springdoc OpenAPI / Swagger UI -->
        <dependency>
            <groupId>org.springdoc</groupId>
            <artifactId>springdoc-openapi-starter-webmvc-ui</artifactId>
            <version>2.3.0</version>
        </dependency>"""

# Insert before </dependencies>
content = content.replace("    </dependencies>", springdoc + "\n    </dependencies>")

with open(path, "w", encoding="utf-8") as f:
    f.write(content)
print("pom.xml updated")
