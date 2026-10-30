package edu.srmist.camunda.order.logic;

import edu.srmist.camunda.common.worker.Vars;
import edu.srmist.camunda.order.logic.Inventory.Product;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.regex.Pattern;

/** Validates an order, checks the simulated inventory and calculates the order total. */
public final class OrderValidator {

  public static final BigDecimal GST_RATE = new BigDecimal("0.18");
  public static final BigDecimal FREE_DELIVERY_FROM = new BigDecimal("999");
  public static final int MAX_QUANTITY = 20;

  private static final Pattern ORDER_ID = Pattern.compile("^ORD-\\d{4}$");
  private static final Pattern CUSTOMER_REF = Pattern.compile("^CUST-\\d{4}$");
  private static final Pattern TEST_TOKEN = Pattern.compile("^tok_test_[a-z_]+$");
  private static final Pattern CARD_LIKE = Pattern.compile("\\d{12,19}");
  private static final Set<String> DELIVERY = Set.of("STANDARD", "EXPRESS", "SAME_DAY");
  private static final Set<String> CATEGORY = Set.of("NEW", "REGULAR", "PREMIUM");

  private OrderValidator() {}

  public static Map<String, Object> validateAndPrice(Map<String, Object> v, Inventory inventory) {
    List<String> errors = new ArrayList<>();

    String orderId = Vars.optString(v, "orderId");
    if (orderId == null || !ORDER_ID.matcher(orderId).matches()) {
      errors.add("orderId must look like ORD-0000");
    }
    String customerRef = Vars.optString(v, "customerRef");
    if (customerRef == null || !CUSTOMER_REF.matcher(customerRef).matches()) {
      errors.add("customerRef must look like CUST-0000");
    }
    String category = Vars.optString(v, "customerCategory");
    if (category == null || !CATEGORY.contains(category)) {
      errors.add("customerCategory must be one of " + CATEGORY);
    }
    String delivery = Vars.optString(v, "deliveryPreference");
    if (delivery == null || !DELIVERY.contains(delivery)) {
      errors.add("deliveryPreference must be one of " + DELIVERY);
    }
    String token = Vars.optString(v, "paymentToken");
    if (token == null) {
      errors.add("paymentToken is required");
    } else if (CARD_LIKE.matcher(token.replaceAll("[ -]", "")).find()) {
      errors.add("raw card numbers are not accepted - send a mock payment token (tok_test_...)");
    } else if (!TEST_TOKEN.matcher(token).matches()) {
      errors.add("paymentToken must be a mock token starting with tok_test_");
    }
    Integer quantity = Vars.optInt(v, "quantity");
    if (quantity == null || quantity < 1 || quantity > MAX_QUANTITY) {
      errors.add("quantity must be between 1 and " + MAX_QUANTITY);
    }
    String productId = Vars.optString(v, "productId");
    Optional<Product> product =
        productId == null ? Optional.empty() : inventory.product(productId);
    if (product.isEmpty()) {
      errors.add("unknown productId '" + productId + "'");
    }

    Map<String, Object> out = new LinkedHashMap<>();
    out.put("orderValid", errors.isEmpty());
    out.put("validationErrors", errors);
    if (!errors.isEmpty()) {
      out.put("stockStatus", "NOT_CHECKED");
      return out;
    }

    Product p = product.get();
    int available = inventory.available(p.productId());
    BigDecimal subtotal = p.unitPrice().multiply(BigDecimal.valueOf(quantity));
    BigDecimal tax = subtotal.multiply(GST_RATE).setScale(2, RoundingMode.HALF_UP);
    BigDecimal deliveryFee = deliveryFee(delivery, subtotal);
    BigDecimal total = subtotal.add(tax).add(deliveryFee).setScale(2, RoundingMode.HALF_UP);

    out.put("productName", p.name());
    out.put("unitPrice", p.unitPrice());
    out.put("availableQuantity", available);
    out.put("stockStatus", available >= quantity ? "IN_STOCK" : "INSUFFICIENT_STOCK");
    out.put("subtotal", subtotal.setScale(2, RoundingMode.HALF_UP));
    out.put("taxAmount", tax);
    out.put("deliveryFee", deliveryFee);
    out.put("orderTotal", total);
    return out;
  }

  static BigDecimal deliveryFee(String delivery, BigDecimal subtotal) {
    return switch (delivery) {
      case "SAME_DAY" -> new BigDecimal("299.00");
      case "EXPRESS" -> new BigDecimal("149.00");
      default -> subtotal.compareTo(FREE_DELIVERY_FROM) >= 0 ? new BigDecimal("0.00") : new BigDecimal("49.00");
    };
  }
}
