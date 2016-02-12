/**
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.pricer.bond;

import com.google.common.collect.ImmutableList;
import com.google.common.primitives.Doubles;
import com.opengamma.strata.collect.ArgChecker;
import com.opengamma.strata.collect.id.StandardId;
import com.opengamma.strata.collect.tuple.Pair;
import com.opengamma.strata.market.sensitivity.PointSensitivities;
import com.opengamma.strata.market.sensitivity.PointSensitivityBuilder;
import com.opengamma.strata.market.value.CompoundedRateType;
import com.opengamma.strata.pricer.rate.LegalEntityDiscountingProvider;
import com.opengamma.strata.product.bond.ResolvedBondFuture;
import com.opengamma.strata.product.bond.ResolvedFixedCouponBond;

/**
 * Pricer for for bond future products.
 * <p>
 * This function provides the ability to price a {@link ResolvedBondFuture}.
 */
public final class DiscountingBondFutureProductPricer extends AbstractBondFutureProductPricer {

  /**
   * Default implementation.
   */
  public static final DiscountingBondFutureProductPricer DEFAULT = new DiscountingBondFutureProductPricer(
      DiscountingFixedCouponBondProductPricer.DEFAULT);

  /**
   * Underlying pricer.
   */
  private final DiscountingFixedCouponBondProductPricer bondPricer;

  /**
   * Creates an instance. 
   * 
   * @param bondPricer  the pricer for {@link ResolvedFixedCouponBond}.
   */
  public DiscountingBondFutureProductPricer(DiscountingFixedCouponBondProductPricer bondPricer) {
    this.bondPricer = ArgChecker.notNull(bondPricer, "bondPricer");
  }

  //-------------------------------------------------------------------------
  /**
   * Calculates the price of the bond future product.
   * <p>
   * The price of the product is the price on the valuation date.
   * 
   * @param future  the future
   * @param provider  the rates provider
   * @return the price of the product, in decimal form
   */
  public double price(ResolvedBondFuture future, LegalEntityDiscountingProvider provider) {
    ImmutableList<Pair<ResolvedFixedCouponBond, StandardId>> basket = future.getDeliveryBasket();
    int size = basket.size();
    double[] priceBonds = new double[size];
    for (int i = 0; i < size; ++i) {
      Pair<ResolvedFixedCouponBond, StandardId> bondSec = basket.get(i);
      ResolvedFixedCouponBond bond = bondSec.getFirst();
      double dirtyPrice = bondPricer.dirtyPriceFromCurves(
          bond, bondSec.getSecond(), provider, future.getLastDeliveryDate());
      priceBonds[i] = bondPricer.cleanPriceFromDirtyPrice(
          bond, future.getLastDeliveryDate(), dirtyPrice) / future.getConversionFactor().get(i);
    }
    return Doubles.min(priceBonds);
  }

  /**
   * Calculates the price of the bond future product with z-spread.
   * <p>
   * The price of the product is the price on the valuation date.
   * <p>
   * The z-spread is a parallel shift applied to continuously compounded rates or periodic compounded rates 
   * of the issuer discounting curve. 
   * 
   * @param future  the future
   * @param provider  the rates provider
   * @param zSpread  the z-spread
   * @param compoundedRateType  the compounded rate type
   * @param periodPerYear  the number of periods per year
   * @return the price of the product, in decimal form
   */
  public double priceWithZSpread(
      ResolvedBondFuture future,
      LegalEntityDiscountingProvider provider,
      double zSpread,
      CompoundedRateType compoundedRateType,
      int periodPerYear) {

    ImmutableList<Pair<ResolvedFixedCouponBond, StandardId>> basket = future.getDeliveryBasket();
    int size = basket.size();
    double[] priceBonds = new double[size];
    for (int i = 0; i < size; ++i) {
      Pair<ResolvedFixedCouponBond, StandardId> bondSec = basket.get(i);
      ResolvedFixedCouponBond bond = bondSec.getFirst();
      double dirtyPrice = bondPricer.dirtyPriceFromCurvesWithZSpread(
          bond, bondSec.getSecond(), provider, zSpread, compoundedRateType, periodPerYear, future.getLastDeliveryDate());
      priceBonds[i] = bondPricer.cleanPriceFromDirtyPrice(
          bond, future.getLastDeliveryDate(), dirtyPrice) / future.getConversionFactor().get(i);
    }
    return Doubles.min(priceBonds);
  }

