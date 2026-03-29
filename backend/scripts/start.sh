#!/bin/bash
# scripts/start.sh — SSM에서 비밀값 주입 후 JVM 기동

export DB_HOST=$(aws ssm get-parameter \
    --name /aion2/db/host --with-decryption \
    --query Parameter.Value --output text --region ap-northeast-2)

export DB_USERNAME=$(aws ssm get-parameter \
    --name /aion2/db/username --with-decryption \
    --query Parameter.Value --output text --region ap-northeast-2)

export DB_PASSWORD=$(aws ssm get-parameter \
    --name /aion2/db/password --with-decryption \
    --query Parameter.Value --output text --region ap-northeast-2)

export JWT_SECRET=$(aws ssm get-parameter \
    --name /aion2/jwt/secret --with-decryption \
    --query Parameter.Value --output text --region ap-northeast-2)

export CLAUDE_API_KEY=$(aws ssm get-parameter \
    --name /aion2/claude/api-key --with-decryption \
    --query Parameter.Value --output text --region ap-northeast-2)

JAR=$(ls /opt/app/quest-checker-*.jar | head -1)

mkdir -p /opt/app/logs

java -Xmx768m -Xms512m \
     -XX:+UseG1GC \
     -Dspring.profiles.active=prod \
     -Dspring.main.lazy-initialization=true \
     -Dlogging.file.name=/opt/app/logs/quest-checker.log \
     -jar "$JAR" &

echo $! > /opt/app/app.pid
echo "[START] PID $(cat /opt/app/app.pid) 기동 완료"
