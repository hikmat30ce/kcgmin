package com.valspar.interfaces.clx.common.beans;

import java.util.Date;
import org.apache.commons.lang3.StringUtils;

public class ShipmentStatusLineBean
{
  private String docId;
  private String senderOrgId;
  private String senderOrgName;
  private String receiverOrgId;
  private String receiverOrgName;
  private Date transDate;
  private String transId;
  private String transType;
  private String timeZone;
  private String shipmentId;
  private String delivery;
  private String reference1;
  private String reference1Type;
  private String reference2;
  private String reference2Type;
  private String equipmentDescription;
  private String trailerNumber;
  private String shipmentStatusCode;
  private String shipmentStatusReason;
  private Date shipmentStatusTime;
  private String returnMessage;
  private String returnCode;
  
  public ShipmentStatusLineBean()
  {
  }

  public void setDocId(String docId)
  {
    this.docId = docId;
  }

  public String getDocId()
  {
    return docId;
  }

  public void setSenderOrgId(String senderOrgId)
  {
    this.senderOrgId = senderOrgId;
  }

  public String getSenderOrgId()
  {
    return senderOrgId;
  }

  public void setSenderOrgName(String senderOrgName)
  {
    this.senderOrgName = senderOrgName;
  }

  public String getSenderOrgName()
  {
    return senderOrgName;
  }

  public void setReceiverOrgId(String receiverOrgId)
  {
    this.receiverOrgId = receiverOrgId;
  }

  public String getReceiverOrgId()
  {
    return receiverOrgId;
  }

  public void setReceiverOrgName(String receiverOrgName)
  {
    this.receiverOrgName = receiverOrgName;
  }

  public String getReceiverOrgName()
  {
    return receiverOrgName;
  }

  public void setTransDate(Date transDate)
  {
    this.transDate = transDate;
  }

  public Date getTransDate()
  {
    return transDate;
  }

  public void setTransId(String transId)
  {
    this.transId = transId;
  }

  public String getTransId()
  {
    return transId;
  }

  public void setTransType(String transType)
  {
    this.transType = transType;
  }

  public String getTransType()
  {
    return transType;
  }

  public void setTimeZone(String timeZone)
  {
    this.timeZone = timeZone;
  }

  public String getTimeZone()
  {
    return timeZone;
  }

  public void setShipmentId(String shipmentId)
  {
    this.shipmentId = shipmentId;
  }

  public String getShipmentId()
  {
    return shipmentId;
  }

  public void setReference1(String reference1)
  {
    this.reference1 = reference1;
  }

  public String getReference1()
  {
    return reference1;
  }

  public void setReference1Type(String reference1Type)
  {
    this.reference1Type = reference1Type;
  }

  public String getReference1Type()
  {
    return reference1Type;
  }

  public void setReference2(String reference2)
  {
    this.reference2 = reference2;
  }

  public String getReference2()
  {
    return reference2;
  }

  public void setReference2Type(String reference2Type)
  {
    this.reference2Type = reference2Type;
  }

  public String getReference2Type()
  {
    return reference2Type;
  }

  public void setEquipmentDescription(String equipmentDescription)
  {
    this.equipmentDescription = equipmentDescription;
  }

  public String getEquipmentDescription()
  {
    return equipmentDescription;
  }

  public void setTrailerNumber(String trailerNumber)
  {
    this.trailerNumber = trailerNumber;
  }

  public String getTrailerNumber()
  {
    return trailerNumber;
  }

  public void setShipmentStatusCode(String shipmentStatusCode)
  {
    this.shipmentStatusCode = shipmentStatusCode;
  }

  public String getShipmentStatusCode()
  {
    return shipmentStatusCode;
  }

  public void setShipmentStatusReason(String shipmentStatusReason)
  {
    this.shipmentStatusReason = shipmentStatusReason;
  }

  public String getShipmentStatusReason()
  {
    return shipmentStatusReason;
  }

  public void setReturnCode(String returnCode)
  {
    this.returnCode = returnCode;
  }

  public String getReturnCode()
  {
    return returnCode;
  }

  public void setShipmentStatusTime(Date shipmentStatusTime)
  {
    this.shipmentStatusTime = shipmentStatusTime;
  }

  public Date getShipmentStatusTime()
  {
    return shipmentStatusTime;
  }

  public void setDelivery(String delivery)
  {
    this.delivery = delivery;
  }

  public String getDelivery()
  {
    return delivery;
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
  
  public String toString()
  {
    StringBuilder sb = new StringBuilder();
    sb.append("transId = ");
    sb.append(this.getTransId());
    sb.append("returnMessage = ");
    sb.append(this.getReturnMessage());
    return sb.toString();
  }
}
