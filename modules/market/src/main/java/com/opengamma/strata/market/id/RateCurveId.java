/**
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.market.id;

import com.opengamma.strata.basics.currency.Currency;

/**
 * Market data ID identifying a rates curve.
 */
public interface RateCurveId extends CurveId {

  /**
   * Returns the currency of the curve.
   *
   * @return the currency of the curve
   */
  public Currency getCurrency();
}
