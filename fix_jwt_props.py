import os

path = 'backend/src/main/java/com/example/fairtripdistribution/security/JwtUtil.java'
with open(path, 'r', encoding='utf-8') as f:
    content = f.read()

content = content.replace('@Value("")', '@Value("")')
content = content.replace('@Value("")', '@Value("")')

with open(path, 'w', encoding='utf-8') as f:
    f.write(content)
print("JwtUtil updated")

# Also update application.properties
path = 'backend/src/main/resources/application.properties'
with open(path, 'r', encoding='utf-8') as f:
    content = f.read()
content = content.replace('jwt.secret', 'app.jwt.secret')
content = content.replace('jwt.expiration', 'app.jwt.expiration')
with open(path, 'w', encoding='utf-8') as f:
    f.write(content)
print("application.properties updated")

# Also update test annotations
base_dir = 'backend/src/test/java/com/example/fairtripdistribution'
for root, dirs, files in os.walk(base_dir):
    for file in files:
        if file.endswith('Test.java'):
            path = os.path.join(root, file)
            with open(path, 'r', encoding='utf-8') as f:
                content = f.read()
            if 'jwt.expiration' in content:
                content = content.replace('jwt.expiration', 'app.jwt.expiration')
                content = content.replace('jwt.secret', 'app.jwt.secret')
                with open(path, 'w', encoding='utf-8') as f:
                    f.write(content)
                print(f"{file} updated")

