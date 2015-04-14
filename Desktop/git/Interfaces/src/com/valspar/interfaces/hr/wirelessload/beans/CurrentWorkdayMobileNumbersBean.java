package com.valspar.interfaces.hr.wirelessload.beans;

import java.util.*;
import javax.persistence.*;

@Entity
@Table(name="MOBILE.CURRENT_WORKDAY_MOBILE_NUMBERS")
public class CurrentWorkdayMobileNumbersBean
{
  private String userName;
  private String firstName;
  private String lastName;
  private String jobTitle;
  private String mobileNumber;
  private String employeeId;
  private String jobFamily;
  private String location;
  private String costCenterName;
  private String costCenterID;
  private String countryISOCode;
  private String internationalPhoneCode;
  private Date creationDate;
  private String emailAddress;
  private long workdayMobileNumberID;
  private String category;
  private Date terminationDate;

  public CurrentWorkdayMobileNumbersBean()
  {
  }
  
  public void setEmployeeId(String employeeId)
  {
    this.employeeId = employeeId;
  }

  @Column(name="EMPLOYEE_ID")
  public String getEmployeeId()
  {
    return employeeId;
  }

  public void setUserName(String userName)
  {
    this.userName = userName;
  }
  
  @Column(name="USERNAME")
  public String getUserName()
  {
    return userName;
  }

  public void setLastName(String lastName)
  {
    this.lastName = lastName;
  }

  @Column(name="LAST_NAME")
  public String getLastName()
  {
    return lastName;
  }

  public void setJobTitle(String jobTitle)
  {
    this.jobTitle = jobTitle;
  }

  @Column(name="JOB_TITLE")
  public String getJobTitle()
  {
    return jobTitle;
  }

  public void setMobileNumber(String mobileNumber)
  {
    this.mobileNumber = mobileNumber;
  }

  @Column(name="MOBILE_NUMBER")
  public String getMobileNumber()
  {
    return mobileNumber;
  }

  public void setFirstName(String firstName)
  {
    this.firstName = firstName;
  }

  @Column(name="FIRST_NAME")
  public String getFirstName()
  {
    return firstName;
  }

  public void setJobFamily(String jobFamily)
  {
    this.jobFamily = jobFamily;
  }

  @Column(name="MAJOR_FAMILY")
  public String getJobFamily()
  {
    return jobFamily;
  }

  public void setLocation(String location)
  {
    this.location = location;
  }

  @Column(name="LOCATION")
  public String getLocation()
  {
    return location;
  }

  public void setCostCenterName(String costCenterName)
  {
    this.costCenterName = costCenterName;
  }

  @Column(name="COST_CENTER_NAME")
  public String getCostCenterName()
  {
    return costCenterName;
  }

  public void setCreationDate(Date creationDate)
  {
    this.creationDate = creationDate;
  }

  @Column(name="CREATION_DATE")
  public Date getCreationDate()
  {
    return creationDate;
  }

  public void setCountryISOCode(String countryISOCode)
  {
    this.countryISOCode = countryISOCode;
  }

  @Column(name="COUNTRY_ISO_CODE")
  public String getCountryISOCode()
  {
    return countryISOCode;
  }

  public void setInternationalPhoneCode(String internationalPhoneCode)
  {
    this.internationalPhoneCode = internationalPhoneCode;
  }

  @Column(name="INTERNATIONAL_PHONE_CODE")
  public String getInternationalPhoneCode()
  {
    return internationalPhoneCode;
  }

  public void setEmailAddress(String emailAddress)
  {
    this.emailAddress = emailAddress;
  }

  @Column(name="EMAIL_ADDRESS")
  public String getEmailAddress()
  {
    return emailAddress;
  }

  public void setWorkdayMobileNumberID(long workdayMobileNumberID)
  {
    this.workdayMobileNumberID = workdayMobileNumberID;
  }

  @Id 
  @Column(name="WORKDAY_MOBILE_NUMBER_ID") 
  @GeneratedValue(generator = "MobileSeq", 
                      strategy = GenerationType.SEQUENCE)
  @SequenceGenerator(name = "MobileSeq", 
                         sequenceName = "mobile.workday_mobile_number_id_seq", 
                         allocationSize = 1)
 public long getWorkdayMobileNumberID()
  {
    return workdayMobileNumberID;
  }

  public void setCategory(String category)
  {
    this.category = category;
  }

  @Column(name="CATEGORY")
  public String getCategory()
  {
    return category;
  }

  public void setCostCenterID(String costCenterID)
  {
    this.costCenterID = costCenterID;
  }

  @Column(name="GL_STRING")
  public String getCostCenterID()
  {
    return costCenterID;
  }

  public void setTerminationDate(Date terminationDate)
  {
    this.terminationDate = terminationDate;
  }

  @Column(name="TERMINATION_DATE")
  public Date getTerminationDate()
  {
    return terminationDate;
  }
}

