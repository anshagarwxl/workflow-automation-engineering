# Lab 1 – Loan approval – Screenshot evidence

All screenshots below were captured on **10 Nov 2026** from the live run on a MacBook: **Camunda 8 Run 8.8.33** (Operate/Tasklist at `localhost:8080`) and **Camunda Desktop Modeler 5.49.0** connected to `c8run (local)`. Operate/Tasklist pages were captured with Playwright while the scenarios ran; Modeler views were captured from the Modeler window. The Operate header of each instance screenshot shows the process instance key, which matches the *Execution results* table in [../test-cases.md](../test-cases.md).

| # | File name | What it shows |
|---|---|---|
| 01 | `01-modeler-bpmn.png` | Desktop Modeler with `loan-approval.bpmn` open (whole diagram visible) |
| 02 | `02-modeler-dmn.png` | Desktop Modeler with `loan-eligibility.dmn` decision table |
| 03 | `03-modeler-rest-connector.png` | Modeler properties panel of the *Request credit assessment* REST Connector (authentication, POST URL, headers) |
| 03b | `03b-modeler-rest-connector-payload.png` | Same connector: request body and `resultVariable = creditCheckResponse` (extra) |
| 04 | `04-deployment.png` | Deployment evidence: worker log lines "Deployed: loan-approval-process…" or Modeler "Deployment succeeded", plus Operate → Processes showing the definition |
| 04b | `04b-modeler-deployed.png` | Desktop Modeler → rocket → *Deploy*: "Process definition deployed – loan-approval.bpmn" against c8run (extra) |
| 05 | `05-operate-overview.png` | Operate process view with the instances of all scenarios (counters on the flow nodes) |
| 06 | `06-operate-approved-instance.png` | Scenario 01 completed instance: highlighted path + variables (`eligibility`, `finalOutcome = APPROVED`) |
| 07 | `07-operate-dmn-evaluation.png` | Operate → Decisions: `loan-eligibility` decision instance with inputs/outputs and matched rule |
| 08 | `08-operate-rejected-instance.png` | Scenario 02 completed instance (DMN REJECTED path) |
| 09 | `09-tasklist-manual-review.png` | Tasklist: *Manual underwriting review* task for LA-1003 with the form filled (Approve) |
| 10 | `10-operate-manual-review-completed.png` | Scenario 03 after task completion (path through the user task, `reviewDecision`, `decidedBy = UNDERWRITER`) |
| 11 | `11-operate-credit-api-error.png` | Scenario 04a/04b: path through the error boundary event, variables `creditCheckStatus = FAILED`, `creditCheckError` |
| 12 | `12-tasklist-review-after-api-error.png` | Tasklist form for LA-1004 showing *Credit check status FAILED* and the error text |
| 13 | `13-operate-incident.png` | Scenario 05: incident on *Validate applicant information* with the error message (monthlyIncome must be numeric) |
| 14 | `14-operate-fix-variable.png` | Scenario 05: editing `monthlyIncome` to 45000 in the Variables panel |
| 15 | `15-operate-incident-resolved.png` | Scenario 05 after **Retry**: incident gone, instance completed with `finalOutcome = APPROVED` |
| 16 | `16-results-table.png` | Terminal/browser output of `GET http://localhost:8091/api/scenarios/results.md` with all PASS |
| 17 | `17-worker-logs.png` | Worker console showing `[validate]`, `[offer]`, `[rejection]` log lines |

**Note:** `14-operate-fix-variable.png` was not kept – the automated capture taken while editing `monthlyIncome` was identical to `13-operate-incident.png`. The fix itself (variable set to 45000, job retries restored, incident retried) is visible in the run log and its result in `15-operate-incident-resolved.png`.