  //-------------------------------------------------------------------------
  /**
   * Calculates the price sensitivity of the bond future product.
   * <p>
   * The price sensitivity of the product is the sensitivity of the price to the underlying curves.
   * <p>
   * Note that the price sensitivity should be no currency. 
   * 
   * @param future  the future
   * @param provider  the rates provider
   * @return the price curve sensitivity of the product
   */
  public PointSensitivities priceSensitivity(ResolvedBondFuture future, LegalEntityDiscountingProvider provider) {
    ImmutableList<Pair<ResolvedFixedCouponBond, StandardId>> basket = future.getDeliveryBasket();
    int size = basket.size();
    double[] priceBonds = new double[size];
    int indexCTD = 0;
    double priceMin = 2d;
    for (int i = 0; i < size; i++) {
      Pair<ResolvedFixedCouponBond, StandardId> bondSec = basket.get(i);
      ResolvedFixedCouponBond bond = bondSec.getFirst();
      double dirtyPrice = bondPricer.dirtyPriceFromCurves(
          bond, bondSec.getSecond(), provider, future.getLastDeliveryDate());
      priceBonds[i] = bondPricer.cleanPriceFromDirtyPrice(
          bond, future.getLastDeliveryDate(), dirtyPrice) / future.getConversionFactor().get(i);
      if (priceBonds[i] < priceMin) {
        priceMin = priceBonds[i];
        indexCTD = i;
      }
    }
    Pair<ResolvedFixedCouponBond, StandardId> bondSec = basket.get(indexCTD);
    ResolvedFixedCouponBond bond = bondSec.getFirst();
    PointSensitivityBuilder pointSensi = bondPricer.dirtyPriceSensitivity(
        bond, bondSec.getSecond(), provider, future.getLastDeliveryDate());
    return pointSensi.multipliedBy(1d / future.getConversionFactor().get(indexCTD)).build();
  }

  /**
   * Calculates the price sensitivity of the bond future product with z-spread.
   * <p>
   * The price sensitivity of the product is the sensitivity of the price to the underlying curves.
   * <p>
   * The z-spread is a parallel shift applied to continuously compounded rates or periodic compounded rates 
   * of the issuer discounting curve. 
   * <p>
   * Note that the price sensitivity should be no currency. 
   * 
   * @param future  the future
   * @param provider  the rates provider
   * @param zSpread  the z-spread
   * @param compoundedRateType  the compounded rate type
   * @param periodPerYear  the number of periods per year
   * @return the price curve sensitivity of the product
   */
  public PointSensitivities priceSensitivityWithZSpread(
      ResolvedBondFuture future,
      LegalEntityDiscountingProvider provider,
      double zSpread,
      CompoundedRateType compoundedRateType,
      int periodPerYear) {

    ImmutableList<Pair<ResolvedFixedCouponBond, StandardId>> basket = future.getDeliveryBasket();
    int size = basket.size();
    double[] priceBonds = new double[size];
    int indexCTD = 0;
    double priceMin = 2d;
    for (int i = 0; i < size; i++) {
      Pair<ResolvedFixedCouponBond, StandardId> bondSec = basket.get(i);
      ResolvedFixedCouponBond bond = bondSec.getFirst();
      double dirtyPrice = bondPricer.dirtyPriceFromCurvesWithZSpread(
          bond, bondSec.getSecond(), provider, zSpread, compoundedRateType, periodPerYear, future.getLastDeliveryDate());
      priceBonds[i] = bondPricer.cleanPriceFromDirtyPrice(
          bond, future.getLastDeliveryDate(), dirtyPrice) / future.getConversionFactor().get(i);
      if (priceBonds[i] < priceMin) {
        priceMin = priceBonds[i];
        indexCTD = i;
      }
    }
    Pair<ResolvedFixedCouponBond, StandardId> bondSec = basket.get(indexCTD);
    ResolvedFixedCouponBond bond = bondSec.getFirst();
    PointSensitivityBuilder pointSensi = bondPricer.dirtyPriceSensitivityWithZspread(
        bond, bondSec.getSecond(), provider, zSpread, compoundedRateType, periodPerYear, future.getLastDeliveryDate());
    return pointSensi.multipliedBy(1d / future.getConversionFactor().get(indexCTD)).build();
  }

}
