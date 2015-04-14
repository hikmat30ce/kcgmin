package com.valspar.interfaces.guardsman.pos.beans;

public class ManualReturnBean
{
  String pricingMethod;
  String pricingDescription;
  String type;
  String itemRtnCnt;
  String transRtnCnt;

  public ManualReturnBean()
  {
  }

  public void setPricingMethod(String pricingMethod)
  {
    this.pricingMethod = pricingMethod;
  }

  public String getPricingMethod()
  {
    return pricingMethod;
  }

  public void setPricingDescription(String pricingDescription)
  {
    this.pricingDescription = pricingDescription;
  }

  public String getPricingDescription()
  {
    return pricingDescription;
  }

  public void setType(String type)
  {
    this.type = type;
  }

  public String getType()
  {
    return type;
  }

  public void setItemRtnCnt(String itemRtnCnt)
  {
    this.itemRtnCnt = itemRtnCnt;
  }

  public String getItemRtnCnt()
  {
    return itemRtnCnt;
  }

  public void setTransRtnCnt(String transRtnCnt)
  {
    this.transRtnCnt = transRtnCnt;
  }

  public String getTransRtnCnt()
  {
    return transRtnCnt;
  }
}
