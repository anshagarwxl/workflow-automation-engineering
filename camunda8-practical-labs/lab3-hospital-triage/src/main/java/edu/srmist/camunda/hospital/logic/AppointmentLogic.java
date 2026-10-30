package edu.srmist.camunda.hospital.logic;

import edu.srmist.camunda.common.worker.Vars;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/** Slot selection and patient-notification logic (pure functions, unit-tested). */
public final class AppointmentLogic {

  private AppointmentLogic() {}

  /** Picks the earliest slot (date, then start time) from the scheduling API result. */
  public static Optional<Map<String, Object>> earliestSlot(Object availableSlots) {
    if (!(availableSlots instanceof List<?> list)) {
      return Optional.empty();
    }
    return list.stream()
        .filter(Map.class::isInstance)
        .map(o -> toStringKeyMap((Map<?, ?>) o))
        .min(
            Comparator.comparing((Map<String, Object> s) -> String.valueOf(s.get("date")))
                .thenComparing(s -> String.valueOf(s.get("startTime"))));
  }

  private static Map<String, Object> toStringKeyMap(Map<?, ?> m) {
    Map<String, Object> out = new LinkedHashMap<>();
    m.forEach((k, val) -> out.put(String.valueOf(k), val));
    return out;
  }

  public static String outcomeFor(String notificationType, Map<String, Object> v) {
    if (notificationType == null) {
      return "UNKNOWN";
    }
    return switch (notificationType) {
      case "REGISTRATION_REJECTED" -> "REJECTED_INVALID";
      case "URGENT_ASSESSMENT" -> "URGENT_ASSESSMENT";
      case "REFERRED" -> "REFERRED_TO_EMERGENCY";
      case "BOOKING_CONFIRMED" -> "BOOKED";
      case "STAFF_SCHEDULING" -> Vars.optString(v, "schedulingOutcome", "STAFF_FOLLOW_UP");
      default -> "UNKNOWN";
    };
  }

  public static Map<String, Object> notification(Map<String, Object> v) {
    String type = Vars.optString(v, "notificationType");
    String ref = Vars.optString(v, "patientRef", "SYN-PAT-0000");
    String message =
        switch (type == null ? "" : type) {
          case "BOOKING_CONFIRMED" ->
              "Appointment " + Vars.optString(v, "bookingId", "-") + " confirmed for " + ref + " on "
                  + Vars.optString(v, "appointmentDate", "-") + " at "
                  + Vars.optString(v, "appointmentTime", "-") + ".";
          case "REGISTRATION_REJECTED" ->
              "Registration could not be accepted: " + v.get("registrationErrors");
          case "URGENT_ASSESSMENT" ->
              "Urgent assessment completed (" + Vars.optString(v, "assessmentOutcome", "-") + ").";
          case "REFERRED" -> "Patient " + ref + " referred to the emergency department.";
          case "STAFF_SCHEDULING" ->
              "No suitable slot. Staff action: " + Vars.optString(v, "schedulingOutcome", "-") + " "
                  + Vars.optString(v, "manualSlotDetails", "");
          default -> "Appointment request update for " + ref + ".";
        };
    Map<String, Object> out = new LinkedHashMap<>();
    out.put("finalOutcome", outcomeFor(type, v));
    out.put("patientMessage", message.trim());
    out.put("notificationStatus", "SIMULATED_SMS_SENT");
    return out;
  }
}
