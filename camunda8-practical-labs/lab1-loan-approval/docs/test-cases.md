# Lab 1 – Loan approval – Test cases

Each row is one file in [../test-data](../test-data) (the JSON holds the full input plus `_expectedRoute` / `_expectedOutcome`). The "Minimum test scenario" column maps the rows to the scenarios required by the assignment; rows marked *(extra)* are additional coverage.

## Planned test cases

| ID | Minimum test scenario | Scenario file | Key inputs | Expected route | Expected final outcome | Actual outcome | Status |
|---|---|---|---|---|---|---|---|
| 01 | Approval criteria met | `01-approved` | applicantId=SYN-1001<br>applicantAge=34<br>monthlyIncome=120000<br>existingMonthlyEmi=5000<br>loanAmount=500000<br>loanTermMonths=36 | Validate -> Credit assessment -> DMN APPROVED -> Issue loan offer | **APPROVED** | _from results table_ | _PASS/FAIL_ |
| 02 | Fails eligibility criteria | `02-rejected-eligibility` | applicantId=SYN-1002<br>applicantAge=41<br>monthlyIncome=70000<br>existingMonthlyEmi=12000<br>loanAmount=400000<br>loanTermMonths=48 | Validate -> Credit assessment -> DMN REJECTED -> Send rejection notice | **REJECTED** | _from results table_ | _PASS/FAIL_ |
| 03 | Requires manual review | `03-manual-review` | applicantId=SYN-1003<br>applicantAge=29<br>monthlyIncome=60000<br>existingMonthlyEmi=8000<br>loanAmount=800000<br>loanTermMonths=60 | Validate -> Credit assessment -> DMN MANUAL_REVIEW -> Manual underwriting review (APPROVE) -> Issue loan offer | **APPROVED** | _from results table_ | _PASS/FAIL_ |
| 04a | Credit API timeout | `04a-credit-api-timeout` | applicantId=SYN-9408<br>applicantAge=38<br>monthlyIncome=90000<br>existingMonthlyEmi=10000<br>loanAmount=600000<br>loanTermMonths=48 | Validate -> Credit assessment (timeout) -> boundary error CREDIT_API_ERROR -> Manual underwriting review (REJECT) -> Send rejection notice | **REJECTED** | _from results table_ | _PASS/FAIL_ |
| 04b | Credit API error (HTTP 500) | `04b-credit-api-error` | applicantId=SYN-9500<br>applicantAge=45<br>monthlyIncome=150000<br>existingMonthlyEmi=20000<br>loanAmount=900000<br>loanTermMonths=60 | Validate -> Credit assessment (HTTP 500) -> boundary error CREDIT_API_ERROR -> Manual underwriting review (REJECT) -> Send rejection notice | **REJECTED** | _from results table_ | _PASS/FAIL_ |
| 05 | Worker failure + correction + recovery | `05-worker-failure-recovery` | applicantId=SYN-1005<br>applicantAge=31<br>monthlyIncome=45k<br>existingMonthlyEmi=2000<br>loanAmount=300000<br>loanTermMonths=36 | Validate (INCIDENT) -> [fix variable in Operate + retry] -> Credit assessment -> DMN APPROVED -> Issue loan offer | **APPROVED** | _from results table_ | _PASS/FAIL_ |
| 06 | Fails validation (extra) | `06-invalid-application` | applicantId=SYN-1006<br>applicantAge=19<br>monthlyIncome=30000<br>existingMonthlyEmi=0<br>loanAmount=6000000<br>loanTermMonths=84 | Validate (INVALID) -> Send rejection notice | **REJECTED** | _from results table_ | _PASS/FAIL_ |

## How the "Actual" columns are produced

1. Start the scenarios: `../scripts/run-scenarios.sh` (failure/recovery scenarios: follow the steps in [../README.md](../README.md)).
2. Complete the human tasks in Tasklist as described in the README.
3. Call `GET http://localhost:8091/api/scenarios/results.md`. The lab app queries the Camunda REST API for each started instance (state, active incidents, active element, process variables) and prints a Markdown table with **Expected outcome**, **Actual outcome** (`finalOutcome` of the completed instance) and **Status** (PASS / FAIL / INCIDENT / IN PROGRESS).
4. Paste that table below and add the Operate / Tasklist screenshot file names as evidence.

## Execution results

