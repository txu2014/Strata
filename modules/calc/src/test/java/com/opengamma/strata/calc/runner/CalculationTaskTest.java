/**
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.calc.runner;

import static com.opengamma.strata.collect.CollectProjectAssertions.assertThat;
import static com.opengamma.strata.collect.Guavate.toImmutableList;
import static com.opengamma.strata.collect.TestHelper.date;
import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Supplier;

import org.testng.annotations.Test;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.opengamma.strata.basics.CalculationTarget;
import com.opengamma.strata.basics.currency.Currency;
import com.opengamma.strata.basics.currency.FxRate;
import com.opengamma.strata.basics.market.FxRateId;
import com.opengamma.strata.basics.market.MarketDataFeed;
import com.opengamma.strata.basics.market.MarketDataId;
import com.opengamma.strata.basics.market.ObservableId;
import com.opengamma.strata.basics.market.TestObservableKey;
import com.opengamma.strata.calc.config.Measure;
import com.opengamma.strata.calc.config.ReportingRules;
import com.opengamma.strata.calc.marketdata.CalculationEnvironment;
import com.opengamma.strata.calc.marketdata.CalculationMarketData;
import com.opengamma.strata.calc.marketdata.FunctionRequirements;
import com.opengamma.strata.calc.marketdata.MarketDataRequirements;
import com.opengamma.strata.calc.marketdata.MarketEnvironment;
import com.opengamma.strata.calc.marketdata.TestId;
import com.opengamma.strata.calc.marketdata.TestKey;
import com.opengamma.strata.calc.marketdata.TestMapping;
import com.opengamma.strata.calc.marketdata.mapping.DefaultMarketDataMappings;
import com.opengamma.strata.calc.marketdata.mapping.MarketDataMappings;
import com.opengamma.strata.calc.runner.function.CalculationFunction;
import com.opengamma.strata.calc.runner.function.result.CurrencyValuesArray;
import com.opengamma.strata.calc.runner.function.result.ScenarioResult;
import com.opengamma.strata.collect.array.DoubleArray;
import com.opengamma.strata.collect.result.FailureReason;
import com.opengamma.strata.collect.result.Result;

/**
 * Test {@link CalculationTask}.
 */
@Test
public class CalculationTaskTest {

  private static final MarketDataMappings MAPPINGS = MarketDataMappings.of(MarketDataFeed.NONE);
  private static final ReportingRules REPORTING_RULES_EMPTY = ReportingRules.empty();
  private static final ReportingRules REPORTING_RULES_USD = ReportingRules.fixedCurrency(Currency.USD);
  private static final TestTarget TARGET = new TestTarget();
  private static final Measure MEASURE = Measure.of("PV");
  private static final Set<Measure> MEASURES = ImmutableSet.of(MEASURE);

  public void requirements() {
    MarketDataFeed marketDataFeed = MarketDataFeed.of("MarketDataVendor");
    MarketDataMappings marketDataMappings =
        DefaultMarketDataMappings.builder()
            .mappings(ImmutableMap.of(TestKey.class, new TestMapping("foo")))
            .marketDataFeed(marketDataFeed)
            .build();
    CalculationTask task =
        CalculationTask.of(new TestTarget(), MEASURE, 0, 0, new TestFunction(), marketDataMappings, REPORTING_RULES_EMPTY);
    MarketDataRequirements requirements = task.requirements();
    Set<? extends MarketDataId<?>> nonObservables = requirements.getNonObservables();
    ImmutableSet<? extends ObservableId> observables = requirements.getObservables();
    ImmutableSet<ObservableId> timeSeries = requirements.getTimeSeries();

    MarketDataId<?> timeSeriesId = TestObservableKey.of("3").toMarketDataId(marketDataFeed);
    assertThat(timeSeries).hasSize(1);
    assertThat(timeSeries.iterator().next()).isEqualTo(timeSeriesId);

    MarketDataId<?> nonObservableId = TestId.of("1");
    assertThat(nonObservables).hasSize(1);
    assertThat(nonObservables.iterator().next()).isEqualTo(nonObservableId);

    MarketDataId<?> observableId = TestObservableKey.of("2").toMarketDataId(marketDataFeed);
    assertThat(observables).hasSize(1);
    assertThat(observables.iterator().next()).isEqualTo(observableId);
  }

