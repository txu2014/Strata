/**
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.pricer.impl.option;

import java.util.Arrays;
import java.util.function.Function;

import com.opengamma.strata.basics.PutCall;
import com.opengamma.strata.basics.value.ValueDerivatives;
import com.opengamma.strata.collect.ArgChecker;
import com.opengamma.strata.collect.array.DoubleArray;
import com.opengamma.strata.collect.array.DoubleMatrix;
import com.opengamma.strata.math.impl.linearalgebra.DecompositionResult;
import com.opengamma.strata.math.impl.linearalgebra.SVDecompositionCommons;
import com.opengamma.strata.math.impl.rootfinding.BracketRoot;
import com.opengamma.strata.math.impl.rootfinding.RidderSingleRootFinder;
import com.opengamma.strata.pricer.impl.volatility.smile.function.SabrFormulaData;
import com.opengamma.strata.pricer.impl.volatility.smile.function.SabrHaganVolatilityFunctionProvider;
import com.opengamma.strata.pricer.impl.volatility.smile.function.VolatilityFunctionProvider;

/**
 * Pricing function in the SABR model with Hagan et al. volatility function and controlled extrapolation for large strikes by extrapolation on call prices.
 * The form of the extrapolation as a function of the strike is
 * \begin{equation*}
 * f(K) = K^{-\mu} \exp\left( a + \frac{b}{K} + \frac{c}{K^2} \right).
 * \end{equation*}
 * <P>
 * Benaim, S., Dodgson, M., and Kainth, D. (2008). An arbitrage-free method for smile extrapolation. Technical report, Royal Bank of Scotland.
 * <P>
 * OpenGamma implementation note: Smile extrapolation, version 1.2, May 2011.
 */
public class SabrExtrapolationRightFunction {
  /**
   * The volatility provider. 
   */
  private final VolatilityFunctionProvider<SabrFormulaData> sabrFunction;
  /**
   * The forward.
   */
  private final double forward;
  /**
   * The time to option expiry.
   */
  private final double timeToExpiry;
  /**
   * The SABR parameter for the pricing below the cut-off strike.
   */
  private final SabrFormulaData sabrData;
  /**
   * The cut-off strike. The smile is extrapolated above that level.
   */
  private final double cutOffStrike;
  /**
   * The tail thickness parameter.
   */
  private final double mu;
  /**
   * The three fitting parameters.
   */
  private final double[] parameter;
  /**
   * The derivative of the three fitting parameters with respect to the forward.
   * <p>
   * Those parameters are computed only when and if required.
   */
  private double[] parameterDerivativeForward = new double[3];
  /**
   * Flag indicating if the parameter derivatives to forward have been computed.
   */
  private boolean parameterDerivativeForwardComputed;
  /**
   * The derivative of the three fitting parameters with respect to the SABR parameters.
   * <p>
   * Those parameters are computed only when and if required.
   */
  private double[][] parameterDerivativeSABR = new double[4][3];
  /**
   * Flag indicating if the parameter derivatives to SABR parameters have been computed.
   */
  private boolean parameterDerivativeSABRComputed;
  /**
   * The Black implied volatility at the cut-off strike.
   */
  private double volatilityK;
  /**
   * The price and its derivatives of order 1 and 2 at the cut-off strike.
   */
  private final double[] priceK = new double[3];
  /**
   * Matrix decomposition. 
   */
  private static final SVDecompositionCommons SVD = new SVDecompositionCommons();
  /**
   * Black function used.
   */
  private static final BlackPriceFunction BLACK_FUNCTION = new BlackPriceFunction();
  /**
   * Value below which the time-to-expiry is considered to be 0 and the price of the fitting parameters fit a price of 0 (OTM).
   */
  private static final double SMALL_EXPIRY = 1.0E-6;
  /**
   * If the time-to-expiry is smaller than {@code SMALL_EXPIRY}, the parameter 'a' is set to be this value. 
   */
  private static final double SMALL_PARAMETER = -1.0E4;
  /**
   * Value below which the price is considered to be 0.
   */
  private static final double SMALL_PRICE = 1.0E-15;


