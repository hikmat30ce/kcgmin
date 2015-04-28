package com.valspar.interfaces.guardsman.pos.beans;

public class SaTypeBean
{
  String saTypeId;
  String erpNo;
  String coverageType;
  String startDate;
  String endDate;

  public SaTypeBean()
  {
  }

  public String getSaTypeId()
  {
    return saTypeId;
  }

  public void setSaTypeId(String saTypeId)
  {
    this.saTypeId = saTypeId;
  }

  public String getErpNo()
  {
    return erpNo;
  }

  public void setErpNo(String erpNo)
  {
    this.erpNo = erpNo;
  }

  public String getCoverageType()
  {
    return coverageType;
  }

  public void setCoverageType(String coverageType)
  {
    this.coverageType = coverageType;
  }

  public String getStartDate()
  {
    return startDate;
  }

  public void setStartDate(String startDate)
  {
    this.startDate = startDate;
  }

  public String getEndDate()
  {
    return endDate;
  }

  public void setEndDate(String endDate)
  {
    this.endDate = endDate;
  }
}
