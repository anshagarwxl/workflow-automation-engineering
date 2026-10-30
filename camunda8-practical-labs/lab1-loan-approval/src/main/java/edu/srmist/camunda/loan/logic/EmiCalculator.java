package edu.srmist.camunda.loan.logic;

import java.math.BigDecimal;
import java.math.MathContext;
import java.math.RoundingMode;

/** Equated Monthly Instalment on a reducing balance: EMI = P*r*(1+r)^n / ((1+r)^n - 1). */
public final class EmiCalculator {

  private static final MathContext MC = MathContext.DECIMAL64;

  private EmiCalculator() {}

  public static BigDecimal emi(BigDecimal principal, BigDecimal annualRatePercent, int months) {
    if (months <= 0) {
      throw new IllegalArgumentException("months must be positive");
    }
    BigDecimal r = annualRatePercent.divide(BigDecimal.valueOf(1200), MC);
    if (r.signum() == 0) {
      return principal.divide(BigDecimal.valueOf(months), 2, RoundingMode.HALF_UP);
    }
    BigDecimal growth = BigDecimal.ONE.add(r).pow(months, MC);
    BigDecimal emi =
        principal.multiply(r, MC).multiply(growth, MC).divide(growth.subtract(BigDecimal.ONE), MC);
    return emi.setScale(2, RoundingMode.HALF_UP);
  }
}
