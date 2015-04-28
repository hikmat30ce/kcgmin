package com.valspar.interfaces.hr.wirelessload.beans;

import java.util.List;

public class IntMobileDeviceBean
{
  private String userName;
  private String firstName;
  private String lastName;
  private String jobTitle;
  private String countryIsoCode;
  private String internationalPhoneCode;
  private String mobileNumber;
  private String employeeId;
  private String jobFamily;
  private String location;
  private String costCenterName;
  private String costCenterID;
  private String emailAddress;
  private String terminationDate;
  private List<PhoneNumberBean> phoneNumbers;

  public void setUserName(String userName)
  {
    this.userName = userName;
  }

  public String getUserName()
  {
    return userName;
  }

  public void setFirstName(String firstName)
  {
    this.firstName = firstName;
  }

  public String getFirstName()
  {
    return firstName;
  }

  public void setLastName(String lastName)
  {
    this.lastName = lastName;
  }

  public String getLastName()
  {
    return lastName;
  }

  public void setJobTitle(String jobTitle)
  {
    this.jobTitle = jobTitle;
  }

  public String getJobTitle()
  {
    return jobTitle;
  }

  public void setCountryIsoCode(String countryIsoCode)
  {
    this.countryIsoCode = countryIsoCode;
  }

  public String getCountryIsoCode()
  {
    return countryIsoCode;
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

  public void setEmployeeId(String employeeId)
  {
    this.employeeId = employeeId;
  }

  public String getEmployeeId()
  {
    return employeeId;
  }

  public void setJobFamily(String jobFamily)
  {
    this.jobFamily = jobFamily;
  }

  public String getJobFamily()
  {
    return jobFamily;
  }

  public void setLocation(String location)
  {
    this.location = location;
  }

  public String getLocation()
  {
    return location;
  }

  public void setCostCenterName(String costCenterName)
  {
    this.costCenterName = costCenterName;
  }

  public String getCostCenterName()
  {
    return costCenterName;
  }

  public void setCostCenterID(String costCenterID)
  {
    this.costCenterID = costCenterID;
  }

  public String getCostCenterID()
  {
    return costCenterID;
  }

  public void setEmailAddress(String emailAddress)
  {
    this.emailAddress = emailAddress;
  }

  public String getEmailAddress()
  {
    return emailAddress;
  }

  public void setTerminationDate(String terminationDate)
  {
    this.terminationDate = terminationDate;
  }

  public String getTerminationDate()
  {
    return terminationDate;
  }

  public void setPhoneNumbers(List<PhoneNumberBean> phoneNumbers)
  {
    this.phoneNumbers = phoneNumbers;
  }

  public List<PhoneNumberBean> getPhoneNumbers()
  {
    return phoneNumbers;
  }
}
