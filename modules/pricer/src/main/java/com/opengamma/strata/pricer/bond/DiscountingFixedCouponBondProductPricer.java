/**
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.pricer.bond;

import java.time.LocalDate;
import java.util.function.Function;

import com.google.common.collect.ImmutableList;
import com.opengamma.strata.basics.currency.CurrencyAmount;
import com.opengamma.strata.basics.currency.Payment;
import com.opengamma.strata.basics.date.DaysAdjustment;
import com.opengamma.strata.basics.schedule.Schedule;
import com.opengamma.strata.basics.schedule.SchedulePeriod;
import com.opengamma.strata.collect.ArgChecker;
import com.opengamma.strata.collect.id.StandardId;
import com.opengamma.strata.market.sensitivity.IssuerCurveZeroRateSensitivity;
import com.opengamma.strata.market.sensitivity.PointSensitivityBuilder;
import com.opengamma.strata.market.sensitivity.RepoCurveZeroRateSensitivity;
import com.opengamma.strata.market.sensitivity.ZeroRateSensitivity;
import com.opengamma.strata.market.value.CompoundedRateType;
import com.opengamma.strata.market.view.IssuerCurveDiscountFactors;
import com.opengamma.strata.market.view.RepoCurveDiscountFactors;
import com.opengamma.strata.math.impl.rootfinding.BracketRoot;
import com.opengamma.strata.math.impl.rootfinding.BrentSingleRootFinder;
import com.opengamma.strata.math.impl.rootfinding.RealSingleRootFinder;
import com.opengamma.strata.pricer.DiscountingPaymentPricer;
import com.opengamma.strata.pricer.impl.bond.DiscountingFixedCouponBondPaymentPeriodPricer;
import com.opengamma.strata.pricer.rate.LegalEntityDiscountingProvider;
import com.opengamma.strata.product.Security;
import com.opengamma.strata.product.bond.FixedCouponBondPaymentPeriod;
import com.opengamma.strata.product.bond.ResolvedFixedCouponBond;
import com.opengamma.strata.product.bond.YieldConvention;

/**
 * Pricer for for rate fixed coupon bond products.
 * <p>
 * This function provides the ability to price a {@link ResolvedFixedCouponBond}.
 */
public class DiscountingFixedCouponBondProductPricer {

  /**
   * Default implementation.
   */
  public static final DiscountingFixedCouponBondProductPricer DEFAULT = new DiscountingFixedCouponBondProductPricer(
      DiscountingFixedCouponBondPaymentPeriodPricer.DEFAULT,
      DiscountingPaymentPricer.DEFAULT);

  /**
   * The root finder.
   */
  private static final RealSingleRootFinder ROOT_FINDER = new BrentSingleRootFinder();
  /**
   * Brackets a root.
   */
  private static final BracketRoot ROOT_BRACKETER = new BracketRoot();

  /**
   * Pricer for {@link Payment}.
   */
  private final DiscountingPaymentPricer nominalPricer;
  /**
   * Pricer for {@link FixedCouponBondPaymentPeriod}.
   */
  private final DiscountingFixedCouponBondPaymentPeriodPricer periodPricer;

  /**
   * Creates an instance.
   * 
   * @param periodPricer  the pricer for {@link FixedCouponBondPaymentPeriod}
   * @param nominalPricer  the pricer for {@link Payment}
   */
  public DiscountingFixedCouponBondProductPricer(
      DiscountingFixedCouponBondPaymentPeriodPricer periodPricer,
      DiscountingPaymentPricer nominalPricer) {

    this.nominalPricer = ArgChecker.notNull(nominalPricer, "nominalPricer");
    this.periodPricer = ArgChecker.notNull(periodPricer, "periodPricer");
  }

  //-------------------------------------------------------------------------
  /**
   * Calculates the present value of the fixed coupon bond product.
   * <p>
   * The present value of the product is the value on the valuation date.
   * The result is expressed using the payment currency of the bond.
   * <p>
   * Coupon payments of the product are considered based on the valuation date. 
   * 
   * @param bond  the product
   * @param provider  the rates provider
   * @return the present value of the fixed coupon bond product
   */
  public CurrencyAmount presentValue(ResolvedFixedCouponBond bond, LegalEntityDiscountingProvider provider) {
    return presentValue(bond, provider, provider.getValuationDate());
  }

  // calculate the present value
  CurrencyAmount presentValue(
      ResolvedFixedCouponBond bond,
      LegalEntityDiscountingProvider provider,
      LocalDate referenceDate) {

    IssuerCurveDiscountFactors discountFactors = provider.issuerCurveDiscountFactors(
        bond.getLegalEntityId(), bond.getCurrency());
    CurrencyAmount pvNominal =
        nominalPricer.presentValue(bond.getNominalPayment(), discountFactors.getDiscountFactors());
    CurrencyAmount pvCoupon =
        presentValueCoupon(bond, discountFactors, referenceDate, bond.getExCouponPeriod().getDays() != 0);
    return pvNominal.plus(pvCoupon);
  }

  /**
   * Calculates the present value of the fixed coupon bond product with z-spread. 
   * <p>
   * The present value of the product is the value on the valuation date.
   * The result is expressed using the payment currency of the bond.
   * <p>
   * The z-spread is a parallel shift applied to continuously compounded rates or
   * periodic compounded rates of the issuer discounting curve. 
   * 
   * @param bond  the product
   * @param provider  the rates provider
   * @param zSpread  the z-spread
   * @param compoundedRateType  the compounded rate type
   * @param periodsPerYear  the number of periods per year
   * @return the present value of the fixed coupon bond product
   */
  public CurrencyAmount presentValueWithZSpread(
      ResolvedFixedCouponBond bond,
      LegalEntityDiscountingProvider provider,
      double zSpread,
      CompoundedRateType compoundedRateType,
      int periodsPerYear) {

    return presentValueWithZSpread(bond, provider, zSpread, compoundedRateType, periodsPerYear, provider.getValuationDate());
  }

