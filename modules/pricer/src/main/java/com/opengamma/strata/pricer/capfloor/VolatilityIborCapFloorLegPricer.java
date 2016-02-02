package com.opengamma.strata.pricer.capfloor;

import com.opengamma.strata.basics.currency.CurrencyAmount;
import com.opengamma.strata.collect.ArgChecker;
import com.opengamma.strata.market.sensitivity.PointSensitivityBuilder;
import com.opengamma.strata.market.view.IborCapletFloorletVolatilities;
import com.opengamma.strata.pricer.impl.capfloor.VolatilityIborCapletFloorletPeriodPricer;
import com.opengamma.strata.pricer.rate.RatesProvider;
import com.opengamma.strata.product.capfloor.ExpandedIborCapFloorLeg;

public class VolatilityIborCapFloorLegPricer {
  
  private final VolatilityIborCapletFloorletPeriodPricer periodPricer;

  public VolatilityIborCapFloorLegPricer(VolatilityIborCapletFloorletPeriodPricer periodPricer) {
    this.periodPricer = ArgChecker.notNull(periodPricer, "periodPricer");
  }

  public CurrencyAmount presentValue(
      ExpandedIborCapFloorLeg capFloorLeg,
      RatesProvider ratesProvider,
      IborCapletFloorletVolatilities volatilities) {

    validate(ratesProvider, volatilities);
    return capFloorLeg.getCapletFloorletPeriods()
        .stream()
        .map(period -> periodPricer.presentValue(period, ratesProvider, volatilities))
        .reduce((c1, c2) -> c1.plus(c2))
        .get();
  }

  public CurrencyAmount presentValueDelta(
      ExpandedIborCapFloorLeg capFloorLeg,
      RatesProvider ratesProvider,
      IborCapletFloorletVolatilities volatilities) {

    validate(ratesProvider, volatilities);
    return capFloorLeg.getCapletFloorletPeriods()
        .stream()
        .map(period -> periodPricer.presentValueDelta(period, ratesProvider, volatilities))
        .reduce((c1, c2) -> c1.plus(c2))
        .get();
  }

  public CurrencyAmount presentValueGamma(
      ExpandedIborCapFloorLeg capFloorLeg,
      RatesProvider ratesProvider,
      IborCapletFloorletVolatilities volatilities) {

    validate(ratesProvider, volatilities);
    return capFloorLeg.getCapletFloorletPeriods()
        .stream()
        .map(period -> periodPricer.presentValueGamma(period, ratesProvider, volatilities))
        .reduce((c1, c2) -> c1.plus(c2))
        .get();
  }

  public CurrencyAmount presentValueTheta(
      ExpandedIborCapFloorLeg capFloorLeg,
      RatesProvider ratesProvider,
      IborCapletFloorletVolatilities volatilities) {

    validate(ratesProvider, volatilities);
    return capFloorLeg.getCapletFloorletPeriods()
        .stream()
        .map(period -> periodPricer.presentValueTheta(period, ratesProvider, volatilities))
        .reduce((c1, c2) -> c1.plus(c2))
        .get();
  }

  public PointSensitivityBuilder presentValueSensitivityVolatility(
      ExpandedIborCapFloorLeg capFloorLeg,
      RatesProvider ratesProvider,
      IborCapletFloorletVolatilities volatilities) {

    validate(ratesProvider, volatilities);
    return capFloorLeg.getCapletFloorletPeriods()
        .stream()
        .map(period -> periodPricer.presentValueSensitivityVolatility(period, ratesProvider, volatilities))
        .reduce((c1, c2) -> c1.combinedWith(c2))
        .get();
  }

  public PointSensitivityBuilder presentValueSensitivityStickyStrike(
      ExpandedIborCapFloorLeg capFloorLeg,
      RatesProvider ratesProvider,
      IborCapletFloorletVolatilities volatilities) {

    validate(ratesProvider, volatilities);
    return capFloorLeg.getCapletFloorletPeriods()
        .stream()
        .map(period -> periodPricer.presentValueSensitivityStickyStrike(period, ratesProvider, volatilities))
        .reduce((p1, p2) -> p1.combinedWith(p2))
        .get();
  }

  public CurrencyAmount currentCash(
      ExpandedIborCapFloorLeg capFloorLeg,
      RatesProvider ratesProvider,
      IborCapletFloorletVolatilities volatilities) {

    validate(ratesProvider, volatilities);
    return capFloorLeg.getCapletFloorletPeriods()
        .stream()
        .filter(period -> period.getPaymentDate().equals(ratesProvider.getValuationDate()))
        .map(period -> periodPricer.presentValue(period, ratesProvider, volatilities))
        .reduce((c1, c2) -> c1.plus(c2))
        .orElse(CurrencyAmount.zero(capFloorLeg.getCurrency()));
  }
  

  //-------------------------------------------------------------------------
  private void validate(RatesProvider ratesProvider, IborCapletFloorletVolatilities volatilities) {
    ArgChecker.isTrue(volatilities.getValuationDate().equals(ratesProvider.getValuationDate()),
        "volatility and rate data must be for the same date");
  }

}
