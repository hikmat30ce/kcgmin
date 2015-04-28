package com.valspar.interfaces.purchasing.cipace.beans;

import java.math.BigDecimal;

public class ActualExpenseBean
{
  private String projectExpenseAutoId;
  private String currencyCode;
  private BigDecimal quantity;
  private BigDecimal unitPrice;
  private BigDecimal amount;

  public void setProjectExpenseAutoId(String projectExpenseAutoId)
  {
    this.projectExpenseAutoId = projectExpenseAutoId;
  }

  public String getProjectExpenseAutoId()
  {
    return projectExpenseAutoId;
  }

  public void setCurrencyCode(String currencyCode)
  {
    this.currencyCode = currencyCode;
  }

  public String getCurrencyCode()
  {
    return currencyCode;
  }

  public void setQuantity(BigDecimal quantity)
  {
    this.quantity = quantity;
  }

  public BigDecimal getQuantity()
  {
    return quantity;
  }

  public void setUnitPrice(BigDecimal unitPrice)
  {
    this.unitPrice = unitPrice;
  }

  public BigDecimal getUnitPrice()
  {
    return unitPrice;
  }

  public void setAmount(BigDecimal amount)
  {
    this.amount = amount;
  }

  public BigDecimal getAmount()
  {
    return amount;
  }
}
