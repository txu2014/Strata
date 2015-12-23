/**
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.pricer.swaption;

import java.time.LocalDate;
import java.time.ZonedDateTime;
import java.util.List;

import com.opengamma.strata.basics.PutCall;
import com.opengamma.strata.basics.currency.CurrencyAmount;
import com.opengamma.strata.basics.currency.MultiCurrencyAmount;
import com.opengamma.strata.collect.ArgChecker;
import com.opengamma.strata.market.sensitivity.PointSensitivityBuilder;
import com.opengamma.strata.market.sensitivity.SwaptionSensitivity;
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
 * Pricer for swaption with par yield curve method of cash settlement based on volatilities.
 * <p>
 * The swap underlying the swaption must have a fixed leg on which the forward rate is computed.
 * The underlying swap must be single currency.
 * <p>
 * The volatility parameters are not adjusted for the underlying swap convention.
 * <p>
 * The value of the swaption after expiry is 0.
 * For a swaption which already expired, negative number is returned by 
 * {@link SwaptionVolatilities#relativeTime(ZonedDateTime)}.
 */
public class VolatilitySwaptionCashParYieldProductPricer {

  /**
   * Default implementation.
   */
  public static final VolatilitySwaptionCashParYieldProductPricer DEFAULT =
      new VolatilitySwaptionCashParYieldProductPricer(DiscountingSwapProductPricer.DEFAULT);
  /** 
   * Pricer for {@link SwapProduct}. 
   */
  private final DiscountingSwapProductPricer swapPricer;

  /**
   * Creates an instance.
   * 
   * @param swapPricer  the pricer for {@link Swap}
   */
  public VolatilitySwaptionCashParYieldProductPricer(DiscountingSwapProductPricer swapPricer) {
    this.swapPricer = ArgChecker.notNull(swapPricer, "swapPricer");
  }

  //-------------------------------------------------------------------------
  /**
   * Gets the swap pricer.
   * 
   * @return the swap pricer
   */
  protected DiscountingSwapProductPricer getSwapPricer() {
    return swapPricer;
  }

  //-------------------------------------------------------------------------
  /**
   * Calculates the present value of the swaption.
   * <p>
   * The result is expressed using the currency of the swaption.
   * 
   * @param swaption  the swaption
   * @param ratesProvider  the rates provider
   * @param swaptionVolatilities  the volatilities
   * @return the present value of the swaption
   */
  public CurrencyAmount presentValue(
      SwaptionProduct swaption,
      RatesProvider ratesProvider,
      SwaptionVolatilities swaptionVolatilities) {

    ExpandedSwaption expanded = swaption.expand();
    validate(expanded, ratesProvider, swaptionVolatilities);
    double expiry = swaptionVolatilities.relativeTime(expanded.getExpiryDateTime());
    ExpandedSwap underlying = expanded.getUnderlying();
    ExpandedSwapLeg fixedLeg = fixedLeg(underlying);
    if (expiry < 0d) { // Option has expired already
      return CurrencyAmount.of(fixedLeg.getCurrency(), 0d);
    }
    double forward = swapPricer.parRate(underlying, ratesProvider);
    double numeraire = calculateNumeraire(expanded, fixedLeg, forward, ratesProvider);
    double strike = calculateStrike(fixedLeg);
    double tenor = swaptionVolatilities.tenor(fixedLeg.getStartDate(), fixedLeg.getEndDate());
    double volatility = swaptionVolatilities.volatility(expiry, tenor, strike, forward);
    PutCall putCall = PutCall.ofPut(fixedLeg.getPayReceive().isReceive());
    double price = numeraire * swaptionVolatilities.price(expiry, putCall, strike, forward, volatility);
    return CurrencyAmount.of(fixedLeg.getCurrency(), price * expanded.getLongShort().sign());
  }