  // calculate the present value
  CurrencyAmount presentValueWithZSpread(
      ResolvedFixedCouponBond bond,
      LegalEntityDiscountingProvider provider,
      double zSpread,
      CompoundedRateType compoundedRateType,
      int periodsPerYear,
      LocalDate referenceDate) {

    IssuerCurveDiscountFactors discountFactors = provider.issuerCurveDiscountFactors(
        bond.getLegalEntityId(), bond.getCurrency());
    CurrencyAmount pvNominal = nominalPricer.presentValue(
        bond.getNominalPayment(), discountFactors.getDiscountFactors(), zSpread, compoundedRateType, periodsPerYear);
    boolean isExCoupon = bond.getExCouponPeriod().getDays() != 0;
    CurrencyAmount pvCoupon = presentValueCouponFromZSpread(
        bond, discountFactors, zSpread, compoundedRateType, periodsPerYear, referenceDate, isExCoupon);
    return pvNominal.plus(pvCoupon);
  }

  //-------------------------------------------------------------------------
  /**
   * Calculates the dirty price of the fixed coupon bond.
   * <p>
   * The fixed coupon bond is represented as {@link Security} where standard ID of the bond is stored.
   * 
   * @param bond  the product
   * @param securityId  the security identifier of the bond
   * @param provider  the rates provider
   * @return the dirty price of the fixed coupon bond security
   */
  public double dirtyPriceFromCurves(
      ResolvedFixedCouponBond bond,
      StandardId securityId,
      LegalEntityDiscountingProvider provider) {

    LocalDate settlementDate = bond.getSettlementDateOffset().adjust(provider.getValuationDate());
    return dirtyPriceFromCurves(bond, securityId, provider, settlementDate);
  }

  /**
   * Calculates the dirty price of the fixed coupon bond under the specified settlement date.
   * <p>
   * The fixed coupon bond is represented as {@link Security} where standard ID of the bond is stored.
   * 
   * @param bond  the product
   * @param securityId  the security identifier of the bond
   * @param provider  the rates provider
   * @param settlementDate  the settlement date
   * @return the dirty price of the fixed coupon bond security
   */
  public double dirtyPriceFromCurves(
      ResolvedFixedCouponBond bond,
      StandardId securityId,
      LegalEntityDiscountingProvider provider,
      LocalDate settlementDate) {

    CurrencyAmount pv = presentValue(bond, provider, settlementDate);
    StandardId legalEntityId = bond.getLegalEntityId();
    double df = provider.repoCurveDiscountFactors(
        securityId, legalEntityId, bond.getCurrency()).discountFactor(settlementDate);
    double notional = bond.getNotional();
    return pv.getAmount() / df / notional;
  }

  /**
   * Calculates the dirty price of the fixed coupon bond with z-spread.
   * <p>
   * The z-spread is a parallel shift applied to continuously compounded rates or periodic
   * compounded rates of the discounting curve.
   * <p>
   * The fixed coupon bond is represented as {@link Security} where standard ID of the bond is stored.
   * 
   * @param bond  the product
   * @param securityId  the security identifier of the bond
   * @param provider  the rates provider
   * @param zSpread  the z-spread
   * @param compoundedRateType  the compounded rate type
   * @param periodsPerYear  the number of periods per year
   * @return the dirty price of the fixed coupon bond security
   */
  public double dirtyPriceFromCurvesWithZSpread(
      ResolvedFixedCouponBond bond,
      StandardId securityId,
      LegalEntityDiscountingProvider provider,
      double zSpread,
      CompoundedRateType compoundedRateType,
      int periodsPerYear) {

    LocalDate settlementDate = bond.getSettlementDateOffset().adjust(provider.getValuationDate());
    return dirtyPriceFromCurvesWithZSpread(bond, securityId, provider, zSpread, compoundedRateType, periodsPerYear, settlementDate);
  }

  /**
   * Calculates the dirty price of the fixed coupon bond under the specified settlement date with z-spread.
   * <p>
   * The z-spread is a parallel shift applied to continuously compounded rates or periodic
   * compounded rates of the discounting curve.
   * <p>
   * The fixed coupon bond is represented as {@link Security} where standard ID of the bond is stored.
   * 
   * @param bond  the product
   * @param securityId  the security identifier of the bond
   * @param provider  the rates provider
   * @param zSpread  the z-spread
   * @param compoundedRateType  the compounded rate type
   * @param periodsPerYear  the number of periods per year
   * @param settlementDate  the settlement date
   * @return the dirty price of the fixed coupon bond security
   */
  public double dirtyPriceFromCurvesWithZSpread(
      ResolvedFixedCouponBond bond,
      StandardId securityId,
      LegalEntityDiscountingProvider provider,
      double zSpread,
      CompoundedRateType compoundedRateType,
      int periodsPerYear,
      LocalDate settlementDate) {

    CurrencyAmount pv = presentValueWithZSpread(bond, provider, zSpread, compoundedRateType, periodsPerYear, settlementDate);
    StandardId legalEntityId = bond.getLegalEntityId();
    double df = provider.repoCurveDiscountFactors(
        securityId, legalEntityId, bond.getCurrency()).discountFactor(settlementDate);
    double notional = bond.getNotional();
    return pv.getAmount() / df / notional;
  }

  //-------------------------------------------------------------------------
  /**
   * Calculates the dirty price of the fixed coupon bond from its settlement date and clean price.
   * 
   * @param bond  the product
   * @param settlementDate  the settlement date
   * @param cleanPrice  the clean price
   * @return the present value of the fixed coupon bond product
   */
  public double dirtyPriceFromCleanPrice(ResolvedFixedCouponBond bond, LocalDate settlementDate, double cleanPrice) {
    double notional = bond.getNotional();
    double accruedInterest = accruedInterest(bond, settlementDate);
    return cleanPrice + accruedInterest / notional;
  }

