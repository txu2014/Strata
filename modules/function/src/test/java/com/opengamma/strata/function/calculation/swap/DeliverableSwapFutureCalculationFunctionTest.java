/**
 * Copyright (C) 2016 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.function.calculation.swap;

import static com.opengamma.strata.basics.date.BusinessDayConventions.MODIFIED_FOLLOWING;
import static com.opengamma.strata.basics.date.DayCounts.ACT_360;
import static com.opengamma.strata.collect.TestHelper.coverPrivateConstructor;
import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;
import java.util.Set;

import org.testng.annotations.Test;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.opengamma.strata.basics.PayReceive;
import com.opengamma.strata.basics.currency.Currency;
import com.opengamma.strata.basics.currency.CurrencyAmount;
import com.opengamma.strata.basics.currency.MultiCurrencyAmount;
import com.opengamma.strata.basics.date.BusinessDayAdjustment;
import com.opengamma.strata.basics.date.DayCounts;
import com.opengamma.strata.basics.date.HolidayCalendar;
import com.opengamma.strata.basics.date.HolidayCalendars;
import com.opengamma.strata.basics.index.IborIndex;
import com.opengamma.strata.basics.index.IborIndices;
import com.opengamma.strata.basics.schedule.Frequency;
import com.opengamma.strata.calc.config.FunctionConfig;
import com.opengamma.strata.calc.config.Measure;
import com.opengamma.strata.calc.config.Measures;
import com.opengamma.strata.calc.config.pricing.FunctionGroup;
import com.opengamma.strata.calc.marketdata.CalculationMarketData;
import com.opengamma.strata.calc.marketdata.FunctionRequirements;
import com.opengamma.strata.calc.runner.function.result.CurrencyValuesArray;
import com.opengamma.strata.calc.runner.function.result.MultiCurrencyValuesArray;
import com.opengamma.strata.calc.runner.function.result.ScenarioResult;
import com.opengamma.strata.collect.id.StandardId;
import com.opengamma.strata.collect.result.Result;
import com.opengamma.strata.function.marketdata.curve.TestMarketDataMap;
import com.opengamma.strata.market.curve.ConstantNodalCurve;
import com.opengamma.strata.market.curve.Curve;
import com.opengamma.strata.market.curve.CurveCurrencyParameterSensitivities;
import com.opengamma.strata.market.curve.Curves;
import com.opengamma.strata.market.key.DiscountCurveKey;
import com.opengamma.strata.market.key.IborIndexCurveKey;
import com.opengamma.strata.market.key.IndexRateKey;
import com.opengamma.strata.market.key.QuoteKey;
import com.opengamma.strata.market.sensitivity.PointSensitivities;
import com.opengamma.strata.pricer.rate.MarketDataRatesProvider;
import com.opengamma.strata.pricer.swap.DiscountingDeliverableSwapFutureTradePricer;
import com.opengamma.strata.product.Security;
import com.opengamma.strata.product.SecurityLink;
import com.opengamma.strata.product.UnitSecurity;
import com.opengamma.strata.product.swap.DeliverableSwapFuture;
import com.opengamma.strata.product.swap.DeliverableSwapFutureTrade;
import com.opengamma.strata.product.swap.Swap;
import com.opengamma.strata.product.swap.SwapLeg;
import com.opengamma.strata.product.swap.type.FixedRateSwapLegConvention;
import com.opengamma.strata.product.swap.type.IborRateSwapLegConvention;

/**
 * Test {@link DeliverableSwapFutureCalculationFunction}.
 */
@Test
public class DeliverableSwapFutureCalculationFunctionTest {

  private static final HolidayCalendar CALENDAR = HolidayCalendars.SAT_SUN;
  private static final BusinessDayAdjustment BDA_MF = BusinessDayAdjustment.of(MODIFIED_FOLLOWING, CALENDAR);
  private static final SwapLeg FIXED_LEG =
      FixedRateSwapLegConvention.of(Currency.GBP, DayCounts.ACT_360, Frequency.P6M, BDA_MF)
          .toLeg(LocalDate.of(2013, 6, 30), LocalDate.of(2016, 6, 30), PayReceive.RECEIVE, 1, 0.001);
  private static final SwapLeg IBOR_LEG =
      IborRateSwapLegConvention.of(IborIndices.GBP_LIBOR_6M)
          .toLeg(LocalDate.of(2013, 6, 30), LocalDate.of(2016, 6, 30), PayReceive.PAY, 1);
  private static final Swap SWAP = Swap.of(FIXED_LEG, IBOR_LEG);
  private static final LocalDate LAST_TRADE = LocalDate.of(2013, 6, 17);
  private static final LocalDate DELIVERY = LocalDate.of(2013, 6, 19);
  private static final double NOTIONAL = 100000;
  private static final DeliverableSwapFuture FUTURE = DeliverableSwapFuture.builder()
      .deliveryDate(DELIVERY)
      .lastTradeDate(LAST_TRADE)
      .notional(NOTIONAL)
      .underlyingSwap(SWAP)
      .build();
  private static final StandardId DSF_ID = StandardId.of("OG-Ticker", "DSF1");
  private static final Security<DeliverableSwapFuture> DSF_SECURITY = UnitSecurity
      .builder(FUTURE)
      .standardId(DSF_ID)
      .build();
  private static final SecurityLink<DeliverableSwapFuture> DSF_SECURITY_LINK = SecurityLink.resolved(DSF_SECURITY);
  private static final double TRADE_PRICE = 0.98 + 31.0 / 32.0 / 100.0; // price quoted in 32nd of 1%
  private static final double REF_PRICE = 0.98 + 30.0 / 32.0 / 100.0; // price quoted in 32nd of 1%
  private static final double MARKET_PRICE = REF_PRICE * 100;
  private static final long QUANTITY = 1234L;
  private static final DeliverableSwapFutureTrade TRADE = DeliverableSwapFutureTrade.builder()
      .quantity(QUANTITY)
      .securityLink(DSF_SECURITY_LINK)
      .tradePrice(TRADE_PRICE)
      .build();
  private static final Currency CURRENCY = SWAP.getPayLeg().get().getCurrency();
  private static final IborIndex INDEX = (IborIndex) SWAP.allIndices().iterator().next();
  private static final LocalDate VAL_DATE = LAST_TRADE.minusDays(7);

