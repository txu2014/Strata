package com.opengamma.strata.pricer.impl.cms;

import static com.opengamma.strata.basics.currency.Currency.EUR;
import static com.opengamma.strata.basics.date.DayCounts.ACT_360;
import static com.opengamma.strata.collect.TestHelper.assertThrowsIllegalArg;
import static com.opengamma.strata.product.swap.SwapIndices.EUR_EURIBOR_1100_5Y;
import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertFalse;
import static org.testng.Assert.assertTrue;

import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;

import org.testng.annotations.Test;

import com.opengamma.strata.basics.currency.CurrencyAmount;
import com.opengamma.strata.collect.array.DoubleArray;
import com.opengamma.strata.market.curve.CurveCurrencyParameterSensitivities;
import com.opengamma.strata.market.sensitivity.PointSensitivityBuilder;
import com.opengamma.strata.market.sensitivity.SwaptionSabrSensitivity;
import com.opengamma.strata.market.surface.InterpolatedNodalSurface;
import com.opengamma.strata.market.surface.SurfaceCurrencyParameterSensitivities;
import com.opengamma.strata.market.surface.SurfaceCurrencyParameterSensitivity;
import com.opengamma.strata.market.surface.SurfaceParameterMetadata;
import com.opengamma.strata.market.surface.meta.SwaptionSurfaceExpiryTenorNodeMetadata;
import com.opengamma.strata.pricer.impl.option.SabrInterestRateParameters;
import com.opengamma.strata.pricer.impl.volatility.smile.function.SabrHaganVolatilityFunctionProvider;
import com.opengamma.strata.pricer.rate.ImmutableRatesProvider;
import com.opengamma.strata.pricer.sensitivity.RatesFiniteDifferenceSensitivityCalculator;
import com.opengamma.strata.pricer.swaption.SabrParametersSwaptionVolatilities;
import com.opengamma.strata.pricer.swaption.SwaptionSabrRateVolatilityDataSet;
import com.opengamma.strata.product.cms.CmsPeriod;

/**
 * Test {@link SabrExtrapolationReplicationCmsPeriodPricer}.
 */
@Test
public class SabrExtrapolationReplicationCmsPeriodPricerTest {

  private static final LocalDate VALUATION = LocalDate.of(2010, 8, 18);
  private static final LocalDate FIXING = LocalDate.of(2020, 4, 24);
  private static final LocalDate START = LocalDate.of(2020, 4, 28);
  private static final LocalDate END = LocalDate.of(2021, 4, 28);
  private static final LocalDate PAYMENT = LocalDate.of(2021, 4, 28);
  private static final LocalDate AFTER_PAYMENT = LocalDate.of(2021, 4, 29);

  private static final ImmutableRatesProvider RATES_PROVIDER =
      SwaptionSabrRateVolatilityDataSet.getRatesProviderEur(VALUATION);
  private static final SabrParametersSwaptionVolatilities VOLATILITIES =
      SwaptionSabrRateVolatilityDataSet.getVolatilitiesEur(VALUATION, false);
  private static final SabrParametersSwaptionVolatilities VOLATILITIES_SHIFT =
      SwaptionSabrRateVolatilityDataSet.getVolatilitiesEur(VALUATION, true);
  private static final double SHIFT = VOLATILITIES_SHIFT.getParameters().getShiftSurface().getZValues().get(0); // constant surface
  private static final ImmutableRatesProvider RATES_PROVIDER_ENDED =
      SwaptionSabrRateVolatilityDataSet.getRatesProviderEur(AFTER_PAYMENT);
  private static final SabrParametersSwaptionVolatilities VOLATILITIES_ENDED =
      SwaptionSabrRateVolatilityDataSet.getVolatilitiesEur(AFTER_PAYMENT, true);

  private static final double ACC_FACTOR = ACT_360.relativeYearFraction(START, END);
  private static final double NOTIONAL = 10000000;
  private static final double STRIKE = 0.04;
  private static final double STRIKE_NEGATIVE = -0.01;

  // CMS - buy
  private static final CmsPeriod COUPON = createCmsCoupon(true);
  private static final CmsPeriod CAPLET = createCmsCaplet(true, STRIKE);
  private static final CmsPeriod FLOORLET = createCmsFloorlet(true, STRIKE);
  // CMS - sell
  private static final CmsPeriod COUPON_SELL = createCmsCoupon(false);
  private static final CmsPeriod CAPLET_SELL = createCmsCaplet(false, STRIKE);
  private static final CmsPeriod FLOORLET_SELL = createCmsFloorlet(false, STRIKE);
  // CMS - zero strikes
  private static final CmsPeriod CAPLET_ZERO = createCmsCaplet(true, 0d);
  private static final CmsPeriod FLOORLET_ZERO = createCmsFloorlet(true, 0d);
  // CMS - negative strikes, to become positive after shift
  private static final CmsPeriod CAPLET_NEGATIVE = createCmsCaplet(true, STRIKE_NEGATIVE);
  private static final CmsPeriod FLOORLET_NEGATIVE = createCmsFloorlet(true, STRIKE_NEGATIVE);
  // CMS - negative strikes, to become zero after shift
  private static final CmsPeriod CAPLET_SHIFT = createCmsCaplet(true, -SHIFT);
  private static final CmsPeriod FLOORLET_SHIFT = createCmsFloorlet(true, -SHIFT);

