/**
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.market.view;

import static com.opengamma.strata.basics.currency.Currency.GBP;
import static com.opengamma.strata.basics.date.DayCounts.ACT_365F;
import static com.opengamma.strata.basics.index.IborIndices.GBP_LIBOR_3M;
import static com.opengamma.strata.basics.index.IborIndices.USD_LIBOR_3M;
import static com.opengamma.strata.collect.TestHelper.assertThrowsIllegalArg;
import static com.opengamma.strata.collect.TestHelper.coverBeanEquals;
import static com.opengamma.strata.collect.TestHelper.coverImmutableBean;
import static com.opengamma.strata.collect.TestHelper.date;
import static org.testng.Assert.assertEquals;

import java.time.LocalDate;

import org.testng.annotations.Test;

import com.opengamma.strata.collect.array.DoubleArray;
import com.opengamma.strata.collect.timeseries.LocalDateDoubleTimeSeries;
import com.opengamma.strata.market.Perturbation;
import com.opengamma.strata.market.curve.Curve;
import com.opengamma.strata.market.curve.CurveMetadata;
import com.opengamma.strata.market.curve.CurveName;
import com.opengamma.strata.market.curve.Curves;
import com.opengamma.strata.market.curve.InterpolatedNodalCurve;
import com.opengamma.strata.market.interpolator.CurveInterpolator;
import com.opengamma.strata.market.interpolator.CurveInterpolators;
import com.opengamma.strata.market.sensitivity.IborRateSensitivity;
import com.opengamma.strata.market.sensitivity.PointSensitivityBuilder;

/**
 * Test {@link DiscountIborIndexRates}.
 */
@Test
public class DiscountIborIndexRatesTest {

  private static final LocalDate DATE_VAL = date(2015, 6, 4);
  private static final LocalDate DATE_BEFORE = date(2015, 6, 3);
  private static final LocalDate DATE_AFTER = date(2015, 7, 30);

  private static final CurveInterpolator INTERPOLATOR = CurveInterpolators.LINEAR;
  private static final CurveName NAME = CurveName.of("TestCurve");
  private static final CurveMetadata METADATA = Curves.zeroRates(NAME, ACT_365F);
  private static final InterpolatedNodalCurve CURVE =
      InterpolatedNodalCurve.of(METADATA, DoubleArray.of(0, 10), DoubleArray.of(0.01, 0.02), INTERPOLATOR);
  private static final InterpolatedNodalCurve CURVE2 =
      InterpolatedNodalCurve.of(METADATA, DoubleArray.of(0, 10), DoubleArray.of(0.01, 0.03), INTERPOLATOR);
  private static final ZeroRateDiscountFactors DFCURVE = ZeroRateDiscountFactors.of(GBP, DATE_VAL, CURVE);
  private static final ZeroRateDiscountFactors DFCURVE2 = ZeroRateDiscountFactors.of(GBP, DATE_VAL, CURVE2);

  private static final double RATE_BEFORE = 0.013d;
  private static final double RATE_VAL = 0.014d;
  private static final LocalDateDoubleTimeSeries SERIES = LocalDateDoubleTimeSeries.builder()
      .put(DATE_BEFORE, RATE_BEFORE)
      .put(DATE_VAL, RATE_VAL)
      .build();
  private static final LocalDateDoubleTimeSeries SERIES_MINIMAL = LocalDateDoubleTimeSeries.of(DATE_VAL, RATE_VAL);
  private static final LocalDateDoubleTimeSeries SERIES_EMPTY = LocalDateDoubleTimeSeries.empty();
  
  private static final double TOLERANCE_RATE = 1.0E-8;

  //-------------------------------------------------------------------------
  public void test_of_withoutFixings() {
    DiscountIborIndexRates test = DiscountIborIndexRates.of(GBP_LIBOR_3M, DFCURVE);
    assertEquals(test.getIndex(), GBP_LIBOR_3M);
    assertEquals(test.getValuationDate(), DATE_VAL);
    assertEquals(test.getFixings(), SERIES_EMPTY);
    assertEquals(test.getDiscountFactors(), DFCURVE);
    assertEquals(test.getCurveName(), NAME);
    assertEquals(test.getParameterCount(), 2);
  }

