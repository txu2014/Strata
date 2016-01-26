/**
 * Copyright (C) 2016 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.function.calculation.fx;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.opengamma.strata.basics.currency.Currency;
import com.opengamma.strata.basics.currency.CurrencyPair;
import com.opengamma.strata.calc.config.Measure;
import com.opengamma.strata.calc.marketdata.CalculationMarketData;
import com.opengamma.strata.calc.marketdata.FunctionRequirements;
import com.opengamma.strata.calc.runner.function.CalculationFunction;
import com.opengamma.strata.calc.runner.function.result.ScenarioResult;
import com.opengamma.strata.collect.result.FailureReason;
import com.opengamma.strata.collect.result.Result;
import com.opengamma.strata.market.key.DiscountCurveKey;
import com.opengamma.strata.product.fx.ExpandedFxSingle;
import com.opengamma.strata.product.fx.FxSingle;
import com.opengamma.strata.product.fx.FxSingleTrade;

/**
 * Perform calculations on a single {@code FxSingleTrade} for each of a set of scenarios.
 * <p>
 * This uses the standard discounting calculation method.
 * The supported built-in measures are:
 * <ul>
 *   <li>{@linkplain Measure#PAR_SPREAD Par spread}
 *   <li>{@linkplain Measure#PRESENT_VALUE Present value}
 *   <li>{@linkplain Measure#PV01 PV01}
 *   <li>{@linkplain Measure#BUCKETED_PV01 Bucketed PV01}
 *   <li>{@linkplain Measure#CURRENCY_EXPOSURE Currency exposure}
 *   <li>{@linkplain Measure#CURRENT_CASH Current cash}
 *   <li>{@linkplain Measure#FORWARD_FX_RATE Forward FX rate}
 * </ul>
 * <p>
 * The default reporting currency is determined to be the base currency of the market convention
 * pair of the two trade currencies.
 */
public class FxSingleCalculationFunction
    implements CalculationFunction<FxSingleTrade> {

  /**
   * The calculations by measure.
   */
  private static final ImmutableMap<Measure, SingleMeasureCalculation> CALCULATORS =
      ImmutableMap.<Measure, SingleMeasureCalculation>builder()
          .put(Measure.PAR_SPREAD, FxSingleMeasureCalculations::parSpread)
          .put(Measure.PRESENT_VALUE, FxSingleMeasureCalculations::presentValue)
          .put(Measure.PV01, FxSingleMeasureCalculations::pv01)
          .put(Measure.BUCKETED_PV01, FxSingleMeasureCalculations::bucketedPv01)
          .put(Measure.CURRENCY_EXPOSURE, FxSingleMeasureCalculations::currencyExposure)
          .put(Measure.CURRENT_CASH, FxSingleMeasureCalculations::currentCash)
          .put(Measure.FORWARD_FX_RATE, FxSingleMeasureCalculations::forwardFxRate)
          .build();

  /**
   * Creates an instance.
   */
  public FxSingleCalculationFunction() {
  }

  //-------------------------------------------------------------------------
  @Override
  public Set<Measure> supportedMeasures() {
    return CALCULATORS.keySet();
  }

  @Override
  public Optional<Currency> defaultReportingCurrency(FxSingleTrade target) {
    Currency base = target.getProduct().getBaseCurrencyAmount().getCurrency();
    Currency counter = target.getProduct().getCounterCurrencyAmount().getCurrency();
    CurrencyPair marketConventionPair = CurrencyPair.of(base, counter).toConventional();
    return Optional.of(marketConventionPair.getBase());
  }

  //-------------------------------------------------------------------------
  @Override
  public FunctionRequirements requirements(FxSingleTrade trade, Set<Measure> measures) {
    FxSingle fx = trade.getProduct();
    Currency baseCurrency = fx.getBaseCurrencyAmount().getCurrency();
    Currency counterCurrency = fx.getCounterCurrencyAmount().getCurrency();

    Set<DiscountCurveKey> discountCurveKeys =
        ImmutableSet.of(DiscountCurveKey.of(baseCurrency), DiscountCurveKey.of(counterCurrency));

    return FunctionRequirements.builder()
        .singleValueRequirements(discountCurveKeys)
        .timeSeriesRequirements()
        .outputCurrencies(baseCurrency, counterCurrency)
        .build();
  }

  //-------------------------------------------------------------------------
  @Override
  public Map<Measure, Result<?>> calculate(
      FxSingleTrade trade,
      Set<Measure> measures,
      CalculationMarketData scenarioMarketData) {

    // expand the trade once for all measures and all scenarios
    ExpandedFxSingle product = trade.getProduct().expand();

    // loop around measures, calculating all scenarios for one measure
    Map<Measure, Result<?>> results = new HashMap<>();
    for (Measure measure : measures) {
      results.put(measure, calculate(measure, trade, product, scenarioMarketData));
    }
    return results;
  }

  // calculate one measure
  private Result<?> calculate(
      Measure measure,
      FxSingleTrade trade,
      ExpandedFxSingle product,
      CalculationMarketData scenarioMarketData) {

    SingleMeasureCalculation calculator = CALCULATORS.get(measure);
    if (calculator == null) {
      return Result.failure(FailureReason.INVALID_INPUT, "Unsupported measure: {}", measure);
    }
    return Result.of(() -> calculator.calculate(trade, product, scenarioMarketData));
  }

  //-------------------------------------------------------------------------
  @FunctionalInterface
  interface SingleMeasureCalculation {
    public abstract ScenarioResult<?> calculate(
        FxSingleTrade trade,
        ExpandedFxSingle product,
        CalculationMarketData marketData);
  }

}
