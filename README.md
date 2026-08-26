cat > README.md <<'EOF'
# Workflow Automation Engineering

## Overview

This repository contains BPMN and DMN process models created using **Camunda Modeler**. The workflows demonstrate BPMN process modeling, Exclusive Gateways, Business Rule Tasks, DMN decision tables, User Tasks, Service Tasks, and decision-based process routing.

## Tools Used

- **Camunda Modeler**
- **BPMN 2.0**
- **DMN**

---

## Employee Leave Approval

### Scenario Description

This process models how an employee's leave request is handled through the company's HR system. The system first checks whether the employee has sufficient leave balance. Depending on the available balance and the manager's approval decision, the request follows an appropriate path and the employee receives the corresponding notification.

### BPMN Process

The process starts when the employee submits a leave request. The HR system checks the employee's leave balance using an Exclusive Gateway.

If sufficient leave balance is available, the request is sent to the manager for approval. A second Exclusive Gateway determines whether the manager approves or rejects the request. An approved request results in the employee's leave balance being updated and an approval notification being sent. If the manager rejects the request, a rejection notification is sent.

If there is insufficient leave balance, the employee receives an insufficient-balance notification. Each possible outcome leads to the end of the process after the appropriate notification is sent.

### BPMN Model

[View Employee Leave Approval BPMN](employee-leave-approval.bpmn)

---

## Online Purchase Order Processing

### Scenario Description

This process models the handling of an online customer purchase order. The system checks whether the requested product is available and then processes the payment if the product is available. The order follows different paths depending on product availability and payment success.

### BPMN Process

The process starts when a customer places an order. The system checks product availability, followed by an Exclusive Gateway that determines whether the product is available.

If the product is unavailable, the customer is notified that the product is out of stock and the process ends. If the product is available, the system processes the payment. A second Exclusive Gateway determines whether the payment is successful.

For successful payment, the system confirms the order, prepares the product for shipment, ships the order, and sends a shipping confirmation to the customer before the process ends. If the payment fails, the customer receives a payment-failure notification and the process ends.

### BPMN Model

[View Online Purchase Order Processing BPMN](online-purchase-order-processing.bpmn)

---

## IT Service Request

### Scenario Description

This process models the handling of an employee IT support request. The help desk registers the request, evaluates its severity, and assigns it to an appropriate technician. The technician then investigates the problem and either resolves it internally or escalates it to an external service provider.

### BPMN Process

The process starts when an employee reports an IT problem. The help desk registers the request and checks its severity using an Exclusive Gateway.

For a low-severity problem, the request is assigned to a support technician. For a high-severity problem, it is assigned to a senior technician. Both paths continue to the investigation of the problem.

A second Exclusive Gateway determines whether the problem can be resolved internally. If it can be resolved, the technician fixes the problem. If it cannot be resolved internally, the problem is escalated to the external service provider.

After either path, the help desk updates the request status and the employee receives a resolution notification. The process then ends.

### BPMN Model

[View IT Service Request BPMN](it-service-request.bpmn)

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

---

## Repository Structure

```text
workflow-automation-engineering/

├── README.md
│
├── employee-leave-approval.bpmn
├── online-purchase-order-processing.bpmn
├── it-service-request.bpmn
│
├── loan-risk-routing/
│   ├── loan-risk-routing.bpmn
│   └── evaluate-loan-risk.dmn
│
└── multi-item-order-discount/
    ├── multi-item-order-discount.bpmn
    └── calculate-discounts.dmn
