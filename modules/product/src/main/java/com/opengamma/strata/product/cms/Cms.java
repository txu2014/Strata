/**
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.product.cms;

import java.io.Serializable;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.Set;

import org.joda.beans.Bean;
import org.joda.beans.BeanBuilder;
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

import com.opengamma.strata.collect.ArgChecker;
import com.opengamma.strata.product.swap.SwapIndex;
import com.opengamma.strata.product.swap.SwapLeg;

/**
 * The class defines a constant maturity swap (CMS) or CMS cap/floor. 
 * <p>
 * The CMS product consists of two legs: CMS leg and pay leg. 
 * The CMS leg of CMS periodically pays coupons based on swap rate, the observed value of {@linkplain SwapIndex swap index},  
 * CMS cap/floor is a set of call/put options on successive swap rates, i.e., CMS caplets/floorlets. 
 * The other leg of the swap is the same as a swap leg of the standard interest rate swap. See {@link SwapLeg}.
 * <p>
 * However, the pay leg is absent for certain CMS products. Instead the premium is paid upfront. See {@link CmsTrade}.
 */
@BeanDefinition
public final class Cms
    implements CmsProduct, ImmutableBean, Serializable {

  /**
   * The CMS leg of the product.
   * <p>
   * This is associated with periodic payments based on swap rate. 
   * The payments are CMS coupons, CMS caplets or CMS floors. 
   */
  @PropertyDefinition(validate = "notNull")
  private final CmsLeg cmsLeg;
  /**
   * The pay leg of the product. 
   * <p>
   * Typically this is associated with periodic fixed or Ibor rate payments without compounding or notioanl exchange. 
   * <p>
   * For certain CMS products, these periodic payments are not made over the lifetime of the product. Instead the 
   * premium is paid upfront. 
   */
  @PropertyDefinition(get = "optional")
  private final SwapLeg payLeg;

  //-------------------------------------------------------------------------
  @ImmutableValidator
  private void validate() {
    if (getPayLeg().isPresent()) {
      ArgChecker.isFalse(payLeg.getPayReceive().equals(cmsLeg.getPayReceive()),
          "Two legs should have different Pay/Receive flags");
    }
  }

  //-------------------------------------------------------------------------
  @Override
  public ExpandedCms expand() {
    return ExpandedCms.builder()
        .cmsLeg(cmsLeg.expand())
        .payLeg(getPayLeg().isPresent() ? payLeg.expand() : null)
        .build();
  }

  //------------------------- AUTOGENERATED START -------------------------
  ///CLOVER:OFF
  /**
   * The meta-bean for {@code Cms}.
   * @return the meta-bean, not null
   */
  public static Cms.Meta meta() {
    return Cms.Meta.INSTANCE;
  }

  static {
    JodaBeanUtils.registerMetaBean(Cms.Meta.INSTANCE);
  }

  /**
   * The serialization version id.
   */
  private static final long serialVersionUID = 1L;

  /**
   * Returns a builder used to create an instance of the bean.
   * @return the builder, not null
   */
  public static Cms.Builder builder() {
    return new Cms.Builder();
  }

  private Cms(
      CmsLeg cmsLeg,
      SwapLeg payLeg) {
    JodaBeanUtils.notNull(cmsLeg, "cmsLeg");
    this.cmsLeg = cmsLeg;
    this.payLeg = payLeg;
    validate();
  }

  @Override
  public Cms.Meta metaBean() {
    return Cms.Meta.INSTANCE;
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
   * Gets the CMS leg of the product.
   * <p>
   * This is associated with periodic payments based on swap rate.
   * The payments are CMS coupons, CMS caplets or CMS floors.
   * @return the value of the property, not null
   */
  public CmsLeg getCmsLeg() {
    return cmsLeg;
  }

  //-----------------------------------------------------------------------
  /**
   * Gets the pay leg of the product.
   * <p>
   * Typically this is associated with periodic fixed or Ibor rate payments without compounding or notioanl exchange.
   * <p>
   * For certain CMS products, these periodic payments are not made over the lifetime of the product. Instead the
   * premium is paid upfront.
   * @return the optional value of the property, not null
   */
  public Optional<SwapLeg> getPayLeg() {
    return Optional.ofNullable(payLeg);
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
      Cms other = (Cms) obj;
      return JodaBeanUtils.equal(cmsLeg, other.cmsLeg) &&
          JodaBeanUtils.equal(payLeg, other.payLeg);
    }
    return false;
  }

  @Override
  public int hashCode() {
    int hash = getClass().hashCode();
    hash = hash * 31 + JodaBeanUtils.hashCode(cmsLeg);
    hash = hash * 31 + JodaBeanUtils.hashCode(payLeg);
    return hash;
  }

  @Override
  public String toString() {
    StringBuilder buf = new StringBuilder(96);
    buf.append("Cms{");
    buf.append("cmsLeg").append('=').append(cmsLeg).append(',').append(' ');
    buf.append("payLeg").append('=').append(JodaBeanUtils.toString(payLeg));
    buf.append('}');
    return buf.toString();
  }

  //-----------------------------------------------------------------------
  /**
   * The meta-bean for {@code Cms}.
   */
  public static final class Meta extends DirectMetaBean {
    /**
     * The singleton instance of the meta-bean.
     */
    static final Meta INSTANCE = new Meta();

    /**
     * The meta-property for the {@code cmsLeg} property.
     */
    private final MetaProperty<CmsLeg> cmsLeg = DirectMetaProperty.ofImmutable(
        this, "cmsLeg", Cms.class, CmsLeg.class);
    /**
     * The meta-property for the {@code payLeg} property.
     */
    private final MetaProperty<SwapLeg> payLeg = DirectMetaProperty.ofImmutable(
        this, "payLeg", Cms.class, SwapLeg.class);
    /**
     * The meta-properties.
     */
    private final Map<String, MetaProperty<?>> metaPropertyMap$ = new DirectMetaPropertyMap(
        this, null,
        "cmsLeg",
        "payLeg");

    /**
     * Restricted constructor.
     */
    private Meta() {
    }

    @Override
    protected MetaProperty<?> metaPropertyGet(String propertyName) {
      switch (propertyName.hashCode()) {
        case -1356515323:  // cmsLeg
          return cmsLeg;
        case -995239866:  // payLeg
          return payLeg;
      }
      return super.metaPropertyGet(propertyName);
    }

    @Override
    public Cms.Builder builder() {
      return new Cms.Builder();
    }

    @Override
    public Class<? extends Cms> beanType() {
      return Cms.class;
    }

    @Override
    public Map<String, MetaProperty<?>> metaPropertyMap() {
      return metaPropertyMap$;
    }

    //-----------------------------------------------------------------------
    /**
     * The meta-property for the {@code cmsLeg} property.
     * @return the meta-property, not null
     */
    public MetaProperty<CmsLeg> cmsLeg() {
      return cmsLeg;
    }

    /**
     * The meta-property for the {@code payLeg} property.
     * @return the meta-property, not null
     */
    public MetaProperty<SwapLeg> payLeg() {
      return payLeg;
    }

    //-----------------------------------------------------------------------
    @Override
    protected Object propertyGet(Bean bean, String propertyName, boolean quiet) {
      switch (propertyName.hashCode()) {
        case -1356515323:  // cmsLeg
          return ((Cms) bean).getCmsLeg();
        case -995239866:  // payLeg
          return ((Cms) bean).payLeg;
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
   * The bean-builder for {@code Cms}.
   */
  public static final class Builder extends DirectFieldsBeanBuilder<Cms> {

    private CmsLeg cmsLeg;
    private SwapLeg payLeg;

    /**
     * Restricted constructor.
     */
    private Builder() {
    }

    /**
     * Restricted copy constructor.
     * @param beanToCopy  the bean to copy from, not null
     */
    private Builder(Cms beanToCopy) {
      this.cmsLeg = beanToCopy.getCmsLeg();
      this.payLeg = beanToCopy.payLeg;
    }

    //-----------------------------------------------------------------------
    @Override
    public Object get(String propertyName) {
      switch (propertyName.hashCode()) {
        case -1356515323:  // cmsLeg
          return cmsLeg;
        case -995239866:  // payLeg
          return payLeg;
        default:
          throw new NoSuchElementException("Unknown property: " + propertyName);
      }
    }

    @Override
    public Builder set(String propertyName, Object newValue) {
      switch (propertyName.hashCode()) {
        case -1356515323:  // cmsLeg
          this.cmsLeg = (CmsLeg) newValue;
          break;
        case -995239866:  // payLeg
          this.payLeg = (SwapLeg) newValue;
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
    public Cms build() {
      return new Cms(
          cmsLeg,
          payLeg);
    }

    //-----------------------------------------------------------------------
    /**
     * Sets the CMS leg of the product.
     * <p>
     * This is associated with periodic payments based on swap rate.
     * The payments are CMS coupons, CMS caplets or CMS floors.
     * @param cmsLeg  the new value, not null
     * @return this, for chaining, not null
     */
    public Builder cmsLeg(CmsLeg cmsLeg) {
      JodaBeanUtils.notNull(cmsLeg, "cmsLeg");
      this.cmsLeg = cmsLeg;
      return this;
    }

    /**
     * Sets the pay leg of the product.
     * <p>
     * Typically this is associated with periodic fixed or Ibor rate payments without compounding or notioanl exchange.
     * <p>
     * For certain CMS products, these periodic payments are not made over the lifetime of the product. Instead the
     * premium is paid upfront.
     * @param payLeg  the new value
     * @return this, for chaining, not null
     */
    public Builder payLeg(SwapLeg payLeg) {
      this.payLeg = payLeg;
      return this;
    }

    //-----------------------------------------------------------------------
    @Override
    public String toString() {
      StringBuilder buf = new StringBuilder(96);
      buf.append("Cms.Builder{");
      buf.append("cmsLeg").append('=').append(JodaBeanUtils.toString(cmsLeg)).append(',').append(' ');
      buf.append("payLeg").append('=').append(JodaBeanUtils.toString(payLeg));
      buf.append('}');
      return buf.toString();
    }

  }

  ///CLOVER:ON
  //-------------------------- AUTOGENERATED END --------------------------
}