  private static final double CUT_OFF_STRIKE = 0.10;
  private static final double MU = 2.50;
  private static final double EPS = 1.0e-5;
  private static final double TOL = 1.0e-12;
  private static final SabrExtrapolationReplicationCmsPeriodPricer PRICER =
      SabrExtrapolationReplicationCmsPeriodPricer.of(CUT_OFF_STRIKE, MU);
  private static final RatesFiniteDifferenceSensitivityCalculator FD_CAL =
      new RatesFiniteDifferenceSensitivityCalculator(EPS);

  public void test_presentValue_zero() {
    CurrencyAmount pv = PRICER.presentValue(COUPON, RATES_PROVIDER, VOLATILITIES);
    CurrencyAmount pvCaplet = PRICER.presentValue(CAPLET_ZERO, RATES_PROVIDER, VOLATILITIES);
    CurrencyAmount pvFloorlet = PRICER.presentValue(FLOORLET_ZERO, RATES_PROVIDER, VOLATILITIES);
    assertEquals(pv.getAmount(), pvCaplet.getAmount(), NOTIONAL * TOL);
    assertEquals(pvFloorlet.getAmount(), 0d, NOTIONAL * TOL);
    CurrencyAmount pvShift = PRICER.presentValue(COUPON, RATES_PROVIDER, VOLATILITIES_SHIFT);
    CurrencyAmount pvCapletShift = PRICER.presentValue(CAPLET_SHIFT, RATES_PROVIDER, VOLATILITIES_SHIFT);
    CurrencyAmount pvFloorletShift = PRICER.presentValue(FLOORLET_SHIFT, RATES_PROVIDER, VOLATILITIES_SHIFT);
    assertEquals(pvShift.getAmount(), pvCapletShift.getAmount(), NOTIONAL * TOL);
    assertEquals(pvFloorletShift.getAmount(), 0d, NOTIONAL * TOL);
  }

  public void test_presentValue_buySell() {
    CurrencyAmount pvBuy = PRICER.presentValue(COUPON, RATES_PROVIDER, VOLATILITIES);
    CurrencyAmount pvCapletBuy = PRICER.presentValue(CAPLET, RATES_PROVIDER, VOLATILITIES);
    CurrencyAmount pvFloorletBuy = PRICER.presentValue(FLOORLET, RATES_PROVIDER, VOLATILITIES);
    CurrencyAmount pvSell = PRICER.presentValue(COUPON_SELL, RATES_PROVIDER, VOLATILITIES);
    CurrencyAmount pvCapletSell = PRICER.presentValue(CAPLET_SELL, RATES_PROVIDER, VOLATILITIES);
    CurrencyAmount pvFloorletSell = PRICER.presentValue(FLOORLET_SELL, RATES_PROVIDER, VOLATILITIES);
    assertEquals(pvBuy.getAmount(), -pvSell.getAmount(), NOTIONAL * TOL);
    assertEquals(pvCapletBuy.getAmount(), -pvCapletSell.getAmount(), NOTIONAL * TOL);
    assertEquals(pvFloorletBuy.getAmount(), -pvFloorletSell.getAmount(), NOTIONAL * TOL);
  }

  public void test_presentValue_afterPayment() {
    CurrencyAmount pv = PRICER.presentValue(COUPON, RATES_PROVIDER_ENDED, VOLATILITIES_ENDED);
    CurrencyAmount pvCaplet = PRICER.presentValue(CAPLET, RATES_PROVIDER_ENDED, VOLATILITIES_ENDED);
    CurrencyAmount pvFloorlet = PRICER.presentValue(FLOORLET, RATES_PROVIDER_ENDED, VOLATILITIES_ENDED);
    assertEquals(pv, CurrencyAmount.zero(EUR));
    assertEquals(pvCaplet, CurrencyAmount.zero(EUR));
    assertEquals(pvFloorlet, CurrencyAmount.zero(EUR));
  }

  //-------------------------------------------------------------------------
  public void test_presentValueSensitivity() {
    PointSensitivityBuilder pvPointCoupon = PRICER.presentValueSensitivity(COUPON_SELL, RATES_PROVIDER, VOLATILITIES);
    CurveCurrencyParameterSensitivities computedCoupon = RATES_PROVIDER
        .curveParameterSensitivity(pvPointCoupon.build());
    CurveCurrencyParameterSensitivities expectedCoupon = FD_CAL.sensitivity(
        RATES_PROVIDER, p -> PRICER.presentValue(COUPON_SELL, p, VOLATILITIES));
    assertTrue(computedCoupon.equalWithTolerance(expectedCoupon, EPS * NOTIONAL * 50d));
    PointSensitivityBuilder pvCapPoint = PRICER.presentValueSensitivity(CAPLET_SELL, RATES_PROVIDER, VOLATILITIES);
    CurveCurrencyParameterSensitivities computedCap = RATES_PROVIDER.curveParameterSensitivity(pvCapPoint.build());
    CurveCurrencyParameterSensitivities expectedCap = FD_CAL.sensitivity(
        RATES_PROVIDER, p -> PRICER.presentValue(CAPLET_SELL, p, VOLATILITIES));
    assertTrue(computedCap.equalWithTolerance(expectedCap, EPS * NOTIONAL * 50d));
    PointSensitivityBuilder pvFloorPoint = PRICER.presentValueSensitivity(FLOORLET_SELL, RATES_PROVIDER, VOLATILITIES);
    CurveCurrencyParameterSensitivities computedFloor = RATES_PROVIDER.curveParameterSensitivity(pvFloorPoint.build());
    CurveCurrencyParameterSensitivities expectedFloor = FD_CAL.sensitivity(
        RATES_PROVIDER, p -> PRICER.presentValue(FLOORLET_SELL, p, VOLATILITIES));
    assertTrue(computedFloor.equalWithTolerance(expectedFloor, EPS * NOTIONAL * 10d));
  }