  /**
   * Test that the result is converted to the reporting currency if it implements CurrencyConvertible and
   * the FX rates are available in the market data. The reporting currency is taken from the reporting rules.
   */
  public void convertResultCurrencyUsingReportingRules() {
    DoubleArray values = DoubleArray.of(1, 2, 3);
    List<FxRate> rates = ImmutableList.of(1.61, 1.62, 1.63).stream()
        .map(rate -> FxRate.of(Currency.GBP, Currency.USD, rate))
        .collect(toImmutableList());
    CurrencyValuesArray list = CurrencyValuesArray.of(Currency.GBP, values);
    CalculationEnvironment marketData = MarketEnvironment.builder()
        .valuationDate(date(2011, 3, 8))
        .addValue(FxRateId.of(Currency.GBP, Currency.USD), rates)
        .build();
    ConvertibleFunction fn = ConvertibleFunction.of(() -> list);
    CalculationTask task = CalculationTask.of(TARGET, MEASURE, 0, 0, fn, MAPPINGS, REPORTING_RULES_USD);

    DoubleArray expectedValues = DoubleArray.of(1 * 1.61, 2 * 1.62, 3 * 1.63);
    CurrencyValuesArray expectedArray = CurrencyValuesArray.of(Currency.USD, expectedValues);

    CalculationResult calculationResult = task.execute(marketData);
    Result<?> result = calculationResult.getResult();
    assertThat(result).hasValue(expectedArray);
  }

  /**
   * Test that the result is converted to the reporting currency if it implements CurrencyConvertible and
   * the FX rates are available in the market data. The default reporting currency is taken from the function.
   */
  public void convertResultCurrencyUsingDefaultReportingCurrency() {
    DoubleArray values = DoubleArray.of(1, 2, 3);
    List<FxRate> rates = ImmutableList.of(1.61, 1.62, 1.63).stream()
        .map(rate -> FxRate.of(Currency.GBP, Currency.USD, rate))
        .collect(toImmutableList());
    CurrencyValuesArray list = CurrencyValuesArray.of(Currency.GBP, values);
    CalculationEnvironment marketData = MarketEnvironment.builder()
        .valuationDate(date(2011, 3, 8))
        .addValue(FxRateId.of(Currency.GBP, Currency.USD), rates)
        .build();
    ConvertibleFunction fn = ConvertibleFunction.of(() -> list, Currency.USD);
    CalculationTask task = CalculationTask.of(TARGET, MEASURE, 0, 0, fn, MAPPINGS, REPORTING_RULES_EMPTY);

    DoubleArray expectedValues = DoubleArray.of(1 * 1.61, 2 * 1.62, 3 * 1.63);
    CurrencyValuesArray expectedArray = CurrencyValuesArray.of(Currency.USD, expectedValues);

    CalculationResult calculationResult = task.execute(marketData);
    Result<?> result = calculationResult.getResult();
    assertThat(result).hasValue(expectedArray);
  }

  /**
   * Test that the result is returned unchanged if it is a failure.
   */
  public void convertResultCurrencyFailure() {
    ConvertibleFunction fn = ConvertibleFunction.of(() -> { throw new RuntimeException("This is a failure"); });
    CalculationTask task = CalculationTask.of(TARGET, MEASURE, 0, 0, fn, MAPPINGS, REPORTING_RULES_USD);
    CalculationEnvironment marketData = MarketEnvironment.builder().valuationDate(date(2011, 3, 8)).build();

    CalculationResult calculationResult = task.execute(marketData);
    Result<?> result = calculationResult.getResult();
    assertThat(result).hasFailureMessageMatching("This is a failure");
  }

