/**
 * Copyright (C) 2016 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.product.fx;

import static com.opengamma.strata.basics.currency.Currency.EUR;
import static com.opengamma.strata.basics.currency.Currency.USD;
import static com.opengamma.strata.collect.TestHelper.assertSerialization;
import static com.opengamma.strata.collect.TestHelper.coverBeanEquals;
import static com.opengamma.strata.collect.TestHelper.coverImmutableBean;
import static com.opengamma.strata.collect.TestHelper.date;
import static org.testng.Assert.assertEquals;

import java.time.LocalDate;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;

import org.testng.annotations.Test;

import com.opengamma.strata.basics.LongShort;
import com.opengamma.strata.basics.PutCall;
import com.opengamma.strata.basics.currency.CurrencyAmount;
import com.opengamma.strata.basics.currency.FxRate;
import com.opengamma.strata.basics.currency.Payment;
import com.opengamma.strata.product.TradeInfo;

/**
 * Test {@link ResolvedFxVanillaOptionTrade}.
 */
@Test
public class ResolvedFxVanillaOptionTradeTest {

  private static final ZonedDateTime EXPIRY_DATE_TIME = ZonedDateTime.of(2015, 2, 14, 12, 15, 0, 0, ZoneOffset.UTC);
  private static final LongShort LONG = LongShort.LONG;
  private static final PutCall CALL = PutCall.CALL;
  private static final FxRate STRIKE = FxRate.of(EUR, USD, 1.3);
  private static final LocalDate PAYMENT_DATE = LocalDate.of(2015, 2, 16);
  private static final double NOTIONAL = 1.0e6;
  private static final CurrencyAmount EUR_AMOUNT = CurrencyAmount.of(EUR, NOTIONAL);
  private static final CurrencyAmount USD_AMOUNT = CurrencyAmount.of(USD, -NOTIONAL * 1.35);
  private static final ResolvedFxSingle FX = ResolvedFxSingle.of(EUR_AMOUNT, USD_AMOUNT, PAYMENT_DATE);
  private static final ResolvedFxVanillaOption SWAP1 = ResolvedFxVanillaOption.builder()
      .putCall(CALL)
      .longShort(LONG)
      .expiry(EXPIRY_DATE_TIME)
      .strike(STRIKE)
      .underlying(FX)
      .build();
  private static final ResolvedFxVanillaOption SWAP2 = ResolvedFxVanillaOption.builder()
      .putCall(CALL)
      .longShort(LONG)
      .expiry(EXPIRY_DATE_TIME.plusSeconds(1))
      .strike(STRIKE)
      .underlying(FX)
      .build();
  private static final TradeInfo TRADE_INFO = TradeInfo.builder().tradeDate(date(2015, 1, 15)).build();
  private static final Payment PREMIUM = Payment.of(EUR_AMOUNT, PAYMENT_DATE);
  private static final Payment PREMIUM2 = Payment.of(EUR_AMOUNT, PAYMENT_DATE.plusDays(1));

  //-------------------------------------------------------------------------
  public void test_builder() {
    ResolvedFxVanillaOptionTrade test = ResolvedFxVanillaOptionTrade.builder()
        .tradeInfo(TRADE_INFO)
        .product(SWAP1)
        .premium(PREMIUM)
        .build();
    assertEquals(test.getTradeInfo(), TRADE_INFO);
    assertEquals(test.getProduct(), SWAP1);
    assertEquals(test.getPremium(), PREMIUM);
  }

  //-------------------------------------------------------------------------
  public void coverage() {
    ResolvedFxVanillaOptionTrade test = ResolvedFxVanillaOptionTrade.builder()
        .tradeInfo(TRADE_INFO)
        .product(SWAP1)
        .premium(PREMIUM)
        .build();
    coverImmutableBean(test);
    ResolvedFxVanillaOptionTrade test2 = ResolvedFxVanillaOptionTrade.builder()
        .product(SWAP2)
        .premium(PREMIUM2)
        .build();
    coverBeanEquals(test, test2);
  }

  public void test_serialization() {
    ResolvedFxVanillaOptionTrade test = ResolvedFxVanillaOptionTrade.builder()
        .tradeInfo(TradeInfo.builder().tradeDate(date(2014, 6, 30)).build())
        .product(SWAP1)
        .premium(PREMIUM)
        .build();
    assertSerialization(test);
  }

}
