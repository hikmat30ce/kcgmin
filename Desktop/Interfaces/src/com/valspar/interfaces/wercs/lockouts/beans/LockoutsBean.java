package com.valspar.interfaces.wercs.lockouts.beans;

public class LockoutsBean
{
  private String casNumber;
  private String componentId;
  private String quantity;
  private String yearRequested;

  public LockoutsBean()
  {
  }

  public void setCasNumber(String casNumber)
  {
    this.casNumber = casNumber;
  }

  public String getCasNumber()
  {
    return casNumber;
  }

  public void setComponentId(String componentId)
  {
    this.componentId = componentId;
  }

  public String getComponentId()
  {
    return componentId;
  }

  public void setQuantity(String quantity)
  {
    this.quantity = quantity;
  }

  public String getQuantity()
  {
    return quantity;
  }

  public void setYearRequested(String yearRequested)
  {
    this.yearRequested = yearRequested;
  }

  public String getYearRequested()
  {
    return yearRequested;
  }
  
  public String toString()
  {
    StringBuilder sb = new StringBuilder();
    sb.append("CAS No: ");
    sb.append(getCasNumber());
    sb.append(", ComponentId: ");
    sb.append(getComponentId());
    sb.append(", Quantity: ");
    sb.append(getQuantity());
    sb.append(", Year: ");
    sb.append(getYearRequested());
    return (sb.toString());
  }
}