  /**
   * Test the result is returned unchanged if it is not CurrencyConvertible.
   */
  public void convertResultCurrencyNotConvertible() {
    TestFunction fn = new TestFunction();
    CalculationTask task = CalculationTask.of(TARGET, MEASURE, 0, 0, fn, MAPPINGS, REPORTING_RULES_USD);
    CalculationEnvironment marketData = MarketEnvironment.builder().valuationDate(date(2011, 3, 8)).build();

    CalculationResult calculationResult = task.execute(marketData);
    Result<?> result = calculationResult.getResult();
    assertThat(result).hasValue(ScenarioResult.of("bar"));
  }

  /**
   * Test a failure is returned for a convertible value if there is no reporting currency.
   */
  public void convertResultCurrencyNoReportingCurrency() {
    DoubleArray values = DoubleArray.of(1, 2, 3);
    List<FxRate> rates = ImmutableList.of(1.61, 1.62, 1.63).stream()
        .map(rate -> FxRate.of(Currency.GBP, Currency.USD, rate))
        .collect(toImmutableList());
    CurrencyValuesArray list = CurrencyValuesArray.of(Currency.GBP, values);
    CalculationEnvironment marketData = MarketEnvironment.builder()
        .valuationDate(date(2011, 3, 8))
        .addValue(FxRateId.of(Currency.GBP, Currency.USD), rates)
        .build();
    ConvertibleFunction fn = ConvertibleFunction.of(() -> list);
    ReportingRules reportingRules = REPORTING_RULES_EMPTY;
    CalculationTask task = CalculationTask.of(TARGET, MEASURE, 0, 0, fn, MAPPINGS, reportingRules);

    CalculationResult calculationResult = task.execute(marketData);
    Result<?> result = calculationResult.getResult();
    assertThat(result).hasFailureMessageMatching("No reporting currency available.*");
  }

  /**
   * Test a non-convertible result is returned even if there is no reporting currency.
   */
  public void nonConvertibleResultReturnedWhenNoReportingCurrency() {
    TestFunction fn = new TestFunction();
    CalculationTask task = CalculationTask.of(TARGET, MEASURE, 0, 0, fn, MAPPINGS, REPORTING_RULES_EMPTY);
    CalculationEnvironment marketData = MarketEnvironment.builder().valuationDate(date(2011, 3, 8)).build();

    CalculationResult calculationResult = task.execute(marketData);
    Result<?> result = calculationResult.getResult();
    assertThat(result).hasValue(ScenarioResult.of("bar"));
  }

  /**
   * Test that a failure is returned if currency conversion fails.
   */
  public void convertResultCurrencyConversionFails() {
    DoubleArray values = DoubleArray.of(1, 2, 3);
    CurrencyValuesArray list = CurrencyValuesArray.of(Currency.GBP, values);
    // Market data doesn't include FX rates, conversion to USD will fail
    CalculationEnvironment marketData = MarketEnvironment.builder().valuationDate(date(2011, 3, 8)).build();
    ConvertibleFunction fn = ConvertibleFunction.of(() -> list);
    CalculationTask task = CalculationTask.of(TARGET, MEASURE, 0, 0, fn, MAPPINGS, REPORTING_RULES_USD);

    CalculationResult calculationResult = task.execute(marketData);
    Result<?> result = calculationResult.getResult();
    assertThat(result).hasFailureMessageMatching("Failed to convert value .* to currency USD");
  }

  /**
   * Tests that executing a function wraps its return value in a success result.
   */
  public void execute() {
    SupplierFunction<String> fn = SupplierFunction.of(() -> "foo");
    CalculationTask task = CalculationTask.of(TARGET, MEASURE, 0, 0, fn, MAPPINGS, REPORTING_RULES_USD);
    CalculationEnvironment marketData = MarketEnvironment.builder().valuationDate(date(2011, 3, 8)).build();

    CalculationResult calculationResult = task.execute(marketData);
    Result<?> result = calculationResult.getResult();
    assertThat(result).hasValue(ScenarioResult.of("foo"));
  }