  //-------------------------------------------------------------------------
  /**
   * Computes the currency exposure of the swaption.
   * <p>
   * This is equivalent to the present value of the swaption.
   * 
   * @param swaption  the swaption
   * @param ratesProvider  the rates provider
   * @param swaptionVolatilities  the volatilities
   * @return the present value of the swaption
   */
  public MultiCurrencyAmount currencyExposure(
      SwaptionProduct swaption,
      RatesProvider ratesProvider,
      SwaptionVolatilities swaptionVolatilities) {

    return MultiCurrencyAmount.of(presentValue(swaption, ratesProvider, swaptionVolatilities));
  }

  //-------------------------------------------------------------------------
  /**
   * Computes the implied volatility of the swaption.
   * 
   * @param swaption  the swaption
   * @param ratesProvider  the rates provider
   * @param swaptionVolatilities  the volatilities
   * @return the implied volatility associated with the swaption
   */
  public double impliedVolatility(
      SwaptionProduct swaption,
      RatesProvider ratesProvider,
      SwaptionVolatilities swaptionVolatilities) {

    ExpandedSwaption expanded = swaption.expand();
    validate(expanded, ratesProvider, swaptionVolatilities);
    double expiry = swaptionVolatilities.relativeTime(expanded.getExpiryDateTime());
    ExpandedSwap underlying = expanded.getUnderlying();
    ExpandedSwapLeg fixedLeg = fixedLeg(underlying);
    ArgChecker.isTrue(expiry >= 0d, "Option must be before expiry to compute an implied volatility");
    double forward = getSwapPricer().parRate(underlying, ratesProvider);
    double strike = calculateStrike(fixedLeg);
    double tenor = swaptionVolatilities.tenor(fixedLeg.getStartDate(), fixedLeg.getEndDate());
    return swaptionVolatilities.volatility(expiry, tenor, strike, forward);
  }

  //-------------------------------------------------------------------------
  /**
   * Calculates the present value delta of the swaption.
   * <p>
   * The present value delta is given by {@code pvbp * priceDelta} where {@code priceDelta}
   * is the first derivative of the price with respect to forward.
   * <p>
   * The result is expressed using the currency of the swaption.
   * 
   * @param swaption  the swaption
   * @param ratesProvider  the rates provider
   * @param swaptionVolatilities  the volatilities
   * @return the present value delta of the swaption
   */
  public CurrencyAmount presentValueDelta(
      SwaptionProduct swaption,
      RatesProvider ratesProvider,
      SwaptionVolatilities swaptionVolatilities) {

    ExpandedSwaption expanded = swaption.expand();
    validate(expanded, ratesProvider, swaptionVolatilities);
    double expiry = swaptionVolatilities.relativeTime(expanded.getExpiryDateTime());
    ExpandedSwap underlying = expanded.getUnderlying();
    ExpandedSwapLeg fixedLeg = fixedLeg(underlying);
    if (expiry < 0d) { // Option has expired already
      return CurrencyAmount.of(fixedLeg.getCurrency(), 0d);
    }
    double forward = getSwapPricer().parRate(underlying, ratesProvider);
    double numeraire = calculateNumeraire(expanded, fixedLeg, forward, ratesProvider);
    double strike = calculateStrike(fixedLeg);
    double tenor = swaptionVolatilities.tenor(fixedLeg.getStartDate(), fixedLeg.getEndDate());
    double volatility = swaptionVolatilities.volatility(expiry, tenor, strike, forward);
    PutCall putCall = PutCall.ofPut(fixedLeg.getPayReceive().isReceive());
    double delta = numeraire * swaptionVolatilities.priceDelta(expiry, putCall, strike, forward, volatility);
    return CurrencyAmount.of(fixedLeg.getCurrency(), delta * expanded.getLongShort().sign());
  }

