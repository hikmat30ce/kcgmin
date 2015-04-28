package com.valspar.interfaces.regulatory.wercsorders.beans;

import com.valspar.interfaces.common.utils.CommonUtility;

public class AddressBean
{
  private String address;
  private String city;
  private String state;
  private String zip;
  private String attentionLine;
  private String countryName;
  private String email;

  public String getAddress()
  {
    return address;
  }

  public void setAddress(String address)
  {
    this.address = address;
  }

  public void setCity(String city)
  {
    this.city = city;
  }

  public String getCity()
  {
    return CommonUtility.nvl(city);
  }

  public void setState(String state)
  {
    this.state = state;
  }

  public String getState()
  {
    return CommonUtility.nvl(state);
  }

  public void setZip(String zip)
  {
    this.zip = zip;
  }

  public String getZip()
  {
    return CommonUtility.nvl(zip);
  }

  public void setAttentionLine(String attentionLine)
  {
    this.attentionLine = attentionLine;
  }

  public String getAttentionLine()
  {
    return CommonUtility.nvl(attentionLine);
  }

  public void setCountryName(String countryName)
  {
    this.countryName = countryName;
  }

  public String getCountryName()
  {
    return CommonUtility.nvl(countryName);
  }

  public String getEmail()
  {
    return email;
  }

  public void setEmail(String email)
  {
    this.email = email;
  }
}
