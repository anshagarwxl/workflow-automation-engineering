package edu.srmist.camunda.common.scenario;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.camunda.client.CamundaClient;
import io.camunda.client.api.response.ProcessInstanceEvent;
import io.camunda.client.api.search.enums.ElementInstanceState;
import io.camunda.client.api.search.enums.ElementInstanceType;
import io.camunda.client.api.search.enums.IncidentState;
import io.camunda.client.api.search.response.ElementInstance;
import io.camunda.client.api.search.response.Incident;
import io.camunda.client.api.search.response.ProcessInstance;
import io.camunda.client.api.search.response.Variable;
import java.io.IOException;
import java.io.InputStream;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.stereotype.Service;

/**
 * Starts process instances from scenario files and builds the test-case results table by querying
 * the Camunda REST API (process instance state, incidents, active elements, variables).
 */
@Service
public class ScenarioService {

  private static final Logger LOG = LoggerFactory.getLogger(ScenarioService.class);
  private static final TypeReference<LinkedHashMap<String, Object>> MAP_TYPE =
      new TypeReference<>() {};

  private final CamundaClient client;
  private final LabProperties props;
  private final ObjectMapper mapper;
  private final List<StartedInstance> started = new CopyOnWriteArrayList<>();

  public ScenarioService(CamundaClient client, LabProperties props, ObjectMapper mapper) {
    this.client = client;
    this.props = props;
    this.mapper = mapper;
  }

  public record StartedInstance(
      String scenarioId, long processInstanceKey, Instant startedAt, Map<String, Object> meta) {}

  /** All scenario files on the classpath, keyed by name (file name without .json). */
  public Map<String, Map<String, Object>> scenarios() {
    Map<String, Map<String, Object>> result = new TreeMap<>();
    try {
      Resource[] resources =
          new PathMatchingResourcePatternResolver().getResources(props.scenarioLocation());
      for (Resource r : resources) {
        String name = r.getFilename() == null ? "unknown" : r.getFilename().replace(".json", "");
        try (InputStream in = r.getInputStream()) {
          result.put(name, mapper.readValue(in, MAP_TYPE));
        }
      }
    } catch (IOException e) {
      throw new IllegalStateException("Cannot read scenario files: " + e.getMessage(), e);
    }
    return result;
  }

  public StartedInstance startScenario(String name) {
    Map<String, Object> scenario = scenarios().get(name);
    if (scenario == null) {
      throw new IllegalArgumentException(
          "Unknown scenario '" + name + "'. Available: " + scenarios().keySet());
    }
    Map<String, Object> vars = ScenarioFiles.processVariables(scenario, LocalDate.now());
    vars.put("scenarioId", name);
    return start(name, vars, ScenarioFiles.metadata(scenario));
  }

  public StartedInstance startWithVariables(Map<String, Object> variables) {
    Map<String, Object> vars = ScenarioFiles.processVariables(variables, LocalDate.now());
    String id = String.valueOf(vars.getOrDefault("scenarioId", "adhoc-" + (started.size() + 1)));
    vars.put("scenarioId", id);
    return start(id, vars, ScenarioFiles.metadata(variables));
  }

  private StartedInstance start(String id, Map<String, Object> vars, Map<String, Object> meta) {
    if (props.mockApiBaseUrl() != null && !props.mockApiBaseUrl().isBlank()) {
      vars.putIfAbsent("mockApiBaseUrl", props.mockApiBaseUrl());
    }
    ProcessInstanceEvent event =
        client
            .newCreateInstanceCommand()
            .bpmnProcessId(props.processId())
            .latestVersion()
            .variables(vars)
            .send()
            .join();
    StartedInstance si =
        new StartedInstance(id, event.getProcessInstanceKey(), Instant.now(), meta);
    started.add(si);
    LOG.info("Started scenario '{}' -> process instance {}", id, si.processInstanceKey());
    return si;
  }

  public List<StartedInstance> started() {
    return List.copyOf(started);
  }

  /** One row of the test-case table, built from live engine data. */
  public record ResultRow(
      String scenarioId,
      long processInstanceKey,
      String description,
      String expectedOutcome,
      String state,
      String actualOutcome,
      String status,
      Map<String, String> variables) {}

  public List<ResultRow> results() {
    List<ResultRow> rows = new ArrayList<>();
    for (StartedInstance si : started) {
      rows.add(resultFor(si));
    }
    return rows;
  }

