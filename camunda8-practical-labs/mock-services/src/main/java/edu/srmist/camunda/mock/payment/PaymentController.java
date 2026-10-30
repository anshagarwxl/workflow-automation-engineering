package edu.srmist.camunda.mock.payment;

import edu.srmist.camunda.mock.faults.FaultRegistry;
import edu.srmist.camunda.mock.payment.PaymentGateway.Charge;
import edu.srmist.camunda.mock.payment.PaymentGateway.ChargeRequest;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Lab 2 – simulated payment gateway. A business decline is a normal HTTP 200 response with
 * {@code status = DECLINED}; only technical problems produce 4xx/5xx.
 */
@RestController
@RequestMapping("/payments/api/v1")
public class PaymentController {

  private static final Logger LOG = LoggerFactory.getLogger(PaymentController.class);

  private final PaymentGateway gateway = new PaymentGateway();
  private final FaultRegistry faults;

  public PaymentController(FaultRegistry faults) {
    this.faults = faults;
  }

  @PostMapping("/charges")
  public ResponseEntity<Charge> charge(
      @RequestHeader(value = "Idempotency-Key", required = false) String idempotencyKey,
      @RequestBody ChargeRequest request) {
    LOG.info("Charge requested (Idempotency-Key={}): {}", idempotencyKey, request);
    faults.apply(FaultRegistry.PAYMENT);
    Charge charge = gateway.charge(request);
    LOG.info("Charge result: {}", charge);
    return ResponseEntity.ok(charge);
  }

  @ExceptionHandler(IllegalArgumentException.class)
  public ResponseEntity<Map<String, String>> invalid(IllegalArgumentException e) {
    return ResponseEntity.badRequest().body(Map.of("error", "INVALID_REQUEST", "message", e.getMessage()));
  }
}
