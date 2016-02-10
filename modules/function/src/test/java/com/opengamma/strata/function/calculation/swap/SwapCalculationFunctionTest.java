/**
 * Copyright (C) 2016 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.function.calculation.swap;

import static com.opengamma.strata.basics.date.DayCounts.ACT_360;
import static com.opengamma.strata.collect.TestHelper.coverPrivateConstructor;
import static com.opengamma.strata.pricer.swap.SwapDummyData.FIXED_RATECALC_SWAP_LEG;
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
import com.opengamma.strata.basics.date.DayCounts;
import com.opengamma.strata.basics.index.IborIndex;
import com.opengamma.strata.basics.market.ReferenceData;
import com.opengamma.strata.calc.config.FunctionConfig;
import com.opengamma.strata.calc.config.Measure;
import com.opengamma.strata.calc.config.Measures;
import com.opengamma.strata.calc.config.pricing.FunctionGroup;
import com.opengamma.strata.calc.marketdata.CalculationMarketData;
import com.opengamma.strata.calc.marketdata.FunctionRequirements;
import com.opengamma.strata.calc.runner.function.result.MultiCurrencyValuesArray;
import com.opengamma.strata.calc.runner.function.result.ScenarioResult;
import com.opengamma.strata.calc.runner.function.result.ValuesArray;
import com.opengamma.strata.collect.result.Result;
import com.opengamma.strata.function.marketdata.curve.TestMarketDataMap;
import com.opengamma.strata.market.amount.CashFlows;
import com.opengamma.strata.market.curve.ConstantNodalCurve;
import com.opengamma.strata.market.curve.Curve;
import com.opengamma.strata.market.curve.CurveCurrencyParameterSensitivities;
import com.opengamma.strata.market.curve.Curves;
import com.opengamma.strata.market.explain.ExplainMap;
import com.opengamma.strata.market.key.DiscountCurveKey;
import com.opengamma.strata.market.key.IborIndexCurveKey;
import com.opengamma.strata.market.key.IndexRateKey;
import com.opengamma.strata.market.sensitivity.PointSensitivities;
import com.opengamma.strata.pricer.impl.swap.DiscountingRatePaymentPeriodPricer;
import com.opengamma.strata.pricer.rate.MarketDataRatesProvider;
import com.opengamma.strata.pricer.swap.DiscountingSwapProductPricer;
import com.opengamma.strata.pricer.swap.SwapDummyData;
import com.opengamma.strata.product.swap.RateAccrualPeriod;
import com.opengamma.strata.product.swap.RatePaymentPeriod;
import com.opengamma.strata.product.swap.ResolvedSwap;
import com.opengamma.strata.product.swap.ResolvedSwapLeg;
import com.opengamma.strata.product.swap.Swap;
import com.opengamma.strata.product.swap.SwapTrade;

/**
 * Test {@link SwapCalculationFunction}.
 */
@Test
public class SwapCalculationFunctionTest {

  public static final SwapTrade TRADE = SwapDummyData.SWAP_TRADE;

  private static final ReferenceData REF_DATA = ReferenceData.standard();
  private static final IborIndex INDEX = (IborIndex) TRADE.getProduct().allIndices().iterator().next();
  private static final Currency CURRENCY = TRADE.getProduct().getPayLeg().get().getCurrency();
  private static final LocalDate VAL_DATE = TRADE.getProduct().getStartDate().minusDays(7);

  //-------------------------------------------------------------------------
  public void test_group() {
    FunctionGroup<SwapTrade> test = SwapFunctionGroups.discounting();
    assertThat(test.configuredMeasures(TRADE)).contains(
        Measures.PAR_RATE,
        Measures.PAR_SPREAD,
        Measures.PRESENT_VALUE,
        Measures.EXPLAIN_PRESENT_VALUE,
        Measures.CASH_FLOWS,
        Measures.PV01,
        Measures.BUCKETED_PV01,
        Measures.BUCKETED_GAMMA_PV01,
        Measures.ACCRUED_INTEREST,
        Measures.LEG_INITIAL_NOTIONAL,
        Measures.LEG_PRESENT_VALUE,
        Measures.CURRENCY_EXPOSURE,
        Measures.CURRENT_CASH        );
    FunctionConfig<SwapTrade> config =
        SwapFunctionGroups.discounting().functionConfig(TRADE, Measures.PRESENT_VALUE).get();
    assertThat(config.createFunction()).isInstanceOf(SwapCalculationFunction.class);
  }

  public void test_requirementsAndCurrency() {
    SwapCalculationFunction function = new SwapCalculationFunction();
    Set<Measure> measures = function.supportedMeasures();
    FunctionRequirements reqs = function.requirements(TRADE, measures);
    assertThat(reqs.getOutputCurrencies()).containsOnly(CURRENCY);
    assertThat(reqs.getSingleValueRequirements()).isEqualTo(
        ImmutableSet.of(DiscountCurveKey.of(CURRENCY), IborIndexCurveKey.of(INDEX)));
    assertThat(reqs.getTimeSeriesRequirements()).isEqualTo(ImmutableSet.of(IndexRateKey.of(INDEX)));
    assertThat(function.naturalCurrency(TRADE)).isEqualTo(CURRENCY);
  }

