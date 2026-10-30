package edu.srmist.camunda.mock.credit;

import edu.srmist.camunda.mock.credit.CreditBureau.Assessment;
import edu.srmist.camunda.mock.credit.CreditBureau.AssessmentRequest;
import edu.srmist.camunda.mock.faults.FaultRegistry;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Lab 1 – simulated credit-assessment API. */
@RestController
@RequestMapping("/credit-bureau/api/v1")
public class CreditBureauController {

  private static final Logger LOG = LoggerFactory.getLogger(CreditBureauController.class);

  private final CreditBureau bureau = new CreditBureau();
  private final FaultRegistry faults;

  public CreditBureauController(FaultRegistry faults) {
    this.faults = faults;
  }

  @PostMapping("/assessments")
  public ResponseEntity<?> assess(@RequestBody AssessmentRequest request) {
    LOG.info("Credit assessment requested: {}", request);
    faults.apply(FaultRegistry.CREDIT);
    if (CreditBureau.TIMEOUT_APPLICANT.equals(request.applicantId())) {
      FaultRegistry.sleep(FaultRegistry.SLOW_DELAY_MS);
    }
    if (CreditBureau.ERROR_APPLICANT.equals(request.applicantId())) {
      return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
          .body(Map.of("error", "BUREAU_INTERNAL_ERROR", "message", "Simulated bureau failure"));
    }
    Assessment assessment = bureau.assess(request);
    LOG.info("Credit assessment result: {}", assessment);
    return ResponseEntity.ok(assessment);
  }

  @ExceptionHandler(IllegalArgumentException.class)
  public ResponseEntity<Map<String, String>> invalid(IllegalArgumentException e) {
    return ResponseEntity.badRequest().body(Map.of("error", "INVALID_REQUEST", "message", e.getMessage()));
  }
}
