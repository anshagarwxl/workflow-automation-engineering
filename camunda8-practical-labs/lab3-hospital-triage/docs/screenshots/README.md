# Lab 3 – Hospital appointment and triage – Screenshot evidence

Capture these from **your own running implementation** and save them in this folder with the given file names (PNG). The report and test-case table refer to these names. Tip: include the browser address bar / Operate instance key so the screenshot can be matched to the test case.

| # | File name | What to capture |
|---|---|---|
| 01 | `01-modeler-bpmn.png` | Desktop Modeler with `hospital-appointment.bpmn` |
| 02 | `02-modeler-dmn.png` | Desktop Modeler with `triage-category.dmn` |
| 03 | `03-modeler-rest-connector.png` | Properties panel of *Retrieve available slots* (GET URL, query parameters, result expression, retries/back-off) |
| 04 | `04-deployment.png` | Deployment evidence (worker log / Modeler) and the definition in Operate |
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
| 18 | `18-worker-logs.png` | Worker console with `[validate]`, `[booking]`, `[notify]` lines |
