/**
 * Copyright (C) 2016 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.pricer.sensitivity;

import static com.opengamma.strata.basics.index.IborIndices.USD_LIBOR_3M;
import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertTrue;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import org.testng.annotations.Test;

import com.opengamma.strata.market.curve.CurveCurrencyParameterSensitivities;
import com.opengamma.strata.market.curve.CurveCurrencyParameterSensitivity;
import com.opengamma.strata.market.curve.CurveParameterMetadata;
import com.opengamma.strata.market.sensitivity.IborRateSensitivity;
import com.opengamma.strata.market.sensitivity.PointSensitivities;
import com.opengamma.strata.pricer.calibration.CalibrationZeroRateUsd3OisIrsBsTest;
import com.opengamma.strata.pricer.rate.RatesProvider;

/**
 * Tests {@link MarketQuoteSensitivityCalculator}.
 */
public class MarketQuoteSensitivityCalculatorTest {
  
  public static final RatesProvider PROVIDER = CalibrationZeroRateUsd3OisIrsBsTest.rateProviderUsd();
  public static final PointSensitivities SENSI = PointSensitivities.of(
      IborRateSensitivity.of(USD_LIBOR_3M, LocalDate.of(2022, 2, 22), 1_000_000));
  
  public static final MarketQuoteSensitivityCalculator MQC = MarketQuoteSensitivityCalculator.DEFAULT;
  
  @Test // Check only the presence of the parameter metadata. The numbers are checked in the calibration tests,
  // e.g. CalibrationZeroRateUsd3OisIrsBsTest
  public void market_quote() {
    CurveCurrencyParameterSensitivities ps = PROVIDER.curveParameterSensitivity(SENSI);
    assertTrue(ps.getSensitivities().size() == 1);
    CurveCurrencyParameterSensitivities mqs = MQC.sensitivity(ps, PROVIDER);
    List<CurveCurrencyParameterSensitivity> mqsList = mqs.getSensitivities();
    assertTrue(mqsList.size() == 3);
    for (int i = 0; i < mqsList.size(); i++) {
      Optional<List<CurveParameterMetadata>> paramMeta = mqsList.get(i).getMetadata().getParameterMetadata();
      assertTrue(paramMeta.isPresent());
      assertEquals(paramMeta.get().size(), mqsList.get(i).getParameterCount());
      for (int j = 0; j < paramMeta.get().size(); j++) {
        assertTrue(paramMeta.get().get(j).getLabel() != null);
      }
    }
  }
  
}
