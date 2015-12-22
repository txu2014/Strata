/**
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.pricer.swaption;

import java.time.LocalDate;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.function.Function;

import com.opengamma.strata.basics.PutCall;
import com.opengamma.strata.basics.currency.CurrencyAmount;
import com.opengamma.strata.basics.currency.MultiCurrencyAmount;
import com.opengamma.strata.basics.date.DayCount;
import com.opengamma.strata.basics.value.ValueDerivatives;
import com.opengamma.strata.collect.ArgChecker;
import com.opengamma.strata.market.sensitivity.PointSensitivityBuilder;
import com.opengamma.strata.market.sensitivity.SwaptionSensitivity;
import com.opengamma.strata.pricer.impl.option.EuropeanVanillaOption;
import com.opengamma.strata.pricer.impl.option.NormalFunctionData;
import com.opengamma.strata.pricer.impl.option.NormalImpliedVolatilityFormula;
import com.opengamma.strata.pricer.impl.option.NormalPriceFunction;
import com.opengamma.strata.pricer.rate.RatesProvider;
import com.opengamma.strata.pricer.swap.DiscountingSwapProductPricer;
import com.opengamma.strata.product.rate.FixedRateObservation;
import com.opengamma.strata.product.rate.RateObservation;
import com.opengamma.strata.product.swap.ExpandedSwap;
import com.opengamma.strata.product.swap.ExpandedSwapLeg;
import com.opengamma.strata.product.swap.PaymentPeriod;
import com.opengamma.strata.product.swap.RatePaymentPeriod;
import com.opengamma.strata.product.swap.Swap;
import com.opengamma.strata.product.swap.SwapLegType;
import com.opengamma.strata.product.swap.SwapProduct;
import com.opengamma.strata.product.swaption.CashSettlement;
import com.opengamma.strata.product.swaption.CashSettlementMethod;
import com.opengamma.strata.product.swaption.ExpandedSwaption;
import com.opengamma.strata.product.swaption.SettlementType;
import com.opengamma.strata.product.swaption.SwaptionProduct;

/**
 * Pricer for swaption with par yield curve method of cash settlement in a normal model on the swap rate.
 * <p>
 * The swap underlying the swaption must have a fixed leg on which the forward rate is computed. The underlying swap
 * must be single currency.
 * <p>
 * The volatility parameters are not adjusted for the underlying swap convention.
 * The volatilities from the provider are taken as such.
 * <p>
 * The value of the swaption after expiry is 0. For a swaption which already expired, negative number is returned by 
 * the method, {@link NormalSwaptionVolatilities#relativeTime(ZonedDateTime)}.
 */
public class NormalSwaptionCashParYieldProductPricer {

  /**
   * Default implementation.
   */
  public static final NormalSwaptionCashParYieldProductPricer DEFAULT =
      new NormalSwaptionCashParYieldProductPricer(DiscountingSwapProductPricer.DEFAULT);

  /** 
   * Pricer for {@link SwapProduct}. 
   */
  private final DiscountingSwapProductPricer swapPricer;
  /** 
   * Normal model pricing function. 
   */
  public static final NormalPriceFunction NORMAL = new NormalPriceFunction();
  /**
   * The formula to infer the normal implied volatility from a price/present value.
   */
  private static final NormalImpliedVolatilityFormula FORMULA_IMPLIED = NormalImpliedVolatilityFormula.DEFAULT;

  /**
   * Creates an instance.
   * 
   * @param swapPricer  the pricer for {@link Swap}
   */
  public NormalSwaptionCashParYieldProductPricer(DiscountingSwapProductPricer swapPricer) {
    this.swapPricer = ArgChecker.notNull(swapPricer, "swapPricer");
  }

