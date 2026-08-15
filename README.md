# Workflow Automation Engineering

## Overview

This repository contains the BPMN process models created for Workflow Automation Engineering. The processes are modeled using **Camunda Modeler** and demonstrate the use of basic BPMN building blocks such as Start Events, Tasks, Exclusive Gateways, alternative process paths, and End Events.

The assignment contains three business process scenarios:

1. Employee Leave Approval
2. Online Purchase Order Processing
3. IT Service Request

## Tools Used

- **Camunda Modeler**
- **BPMN 2.0**

---

## Scenario 1: Employee Leave Approval

### Scenario Description

This process models how an employee's leave request is handled through the company's HR system. The system first checks whether the employee has sufficient leave balance. Depending on the available balance and the manager's approval decision, the request follows an appropriate path and the employee receives the corresponding notification.

### BPMN Process

The process starts when the employee submits a leave request. The HR system checks the employee's leave balance using an Exclusive Gateway.

If sufficient leave balance is available, the request is sent to the manager for approval. A second Exclusive Gateway determines whether the manager approves or rejects the request. An approved request results in the employee's leave balance being updated and an approval notification being sent. If the manager rejects the request, a rejection notification is sent.

If there is insufficient leave balance, the employee receives an insufficient-balance notification. Each possible outcome leads to the end of the process after the appropriate notification is sent.

### BPMN Model

[View Employee Leave Approval BPMN](scenario-1/employee-leave-approval.bpmn)

---

## Scenario 2: Online Purchase Order Processing

### Scenario Description

This process models the handling of an online customer purchase order. The system checks whether the requested product is available and then processes the payment if the product is available. The order follows different paths depending on product availability and payment success.

### BPMN Process

The process starts when a customer places an order. The system checks product availability, followed by an Exclusive Gateway that determines whether the product is available.

If the product is unavailable, the customer is notified that the product is out of stock and the process ends. If the product is available, the system processes the payment. A second Exclusive Gateway determines whether the payment is successful.

For successful payment, the system confirms the order, prepares the product for shipment, ships the order, and sends a shipping confirmation to the customer before the process ends. If the payment fails, the customer receives a payment-failure notification and the process ends.

### BPMN Model

[View Online Purchase Order Processing BPMN](scenario-2/online-purchase-order-processing.bpmn)

---

## Scenario 3: IT Service Request

### Scenario Description

This process models the handling of an employee IT support request. The help desk registers the request, evaluates its severity, and assigns it to an appropriate technician. The technician then investigates the problem and either resolves it internally or escalates it to an external service provider.

### BPMN Process

The process starts when an employee reports an IT problem. The help desk registers the request and checks its severity using an Exclusive Gateway.

For a low-severity problem, the request is assigned to a support technician. For a high-severity problem, it is assigned to a senior technician. Both paths continue to the investigation of the problem.

A second Exclusive Gateway determines whether the problem can be resolved internally. If it can be resolved, the technician fixes the problem. If it cannot be resolved internally, the problem is escalated to the external service provider.

After either path, the help desk updates the request status and the employee receives a resolution notification. The process then ends.

### BPMN Model

[View IT Service Request BPMN](scenario-3/it-service-request.bpmn)

---

## Repository Structure

```text
workflow-automation-engineering/
│
├── README.md
│
├── scenario-1/
│   └── employee-leave-approval.bpmn
│
├── scenario-2/
│   └── online-purchase-order-processing.bpmn
│
└── scenario-3/
    └── it-service-request.bpmn