  /**
   * Creates an instance with default volatility provider. 
   * <p>
   * The default volatility provider is {@link SabrHaganVolatilityFunctionProvider}.
   * 
   * @param forward  the forward
   * @param sabrData  the SABR formula data
   * @param cutOffStrike  the cut-off-strike
   * @param timeToExpiry  the time to expiration
   * @param mu  the mu parameter
   * @return the instance
   */
  public static SabrExtrapolationRightFunction of(
      double forward,
      SabrFormulaData sabrData,
      double cutOffStrike,
      double timeToExpiry,
      double mu) {
    return new SabrExtrapolationRightFunction(forward, sabrData, cutOffStrike, timeToExpiry, mu,
        SabrHaganVolatilityFunctionProvider.DEFAULT);
  }

  /**
   * Creates an instance with volatility provider specified. 
   * 
   * @param forward  the forward
   * @param sabrData  the SABR formula data
   * @param cutOffStrike  the cut-off-strike
   * @param timeToExpiry  the time to expiration
   * @param mu  the mu parameter
   * @param volatilityFunction  the volatility function
   * @return the instance
   */
  public static SabrExtrapolationRightFunction of(
      double forward, SabrFormulaData
      sabrData,
      double cutOffStrike,
      double timeToExpiry,
      double mu,
      VolatilityFunctionProvider<SabrFormulaData> volatilityFunction) {
    return new SabrExtrapolationRightFunction(forward, sabrData, cutOffStrike, timeToExpiry, mu, volatilityFunction);
  }

  // private constructor
  private SabrExtrapolationRightFunction(double forward, SabrFormulaData sabrData, double cutOffStrike,
      double timeToExpiry, double mu, VolatilityFunctionProvider<SabrFormulaData> volatilityFunction) {
    ArgChecker.notNull(sabrData, "sabrData");
    ArgChecker.notNull(volatilityFunction, "volatilityFunction");
    this.sabrFunction = volatilityFunction;
    this.forward = forward;
    this.sabrData = sabrData;
    this.cutOffStrike = cutOffStrike;
    this.timeToExpiry = timeToExpiry;
    this.mu = mu;
    if (timeToExpiry > SMALL_EXPIRY) {
      parameter = computesFittingParameters();
    } else { // Implementation note: when time to expiry is very small, the price above the cut-off strike and its derivatives should be 0 (or at least very small).
      parameter = new double[] {SMALL_PARAMETER, 0.0, 0.0 };
      parameterDerivativeForward = new double[3];
      parameterDerivativeForwardComputed = true;
      parameterDerivativeSABR = new double[4][3];
      parameterDerivativeSABRComputed = true;
    }
  }

  //-------------------------------------------------------------------------
  /**
   * Computes the option price with numeraire=1. 
   * <p>
   * The price is SABR below the cut-off strike and extrapolated beyond.
   * 
   * @param strike  the strike of the option
   * @param putCall  whether the option is put or call
   * @return the option price
   */
  public double price(double strike, PutCall putCall) {
    double price = 0d;
    if (strike <= cutOffStrike) { // Uses Hagan et al SABR function.
      double volatility = sabrFunction.getVolatility(forward, strike, timeToExpiry, sabrData);
      price = BlackFormulaRepository.price(forward, strike, timeToExpiry, volatility, putCall.isCall());
    } else { // Uses extrapolation for call.
      price = extrapolation(strike);
      if (putCall.isPut()) { // Put by call/put parity
        price -= (forward - strike);
      }
    }
    return price;
  }

  /**
   * Computes the option price derivative with respect to the strike. 
   * <p>
   * The price is SABR below the cut-off strike and extrapolated beyond.
   * 
   * @param strike  the strike of the option
   * @param putCall  whether the option is put or call
   * @return the option price derivative
   */
  public double priceDerivativeStrike(double strike, PutCall putCall) {
    double pDK = 0.0;
    if (strike <= cutOffStrike) { // Uses Hagan et al SABR function.
      BlackPriceFunction blackFunction = new BlackPriceFunction();
      ValueDerivatives volatilityAdjoint = sabrFunction.getVolatilityAdjoint(forward, strike, timeToExpiry, sabrData);
      EuropeanVanillaOption option = EuropeanVanillaOption.of(strike, timeToExpiry, putCall);
      BlackFunctionData dataBlack = BlackFunctionData.of(forward, 1.0, volatilityAdjoint.getValue());
      ValueDerivatives bsAdjoint = blackFunction.getPriceAdjoint(option, dataBlack);
      pDK = bsAdjoint.getDerivative(2) + bsAdjoint.getDerivative(1) * volatilityAdjoint.getDerivative(1);
    } else { // Uses extrapolation for call.
      pDK = extrapolationDerivative(strike);
      if (putCall.isPut()) { // Put by call/put parity
        pDK += 1.0;
      }
    }
    return pDK;
  }

