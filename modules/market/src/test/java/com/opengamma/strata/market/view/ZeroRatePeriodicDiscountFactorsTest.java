/**
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.market.view;

import static com.opengamma.strata.basics.currency.Currency.GBP;
import static com.opengamma.strata.basics.currency.Currency.USD;
import static com.opengamma.strata.basics.date.DayCounts.ACT_365F;
import static com.opengamma.strata.collect.TestHelper.assertThrowsIllegalArg;
import static com.opengamma.strata.collect.TestHelper.coverBeanEquals;
import static com.opengamma.strata.collect.TestHelper.coverImmutableBean;
import static com.opengamma.strata.collect.TestHelper.date;
import static com.opengamma.strata.market.value.CompoundedRateType.CONTINUOUS;
import static com.opengamma.strata.market.value.CompoundedRateType.PERIODIC;
import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertTrue;

import java.time.LocalDate;

import org.testng.annotations.Test;

import com.opengamma.strata.collect.array.DoubleArray;
import com.opengamma.strata.market.Perturbation;
import com.opengamma.strata.market.ValueType;
import com.opengamma.strata.market.curve.Curve;
import com.opengamma.strata.market.curve.CurveInfoType;
import com.opengamma.strata.market.curve.CurveMetadata;
import com.opengamma.strata.market.curve.CurveName;
import com.opengamma.strata.market.curve.CurveUnitParameterSensitivities;
import com.opengamma.strata.market.curve.Curves;
import com.opengamma.strata.market.curve.DefaultCurveMetadata;
import com.opengamma.strata.market.curve.InterpolatedNodalCurve;
import com.opengamma.strata.market.interpolator.CurveInterpolator;
import com.opengamma.strata.market.interpolator.CurveInterpolators;
import com.opengamma.strata.market.sensitivity.ZeroRateSensitivity;

/**
 * Test {@link ZeroRatePeriodicDiscountFactors}.
 */
@Test
public class ZeroRatePeriodicDiscountFactorsTest {

  private static final LocalDate DATE_VAL = date(2015, 6, 4);
  private static final LocalDate DATE_AFTER = date(2016, 7, 21);

  private static final CurveInterpolator INTERPOLATOR = CurveInterpolators.LINEAR;
  private static final CurveName NAME = CurveName.of("TestCurve");
  private static final int CMP_PERIOD = 2;
  private static final CurveMetadata META_ZERO_PERIODIC = DefaultCurveMetadata.builder()
      .curveName(NAME)
      .xValueType(ValueType.YEAR_FRACTION)
      .yValueType(ValueType.ZERO_RATE)
      .dayCount(ACT_365F)
      .addInfo(CurveInfoType.COMPOUNDING_PER_YEAR, CMP_PERIOD)
      .build();

  private static final InterpolatedNodalCurve CURVE =
      InterpolatedNodalCurve.of(META_ZERO_PERIODIC, DoubleArray.of(0, 10), DoubleArray.of(0.01, 0.02), INTERPOLATOR);
  private static final InterpolatedNodalCurve CURVE2 =
      InterpolatedNodalCurve.of(META_ZERO_PERIODIC, DoubleArray.of(0, 10), DoubleArray.of(2, 3), INTERPOLATOR);

  private static final double SPREAD = 0.05;
  private static final double TOLERANCE_DF = 1.0e-12;
  private static final double TOLERANCE_DELTA = 1.0e-10;

  //-------------------------------------------------------------------------
  public void test_of() {
    ZeroRatePeriodicDiscountFactors test = ZeroRatePeriodicDiscountFactors.of(GBP, DATE_VAL, CURVE);
    assertEquals(test.getCurrency(), GBP);
    assertEquals(test.getValuationDate(), DATE_VAL);
    assertEquals(test.getCurve(), CURVE);
    assertEquals(test.getCurveName(), NAME);
    assertEquals(test.getParameterCount(), 2);
  }

