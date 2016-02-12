/**
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 * 
 * Please see distribution for license.
 */
package com.opengamma.strata.pricer.calibration;

import java.util.function.BiFunction;
import java.util.function.ToDoubleBiFunction;

import com.opengamma.strata.basics.Trade;
import com.opengamma.strata.basics.market.ReferenceData;
import com.opengamma.strata.collect.ArgChecker;
import com.opengamma.strata.market.curve.CurveCurrencyParameterSensitivities;
import com.opengamma.strata.market.sensitivity.PointSensitivities;
import com.opengamma.strata.pricer.deposit.DiscountingIborFixingDepositProductPricer;
import com.opengamma.strata.pricer.deposit.DiscountingTermDepositProductPricer;
import com.opengamma.strata.pricer.fra.DiscountingFraProductPricer;
import com.opengamma.strata.pricer.fx.DiscountingFxSwapProductPricer;
import com.opengamma.strata.pricer.index.DiscountingIborFutureTradePricer;
import com.opengamma.strata.pricer.rate.RatesProvider;
import com.opengamma.strata.pricer.swap.DiscountingSwapProductPricer;
import com.opengamma.strata.product.deposit.IborFixingDepositTrade;
import com.opengamma.strata.product.deposit.TermDepositTrade;
import com.opengamma.strata.product.fra.FraTrade;
import com.opengamma.strata.product.fx.FxSwapTrade;
import com.opengamma.strata.product.index.IborFutureTrade;
import com.opengamma.strata.product.swap.SwapTrade;

/**
 * Provides calibration measures for a single type of trade based on functions.
 * <p>
 * This is initialized using functions that typically refer to pricers.
 * 
 * @param <T> the trade type
 */
