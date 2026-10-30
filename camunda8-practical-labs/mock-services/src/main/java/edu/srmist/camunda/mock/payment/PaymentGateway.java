package edu.srmist.camunda.mock.payment;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Synthetic payment gateway. It never accepts card numbers – only test tokens:
 *
 * <ul>
 *   <li>{@code tok_test_visa_success}, {@code tok_test_mastercard_success} – AUTHORISED
 *   <li>{@code tok_test_card_declined} – DECLINED (CARD_DECLINED)
 *   <li>{@code tok_test_insufficient_funds} – DECLINED (INSUFFICIENT_FUNDS)
 *   <li>any other token – DECLINED (INVALID_TOKEN)
 *   <li>amount above {@link #SINGLE_CHARGE_LIMIT} – DECLINED (AMOUNT_LIMIT_EXCEEDED)
 * </ul>
 *
 * Charges are idempotent per order id: retrying the same order returns the original charge, so
 * a retry after an outage can never charge a customer twice.
 */
public class PaymentGateway {

  public static final BigDecimal SINGLE_CHARGE_LIMIT = new BigDecimal("500000");

  public record ChargeRequest(
      String orderId, BigDecimal amount, String currency, String paymentToken, String customerRef) {}

  public record Charge(
      String chargeId,
      String orderId,
      String status,
      String declineReason,
      BigDecimal amount,
      String currency,
      String processedAt,
      boolean idempotentReplay) {}

  private final Map<String, Charge> chargesByOrder = new ConcurrentHashMap<>();
  private final AtomicLong sequence = new AtomicLong(70000);

  public Charge charge(ChargeRequest request) {
    if (request.orderId() == null || request.orderId().isBlank()) {
      throw new IllegalArgumentException("orderId is required");
    }
    if (request.amount() == null || request.amount().signum() <= 0) {
      throw new IllegalArgumentException("amount must be greater than zero");
    }
    Charge existing = chargesByOrder.get(request.orderId());
    if (existing != null) {
      return new Charge(
          existing.chargeId(),
          existing.orderId(),
          existing.status(),
          existing.declineReason(),
          existing.amount(),
          existing.currency(),
          existing.processedAt(),
          true);
    }
    String declineReason = declineReason(request);
    Charge charge =
        new Charge(
            "CHG-" + sequence.incrementAndGet(),
            request.orderId(),
            declineReason == null ? "AUTHORISED" : "DECLINED",
            declineReason,
            request.amount(),
            request.currency() == null ? "INR" : request.currency(),
            Instant.now().toString(),
            false);
    chargesByOrder.put(request.orderId(), charge);
    return charge;
  }

  static String declineReason(ChargeRequest request) {
    String token = request.paymentToken() == null ? "" : request.paymentToken();
    if (request.amount().compareTo(SINGLE_CHARGE_LIMIT) > 0) {
      return "AMOUNT_LIMIT_EXCEEDED";
    }
    return switch (token) {
      case "tok_test_visa_success", "tok_test_mastercard_success" -> null;
      case "tok_test_card_declined" -> "CARD_DECLINED";
      case "tok_test_insufficient_funds" -> "INSUFFICIENT_FUNDS";
      default -> "INVALID_TOKEN";
    };
  }
}
