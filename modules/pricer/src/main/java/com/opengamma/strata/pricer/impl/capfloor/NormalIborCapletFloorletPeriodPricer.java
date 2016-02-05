/**
 * Copyright (C) 2016 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.pricer.impl.capfloor;

import com.opengamma.strata.collect.ArgChecker;
import com.opengamma.strata.market.view.IborCapletFloorletVolatilities;
import com.opengamma.strata.pricer.capfloor.NormalIborCapletFloorletVolatilities;

/**
 * Pricer for caplet/floorlet in a normal or Bachelier model. 
 * <p>
 * The value of the caplet/floorlet after expiry is a fixed currency amount or zero depending on an observed index rate. 
 * The value is zero if valuation date is after payment date of the cap/floor.  
 */
public class NormalIborCapletFloorletPeriodPricer
    extends VolatilityIborCapletFloorletPeriodPricer {

  /**
  * Default implementation.
  */
  static final public NormalIborCapletFloorletPeriodPricer DEFAULT = new NormalIborCapletFloorletPeriodPricer();

  @Override
  protected void validate(IborCapletFloorletVolatilities volatilities) {
    ArgChecker.isTrue(volatilities instanceof NormalIborCapletFloorletVolatilities, "volatilities must be normal volatilities");
  }

}