  //-------------------------------------------------------------------------
  /**
   * Calculates the present value of the swaption product.
   * <p>
   * The result is expressed using the currency of the swaption.
   * 
   * @param swaption  the product to price
   * @param ratesProvider  the rates provider
   * @param swaptionVolatilities  the volatilities
   * @return the present value of the swaption product
   */
  public CurrencyAmount presentValue(
      SwaptionProduct swaption,
      RatesProvider ratesProvider,
      NormalSwaptionVolatilities swaptionVolatilities) {

    ExpandedSwaption expanded = swaption.expand();
    validate(ratesProvider, expanded, swaptionVolatilities);
    ZonedDateTime expiryDateTime = expanded.getExpiryDateTime();
    double expiry = swaptionVolatilities.relativeTime(expiryDateTime);
    ExpandedSwap underlying = expanded.getUnderlying();
    ExpandedSwapLeg fixedLeg = fixedLeg(underlying);
    if (expiry < 0d) { // Option has expired already
      return CurrencyAmount.of(fixedLeg.getCurrency(), 0d);
    }
    double forward = swapPricer.parRate(underlying, ratesProvider);
    double annuityCash = swapPricer.getLegPricer().annuityCash(fixedLeg, forward);
    double discountSettle = ratesProvider.discountFactor(
        fixedLeg.getCurrency(), ((CashSettlement) expanded.getSwaptionSettlement()).getSettlementDate());
    double strike = getStrike(fixedLeg);
    double tenor = swaptionVolatilities.tenor(fixedLeg.getStartDate(), fixedLeg.getEndDate());
    double volatility = swaptionVolatilities.volatility(expiryDateTime, tenor, strike, forward);
    boolean isCall = fixedLeg.getPayReceive().isPay();
    NormalFunctionData normalData = NormalFunctionData.of(forward, Math.abs(annuityCash * discountSettle), volatility);
    EuropeanVanillaOption option = EuropeanVanillaOption.of(strike, expiry, isCall ? PutCall.CALL : PutCall.PUT);
    Function<NormalFunctionData, Double> func = NORMAL.getPriceFunction(option);
    double price = func.apply(normalData);
    return CurrencyAmount.of(fixedLeg.getCurrency(), price * expanded.getLongShort().sign());
  }

  //-------------------------------------------------------------------------
  /**
   * Computes the currency exposure of the swaption product.
   * 
   * @param swaption  the swaption to price
   * @param ratesProvider  the rates provider
   * @param swaptionVolatilities  the volatilities
   * @return the present value of the swaption product
   */
  public MultiCurrencyAmount currencyExposure(
      SwaptionProduct swaption,
      RatesProvider ratesProvider,
      NormalSwaptionVolatilities swaptionVolatilities) {

    return MultiCurrencyAmount.of(presentValue(swaption, ratesProvider, swaptionVolatilities));
  }

  //-------------------------------------------------------------------------
  /**
   * Computes the implied normal volatility of the swaption.
   * 
   * @param swaption  the product to price
   * @param ratesProvider  the rates provider
   * @param swaptionVolatilities  the volatilities
   * @return the normal implied volatility associated to the swaption
   */
  public double impliedVolatility(
      SwaptionProduct swaption,
      RatesProvider ratesProvider,
      NormalSwaptionVolatilities swaptionVolatilities) {

    ExpandedSwaption expanded = swaption.expand();
    validate(ratesProvider, expanded, swaptionVolatilities);
    ZonedDateTime expiryDateTime = expanded.getExpiryDateTime();
    double expiry = swaptionVolatilities.relativeTime(expiryDateTime);
    ExpandedSwap underlying = expanded.getUnderlying();
    ExpandedSwapLeg fixedLeg = fixedLeg(underlying);
    ArgChecker.isTrue(expiry >= 0d, "Option must be before expiry to compute an implied volatility");
    double forward = swapPricer.parRate(underlying, ratesProvider);
    double strike = getStrike(fixedLeg);
    double tenor = swaptionVolatilities.tenor(fixedLeg.getStartDate(), fixedLeg.getEndDate());
    return swaptionVolatilities.volatility(expiryDateTime, tenor, strike, forward);
  }