  public void test_presentValueSensitivity_shift() {
    PointSensitivityBuilder pvPointCoupon = PRICER.presentValueSensitivity(COUPON, RATES_PROVIDER, VOLATILITIES_SHIFT);
    CurveCurrencyParameterSensitivities computedCoupon = RATES_PROVIDER
        .curveParameterSensitivity(pvPointCoupon.build());
    CurveCurrencyParameterSensitivities expectedCoupon = FD_CAL.sensitivity(
        RATES_PROVIDER, p -> PRICER.presentValue(COUPON, p, VOLATILITIES_SHIFT));
    assertTrue(computedCoupon.equalWithTolerance(expectedCoupon, EPS * NOTIONAL * 50d));
    PointSensitivityBuilder pvCapPoint = PRICER.presentValueSensitivity(CAPLET_NEGATIVE, RATES_PROVIDER, VOLATILITIES_SHIFT);
    CurveCurrencyParameterSensitivities computedCap = RATES_PROVIDER.curveParameterSensitivity(pvCapPoint.build());
    CurveCurrencyParameterSensitivities expectedCap = FD_CAL.sensitivity(
        RATES_PROVIDER, p -> PRICER.presentValue(CAPLET_NEGATIVE, p, VOLATILITIES_SHIFT));
    assertTrue(computedCap.equalWithTolerance(expectedCap, EPS * NOTIONAL * 50d));
    PointSensitivityBuilder pvFloorPoint = PRICER.presentValueSensitivity(FLOORLET_NEGATIVE, RATES_PROVIDER, VOLATILITIES_SHIFT);
    CurveCurrencyParameterSensitivities computedFloor = RATES_PROVIDER.curveParameterSensitivity(pvFloorPoint.build());
    CurveCurrencyParameterSensitivities expectedFloor = FD_CAL.sensitivity(
        RATES_PROVIDER, p -> PRICER.presentValue(FLOORLET_NEGATIVE, p, VOLATILITIES_SHIFT));
    assertTrue(computedFloor.equalWithTolerance(expectedFloor, EPS * NOTIONAL * 10d));
  }

  public void test_presentValueSensitivity_afterPayment() {
    PointSensitivityBuilder pt = PRICER.presentValueSensitivity(COUPON, RATES_PROVIDER_ENDED, VOLATILITIES_ENDED);
    PointSensitivityBuilder ptCap = PRICER.presentValueSensitivity(CAPLET, RATES_PROVIDER_ENDED, VOLATILITIES_ENDED);
    PointSensitivityBuilder ptFloor = PRICER.presentValueSensitivity(FLOORLET, RATES_PROVIDER_ENDED, VOLATILITIES_ENDED);
    assertEquals(pt, PointSensitivityBuilder.none());
    assertEquals(ptCap, PointSensitivityBuilder.none());
    assertEquals(ptFloor, PointSensitivityBuilder.none());
  }

