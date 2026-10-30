package edu.srmist.camunda.order.logic;

import edu.srmist.camunda.common.worker.Vars;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.LinkedHashMap;
import java.util.Map;

/** Fulfilment and customer notification logic. */
public final class OrderOutcomes {

  private OrderOutcomes() {}

  /** Maps the notification type (set by the BPMN input mapping) to the final order outcome. */
  public static String outcomeFor(String notificationType) {
    if (notificationType == null) {
      return "UNKNOWN";
    }
    return switch (notificationType) {
      case "ORDER_CONFIRMED" -> "FULFILLED";
      case "ORDER_REJECTED" -> "REJECTED_INVALID";
      case "OUT_OF_STOCK" -> "OUT_OF_STOCK";
      case "VERIFICATION_REJECTED" -> "CANCELLED_AFTER_REVIEW";
      case "PAYMENT_DECLINED" -> "PAYMENT_DECLINED";
      default -> "UNKNOWN";
    };
  }

  public static Map<String, Object> notification(Map<String, Object> v) {
    String type = Vars.optString(v, "notificationType");
    String orderId = Vars.optString(v, "orderId", "ORD-0000");
    String message =
        switch (type == null ? "" : type) {
          case "ORDER_CONFIRMED" ->
              "Order " + orderId + " confirmed. Shipment " + Vars.optString(v, "shipmentId", "-")
                  + " will be dispatched by " + Vars.optString(v, "dispatchBy", "-") + ".";
          case "ORDER_REJECTED" -> "Order " + orderId + " could not be accepted: " + v.get("validationErrors");
          case "OUT_OF_STOCK" ->
              "Sorry, only " + v.get("availableQuantity") + " unit(s) of " + v.get("productName")
                  + " are in stock. Order " + orderId + " was not fulfilled.";
          case "VERIFICATION_REJECTED" ->
              "Order " + orderId + " was cancelled after manual verification.";
          case "PAYMENT_DECLINED" ->
              "Payment for order " + orderId + " was declined (" + Vars.optString(v, "paymentDeclineReason", "-") + ").";
          default -> "Order " + orderId + " update.";
        };
    Map<String, Object> out = new LinkedHashMap<>();
    out.put("finalOutcome", outcomeFor(type));
    out.put("customerMessage", message);
    out.put("notificationStatus", "SIMULATED_EMAIL_SENT");
    return out;
  }

  /** Reserves stock and plans the shipment. Returns null if the stock is gone in the meantime. */
  public static Map<String, Object> fulfil(Map<String, Object> v, Inventory inventory, Instant now) {
    String productId = Vars.optString(v, "productId");
    Integer quantity = Vars.optInt(v, "quantity");
    if (!inventory.reserve(productId, quantity == null ? 0 : quantity)) {
      return null;
    }
    Integer slaHours = null;
    Object decision = v.get("priorityDecision");
    if (decision instanceof Map<?, ?> m && m.get("fulfilmentSlaHours") instanceof Number n) {
      slaHours = n.intValue();
    }
    int hours = slaHours == null ? 72 : slaHours;
    String priority =
        decision instanceof Map<?, ?> m2 && m2.get("priority") != null
            ? String.valueOf(m2.get("priority"))
            : "LOW";
    String orderId = Vars.optString(v, "orderId", "ORD-0000");
    String warehouse =
        inventory.product(productId).map(Inventory.Product::warehouse).orElse("WH-CENTRAL");

    Map<String, Object> out = new LinkedHashMap<>();
    out.put("fulfilmentStatus", "DISPATCH_SCHEDULED");
    out.put("shipmentId", "SHP-" + orderId.replace("ORD-", ""));
    out.put("warehouse", warehouse);
    out.put("fulfilmentPriority", priority);
    out.put("dispatchBy", now.plus(hours, ChronoUnit.HOURS).truncatedTo(ChronoUnit.MINUTES).toString());
    out.put("remainingStock", inventory.available(productId));
    return out;
  }
}
