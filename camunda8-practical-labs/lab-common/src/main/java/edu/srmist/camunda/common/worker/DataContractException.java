package edu.srmist.camunda.common.worker;

/**
 * Thrown when process data does not match the contract a worker expects (missing variable, wrong
 * type). Retrying will not fix such data, so workers fail the job with zero retries and an
 * incident is raised immediately. The fix is to correct the variable in Operate and retry.
 */
public class DataContractException extends RuntimeException {
  public DataContractException(String message) {
    super(message);
  }
}
