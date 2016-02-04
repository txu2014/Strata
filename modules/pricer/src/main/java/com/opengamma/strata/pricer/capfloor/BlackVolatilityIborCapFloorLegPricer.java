package com.opengamma.strata.pricer.capfloor;

import com.opengamma.strata.pricer.impl.capfloor.BlackIborCapletFloorletPeriodPricer;

public class BlackVolatilityIborCapFloorLegPricer
    extends VolatilityIborCapFloorLegPricer {

  /**
   * Default implementation. 
   */
  public static final BlackVolatilityIborCapFloorLegPricer DEFAULT =
      new BlackVolatilityIborCapFloorLegPricer(BlackIborCapletFloorletPeriodPricer.DEFAULT);

  public BlackVolatilityIborCapFloorLegPricer(BlackIborCapletFloorletPeriodPricer periodPricer) {
    super(periodPricer);
  }

}
