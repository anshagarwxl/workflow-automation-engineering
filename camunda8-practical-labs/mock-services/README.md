# mock-services

Spring Boot app (port **8090**) that simulates the external systems called by the three labs. All data is synthetic and in memory.

| Lab | Endpoint | Behaviour |
|---|---|---|
| 1 | `POST /credit-bureau/api/v1/assessments` | bureau records for `SYN-1001`…`SYN-1006`; unknown id → `NO_HISTORY` (score 600); `SYN-9408` → 30 s delay (timeout); `SYN-9500` → HTTP 500 |
| 2 | `POST /payments/api/v1/charges` | token-based: `tok_test_visa_success` / `tok_test_mastercard_success` → AUTHORISED, `tok_test_card_declined`, `tok_test_insufficient_funds` → DECLINED; idempotent per `orderId` |
| 3 | `GET /scheduling/api/v1/slots`, `POST /scheduling/api/v1/bookings` | 30-min slots per department/date; DERMATOLOGY fully booked; booking 201 / 409 |
| all | `GET /admin/faults`, `PUT /admin/faults/{credit\|payment\|scheduling}?mode=UP\|DOWN\|SLOW` | fault injection: DOWN → HTTP 503, SLOW → 30 s delay |

Run: `mvn -pl mock-services spring-boot:run`. Source: [src/main/java/edu/srmist/camunda/mock](src/main/java/edu/srmist/camunda/mock).
