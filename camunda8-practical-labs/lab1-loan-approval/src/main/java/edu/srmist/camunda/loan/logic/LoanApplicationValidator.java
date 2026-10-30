package edu.srmist.camunda.loan.logic;

import edu.srmist.camunda.common.worker.Vars;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * Validates a loan application and derives the affordability figures used by the DMN decision.
 *
 * <p>Business-rule violations (age out of range, amount too high, ...) produce {@code
 * validationStatus = INVALID} and a list of messages – the process routes these to the rejection
 * path. Data-contract violations (a non-numeric income, a missing field the form guarantees) throw
 * {@link edu.srmist.camunda.common.worker.DataContractException}; the worker turns that into an
 * incident because no automatic retry can fix it.
 */
public final class LoanApplicationValidator {

  /** Educational assumption used for affordability: 12% p.a. reducing balance. */
  public static final BigDecimal ASSUMED_RATE_PERCENT = new BigDecimal("12.0");

  public static final BigDecimal MIN_LOAN = new BigDecimal("10000");
  public static final BigDecimal MAX_LOAN = new BigDecimal("5000000");
  public static final int MIN_TERM = 6;
  public static final int MAX_TERM = 84;
  public static final int MIN_AGE = 21;
  public static final int MAX_AGE = 65;

  private static final Pattern APPLICATION_ID = Pattern.compile("^LA-\\d{4}$");
  private static final Pattern SYNTHETIC_APPLICANT = Pattern.compile("^SYN-\\d{4}$");
  private static final Pattern EMAIL = Pattern.compile("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$");
  private static final Set<String> EMPLOYMENT = Set.of("SALARIED", "SELF_EMPLOYED", "UNEMPLOYED");

  private LoanApplicationValidator() {}

  public static Map<String, Object> validate(Map<String, Object> v) {
    List<String> errors = new ArrayList<>();

    String applicationId = Vars.optString(v, "applicationId");
    if (applicationId == null || !APPLICATION_ID.matcher(applicationId).matches()) {
      errors.add("applicationId must look like LA-0000");
    }
    String applicantId = Vars.optString(v, "applicantId");
    if (applicantId == null || !SYNTHETIC_APPLICANT.matcher(applicantId).matches()) {
      errors.add("applicantId must be a synthetic id like SYN-0000 (no real identifiers)");
    }
    if (Vars.optString(v, "applicantName") == null) {
      errors.add("applicantName is required");
    }
    String email = Vars.optString(v, "email");
    if (email == null || !EMAIL.matcher(email).matches()) {
      errors.add("email must be a valid address (use @example.com for test data)");
    }
    Integer age = Vars.optInt(v, "applicantAge");
    if (age == null || age < MIN_AGE || age > MAX_AGE) {
      errors.add("applicant age must be between " + MIN_AGE + " and " + MAX_AGE);
    }
    String employment = Vars.optString(v, "employmentType");
    if (employment == null || !EMPLOYMENT.contains(employment)) {
      errors.add("employmentType must be one of " + EMPLOYMENT);
    }
    Integer years = Vars.optInt(v, "employmentYears");
    if (years == null || years < 0) {
      errors.add("employmentYears must be zero or more");
    }

    // Numeric fields: a non-numeric value throws DataContractException (-> incident).
    BigDecimal income = Vars.requireNumber(v, "monthlyIncome");
    BigDecimal existingEmi = Vars.optNumber(v, "existingMonthlyEmi");
    if (existingEmi == null) {
      existingEmi = BigDecimal.ZERO;
    }
    BigDecimal amount = Vars.requireNumber(v, "loanAmount");
    Integer term = Vars.optInt(v, "loanTermMonths");

    if (income.signum() <= 0) {
      errors.add("monthlyIncome must be greater than zero");
    }
    if (existingEmi.signum() < 0) {
      errors.add("existingMonthlyEmi cannot be negative");
    }
    if (amount.compareTo(MIN_LOAN) < 0 || amount.compareTo(MAX_LOAN) > 0) {
      errors.add("loanAmount must be between " + MIN_LOAN + " and " + MAX_LOAN);
    }
    if (term == null || term < MIN_TERM || term > MAX_TERM) {
      errors.add("loanTermMonths must be between " + MIN_TERM + " and " + MAX_TERM);
    }

    Map<String, Object> out = new LinkedHashMap<>();
    out.put("validationStatus", errors.isEmpty() ? "VALID" : "INVALID");
    out.put("validationErrors", errors);
    if (errors.isEmpty()) {
      BigDecimal emi = EmiCalculator.emi(amount, ASSUMED_RATE_PERCENT, term);
      BigDecimal dti = existingEmi.add(emi).divide(income, 3, RoundingMode.HALF_UP);
      BigDecimal lti = amount.divide(income.multiply(BigDecimal.valueOf(12)), 3, RoundingMode.HALF_UP);
      out.put("proposedEmi", emi);
      out.put("dtiRatio", dti);
      out.put("loanToIncomeRatio", lti);
      out.put("assumedInterestRate", ASSUMED_RATE_PERCENT);
    }
    return out;
  }
}
