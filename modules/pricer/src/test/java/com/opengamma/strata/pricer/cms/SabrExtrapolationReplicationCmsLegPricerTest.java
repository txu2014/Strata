/**
 * Copyright (C) 2016 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.pricer.cms;

import static com.opengamma.strata.basics.PayReceive.PAY;
import static com.opengamma.strata.basics.PayReceive.RECEIVE;
import static com.opengamma.strata.basics.currency.Currency.EUR;
import static com.opengamma.strata.basics.date.HolidayCalendars.EUTA;
import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertTrue;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import org.testng.annotations.Test;

import com.opengamma.strata.basics.currency.CurrencyAmount;
import com.opengamma.strata.basics.date.BusinessDayAdjustment;
import com.opengamma.strata.basics.date.BusinessDayConventions;
import com.opengamma.strata.basics.schedule.Frequency;
import com.opengamma.strata.basics.schedule.PeriodicSchedule;
import com.opengamma.strata.basics.schedule.RollConventions;
import com.opengamma.strata.basics.schedule.StubConvention;
import com.opengamma.strata.basics.value.ValueAdjustment;
import com.opengamma.strata.basics.value.ValueSchedule;
import com.opengamma.strata.basics.value.ValueStep;
import com.opengamma.strata.collect.timeseries.LocalDateDoubleTimeSeries;
import com.opengamma.strata.market.curve.CurveCurrencyParameterSensitivities;
import com.opengamma.strata.market.sensitivity.PointSensitivityBuilder;
import com.opengamma.strata.market.sensitivity.SwaptionSabrSensitivities;
import com.opengamma.strata.pricer.impl.cms.SabrExtrapolationReplicationCmsPeriodPricer;
import com.opengamma.strata.pricer.rate.ImmutableRatesProvider;
import com.opengamma.strata.pricer.sensitivity.RatesFiniteDifferenceSensitivityCalculator;
import com.opengamma.strata.pricer.swaption.SabrParametersSwaptionVolatilities;
import com.opengamma.strata.pricer.swaption.SwaptionSabrRateVolatilityDataSet;
import com.opengamma.strata.product.cms.CmsLeg;
import com.opengamma.strata.product.cms.CmsPeriod;
import com.opengamma.strata.product.cms.ExpandedCmsLeg;
import com.opengamma.strata.product.swap.SwapIndex;
import com.opengamma.strata.product.swap.SwapIndices;

/**
 * Test {@link SabrExtrapolationReplicationCmsLegPricer}.
 */