  /**
   * Computes the implied normal volatility from the present value of a swaption.
   * <p>
   * The guess volatility for the start of the root-finding process is 1%.
   * 
   * @param swaption  the product to price
   * @param ratesProvider  the rates provider
   * @param dayCount  the day-count used to estimate the time between valuation date and swaption expiry
   * @param presentValue  the present value of the swaption product
   * @return the implied volatility associated to the present value
   */
  public double impliedVolatilityFromPresentValue(
      SwaptionProduct swaption,
      RatesProvider ratesProvider,
      DayCount dayCount,
      double presentValue) {

    ExpandedSwaption expanded = swaption.expand();
    double sign = expanded.getLongShort().sign();
    ArgChecker.isTrue(presentValue * sign > 0, "Present value sign must be in line with the option Long/Short flag ");
    validateSwaption(ratesProvider, expanded);
    LocalDate valuationDate = ratesProvider.getValuationDate();
    LocalDate expiryDate = expanded.getExpiryDate();
    ArgChecker.isTrue(expiryDate.isAfter(valuationDate),
        "Expiry should be after valuation date to compute an implied volatility");
    double expiry = dayCount.yearFraction(valuationDate, expiryDate);
    ExpandedSwap underlying = expanded.getUnderlying();
    ExpandedSwapLeg fixedLeg = fixedLeg(underlying);
    double forward = swapPricer.parRate(underlying, ratesProvider);
    double annuityCash = swapPricer.getLegPricer().annuityCash(fixedLeg, forward);
    double discountSettle = ratesProvider.discountFactor(
        fixedLeg.getCurrency(), ((CashSettlement) expanded.getSwaptionSettlement()).getSettlementDate());
    double strike = getStrike(fixedLeg);
    NormalFunctionData normalData = NormalFunctionData.of(forward, Math.abs(annuityCash * discountSettle), 0.01);
    boolean isCall = fixedLeg.getPayReceive().isPay();
    // Payer at strike is exercise when rate > strike, i.e. call on rate
    EuropeanVanillaOption option = EuropeanVanillaOption.of(strike, expiry, isCall ? PutCall.CALL : PutCall.PUT);
    return FORMULA_IMPLIED.impliedVolatility(normalData, option, Math.abs(presentValue));
  }

  //-------------------------------------------------------------------------
  /**
   * Calculates the present value delta of the swaption product.
   * <p>
   * The present value delta is given by {@code discountFactor * annuityCash * normalDelta} where {@code normalDelta} 
   * is the first derivative of normal price with respect to forward. 
   * <p>
   * The result is expressed using the currency of the swaption.
   * 
   * @param swaption  the product to price
   * @param ratesProvider  the rates provider
   * @param swaptionVolatilities  the volatilities
   * @return the present value delta of the swaption product
   */
  public CurrencyAmount presentValueDelta(
      SwaptionProduct swaption,
      RatesProvider ratesProvider,
      NormalSwaptionVolatilities swaptionVolatilities) {

    ExpandedSwaption expanded = swaption.expand();
    validate(ratesProvider, expanded, swaptionVolatilities);
    ZonedDateTime expiryDateTime = expanded.getExpiryDateTime();
    double expiry = swaptionVolatilities.relativeTime(expiryDateTime);
    ExpandedSwap underlying = expanded.getUnderlying();
    ExpandedSwapLeg fixedLeg = fixedLeg(underlying);
    if (expiry < 0d) { // Option has expired already
      return CurrencyAmount.of(fixedLeg.getCurrency(), 0d);
    }
    double forward = swapPricer.parRate(underlying, ratesProvider);
    double annuityCash = swapPricer.getLegPricer().annuityCash(fixedLeg, forward);
    double discountSettle = ratesProvider.discountFactor(
        fixedLeg.getCurrency(), ((CashSettlement) expanded.getSwaptionSettlement()).getSettlementDate());
    double strike = getStrike(fixedLeg);
    double tenor = swaptionVolatilities.tenor(fixedLeg.getStartDate(), fixedLeg.getEndDate());
    double volatility = swaptionVolatilities.volatility(expiryDateTime, tenor, strike, forward);
    boolean isCall = fixedLeg.getPayReceive().isPay();
    NormalFunctionData normalData = NormalFunctionData.of(forward, Math.abs(annuityCash * discountSettle), volatility);
    EuropeanVanillaOption option = EuropeanVanillaOption.of(strike, expiry, isCall ? PutCall.CALL : PutCall.PUT);
    double delta = NORMAL.getDelta(option, normalData);
    return CurrencyAmount.of(fixedLeg.getCurrency(), delta * expanded.getLongShort().sign());
  }