  //-------------------------------------------------------------------------
  public void test_presentValueSensitivitySabrParameter() {
    SwaptionSabrSensitivity pvPointCoupon =
        PRICER.presentValueSensitivitySabrParameter(COUPON_SELL, RATES_PROVIDER, VOLATILITIES);
    SurfaceCurrencyParameterSensitivities computedCoupon = VOLATILITIES.surfaceCurrencyParameterSensitivity(pvPointCoupon);
    SwaptionSabrSensitivity pvCapPoint =
        PRICER.presentValueSensitivitySabrParameter(CAPLET_SELL, RATES_PROVIDER, VOLATILITIES);
    SurfaceCurrencyParameterSensitivities computedCap = VOLATILITIES.surfaceCurrencyParameterSensitivity(pvCapPoint);
    SwaptionSabrSensitivity pvFloorPoint =
        PRICER.presentValueSensitivitySabrParameter(FLOORLET_SELL, RATES_PROVIDER, VOLATILITIES);
    SurfaceCurrencyParameterSensitivities computedFloor = VOLATILITIES.surfaceCurrencyParameterSensitivity(pvFloorPoint);

    SabrInterestRateParameters sabr = VOLATILITIES.getParameters();
    // alpha surface
    InterpolatedNodalSurface surfaceAlpha = (InterpolatedNodalSurface) sabr.getAlphaSurface();
    SurfaceCurrencyParameterSensitivity sensiCouponAlpha = computedCoupon.getSensitivity(surfaceAlpha.getName(), EUR);
    int nParamsAlpha = surfaceAlpha.getParameterCount();
    for (int i = 0; i < nParamsAlpha; ++i) {
      InterpolatedNodalSurface[] bumpedSurfaces = bumpSurface(surfaceAlpha, i);
      SabrInterestRateParameters sabrUp = SabrInterestRateParameters.of(bumpedSurfaces[0], sabr.getBetaSurface(),
          sabr.getRhoSurface(), sabr.getNuSurface(), SabrHaganVolatilityFunctionProvider.DEFAULT);
      SabrInterestRateParameters sabrDw = SabrInterestRateParameters.of(bumpedSurfaces[1], sabr.getBetaSurface(),
          sabr.getRhoSurface(), sabr.getNuSurface(), SabrHaganVolatilityFunctionProvider.DEFAULT);
      testSensitivityValue(
          COUPON_SELL, CAPLET_SELL, FLOORLET_SELL,
          sensiCouponAlpha.getMetadata().getParameterMetadata().get(),
          surfaceAlpha.getXValues().get(i),
          surfaceAlpha.getYValues().get(i),
          sensiCouponAlpha.getSensitivity(),
          computedCap.getSensitivity(surfaceAlpha.getName(), EUR).getSensitivity(),
          computedFloor.getSensitivity(surfaceAlpha.getName(), EUR).getSensitivity(),
          replaceSabrParameters(sabrUp),
          replaceSabrParameters(sabrDw));
    }
    // beta surface
    InterpolatedNodalSurface surfaceBeta = (InterpolatedNodalSurface) sabr.getBetaSurface();
    SurfaceCurrencyParameterSensitivity sensiCouponBeta = computedCoupon.getSensitivity(surfaceBeta.getName(), EUR);
    int nParamsBeta = surfaceBeta.getParameterCount();
    for (int i = 0; i < nParamsBeta; ++i) {
      InterpolatedNodalSurface[] bumpedSurfaces = bumpSurface(surfaceBeta, i);
      SabrInterestRateParameters sabrUp = SabrInterestRateParameters.of(sabr.getAlphaSurface(), bumpedSurfaces[0],
          sabr.getRhoSurface(), sabr.getNuSurface(), SabrHaganVolatilityFunctionProvider.DEFAULT);
      SabrInterestRateParameters sabrDw = SabrInterestRateParameters.of(sabr.getAlphaSurface(), bumpedSurfaces[1],
          sabr.getRhoSurface(), sabr.getNuSurface(), SabrHaganVolatilityFunctionProvider.DEFAULT);
      testSensitivityValue(
          COUPON_SELL, CAPLET_SELL, FLOORLET_SELL,
          sensiCouponBeta.getMetadata().getParameterMetadata().get(),
          surfaceBeta.getXValues().get(i),
          surfaceBeta.getYValues().get(i),
          sensiCouponBeta.getSensitivity(),
          computedCap.getSensitivity(surfaceBeta.getName(), EUR).getSensitivity(),
          computedFloor.getSensitivity(surfaceBeta.getName(), EUR).getSensitivity(),
          replaceSabrParameters(sabrUp),
          replaceSabrParameters(sabrDw));
    }
    // rho surface
    InterpolatedNodalSurface surfaceRho = (InterpolatedNodalSurface) sabr.getRhoSurface();
    SurfaceCurrencyParameterSensitivity sensiCouponRho = computedCoupon.getSensitivity(surfaceRho.getName(), EUR);
    int nParamsRho = surfaceRho.getParameterCount();
    for (int i = 0; i < nParamsRho; ++i) {
      InterpolatedNodalSurface[] bumpedSurfaces = bumpSurface(surfaceRho, i);
      SabrInterestRateParameters sabrUp = SabrInterestRateParameters.of(sabr.getAlphaSurface(), sabr.getBetaSurface(),
          bumpedSurfaces[0], sabr.getNuSurface(), SabrHaganVolatilityFunctionProvider.DEFAULT);
      SabrInterestRateParameters sabrDw = SabrInterestRateParameters.of(sabr.getAlphaSurface(), sabr.getBetaSurface(),
          bumpedSurfaces[1], sabr.getNuSurface(), SabrHaganVolatilityFunctionProvider.DEFAULT);
      testSensitivityValue(
          COUPON_SELL, CAPLET_SELL, FLOORLET_SELL,
          sensiCouponRho.getMetadata().getParameterMetadata().get(),
          surfaceRho.getXValues().get(i),
          surfaceRho.getYValues().get(i),
          sensiCouponRho.getSensitivity(),
          computedCap.getSensitivity(surfaceRho.getName(), EUR).getSensitivity(),
          computedFloor.getSensitivity(surfaceRho.getName(), EUR).getSensitivity(),
          replaceSabrParameters(sabrUp),
          replaceSabrParameters(sabrDw));
    }
    // nu surface
    InterpolatedNodalSurface surfaceNu = (InterpolatedNodalSurface) sabr.getNuSurface();
    SurfaceCurrencyParameterSensitivity sensiCouponNu = computedCoupon.getSensitivity(surfaceNu.getName(), EUR);
    int nParamsNu = surfaceNu.getParameterCount();
    for (int i = 0; i < nParamsNu; ++i) {
      InterpolatedNodalSurface[] bumpedSurfaces = bumpSurface(surfaceNu, i);
      SabrInterestRateParameters sabrUp = SabrInterestRateParameters.of(sabr.getAlphaSurface(), sabr.getBetaSurface(),
          sabr.getRhoSurface(), bumpedSurfaces[0], SabrHaganVolatilityFunctionProvider.DEFAULT);
      SabrInterestRateParameters sabrDw = SabrInterestRateParameters.of(sabr.getAlphaSurface(), sabr.getBetaSurface(),
          sabr.getRhoSurface(), bumpedSurfaces[1], SabrHaganVolatilityFunctionProvider.DEFAULT);
      testSensitivityValue(
          COUPON_SELL, CAPLET_SELL, FLOORLET_SELL,
          sensiCouponNu.getMetadata().getParameterMetadata().get(),
          surfaceNu.getXValues().get(i),
          surfaceNu.getYValues().get(i),
          sensiCouponNu.getSensitivity(),
          computedCap.getSensitivity(surfaceNu.getName(), EUR).getSensitivity(),
          computedFloor.getSensitivity(surfaceNu.getName(), EUR).getSensitivity(),
          replaceSabrParameters(sabrUp),
          replaceSabrParameters(sabrDw));
    }
  }