@Test
public class SabrExtrapolationReplicationCmsLegPricerTest {
  // CMS legs
  private static final SwapIndex INDEX = SwapIndices.EUR_EURIBOR_1100_5Y;
  private static final LocalDate START = LocalDate.of(2015, 10, 21);
  private static final LocalDate END = LocalDate.of(2020, 10, 21);
  private static final Frequency FREQUENCY = Frequency.P12M;
  private static final BusinessDayAdjustment BUSS_ADJ_EUR =
      BusinessDayAdjustment.of(BusinessDayConventions.FOLLOWING, EUTA);
  private static final PeriodicSchedule SCHEDULE_EUR =
      PeriodicSchedule.of(START, END, FREQUENCY, BUSS_ADJ_EUR, StubConvention.NONE, RollConventions.NONE);
  private static final double CAP_VALUE = 0.0125;
  private static final ValueSchedule CAP_STRIKE = ValueSchedule.of(CAP_VALUE);
  private static final List<ValueStep> FLOOR_STEPS = new ArrayList<ValueStep>();
  private static final List<ValueStep> NOTIONAL_STEPS = new ArrayList<ValueStep>();
  private static final double FLOOR_VALUE_0 = 0.014;
  private static final double FLOOR_VALUE_1 = 0.0135;
  private static final double FLOOR_VALUE_2 = 0.012;
  private static final double FLOOR_VALUE_3 = 0.013;
  private static final double NOTIONAL_VALUE_0 = 1.0e6;
  private static final double NOTIONAL_VALUE_1 = 1.1e6;
  private static final double NOTIONAL_VALUE_2 = 0.9e6;
  private static final double NOTIONAL_VALUE_3 = 1.2e6;
  static {
    FLOOR_STEPS.add(ValueStep.of(1, ValueAdjustment.ofReplace(FLOOR_VALUE_1)));
    FLOOR_STEPS.add(ValueStep.of(2, ValueAdjustment.ofReplace(FLOOR_VALUE_2)));
    FLOOR_STEPS.add(ValueStep.of(3, ValueAdjustment.ofReplace(FLOOR_VALUE_3)));
    NOTIONAL_STEPS.add(ValueStep.of(1, ValueAdjustment.ofReplace(NOTIONAL_VALUE_1)));
    NOTIONAL_STEPS.add(ValueStep.of(2, ValueAdjustment.ofReplace(NOTIONAL_VALUE_2)));
    NOTIONAL_STEPS.add(ValueStep.of(3, ValueAdjustment.ofReplace(NOTIONAL_VALUE_3)));
  }
  private static final ValueSchedule FLOOR_STRIKE = ValueSchedule.of(FLOOR_VALUE_0, FLOOR_STEPS);
  private static final ValueSchedule NOTIONAL = ValueSchedule.of(NOTIONAL_VALUE_0, NOTIONAL_STEPS);
  private static final CmsLeg CAP_LEG = CmsLeg.builder()
      .capSchedule(CAP_STRIKE)
      .index(INDEX)
      .notional(NOTIONAL)
      .payReceive(RECEIVE)
      .paymentSchedule(SCHEDULE_EUR)
      .build();
  private static final CmsLeg FLOOR_LEG = CmsLeg.builder()
      .floorSchedule(FLOOR_STRIKE)
      .index(INDEX)
      .notional(NOTIONAL)
      .payReceive(RECEIVE)
      .paymentSchedule(SCHEDULE_EUR)
      .build();
  private static final CmsLeg COUPON_LEG = CmsLeg.builder()
      .index(INDEX)
      .notional(NOTIONAL)
      .payReceive(PAY)
      .paymentSchedule(SCHEDULE_EUR)
      .build();
  // providers
  private static final LocalDate VALUATION = LocalDate.of(2015, 8, 18);
  private static final ImmutableRatesProvider RATES_PROVIDER =
      SwaptionSabrRateVolatilityDataSet.getRatesProviderEur(VALUATION);
  private static final SabrParametersSwaptionVolatilities VOLATILITIES =
      SwaptionSabrRateVolatilityDataSet.getVolatilitiesEur(VALUATION, true);
  // providers - valuation after the first payment
  private static final LocalDate AFTER_PAYMENT = LocalDate.of(2016, 11, 25);  // the first cms payment is 2016-10-21.
  private static final LocalDate FIXING = LocalDate.of(2016, 10, 19); // fixing for the second period.
  private static final double OBS_INDEX = 0.013;
  private static final LocalDateDoubleTimeSeries TIME_SERIES = LocalDateDoubleTimeSeries.of(FIXING, OBS_INDEX);
  private static final ImmutableRatesProvider RATES_PROVIDER_AFTER_PERIOD =
      SwaptionSabrRateVolatilityDataSet.getRatesProviderEur(AFTER_PAYMENT, TIME_SERIES);
  private static final SabrParametersSwaptionVolatilities VOLATILITIES_AFTER_PERIOD =
      SwaptionSabrRateVolatilityDataSet.getVolatilitiesEur(AFTER_PAYMENT, true);
  // providers - valuation on the payment date
  private static final LocalDate PAYMENT = LocalDate.of(2017, 10, 23); // payment date of the second payment
  private static final ImmutableRatesProvider RATES_PROVIDER_ON_PAY =
      SwaptionSabrRateVolatilityDataSet.getRatesProviderEur(PAYMENT, TIME_SERIES);
  private static final SabrParametersSwaptionVolatilities VOLATILITIES_ON_PAY =
      SwaptionSabrRateVolatilityDataSet.getVolatilitiesEur(PAYMENT, true);
  // providers - valuation after maturity date
  private static final LocalDate ENDED = END.plusDays(7);
  private static final ImmutableRatesProvider RATES_PROVIDER_ENDED =
      SwaptionSabrRateVolatilityDataSet.getRatesProviderEur(ENDED);
  private static final SabrParametersSwaptionVolatilities VOLATILITIES_ENDED =
      SwaptionSabrRateVolatilityDataSet.getVolatilitiesEur(ENDED, true);
  // pricers
  private static final double CUT_OFF_STRIKE = 0.10;
  private static final double MU = 2.50;
  private static final double EPS = 1.0e-5;
  private static final double TOL = 1.0e-12;
  private static final SabrExtrapolationReplicationCmsPeriodPricer PERIOD_PRICER =
      SabrExtrapolationReplicationCmsPeriodPricer.of(CUT_OFF_STRIKE, MU);
  private static final SabrExtrapolationReplicationCmsLegPricer LEG_PRICER =
      new SabrExtrapolationReplicationCmsLegPricer(PERIOD_PRICER);
  private static final RatesFiniteDifferenceSensitivityCalculator FD_CAL =
      new RatesFiniteDifferenceSensitivityCalculator(EPS);

