package edu.srmist.camunda.mock.faults;

import edu.srmist.camunda.mock.faults.FaultRegistry.Mode;
import java.util.Map;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Fault injection API.
 *
 * <pre>
 *   GET /admin/faults
 *   PUT /admin/faults/payment?mode=DOWN
 *   PUT /admin/faults/payment?mode=UP
 * </pre>
 */
@RestController
@RequestMapping("/admin/faults")
public class FaultAdminController {

  private final FaultRegistry faults;

  public FaultAdminController(FaultRegistry faults) {
    this.faults = faults;
  }

  @GetMapping
  public Map<String, Mode> list() {
    return faults.all();
  }

  @PutMapping("/{service}")
  public Map<String, Mode> set(
      @PathVariable("service") String service, @RequestParam("mode") Mode mode) {
    faults.set(service, mode);
    return faults.all();
  }

  @ExceptionHandler(IllegalArgumentException.class)
  public ResponseEntity<Map<String, String>> unknown(IllegalArgumentException e) {
    return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
  }
}