  public void test_presentValueSensitivitySabrParameter_shift() {
    SwaptionSabrSensitivity pvPointCoupon =
        PRICER.presentValueSensitivitySabrParameter(COUPON, RATES_PROVIDER, VOLATILITIES_SHIFT);
    SurfaceCurrencyParameterSensitivities computedCoupon = VOLATILITIES_SHIFT
        .surfaceCurrencyParameterSensitivity(pvPointCoupon);
    SwaptionSabrSensitivity pvCapPoint =
        PRICER.presentValueSensitivitySabrParameter(CAPLET_NEGATIVE, RATES_PROVIDER, VOLATILITIES_SHIFT);
    SurfaceCurrencyParameterSensitivities computedCap = VOLATILITIES_SHIFT
        .surfaceCurrencyParameterSensitivity(pvCapPoint);
    SwaptionSabrSensitivity pvFloorPoint =
        PRICER.presentValueSensitivitySabrParameter(FLOORLET_NEGATIVE, RATES_PROVIDER, VOLATILITIES_SHIFT);
    SurfaceCurrencyParameterSensitivities computedFloor = VOLATILITIES_SHIFT
        .surfaceCurrencyParameterSensitivity(pvFloorPoint);

    SabrInterestRateParameters sabr = VOLATILITIES_SHIFT.getParameters();
    // alpha surface
    InterpolatedNodalSurface surfaceAlpha = (InterpolatedNodalSurface) sabr.getAlphaSurface();
    SurfaceCurrencyParameterSensitivity sensiCouponAlpha = computedCoupon.getSensitivity(surfaceAlpha.getName(), EUR);
    int nParamsAlpha = surfaceAlpha.getParameterCount();
    for (int i = 0; i < nParamsAlpha; ++i) {
      InterpolatedNodalSurface[] bumpedSurfaces = bumpSurface(surfaceAlpha, i);
      SabrInterestRateParameters sabrUp = SabrInterestRateParameters.of(bumpedSurfaces[0], sabr.getBetaSurface(),
          sabr.getRhoSurface(), sabr.getNuSurface(), SabrHaganVolatilityFunctionProvider.DEFAULT, sabr.getShiftSurface());
      SabrInterestRateParameters sabrDw = SabrInterestRateParameters.of(bumpedSurfaces[1], sabr.getBetaSurface(),
          sabr.getRhoSurface(), sabr.getNuSurface(), SabrHaganVolatilityFunctionProvider.DEFAULT, sabr.getShiftSurface());
      testSensitivityValue(
          COUPON, CAPLET_NEGATIVE, FLOORLET_NEGATIVE,
          sensiCouponAlpha.getMetadata().getParameterMetadata().get(),
          surfaceAlpha.getXValues().get(i),
          surfaceAlpha.getYValues().get(i),
          sensiCouponAlpha.getSensitivity(),
          computedCap.getSensitivity(surfaceAlpha.getName(), EUR).getSensitivity(),
          computedFloor.getSensitivity(surfaceAlpha.getName(), EUR).getSensitivity(),
          replaceSabrParameters(sabrUp),
          replaceSabrParameters(sabrDw));
    }
    // beta surface
    InterpolatedNodalSurface surfaceBeta = (InterpolatedNodalSurface) sabr.getBetaSurface();
    SurfaceCurrencyParameterSensitivity sensiCouponBeta = computedCoupon.getSensitivity(surfaceBeta.getName(), EUR);
    int nParamsBeta = surfaceBeta.getParameterCount();
    for (int i = 0; i < nParamsBeta; ++i) {
      InterpolatedNodalSurface[] bumpedSurfaces = bumpSurface(surfaceBeta, i);
      SabrInterestRateParameters sabrUp = SabrInterestRateParameters.of(sabr.getAlphaSurface(), bumpedSurfaces[0],
          sabr.getRhoSurface(), sabr.getNuSurface(), SabrHaganVolatilityFunctionProvider.DEFAULT, sabr.getShiftSurface());
      SabrInterestRateParameters sabrDw = SabrInterestRateParameters.of(sabr.getAlphaSurface(), bumpedSurfaces[1],
          sabr.getRhoSurface(), sabr.getNuSurface(), SabrHaganVolatilityFunctionProvider.DEFAULT, sabr.getShiftSurface());
      testSensitivityValue(
          COUPON, CAPLET_NEGATIVE, FLOORLET_NEGATIVE,
          sensiCouponBeta.getMetadata().getParameterMetadata().get(),
          surfaceBeta.getXValues().get(i),
          surfaceBeta.getYValues().get(i),
          sensiCouponBeta.getSensitivity(),
          computedCap.getSensitivity(surfaceBeta.getName(), EUR).getSensitivity(),
          computedFloor.getSensitivity(surfaceBeta.getName(), EUR).getSensitivity(),
          replaceSabrParameters(sabrUp),
          replaceSabrParameters(sabrDw));
    }
    // rho surface
    InterpolatedNodalSurface surfaceRho = (InterpolatedNodalSurface) sabr.getRhoSurface();
    SurfaceCurrencyParameterSensitivity sensiCouponRho = computedCoupon.getSensitivity(surfaceRho.getName(), EUR);
    int nParamsRho = surfaceRho.getParameterCount();
    for (int i = 0; i < nParamsRho; ++i) {
      InterpolatedNodalSurface[] bumpedSurfaces = bumpSurface(surfaceRho, i);
      SabrInterestRateParameters sabrUp = SabrInterestRateParameters.of(sabr.getAlphaSurface(), sabr.getBetaSurface(),
          bumpedSurfaces[0], sabr.getNuSurface(), SabrHaganVolatilityFunctionProvider.DEFAULT, sabr.getShiftSurface());
      SabrInterestRateParameters sabrDw = SabrInterestRateParameters.of(sabr.getAlphaSurface(), sabr.getBetaSurface(),
          bumpedSurfaces[1], sabr.getNuSurface(), SabrHaganVolatilityFunctionProvider.DEFAULT, sabr.getShiftSurface());
      testSensitivityValue(
          COUPON, CAPLET_NEGATIVE, FLOORLET_NEGATIVE,
          sensiCouponRho.getMetadata().getParameterMetadata().get(),
          surfaceRho.getXValues().get(i),
          surfaceRho.getYValues().get(i),
          sensiCouponRho.getSensitivity(),
          computedCap.getSensitivity(surfaceRho.getName(), EUR).getSensitivity(),
          computedFloor.getSensitivity(surfaceRho.getName(), EUR).getSensitivity(),
          replaceSabrParameters(sabrUp),
          replaceSabrParameters(sabrDw));
    }
    // nu surface
    InterpolatedNodalSurface surfaceNu = (InterpolatedNodalSurface) sabr.getNuSurface();
    SurfaceCurrencyParameterSensitivity sensiCouponNu = computedCoupon.getSensitivity(surfaceNu.getName(), EUR);
    int nParamsNu = surfaceNu.getParameterCount();
    for (int i = 0; i < nParamsNu; ++i) {
      InterpolatedNodalSurface[] bumpedSurfaces = bumpSurface(surfaceNu, i);
      SabrInterestRateParameters sabrUp = SabrInterestRateParameters.of(sabr.getAlphaSurface(), sabr.getBetaSurface(),
          sabr.getRhoSurface(), bumpedSurfaces[0], SabrHaganVolatilityFunctionProvider.DEFAULT, sabr.getShiftSurface());
      SabrInterestRateParameters sabrDw = SabrInterestRateParameters.of(sabr.getAlphaSurface(), sabr.getBetaSurface(),
          sabr.getRhoSurface(), bumpedSurfaces[1], SabrHaganVolatilityFunctionProvider.DEFAULT, sabr.getShiftSurface());
      testSensitivityValue(
          COUPON, CAPLET_NEGATIVE, FLOORLET_NEGATIVE,
          sensiCouponNu.getMetadata().getParameterMetadata().get(),
          surfaceNu.getXValues().get(i),
          surfaceNu.getYValues().get(i),
          sensiCouponNu.getSensitivity(),
          computedCap.getSensitivity(surfaceNu.getName(), EUR).getSensitivity(),
          computedFloor.getSensitivity(surfaceNu.getName(), EUR).getSensitivity(),
          replaceSabrParameters(sabrUp),
          replaceSabrParameters(sabrDw));
    }
  }

