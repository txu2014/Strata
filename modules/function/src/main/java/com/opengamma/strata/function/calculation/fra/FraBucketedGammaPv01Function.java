/**
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 * 
 * Please see distribution for license.
 */
package com.opengamma.strata.function.calculation.fra;

import static com.opengamma.strata.calc.runner.function.FunctionUtils.toScenarioResult;
import static java.util.stream.Collectors.toSet;

import java.util.HashSet;
import java.util.Set;
import java.util.stream.IntStream;

import com.google.common.collect.Iterables;
import com.opengamma.strata.basics.currency.Currency;
import com.opengamma.strata.basics.index.IborIndex;
import com.opengamma.strata.basics.index.Index;
import com.opengamma.strata.basics.market.MarketData;
import com.opengamma.strata.calc.marketdata.CalculationMarketData;
import com.opengamma.strata.calc.runner.SingleCalculationMarketData;
import com.opengamma.strata.calc.runner.function.result.ScenarioResult;
import com.opengamma.strata.collect.Messages;
import com.opengamma.strata.function.calculation.rate.MarketDataUtils;
import com.opengamma.strata.market.curve.Curve;
import com.opengamma.strata.market.curve.CurveCurrencyParameterSensitivities;
import com.opengamma.strata.market.curve.CurveCurrencyParameterSensitivity;
import com.opengamma.strata.market.curve.NodalCurve;
import com.opengamma.strata.market.key.DiscountCurveKey;
import com.opengamma.strata.market.key.IborIndexCurveKey;
import com.opengamma.strata.market.sensitivity.PointSensitivities;
import com.opengamma.strata.pricer.rate.RatesProvider;
import com.opengamma.strata.pricer.sensitivity.CurveGammaCalculator;
import com.opengamma.strata.product.fra.ExpandedFra;
import com.opengamma.strata.product.fra.Fra;
import com.opengamma.strata.product.fra.FraTrade;

/**
 * Calculates Gamma PV01, the second-order present value sensitivity of a {@link FraTrade}
 * for each of a set of scenarios.
 * <p>
 * This implementation only supports calculating the measure when using a single curve for
 * discounting and forecasting.
 */
public class FraBucketedGammaPv01Function
    extends AbstractFraFunction<CurveCurrencyParameterSensitivities> {

  @Override
  public ScenarioResult<CurveCurrencyParameterSensitivities> execute(FraTrade trade, CalculationMarketData marketData) {
    ExpandedFra expandedFra = trade.getProduct().expand();
    return IntStream.range(0, marketData.getScenarioCount())
        .mapToObj(index -> new SingleCalculationMarketData(marketData, index))
        .map(md -> execute(trade.getProduct(), expandedFra, md))
        .collect(toScenarioResult());
  }

  @Override
  protected CurveCurrencyParameterSensitivities execute(ExpandedFra product, RatesProvider ratesProvider) {
    throw new UnsupportedOperationException("execute(FraTrade) overridden instead");
  }

  //-------------------------------------------------------------------------
  // calculate the gamma sensitivity
  private CurveCurrencyParameterSensitivities execute(
      Fra fra,
      ExpandedFra expandedFra,
      MarketData marketData) {

    // find the curve and check it is valid
    Currency currency = expandedFra.getCurrency();
    NodalCurve nodalCurve = findNodalCurve(marketData, currency);

    // find indices and validate there is only one curve
    Set<IborIndex> indices = new HashSet<>();
    indices.add(fra.getIndex());
    fra.getIndexInterpolated().ifPresent(indices::add);
    validateSingleCurve(indices, marketData, nodalCurve);

    // calculate gamma
    CurveCurrencyParameterSensitivity gamma = CurveGammaCalculator.DEFAULT.calculateSemiParallelGamma(
        nodalCurve, currency, c -> calculateCurveSensitivity(expandedFra, currency, indices, marketData, c));
    return CurveCurrencyParameterSensitivities.of(gamma).multipliedBy(ONE_BASIS_POINT * ONE_BASIS_POINT);
  }

  // finds the discount curve and ensures it is a NodalCurve
  private NodalCurve findNodalCurve(MarketData marketData, Currency currency) {
    Curve curve = marketData.getValue(DiscountCurveKey.of(currency));
    if (!(curve instanceof NodalCurve)) {
      throw new IllegalArgumentException(Messages.format(
          "Implementation only supports nodal curves; unsupported curve type: {}", curve.getClass().getSimpleName()));
    }
    return (NodalCurve) curve;
  }

  // validates that the indices all resolve to the single specified curve
  private void validateSingleCurve(Set<IborIndex> indices, MarketData marketData, NodalCurve nodalCurve) {
    Set<IborIndexCurveKey> differentForwardCurves = indices.stream()
        .map(IborIndexCurveKey::of)
        .filter(k -> !nodalCurve.equals(marketData.getValue(k)))
        .collect(toSet());
    if (!differentForwardCurves.isEmpty()) {
      throw new IllegalArgumentException(
          Messages.format("Implementation only supports a single curve, but discounting curve is different from " +
              "index curves for indices: {}", differentForwardCurves));
    }
  }

  // calculates the sensitivity
  private CurveCurrencyParameterSensitivity calculateCurveSensitivity(
      ExpandedFra expandedFra,
      Currency currency,
      Set<? extends Index> indices,
      MarketData marketData,
      NodalCurve bumpedCurve) {

    RatesProvider ratesProvider = MarketDataUtils.toSingleCurveRatesProvider(marketData, currency, indices, bumpedCurve);
    PointSensitivities pointSensitivities = pricer().presentValueSensitivity(expandedFra, ratesProvider);
    CurveCurrencyParameterSensitivities paramSensitivities = ratesProvider.curveParameterSensitivity(pointSensitivities);
    return Iterables.getOnlyElement(paramSensitivities.getSensitivities());
  }

}
