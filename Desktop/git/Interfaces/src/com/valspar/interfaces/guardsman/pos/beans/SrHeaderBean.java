package com.valspar.interfaces.guardsman.pos.beans;

import java.util.ArrayList;

public class SrHeaderBean
{
  ArrayList SrDetails = new ArrayList();
  String transCode;
  boolean hasItems;

  public SrHeaderBean()
  {
  }

  public ArrayList getSrDetails()
  {
    return SrDetails;
  }

  public void addSrDetail(SrDetailBean inSrDetailBean)
  {
    this.getSrDetails().add(inSrDetailBean);
  }

  public String getTransCode()
  {
    return transCode;
  }

  public void setTransCode(String transCode)
  {
    this.transCode = transCode;
  }

  public boolean isHasItems()
  {
    return hasItems;
  }

  public void setHasItems(boolean hasItems)
  {
    this.hasItems = hasItems;
  }
}
