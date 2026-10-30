package edu.srmist.camunda.order;

import edu.srmist.camunda.common.LabCommonConfiguration;
import io.camunda.client.annotation.Deployment;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Import;

/** Lab 2 – E-commerce order processing and fulfilment. Deploys models/ and starts the workers. */
@SpringBootApplication
@Import(LabCommonConfiguration.class)
@Deployment(
    resources = {
      "classpath*:/models/*.bpmn",
      "classpath*:/models/*.dmn",
      "classpath*:/models/*.form"
    })
public class OrderProcessingApplication {
  public static void main(String[] args) {
    SpringApplication.run(OrderProcessingApplication.class, args);
  }
}
