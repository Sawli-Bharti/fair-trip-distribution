import re

path = 'backend/src/main/java/com/example/fairtripdistribution/config/RedisConfig.java'
with open(path, 'r', encoding='utf-8') as f:
    content = f.read()

replacement = """        return RedisCacheManager.builder(connectionFactory)
                .cacheDefaults(config)
                .withCacheConfiguration("dailyReports", config.entryTtl(Duration.ofMinutes(5)))
                .withCacheConfiguration("monthlyReports", config.entryTtl(Duration.ofMinutes(5)))
                .build();"""
                
content = re.sub(r'return RedisCacheManager\.builder\(connectionFactory\)[\s\S]*?\.build\(\);', replacement, content)

with open(path, 'w', encoding='utf-8') as f:
    f.write(content)

print("Updated RedisConfig")
