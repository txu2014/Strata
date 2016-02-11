/**
 * Copyright (C) 2016 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.product.cms;

import static com.opengamma.strata.basics.currency.Currency.EUR;
import static com.opengamma.strata.collect.TestHelper.assertSerialization;
import static com.opengamma.strata.collect.TestHelper.coverBeanEquals;
import static com.opengamma.strata.collect.TestHelper.coverImmutableBean;
import static com.opengamma.strata.collect.TestHelper.date;
import static org.testng.Assert.assertEquals;

import java.util.Optional;

import org.testng.annotations.Test;

import com.opengamma.strata.basics.currency.CurrencyAmount;
import com.opengamma.strata.basics.currency.Payment;
import com.opengamma.strata.product.TradeInfo;

/**
 * Test {@link ResolvedCmsTrade}.
 */
@Test
public class ResolvedCmsTradeTest {

  private static final TradeInfo TRADE_INFO = TradeInfo.builder().tradeDate(date(2016, 6, 30)).build();
  private static final ResolvedCms PRODUCT = ResolvedCms.of(ResolvedCmsTest.CMS_LEG, ResolvedCmsTest.PAY_LEG);
  private static final Payment PREMIUM = Payment.of(CurrencyAmount.of(EUR, -0.001 * 1.0e6), date(2016, 7, 2));

  //-------------------------------------------------------------------------
  public void test_builder() {
    ResolvedCmsTrade test = ResolvedCmsTrade.builder()
        .product(PRODUCT)
        .build();
    assertEquals(test.getTradeInfo(), TradeInfo.EMPTY);
    assertEquals(test.getProduct(), PRODUCT);
    assertEquals(test.getPremium(), Optional.empty());
  }

  public void test_builder_full() {
    ResolvedCmsTrade test = ResolvedCmsTrade.builder()
        .tradeInfo(TRADE_INFO)
        .product(PRODUCT)
        .premium(PREMIUM)
        .build();
    assertEquals(test.getTradeInfo(), TRADE_INFO);
    assertEquals(test.getProduct(), PRODUCT);
    assertEquals(test.getPremium(), Optional.of(PREMIUM));
  }

  //-------------------------------------------------------------------------
  public void coverage() {
    ResolvedCmsTrade test = ResolvedCmsTrade.builder()
        .tradeInfo(TRADE_INFO)
        .product(PRODUCT)
        .premium(PREMIUM)
        .build();
    coverImmutableBean(test);
    ResolvedCmsTrade test2 = ResolvedCmsTrade.builder()
        .product(PRODUCT)
        .build();
    coverBeanEquals(test, test2);
  }

  public void test_serialization() {
    ResolvedCmsTrade test = ResolvedCmsTrade.builder()
        .product(PRODUCT)
        .build();
    assertSerialization(test);
  }

}
