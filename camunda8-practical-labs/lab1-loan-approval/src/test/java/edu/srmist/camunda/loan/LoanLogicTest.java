package edu.srmist.camunda.loan;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import edu.srmist.camunda.common.worker.DataContractException;
import edu.srmist.camunda.loan.logic.EmiCalculator;
import edu.srmist.camunda.loan.logic.LoanApplicationValidator;
import edu.srmist.camunda.loan.logic.LoanOutcomes;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class LoanLogicTest {

  private static Map<String, Object> application() {
    Map<String, Object> v = new HashMap<>();
    v.put("applicationId", "LA-1001");
    v.put("applicantId", "SYN-1001");
    v.put("applicantName", "Asha Test");
    v.put("applicantAge", 34);
    v.put("email", "asha.test@example.com");
    v.put("employmentType", "SALARIED");
    v.put("employmentYears", 6);
    v.put("monthlyIncome", 120000);
    v.put("existingMonthlyEmi", 5000);
    v.put("loanAmount", 500000);
    v.put("loanTermMonths", 36);
    return v;
  }

  @Test
  void emiMatchesStandardFormula() {
    // 5,00,000 at 12% p.a. for 36 months = 16,607.15
    assertEquals(
        new BigDecimal("16607.15"), EmiCalculator.emi(new BigDecimal("500000"), new BigDecimal("12"), 36));
  }

  @Test
  void validApplicationProducesAffordabilityRatios() {
    Map<String, Object> out = LoanApplicationValidator.validate(application());
    assertEquals("VALID", out.get("validationStatus"));
    assertEquals(new BigDecimal("0.180"), out.get("dtiRatio"));
    assertEquals(new BigDecimal("0.347"), out.get("loanToIncomeRatio"));
  }

  @Test
  void businessRuleViolationsMakeApplicationInvalid() {
    Map<String, Object> v = application();
    v.put("applicantAge", 19);
    v.put("loanAmount", 6000000);
    Map<String, Object> out = LoanApplicationValidator.validate(v);
    assertEquals("INVALID", out.get("validationStatus"));
    assertEquals(2, ((List<?>) out.get("validationErrors")).size());
  }

  @Test
  void realLookingIdentifierIsRejected() {
    Map<String, Object> v = application();
    v.put("applicantId", "ABCDE1234F");
    assertEquals("INVALID", LoanApplicationValidator.validate(v).get("validationStatus"));
  }

  @Test
  void nonNumericIncomeIsADataContractViolation() {
    Map<String, Object> v = application();
    v.put("monthlyIncome", "45k");
    DataContractException e =
        assertThrows(DataContractException.class, () -> LoanApplicationValidator.validate(v));
    assertTrue(e.getMessage().contains("monthlyIncome"));
  }

  @Test
  void numericStringIncomeIsAccepted() {
    Map<String, Object> v = application();
    v.put("monthlyIncome", "45000");
    assertEquals("VALID", LoanApplicationValidator.validate(v).get("validationStatus"));
  }

  @Test
  void offerUsesUnderwriterAmountAndRiskBandRate() {
    Map<String, Object> v = application();
    v.put("eligibility", Map.of("decision", "MANUAL_REVIEW", "riskBand", "MEDIUM", "reason", "x"));
    v.put("reviewDecision", "APPROVE");
    v.put("approvedAmount", 400000);
    Map<String, Object> offer = LoanOutcomes.offer(v, LocalDate.of(2026, 11, 1));
    assertEquals("APPROVED", offer.get("finalOutcome"));
    assertEquals("UNDERWRITER", offer.get("decidedBy"));
    assertEquals(new BigDecimal("400000"), offer.get("sanctionedAmount"));
    assertEquals(new BigDecimal("12.50"), offer.get("offeredInterestRate"));
    assertEquals("2026-11-16", offer.get("offerValidUntil"));
  }

  @Test
  void rejectionExplainsWhy() {
    Map<String, Object> v = application();
    v.put("validationStatus", "VALID");
    v.put("eligibility", Map.of("decision", "REJECTED", "reason", "Credit score below 550"));
    Map<String, Object> out = LoanOutcomes.rejection(v);
    assertEquals("REJECTED", out.get("finalOutcome"));
    assertEquals("AUTOMATED_DMN", out.get("decidedBy"));
    assertEquals(List.of("Eligibility rules: Credit score below 550"), out.get("rejectionReasons"));
  }
}