  public void test_presentValue() {
    ExpandedCmsLeg expand = CAP_LEG.expand();
    CurrencyAmount computed = LEG_PRICER.presentValue(expand, RATES_PROVIDER, VOLATILITIES);
    double expected = 0d;
    List<CmsPeriod> cms = expand.getCmsPeriods();
    int size = cms.size();
    for (int i = 0; i < size; ++i) {
      expected += PERIOD_PRICER.presentValue(cms.get(i), RATES_PROVIDER, VOLATILITIES).getAmount();
    }
    assertEquals(computed.getCurrency(), EUR);
    assertEquals(computed.getAmount(), expected, NOTIONAL_VALUE_0 * TOL);
  }

  public void test_presentValue_afterPay() {
    ExpandedCmsLeg expand = FLOOR_LEG.expand();
    CurrencyAmount computed = LEG_PRICER.presentValue(expand, RATES_PROVIDER_AFTER_PERIOD, VOLATILITIES_AFTER_PERIOD);
    double expected = 0d;
    List<CmsPeriod> cms = expand.getCmsPeriods();
    int size = cms.size();
    for (int i = 1; i < size; ++i) {
      expected += PERIOD_PRICER.presentValue(
          cms.get(i), RATES_PROVIDER_AFTER_PERIOD, VOLATILITIES_AFTER_PERIOD).getAmount();
    }
    assertEquals(computed.getCurrency(), EUR);
    assertEquals(computed.getAmount(), expected, NOTIONAL_VALUE_0 * TOL);
  }

  public void test_presentValue_ended() {
    ExpandedCmsLeg expand = COUPON_LEG.expand();
    CurrencyAmount computed = LEG_PRICER.presentValue(expand, RATES_PROVIDER_ENDED, VOLATILITIES_ENDED);
    assertEquals(computed, CurrencyAmount.zero(EUR));
  }

  //-------------------------------------------------------------------------
  public void test_presentValueSensitivity() {
    ExpandedCmsLeg expand = FLOOR_LEG.expand();
    PointSensitivityBuilder point = LEG_PRICER.presentValueSensitivity(expand, RATES_PROVIDER, VOLATILITIES);
    CurveCurrencyParameterSensitivities computed = RATES_PROVIDER.curveParameterSensitivity(point.build());
    CurveCurrencyParameterSensitivities expected =
        FD_CAL.sensitivity(RATES_PROVIDER, p -> LEG_PRICER.presentValue(expand, p, VOLATILITIES));
    assertTrue(computed.equalWithTolerance(expected, EPS * NOTIONAL_VALUE_0 * 80d));
  }

  public void test_presentValueSensitivity_afterPay() {
    ExpandedCmsLeg expand = COUPON_LEG.expand();
    PointSensitivityBuilder point =
        LEG_PRICER.presentValueSensitivity(expand, RATES_PROVIDER_AFTER_PERIOD, VOLATILITIES_AFTER_PERIOD);
    CurveCurrencyParameterSensitivities computed = RATES_PROVIDER_AFTER_PERIOD.curveParameterSensitivity(point.build());
    CurveCurrencyParameterSensitivities expected = FD_CAL.sensitivity(
        RATES_PROVIDER_AFTER_PERIOD, p -> LEG_PRICER.presentValue(expand, p, VOLATILITIES_AFTER_PERIOD));
    assertTrue(computed.equalWithTolerance(expected, EPS * NOTIONAL_VALUE_0 * 10d));
  }

  public void test_presentValueSensitivity_ended() {
    ExpandedCmsLeg expand = CAP_LEG.expand();
    PointSensitivityBuilder computed = LEG_PRICER.presentValueSensitivity(expand, RATES_PROVIDER_ENDED, VOLATILITIES_ENDED);
    assertEquals(computed, PointSensitivityBuilder.none());
  }

  //-------------------------------------------------------------------------
  public void test_presentValueSensitivitySabrParameter() {
    ExpandedCmsLeg expand = FLOOR_LEG.expand();
    SwaptionSabrSensitivities computed =
        LEG_PRICER.presentValueSensitivitySabrParameter(expand, RATES_PROVIDER, VOLATILITIES);
    SwaptionSabrSensitivities expected = SwaptionSabrSensitivities.empty();
    List<CmsPeriod> cms = expand.getCmsPeriods();
    int size = cms.size();
    for (int i = 0; i < size; ++i) {
      expected = expected.add(
          PERIOD_PRICER.presentValueSensitivitySabrParameter(cms.get(i), RATES_PROVIDER, VOLATILITIES));
    }
    assertEquals(computed, expected);
  }

  public void test_presentValueSensitivitySabrParameter_afterPay() {
    ExpandedCmsLeg expand = FLOOR_LEG.expand();
    SwaptionSabrSensitivities computed =
        LEG_PRICER.presentValueSensitivitySabrParameter(expand, RATES_PROVIDER_AFTER_PERIOD, VOLATILITIES_AFTER_PERIOD);
    SwaptionSabrSensitivities expected = SwaptionSabrSensitivities.empty();
    List<CmsPeriod> cms = expand.getCmsPeriods();
    int size = cms.size();
    for (int i = 0; i < size; ++i) {
      expected = expected.add(PERIOD_PRICER.presentValueSensitivitySabrParameter(
          cms.get(i), RATES_PROVIDER_AFTER_PERIOD, VOLATILITIES_AFTER_PERIOD));
    }
    assertEquals(computed, expected);
  }

