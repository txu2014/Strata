/**
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.pricer.fx;

import java.time.LocalDate;

import com.opengamma.strata.basics.currency.MultiCurrencyAmount;
import com.opengamma.strata.basics.currency.Payment;
import com.opengamma.strata.collect.ArgChecker;
import com.opengamma.strata.market.sensitivity.PointSensitivities;
import com.opengamma.strata.market.sensitivity.ZeroRateSensitivity;
import com.opengamma.strata.pricer.rate.RatesProvider;
import com.opengamma.strata.product.fx.ExpandedFxSwap;
import com.opengamma.strata.product.fx.FxSingleProduct;
import com.opengamma.strata.product.fx.FxSwapProduct;

/**
 * Pricer for foreign exchange swap transaction products.
 * <p>
 * This function provides the ability to price an {@link FxSwapProduct}.
 */
public class DiscountingFxSwapProductPricer {

  /**
   * Default implementation.
   */
  public static final DiscountingFxSwapProductPricer DEFAULT =
      new DiscountingFxSwapProductPricer(DiscountingFxSingleProductPricer.DEFAULT);

  /**
   * Underlying single FX pricer.
   */
  private final DiscountingFxSingleProductPricer fxPricer;

  /**
   * Creates an instance.
   * 
   * @param fxPricer  the pricer for {@link FxSingleProduct}
   */
  public DiscountingFxSwapProductPricer(
      DiscountingFxSingleProductPricer fxPricer) {
    this.fxPricer = ArgChecker.notNull(fxPricer, "fxPricer");
  }

  //-------------------------------------------------------------------------
  /**
   * Calculates the present value of the FX swap product.
   * <p>
   * This discounts each payment on each leg in its own currency.
   * 
   * @param product  the product to price
   * @param provider  the rates provider
   * @return the present value in the two natural currencies
   */
  public MultiCurrencyAmount presentValue(FxSwapProduct product, RatesProvider provider) {
    ExpandedFxSwap fx = product.expand();
    MultiCurrencyAmount farPv = fxPricer.presentValue(fx.getFarLeg(), provider);
    MultiCurrencyAmount nearPv = fxPricer.presentValue(fx.getNearLeg(), provider);
    return nearPv.plus(farPv);
  }

  /**
   * Calculates the present value sensitivity of the FX swap product.
   * <p>
   * The present value sensitivity of the product is the sensitivity of the present value to
   * the underlying curves.
   * 
   * @param product  the product to price
   * @param provider  the rates provider
   * @return the present value sensitivity
   */
  public PointSensitivities presentValueSensitivity(FxSwapProduct product, RatesProvider provider) {
    ExpandedFxSwap fx = product.expand();
    PointSensitivities nearSens = fxPricer.presentValueSensitivity(fx.getNearLeg(), provider);
    PointSensitivities farSens = fxPricer.presentValueSensitivity(fx.getFarLeg(), provider);
    return nearSens.combinedWith(farSens);
  }

  //-------------------------------------------------------------------------
  /**
   * Calculates the currency exposure of the FX swap product.
   * <p>
   * This discounts each payment on each leg in its own currency.
   * 
   * @param product  the product to price
   * @param provider  the rates provider
   * @return the currency exposure
   */
  public MultiCurrencyAmount currencyExposure(FxSwapProduct product, RatesProvider provider) {
    return presentValue(product, provider);
  }

  /**
   * Calculates the par spread. 
   * <p>
   * The par spread is the spread that should be added to the FX forward points to have a zero value.
   * 
   * @param product  the product to price
   * @param provider  the rates provider
   * @return the spread
   */
  public double parSpread(FxSwapProduct product, RatesProvider provider) {
    ExpandedFxSwap fx = product.expand();
    Payment counterPaymentNear = fx.getNearLeg().getCounterCurrencyPayment();
    MultiCurrencyAmount pv = presentValue(fx, provider);
    double pvCounterCcy = pv.convertedTo(counterPaymentNear.getCurrency(), provider).getAmount();
    double dfEnd = provider.discountFactor(counterPaymentNear.getCurrency(), fx.getFarLeg().getPaymentDate());
    double notionalBaseCcy = fx.getNearLeg().getBaseCurrencyPayment().getAmount();
    return -pvCounterCcy / (notionalBaseCcy * dfEnd);
  }

  /**
   * Calculates the par spread sensitivity to the curves. 
   * <p>
   * The sensitivity is reported in the counter currency of the product, but is actually dimensionless.
   * 
   * @param product  the product to price
   * @param provider  the rates provider
   * @return the spread curve sensitivity
   */
  public PointSensitivities parSpreadSensitivity(FxSwapProduct product, RatesProvider provider) {
    ExpandedFxSwap fx = product.expand();
    Payment counterPaymentNear = fx.getNearLeg().getCounterCurrencyPayment();
    MultiCurrencyAmount pv = presentValue(fx, provider);
    double pvCounterCcy = pv.convertedTo(counterPaymentNear.getCurrency(), provider).getAmount();
    double dfEnd = provider.discountFactor(counterPaymentNear.getCurrency(), fx.getFarLeg().getPaymentDate());
    double notionalBaseCcy = fx.getNearLeg().getBaseCurrencyPayment().getAmount();
    double ps = -pvCounterCcy / (notionalBaseCcy * dfEnd);
    // backward sweep
    double psBar = 1d;
    double pvCounterCcyBar = -1d / (notionalBaseCcy * dfEnd) * psBar;
    double dfEndBar = -ps / dfEnd * psBar;
    ZeroRateSensitivity ddfEnddr = provider.discountFactors(counterPaymentNear.getCurrency())
        .zeroRatePointSensitivity(fx.getFarLeg().getPaymentDate());
    PointSensitivities result = ddfEnddr.multipliedBy(dfEndBar).build();
    PointSensitivities dpvdr = presentValueSensitivity(fx, provider);
    PointSensitivities dpvdrConverted = dpvdr.convertedTo(counterPaymentNear.getCurrency(), provider);
    return result.combinedWith(dpvdrConverted.multipliedBy(pvCounterCcyBar));
  }

  /**
   * Calculates the current cash of the FX swap product.
   * 
   * @param product  the product to price
   * @param valuationDate  the valuation date
   * @return the current cash
   */
  public MultiCurrencyAmount currentCash(FxSwapProduct product, LocalDate valuationDate) {
    ExpandedFxSwap fx = product.expand();
    MultiCurrencyAmount farPv = fxPricer.currentCash(fx.getFarLeg(), valuationDate);
    MultiCurrencyAmount nearPv = fxPricer.currentCash(fx.getNearLeg(), valuationDate);
    return nearPv.plus(farPv);
  }
}
