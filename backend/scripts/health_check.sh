#!/bin/bash
# scripts/health_check.sh — 배포 검증 (실패 시 CodeDeploy 자동 롤백)
RETRY=12
INTERVAL=5

for i in $(seq 1 $RETRY); do
    STATUS=$(curl -s -o /dev/null -w "%{http_code}" \
        http://localhost:8080/api/actuator/health)
    if [ "$STATUS" = "200" ]; then
        echo "[HEALTH] OK (시도 $i/$RETRY)"
        exit 0
    fi
    echo "[HEALTH] 대기 중... ($i/$RETRY) status=$STATUS"
    sleep $INTERVAL
done

echo "[HEALTH] 실패 → CodeDeploy 자동 롤백"
exit 1
