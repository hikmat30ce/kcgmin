package com.valspar.interfaces.hr.workdayadsync.beans;

public class Employee
{
  private byte[] base64ImageData;
  private String imageFileName;
  private String userName;
  private String workAddressLine1;
  private String workAddressLine2;
  private String workAddressLine3;
  private String workAddressLine4;
  private String workAddressCity;
  private String workAddressState;
  private String workAddressPostalCode;
  private String workAddressCountry;
  private String region;
  private String legalFirstName;
  private String legalLastName;
  private String primaryWorkEmail;
  private String employeeID;
  private String primaryWorkPhone;
  private String primaryVoipPhone;
  private String businessTitle;
  private String managerUserName;
  private String jobFamily;
  private String costCenterHierarchyName;
  private String supervisoryOrganizationName;

  public Employee()
  {
  }

  public void setUserName(String userName)
  {
    this.userName = userName;
  }

  public String getUserName()
  {
    return userName;
  }

  public void setEmployeeID(String employeeID)
  {
    this.employeeID = employeeID;
  }

  public String getEmployeeID()
  {
    return employeeID;
  }

  public void setPrimaryWorkPhone(String primaryWorkPhone)
  {
    this.primaryWorkPhone = primaryWorkPhone;
  }

  public String getPrimaryWorkPhone()
  {
    return primaryWorkPhone;
  }

  public void setPrimaryVoipPhone(String primaryVoipPhone)
  {
    this.primaryVoipPhone = primaryVoipPhone;
  }

  public String getPrimaryVoipPhone()
  {
    return primaryVoipPhone;
  }

  public void setBusinessTitle(String businessTitle)
  {
    this.businessTitle = businessTitle;
  }

  public String getBusinessTitle()
  {
    return businessTitle;
  }

  public void setWorkAddressLine1(String workAddressLine1)
  {
    this.workAddressLine1 = workAddressLine1;
  }

  public String getWorkAddressLine1()
  {
    return workAddressLine1;
  }

  public void setWorkAddressLine3(String workAddressLine3)
  {
    this.workAddressLine3 = workAddressLine3;
  }

  public String getWorkAddressLine3()
  {
    return workAddressLine3;
  }

  public void setWorkAddressCity(String workAddressCity)
  {
    this.workAddressCity = workAddressCity;
  }

  public String getWorkAddressCity()
  {
    return workAddressCity;
  }

  public void setWorkAddressState(String workAddressState)
  {
    this.workAddressState = workAddressState;
  }

  public String getWorkAddressState()
  {
    return workAddressState;
  }

  public void setWorkAddressPostalCode(String workAddressPostalCode)
  {
    this.workAddressPostalCode = workAddressPostalCode;
  }

  public String getWorkAddressPostalCode()
  {
    return workAddressPostalCode;
  }

  public void setWorkAddressCountry(String workAddressCountry)
  {
    this.workAddressCountry = workAddressCountry;
  }

  public String getWorkAddressCountry()
  {
    return workAddressCountry;
  }

  public void setJobFamily(String jobFamily)
  {
    this.jobFamily = jobFamily;
  }

  public String getJobFamily()
  {
    return jobFamily;
  }

  public void setBase64ImageData(byte[] base64ImageData)
  {
    this.base64ImageData = base64ImageData;
  }

  public byte[] getBase64ImageData()
  {
    return base64ImageData;
  }

  public void setWorkAddressLine2(String workAddressLine2)
  {
    this.workAddressLine2 = workAddressLine2;
  }

  public String getWorkAddressLine2()
  {
    return workAddressLine2;
  }

  public void setWorkAddressLine4(String workAddressLine4)
  {
    this.workAddressLine4 = workAddressLine4;
  }

  public String getWorkAddressLine4()
  {
    return workAddressLine4;
  }

  public void setRegion(String region)
  {
    this.region = region;
  }

  public String getRegion()
  {
    return region;
  }

  public void setPrimaryWorkEmail(String primaryWorkEmail)
  {
    this.primaryWorkEmail = primaryWorkEmail;
  }

  public String getPrimaryWorkEmail()
  {
    return primaryWorkEmail;
  }

  public void setLegalFirstName(String legalFirstName)
  {
    this.legalFirstName = legalFirstName;
  }

  public String getLegalFirstName()
  {
    return legalFirstName;
  }

  public void setLegalLastName(String legalLastName)
  {
    this.legalLastName = legalLastName;
  }

  public String getLegalLastName()
  {
    return legalLastName;
  }

  public void setImageFileName(String imageFileName)
  {
    this.imageFileName = imageFileName;
  }

  public String getImageFileName()
  {
    return imageFileName;
  }

  public void setManagerUserName(String managerUserName)
  {
    this.managerUserName = managerUserName;
  }

  public String getManagerUserName()
  {
    return managerUserName;
  }

  public void setCostCenterHierarchyName(String costCenterHierarchyName)
  {
    this.costCenterHierarchyName = costCenterHierarchyName;
  }

  public String getCostCenterHierarchyName()
  {
    return costCenterHierarchyName;
  }

  public void setSupervisoryOrganizationName(String supervisoryOrganizationName)
  {
    this.supervisoryOrganizationName = supervisoryOrganizationName;
  }

  public String getSupervisoryOrganizationName()
  {
    return supervisoryOrganizationName;
  }
}