  /**
   * Computes the option price derivative with respect to the forward. 
   * <p>
   * The price is SABR below the cut-off strike and extrapolated beyond.
   * 
   * @param strike  the strike of the option
   * @param putCall  whether the option is put or call
   * @return the option price derivative
   */
  public double priceDerivativeForward(double strike, PutCall putCall) {
    double priceDerivative;
    if (strike <= cutOffStrike) { // Uses Hagan et al SABR function.
      ValueDerivatives volatilityA = sabrFunction.getVolatilityAdjoint(forward, strike, timeToExpiry, sabrData);
      BlackFunctionData dataBlack = BlackFunctionData.of(forward, 1d, volatilityA.getValue());
      EuropeanVanillaOption option = EuropeanVanillaOption.of(strike, timeToExpiry, putCall);
      ValueDerivatives pA = BLACK_FUNCTION.getPriceAdjoint(option, dataBlack);
      priceDerivative = pA.getDerivative(0) + pA.getDerivative(1) * volatilityA.getDerivative(0);
    } else { // Uses extrapolation for call.
      if (!parameterDerivativeForwardComputed) {
        parameterDerivativeForward = computesParametersDerivativeForward();
        parameterDerivativeForwardComputed = true;
      }
      double f = extrapolation(strike);
      double fDa = f;
      double fDb = f / strike;
      double fDc = fDb / strike;
      priceDerivative = fDa * parameterDerivativeForward[0] + fDb * parameterDerivativeForward[1] + fDc *
          parameterDerivativeForward[2];
      if (putCall.isPut()) { // Put by call/put parity
        priceDerivative -= 1;
      }
    }
    return priceDerivative;
  }

  /**
   * Computes the option price derivative with respect to the SABR parameters. 
   * <p>
   * The price is SABR below the cut-off strike and extrapolated beyond.
   * 
   * @param strike  the strike of the option
   * @param putCall  whether the option is put or call
   * @return the option and its derivative
   */
  public ValueDerivatives priceAdjointSabr(double strike, PutCall putCall) {
    double[] priceDerivativeSABR = new double[4];
    double price;
    if (strike <= cutOffStrike) { // Uses Hagan et al SABR function.
      ValueDerivatives volatilityA = sabrFunction.getVolatilityAdjoint(forward, strike, timeToExpiry, sabrData);
      BlackFunctionData dataBlack = BlackFunctionData.of(forward, 1d, volatilityA.getValue());
      EuropeanVanillaOption option = EuropeanVanillaOption.of(strike, timeToExpiry, putCall);
      ValueDerivatives pA = BLACK_FUNCTION.getPriceAdjoint(option, dataBlack);
      price = pA.getValue();
      for (int loopparam = 0; loopparam < 4; loopparam++) {
        priceDerivativeSABR[loopparam] = pA.getDerivative(1) * volatilityA.getDerivative(loopparam + 2);
      }
    } else { // Uses extrapolation for call.
      if (!parameterDerivativeSABRComputed) {
        parameterDerivativeSABR = computesParametersDerivativeSABR();
        parameterDerivativeSABRComputed = true;
      }
      double f = extrapolation(strike);
      double fDa = f;
      double fDb = f / strike;
      double fDc = fDb / strike;
      price = putCall.isCall() ? f : f - forward + strike; // Put by call/put parity
      for (int loopparam = 0; loopparam < 4; loopparam++) {
        priceDerivativeSABR[loopparam] = fDa * parameterDerivativeSABR[loopparam][0] + fDb *
            parameterDerivativeSABR[loopparam][1] + fDc * parameterDerivativeSABR[loopparam][2];
      }
    }
    return ValueDerivatives.of(price, DoubleArray.ofUnsafe(priceDerivativeSABR));
  }