  public void test_of_withFixings() {
    DiscountIborIndexRates test = DiscountIborIndexRates.of(GBP_LIBOR_3M, DFCURVE, SERIES);
    assertEquals(test.getIndex(), GBP_LIBOR_3M);
    assertEquals(test.getValuationDate(), DATE_VAL);
    assertEquals(test.getFixings(), SERIES);
    assertEquals(test.getDiscountFactors(), DFCURVE);
    assertEquals(test.getCurveName(), NAME);
    assertEquals(test.getParameterCount(), 2);
  }

  //-------------------------------------------------------------------------
  public void test_applyPerturbation() {
    Perturbation<Curve> perturbation = curve -> CURVE2;
    DiscountIborIndexRates base = DiscountIborIndexRates.of(GBP_LIBOR_3M, DFCURVE, SERIES);
    DiscountIborIndexRates test = base.applyPerturbation(perturbation);
    test = test.withDiscountFactors(DFCURVE2);
    assertEquals(test.getIndex(), GBP_LIBOR_3M);
    assertEquals(test.getValuationDate(), DATE_VAL);
    assertEquals(test.getFixings(), SERIES);
    assertEquals(test.getDiscountFactors(), DFCURVE2);
    assertEquals(test.getCurveName(), NAME);
    assertEquals(test.getParameterCount(), 2);
  }

  public void test_withDiscountFactors() {
    DiscountIborIndexRates test = DiscountIborIndexRates.of(GBP_LIBOR_3M, DFCURVE, SERIES);
    test = test.withDiscountFactors(DFCURVE2);
    assertEquals(test.getIndex(), GBP_LIBOR_3M);
    assertEquals(test.getValuationDate(), DATE_VAL);
    assertEquals(test.getFixings(), SERIES);
    assertEquals(test.getDiscountFactors(), DFCURVE2);
    assertEquals(test.getCurveName(), NAME);
    assertEquals(test.getParameterCount(), 2);
  }

  //-------------------------------------------------------------------------
  public void test_rate_beforeValuation_fixing() {
    DiscountIborIndexRates test = DiscountIborIndexRates.of(GBP_LIBOR_3M, DFCURVE, SERIES);
    assertEquals(test.rate(DATE_BEFORE), RATE_BEFORE);
  }

  public void test_rate_beforeValuation_noFixing_emptySeries() {
    DiscountIborIndexRates test = DiscountIborIndexRates.of(GBP_LIBOR_3M, DFCURVE, SERIES_EMPTY);
    assertThrowsIllegalArg(() -> test.rate(DATE_BEFORE));
  }

  public void test_rate_beforeValuation_noFixing_notEmptySeries() {
    DiscountIborIndexRates test = DiscountIborIndexRates.of(GBP_LIBOR_3M, DFCURVE, SERIES_MINIMAL);
    assertThrowsIllegalArg(() -> test.rate(DATE_BEFORE));
  }

  public void test_rate_onValuation_fixing() {
    DiscountIborIndexRates test = DiscountIborIndexRates.of(GBP_LIBOR_3M, DFCURVE, SERIES);
    assertEquals(test.rate(DATE_VAL), RATE_VAL);
  }

  public void test_rateIgnoringTimeSeries_onValuation_fixing() {
    DiscountIborIndexRates test = DiscountIborIndexRates.of(GBP_LIBOR_3M, DFCURVE, SERIES);
    LocalDate startDate = GBP_LIBOR_3M.calculateEffectiveFromFixing(DATE_VAL);
    LocalDate endDate = GBP_LIBOR_3M.calculateMaturityFromEffective(startDate);
    double accrualFactor = GBP_LIBOR_3M.getDayCount().yearFraction(startDate, endDate);
    double expected = (DFCURVE.discountFactor(startDate) / DFCURVE.discountFactor(endDate) - 1) / accrualFactor;
    assertEquals(test.rateIgnoringTimeSeries(DATE_VAL), expected, TOLERANCE_RATE);
  }

