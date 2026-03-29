#!/bin/bash
# health_check.sh
for i in $(seq 1 12); do
    STATUS=$(curl -s -o /dev/null -w "%{http_code}" http://localhost:8080/api/actuator/health)
    [ "$STATUS" = "200" ] && echo "[HEALTH] OK" && exit 0
    echo "[HEALTH] 대기 ($i/12)..." && sleep 5
done
echo "[HEALTH] 실패 → 자동 롤백"
exit 1