  //-------------------------------------------------------------------------
  /**
   * Gets the underlying SABR data.
   * 
   * @return the sabrData
   */
  public SabrFormulaData getSabrData() {
    return sabrData;
  }

  /**
   * Gets the cut-off strike. 
   * <p>
   * The smile is extrapolated above that level.
   * 
   * @return the cutOffStrike
   */
  public double getCutOffStrike() {
    return cutOffStrike;
  }

  /**
   * Gets the tail thickness parameter.
   * 
   * @return The mu parameter.
   */
  public double getMu() {
    return mu;
  }

  /**
   * Gets the time to expiry.
   * 
   * @return the time to expiry.
   */
  public double getTimeToExpiry() {
    return timeToExpiry;
  }

  /**
   * Gets the three fitting parameters.
   * 
   * @return the parameters.
   */
  public double[] getParameter() {
    return parameter;
  }

  /**
   * Gets the three fitting parameters derivatives with respect to the forward.
   * 
   * @return the parameters derivative
   */
  public double[] getParameterDerivativeForward() {
    if (!parameterDerivativeForwardComputed) {
      parameterDerivativeForward = computesParametersDerivativeForward();
      parameterDerivativeForwardComputed = true;
    }
    return parameterDerivativeForward;
  }

  /**
   * Gets the three fitting parameters derivatives with respect to the SABR parameters.
   * 
   * @return the parameters derivative.
   */
  public double[][] getParameterDerivativeSABR() {
    if (!parameterDerivativeSABRComputed) {
      parameterDerivativeSABR = computesParametersDerivativeSABR();
      parameterDerivativeSABRComputed = true;
    }
    return parameterDerivativeSABR;
  }

  //-------------------------------------------------------------------------
  private double[] computesFittingParameters() {
    double[] param = new double[3]; // Implementation note: called a,b,c in the note.
    EuropeanVanillaOption option = EuropeanVanillaOption.of(cutOffStrike, timeToExpiry, PutCall.CALL);
    // Computes derivatives at cut-off.
    double[] vD = new double[6];
    double[][] vD2 = new double[2][2];
    volatilityK = sabrFunction.getVolatilityAdjoint2(forward, cutOffStrike, timeToExpiry, sabrData, vD, vD2);
    BlackFunctionData dataBlack = BlackFunctionData.of(forward, 1d, volatilityK);
    double[] bsD = new double[3];
    double[][] bsD2 = new double[3][3];
    priceK[0] = BLACK_FUNCTION.getPriceAdjoint2(option, dataBlack, bsD, bsD2);
    priceK[1] = bsD[2] + bsD[1] * vD[1];
    priceK[2] = bsD2[2][2] + bsD2[1][2] * vD[1] + (bsD2[2][1] + bsD2[1][1] * vD[1]) * vD[1] + bsD[1] * vD2[1][1];
    if (Math.abs(priceK[0]) < SMALL_PRICE && Math.abs(priceK[1]) < SMALL_PRICE && Math.abs(priceK[2]) < SMALL_PRICE) {
      // Implementation note: If value and its derivatives is too small, then parameters are such that the extrapolated price is "very small".
      return new double[] {-100.0, 0, 0 };
    }
    Function<Double, Double> toSolveC = getCFunction(priceK, cutOffStrike, mu);
    BracketRoot bracketer = new BracketRoot();
    double accuracy = 1.0E-5;
    RidderSingleRootFinder rootFinder = new RidderSingleRootFinder(accuracy);
    double[] range = bracketer.getBracketedPoints(toSolveC, -1.0, 1.0);
    param[2] = rootFinder.getRoot(toSolveC, range[0], range[1]);
    param[1] = -2 * param[2] / cutOffStrike - (priceK[1] / priceK[0] * cutOffStrike + mu) * cutOffStrike;
    param[0] = Math.log(priceK[0] / Math.pow(cutOffStrike, -mu)) - param[1] / cutOffStrike - param[2] /
        (cutOffStrike * cutOffStrike);
    return param;
  }

