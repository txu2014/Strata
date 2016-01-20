/**
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.calc.runner.function;

import com.opengamma.strata.basics.CalculationTarget;
import com.opengamma.strata.calc.marketdata.CalculationMarketData;
import com.opengamma.strata.calc.marketdata.FunctionRequirements;

/**
 * A function that calculates a value for a target using multiple sets of market data.
 * <p>
 * If the calculated value contains any currency amounts and implements {@link CurrencyConvertible}
 * the calculation engine will automatically convert the amounts into the reporting currencies.
 * <p>
 * If the calculated value contains any currency amounts and the automatic currency conversion is
 * insufficient the function should implement {@link CurrencyAwareCalculationSingleFunction}.
 *
 * @param <T>  the type of target handled by this function
 * @param <R>  the return type of this function
 */
public interface CalculationSingleFunction<T extends CalculationTarget, R>
    extends CalculationFunction<T> {

  /**
   * Determines the market data requirements that function has to perform its calculations.
   * <p>
   * Any market data needed by the {@code execute} method should be specified.
   *
   * @param target  the target of the calculation
   * @return the requirements specifying the market data the function needs to perform calculations
   */
  public abstract FunctionRequirements requirements(T target);

  /**
   * Calculates a value for the target using multiple sets of market data.
   * <p>
   * If the calculated value contains any currency amounts and implements {@link CurrencyConvertible}
   * the calculation engine will automatically convert the amounts into the reporting currency.
   *
   * @param target  the target of the calculation
   * @param marketData  the market data used in the calculation
   * @return the result of the calculation
   */
  public abstract R execute(T target, CalculationMarketData marketData);

}