  public void test_rate_onValuation_noFixing() {
    DiscountIborIndexRates test = DiscountIborIndexRates.of(GBP_LIBOR_3M, DFCURVE, SERIES_EMPTY);
    LocalDate startDate = GBP_LIBOR_3M.calculateEffectiveFromFixing(DATE_VAL);
    LocalDate endDate = GBP_LIBOR_3M.calculateMaturityFromEffective(startDate);
    double accrualFactor = GBP_LIBOR_3M.getDayCount().yearFraction(startDate, endDate);
    double expected = (DFCURVE.discountFactor(startDate) / DFCURVE.discountFactor(endDate) - 1) / accrualFactor;
    assertEquals(test.rate(DATE_VAL), expected, TOLERANCE_RATE);
    assertEquals(test.rateIgnoringTimeSeries(DATE_VAL), expected, TOLERANCE_RATE);
  }

  public void test_rate_afterValuation() {
    DiscountIborIndexRates test = DiscountIborIndexRates.of(GBP_LIBOR_3M, DFCURVE, SERIES);
    LocalDate startDate = GBP_LIBOR_3M.calculateEffectiveFromFixing(DATE_AFTER);
    LocalDate endDate = GBP_LIBOR_3M.calculateMaturityFromEffective(startDate);
    double accrualFactor = GBP_LIBOR_3M.getDayCount().yearFraction(startDate, endDate);
    double expected = (DFCURVE.discountFactor(startDate) / DFCURVE.discountFactor(endDate) - 1) / accrualFactor;
    assertEquals(test.rate(DATE_AFTER), expected, TOLERANCE_RATE);
  }

  //-------------------------------------------------------------------------
  public void test_ratePointSensitivity_fixing() {
    DiscountIborIndexRates test = DiscountIborIndexRates.of(GBP_LIBOR_3M, DFCURVE, SERIES);
    assertEquals(test.ratePointSensitivity(DATE_BEFORE), PointSensitivityBuilder.none());
    assertEquals(test.ratePointSensitivity(DATE_VAL), PointSensitivityBuilder.none());
  }
  
  public void test_rateIgnoringTimeSeriesPointSensitivity_onValuation() {
    DiscountIborIndexRates test = DiscountIborIndexRates.of(GBP_LIBOR_3M, DFCURVE, SERIES);
    IborRateSensitivity expected = IborRateSensitivity.of(GBP_LIBOR_3M, DATE_VAL, 1d);
    assertEquals(test.rateIgnoringTimeSeriesPointSensitivity(DATE_VAL), expected);
  }

  public void test_ratePointSensitivity_onValuation_noFixing() {
    DiscountIborIndexRates test = DiscountIborIndexRates.of(GBP_LIBOR_3M, DFCURVE, SERIES_EMPTY);
    IborRateSensitivity expected = IborRateSensitivity.of(GBP_LIBOR_3M, DATE_VAL, 1d);
    assertEquals(test.ratePointSensitivity(DATE_VAL), expected);
    assertEquals(test.rateIgnoringTimeSeriesPointSensitivity(DATE_VAL), expected);
  }

  public void test_ratePointSensitivity_afterValuation() {
    DiscountIborIndexRates test = DiscountIborIndexRates.of(GBP_LIBOR_3M, DFCURVE, SERIES);
    IborRateSensitivity expected = IborRateSensitivity.of(GBP_LIBOR_3M, DATE_AFTER, 1d);
    assertEquals(test.ratePointSensitivity(DATE_AFTER), expected);
  }

  //-------------------------------------------------------------------------
  // proper end-to-end tests are elsewhere
  public void test_curveParameterSensitivity() {
    DiscountIborIndexRates test = DiscountIborIndexRates.of(GBP_LIBOR_3M, DFCURVE, SERIES);
    IborRateSensitivity point = IborRateSensitivity.of(GBP_LIBOR_3M, DATE_AFTER, GBP, 1d);
    assertEquals(test.curveParameterSensitivity(point).size(), 1);
  }

  //-------------------------------------------------------------------------
  public void coverage() {
    DiscountIborIndexRates test = DiscountIborIndexRates.of(GBP_LIBOR_3M, DFCURVE, SERIES);
    coverImmutableBean(test);
    DiscountIborIndexRates test2 = DiscountIborIndexRates.of(USD_LIBOR_3M, DFCURVE2, SERIES_EMPTY);
    coverBeanEquals(test, test2);
  }

}