  /**
   * Computes the derivative of the three fitting parameters with respect to the forward. 
   * The computation requires some third order derivatives; they are computed by finite difference on the second order derivatives.
   * Used to compute the derivative of the price with respect to the forward.
   * @return The derivatives.
   */
  private double[] computesParametersDerivativeForward() {
    if (Math.abs(priceK[0]) < SMALL_PRICE && Math.abs(priceK[1]) < SMALL_PRICE && Math.abs(priceK[2]) < SMALL_PRICE) {
      // Implementation note: If value and its derivatives is too small, then parameters are such that the extrapolated price is "very small".
      return new double[] {0.0, 0.0, 0.0 };
    }
    // Derivative of price with respect to forward.
    double[] pDF = new double[3];
    double shift = 0.00001;
    EuropeanVanillaOption option = EuropeanVanillaOption.of(cutOffStrike, timeToExpiry, PutCall.CALL);
    double[] vD = new double[6];
    double[][] vD2 = new double[2][2];
    sabrFunction.getVolatilityAdjoint2(forward, cutOffStrike, timeToExpiry, sabrData, vD, vD2);
    BlackFunctionData dataBlack = BlackFunctionData.of(forward, 1.0, volatilityK);
    double[] bsD = new double[3];
    double[][] bsD2 = new double[3][3];
    BLACK_FUNCTION.getPriceAdjoint2(option, dataBlack, bsD, bsD2);
    pDF[0] = bsD[0] + bsD[1] * vD[0];
    pDF[1] = bsD2[0][2] + bsD2[1][0] * vD[1] + (bsD2[2][1] + bsD2[1][1] * vD[1]) * vD[0] + bsD[1] * vD2[1][0];
    EuropeanVanillaOption optionKP = EuropeanVanillaOption.of(cutOffStrike * (1 + shift), timeToExpiry, PutCall.CALL);
    double[] bsDKP = new double[3];
    double[][] bsD2KP = new double[3][3];
    BLACK_FUNCTION.getPriceAdjoint2(optionKP, dataBlack, bsDKP, bsD2KP);
    double bsD3FKK = (bsD2KP[2][0] - bsD2[2][0]) / (cutOffStrike * shift);
    BlackFunctionData dataBlackVP = BlackFunctionData.of(forward, 1.0, volatilityK * (1 + shift));
    double[] bsDVP = new double[3];
    double[][] bsD2VP = new double[3][3];
    BLACK_FUNCTION.getPriceAdjoint2(option, dataBlackVP, bsDVP, bsD2VP);
    double bsD3sss = (bsD2VP[1][1] - bsD2[1][1]) / (volatilityK * shift);
    double bsD3sFK = (bsD2VP[0][2] - bsD2[0][2]) / (volatilityK * shift);
    double bsD3sFs = (bsD2VP[0][1] - bsD2[0][1]) / (volatilityK * shift);
    double bsD3sKK = (bsD2VP[2][2] - bsD2[2][2]) / (volatilityK * shift);
    double bsD3ssK = (bsD2VP[1][2] - bsD2[1][2]) / (volatilityK * shift);
    double[] vDKP = new double[6];
    double[][] vD2KP = new double[2][2];
    sabrFunction.getVolatilityAdjoint2(forward, cutOffStrike * (1 + shift), timeToExpiry, sabrData, vDKP, vD2KP);
    double vD3KKF = (vD2KP[1][0] - vD2[1][0]) / (cutOffStrike * shift);
    pDF[2] = bsD3FKK + bsD3sFK * vD[1] + (bsD3sFK + bsD3sFs * vD[1]) * vD[1] + bsD2[1][0] * vD2[1][1] +
        (bsD3sKK + bsD3ssK * vD[1] + (bsD3ssK + bsD3sss * vD[1]) * vD[1] + bsD2[1][1] * vD2[1][1])
        * vD[0] + 2 * (bsD2[2][1] + bsD2[1][1] * vD[1]) * vD2[1][0] + bsD[1] * vD3KKF;
    // Derivative of f with respect to abc.
    double[][] fD = new double[3][3]; // fD[i][j]: derivative with respect to jth variable of f_i
    double f = priceK[0];
    double fp = priceK[1];
    double fpp = priceK[2];
    fD[0][0] = f;
    fD[0][1] = f / cutOffStrike;
    fD[0][2] = fD[0][1] / cutOffStrike;
    fD[1][0] = fp;
    fD[1][1] = (fp - fD[0][1]) / cutOffStrike;
    fD[1][2] = (fp - 2 * fD[0][1]) / (cutOffStrike * cutOffStrike);
    fD[2][0] = fpp;
    fD[2][1] = (fpp + fD[0][2] *
        (2 * (mu + 1) + 2 * parameter[1] / cutOffStrike + 4 * parameter[2] / (cutOffStrike * cutOffStrike))) /
        cutOffStrike;
    fD[2][2] = (fpp + fD[0][2] *
        (2 * (2 * mu + 3) + 4 * parameter[1] / cutOffStrike + 8 * parameter[2] / (cutOffStrike * cutOffStrike))) /
        (cutOffStrike * cutOffStrike);
    DecompositionResult decmp = SVD.apply(DoubleMatrix.ofUnsafe(fD));
    return decmp.solve(pDF);
  }

