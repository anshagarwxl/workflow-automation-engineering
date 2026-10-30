#!/usr/bin/env bash
# Switch the simulated scheduling service: ./fault.sh down | up | slow | status
set -euo pipefail
MODE=$(echo "${1:-status}" | tr '[:lower:]' '[:upper:]')
if [ "$MODE" = "STATUS" ]; then curl -sf "http://localhost:8090/admin/faults"; echo; exit 0; fi
curl -sf -X PUT "http://localhost:8090/admin/faults/scheduling?mode=$MODE"; echo
