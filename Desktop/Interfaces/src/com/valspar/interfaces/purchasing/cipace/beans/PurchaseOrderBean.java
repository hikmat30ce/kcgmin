package com.valspar.interfaces.purchasing.cipace.beans;

import java.util.List;

public class PurchaseOrderBean extends PoHeadersInterfaceBean
{
  private List<PurchaseOrderLineBean> lines;

  public void setLines(List<PurchaseOrderLineBean> lines)
  {
    this.lines = lines;
  }

  @Override
  public List<PurchaseOrderLineBean> getLines()
  {
    return lines;
  }
}
