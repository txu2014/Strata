/**
 * Copyright (C) 2016 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.product.index;

import static com.opengamma.strata.collect.TestHelper.assertSerialization;
import static com.opengamma.strata.collect.TestHelper.coverBeanEquals;
import static com.opengamma.strata.collect.TestHelper.coverImmutableBean;
import static com.opengamma.strata.collect.TestHelper.date;
import static org.testng.Assert.assertEquals;

import java.time.LocalDate;

import org.testng.annotations.Test;

import com.opengamma.strata.basics.currency.Currency;
import com.opengamma.strata.basics.index.IborIndices;
import com.opengamma.strata.collect.id.StandardId;
import com.opengamma.strata.product.TradeInfo;

/**
 * Test {@link ResolvedIborFutureTrade}.
 */
@Test
public class ResolvedIborFutureTradeTest {

  private static final LocalDate TRADE_DATE = date(2015, 2, 17);
  private static final long QUANTITY = 35;
  private static final double INITIAL_PRICE = 1.015;
  private static final StandardId FUTURE_ID = StandardId.of("OG-Ticker", "Future1");
  private static final StandardId FUTURE_ID2 = StandardId.of("OG-Ticker", "Future2");

  private static final ResolvedIborFuture FUTURE = ResolvedIborFuture.builder()
      .currency(Currency.USD)
      .notional(1_000_000d)
      .lastTradeDate(date(2015, 3, 16))
      .index(IborIndices.USD_LIBOR_3M)
      .build();

  //-------------------------------------------------------------------------
  public void test_builder() {
    ResolvedIborFutureTrade test = ResolvedIborFutureTrade.builder()
        .tradeInfo(TradeInfo.builder().tradeDate(TRADE_DATE).build())
        .product(FUTURE)
        .securityStandardId(FUTURE_ID)
        .quantity(QUANTITY)
        .initialPrice(INITIAL_PRICE)
        .build();
    assertEquals(test.getTradeInfo(), TradeInfo.builder().tradeDate(TRADE_DATE).build());
    assertEquals(test.getProduct(), FUTURE);
    assertEquals(test.getSecurityStandardId(), FUTURE_ID);
    assertEquals(test.getQuantity(), QUANTITY);
    assertEquals(test.getInitialPrice(), INITIAL_PRICE);
  }

  //-------------------------------------------------------------------------
  public void coverage() {
    ResolvedIborFutureTrade test = ResolvedIborFutureTrade.builder()
        .tradeInfo(TradeInfo.builder().tradeDate(TRADE_DATE).build())
        .product(FUTURE)
        .securityStandardId(FUTURE_ID)
        .quantity(QUANTITY)
        .initialPrice(INITIAL_PRICE)
        .build();
    coverImmutableBean(test);
    ResolvedIborFutureTrade test2 = ResolvedIborFutureTrade.builder()
        .tradeInfo(TradeInfo.builder().tradeDate(TRADE_DATE).build())
        .product(FUTURE)
        .securityStandardId(FUTURE_ID2)
        .quantity(QUANTITY + 1)
        .initialPrice(INITIAL_PRICE + 0.1)
        .build();
    coverBeanEquals(test, test2);
  }

  public void test_serialization() {
    ResolvedIborFutureTrade test = ResolvedIborFutureTrade.builder()
        .tradeInfo(TradeInfo.builder().tradeDate(TRADE_DATE).build())
        .product(FUTURE)
        .securityStandardId(FUTURE_ID)
        .quantity(QUANTITY)
        .initialPrice(INITIAL_PRICE)
        .build();
    assertSerialization(test);
  }

}
