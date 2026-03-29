#!/bin/bash
# start.sh — SSM에서 비밀값 주입 후 JVM 기동

R="ap-northeast-2"
get() { aws ssm get-parameter --name "$1" --with-decryption --query Parameter.Value --output text --region $R; }

export DB_HOST=$(get /aion2/db/host)
export DB_USERNAME=$(get /aion2/db/username)
export DB_PASSWORD=$(get /aion2/db/password)
export JWT_SECRET=$(get /aion2/jwt/secret)
export CLAUDE_API_KEY=$(get /aion2/claude/api-key)
export KAKAO_CLIENT_ID=$(get /aion2/oauth/kakao/client-id)
export KAKAO_CLIENT_SECRET=$(get /aion2/oauth/kakao/client-secret)
export NAVER_CLIENT_ID=$(get /aion2/oauth/naver/client-id)
export NAVER_CLIENT_SECRET=$(get /aion2/oauth/naver/client-secret)
export GOOGLE_CLIENT_ID=$(get /aion2/oauth/google/client-id)
export GOOGLE_CLIENT_SECRET=$(get /aion2/oauth/google/client-secret)
export FRONTEND_URL=https://your-domain.com   # 실제 도메인으로 변경

JAR=$(ls /opt/app/quest-checker-*.jar | head -1)
mkdir -p /opt/app/logs

java -Xmx768m -Xms512m -XX:+UseG1GC \
     -Dspring.profiles.active=prod \
     -Dspring.main.lazy-initialization=true \
     -Dlogging.file.name=/opt/app/logs/quest-checker.log \
     -jar "$JAR" &

echo $! > /opt/app/app.pid
echo "[START] PID $(cat /opt/app/app.pid)"
