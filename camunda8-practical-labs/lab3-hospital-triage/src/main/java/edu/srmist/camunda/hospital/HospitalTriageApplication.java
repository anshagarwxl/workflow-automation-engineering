package edu.srmist.camunda.hospital;

import edu.srmist.camunda.common.LabCommonConfiguration;
import io.camunda.client.annotation.Deployment;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Import;

/** Lab 3 – Hospital appointment and patient triage. Deploys models/ and starts the workers. */
@SpringBootApplication
@Import(LabCommonConfiguration.class)
@Deployment(
    resources = {
      "classpath*:/models/*.bpmn",
      "classpath*:/models/*.dmn",
      "classpath*:/models/*.form"
    })
public class HospitalTriageApplication {
  public static void main(String[] args) {
    SpringApplication.run(HospitalTriageApplication.class, args);
  }
}
