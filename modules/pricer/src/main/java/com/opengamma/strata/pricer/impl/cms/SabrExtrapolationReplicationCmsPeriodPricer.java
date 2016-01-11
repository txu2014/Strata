package com.opengamma.strata.pricer.impl.cms;

import java.time.ZonedDateTime;
import java.util.function.Function;

import com.opengamma.strata.basics.PutCall;
import com.opengamma.strata.basics.currency.Currency;
import com.opengamma.strata.basics.currency.CurrencyAmount;
import com.opengamma.strata.collect.ArgChecker;
import com.opengamma.strata.collect.array.DoubleArray;
import com.opengamma.strata.market.curve.CurveInfoType;
import com.opengamma.strata.market.sensitivity.PointSensitivityBuilder;
import com.opengamma.strata.market.sensitivity.SwaptionSabrSensitivity;
import com.opengamma.strata.market.view.ZeroRateDiscountFactors;
import com.opengamma.strata.math.impl.MathException;
import com.opengamma.strata.math.impl.integration.RungeKuttaIntegrator1D;
import com.opengamma.strata.pricer.impl.option.SabrExtrapolationRightFunction;
import com.opengamma.strata.pricer.impl.volatility.smile.function.SabrFormulaData;
import com.opengamma.strata.pricer.rate.RatesProvider;
import com.opengamma.strata.pricer.swap.DiscountingSwapProductPricer;
import com.opengamma.strata.pricer.swaption.SabrSwaptionVolatilities;
import com.opengamma.strata.product.cms.CmsPeriod;
import com.opengamma.strata.product.cms.CmsPeriodType;
import com.opengamma.strata.product.swap.ExpandedSwap;
import com.opengamma.strata.product.swap.ExpandedSwapLeg;
import com.opengamma.strata.product.swap.RatePaymentPeriod;
import com.opengamma.strata.product.swap.Swap;
import com.opengamma.strata.product.swap.SwapLegType;

/**
 *  Computes the price of a CMS coupon/caplet/floorlet by swaption replication on a SABR formula with extrapolation.
 *  <p>
 *  The replication requires numerical integration. This is completed by {@link RungeKuttaIntegrator1D}.
 *  <p>
 *  Reference: Hagan, P. S. (2003). Convexity conundrums: Pricing CMS swaps, caps, and floors. 
 *  Wilmott Magazine, March, pages 38--44.
 *  OpenGamma implementation note: Replication pricing for linear and TEC format CMS, Version 1.2, March 2011.
 *  OpenGamma implementation note for the extrapolation: Smile extrapolation, version 1.2, May 2011.
 */
public class SabrExtrapolationReplicationCmsPeriodPricer {
  /**
   * The minimal number of iterations for the numerical integration.
   */
  private static final int NUM_ITER = 10;
  /**
   * The relative tolerance for the numerical integration.
   */
  private static final double REL_TOL = 1.0e-10;

  /**
   * Pricer for the underlying swap. 
   */
  private final DiscountingSwapProductPricer swapPricer;
  /**
   * The cut-off strike. 
   * <p>
   * The smile is extrapolated above that level.
   */
  private final double cutOffStrike;
  /**
   * The tail thickness parameter.
   * <p>
   * This must be greater than 0 in order to ensure that the call price converges to 0 for infinite strike.
   */
  private final double mu;
  /**
   * Range of the integral. 
   * <p> 
   * This represents the approximation of infinity in the strike dimension.
   * Thus the integral range is {@code [strike, strike+integrationInterval]}.
   * <p>
   * Used only for caplets and coupons.
   */
  private final double interval;

  //-------------------------------------------------------------------------
  /**
   * Obtains the pricer. 
   * 
   * @param swapPricer  the pricer for underlying swap
   * @param cutOffStrike  the cut-off strike value
   * @param mu  the tail thickness
   * @param integrationInterval  the integration interval
   * @return the pricer
   */
  public static SabrExtrapolationReplicationCmsPeriodPricer of(
      DiscountingSwapProductPricer swapPricer,
      double cutOffStrike,
      double mu,
      double integrationInterval) {

    return new SabrExtrapolationReplicationCmsPeriodPricer(swapPricer, cutOffStrike, mu, integrationInterval);
  }

