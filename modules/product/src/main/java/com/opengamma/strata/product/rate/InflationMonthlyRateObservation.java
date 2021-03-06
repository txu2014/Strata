/**
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.product.rate;

import java.io.Serializable;
import java.time.YearMonth;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Set;

import org.joda.beans.Bean;
import org.joda.beans.BeanDefinition;
import org.joda.beans.ImmutableBean;
import org.joda.beans.ImmutableValidator;
import org.joda.beans.JodaBeanUtils;
import org.joda.beans.MetaProperty;
import org.joda.beans.Property;
import org.joda.beans.PropertyDefinition;
import org.joda.beans.impl.direct.DirectFieldsBeanBuilder;
import org.joda.beans.impl.direct.DirectMetaBean;
import org.joda.beans.impl.direct.DirectMetaProperty;
import org.joda.beans.impl.direct.DirectMetaPropertyMap;

import com.google.common.collect.ImmutableSet;
import com.opengamma.strata.basics.index.Index;
import com.opengamma.strata.basics.index.PriceIndex;
import com.opengamma.strata.collect.ArgChecker;

/**
 * Defines the observation of inflation figures from a price index.
 * <p>
 * A price index is typically published monthly and has a delay before publication.
 * The rate observed by this instance will be based on two observations of the index,
 * one relative to the accrual start date and one relative to the accrual end date.
 */
