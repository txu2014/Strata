/**
 * Copyright (C) 2016 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.pricer.impl.capfloor;

import com.opengamma.strata.collect.ArgChecker;
import com.opengamma.strata.market.view.IborCapletFloorletVolatilities;
import com.opengamma.strata.pricer.capfloor.BlackIborCapletFloorletVolatilities;

/**
 * Pricer for caplet/floorlet in a log-normal or Black model. 
 * <p>
 * The value of the caplet/floorlet after expiry is a fixed currency amount or zero depending on an observed index rate. 
 * The value is zero if valuation date is after payment date of the cap/floor.  
 */
public class BlackIborCapletFloorletPeriodPricer
    extends VolatilityIborCapletFloorletPeriodPricer {

  /**
   * Default implementation.
   */
  static final public BlackIborCapletFloorletPeriodPricer DEFAULT = new BlackIborCapletFloorletPeriodPricer();

  @Override
  protected void validate(IborCapletFloorletVolatilities volatilities) {
    ArgChecker.isTrue(volatilities instanceof BlackIborCapletFloorletVolatilities, "volatilities must be Black volatilities");
  }

}
