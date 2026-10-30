package edu.srmist.camunda.common.scenario;

import java.util.List;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Per-lab settings (see each lab's application.yaml, prefix {@code lab}).
 *
 * @param processId BPMN process id that scenarios start
 * @param scenarioLocation resource pattern of the scenario JSON files
 * @param outcomeVariable variable that holds the final business outcome of an instance
 * @param reportVariables variables shown in the results report
 * @param mockApiBaseUrl base URL of the mock REST services, passed to the process as {@code
 *     mockApiBaseUrl} so the REST Connector knows where to call (leave empty to use the default
 *     http://localhost:8090 defined in the BPMN)
 */
@ConfigurationProperties("lab")
public record LabProperties(
    String processId,
    String scenarioLocation,
    String outcomeVariable,
    List<String> reportVariables,
    String mockApiBaseUrl) {

  public LabProperties {
    if (scenarioLocation == null || scenarioLocation.isBlank()) {
      scenarioLocation = "classpath*:/test-data/*.json";
    }
    if (outcomeVariable == null || outcomeVariable.isBlank()) {
      outcomeVariable = "finalOutcome";
    }
    reportVariables = reportVariables == null ? List.of() : List.copyOf(reportVariables);
  }
}
