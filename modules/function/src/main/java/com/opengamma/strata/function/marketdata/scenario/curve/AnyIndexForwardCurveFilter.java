/**
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.function.marketdata.scenario.curve;

import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Set;

import org.joda.beans.BeanBuilder;
import org.joda.beans.BeanDefinition;
import org.joda.beans.ImmutableBean;
import org.joda.beans.JodaBeanUtils;
import org.joda.beans.MetaProperty;
import org.joda.beans.Property;
import org.joda.beans.impl.direct.DirectFieldsBeanBuilder;
import org.joda.beans.impl.direct.DirectMetaBean;
import org.joda.beans.impl.direct.DirectMetaPropertyMap;

import com.opengamma.strata.basics.market.MarketDataBox;
import com.opengamma.strata.calc.marketdata.scenario.MarketDataFilter;
import com.opengamma.strata.market.curve.Curve;
import com.opengamma.strata.market.id.IndexCurveId;

/**
 * A market data filter that matches any forward curve for an index.
 * <p>
 * The {@link #matches} method always returns true.
 */
@BeanDefinition(builderScope = "private")
public final class AnyIndexForwardCurveFilter
    implements MarketDataFilter<Curve, IndexCurveId>, ImmutableBean {

  /**
   * The single shared instance.
   */
  public static final AnyIndexForwardCurveFilter INSTANCE = new AnyIndexForwardCurveFilter();

  //-------------------------------------------------------------------------

  @Override
  public Class<?> getMarketDataIdType() {
    return IndexCurveId.class;
  }

  @Override
  public boolean matches(IndexCurveId marketDataId, MarketDataBox<Curve> marketData) {
    return true;
  }

  //------------------------- AUTOGENERATED START -------------------------
  ///CLOVER:OFF
  /**
   * The meta-bean for {@code AnyIndexForwardCurveFilter}.
   * @return the meta-bean, not null
   */
  public static AnyIndexForwardCurveFilter.Meta meta() {
    return AnyIndexForwardCurveFilter.Meta.INSTANCE;
  }

  static {
    JodaBeanUtils.registerMetaBean(AnyIndexForwardCurveFilter.Meta.INSTANCE);
  }

  private AnyIndexForwardCurveFilter() {
  }

  @Override
  public AnyIndexForwardCurveFilter.Meta metaBean() {
    return AnyIndexForwardCurveFilter.Meta.INSTANCE;
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
  @Override
  public boolean equals(Object obj) {
    if (obj == this) {
      return true;
    }
    if (obj != null && obj.getClass() == this.getClass()) {
      return true;
    }
    return false;
  }

  @Override
  public int hashCode() {
    int hash = getClass().hashCode();
    return hash;
  }

  @Override
  public String toString() {
    StringBuilder buf = new StringBuilder(32);
    buf.append("AnyIndexForwardCurveFilter{");
    buf.append('}');
    return buf.toString();
  }

  //-----------------------------------------------------------------------
  /**
   * The meta-bean for {@code AnyIndexForwardCurveFilter}.
   */
  public static final class Meta extends DirectMetaBean {
    /**
     * The singleton instance of the meta-bean.
     */
    static final Meta INSTANCE = new Meta();

    /**
     * The meta-properties.
     */
    private final Map<String, MetaProperty<?>> metaPropertyMap$ = new DirectMetaPropertyMap(
        this, null);

    /**
     * Restricted constructor.
     */
    private Meta() {
    }

    @Override
    public BeanBuilder<? extends AnyIndexForwardCurveFilter> builder() {
      return new AnyIndexForwardCurveFilter.Builder();
    }

    @Override
    public Class<? extends AnyIndexForwardCurveFilter> beanType() {
      return AnyIndexForwardCurveFilter.class;
    }

    @Override
    public Map<String, MetaProperty<?>> metaPropertyMap() {
      return metaPropertyMap$;
    }

    //-----------------------------------------------------------------------
  }

  //-----------------------------------------------------------------------
  /**
   * The bean-builder for {@code AnyIndexForwardCurveFilter}.
   */
  private static final class Builder extends DirectFieldsBeanBuilder<AnyIndexForwardCurveFilter> {

    /**
     * Restricted constructor.
     */
    private Builder() {
    }

    //-----------------------------------------------------------------------
    @Override
    public Object get(String propertyName) {
      throw new NoSuchElementException("Unknown property: " + propertyName);
    }

    @Override
    public Builder set(String propertyName, Object newValue) {
      throw new NoSuchElementException("Unknown property: " + propertyName);
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
    public AnyIndexForwardCurveFilter build() {
      return new AnyIndexForwardCurveFilter();
    }

    //-----------------------------------------------------------------------
    @Override
    public String toString() {
      return "AnyIndexForwardCurveFilter.Builder{}";
    }

  }

  ///CLOVER:ON
  //-------------------------- AUTOGENERATED END --------------------------
}
