package edu.srmist.camunda.hospital;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import edu.srmist.camunda.hospital.logic.AppointmentLogic;
import edu.srmist.camunda.hospital.logic.RegistrationValidator;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class HospitalLogicTest {

  private static final LocalDate TODAY = LocalDate.of(2026, 11, 2);

  private static Map<String, Object> registration() {
    Map<String, Object> v = new HashMap<>();
    v.put("patientRef", "SYN-PAT-3001");
    v.put("ageYears", 34);
    v.put("department", "GENERAL_MEDICINE");
    v.put("preferredDate", "2026-11-05");
    v.put("preferredSession", "MORNING");
    v.put("symptomsSummary", "Mild cough");
    v.put("painScore", 2);
    v.put("symptomDurationDays", 5);
    v.put("chestPain", false);
    return v;
  }

  @Test
  void validRegistrationIsNormalised() {
    Map<String, Object> v = registration();
    v.put("breathingDifficulty", "no");
    Map<String, Object> out = RegistrationValidator.validate(v, TODAY);
    assertEquals(true, out.get("registrationValid"));
    assertEquals("2026-11-05", out.get("appointmentDate"));
    assertEquals(false, out.get("breathingDifficulty"));
    assertEquals(false, out.get("highFever"));
  }

  @Test
  void realNamesAndPastDatesAreRejected() {
    Map<String, Object> v = registration();
    v.put("patientRef", "John Smith");
    v.put("preferredDate", "2026-10-31");
    Map<String, Object> out = RegistrationValidator.validate(v, TODAY);
    assertEquals(false, out.get("registrationValid"));
    assertEquals(2, ((List<?>) out.get("registrationErrors")).size());
  }

  @Test
  void missingDateDefaultsToTomorrowAndFullTimestampIsAccepted() {
    Map<String, Object> v = registration();
    v.remove("preferredDate");
    assertEquals("2026-11-03", RegistrationValidator.validate(v, TODAY).get("appointmentDate"));
    v.put("preferredDate", "2026-11-06T00:00:00Z");
    assertEquals("2026-11-06", RegistrationValidator.validate(v, TODAY).get("appointmentDate"));
  }

  @Test
  void staffSchedulingOutcomeBecomesFinalOutcome() {
    Map<String, Object> v = registration();
    v.put("notificationType", "STAFF_SCHEDULING");
    v.put("schedulingOutcome", "WAITLISTED");
    assertEquals("WAITLISTED", AppointmentLogic.notification(v).get("finalOutcome"));
    v.put("notificationType", "BOOKING_CONFIRMED");
    assertEquals("BOOKED", AppointmentLogic.notification(v).get("finalOutcome"));
  }
}
