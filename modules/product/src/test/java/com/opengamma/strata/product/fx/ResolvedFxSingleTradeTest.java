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
 * Test {@link ResolvedFxSingleTrade}.
 */
@Test
public class ResolvedFxSingleTradeTest {

  private static final CurrencyAmount GBP_P1000 = CurrencyAmount.of(GBP, 1_000);
  private static final CurrencyAmount GBP_M1000 = CurrencyAmount.of(GBP, -1_000);
  private static final CurrencyAmount USD_P1600 = CurrencyAmount.of(USD, 1_600);
  private static final CurrencyAmount USD_M1600 = CurrencyAmount.of(USD, -1_600);
  private static final LocalDate DATE_2015_06_30 = date(2015, 6, 30);
  private static final ResolvedFxSingle FWD1 = ResolvedFxSingle.of(GBP_P1000, USD_M1600, DATE_2015_06_30);
  private static final ResolvedFxSingle FWD2 = ResolvedFxSingle.of(GBP_M1000, USD_P1600, DATE_2015_06_30);
  private static final TradeInfo TRADE_INFO = TradeInfo.builder().tradeDate(date(2015, 1, 15)).build();

  //-------------------------------------------------------------------------
  public void test_builder() {
    ResolvedFxSingleTrade test = ResolvedFxSingleTrade.builder()
        .tradeInfo(TRADE_INFO)
        .product(FWD1)
        .build();
    assertEquals(test.getTradeInfo(), TRADE_INFO);
    assertEquals(test.getProduct(), FWD1);
  }

  //-------------------------------------------------------------------------
  public void coverage() {
    ResolvedFxSingleTrade test = ResolvedFxSingleTrade.builder()
        .tradeInfo(TRADE_INFO)
        .product(FWD1)
        .build();
    coverImmutableBean(test);
    ResolvedFxSingleTrade test2 = ResolvedFxSingleTrade.builder()
        .product(FWD2)
        .build();
    coverBeanEquals(test, test2);
  }

  public void test_serialization() {
    ResolvedFxSingleTrade test = ResolvedFxSingleTrade.builder()
        .tradeInfo(TradeInfo.builder().tradeDate(date(2014, 6, 30)).build())
        .product(FWD1)
        .build();
    assertSerialization(test);
  }

}