  /**
   * Calculates the clean price of the fixed coupon bond from its settlement date and dirty price.
   * 
   * @param bond  the product
   * @param settlementDate  the settlement date
   * @param dirtyPrice  the dirty price
   * @return the present value of the fixed coupon bond product
   */
  public double cleanPriceFromDirtyPrice(ResolvedFixedCouponBond bond, LocalDate settlementDate, double dirtyPrice) {
    double notional = bond.getNotional();
    double accruedInterest = accruedInterest(bond, settlementDate);
    return dirtyPrice - accruedInterest / notional;
  }

  //-------------------------------------------------------------------------
  /**
   * Calculates the z-spread of the fixed coupon bond from curves and dirty price.
   * <p>
   * The z-spread is a parallel shift applied to continuously compounded rates or periodic
   * compounded rates of the discounting curve associated to the bond (Issuer Entity)
   * to match the dirty price.
   * 
   * @param bond  the product
   * @param securityId  the security identifier of the bond
   * @param provider  the rates provider
   * @param dirtyPrice  the dirtyPrice
   * @param compoundedRateType  the compounded rate type
   * @param periodsPerYear  the number of periods per year
   * @return the z-spread of the fixed coupon bond security
   */
  public double zSpreadFromCurvesAndDirtyPrice(
      ResolvedFixedCouponBond bond,
      StandardId securityId,
      LegalEntityDiscountingProvider provider,
      double dirtyPrice,
      CompoundedRateType compoundedRateType,
      int periodsPerYear) {

    final Function<Double, Double> residual = new Function<Double, Double>() {
      @Override
      public Double apply(final Double z) {
        return dirtyPriceFromCurvesWithZSpread(bond, securityId, provider, z, compoundedRateType, periodsPerYear) - dirtyPrice;
      }
    };
    double[] range = ROOT_BRACKETER.getBracketedPoints(residual, -0.01, 0.01); // Starting range is [-1%, 1%]
    return ROOT_FINDER.getRoot(residual, range[0], range[1]);
  }

  //-------------------------------------------------------------------------
  /**
   * Calculates the present value sensitivity of the fixed coupon bond product.
   * <p>
   * The present value sensitivity of the product is the sensitivity of the present value to
   * the underlying curves.
   * 
   * @param bond  the product
   * @param provider  the rates provider
   * @return the present value curve sensitivity of the product
   */
  public PointSensitivityBuilder presentValueSensitivity(
      ResolvedFixedCouponBond bond,
      LegalEntityDiscountingProvider provider) {

    return presentValueSensitivity(bond, provider, provider.getValuationDate());
  }

  // calculate the present value sensitivity
  PointSensitivityBuilder presentValueSensitivity(
      ResolvedFixedCouponBond bond,
      LegalEntityDiscountingProvider provider,
      LocalDate referenceDate) {

    IssuerCurveDiscountFactors discountFactors = provider.issuerCurveDiscountFactors(
        bond.getLegalEntityId(), bond.getCurrency());
    PointSensitivityBuilder pvNominal = presentValueSensitivityNominal(bond, discountFactors);
    PointSensitivityBuilder pvCoupon = presentValueSensitivityCoupon(
        bond, discountFactors, referenceDate, bond.getExCouponPeriod().getDays() != 0);
    return pvNominal.combinedWith(pvCoupon);
  }

  /**
   * Calculates the present value sensitivity of the fixed coupon bond with z-spread.
   * <p>
   * The present value sensitivity of the product is the sensitivity of the present value to
   * the underlying curves.
   * <p>
   * The z-spread is a parallel shift applied to continuously compounded rates or
   * periodic compounded rates of the issuer discounting curve. 
   * 
   * @param bond  the product
   * @param provider  the rates provider
   * @param zSpread  the z-spread
   * @param compoundedRateType  the compounded rate type
   * @param periodsPerYear  the number of periods per year
   * @return the present value curve sensitivity of the product
   */
  public PointSensitivityBuilder presentValueSensitivityWithZSpread(
      ResolvedFixedCouponBond bond,
      LegalEntityDiscountingProvider provider,
      double zSpread,
      CompoundedRateType compoundedRateType,
      int periodsPerYear) {

    return presentValueSensitivityWithZSpread(
        bond, provider, zSpread, compoundedRateType, periodsPerYear, provider.getValuationDate());
  }

  // calculate the present value sensitivity
  PointSensitivityBuilder presentValueSensitivityWithZSpread(
      ResolvedFixedCouponBond bond,
      LegalEntityDiscountingProvider provider,
      double zSpread,
      CompoundedRateType compoundedRateType,
      int periodsPerYear,
      LocalDate referenceDate) {

    IssuerCurveDiscountFactors discountFactors = provider.issuerCurveDiscountFactors(
        bond.getLegalEntityId(), bond.getCurrency());
    PointSensitivityBuilder pvNominal = presentValueSensitivityNominalFromZSpread(
        bond, discountFactors, zSpread, compoundedRateType, periodsPerYear);
    boolean isExCoupon = bond.getExCouponPeriod().getDays() != 0;
    PointSensitivityBuilder pvCoupon = presentValueSensitivityCouponFromZSpread(
        bond, discountFactors, zSpread, compoundedRateType, periodsPerYear, referenceDate, isExCoupon);
    return pvNominal.combinedWith(pvCoupon);
  }

