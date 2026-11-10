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

Executed live on **10 Nov 2026** on a MacBook (macOS) against **Camunda 8 Run 8.8.33** (Zeebe + Operate + Tasklist + Connectors on `localhost:8080`), models opened and deployed from **Camunda Desktop Modeler 5.49.0**. Table produced by `GET http://localhost:8092/api/scenarios/results.md`; the user tasks were completed in Tasklist and incidents were resolved in Operate as described per scenario. Screenshots: [screenshots/](screenshots/).

| Scenario | Process instance | Description | Expected outcome | Actual outcome | Status | Key variables |
|---|---|---|---|---|---|---|
| 01-in-stock-payment-success | 2251799813688372 | Stock available and payment succeeds (2 x headphones, standard delivery) | FULFILLED | FULFILLED | PASS | orderId=ORD-2001<br>orderValid=true<br>stockStatus=IN_STOCK<br>orderTotal=5897.64<br>priorityDecision={"priority":"LOW","fulfilmentSlaHours":72,"reason":"Standard order"}<br>paymentStatus=AUTHORISED<br>paymentDeclineReason=null<br>shipmentId=SHP-2001 |
| 02-insufficient-stock | 2251799813688387 | 5 graphics cards requested but only 2 in stock | OUT_OF_STOCK | OUT_OF_STOCK | PASS | orderId=ORD-2002<br>orderValid=true<br>stockStatus=INSUFFICIENT_STOCK<br>orderTotal=383494.1 |
| 03-payment-declined | 2251799813688401 | Mock gateway declines the card token (premium customer, express delivery) | PAYMENT_DECLINED | PAYMENT_DECLINED | PASS | orderId=ORD-2003<br>orderValid=true<br>stockStatus=IN_STOCK<br>orderTotal=4867.82<br>priorityDecision={"priority":"HIGH","fulfilmentSlaHours":12,"reason":"Premium customer with express delivery"}<br>paymentStatus=DECLINED<br>paymentDeclineReason=CARD_DECLINED |
| 04-high-value-manual-review | 2251799813688453 | 2 x Laptop Pro = INR 2,12,397.64 (>= 1,00,000) -> manual verification; approve it in Tasklist | FULFILLED | FULFILLED | PASS | orderId=ORD-2004<br>orderValid=true<br>stockStatus=IN_STOCK<br>orderTotal=212397.64<br>priorityDecision={"priority":"HIGH","fulfilmentSlaHours":24,"reason":"High-value order (>= 50,000)"}<br>verificationDecision=APPROVE<br>paymentStatus=AUTHORISED<br>paymentDeclineReason=null<br>shipmentId=SHP-2004 |
| 06-invalid-order | 2251799813688549 | Quantity 0 and a raw card number instead of a token: rejected by validation | REJECTED_INVALID | REJECTED_INVALID | PASS | orderId=ORD-2006<br>orderValid=false<br>stockStatus=NOT_CHECKED |
| 07-new-customer-review-rejected | 2251799813688597 | NEW customer ordering INR 58,998.82 (>= 25,000) -> manual verification; reject it in Tasklist | CANCELLED_AFTER_REVIEW | CANCELLED_AFTER_REVIEW | PASS | orderId=ORD-2007<br>orderValid=true<br>stockStatus=IN_STOCK<br>orderTotal=58998.82<br>priorityDecision={"priority":"HIGH","fulfilmentSlaHours":24,"reason":"High-value order (>= 50,000)"}<br>verificationDecision=REJECT |
| 05-payment-service-outage-recovery | 2251799813688945 | FIRST set payment mock DOWN (PUT http://localhost:8090/admin/faults/payment?mode=DOWN), then start. Connector retries 3x (10 s back-off) and... | FULFILLED | FULFILLED | PASS | orderId=ORD-2005<br>orderValid=true<br>stockStatus=IN_STOCK<br>orderTotal=22717.82<br>priorityDecision={"priority":"CRITICAL","fulfilmentSlaHours":6,"reason":"Same-day delivery requested"}<br>paymentStatus=AUTHORISED<br>paymentDeclineReason=null<br>shipmentId=SHP-2005 |
| 08-inventory-outage-recovery | 2251799813689190 | Alternative failure: FIRST set the inventory dependency of the worker DOWN (PUT http://localhost:8092/admin/worker-faults/inventory?mode=DOW... | FULFILLED | FULFILLED | PASS | orderId=ORD-2008<br>orderValid=true<br>stockStatus=IN_STOCK<br>orderTotal=2948.82<br>priorityDecision={"priority":"MEDIUM","fulfilmentSlaHours":48,"reason":"Premium customer, standard delivery"}<br>paymentStatus=AUTHORISED<br>paymentDeclineReason=null<br>shipmentId=SHP-2008 |

## Unit tests of the worker logic

`mvn -pl lab-common,lab2-order-processing verify` runs the JUnit tests in [../src/test/java](../src/test/java). DMN sample evaluations: [dmn-rules.md](dmn-rules.md).
