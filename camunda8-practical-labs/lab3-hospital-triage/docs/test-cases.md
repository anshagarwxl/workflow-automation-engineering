# Lab 3 – Hospital appointment and triage – Test cases

Each row is one file in [../test-data](../test-data) (the JSON holds the full input plus `_expectedRoute` / `_expectedOutcome`). The "Minimum test scenario" column maps the rows to the scenarios required by the assignment; rows marked *(extra)* are additional coverage.

## Planned test cases

| ID | Minimum test scenario | Scenario file | Key inputs | Expected route | Expected final outcome | Actual outcome | Status |
|---|---|---|---|---|---|---|---|
| 01 | Routine request, slot available | `01-routine-slot-available` | patientRef=SYN-PAT-3001<br>ageYears=34<br>department=GENERAL_MEDICINE<br>painScore=2<br>chestPain=false<br>highFever=false<br>symptomDurationDays=5 | Validate -> DMN ROUTINE -> Retrieve slots (10) -> Request booking CONFIRMED -> Confirmation | **BOOKED** | _from results table_ | _PASS/FAIL_ |
| 02 | Priority staff review | `02-priority-staff-review` | patientRef=SYN-PAT-3002<br>ageYears=52<br>department=ORTHOPAEDICS<br>painScore=6<br>chestPain=false<br>highFever=false<br>symptomDurationDays=3 | Validate -> DMN PRIORITY_REVIEW -> Priority staff review (PROCEED) -> Retrieve slots -> Booking CONFIRMED | **BOOKED** | _from results table_ | _PASS/FAIL_ |
| 03 | Urgent assessment → immediate human assessment | `03-urgent-assessment` | patientRef=SYN-PAT-3003<br>ageYears=61<br>department=CARDIOLOGY<br>painScore=4<br>chestPain=true<br>highFever=false<br>symptomDurationDays=0 | Validate -> DMN URGENT_ASSESSMENT -> Immediate clinical assessment -> Record urgent assessment | **URGENT_ASSESSMENT** | _from results table_ | _PASS/FAIL_ |
| 04 | No appointment slots available | `04-no-slots-available` | patientRef=SYN-PAT-3004<br>ageYears=27<br>department=DERMATOLOGY<br>painScore=1<br>chestPain=false<br>highFever=false<br>symptomDurationDays=10 | Validate -> DMN ROUTINE -> Retrieve slots (0) -> Staff review: no suitable slot (WAITLISTED) -> Notify | **WAITLISTED** | _from results table_ | _PASS/FAIL_ |
| 05 | Scheduling API failure + recovery | `05-scheduling-api-outage-recovery` (manual start) | patientRef=SYN-PAT-3005<br>ageYears=6<br>department=PAEDIATRICS<br>painScore=3<br>chestPain=false<br>highFever=false<br>symptomDurationDays=2 | Validate -> DMN ROUTINE -> Retrieve slots (INCIDENT) -> [service UP + retry] -> Booking CONFIRMED | **BOOKED** | _from results table_ | _PASS/FAIL_ |
| 06 | Invalid registration (extra) | `06-invalid-registration` | patientRef=John Smith<br>ageYears=34<br>department=GENERAL_MEDICINE<br>painScore=2<br>chestPain=false<br>highFever=false<br>symptomDurationDays=5 | Validate (registrationValid=false) -> Notify: registration rejected | **REJECTED_INVALID** | _from results table_ | _PASS/FAIL_ |

## How the "Actual" columns are produced

1. Start the scenarios: `../scripts/run-scenarios.sh` (failure/recovery scenarios: follow the steps in [../README.md](../README.md)).
2. Complete the human tasks in Tasklist as described in the README.
3. Call `GET http://localhost:8093/api/scenarios/results.md`. The lab app queries the Camunda REST API for each started instance (state, active incidents, active element, process variables) and prints a Markdown table with **Expected outcome**, **Actual outcome** (`finalOutcome` of the completed instance) and **Status** (PASS / FAIL / INCIDENT / IN PROGRESS).
4. Paste that table below and add the Operate / Tasklist screenshot file names as evidence.

