package com.valspar.interfaces.clx.common.beans;

import java.util.*;

public class ShipmentStatusStagingBean extends StagingBean
{
  private String orderNumber;
  private String deliveryNumber;
  private String transferBatch;
  private String orgnCode;
  private List<ShipmentStatusLineBean> shipmentStatusLineBeanList = new ArrayList<ShipmentStatusLineBean>();
  
  public ShipmentStatusStagingBean()
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

  public void setOrgnCode(String orgnCode)
  {
    this.orgnCode = orgnCode;
  }

  public String getOrgnCode()
  {
    return orgnCode;
  }

  public void setShipmentStatusLineBeanList(List<ShipmentStatusLineBean> shipmentStatusLineBeanList)
  {
    this.shipmentStatusLineBeanList = shipmentStatusLineBeanList;
  }

  public List<ShipmentStatusLineBean> getShipmentStatusLineBeanList()
  {
    return shipmentStatusLineBeanList;
  }
  
  public String getDeliveryOrTransferBatch()
  {
    if (this.getDeliveryNumber() != null)
    {
      return this.getDeliveryNumber();
    }
    else
    {
      return this.getTransferBatch();
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
  
  public String getStagingTableName()
  {
    return "VALSPAR.VCA_CLX_DELIVERY_STAGE";
  }
}