@BeanDefinition
public final class InflationMonthlyRateObservation
    implements RateObservation, ImmutableBean, Serializable {

  /**
   * The index of prices.
   * <p>
   * The pay-off is computed based on this index
   */
  @PropertyDefinition(validate = "notNull")
  private final PriceIndex index;
  /**
   * The reference month for the index relative to the accrual start date.
   * <p>
   * The reference month is typically three months before the accrual start date.
   */
  @PropertyDefinition(validate = "notNull")
  private final YearMonth referenceStartMonth;
  /**
   * The reference month for the index relative to the accrual end date.
   * <p>
   * The reference month is typically three months before the accrual end date.
   * Must be after the reference start month.
   */
  @PropertyDefinition(validate = "notNull")
  private final YearMonth referenceEndMonth;

  //-------------------------------------------------------------------------
  /**
   * Creates an {@code InflationMonthlyRateObservation} from an index, reference
   * start month and reference end month.
   * 
   * @param index  the index
   * @param referenceStartMonth  the reference start month
   * @param referenceEndMonth  the reference end month
   * @return the inflation rate observation
   */
  public static InflationMonthlyRateObservation of(
      PriceIndex index,
      YearMonth referenceStartMonth,
      YearMonth referenceEndMonth) {

    return InflationMonthlyRateObservation.builder()
        .index(index)
        .referenceStartMonth(referenceStartMonth)
        .referenceEndMonth(referenceEndMonth)
        .build();
  }

  @ImmutableValidator
  private void validate() {
    ArgChecker.inOrderNotEqual(referenceStartMonth, referenceEndMonth, "referenceStartMonth", "referenceEndMonth");
  }

  //-------------------------------------------------------------------------
  @Override
  public void collectIndices(ImmutableSet.Builder<Index> builder) {
    builder.add(index);
  }

  //------------------------- AUTOGENERATED START -------------------------
  ///CLOVER:OFF
  /**
   * The meta-bean for {@code InflationMonthlyRateObservation}.
   * @return the meta-bean, not null
   */
  public static InflationMonthlyRateObservation.Meta meta() {
    return InflationMonthlyRateObservation.Meta.INSTANCE;
  }

  static {
    JodaBeanUtils.registerMetaBean(InflationMonthlyRateObservation.Meta.INSTANCE);
  }

  /**
   * The serialization version id.
   */
  private static final long serialVersionUID = 1L;

  /**
   * Returns a builder used to create an instance of the bean.
   * @return the builder, not null
   */
  public static InflationMonthlyRateObservation.Builder builder() {
    return new InflationMonthlyRateObservation.Builder();
  }

  private InflationMonthlyRateObservation(
      PriceIndex index,
      YearMonth referenceStartMonth,
      YearMonth referenceEndMonth) {
    JodaBeanUtils.notNull(index, "index");
    JodaBeanUtils.notNull(referenceStartMonth, "referenceStartMonth");
    JodaBeanUtils.notNull(referenceEndMonth, "referenceEndMonth");
    this.index = index;
    this.referenceStartMonth = referenceStartMonth;
    this.referenceEndMonth = referenceEndMonth;
    validate();
  }

  @Override
  public InflationMonthlyRateObservation.Meta metaBean() {
    return InflationMonthlyRateObservation.Meta.INSTANCE;
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
   * Gets the index of prices.
   * <p>
   * The pay-off is computed based on this index
   * @return the value of the property, not null
   */
  public PriceIndex getIndex() {
    return index;
  }

  //-----------------------------------------------------------------------
  /**
   * Gets the reference month for the index relative to the accrual start date.
   * <p>
   * The reference month is typically three months before the accrual start date.
   * @return the value of the property, not null
   */
  public YearMonth getReferenceStartMonth() {
    return referenceStartMonth;
  }

  //-----------------------------------------------------------------------
  /**
   * Gets the reference month for the index relative to the accrual end date.
   * <p>
   * The reference month is typically three months before the accrual end date.
   * Must be after the reference start month.
   * @return the value of the property, not null
   */
  public YearMonth getReferenceEndMonth() {
    return referenceEndMonth;
  }

  //-----------------------------------------------------------------------
  /**
   * Returns a builder that allows this bean to be mutated.
   * @return the mutable builder, not null
   */
  public Builder toBuilder() {
    return new Builder(this);
  }

  @Override
  public boolean equals(Object obj) {
    if (obj == this) {
      return true;
    }
    if (obj != null && obj.getClass() == this.getClass()) {
      InflationMonthlyRateObservation other = (InflationMonthlyRateObservation) obj;
      return JodaBeanUtils.equal(index, other.index) &&
          JodaBeanUtils.equal(referenceStartMonth, other.referenceStartMonth) &&
          JodaBeanUtils.equal(referenceEndMonth, other.referenceEndMonth);
    }
    return false;
  }

  @Override
  public int hashCode() {
    int hash = getClass().hashCode();
    hash = hash * 31 + JodaBeanUtils.hashCode(index);
    hash = hash * 31 + JodaBeanUtils.hashCode(referenceStartMonth);
    hash = hash * 31 + JodaBeanUtils.hashCode(referenceEndMonth);
    return hash;
  }

  @Override
  public String toString() {
    StringBuilder buf = new StringBuilder(128);
    buf.append("InflationMonthlyRateObservation{");
    buf.append("index").append('=').append(index).append(',').append(' ');
    buf.append("referenceStartMonth").append('=').append(referenceStartMonth).append(',').append(' ');
    buf.append("referenceEndMonth").append('=').append(JodaBeanUtils.toString(referenceEndMonth));
    buf.append('}');
    return buf.toString();
  }

  //-----------------------------------------------------------------------
  /**
   * The meta-bean for {@code InflationMonthlyRateObservation}.
   */
  public static final class Meta extends DirectMetaBean {
    /**
     * The singleton instance of the meta-bean.
     */
    static final Meta INSTANCE = new Meta();

    /**
     * The meta-property for the {@code index} property.
     */
    private final MetaProperty<PriceIndex> index = DirectMetaProperty.ofImmutable(
        this, "index", InflationMonthlyRateObservation.class, PriceIndex.class);
    /**
     * The meta-property for the {@code referenceStartMonth} property.
     */
    private final MetaProperty<YearMonth> referenceStartMonth = DirectMetaProperty.ofImmutable(
        this, "referenceStartMonth", InflationMonthlyRateObservation.class, YearMonth.class);
    /**
     * The meta-property for the {@code referenceEndMonth} property.
     */
    private final MetaProperty<YearMonth> referenceEndMonth = DirectMetaProperty.ofImmutable(
        this, "referenceEndMonth", InflationMonthlyRateObservation.class, YearMonth.class);
    /**
     * The meta-properties.
     */
    private final Map<String, MetaProperty<?>> metaPropertyMap$ = new DirectMetaPropertyMap(
        this, null,
        "index",
        "referenceStartMonth",
        "referenceEndMonth");

    /**
     * Restricted constructor.
     */
    private Meta() {
    }

    @Override
    protected MetaProperty<?> metaPropertyGet(String propertyName) {
      switch (propertyName.hashCode()) {
        case 100346066:  // index
          return index;
        case -1306094359:  // referenceStartMonth
          return referenceStartMonth;
        case 1861034704:  // referenceEndMonth
          return referenceEndMonth;
      }
      return super.metaPropertyGet(propertyName);
    }

    @Override
    public InflationMonthlyRateObservation.Builder builder() {
      return new InflationMonthlyRateObservation.Builder();
    }

    @Override
    public Class<? extends InflationMonthlyRateObservation> beanType() {
      return InflationMonthlyRateObservation.class;
    }

    @Override
    public Map<String, MetaProperty<?>> metaPropertyMap() {
      return metaPropertyMap$;
    }

    //-----------------------------------------------------------------------
    /**
     * The meta-property for the {@code index} property.
     * @return the meta-property, not null
     */
    public MetaProperty<PriceIndex> index() {
      return index;
    }

    /**
     * The meta-property for the {@code referenceStartMonth} property.
     * @return the meta-property, not null
     */
    public MetaProperty<YearMonth> referenceStartMonth() {
      return referenceStartMonth;
    }

    /**
     * The meta-property for the {@code referenceEndMonth} property.
     * @return the meta-property, not null
     */
    public MetaProperty<YearMonth> referenceEndMonth() {
      return referenceEndMonth;
    }

    //-----------------------------------------------------------------------
    @Override
    protected Object propertyGet(Bean bean, String propertyName, boolean quiet) {
      switch (propertyName.hashCode()) {
        case 100346066:  // index
          return ((InflationMonthlyRateObservation) bean).getIndex();
        case -1306094359:  // referenceStartMonth
          return ((InflationMonthlyRateObservation) bean).getReferenceStartMonth();
        case 1861034704:  // referenceEndMonth
          return ((InflationMonthlyRateObservation) bean).getReferenceEndMonth();
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
   * The bean-builder for {@code InflationMonthlyRateObservation}.
   */
  public static final class Builder extends DirectFieldsBeanBuilder<InflationMonthlyRateObservation> {

    private PriceIndex index;
    private YearMonth referenceStartMonth;
    private YearMonth referenceEndMonth;

    /**
     * Restricted constructor.
     */
    private Builder() {
    }

    /**
     * Restricted copy constructor.
     * @param beanToCopy  the bean to copy from, not null
     */
    private Builder(InflationMonthlyRateObservation beanToCopy) {
      this.index = beanToCopy.getIndex();
      this.referenceStartMonth = beanToCopy.getReferenceStartMonth();
      this.referenceEndMonth = beanToCopy.getReferenceEndMonth();
    }

    //-----------------------------------------------------------------------
    @Override
    public Object get(String propertyName) {
      switch (propertyName.hashCode()) {
        case 100346066:  // index
          return index;
        case -1306094359:  // referenceStartMonth
          return referenceStartMonth;
        case 1861034704:  // referenceEndMonth
          return referenceEndMonth;
        default:
          throw new NoSuchElementException("Unknown property: " + propertyName);
      }
    }

    @Override
    public Builder set(String propertyName, Object newValue) {
      switch (propertyName.hashCode()) {
        case 100346066:  // index
          this.index = (PriceIndex) newValue;
          break;
        case -1306094359:  // referenceStartMonth
          this.referenceStartMonth = (YearMonth) newValue;
          break;
        case 1861034704:  // referenceEndMonth
          this.referenceEndMonth = (YearMonth) newValue;
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
    public InflationMonthlyRateObservation build() {
      return new InflationMonthlyRateObservation(
          index,
          referenceStartMonth,
          referenceEndMonth);
    }

    //-----------------------------------------------------------------------
    /**
     * Sets the index of prices.
     * <p>
     * The pay-off is computed based on this index
     * @param index  the new value, not null
     * @return this, for chaining, not null
     */
    public Builder index(PriceIndex index) {
      JodaBeanUtils.notNull(index, "index");
      this.index = index;
      return this;
    }

    /**
     * Sets the reference month for the index relative to the accrual start date.
     * <p>
     * The reference month is typically three months before the accrual start date.
     * @param referenceStartMonth  the new value, not null
     * @return this, for chaining, not null
     */
    public Builder referenceStartMonth(YearMonth referenceStartMonth) {
      JodaBeanUtils.notNull(referenceStartMonth, "referenceStartMonth");
      this.referenceStartMonth = referenceStartMonth;
      return this;
    }

    /**
     * Sets the reference month for the index relative to the accrual end date.
     * <p>
     * The reference month is typically three months before the accrual end date.
     * Must be after the reference start month.
     * @param referenceEndMonth  the new value, not null
     * @return this, for chaining, not null
     */
    public Builder referenceEndMonth(YearMonth referenceEndMonth) {
      JodaBeanUtils.notNull(referenceEndMonth, "referenceEndMonth");
      this.referenceEndMonth = referenceEndMonth;
      return this;
    }

    //-----------------------------------------------------------------------
    @Override
    public String toString() {
      StringBuilder buf = new StringBuilder(128);
      buf.append("InflationMonthlyRateObservation.Builder{");
      buf.append("index").append('=').append(JodaBeanUtils.toString(index)).append(',').append(' ');
      buf.append("referenceStartMonth").append('=').append(JodaBeanUtils.toString(referenceStartMonth)).append(',').append(' ');
      buf.append("referenceEndMonth").append('=').append(JodaBeanUtils.toString(referenceEndMonth));
      buf.append('}');
      return buf.toString();
    }

  }

  ///CLOVER:ON
  //-------------------------- AUTOGENERATED END --------------------------
}
