# Lab 3 – Process-variable dictionary and routing

Process `hospital-appointment-process`. All patient data is synthetic.

## Input (start form / REST start)

| Variable | Type | Example | Purpose |
|---|---|---|---|
| `patientRef` | String | `SYN-PAT-3001` | synthetic patient reference (real names / record numbers rejected) |
| `ageYears` | Number | `34` | DMN input |
| `department` | String | `GENERAL_MEDICINE` | GENERAL_MEDICINE, CARDIOLOGY, ORTHOPAEDICS, PAEDIATRICS, DERMATOLOGY |
| `preferredDate` | String (date) | `2026-11-05` | appointment preference |
| `preferredSession` | String | `MORNING` | ANY / MORNING / AFTERNOON |
| `symptomsSummary` | String | `Mild cough for five days` | free text, 3–500 characters |
| `chestPain` | Boolean | `false` | urgency indicator – DMN input |
| `breathingDifficulty` | Boolean | `false` | urgency indicator – DMN input |
| `painScore` | Number | `2` | 0–10 – DMN input |
| `highFever` | Boolean | `false` | urgency indicator – DMN input |
| `symptomDurationDays` | Number | `5` | DMN input |
| `scenarioId`, `mockApiBaseUrl` | String | | scenario link / optional connector base URL |

## Set by *Validate registration* (worker `appointment.validate-registration`)

| Variable | Type | Example | Purpose |
|---|---|---|---|
| `registrationValid` | Boolean | `true` | routes *Registration valid?* |
| `registrationErrors` | List&lt;String&gt; | `["preferredDate cannot be in the past"]` | rejection reasons |
| `appointmentDate` | String (date) | `2026-11-05` | normalised date (tomorrow if none given) – used in the slot query |
| `preferredSession` | String | `ANY` | normalised session |
| `chestPain`, `breathingDifficulty`, `highFever` | Boolean | `false` | normalised to real booleans for the DMN |
| `symptomDurationDays` | Number | `5` | defaulted to 0 if missing |

## Set by *Apply triage rules* (DMN `triage-category`, result variable `triage`)

| Variable | Type | Example | Purpose |
|---|---|---|---|
| `triage` | Context | `{category: "ROUTINE", reason: "No urgency indicators", targetResponse: "Next available appointment"}` | `triage.category` routes *Triage category?* |

## Set by the human tasks (Tasklist forms)

| Variable | Type | Set by | Purpose |
|---|---|---|---|
| `assessmentOutcome`, `assessedBy`, `assessmentNotes` | String | *Immediate clinical assessment* | record of the (simulated) urgent assessment |
| `priorityReviewOutcome` | String | *Priority staff review* | `PROCEED_TO_BOOKING` / `REFER_TO_EMERGENCY` – routes *Review outcome?* |
| `reviewedBy`, `reviewNotes` | String | *Priority staff review* | audit |
| `schedulingOutcome` | String | *Staff review: no suitable slot* | `WAITLISTED` / `MANUAL_SLOT_ASSIGNED` / `REFERRED_ELSEWHERE` – becomes `finalOutcome` |
| `manualSlotDetails`, `handledBy` | String | *Staff review: no suitable slot* | details / audit |

## Set by *Retrieve available slots* (REST Connector result expression)

| Variable | Type | Example | Purpose |
|---|---|---|---|
| `availableSlots` | List&lt;Context&gt; | `[{slotId: "GEN-20261105-0900", date, startTime, endTime, doctorCode, session}]` | slots returned by the scheduling API |
| `availableSlotCount` | Number | `6` | routes *Suitable slot available?* |
| `slotLookupStatus` | String | `COMPLETED` | |

## Set by *Book earliest slot* (REST Connector result expression / `SLOT_TAKEN` boundary event)

| Variable | Type | Example | Purpose |
|---|---|---|---|
| `bookingStatus` | String | `CONFIRMED` / `SLOT_TAKEN` | `SLOT_TAKEN` is set by the boundary event's output mapping |
| `bookingResponse` | Context | `{status: 201, headers: {…}, body: {…}}` | full HTTP response (connector *Result variable*) |
| `bookingId` | String | `BKG-90001` | scheduling-system reference |
| `appointmentSlotId` | String | `GEN-20261105-0900` | booked slot |
| `appointmentTime` | String | `09:00-09:30` | |
| `doctorCode` | String | `DR-GEN-02` | synthetic clinician code |
| `bookingProblem` | String | `HTTP 409 - slot already booked` | reason when the slot was lost |

## Set by the notification tasks (worker `appointment.notify-patient`)

| Variable | Type | Example | Purpose |
|---|---|---|---|
| `notificationType` | String (task-local input) | `BOOKING_CONFIRMED` | chosen by each task's input mapping |
| `finalOutcome` | String | `BOOKED`, `URGENT_ASSESSMENT`, `REFERRED_TO_EMERGENCY`, `WAITLISTED`, `MANUAL_SLOT_ASSIGNED`, `REFERRED_ELSEWHERE`, `REJECTED_INVALID` | final result |
| `patientMessage` | String | `Appointment BKG-90001 confirmed…` | simulated SMS text |
| `notificationStatus` | String | `SIMULATED_SMS_SENT` | |

## Gateway routing

| Gateway | Flow | Condition |
|---|---|---|
| Registration valid? | Valid | `=registrationValid = true` (default → *Notify: registration rejected*) |
| Triage category? | Urgent assessment | `=triage.category = "URGENT_ASSESSMENT"` |
| | Priority review | `=triage.category = "PRIORITY_REVIEW"` |
| | Routine | default |
| Review outcome? | Refer to emergency | `=priorityReviewOutcome = "REFER_TO_EMERGENCY"` (default → proceed to booking) |
| Suitable slot available? | Yes | `=availableSlotCount > 0` (default → staff review) |
| Error boundary on *Book earliest slot* | Slot taken (HTTP 409) | BPMN error code `SLOT_TAKEN` → staff review |

Urgent cases never wait for the scheduling API – they go straight to a human, which is the safe design for the simulated scenario.
