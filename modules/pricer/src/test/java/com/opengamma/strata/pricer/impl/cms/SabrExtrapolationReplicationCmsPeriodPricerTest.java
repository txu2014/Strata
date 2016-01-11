package com.opengamma.strata.pricer.impl.cms;

import static com.opengamma.strata.basics.currency.Currency.EUR;
import static com.opengamma.strata.basics.date.DayCounts.ACT_360;

import java.time.LocalDate;

import org.testng.annotations.Test;

import com.opengamma.strata.basics.currency.CurrencyAmount;
import com.opengamma.strata.market.curve.CurveCurrencyParameterSensitivities;
import com.opengamma.strata.market.sensitivity.PointSensitivityBuilder;
import com.opengamma.strata.market.sensitivity.SwaptionSabrSensitivity;
import com.opengamma.strata.pricer.rate.RatesProvider;
import com.opengamma.strata.pricer.swaption.SabrSwaptionVolatilities;
import com.opengamma.strata.pricer.swaption.SwaptionSabrRateVolatilityDataSet;
import com.opengamma.strata.product.cms.CmsPeriod;
import com.opengamma.strata.product.swap.SwapIndices;

/**
 * Test {@link SabrExtrapolationReplicationCmsPeriodPricer}.
 */
@Test(enabled = false)
public class SabrExtrapolationReplicationCmsPeriodPricerTest {

  private static final LocalDate VALUATION = LocalDate.of(2010, 8, 18);
  private static final LocalDate FIXING = LocalDate.of(2020, 4, 24);
  private static final LocalDate START = LocalDate.of(2020, 4, 28);
  private static final LocalDate END = LocalDate.of(2021, 4, 28);
  private static final LocalDate PAYMENT = LocalDate.of(2021, 4, 28);
  private static final double ACC_FACTOR = ACT_360.relativeYearFraction(START, END); //  1.0138888888888888;
  private static final double NOTIONAL = 10000000;
  private static final CmsPeriod COUPON = CmsPeriod.builder().dayCount(ACT_360).currency(EUR)
      .index(SwapIndices.EUR_EURIBOR_1100_5Y).startDate(START).endDate(END).fixingDate(FIXING).notional(NOTIONAL)
      .paymentDate(PAYMENT).yearFraction(ACC_FACTOR).build();
  private static final CmsPeriod CAPLET = CmsPeriod.builder().dayCount(ACT_360).currency(EUR)
      .index(SwapIndices.EUR_EURIBOR_1100_5Y).startDate(START).endDate(END).fixingDate(FIXING).notional(NOTIONAL)
      .paymentDate(PAYMENT).yearFraction(ACC_FACTOR).caplet(0.04).build();
  private static final CmsPeriod FLOORLET = CmsPeriod.builder().dayCount(ACT_360).currency(EUR)
      .index(SwapIndices.EUR_EURIBOR_1100_5Y).startDate(START).endDate(END).fixingDate(FIXING).notional(NOTIONAL)
      .paymentDate(PAYMENT).yearFraction(ACC_FACTOR).floorlet(0.04).build();

  private static final double CUT_OFF_STRIKE = 0.10;
  private static final double MU = 2.50;
  private static final SabrExtrapolationReplicationCmsPeriodPricer PRICER =
      SabrExtrapolationReplicationCmsPeriodPricer.of(CUT_OFF_STRIKE, MU, 1d);
  private static final RatesProvider RATES_PROVIDER = SwaptionSabrRateVolatilityDataSet.getRatesProviderEur(VALUATION);
  private static final SabrSwaptionVolatilities VOLATILITIES = SwaptionSabrRateVolatilityDataSet.getVolatilitiesEur(
      VALUATION, false);
  