  //-------------------------------------------------------------------------
  /**
   * Calculates the present value gamma of the swaption.
   * <p>
   * The present value gamma is given by {@code pvbp * priceGamma} where {@code priceGamma}
   * is the second derivative of the price with respect to forward.
   * <p>
   * The result is expressed using the currency of the swaption.
   * 
   * @param swaption  the swaption
   * @param ratesProvider  the rates provider
   * @param swaptionVolatilities  the volatilities
   * @return the present value gamma of the swaption
   */
  public CurrencyAmount presentValueGamma(
      SwaptionProduct swaption,
      RatesProvider ratesProvider,
      SwaptionVolatilities swaptionVolatilities) {

    ExpandedSwaption expanded = swaption.expand();
    validate(expanded, ratesProvider, swaptionVolatilities);
    double expiry = swaptionVolatilities.relativeTime(expanded.getExpiryDateTime());
    ExpandedSwap underlying = expanded.getUnderlying();
    ExpandedSwapLeg fixedLeg = fixedLeg(underlying);
    if (expiry < 0d) { // Option has expired already
      return CurrencyAmount.of(fixedLeg.getCurrency(), 0d);
    }
    double forward = getSwapPricer().parRate(underlying, ratesProvider);
    double numeraire = calculateNumeraire(expanded, fixedLeg, forward, ratesProvider);
    double strike = calculateStrike(fixedLeg);
    double tenor = swaptionVolatilities.tenor(fixedLeg.getStartDate(), fixedLeg.getEndDate());
    double volatility = swaptionVolatilities.volatility(expiry, tenor, strike, forward);
    PutCall putCall = PutCall.ofPut(fixedLeg.getPayReceive().isReceive());
    double gamma = numeraire * swaptionVolatilities.priceGamma(expiry, putCall, strike, forward, volatility);
    return CurrencyAmount.of(fixedLeg.getCurrency(), gamma * expanded.getLongShort().sign());
  }

  //-------------------------------------------------------------------------
  /**
   * Calculates the present value of the swaption.
   * <p>
   * The present value theta is given by {@code pvbp * priceTheta} where {@code priceTheta}
   * is the minus of the price sensitivity to {@code timeToExpiry}.
   * <p>
   * The result is expressed using the currency of the swaption.
   * 
   * @param swaption  the swaption
   * @param ratesProvider  the rates provider
   * @param swaptionVolatilities  the volatilities
   * @return the present value theta of the swaption
   */
  public CurrencyAmount presentValueTheta(
      SwaptionProduct swaption,
      RatesProvider ratesProvider,
      SwaptionVolatilities swaptionVolatilities) {

    ExpandedSwaption expanded = swaption.expand();
    validate(expanded, ratesProvider, swaptionVolatilities);
    double expiry = swaptionVolatilities.relativeTime(expanded.getExpiryDateTime());
    ExpandedSwap underlying = expanded.getUnderlying();
    ExpandedSwapLeg fixedLeg = fixedLeg(underlying);
    if (expiry < 0d) { // Option has expired already
      return CurrencyAmount.of(fixedLeg.getCurrency(), 0d);
    }
    double forward = getSwapPricer().parRate(underlying, ratesProvider);
    double numeraire = calculateNumeraire(expanded, fixedLeg, forward, ratesProvider);
    double strike = calculateStrike(fixedLeg);
    double tenor = swaptionVolatilities.tenor(fixedLeg.getStartDate(), fixedLeg.getEndDate());
    double volatility = swaptionVolatilities.volatility(expiry, tenor, strike, forward);
    PutCall putCall = PutCall.ofPut(fixedLeg.getPayReceive().isReceive());
    double theta = numeraire * swaptionVolatilities.priceTheta(expiry, putCall, strike, forward, volatility);
    return CurrencyAmount.of(fixedLeg.getCurrency(), theta * expanded.getLongShort().sign());
  }