  public void test_of_badCurve() {
    InterpolatedNodalCurve notYearFraction = InterpolatedNodalCurve.of(
        Curves.prices(NAME), DoubleArray.of(0, 10), DoubleArray.of(1, 2), INTERPOLATOR);
    InterpolatedNodalCurve notZeroRate = InterpolatedNodalCurve.of(
        Curves.discountFactors(NAME, ACT_365F), DoubleArray.of(0, 10), DoubleArray.of(1, 2), INTERPOLATOR);
    CurveMetadata noDayCountMetadata = DefaultCurveMetadata.builder()
        .curveName(NAME)
        .xValueType(ValueType.YEAR_FRACTION)
        .yValueType(ValueType.ZERO_RATE)
        .addInfo(CurveInfoType.COMPOUNDING_PER_YEAR, 4)
        .build();
    InterpolatedNodalCurve notDayCount = InterpolatedNodalCurve.of(
        noDayCountMetadata, DoubleArray.of(0, 10), DoubleArray.of(1, 2), INTERPOLATOR);
    CurveMetadata metaNoCompoundPerYear = DefaultCurveMetadata.builder()
        .curveName(NAME)
        .xValueType(ValueType.YEAR_FRACTION)
        .yValueType(ValueType.ZERO_RATE)
        .dayCount(ACT_365F)
        .build();
    InterpolatedNodalCurve notCompoundPerYear = InterpolatedNodalCurve.of(
        metaNoCompoundPerYear, DoubleArray.of(0, 10), DoubleArray.of(1, 2), INTERPOLATOR);
    CurveMetadata metaNegativeNb = DefaultCurveMetadata.builder()
        .curveName(NAME)
        .xValueType(ValueType.YEAR_FRACTION)
        .yValueType(ValueType.ZERO_RATE)
        .dayCount(ACT_365F)
        .addInfo(CurveInfoType.COMPOUNDING_PER_YEAR, -1)
        .build();
    InterpolatedNodalCurve curveNegativeNb = InterpolatedNodalCurve.of(
        metaNegativeNb, DoubleArray.of(0, 10), DoubleArray.of(1, 2), INTERPOLATOR);
    assertThrowsIllegalArg(() -> ZeroRatePeriodicDiscountFactors.of(GBP, DATE_VAL, notYearFraction));
    assertThrowsIllegalArg(() -> ZeroRatePeriodicDiscountFactors.of(GBP, DATE_VAL, notZeroRate));
    assertThrowsIllegalArg(() -> ZeroRatePeriodicDiscountFactors.of(GBP, DATE_VAL, notDayCount));
    assertThrowsIllegalArg(() -> ZeroRatePeriodicDiscountFactors.of(GBP, DATE_VAL, notCompoundPerYear));
    assertThrowsIllegalArg(() -> ZeroRatePeriodicDiscountFactors.of(GBP, DATE_VAL, curveNegativeNb));
  }

  //-------------------------------------------------------------------------
  public void test_discountFactor() {
    ZeroRatePeriodicDiscountFactors test = ZeroRatePeriodicDiscountFactors.of(GBP, DATE_VAL, CURVE);
    double relativeYearFraction = ACT_365F.relativeYearFraction(DATE_VAL, DATE_AFTER);
    double expected = Math.pow(1.0d + CURVE.yValue(relativeYearFraction) / CMP_PERIOD,
        -CMP_PERIOD * relativeYearFraction);
    assertEquals(test.discountFactor(DATE_AFTER), expected);
  }

  //-------------------------------------------------------------------------
  public void test_discountFactorWithSpread_continuous() {
    ZeroRatePeriodicDiscountFactors test = ZeroRatePeriodicDiscountFactors.of(GBP, DATE_VAL, CURVE);
    double relativeYearFraction = ACT_365F.relativeYearFraction(DATE_VAL, DATE_AFTER);
    double df = test.discountFactor(DATE_AFTER);
    double expected = df * Math.exp(-SPREAD * relativeYearFraction);
    assertEquals(test.discountFactorWithSpread(DATE_AFTER, SPREAD, CONTINUOUS, 0), expected, TOLERANCE_DF);
  }

  public void test_discountFactorWithSpread_periodic() {
    int periodPerYear = 4;
    ZeroRatePeriodicDiscountFactors test = ZeroRatePeriodicDiscountFactors.of(GBP, DATE_VAL, CURVE);
    double relativeYearFraction = ACT_365F.relativeYearFraction(DATE_VAL, DATE_AFTER);
    double discountFactorBase = test.discountFactor(DATE_AFTER);
    double onePlus = Math.pow(discountFactorBase, -1.0d / (periodPerYear * relativeYearFraction));
    double expected = Math.pow(onePlus + SPREAD / periodPerYear, -periodPerYear * relativeYearFraction);
    assertEquals(test.discountFactorWithSpread(DATE_AFTER, SPREAD, PERIODIC, periodPerYear), expected, TOLERANCE_DF);
  }

