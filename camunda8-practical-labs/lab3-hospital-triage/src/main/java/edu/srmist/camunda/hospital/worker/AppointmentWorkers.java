package edu.srmist.camunda.hospital.worker;

import edu.srmist.camunda.common.worker.DataContractException;
import edu.srmist.camunda.common.worker.JobFailures;
import edu.srmist.camunda.common.worker.Vars;
import edu.srmist.camunda.hospital.logic.AppointmentLogic;
import edu.srmist.camunda.hospital.logic.RegistrationValidator;
import edu.srmist.camunda.hospital.worker.SchedulingClient.BookingResult;
import edu.srmist.camunda.hospital.worker.SchedulingClient.TransientSchedulingException;
import io.camunda.client.annotation.JobWorker;
import io.camunda.client.api.response.ActivatedJob;
import java.time.Duration;
import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/** External job workers for the hospital appointment and triage process. */
@Component
public class AppointmentWorkers {

  private static final Logger LOG = LoggerFactory.getLogger(AppointmentWorkers.class);
  private static final Duration BACKOFF = Duration.ofSeconds(10);

  private final SchedulingClient scheduling;

  public AppointmentWorkers(SchedulingClient scheduling) {
    this.scheduling = scheduling;
  }

  /** Service task "Validate registration". */
  @JobWorker(type = "appointment.validate-registration", fetchAllVariables = true)
  public Map<String, Object> validateRegistration(ActivatedJob job) {
    try {
      Map<String, Object> result = RegistrationValidator.validate(job.getVariablesAsMap(), LocalDate.now());
      LOG.info("[validate] instance={} {}", job.getProcessInstanceKey(), result);
      return result;
    } catch (DataContractException e) {
      throw JobFailures.nonRetryable("Registration data invalid: " + e.getMessage() + ". Fix the variable in Operate and retry.");
    }
  }

  /** Service task "Request appointment booking": picks the earliest slot and books it. */
  @JobWorker(type = "appointment.request-booking", fetchAllVariables = true)
  public Map<String, Object> requestBooking(ActivatedJob job) {
    Map<String, Object> v = job.getVariablesAsMap();
    Optional<Map<String, Object>> slot = AppointmentLogic.earliestSlot(v.get("availableSlots"));
    Map<String, Object> out = new LinkedHashMap<>();
    if (slot.isEmpty()) {
      out.put("bookingStatus", "NO_SLOT");
      return out;
    }
    Map<String, Object> s = slot.get();
    String category = "ROUTINE";
    if (v.get("triage") instanceof Map<?, ?> t && t.get("category") != null) {
      category = String.valueOf(t.get("category"));
    }
    Map<String, Object> request = new LinkedHashMap<>();
    request.put("slotId", s.get("slotId"));
    request.put("patientRef", v.get("patientRef"));
    request.put("urgencyCategory", category);
    request.put("requestedBy", "camunda-lab3-worker");
    try {
      BookingResult result = scheduling.book(Vars.optString(v, "mockApiBaseUrl"), request);
      if (result.confirmed()) {
        out.put("bookingStatus", "CONFIRMED");
        out.put("bookingId", result.body().get("bookingId"));
        out.put("appointmentSlotId", s.get("slotId"));
        out.put("appointmentDate", s.get("date"));
        out.put("appointmentTime", s.get("startTime") + "-" + s.get("endTime"));
        out.put("doctorCode", s.get("doctorCode"));
      } else {
        out.put("bookingStatus", "SLOT_TAKEN");
        out.put("bookingProblem", result.problem());
      }
      LOG.info("[booking] instance={} {}", job.getProcessInstanceKey(), out);
      return out;
    } catch (TransientSchedulingException e) {
      throw JobFailures.retryable(job, e.getMessage(), BACKOFF);
    }
  }

  /** All "Notify ..." service tasks (notification type comes from the BPMN input mapping). */
  @JobWorker(type = "appointment.notify-patient", fetchAllVariables = true)
  public Map<String, Object> notifyPatient(ActivatedJob job) {
    Map<String, Object> result = AppointmentLogic.notification(job.getVariablesAsMap());
    LOG.info("[notify] instance={} {}", job.getProcessInstanceKey(), result);
    return result;
  }
}
