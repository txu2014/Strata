/**
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 * <p>
 * Please see distribution for license.
 */
package com.opengamma.strata.engine.config;

import static com.opengamma.strata.collect.CollectProjectAssertions.assertThat;

import java.util.Optional;

import org.testng.annotations.Test;

import com.opengamma.strata.basics.CalculationTarget;
import com.opengamma.strata.engine.marketdata.mapping.MarketDataMappings;

@Test
public class MarketDataRulesTest {

  private static final TestTrade1 TRADE1 = new TestTrade1();
  private static final TestTrade2 TRADE2 = new TestTrade2();
  private static final TestTrade3 TRADE3 = new TestTrade3();
  private static final TestTrade4 TRADE4 = new TestTrade4();
  private static final MarketDataMappings MAPPINGS1 = mappings("1");
  private static final MarketDataMappings MAPPINGS2 = mappings("2");
  private static final MarketDataMappings MAPPINGS3 = mappings("3");

  private static final SimpleMarketDataRules RULES1 =
      SimpleMarketDataRules.builder()
          .addMappings(TestTrade1.class, MAPPINGS1)
          .build();

  private static final SimpleMarketDataRules RULES2 =
      SimpleMarketDataRules.builder()
          .addMappings(TestTrade2.class, MAPPINGS2)
          .build();

  private static final SimpleMarketDataRules RULES3 =
      SimpleMarketDataRules.builder()
          .addMappings(TestTrade3.class, MAPPINGS3)
          .build();

  public void ofEmpty() {
    MarketDataRules rules = MarketDataRules.of();
    Optional<MarketDataMappings> mappings = rules.mappings(TRADE1);

    assertThat(rules).isInstanceOf(EmptyMarketDataRules.class);
    assertThat(mappings).isEmpty();
  }

  public void ofSingle() {
    MarketDataRules rules = MarketDataRules.of(RULES1);
    assertThat(rules).isInstanceOf(SimpleMarketDataRules.class);
    Optional<MarketDataMappings> mappings = rules.mappings(TRADE1);
    assertThat(mappings).hasValue(MAPPINGS1);
  }

  public void ofMultiple() {
    MarketDataRules rules = MarketDataRules.of(RULES1, RULES2);
    Optional<MarketDataMappings> mappings1 = rules.mappings(TRADE1);
    Optional<MarketDataMappings> mappings2 = rules.mappings(TRADE2);
    Optional<MarketDataMappings> mappings3 = rules.mappings(TRADE3);

    assertThat(mappings1).hasValue(MAPPINGS1);
    assertThat(mappings2).hasValue(MAPPINGS2);
    assertThat(mappings3).isEmpty();
  }

  public void composedWithComposite() {
    CompositeMarketDataRules compositeRules = CompositeMarketDataRules.builder().rules(RULES1, RULES2).build();
    MarketDataRules rules = compositeRules.composedWith(RULES3);
    Optional<MarketDataMappings> mappings1 = rules.mappings(TRADE1);
    Optional<MarketDataMappings> mappings2 = rules.mappings(TRADE2);
    Optional<MarketDataMappings> mappings3 = rules.mappings(TRADE3);
    Optional<MarketDataMappings> mappings4 = rules.mappings(TRADE4);

    assertThat(mappings1).hasValue(MAPPINGS1);
    assertThat(mappings2).hasValue(MAPPINGS2);
    assertThat(mappings3).hasValue(MAPPINGS3);
    assertThat(mappings4).isEmpty();
  }

  public void composedWithEmpty() {
    MarketDataRules rules = MarketDataRules.EMPTY.composedWith(RULES1);
    Optional<MarketDataMappings> mappings = rules.mappings(TRADE1);

    assertThat(rules).isInstanceOf(SimpleMarketDataRules.class);
    assertThat(mappings).hasValue(MAPPINGS1);
  }

  public void composedWithSimple() {
    MarketDataRules rules = RULES1.composedWith(RULES2);
    Optional<MarketDataMappings> mappings1 = rules.mappings(TRADE1);
    Optional<MarketDataMappings> mappings2 = rules.mappings(TRADE2);
    Optional<MarketDataMappings> mappings3 = rules.mappings(TRADE3);

    assertThat(mappings1).hasValue(MAPPINGS1);
    assertThat(mappings2).hasValue(MAPPINGS2);
    assertThat(mappings3).isEmpty();
  }

  private static MarketDataMappings mappings(String curveGroupName) {
    return MarketDataMappings.builder()
        .curveGroup(curveGroupName)
        .build();
  }

  private static final class TestTrade1 implements CalculationTarget { }
  private static final class TestTrade2 implements CalculationTarget { }
  private static final class TestTrade3 implements CalculationTarget { }
  private static final class TestTrade4 implements CalculationTarget { }
}