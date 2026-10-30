#!/usr/bin/env bash
# Switch the simulated payment service: ./fault.sh down | up | slow | status
set -euo pipefail
MODE=$(echo "${1:-status}" | tr '[:lower:]' '[:upper:]')
if [ "$MODE" = "STATUS" ]; then curl -sf "http://localhost:8090/admin/faults"; echo; exit 0; fi
curl -sf -X PUT "http://localhost:8090/admin/faults/payment?mode=$MODE"; echo
# Inventory dependency of the job worker (alternative failure):
#   curl -X PUT "http://localhost:8092/admin/worker-faults/inventory?mode=DOWN"   (then mode=UP)