  public void test_presentValueSensitivitySabrParameter_ended() {
    ExpandedCmsLeg expand = CAP_LEG.expand();
    SwaptionSabrSensitivities computed =
        LEG_PRICER.presentValueSensitivitySabrParameter(expand, RATES_PROVIDER_ENDED, VOLATILITIES_ENDED);
    List<CmsPeriod> cms = expand.getCmsPeriods();
    int size = cms.size();
    for (int i = 0; i < size; ++i) {
      assertEquals(computed.getSensitivities().get(i).getAlphaSensitivity(), 0d);
      assertEquals(computed.getSensitivities().get(i).getBetaSensitivity(), 0d);
      assertEquals(computed.getSensitivities().get(i).getRhoSensitivity(), 0d);
      assertEquals(computed.getSensitivities().get(i).getNuSensitivity(), 0d);
    }
  }

  //-------------------------------------------------------------------------
  public void test_presentValueSensitivityStrike() {
    ExpandedCmsLeg expand = CAP_LEG.expand();
    double computed = LEG_PRICER.presentValueSensitivityStrike(expand, RATES_PROVIDER, VOLATILITIES);
    double expected = 0d;
    List<CmsPeriod> cms = expand.getCmsPeriods();
    int size = cms.size();
    for (int i = 0; i < size; ++i) {
      expected += PERIOD_PRICER.presentValueSensitivityStrike(cms.get(i), RATES_PROVIDER, VOLATILITIES);
    }
    assertEquals(computed, expected, NOTIONAL_VALUE_0 * TOL);
  }

  public void test_presentValueSensitivityStrike_afterPay() {
    ExpandedCmsLeg expand = FLOOR_LEG.expand();
    double computed = LEG_PRICER.presentValueSensitivityStrike(expand, RATES_PROVIDER_AFTER_PERIOD,
        VOLATILITIES_AFTER_PERIOD);
    double expected = 0d;
    List<CmsPeriod> cms = expand.getCmsPeriods();
    int size = cms.size();
    for (int i = 1; i < size; ++i) {
      expected += PERIOD_PRICER.presentValueSensitivityStrike(
          cms.get(i), RATES_PROVIDER_AFTER_PERIOD, VOLATILITIES_AFTER_PERIOD);
    }
    assertEquals(computed, expected, NOTIONAL_VALUE_0 * TOL);
  }

  public void test_presentValueSensitivityStrike_ended() {
    ExpandedCmsLeg expand = CAP_LEG.expand();
    double computed = LEG_PRICER.presentValueSensitivityStrike(expand, RATES_PROVIDER_ENDED, VOLATILITIES_ENDED);
    assertEquals(computed, 0d);
  }

  //-------------------------------------------------------------------------
  public void test_currentCash() {
    ExpandedCmsLeg expand = FLOOR_LEG.expand();
    CurrencyAmount computed = LEG_PRICER.currentCash(expand, RATES_PROVIDER, VOLATILITIES);
    assertEquals(computed, CurrencyAmount.zero(EUR));
  }

  public void test_currentCash_onPay() {
    ExpandedCmsLeg expand = CAP_LEG.expand();
    CurrencyAmount computed = LEG_PRICER.currentCash(expand, RATES_PROVIDER_ON_PAY, VOLATILITIES_ON_PAY);
    assertEquals(computed.getAmount(), NOTIONAL_VALUE_1 * (OBS_INDEX - CAP_VALUE) * 367d / 360d, NOTIONAL_VALUE_0 * TOL);
  }

  public void test_currentCash_twoPayments() {
    ExpandedCmsLeg expand1 = FLOOR_LEG.expand();
    ExpandedCmsLeg expand2 = CAP_LEG.expand();
    ExpandedCmsLeg leg = ExpandedCmsLeg.builder()
        .cmsPeriods(expand1.getCmsPeriods().get(1), expand2.getCmsPeriods().get(1))
        .payReceive(RECEIVE)
        .build();
    CurrencyAmount computed = LEG_PRICER.currentCash(leg, RATES_PROVIDER_ON_PAY, VOLATILITIES_ON_PAY);
    assertEquals(computed.getAmount(),
        NOTIONAL_VALUE_1 * (OBS_INDEX - CAP_VALUE + FLOOR_VALUE_1 - OBS_INDEX) * 367d / 360d, NOTIONAL_VALUE_0 * TOL);
  }
}
