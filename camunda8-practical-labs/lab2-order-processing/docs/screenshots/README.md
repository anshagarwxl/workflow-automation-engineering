# Lab 2 – Order processing – Screenshot evidence

Capture these from **your own running implementation** and save them in this folder with the given file names (PNG). The report and test-case table refer to these names. Tip: include the browser address bar / Operate instance key so the screenshot can be matched to the test case.

| # | File name | What to capture |
|---|---|---|
| 01 | `01-modeler-bpmn.png` | Desktop Modeler with `order-processing.bpmn` |
| 02 | `02-modeler-dmn.png` | Desktop Modeler with `order-priority.dmn` |
| 03 | `03-modeler-rest-connector.png` | Properties panel of *Process payment* (URL, Idempotency-Key header, body, result expression, retries/back-off) |
| 04 | `04-deployment.png` | Deployment evidence (worker log "Deployed…" / Modeler success) and the definition in Operate |
| 05 | `05-operate-overview.png` | Operate process view with all scenario instances |
| 06 | `06-operate-fulfilled-instance.png` | Scenario 01: completed path to *Order fulfilled* with `orderTotal`, `paymentStatus = AUTHORISED`, `shipmentId` |
| 07 | `07-operate-dmn-evaluation.png` | Operate → Decisions: `order-priority` evaluation (inputs, matched rule, outputs) |
| 08 | `08-operate-out-of-stock.png` | Scenario 02: path to *Not fulfilled – out of stock* with `stockStatus = INSUFFICIENT_STOCK` |
| 09 | `09-operate-payment-declined.png` | Scenario 03: path to *Payment declined* with `paymentDeclineReason = CARD_DECLINED` |
| 10 | `10-tasklist-manual-verification.png` | Tasklist: *Manually verify order* for ORD-2004 (form filled, Approve) |
| 11 | `11-operate-verified-and-fulfilled.png` | Scenario 04 after approval: path through the user task to *Order fulfilled* |
| 12 | `12-tasklist-verification-rejected.png` | Scenario 07: Reject in Tasklist, then the instance at *Order cancelled* |
| 13 | `13-mock-payment-down.png` | Terminal: `PUT /admin/faults/payment?mode=DOWN` response |
| 14 | `14-operate-payment-incident.png` | Scenario 05: incident on *Process payment* (HTTP 503 message, retries exhausted) |
| 15 | `15-mock-payment-up.png` | Terminal: `PUT /admin/faults/payment?mode=UP` response |
| 16 | `16-operate-incident-resolved.png` | Scenario 05 after **Retry**: instance completed (`paymentStatus = AUTHORISED`, `finalOutcome = FULFILLED`) |
| 17 | `17-results-table.png` | Output of `GET http://localhost:8092/api/scenarios/results.md` |
| 18 | `18-worker-logs.png` | Worker console with `[validate]`, `[fulfil]`, `[notify]` lines |
