# Live demo – Desktop Modeler + Camunda 8 Run

How to show the three labs live. Everything below was rehearsed end-to-end on 10 Nov 2026 (Camunda 8 Run 8.8.33, Desktop Modeler 5.49.0, JDK 21, macOS); the recorded results are in each lab's `docs/test-cases.md` and the screenshots in `docs/screenshots/`.

**Rule followed in all labs:** every call to an external URL is made by a **REST Connector** task (`io.camunda:http-json:1`), never by a Java service-task worker. Java workers only do in-process work (validation, offer calculation, notifications). See the table *External calls use the REST Connector only* in [../README.md](../README.md).

| Lab | REST Connector task(s) | Endpoint (mock-services :8090) |
|---|---|---|
| 1 Loan approval | *Request credit assessment* | `POST /credit-bureau/api/v1/assessments` |
| 2 Order processing | *Process payment* | `POST /payments/api/v1/charges` |
| 3 Hospital triage | *Retrieve available slots*, *Book earliest slot* | `GET /scheduling/api/v1/slots`, `POST /scheduling/api/v1/bookings` |

## 1. Start the platform (≈ 2 min)

```bash
cd ~/Developer/camunda/c8run-8.8.33 && ./start.sh          # Zeebe, Operate, Tasklist, Connectors on :8080
cd <repo>/camunda8-practical-labs && mvn -B clean package   # 24 unit tests, builds the jars
java -jar mock-services/target/mock-services-1.0.0.jar &            # :8090 credit / payment / scheduling + fault switches
java -jar lab1-loan-approval/target/lab1-loan-approval-1.0.0.jar &  # :8091
java -jar lab2-order-processing/target/lab2-order-processing-1.0.0.jar &  # :8092
java -jar lab3-hospital-triage/target/lab3-hospital-triage-1.0.0.jar &    # :8093
```

Open Operate http://localhost:8080/operate and Tasklist http://localhost:8080/tasklist (login `demo` / `demo`).

## 2. Show the models in Desktop Modeler and deploy

1. Desktop Modeler → *File → Open File…* → `labN/models/*.bpmn` and `*.dmn`.
2. Click a REST Connector task → properties panel shows **REST Outbound Connector**: method, URL, headers, request body, result variable / result expression, error expression, retries.
3. Open the DMN → decision table (hit policy *First*).
4. Click the **rocket** icon (bottom bar, connection *c8run (local)*) → **Deploy** → toast *"Process definition deployed"*. Deploy the DMN and forms the same way (the lab apps also deploy them on start-up via `@Deployment`).
5. Operate → *Processes* shows the deployed definition.

## 3. Run the scenarios

```bash
curl -X POST http://localhost:8091/api/scenarios/start-all   # 8092 for Lab 2, 8093 for Lab 3
```

Then walk through:

* **Operate**: completed instances (highlighted path + variables), *Decisions* tab for the DMN evaluation.
* **Tasklist**: claim and complete the user tasks (underwriter review, manual order verification, priority / urgent / no-slot staff tasks).
* `curl http://localhost:809N/api/scenarios/results.md` prints expected vs actual for every scenario.

## 4. Failure → incident → fix → retry

| Lab | Break it | What you see | Fix |
|---|---|---|---|
| 1 | scenario `05-worker-failure-recovery` (`monthlyIncome = "45k"`) | worker fails with 0 retries → incident on *Validate applicant information* | Operate → Variables → `monthlyIncome = 45000` → **Retry** → APPROVED |
| 1 | scenarios `04a` / `04b` (bureau timeout / HTTP 500) | REST Connector error caught by error boundary → manual review | Tasklist → decide |
| 2 | `curl -X PUT "localhost:8090/admin/faults/payment?mode=DOWN"`, start `05-payment-service-outage-recovery` | connector retries 3× (PT10S back-off) → incident on *Process payment* | `mode=UP` → **Retry** in Operate → FULFILLED |
| 3 | `curl -X PUT "localhost:8090/admin/faults/scheduling?mode=DOWN"`, start `05-scheduling-api-outage-recovery` | incident on *Retrieve available slots* (HTTP 503) | `mode=UP` → **Retry** → BOOKED |
| 3 | slot taken between lookup and booking | *Book earliest slot* gets HTTP 409 → error expression raises BPMN error `SLOT_TAKEN` → staff review task | Tasklist → staff books manually / wait-lists |

Start a single scenario with `curl -X POST http://localhost:809N/api/scenarios/<name>/start`.

## 5. Stop everything

Kill the four `java -jar` processes, then `cd ~/Developer/camunda/c8run-8.8.33 && ./shutdown.sh`.
