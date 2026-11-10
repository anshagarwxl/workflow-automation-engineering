# Lab 1 – REST integration and API examples

## Credit-assessment REST Connector

BPMN task `Task_CreditAssessment` ("Request credit assessment") uses the Camunda **REST Connector** (element template `io.camunda.connectors.HttpJson.v2`, version 11). Configuration exactly as stored in [`loan-approval.bpmn`](../models/loan-approval.bpmn):

| Property | Value |
|---|---|
| Task definition type | `io.camunda:http-json:1` |
| Retries | `3` |
| Input `authentication.type` | `noAuth` |
| Input `method` | `POST` |
| Input `url` | `=(if is defined(mockApiBaseUrl) then mockApiBaseUrl else "http://localhost:8090") + "/credit-bureau/api/v1/assessments"` |
| Input `headers` | `={"X-Correlation-Id": applicationId}` |
| Input `storeResponse` | `false` |
| Input `connectionTimeoutInSeconds` | `5` |
| Input `readTimeoutInSeconds` | `5` |
| Input `body` | `={applicationId: applicationId, applicantId: applicantId, monthlyIncome: monthlyIncome, existingMonthlyEmi: existingMonthlyEmi, loanAmount: loanAmount}` |
| Input `ignoreNullValues` | `false` |
| Header `elementTemplateVersion` | `11` |
| Header `elementTemplateId` | `io.camunda.connectors.HttpJson.v2` |
| Header `resultVariable` | `creditCheckResponse` |
| Header `resultExpression` | `={creditCheckStatus: "COMPLETED", creditAssessmentId: response.body.assessmentId, creditScore: response.body.creditScore, riskGrade: response.body.riskGrade, bureauStatus: response.body.bureauStatus, activeLoans: response.body.activeLoans, defaultsLast24Months: response.body.defaultsLast24Months}` |
| Header `errorExpression` | `=if error != null then bpmnError("CREDIT_API_ERROR", "Credit assessment API failed: " + string(if error.message != null then error.message else "no details"), {creditCheckError: string(if error.message != null then error.message else "no details")}) else null` |
| Header `retryBackoff` | `PT0S` |

* **URL** – `mockApiBaseUrl` is optional; without it the connector calls `http://localhost:8090`.
* **Result variable** `creditCheckResponse` stores the whole HTTP response (`status`, `headers`, `body`) – inspect it in Operate exactly as in the course tutorial.
* **Result expression** maps the response body into process variables (see [`credit-assessment.result-variables.json`](credit-assessment.result-variables.json)).
* **Error expression** turns *any* failure (HTTP 4xx/5xx, connection refused, the 5 s read timeout) into the BPMN error `CREDIT_API_ERROR` with variable `creditCheckError`. The error boundary event on the task catches it, sets `creditCheckStatus = "FAILED"` and routes the application to manual underwriting instead of leaving it stuck.

## Request / response

| File | Content |
|---|---|
| [credit-assessment.request.json](credit-assessment.request.json) | body sent by the connector (built from process variables) |
| [credit-assessment.response.json](credit-assessment.response.json) | HTTP 200 response from the mock bureau |
| [credit-assessment.error-500.response.json](credit-assessment.error-500.response.json) | HTTP 500 response for applicant `SYN-9500` |
| [credit-assessment.result-variables.json](credit-assessment.result-variables.json) | variables written by the result expression |
| [start-instance.request.json](start-instance.request.json) | body for `POST http://localhost:8091/api/process-instances` |
| [camunda-rest-create-instance.request.json](camunda-rest-create-instance.request.json) | body for `POST http://localhost:8080/v2/process-instances` (Camunda REST API) |
| [requests.http](requests.http) | runnable requests (IntelliJ / VS Code REST Client) |

curl equivalent of the connector call:

```bash
curl -X POST http://localhost:8090/credit-bureau/api/v1/assessments \
  -H "Content-Type: application/json" -H "X-Correlation-Id: LA-1001" \
  -d @credit-assessment.request.json
```

Synthetic bureau records: `SYN-1001` (780, A), `SYN-1002` (520, E, 1 default), `SYN-1003` (690, C), `SYN-1004` (640, D, 2 defaults), `SYN-1005` (750, A), `SYN-1006` (735, B); any other id → `NO_HISTORY`, score 600. `SYN-9408` → no answer within the timeout, `SYN-9500` → HTTP 500.
