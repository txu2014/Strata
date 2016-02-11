/**
 * Copyright (C) 2016 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.product.fx;

import static com.opengamma.strata.basics.currency.Currency.GBP;
import static com.opengamma.strata.basics.currency.Currency.USD;
import static com.opengamma.strata.basics.index.FxIndices.GBP_USD_WM;
import static com.opengamma.strata.collect.TestHelper.assertSerialization;
import static com.opengamma.strata.collect.TestHelper.coverBeanEquals;
import static com.opengamma.strata.collect.TestHelper.coverImmutableBean;
import static com.opengamma.strata.collect.TestHelper.date;
import static org.testng.Assert.assertEquals;

import java.time.LocalDate;

import org.testng.annotations.Test;

import com.opengamma.strata.basics.currency.CurrencyAmount;
import com.opengamma.strata.basics.currency.FxRate;
import com.opengamma.strata.product.TradeInfo;

/**
 * Test {@link ResolvedFxNdfTrade}.
 */
@Test
public class ResolvedFxNdfTradeTest {

  private static final FxRate FX_RATE = FxRate.of(GBP, USD, 1.5d);
  private static final FxRate FX_RATE2 = FxRate.of(GBP, USD, 1.6d);
  private static final double NOTIONAL = 100_000_000;
  private static final CurrencyAmount CURRENCY_NOTIONAL = CurrencyAmount.of(GBP, NOTIONAL);
  private static final LocalDate PAYMENT_DATE = LocalDate.of(2015, 3, 19);
  private static final ResolvedFxNdf NDF1 = ResolvedFxNdf.builder()
      .agreedFxRate(FX_RATE)
      .index(GBP_USD_WM)
      .paymentDate(PAYMENT_DATE)
      .settlementCurrencyNotional(CURRENCY_NOTIONAL)
      .build();
  private static final ResolvedFxNdf NDF2 = ResolvedFxNdf.builder()
      .agreedFxRate(FX_RATE2)
      .index(GBP_USD_WM)
      .paymentDate(PAYMENT_DATE)
      .settlementCurrencyNotional(CURRENCY_NOTIONAL)
      .build();
  private static final TradeInfo TRADE_INFO = TradeInfo.builder().tradeDate(date(2015, 1, 15)).build();

  //-------------------------------------------------------------------------
  public void test_builder() {
    ResolvedFxNdfTrade test = ResolvedFxNdfTrade.builder()
        .tradeInfo(TRADE_INFO)
        .product(NDF1)
        .build();
    assertEquals(test.getTradeInfo(), TRADE_INFO);
    assertEquals(test.getProduct(), NDF1);
  }

  //-------------------------------------------------------------------------
  public void coverage() {
    ResolvedFxNdfTrade test = ResolvedFxNdfTrade.builder()
        .tradeInfo(TRADE_INFO)
        .product(NDF1)
        .build();
    coverImmutableBean(test);
    ResolvedFxNdfTrade test2 = ResolvedFxNdfTrade.builder()
        .product(NDF2)
        .build();
    coverBeanEquals(test, test2);
  }

  public void test_serialization() {
    ResolvedFxNdfTrade test = ResolvedFxNdfTrade.builder()
        .tradeInfo(TradeInfo.builder().tradeDate(date(2014, 6, 30)).build())
        .product(NDF1)
        .build();
    assertSerialization(test);
  }

}
