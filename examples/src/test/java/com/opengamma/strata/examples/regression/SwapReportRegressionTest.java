/**
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 * 
 * Please see distribution for license.
 */
package com.opengamma.strata.examples.regression;

import static com.opengamma.strata.function.StandardComponents.marketDataFactory;

import java.time.LocalDate;
import java.util.List;

import org.testng.annotations.Test;

import com.google.common.collect.ImmutableList;
import com.opengamma.strata.basics.PayReceive;
import com.opengamma.strata.basics.Trade;
import com.opengamma.strata.basics.currency.Currency;
import com.opengamma.strata.basics.date.BusinessDayAdjustment;
import com.opengamma.strata.basics.date.BusinessDayConventions;
import com.opengamma.strata.basics.date.DayCounts;
import com.opengamma.strata.basics.date.DaysAdjustment;
import com.opengamma.strata.basics.date.HolidayCalendars;
import com.opengamma.strata.basics.index.IborIndices;
import com.opengamma.strata.basics.schedule.Frequency;
import com.opengamma.strata.basics.schedule.PeriodicSchedule;
import com.opengamma.strata.calc.CalculationRules;
import com.opengamma.strata.calc.Column;
import com.opengamma.strata.calc.config.Measure;
import com.opengamma.strata.calc.config.ReportingRules;
import com.opengamma.strata.calc.marketdata.MarketEnvironment;
import com.opengamma.strata.calc.marketdata.config.MarketDataConfig;
import com.opengamma.strata.calc.runner.CalculationRunner;
import com.opengamma.strata.calc.runner.CalculationRunnerFactory;
import com.opengamma.strata.calc.runner.Results;
import com.opengamma.strata.collect.id.StandardId;
import com.opengamma.strata.examples.data.ExampleData;
import com.opengamma.strata.examples.marketdata.ExampleMarketData;
import com.opengamma.strata.examples.marketdata.ExampleMarketDataBuilder;
import com.opengamma.strata.function.StandardComponents;
import com.opengamma.strata.product.TradeInfo;
import com.opengamma.strata.product.swap.FixedRateCalculation;
import com.opengamma.strata.product.swap.IborRateCalculation;
import com.opengamma.strata.product.swap.NotionalSchedule;
import com.opengamma.strata.product.swap.PaymentSchedule;
import com.opengamma.strata.product.swap.RateCalculationSwapLeg;
import com.opengamma.strata.product.swap.Swap;
import com.opengamma.strata.product.swap.SwapLeg;
import com.opengamma.strata.product.swap.SwapTrade;
import com.opengamma.strata.report.ReportCalculationResults;
import com.opengamma.strata.report.trade.TradeReport;
import com.opengamma.strata.report.trade.TradeReportTemplate;

/**
 * Regression test for an example swap report.
 */
@Test
public class SwapReportRegressionTest {

  /**
   * Tests the full set of results against a golden copy.
   */
  public void testResults() {
    List<Trade> trades = ImmutableList.of(createTrade1());

    List<Column> columns = ImmutableList.of(
        Column.of(Measure.LEG_INITIAL_NOTIONAL),
        Column.of(Measure.PRESENT_VALUE),
        Column.of(Measure.LEG_PRESENT_VALUE),
        Column.of(Measure.PV01),
        Column.of(Measure.ACCRUED_INTEREST));

    ExampleMarketDataBuilder marketDataBuilder = ExampleMarketData.builder();

    CalculationRules rules = CalculationRules.builder()
        .pricingRules(StandardComponents.pricingRules())
        .marketDataRules(marketDataBuilder.rules())
        .reportingRules(ReportingRules.fixedCurrency(Currency.USD))
        .build();

    LocalDate valuationDate = LocalDate.of(2009, 7, 31);
    MarketEnvironment marketSnapshot = marketDataBuilder.buildSnapshot(valuationDate);

    CalculationRunner runner = CalculationRunnerFactory.ofSingleThreaded()
        .createWithMarketDataBuilder(trades, columns, rules, marketDataFactory(), MarketDataConfig.empty());
    Results results = runner.calculateSingleScenario(marketSnapshot);

    ReportCalculationResults calculationResults = ReportCalculationResults.of(
        valuationDate,
        trades,
        columns,
        results);

    TradeReportTemplate reportTemplate = ExampleData.loadTradeReportTemplate("swap-report-regression-test-template");
    TradeReport tradeReport = TradeReport.of(calculationResults, reportTemplate);

    String expectedResults = ExampleData.loadExpectedResults("swap-report");

    TradeReportRegressionTestUtils.assertAsciiTableEquals(tradeReport.toAsciiTableString(), expectedResults);
  }

  private static Trade createTrade1() {
    NotionalSchedule notional = NotionalSchedule.of(Currency.USD, 12_000_000);

    PeriodicSchedule accrual = PeriodicSchedule.builder()
        .startDate(LocalDate.of(2006, 2, 24))
        .endDate(LocalDate.of(2011, 2, 24))
        .frequency(Frequency.P3M)
        .businessDayAdjustment(BusinessDayAdjustment.of(BusinessDayConventions.MODIFIED_FOLLOWING, HolidayCalendars.USNY))
        .build();

    PaymentSchedule payment = PaymentSchedule.builder()
        .paymentFrequency(Frequency.P3M)
        .paymentDateOffset(DaysAdjustment.ofBusinessDays(2, HolidayCalendars.USNY))
        .build();

    SwapLeg payLeg = RateCalculationSwapLeg.builder()
        .payReceive(PayReceive.PAY)
        .accrualSchedule(accrual)
        .paymentSchedule(payment)
        .notionalSchedule(notional)
        .calculation(FixedRateCalculation.of(0.05004, DayCounts.ACT_360))
        .build();

    SwapLeg receiveLeg = RateCalculationSwapLeg.builder()
        .payReceive(PayReceive.RECEIVE)
        .accrualSchedule(accrual)
        .paymentSchedule(payment)
        .notionalSchedule(notional)
        .calculation(IborRateCalculation.of(IborIndices.USD_LIBOR_3M))
        .build();

    return SwapTrade.builder()
        .product(Swap.builder()
            .legs(payLeg, receiveLeg)
            .build())
        .tradeInfo(TradeInfo.builder()
            .id(StandardId.of("mn", "14248"))
            .counterparty(StandardId.of("mn", "Dealer A"))
            .settlementDate(LocalDate.of(2006, 2, 24))
            .build())
        .build();
  }

}
