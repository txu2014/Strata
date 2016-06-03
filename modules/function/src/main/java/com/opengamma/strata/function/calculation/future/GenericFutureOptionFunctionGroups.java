/**
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 * 
 * Please see distribution for license.
 */
package com.opengamma.strata.function.calculation.future;

import com.opengamma.strata.calc.config.Measures;
import com.opengamma.strata.calc.config.pricing.DefaultFunctionGroup;
import com.opengamma.strata.calc.config.pricing.FunctionGroup;
import com.opengamma.strata.product.future.GenericFutureOptionTrade;

/**
 * Contains function groups for built-in generic future option calculation functions.
 * <p>
 * Function groups are used in pricing rules to allow the engine to calculate the
 * measures provided by the functions in the group.
 */
public final class GenericFutureOptionFunctionGroups {

  /**
   * The group with pricers based on market methods.
   */
  private static final FunctionGroup<GenericFutureOptionTrade> MARKET_GROUP =
      DefaultFunctionGroup.builder(GenericFutureOptionTrade.class).name("GenericFutureOptionTradeMarket")
          .addFunction(Measures.PRESENT_VALUE, GenericFutureOptionCalculationFunction.class)
          .addFunction(Measures.PRESENT_VALUE_MULTI_CCY, GenericFutureOptionCalculationFunction.class)
          .build();

  /**
   * Restricted constructor.
   */
  private GenericFutureOptionFunctionGroups() {
  }

  //-------------------------------------------------------------------------
  /**
   * Obtains the function group providing all built-in measures on generic future option
   * trades based solely on querying the market for the present value.
   * <p>
   * The supported built-in measures are:
   * <ul>
   *   <li>{@linkplain Measures#PRESENT_VALUE Present value}
   * </ul>
   * 
   * @return the function group
   */
  public static FunctionGroup<GenericFutureOptionTrade> market() {
    return MARKET_GROUP;
  }

}