  public void test_presentValueSensitivitySabrParameter_afterPayment() {
    SwaptionSabrSensitivity sensi =
        PRICER.presentValueSensitivitySabrParameter(COUPON, RATES_PROVIDER_ENDED, VOLATILITIES_ENDED);
    SwaptionSabrSensitivity sensiCap =
        PRICER.presentValueSensitivitySabrParameter(CAPLET, RATES_PROVIDER_ENDED, VOLATILITIES_ENDED);
    SwaptionSabrSensitivity sensiFloor =
        PRICER.presentValueSensitivitySabrParameter(FLOORLET, RATES_PROVIDER_ENDED, VOLATILITIES_ENDED);
    SwaptionSabrSensitivity sensiExpected = SwaptionSabrSensitivity.of(
        EUR_EURIBOR_1100_5Y.getTemplate().getConvention(), FIXING.atStartOfDay(ZoneOffset.UTC), 5d, EUR, 0d, 0d, 0d, 0d);
    assertEquals(sensi, sensiExpected);
    assertEquals(sensiCap, sensiExpected);
    assertEquals(sensiFloor, sensiExpected);
  }

  //-------------------------------------------------------------------------
  public void test_presentValueSensitivityStrike() {
    double computedCaplet = PRICER.presentValueSensitivityStrike(CAPLET, RATES_PROVIDER, VOLATILITIES);
    CmsPeriod capletUp = CmsPeriod.builder().dayCount(ACT_360).currency(EUR)
        .index(EUR_EURIBOR_1100_5Y).startDate(START).endDate(END).fixingDate(FIXING).notional(NOTIONAL)
        .paymentDate(PAYMENT).yearFraction(ACC_FACTOR).caplet(STRIKE + EPS).build();
    CmsPeriod capletDw = CmsPeriod.builder().dayCount(ACT_360).currency(EUR)
        .index(EUR_EURIBOR_1100_5Y).startDate(START).endDate(END).fixingDate(FIXING).notional(NOTIONAL)
        .paymentDate(PAYMENT).yearFraction(ACC_FACTOR).caplet(STRIKE - EPS).build();
    double expectedCaplet = 0.5 * (PRICER.presentValue(capletUp, RATES_PROVIDER, VOLATILITIES).getAmount()
        - PRICER.presentValue(capletDw, RATES_PROVIDER, VOLATILITIES).getAmount()) / EPS;
    assertEquals(computedCaplet, expectedCaplet, NOTIONAL * EPS);
    double computedFloorlet = PRICER.presentValueSensitivityStrike(FLOORLET, RATES_PROVIDER, VOLATILITIES);
    CmsPeriod floorletUp = CmsPeriod.builder().dayCount(ACT_360).currency(EUR)
        .index(EUR_EURIBOR_1100_5Y).startDate(START).endDate(END).fixingDate(FIXING).notional(NOTIONAL)
        .paymentDate(PAYMENT).yearFraction(ACC_FACTOR).floorlet(STRIKE + EPS).build();
    CmsPeriod floorletDw = CmsPeriod.builder().dayCount(ACT_360).currency(EUR)
        .index(EUR_EURIBOR_1100_5Y).startDate(START).endDate(END).fixingDate(FIXING).notional(NOTIONAL)
        .paymentDate(PAYMENT).yearFraction(ACC_FACTOR).floorlet(STRIKE - EPS).build();
    double expectedFloorlet = 0.5 * (PRICER.presentValue(floorletUp, RATES_PROVIDER, VOLATILITIES).getAmount()
        - PRICER.presentValue(floorletDw, RATES_PROVIDER, VOLATILITIES).getAmount()) / EPS;
    assertEquals(computedFloorlet, expectedFloorlet, NOTIONAL * EPS);
  }

