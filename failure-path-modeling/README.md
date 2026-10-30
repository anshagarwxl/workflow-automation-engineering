# BPMN failure-path modelling (Exercise 3)

Collaboration models that cover the happy path **and** the realistic failure paths (invalid input, timeouts, escalations, retries with limits, cancellations), each with a companion *Failure Path Register* (PDF) explaining every element and why that BPMN type was chosen.

| Assignment | BPMN model | Failure path register |
|---|---|---|
| 1. Student Project Approval & Allocation System | [student-project-approval.bpmn](student-project-approval/student-project-approval.bpmn) | [failure-path-register.pdf](student-project-approval/failure-path-register.pdf) |
| 2. Logistics & Shipment Exception Management | [shipment-exception-management.bpmn](shipment-exception-management/shipment-exception-management.bpmn) | [failure-path-register.pdf](shipment-exception-management/failure-path-register.pdf) |

## Student Project Approval & Allocation System

One pool with five lanes (Student/Team, Project Coordinator, Review Committee incl. HoD, Faculty Guide, Project Management System). Starts when a team decides to submit a proposal; ends when a guide is allocated and the team is notified, or the proposal is closed as rejected, lapsed, withdrawn or invalid. Failure paths include late submissions, invalid forms with limited correction attempts, similarity matches, limited revision cycles, committee reminders and HoD escalation, guide unavailability (manual allocation / waiting list) and withdrawals.

## Logistics & Shipment Exception Management

One pool with seven lanes (Shipper, Control Tower/Dispatcher, Pickup Agent, Hub/Warehouse, Delivery Agent, Recipient, Claims Department). Starts when a shipper books a shipment; ends when it is delivered and closed, returned to sender, or closed as lost, cancelled or claim-settled. Failure paths include booking validation with limited corrections, missed pickup windows (timer boundary events), scanner outages, customs holds with a document-wait timer, parcel damage, and transit breakdowns/ETA breaches with limited reassignments.