  public void test_discountFactorWithSpread_smallYearFraction() {
    ZeroRatePeriodicDiscountFactors test = ZeroRatePeriodicDiscountFactors.of(GBP, DATE_VAL, CURVE);
    assertEquals(test.discountFactorWithSpread(DATE_VAL, SPREAD, PERIODIC, 1), 1d, TOLERANCE_DF);
  }

  //-------------------------------------------------------------------------
  public void test_zeroRatePointSensitivity() {
    ZeroRatePeriodicDiscountFactors test = ZeroRatePeriodicDiscountFactors.of(GBP, DATE_VAL, CURVE);
    double relativeYearFraction = ACT_365F.relativeYearFraction(DATE_VAL, DATE_AFTER);
    double df = test.discountFactor(DATE_AFTER);
    ZeroRateSensitivity expected = ZeroRateSensitivity.of(GBP, DATE_AFTER, -df * relativeYearFraction);
    assertEquals(test.zeroRatePointSensitivity(DATE_AFTER), expected);
  }

  public void test_zeroRatePointSensitivity_sensitivityCurrency() {
    ZeroRatePeriodicDiscountFactors test = ZeroRatePeriodicDiscountFactors.of(GBP, DATE_VAL, CURVE);
    double relativeYearFraction = ACT_365F.relativeYearFraction(DATE_VAL, DATE_AFTER);
    double df = test.discountFactor(DATE_AFTER);
    ZeroRateSensitivity expected = ZeroRateSensitivity.of(GBP, DATE_AFTER, USD, -df * relativeYearFraction);
    assertEquals(test.zeroRatePointSensitivity(DATE_AFTER, USD), expected);
  }

  //-------------------------------------------------------------------------
  public void test_zeroRatePointSensitivityWithSpread_continous() {
    ZeroRatePeriodicDiscountFactors test = ZeroRatePeriodicDiscountFactors.of(GBP, DATE_VAL, CURVE);
    double relativeYearFraction = ACT_365F.relativeYearFraction(DATE_VAL, DATE_AFTER);
    double df = test.discountFactorWithSpread(DATE_AFTER, SPREAD, CONTINUOUS, 0);
    ZeroRateSensitivity expected = ZeroRateSensitivity.of(GBP, DATE_AFTER, -df * relativeYearFraction);
    ZeroRateSensitivity computed = test.zeroRatePointSensitivityWithSpread(DATE_AFTER, SPREAD, CONTINUOUS, 0);
    assertTrue(computed.compareKey(expected) == 0);
    assertEquals(computed.getSensitivity(), expected.getSensitivity(), TOLERANCE_DELTA);
  }

  public void test_zeroRatePointSensitivityWithSpread_sensitivityCurrency_continous() {
    ZeroRatePeriodicDiscountFactors test = ZeroRatePeriodicDiscountFactors.of(GBP, DATE_VAL, CURVE);
    double relativeYearFraction = ACT_365F.relativeYearFraction(DATE_VAL, DATE_AFTER);
    double df = test.discountFactorWithSpread(DATE_AFTER, SPREAD, CONTINUOUS, 0);
    ZeroRateSensitivity expected = ZeroRateSensitivity.of(GBP, DATE_AFTER, USD, -df * relativeYearFraction);
    ZeroRateSensitivity computed = test.zeroRatePointSensitivityWithSpread(DATE_AFTER, USD, SPREAD, CONTINUOUS, 0);
    assertTrue(computed.compareKey(expected) == 0);
    assertEquals(computed.getSensitivity(), expected.getSensitivity(), TOLERANCE_DELTA);
  }

  public void test_zeroRatePointSensitivityWithSpread_periodic() {
    int periodPerYear = 4;
    ZeroRatePeriodicDiscountFactors test = ZeroRatePeriodicDiscountFactors.of(GBP, DATE_VAL, CURVE);
    double relativeYearFraction = ACT_365F.relativeYearFraction(DATE_VAL, DATE_AFTER);
    double df = test.discountFactorWithSpread(DATE_AFTER, SPREAD, PERIODIC, periodPerYear);
    ZeroRateSensitivity expected = ZeroRateSensitivity.of(GBP, DATE_AFTER, -df * relativeYearFraction);
    ZeroRateSensitivity computed = test.zeroRatePointSensitivityWithSpread(DATE_AFTER, SPREAD, PERIODIC, periodPerYear);
    assertTrue(computed.compareKey(expected) == 0);
    assertEquals(computed.getSensitivity(), expected.getSensitivity(), TOLERANCE_DELTA);
  }

