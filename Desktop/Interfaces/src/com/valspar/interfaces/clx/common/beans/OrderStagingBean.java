package com.valspar.interfaces.clx.common.beans;

import java.util.*;

public class OrderStagingBean extends StagingBean
{
  private String orderNumber;
  private String deliveryNumber;
  private String transferBatch;
  private String orgnCode;
  private String transferNumber;
  private String carrierSCAC;
  private Date bolPrintDate;
  private List<OrderLineBean> orderLineBeanList = new ArrayList<OrderLineBean>();
  private boolean tempControlled;
  private boolean hazmat;
  private boolean expedited;
  
  public OrderStagingBean()
  {
  }

  public void setOrderNumber(String orderNumber)
  {
    this.orderNumber = orderNumber;
  }

  public String getOrderNumber()
  {
    return orderNumber;
  }

  public void setDeliveryNumber(String deliveryNumber)
  {
    this.deliveryNumber = deliveryNumber;
  }

  public String getDeliveryNumber()
  {
    return deliveryNumber;
  }

  public void setBolPrintDate(Date bolPrintDate)
  {
    this.bolPrintDate = bolPrintDate;
  }

  public Date getBolPrintDate()
  {
    return bolPrintDate;
  }

  public void setTransferNumber(String transferNumber)
  {
    this.transferNumber = transferNumber;
  }

  public String getTransferNumber()
  {
    return transferNumber;
  }

  public void setOrgnCode(String orgnCode)
  {
    this.orgnCode = orgnCode;
  }

  public String getOrgnCode()
  {
    return orgnCode;
  }

  public boolean isInventoryTransfer()
  {
    if (this.getTransferBatch() != null)
    {
      return true;
    }
    else
    {
      return false;
    }
  }

  public void setTransferBatch(String transferBatch)
  {
    this.transferBatch = transferBatch;
  }

  public String getTransferBatch()
  {
    return transferBatch;
  }

  public void setOrderLineBeanList(List<OrderLineBean> orderLineBeanList)
  {
    this.orderLineBeanList = orderLineBeanList;
  }

  public List<OrderLineBean> getOrderLineBeanList()
  {
    return orderLineBeanList;
  }

  public String getStagingTableName()
  {
    return "VALSPAR.VCA_CLX_ORDER_STAGE";
  }
  
  public List<String> getProductIdList()
  {
    List<String> productList = new ArrayList<String>();
    for (OrderLineBean orderLineBean: this.getOrderLineBeanList())
    {
      productList.add(orderLineBean.getProductId());
    }
    return productList;
  }

  public void setTempControlled(boolean tempControlled)
  {
    this.tempControlled = tempControlled;
  }

  public boolean isTempControlled()
  {
    return tempControlled;
  }

  public void setExpedited(boolean expedited)
  {
    this.expedited = expedited;
  }

  public boolean isExpedited()
  {
    return expedited;
  }

  public void setHazmat(boolean hazmat)
  {
    this.hazmat = hazmat;
  }

  public boolean isHazmat()
  {
    return hazmat;
  }

  public void setCarrierSCAC(String carrierSCAC)
  {
    this.carrierSCAC = carrierSCAC;
  }

  public String getCarrierSCAC()
  {
    return carrierSCAC;
  }
}