  //-------------------------------------------------------------------------
  /**
   * Calculates the dirty price sensitivity of the fixed coupon bond product.
   * <p>
   * The dirty price sensitivity of the security is the sensitivity of the present value to
   * the underlying curves.
   * 
   * @param bond  the product
   * @param securityId  the security identifier of the bond
   * @param provider  the rates provider
   * @return the dirty price value curve sensitivity of the security
   */
  public PointSensitivityBuilder dirtyPriceSensitivity(
      ResolvedFixedCouponBond bond,
      StandardId securityId,
      LegalEntityDiscountingProvider provider) {

    LocalDate settlementDate = bond.getSettlementDateOffset().adjust(provider.getValuationDate());
    return dirtyPriceSensitivity(bond, securityId, provider, settlementDate);
  }

  // calculate the dirty price sensitivity
  PointSensitivityBuilder dirtyPriceSensitivity(
      ResolvedFixedCouponBond bond,
      StandardId securityId,
      LegalEntityDiscountingProvider provider,
      LocalDate referenceDate) {

    StandardId legalEntityId = bond.getLegalEntityId();
    RepoCurveDiscountFactors discountFactors =
        provider.repoCurveDiscountFactors(securityId, legalEntityId, bond.getCurrency());
    double df = discountFactors.discountFactor(referenceDate);
    CurrencyAmount pv = presentValue(bond, provider);
    double notional = bond.getNotional();
    PointSensitivityBuilder pvSensi = presentValueSensitivity(bond, provider).multipliedBy(1d / df / notional);
    RepoCurveZeroRateSensitivity dfSensi = discountFactors.zeroRatePointSensitivity(referenceDate)
        .multipliedBy(-pv.getAmount() / df / df / notional);
    return pvSensi.combinedWith(dfSensi);
  }

  /**
   * Calculates the dirty price sensitivity of the fixed coupon bond with z-spread.
   * <p>
   * The dirty price sensitivity of the security is the sensitivity of the present value to
   * the underlying curves.
   * <p>
   * The z-spread is a parallel shift applied to continuously compounded rates or periodic
   * compounded rates of the discounting curve.
   * 
   * @param bond  the product
   * @param securityId  the security identifier of the bond
   * @param provider  the rates provider
   * @param zSpread  the z-spread
   * @param compoundedRateType  the compounded rate type
   * @param periodsPerYear  the number of periods per year
   * @return the dirty price curve sensitivity of the security
   */
  public PointSensitivityBuilder dirtyPriceSensitivityWithZspread(
      ResolvedFixedCouponBond bond,
      StandardId securityId,
      LegalEntityDiscountingProvider provider,
      double zSpread,
      CompoundedRateType compoundedRateType,
      int periodsPerYear) {

    LocalDate settlementDate = bond.getSettlementDateOffset().adjust(provider.getValuationDate());
    return dirtyPriceSensitivityWithZspread(bond, securityId, provider, zSpread, compoundedRateType, periodsPerYear, settlementDate);
  }

  // calculate the dirty price sensitivity
  PointSensitivityBuilder dirtyPriceSensitivityWithZspread(
      ResolvedFixedCouponBond bond,
      StandardId securityId,
      LegalEntityDiscountingProvider provider,
      double zSpread,
      CompoundedRateType compoundedRateType,
      int periodsPerYear,
      LocalDate referenceDate) {

    StandardId legalEntityId = bond.getLegalEntityId();
    RepoCurveDiscountFactors discountFactors =
        provider.repoCurveDiscountFactors(securityId, legalEntityId, bond.getCurrency());
    double df = discountFactors.discountFactor(referenceDate);
    CurrencyAmount pv = presentValueWithZSpread(bond, provider, zSpread, compoundedRateType, periodsPerYear);
    double notional = bond.getNotional();
    PointSensitivityBuilder pvSensi = presentValueSensitivityWithZSpread(
        bond, provider, zSpread, compoundedRateType, periodsPerYear).multipliedBy(1d / df / notional);
    RepoCurveZeroRateSensitivity dfSensi = discountFactors.zeroRatePointSensitivity(referenceDate)
        .multipliedBy(-pv.getAmount() / df / df / notional);
    return pvSensi.combinedWith(dfSensi);
  }

  //-------------------------------------------------------------------------
  /**
   * Calculates the accrued interest of the fixed coupon bond with the specified settlement date.
   * 
   * @param bond  the product
   * @param settlementDate  the settlement date
   * @return the accrued interest of the product 
   */
  public double accruedInterest(ResolvedFixedCouponBond bond, LocalDate settlementDate) {
    Schedule scheduleAdjusted = bond.getPeriodicSchedule().createSchedule();
    Schedule scheduleUnadjusted = scheduleAdjusted.toUnadjusted();
    if (scheduleUnadjusted.getPeriods().get(0).getStartDate().isAfter(settlementDate)) {
      return 0d;
    }
    double notional = bond.getNotional();
    int couponIndex = couponIndex(scheduleUnadjusted, settlementDate);
    SchedulePeriod schedulePeriod = scheduleUnadjusted.getPeriod(couponIndex);
    LocalDate previousAccrualDate = schedulePeriod.getStartDate();
    LocalDate paymentDate = scheduleAdjusted.getPeriod(couponIndex).getEndDate();
    double fixedRate = bond.getFixedRate();
    double accruedInterest = bond.getDayCount()
        .yearFraction(previousAccrualDate, settlementDate, scheduleUnadjusted) * fixedRate * notional;
    DaysAdjustment exCouponDays = bond.getExCouponPeriod();
    double result = 0d;
    if (exCouponDays.getDays() != 0 && settlementDate.isAfter(exCouponDays.adjust(paymentDate))) {
      result = accruedInterest - notional * fixedRate *
          schedulePeriod.yearFraction(bond.getDayCount(), scheduleUnadjusted);
    } else {
      result = accruedInterest;
    }
    return result;
  }