  public void test_zeroRatePointSensitivityWithSpread_sensitivityCurrency_periodic() {
    int periodPerYear = 4;
    ZeroRatePeriodicDiscountFactors test = ZeroRatePeriodicDiscountFactors.of(GBP, DATE_VAL, CURVE);
    double relativeYearFraction = ACT_365F.relativeYearFraction(DATE_VAL, DATE_AFTER);
    double df = test.discountFactorWithSpread(DATE_AFTER, SPREAD, PERIODIC, periodPerYear);
    ZeroRateSensitivity expected = ZeroRateSensitivity.of(GBP, DATE_AFTER, USD, -df * relativeYearFraction);
    ZeroRateSensitivity computed = test.zeroRatePointSensitivityWithSpread(DATE_AFTER, USD, SPREAD, PERIODIC, periodPerYear);
    assertTrue(computed.compareKey(expected) == 0);
    assertEquals(computed.getSensitivity(), expected.getSensitivity(), TOLERANCE_DELTA);
  }

  public void test_zeroRatePointSensitivityWithSpread_smallYearFraction() {
    ZeroRatePeriodicDiscountFactors test = ZeroRatePeriodicDiscountFactors.of(GBP, DATE_VAL, CURVE);
    ZeroRateSensitivity expected = ZeroRateSensitivity.of(GBP, DATE_VAL, -0d);
    assertEquals(test.zeroRatePointSensitivityWithSpread(DATE_VAL, SPREAD, CONTINUOUS, 0), expected);
  }

  public void test_zeroRatePointSensitivityWithSpread_sensitivityCurrency_smallYearFraction() {
    ZeroRatePeriodicDiscountFactors test = ZeroRatePeriodicDiscountFactors.of(GBP, DATE_VAL, CURVE);
    ZeroRateSensitivity expected = ZeroRateSensitivity.of(GBP, DATE_VAL, USD, -0d);
    assertEquals(test.zeroRatePointSensitivityWithSpread(DATE_VAL, USD, SPREAD, PERIODIC, 2), expected);
  }

  //-------------------------------------------------------------------------
  public void test_unitParameterSensitivity() {
    ZeroRatePeriodicDiscountFactors test = ZeroRatePeriodicDiscountFactors.of(GBP, DATE_VAL, CURVE);
    double relativeYearFraction = ACT_365F.relativeYearFraction(DATE_VAL, DATE_AFTER);
    CurveUnitParameterSensitivities expected = CurveUnitParameterSensitivities.of(
        CURVE.yValueParameterSensitivity(relativeYearFraction));
    assertEquals(test.unitParameterSensitivity(DATE_AFTER), expected);
  }

  //-------------------------------------------------------------------------
  // proper end-to-end FD tests are elsewhere
  public void test_curveParameterSensitivity() {
    ZeroRatePeriodicDiscountFactors test = ZeroRatePeriodicDiscountFactors.of(GBP, DATE_VAL, CURVE);
    ZeroRateSensitivity point = ZeroRateSensitivity.of(GBP, DATE_AFTER, 1d);
    assertEquals(test.curveParameterSensitivity(point).size(), 1);
  }

  //-------------------------------------------------------------------------
  public void test_applyPerturbation() {
    Perturbation<Curve> perturbation = curve -> CURVE2;
    ZeroRatePeriodicDiscountFactors test =
        ZeroRatePeriodicDiscountFactors.of(GBP, DATE_VAL, CURVE).applyPerturbation(perturbation);
    assertEquals(test.getCurve(), CURVE2);
  }

  public void test_withCurve() {
    ZeroRatePeriodicDiscountFactors test = ZeroRatePeriodicDiscountFactors.of(GBP, DATE_VAL, CURVE).withCurve(CURVE2);
    assertEquals(test.getCurve(), CURVE2);
  }

  //-------------------------------------------------------------------------
  public void coverage() {
    ZeroRatePeriodicDiscountFactors test = ZeroRatePeriodicDiscountFactors.of(GBP, DATE_VAL, CURVE);
    coverImmutableBean(test);
    ZeroRatePeriodicDiscountFactors test2 = ZeroRatePeriodicDiscountFactors.of(USD, DATE_VAL.plusDays(1), CURVE2);
    coverBeanEquals(test, test2);
  }

}
