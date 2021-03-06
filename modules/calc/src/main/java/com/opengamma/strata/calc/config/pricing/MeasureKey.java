/**
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.calc.config.pricing;

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

import com.opengamma.strata.basics.CalculationTarget;
import com.opengamma.strata.calc.config.Measure;

/**
 * Key for storing calculation configuration, consisting of the type of the target
 * handled by the calculation and the measure calculated by the function.
 */
@BeanDefinition(builderScope = "private")
final class MeasureKey implements ImmutableBean {

  /**
   * The type of the target of the calculation, such as a trade.
   */
  @PropertyDefinition(validate = "notNull")
  private final Class<? extends CalculationTarget> targetType;
  /**
   * The measure that is the output of the calculation.
   */
  @PropertyDefinition(validate = "notNull")
  private final Measure measure;

  //-------------------------------------------------------------------------
  /**
   * Obtains a key for a target type and measure.
   *
   * @param targetType  the type of the target
   * @param measure  the calculated measure
   * @return a key for the target type and measure
   */
  static MeasureKey of(Class<? extends CalculationTarget> targetType, Measure measure) {
    return new MeasureKey(targetType, measure);
  }

  //------------------------- AUTOGENERATED START -------------------------
  ///CLOVER:OFF
  /**
   * The meta-bean for {@code MeasureKey}.
   * @return the meta-bean, not null
   */
  public static MeasureKey.Meta meta() {
    return MeasureKey.Meta.INSTANCE;
  }

  static {
    JodaBeanUtils.registerMetaBean(MeasureKey.Meta.INSTANCE);
  }

  private MeasureKey(
      Class<? extends CalculationTarget> targetType,
      Measure measure) {
    JodaBeanUtils.notNull(targetType, "targetType");
    JodaBeanUtils.notNull(measure, "measure");
    this.targetType = targetType;
    this.measure = measure;
  }

  @Override
  public MeasureKey.Meta metaBean() {
    return MeasureKey.Meta.INSTANCE;
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
   * Gets the type of the target of the calculation, such as a trade.
   * @return the value of the property, not null
   */
  public Class<? extends CalculationTarget> getTargetType() {
    return targetType;
  }

  //-----------------------------------------------------------------------
  /**
   * Gets the measure that is the output of the calculation.
   * @return the value of the property, not null
   */
  public Measure getMeasure() {
    return measure;
  }

  //-----------------------------------------------------------------------
  @Override
  public boolean equals(Object obj) {
    if (obj == this) {
      return true;
    }
    if (obj != null && obj.getClass() == this.getClass()) {
      MeasureKey other = (MeasureKey) obj;
      return JodaBeanUtils.equal(targetType, other.targetType) &&
          JodaBeanUtils.equal(measure, other.measure);
    }
    return false;
  }

  @Override
  public int hashCode() {
    int hash = getClass().hashCode();
    hash = hash * 31 + JodaBeanUtils.hashCode(targetType);
    hash = hash * 31 + JodaBeanUtils.hashCode(measure);
    return hash;
  }

  @Override
  public String toString() {
    StringBuilder buf = new StringBuilder(96);
    buf.append("MeasureKey{");
    buf.append("targetType").append('=').append(targetType).append(',').append(' ');
    buf.append("measure").append('=').append(JodaBeanUtils.toString(measure));
    buf.append('}');
    return buf.toString();
  }

  //-----------------------------------------------------------------------
  /**
   * The meta-bean for {@code MeasureKey}.
   */
  public static final class Meta extends DirectMetaBean {
    /**
     * The singleton instance of the meta-bean.
     */
    static final Meta INSTANCE = new Meta();

    /**
     * The meta-property for the {@code targetType} property.
     */
    @SuppressWarnings({"unchecked", "rawtypes" })
    private final MetaProperty<Class<? extends CalculationTarget>> targetType = DirectMetaProperty.ofImmutable(
        this, "targetType", MeasureKey.class, (Class) Class.class);
    /**
     * The meta-property for the {@code measure} property.
     */
    private final MetaProperty<Measure> measure = DirectMetaProperty.ofImmutable(
        this, "measure", MeasureKey.class, Measure.class);
    /**
     * The meta-properties.
     */
    private final Map<String, MetaProperty<?>> metaPropertyMap$ = new DirectMetaPropertyMap(
        this, null,
        "targetType",
        "measure");

    /**
     * Restricted constructor.
     */
    private Meta() {
    }

    @Override
    protected MetaProperty<?> metaPropertyGet(String propertyName) {
      switch (propertyName.hashCode()) {
        case 486622315:  // targetType
          return targetType;
        case 938321246:  // measure
          return measure;
      }
      return super.metaPropertyGet(propertyName);
    }

    @Override
    public BeanBuilder<? extends MeasureKey> builder() {
      return new MeasureKey.Builder();
    }

    @Override
    public Class<? extends MeasureKey> beanType() {
      return MeasureKey.class;
    }

    @Override
    public Map<String, MetaProperty<?>> metaPropertyMap() {
      return metaPropertyMap$;
    }

    //-----------------------------------------------------------------------
    /**
     * The meta-property for the {@code targetType} property.
     * @return the meta-property, not null
     */
    public MetaProperty<Class<? extends CalculationTarget>> targetType() {
      return targetType;
    }

    /**
     * The meta-property for the {@code measure} property.
     * @return the meta-property, not null
     */
    public MetaProperty<Measure> measure() {
      return measure;
    }

    //-----------------------------------------------------------------------
    @Override
    protected Object propertyGet(Bean bean, String propertyName, boolean quiet) {
      switch (propertyName.hashCode()) {
        case 486622315:  // targetType
          return ((MeasureKey) bean).getTargetType();
        case 938321246:  // measure
          return ((MeasureKey) bean).getMeasure();
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
   * The bean-builder for {@code MeasureKey}.
   */
  private static final class Builder extends DirectFieldsBeanBuilder<MeasureKey> {

    private Class<? extends CalculationTarget> targetType;
    private Measure measure;

    /**
     * Restricted constructor.
     */
    private Builder() {
    }

    //-----------------------------------------------------------------------
    @Override
    public Object get(String propertyName) {
      switch (propertyName.hashCode()) {
        case 486622315:  // targetType
          return targetType;
        case 938321246:  // measure
          return measure;
        default:
          throw new NoSuchElementException("Unknown property: " + propertyName);
      }
    }

    @SuppressWarnings("unchecked")
    @Override
    public Builder set(String propertyName, Object newValue) {
      switch (propertyName.hashCode()) {
        case 486622315:  // targetType
          this.targetType = (Class<? extends CalculationTarget>) newValue;
          break;
        case 938321246:  // measure
          this.measure = (Measure) newValue;
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
    public MeasureKey build() {
      return new MeasureKey(
          targetType,
          measure);
    }

    //-----------------------------------------------------------------------
    @Override
    public String toString() {
      StringBuilder buf = new StringBuilder(96);
      buf.append("MeasureKey.Builder{");
      buf.append("targetType").append('=').append(JodaBeanUtils.toString(targetType)).append(',').append(' ');
      buf.append("measure").append('=').append(JodaBeanUtils.toString(measure));
      buf.append('}');
      return buf.toString();
    }

  }

  ///CLOVER:ON
  //-------------------------- AUTOGENERATED END --------------------------
}