  /**
   * Calculates the present value gamma of the swaption product.
   * <p>
   * The present value gamma is given by {@code discountFactor * annuityCash * normalGamma} where {@code normalGamma}
   * is the second derivative of normal price with respect to forward. 
   * <p>
   * The result is expressed using the currency of the swaption.
   * 
   * @param swaption  the product to price
   * @param ratesProvider  the rates provider
   * @param swaptionVolatilities  the volatilities
   * @return the present value gamma of the swaption product
   */
  public CurrencyAmount presentValueGamma(
      SwaptionProduct swaption,
      RatesProvider ratesProvider,
      NormalSwaptionVolatilities swaptionVolatilities) {

    ExpandedSwaption expanded = swaption.expand();
    validate(ratesProvider, expanded, swaptionVolatilities);
    ZonedDateTime expiryDateTime = expanded.getExpiryDateTime();
    double expiry = swaptionVolatilities.relativeTime(expiryDateTime);
    ExpandedSwap underlying = expanded.getUnderlying();
    ExpandedSwapLeg fixedLeg = fixedLeg(underlying);
    if (expiry < 0d) { // Option has expired already
      return CurrencyAmount.of(fixedLeg.getCurrency(), 0d);
    }
    double forward = swapPricer.parRate(underlying, ratesProvider);
    double annuityCash = swapPricer.getLegPricer().annuityCash(fixedLeg, forward);
    double discountSettle = ratesProvider.discountFactor(
        fixedLeg.getCurrency(), ((CashSettlement) expanded.getSwaptionSettlement()).getSettlementDate());
    double strike = getStrike(fixedLeg);
    double tenor = swaptionVolatilities.tenor(fixedLeg.getStartDate(), fixedLeg.getEndDate());
    double volatility = swaptionVolatilities.volatility(expiryDateTime, tenor, strike, forward);
    boolean isCall = fixedLeg.getPayReceive().isPay();
    NormalFunctionData normalData = NormalFunctionData.of(forward, Math.abs(annuityCash * discountSettle), volatility);
    EuropeanVanillaOption option = EuropeanVanillaOption.of(strike, expiry, isCall ? PutCall.CALL : PutCall.PUT);
    double gamma = NORMAL.getGamma(option, normalData);
    return CurrencyAmount.of(fixedLeg.getCurrency(), gamma * expanded.getLongShort().sign());
  }

  /**
   * Calculates the present value theta of the swaption product.
   * <p>
   * The present value theta is given by {@code discountFactor * annuityCash * normalTheta} where {@code normalTheta} 
   * is the minus of the normal price sensitivity to {@code timeToExpiry}. 
   * <p>
   * The result is expressed using the currency of the swaption.
   * 
   * @param swaption  the product to price
   * @param ratesProvider  the rates provider
   * @param swaptionVolatilities  the volatilities
   * @return the present value theta of the swaption product
   */
  public CurrencyAmount presentValueTheta(
      SwaptionProduct swaption,
      RatesProvider ratesProvider,
      NormalSwaptionVolatilities swaptionVolatilities) {

    ExpandedSwaption expanded = swaption.expand();
    validate(ratesProvider, expanded, swaptionVolatilities);
    ZonedDateTime expiryDateTime = expanded.getExpiryDateTime();
    double expiry = swaptionVolatilities.relativeTime(expiryDateTime);
    ExpandedSwap underlying = expanded.getUnderlying();
    ExpandedSwapLeg fixedLeg = fixedLeg(underlying);
    if (expiry < 0d) { // Option has expired already
      return CurrencyAmount.of(fixedLeg.getCurrency(), 0d);
    }
    double forward = swapPricer.parRate(underlying, ratesProvider);
    double annuityCash = swapPricer.getLegPricer().annuityCash(fixedLeg, forward);
    double discountSettle = ratesProvider.discountFactor(
        fixedLeg.getCurrency(), ((CashSettlement) expanded.getSwaptionSettlement()).getSettlementDate());
    double strike = getStrike(fixedLeg);
    double tenor = swaptionVolatilities.tenor(fixedLeg.getStartDate(), fixedLeg.getEndDate());
    double volatility = swaptionVolatilities.volatility(expiryDateTime, tenor, strike, forward);
    boolean isCall = fixedLeg.getPayReceive().isPay();
    NormalFunctionData normalData = NormalFunctionData.of(forward, Math.abs(annuityCash * discountSettle), volatility);
    EuropeanVanillaOption option = EuropeanVanillaOption.of(strike, expiry, isCall ? PutCall.CALL : PutCall.PUT);
    double theta = NORMAL.getTheta(option, normalData);
    return CurrencyAmount.of(fixedLeg.getCurrency(), theta * expanded.getLongShort().sign());
  }