  public void test_simpleMeasures() {
    SwapCalculationFunction function = new SwapCalculationFunction();
    CalculationMarketData md = marketData();
    MarketDataRatesProvider provider = MarketDataRatesProvider.of(md.scenario(0));
    DiscountingSwapProductPricer pricer = DiscountingSwapProductPricer.DEFAULT;
    ResolvedSwap resolved = TRADE.getProduct().resolve(REF_DATA);
    MultiCurrencyAmount expectedPv = pricer.presentValue(resolved, provider);
    double expectedParRate = pricer.parRate(resolved, provider);
    double expectedParSpread = pricer.parSpread(resolved, provider);
    ExplainMap expectedExplainPv = pricer.explainPresentValue(resolved, provider);
    CashFlows expectedCashFlows = pricer.cashFlows(resolved, provider);
    MultiCurrencyAmount expectedExposure = pricer.currencyExposure(resolved, provider);
    MultiCurrencyAmount expectedCash = pricer.currentCash(resolved, provider);

    Set<Measure> measures = ImmutableSet.of(Measures.PRESENT_VALUE,
        Measures.PRESENT_VALUE_MULTI_CCY,
        Measures.PAR_RATE,
        Measures.PAR_SPREAD,
        Measures.EXPLAIN_PRESENT_VALUE,
        Measures.CASH_FLOWS, Measures.CURRENCY_EXPOSURE, Measures.CURRENT_CASH);
    assertThat(function.calculate(TRADE, measures, md))
        .containsEntry(
            Measures.PRESENT_VALUE, Result.success(MultiCurrencyValuesArray.of(ImmutableList.of(expectedPv))))
        .containsEntry(
            Measures.PRESENT_VALUE_MULTI_CCY, Result.success(MultiCurrencyValuesArray.of(ImmutableList.of(expectedPv))))
        .containsEntry(
            Measures.PAR_RATE, Result.success(ValuesArray.of(ImmutableList.of(expectedParRate))))
        .containsEntry(
            Measures.PAR_SPREAD, Result.success(ValuesArray.of(ImmutableList.of(expectedParSpread))))
        .containsEntry(
            Measures.EXPLAIN_PRESENT_VALUE, Result.success(ScenarioResult.of(ImmutableList.of(expectedExplainPv))))
        .containsEntry(
            Measures.CASH_FLOWS, Result.success(ScenarioResult.of(ImmutableList.of(expectedCashFlows))))
        .containsEntry(
            Measures.CURRENCY_EXPOSURE, Result.success(MultiCurrencyValuesArray.of(ImmutableList.of(expectedExposure))))
        .containsEntry(
            Measures.CURRENT_CASH, Result.success(MultiCurrencyValuesArray.of(ImmutableList.of(expectedCash))));
  }

  public void test_pv01() {
    SwapCalculationFunction function = new SwapCalculationFunction();
    CalculationMarketData md = marketData();
    MarketDataRatesProvider provider = MarketDataRatesProvider.of(md.scenario(0));
    DiscountingSwapProductPricer pricer = DiscountingSwapProductPricer.DEFAULT;
    ResolvedSwap resolved = TRADE.getProduct().resolve(REF_DATA);
    PointSensitivities pvPointSens = pricer.presentValueSensitivity(resolved, provider).build();
    CurveCurrencyParameterSensitivities pvParamSens = provider.curveParameterSensitivity(pvPointSens);
    MultiCurrencyAmount expectedPv01 = pvParamSens.total().multipliedBy(1e-4);
    CurveCurrencyParameterSensitivities expectedBucketedPv01 = pvParamSens.multipliedBy(1e-4);

    Set<Measure> measures = ImmutableSet.of(Measures.PV01, Measures.BUCKETED_PV01);
    assertThat(function.calculate(TRADE, measures, md))
        .containsEntry(
            Measures.PV01, Result.success(MultiCurrencyValuesArray.of(ImmutableList.of(expectedPv01))))
        .containsEntry(
            Measures.BUCKETED_PV01, Result.success(ScenarioResult.of(ImmutableList.of(expectedBucketedPv01))));
  }

  public void test_accruedInterest() {
    SwapCalculationFunction function = new SwapCalculationFunction();
    Currency ccy = FIXED_RATECALC_SWAP_LEG.getCurrency();
    LocalDate valDate = FIXED_RATECALC_SWAP_LEG.getEndDate().minusDays(7);
    SwapTrade trade = SwapTrade.builder()
        .product(Swap.of(FIXED_RATECALC_SWAP_LEG))
        .build();

    Set<Measure> measures = ImmutableSet.of(Measures.ACCRUED_INTEREST);
    CalculationMarketData md = new TestMarketDataMap(valDate, ImmutableMap.of(), ImmutableMap.of());
    MarketDataRatesProvider provider = MarketDataRatesProvider.of(md.scenario(0));

    // create a period with altered end date, and price it
    ResolvedSwapLeg expanded = FIXED_RATECALC_SWAP_LEG.resolve(REF_DATA);
    RatePaymentPeriod rpp = (RatePaymentPeriod) expanded.getPaymentPeriods().get(expanded.getPaymentPeriods().size() - 1);
    RateAccrualPeriod rap = rpp.getAccrualPeriods().get(0);
    RateAccrualPeriod rap2 = rap.toBuilder()
        .endDate(valDate)
        .unadjustedEndDate(valDate)
        .yearFraction(DayCounts.ACT_365F.yearFraction(rap.getStartDate(), valDate))
        .build();
    RatePaymentPeriod rpp2 = rpp.toBuilder().accrualPeriods(rap2).build();
    CurrencyAmount expected = CurrencyAmount.of(
        ccy, DiscountingRatePaymentPeriodPricer.DEFAULT.forecastValue(rpp2, provider));

    MultiCurrencyValuesArray expectedArray = MultiCurrencyValuesArray.of(MultiCurrencyAmount.of(expected));
    assertThat(function.calculate(trade, measures, md))
        .containsEntry(Measures.ACCRUED_INTEREST, Result.success(expectedArray));
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
    coverPrivateConstructor(SwapFunctionGroups.class);
    coverPrivateConstructor(SwapMeasureCalculations.class);
  }

}