  /**
   * Computes the derivative of the three fitting parameters with respect to the SABR parameters. 
   * The computation requires some third order derivatives; they are computed by finite difference on the second order derivatives.
   * Used to compute the derivative of the price with respect to the SABR parameters.
   * @return The derivatives.
   */
  private double[][] computesParametersDerivativeSABR() {
    double[][] result = new double[4][3];
    if (Math.abs(priceK[0]) < SMALL_PRICE && Math.abs(priceK[1]) < SMALL_PRICE && Math.abs(priceK[2]) < SMALL_PRICE) {
      // Implementation note: If value and its derivatives is too small, then parameters are such that the extrapolated price is "very small".
      return result;
    }
    // Derivative of price with respect to SABR parameters.
    double[][] pDSABR = new double[4][3]; // parameter SABR - equation
    double shift = 0.00001;
    EuropeanVanillaOption option = EuropeanVanillaOption.of(cutOffStrike, timeToExpiry, PutCall.CALL);
    double[] vD = new double[6];
    double[][] vD2 = new double[2][2];
    sabrFunction.getVolatilityAdjoint2(forward, cutOffStrike, timeToExpiry, sabrData, vD, vD2);
    BlackFunctionData dataBlack = BlackFunctionData.of(forward, 1d, volatilityK);
    for (int loopparam = 0; loopparam < 4; loopparam++) {
      int paramIndex = 2 + loopparam;
      double[] bsD = new double[3];
      double[][] bsD2 = new double[3][3];
      BLACK_FUNCTION.getPriceAdjoint2(option, dataBlack, bsD, bsD2);
      pDSABR[loopparam][0] = bsD[1] * vD[paramIndex];
      double[] vDpP = new double[6];
      double[][] vD2pP = new double[2][2];
      SabrFormulaData sabrDatapP;
      double param;
      double paramShift;
      switch (loopparam) {
        case 0:
          param = sabrData.getAlpha();
          paramShift = param * shift; // Relative shift to cope with difference in order of magnitude.
          sabrDatapP = sabrData.withAlpha(param + paramShift);
          break;
        case 1:
          param = sabrData.getBeta();
          paramShift = shift; // Absolute shift as usually 0 <= beta <= 1; beta can be zero, so relative shift is not possible.
          sabrDatapP = sabrData.withBeta(param + paramShift);
          break;
        case 2:
          param = sabrData.getRho();
          paramShift = shift; // Absolute shift as -1 <= rho <= 1; rho can be zero, so relative shift is not possible.
          sabrDatapP = sabrData.withRho(param + paramShift);
          break;
        default:
          param = sabrData.getNu();
          paramShift = param * shift; // Relative shift to cope with difference in order of magnitude.
          sabrDatapP = sabrData.withNu(param + paramShift);
          break;
      }
      sabrFunction.getVolatilityAdjoint2(forward, cutOffStrike, timeToExpiry, sabrDatapP, vDpP, vD2pP);
      double vD2Kp = (vDpP[1] - vD[1]) / paramShift;
      double vD3KKa = (vD2pP[1][1] - vD2[1][1]) / paramShift;
      pDSABR[loopparam][1] = (bsD2[2][1] + bsD2[1][1] * vD[1]) * vD[paramIndex] + bsD[1] * vD2Kp;
      double[] bsDVP = new double[3];
      double[][] bsD2VP = new double[3][3];
      BlackFunctionData dataBlackVP = BlackFunctionData.of(forward, 1d, volatilityK * (1d + shift));
      BLACK_FUNCTION.getPriceAdjoint2(option, dataBlackVP, bsDVP, bsD2VP);
      double bsD3sss = (bsD2VP[1][1] - bsD2[1][1]) / (volatilityK * shift);
      double bsD3sKK = (bsD2VP[2][2] - bsD2[2][2]) / (volatilityK * shift);
      double bsD3ssK = (bsD2VP[1][2] - bsD2[1][2]) / (volatilityK * shift);
      pDSABR[loopparam][2] = (bsD3sKK + bsD3ssK * vD[1] + (bsD3ssK + bsD3sss * vD[1]) * vD[1] + bsD2[1][1] * vD2[1][1]) *
          vD[paramIndex] + 2 * (bsD2[1][2] + bsD2[1][1] * vD[1]) * vD2Kp + bsD[1]
          * vD3KKa;
    }
    // Derivative of f with respect to abc.
    double[][] fD = new double[3][3]; // fD[i][j]: derivative with respect to jth variable of f_i
    double f = priceK[0];
    double fp = priceK[1];
    double fpp = priceK[2];
    fD[0][0] = f;
    fD[0][1] = f / cutOffStrike;
    fD[0][2] = fD[0][1] / cutOffStrike;
    fD[1][0] = fp;
    fD[1][1] = (fp - fD[0][1]) / cutOffStrike;
    fD[1][2] = (fp - 2 * fD[0][1]) / (cutOffStrike * cutOffStrike);
    fD[2][0] = fpp;
    fD[2][1] = (fpp + fD[0][2] *
        (2 * (mu + 1) + 2 * parameter[1] / cutOffStrike + 4 * parameter[2] / (cutOffStrike * cutOffStrike))) /
        cutOffStrike;
    fD[2][2] = (fpp + fD[0][2] *
        (2 * (2 * mu + 3) + 4 * parameter[1] / cutOffStrike + 8 * parameter[2] / (cutOffStrike * cutOffStrike))) /
        (cutOffStrike * cutOffStrike);
    DoubleMatrix fDmatrix = DoubleMatrix.ofUnsafe(fD);

    DecompositionResult decmp = SVD.apply(fDmatrix);
    for (int loopparam = 0; loopparam < 4; loopparam++) {
      result[loopparam] = decmp.solve(pDSABR[loopparam]);
    }
    return result;
  }

