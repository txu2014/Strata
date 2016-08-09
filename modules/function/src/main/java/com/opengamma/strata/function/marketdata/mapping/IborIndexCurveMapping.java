/**
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.function.marketdata.mapping;

import java.io.Serializable;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Set;

import org.joda.beans.Bean;
import org.joda.beans.BeanBuilder;
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

import com.opengamma.strata.basics.market.MarketDataFeed;
import com.opengamma.strata.calc.marketdata.mapping.MarketDataMapping;
import com.opengamma.strata.market.curve.Curve;
import com.opengamma.strata.market.curve.CurveGroupName;
import com.opengamma.strata.market.id.IborIndexCurveId;
import com.opengamma.strata.market.key.IborIndexCurveKey;

/**
 * Market data mapping that accepts a {@link IborIndexCurveKey} and returns an {@link IborIndexCurveId}
 * with the name of the curve group that is the source of the curve.
 */
@BeanDefinition(builderScope = "private")
public final class IborIndexCurveMapping
    implements MarketDataMapping<Curve, IborIndexCurveKey>, ImmutableBean, Serializable {

  /**
   * The name of the curve group from which the curve should be taken.
   */
  @PropertyDefinition(validate = "notNull")
  private final CurveGroupName curveGroupName;
  /**
   * The market data feed used to source any quotes used to build the curve.
   */
  @PropertyDefinition(validate = "notNull")
  private final MarketDataFeed marketDataFeed;

  //-------------------------------------------------------------------------
  /**
   * Returns a mapping that accepts a {@link IborIndexCurveKey} and returns a {@link IborIndexCurveId}
   * with the name of the curve group that is the source of the curve.
   *
   * @param curveGroupName  the name of the curve group
   * @param marketDataFeed  the market data feed used to source any quotes used to build the curve
   * @return a curve ID with the name of the curve group which is the source of the curve
   */
  public static IborIndexCurveMapping of(CurveGroupName curveGroupName, MarketDataFeed marketDataFeed) {
    return new IborIndexCurveMapping(curveGroupName, marketDataFeed);
  }

  //-------------------------------------------------------------------------
  @Override
  public Class<IborIndexCurveKey> getMarketDataKeyType() {
    return IborIndexCurveKey.class;
  }

  @Override
  public IborIndexCurveId getIdForKey(IborIndexCurveKey key) {
    return IborIndexCurveId.of(key.getIndex(), curveGroupName, marketDataFeed);
  }

  //------------------------- AUTOGENERATED START -------------------------
  ///CLOVER:OFF
  /**
   * The meta-bean for {@code IborIndexCurveMapping}.
   * @return the meta-bean, not null
   */
  public static IborIndexCurveMapping.Meta meta() {
    return IborIndexCurveMapping.Meta.INSTANCE;
  }

  static {
    JodaBeanUtils.registerMetaBean(IborIndexCurveMapping.Meta.INSTANCE);
  }

  /**
   * The serialization version id.
   */
  private static final long serialVersionUID = 1L;

  private IborIndexCurveMapping(
      CurveGroupName curveGroupName,
      MarketDataFeed marketDataFeed) {
    JodaBeanUtils.notNull(curveGroupName, "curveGroupName");
    JodaBeanUtils.notNull(marketDataFeed, "marketDataFeed");
    this.curveGroupName = curveGroupName;
    this.marketDataFeed = marketDataFeed;
  }

  @Override
  public IborIndexCurveMapping.Meta metaBean() {
    return IborIndexCurveMapping.Meta.INSTANCE;
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
   * Gets the name of the curve group from which the curve should be taken.
   * @return the value of the property, not null
   */
  public CurveGroupName getCurveGroupName() {
    return curveGroupName;
  }

  //-----------------------------------------------------------------------
  /**
   * Gets the market data feed used to source any quotes used to build the curve.
   * @return the value of the property, not null
   */
  public MarketDataFeed getMarketDataFeed() {
    return marketDataFeed;
  }

  //-----------------------------------------------------------------------
  @Override
  public boolean equals(Object obj) {
    if (obj == this) {
      return true;
    }
    if (obj != null && obj.getClass() == this.getClass()) {
      IborIndexCurveMapping other = (IborIndexCurveMapping) obj;
      return JodaBeanUtils.equal(curveGroupName, other.curveGroupName) &&
          JodaBeanUtils.equal(marketDataFeed, other.marketDataFeed);
    }
    return false;
  }

  @Override
  public int hashCode() {
    int hash = getClass().hashCode();
    hash = hash * 31 + JodaBeanUtils.hashCode(curveGroupName);
    hash = hash * 31 + JodaBeanUtils.hashCode(marketDataFeed);
    return hash;
  }

  @Override
  public String toString() {
    StringBuilder buf = new StringBuilder(96);
    buf.append("IborIndexCurveMapping{");
    buf.append("curveGroupName").append('=').append(curveGroupName).append(',').append(' ');
    buf.append("marketDataFeed").append('=').append(JodaBeanUtils.toString(marketDataFeed));
    buf.append('}');
    return buf.toString();
  }

  //-----------------------------------------------------------------------
  /**
   * The meta-bean for {@code IborIndexCurveMapping}.
   */
  public static final class Meta extends DirectMetaBean {
    /**
     * The singleton instance of the meta-bean.
     */
    static final Meta INSTANCE = new Meta();

    /**
     * The meta-property for the {@code curveGroupName} property.
     */
    private final MetaProperty<CurveGroupName> curveGroupName = DirectMetaProperty.ofImmutable(
        this, "curveGroupName", IborIndexCurveMapping.class, CurveGroupName.class);
    /**
     * The meta-property for the {@code marketDataFeed} property.
     */
    private final MetaProperty<MarketDataFeed> marketDataFeed = DirectMetaProperty.ofImmutable(
        this, "marketDataFeed", IborIndexCurveMapping.class, MarketDataFeed.class);
    /**
     * The meta-properties.
     */
    private final Map<String, MetaProperty<?>> metaPropertyMap$ = new DirectMetaPropertyMap(
        this, null,
        "curveGroupName",
        "marketDataFeed");

    /**
     * Restricted constructor.
     */
    private Meta() {
    }

    @Override
    protected MetaProperty<?> metaPropertyGet(String propertyName) {
      switch (propertyName.hashCode()) {
        case -382645893:  // curveGroupName
          return curveGroupName;
        case 842621124:  // marketDataFeed
          return marketDataFeed;
      }
      return super.metaPropertyGet(propertyName);
    }

    @Override
    public BeanBuilder<? extends IborIndexCurveMapping> builder() {
      return new IborIndexCurveMapping.Builder();
    }

    @Override
    public Class<? extends IborIndexCurveMapping> beanType() {
      return IborIndexCurveMapping.class;
    }

    @Override
    public Map<String, MetaProperty<?>> metaPropertyMap() {
      return metaPropertyMap$;
    }

    //-----------------------------------------------------------------------
    /**
     * The meta-property for the {@code curveGroupName} property.
     * @return the meta-property, not null
     */
    public MetaProperty<CurveGroupName> curveGroupName() {
      return curveGroupName;
    }

    /**
     * The meta-property for the {@code marketDataFeed} property.
     * @return the meta-property, not null
     */
    public MetaProperty<MarketDataFeed> marketDataFeed() {
      return marketDataFeed;
    }

    //-----------------------------------------------------------------------
    @Override
    protected Object propertyGet(Bean bean, String propertyName, boolean quiet) {
      switch (propertyName.hashCode()) {
        case -382645893:  // curveGroupName
          return ((IborIndexCurveMapping) bean).getCurveGroupName();
        case 842621124:  // marketDataFeed
          return ((IborIndexCurveMapping) bean).getMarketDataFeed();
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
   * The bean-builder for {@code IborIndexCurveMapping}.
   */
  private static final class Builder extends DirectFieldsBeanBuilder<IborIndexCurveMapping> {

    private CurveGroupName curveGroupName;
    private MarketDataFeed marketDataFeed;

    /**
     * Restricted constructor.
     */
    private Builder() {
    }

    //-----------------------------------------------------------------------
    @Override
    public Object get(String propertyName) {
      switch (propertyName.hashCode()) {
        case -382645893:  // curveGroupName
          return curveGroupName;
        case 842621124:  // marketDataFeed
          return marketDataFeed;
        default:
          throw new NoSuchElementException("Unknown property: " + propertyName);
      }
    }

    @Override
    public Builder set(String propertyName, Object newValue) {
      switch (propertyName.hashCode()) {
        case -382645893:  // curveGroupName
          this.curveGroupName = (CurveGroupName) newValue;
          break;
        case 842621124:  // marketDataFeed
          this.marketDataFeed = (MarketDataFeed) newValue;
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
    public IborIndexCurveMapping build() {
      return new IborIndexCurveMapping(
          curveGroupName,
          marketDataFeed);
    }

    //-----------------------------------------------------------------------
    @Override
    public String toString() {
      StringBuilder buf = new StringBuilder(96);
      buf.append("IborIndexCurveMapping.Builder{");
      buf.append("curveGroupName").append('=').append(JodaBeanUtils.toString(curveGroupName)).append(',').append(' ');
      buf.append("marketDataFeed").append('=').append(JodaBeanUtils.toString(marketDataFeed));
      buf.append('}');
      return buf.toString();
    }

  }

  ///CLOVER:ON
  //-------------------------- AUTOGENERATED END --------------------------
}