  //-------------------------------------------------------------------------
  public void test_group() {
    FunctionGroup<DeliverableSwapFutureTrade> test = DeliverableSwapFutureFunctionGroups.discounting();
    assertThat(test.configuredMeasures(TRADE)).contains(
        Measures.PRESENT_VALUE,
        Measures.PV01,
        Measures.BUCKETED_PV01);
    FunctionConfig<DeliverableSwapFutureTrade> config =
        DeliverableSwapFutureFunctionGroups.discounting().functionConfig(TRADE, Measures.PRESENT_VALUE).get();
    assertThat(config.createFunction()).isInstanceOf(DeliverableSwapFutureCalculationFunction.class);
  }

  public void test_requirementsAndCurrency() {
    DeliverableSwapFutureCalculationFunction function = new DeliverableSwapFutureCalculationFunction();
    Set<Measure> measures = function.supportedMeasures();
    FunctionRequirements reqs = function.requirements(TRADE, measures);
    assertThat(reqs.getOutputCurrencies()).containsOnly(CURRENCY);
    assertThat(reqs.getSingleValueRequirements()).isEqualTo(
        ImmutableSet.of(
            QuoteKey.of(DSF_ID),
            DiscountCurveKey.of(CURRENCY),
            IborIndexCurveKey.of(INDEX)));
    assertThat(reqs.getTimeSeriesRequirements()).isEqualTo(ImmutableSet.of(IndexRateKey.of(INDEX)));
    assertThat(function.naturalCurrency(TRADE)).isEqualTo(CURRENCY);
  }

  public void test_simpleMeasures() {
    DeliverableSwapFutureCalculationFunction function = new DeliverableSwapFutureCalculationFunction();
    CalculationMarketData md = marketData();
    MarketDataRatesProvider provider = MarketDataRatesProvider.of(md.scenario(0));
    DiscountingDeliverableSwapFutureTradePricer pricer = DiscountingDeliverableSwapFutureTradePricer.DEFAULT;
    CurrencyAmount expectedPv = pricer.presentValue(TRADE, provider, REF_PRICE);

    Set<Measure> measures = ImmutableSet.of(Measures.PRESENT_VALUE, Measures.PRESENT_VALUE_MULTI_CCY);
    assertThat(function.calculate(TRADE, measures, md))
        .containsEntry(
            Measures.PRESENT_VALUE, Result.success(CurrencyValuesArray.of(ImmutableList.of(expectedPv))))
        .containsEntry(
            Measures.PRESENT_VALUE_MULTI_CCY, Result.success(CurrencyValuesArray.of(ImmutableList.of(expectedPv))));
  }

  public void test_pv01() {
    DeliverableSwapFutureCalculationFunction function = new DeliverableSwapFutureCalculationFunction();
    CalculationMarketData md = marketData();
    MarketDataRatesProvider provider = MarketDataRatesProvider.of(md.scenario(0));
    DiscountingDeliverableSwapFutureTradePricer pricer = DiscountingDeliverableSwapFutureTradePricer.DEFAULT;
    PointSensitivities pvPointSens = pricer.presentValueSensitivity(TRADE, provider);
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

  //-------------------------------------------------------------------------
  private CalculationMarketData marketData() {
    Curve curve = ConstantNodalCurve.of(Curves.discountFactors("Test", ACT_360), 0.99);
    TestMarketDataMap md = new TestMarketDataMap(
        VAL_DATE,
        ImmutableMap.of(
            DiscountCurveKey.of(CURRENCY), curve,
            IborIndexCurveKey.of(INDEX), curve,
            QuoteKey.of(DSF_ID), MARKET_PRICE),
        ImmutableMap.of());
    return md;
  }

  //-------------------------------------------------------------------------
  public void coverage() {
    coverPrivateConstructor(DeliverableSwapFutureFunctionGroups.class);
    coverPrivateConstructor(DeliverableSwapFutureMeasureCalculations.class);
  }

}