## Execution results

Executed live on **10 Nov 2026** on a MacBook (macOS) against **Camunda 8 Run 8.8.33** (Zeebe + Operate + Tasklist + Connectors on `localhost:8080`), models opened and deployed from **Camunda Desktop Modeler 5.49.0**. Table produced by `GET http://localhost:8093/api/scenarios/results.md`; the user tasks were completed in Tasklist and incidents were resolved in Operate as described per scenario. Screenshots: [screenshots/](screenshots/).

| Scenario | Process instance | Description | Expected outcome | Actual outcome | Status | Key variables |
|---|---|---|---|---|---|---|
| 01-routine-slot-available | 2251799813689476 | Routine request (pain 2, no red flags) with free morning slots in General Medicine | BOOKED | BOOKED | PASS | patientRef=SYN-PAT-3001<br>registrationValid=true<br>triage={"category":"ROUTINE","reason":"No urgency indicators","targetResponse":"Next available appointment"}<br>availableSlotCount=6<br>bookingStatus=CONFIRMED<br>bookingId=BKG-90001<br>appointmentTime=09:00-09:30 |
| 02-priority-staff-review | 2251799813689495 | Moderate pain (6/10) -> priority staff review; choose 'Proceed to booking' in Tasklist | BOOKED | BOOKED | PASS | patientRef=SYN-PAT-3002<br>registrationValid=true<br>triage={"category":"PRIORITY_REVIEW","reason":"Moderate pain (5-7)","targetResponse":"Within 4 hours"}<br>priorityReviewOutcome=PROCEED_TO_BOOKING<br>availableSlotCount=4<br>bookingStatus=CONFIRMED<br>bookingId=BKG-90002<br>appointmentTime=14:00-14:30 |
| 03-urgent-assessment | 2251799813689513 | Chest pain reported -> urgent assessment task for clinicians (complete it in Tasklist) | URGENT_ASSESSMENT | URGENT_ASSESSMENT | PASS | patientRef=SYN-PAT-3003<br>registrationValid=true<br>triage={"category":"URGENT_ASSESSMENT","reason":"Chest pain reported","targetResponse":"Immediate"} |
| 04-no-slots-available | 2251799813689559 | Routine dermatology request; the mock calendar has no free dermatology slots -> staff task; choose 'Added to waiting list' | WAITLISTED | WAITLISTED | PASS | patientRef=SYN-PAT-3004<br>registrationValid=true<br>triage={"category":"ROUTINE","reason":"No urgency indicators","targetResponse":"Next available appointment"}<br>availableSlotCount=0<br>schedulingOutcome=WAITLISTED |
| 06-invalid-registration | 2251799813689610 | Real-looking patient name instead of a synthetic reference and a date in the past -> registration rejected | REJECTED_INVALID | REJECTED_INVALID | PASS | patientRef=John Smith<br>registrationValid=false |
| 05-scheduling-api-outage-recovery | 2251799813690028 | FIRST set scheduling mock DOWN (PUT http://localhost:8090/admin/faults/scheduling?mode=DOWN), then start. Slot lookup retries 3x (10 s back-... | BOOKED | BOOKED | PASS | patientRef=SYN-PAT-3005<br>registrationValid=true<br>triage={"category":"ROUTINE","reason":"No urgency indicators","targetResponse":"Next available appointment"}<br>availableSlotCount=10<br>bookingStatus=CONFIRMED<br>bookingId=BKG-90003<br>appointmentTime=09:00-09:30 |

## Unit tests of the worker logic

`mvn -pl lab-common,lab3-hospital-triage verify` runs the JUnit tests in [../src/test/java](../src/test/java). DMN sample evaluations: [dmn-rules.md](dmn-rules.md).
