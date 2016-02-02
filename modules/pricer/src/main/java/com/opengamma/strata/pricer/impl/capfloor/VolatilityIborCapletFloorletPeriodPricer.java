package com.opengamma.strata.pricer.impl.capfloor;

import com.opengamma.strata.basics.PutCall;
import com.opengamma.strata.basics.currency.Currency;
import com.opengamma.strata.basics.currency.CurrencyAmount;
import com.opengamma.strata.collect.ArgChecker;
import com.opengamma.strata.market.sensitivity.IborCapletFloorletSensitivity;
import com.opengamma.strata.market.sensitivity.PointSensitivityBuilder;
import com.opengamma.strata.market.view.IborCapletFloorletVolatilities;
import com.opengamma.strata.pricer.rate.RatesProvider;
import com.opengamma.strata.product.capfloor.IborCapletFloorletPeriod;

public class VolatilityIborCapletFloorletPeriodPricer {

  public static final VolatilityIborCapletFloorletPeriodPricer DEFAULT = new VolatilityIborCapletFloorletPeriodPricer();
  

  //-------------------------------------------------------------------------
  /**
   * Calculates the present value of the caplet/floorlet period.
   * <p>
   * The result is expressed using the currency of the period.
   * 
   * @param period  the caplet/floorlet period
   * @param ratesProvider  the rates provider
   * @param volatilities  the volatilities
   * @return the present value
   */
  public CurrencyAmount presentValue(
      IborCapletFloorletPeriod period,
      RatesProvider ratesProvider,
      IborCapletFloorletVolatilities volatilities) {

    double expiry = volatilities.relativeTime(period.getFixingDateTime());
    Currency currency = period.getCurrency();
    if (expiry < 0d) { // Option has expired already
      return CurrencyAmount.of(currency, 0d);
    }
    double forward = ratesProvider.iborIndexRates(period.getIndex()).rate(period.getFixingDate());
    double strike = period.getStrike();
    double volatility = volatilities.volatility(expiry, strike, forward);
    PutCall putCall = period.getPutCall();
    double df = ratesProvider.discountFactor(currency, period.getPaymentDate());
    double price = df * period.getYearFraction() * volatilities.price(expiry, putCall, strike, forward, volatility);
    return CurrencyAmount.of(currency, price * period.getNotional());
  }

  //-------------------------------------------------------------------------
  /**
   * Computes the implied volatility of the caplet/floorlet.
   * 
   * @param period  the caplet/floorlet period
   * @param ratesProvider  the rates provider
   * @param volatilities  the volatilities
   * @return the implied volatility
   */
  public double impliedVolatility(
      IborCapletFloorletPeriod period,
      RatesProvider ratesProvider,
      IborCapletFloorletVolatilities volatilities) {

    double expiry = volatilities.relativeTime(period.getFixingDateTime());
    ArgChecker.isTrue(expiry >= 0d, "Option must be before expiry to compute an implied volatility");
    double forward = ratesProvider.iborIndexRates(period.getIndex()).rate(period.getFixingDate());
    double strike = period.getStrike();
    return volatilities.volatility(expiry, strike, forward);
  }

  //-------------------------------------------------------------------------
  /**
   * Calculates the present value delta of the caplet/floorlet period.
   * <p>
   * The present value delta is given by the first derivative of the present value with respect to forward. 
   * 
   * @param period  the caplet/floorlet period
   * @param ratesProvider  the rates provider
   * @param volatilities  the volatilities
   * @return the present value delta
   */
  public CurrencyAmount presentValueDelta(
      IborCapletFloorletPeriod period,
      RatesProvider ratesProvider,
      IborCapletFloorletVolatilities volatilities) {

    double expiry = volatilities.relativeTime(period.getFixingDateTime());
    Currency currency = period.getCurrency();
    if (expiry < 0d) { // Option has expired already
      return CurrencyAmount.of(currency, 0d);
    }
    double forward = ratesProvider.iborIndexRates(period.getIndex()).rate(period.getFixingDate());
    double strike = period.getStrike();
    double volatility = volatilities.volatility(expiry, strike, forward);
    PutCall putCall = period.getPutCall();
    double df = ratesProvider.discountFactor(currency, period.getPaymentDate());
    double priceDelta = df * period.getYearFraction() *
        volatilities.priceDelta(expiry, putCall, strike, forward, volatility);
    return CurrencyAmount.of(currency, priceDelta * period.getNotional());
  }

  //-------------------------------------------------------------------------
  /**
   * Calculates the present value gamma of the caplet/floorlet period.
   * <p>
   * The present value gamma is given by the second derivative of the present value with respect to forward. 
   * 
   * @param period  the caplet/floorlet period
   * @param ratesProvider  the rates provider
   * @param volatilities  the volatilities
   * @return the present value gamma
   */
  public CurrencyAmount presentValueGamma(
      IborCapletFloorletPeriod period,
      RatesProvider ratesProvider,
      IborCapletFloorletVolatilities volatilities) {

    double expiry = volatilities.relativeTime(period.getFixingDateTime());
    Currency currency = period.getCurrency();
    if (expiry < 0d) { // Option has expired already
      return CurrencyAmount.of(currency, 0d);
    }
    double forward = ratesProvider.iborIndexRates(period.getIndex()).rate(period.getFixingDate());
    double strike = period.getStrike();
    double volatility = volatilities.volatility(expiry, strike, forward);
    PutCall putCall = period.getPutCall();
    double df = ratesProvider.discountFactor(currency, period.getPaymentDate());
    double priceGamma = df * period.getYearFraction() *
        volatilities.priceGamma(expiry, putCall, strike, forward, volatility);
    return CurrencyAmount.of(currency, priceGamma * period.getNotional());
  }

