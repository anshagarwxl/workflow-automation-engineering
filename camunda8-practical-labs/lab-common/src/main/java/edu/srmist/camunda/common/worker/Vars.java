package edu.srmist.camunda.common.worker;

import java.math.BigDecimal;
import java.util.Map;

/**
 * Type-safe readers for process variables.
 *
 * <p>Process variables arrive as JSON and are deserialised into a {@code Map<String, Object>}.
 * These helpers turn missing or badly typed data into a {@link DataContractException} with a
 * message that is readable in Operate (instead of a raw ClassCastException / Jackson stack trace).
 */
public final class Vars {

  private Vars() {}

  public static String optString(Map<String, Object> vars, String key) {
    Object value = vars.get(key);
    if (value == null) {
      return null;
    }
    String s = String.valueOf(value).trim();
    return s.isEmpty() ? null : s;
  }

  public static String optString(Map<String, Object> vars, String key, String defaultValue) {
    String s = optString(vars, key);
    return s == null ? defaultValue : s;
  }

  /** Reads a number. Accepts JSON numbers or numeric strings ("45000"); rejects anything else. */
  public static BigDecimal optNumber(Map<String, Object> vars, String key) {
    Object value = vars.get(key);
    if (value == null) {
      return null;
    }
    if (value instanceof Number n) {
      return new BigDecimal(n.toString());
    }
    String s = String.valueOf(value).trim();
    if (s.isEmpty()) {
      return null;
    }
    try {
      return new BigDecimal(s);
    } catch (NumberFormatException e) {
      throw new DataContractException(
          "Process variable '" + key + "' must be numeric but was '" + s + "'");
    }
  }

  public static BigDecimal requireNumber(Map<String, Object> vars, String key) {
    BigDecimal n = optNumber(vars, key);
    if (n == null) {
      throw new DataContractException("Required process variable '" + key + "' is missing");
    }
    return n;
  }

  public static Integer optInt(Map<String, Object> vars, String key) {
    BigDecimal n = optNumber(vars, key);
    if (n == null) {
      return null;
    }
    try {
      return n.intValueExact();
    } catch (ArithmeticException e) {
      throw new DataContractException(
          "Process variable '" + key + "' must be a whole number but was '" + n + "'");
    }
  }

  /** Reads a boolean. Accepts JSON booleans or the strings "true"/"false"/"yes"/"no". */
  public static boolean optBoolean(Map<String, Object> vars, String key) {
    Object value = vars.get(key);
    if (value == null) {
      return false;
    }
    if (value instanceof Boolean b) {
      return b;
    }
    String s = String.valueOf(value).trim().toLowerCase();
    return switch (s) {
      case "true", "yes", "y", "1" -> true;
      case "false", "no", "n", "0", "" -> false;
      default ->
          throw new DataContractException(
              "Process variable '" + key + "' must be true/false but was '" + value + "'");
    };
  }
}
