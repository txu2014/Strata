/**
 * Copyright (C) 2014 - present by OpenGamma Inc. and the OpenGamma group of companies
 * 
 * Please see distribution for license.
 */
package com.opengamma.strata.basics.index;

import java.io.Serializable;
import java.time.LocalDate;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Set;

import org.joda.beans.Bean;
import org.joda.beans.BeanDefinition;
import org.joda.beans.ImmutableBean;
import org.joda.beans.JodaBeanUtils;
import org.joda.beans.MetaProperty;
import org.joda.beans.Property;
import org.joda.beans.PropertyDefinition;
import org.joda.beans.impl.direct.DirectFieldsBeanBuilder;
import org.joda.beans.impl.direct.DirectMetaBean;
import org.joda.beans.impl.direct.DirectMetaProperty;
import org.joda.beans.impl.direct.DirectMetaPropertyMap;

import com.opengamma.strata.basics.currency.Currency;
import com.opengamma.strata.basics.date.DayCount;
import com.opengamma.strata.basics.date.HolidayCalendar;
import com.opengamma.strata.basics.date.Tenor;
import com.opengamma.strata.collect.ArgChecker;

/**
 * An overnight index, such as Sonia or Eonia.
 * <p>
 * An index represented by this class relates to lending over one night.
 * The rate typically refers to "Today/Tomorrow" but might refer to "Tomorrow/Next".
 * <p>
 * The index is defined by four dates.
 * The fixing date is the date on which the index is to be observed.
 * The publication date is the date on which the fixed rate is actually published.
 * The effective date is the date on which the implied deposit starts.
 * The maturity date is the date on which the implied deposit ends.
 */