  /**
   * Obtains the pricer with default swap pricer. 
   * 
   * @param cutOffStrike  the cut-off strike value
   * @param mu  the tail thickness
   * @param integrationInterval  the integration interval
   * @return the pricer
   */
  public static SabrExtrapolationReplicationCmsPeriodPricer of(
      double cutOffStrike,
      double mu,
      double integrationInterval) {

    return new SabrExtrapolationReplicationCmsPeriodPricer(
        DiscountingSwapProductPricer.DEFAULT, cutOffStrike, mu, integrationInterval);
  }

  private SabrExtrapolationReplicationCmsPeriodPricer(
      DiscountingSwapProductPricer swapPricer,
      double cutOffStrike,
      double mu,
      double integrationInterval) {

    this.swapPricer = ArgChecker.notNull(swapPricer, "swapPricer");
    this.cutOffStrike = cutOffStrike;
    this.mu = ArgChecker.notNegativeOrZero(mu, "mu");
    this.interval = integrationInterval;
  }

  //-------------------------------------------------------------------------
  /**
   * Computes the present value by replication in SABR framework with extrapolation on the right.
   * 
   * @param cmsPeriod  the CMS 
   * @param provider  the rates provider
   * @param swaptionVolatilities  the swaption volatilities
   * @return the present value
   */
  public CurrencyAmount presentValue(
      CmsPeriod cmsPeriod,
      RatesProvider provider,
      SabrSwaptionVolatilities swaptionVolatilities) {
    Currency ccy = cmsPeriod.getCurrency();
    Swap swap = cmsPeriod.getUnderlyingSwap();
    ExpandedSwap expandedSwap = swap.expand();
    double dfPayment = provider.discountFactor(ccy, cmsPeriod.getPaymentDate()); // 0.8518053333230845 OK TODO
    ZonedDateTime valuationDate = swaptionVolatilities.getValuationDateTime();
    double expiryTime = swaptionVolatilities.relativeTime(
        cmsPeriod.getFixingDate().atTime(valuationDate.toLocalTime()).atZone(valuationDate.getZone()));
    //    double tenor = swaptionVolatilities.tenor(swap.getStartDate(), swap.getEndDate()); // TODO
    double tenor = swaptionVolatilities.getDayCount().relativeYearFraction(swap.getStartDate(), swap.getEndDate());
    double alpha = swaptionVolatilities.getParameters().alpha(expiryTime, tenor);
    double beta = swaptionVolatilities.getParameters().beta(expiryTime, tenor);
    double rho = swaptionVolatilities.getParameters().rho(expiryTime, tenor);
    double nu = swaptionVolatilities.getParameters().nu(expiryTime, tenor);
    SabrFormulaData sabrPoint = SabrFormulaData.of(alpha, beta, rho, nu);
    // SABRFormulaData [alpha=0.054442381748467536, beta=0.5, rho=-0.13894045628831167, nu=0.41115236503064934] OK TODO
    double shift = swaptionVolatilities.getParameters().shift(expiryTime, tenor);
    double forward = swapPricer.parRate(expandedSwap, provider) + shift; // 0.01510740653964031 OK
    double strike = cmsPeriod.getCmsPeriodType().equals(CmsPeriodType.COUPON) ? 0d : cmsPeriod.getStrike() + shift;
    double shiftedCutOff = cutOffStrike + shift;
//    double eta = cmsPeriod.getDayCount().relativeYearFraction(cmsPeriod.getPaymentDate(), swap.getStartDate()); // -0.99814357362078
    //    double eta = DayCounts.ACT_ACT_ISDA.relativeYearFraction(cmsPeriod.getPaymentDate(), swap.getStartDate()); TODO
    double eta = ((ZeroRateDiscountFactors) provider.discountFactors(ccy)).getCurve().getMetadata()
        .getInfo(CurveInfoType.DAY_COUNT).relativeYearFraction(cmsPeriod.getPaymentDate(), swap.getStartDate());
    CmsIntegrantProvider intProv =
        new CmsIntegrantProvider(cmsPeriod, expandedSwap, sabrPoint, forward, strike, expiryTime, shiftedCutOff, eta);
    double factor = dfPayment / intProv.h(forward) * intProv.g(forward);
    double strikePart = factor * intProv.k(strike) * intProv.bs(strike);
    double absoluteTolerance = 1d / (factor * Math.abs(cmsPeriod.getNotional()) * cmsPeriod.getYearFraction());
    RungeKuttaIntegrator1D integrator = new RungeKuttaIntegrator1D(absoluteTolerance, REL_TOL, NUM_ITER);
    double integralPart = 0d;
    try {
      if (intProv.getPutCall().isCall()) {
        integralPart = dfPayment * integrator.integrate(intProv.integrant(), strike, strike + interval);
      } else {
        integralPart = dfPayment * integrator.integrate(intProv.integrant(), 0d, strike);
      }
    } catch (Exception e) {
      throw new MathException(e);
    } // 0.0024887428964362733 TODO
    double priceCMS = (strikePart + integralPart) * cmsPeriod.getNotional() * cmsPeriod.getYearFraction();
    return CurrencyAmount.of(ccy, priceCMS);
  }

