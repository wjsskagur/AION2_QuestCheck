#!/bin/bash
# stop.sh
PID=$(pgrep -f "quest-checker")
if [ -n "$PID" ]; then
    echo "[STOP] PID $PID 종료 중..."
    kill -TERM $PID && sleep 8
    kill -9 $PID 2>/dev/null || true
fi
echo "[STOP] 완료"
