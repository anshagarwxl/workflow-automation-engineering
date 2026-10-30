# Lab 2 – Order processing – Test cases

Each row is one file in [../test-data](../test-data) (the JSON holds the full input plus `_expectedRoute` / `_expectedOutcome`). The "Minimum test scenario" column maps the rows to the scenarios required by the assignment; rows marked *(extra)* are additional coverage.

## Planned test cases

| ID | Minimum test scenario | Scenario file | Key inputs | Expected route | Expected final outcome | Actual outcome | Status |
|---|---|---|---|---|---|---|---|
| 01 | Stock available, payment succeeds | `01-in-stock-payment-success` | customerCategory=REGULAR<br>productId=SKU-HEADPHONES<br>quantity=2<br>deliveryPreference=STANDARD<br>paymentToken=tok_test_visa_success | Validate -> In stock -> DMN LOW -> no verification -> Payment AUTHORISED -> Fulfil -> Confirmation | **FULFILLED** | _from results table_ | _PASS/FAIL_ |
| 02 | Insufficient stock | `02-insufficient-stock` | customerCategory=REGULAR<br>productId=SKU-GPU-RTX<br>quantity=5<br>deliveryPreference=STANDARD<br>paymentToken=tok_test_visa_success | Validate -> Insufficient stock -> Notify: out of stock | **OUT_OF_STOCK** | _from results table_ | _PASS/FAIL_ |
| 03 | Payment declined | `03-payment-declined` | customerCategory=PREMIUM<br>productId=SKU-KEYBOARD<br>quantity=1<br>deliveryPreference=EXPRESS<br>paymentToken=tok_test_card_declined | Validate -> In stock -> DMN HIGH -> Payment DECLINED -> Notify: payment declined | **PAYMENT_DECLINED** | _from results table_ | _PASS/FAIL_ |
| 04 | High-value order → manual review | `04-high-value-manual-review` | customerCategory=REGULAR<br>productId=SKU-LAPTOP-PRO<br>quantity=2<br>deliveryPreference=STANDARD<br>paymentToken=tok_test_mastercard_success | Validate -> In stock -> DMN HIGH -> Manually verify order (APPROVE) -> Payment AUTHORISED -> Fulfil | **FULFILLED** | _from results table_ | _PASS/FAIL_ |
| 05 | Payment service failure + recovery | `05-payment-service-outage-recovery` (manual start) | customerCategory=REGULAR<br>productId=SKU-MONITOR<br>quantity=1<br>deliveryPreference=SAME_DAY<br>paymentToken=tok_test_visa_success | Validate -> DMN CRITICAL -> Process payment (INCIDENT) -> [service UP + retry] -> AUTHORISED -> Fulfil | **FULFILLED** | _from results table_ | _PASS/FAIL_ |
| 06 | Invalid order (extra) | `06-invalid-order` | customerCategory=REGULAR<br>productId=SKU-HEADPHONES<br>quantity=0<br>deliveryPreference=STANDARD<br>paymentToken=4111 1111 1111 1111 | Validate (orderValid=false) -> Notify: order rejected | **REJECTED_INVALID** | _from results table_ | _PASS/FAIL_ |
| 07 | Exception condition, review rejects (extra) | `07-new-customer-review-rejected` | customerCategory=NEW<br>productId=SKU-PHONE<br>quantity=1<br>deliveryPreference=STANDARD<br>paymentToken=tok_test_visa_success | Validate -> DMN HIGH -> Manually verify order (REJECT) -> Notify: cancelled after review | **CANCELLED_AFTER_REVIEW** | _from results table_ | _PASS/FAIL_ |
| 08 | Inventory service failure + recovery (extra) | `08-inventory-outage-recovery` (manual start) | customerCategory=PREMIUM<br>productId=SKU-HEADPHONES<br>quantity=1<br>deliveryPreference=STANDARD<br>paymentToken=tok_test_visa_success | Validate (INCIDENT) -> [inventory UP + retry] -> In stock -> Payment AUTHORISED -> Fulfil | **FULFILLED** | _from results table_ | _PASS/FAIL_ |

## How the "Actual" columns are produced

1. Start the scenarios: `../scripts/run-scenarios.sh` (failure/recovery scenarios: follow the steps in [../README.md](../README.md)).
2. Complete the human tasks in Tasklist as described in the README.
3. Call `GET http://localhost:8092/api/scenarios/results.md`. The lab app queries the Camunda REST API for each started instance (state, active incidents, active element, process variables) and prints a Markdown table with **Expected outcome**, **Actual outcome** (`finalOutcome` of the completed instance) and **Status** (PASS / FAIL / INCIDENT / IN PROGRESS).
4. Paste that table below and add the Operate / Tasklist screenshot file names as evidence.

## Execution results

<!-- Paste the output of GET http://localhost:8092/api/scenarios/results.md here after running the scenarios. -->

_Not executed yet – paste the live results table here._

## Unit tests of the worker logic

`mvn -pl lab-common,lab2-order-processing verify` runs the JUnit tests in [../src/test/java](../src/test/java). DMN sample evaluations: [dmn-rules.md](dmn-rules.md).
