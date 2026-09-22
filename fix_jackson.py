import re

path = 'backend/src/main/java/com/example/fairtripdistribution/config/RedisConfig.java'
with open(path, 'r', encoding='utf-8') as f:
    content = f.read()

content = content.replace('import com.fasterxml.jackson.databind.ObjectMapper;', 'import com.fasterxml.jackson.databind.ObjectMapper;\nimport com.fasterxml.jackson.databind.SerializationFeature;\nimport com.fasterxml.jackson.annotation.JsonIgnoreType;')
content = content.replace('objectMapper.registerModule(new JavaTimeModule());', 'objectMapper.registerModule(new JavaTimeModule());\n        objectMapper.disable(SerializationFeature.FAIL_ON_EMPTY_BEANS);\n        objectMapper.addMixIn(Object.class, HibernateProxyMixin.class);')

mixin = """
    @JsonIgnoreType
    private static class HibernateProxyMixin {}
"""

content = content.replace('public class RedisConfig implements CachingConfigurer {', 'public class RedisConfig implements CachingConfigurer {\n' + mixin)

with open(path, 'w', encoding='utf-8') as f:
    f.write(content)

print("Fixed Jackson")