@BeanDefinition
public final class ImmutableOvernightIndex
    implements OvernightIndex, ImmutableBean, Serializable {

  /**
   * The identifier, such as 'GBP-SONIA'.
   */
  @PropertyDefinition(validate = "notNull", overrideGet = true)
  private final OvernightIndexId id;
  /**
   * The currency of the index.
   */
  @PropertyDefinition(validate = "notNull", overrideGet = true)
  private final Currency currency;
  /**
   * The calendar that the index uses.
   * <p>
   * All dates are calculated with reference to the same calendar.
   */
  @PropertyDefinition(validate = "notNull", overrideGet = true)
  private final HolidayCalendar fixingCalendar;
  /**
   * The number of days to add to the fixing date to obtain the publication date.
   * <p>
   * In most cases, the fixing rate is available on the fixing date.
   * In a few cases, publication of the fixing rate is delayed until the following business day.
   * This property is zero if publication is on the fixing date, or one if it is the next day.
   */
  @PropertyDefinition(validate = "notNull", overrideGet = true)
  private final int publicationDateOffset;
  /**
   * The number of days to add to the fixing date to obtain the effective date.
   * <p>
   * In most cases, the settlement date and start of the implied deposit is on the fixing date.
   * In a few cases, the settlement date is the following business day.
   * This property is zero if settlement is on the fixing date, or one if it is the next day.
   * Maturity is always one business day after the settlement date.
   */
  @PropertyDefinition(validate = "notNull", overrideGet = true)
  private final int effectiveDateOffset;
  /**
   * The day count convention.
   */
  @PropertyDefinition(validate = "notNull", overrideGet = true)
  private final DayCount dayCount;

  //-------------------------------------------------------------------------
  /**
   * Gets the tenor of the index, which is always one day.
   * 
   * @return the one day tenor
   */
  @Override
  public Tenor getTenor() {
    return Tenor.TENOR_1D;
  }

  //-------------------------------------------------------------------------
  /**
   * Calculates the publication date from the fixing date.
   * <p>
   * The fixing date is the date on which the index is to be observed.
   * The publication date is the date on which the fixed rate is actually published.
   * <p>
   * No error is thrown if the input date is not a valid fixing date.
   * Instead, the fixing date is moved to the next valid fixing date and then processed.
   * 
   * @param fixingDate  the fixing date
   * @return the publication date
   */
  @Override
  public LocalDate calculatePublicationFromFixing(LocalDate fixingDate) {
    ArgChecker.notNull(fixingDate, "fixingDate");
    return fixingCalendar.shift(fixingCalendar.nextOrSame(fixingDate), publicationDateOffset);
  }

  /**
   * Calculates the effective date from the fixing date.
   * <p>
   * The fixing date is the date on which the index is to be observed.
   * The effective date is the date on which the implied deposit starts.
   * <p>
   * No error is thrown if the input date is not a valid fixing date.
   * Instead, the fixing date is moved to the next valid fixing date and then processed.
   * 
   * @param fixingDate  the fixing date
   * @return the effective date
   */
  @Override
  public LocalDate calculateEffectiveFromFixing(LocalDate fixingDate) {
    ArgChecker.notNull(fixingDate, "fixingDate");
    return fixingCalendar.shift(fixingCalendar.nextOrSame(fixingDate), effectiveDateOffset);
  }

  /**
   * Calculates the fixing date from the effective date.
   * <p>
   * The fixing date is the date on which the index is to be observed.
   * The effective date is the date on which the implied deposit starts.
   * <p>
   * No error is thrown if the input date is not a valid effective date.
   * Instead, the effective date is moved to the next valid effective date and then processed.
   * 
   * @param effectiveDate  the effective date
   * @return the fixing date
   */
  @Override
  public LocalDate calculateFixingFromEffective(LocalDate effectiveDate) {
    ArgChecker.notNull(effectiveDate, "effectiveDate");
    return fixingCalendar.shift(fixingCalendar.nextOrSame(effectiveDate), -effectiveDateOffset);
  }

  /**
   * Calculates the maturity date from the effective date.
   * <p>
   * The effective date is the date on which the implied deposit starts.
   * The maturity date is the date on which the implied deposit ends.
   * <p>
   * No error is thrown if the input date is not a valid effective date.
   * Instead, the effective date is moved to the next valid effective date and then processed.
   * 
   * @param effectiveDate  the effective date
   * @return the maturity date
   */
  @Override
  public LocalDate calculateMaturityFromEffective(LocalDate effectiveDate) {
    ArgChecker.notNull(effectiveDate, "effectiveDate");
    return fixingCalendar.shift(fixingCalendar.nextOrSame(effectiveDate), 1);
  }

  //-------------------------------------------------------------------------
  @Override
  public boolean equals(Object obj) {
    if (obj == this) {
      return true;
    }
    if (obj instanceof ImmutableOvernightIndex) {
      return id.equals(((ImmutableOvernightIndex) obj).id);
    }
    return false;
  }

  @Override
  public int hashCode() {
    return id.hashCode();
  }

  //-------------------------------------------------------------------------
  /**
   * Returns the name of the index.
   * 
   * @return the name of the index
   */
  @Override
  public String toString() {
    return getName();
  }

  //------------------------- AUTOGENERATED START -------------------------
  ///CLOVER:OFF
  /**
   * The meta-bean for {@code ImmutableOvernightIndex}.
   * @return the meta-bean, not null
   */
  public static ImmutableOvernightIndex.Meta meta() {
    return ImmutableOvernightIndex.Meta.INSTANCE;
  }

  static {
    JodaBeanUtils.registerMetaBean(ImmutableOvernightIndex.Meta.INSTANCE);
  }

  /**
   * The serialization version id.
   */
  private static final long serialVersionUID = 1L;

  /**
   * Returns a builder used to create an instance of the bean.
   * @return the builder, not null
   */
  public static ImmutableOvernightIndex.Builder builder() {
    return new ImmutableOvernightIndex.Builder();
  }

  private ImmutableOvernightIndex(
      OvernightIndexId id,
      Currency currency,
      HolidayCalendar fixingCalendar,
      int publicationDateOffset,
      int effectiveDateOffset,
      DayCount dayCount) {
    JodaBeanUtils.notNull(id, "id");
    JodaBeanUtils.notNull(currency, "currency");
    JodaBeanUtils.notNull(fixingCalendar, "fixingCalendar");
    JodaBeanUtils.notNull(publicationDateOffset, "publicationDateOffset");
    JodaBeanUtils.notNull(effectiveDateOffset, "effectiveDateOffset");
    JodaBeanUtils.notNull(dayCount, "dayCount");
    this.id = id;
    this.currency = currency;
    this.fixingCalendar = fixingCalendar;
    this.publicationDateOffset = publicationDateOffset;
    this.effectiveDateOffset = effectiveDateOffset;
    this.dayCount = dayCount;
  }

  @Override
  public ImmutableOvernightIndex.Meta metaBean() {
    return ImmutableOvernightIndex.Meta.INSTANCE;
  }

  @Override
  public <R> Property<R> property(String propertyName) {
    return metaBean().<R>metaProperty(propertyName).createProperty(this);
  }

  @Override
  public Set<String> propertyNames() {
    return metaBean().metaPropertyMap().keySet();
  }

  //-----------------------------------------------------------------------
  /**
   * Gets the identifier, such as 'GBP-SONIA'.
   * @return the value of the property, not null
   */
  @Override
  public OvernightIndexId getId() {
    return id;
  }

  //-----------------------------------------------------------------------
  /**
   * Gets the currency of the index.
   * @return the value of the property, not null
   */
  @Override
  public Currency getCurrency() {
    return currency;
  }

  //-----------------------------------------------------------------------
  /**
   * Gets the calendar that the index uses.
   * <p>
   * All dates are calculated with reference to the same calendar.
   * @return the value of the property, not null
   */
  @Override
  public HolidayCalendar getFixingCalendar() {
    return fixingCalendar;
  }

  //-----------------------------------------------------------------------
  /**
   * Gets the number of days to add to the fixing date to obtain the publication date.
   * <p>
   * In most cases, the fixing rate is available on the fixing date.
   * In a few cases, publication of the fixing rate is delayed until the following business day.
   * This property is zero if publication is on the fixing date, or one if it is the next day.
   * @return the value of the property, not null
   */
  @Override
  public int getPublicationDateOffset() {
    return publicationDateOffset;
  }

  //-----------------------------------------------------------------------
  /**
   * Gets the number of days to add to the fixing date to obtain the effective date.
   * <p>
   * In most cases, the settlement date and start of the implied deposit is on the fixing date.
   * In a few cases, the settlement date is the following business day.
   * This property is zero if settlement is on the fixing date, or one if it is the next day.
   * Maturity is always one business day after the settlement date.
   * @return the value of the property, not null
   */
  @Override
  public int getEffectiveDateOffset() {
    return effectiveDateOffset;
  }

  //-----------------------------------------------------------------------
  /**
   * Gets the day count convention.
   * @return the value of the property, not null
   */
  @Override
  public DayCount getDayCount() {
    return dayCount;
  }

  //-----------------------------------------------------------------------
  /**
   * Returns a builder that allows this bean to be mutated.
   * @return the mutable builder, not null
   */
  public Builder toBuilder() {
    return new Builder(this);
  }

  //-----------------------------------------------------------------------
  /**
   * The meta-bean for {@code ImmutableOvernightIndex}.
   */
  public static final class Meta extends DirectMetaBean {
    /**
     * The singleton instance of the meta-bean.
     */
    static final Meta INSTANCE = new Meta();

    /**
     * The meta-property for the {@code id} property.
     */
    private final MetaProperty<OvernightIndexId> id = DirectMetaProperty.ofImmutable(
        this, "id", ImmutableOvernightIndex.class, OvernightIndexId.class);
    /**
     * The meta-property for the {@code currency} property.
     */
    private final MetaProperty<Currency> currency = DirectMetaProperty.ofImmutable(
        this, "currency", ImmutableOvernightIndex.class, Currency.class);
    /**
     * The meta-property for the {@code fixingCalendar} property.
     */
    private final MetaProperty<HolidayCalendar> fixingCalendar = DirectMetaProperty.ofImmutable(
        this, "fixingCalendar", ImmutableOvernightIndex.class, HolidayCalendar.class);
    /**
     * The meta-property for the {@code publicationDateOffset} property.
     */
    private final MetaProperty<Integer> publicationDateOffset = DirectMetaProperty.ofImmutable(
        this, "publicationDateOffset", ImmutableOvernightIndex.class, Integer.TYPE);
    /**
     * The meta-property for the {@code effectiveDateOffset} property.
     */
    private final MetaProperty<Integer> effectiveDateOffset = DirectMetaProperty.ofImmutable(
        this, "effectiveDateOffset", ImmutableOvernightIndex.class, Integer.TYPE);
    /**
     * The meta-property for the {@code dayCount} property.
     */
    private final MetaProperty<DayCount> dayCount = DirectMetaProperty.ofImmutable(
        this, "dayCount", ImmutableOvernightIndex.class, DayCount.class);
    /**
     * The meta-properties.
     */
    private final Map<String, MetaProperty<?>> metaPropertyMap$ = new DirectMetaPropertyMap(
        this, null,
        "id",
        "currency",
        "fixingCalendar",
        "publicationDateOffset",
        "effectiveDateOffset",
        "dayCount");

    /**
     * Restricted constructor.
     */
    private Meta() {
    }

    @Override
    protected MetaProperty<?> metaPropertyGet(String propertyName) {
      switch (propertyName.hashCode()) {
        case 3355:  // id
          return id;
        case 575402001:  // currency
          return currency;
        case 394230283:  // fixingCalendar
          return fixingCalendar;
        case 1901198637:  // publicationDateOffset
          return publicationDateOffset;
        case 1571923688:  // effectiveDateOffset
          return effectiveDateOffset;
        case 1905311443:  // dayCount
          return dayCount;
      }
      return super.metaPropertyGet(propertyName);
    }

    @Override
    public ImmutableOvernightIndex.Builder builder() {
      return new ImmutableOvernightIndex.Builder();
    }

    @Override
    public Class<? extends ImmutableOvernightIndex> beanType() {
      return ImmutableOvernightIndex.class;
    }

    @Override
    public Map<String, MetaProperty<?>> metaPropertyMap() {
      return metaPropertyMap$;
    }

    //-----------------------------------------------------------------------
    /**
     * The meta-property for the {@code id} property.
     * @return the meta-property, not null
     */
    public MetaProperty<OvernightIndexId> id() {
      return id;
    }

    /**
     * The meta-property for the {@code currency} property.
     * @return the meta-property, not null
     */
    public MetaProperty<Currency> currency() {
      return currency;
    }

    /**
     * The meta-property for the {@code fixingCalendar} property.
     * @return the meta-property, not null
     */
    public MetaProperty<HolidayCalendar> fixingCalendar() {
      return fixingCalendar;
    }

    /**
     * The meta-property for the {@code publicationDateOffset} property.
     * @return the meta-property, not null
     */
    public MetaProperty<Integer> publicationDateOffset() {
      return publicationDateOffset;
    }

    /**
     * The meta-property for the {@code effectiveDateOffset} property.
     * @return the meta-property, not null
     */
    public MetaProperty<Integer> effectiveDateOffset() {
      return effectiveDateOffset;
    }

    /**
     * The meta-property for the {@code dayCount} property.
     * @return the meta-property, not null
     */
    public MetaProperty<DayCount> dayCount() {
      return dayCount;
    }

    //-----------------------------------------------------------------------
    @Override
    protected Object propertyGet(Bean bean, String propertyName, boolean quiet) {
      switch (propertyName.hashCode()) {
        case 3355:  // id
          return ((ImmutableOvernightIndex) bean).getId();
        case 575402001:  // currency
          return ((ImmutableOvernightIndex) bean).getCurrency();
        case 394230283:  // fixingCalendar
          return ((ImmutableOvernightIndex) bean).getFixingCalendar();
        case 1901198637:  // publicationDateOffset
          return ((ImmutableOvernightIndex) bean).getPublicationDateOffset();
        case 1571923688:  // effectiveDateOffset
          return ((ImmutableOvernightIndex) bean).getEffectiveDateOffset();
        case 1905311443:  // dayCount
          return ((ImmutableOvernightIndex) bean).getDayCount();
      }
      return super.propertyGet(bean, propertyName, quiet);
    }

    @Override
    protected void propertySet(Bean bean, String propertyName, Object newValue, boolean quiet) {
      metaProperty(propertyName);
      if (quiet) {
        return;
      }
      throw new UnsupportedOperationException("Property cannot be written: " + propertyName);
    }

  }

  //-----------------------------------------------------------------------
  /**
   * The bean-builder for {@code ImmutableOvernightIndex}.
   */
  public static final class Builder extends DirectFieldsBeanBuilder<ImmutableOvernightIndex> {

    private OvernightIndexId id;
    private Currency currency;
    private HolidayCalendar fixingCalendar;
    private int publicationDateOffset;
    private int effectiveDateOffset;
    private DayCount dayCount;

    /**
     * Restricted constructor.
     */
    private Builder() {
    }

    /**
     * Restricted copy constructor.
     * @param beanToCopy  the bean to copy from, not null
     */
    private Builder(ImmutableOvernightIndex beanToCopy) {
      this.id = beanToCopy.getId();
      this.currency = beanToCopy.getCurrency();
      this.fixingCalendar = beanToCopy.getFixingCalendar();
      this.publicationDateOffset = beanToCopy.getPublicationDateOffset();
      this.effectiveDateOffset = beanToCopy.getEffectiveDateOffset();
      this.dayCount = beanToCopy.getDayCount();
    }

    //-----------------------------------------------------------------------
    @Override
    public Object get(String propertyName) {
      switch (propertyName.hashCode()) {
        case 3355:  // id
          return id;
        case 575402001:  // currency
          return currency;
        case 394230283:  // fixingCalendar
          return fixingCalendar;
        case 1901198637:  // publicationDateOffset
          return publicationDateOffset;
        case 1571923688:  // effectiveDateOffset
          return effectiveDateOffset;
        case 1905311443:  // dayCount
          return dayCount;
        default:
          throw new NoSuchElementException("Unknown property: " + propertyName);
      }
    }

    @Override
    public Builder set(String propertyName, Object newValue) {
      switch (propertyName.hashCode()) {
        case 3355:  // id
          this.id = (OvernightIndexId) newValue;
          break;
        case 575402001:  // currency
          this.currency = (Currency) newValue;
          break;
        case 394230283:  // fixingCalendar
          this.fixingCalendar = (HolidayCalendar) newValue;
          break;
        case 1901198637:  // publicationDateOffset
          this.publicationDateOffset = (Integer) newValue;
          break;
        case 1571923688:  // effectiveDateOffset
          this.effectiveDateOffset = (Integer) newValue;
          break;
        case 1905311443:  // dayCount
          this.dayCount = (DayCount) newValue;
          break;
        default:
          throw new NoSuchElementException("Unknown property: " + propertyName);
      }
      return this;
    }

    @Override
    public Builder set(MetaProperty<?> property, Object value) {
      super.set(property, value);
      return this;
    }

    @Override
    public Builder setString(String propertyName, String value) {
      setString(meta().metaProperty(propertyName), value);
      return this;
    }

    @Override
    public Builder setString(MetaProperty<?> property, String value) {
      super.setString(property, value);
      return this;
    }

    @Override
    public Builder setAll(Map<String, ? extends Object> propertyValueMap) {
      super.setAll(propertyValueMap);
      return this;
    }

    @Override
    public ImmutableOvernightIndex build() {
      return new ImmutableOvernightIndex(
          id,
          currency,
          fixingCalendar,
          publicationDateOffset,
          effectiveDateOffset,
          dayCount);
    }

    //-----------------------------------------------------------------------
    /**
     * Sets the identifier, such as 'GBP-SONIA'.
     * @param id  the new value, not null
     * @return this, for chaining, not null
     */
    public Builder id(OvernightIndexId id) {
      JodaBeanUtils.notNull(id, "id");
      this.id = id;
      return this;
    }

    /**
     * Sets the currency of the index.
     * @param currency  the new value, not null
     * @return this, for chaining, not null
     */
    public Builder currency(Currency currency) {
      JodaBeanUtils.notNull(currency, "currency");
      this.currency = currency;
      return this;
    }

    /**
     * Sets the calendar that the index uses.
     * <p>
     * All dates are calculated with reference to the same calendar.
     * @param fixingCalendar  the new value, not null
     * @return this, for chaining, not null
     */
    public Builder fixingCalendar(HolidayCalendar fixingCalendar) {
      JodaBeanUtils.notNull(fixingCalendar, "fixingCalendar");
      this.fixingCalendar = fixingCalendar;
      return this;
    }

    /**
     * Sets the number of days to add to the fixing date to obtain the publication date.
     * <p>
     * In most cases, the fixing rate is available on the fixing date.
     * In a few cases, publication of the fixing rate is delayed until the following business day.
     * This property is zero if publication is on the fixing date, or one if it is the next day.
     * @param publicationDateOffset  the new value, not null
     * @return this, for chaining, not null
     */
    public Builder publicationDateOffset(int publicationDateOffset) {
      JodaBeanUtils.notNull(publicationDateOffset, "publicationDateOffset");
      this.publicationDateOffset = publicationDateOffset;
      return this;
    }

    /**
     * Sets the number of days to add to the fixing date to obtain the effective date.
     * <p>
     * In most cases, the settlement date and start of the implied deposit is on the fixing date.
     * In a few cases, the settlement date is the following business day.
     * This property is zero if settlement is on the fixing date, or one if it is the next day.
     * Maturity is always one business day after the settlement date.
     * @param effectiveDateOffset  the new value, not null
     * @return this, for chaining, not null
     */
    public Builder effectiveDateOffset(int effectiveDateOffset) {
      JodaBeanUtils.notNull(effectiveDateOffset, "effectiveDateOffset");
      this.effectiveDateOffset = effectiveDateOffset;
      return this;
    }

    /**
     * Sets the day count convention.
     * @param dayCount  the new value, not null
     * @return this, for chaining, not null
     */
    public Builder dayCount(DayCount dayCount) {
      JodaBeanUtils.notNull(dayCount, "dayCount");
      this.dayCount = dayCount;
      return this;
    }

    //-----------------------------------------------------------------------
    @Override
    public String toString() {
      StringBuilder buf = new StringBuilder(224);
      buf.append("ImmutableOvernightIndex.Builder{");
      buf.append("id").append('=').append(JodaBeanUtils.toString(id)).append(',').append(' ');
      buf.append("currency").append('=').append(JodaBeanUtils.toString(currency)).append(',').append(' ');
      buf.append("fixingCalendar").append('=').append(JodaBeanUtils.toString(fixingCalendar)).append(',').append(' ');
      buf.append("publicationDateOffset").append('=').append(JodaBeanUtils.toString(publicationDateOffset)).append(',').append(' ');
      buf.append("effectiveDateOffset").append('=').append(JodaBeanUtils.toString(effectiveDateOffset)).append(',').append(' ');
      buf.append("dayCount").append('=').append(JodaBeanUtils.toString(dayCount));
      buf.append('}');
      return buf.toString();
    }

  }

  ///CLOVER:ON
  //-------------------------- AUTOGENERATED END --------------------------
}
