# Lab 2 – Process-variable dictionary and gateway routing

Process `order-processing-process`. All values are synthetic.

## Input (start form / REST start)

| Variable | Type | Example | Purpose |
|---|---|---|---|
| `orderId` | String | `ORD-2001` | order reference, also the payment idempotency key |
| `customerRef` | String | `CUST-2001` | synthetic customer reference |
| `customerCategory` | String | `REGULAR` | `NEW` / `REGULAR` / `PREMIUM` – DMN input and exception rule |
| `productId` | String | `SKU-HEADPHONES` | product in the simulated catalogue |
| `quantity` | Number | `2` | 1–20 |
| `deliveryPreference` | String | `STANDARD` | `STANDARD` / `EXPRESS` / `SAME_DAY` – DMN input |
| `paymentToken` | String | `tok_test_visa_success` | mock payment token (card numbers are rejected) |
| `scenarioId`, `mockApiBaseUrl` | String | | added by the scenario runner / optional connector base URL |

## Set by *Validate order, check stock and price* (worker `order.validate-and-price`)

| Variable | Type | Example | Purpose |
|---|---|---|---|
| `orderValid` | Boolean | `true` | routes *Order valid?* |
| `validationErrors` | List&lt;String&gt; | `["quantity must be between 1 and 20"]` | reasons for rejection |
| `productName` | String | `Wireless Headphones` | display |
| `unitPrice` | Number | `2499` | catalogue price (INR) |
| `availableQuantity` | Number | `25` | stock at validation time |
| `stockStatus` | String | `IN_STOCK` / `INSUFFICIENT_STOCK` / `NOT_CHECKED` | routes *Stock available?* |
| `subtotal` | Number | `4998.00` | unit price × quantity |
| `taxAmount` | Number | `899.64` | 18 % GST |
| `deliveryFee` | Number | `0.00` | STANDARD free from 999 (else 49), EXPRESS 149, SAME_DAY 299 |
| `orderTotal` | Number | `5897.64` | DMN input, verification rule, payment amount |

## Set by *Determine order priority* (DMN `order-priority`, result variable `priorityDecision`)

| Variable | Type | Example | Purpose |
|---|---|---|---|
| `priorityDecision` | Context | `{priority: "LOW", fulfilmentSlaHours: 72, reason: "Standard order"}` | priority and dispatch SLA used by fulfilment |

## Set by *Manually verify order* (form `order-manual-verification`)

| Variable | Type | Example | Purpose |
|---|---|---|---|
| `verificationDecision` | String | `APPROVE` / `REJECT` | routes *Verification approved?* |
| `verifiedBy` | String | `OPS-11` | who verified |
| `verificationNotes` | String | `Customer confirmed by phone` | audit note |

## Set by *Process payment* (REST Connector result expression)

| Variable | Type | Example | Purpose |
|---|---|---|---|
| `paymentStatus` | String | `AUTHORISED` / `DECLINED` | routes *Payment authorised?* |
| `paymentChargeId` | String | `CHG-70001` | gateway reference |
| `paymentDeclineReason` | String | `CARD_DECLINED` | reason when declined |
| `paymentProcessedAt` | String (timestamp) | `2026-11-02T10:20:11Z` | gateway time |

## Set by *Fulfil order* (worker `order.fulfil`)

| Variable | Type | Example | Purpose |
|---|---|---|---|
| `fulfilmentStatus` | String | `DISPATCH_SCHEDULED` | fulfilment state |
| `shipmentId` | String | `SHP-2001` | shipment reference |
| `warehouse` | String | `WH-CHENNAI` | dispatching warehouse |
| `fulfilmentPriority` | String | `CRITICAL` | copied from the DMN |
| `dispatchBy` | String (timestamp) | `2026-11-02T16:00Z` | now + `fulfilmentSlaHours` |
| `remainingStock` | Number | `23` | stock after reservation |

## Set by the notification tasks (worker `order.notify-customer`)

| Variable | Type | Example | Purpose |
|---|---|---|---|
| `notificationType` | String (task-local input) | `ORDER_CONFIRMED` | set by each task's input mapping – not propagated |
| `finalOutcome` | String | `FULFILLED`, `OUT_OF_STOCK`, `PAYMENT_DECLINED`, `CANCELLED_AFTER_REVIEW`, `REJECTED_INVALID` | final business result |
| `customerMessage` | String | `Order ORD-2001 confirmed…` | simulated notification text |
| `notificationStatus` | String | `SIMULATED_EMAIL_SENT` | |

## Gateway routing

| Gateway | Flow | Condition | Why |
|---|---|---|---|
| Order valid? | Valid | `=orderValid = true` | default → *Notify: order rejected* |
| Stock available? | In stock | `=stockStatus = "IN_STOCK"` | default → *Notify: out of stock*; the order never reaches payment |
| High-value or exception order? | Yes | `=orderTotal >= 100000 or (customerCategory = "NEW" and orderTotal >= 25000)` | defined verification condition; default *No* goes straight to payment |
| Verification approved? | Approved | `=verificationDecision = "APPROVE"` | default → *Notify: cancelled after review* |
| Payment authorised? | Authorised | `=paymentStatus = "AUTHORISED"` | default → *Notify: payment declined* |

Every exclusive gateway has a default flow, so an unexpected value never blocks a token. Verification happens **before** payment so that a rejected order is never charged.