  /**
   * Tests that executing a function that throws an exception wraps the exception in a failure result.
   */
  public void executeException() {
    SupplierFunction<String> fn = SupplierFunction.of(() -> {
      throw new IllegalArgumentException("foo");
    });
    CalculationTask task = CalculationTask.of(TARGET, MEASURE, 0, 0, fn, MAPPINGS, REPORTING_RULES_USD);
    CalculationEnvironment marketData = MarketEnvironment.builder()
        .valuationDate(date(2011, 3, 8)).build();

    CalculationResult calculationResult = task.execute(marketData);
    Result<?> result = calculationResult.getResult();
    assertThat(result).isFailure(FailureReason.ERROR).hasFailureMessageMatching("foo");
  }

  /**
   * Tests that executing a function that returns a success result returns the underlying result without wrapping it.
   */
  public void executeSuccessResultValue() {
    SupplierFunction<Result<ScenarioResult<String>>> fn =
        SupplierFunction.of(() -> Result.success(ScenarioResult.of("foo")));
    CalculationTask task = CalculationTask.of(TARGET, MEASURE, 0, 0, fn, MAPPINGS, REPORTING_RULES_USD);
    CalculationEnvironment marketData = MarketEnvironment.builder().valuationDate(date(2011, 3, 8)).build();

    CalculationResult calculationResult = task.execute(marketData);
    Result<?> result = calculationResult.getResult();
    assertThat(result).hasValue(ScenarioResult.of("foo"));
  }

  /**
   * Tests that executing a function that returns a failure result returns the underlying result without wrapping it.
   */
  public void executeFailureResultValue() {
    SupplierFunction<Result<String>> fn =
        SupplierFunction.of(() -> Result.failure(FailureReason.NOT_APPLICABLE, "bar"));
    CalculationTask task = CalculationTask.of(TARGET, MEASURE, 0, 0, fn, MAPPINGS, REPORTING_RULES_USD);
    CalculationEnvironment marketData = MarketEnvironment.builder().valuationDate(date(2011, 3, 8)).build();

    CalculationResult calculationResult = task.execute(marketData);
    Result<?> result = calculationResult.getResult();
    assertThat(result).isFailure(FailureReason.NOT_APPLICABLE).hasFailureMessageMatching("bar");
  }

  /**
   * Tests that requirements are added for the FX rates needed to convert the results into the reporting currency.
   */
  public void fxConversionRequirements() {
    OutputCurrenciesFunction fn = new OutputCurrenciesFunction();
    CalculationTask task = CalculationTask.of(TARGET, MEASURE, 0, 0, fn, MAPPINGS, REPORTING_RULES_USD);
    MarketDataRequirements requirements = task.requirements();

    assertThat(requirements.getNonObservables()).containsOnly(
        FxRateId.of(Currency.GBP, Currency.USD),
        FxRateId.of(Currency.EUR, Currency.USD));
  }

  public void testToString() {
    OutputCurrenciesFunction fn = new OutputCurrenciesFunction();
    CalculationTask task = CalculationTask.of(TARGET, MEASURE, 1, 2, fn, MAPPINGS, REPORTING_RULES_USD);
    assertThat(task.toString()).isEqualTo("CalculationTask[cell=(1, 2), measure=PV]");
  }

  //-------------------------------------------------------------------------
  private static class TestTarget implements CalculationTarget {
  }

  //-------------------------------------------------------------------------
  /**
   * Function that returns a value that is not currency convertible.
   */
  public static final class TestFunction implements CalculationFunction<TestTarget> {

    @Override
    public Set<Measure> supportedMeasures() {
      return MEASURES;
    }