  /**
   * The extrapolation function.
   * @param strike The strike.
   * @return The extrapolated price.
   */
  private double extrapolation(double strike) {
    return Math.pow(strike, -mu) *
        Math.exp(parameter[0] + parameter[1] / strike + parameter[2] / (strike * strike));
  }

  /**
   * The first order derivative of the extrapolation function with respect to the strike.
   * @param strike The strike.
   * @return The extrapolated price.
   */
  private double extrapolationDerivative(double strike) {
    return -extrapolation(strike) * (mu + (parameter[1] + 2 * parameter[2] / strike) / strike) / strike;
  }

  private Function<Double, Double> getCFunction(double[] price, double cutOffStrike, double mu) {
    double[] cPrice = Arrays.copyOf(price, price.length);
    return new Function<Double, Double>() {
      @Override
      public Double apply(Double c) {
        double b = -2 * c / cutOffStrike - (cPrice[1] / cPrice[0] * cutOffStrike + mu) * cutOffStrike;
        double k2 = cutOffStrike * cutOffStrike;
        double res = -cPrice[2] / cPrice[0] * k2 + mu * (mu + 1) + 2 * b * (mu + 1) / cutOffStrike +
            (2 * c * (2 * mu + 3) + b * b) / k2 + 4 * b * c / (k2 * cutOffStrike) + 4 * c * c / (k2 * k2);
        return res;
      }
    };
  }

}
