#!/bin/bash
# Export Railway logs to Better Stack
# Usage: ./scripts/export-logs-to-betterstack.sh

set -e

# Configuration
RAILWAY_SERVICE="tateca-backend"
BETTER_STACK_TOKEN="${BETTER_STACK_SOURCE_TOKEN}"
BETTER_STACK_URL="https://in.logs.betterstack.com"

if [ -z "$BETTER_STACK_TOKEN" ]; then
  echo "Error: BETTER_STACK_SOURCE_TOKEN environment variable is not set"
  exit 1
fi

echo "Exporting logs from Railway service: $RAILWAY_SERVICE"
echo "Sending to Better Stack..."

# Export logs from Railway (last 1 hour) and send to Better Stack
railway logs --service=$RAILWAY_SERVICE --json | while IFS= read -r line; do
  # Send each log line to Better Stack
  curl -X POST "$BETTER_STACK_URL" \
    -H "Authorization: Bearer $BETTER_STACK_TOKEN" \
    -H "Content-Type: application/json" \
    -d "$line" \
    --silent --show-error
done

echo "Log export completed"
