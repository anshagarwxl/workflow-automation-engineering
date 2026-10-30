package edu.srmist.camunda.common.scenario;

import edu.srmist.camunda.common.scenario.ScenarioService.ResultRow;
import edu.srmist.camunda.common.scenario.ScenarioService.StartedInstance;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Helper API for executing the test scenarios.
 *
 * <pre>
 * GET  /api/scenarios                 list scenario files with description + expected outcome
 * POST /api/scenarios/{name}/start    start one scenario
 * POST /api/scenarios/start-all       start every scenario (except ones marked "_manualStartOnly")
 * POST /api/process-instances         start an instance with any JSON body (Postman / curl)
 * GET  /api/scenarios/results         live results (JSON)
 * GET  /api/scenarios/results.md      live results as a Markdown test-case table
 * </pre>
 */
@RestController
@RequestMapping("/api")
public class ScenarioController {

  private final ScenarioService service;

  public ScenarioController(ScenarioService service) {
    this.service = service;
  }

  @GetMapping("/scenarios")
  public Map<String, Map<String, Object>> list() {
    Map<String, Map<String, Object>> out = new LinkedHashMap<>();
    service.scenarios().forEach((name, s) -> out.put(name, ScenarioFiles.metadata(s)));
    return out;
  }

  @PostMapping("/scenarios/{name}/start")
  public StartedInstance start(@PathVariable("name") String name) {
    return service.startScenario(name);
  }

  @PostMapping("/scenarios/start-all")
  public List<StartedInstance> startAll() {
    List<StartedInstance> result = new ArrayList<>();
    service
        .scenarios()
        .forEach(
            (name, s) -> {
              if (!Boolean.TRUE.equals(s.get("_manualStartOnly"))) {
                result.add(service.startScenario(name));
              }
            });
    return result;
  }

  @PostMapping("/process-instances")
  public StartedInstance startAdHoc(@RequestBody Map<String, Object> variables) {
    return service.startWithVariables(variables);
  }

  @GetMapping("/scenarios/results")
  public List<ResultRow> results() {
    return service.results();
  }

  @GetMapping(value = "/scenarios/results.md", produces = MediaType.TEXT_PLAIN_VALUE)
  public String resultsMarkdown() {
    return service.resultsMarkdown();
  }

  @ExceptionHandler(IllegalArgumentException.class)
  public ResponseEntity<Map<String, String>> badRequest(IllegalArgumentException e) {
    return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of("error", e.getMessage()));
  }
}
