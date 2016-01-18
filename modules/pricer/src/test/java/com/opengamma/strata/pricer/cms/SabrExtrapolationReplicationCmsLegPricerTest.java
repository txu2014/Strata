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
import com.opengamma.strata.pricer.impl.cms.SabrExtrapolationReplicationCmsPeriodPricer;
import com.opengamma.strata.pricer.rate.ImmutableRatesProvider;
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

  private static final LocalDate VALUATION = LocalDate.of(2015, 8, 18);
  private static final SwapIndex INDEX = SwapIndices.EUR_EURIBOR_1100_10Y;
  private static final LocalDate START = LocalDate.of(2015, 10, 21);
  private static final LocalDate END = LocalDate.of(2020, 10, 21);
  private static final Frequency FREQUENCY = Frequency.P12M;
  private static final BusinessDayAdjustment BUSS_ADJ_EUR =
      BusinessDayAdjustment.of(BusinessDayConventions.FOLLOWING, EUTA);
  private static final PeriodicSchedule SCHEDULE_EUR =
      PeriodicSchedule.of(START, END, FREQUENCY, BUSS_ADJ_EUR, StubConvention.NONE, RollConventions.NONE);
  private static final ValueSchedule CAP_STRIKE = ValueSchedule.of(0.0125);
  private static final List<ValueStep> FLOOR_STEPS = new ArrayList<ValueStep>();
  private static final List<ValueStep> NOTIONAL_STEPS = new ArrayList<ValueStep>();
  static {
    FLOOR_STEPS.add(ValueStep.of(1, ValueAdjustment.ofReplace(0.02)));
    NOTIONAL_STEPS.add(ValueStep.of(1, ValueAdjustment.ofReplace(1.2e6)));
  }
  private static final ValueSchedule FLOOR_STRIKE = ValueSchedule.of(0.011, FLOOR_STEPS);
  private static final ValueSchedule NOTIONAL = ValueSchedule.of(1.0e6, NOTIONAL_STEPS);

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

  private static final ImmutableRatesProvider RATES_PROVIDER =
      SwaptionSabrRateVolatilityDataSet.getRatesProviderEur(VALUATION);
  private static final SabrParametersSwaptionVolatilities VOLATILITIES =
      SwaptionSabrRateVolatilityDataSet.getVolatilitiesEur(VALUATION, true);

  private static final LocalDate PAYMENT = LocalDate.of(2016, 10, 21);
  private static final LocalDate FIXING = LocalDate.of(2015, 10, 19);

  private static final double CUT_OFF_STRIKE = 0.10;
  private static final double MU = 2.50;
  private static final SabrExtrapolationReplicationCmsPeriodPricer PERIOD_PRICER =
      SabrExtrapolationReplicationCmsPeriodPricer.of(CUT_OFF_STRIKE, MU);
  private static final SabrExtrapolationReplicationCmsLegPricer LEG_PRICER =
      new SabrExtrapolationReplicationCmsLegPricer(PERIOD_PRICER);

  public void test_presentValue() {
    ExpandedCmsLeg expandCap = CAP_LEG.expand();
    CurrencyAmount computedCap = LEG_PRICER.presentValue(expandCap, RATES_PROVIDER, VOLATILITIES);
    double expectedCap = 0d;
    for (CmsPeriod cms : expandCap.getCmsPeriods()) {
      expectedCap += PERIOD_PRICER.presentValue(cms, RATES_PROVIDER, VOLATILITIES).getAmount();
    }
    assertEquals(computedCap.getCurrency(), EUR);
    assertEquals(computedCap.getAmount(), expectedCap);
  }

}