  //-------------------------------------------------------------------------
  /**
   * Calculates the dirty price of the fixed coupon bond from yield.
   * <p>
   * The yield must be fractional.
   * The dirty price is computed for {@link YieldConvention}, and the result is expressed in fraction. 
   * 
   * @param bond  the product
   * @param settlementDate  the settlement date
   * @param yield  the yield
   * @return the dirty price of the product 
   */
  public double dirtyPriceFromYield(ResolvedFixedCouponBond bond, LocalDate settlementDate, double yield) {
    ImmutableList<FixedCouponBondPaymentPeriod> payments = bond.getPeriodicPayments();
    int nCoupon = payments.size() - couponIndex(payments, settlementDate);
    YieldConvention yieldConvention = bond.getYieldConvention();
    if (nCoupon == 1) {
      if (yieldConvention.equals(YieldConvention.US_STREET) || yieldConvention.equals(YieldConvention.GERMAN_BONDS)) {
        FixedCouponBondPaymentPeriod payment = payments.get(payments.size() - 1);
        return (1d + payment.getFixedRate() * payment.getYearFraction()) /
            (1d + factorToNextCoupon(bond, settlementDate) * yield /
                ((double) bond.getPeriodicSchedule().getFrequency().eventsPerYear()));
      }
    }
    if ((yieldConvention.equals(YieldConvention.US_STREET)) ||
        (yieldConvention.equals(YieldConvention.UK_BUMP_DMO)) ||
        (yieldConvention.equals(YieldConvention.GERMAN_BONDS))) {
      return dirtyPriceFromYieldStandard(bond, settlementDate, yield);
    }
    if (yieldConvention.equals(YieldConvention.JAPAN_SIMPLE)) {
      LocalDate maturityDate = bond.getPeriodicSchedule().getEndDate();
      if (settlementDate.isAfter(maturityDate)) {
        return 0d;
      }
      double maturity = bond.getDayCount().relativeYearFraction(settlementDate, maturityDate);
      double cleanPrice = (1d + bond.getFixedRate() * maturity) / (1d + yield * maturity);
      return dirtyPriceFromCleanPrice(bond, settlementDate, cleanPrice);
    }
    throw new UnsupportedOperationException("The convention " + yieldConvention.name() + " is not supported.");
  }

  private double dirtyPriceFromYieldStandard(
      ResolvedFixedCouponBond bond,
      LocalDate settlementDate,
      double yield) {

    int nbCoupon = bond.getPeriodicPayments().size();
    double factorOnPeriod = 1 + yield / ((double) bond.getPeriodicSchedule().getFrequency().eventsPerYear());
    double fixedRate = bond.getFixedRate();
    double pvAtFirstCoupon = 0;
    int pow = 0;
    for (int loopcpn = 0; loopcpn < nbCoupon; loopcpn++) {
      FixedCouponBondPaymentPeriod payment = bond.getPeriodicPayments().get(loopcpn);
      if ((bond.getExCouponPeriod().getDays() != 0 && !settlementDate.isAfter(payment.getDetachmentDate())) ||
          (bond.getExCouponPeriod().getDays() == 0 && payment.getPaymentDate().isAfter(settlementDate))) {
        pvAtFirstCoupon += fixedRate * payment.getYearFraction() / Math.pow(factorOnPeriod, pow);
        ++pow;
      }
    }
    pvAtFirstCoupon += 1d / Math.pow(factorOnPeriod, pow - 1);
    return pvAtFirstCoupon * Math.pow(factorOnPeriod, -factorToNextCoupon(bond, settlementDate));
  }

  /**
   * Calculates the yield of the fixed coupon bond product from dirty price.
   * <p>
   * The dirty price must be fractional. 
   * If the analytic formula is not available, the yield is computed by solving
   * a root-finding problem with {@link #dirtyPriceFromYield(ResolvedFixedCouponBond, LocalDate, double)}.  
   * The result is also expressed in fraction. 
   * 
   * @param bond  the product
   * @param settlementDate  the settlement date
   * @param dirtyPrice  the dirty price
   * @return the yield of the product 
   */
  public double yieldFromDirtyPrice(ResolvedFixedCouponBond bond, LocalDate settlementDate, double dirtyPrice) {
    if (bond.getYieldConvention().equals(YieldConvention.JAPAN_SIMPLE)) {
      double cleanPrice = cleanPriceFromDirtyPrice(bond, settlementDate, dirtyPrice);
      LocalDate maturityDate = bond.getPeriodicSchedule().getEndDate();
      double maturity = bond.getDayCount().relativeYearFraction(settlementDate, maturityDate);
      return (bond.getFixedRate() + (1d - cleanPrice) / maturity) / cleanPrice;
    }

    final Function<Double, Double> priceResidual = new Function<Double, Double>() {
      @Override
      public Double apply(final Double y) {
        return dirtyPriceFromYield(bond, settlementDate, y) - dirtyPrice;
      }
    };
    double[] range = ROOT_BRACKETER.getBracketedPoints(priceResidual, 0.00, 0.20);
    double yield = ROOT_FINDER.getRoot(priceResidual, range[0], range[1]);
    return yield;
  }