public class TradeCalibrationMeasure<T extends Trade>
    implements CalibrationMeasure<T> {

  // hard-coded reference data
  private static final ReferenceData REF_DATA = ReferenceData.standard();

  /**
   * The calibrator for {@link FraTrade} using par spread discounting.
   */
  public static final TradeCalibrationMeasure<FraTrade> FRA_PAR_SPREAD =
      TradeCalibrationMeasure.of(
          "FraParSpreadDiscounting",
          FraTrade.class,
          (trade, p) -> DiscountingFraProductPricer.DEFAULT.parSpread(trade.getProduct().resolve(REF_DATA), p),
          (trade, p) -> DiscountingFraProductPricer.DEFAULT.parSpreadSensitivity(trade.getProduct().resolve(REF_DATA), p));

  /**
   * The calibrator for {@link IborFutureTrade} using par spread discounting.
   */
  public static final TradeCalibrationMeasure<IborFutureTrade> IBOR_FUTURE_PAR_SPREAD =
      TradeCalibrationMeasure.of(
          "IborFutureParSpreadDiscounting",
          IborFutureTrade.class,
          (trade, p) -> DiscountingIborFutureTradePricer.DEFAULT.parSpread(trade.resolve(REF_DATA), p, 0.0),
          (trade, p) -> DiscountingIborFutureTradePricer.DEFAULT.parSpreadSensitivity(trade.resolve(REF_DATA), p));

  /**
   * The calibrator for {@link SwapTrade} using par spread discounting.
   */
  public static final TradeCalibrationMeasure<SwapTrade> SWAP_PAR_SPREAD =
      TradeCalibrationMeasure.of(
          "SwapParSpreadDiscounting",
          SwapTrade.class,
          (trade, p) -> DiscountingSwapProductPricer.DEFAULT.parSpread(trade.getProduct().resolve(REF_DATA), p),
          (trade, p) -> DiscountingSwapProductPricer.DEFAULT.parSpreadSensitivity(
              trade.getProduct().resolve(REF_DATA), p).build());

  /**
   * The calibrator for {@link IborFixingDepositTrade} using par spread discounting.
   */
  public static final TradeCalibrationMeasure<IborFixingDepositTrade> IBOR_FIXING_DEPOSIT_PAR_SPREAD =
      TradeCalibrationMeasure.of(
          "IborFixingDepositParSpreadDiscounting",
          IborFixingDepositTrade.class,
          (trade, p) -> DiscountingIborFixingDepositProductPricer.DEFAULT.parSpread(trade.getProduct().resolve(REF_DATA), p),
          (trade, p) -> DiscountingIborFixingDepositProductPricer.DEFAULT.parSpreadSensitivity(
              trade.getProduct().resolve(REF_DATA), p));

  /**
   * The calibrator for {@link TermDepositTrade} using par spread discounting.
   */
  public static final TradeCalibrationMeasure<TermDepositTrade> TERM_DEPOSIT_PAR_SPREAD =
      TradeCalibrationMeasure.of(
          "TermDepositParSpreadDiscounting",
          TermDepositTrade.class,
              (trade, p) -> DiscountingTermDepositProductPricer.DEFAULT.parSpread(trade.getProduct().resolve(REF_DATA), p),
              (trade, p) -> DiscountingTermDepositProductPricer.DEFAULT.parSpreadSensitivity(
                  trade.getProduct().resolve(REF_DATA), p));

  /**
   * The calibrator for {@link FxSwapTrade} using par spread discounting.
   */
  public static final TradeCalibrationMeasure<FxSwapTrade> FX_SWAP_PAR_SPREAD =
      TradeCalibrationMeasure.of(
          "FxSwapParSpreadDiscounting",
          FxSwapTrade.class,
          (trade, p) -> DiscountingFxSwapProductPricer.DEFAULT.parSpread(trade.getProduct().resolve(REF_DATA), p),
          (trade, p) -> DiscountingFxSwapProductPricer.DEFAULT.parSpreadSensitivity(trade.getProduct().resolve(REF_DATA), p));

  //-------------------------------------------------------------------------
  /**
   * The name.
   */
  private final String name;
  /**
   * The trade type.
   */
  private final Class<T> tradeType;
  /**
   * The value measure.
   */
  private final ToDoubleBiFunction<T, RatesProvider> valueFn;
  /**
   * The sensitivity measure.
   */
  private final BiFunction<T, RatesProvider, PointSensitivities> sensitivityFn;

  //-------------------------------------------------------------------------
  /**
   * Obtains a calibrator for a specific type of trade.
   * <p>
   * The functions typically refer to pricers.
   * 
   * @param <R>  the trade type
   * @param name  the name
   * @param tradeType  the trade type
   * @param valueFn  the function for calculating the value
   * @param sensitivityFn  the function for calculating the sensitivity
   * @return the calibrator
   */
  public static <R extends Trade> TradeCalibrationMeasure<R> of(
      String name,
      Class<R> tradeType,
      ToDoubleBiFunction<R, RatesProvider> valueFn,
      BiFunction<R, RatesProvider, PointSensitivities> sensitivityFn) {

    return new TradeCalibrationMeasure<R>(name, tradeType, valueFn, sensitivityFn);
  }

  // restricted constructor
  private TradeCalibrationMeasure(
      String name,
      Class<T> tradeType,
      ToDoubleBiFunction<T, RatesProvider> valueFn,
      BiFunction<T, RatesProvider, PointSensitivities> sensitivityFn) {

    this.name = name;
    this.tradeType = tradeType;
    this.valueFn = ArgChecker.notNull(valueFn, "valueFn");
    this.sensitivityFn = ArgChecker.notNull(sensitivityFn, "sensitivityFn");
  }

  //-------------------------------------------------------------------------
  @Override
  public Class<T> getTradeType() {
    return tradeType;
  }

  //-------------------------------------------------------------------------
  @Override
  public double value(T trade, RatesProvider provider) {
    return valueFn.applyAsDouble(trade, provider);
  }

  @Override
  public CurveCurrencyParameterSensitivities sensitivities(T trade, RatesProvider provider) {
    PointSensitivities pts = sensitivityFn.apply(trade, provider);
    return provider.curveParameterSensitivity(pts);
  }

  //-------------------------------------------------------------------------
  @Override
  public String toString() {
    return name;
  }

}
