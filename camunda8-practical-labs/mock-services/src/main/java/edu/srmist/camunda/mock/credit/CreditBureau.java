package edu.srmist.camunda.mock.credit;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Synthetic credit bureau. Bureau records exist for a fixed set of synthetic applicant ids; any
 * other id is treated as "no credit history". Two ids trigger technical failures for testing:
 *
 * <ul>
 *   <li>{@code SYN-9408} – the bureau does not answer in time (read timeout in the connector)
 *   <li>{@code SYN-9500} – the bureau returns HTTP 500
 * </ul>
 */
public class CreditBureau {

  public static final String TIMEOUT_APPLICANT = "SYN-9408";
  public static final String ERROR_APPLICANT = "SYN-9500";

  /** Synthetic bureau record. */
  public record BureauRecord(int creditScore, int activeLoans, int defaultsLast24Months) {}

  public record AssessmentRequest(
      String applicationId,
      String applicantId,
      BigDecimal monthlyIncome,
      BigDecimal existingMonthlyEmi,
      BigDecimal loanAmount) {}

  public record Assessment(
      String assessmentId,
      String applicationId,
      String applicantId,
      String bureauStatus,
      int creditScore,
      String riskGrade,
      int activeLoans,
      int defaultsLast24Months,
      BigDecimal existingObligationRatio,
      String assessedAt) {}

  private static final Map<String, BureauRecord> RECORDS =
      Map.of(
          "SYN-1001", new BureauRecord(780, 1, 0),
          "SYN-1002", new BureauRecord(520, 3, 1),
          "SYN-1003", new BureauRecord(690, 2, 0),
          "SYN-1004", new BureauRecord(640, 2, 2),
          "SYN-1005", new BureauRecord(750, 0, 0),
          "SYN-1006", new BureauRecord(735, 1, 0));

  /** Score used when the bureau has no record for an applicant. */
  public static final int NO_HISTORY_SCORE = 600;

  private final AtomicLong sequence = new AtomicLong(5000);

  public Assessment assess(AssessmentRequest request) {
    if (request.applicantId() == null || request.applicantId().isBlank()) {
      throw new IllegalArgumentException("applicantId is required");
    }
    BureauRecord record = RECORDS.get(request.applicantId());
    String status = record == null ? "NO_HISTORY" : "FOUND";
    if (record == null) {
      record = new BureauRecord(NO_HISTORY_SCORE, 0, 0);
    }
    BigDecimal obligationRatio = BigDecimal.ZERO;
    if (request.monthlyIncome() != null
        && request.monthlyIncome().signum() > 0
        && request.existingMonthlyEmi() != null) {
      obligationRatio =
          request.existingMonthlyEmi().divide(request.monthlyIncome(), 3, RoundingMode.HALF_UP);
    }
    return new Assessment(
        "CBA-" + sequence.incrementAndGet(),
        request.applicationId(),
        request.applicantId(),
        status,
        record.creditScore(),
        grade(record.creditScore()),
        record.activeLoans(),
        record.defaultsLast24Months(),
        obligationRatio,
        Instant.now().toString());
  }

  static String grade(int score) {
    if (score >= 750) return "A";
    if (score >= 700) return "B";
    if (score >= 650) return "C";
    if (score >= 550) return "D";
    return "E";
  }
}
