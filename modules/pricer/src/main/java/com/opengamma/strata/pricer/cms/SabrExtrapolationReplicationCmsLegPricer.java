package com.opengamma.strata.pricer.cms;

import java.util.stream.Collectors;

import com.opengamma.strata.basics.currency.CurrencyAmount;
import com.opengamma.strata.collect.ArgChecker;
import com.opengamma.strata.market.sensitivity.PointSensitivityBuilder;
import com.opengamma.strata.pricer.impl.cms.SabrExtrapolationReplicationCmsPeriodPricer;
import com.opengamma.strata.pricer.rate.RatesProvider;
import com.opengamma.strata.pricer.swaption.SabrSwaptionVolatilities;
import com.opengamma.strata.product.cms.CmsLeg;
import com.opengamma.strata.product.cms.CmsPeriod;
import com.opengamma.strata.product.cms.ExpandedCmsLeg;

public class SabrExtrapolationReplicationCmsLegPricer {
  
  /**
   * The pricer for {@link CmsPeriod}.
   */
  private final SabrExtrapolationReplicationCmsPeriodPricer cmsPeriodPricer;

  public SabrExtrapolationReplicationCmsLegPricer(SabrExtrapolationReplicationCmsPeriodPricer cmsPeriodPricer) {
    this.cmsPeriodPricer = ArgChecker.notNull(cmsPeriodPricer, "cmsPeriodPricer");
  }

  public CurrencyAmount presentValue(
      CmsLeg cmsLeg,
      RatesProvider ratesProvider,
      SabrSwaptionVolatilities swaptionVolatilities) {
    ExpandedCmsLeg expand = cmsLeg.expand();
    double pvTotal = expand
        .getCmsPeriods()
        .stream()
        .map(cmsPeriod -> cmsPeriodPricer.presentValue(cmsPeriod, ratesProvider, swaptionVolatilities))
        .collect(Collectors.summingDouble(CurrencyAmount::getAmount));
    return CurrencyAmount.of(cmsLeg.getCurrency(), pvTotal);
  }

  public PointSensitivityBuilder presentValueSensitivity(
      CmsLeg cmsLeg,
      RatesProvider ratesProvider,
      SabrSwaptionVolatilities swaptionVolatilities) {
    ExpandedCmsLeg expand = cmsLeg.expand();
    PointSensitivityBuilder point = PointSensitivityBuilder.none();
    for (CmsPeriod cmsPeriod : expand.getCmsPeriods()) {
      point = point.combinedWith(
          cmsPeriodPricer.presentValueSensitivity(cmsPeriod, ratesProvider, swaptionVolatilities));
    }
    return point;
  }

  // TODO extend sabr sensitivity
  //  public SwaptionSabrSensitivity presentValueSensitivitySabr(
  //      CmsLeg cmsLeg,
  //      RatesProvider ratesProvider,
  //      SabrSwaptionVolatilities swaptionVolatilities) {
  //    ExpandedCmsLeg expand = cmsLeg.expand();
  //    SwaptionSabrSensitivity point = SwaptionSabrSensitivity.none();
  //    for (CmsPeriod cmsPeriod : expand.getCmsPeriods()) {
  //      point = point.combinedWith(
  //          cmsPeriodPricer.presentValueSensitivitySabr(cmsPeriod, ratesProvider, swaptionVolatilities));
  //    }
  //    return point;
  //  }

  public double presentValueSensitivityStrike(
      CmsLeg cmsLeg,
      RatesProvider ratesProvider,
      SabrSwaptionVolatilities swaptionVolatilities) {
    ExpandedCmsLeg expand = cmsLeg.expand();
    return expand
        .getCmsPeriods()
        .stream()
        .map(cmsPeriod -> cmsPeriodPricer.presentValueSensitivityStrike(cmsPeriod, ratesProvider, swaptionVolatilities))
        .collect(Collectors.summingDouble(Double::doubleValue));
  }
}