  public PointSensitivityBuilder presentValueSensitivity(
      CmsPeriod cmsPeriod,
      RatesProvider provider,
      SabrSwaptionVolatilities swaptionVolatilities) {
    Currency ccy = cmsPeriod.getCurrency();
    Swap swap = cmsPeriod.getUnderlyingSwap();
    ExpandedSwap expandedSwap = swap.expand();
    double dfPayment = provider.discountFactor(ccy, cmsPeriod.getPaymentDate()); // 0.8518053333230845 OK TODO
    ZonedDateTime valuationDate = swaptionVolatilities.getValuationDateTime();
    double expiryTime = swaptionVolatilities.relativeTime(
        cmsPeriod.getFixingDate().atTime(valuationDate.toLocalTime()).atZone(valuationDate.getZone()));
    //    double tenor = swaptionVolatilities.tenor(swap.getStartDate(), swap.getEndDate()); // TODO
    double tenor = swaptionVolatilities.getDayCount().relativeYearFraction(swap.getStartDate(), swap.getEndDate());
    double alpha = swaptionVolatilities.getParameters().alpha(expiryTime, tenor);
    double beta = swaptionVolatilities.getParameters().beta(expiryTime, tenor);
    double rho = swaptionVolatilities.getParameters().rho(expiryTime, tenor);
    double nu = swaptionVolatilities.getParameters().nu(expiryTime, tenor);
    SabrFormulaData sabrPoint = SabrFormulaData.of(alpha, beta, rho, nu);
    // SABRFormulaData [alpha=0.054442381748467536, beta=0.5, rho=-0.13894045628831167, nu=0.41115236503064934] OK TODO
    double shift = swaptionVolatilities.getParameters().shift(expiryTime, tenor);
    double forward = swapPricer.parRate(expandedSwap, provider) + shift; // 0.01510740653964031 OK
    double strike = cmsPeriod.getCmsPeriodType().equals(CmsPeriodType.COUPON) ? 0d : cmsPeriod.getStrike() + shift;
    double shiftedCutOff = cutOffStrike + shift;
//    double eta = cmsPeriod.getDayCount().relativeYearFraction(cmsPeriod.getPaymentDate(), swap.getStartDate()); // -0.99814357362078
    //    double eta = DayCounts.ACT_ACT_ISDA.relativeYearFraction(cmsPeriod.getPaymentDate(), swap.getStartDate()); TODO
    double eta = ((ZeroRateDiscountFactors) provider.discountFactors(ccy)).getCurve().getMetadata()
        .getInfo(CurveInfoType.DAY_COUNT).relativeYearFraction(cmsPeriod.getPaymentDate(), swap.getStartDate());
    CmsDeltaIntegrantProvider intProv = new CmsDeltaIntegrantProvider(
        cmsPeriod, expandedSwap, sabrPoint, forward, strike, expiryTime, shiftedCutOff, eta);
    double factor = dfPayment / intProv.h(forward) * intProv.g(forward);
    double absoluteTolerance = 1d / (factor * Math.abs(cmsPeriod.getNotional()) * cmsPeriod.getYearFraction());
    RungeKuttaIntegrator1D integrator = new RungeKuttaIntegrator1D(absoluteTolerance, REL_TOL, NUM_ITER);
    // Price
    double[] bs = intProv.bsbsp(strike);
    double[] n = intProv.nnp(forward);
    double strikePartPrice = dfPayment * intProv.k(strike) * n[0] * bs[0];
    double integralPartPrice = 0d;
    double integralPart = 0d;
    try {
      if (intProv.getPutCall().isCall()) {
        integralPartPrice = dfPayment * integrator.integrate(intProv.integrant(), strike, strike + interval);
        integralPart = dfPayment * integrator.integrate(intProv.integrantDelta(), strike, strike + interval); // 0.07827123535034726
      } else {
        integralPartPrice = dfPayment * integrator.integrate(intProv.integrant(), 0d, strike);
        integralPart = dfPayment * integrator.integrate(intProv.integrantDelta(), 0d, strike);
      }
    } catch (Exception e) {
      throw new MathException(e);
    } // 0.0024887428964362733 TODO
    double price = (strikePartPrice + integralPartPrice) * cmsPeriod.getNotional() * cmsPeriod.getYearFraction();
    double strikePart = dfPayment * intProv.k(strike) * (n[1] * bs[0] + n[0] * bs[1]); // 0.2280528162880303 OK
    double deltaFwd = (strikePart + integralPart) * cmsPeriod.getNotional() * cmsPeriod.getYearFraction(); // 3105785.5235557724
    PointSensitivityBuilder sensiFwd = swapPricer.parRateSensitivity(expandedSwap, provider).multipliedBy(deltaFwd);
    double deltaPD = price / dfPayment;
    PointSensitivityBuilder sensiDf = provider.discountFactors(ccy)
        .zeroRatePointSensitivity(cmsPeriod.getPaymentDate())
        .multipliedBy(deltaPD);

    return sensiFwd.combinedWith(sensiDf);
  }

