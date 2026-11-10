package edu.srmist.camunda.hospital.worker;

import edu.srmist.camunda.common.worker.DataContractException;
import edu.srmist.camunda.common.worker.JobFailures;
import edu.srmist.camunda.hospital.logic.AppointmentLogic;
import edu.srmist.camunda.hospital.logic.RegistrationValidator;
import io.camunda.client.annotation.JobWorker;
import io.camunda.client.api.response.ActivatedJob;
import java.time.LocalDate;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * External job workers for the hospital appointment and triage process.
 *
 * <p>These workers only compute (validation, notification text). Every call to an external URL –
 * the slot lookup and the booking – is made by a REST Connector task in the BPMN, not by Java code.
 */
@Component
public class AppointmentWorkers {

  private static final Logger LOG = LoggerFactory.getLogger(AppointmentWorkers.class);
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

  /** All "Notify ..." service tasks (notification type comes from the BPMN input mapping). */
  @JobWorker(type = "appointment.notify-patient", fetchAllVariables = true)
  public Map<String, Object> notifyPatient(ActivatedJob job) {
    Map<String, Object> result = AppointmentLogic.notification(job.getVariablesAsMap());
    LOG.info("[notify] instance={} {}", job.getProcessInstanceKey(), result);
    return result;
  }
}
