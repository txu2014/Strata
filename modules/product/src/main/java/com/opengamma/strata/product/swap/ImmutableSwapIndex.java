/**
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.product.swap;

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

import com.opengamma.strata.product.swap.type.FixedIborSwapTemplate;

/**
 * A swap index implementation based on an immutable set of rules.
 * <p>
 * A standard immutable implementation of {@link SwapIndex} that defines the swap trade template,
 * including the swap convention and tenor.
 * <p>
 * In most cases, applications should refer to indices by name, using {@link SwapIndex#of(String)}.
 * The named index will typically be resolved to an instance of this class.
 * As such, it is recommended to use the {@code SwapIndex} interface in application
 * code rather than directly referring to this class.
 */
@BeanDefinition
public final class ImmutableSwapIndex
    implements SwapIndex, ImmutableBean, Serializable {
  /**
  * The index name.
  */
  @PropertyDefinition(validate = "notEmpty", overrideGet = true)
  private final String name;
  /**
  * The template for creating Fixed-Ibor swap.
  */
  @PropertyDefinition(validate = "notNull", overrideGet = true)
  private final FixedIborSwapTemplate template;

  //-------------------------------------------------------------------------
  /**
   * Obtains a {@code ImmutableSwapIndex}.
   * 
   * @param name  the index name
   * @param template  the swap template
   * @return the index
   */
  public static ImmutableSwapIndex of(String name, FixedIborSwapTemplate template) {
    return new ImmutableSwapIndex(name, template);
  }

  //-------------------------------------------------------------------------
  @Override
  public boolean equals(Object obj) {
    if (obj == this) {
      return true;
    }
    if (obj instanceof ImmutableSwapIndex) {
      return name.equals(((ImmutableSwapIndex) obj).name);
    }
    return false;
  }

  @Override
  public int hashCode() {
    return name.hashCode();
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
   * The meta-bean for {@code ImmutableSwapIndex}.
   * @return the meta-bean, not null
   */
  public static ImmutableSwapIndex.Meta meta() {
    return ImmutableSwapIndex.Meta.INSTANCE;
  }

  static {
    JodaBeanUtils.registerMetaBean(ImmutableSwapIndex.Meta.INSTANCE);
  }

  /**
   * The serialization version id.
   */
  private static final long serialVersionUID = 1L;

  /**
   * Returns a builder used to create an instance of the bean.
   * @return the builder, not null
   */
  public static ImmutableSwapIndex.Builder builder() {
    return new ImmutableSwapIndex.Builder();
  }

  private ImmutableSwapIndex(
      String name,
      FixedIborSwapTemplate template) {
    JodaBeanUtils.notEmpty(name, "name");
    JodaBeanUtils.notNull(template, "template");
    this.name = name;
    this.template = template;
  }

  @Override
  public ImmutableSwapIndex.Meta metaBean() {
    return ImmutableSwapIndex.Meta.INSTANCE;
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
   * Gets the index name.
   * @return the value of the property, not empty
   */
  @Override
  public String getName() {
    return name;
  }

  //-----------------------------------------------------------------------
  /**
   * Gets the template for creating Fixed-Ibor swap.
   * @return the value of the property, not null
   */
  @Override
  public FixedIborSwapTemplate getTemplate() {
    return template;
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
   * The meta-bean for {@code ImmutableSwapIndex}.
   */
  public static final class Meta extends DirectMetaBean {
    /**
     * The singleton instance of the meta-bean.
     */
    static final Meta INSTANCE = new Meta();

    /**
     * The meta-property for the {@code name} property.
     */
    private final MetaProperty<String> name = DirectMetaProperty.ofImmutable(
        this, "name", ImmutableSwapIndex.class, String.class);
    /**
     * The meta-property for the {@code template} property.
     */
    private final MetaProperty<FixedIborSwapTemplate> template = DirectMetaProperty.ofImmutable(
        this, "template", ImmutableSwapIndex.class, FixedIborSwapTemplate.class);
    /**
     * The meta-properties.
     */
    private final Map<String, MetaProperty<?>> metaPropertyMap$ = new DirectMetaPropertyMap(
        this, null,
        "name",
        "template");

    /**
     * Restricted constructor.
     */
    private Meta() {
    }

    @Override
    protected MetaProperty<?> metaPropertyGet(String propertyName) {
      switch (propertyName.hashCode()) {
        case 3373707:  // name
          return name;
        case -1321546630:  // template
          return template;
      }
      return super.metaPropertyGet(propertyName);
    }

    @Override
    public ImmutableSwapIndex.Builder builder() {
      return new ImmutableSwapIndex.Builder();
    }

    @Override
    public Class<? extends ImmutableSwapIndex> beanType() {
      return ImmutableSwapIndex.class;
    }

    @Override
    public Map<String, MetaProperty<?>> metaPropertyMap() {
      return metaPropertyMap$;
    }

    //-----------------------------------------------------------------------
    /**
     * The meta-property for the {@code name} property.
     * @return the meta-property, not null
     */
    public MetaProperty<String> name() {
      return name;
    }

    /**
     * The meta-property for the {@code template} property.
     * @return the meta-property, not null
     */
    public MetaProperty<FixedIborSwapTemplate> template() {
      return template;
    }

    //-----------------------------------------------------------------------
    @Override
    protected Object propertyGet(Bean bean, String propertyName, boolean quiet) {
      switch (propertyName.hashCode()) {
        case 3373707:  // name
          return ((ImmutableSwapIndex) bean).getName();
        case -1321546630:  // template
          return ((ImmutableSwapIndex) bean).getTemplate();
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
   * The bean-builder for {@code ImmutableSwapIndex}.
   */
  public static final class Builder extends DirectFieldsBeanBuilder<ImmutableSwapIndex> {

    private String name;
    private FixedIborSwapTemplate template;

    /**
     * Restricted constructor.
     */
    private Builder() {
    }

    /**
     * Restricted copy constructor.
     * @param beanToCopy  the bean to copy from, not null
     */
    private Builder(ImmutableSwapIndex beanToCopy) {
      this.name = beanToCopy.getName();
      this.template = beanToCopy.getTemplate();
    }

    //-----------------------------------------------------------------------
    @Override
    public Object get(String propertyName) {
      switch (propertyName.hashCode()) {
        case 3373707:  // name
          return name;
        case -1321546630:  // template
          return template;
        default:
          throw new NoSuchElementException("Unknown property: " + propertyName);
      }
    }

    @Override
    public Builder set(String propertyName, Object newValue) {
      switch (propertyName.hashCode()) {
        case 3373707:  // name
          this.name = (String) newValue;
          break;
        case -1321546630:  // template
          this.template = (FixedIborSwapTemplate) newValue;
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
    public ImmutableSwapIndex build() {
      return new ImmutableSwapIndex(
          name,
          template);
    }

    //-----------------------------------------------------------------------
    /**
     * Sets the index name.
     * @param name  the new value, not empty
     * @return this, for chaining, not null
     */
    public Builder name(String name) {
      JodaBeanUtils.notEmpty(name, "name");
      this.name = name;
      return this;
    }

    /**
     * Sets the template for creating Fixed-Ibor swap.
     * @param template  the new value, not null
     * @return this, for chaining, not null
     */
    public Builder template(FixedIborSwapTemplate template) {
      JodaBeanUtils.notNull(template, "template");
      this.template = template;
      return this;
    }

    //-----------------------------------------------------------------------
    @Override
    public String toString() {
      StringBuilder buf = new StringBuilder(96);
      buf.append("ImmutableSwapIndex.Builder{");
      buf.append("name").append('=').append(JodaBeanUtils.toString(name)).append(',').append(' ');
      buf.append("template").append('=').append(JodaBeanUtils.toString(template));
      buf.append('}');
      return buf.toString();
    }

  }

  ///CLOVER:ON
  //-------------------------- AUTOGENERATED END --------------------------
}
