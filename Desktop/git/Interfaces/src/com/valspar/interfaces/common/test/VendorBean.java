package com.valspar.interfaces.common.test;

import javax.persistence.*;

@Entity
@Table(name = "VALTRACK.VENDOR_TEST")
public class VendorBean
{
  @Id
  @Column(name = "vendor_id")
  private String vendorId;
  @Column(name = "vendor_name")
  private String vendorName;
  @Column(name = "vendor_code")
  private String vendorCode;
  @Column(name = "vendor_type_lookup_code")
  private String vendorTypeLookupCode;

  public void setVendorId(String vendorId)
  {
    this.vendorId = vendorId;
  }

  public String getVendorId()
  {
    return vendorId;
  }

  public void setVendorName(String vendorName)
  {
    this.vendorName = vendorName;
  }

  public String getVendorName()
  {
    return vendorName;
  }

  public void setVendorCode(String vendorCode)
  {
    this.vendorCode = vendorCode;
  }

  public String getVendorCode()
  {
    return vendorCode;
  }

  public void setVendorTypeLookupCode(String vendorTypeLookupCode)
  {
    this.vendorTypeLookupCode = vendorTypeLookupCode;
  }

  public String getVendorTypeLookupCode()
  {
    return vendorTypeLookupCode;
  }
}
