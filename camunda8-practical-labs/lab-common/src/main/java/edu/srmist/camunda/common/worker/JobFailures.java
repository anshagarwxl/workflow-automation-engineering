package edu.srmist.camunda.common.worker;

import io.camunda.client.api.response.ActivatedJob;
import io.camunda.client.exception.CamundaError;
import io.camunda.client.exception.JobError;
import java.time.Duration;

/**
 * Builds job failures with an explicit retry policy.
 *
 * <ul>
 *   <li>{@link #nonRetryable} – bad data: retries = 0, incident is raised immediately.
 *   <li>{@link #retryable} – transient technical error (service down): retries are decremented
 *       with a back-off; when they reach 0 Zeebe raises an incident.
 * </ul>
 */
public final class JobFailures {

  private JobFailures() {}

  public static JobError nonRetryable(String message) {
    return CamundaError.jobError(message, null, 0);
  }

  public static JobError retryable(ActivatedJob job, String message, Duration backoff) {
    int remaining = Math.max(job.getRetries() - 1, 0);
    return CamundaError.jobError(
        message + " (retries left: " + remaining + ")", null, remaining, backoff);
  }
}
