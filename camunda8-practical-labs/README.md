# Camunda 8 – End-to-End Workflow Automation (PBJ-2 Practical Assignment, Labs 1–3)

Part of [workflow-automation-engineering](../README.md). Three complete workflow implementations on **Camunda 8.8** with BPMN, DMN, Camunda Forms, Java/Spring Boot external job workers, REST Connectors calling mock services, Tasklist human tasks and Operate monitoring with incident recovery.

| Lab | Process | BPMN / DMN / Forms | Worker app | Docs |
|---|---|---|---|---|
| 1 | Loan approval | [models](lab1-loan-approval/models) | [lab1-loan-approval](lab1-loan-approval) (port 8091) | [report](lab1-loan-approval/docs/REPORT.md) |
| 2 | E-commerce order processing and fulfilment | [models](lab2-order-processing/models) | [lab2-order-processing](lab2-order-processing) (port 8092) | [report](lab2-order-processing/docs/REPORT.md) |
| 3 | Hospital appointment and patient triage | [models](lab3-hospital-triage/models) | [lab3-hospital-triage](lab3-hospital-triage) (port 8093) | [report](lab3-hospital-triage/docs/REPORT.md) |

All data is **synthetic**. No real payment credentials, secrets or patient data are used anywhere; the workers actively reject real-looking identifiers (card numbers, real names).

## Presentation

[presentation/PBJ-2-Camunda-8-Labs.pptx](presentation/PBJ-2-Camunda-8-Labs.pptx) – 14-slide deck covering the brief, architecture, each lab, failure handling, incident recovery and verification, with speaker notes on every slide.

## Architecture

```
                         Camunda 8.8 (Camunda 8 Run)
   Desktop Modeler  ──►  Zeebe engine  ◄──── REST Connectors runtime ───► mock-services :8090
   (BPMN/DMN/forms)      Operate / Tasklist                                 /credit-bureau (Lab 1)
                               ▲                                           /payments      (Lab 2)
                  gRPC/REST    │ job activation / completion               /scheduling    (Lab 3)
           ┌───────────────────┼───────────────────┐                       /admin/faults  (fault injection)
     lab1 worker :8091   lab2 worker :8092   lab3 worker :8093
     (Spring Boot + camunda-spring-boot-starter; deploys its models on start-up)
```

* **BPMN** orchestrates each process; **DMN** (business rule task) makes the classification decision.
* **External job workers** (Java 17, Spring Boot 3.5, `io.camunda:camunda-spring-boot-starter:8.8.x`) implement validation, pricing, booking, notifications.
* **REST Connector** (`io.camunda:http-json:1`, Connector template `io.camunda.connectors.HttpJson.v2`) calls the external service.
* **mock-services** simulates the credit bureau, payment gateway and hospital scheduling system and can be switched DOWN/SLOW to create incidents.
* **Tasklist** handles the human tasks (Camunda user tasks with linked Camunda Forms); **Operate** is used to monitor, diagnose incidents, edit variables and retry.

## Repository layout

```
pom.xml                  Maven parent (Java 17, Spring Boot 3.5, Camunda 8.8 client)
lab-common/              shared helpers: variable readers, job-failure policy, scenario runner API, fault switches
mock-services/           mock REST APIs + fault injection (port 8090)
labN-*/
  models/                *.bpmn, *.dmn, *.form  (+ dmn-tests/ sample DMN evaluations)
  src/main/java/...      Spring Boot worker application
  src/test/java/...      JUnit tests for the worker logic
  test-data/             one JSON file per test scenario (input + expected outcome)
  api-examples/          REST request/response examples (+ requests.http)
  scripts/               run-scenarios.sh / .ps1 and fault toggles
  docs/                  REPORT.md, process-variables.md, dmn-rules.md, test-cases.md, images/, screenshots/
docs/SETUP.md            installing and running Camunda 8 Run, Modeler, the apps
presentation/            PBJ-2 slide deck (.pptx, with speaker notes)
```

## Quick start