  public void test_presentValueSensitivityStrike_shift() {
    double computedCaplet = PRICER.presentValueSensitivityStrike(CAPLET_NEGATIVE, RATES_PROVIDER, VOLATILITIES_SHIFT);
    CmsPeriod capletUp = CmsPeriod.builder().dayCount(ACT_360).currency(EUR)
        .index(EUR_EURIBOR_1100_5Y).startDate(START).endDate(END).fixingDate(FIXING).notional(NOTIONAL)
        .paymentDate(PAYMENT).yearFraction(ACC_FACTOR).caplet(STRIKE_NEGATIVE + EPS).build();
    CmsPeriod capletDw = CmsPeriod.builder().dayCount(ACT_360).currency(EUR)
        .index(EUR_EURIBOR_1100_5Y).startDate(START).endDate(END).fixingDate(FIXING).notional(NOTIONAL)
        .paymentDate(PAYMENT).yearFraction(ACC_FACTOR).caplet(STRIKE_NEGATIVE - EPS).build();
    double expectedCaplet = 0.5 * (PRICER.presentValue(capletUp, RATES_PROVIDER, VOLATILITIES_SHIFT).getAmount()
        - PRICER.presentValue(capletDw, RATES_PROVIDER, VOLATILITIES_SHIFT).getAmount()) / EPS;
    assertEquals(computedCaplet, expectedCaplet, NOTIONAL * EPS);
    double computedFloorlet = PRICER.presentValueSensitivityStrike(FLOORLET_NEGATIVE, RATES_PROVIDER, VOLATILITIES_SHIFT);
    CmsPeriod floorletUp = CmsPeriod.builder().dayCount(ACT_360).currency(EUR)
        .index(EUR_EURIBOR_1100_5Y).startDate(START).endDate(END).fixingDate(FIXING).notional(NOTIONAL)
        .paymentDate(PAYMENT).yearFraction(ACC_FACTOR).floorlet(STRIKE_NEGATIVE + EPS).build();
    CmsPeriod floorletDw = CmsPeriod.builder().dayCount(ACT_360).currency(EUR)
        .index(EUR_EURIBOR_1100_5Y).startDate(START).endDate(END).fixingDate(FIXING).notional(NOTIONAL)
        .paymentDate(PAYMENT).yearFraction(ACC_FACTOR).floorlet(STRIKE_NEGATIVE - EPS).build();
    double expectedFloorlet = 0.5 * (PRICER.presentValue(floorletUp, RATES_PROVIDER, VOLATILITIES_SHIFT).getAmount()
        - PRICER.presentValue(floorletDw, RATES_PROVIDER, VOLATILITIES_SHIFT).getAmount()) / EPS;
    assertEquals(computedFloorlet, expectedFloorlet, NOTIONAL * EPS);
  }