  private ResultRow resultFor(StartedInstance si) {
    long key = si.processInstanceKey();
    String description = String.valueOf(si.meta().getOrDefault("description", ""));
    String expected = String.valueOf(si.meta().getOrDefault("expectedOutcome", ""));
    try {
      ProcessInstance pi = client.newProcessInstanceGetRequest(key).send().join();
      Map<String, String> vars = variables(key);
      String state = String.valueOf(pi.getState());
      String actual;
      String status;
      switch (pi.getState()) {
        case COMPLETED -> {
          actual = vars.getOrDefault(props.outcomeVariable(), "COMPLETED");
          status = expected.isBlank() ? "DONE" : (expected.equals(actual) ? "PASS" : "FAIL");
        }
        case ACTIVE -> {
          List<Incident> incidents =
              client
                  .newIncidentSearchRequest()
                  .filter(f -> f.processInstanceKey(key).state(IncidentState.ACTIVE))
                  .send()
                  .join()
                  .items();
          if (!incidents.isEmpty()) {
            Incident i = incidents.get(0);
            actual = "INCIDENT at " + i.getElementId() + ": " + shorten(i.getErrorMessage());
            status = "INCIDENT - fix and retry in Operate";
          } else {
            actual = "WAITING at " + activeElements(key);
            status = "IN PROGRESS - complete the task in Tasklist";
          }
        }
        default -> {
          actual = state;
          status = "CHECK";
        }
      }
      Map<String, String> selected = new LinkedHashMap<>();
      for (String name : props.reportVariables()) {
        if (vars.containsKey(name)) {
          selected.put(name, vars.get(name));
        }
      }
      return new ResultRow(
          si.scenarioId(), key, description, expected, state, actual, status, selected);
    } catch (RuntimeException e) {
      // Operate/Tasklist data is indexed asynchronously; a just-started instance may not be
      // searchable for a second or two.
      return new ResultRow(
          si.scenarioId(),
          key,
          description,
          expected,
          "UNKNOWN",
          "Not yet available: " + shorten(e.getMessage()),
          "RETRY IN A FEW SECONDS",
          Map.of());
    }
  }

  private String activeElements(long key) {
    List<ElementInstance> active =
        client
            .newElementInstanceSearchRequest()
            .filter(f -> f.processInstanceKey(key).state(ElementInstanceState.ACTIVE))
            .send()
            .join()
            .items();
    String names =
        active.stream()
            .filter(e -> e.getType() != ElementInstanceType.PROCESS)
            .map(e -> e.getElementName() == null ? e.getElementId() : e.getElementName())
            .collect(Collectors.joining(", "));
    return names.isBlank() ? "(engine busy)" : names;
  }

  private Map<String, String> variables(long key) {
    List<Variable> vars =
        client
            .newVariableSearchRequest()
            .filter(f -> f.processInstanceKey(key))
            .page(p -> p.limit(200))
            .send()
            .join()
            .items();
    Map<String, String> out = new TreeMap<>();
    for (Variable v : vars) {
      // Only process-level variables (scope = process instance), not task-local ones.
      if (v.getScopeKey() != null && v.getScopeKey() == key) {
        out.put(v.getName(), plain(v.getValue()));
      }
    }
    return out;
  }

  private String plain(String json) {
    if (json == null) {
      return "null";
    }
    try {
      JsonNode node = mapper.readTree(json);
      return node.isTextual() ? node.asText() : node.toString();
    } catch (IOException e) {
      return json;
    }
  }

  private static String shorten(String s) {
    if (s == null) {
      return "";
    }
    String oneLine = s.replace('\n', ' ').replace('|', '/');
    return oneLine.length() > 140 ? oneLine.substring(0, 140) + "..." : oneLine;
  }

  /** Markdown version of {@link #results()}, ready to paste into docs/test-cases.md. */
  public String resultsMarkdown() {
    StringBuilder sb = new StringBuilder();
    sb.append("| Scenario | Process instance | Description | Expected outcome | Actual outcome | Status | Key variables |\n");
    sb.append("|---|---|---|---|---|---|---|\n");
    for (ResultRow r : results()) {
      String keyVars =
          r.variables().entrySet().stream()
              .map(e -> e.getKey() + "=" + shorten(e.getValue()))
              .collect(Collectors.joining("<br>"));
      sb.append("| ")
          .append(r.scenarioId())
          .append(" | ")
          .append(r.processInstanceKey())
          .append(" | ")
          .append(shorten(r.description()))
          .append(" | ")
          .append(r.expectedOutcome())
          .append(" | ")
          .append(r.actualOutcome())
          .append(" | ")
          .append(r.status())
          .append(" | ")
          .append(keyVars)
          .append(" |\n");
    }
    return sb.toString();
  }
}
