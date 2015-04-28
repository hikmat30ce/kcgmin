package com.valspar.interfaces.purchasing.cipace.beans;

import org.apache.commons.lang.builder.EqualsBuilder;
import org.apache.commons.lang.builder.HashCodeBuilder;

public class VendorSiteBean
{
  private String oracleDatabaseName;
  private String addressLine;
  private String zipCode;
  private String billTo;
  private String billToLocationId;
  private String city;
  private String country;
  private String operatingUnit;
  private String orgId;
  private String province;
  private String cipRegionId;
  private String siteBillToLocation;
  private String siteBillToLocationId;
  private String state;
  private String vendorId;
  private String name;
  private String vendorSiteCode;
  private String vendorSiteId;
  private String vendorType;
  private String vendorAutoId;
  private boolean active;

  @Override
  public boolean equals(Object object)
  {
    return EqualsBuilder.reflectionEquals(this, object);
  }

  @Override
  public int hashCode()
  {
    return HashCodeBuilder.reflectionHashCode(this);
  }

  public void setAddressLine(String addressLine)
  {
    this.addressLine = addressLine;
  }

  public String getAddressLine()
  {
    return addressLine;
  }

  public void setZipCode(String zipCode)
  {
    this.zipCode = zipCode;
  }

  public String getZipCode()
  {
    return zipCode;
  }

  public void setBillTo(String billTo)
  {
    this.billTo = billTo;
  }

  public String getBillTo()
  {
    return billTo;
  }

  public void setBillToLocationId(String billToLocationId)
  {
    this.billToLocationId = billToLocationId;
  }

  public String getBillToLocationId()
  {
    return billToLocationId;
  }

  public void setCity(String city)
  {
    this.city = city;
  }

  public String getCity()
  {
    return city;
  }

  public void setCountry(String country)
  {
    this.country = country;
  }

  public String getCountry()
  {
    return country;
  }

  public void setOperatingUnit(String operatingUnit)
  {
    this.operatingUnit = operatingUnit;
  }

  public String getOperatingUnit()
  {
    return operatingUnit;
  }

  public void setOrgId(String orgId)
  {
    this.orgId = orgId;
  }

  public String getOrgId()
  {
    return orgId;
  }

  public void setProvince(String province)
  {
    this.province = province;
  }

  public String getProvince()
  {
    return province;
  }

  public void setSiteBillToLocation(String siteBillToLocation)
  {
    this.siteBillToLocation = siteBillToLocation;
  }

  public String getSiteBillToLocation()
  {
    return siteBillToLocation;
  }

  public void setSiteBillToLocationId(String siteBillToLocationId)
  {
    this.siteBillToLocationId = siteBillToLocationId;
  }

  public String getSiteBillToLocationId()
  {
    return siteBillToLocationId;
  }

  public void setState(String state)
  {
    this.state = state;
  }

  public String getState()
  {
    return state;
  }

  public void setVendorId(String vendorId)
  {
    this.vendorId = vendorId;
  }

  public String getVendorId()
  {
    return vendorId;
  }

  public void setName(String name)
  {
    this.name = name;
  }

  public String getName()
  {
    return name;
  }

  public void setVendorSiteCode(String vendorSiteCode)
  {
    this.vendorSiteCode = vendorSiteCode;
  }

  public String getVendorSiteCode()
  {
    return vendorSiteCode;
  }

  public void setVendorSiteId(String vendorSiteId)
  {
    this.vendorSiteId = vendorSiteId;
  }

  public String getVendorSiteId()
  {
    return vendorSiteId;
  }

  public void setVendorType(String vendorType)
  {
    this.vendorType = vendorType;
  }

  public String getVendorType()
  {
    return vendorType;
  }

  public void setVendorAutoId(String vendorAutoId)
  {
    this.vendorAutoId = vendorAutoId;
  }

  public String getVendorAutoId()
  {
    return vendorAutoId;
  }

  public void setActive(boolean active)
  {
    this.active = active;
  }

  public boolean isActive()
  {
    return active;
  }

  public void setOracleDatabaseName(String oracleDatabaseName)
  {
    this.oracleDatabaseName = oracleDatabaseName;
  }

  public String getOracleDatabaseName()
  {
    return oracleDatabaseName;
  }

  public void setCipRegionId(String cipRegionId)
  {
    this.cipRegionId = cipRegionId;
  }

  public String getCipRegionId()
  {
    return cipRegionId;
  }
}
