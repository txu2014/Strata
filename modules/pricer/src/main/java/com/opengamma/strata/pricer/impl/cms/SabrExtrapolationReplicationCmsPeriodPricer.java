package com.opengamma.strata.pricer.impl.cms;

import com.opengamma.strata.collect.ArgChecker;
import com.opengamma.strata.pricer.swap.DiscountingSwapProductPricer;

public class SabrExtrapolationReplicationCmsPeriodPricer {

  
  private final DiscountingSwapProductPricer swapPricer;
  /**
   * The cut-off strike. The smile is extrapolated above that level.
   */
  private final double cutOffStrike;
  /**
   * The tail thickness parameter.
   */
  private final double mu;

  public SabrExtrapolationReplicationCmsPeriodPricer(DiscountingSwapProductPricer swapPricer, double cutOffStrike,
      double mu) {
    this.swapPricer = ArgChecker.notNull(swapPricer, "swapPricer");
    // TODO check
    this.cutOffStrike = cutOffStrike;
    this.mu = mu;
  }

}
