package com.valspar.interfaces.guardsman.pos.beans;

public class PlanPrintingBean
{
  String saTypeDescription;
  String saType;
  String planCnt;
  
  public PlanPrintingBean()
  {
  }

  public void setSaTypeDescription(String saTypeDescription)
  {
    this.saTypeDescription = saTypeDescription;
  }

  public String getSaTypeDescription()
  {
    return saTypeDescription;
  }

  public void setSaType(String saType)
  {
    this.saType = saType;
  }

  public String getSaType()
  {
    return saType;
  }

  public void setPlanCnt(String planCnt)
  {
    this.planCnt = planCnt;
  }

  public String getPlanCnt()
  {
    return planCnt;
  }
}