  //small discrepancy due to difference in tenor computation and day count
  public void test() {
    CurrencyAmount pv = PRICER.presentValue(COUPON, RATES_PROVIDER, VOLATILITIES);
    System.out.println(pv + "\t" + 151876.37983598877 + "\t" + (pv.getAmount() - 151876.37983598877) /
        151876.37983598877);
    CurrencyAmount pvCaplet = PRICER.presentValue(CAPLET, RATES_PROVIDER, VOLATILITIES);
    System.out.println(pvCaplet + "\t" + 50123.76903753489 + "\t" + (pvCaplet.getAmount() - 50123.76903753489) /
        50123.76903753489);
    CurrencyAmount pvFloorlet = PRICER.presentValue(FLOORLET, RATES_PROVIDER, VOLATILITIES);
    System.out.println(pvFloorlet + "\t" + 278028.6909303699 + "\t" + (pvFloorlet.getAmount() - 278028.6909303699) /
        278028.6909303699);
  }

  public void testSensi() {
    PointSensitivityBuilder pvPoint = PRICER.presentValueSensitivity(COUPON, RATES_PROVIDER, VOLATILITIES);
    CurveCurrencyParameterSensitivities sensi = RATES_PROVIDER.curveParameterSensitivity(pvPoint.build());
    System.out.println(sensi.getSensitivities().get(0).getSensitivity());
    System.out.println(sensi.getSensitivities().get(1).getSensitivity());
    System.out
        .println((sensi.getSensitivities().get(0).getSensitivity().get(5) + 1587331.111365128) / 1587331.111365128);
    System.out
        .println((sensi.getSensitivities().get(1).getSensitivity().get(4) + 1200067.1024512479) / 1200067.1024512479);
    System.out
        .println((sensi.getSensitivities().get(1).getSensitivity().get(5) - 1.1008513287684895E7) / 1.1008513287684895E7);
    System.out.println("\n");
    //    {[EUR Dsc, EUR]= (0.0, 0.0, 0.0, 0.0, 0.0, -1587331.111365128) , 
    //     [EUR EURIBOR 6M, EUR]= (0.0, 0.0, 0.0, 0.0, -1200067.1024512479, 1.1008513287684895E7) }
    PointSensitivityBuilder pvCapletPoint = PRICER.presentValueSensitivity(CAPLET, RATES_PROVIDER, VOLATILITIES);
    CurveCurrencyParameterSensitivities sensiCaplet = RATES_PROVIDER.curveParameterSensitivity(pvCapletPoint.build());
    System.out.println(sensiCaplet.getSensitivities().get(0).getSensitivity());
    System.out.println(sensiCaplet.getSensitivities().get(1).getSensitivity());
    System.out
        .println((sensiCaplet.getSensitivities().get(0).getSensitivity().get(5) + 524231.0792054231) / 524231.0792054231);
    System.out
        .println((sensiCaplet.getSensitivities().get(1).getSensitivity().get(4) + 384153.78014323284) / 384153.78014323284);
    System.out
        .println((sensiCaplet.getSensitivities().get(1).getSensitivity().get(5) - 3523937.940289437) / 3523937.940289437);
    System.out.println("\n");
    //    {[EUR Dsc, EUR]= (0.0, 0.0, 0.0, 0.0, 0.0, -524231.0792054231) ,
    //        [EUR EURIBOR 6M, EUR]= (0.0, 0.0, 0.0, 0.0, -384153.78014323284, 3523937.940289437) }
    PointSensitivityBuilder pvFloorletPoint = PRICER.presentValueSensitivity(FLOORLET, RATES_PROVIDER, VOLATILITIES);
    CurveCurrencyParameterSensitivities sensiFloorlet = RATES_PROVIDER.curveParameterSensitivity(pvFloorletPoint
        .build());
    System.out.println(sensiFloorlet.getSensitivities().get(0).getSensitivity());
    System.out.println(sensiFloorlet.getSensitivities().get(1).getSensitivity());
    System.out
        .println((sensiFloorlet.getSensitivities().get(0).getSensitivity().get(5) + 3002338.118661252) / 3002338.118661252);
    System.out
        .println((sensiFloorlet.getSensitivities().get(1).getSensitivity().get(4) - 959096.0335566162) / 959096.0335566162);
    System.out
        .println((sensiFloorlet.getSensitivities().get(1).getSensitivity().get(5) + 8798025.8837258) / 8798025.8837258);
    System.out.println("\n");
    //    {[EUR Dsc, EUR]= (0.0, 0.0, 0.0, 0.0, 0.0, -3002338.118661252) , 
    //        [EUR EURIBOR 6M, EUR]= (0.0, 0.0, 0.0, 0.0, 959096.0335566162, -8798025.8837258) }
  }