Prerequisites: JDK 17+ (21 recommended), Maven 3.9+, [Camunda 8 Run 8.8](https://docs.camunda.io/docs/8.8/self-managed/quickstart/developer-quickstart/c8run/) and Camunda Desktop Modeler 5.x. Full steps in [docs/SETUP.md](docs/SETUP.md).

```bash
# all commands below run inside this folder:  cd camunda8-practical-labs

# 1. start Camunda 8 Run (separate terminal, inside the c8run folder)
./start.sh                      # Windows: .\c8run.exe start

# 2. build everything and run the tests
mvn clean verify

# 3. start the mock services and one lab (each in its own terminal)
mvn -pl mock-services spring-boot:run
mvn -pl lab1-loan-approval spring-boot:run      # deploys BPMN + DMN + forms, starts workers

# 4. run the test scenarios
./lab1-loan-approval/scripts/run-scenarios.sh   # Windows: .\lab1-loan-approval\scripts\run-scenarios.ps1

# 5. complete human tasks in Tasklist, inspect instances in Operate
#    http://localhost:8080/tasklist  and  http://localhost:8080/operate   (demo / demo)

# 6. print the live test-case table (expected vs actual from the engine)
curl http://localhost:8091/api/scenarios/results.md
```

Labs 2 and 3 are identical, using ports 8092 and 8093.

## Submission checklist → where to find it

| Checklist item | Lab 1 | Lab 2 | Lab 3 |
|---|---|---|---|
| BPMN model files + process design screenshot | [bpmn](lab1-loan-approval/models/loan-approval.bpmn), [png](lab1-loan-approval/docs/images/loan-approval.png) | [bpmn](lab2-order-processing/models/order-processing.bpmn), [png](lab2-order-processing/docs/images/order-processing.png) | [bpmn](lab3-hospital-triage/models/hospital-appointment.bpmn), [png](lab3-hospital-triage/docs/images/hospital-appointment.png) |
| DMN files, rule descriptions, sample results | [dmn](lab1-loan-approval/models/loan-eligibility.dmn), [rules](lab1-loan-approval/docs/dmn-rules.md) | [dmn](lab2-order-processing/models/order-priority.dmn), [rules](lab2-order-processing/docs/dmn-rules.md) | [dmn](lab3-hospital-triage/models/triage-category.dmn), [rules](lab3-hospital-triage/docs/dmn-rules.md) |
| Worker source code + run instructions | [src](lab1-loan-approval/src/main/java/edu/srmist/camunda/loan), [README](lab1-loan-approval/README.md) | [src](lab2-order-processing/src/main/java/edu/srmist/camunda/order), [README](lab2-order-processing/README.md) | [src](lab3-hospital-triage/src/main/java/edu/srmist/camunda/hospital), [README](lab3-hospital-triage/README.md) |
| REST Connector configuration + API examples | [api-examples](lab1-loan-approval/api-examples) | [api-examples](lab2-order-processing/api-examples) | [api-examples](lab3-hospital-triage/api-examples) |
| Process-variable dictionary | [variables](lab1-loan-approval/docs/process-variables.md) | [variables](lab2-order-processing/docs/process-variables.md) | [variables](lab3-hospital-triage/docs/process-variables.md) |
| Test cases (success, rejected, manual review, failure/recovery) | [test-cases](lab1-loan-approval/docs/test-cases.md) | [test-cases](lab2-order-processing/docs/test-cases.md) | [test-cases](lab3-hospital-triage/docs/test-cases.md) |
| Deployment + execution evidence, Tasklist, Operate, incident resolution | [screenshots](lab1-loan-approval/docs/screenshots) | [screenshots](lab2-order-processing/docs/screenshots) | [screenshots](lab3-hospital-triage/docs/screenshots) |
| Short report | [REPORT](lab1-loan-approval/docs/REPORT.md) | [REPORT](lab2-order-processing/docs/REPORT.md) | [REPORT](lab3-hospital-triage/docs/REPORT.md) |

> Screenshots in `docs/screenshots/` must be captured from your own running implementation. Each folder contains a README listing exactly which screens to capture and the file name to save them under. Diagrams in `docs/images/` are rendered directly from the model files in this repository.

## How the models were verified

* All BPMN files pass `bpmnlint` with the **camunda-compat `camunda-cloud-8-8` rule set** (the same checks Desktop Modeler runs before deploying), including FEEL syntax of every expression.
* Every DMN table is evaluated against the cases in `models/dmn-tests/*.json` (FEEL interpreter); the results are tabulated in each lab's `docs/dmn-rules.md`.
* Every form validates against the official Camunda Forms JSON schema.
* The worker logic is covered by JUnit tests (`mvn verify`).