  //-------------------------------------------------------------------------
  /**
   * Calculates the modified duration of the fixed coupon bond product from yield.
   * <p>
   * The modified duration is defined as the minus of the first derivative of dirty price
   * with respect to yield, divided by the dirty price. 
   * <p>
   * The input yield must be fractional. The dirty price and its derivative are
   * computed for {@link YieldConvention}, and the result is expressed in fraction. 
   * 
   * @param bond  the product
   * @param settlementDate  the settlement date
   * @param yield  the yield
   * @return the modified duration of the product 
   */
  public double modifiedDurationFromYield(ResolvedFixedCouponBond bond, LocalDate settlementDate, double yield) {
    ImmutableList<FixedCouponBondPaymentPeriod> payments = bond.getPeriodicPayments();
    int nCoupon = payments.size() - couponIndex(payments, settlementDate);
    YieldConvention yieldConvention = bond.getYieldConvention();
    if (nCoupon == 1) {
      if (yieldConvention.equals(YieldConvention.US_STREET) || yieldConvention.equals(YieldConvention.GERMAN_BONDS)) {
        double couponPerYear = bond.getPeriodicSchedule().getFrequency().eventsPerYear();
        double factor = factorToNextCoupon(bond, settlementDate);
        return factor / couponPerYear / (1d + factor * yield / couponPerYear);
      }
    }
    if (yieldConvention.equals(YieldConvention.US_STREET) ||
        yieldConvention.equals(YieldConvention.UK_BUMP_DMO) ||
        yieldConvention.equals(YieldConvention.GERMAN_BONDS)) {
      return modifiedDurationFromYieldStandard(bond, settlementDate, yield);
    }
    if (yieldConvention.equals(YieldConvention.JAPAN_SIMPLE)) {
      LocalDate maturityDate = bond.getPeriodicSchedule().getEndDate();
      if (settlementDate.isAfter(maturityDate)) {
        return 0d;
      }
      double maturity = bond.getDayCount().relativeYearFraction(settlementDate, maturityDate);
      double num = 1d + bond.getFixedRate() * maturity;
      double den = 1d + yield * maturity;
      double dirtyPrice = dirtyPriceFromCleanPrice(bond, settlementDate, num / den);
      return num * maturity / den / den / dirtyPrice;
    }
    throw new UnsupportedOperationException("The convention " + yieldConvention.name() + " is not supported.");
  }

  private double modifiedDurationFromYieldStandard(
      ResolvedFixedCouponBond bond,
      LocalDate settlementDate,
      double yield) {

    int nbCoupon = bond.getPeriodicPayments().size();
    double couponPerYear = bond.getPeriodicSchedule().getFrequency().eventsPerYear();
    double factorToNextCoupon = factorToNextCoupon(bond, settlementDate);
    double factorOnPeriod = 1 + yield / couponPerYear;
    double nominal = bond.getNotional();
    double fixedRate = bond.getFixedRate();
    double mdAtFirstCoupon = 0d;
    double pvAtFirstCoupon = 0d;
    int pow = 0;
    for (int loopcpn = 0; loopcpn < nbCoupon; loopcpn++) {
      FixedCouponBondPaymentPeriod payment = bond.getPeriodicPayments().get(loopcpn);
      if ((bond.getExCouponPeriod().getDays() != 0 && !settlementDate.isAfter(payment.getDetachmentDate())) ||
          (bond.getExCouponPeriod().getDays() == 0 && payment.getPaymentDate().isAfter(settlementDate))) {
        mdAtFirstCoupon += payment.getYearFraction() / Math.pow(factorOnPeriod, pow + 1) *
            (pow + factorToNextCoupon) / couponPerYear;
        pvAtFirstCoupon += payment.getYearFraction() / Math.pow(factorOnPeriod, pow);
        ++pow;
      }
    }
    mdAtFirstCoupon *= fixedRate * nominal;
    pvAtFirstCoupon *= fixedRate * nominal;
    mdAtFirstCoupon += nominal / Math.pow(factorOnPeriod, pow) * (pow - 1 + factorToNextCoupon) /
        couponPerYear;
    pvAtFirstCoupon += nominal / Math.pow(factorOnPeriod, pow - 1);
    double md = mdAtFirstCoupon / pvAtFirstCoupon;
    return md;
  }

  /**
   * Calculates the Macaulay duration of the fixed coupon bond product from yield.
   * <p>
   * Macaulay defined an alternative way of weighting the future cash flows. 
   * <p>
   * The input yield must be fractional. The dirty price and its derivative are
   * computed for {@link YieldConvention}, and the result is expressed in fraction. 
   * 
   * @param bond  the product
   * @param settlementDate  the settlement date
   * @param yield  the yield
   * @return the modified duration of the product 
   */
  public double macaulayDurationFromYield(ResolvedFixedCouponBond bond, LocalDate settlementDate, double yield) {
    ImmutableList<FixedCouponBondPaymentPeriod> payments = bond.getPeriodicPayments();
    int nCoupon = payments.size() - couponIndex(payments, settlementDate);
    YieldConvention yieldConvention = bond.getYieldConvention();
    if ((yieldConvention.equals(YieldConvention.US_STREET)) && (nCoupon == 1)) {
      return factorToNextCoupon(bond, settlementDate) /
          bond.getPeriodicSchedule().getFrequency().eventsPerYear();
    }
    if ((yieldConvention.equals(YieldConvention.US_STREET)) ||
        (yieldConvention.equals(YieldConvention.UK_BUMP_DMO)) ||
        (yieldConvention.equals(YieldConvention.GERMAN_BONDS))) {
      return modifiedDurationFromYield(bond, settlementDate, yield) *
          (1d + yield / bond.getPeriodicSchedule().getFrequency().eventsPerYear());
    }
    throw new UnsupportedOperationException("The convention " + yieldConvention.name() + " is not supported.");
  }

