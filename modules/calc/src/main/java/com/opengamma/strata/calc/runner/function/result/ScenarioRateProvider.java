/**
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.calc.runner.function.result;

import com.opengamma.strata.basics.currency.Currency;
import com.opengamma.strata.basics.currency.FxRate;
import com.opengamma.strata.basics.currency.FxRateProvider;
import com.opengamma.strata.basics.market.FxRateKey;
import com.opengamma.strata.basics.market.MarketDataBox;
import com.opengamma.strata.calc.marketdata.CalculationMarketData;
import com.opengamma.strata.collect.Messages;

/**
 * A provider of FX rates which takes its data from one scenario in a set of data for multiple scenarios.
 */
public class ScenarioRateProvider implements FxRateProvider {

  /** The market data for a set of scenarios. */
  private final CalculationMarketData marketData;

  /** The index of the scenario in {@link #marketData} from which the FX rates are taken. */
  private final int scenarioIndex;

  /**
   * Returns a rate provider which uses rates from the scenario at the specified index in the market data.
   *
   * @param marketData  market data for a set of scenarios
   * @param scenarioIndex  the index of the scenario from which FX rates are taken
   * @return a rate provider which uses rates from the scenario at the specified index in the market data
   */
  public static ScenarioRateProvider of(CalculationMarketData marketData, int scenarioIndex) {
    return new ScenarioRateProvider(marketData, scenarioIndex);
  }

  private ScenarioRateProvider(CalculationMarketData marketData, int scenarioIndex) {
    this.marketData = marketData;
    this.scenarioIndex = scenarioIndex;

    if (marketData.getScenarioCount() <= scenarioIndex) {
      throw new IllegalArgumentException(
          Messages.format(
              "The number of scenarios is greater than the number of rates ({})",
              marketData.getScenarioCount()));
    }
  }

  @Override
  public double fxRate(Currency baseCurrency, Currency counterCurrency) {
    if (baseCurrency.equals(counterCurrency)) {
      return 1;
    }
    MarketDataBox<FxRate> rates = marketData.getValue(FxRateKey.of(baseCurrency, counterCurrency));
    return rates.getValue(scenarioIndex).fxRate(baseCurrency, counterCurrency);
  }
}
