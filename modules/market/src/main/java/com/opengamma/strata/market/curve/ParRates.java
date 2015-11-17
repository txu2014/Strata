/**
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.market.curve;

import java.io.Serializable;
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

import com.google.common.collect.ImmutableMap;
import com.opengamma.strata.basics.market.MarketDataKey;

/**
 * The input data used when calibrating a curve.
 * <p>
 * This class contains the current market value of a set of instruments used when calibrating a curve.
 */
@BeanDefinition
public final class ParRates
    implements ImmutableBean, Serializable {

  /** The market data. */
  @PropertyDefinition(validate = "notNull", builderType = "Map<? extends MarketDataKey<?>, ?>")
  private final ImmutableMap<? extends MarketDataKey<?>, ?> rates;

  /** The metadata for the curve. */
  @PropertyDefinition(validate = "notNull")
  private final CurveMetadata curveMetadata;

  //-------------------------------------------------------------------------
  /**
   * Returns a {@code ParRates} instance containing the specified rates.
   *
   * @param rates  a map of par rates, keyed by the ID of the data
   * @param metadata  the metadata for the curve
   * @return a {@code ParRates} instance containing the specified rates
   */
  public static ParRates of(Map<? extends MarketDataKey<?>, ?> rates, CurveMetadata metadata) {
    return new ParRates(rates, metadata);
  }

  //------------------------- AUTOGENERATED START -------------------------
  ///CLOVER:OFF
  /**
   * The meta-bean for {@code ParRates}.
   * @return the meta-bean, not null
   */
  public static ParRates.Meta meta() {
    return ParRates.Meta.INSTANCE;
  }

  static {
    JodaBeanUtils.registerMetaBean(ParRates.Meta.INSTANCE);
  }

  /**
   * The serialization version id.
   */
  private static final long serialVersionUID = 1L;

  /**
   * Returns a builder used to create an instance of the bean.
   * @return the builder, not null
   */
  public static ParRates.Builder builder() {
    return new ParRates.Builder();
  }

  private ParRates(
      Map<? extends MarketDataKey<?>, ?> rates,
      CurveMetadata curveMetadata) {
    JodaBeanUtils.notNull(rates, "rates");
    JodaBeanUtils.notNull(curveMetadata, "curveMetadata");
    this.rates = ImmutableMap.copyOf(rates);
    this.curveMetadata = curveMetadata;
  }

  @Override
  public ParRates.Meta metaBean() {
    return ParRates.Meta.INSTANCE;
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
   * Gets the market data.
   * @return the value of the property, not null
   */
  public ImmutableMap<? extends MarketDataKey<?>, ?> getRates() {
    return rates;
  }

  //-----------------------------------------------------------------------
  /**
   * Gets the metadata for the curve.
   * @return the value of the property, not null
   */
  public CurveMetadata getCurveMetadata() {
    return curveMetadata;
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
      ParRates other = (ParRates) obj;
      return JodaBeanUtils.equal(rates, other.rates) &&
          JodaBeanUtils.equal(curveMetadata, other.curveMetadata);
    }
    return false;
  }

  @Override
  public int hashCode() {
    int hash = getClass().hashCode();
    hash = hash * 31 + JodaBeanUtils.hashCode(rates);
    hash = hash * 31 + JodaBeanUtils.hashCode(curveMetadata);
    return hash;
  }

  @Override
  public String toString() {
    StringBuilder buf = new StringBuilder(96);
    buf.append("ParRates{");
    buf.append("rates").append('=').append(rates).append(',').append(' ');
    buf.append("curveMetadata").append('=').append(JodaBeanUtils.toString(curveMetadata));
    buf.append('}');
    return buf.toString();
  }

  //-----------------------------------------------------------------------
  /**
   * The meta-bean for {@code ParRates}.
   */
  public static final class Meta extends DirectMetaBean {
    /**
     * The singleton instance of the meta-bean.
     */
    static final Meta INSTANCE = new Meta();

    /**
     * The meta-property for the {@code rates} property.
     */
    @SuppressWarnings({"unchecked", "rawtypes" })
    private final MetaProperty<ImmutableMap<? extends MarketDataKey<?>, ?>> rates = DirectMetaProperty.ofImmutable(
        this, "rates", ParRates.class, (Class) ImmutableMap.class);
    /**
     * The meta-property for the {@code curveMetadata} property.
     */
    private final MetaProperty<CurveMetadata> curveMetadata = DirectMetaProperty.ofImmutable(
        this, "curveMetadata", ParRates.class, CurveMetadata.class);
    /**
     * The meta-properties.
     */
    private final Map<String, MetaProperty<?>> metaPropertyMap$ = new DirectMetaPropertyMap(
        this, null,
        "rates",
        "curveMetadata");

    /**
     * Restricted constructor.
     */
    private Meta() {
    }

    @Override
    protected MetaProperty<?> metaPropertyGet(String propertyName) {
      switch (propertyName.hashCode()) {
        case 108285843:  // rates
          return rates;
        case 278233406:  // curveMetadata
          return curveMetadata;
      }
      return super.metaPropertyGet(propertyName);
    }

    @Override
    public ParRates.Builder builder() {
      return new ParRates.Builder();
    }

    @Override
    public Class<? extends ParRates> beanType() {
      return ParRates.class;
    }

    @Override
    public Map<String, MetaProperty<?>> metaPropertyMap() {
      return metaPropertyMap$;
    }

    //-----------------------------------------------------------------------
    /**
     * The meta-property for the {@code rates} property.
     * @return the meta-property, not null
     */
    public MetaProperty<ImmutableMap<? extends MarketDataKey<?>, ?>> rates() {
      return rates;
    }

    /**
     * The meta-property for the {@code curveMetadata} property.
     * @return the meta-property, not null
     */
    public MetaProperty<CurveMetadata> curveMetadata() {
      return curveMetadata;
    }

    //-----------------------------------------------------------------------
    @Override
    protected Object propertyGet(Bean bean, String propertyName, boolean quiet) {
      switch (propertyName.hashCode()) {
        case 108285843:  // rates
          return ((ParRates) bean).getRates();
        case 278233406:  // curveMetadata
          return ((ParRates) bean).getCurveMetadata();
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
   * The bean-builder for {@code ParRates}.
   */
  public static final class Builder extends DirectFieldsBeanBuilder<ParRates> {

    private Map<? extends MarketDataKey<?>, ?> rates = ImmutableMap.of();
    private CurveMetadata curveMetadata;

    /**
     * Restricted constructor.
     */
    private Builder() {
    }

    /**
     * Restricted copy constructor.
     * @param beanToCopy  the bean to copy from, not null
     */
    private Builder(ParRates beanToCopy) {
      this.rates = beanToCopy.getRates();
      this.curveMetadata = beanToCopy.getCurveMetadata();
    }

    //-----------------------------------------------------------------------
    @Override
    public Object get(String propertyName) {
      switch (propertyName.hashCode()) {
        case 108285843:  // rates
          return rates;
        case 278233406:  // curveMetadata
          return curveMetadata;
        default:
          throw new NoSuchElementException("Unknown property: " + propertyName);
      }
    }

    @SuppressWarnings("unchecked")
    @Override
    public Builder set(String propertyName, Object newValue) {
      switch (propertyName.hashCode()) {
        case 108285843:  // rates
          this.rates = (Map<? extends MarketDataKey<?>, ?>) newValue;
          break;
        case 278233406:  // curveMetadata
          this.curveMetadata = (CurveMetadata) newValue;
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
    public ParRates build() {
      return new ParRates(
          rates,
          curveMetadata);
    }

    //-----------------------------------------------------------------------
    /**
     * Sets the market data.
     * @param rates  the new value, not null
     * @return this, for chaining, not null
     */
    public Builder rates(Map<? extends MarketDataKey<?>, ?> rates) {
      JodaBeanUtils.notNull(rates, "rates");
      this.rates = rates;
      return this;
    }

    /**
     * Sets the metadata for the curve.
     * @param curveMetadata  the new value, not null
     * @return this, for chaining, not null
     */
    public Builder curveMetadata(CurveMetadata curveMetadata) {
      JodaBeanUtils.notNull(curveMetadata, "curveMetadata");
      this.curveMetadata = curveMetadata;
      return this;
    }

    //-----------------------------------------------------------------------
    @Override
    public String toString() {
      StringBuilder buf = new StringBuilder(96);
      buf.append("ParRates.Builder{");
      buf.append("rates").append('=').append(JodaBeanUtils.toString(rates)).append(',').append(' ');
      buf.append("curveMetadata").append('=').append(JodaBeanUtils.toString(curveMetadata));
      buf.append('}');
      return buf.toString();
    }

  }

  ///CLOVER:ON
  //-------------------------- AUTOGENERATED END --------------------------
}