  //-------------------------------------------------------------------------
  /**
   * Calculates the present value sensitivity of the swaption product.
   * <p>
   * The present value sensitivity of the product is the sensitivity of the present value to
   * the underlying curves.
   * 
   * @param swaption  the swaption product
   * @param ratesProvider  the rates provider
   * @param swaptionVolatilities  the volatilities
   * @return the present value curve sensitivity of the swap product
   */
  public PointSensitivityBuilder presentValueSensitivityStickyStrike(
      SwaptionProduct swaption,
      RatesProvider ratesProvider,
      NormalSwaptionVolatilities swaptionVolatilities) {

    ExpandedSwaption expanded = swaption.expand();
    validate(ratesProvider, expanded, swaptionVolatilities);
    ZonedDateTime expiryDateTime = expanded.getExpiryDateTime();
    double expiry = swaptionVolatilities.relativeTime(expiryDateTime);
    ExpandedSwap underlying = expanded.getUnderlying();
    ExpandedSwapLeg fixedLeg = fixedLeg(underlying);
    if (expiry < 0d) { // Option has expired already
      return PointSensitivityBuilder.none();
    }
    double forward = swapPricer.parRate(underlying, ratesProvider);
    double annuityCash = swapPricer.getLegPricer().annuityCash(fixedLeg, forward);
    double annuityCashDr = swapPricer.getLegPricer().annuityCashDerivative(fixedLeg, forward);
    LocalDate settlementDate = ((CashSettlement) expanded.getSwaptionSettlement()).getSettlementDate();
    double discountSettle = ratesProvider.discountFactor(fixedLeg.getCurrency(), settlementDate);
    double strike = getStrike(fixedLeg);
    double tenor = swaptionVolatilities.tenor(fixedLeg.getStartDate(), fixedLeg.getEndDate());
    double volatility = swaptionVolatilities.volatility(expiryDateTime, tenor, strike, forward);
    boolean isCall = fixedLeg.getPayReceive().isPay();
    NormalFunctionData normalData = NormalFunctionData.of(forward, 1d, volatility);
    EuropeanVanillaOption option = EuropeanVanillaOption.of(strike, expiry, isCall ? PutCall.CALL : PutCall.PUT);
    ValueDerivatives priceAdj = NORMAL.getPriceAdjoint(option, normalData);
    double price = priceAdj.getValue();
    double delta = priceAdj.getDerivative(0);
    PointSensitivityBuilder forwardSensi = swapPricer.parRateSensitivity(underlying, ratesProvider);
    PointSensitivityBuilder discountSettleSensi =
        ratesProvider.discountFactors(fixedLeg.getCurrency()).zeroRatePointSensitivity(settlementDate);
    double sign = expanded.getLongShort().sign();
    return forwardSensi.multipliedBy(sign * discountSettle * (annuityCash * delta + annuityCashDr * price))
        .combinedWith(discountSettleSensi.multipliedBy(sign * annuityCash * price));
  }