    @Override
    public FunctionRequirements requirements(TestTarget target, Set<Measure> measures) {
      return FunctionRequirements.builder()
          .singleValueRequirements(
              ImmutableSet.of(
                  TestKey.of("1"),
                  TestObservableKey.of("2")))
          .timeSeriesRequirements(TestObservableKey.of("3"))
          .build();
    }

    @Override
    public Map<Measure, Result<?>> calculate(
        TestTarget target,
        Set<Measure> measures,
        CalculationMarketData marketData) {

      ScenarioResult<String> array = ScenarioResult.of("bar");
      return ImmutableMap.of(MEASURE, Result.success(array));
    }
  }

  //-------------------------------------------------------------------------
  /**
   * Function that returns a value that is currency convertible.
   */
  private static final class ConvertibleFunction
      implements CalculationFunction<TestTarget> {

    private final Supplier<CurrencyValuesArray> supplier;
    private final Optional<Currency> reportingCurrency;

    public static ConvertibleFunction of(Supplier<CurrencyValuesArray> supplier) {
      return new ConvertibleFunction(supplier, Optional.<Currency>empty());
    }

    public static ConvertibleFunction of(Supplier<CurrencyValuesArray> supplier, Currency reportingCurrency) {
      return new ConvertibleFunction(supplier, Optional.of(reportingCurrency));
    }

    private ConvertibleFunction(Supplier<CurrencyValuesArray> supplier, Optional<Currency> reportingCurrency) {
      this.supplier = supplier;
      this.reportingCurrency = reportingCurrency;
    }

    @Override
    public Set<Measure> supportedMeasures() {
      return MEASURES;
    }

    @Override
    public Optional<Currency> defaultReportingCurrency(TestTarget target) {
      return reportingCurrency;
    }

    @Override
    public FunctionRequirements requirements(TestTarget target, Set<Measure> measures) {
      return FunctionRequirements.empty();
    }

    @Override
    public Map<Measure, Result<?>> calculate(
        TestTarget target,
        Set<Measure> measures,
        CalculationMarketData marketData) {

      return ImmutableMap.of(MEASURE, Result.success(supplier.get()));
    }
  }

  //-------------------------------------------------------------------------
  /**
   * Function that returns a value from a Supplier.
   */
  private static final class SupplierFunction<T> implements CalculationFunction<TestTarget> {

    private final Supplier<T> supplier;

    public static <T> SupplierFunction<T> of(Supplier<T> supplier) {
      return new SupplierFunction<>(supplier);
    }

    private SupplierFunction(Supplier<T> supplier) {
      this.supplier = supplier;
    }

    @Override
    public Set<Measure> supportedMeasures() {
      return MEASURES;
    }

    @Override
    public FunctionRequirements requirements(TestTarget target, Set<Measure> measures) {
      return FunctionRequirements.empty();
    }

    @SuppressWarnings("unchecked")
    @Override
    public Map<Measure, Result<?>> calculate(
        TestTarget target,
        Set<Measure> measures,
        CalculationMarketData marketData) {

      T obj = supplier.get();
      if (obj instanceof Result<?>) {
        return ImmutableMap.of(MEASURE, (Result<?>) obj);
      }
      ScenarioResult<Object> array = ScenarioResult.of(obj);
      return ImmutableMap.of(MEASURE, Result.success(array));
    }
  }

  //-------------------------------------------------------------------------
  /**
   * Function that returns requirements containing output currencies.
   */
  private static final class OutputCurrenciesFunction implements CalculationFunction<TestTarget> {

    @Override
    public Set<Measure> supportedMeasures() {
      return MEASURES;
    }

    @Override
    public FunctionRequirements requirements(TestTarget target, Set<Measure> measures) {
      return FunctionRequirements.builder()
          .outputCurrencies(Currency.GBP, Currency.EUR, Currency.USD)
          .build();
    }

    @Override
    public Map<Measure, Result<?>> calculate(
        TestTarget target,
        Set<Measure> measures,
        CalculationMarketData marketData) {

      throw new UnsupportedOperationException("calculate not implemented");
    }
  }

}
