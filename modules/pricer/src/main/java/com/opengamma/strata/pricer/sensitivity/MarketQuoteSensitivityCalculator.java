/**
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.pricer.sensitivity;

import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

import com.opengamma.strata.collect.ArgChecker;
import com.opengamma.strata.collect.array.DoubleArray;
import com.opengamma.strata.collect.array.DoubleMatrix;
import com.opengamma.strata.market.curve.Curve;
import com.opengamma.strata.market.curve.CurveCurrencyParameterSensitivities;
import com.opengamma.strata.market.curve.CurveCurrencyParameterSensitivity;
import com.opengamma.strata.market.curve.CurveInfoType;
import com.opengamma.strata.market.curve.CurveMetadata;
import com.opengamma.strata.market.curve.CurveName;
import com.opengamma.strata.market.curve.CurveParameterSize;
import com.opengamma.strata.market.curve.DefaultCurveMetadata;
import com.opengamma.strata.market.curve.JacobianCalibrationMatrix;
import com.opengamma.strata.math.impl.matrix.MatrixAlgebra;
import com.opengamma.strata.math.impl.matrix.OGMatrixAlgebra;
import com.opengamma.strata.pricer.rate.RatesProvider;

/**
 * Calculator to obtain the Market Quote sensitivities.
 * <p>
 * This needs the {@link JacobianCalibrationMatrix} obtained during curve calibration.
 * The Market Quote sensitivities are also called Par Rate when the instruments used
 * in the curve calibration are quoted in rate, e.g. IRS, FRA or OIS.
 */
public class MarketQuoteSensitivityCalculator {

  /**
   * The default instance.
   */
  public static final MarketQuoteSensitivityCalculator DEFAULT = new MarketQuoteSensitivityCalculator();
  /**
   * The matrix algebra used for matrix inversion.
   */
  private static final MatrixAlgebra MATRIX_ALGEBRA = new OGMatrixAlgebra();

  //-------------------------------------------------------------------------
  /**
   * Calculates the market quote sensitivities from parameter sensitivity.  
   * 
   * @param paramSensitivities  the curve parameter sensitivities
   * @param provider  the rates provider, containing Jacobian calibration information
   * @return the market quote sensitivities
   */
  public CurveCurrencyParameterSensitivities sensitivity(
      CurveCurrencyParameterSensitivities paramSensitivities,
      RatesProvider provider) {

    ArgChecker.notNull(paramSensitivities, "paramSensitivities");
    ArgChecker.notNull(provider, "provider");
    
    // Collect all the relevant curves by name, including the indirect ones, and their metadata
    Map<CurveName, CurveMetadata> metadataMap = new HashMap<>();
    for (CurveCurrencyParameterSensitivity paramSens : paramSensitivities.getSensitivities()) {
      CurveMetadata metadata = paramSens.getMetadata();
      JacobianCalibrationMatrix info = metadata.findInfo(CurveInfoType.JACOBIAN)
          .orElseThrow(() -> new IllegalArgumentException(
              "Market Quote sensitivity requires Jacobian calibration information"));
      for(CurveParameterSize p:info.getOrder()) {
        Curve curve = provider.findCurve(p.getName())
            .orElseThrow(() -> new IllegalArgumentException(
                "Market Quote sensitivity requires curve: " + p.getName()));
        metadataMap.put(p.getName(), curve.getMetadata()); // If curve referenced several times, item overridden
      }
    }

    // Compute market quote sensitivities and label them
    CurveCurrencyParameterSensitivities result = CurveCurrencyParameterSensitivities.empty();
    for (CurveCurrencyParameterSensitivity paramSens : paramSensitivities.getSensitivities()) {
      CurveMetadata metadata = paramSens.getMetadata();
      JacobianCalibrationMatrix info = metadata.findInfo(CurveInfoType.JACOBIAN)
          .orElseThrow(() -> new IllegalArgumentException(
              "Market Quote sensitivity requires Jacobian calibration information"));

      // calculate the market quote sensitivity using the Jacobian
      DoubleMatrix jacobian = info.getJacobianMatrix();
      DoubleArray paramSensMatrix = paramSens.getSensitivity();
      DoubleArray marketQuoteSensArray = (DoubleArray) MATRIX_ALGEBRA.multiply(paramSensMatrix, jacobian);

      // split between different curves
      Map<CurveName, DoubleArray> split = info.splitValues(marketQuoteSensArray);
      for (Entry<CurveName, DoubleArray> entry : split.entrySet()) {
        CurveMetadata metaAvailable = metadataMap.get(entry.getKey());
        CurveCurrencyParameterSensitivity marketQuoteSensObject = CurveCurrencyParameterSensitivity.of(
            (metaAvailable != null) ? metaAvailable : DefaultCurveMetadata.of(entry.getKey()),
            paramSens.getCurrency(),
            entry.getValue());
        result = result.combinedWith(marketQuoteSensObject);
      }
    }
    return result;
  }

}
