# Workflow Automation Engineering

Coursework for **Workflow Automation Engineering** (B.Tech CSE, SRM Institute of Science and Technology) – Ansh Agarwal.

The repository grows from BPMN modelling basics, through DMN-based decisions and failure-path modelling, to fully executable Camunda 8 workflows with Java job workers, REST connectors, Tasklist and Operate.

| # | Folder | Topic | Tools |
|---|---|---|---|
| 1 | [basic-bpmn-processes](basic-bpmn-processes) | BPMN 2.0 process models with exclusive-gateway routing (leave approval, purchase order, IT service request) | Camunda Modeler |
| 2 | [bpmn-dmn-decisions](bpmn-dmn-decisions) | Business Rule Tasks with DMN decision tables (loan risk routing, multi-item order discount) | Camunda Modeler, DMN |
| 3 | [failure-path-modeling](failure-path-modeling) | Collaboration models with failure paths + failure path registers (student project approval, shipment exception management) | Camunda Modeler |
| 4 | [camunda8-practical-labs](camunda8-practical-labs) | **PBJ-2 practical – Camunda 8 end-to-end automation**: loan approval, e-commerce order processing, hospital appointment & triage | Camunda 8.8, Java 17, Spring Boot, REST Connector, Tasklist, Operate |

## Repository structure

```text
workflow-automation-engineering/
├── README.md
├── basic-bpmn-processes/
│   ├── README.md
│   ├── employee-leave-approval.bpmn
│   ├── online-purchase-order-processing.bpmn
│   └── it-service-request.bpmn
├── bpmn-dmn-decisions/
│   ├── README.md
│   ├── loan-risk-routing/            (loan-risk-routing.bpmn, evaluate-loan-risk.dmn)
│   └── multi-item-order-discount/    (multi-item-order-discount.bpmn, calculate-discounts.dmn)
├── failure-path-modeling/
│   ├── README.md
│   ├── student-project-approval/     (student-project-approval.bpmn, failure-path-register.pdf)
│   └── shipment-exception-management/(shipment-exception-management.bpmn, failure-path-register.pdf)
└── camunda8-practical-labs/          Maven multi-module project
    ├── README.md                     overview, quick start, submission checklist
    ├── docs/SETUP.md                 Camunda 8 Run + Modeler + run guide
    ├── lab-common/  mock-services/
    ├── lab1-loan-approval/
    ├── lab2-order-processing/
    └── lab3-hospital-triage/
```

Open any `.bpmn` / `.dmn` file in Camunda Desktop Modeler to view or edit it. For the Camunda 8 labs, start with [camunda8-practical-labs/README.md](camunda8-practical-labs/README.md).
