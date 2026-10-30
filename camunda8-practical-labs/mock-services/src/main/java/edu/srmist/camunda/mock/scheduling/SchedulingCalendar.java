package edu.srmist.camunda.mock.scheduling;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Synthetic hospital appointment calendar.
 *
 * <p>Every department has morning (09:00–11:30) and afternoon (14:00–15:30) 30-minute slots on
 * every date. DERMATOLOGY is fully booked on all dates, which is how the "no appointment slots
 * available" scenario is produced. Booked slots disappear from the availability list.
 */
public class SchedulingCalendar {

  public record Slot(
      String slotId,
      String department,
      String doctorCode,
      String date,
      String startTime,
      String endTime,
      String session) {}

  public record BookingRequest(
      String slotId, String patientRef, String urgencyCategory, String requestedBy) {}

  public record Booking(
      String bookingId,
      String status,
      String slotId,
      String patientRef,
      String urgencyCategory,
      String bookedAt) {}

  public static class SlotNotAvailableException extends RuntimeException {
    public SlotNotAvailableException(String message) {
      super(message);
    }
  }

  private static final Map<String, String> DEPARTMENTS =
      Map.of(
          "GENERAL_MEDICINE", "GEN",
          "CARDIOLOGY", "CAR",
          "ORTHOPAEDICS", "ORT",
          "PAEDIATRICS", "PAE",
          "DERMATOLOGY", "DER");

  /** Departments with no free capacity at all (synthetic). */
  private static final List<String> FULLY_BOOKED = List.of("DERMATOLOGY");

  private static final List<LocalTime> MORNING =
      List.of(t(9, 0), t(9, 30), t(10, 0), t(10, 30), t(11, 0), t(11, 30));
  private static final List<LocalTime> AFTERNOON =
      List.of(t(14, 0), t(14, 30), t(15, 0), t(15, 30));

  private static final DateTimeFormatter ID_DATE = DateTimeFormatter.BASIC_ISO_DATE;
  private static final DateTimeFormatter ID_TIME = DateTimeFormatter.ofPattern("HHmm");

  private final Map<String, Booking> bookingsBySlot = new ConcurrentHashMap<>();
  private final AtomicLong sequence = new AtomicLong(90000);

  private static LocalTime t(int h, int m) {
    return LocalTime.of(h, m);
  }

  public List<Slot> availableSlots(String department, LocalDate date, String session) {
    String dept = normaliseDepartment(department);
    String sess = session == null ? "ANY" : session.toUpperCase(Locale.ROOT);
    List<Slot> slots = new ArrayList<>();
    if (FULLY_BOOKED.contains(dept)) {
      return slots;
    }
    if (sess.equals("ANY") || sess.equals("MORNING")) {
      MORNING.forEach(time -> addIfFree(slots, dept, date, time, "MORNING"));
    }
    if (sess.equals("ANY") || sess.equals("AFTERNOON")) {
      AFTERNOON.forEach(time -> addIfFree(slots, dept, date, time, "AFTERNOON"));
    }
    return slots;
  }

  public synchronized Booking book(BookingRequest request) {
    Slot slot = parseSlot(request.slotId());
    if (FULLY_BOOKED.contains(slot.department()) || bookingsBySlot.containsKey(slot.slotId())) {
      throw new SlotNotAvailableException("Slot " + request.slotId() + " is no longer available");
    }
    Booking booking =
        new Booking(
            "BKG-" + sequence.incrementAndGet(),
            "CONFIRMED",
            slot.slotId(),
            request.patientRef(),
            request.urgencyCategory(),
            Instant.now().toString());
    bookingsBySlot.put(slot.slotId(), booking);
    return booking;
  }

  private void addIfFree(List<Slot> slots, String dept, LocalDate date, LocalTime time, String s) {
    Slot slot = slot(dept, date, time, s);
    if (!bookingsBySlot.containsKey(slot.slotId())) {
      slots.add(slot);
    }
  }

  static Slot slot(String dept, LocalDate date, LocalTime time, String session) {
    String code = DEPARTMENTS.get(dept);
    String id = code + "-" + date.format(ID_DATE) + "-" + time.format(ID_TIME);
    int doctorNo = (time.getHour() % 2) + 1;
    return new Slot(
        id,
        dept,
        "DR-" + code + "-0" + doctorNo,
        date.toString(),
        time.toString(),
        time.plusMinutes(30).toString(),
        session);
  }

  /** Slot ids look like GEN-20261105-0930. */
  static Slot parseSlot(String slotId) {
    if (slotId == null) {
      throw new IllegalArgumentException("slotId is required");
    }
    String[] parts = slotId.split("-");
    if (parts.length != 3) {
      throw new IllegalArgumentException("Malformed slotId '" + slotId + "'");
    }
    String dept =
        DEPARTMENTS.entrySet().stream()
            .filter(e -> e.getValue().equals(parts[0]))
            .map(Map.Entry::getKey)
            .findFirst()
            .orElseThrow(() -> new IllegalArgumentException("Unknown department code in " + slotId));
    LocalDate date = LocalDate.parse(parts[1], ID_DATE);
    LocalTime time = LocalTime.parse(parts[2], ID_TIME);
    String session = time.isBefore(LocalTime.NOON) ? "MORNING" : "AFTERNOON";
    if (!MORNING.contains(time) && !AFTERNOON.contains(time)) {
      throw new IllegalArgumentException("No such slot time in " + slotId);
    }
    return slot(dept, date, time, session);
  }

  static String normaliseDepartment(String department) {
    String dept = department == null ? "" : department.trim().toUpperCase(Locale.ROOT);
    if (!DEPARTMENTS.containsKey(dept)) {
      throw new IllegalArgumentException(
          "Unknown department '" + department + "'. Known: " + DEPARTMENTS.keySet());
    }
    return dept;
  }
}
