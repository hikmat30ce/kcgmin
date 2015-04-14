package com.valspar.interfaces.hr.wirelessload.beans;

public class PhoneNumberBean
{
  private String internationalPhoneCode;
  private String mobileNumber;
  private String phoneType;

  public PhoneNumberBean()
  {
  }

  public void setInternationalPhoneCode(String internationalPhoneCode)
  {
    this.internationalPhoneCode = internationalPhoneCode;
  }

  public String getInternationalPhoneCode()
  {
    return internationalPhoneCode;
  }

  public void setMobileNumber(String mobileNumber)
  {
    this.mobileNumber = mobileNumber;
  }

  public String getMobileNumber()
  {
    return mobileNumber;
  }

  public void setPhoneType(String phoneType)
  {
    this.phoneType = phoneType;
  }

  public String getPhoneType()
  {
    return phoneType;
  }
}