  /**
   * Calculates the convexity of the fixed coupon bond product from yield.
   * <p>
   * The convexity is defined as the second derivative of dirty price with respect
   * to yield, divided by the dirty price. 
   * <p>
   * The input yield must be fractional. The dirty price and its derivative are
   * computed for {@link YieldConvention}, and the result is expressed in fraction. 
   * 
   * @param bond  the product
   * @param settlementDate  the settlement date
   * @param yield  the yield
   * @return the convexity of the product 
   */
  public double convexityFromYield(ResolvedFixedCouponBond bond, LocalDate settlementDate, double yield) {
    ImmutableList<FixedCouponBondPaymentPeriod> payments = bond.getPeriodicPayments();
    int nCoupon = payments.size() - couponIndex(payments, settlementDate);
    YieldConvention yieldConvention = bond.getYieldConvention();
    if (nCoupon == 1) {
      if (yieldConvention.equals(YieldConvention.US_STREET) || yieldConvention.equals(YieldConvention.GERMAN_BONDS)) {
        double couponPerYear = bond.getPeriodicSchedule().getFrequency().eventsPerYear();
        double factorToNextCoupon = factorToNextCoupon(bond, settlementDate);
        double timeToPay = factorToNextCoupon / couponPerYear;
        double disc = (1d + factorToNextCoupon * yield / couponPerYear);
        return 2d * timeToPay * timeToPay / (disc * disc);
      }
    }
    if (yieldConvention.equals(YieldConvention.US_STREET) || yieldConvention.equals(YieldConvention.UK_BUMP_DMO) ||
        yieldConvention.equals(YieldConvention.GERMAN_BONDS)) {
      return convexityFromYieldStandard(bond, settlementDate, yield);
    }
    if (yieldConvention.equals(YieldConvention.JAPAN_SIMPLE)) {
      LocalDate maturityDate = bond.getPeriodicSchedule().getEndDate();
      if (settlementDate.isAfter(maturityDate)) {
        return 0d;
      }
      double maturity = bond.getDayCount().relativeYearFraction(settlementDate, maturityDate);
      double num = 1d + bond.getFixedRate() * maturity;
      double den = 1d + yield * maturity;
      double dirtyPrice = dirtyPriceFromCleanPrice(bond, settlementDate, num / den);
      return 2d * num * Math.pow(maturity, 2) * Math.pow(den, -3) / dirtyPrice;
    }
    throw new UnsupportedOperationException("The convention " + yieldConvention.name() + " is not supported.");
  }

  // assumes notional and coupon rate are constant across the payments. 
  private double convexityFromYieldStandard(
      ResolvedFixedCouponBond bond,
      LocalDate settlementDate,
      double yield) {

    int nbCoupon = bond.getPeriodicPayments().size();
    double couponPerYear = bond.getPeriodicSchedule().getFrequency().eventsPerYear();
    double factorToNextCoupon = factorToNextCoupon(bond, settlementDate);
    double factorOnPeriod = 1 + yield / couponPerYear;
    double nominal = bond.getNotional();
    double fixedRate = bond.getFixedRate();
    double cvAtFirstCoupon = 0;
    double pvAtFirstCoupon = 0;
    int pow = 0;
    for (int loopcpn = 0; loopcpn < nbCoupon; loopcpn++) {
      FixedCouponBondPaymentPeriod payment = bond.getPeriodicPayments().get(loopcpn);
      if ((bond.getExCouponPeriod().getDays() != 0 && !settlementDate.isAfter(payment.getDetachmentDate())) ||
          (bond.getExCouponPeriod().getDays() == 0 && payment.getPaymentDate().isAfter(settlementDate))) {
        cvAtFirstCoupon += payment.getYearFraction() / Math.pow(factorOnPeriod, pow + 2) *
            (pow + factorToNextCoupon) * (pow + factorToNextCoupon + 1);
        pvAtFirstCoupon += payment.getYearFraction() / Math.pow(factorOnPeriod, pow);
        ++pow;
      }
    }
    cvAtFirstCoupon *= fixedRate * nominal / (couponPerYear * couponPerYear);
    pvAtFirstCoupon *= fixedRate * nominal;
    cvAtFirstCoupon += nominal / Math.pow(factorOnPeriod, pow + 1) * (pow - 1 + factorToNextCoupon) *
        (pow + factorToNextCoupon) / (couponPerYear * couponPerYear);
    pvAtFirstCoupon += nominal / Math.pow(factorOnPeriod, pow - 1);
    final double pv = pvAtFirstCoupon * Math.pow(factorOnPeriod, -factorToNextCoupon);
    final double cv = cvAtFirstCoupon * Math.pow(factorOnPeriod, -factorToNextCoupon) / pv;
    return cv;
  }

  //-------------------------------------------------------------------------
  private double factorToNextCoupon(ResolvedFixedCouponBond bond, LocalDate settlementDate) {
    if (bond.getPeriodicPayments().get(0).getStartDate().isAfter(settlementDate)) {
      return 0d;
    }
    int couponIndex = couponIndex(bond.getPeriodicPayments(), settlementDate);
    double factorSpot = accruedInterest(bond, settlementDate) / bond.getFixedRate() / bond.getNotional();
    double factorPeriod = bond.getPeriodicPayments().get(couponIndex).getYearFraction();
    return (factorPeriod - factorSpot) / factorPeriod;
  }

  private int couponIndex(Schedule schedule, LocalDate date) {
    int nbCoupon = schedule.getPeriods().size();
    int couponIndex = 0;
    for (int loopcpn = 0; loopcpn < nbCoupon; ++loopcpn) {
      if (schedule.getPeriods().get(loopcpn).getEndDate().isAfter(date)) {
        couponIndex = loopcpn;
        break;
      }
    }
    return couponIndex;
  }

  private int couponIndex(ImmutableList<FixedCouponBondPaymentPeriod> list, LocalDate date) {
    int nbCoupon = list.size();
    int couponIndex = 0;
    for (int loopcpn = 0; loopcpn < nbCoupon; ++loopcpn) {
      if (list.get(loopcpn).getEndDate().isAfter(date)) {
        couponIndex = loopcpn;
        break;
      }
    }
    return couponIndex;
  }

  //-------------------------------------------------------------------------
  private CurrencyAmount presentValueCoupon(
      ResolvedFixedCouponBond bond,
      IssuerCurveDiscountFactors discountFactors,
      LocalDate referenceDate,
      boolean exCoupon) {

    double total = 0d;
    for (FixedCouponBondPaymentPeriod period : bond.getPeriodicPayments()) {
      if ((exCoupon && period.getDetachmentDate().isAfter(referenceDate)) ||
          (!exCoupon && period.getPaymentDate().isAfter(referenceDate))) {
        total += periodPricer.presentValue(period, discountFactors);
      }
    }
    return CurrencyAmount.of(bond.getCurrency(), total);
  }

