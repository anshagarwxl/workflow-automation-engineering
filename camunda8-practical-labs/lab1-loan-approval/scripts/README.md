# Scripts

| Script | What it does |
|---|---|
| `run-scenarios.sh` / `run-scenarios.ps1` | lists scenarios, starts all automatic ones (or one: `./run-scenarios.sh 03-manual-review`), prints the live results table |

No fault switch is needed for Lab 1: the worker-failure scenario (`05-worker-failure-recovery`) fails because of its data, and the credit API error/timeout scenarios (`04a`, `04b`) use special synthetic applicant ids. To make *every* credit call fail instead:

```bash
curl -X PUT "http://localhost:8090/admin/faults/credit?mode=DOWN"   # or SLOW for timeouts
curl -X PUT "http://localhost:8090/admin/faults/credit?mode=UP"
```
