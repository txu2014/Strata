/**
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.function;

import static com.opengamma.strata.collect.TestHelper.coverPrivateConstructor;
import static org.assertj.core.api.Assertions.assertThat;

import org.testng.annotations.Test;

import com.opengamma.strata.calc.config.pricing.PricingRules;
import com.opengamma.strata.function.calculation.deposit.TermDepositCalculationFunctionTest;
import com.opengamma.strata.function.calculation.fra.FraCalculationFunctionTest;
import com.opengamma.strata.function.calculation.fx.FxNdfFunctionGroupsTest;
import com.opengamma.strata.function.calculation.fx.FxSingleFunctionGroupsTest;
import com.opengamma.strata.function.calculation.fx.FxSwapFunctionGroupsTest;
import com.opengamma.strata.function.calculation.swap.SwapFunctionGroupsTest;

/**
 * Test {@link StandardPricingRules}.
 */
@Test
public class StandardPricingRulesTest {

  public void test_standard() {
    PricingRules test = StandardPricingRules.standard();
    assertThat(test.configuredMeasures(FraCalculationFunctionTest.TRADE)).isNotEmpty();
    assertThat(test.configuredMeasures(FxSingleFunctionGroupsTest.TRADE)).isNotEmpty();
    assertThat(test.configuredMeasures(FxNdfFunctionGroupsTest.TRADE)).isNotEmpty();
    assertThat(test.configuredMeasures(FxSwapFunctionGroupsTest.TRADE)).isNotEmpty();
    assertThat(test.configuredMeasures(SwapFunctionGroupsTest.TRADE)).isNotEmpty();
    assertThat(test.configuredMeasures(TermDepositCalculationFunctionTest.TRADE)).isNotEmpty();
  }

  public void coverage() {
    coverPrivateConstructor(StandardPricingRules.class);
  }

}
