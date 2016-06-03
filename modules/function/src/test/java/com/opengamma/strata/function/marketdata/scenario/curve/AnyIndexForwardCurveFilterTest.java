/**
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.function.marketdata.scenario.curve;

import static com.opengamma.strata.collect.TestHelper.coverImmutableBean;
import static org.assertj.core.api.Assertions.assertThat;

import org.testng.annotations.Test;

import com.opengamma.strata.basics.index.IborIndices;
import com.opengamma.strata.basics.market.MarketDataBox;
import com.opengamma.strata.market.curve.ConstantNodalCurve;
import com.opengamma.strata.market.curve.Curve;
import com.opengamma.strata.market.curve.CurveGroupName;
import com.opengamma.strata.market.id.IborIndexCurveId;
import com.opengamma.strata.market.id.IndexCurveId;

/**
 * Test {@link AnyIndexForwardCurveFilter}.
 */
@Test
public class AnyIndexForwardCurveFilterTest {

  public void match() {
    AnyIndexForwardCurveFilter test = AnyIndexForwardCurveFilter.INSTANCE;
    assertThat(test.getMarketDataIdType()).isEqualTo(IndexCurveId.class);
    IndexCurveId id = IborIndexCurveId.of(IborIndices.USD_LIBOR_1M, CurveGroupName.of("curveName"));
    Curve curve = ConstantNodalCurve.of("curveName", 1);
    assertThat(test.matches(id, MarketDataBox.ofSingleValue(curve))).isTrue();
  }

  //-------------------------------------------------------------------------
  public void coverage() {
    AnyIndexForwardCurveFilter test = AnyIndexForwardCurveFilter.INSTANCE;
    coverImmutableBean(test);
  }

}
