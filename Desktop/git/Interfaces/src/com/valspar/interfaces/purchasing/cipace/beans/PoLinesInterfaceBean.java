package com.valspar.interfaces.purchasing.cipace.beans;

import java.math.BigDecimal;
import java.util.Date;

public class PoLinesInterfaceBean
{
  private String interfaceLineId;

  private String itemCategoryId;
  private String itemCategory;
  private String itemDescription;
  private String uom;
  private BigDecimal quantity;
  private BigDecimal unitPrice;
  private Date promisedDate;
  private int lineNumber;
  
  public String getAction()
  {
    return "ORIGINAL";
  }

  public void setUom(String uom)
  {
    this.uom = uom;
  }

  public String getUom()
  {
    return uom;
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

  public void setPromisedDate(Date promisedDate)
  {
    this.promisedDate = promisedDate;
  }

  public Date getPromisedDate()
  {
    return promisedDate;
  }

  public void setLineNumber(int lineNumber)
  {
    this.lineNumber = lineNumber;
  }

  public int getLineNumber()
  {
    return lineNumber;
  }

  public void setInterfaceLineId(String interfaceLineId)
  {
    this.interfaceLineId = interfaceLineId;
  }

  public String getInterfaceLineId()
  {
    return interfaceLineId;
  }

  public void setItemDescription(String itemDescription)
  {
    this.itemDescription = itemDescription;
  }

  public String getItemDescription()
  {
    return itemDescription;
  }

  public void setItemCategoryId(String itemCategoryId)
  {
    this.itemCategoryId = itemCategoryId;
  }

  public String getItemCategoryId()
  {
    return itemCategoryId;
  }

  public void setItemCategory(String itemCategory)
  {
    this.itemCategory = itemCategory;
  }

  public String getItemCategory()
  {
    return itemCategory;
  }
}
