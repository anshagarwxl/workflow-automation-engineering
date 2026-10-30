package edu.srmist.camunda.loan.logic;

import edu.srmist.camunda.common.worker.Vars;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Builds the final offer / rejection notice from the process variables. */
public final class LoanOutcomes {

  private LoanOutcomes() {}

  /** Educational pricing grid: interest rate by risk band. */
  static BigDecimal rateFor(String riskBand) {
    if (riskBand == null) {
      return new BigDecimal("13.50");
    }
    return switch (riskBand) {
      case "LOW" -> new BigDecimal("10.50");
      case "MEDIUM" -> new BigDecimal("12.50");
      default -> new BigDecimal("14.50");
    };
  }

  @SuppressWarnings("unchecked")
  static String eligibility(Map<String, Object> v, String field) {
    Object e = v.get("eligibility");
    if (e instanceof Map<?, ?> m && m.get(field) != null) {
      return String.valueOf(((Map<String, Object>) m).get(field));
    }
    return null;
  }

  static String decidedBy(Map<String, Object> v) {
    return Vars.optString(v, "reviewDecision") != null ? "UNDERWRITER" : "AUTOMATED_DMN";
  }

  public static Map<String, Object> offer(Map<String, Object> v, LocalDate today) {
    String applicationId = Vars.optString(v, "applicationId", "LA-0000");
    BigDecimal requested = Vars.requireNumber(v, "loanAmount");
    BigDecimal approved = Vars.optNumber(v, "approvedAmount");
    BigDecimal sanctioned =
        approved != null && approved.signum() > 0 && approved.compareTo(requested) <= 0
            ? approved
            : requested;
    Integer term = Vars.optInt(v, "loanTermMonths");
    String riskBand = eligibility(v, "riskBand");
    BigDecimal rate = rateFor(riskBand);

    Map<String, Object> out = new LinkedHashMap<>();
    out.put("finalOutcome", "APPROVED");
    out.put("decidedBy", decidedBy(v));
    out.put("offerId", "OFR-" + applicationId.replace("LA-", ""));
    out.put("sanctionedAmount", sanctioned);
    out.put("offeredInterestRate", rate);
    out.put("offeredEmi", EmiCalculator.emi(sanctioned, rate, term == null ? 12 : term));
    out.put("offerValidUntil", today.plusDays(15).toString());
    out.put("notificationStatus", "SIMULATED_EMAIL_SENT");
    return out;
  }

  @SuppressWarnings("unchecked")
  public static Map<String, Object> rejection(Map<String, Object> v) {
    List<String> reasons = new ArrayList<>();
    if ("INVALID".equals(Vars.optString(v, "validationStatus"))) {
      Object errors = v.get("validationErrors");
      if (errors instanceof List<?> list) {
        list.forEach(e -> reasons.add("Validation: " + e));
      }
    } else if ("REJECT".equals(Vars.optString(v, "reviewDecision"))) {
      reasons.add("Underwriter: " + Vars.optString(v, "reviewerComments", "no comment"));
    } else {
      String reason = eligibility(v, "reason");
      reasons.add("Eligibility rules: " + (reason == null ? "not eligible" : reason));
    }
    String applicationId = Vars.optString(v, "applicationId", "LA-0000");
    Map<String, Object> out = new LinkedHashMap<>();
    out.put("finalOutcome", "REJECTED");
    out.put(
        "decidedBy",
        "INVALID".equals(Vars.optString(v, "validationStatus")) ? "VALIDATION" : decidedBy(v));
    out.put("rejectionReasons", reasons);
    out.put("noticeId", "REJ-" + applicationId.replace("LA-", ""));
    out.put("notificationStatus", "SIMULATED_EMAIL_SENT");
    return out;
  }
}
