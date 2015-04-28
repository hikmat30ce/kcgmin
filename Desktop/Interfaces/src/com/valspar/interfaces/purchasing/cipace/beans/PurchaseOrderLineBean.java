package com.valspar.interfaces.purchasing.cipace.beans;

import java.math.BigDecimal;

public class PurchaseOrderLineBean extends PoLinesInterfaceBean
{
  private String cipAcePoLineAutoId;
  private String invoiceCurrencyCode;
  private BigDecimal invoicedUnitPrice = BigDecimal.ZERO;
  private BigDecimal invoicedQuantity = BigDecimal.ZERO;
  private BigDecimal invoicedAmount = BigDecimal.ZERO;
  private BigDecimal receivedQuantity = BigDecimal.ZERO;
  private String projectExpenseAutoId;

  public void setCipAcePoLineAutoId(String cipAcePoLineAutoId)
  {
    this.cipAcePoLineAutoId = cipAcePoLineAutoId;
  }

  public String getCipAcePoLineAutoId()
  {
    return cipAcePoLineAutoId;
  }

  public void setProjectExpenseAutoId(String projectExpenseAutoId)
  {
    this.projectExpenseAutoId = projectExpenseAutoId;
  }

  public String getProjectExpenseAutoId()
  {
    return projectExpenseAutoId;
  }

  public void setInvoicedUnitPrice(BigDecimal invoicedUnitPrice)
  {
    this.invoicedUnitPrice = invoicedUnitPrice;
  }

  public BigDecimal getInvoicedUnitPrice()
  {
    return invoicedUnitPrice;
  }

  public void setInvoicedQuantity(BigDecimal invoicedQuantity)
  {
    this.invoicedQuantity = invoicedQuantity;
  }

  public BigDecimal getInvoicedQuantity()
  {
    return invoicedQuantity;
  }

  public void setInvoicedAmount(BigDecimal invoicedAmount)
  {
    this.invoicedAmount = invoicedAmount;
  }

  public BigDecimal getInvoicedAmount()
  {
    return invoicedAmount;
  }

  public void setReceivedQuantity(BigDecimal receivedQuantity)
  {
    this.receivedQuantity = receivedQuantity;
  }

  public BigDecimal getReceivedQuantity()
  {
    return receivedQuantity;
  }

  public void setInvoiceCurrencyCode(String invoiceCurrencyCode)
  {
    this.invoiceCurrencyCode = invoiceCurrencyCode;
  }

  public String getInvoiceCurrencyCode()
  {
    return invoiceCurrencyCode;
  }
}
