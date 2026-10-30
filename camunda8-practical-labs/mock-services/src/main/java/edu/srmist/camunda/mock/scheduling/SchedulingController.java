package edu.srmist.camunda.mock.scheduling;

import edu.srmist.camunda.mock.faults.FaultRegistry;
import edu.srmist.camunda.mock.scheduling.SchedulingCalendar.Booking;
import edu.srmist.camunda.mock.scheduling.SchedulingCalendar.BookingRequest;
import edu.srmist.camunda.mock.scheduling.SchedulingCalendar.Slot;
import edu.srmist.camunda.mock.scheduling.SchedulingCalendar.SlotNotAvailableException;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** Lab 3 – simulated hospital scheduling API (synthetic data only). */
@RestController
@RequestMapping("/scheduling/api/v1")
public class SchedulingController {

  private static final Logger LOG = LoggerFactory.getLogger(SchedulingController.class);

  private final SchedulingCalendar calendar = new SchedulingCalendar();
  private final FaultRegistry faults;

  public SchedulingController(FaultRegistry faults) {
    this.faults = faults;
  }

  @GetMapping("/slots")
  public Map<String, Object> slots(
      @RequestParam("department") String department,
      @RequestParam("date") String date,
      @RequestParam(value = "session", required = false, defaultValue = "ANY") String session) {
    LOG.info("Slot lookup: department={}, date={}, session={}", department, date, session);
    faults.apply(FaultRegistry.SCHEDULING);
    List<Slot> slots = calendar.availableSlots(department, LocalDate.parse(date), session);
    Map<String, Object> body = new LinkedHashMap<>();
    body.put("department", department);
    body.put("date", date);
    body.put("session", session);
    body.put("slotCount", slots.size());
    body.put("slots", slots);
    return body;
  }

  @PostMapping("/bookings")
  public ResponseEntity<Booking> book(@RequestBody BookingRequest request) {
    LOG.info("Booking requested: {}", request);
    faults.apply(FaultRegistry.SCHEDULING);
    return ResponseEntity.status(HttpStatus.CREATED).body(calendar.book(request));
  }

  @ExceptionHandler(SlotNotAvailableException.class)
  public ResponseEntity<Map<String, String>> conflict(SlotNotAvailableException e) {
    return ResponseEntity.status(HttpStatus.CONFLICT)
        .body(Map.of("error", "SLOT_NOT_AVAILABLE", "message", e.getMessage()));
  }

  @ExceptionHandler({IllegalArgumentException.class, DateTimeParseException.class})
  public ResponseEntity<Map<String, String>> invalid(RuntimeException e) {
    return ResponseEntity.badRequest().body(Map.of("error", "INVALID_REQUEST", "message", e.getMessage()));
  }
}
