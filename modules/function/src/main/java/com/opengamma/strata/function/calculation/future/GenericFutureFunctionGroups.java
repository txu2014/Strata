/**
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 * 
 * Please see distribution for license.
 */
package com.opengamma.strata.function.calculation.future;

import com.opengamma.strata.calc.config.Measures;
import com.opengamma.strata.calc.config.pricing.DefaultFunctionGroup;
import com.opengamma.strata.calc.config.pricing.FunctionGroup;
import com.opengamma.strata.product.future.GenericFutureTrade;

/**
 * Contains function groups for built-in generic future calculation functions.
 * <p>
 * Function groups are used in pricing rules to allow the engine to calculate the
 * measures provided by the functions in the group.
 */
public final class GenericFutureFunctionGroups {

  /**
   * The group with pricers based on market methods.
   */
  private static final FunctionGroup<GenericFutureTrade> MARKET_GROUP =
      DefaultFunctionGroup.builder(GenericFutureTrade.class).name("GenericFutureTradeMarket")
          .addFunction(Measures.PRESENT_VALUE, GenericFutureCalculationFunction.class)
          .build();

  /**
   * Restricted constructor.
   */
  private GenericFutureFunctionGroups() {
  }

  //-------------------------------------------------------------------------
  /**
   * Obtains the function group providing all built-in measures on generic future
   * trades based solely on querying the market for the present value.
   * <p>
   * The supported built-in measures are:
   * <ul>
   *   <li>{@linkplain Measures#PRESENT_VALUE Present value}
   *   <li>{@linkplain Measures#PRESENT_VALUE_MULTI_CCY Present value with no currency conversion}
   * </ul>
   * 
   * @return the function group
   */
  public static FunctionGroup<GenericFutureTrade> market() {
    return MARKET_GROUP;
  }

}