  //-------------------------------------------------------------------------
  /**
   * Calculates the present value sensitivity to the implied volatility of the swaption product.
   * <p>
   * The sensitivity to the normal volatility is also called normal vega.
   * 
   * @param swaption  the swaption product
   * @param ratesProvider  the rates provider
   * @param swaptionVolatilities  the volatilities
   * @return the point sensitivity to the normal volatility
   */
  public SwaptionSensitivity presentValueSensitivityNormalVolatility(
      SwaptionProduct swaption,
      RatesProvider ratesProvider,
      NormalSwaptionVolatilities swaptionVolatilities) {

    ExpandedSwaption expanded = swaption.expand();
    validate(ratesProvider, expanded, swaptionVolatilities);
    ZonedDateTime expiryDateTime = expanded.getExpiryDateTime();
    double expiry = swaptionVolatilities.relativeTime(expiryDateTime);
    ExpandedSwap underlying = expanded.getUnderlying();
    ExpandedSwapLeg fixedLeg = fixedLeg(underlying);
    double tenor = swaptionVolatilities.tenor(fixedLeg.getStartDate(), fixedLeg.getEndDate());
    double strike = getStrike(fixedLeg);
    if (expiry < 0d) { // Option has expired already
      return SwaptionSensitivity.of(
          swaptionVolatilities.getConvention(), expiryDateTime, tenor, strike, 0d, fixedLeg.getCurrency(), 0d);
    }
    double forward = swapPricer.parRate(underlying, ratesProvider);
    double annuityCash = swapPricer.getLegPricer().annuityCash(fixedLeg, forward);
    double discountSettle = ratesProvider.discountFactor(
        fixedLeg.getCurrency(), ((CashSettlement) expanded.getSwaptionSettlement()).getSettlementDate());
    double volatility = swaptionVolatilities.volatility(expiryDateTime, tenor, strike, forward);
    boolean isCall = fixedLeg.getPayReceive().isPay();
    NormalFunctionData normalData = NormalFunctionData.of(forward, Math.abs(annuityCash * discountSettle), volatility);
    EuropeanVanillaOption option = EuropeanVanillaOption.of(strike, expiry, isCall ? PutCall.CALL : PutCall.PUT);
    double vegaUnsigned = NORMAL.getVega(option, normalData);
    double vega = vegaUnsigned * expanded.getLongShort().sign();
    return SwaptionSensitivity.of(
        swaptionVolatilities.getConvention(), expiryDateTime, tenor, strike, forward, fixedLeg.getCurrency(), vega);
  }

  //-------------------------------------------------------------------------
  // check that one leg is fixed and return it
  private ExpandedSwapLeg fixedLeg(ExpandedSwap swap) {
    ArgChecker.isFalse(swap.isCrossCurrency(), "Swap must be single currency");
    // find fixed leg
    List<ExpandedSwapLeg> fixedLegs = swap.getLegs(SwapLegType.FIXED);
    if (fixedLegs.isEmpty()) {
      throw new IllegalArgumentException("Swap must contain a fixed leg");
    }
    return fixedLegs.get(0);
  }

  // get fixed rate 
  private double getStrike(ExpandedSwapLeg fixedLeg) {
    PaymentPeriod paymentPeriod = fixedLeg.getPaymentPeriods().get(0);
    ArgChecker.isTrue(paymentPeriod instanceof RatePaymentPeriod, "Payment period must be RatePaymentPeriod");
    RatePaymentPeriod ratePaymentPeriod = (RatePaymentPeriod) paymentPeriod;
    // compounding is caught when par rate is computed
    RateObservation rateObservation = ratePaymentPeriod.getAccrualPeriods().get(0).getRateObservation();
    ArgChecker.isTrue(rateObservation instanceof FixedRateObservation, "Swap leg must be fixed leg");
    return ((FixedRateObservation) rateObservation).getRate();
  }

  // validate that the rates and volatilities providers are coherent
  private void validate(
      RatesProvider ratesProvider,
      ExpandedSwaption swaption,
      NormalSwaptionVolatilities swaptionVolatilities) {

    ArgChecker.isTrue(swaptionVolatilities.getValuationDateTime().toLocalDate().equals(ratesProvider.getValuationDate()),
        "Volatility and rate data must be for the same date");
    ArgChecker.isFalse(swaption.getUnderlying().isCrossCurrency(), "Underlying swap must be single currency");
    ArgChecker.isTrue(swaption.getSwaptionSettlement().getSettlementType().equals(SettlementType.CASH),
        "Swaption must be cash settlement");
    CashSettlement cashSettle = (CashSettlement) swaption.getSwaptionSettlement();
    ArgChecker.isTrue(cashSettle.getCashSettlementMethod().equals(CashSettlementMethod.PAR_YIELD),
        "Cash settlement method must be par yield");
  }

  // validate that the rates and volatilities providers are coherent
  private void validateSwaption(RatesProvider ratesProvider, ExpandedSwaption swaption) {
    ArgChecker.isFalse(swaption.getUnderlying().isCrossCurrency(), "underlying swap should be single currency");
    ArgChecker.isTrue(swaption.getSwaptionSettlement().getSettlementType().equals(SettlementType.CASH),
        "swaption should be cash settlement");
    CashSettlement cashSettle = (CashSettlement) swaption.getSwaptionSettlement();
    ArgChecker.isTrue(cashSettle.getCashSettlementMethod().equals(CashSettlementMethod.PAR_YIELD),
        "cash settlement method should be par yield");
  }

}