  public SwaptionSabrSensitivity presentValueSensitivitySabr(
      CmsPeriod cmsPeriod,
      RatesProvider provider,
      SabrSwaptionVolatilities swaptionVolatilities) {
    Currency ccy = cmsPeriod.getCurrency();
    Swap swap = cmsPeriod.getUnderlyingSwap();
    ExpandedSwap expandedSwap = swap.expand();
    double dfPayment = provider.discountFactor(ccy, cmsPeriod.getPaymentDate()); // 0.8518053333230845 OK TODO
    ZonedDateTime valuationDate = swaptionVolatilities.getValuationDateTime();
    ZonedDateTime expiryDate =
        cmsPeriod.getFixingDate().atTime(valuationDate.toLocalTime()).atZone(valuationDate.getZone());
    double expiryTime = swaptionVolatilities.relativeTime(expiryDate);
    //    double tenor = swaptionVolatilities.tenor(swap.getStartDate(), swap.getEndDate()); // TODO
    double tenor = swaptionVolatilities.getDayCount().relativeYearFraction(swap.getStartDate(), swap.getEndDate());
    double alpha = swaptionVolatilities.getParameters().alpha(expiryTime, tenor);
    double beta = swaptionVolatilities.getParameters().beta(expiryTime, tenor);
    double rho = swaptionVolatilities.getParameters().rho(expiryTime, tenor);
    double nu = swaptionVolatilities.getParameters().nu(expiryTime, tenor);
    SabrFormulaData sabrPoint = SabrFormulaData.of(alpha, beta, rho, nu);
    // SABRFormulaData [alpha=0.054442381748467536, beta=0.5, rho=-0.13894045628831167, nu=0.41115236503064934] OK TODO
    double shift = swaptionVolatilities.getParameters().shift(expiryTime, tenor);
    double forward = swapPricer.parRate(expandedSwap, provider) + shift; // 0.01510740653964031 OK
    double strike = cmsPeriod.getCmsPeriodType().equals(CmsPeriodType.COUPON) ? 0d : cmsPeriod.getStrike() + shift;
    double shiftedCutOff = cutOffStrike + shift;
    //      double eta = cmsPeriod.getDayCount().relativeYearFraction(cmsPeriod.getPaymentDate(), swap.getStartDate()); // -0.99814357362078
    //    double eta = DayCounts.ACT_ACT_ISDA.relativeYearFraction(cmsPeriod.getPaymentDate(), swap.getStartDate()); TODO
    double eta = ((ZeroRateDiscountFactors) provider.discountFactors(ccy)).getCurve().getMetadata()
        .getInfo(CurveInfoType.DAY_COUNT).relativeYearFraction(cmsPeriod.getPaymentDate(), swap.getStartDate());
    CmsIntegrantProvider intProv = new CmsIntegrantProvider(
        cmsPeriod, expandedSwap, sabrPoint, forward, strike, expiryTime, shiftedCutOff, eta);
    double factor = dfPayment / intProv.h(forward) * intProv.g(forward);
    double factor2 = factor * intProv.k(strike);
    double[] strikePartPrice = intProv.getSabrExtrapolation().priceAdjointSabr(strike, intProv.getPutCall())
        .getDerivatives().multipliedBy(factor2).toArray();
    double absoluteTolerance = 1.0 / (factor * Math.abs(cmsPeriod.getNotional()) * cmsPeriod.getYearFraction());
    double relativeTolerance = 1E-3; // TODO investigate this
    RungeKuttaIntegrator1D integrator = new RungeKuttaIntegrator1D(absoluteTolerance, relativeTolerance, NUM_ITER);
    double[] integralPart = new double[4];
    double[] totalSensi = new double[4];
    for (int loopparameter = 0; loopparameter < 4; loopparameter++) {
      try {
        if (intProv.getPutCall().isCall()) {
          integralPart[loopparameter] =
              dfPayment * integrator.integrate(intProv.integrantVaga(loopparameter), strike, strike + interval);
        } else {
          integralPart[loopparameter] =
              dfPayment * integrator.integrate(intProv.integrantVaga(loopparameter), 0d, strike);
        }
      } catch (Exception e) {
        throw new RuntimeException(e);
      }
      totalSensi[loopparameter] = (strikePartPrice[loopparameter] + integralPart[loopparameter]) *
          cmsPeriod.getNotional() * cmsPeriod.getYearFraction();
    }
    return SwaptionSabrSensitivity.of(cmsPeriod.getIndex().getTemplate().getConvention(),
        expiryDate, tenor, ccy, totalSensi[0], totalSensi[1], totalSensi[2], totalSensi[3]);
  }

