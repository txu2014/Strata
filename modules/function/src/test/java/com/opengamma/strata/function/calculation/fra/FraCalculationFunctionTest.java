/**
 * Copyright (C) 2016 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.function.calculation.fra;

import static com.opengamma.strata.basics.date.DayCounts.ACT_360;
import static com.opengamma.strata.collect.TestHelper.coverPrivateConstructor;
import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;
import java.util.Set;

import org.testng.annotations.Test;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.opengamma.strata.basics.currency.Currency;
import com.opengamma.strata.basics.currency.CurrencyAmount;
import com.opengamma.strata.basics.currency.MultiCurrencyAmount;
import com.opengamma.strata.basics.index.IborIndex;
import com.opengamma.strata.calc.config.FunctionConfig;
import com.opengamma.strata.calc.config.Measure;
import com.opengamma.strata.calc.config.pricing.FunctionGroup;
import com.opengamma.strata.calc.marketdata.CalculationMarketData;
import com.opengamma.strata.calc.marketdata.FunctionRequirements;
import com.opengamma.strata.calc.runner.SingleCalculationMarketData;
import com.opengamma.strata.calc.runner.function.result.CurrencyValuesArray;
import com.opengamma.strata.calc.runner.function.result.DefaultScenarioResult;
import com.opengamma.strata.calc.runner.function.result.FxConvertibleList;
import com.opengamma.strata.calc.runner.function.result.MultiCurrencyValuesArray;
import com.opengamma.strata.calc.runner.function.result.ValuesArray;
import com.opengamma.strata.collect.result.Result;
import com.opengamma.strata.function.marketdata.MarketDataRatesProvider;
import com.opengamma.strata.function.marketdata.curve.TestMarketDataMap;
import com.opengamma.strata.market.curve.ConstantNodalCurve;
import com.opengamma.strata.market.curve.Curve;
import com.opengamma.strata.market.curve.CurveCurrencyParameterSensitivities;
import com.opengamma.strata.market.curve.Curves;
import com.opengamma.strata.market.explain.ExplainMap;
import com.opengamma.strata.market.key.DiscountCurveKey;
import com.opengamma.strata.market.key.IborIndexCurveKey;
import com.opengamma.strata.market.key.IndexRateKey;
import com.opengamma.strata.market.sensitivity.PointSensitivities;
import com.opengamma.strata.pricer.fra.DiscountingFraProductPricer;
import com.opengamma.strata.pricer.fra.FraDummyData;
import com.opengamma.strata.product.fra.FraTrade;

/**
 * Test {@link FraCalculationFunction}.
 */
@Test
public class FraCalculationFunctionTest {

  public static final FraTrade TRADE = FraDummyData.FRA_TRADE;
  private static final IborIndex INDEX = TRADE.getProduct().getIndex();
  private static final Currency CURRENCY = TRADE.getProduct().getCurrency();
  private static final LocalDate VAL_DATE = TRADE.getProduct().getStartDate().minusDays(7);

  //-------------------------------------------------------------------------
  public void test_group() {
    FunctionGroup<FraTrade> test = FraFunctionGroups.discounting();
    assertThat(test.configuredMeasures(TRADE)).contains(
        Measure.PAR_RATE,
        Measure.PAR_SPREAD,
        Measure.PRESENT_VALUE,
        Measure.EXPLAIN_PRESENT_VALUE,
        Measure.PV01,
        Measure.BUCKETED_PV01,
        Measure.BUCKETED_GAMMA_PV01);
    FunctionConfig<FraTrade> config =
        FraFunctionGroups.discounting().functionConfig(TRADE, Measure.PRESENT_VALUE).get();
    assertThat(config.createFunction()).isInstanceOf(FraCalculationFunction.class);
  }

