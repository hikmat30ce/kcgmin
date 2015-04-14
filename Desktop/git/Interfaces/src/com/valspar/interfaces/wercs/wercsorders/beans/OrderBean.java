package com.valspar.interfaces.wercs.wercsorders.beans;

import com.valspar.interfaces.common.utils.*;

public class OrderBean
{
  private String plant;
  private String customerId;
  private String shipToState;
  private String shipToCountry;
  private String custOrder;
  private String quantity;
  private String alias;
  private String errorCode;
  private String shipFromCountry;

  public String getPlant()
  {
    return CommonUtility.nvl(plant);
  }
  public void setPlant(String plant)
  {
    this.plant = plant;
  }
  public void setCustomerId(String customerId)
  {
    this.customerId = customerId;
  }
  public String getCustomerId()
  {
    return CommonUtility.nvl(customerId);
  }

  public void setShipToState(String shipToState)
  {
    this.shipToState = shipToState;
  }
  public String getShipToState()
  {
    return CommonUtility.nvl(shipToState);
  }

  public void setShipToCountry(String shipToCountry)
  {
    this.shipToCountry = shipToCountry;
  }
  public String getShipToCountry()
  {
    return CommonUtility.nvl(shipToCountry);
  }
  public void setCustOrder(String custOrder)
  {
    this.custOrder = custOrder;
  }
  public String getCustOrder()
  {
    return custOrder;
  }
  public void setQuantity(String quantity)
  {
    this.quantity = quantity;
  }
  public String getQuantity()
  {
    return quantity;
  }

  public void setAlias(String alias)
  {
    this.alias = alias;
  }
  public String getAlias()
  {
    return alias;
  }
  public void setErrorCode(String errorCode)
  {
    this.errorCode = errorCode;
  }
  public String getErrorCode()
  {
    return errorCode;
  }
  public void setShipFromCountry(String shipFromCountry)
  {
    this.shipFromCountry = shipFromCountry;
  }
  public String getShipFromCountry()
  {
    return CommonUtility.nvl(shipFromCountry);
  }
}
