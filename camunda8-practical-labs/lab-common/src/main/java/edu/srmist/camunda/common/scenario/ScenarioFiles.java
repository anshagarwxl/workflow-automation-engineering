package edu.srmist.camunda.common.scenario;

import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Pure helpers for scenario files (no Spring / Camunda dependency, unit-tested).
 *
 * <p>A scenario file is the JSON payload used to start a process instance. Keys starting with
 * {@code _} are metadata (description, expected outcome) and are not sent to the engine. String
 * values {@code {{TODAY}}} / {@code {{TODAY+N}}} are replaced with ISO dates so date-based
 * scenarios never go stale.
 */
public final class ScenarioFiles {

  private static final Pattern DATE_TOKEN = Pattern.compile("\\{\\{TODAY([+-]\\d+)?}}");

  private ScenarioFiles() {}

  public static Map<String, Object> processVariables(Map<String, Object> scenario, LocalDate today) {
    Map<String, Object> vars = new LinkedHashMap<>();
    scenario.forEach(
        (k, v) -> {
          if (!k.startsWith("_")) {
            vars.put(k, resolve(v, today));
          }
        });
    return vars;
  }

  public static Map<String, Object> metadata(Map<String, Object> scenario) {
    Map<String, Object> meta = new LinkedHashMap<>();
    scenario.forEach(
        (k, v) -> {
          if (k.startsWith("_")) {
            meta.put(k.substring(1), v);
          }
        });
    return meta;
  }

  @SuppressWarnings("unchecked")
  static Object resolve(Object value, LocalDate today) {
    if (value instanceof String s) {
      Matcher m = DATE_TOKEN.matcher(s);
      if (m.matches()) {
        int offset = m.group(1) == null ? 0 : Integer.parseInt(m.group(1));
        return today.plusDays(offset).toString();
      }
      return s;
    }
    if (value instanceof Map<?, ?> map) {
      Map<String, Object> out = new LinkedHashMap<>();
      ((Map<String, Object>) map).forEach((k, v) -> out.put(k, resolve(v, today)));
      return out;
    }
    if (value instanceof List<?> list) {
      return list.stream().map(v -> resolve(v, today)).toList();
    }
    return value;
  }
}
