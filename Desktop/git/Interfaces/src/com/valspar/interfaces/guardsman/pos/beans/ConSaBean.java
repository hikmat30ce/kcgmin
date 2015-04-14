package com.valspar.interfaces.guardsman.pos.beans;

public class ConSaBean
{
  String coverageType;
  String saAmt;
  String saTypeId;
  String pricingCode;
  String samConSAId;
  String delPurDt;

  public ConSaBean()
  {
  }

  public String getCoverageType()
  {
    return coverageType;
  }

  public void setCoverageType(String coverageType)
  {
    this.coverageType = coverageType;
  }

  public String getSaAmt()
  {
    return saAmt;
  }

  public void setSaAmt(String saAmt)
  {
    this.saAmt = saAmt;
  }

  public String getSaTypeId()
  {
    return saTypeId;
  }

  public void setSaTypeId(String saTypeId)
  {
    this.saTypeId = saTypeId;
  }

  public String getSamConSAId()
  {
    return samConSAId;
  }

  public void setSamConSAId(String samConSAId)
  {
    this.samConSAId = samConSAId;
  }

  public void setDelPurDt(String delPurDt)
  {
    this.delPurDt = delPurDt;
  }

  public String getDelPurDt()
  {
    return delPurDt;
  }

  public void setPricingCode(String pricingCode)
  {
    this.pricingCode = pricingCode;
  }

  public String getPricingCode()
  {
    return pricingCode;
  }
}
