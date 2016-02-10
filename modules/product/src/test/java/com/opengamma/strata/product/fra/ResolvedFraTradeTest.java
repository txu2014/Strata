/**
 * Copyright (C) 2016 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.product.fra;

import static com.opengamma.strata.basics.currency.Currency.GBP;
import static com.opengamma.strata.basics.index.IborIndices.GBP_LIBOR_3M;
import static com.opengamma.strata.collect.TestHelper.assertSerialization;
import static com.opengamma.strata.collect.TestHelper.coverBeanEquals;
import static com.opengamma.strata.collect.TestHelper.coverImmutableBean;
import static com.opengamma.strata.collect.TestHelper.date;
import static com.opengamma.strata.product.fra.FraDiscountingMethod.ISDA;
import static org.testng.Assert.assertEquals;

import org.testng.annotations.Test;

import com.opengamma.strata.product.TradeInfo;
import com.opengamma.strata.product.rate.IborRateObservation;

/**
 * Test {@link ResolvedFraTrade}.
 */
@Test
public class ResolvedFraTradeTest {

  private static final double NOTIONAL_1M = 1_000_000d;
  private static final double NOTIONAL_2M = 2_000_000d;
  private static final ResolvedFra FRA1 = ResolvedFra.builder()
      .paymentDate(date(2015, 6, 16))
      .startDate(date(2015, 6, 15))
      .endDate(date(2015, 9, 15))
      .yearFraction(0.25d)
      .fixedRate(0.25d)
      .floatingRate(IborRateObservation.of(GBP_LIBOR_3M, date(2015, 6, 12)))
      .currency(GBP)
      .notional(NOTIONAL_1M)
      .discounting(ISDA)
      .build();
  private static final ResolvedFra FRA2 = FRA1.toBuilder().notional(NOTIONAL_2M).build();
  private static final TradeInfo TRADE_INFO = TradeInfo.builder().tradeDate(date(2014, 6, 30)).build();

  //-------------------------------------------------------------------------
  public void test_of() {
    ResolvedFraTrade test = ResolvedFraTrade.of(TRADE_INFO, FRA1);
    assertEquals(test.getProduct(), FRA1);
    assertEquals(test.getTradeInfo(), TRADE_INFO);
  }

  public void test_builder() {
    ResolvedFraTrade test = ResolvedFraTrade.builder()
        .product(FRA1)
        .build();
    assertEquals(test.getTradeInfo(), TradeInfo.EMPTY);
    assertEquals(test.getProduct(), FRA1);
  }

  //-------------------------------------------------------------------------
  public void coverage() {
    ResolvedFraTrade test = ResolvedFraTrade.builder()
        .tradeInfo(TradeInfo.builder().tradeDate(date(2014, 6, 30)).build())
        .product(FRA1)
        .build();
    coverImmutableBean(test);
    ResolvedFraTrade test2 = ResolvedFraTrade.builder()
        .product(FRA2)
        .build();
    coverBeanEquals(test, test2);
  }

  public void test_serialization() {
    ResolvedFraTrade test = ResolvedFraTrade.builder()
        .tradeInfo(TradeInfo.builder().tradeDate(date(2014, 6, 30)).build())
        .product(FRA1)
        .build();
    assertSerialization(test);
  }

}
