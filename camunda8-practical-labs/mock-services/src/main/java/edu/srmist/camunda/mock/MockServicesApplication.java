package edu.srmist.camunda.mock;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Simulated external systems called by the REST Connectors of the three labs.
 *
 * <ul>
 *   <li>Lab 1 – Credit bureau: {@code POST /credit-bureau/api/v1/assessments}
 *   <li>Lab 2 – Payment gateway: {@code POST /payments/api/v1/charges}
 *   <li>Lab 3 – Hospital scheduling: {@code GET /scheduling/api/v1/slots}, {@code POST
 *       /scheduling/api/v1/bookings}
 *   <li>Fault injection: {@code PUT /admin/faults/{credit|payment|scheduling}?mode=UP|DOWN|SLOW}
 * </ul>
 *
 * All data is synthetic. No real credentials, card numbers or patient data are used.
 */
@SpringBootApplication
public class MockServicesApplication {
  public static void main(String[] args) {
    SpringApplication.run(MockServicesApplication.class, args);
  }
}
