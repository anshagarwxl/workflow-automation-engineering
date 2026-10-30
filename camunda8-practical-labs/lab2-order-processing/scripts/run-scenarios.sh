#!/usr/bin/env bash
# Lab 2 - Order processing: start the automatic test scenarios and print the live results table.
# Requires: Camunda 8 Run, mock-services (:8090) and this lab app (:8092) running.
set -euo pipefail
APP=${APP:-http://localhost:8092}

echo "== Scenarios available"
curl -sf "$APP/api/scenarios"; echo

if [ "${1:-}" != "" ]; then
  echo "== Starting scenario $1"
  curl -sf -X POST "$APP/api/scenarios/$1/start"; echo
else
  echo "== Starting all automatic scenarios (failure/recovery ones are started manually, see README)"
  curl -sf -X POST "$APP/api/scenarios/start-all"; echo
fi

echo "== Waiting 15 s for workers, connectors and Operate indexing..."
sleep 15
echo "== Results (complete pending human tasks in Tasklist, then run: curl $APP/api/scenarios/results.md)"
curl -sf "$APP/api/scenarios/results.md"
