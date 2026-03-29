#!/bin/bash
# scripts/stop.sh — 기존 프로세스 Graceful Shutdown
PID=$(pgrep -f "quest-checker")
if [ -n "$PID" ]; then
    echo "[STOP] PID $PID 종료 중 (SIGTERM)..."
    kill -TERM $PID
    sleep 8
    if kill -0 $PID 2>/dev/null; then
        echo "[STOP] SIGKILL 강제 종료"
        kill -9 $PID
    fi
fi
echo "[STOP] 완료"
