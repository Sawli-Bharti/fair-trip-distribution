import os

path = 'backend/src/main/java/com/example/fairtripdistribution/security/JwtUtil.java'
with open(path, 'r', encoding='utf-8') as f:
    lines = f.readlines()

for i, line in enumerate(lines):
    if '@Value("")' in line:
        if 'secret' in lines[i+1]:
            lines[i] = '    @Value("")\n'
        elif 'jwtExpirationInMs' in lines[i+1]:
            lines[i] = '    @Value("")\n'

with open(path, 'w', encoding='utf-8') as f:
    f.writelines(lines)
print("JwtUtil truly fixed")