  public void test_requirementsAndCurrency() {
    FraCalculationFunction function = new FraCalculationFunction();
    Set<Measure> measures = function.supportedMeasures();
    FunctionRequirements reqs = function.requirements(TRADE, measures);
    assertThat(reqs.getOutputCurrencies()).containsOnly(CURRENCY);
    assertThat(reqs.getSingleValueRequirements()).isEqualTo(
        ImmutableSet.of(DiscountCurveKey.of(CURRENCY), IborIndexCurveKey.of(INDEX)));
    assertThat(reqs.getTimeSeriesRequirements()).isEqualTo(ImmutableSet.of(IndexRateKey.of(INDEX)));
    assertThat(function.defaultReportingCurrency(TRADE)).hasValue(CURRENCY);
  }

  public void test_simpleMeasures() {
    FraCalculationFunction function = new FraCalculationFunction();
    CalculationMarketData md = marketData();
    MarketDataRatesProvider provider = new MarketDataRatesProvider(new SingleCalculationMarketData(md, 0));
    DiscountingFraProductPricer pricer = DiscountingFraProductPricer.DEFAULT;
    CurrencyAmount expectedPv = pricer.presentValue(TRADE.getProduct(), provider);
    double expectedParRate = pricer.parRate(TRADE.getProduct(), provider);
    double expectedParSpread = pricer.parSpread(TRADE.getProduct(), provider);
    ExplainMap expectedExplainPv = pricer.explainPresentValue(TRADE.getProduct(), provider);

    Set<Measure> measures = ImmutableSet.of(
        Measure.PRESENT_VALUE, Measure.PAR_RATE, Measure.PAR_SPREAD, Measure.EXPLAIN_PRESENT_VALUE);
    assertThat(function.calculate(TRADE, measures, md))
        .containsEntry(
            Measure.PRESENT_VALUE, Result.success(CurrencyValuesArray.of(ImmutableList.of(expectedPv))))
        .containsEntry(
            Measure.PAR_RATE, Result.success(ValuesArray.of(ImmutableList.of(expectedParRate))))
        .containsEntry(
            Measure.PAR_SPREAD, Result.success(ValuesArray.of(ImmutableList.of(expectedParSpread))))
        .containsEntry(
            Measure.EXPLAIN_PRESENT_VALUE, Result.success(DefaultScenarioResult.of(ImmutableList.of(expectedExplainPv))));
  }

  public void test_pv01() {
    FraCalculationFunction function = new FraCalculationFunction();
    CalculationMarketData md = marketData();
    MarketDataRatesProvider provider = new MarketDataRatesProvider(new SingleCalculationMarketData(md, 0));
    DiscountingFraProductPricer pricer = DiscountingFraProductPricer.DEFAULT;
    PointSensitivities pvPointSens = pricer.presentValueSensitivity(TRADE.getProduct(), provider);
    CurveCurrencyParameterSensitivities pvParamSens = provider.curveParameterSensitivity(pvPointSens);
    MultiCurrencyAmount expectedPv01 = pvParamSens.total().multipliedBy(1e-4);
    CurveCurrencyParameterSensitivities expectedBucketedPv01 = pvParamSens.multipliedBy(1e-4);

    Set<Measure> measures = ImmutableSet.of(Measure.PV01, Measure.BUCKETED_PV01);
    assertThat(function.calculate(TRADE, measures, md))
        .containsEntry(
            Measure.PV01, Result.success(MultiCurrencyValuesArray.of(ImmutableList.of(expectedPv01))))
        .containsEntry(
            Measure.BUCKETED_PV01, Result.success(FxConvertibleList.of(ImmutableList.of(expectedBucketedPv01))));
  }

  //-------------------------------------------------------------------------
  private CalculationMarketData marketData() {
    Curve curve = ConstantNodalCurve.of(Curves.discountFactors("Test", ACT_360), 0.99);
    TestMarketDataMap md = new TestMarketDataMap(
        VAL_DATE,
        ImmutableMap.of(DiscountCurveKey.of(CURRENCY), curve, IborIndexCurveKey.of(INDEX), curve),
        ImmutableMap.of());
    return md;
  }

  //-------------------------------------------------------------------------
  public void coverage() {
    coverPrivateConstructor(FraFunctionGroups.class);
    coverPrivateConstructor(FraMeasureCalculations.class);
  }

}
