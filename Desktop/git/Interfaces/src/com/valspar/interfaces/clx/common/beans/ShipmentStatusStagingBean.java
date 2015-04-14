package com.valspar.interfaces.clx.common.beans;

import java.util.*;
import org.apache.commons.lang3.StringUtils;

public class ShipmentStatusStagingBean
{
  private String orderNumber;
  private String deliveryNumber;
  private String batchNumber;
  private String orgnCode;
  private String actionCode;
  private String eventType;
  private Date creationDate;
  private Date lastUpdateDate;
  private String transId;
  private String status;
  private String lastUpdatedBy;
  private String returnCode;
  private String returnMessage;
  private List<ShipmentStatusLineBean> shipmentStatusLineBeanList = new ArrayList<ShipmentStatusLineBean>();
  private String generatedXmlMessage;
  private String senderId;
  private String senderIdKey;
  
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

  public void setActionCode(String actionCode)
  {
    this.actionCode = actionCode;
  }

  public String getActionCode()
  {
    return actionCode;
  }

  public void setEventType(String eventType)
  {
    this.eventType = eventType;
  }

  public String getEventType()
  {
    return eventType;
  }

  public void setTransId(String transId)
  {
    this.transId = transId;
  }

  public String getTransId()
  {
    return transId;
  }

  public void setStatus(String status)
  {
    this.status = status;
  }

  public String getStatus()
  {
    return status;
  }

  public void setLastUpdatedBy(String lastUpdatedBy)
  {
    this.lastUpdatedBy = lastUpdatedBy;
  }

  public String getLastUpdatedBy()
  {
    return lastUpdatedBy;
  }

  public void setCreationDate(Date creationDate)
  {
    this.creationDate = creationDate;
  }

  public Date getCreationDate()
  {
    return creationDate;
  }

  public void setLastUpdateDate(Date lastUpdateDate)
  {
    this.lastUpdateDate = lastUpdateDate;
  }

  public Date getLastUpdateDate()
  {
    return lastUpdateDate;
  }
  
  public boolean isDeleteAction()
  {
    if (StringUtils.equalsIgnoreCase(this.getActionCode(), "DELETE"))
    {
      return true;
    }
    else 
    {
      return false;
    }
  }

  public void setBatchNumber(String batchNumber)
  {
    this.batchNumber = batchNumber;
  }

  public String getBatchNumber()
  {
    return batchNumber;
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

  public void setReturnCode(String returnCode)
  {
    this.returnCode = returnCode;
  }

  public String getReturnCode()
  {
    return returnCode;
  }

  public void setReturnMessage(String returnMessage)
  {
    this.returnMessage = returnMessage;
  }

  public String getReturnMessage()
  {
    return returnMessage;
  }
  
  public String getInterfaceStatusCode()
  {
    if (this.getReturnCode() != null && StringUtils.equalsIgnoreCase(this.getReturnCode(), "0"))
    {
      return "S";
    }
    else
    {
      return "E";
    }
  }
  
  public boolean isErrorStatus()
  {
    if (StringUtils.equalsIgnoreCase(this.getInterfaceStatusCode(), "E"))
    {
      return true;
    }
    else
    {
      return false;
    }
  }

  public void setGeneratedXmlMessage(String generatedXmlMessage)
  {
    this.generatedXmlMessage = generatedXmlMessage;
  }

  public String getGeneratedXmlMessage()
  {
    return generatedXmlMessage;
  }
  
  public String getDeliveryOrTransferBatch()
  {
    if (this.getDeliveryNumber() != null)
    {
      return this.getDeliveryNumber();
    }
    else
    {
      return this.getBatchNumber();
    }
  }

  public void setSenderId(String senderId)
  {
    this.senderId = senderId;
  }

  public String getSenderId()
  {
    return senderId;
  }

  public void setSenderIdKey(String senderIdKey)
  {
    this.senderIdKey = senderIdKey;
  }

  public String getSenderIdKey()
  {
    return senderIdKey;
  }
}