  public double presentValueSensitivityStrike(
      CmsPeriod cmsPeriod,
      RatesProvider provider,
      SabrSwaptionVolatilities swaptionVolatilities) {
    ArgChecker.isFalse(cmsPeriod.getCmsPeriodType().equals(CmsPeriodType.COUPON),
        "presentValueSensitivityStrike is not relevant for CMS coupon");
    Currency ccy = cmsPeriod.getCurrency();
    Swap swap = cmsPeriod.getUnderlyingSwap();
    ExpandedSwap expandedSwap = swap.expand();
    double dfPayment = provider.discountFactor(ccy, cmsPeriod.getPaymentDate()); // 0.8518053333230845 OK TODO
    ZonedDateTime valuationDate = swaptionVolatilities.getValuationDateTime();
    double expiryTime = swaptionVolatilities.relativeTime(
        cmsPeriod.getFixingDate().atTime(valuationDate.toLocalTime()).atZone(valuationDate.getZone()));
    //    double tenor = swaptionVolatilities.tenor(swap.getStartDate(), swap.getEndDate()); // TODO
    double tenor = swaptionVolatilities.getDayCount().relativeYearFraction(swap.getStartDate(), swap.getEndDate());
    double alpha = swaptionVolatilities.getParameters().alpha(expiryTime, tenor);
    double beta = swaptionVolatilities.getParameters().beta(expiryTime, tenor);
    double rho = swaptionVolatilities.getParameters().rho(expiryTime, tenor);
    double nu = swaptionVolatilities.getParameters().nu(expiryTime, tenor);
    SabrFormulaData sabrPoint = SabrFormulaData.of(alpha, beta, rho, nu);
    // SABRFormulaData [alpha=0.054442381748467536, beta=0.5, rho=-0.13894045628831167, nu=0.41115236503064934] OK TODO
    double shift = swaptionVolatilities.getParameters().shift(expiryTime, tenor);
    double forward = swapPricer.parRate(expandedSwap, provider) + shift; // 0.01510740653964031 OK
    double strike = cmsPeriod.getStrike() + shift;
    double shiftedCutOff = cutOffStrike + shift;
    //      double eta = cmsPeriod.getDayCount().relativeYearFraction(cmsPeriod.getPaymentDate(), swap.getStartDate()); // -0.99814357362078
    //    double eta = DayCounts.ACT_ACT_ISDA.relativeYearFraction(cmsPeriod.getPaymentDate(), swap.getStartDate()); TODO
    double eta = ((ZeroRateDiscountFactors) provider.discountFactors(ccy)).getCurve().getMetadata()
        .getInfo(CurveInfoType.DAY_COUNT).relativeYearFraction(cmsPeriod.getPaymentDate(), swap.getStartDate());
    CmsIntegrantProvider intProv = new CmsIntegrantProvider(
        cmsPeriod, expandedSwap, sabrPoint, forward, strike, expiryTime, shiftedCutOff, eta);
    double factor = dfPayment * intProv.g(forward) / intProv.h(forward);
    double absoluteTolerance = 1.0E-9;
    double relativeTolerance = 1.0E-5;
    RungeKuttaIntegrator1D integrator = new RungeKuttaIntegrator1D(absoluteTolerance, relativeTolerance, NUM_ITER);
    double[] kpkpp = intProv.kpkpp(strike);
    double firstPart;
    double thirdPart;
    if (intProv.getPutCall().isCall()) {
      firstPart = -kpkpp[0] * intProv.bs(strike);
      thirdPart = integrator.integrate(intProv.integrantDualDelta(), strike, strike + interval);
    } else {
      firstPart = 3d * kpkpp[0] * intProv.bs(strike);
      thirdPart = integrator.integrate(intProv.integrantDualDelta(), 0d, strike);
    }
    double secondPart =
        intProv.k(strike) * intProv.getSabrExtrapolation().priceDerivativeStrike(strike, intProv.getPutCall());
    return cmsPeriod.getNotional() * cmsPeriod.getYearFraction() * factor * (firstPart + secondPart + thirdPart);
  }