  //-------------------------------------------------------------------------
  /**
   * Calculates the present value sensitivity of the swaption.
   * <p>
   * The present value sensitivity of the product is the sensitivity of the present value to
   * the underlying curves.
   * 
   * @param swaption  the swaption
   * @param ratesProvider  the rates provider
   * @param swaptionVolatilities  the volatilities
   * @return the present value curve sensitivity of the swap product
   */
  public PointSensitivityBuilder presentValueSensitivityStickyStrike(
      SwaptionProduct swaption,
      RatesProvider ratesProvider,
      SwaptionVolatilities swaptionVolatilities) {

    ExpandedSwaption expanded = swaption.expand();
    validate(expanded, ratesProvider, swaptionVolatilities);
    double expiry = swaptionVolatilities.relativeTime(expanded.getExpiryDateTime());
    ExpandedSwap underlying = expanded.getUnderlying();
    ExpandedSwapLeg fixedLeg = fixedLeg(underlying);
    if (expiry < 0d) { // Option has expired already
      return PointSensitivityBuilder.none();
    }
    double forward = getSwapPricer().parRate(underlying, ratesProvider);
    double annuityCash = getSwapPricer().getLegPricer().annuityCash(fixedLeg, forward);
    double annuityCashDr = getSwapPricer().getLegPricer().annuityCashDerivative(fixedLeg, forward);
    LocalDate settlementDate = ((CashSettlement) expanded.getSwaptionSettlement()).getSettlementDate();
    double discountSettle = ratesProvider.discountFactor(fixedLeg.getCurrency(), settlementDate);
    double strike = calculateStrike(fixedLeg);
    double tenor = swaptionVolatilities.tenor(fixedLeg.getStartDate(), fixedLeg.getEndDate());
    double volatility = swaptionVolatilities.volatility(expiry, tenor, strike, forward);
    PutCall putCall = PutCall.ofPut(fixedLeg.getPayReceive().isReceive());
    double price = swaptionVolatilities.price(expiry, putCall, strike, forward, volatility);
    double delta = swaptionVolatilities.priceDelta(expiry, putCall, strike, forward, volatility);
    // Backward sweep
    PointSensitivityBuilder forwardSensi = getSwapPricer().parRateSensitivity(underlying, ratesProvider);
    PointSensitivityBuilder discountSettleSensi =
        ratesProvider.discountFactors(fixedLeg.getCurrency()).zeroRatePointSensitivity(settlementDate);
    double sign = expanded.getLongShort().sign();
    return forwardSensi.multipliedBy(sign * discountSettle * (annuityCash * delta + annuityCashDr * price))
        .combinedWith(discountSettleSensi.multipliedBy(sign * annuityCash * price));
  }

  //-------------------------------------------------------------------------
  /**
   * Calculates the present value sensitivity to the implied volatility of the swaption.
   * <p>
   * The sensitivity to the implied volatility is also called vega.
   * 
   * @param swaption  the swaption
   * @param ratesProvider  the rates provider
   * @param swaptionVolatilities  the volatilities
   * @return the point sensitivity to the volatility
   */
  public SwaptionSensitivity presentValueSensitivityVolatility(
      SwaptionProduct swaption,
      RatesProvider ratesProvider,
      SwaptionVolatilities swaptionVolatilities) {

    ExpandedSwaption expanded = swaption.expand();
    validate(expanded, ratesProvider, swaptionVolatilities);
    ZonedDateTime expiryDateTime = expanded.getExpiryDateTime();
    double expiry = swaptionVolatilities.relativeTime(expiryDateTime);
    ExpandedSwap underlying = expanded.getUnderlying();
    ExpandedSwapLeg fixedLeg = fixedLeg(underlying);
    double tenor = swaptionVolatilities.tenor(fixedLeg.getStartDate(), fixedLeg.getEndDate());
    double strike = calculateStrike(fixedLeg);
    if (expiry < 0d) { // Option has expired already
      return SwaptionSensitivity.of(
          swaptionVolatilities.getConvention(), expiryDateTime, tenor, strike, 0d, fixedLeg.getCurrency(), 0d);
    }
    double forward = getSwapPricer().parRate(underlying, ratesProvider);
    double numeraire = calculateNumeraire(expanded, fixedLeg, forward, ratesProvider);
    double volatility = swaptionVolatilities.volatility(expiry, tenor, strike, forward);
    PutCall putCall = PutCall.ofPut(fixedLeg.getPayReceive().isReceive());
    double vega = numeraire * swaptionVolatilities.priceVega(expiry, putCall, strike, forward, volatility);
    return SwaptionSensitivity.of(
        swaptionVolatilities.getConvention(),
        expiryDateTime,
        tenor,
        strike,
        forward,
        fixedLeg.getCurrency(),
        vega * expanded.getLongShort().sign());
  }

