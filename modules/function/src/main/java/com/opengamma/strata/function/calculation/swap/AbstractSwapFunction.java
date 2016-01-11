/**
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.function.calculation.swap;

import static com.opengamma.strata.calc.runner.function.FunctionUtils.toScenarioResult;
import static com.opengamma.strata.collect.Guavate.toImmutableSet;

import java.util.Optional;
import java.util.Set;
import java.util.stream.IntStream;

import com.google.common.collect.Sets;
import com.opengamma.strata.basics.currency.Currency;
import com.opengamma.strata.basics.index.Index;
import com.opengamma.strata.basics.market.MarketDataKey;
import com.opengamma.strata.basics.market.ObservableKey;
import com.opengamma.strata.calc.marketdata.CalculationMarketData;
import com.opengamma.strata.calc.marketdata.FunctionRequirements;
import com.opengamma.strata.calc.runner.SingleCalculationMarketData;
import com.opengamma.strata.calc.runner.function.result.ScenarioResult;
import com.opengamma.strata.function.calculation.AbstractCalculationFunction;
import com.opengamma.strata.function.marketdata.MarketDataRatesProvider;
import com.opengamma.strata.market.key.DiscountCurveKey;
import com.opengamma.strata.market.key.IndexRateKey;
import com.opengamma.strata.market.key.MarketDataKeys;
import com.opengamma.strata.pricer.rate.RatesProvider;
import com.opengamma.strata.pricer.swap.DiscountingSwapProductPricer;
import com.opengamma.strata.product.swap.ExpandedSwap;
import com.opengamma.strata.product.swap.Swap;
import com.opengamma.strata.product.swap.SwapLeg;
import com.opengamma.strata.product.swap.SwapTrade;

/**
 * Perform calculations on a single {@code SwapTrade} for each of a set of scenarios.
 * <p>
 * The default reporting currency is determined from the first swap leg.
 * 
 * @param <T>  the return type
 */
public abstract class AbstractSwapFunction<T>
    extends AbstractCalculationFunction<SwapTrade, ScenarioResult<T>> {

  /**
   * Creates a new instance which will return results from the {@code execute} method that support automatic
   * currency conversion if the underlying results support it.
   */
  protected AbstractSwapFunction() {
    super();
  }

  /**
   * Creates a new instance.
   *
   * @param convertCurrencies if this is true the value returned by the {@code execute} method will support
   *   automatic currency conversion if the underlying results support it
   */
  protected AbstractSwapFunction(boolean convertCurrencies) {
    super(convertCurrencies);
  }

  //-------------------------------------------------------------------------
  /**
   * Returns the pricer.
   * 
   * @return the pricer
   */
  protected DiscountingSwapProductPricer pricer() {
    return DiscountingSwapProductPricer.DEFAULT;
  }

  @Override
  public FunctionRequirements requirements(SwapTrade trade) {
    Swap swap = trade.getProduct();
    Set<Index> indices = swap.allIndices();

    Set<ObservableKey> indexRateKeys =
        indices.stream()
            .map(IndexRateKey::of)
            .collect(toImmutableSet());

    Set<MarketDataKey<?>> indexCurveKeys =
        indices.stream()
            .map(MarketDataKeys::indexCurve)
            .collect(toImmutableSet());

    Set<DiscountCurveKey> discountCurveKeys =
        swap.getLegs().stream()
            .map(SwapLeg::getCurrency)
            .map(DiscountCurveKey::of)
            .collect(toImmutableSet());

    return FunctionRequirements.builder()
        .singleValueRequirements(Sets.union(indexCurveKeys, discountCurveKeys))
        .timeSeriesRequirements(indexRateKeys)
        .outputCurrencies(swap.getLegs().stream().map(SwapLeg::getCurrency).collect(toImmutableSet()))
        .build();
  }

  @Override
  public ScenarioResult<T> execute(SwapTrade trade, CalculationMarketData marketData) {
    ExpandedSwap product = trade.getProduct().expand();
    return IntStream.range(0, marketData.getScenarioCount())
        .mapToObj(index -> new SingleCalculationMarketData(marketData, index))
        .map(MarketDataRatesProvider::new)
        .map(provider -> execute(product, provider))
        .collect(toScenarioResult(isConvertCurrencies()));
  }

  @Override
  public Optional<Currency> defaultReportingCurrency(SwapTrade target) {
    return Optional.of(target.getProduct().getLegs().get(0).getCurrency());
  }

  // execute for a single trade
  protected abstract T execute(ExpandedSwap product, RatesProvider provider);

}
