/**
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.product.swaption;

import static com.opengamma.strata.collect.TestHelper.assertSerialization;
import static com.opengamma.strata.collect.TestHelper.coverBeanEquals;
import static com.opengamma.strata.collect.TestHelper.coverImmutableBean;
import static com.opengamma.strata.collect.TestHelper.date;
import static org.testng.Assert.assertEquals;

import org.testng.annotations.Test;

import com.opengamma.strata.basics.currency.Currency;
import com.opengamma.strata.basics.currency.CurrencyAmount;
import com.opengamma.strata.basics.currency.Payment;
import com.opengamma.strata.basics.market.ReferenceData;
import com.opengamma.strata.product.TradeInfo;

/**
 * Test {@link ResolvedSwaptionTrade}. 
 */
@Test
public class ResolvedSwaptionTradeTest {

  private static final ReferenceData REF_DATA = ReferenceData.standard();
  private static final ResolvedSwaption SWAPTION = SwaptionTradeTest.SWAPTION.resolve(REF_DATA);
  private static final TradeInfo TRADE_INFO = TradeInfo.builder().tradeDate(date(2014, 6, 30)).build();
  private static final Payment PREMIUM = Payment.of(CurrencyAmount.of(Currency.USD, -3150000d), date(2014, 3, 17));
  private static final Payment PREMIUM2 = Payment.of(CurrencyAmount.of(Currency.USD, -3160000d), date(2014, 3, 17));

  //-------------------------------------------------------------------------
  public void test_of() {
    ResolvedSwaptionTrade test = ResolvedSwaptionTrade.of(TRADE_INFO, SWAPTION, PREMIUM);
    assertEquals(test.getProduct(), SWAPTION);
    assertEquals(test.getTradeInfo(), TRADE_INFO);
  }

  public void test_builder() {
    ResolvedSwaptionTrade test = ResolvedSwaptionTrade.builder()
        .product(SWAPTION)
        .tradeInfo(TRADE_INFO)
        .premium(PREMIUM)
        .build();
    assertEquals(test.getProduct(), SWAPTION);
    assertEquals(test.getTradeInfo(), TRADE_INFO);
  }

  //-------------------------------------------------------------------------
  public void coverage() {
    ResolvedSwaptionTrade test1 = ResolvedSwaptionTrade.builder()
        .product(SWAPTION)
        .tradeInfo(TRADE_INFO)
        .premium(PREMIUM)
        .build();
    coverImmutableBean(test1);
    ResolvedSwaptionTrade test2 = ResolvedSwaptionTrade.builder()
        .product(SWAPTION)
        .premium(PREMIUM2)
        .build();
    coverBeanEquals(test1, test2);
  }

  public void test_serialization() {
    ResolvedSwaptionTrade test = ResolvedSwaptionTrade.builder()
        .product(SWAPTION)
        .tradeInfo(TRADE_INFO)
        .premium(PREMIUM)
        .build();
    assertSerialization(test);
  }

}
