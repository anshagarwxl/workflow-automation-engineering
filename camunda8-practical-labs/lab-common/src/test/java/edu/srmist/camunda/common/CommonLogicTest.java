package edu.srmist.camunda.common;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import edu.srmist.camunda.common.scenario.ScenarioFiles;
import edu.srmist.camunda.common.worker.DataContractException;
import edu.srmist.camunda.common.worker.Vars;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.Map;
import org.junit.jupiter.api.Test;

class CommonLogicTest {

  @Test
  void numbersAcceptNumericStringsAndRejectText() {
    Map<String, Object> v = Map.of("a", 45000, "b", "45000.50", "c", "45k");
    assertEquals(new BigDecimal("45000"), Vars.optNumber(v, "a"));
    assertEquals(new BigDecimal("45000.50"), Vars.optNumber(v, "b"));
    assertThrows(DataContractException.class, () -> Vars.optNumber(v, "c"));
  }

  @Test
  void booleansAreLenient() {
    Map<String, Object> v = Map.of("a", true, "b", "yes", "c", "false");
    assertTrue(Vars.optBoolean(v, "a"));
    assertTrue(Vars.optBoolean(v, "b"));
    assertFalse(Vars.optBoolean(v, "c"));
    assertFalse(Vars.optBoolean(v, "missing"));
  }

  @Test
  void scenarioMetadataIsStrippedAndDateTokensResolved() {
    Map<String, Object> scenario = new LinkedHashMap<>();
    scenario.put("_description", "demo");
    scenario.put("preferredDate", "{{TODAY+3}}");
    scenario.put("pastDate", "{{TODAY-2}}");
    scenario.put("name", "x");
    LocalDate today = LocalDate.of(2026, 11, 2);
    Map<String, Object> vars = ScenarioFiles.processVariables(scenario, today);
    assertEquals(Map.of("preferredDate", "2026-11-05", "pastDate", "2026-10-31", "name", "x"), vars);
    assertEquals(Map.of("description", "demo"), ScenarioFiles.metadata(scenario));
  }
}