  private CurrencyAmount presentValueCouponFromZSpread(
      ResolvedFixedCouponBond bond,
      IssuerCurveDiscountFactors discountFactors,
      double zSpread,
      CompoundedRateType compoundedRateType,
      int periodsPerYear,
      LocalDate referenceDate,
      boolean exCoupon) {

    double total = 0d;
    for (FixedCouponBondPaymentPeriod period : bond.getPeriodicPayments()) {
      if ((exCoupon && period.getDetachmentDate().isAfter(referenceDate)) ||
          (!exCoupon && period.getPaymentDate().isAfter(referenceDate))) {
        total += periodPricer.presentValueWithSpread(period, discountFactors, zSpread, compoundedRateType, periodsPerYear);
      }
    }
    return CurrencyAmount.of(bond.getCurrency(), total);
  }

  //-------------------------------------------------------------------------
  private PointSensitivityBuilder presentValueSensitivityCoupon(
      ResolvedFixedCouponBond bond,
      IssuerCurveDiscountFactors discountFactors,
      LocalDate referenceDate,
      boolean exCoupon) {

    PointSensitivityBuilder builder = PointSensitivityBuilder.none();
    for (FixedCouponBondPaymentPeriod period : bond.getPeriodicPayments()) {
      if ((exCoupon && period.getDetachmentDate().isAfter(referenceDate)) ||
          (!exCoupon && period.getPaymentDate().isAfter(referenceDate))) {
        builder = builder.combinedWith(periodPricer.presentValueSensitivity(period, discountFactors));
      }
    }
    return builder;
  }

  private PointSensitivityBuilder presentValueSensitivityCouponFromZSpread(
      ResolvedFixedCouponBond bond,
      IssuerCurveDiscountFactors discountFactors,
      double zSpread,
      CompoundedRateType compoundedRateType,
      int periodsPerYear,
      LocalDate referenceDate,
      boolean exCoupon) {

    PointSensitivityBuilder builder = PointSensitivityBuilder.none();
    for (FixedCouponBondPaymentPeriod period : bond.getPeriodicPayments()) {
      if ((exCoupon && period.getDetachmentDate().isAfter(referenceDate)) ||
          (!exCoupon && period.getPaymentDate().isAfter(referenceDate))) {
        builder = builder.combinedWith(periodPricer.presentValueSensitivityWithSpread(
            period, discountFactors, zSpread, compoundedRateType, periodsPerYear));
      }
    }
    return builder;
  }

  private PointSensitivityBuilder presentValueSensitivityNominal(
      ResolvedFixedCouponBond bond,
      IssuerCurveDiscountFactors discountFactors) {

    Payment nominal = bond.getNominalPayment();
    PointSensitivityBuilder pt = nominalPricer.presentValueSensitivity(nominal, discountFactors.getDiscountFactors());
    if (pt instanceof ZeroRateSensitivity) {
      return IssuerCurveZeroRateSensitivity.of((ZeroRateSensitivity) pt, discountFactors.getLegalEntityGroup());
    }
    return pt; // NoPointSensitivity
  }

  private PointSensitivityBuilder presentValueSensitivityNominalFromZSpread(
      ResolvedFixedCouponBond bond,
      IssuerCurveDiscountFactors discountFactors,
      double zSpread,
      CompoundedRateType compoundedRateType,
      int periodsPerYear) {

    Payment nominal = bond.getNominalPayment();
    PointSensitivityBuilder pt = nominalPricer.presentValueSensitivity(
        nominal, discountFactors.getDiscountFactors(), zSpread, compoundedRateType, periodsPerYear);
    if (pt instanceof ZeroRateSensitivity) {
      return IssuerCurveZeroRateSensitivity.of((ZeroRateSensitivity) pt, discountFactors.getLegalEntityGroup());
    }
    return pt; // NoPointSensitivity
  }

  //-------------------------------------------------------------------------
  // compute pv of coupon payment(s) s.t. referenceDate1 < coupon <= referenceDate2
  double presentValueCoupon(
      ResolvedFixedCouponBond bond,
      IssuerCurveDiscountFactors discountFactors,
      LocalDate referenceDate1,
      LocalDate referenceDate2,
      boolean exCoupon) {

    double pvDiff = 0d;
    for (FixedCouponBondPaymentPeriod period : bond.getPeriodicPayments()) {
      if ((exCoupon && period.getDetachmentDate().isAfter(referenceDate1) && !period.getDetachmentDate().isAfter(referenceDate2)) ||
          (!exCoupon && period.getPaymentDate().isAfter(referenceDate1) && !period.getPaymentDate().isAfter(referenceDate2))) {
        pvDiff += periodPricer.presentValue(period, discountFactors);
      }
    }
    return pvDiff;
  }

  // compute pv of coupon payment(s) s.t. referenceDate1 < coupon <= referenceDate2
  double presentValueCouponWithZSpread(
      ResolvedFixedCouponBond expanded,
      IssuerCurveDiscountFactors discountFactors,
      LocalDate referenceDate1,
      LocalDate referenceDate2,
      double zSpread,
      CompoundedRateType compoundedRateType,
      int periodsPerYear,
      boolean exCoupon) {

    double pvDiff = 0d;
    for (FixedCouponBondPaymentPeriod period : expanded.getPeriodicPayments()) {
      if ((exCoupon && period.getDetachmentDate().isAfter(referenceDate1) && !period.getDetachmentDate().isAfter(referenceDate2)) ||
          (!exCoupon && period.getPaymentDate().isAfter(referenceDate1) && !period.getPaymentDate().isAfter(referenceDate2))) {
        pvDiff += periodPricer.presentValueWithSpread(period, discountFactors, zSpread, compoundedRateType, periodsPerYear);
      }
    }
    return pvDiff;
  }

}