Executed live on **10 Nov 2026** on a MacBook (macOS) against **Camunda 8 Run 8.8.33** (Zeebe + Operate + Tasklist + Connectors on `localhost:8080`), models opened and deployed from **Camunda Desktop Modeler 5.49.0**. Table produced by `GET http://localhost:8091/api/scenarios/results.md`; the user tasks were completed in Tasklist and incidents were resolved in Operate as described per scenario. Screenshots: [screenshots/](screenshots/).

| Scenario | Process instance | Description | Expected outcome | Actual outcome | Status | Key variables |
|---|---|---|---|---|---|---|
| 01-approved | 2251799813687411 | Strong salaried applicant meeting all approval criteria | APPROVED | APPROVED | PASS | applicationId=LA-1001<br>validationStatus=VALID<br>creditCheckStatus=COMPLETED<br>creditScore=780<br>dtiRatio=0.18<br>eligibility={"decision":"APPROVED","reason":"Meets all automatic approval criteria","riskBand":"LOW"}<br>decidedBy=AUTOMATED_DMN<br>sanctionedAmount=500000 |
| 02-rejected-eligibility | 2251799813687431 | Applicant fails eligibility: bureau credit score 520 (< 550) | REJECTED | REJECTED | PASS | applicationId=LA-1002<br>validationStatus=VALID<br>creditCheckStatus=COMPLETED<br>creditScore=520<br>dtiRatio=0.322<br>eligibility={"decision":"REJECTED","reason":"Credit score below 550","riskBand":"HIGH"}<br>decidedBy=AUTOMATED_DMN<br>rejectionReasons=["Eligibility rules: Credit score below 550"] |
| 03-manual-review | 2251799813687450 | Near-prime applicant (score 690, DTI 0.43) referred to an underwriter; approve it in Tasklist | APPROVED | APPROVED | PASS | applicationId=LA-1003<br>validationStatus=VALID<br>creditCheckStatus=COMPLETED<br>creditScore=690<br>dtiRatio=0.43<br>eligibility={"decision":"MANUAL_REVIEW","reason":"Good profile but outside automatic approval thresholds","riskBand":"MEDIUM"}<br>reviewDecision=APPROVE<br>decidedBy=UNDERWRITER<br>sanctionedAmount=800000 |
| 04a-credit-api-timeout | 2251799813687469 | Credit bureau does not answer within the 5 s read timeout (applicant SYN-9408); error is caught and routed to manual review; reject it in Ta... | REJECTED | REJECTED | PASS | applicationId=LA-1004<br>validationStatus=VALID<br>creditCheckStatus=FAILED<br>dtiRatio=0.287<br>reviewDecision=REJECT<br>decidedBy=UNDERWRITER<br>rejectionReasons=["Underwriter: Credit bureau timed out; cannot verify credit history"] |
| 04b-credit-api-error | 2251799813687511 | Credit bureau answers HTTP 500 (applicant SYN-9500); error is caught and routed to manual review; reject it in Tasklist | REJECTED | REJECTED | PASS | applicationId=LA-1007<br>validationStatus=VALID<br>creditCheckStatus=FAILED<br>dtiRatio=0.267<br>reviewDecision=REJECT<br>decidedBy=UNDERWRITER<br>rejectionReasons=["Underwriter: Credit bureau returned HTTP 500; history cannot be verified"] |
| 05-worker-failure-recovery | 2251799813687600 | monthlyIncome is sent as the text '45k'. The validation worker cannot parse it and fails the job with 0 retries -> incident. Fix: in Operate... | APPROVED | APPROVED | PASS | applicationId=LA-1005<br>validationStatus=VALID<br>creditCheckStatus=COMPLETED<br>creditScore=750<br>dtiRatio=0.266<br>eligibility={"decision":"APPROVED","reason":"Meets all automatic approval criteria","riskBand":"LOW"}<br>decidedBy=AUTOMATED_DMN<br>sanctionedAmount=300000 |
| 06-invalid-application | 2251799813687642 | Applicant aged 19 and loan amount above the 50 lakh limit: validation fails, application is rejected without calling the bureau | REJECTED | REJECTED | PASS | applicationId=LA-1006<br>validationStatus=INVALID<br>decidedBy=VALIDATION<br>rejectionReasons=["Validation: applicant age must be between 21 and 65","Validation: loanAmount must be between 10000 and 5000000"] |

## Unit tests of the worker logic

`mvn -pl lab-common,lab1-loan-approval verify` runs the JUnit tests in [../src/test/java](../src/test/java). DMN sample evaluations: [dmn-rules.md](dmn-rules.md).
