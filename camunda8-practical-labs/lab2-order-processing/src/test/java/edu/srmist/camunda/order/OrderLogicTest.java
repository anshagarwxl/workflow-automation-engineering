package edu.srmist.camunda.order;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import edu.srmist.camunda.order.logic.Inventory;
import edu.srmist.camunda.order.logic.OrderOutcomes;
import edu.srmist.camunda.order.logic.OrderValidator;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class OrderLogicTest {

  private static Map<String, Object> order(String sku, int qty, String delivery, String token) {
    Map<String, Object> v = new HashMap<>();
    v.put("orderId", "ORD-2001");
    v.put("customerRef", "CUST-2001");
    v.put("customerCategory", "REGULAR");
    v.put("productId", sku);
    v.put("quantity", qty);
    v.put("deliveryPreference", delivery);
    v.put("paymentToken", token);
    return v;
  }

  @Test
  void pricesAnInStockOrderWithGstAndFreeDelivery() {
    Map<String, Object> out =
        OrderValidator.validateAndPrice(
            order("SKU-HEADPHONES", 2, "STANDARD", "tok_test_visa_success"), new Inventory());
    assertEquals(true, out.get("orderValid"));
    assertEquals("IN_STOCK", out.get("stockStatus"));
    assertEquals(new BigDecimal("899.64"), out.get("taxAmount"));
    assertEquals(new BigDecimal("0.00"), out.get("deliveryFee"));
    assertEquals(new BigDecimal("5897.64"), out.get("orderTotal"));
  }

  @Test
  void highValueOrderTotal() {
    Map<String, Object> out =
        OrderValidator.validateAndPrice(
            order("SKU-LAPTOP-PRO", 2, "STANDARD", "tok_test_mastercard_success"), new Inventory());
    assertEquals(new BigDecimal("212397.64"), out.get("orderTotal"));
  }

  @Test
  void detectsInsufficientStock() {
    Map<String, Object> out =
        OrderValidator.validateAndPrice(
            order("SKU-GPU-RTX", 5, "STANDARD", "tok_test_visa_success"), new Inventory());
    assertEquals("INSUFFICIENT_STOCK", out.get("stockStatus"));
    assertEquals(2, out.get("availableQuantity"));
  }

  @Test
  void rejectsRawCardNumbersAndBadQuantity() {
    Map<String, Object> out =
        OrderValidator.validateAndPrice(
            order("SKU-HEADPHONES", 0, "STANDARD", "4111 1111 1111 1111"), new Inventory());
    assertEquals(false, out.get("orderValid"));
    List<?> errors = (List<?>) out.get("validationErrors");
    assertEquals(2, errors.size());
    assertTrue(errors.toString().contains("raw card numbers"));
  }

  @Test
  void fulfilmentReservesStockAndUsesSla() {
    Inventory inventory = new Inventory();
    Map<String, Object> v = order("SKU-MONITOR", 1, "SAME_DAY", "tok_test_visa_success");
    v.put("priorityDecision", Map.of("priority", "CRITICAL", "fulfilmentSlaHours", 6));
    Map<String, Object> out =
        OrderOutcomes.fulfil(v, inventory, Instant.parse("2026-11-01T10:00:00Z"));
    assertNotNull(out);
    assertEquals("2026-11-01T16:00:00Z", out.get("dispatchBy"));
    assertEquals(7, out.get("remainingStock"));

    Map<String, Object> tooMany = order("SKU-GPU-RTX", 3, "STANDARD", "tok_test_visa_success");
    assertNull(OrderOutcomes.fulfil(tooMany, inventory, Instant.now()));
  }

  @Test
  void notificationTypeMapsToFinalOutcome() {
    Map<String, Object> v = order("SKU-KEYBOARD", 1, "EXPRESS", "tok_test_card_declined");
    v.put("notificationType", "PAYMENT_DECLINED");
    v.put("paymentDeclineReason", "CARD_DECLINED");
    Map<String, Object> out = OrderOutcomes.notification(v);
    assertEquals("PAYMENT_DECLINED", out.get("finalOutcome"));
    assertTrue(out.get("customerMessage").toString().contains("CARD_DECLINED"));
  }
}