  //-------------------------------------------------------------------------
  /**
   * Calculates the numeraire, used to multiply the results.
   * 
   * @param expanded  the swap
   * @param fixedLeg  the fixed leg
   * @param forward  the forward rate
   * @param ratesProvider  the rates provider
   * @return the numeraire
   */
  protected double calculateNumeraire(
      ExpandedSwaption expanded,
      ExpandedSwapLeg fixedLeg,
      double forward,
      RatesProvider ratesProvider) {

    double annuityCash = swapPricer.getLegPricer().annuityCash(fixedLeg, forward);
    CashSettlement cashSettlement = (CashSettlement) expanded.getSwaptionSettlement();
    double discountSettle = ratesProvider.discountFactor(fixedLeg.getCurrency(), cashSettlement.getSettlementDate());
    return Math.abs(annuityCash * discountSettle);
  }

  /**
   * Calculates the strike.
   * 
   * @param fixedLeg  the fixed leg
   * @return the strike
   */
  protected double calculateStrike(ExpandedSwapLeg fixedLeg) {
    PaymentPeriod paymentPeriod = fixedLeg.getPaymentPeriods().get(0);
    ArgChecker.isTrue(paymentPeriod instanceof RatePaymentPeriod, "Payment period must be RatePaymentPeriod");
    RatePaymentPeriod ratePaymentPeriod = (RatePaymentPeriod) paymentPeriod;
    // compounding is caught when par rate is computed
    RateObservation rateObservation = ratePaymentPeriod.getAccrualPeriods().get(0).getRateObservation();
    ArgChecker.isTrue(rateObservation instanceof FixedRateObservation, "Swap leg must be fixed leg");
    return ((FixedRateObservation) rateObservation).getRate();
  }

  /**
   * Checks that there is exactly one fixed leg and returns it.
   * 
   * @param swap  the swap
   * @return the fixed leg
   */
  protected ExpandedSwapLeg fixedLeg(ExpandedSwap swap) {
    ArgChecker.isFalse(swap.isCrossCurrency(), "Swap must be single currency");
    // find fixed leg
    List<ExpandedSwapLeg> fixedLegs = swap.getLegs(SwapLegType.FIXED);
    if (fixedLegs.isEmpty()) {
      throw new IllegalArgumentException("Swap must contain a fixed leg");
    }
    return fixedLegs.get(0);
  }

  /**
   * Validates that the rates and volatilities providers are coherent
   * and that the swaption is single currency cash par-yield.
   * 
   * @param swaption  the swaption
   * @param ratesProvider  the rates provider
   * @param swaptionVolatilities  the volatilities
   */
  protected void validate(
      ExpandedSwaption swaption,
      RatesProvider ratesProvider,
      SwaptionVolatilities swaptionVolatilities) {

    ArgChecker.isTrue(swaptionVolatilities.getValuationDate().equals(ratesProvider.getValuationDate()),
        "Volatility and rate data must be for the same date");
    validateSwaption(swaption);
  }

  /**
   * Validates that the swaption is single currency cash par-yield.
   * 
   * @param swaption  the swaption
   */
  protected void validateSwaption(ExpandedSwaption swaption) {
    ArgChecker.isFalse(swaption.getUnderlying().isCrossCurrency(), "Underlying swap must be single currency");
    ArgChecker.isTrue(swaption.getSwaptionSettlement().getSettlementType().equals(SettlementType.CASH),
        "Swaption must be cash settlement");
    CashSettlement cashSettle = (CashSettlement) swaption.getSwaptionSettlement();
    ArgChecker.isTrue(cashSettle.getCashSettlementMethod().equals(CashSettlementMethod.PAR_YIELD),
        "Cash settlement method must be par yield");
  }

}
