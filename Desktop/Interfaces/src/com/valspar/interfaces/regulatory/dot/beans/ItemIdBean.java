package com.valspar.interfaces.regulatory.dot.beans;

import com.valspar.interfaces.common.enums.DataSource;

public class ItemIdBean
{
  private String itemId;
  private String inventoryItemId;
  private DataSource datasource;

  public void setItemId(String itemId)
  {
    this.itemId = itemId;
  }
  public String getItemId()
  {
    return itemId;
  }
  public void setInventoryItemId(String inventoryItemId)
  {
    this.inventoryItemId = inventoryItemId;
  }
  public String getInventoryItemId()
  {
    return inventoryItemId;
  }

  public String toString()
  {
    StringBuffer sb = new StringBuffer();
    sb.append("\n");
    sb.append("\t getInventoryItemId() = ");
    sb.append(getInventoryItemId());
    sb.append("\n");
    sb.append("\t getItemId() = ");
    sb.append(getItemId());
    sb.append("\n");
    sb.append("\t getDatasource().getInstanceCodeOf11i() = ");
    sb.append(getDatasource().getInstanceCodeOf11i());
    sb.append("\n");
    return sb.toString();
  }

  public boolean isNA()
  {
    boolean isNA = false;
    
    if (this.getDatasource().getInstanceCodeOf11i().startsWith("NA"))
    {
      isNA = true;
    }
    return isNA;
  }

  public boolean isEMEAI()
  {
    boolean isEMEAI = false;
    
    if (this.getDatasource().getInstanceCodeOf11i().startsWith("IN"))
    {
      isEMEAI = true;
    }
    
    return isEMEAI;
  }
  
  public boolean isASIAPAC()
  {
    boolean isASIAPAC = false;
    
    if (this.getDatasource().getInstanceCodeOf11i().startsWith("PA"))
    {
      isASIAPAC = true;
    }
    
    return isASIAPAC;
  }

  public void setDatasource(DataSource datasource)
  {
    this.datasource = datasource;
  }

  public DataSource getDatasource()
  {
    return datasource;
  }
}
