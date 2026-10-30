package edu.srmist.camunda.hospital.logic;

import edu.srmist.camunda.common.worker.Vars;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * Validates the (synthetic) patient registration and normalises the urgency indicators so the
 * DMN triage table always receives typed values (booleans / numbers).
 */
public final class RegistrationValidator {

  public static final Set<String> DEPARTMENTS =
      Set.of("GENERAL_MEDICINE", "CARDIOLOGY", "ORTHOPAEDICS", "PAEDIATRICS", "DERMATOLOGY");
  public static final Set<String> SESSIONS = Set.of("ANY", "MORNING", "AFTERNOON");
  public static final int MAX_DAYS_AHEAD = 90;

  private static final Pattern SYNTHETIC_PATIENT = Pattern.compile("^SYN-PAT-\\d{4}$");

  private RegistrationValidator() {}

  public static Map<String, Object> validate(Map<String, Object> v, LocalDate today) {
    List<String> errors = new ArrayList<>();

    String patientRef = Vars.optString(v, "patientRef");
    if (patientRef == null || !SYNTHETIC_PATIENT.matcher(patientRef).matches()) {
      errors.add("patientRef must be a synthetic reference like SYN-PAT-0000 (no real names or record numbers)");
    }
    Integer age = Vars.optInt(v, "ageYears");
    if (age == null || age < 0 || age > 120) {
      errors.add("ageYears must be between 0 and 120");
    }
    String department = Vars.optString(v, "department");
    if (department == null || !DEPARTMENTS.contains(department)) {
      errors.add("department must be one of " + DEPARTMENTS);
    }
    String session = Vars.optString(v, "preferredSession", "ANY");
    if (!SESSIONS.contains(session)) {
      errors.add("preferredSession must be one of " + SESSIONS);
    }
    String summary = Vars.optString(v, "symptomsSummary");
    if (summary == null || summary.length() < 3 || summary.length() > 500) {
      errors.add("symptomsSummary is required (3-500 characters)");
    }
    Integer pain = Vars.optInt(v, "painScore");
    if (pain == null || pain < 0 || pain > 10) {
      errors.add("painScore must be between 0 and 10");
    }
    Integer duration = Vars.optInt(v, "symptomDurationDays");
    if (duration == null) {
      duration = 0;
    } else if (duration < 0) {
      errors.add("symptomDurationDays cannot be negative");
    }

    LocalDate appointmentDate = today.plusDays(1);
    String preferred = Vars.optString(v, "preferredDate");
    if (preferred != null) {
      try {
        // the form date picker sends yyyy-MM-dd; tolerate a full timestamp as well
        appointmentDate = LocalDate.parse(preferred.length() > 10 ? preferred.substring(0, 10) : preferred);
        if (appointmentDate.isBefore(today)) {
          errors.add("preferredDate cannot be in the past");
        } else if (appointmentDate.isAfter(today.plusDays(MAX_DAYS_AHEAD))) {
          errors.add("preferredDate must be within " + MAX_DAYS_AHEAD + " days");
        }
      } catch (DateTimeParseException e) {
        errors.add("preferredDate must be an ISO date (yyyy-MM-dd)");
      }
    }

    Map<String, Object> out = new LinkedHashMap<>();
    out.put("registrationValid", errors.isEmpty());
    out.put("registrationErrors", errors);
    out.put("appointmentDate", appointmentDate.toString());
    out.put("preferredSession", session);
    out.put("chestPain", Vars.optBoolean(v, "chestPain"));
    out.put("breathingDifficulty", Vars.optBoolean(v, "breathingDifficulty"));
    out.put("highFever", Vars.optBoolean(v, "highFever"));
    out.put("symptomDurationDays", duration);
    return out;
  }
}
