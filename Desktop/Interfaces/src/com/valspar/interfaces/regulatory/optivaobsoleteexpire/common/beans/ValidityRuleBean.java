package com.valspar.interfaces.regulatory.optivaobsoleteexpire.common.beans;

public class ValidityRuleBean
{
  private String ruleId;  
  private String orgnCode;
  
  public ValidityRuleBean()
  {
  }

  public void setRuleId(String ruleId)
  {
    this.ruleId = ruleId;
  }

  public String getRuleId()
  {
    return ruleId;
  }

  public void setOrgnCode(String orgnCode)
  {
    this.orgnCode = orgnCode;
  }

  public String getOrgnCode()
  {
    return orgnCode;
  }
}
