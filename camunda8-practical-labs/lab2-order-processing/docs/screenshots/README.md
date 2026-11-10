# Lab 2 – Order processing – Screenshot evidence

All screenshots below were captured on **10 Nov 2026** from the live run on a MacBook: **Camunda 8 Run 8.8.33** (Operate/Tasklist at `localhost:8080`) and **Camunda Desktop Modeler 5.49.0** connected to `c8run (local)`. Operate/Tasklist pages were captured with Playwright while the scenarios ran; Modeler views were captured from the Modeler window. The Operate header of each instance screenshot shows the process instance key, which matches the *Execution results* table in [../test-cases.md](../test-cases.md).

| # | File name | What it shows |
|---|---|---|
| 01 | `01-modeler-bpmn.png` | Desktop Modeler with `order-processing.bpmn` |
| 02 | `02-modeler-dmn.png` | Desktop Modeler with `order-priority.dmn` |
| 03 | `03-modeler-rest-connector.png` | Properties panel of the *Process payment* REST Connector (POST URL, `Idempotency-Key` header) |
| 03b | `03b-modeler-rest-connector-payload.png` | Same connector: request body and result expression (extra) |
| 04 | `04-deployment.png` | Deployment evidence (worker log "Deployed…" / Modeler success) and the definition in Operate |
| 04b | `04b-modeler-deployed.png` | Desktop Modeler → rocket → *Deploy*: "order-processing.bpmn deployed" against c8run (extra) |
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
| 19 | `19-operate-inventory-incident.png` | Scenario 08 (alternative failure): worker incident on *Validate order, check stock and price* while the inventory dependency is DOWN (extra) |
