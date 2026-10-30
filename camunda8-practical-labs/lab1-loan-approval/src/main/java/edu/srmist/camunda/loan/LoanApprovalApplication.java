package edu.srmist.camunda.loan;

import edu.srmist.camunda.common.LabCommonConfiguration;
import io.camunda.client.annotation.Deployment;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Import;

/**
 * Lab 1 – Loan approval. On start-up the BPMN, DMN and form files in {@code models/} are deployed
 * to Camunda and the job workers start polling for jobs.
 */
@SpringBootApplication
@Import(LabCommonConfiguration.class)
@Deployment(
    resources = {
      "classpath*:/models/*.bpmn",
      "classpath*:/models/*.dmn",
      "classpath*:/models/*.form"
    })
public class LoanApprovalApplication {
  public static void main(String[] args) {
    SpringApplication.run(LoanApprovalApplication.class, args);
  }
}
