/**
 * Copyright (C) 2014 - present by OpenGamma Inc. and the OpenGamma group of companies
 * 
 * Please see distribution for license.
 */
package com.opengamma.strata.basics.index;

import java.time.LocalDate;

import org.joda.convert.FromString;
import org.joda.convert.ToString;

import com.opengamma.strata.basics.currency.CurrencyPair;
import com.opengamma.strata.basics.date.HolidayCalendar;
import com.opengamma.strata.collect.ArgChecker;
import com.opengamma.strata.collect.named.ExtendedEnum;
import com.opengamma.strata.collect.named.Named;

/**
 * An index of foreign exchange rates.
 * <p>
 * An FX rate is the conversion rate between two currencies.
 * An FX index is the rate as published by a specific organization, typically
 * at a well-known time-of-day.
 * <p>
 * The index is defined by two dates.
 * The fixing date is the date on which the index is to be observed.
 * The maturity date is the date on which delivery of the implied exchange occurs.
 * <p>
 * The most common implementations are provided in {@link FxIndices}.
 * <p>
 * All implementations of this interface must be immutable and thread-safe.
 */
public interface FxIndex
    extends Index, Named {

  /**
   * Obtains an instance from the specified unique name.
   * 
   * @param uniqueName  the unique name
   * @return the index
   * @throws IllegalArgumentException if the name is not known
   */
  @FromString
  public static FxIndex of(String uniqueName) {
    ArgChecker.notNull(uniqueName, "uniqueName");
    return extendedEnum().lookup(uniqueName);
  }

  /**
   * Gets the extended enum helper.
   * <p>
   * This helper allows instances of the index to be looked up.
   * It also provides the complete set of available instances.
   * 
   * @return the extended enum helper
   */
  public static ExtendedEnum<FxIndex> extendedEnum() {
    return FxIndices.ENUM_LOOKUP;
  }

  //-------------------------------------------------------------------------
  /**
   * Gets the currency pair of the index.
   * 
   * @return the currency pair of the index
   */
  public abstract CurrencyPair getCurrencyPair();

  /**
   * Gets the calendar that determines which dates are fixing dates.
   * <p>
   * The rate will be fixed on each business day in this calendar.
   * 
   * @return the calendar used to determine the fixing dates of the index
   */
  public abstract HolidayCalendar getFixingCalendar();

  //-------------------------------------------------------------------------
  /**
   * Calculates the maturity date from the fixing date.
   * <p>
   * The fixing date is the date on which the index is to be observed.
   * The maturity date is the date on which the implied amount is delivered/exchanged.
   * The maturity date is typically two days after the fixing date.
   * <p>
   * No error is thrown if the input date is not a valid fixing date.
   * Instead, the fixing date is moved to the next valid fixing date and then processed.
   * <p>
   * The maturity date is also known as the <i>value date</i>.
   * 
   * @param fixingDate  the fixing date
   * @return the maturity date
   */
  public abstract LocalDate calculateMaturityFromFixing(LocalDate fixingDate);

  /**
   * Calculates the fixing date from the maturity date.
   * <p>
   * The fixing date is the date on which the index is to be observed.
   * The maturity date is the date on which the implied amount is delivered/exchanged.
   * The maturity date is typically two days after the fixing date.
   * <p>
   * No error is thrown if the input date is not a valid effective date.
   * Instead, the effective date is moved to the next valid effective date and then processed.
   * <p>
   * The maturity date is also known as the <i>value date</i>.
   * 
   * @param maturityDate  the maturity date
   * @return the fixing date
   */
  public abstract LocalDate calculateFixingFromMaturity(LocalDate maturityDate);

  //-------------------------------------------------------------------------
  /**
   * Gets the name that uniquely identifies this index.
   * <p>
   * This name is used in serialization and can be parsed using {@link #of(String)}.
   * 
   * @return the unique name
   */
  @ToString
  @Override
  public abstract String getName();

}