  //-------------------------------------------------------------------------
  /**
   * Calculates the present value theta of the caplet/floorlet period.
   * <p>
   * The present value theta is given by the minus of the present value sensitivity to {@code timeToExpiry}. 
   * 
   * @param period  the caplet/floorlet period
   * @param ratesProvider  the rates provider
   * @param volatilities  the volatilities
   * @return the present value theta
   */
  public CurrencyAmount presentValueTheta(
      IborCapletFloorletPeriod period,
      RatesProvider ratesProvider,
      IborCapletFloorletVolatilities volatilities) {

    double expiry = volatilities.relativeTime(period.getFixingDateTime());
    Currency currency = period.getCurrency();
    if (expiry < 0d) { // Option has expired already
      return CurrencyAmount.of(currency, 0d);
    }
    double forward = ratesProvider.iborIndexRates(period.getIndex()).rate(period.getFixingDate());
    double strike = period.getStrike();
    double volatility = volatilities.volatility(expiry, strike, forward);
    PutCall putCall = period.getPutCall();
    double df = ratesProvider.discountFactor(currency, period.getPaymentDate());
    double priceTheta = df * period.getYearFraction() *
        volatilities.priceTheta(expiry, putCall, strike, forward, volatility);
    return CurrencyAmount.of(currency, priceTheta * period.getNotional());
  }

  //-------------------------------------------------------------------------
  /**
   * Calculates the present value sensitivity of the caplet/floorlet.
   * <p>
   * The present value sensitivity is the sensitivity of the present value to the underlying curves.
   * 
   * @param period  the caplet/floorlet period
   * @param ratesProvider  the rates provider
   * @param volatilities  the volatilities
   * @return the present value curve sensitivity
   */
  public PointSensitivityBuilder presentValueSensitivityStickyStrike(
      IborCapletFloorletPeriod period,
      RatesProvider ratesProvider,
      IborCapletFloorletVolatilities volatilities) {

    double expiry = volatilities.relativeTime(period.getFixingDateTime());
    Currency currency = period.getCurrency();
    if (expiry < 0d) { // Option has expired already
      return PointSensitivityBuilder.none();
    }
    double forward = ratesProvider.iborIndexRates(period.getIndex()).rate(period.getFixingDate());
    PointSensitivityBuilder forwardSensi = ratesProvider.iborIndexRates(period.getIndex()).ratePointSensitivity(
        period.getFixingDate());
    double strike = period.getStrike();
    double volatility = volatilities.volatility(expiry, strike, forward);
    PutCall putCall = period.getPutCall();
    double df = ratesProvider.discountFactor(currency, period.getPaymentDate());
    PointSensitivityBuilder dfSensi = ratesProvider.discountFactors(currency).zeroRatePointSensitivity(
        period.getPaymentDate());
    double factor = period.getNotional() * period.getYearFraction();
    double fwdPv = factor * volatilities.price(expiry, putCall, strike, forward, volatility);
    double fwdDelta = factor * volatilities.priceDelta(expiry, putCall, strike, forward, volatility);
    return dfSensi.multipliedBy(fwdPv).combinedWith(forwardSensi.multipliedBy(fwdDelta * df));
  }

  //-------------------------------------------------------------------------
  /**
   * Calculates the present value sensitivity to the implied volatility of the caplet/floorlet.
   * <p>
   * The sensitivity to the implied volatility is also called vega.
   * 
   * @param period  the caplet/floorlet period
   * @param ratesProvider  the rates provider
   * @param volatilities  the volatilities
   * @return the point sensitivity to the volatility
   */
  public PointSensitivityBuilder presentValueSensitivityVolatility(
      IborCapletFloorletPeriod period,
      RatesProvider ratesProvider,
      IborCapletFloorletVolatilities volatilities) {

    double expiry = volatilities.relativeTime(period.getFixingDateTime());
    double strike = period.getStrike();
    Currency currency = period.getCurrency();
    if (expiry < 0d) { // Option has expired already
      return PointSensitivityBuilder.none();
    }
    double forward = ratesProvider.iborIndexRates(period.getIndex()).rate(period.getFixingDate());
    double volatility = volatilities.volatility(expiry, strike, forward);
    PutCall putCall = period.getPutCall();
    double df = ratesProvider.discountFactor(currency, period.getPaymentDate());
    double vega = df * period.getYearFraction() * volatilities.priceVega(expiry, putCall, strike, forward, volatility);
    return IborCapletFloorletSensitivity.of(
        period.getIndex(),
        period.getFixingDateTime(),
        strike,
        forward,
        currency,
        vega * period.getNotional());
  }
  
}
