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

<!-- Paste the output of GET http://localhost:8091/api/scenarios/results.md here after running the scenarios. -->

_Not executed yet – paste the live results table here._

## Unit tests of the worker logic

`mvn -pl lab-common,lab1-loan-approval verify` runs the JUnit tests in [../src/test/java](../src/test/java). DMN sample evaluations: [dmn-rules.md](dmn-rules.md).