  //-------------------------------------------------------------------------
  /**
   * Inner class to implement the integration used in price replication.
   */
  private class CmsIntegrantProvider {
    /* Small parameter below which a value is regarded as 0. */
    protected static final double EPS = 1E-10;
    private final int nbFixedPeriod;
    private final int nbFixedPaymentYear;
    private final double tau;
    private final double eta;
    private final double strike;
    private final double factor;
    private final SabrExtrapolationRightFunction sabrExtrapolation;
    private final PutCall putCall;

    /**
     * Gets the nbFixedPeriod field.
     * 
     * @return the nbFixedPeriod
     */
    public int getNbFixedPeriod() {
      return nbFixedPeriod;
    }

    /**
     * Gets the nbFixedPaymentYear field.
     * 
     * @return the nbFixedPaymentYear
     */
    public int getNbFixedPaymentYear() {
      return nbFixedPaymentYear;
    }

    /**
     * Gets the tau field.
     * 
     * @return the tau
     */
    public double getTau() {
      return tau;
    }

    /**
     * Gets the eta field.
     * 
     * @return the eta
     */
    public double getEta() {
      return eta;
    }

    /**
     * Gets the putCall field.
     * 
     * @return the putCall
     */
    public PutCall getPutCall() {
      return putCall;
    }

    /**
     * Gets the strike field.
     * 
     * @return the strike
     */
    protected double getStrike() {
      return strike;
    }

    /**
     * Gets the sabrExtrapolation field.
     * 
     * @return the sabrExtrapolation
     */
    public SabrExtrapolationRightFunction getSabrExtrapolation() {
      return sabrExtrapolation;
    }

    public CmsIntegrantProvider(
        CmsPeriod cmsPeriod,
        ExpandedSwap swap,
        SabrFormulaData sabrPoint,
        double forward,
        double strike,
        double timeToExpiry,
        double shiftedCutOff,
        double eta) {

      ExpandedSwapLeg fixedLeg = swap.getLegs(SwapLegType.FIXED).get(0);
      this.nbFixedPeriod = fixedLeg.getPaymentPeriods().size();
      this.nbFixedPaymentYear = (int) Math.round(1d /
          ((RatePaymentPeriod) fixedLeg.getPaymentPeriods().get(0)).getAccrualPeriods().get(0).getYearFraction());
      this.tau = 1d / nbFixedPaymentYear;
      this.eta = eta;
      this.sabrExtrapolation = SabrExtrapolationRightFunction.of(forward, sabrPoint, shiftedCutOff, timeToExpiry, mu);
      this.putCall = cmsPeriod.getCmsPeriodType().equals(CmsPeriodType.FLOORLET) ? PutCall.PUT : PutCall.CALL;
      this.strike = strike;
      this.factor = g(forward) / h(forward);
    }

