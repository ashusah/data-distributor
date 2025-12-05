#!/usr/bin/env bash
set -euo pipefail
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
LOG_DIR="$SCRIPT_DIR/logs"
mkdir -p "$LOG_DIR"
LOG_FILE="$LOG_DIR/compose-$(date +%Y%m%d-%H%M%S).log"
echo "Writing Docker Compose logs to $LOG_FILE"
docker compose -f "$SCRIPT_DIR/../docker-compose.yml" up --build 2>&1 | tee "$LOG_FILE"