  public void testSensiSabr() {
    SwaptionSabrSensitivity pvSabr = PRICER.presentValueSensitivitySabr(COUPON, RATES_PROVIDER, VOLATILITIES);
    //    {[9.684078149562094, 4.99814357362078]=676548.082462047}
    //    {[9.684078149562094, 4.99814357362078]=-113612.25889627806}
    //    {[9.684078149562094, 4.99814357362078]=114393.12936774408}
    //    {[9.684078149562094, 4.99814357362078]=38014.134518636296}
    System.out.println((pvSabr.getAlphaSensitivity() - 676548.082462047) / 676548.082462047);
    System.out.println((pvSabr.getBetaSensitivity() + 113612.25889627806) / 113612.25889627806);
    System.out.println((pvSabr.getNuSensitivity() - 114393.12936774408) / 114393.12936774408);
    System.out.println((pvSabr.getRhoSensitivity() - 38014.134518636296) / 38014.134518636296);
    SwaptionSabrSensitivity pvCaplePoint = PRICER.presentValueSensitivitySabr(CAPLET, RATES_PROVIDER, VOLATILITIES);
    //    {[9.684078149562094, 4.99814357362078]=1578794.160322719}
    //    {[9.684078149562094, 4.99814357362078]=-294972.16499438527}
    //    {[9.684078149562094, 4.99814357362078]=185534.1629758237}
    //    {[9.684078149562094, 4.99814357362078]=78852.70260646695}
    System.out.println((pvCaplePoint.getAlphaSensitivity() - 1578794.160322719) / 1578794.160322719);
    System.out.println((pvCaplePoint.getBetaSensitivity() + 294972.16499438527) / 294972.16499438527);
    System.out.println((pvCaplePoint.getNuSensitivity() - 185534.1629758237) / 185534.1629758237);
    System.out.println((pvCaplePoint.getRhoSensitivity() - 78852.70260646695) / 78852.70260646695);
    SwaptionSabrSensitivity pvFloorletPoint = PRICER
        .presentValueSensitivitySabr(FLOORLET, RATES_PROVIDER, VOLATILITIES);
    //    {[9.684078149562094, 4.99814357362078]=1191636.0883491992}
    //    {[9.684078149562094, 4.99814357362078]=-247505.01632522492}
    //    {[9.684078149562094, 4.99814357362078]=82398.67706855525}
    //    {[9.684078149562094, 4.99814357362078]=47334.55377989027}
    System.out.println((pvFloorletPoint.getAlphaSensitivity() - 1191636.0883491992) / 1191636.0883491992);
    System.out.println((pvFloorletPoint.getBetaSensitivity() + 247505.01632522492) / 247505.01632522492);
    System.out.println((pvFloorletPoint.getNuSensitivity() - 82398.67706855525) / 82398.67706855525);
    System.out.println((pvFloorletPoint.getRhoSensitivity() - 47334.55377989027) / 47334.55377989027);
    System.out.println("\n");
  }

  public void testStrike() {
    //    double pv = PRICER.presentValueSensitivityStrike(COUPON, RATES_PROVIDER, VOLATILITIES);
    //    System.out.println(pv);
    double pvCaplet = PRICER.presentValueSensitivityStrike(CAPLET, RATES_PROVIDER, VOLATILITIES);
    System.out.println(pvCaplet); // -786826.2013255818
    double pvFloorlet = PRICER.presentValueSensitivityStrike(FLOORLET, RATES_PROVIDER, VOLATILITIES);
    System.out.println(pvFloorlet); // 9776981.494443709
  }
}