    /**
     * Obtains the integrant used in price replication.
     * 
     * @return the integrant
     */
    Function<Double, Double> integrant(){
      return new Function<Double, Double>() {
        @Override
        public Double apply(Double x) {
          double[] kD = kpkpp(x);
          // Implementation note: kD[0] contains the first derivative of k; kD[1] the second derivative of k.
          return factor * (kD[1] * (x - strike) + 2d * kD[0]) * bs(x);
        }
      };
    }

    /**
     * Obtains the integrant sensitivity to the i-th SABR parameter.
     * 
     * @param i  the index of SABR parameters
     * @return the vega integrant
     */
    Function<Double, Double> integrantVaga(int i) {
      return new Function<Double, Double>() {
        @Override
        public Double apply(Double x) {
          double[] kD = kpkpp(x);
          // Implementation note: kD[0] contains the first derivative of k; kD[1] the second derivative of k.
          DoubleArray priceDerivativeSABR = getSabrExtrapolation().priceAdjointSabr(x, putCall).getDerivatives();
          return priceDerivativeSABR.get(i) * (factor * (kD[1] * (x - strike) + 2d * kD[0]));
        }
      };
    }

    /**
     * Obtains the integrant sensitivity to strike.
     * 
     * @return the dual delta integrant
     */
    Function<Double, Double> integrantDualDelta() {
      return new Function<Double, Double>() {
        @Override
        public Double apply(Double x) {
          double[] kD = kpkpp(x);
          // Implementation note: kD[0] contains the first derivative of k; kD[1] the second derivative of k.
          return -kD[1] * bs(x);
        }
      };
    }

    /**
     * The approximation of the discount factor as function of the swap rate.
     * 
     * @param x  the swap rate.
     * @return the discount factor.
     */
    double h(double x) {
      return Math.pow(1d + tau * x, eta);
    }

    /**
     * The cash annuity.
     * 
     * @param x  the swap rate.
     * @return the annuity.
     */
    double g(double x) {
      if (x >= EPS) {
        double periodFactor = 1d + x / nbFixedPaymentYear;
        double nPeriodDiscount = Math.pow(periodFactor, -nbFixedPeriod);
        return (1d - nPeriodDiscount) / x;
      }
      return ((double) nbFixedPeriod) / nbFixedPaymentYear;
    }

    /**
     * The factor used in the strike part and in the integration of the replication.
     * 
     * @param x  the swap rate.
     * @return the factor.
     */
    double k(double x) {
      double g;
      double h;
      if (x >= EPS) {
        double periodFactor = 1d + x / nbFixedPaymentYear;
        double nPeriodDiscount = Math.pow(periodFactor, -nbFixedPeriod);
        g = (1d - nPeriodDiscount) / x;
        h = Math.pow(1.0 + tau * x, eta);
      } else {
        g = ((double) nbFixedPeriod) / nbFixedPaymentYear;
        h = 1d;
      }
      return h / g;
    }