  public void test_presentValueSensitivityStrike_afterPayment() {
    double sensiCap = PRICER.presentValueSensitivityStrike(CAPLET, RATES_PROVIDER_ENDED, VOLATILITIES_ENDED);
    double sensiFloor = PRICER.presentValueSensitivityStrike(FLOORLET, RATES_PROVIDER_ENDED, VOLATILITIES_ENDED);
    assertEquals(sensiCap, 0d);
    assertEquals(sensiFloor, 0d);
  }

  public void test_presentValueSensitivityStrike_coupon() {
    assertThrowsIllegalArg(() -> PRICER.presentValueSensitivityStrike(COUPON, RATES_PROVIDER, VOLATILITIES));
  }

  //-------------------------------------------------------------------------
  private InterpolatedNodalSurface[] bumpSurface(InterpolatedNodalSurface surface, int position) {
    DoubleArray zValues = surface.getZValues();
    InterpolatedNodalSurface surfaceUp = surface.withZValues(zValues.with(position, zValues.get(position) + EPS));
    InterpolatedNodalSurface surfaceDw = surface.withZValues(zValues.with(position, zValues.get(position) - EPS));
    return new InterpolatedNodalSurface[] {surfaceUp, surfaceDw };
  }

  private SabrParametersSwaptionVolatilities replaceSabrParameters(SabrInterestRateParameters sabrParams) {
    return SabrParametersSwaptionVolatilities.of(
        sabrParams, VOLATILITIES.getConvention(), VOLATILITIES.getValuationDateTime(), VOLATILITIES.getDayCount());
  }

  private void testSensitivityValue(CmsPeriod coupon, CmsPeriod caplet, CmsPeriod floorlet,
      List<SurfaceParameterMetadata> listMeta, double expiry, double tenor, DoubleArray computedCouponSensi,
      DoubleArray computedCapSensi, DoubleArray computedFloorSensi, SabrParametersSwaptionVolatilities volsUp,
      SabrParametersSwaptionVolatilities volsDw) {
    double expectedCoupon = 0.5 * (PRICER.presentValue(coupon, RATES_PROVIDER, volsUp).getAmount()
        - PRICER.presentValue(coupon, RATES_PROVIDER, volsDw).getAmount()) / EPS;
    double expectedCap = 0.5 * (PRICER.presentValue(caplet, RATES_PROVIDER, volsUp).getAmount()
        - PRICER.presentValue(caplet, RATES_PROVIDER, volsDw).getAmount()) / EPS;
    double expectedFloor = 0.5 * (PRICER.presentValue(floorlet, RATES_PROVIDER, volsUp).getAmount()
        - PRICER.presentValue(floorlet, RATES_PROVIDER, volsDw).getAmount()) / EPS;
    int position = -1;
    for (int j = 0; j < listMeta.size(); ++j) {
      SwaptionSurfaceExpiryTenorNodeMetadata cast = (SwaptionSurfaceExpiryTenorNodeMetadata) listMeta.get(j);
      if (cast.getTenor() == tenor && cast.getYearFraction() == expiry) {
        position = j;
      }
    }
    assertFalse(position == -1, "sensitivity is not found");
    assertEquals(computedCouponSensi.get(position), expectedCoupon, EPS * NOTIONAL * 10d);
    assertEquals(computedCapSensi.get(position), expectedCap, EPS * NOTIONAL * 10d);
    assertEquals(computedFloorSensi.get(position), expectedFloor, EPS * NOTIONAL * 10d);
  }

  private static CmsPeriod createCmsCoupon(boolean isBuy) {
    double notional = isBuy ? NOTIONAL : -NOTIONAL;
    return CmsPeriod.builder()
        .dayCount(ACT_360)
        .currency(EUR)
        .index(EUR_EURIBOR_1100_5Y)
        .startDate(START)
        .endDate(END)
        .fixingDate(FIXING)
        .notional(notional)
        .paymentDate(PAYMENT)
        .yearFraction(ACC_FACTOR)
        .build();
  }

  private static CmsPeriod createCmsCaplet(boolean isBuy, double strike) {
    double notional = isBuy ? NOTIONAL : -NOTIONAL;
    return CmsPeriod.builder()
        .dayCount(ACT_360)
        .currency(EUR)
        .index(EUR_EURIBOR_1100_5Y)
        .startDate(START)
        .endDate(END)
        .fixingDate(FIXING)
        .notional(notional)
        .paymentDate(PAYMENT)
        .yearFraction(ACC_FACTOR)
        .caplet(strike)
        .build();
  }

  private static CmsPeriod createCmsFloorlet(boolean isBuy, double strike) {
    double notional = isBuy ? NOTIONAL : -NOTIONAL;
    return CmsPeriod.builder()
        .dayCount(ACT_360)
        .currency(EUR)
        .index(EUR_EURIBOR_1100_5Y)
        .startDate(START)
        .endDate(END)
        .fixingDate(FIXING)
        .notional(notional)
        .paymentDate(PAYMENT)
        .yearFraction(ACC_FACTOR)
        .floorlet(strike)
        .build();
  }

}
