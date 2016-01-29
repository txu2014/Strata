/**
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.calc.config.pricing;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Map;
import java.util.Optional;
import java.util.Set;

import org.testng.annotations.Test;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.opengamma.strata.basics.CalculationTarget;
import com.opengamma.strata.calc.config.FunctionConfig;
import com.opengamma.strata.calc.config.Measure;
import com.opengamma.strata.calc.marketdata.CalculationMarketData;
import com.opengamma.strata.calc.marketdata.FunctionRequirements;
import com.opengamma.strata.calc.runner.function.CalculationFunction;
import com.opengamma.strata.calc.runner.function.result.ScenarioResult;
import com.opengamma.strata.collect.result.Result;

/**
 * Test {@link PricingRule}.
 */
@Test
public class PricingRuleTest {

  private static final Measure MEASURE1 = Measure.of("1");
  private static final Measure MEASURE2 = Measure.of("2");
  private static final Measure MEASURE3 = Measure.of("3");

  private static final FunctionGroup<TestTrade1> GROUP =
      DefaultFunctionGroup.builder(TestTrade1.class)
          .name("GroupName")
          .addFunction(MEASURE1, FunctionConfig.of(TestFunction1.class))
          .addFunction(MEASURE2, FunctionConfig.of(TestFunction2.class))
          .build();

  private static final PricingRule<TestTrade1> PRICING_RULE =
      PricingRule.builder(TestTrade1.class)
          .functionGroup(GROUP)
          .addArgument("foo", "bar")
          .addMeasures(MEASURE1, MEASURE2)
          .build();

  public void groupAvailable() {
    Optional<ConfiguredFunctionGroup> result = PRICING_RULE.functionGroup(new TestTrade1(), MEASURE1);
    assertThat(result).hasValue(ConfiguredFunctionGroup.of(GROUP, ImmutableMap.of("foo", "bar")));
  }

  public void differentTargetType() {
    Optional<ConfiguredFunctionGroup> result = PRICING_RULE.functionGroup(new TestTrade2(), MEASURE1);
    assertThat(result).isEmpty();
  }

  public void measureNotFound() {
    Optional<ConfiguredFunctionGroup> result = PRICING_RULE.functionGroup(new TestTrade1(), MEASURE3);
    assertThat(result).isEmpty();
  }

  public void measureInRuleButNotGroup() {
    FunctionGroup<TestTrade1> group =
        DefaultFunctionGroup.builder(TestTrade1.class)
            .name("GroupName")
            .addFunction(MEASURE1, FunctionConfig.of(TestFunction1.class))
            .addFunction(MEASURE2, FunctionConfig.of(TestFunction2.class))
            .build();

    PricingRule<TestTrade1> pricingRule =
        PricingRule.builder(TestTrade1.class)
            .functionGroup(group)
            .addMeasures(MEASURE1)
            .build();

    Optional<ConfiguredFunctionGroup> functionGroup = pricingRule.functionGroup(new TestTrade2(), MEASURE2);
    assertThat(functionGroup).isEmpty();

    Set<Measure> trade1Measures = pricingRule.configuredMeasures(new TestTrade1());
    assertThat(trade1Measures).containsOnly(MEASURE1);

    Set<Measure> trade2Measures = pricingRule.configuredMeasures(new TestTrade2());
    assertThat(trade2Measures).isEmpty();
  }

  /**
   * If a rule has an empty set of measures it means it should be used for all measures
   */
  public void ruleWithNoMeasures() {
    PricingRule<TestTrade1> rule =
        PricingRule.builder(TestTrade1.class)
            .functionGroup(GROUP)
            .build();

    Optional<ConfiguredFunctionGroup> functionGroup = rule.functionGroup(new TestTrade1(), MEASURE1);
    assertThat(functionGroup).hasValue(ConfiguredFunctionGroup.of(GROUP));

    Set<Measure> measures = rule.configuredMeasures(new TestTrade1());
    assertThat(measures).containsOnly(MEASURE1, MEASURE2);
  }

  public void measuresMatchingFunctionGroup() {
    Set<Measure> measures = PRICING_RULE.configuredMeasures(new TestTrade1());
    assertThat(measures).containsOnly(MEASURE1, MEASURE2);
  }

  //-------------------------------------------------------------------------
  private static final class TestTrade1 implements CalculationTarget {
  }

  //-------------------------------------------------------------------------
  private static final class TestTrade2 implements CalculationTarget {
  }

  //-------------------------------------------------------------------------
  // function for testing
  public static final class TestFunction1 implements CalculationFunction<TestTrade1> {

    @Override
    public Set<Measure> supportedMeasures() {
      return ImmutableSet.of(MEASURE1);
    }

    @Override
    public FunctionRequirements requirements(TestTrade1 target, Set<Measure> measures) {
      return FunctionRequirements.empty();
    }

    @Override
    public Map<Measure, Result<?>> calculate(
        TestTrade1 target,
        Set<Measure> measures,
        CalculationMarketData marketData) {

      ScenarioResult<String> array = ScenarioResult.of("foo");
      return ImmutableMap.of(MEASURE1, Result.success(array));
    }
  }

  //-------------------------------------------------------------------------
  // function for testing
  public static final class TestFunction2 implements CalculationFunction<TestTrade1> {

    @Override
    public Set<Measure> supportedMeasures() {
      return ImmutableSet.of(MEASURE1);
    }

    @Override
    public FunctionRequirements requirements(TestTrade1 target, Set<Measure> measures) {
      return FunctionRequirements.empty();
    }

    @Override
    public Map<Measure, Result<?>> calculate(
        TestTrade1 target,
        Set<Measure> measures,
        CalculationMarketData marketData) {

      ScenarioResult<String> array = ScenarioResult.of("foo");
      return ImmutableMap.of(MEASURE1, Result.success(array));
    }
  }

}
