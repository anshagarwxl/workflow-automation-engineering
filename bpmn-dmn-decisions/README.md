# BPMN with DMN decision tables

Processes that delegate a decision to a DMN table through a Business Rule Task.

| Process | BPMN | DMN | Hit policy |
|---|---|---|---|
| Loan risk routing | [loan-risk-routing.bpmn](loan-risk-routing/loan-risk-routing.bpmn) | [evaluate-loan-risk.dmn](loan-risk-routing/evaluate-loan-risk.dmn) | Unique |
| Multi-item order discount | [multi-item-order-discount.bpmn](multi-item-order-discount/multi-item-order-discount.bpmn) | [calculate-discounts.dmn](multi-item-order-discount/calculate-discounts.dmn) | Collect (Sum) |

---

## Loan Risk Routing

### Scenario Description

This process models an end-to-end loan approval workflow. A Business Rule Task evaluates the loan application using a DMN decision table and determines the applicant's risk tier and whether manual review is required.

### BPMN Process

The process starts when a customer submits a loan application containing applicant age, credit score, and loan amount.

The Business Rule Task evaluates the application using the `evaluate-loan-risk` DMN decision table. The resulting risk tier is then evaluated by an Exclusive Gateway.

Low-risk applications are automatically approved and disbursed. Medium-risk applications or applications requiring manual review are routed to an underwriter. High-risk applications that do not require manual review are automatically rejected and the customer is notified.

### Models

- [View Loan Risk Routing BPMN](loan-risk-routing/loan-risk-routing.bpmn)
- [View Loan Risk DMN](loan-risk-routing/evaluate-loan-risk.dmn)

---

## Multi-Item Order Discount

### Scenario Description

This process models discount calculation and fulfillment for customer orders. A Business Rule Task uses a DMN decision table with a Collect Sum hit policy to aggregate all applicable discounts.

### BPMN Process

The process receives the customer's tier, cart value, and promotional code. The Business Rule Task evaluates the applicable discount rules and stores the aggregated discount percentage.

A Script Task then calculates the final invoice amount using:

`finalAmount = cartValue * (1 - (totalDiscount / 100))`

An Exclusive Gateway determines whether manager sign-off is required. Orders with a final amount greater than or equal to 1000 are routed to the Manager Sign-off User Task. Orders below 1000 proceed directly to the warehouse.

Both paths continue to the Send Order to Warehouse Service Task, completing the fulfillment flow.

### Models

- [View Multi-Item Order Discount BPMN](multi-item-order-discount/multi-item-order-discount.bpmn)
- [View Discount Calculation DMN](multi-item-order-discount/calculate-discounts.dmn)

### Sample Calculation

For:

- Customer tier: `PREMIUM`
- Cart value: `600`
- Promo code: `FESTIVE10`

The applicable discounts are:

- Premium customer: `10%`
- Cart value >= 500: `5%`
- FESTIVE10 promo code: `10%`

Total discount:

`10 + 5 + 10 = 25%`

Final amount:

`600 * (1 - 25 / 100) = 450`

Since the final amount is below 1000, the order follows the direct warehouse path.