    /**
     * The first and second derivative of the function k.
     * <p>
     * The first element is the first derivative and the second element is second derivative.
     * 
     * @param x  the swap rate.
     * @return the derivatives
     */
    protected double[] kpkpp(double x) {
      double periodFactor = 1d + x / nbFixedPaymentYear;
      double nPeriodDiscount = Math.pow(periodFactor, -nbFixedPeriod);
      /**
       * The value of the annuity and its first and second derivative.
       */
      double g, gp, gpp;
      if (x >= EPS) {
        g = (1d - nPeriodDiscount) / x;
        gp = -g / x + nbFixedPeriod * nPeriodDiscount / (x * nbFixedPaymentYear * periodFactor);
        gpp = 2d / (x * x) * g - 2d * nbFixedPeriod * nPeriodDiscount / (x * x * nbFixedPaymentYear * periodFactor)
            - (nbFixedPeriod + 1d) * nbFixedPeriod * nPeriodDiscount
            / (x * nbFixedPaymentYear * nbFixedPaymentYear * periodFactor * periodFactor);
      } else {
        // Implementation comment: When x is (almost) 0, useful for CMS swaps which are priced as CMS cap of strike 0.
        g = ((double) nbFixedPeriod) / nbFixedPaymentYear;
        gp = -0.5d * nbFixedPeriod * (nbFixedPeriod + 1d) / (nbFixedPaymentYear * nbFixedPaymentYear);
        gpp = 0.5d * nbFixedPeriod * (nbFixedPeriod + 1d) * (1d + (nbFixedPeriod + 2d) / 3d) /
            (nbFixedPaymentYear * nbFixedPaymentYear * nbFixedPaymentYear);
      }
      double h = Math.pow(1d + tau * x, eta);
      double hp = eta * tau * h / periodFactor;
      double hpp = (eta - 1d) * tau * hp / periodFactor;
      double kp = hp / g - h * gp / (g * g);
      double kpp = hpp / g - 2d * hp * gp / (g * g) - h * (gpp / (g * g) - 2d * (gp * gp) / (g * g * g));
      return new double[] {kp, kpp };
    }

    /**
     * The Black price with numeraire 1 as function of the strike.
     * 
     * @param strike  the strike.
     * @return the Black prcie.
     */
    double bs(double strike) {
      return sabrExtrapolation.price(strike, putCall);
    }
  }
  
  /**
   * Inner class to implement the integration used for delta calculation.
   */
  private class CmsDeltaIntegrantProvider extends CmsIntegrantProvider {

    private final double[] nnp;

    public CmsDeltaIntegrantProvider(
    CmsPeriod cmsPeriod,
    ExpandedSwap swap,
    SabrFormulaData sabrPoint,
    double forward,
    double strike,
    double timeToExpiry,
    double shiftedCutOff,
    double eta){
      super(cmsPeriod, swap, sabrPoint, forward, strike, timeToExpiry, shiftedCutOff, eta);
      nnp = nnp(forward);
    }

    /**
     * Obtains the integrant sensitivity to forward.
     * 
     * @return the delta integrant
     */
    Function<Double, Double> integrantDelta() {
      return new Function<Double, Double>() {
        @Override
        public Double apply(Double x) {
          double[] kD = kpkpp(x);
          // Implementation note: kD[0] contains the first derivative of k; kD[1] the second derivative of k.
          double[] bs = bsbsp(x);
          return (kD[1] * (x - getStrike()) + 2d * kD[0]) * (nnp[1] * bs[0] + nnp[0] * bs[1]);
        }
      };
    }

    /**
     * The Black price and its derivative with respect to the forward.
     * 
     * @param strike  the strike.
     * @return the Black price and its derivative.
     */
    private double[] bsbsp(double strike) {
      double[] result = new double[2];
      result[0] = getSabrExtrapolation().price(strike, getPutCall());
      result[1] = getSabrExtrapolation().priceDerivativeForward(strike, getPutCall());
      return result;
    }

    private double[] nnp(double x) {
      double[] result = new double[2];
      double[] ggp = ggp(x);
      double[] hhp = hhp(x);
      result[0] = ggp[0] / hhp[0];
      result[1] = ggp[1] / hhp[0] - ggp[0] * hhp[1] / (hhp[0] * hhp[0]);
      return result;
    }

    private double[] ggp(double x) {
      double[] result = new double[2];
      if (x >= EPS) {
        double periodFactor = 1d + x / getNbFixedPaymentYear();
        double nPeriodDiscount = Math.pow(periodFactor, -getNbFixedPeriod());
        result[0] = (1d - nPeriodDiscount) / x;
        result[1] = -result[0] / x + getTau() * getNbFixedPeriod() * nPeriodDiscount / (x * periodFactor);
      } else {
        result[0] = getNbFixedPeriod() * getTau();
        result[1] = -0.5d * getNbFixedPeriod() * (getNbFixedPeriod() + 1d) * getTau() * getTau();
      }
      return result;
    }

    private double[] hhp(double x) {
      double[] result = new double[2];
      result[0] = Math.pow(1d + getTau() * x, getEta());
      result[1] = getEta() * getTau() * result[0] / (1d + x * getTau());
      return result;
    }

  }

}
