/**
 * Copyright (C) 2016 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.product.fx;

import static com.opengamma.strata.basics.currency.Currency.GBP;
import static com.opengamma.strata.basics.currency.Currency.USD;
import static com.opengamma.strata.collect.TestHelper.assertSerialization;
import static com.opengamma.strata.collect.TestHelper.coverBeanEquals;
import static com.opengamma.strata.collect.TestHelper.coverImmutableBean;
import static com.opengamma.strata.collect.TestHelper.date;
import static org.testng.Assert.assertEquals;

import java.time.LocalDate;

import org.testng.annotations.Test;

import com.opengamma.strata.basics.currency.CurrencyAmount;
import com.opengamma.strata.product.TradeInfo;

/**
 * Test {@link ResolvedFxSwapTrade}.
 */
@Test
public class ResolvedFxSwapTradeTest {

  private static final CurrencyAmount GBP_P1000 = CurrencyAmount.of(GBP, 1_000);
  private static final CurrencyAmount GBP_M1000 = CurrencyAmount.of(GBP, -1_000);
  private static final CurrencyAmount USD_P1550 = CurrencyAmount.of(USD, 1_550);
  private static final CurrencyAmount USD_M1600 = CurrencyAmount.of(USD, -1_600);
  private static final CurrencyAmount USD_M1610 = CurrencyAmount.of(USD, -1_1610);
  private static final LocalDate DATE_2011_11_21 = date(2011, 11, 21);
  private static final LocalDate DATE_2011_12_21 = date(2011, 12, 21);
  private static final ResolvedFxSingle NEAR_LEG = ResolvedFxSingle.of(GBP_P1000, USD_M1600, DATE_2011_11_21);
  private static final ResolvedFxSingle NEAR_LEG2 = ResolvedFxSingle.of(GBP_P1000, USD_M1610, DATE_2011_11_21);
  private static final ResolvedFxSingle FAR_LEG = ResolvedFxSingle.of(GBP_M1000, USD_P1550, DATE_2011_12_21);
  private static final ResolvedFxSwap SWAP1 = ResolvedFxSwap.of(NEAR_LEG, FAR_LEG);
  private static final ResolvedFxSwap SWAP2 = ResolvedFxSwap.of(NEAR_LEG2, FAR_LEG);
  private static final TradeInfo TRADE_INFO = TradeInfo.builder().tradeDate(date(2015, 1, 15)).build();

  //-------------------------------------------------------------------------
  public void test_builder() {
    ResolvedFxSwapTrade test = ResolvedFxSwapTrade.builder()
        .tradeInfo(TRADE_INFO)
        .product(SWAP1)
        .build();
    assertEquals(test.getTradeInfo(), TRADE_INFO);
    assertEquals(test.getProduct(), SWAP1);
  }

  //-------------------------------------------------------------------------
  public void coverage() {
    ResolvedFxSwapTrade test = ResolvedFxSwapTrade.builder()
        .tradeInfo(TRADE_INFO)
        .product(SWAP1)
        .build();
    coverImmutableBean(test);
    ResolvedFxSwapTrade test2 = ResolvedFxSwapTrade.builder()
        .product(SWAP2)
        .build();
    coverBeanEquals(test, test2);
  }

  public void test_serialization() {
    ResolvedFxSwapTrade test = ResolvedFxSwapTrade.builder()
        .tradeInfo(TradeInfo.builder().tradeDate(date(2014, 6, 30)).build())
        .product(SWAP1)
        .build();
    assertSerialization(test);
  }

}
