package com.valspar.interfaces.wercs.optivatowercs.beans;

import com.valspar.interfaces.wercs.common.beans.BaseProductBean;

public class ProductBean extends BaseProductBean
{
  private String id;
  private boolean sameProduct;
  private String priority;
  private boolean bypassCompare;
  private String status;

  public ProductBean()
  {
  }

  public String getId()
  {
    return id;
  }

  public void setId(String newId)
  {
    id = newId;
  }

  public boolean isSameProduct()
  {
    return sameProduct;
  }

  public void setSameProduct(boolean newSameProduct)
  {
    sameProduct = newSameProduct;
  }

  public String getPriority()
  {
    return priority;
  }

  public void setPriority(String newPriority)
  {
    priority = newPriority;
  }

  public boolean isBypassCompare()
  {
    return bypassCompare;
  }

  public void setBypassCompare(boolean bypassCompare)
  {
    this.bypassCompare = bypassCompare;
  }

  public void setStatus(String status)
  {
    this.status = status;
  }

  public String getStatus()
  {
    return status;
  }
}
