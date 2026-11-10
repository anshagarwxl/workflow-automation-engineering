# Lab 3 – Hospital appointment and triage – Screenshot evidence

All screenshots below were captured on **10 Nov 2026** from the live run on a MacBook: **Camunda 8 Run 8.8.33** (Operate/Tasklist at `localhost:8080`) and **Camunda Desktop Modeler 5.49.0** connected to `c8run (local)`. Operate/Tasklist pages were captured with Playwright while the scenarios ran; Modeler views were captured from the Modeler window. The Operate header of each instance screenshot shows the process instance key, which matches the *Execution results* table in [../test-cases.md](../test-cases.md).

| # | File name | What it shows |
|---|---|---|
| 01 | `01-modeler-bpmn.png` | Desktop Modeler with `hospital-appointment.bpmn` |
| 02 | `02-modeler-dmn.png` | Desktop Modeler with `triage-category.dmn` (input columns) |
| 02b | `02b-modeler-dmn-outputs.png` | Same table scrolled right: output columns *Triage category*, *Reason*, *Target response* (extra) |
| 03 | `03-modeler-rest-connector.png` | Properties panel of the *Book earliest slot* REST Connector (POST URL `/scheduling/api/v1/bookings`) |
| 03b | `03b-modeler-rest-connector-payload.png` | *Book earliest slot* REST Connector: request body + output mapping (`bookingResponse`, result expression) (extra) |
| 03c | `03c-modeler-connector-error-handling.png` | *Book earliest slot* error expression: HTTP 409 → BPMN error `SLOT_TAKEN` (extra) |
| 04 | `04-deployment.png` | Deployment evidence (worker log / Modeler) and the definition in Operate |
| 04b | `04b-modeler-deployed.png` | Desktop Modeler → rocket → *Deploy*: "hospital-appointment.bpmn deployed" against c8run (extra) |
| 05 | `05-operate-overview.png` | Operate process view with all scenario instances |
| 06 | `06-operate-routine-booked.png` | Scenario 01: path to *Appointment booked* with `triage.category = ROUTINE`, `availableSlotCount`, `bookingId` |
| 07 | `07-operate-dmn-evaluation.png` | Operate → Decisions: `triage-category` evaluation |
| 08 | `08-tasklist-priority-review.png` | Tasklist: *Priority staff review* for SYN-PAT-3002 (Proceed to booking) |
| 09 | `09-operate-priority-booked.png` | Scenario 02 after the review: path through the user task to *Appointment booked* |
| 10 | `10-tasklist-urgent-assessment.png` | Tasklist: *Immediate clinical assessment* for SYN-PAT-3003 |
| 11 | `11-operate-urgent-handled.png` | Scenario 03 completed at *Urgent case handled* |
| 12 | `12-tasklist-no-slot-review.png` | Tasklist: *Staff review: no suitable slot* for SYN-PAT-3004 (Added to waiting list) |
| 13 | `13-operate-no-slot-handled.png` | Scenario 04 completed at *Handled by staff*, `availableSlotCount = 0`, `finalOutcome = WAITLISTED` |
| 14 | `14-mock-scheduling-down.png` | Terminal: `PUT /admin/faults/scheduling?mode=DOWN` response |
| 15 | `15-operate-scheduling-incident.png` | Scenario 05: incident on *Retrieve available slots* (HTTP 503) |
| 16 | `16-operate-incident-resolved.png` | Scenario 05 after `mode=UP` and **Retry**: instance completed with `finalOutcome = BOOKED` |
| 17 | `17-results-table.png` | Output of `GET http://localhost:8093/api/scenarios/results.md` |
| 18 | `18-worker-logs.png` | Worker console with `[validate]` and `[notify]` lines (slot lookup and booking run in the REST Connector runtime, not in the workers) |
