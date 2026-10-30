# Lab 2 – REST integration and API examples

## Payment REST Connector

BPMN task `Task_ProcessPayment` ("Process payment") uses the Camunda **REST Connector** (`io.camunda.connectors.HttpJson.v2`, version 11). Configuration as stored in [`order-processing.bpmn`](../models/order-processing.bpmn):

| Property | Value |
|---|---|
| Task definition type | `io.camunda:http-json:1` |
| Retries | `3` |
| Input `authentication.type` | `noAuth` |
| Input `method` | `POST` |
| Input `url` | `=(if is defined(mockApiBaseUrl) then mockApiBaseUrl else "http://localhost:8090") + "/payments/api/v1/charges"` |
| Input `headers` | `={"Idempotency-Key": orderId}` |
| Input `storeResponse` | `false` |
| Input `connectionTimeoutInSeconds` | `5` |
| Input `readTimeoutInSeconds` | `10` |
| Input `body` | `={orderId: orderId, amount: orderTotal, currency: "INR", paymentToken: paymentToken, customerRef: customerRef}` |
| Input `ignoreNullValues` | `false` |
| Header `elementTemplateVersion` | `11` |
| Header `elementTemplateId` | `io.camunda.connectors.HttpJson.v2` |
| Header `resultExpression` | `={paymentStatus: response.body.status, paymentChargeId: response.body.chargeId, paymentDeclineReason: response.body.declineReason, paymentProcessedAt: response.body.processedAt}` |
| Header `retryBackoff` | `PT10S` |

* No real card data: the body carries a **mock token** (`tok_test_...`). The validation worker rejects anything that looks like a card number before payment is ever attempted.
* `Idempotency-Key = orderId`: the mock gateway returns the original charge if the same order is retried, so a retry after an outage cannot charge twice.
* A **decline is a business outcome** (HTTP 200, `status = DECLINED`) and is routed by the "Payment authorised?" gateway.
* A **technical failure** (HTTP 503, timeout) is retried 3 times with a 10 s back-off (`retryBackoff = PT10S`); then Zeebe raises an incident that is resolved in Operate.

## Request / response

| File | Content |
|---|---|
| [payment-charge.request.json](payment-charge.request.json) | body sent by the connector |
| [payment-charge.authorised.response.json](payment-charge.authorised.response.json) | HTTP 200, authorised |
| [payment-charge.declined.response.json](payment-charge.declined.response.json) | HTTP 200, declined (`tok_test_card_declined`) |
| [payment-charge.outage-503.response.json](payment-charge.outage-503.response.json) | HTTP 503 while the payment fault is DOWN |
| [payment-charge.result-variables.json](payment-charge.result-variables.json) | variables written by the result expression |
| [start-instance.request.json](start-instance.request.json) / [camunda-rest-create-instance.request.json](camunda-rest-create-instance.request.json) | starting an order via the lab app / Camunda REST API |
| [requests.http](requests.http) | runnable requests |

Mock tokens: `tok_test_visa_success`, `tok_test_mastercard_success` → AUTHORISED; `tok_test_card_declined` → CARD_DECLINED; `tok_test_insufficient_funds` → INSUFFICIENT_FUNDS; anything else → INVALID_TOKEN; amount > 5,00,000 → AMOUNT_LIMIT_EXCEEDED.
