# Setup and run guide

## 1. Install the tools

| Tool | Version | Notes |
|---|---|---|
| JDK | 21 (17+ for the worker apps; Camunda 8 Run itself needs 21–23) | `java -version`, set `JAVA_HOME` |
| Maven | 3.9+ | `mvn -v` |
| Camunda 8 Run | 8.8.x | download from `https://downloads.camunda.cloud/release/camunda/c8run/8.8/`, extract the archive |
| Camunda Desktop Modeler | 5.x (supports Camunda 8.8) | used to open, inspect and screenshot the models |
| curl (or Postman) | any | the `.http` files also work in IntelliJ / VS Code REST Client |

## 2. Start Camunda 8 Run

```bash
cd c8run
./start.sh            # macOS / Linux
.\c8run.exe start     # Windows (PowerShell)
```

When it is up:

| Component | URL |
|---|---|
| Operate | http://localhost:8080/operate |
| Tasklist | http://localhost:8080/tasklist |
| REST API (v2) | http://localhost:8080/v2/ |
| gRPC (Zeebe) | localhost:26500 |
| Connectors runtime | bundled, starts automatically |

Login `demo` / `demo`. The API is unprotected by default, so the worker apps use `camunda.client.auth.method: none`. If your installation returns HTTP 401, change it to `basic` with `demo` / `demo` in each lab's `application.yaml`.

Stop with `./shutdown.sh` (Windows `.\c8run.exe stop`).

### Using the Docker Compose distribution instead

`./start.sh --docker` starts the Docker Compose stack. Then:

* set `camunda.client.rest-address: http://localhost:8088` in each lab's `application.yaml` (Operate/Tasklist are on port 8088 too);
* the Connectors container cannot reach `localhost` on your machine, so set `lab.mock-api-base-url: http://host.docker.internal:8090` – the scenario runner passes it to every instance as the `mockApiBaseUrl` variable, which the REST Connector URLs use.

## 3. Build and test

Run all Maven commands from the `camunda8-practical-labs` folder.

```bash
mvn clean verify
```

Builds all modules and runs the JUnit tests of the worker logic.

## 4. Run the services

Use one terminal per process:

```bash
mvn -pl mock-services spring-boot:run          # :8090  credit bureau, payment gateway, scheduling, fault switches
mvn -pl lab1-loan-approval spring-boot:run     # :8091
mvn -pl lab2-order-processing spring-boot:run  # :8092
mvn -pl lab3-hospital-triage spring-boot:run   # :8093
```

On start-up every lab app **deploys its BPMN, DMN and form files** (`@Deployment` annotation) and opens its job workers. Check the log for `Deployed:` lines, then open Operate → *Processes* to see the deployed definition.

Alternatively deploy from Desktop Modeler (5.4x ships a ready *c8run (local)* connection – just click the rocket icon and **Deploy**; older versions: open the `.bpmn`, `.dmn` and `.form` files of a lab, click the rocket icon, choose *Camunda 8 Self-Managed*, cluster endpoint `http://localhost:26500` (or `http://localhost:8080` if your Modeler version deploys over REST), no authentication). Deploy the DMN and forms as well as the BPMN. A full demo walkthrough is in [DEMO.md](DEMO.md).

## 5. Run the scenarios

```bash
./lab1-loan-approval/scripts/run-scenarios.sh        # Linux / macOS / Git Bash
.\lab1-loan-approval\scripts\run-scenarios.ps1       # Windows PowerShell
```

The script lists the scenarios, starts all automatic ones and prints the live results table. Failure/recovery scenarios are marked *manual start* because a fault must be switched on first – the lab README walks through them step by step.

Useful endpoints of every lab app (replace the port):

| Method | URL | Purpose |
|---|---|---|
| GET | `http://localhost:8091/api/scenarios` | list scenarios with description + expected outcome |
| POST | `http://localhost:8091/api/scenarios/{name}/start` | start one scenario |
| POST | `http://localhost:8091/api/scenarios/start-all` | start all automatic scenarios |
| POST | `http://localhost:8091/api/process-instances` | start an instance with any JSON body |
| GET | `http://localhost:8091/api/scenarios/results.md` | live test-case table (expected vs actual) |

Fault switches (mock services): `PUT http://localhost:8090/admin/faults/{credit|payment|scheduling}?mode=UP|DOWN|SLOW`, `GET http://localhost:8090/admin/faults`.

## 6. Troubleshooting

| Symptom | Fix |
|---|---|
| App fails with connection refused on 26500/8080 | Camunda 8 Run is not running yet |
| HTTP 401 from the client | switch `camunda.client.auth.method` to `basic` (demo/demo) |
| REST Connector task raises "Connection refused" | mock-services not running, or Camunda runs in Docker (set `mockApiBaseUrl`) |
| Results table says "Not yet available" | Operate data is indexed asynchronously – call it again after a few seconds |
| Human task not visible in Tasklist | in the task list filter choose *All open tasks*; tasks are assigned to candidate groups |
