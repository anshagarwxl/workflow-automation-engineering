package edu.srmist.camunda.loan.worker;

import edu.srmist.camunda.common.worker.DataContractException;
import edu.srmist.camunda.common.worker.JobFailures;
import edu.srmist.camunda.loan.logic.LoanApplicationValidator;
import edu.srmist.camunda.loan.logic.LoanOutcomes;
import io.camunda.client.annotation.JobWorker;
import io.camunda.client.api.response.ActivatedJob;
import java.time.LocalDate;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * External job workers for the loan approval process. Each method subscribes to one job type
 * (the {@code zeebe:taskDefinition type} of a service task). The returned map is merged into the
 * process variables when the job is completed (autoComplete = true).
 */
@Component
public class LoanWorkers {

  private static final Logger LOG = LoggerFactory.getLogger(LoanWorkers.class);

  /** Service task "Validate applicant information". */
  @JobWorker(type = "loan.validate-application", fetchAllVariables = true)
  public Map<String, Object> validateApplication(ActivatedJob job) {
    Map<String, Object> vars = job.getVariablesAsMap();
    LOG.info(
        "[validate] instance={} application={} (attempt with {} retries left)",
        job.getProcessInstanceKey(),
        vars.get("applicationId"),
        job.getRetries());
    try {
      Map<String, Object> result = LoanApplicationValidator.validate(vars);
      LOG.info("[validate] instance={} result={}", job.getProcessInstanceKey(), result);
      return result;
    } catch (DataContractException e) {
      // Bad data will not fix itself: fail without retries -> incident in Operate.
      LOG.warn("[validate] instance={} data contract violation: {}", job.getProcessInstanceKey(), e.getMessage());
      throw JobFailures.nonRetryable(
          "Validation worker cannot process the application: "
              + e.getMessage()
              + ". Fix the variable in Operate and retry.");
    }
  }

  /** Service task "Issue loan offer". */
  @JobWorker(type = "loan.issue-offer", fetchAllVariables = true)
  public Map<String, Object> issueOffer(ActivatedJob job) {
    Map<String, Object> result = LoanOutcomes.offer(job.getVariablesAsMap(), LocalDate.now());
    LOG.info("[offer] instance={} {}", job.getProcessInstanceKey(), result);
    return result;
  }

  /** Service task "Send rejection notice". */
  @JobWorker(type = "loan.send-rejection", fetchAllVariables = true)
  public Map<String, Object> sendRejection(ActivatedJob job) {
    Map<String, Object> result = LoanOutcomes.rejection(job.getVariablesAsMap());
    LOG.info("[rejection] instance={} {}", job.getProcessInstanceKey(), result);
    return result;
  }
}
