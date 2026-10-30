package edu.srmist.camunda.order.logic;

import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

/**
 * Simulated product catalogue and stock levels (in memory, reset on restart). Synthetic data.
 */
public class Inventory {

  public record Product(String productId, String name, BigDecimal unitPrice, String warehouse) {}

  private final Map<String, Product> products = new LinkedHashMap<>();
  private final Map<String, Integer> stock = new LinkedHashMap<>();

  public Inventory() {
    add("SKU-HEADPHONES", "Wireless Headphones", "2499", 25, "WH-CHENNAI");
    add("SKU-KEYBOARD", "Mechanical Keyboard", "3999", 40, "WH-CHENNAI");
    add("SKU-MONITOR", "27in Monitor", "18999", 8, "WH-BENGALURU");
    add("SKU-PHONE", "Smartphone X", "49999", 10, "WH-BENGALURU");
    add("SKU-GPU-RTX", "Graphics Card", "64999", 2, "WH-DELHI");
    add("SKU-LAPTOP-PRO", "Laptop Pro 14", "89999", 5, "WH-DELHI");
  }

  private void add(String id, String name, String price, int qty, String warehouse) {
    products.put(id, new Product(id, name, new BigDecimal(price), warehouse));
    stock.put(id, qty);
  }

  public Optional<Product> product(String productId) {
    return Optional.ofNullable(products.get(productId));
  }

  public synchronized int available(String productId) {
    return stock.getOrDefault(productId, 0);
  }

  /** Reserves stock for an order; returns false if there is not enough left. */
  public synchronized boolean reserve(String productId, int quantity) {
    int left = available(productId);
    if (left < quantity) {
      return false;
    }
    stock.put(productId, left - quantity);
    return true;
  }
}
