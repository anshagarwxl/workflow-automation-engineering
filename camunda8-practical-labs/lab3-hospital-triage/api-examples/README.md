# Lab 3 – Mock scheduling API and REST Connector

## Mock hospital scheduling API (mock-services, port 8090)

| Method | Path | Purpose |
|---|---|---|
| GET | `/scheduling/api/v1/slots?department=&date=yyyy-MM-dd&session=ANY\|MORNING\|AFTERNOON` | free 30-minute slots (morning 09:00–11:30, afternoon 14:00–15:30). DERMATOLOGY is always fully booked. |
| POST | `/scheduling/api/v1/bookings` | books a slot → 201 CONFIRMED, or 409 SLOT_NOT_AVAILABLE |
| PUT | `/admin/faults/scheduling?mode=UP\|DOWN\|SLOW` | fault injection (DOWN = HTTP 503, SLOW = 30 s delay) |

Departments: GENERAL_MEDICINE, CARDIOLOGY, ORTHOPAEDICS, PAEDIATRICS, DERMATOLOGY. All data is synthetic and kept in memory.

## Slot-lookup REST Connector

BPMN task `Task_RetrieveSlots` ("Retrieve available slots") – configuration as stored in [`hospital-appointment.bpmn`](../models/hospital-appointment.bpmn):

| Property | Value |
|---|---|
| Task definition type | `io.camunda:http-json:1` |
| Retries | `3` |
| Input `authentication.type` | `noAuth` |
| Input `method` | `GET` |
| Input `url` | `=(if is defined(mockApiBaseUrl) then mockApiBaseUrl else "http://localhost:8090") + "/scheduling/api/v1/slots"` |
| Input `queryParameters` | `={department: department, date: appointmentDate, session: preferredSession}` |
| Input `storeResponse` | `false` |
| Input `connectionTimeoutInSeconds` | `5` |
| Input `readTimeoutInSeconds` | `10` |
| Input `ignoreNullValues` | `false` |
| Header `elementTemplateVersion` | `11` |
| Header `elementTemplateId` | `io.camunda.connectors.HttpJson.v2` |
| Header `resultExpression` | `={availableSlots: response.body.slots, availableSlotCount: response.body.slotCount, slotLookupStatus: "COMPLETED"}` |
| Header `retryBackoff` | `PT10S` |

* Query parameters are built from process variables (`department`, `appointmentDate`, `preferredSession`).
* HTTP 5xx / timeouts are retried 3 times with a 10 s back-off, then an incident is raised (scenario 05).
* The booking is a second REST Connector, described below.

## Booking REST Connector

BPMN task `Task_RequestBooking` ("Book earliest slot") – a second REST Connector (POST). No Java worker calls the scheduling API.

| Property | Value |
|---|---|
| Task definition type | `io.camunda:http-json:1` |
| Retries | `3` |
| Input `authentication.type` | `noAuth` |
| Input `method` | `POST` |
| Input `url` | `=(if is defined(mockApiBaseUrl) then mockApiBaseUrl else "http://localhost:8090") + "/scheduling/api/v1/bookings"` |
| Input `storeResponse` | `false` |
| Input `connectionTimeoutInSeconds` | `5` |
| Input `readTimeoutInSeconds` | `10` |
| Input `body` | `={slotId: availableSlots[1].slotId, patientRef: patientRef, urgencyCategory: triage.category, requestedBy: "camunda-rest-connector"}` |
| Input `ignoreNullValues` | `false` |
| Header `elementTemplateVersion` | `11` |
| Header `elementTemplateId` | `io.camunda.connectors.HttpJson.v2` |
| Header `resultVariable` | `bookingResponse` |
| Header `resultExpression` | `={bookingStatus: response.body.status, bookingId: response.body.bookingId, appointmentSlotId: response.body.slotId, appointmentDate: response.body.date, appointmentTime: response.body.startTime + "-" + response.body.endTime, doctorCode: response.body.doctorCode}` |
| Header `errorExpression` | `=if error.code = "409" then bpmnError("SLOT_TAKEN", "Selected slot is no longer available", {bookingProblem: "HTTP 409 - slot already booked"}) else null` |
| Header `retryBackoff` | `PT10S` |

* The body takes the first (earliest) slot returned by the lookup: `availableSlots[1].slotId` (FEEL lists are 1-based).
* **Result variable** `bookingResponse` keeps the full response; the **result expression** copies `bookingId`, date, time and doctor into process variables (connector result expressions only see `response`, so the mock returns the slot details in the booking response).
* **Error expression**: HTTP 409 → BPMN error `SLOT_TAKEN`, caught by the boundary event *Slot taken (HTTP 409)* → staff review. Any other failure returns `null` → normal retries (3 × `PT10S`) → incident.

## Request / response files

| File | Content |
|---|---|
| [slots.request.txt](slots.request.txt) / [slots.response.json](slots.response.json) | slot lookup and HTTP 200 response |
| [slots.no-slots.response.json](slots.no-slots.response.json) | empty result (dermatology) |
| [slots.outage-503.response.json](slots.outage-503.response.json) | HTTP 503 while the fault is DOWN |
| [slots.result-variables.json](slots.result-variables.json) | variables written by the result expression |
| [booking.request.json](booking.request.json) / [booking.response.json](booking.response.json) / [booking.conflict-409.response.json](booking.conflict-409.response.json) | booking call made by the worker |
| [start-instance.request.json](start-instance.request.json) / [camunda-rest-create-instance.request.json](camunda-rest-create-instance.request.json) | starting a request |
| [requests.http](requests.http) | runnable requests |
