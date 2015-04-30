package com.valspar.interfaces.clx.common.beans;

import java.util.Date;
import org.apache.commons.lang3.StringUtils;

public class StagingBean
{
  private String actionCode;
  private String eventType;
  private String erpUserId;
  private String transId;
  private String status;
  private String returnMessage;
  private String returnCode;
  private Date creationDate;
  private Date lastUpdateDate;
  private String lastUpdatedBy;
  private String senderIdKey;
  private String senderId;
  private String generatedXmlMessage;
  private String stagingTableName;
  
  public StagingBean()
  {
  }

  public void setErpUserId(String erpUserId)
  {
    this.erpUserId = erpUserId;
  }

  public String getErpUserId()
  {
    return erpUserId;
  }

  public void setTransId(String transId)
  {
    this.transId = transId;
  }

  public String getTransId()
  {
    return transId;
  }

  public void setStagingTableName(String stagingTableName)
  {
    this.stagingTableName = stagingTableName;
  }

  public String getStagingTableName()
  {
    return stagingTableName;
  }

  public void setSenderIdKey(String senderIdKey)
  {
    this.senderIdKey = senderIdKey;
  }

  public String getSenderIdKey()
  {
    return senderIdKey;
  }

  public void setSenderId(String senderId)
  {
    this.senderId = senderId;
  }

  public String getSenderId()
  {
    return senderId;
  }

  public void setStatus(String status)
  {
    this.status = status;
  }

  public String getStatus()
  {
    return status;
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

  public void setReturnMessage(String returnMessage)
  {
    this.returnMessage = returnMessage;
  }

  public String getReturnMessage()
  {
    return returnMessage;
  }

  public void setReturnCode(String returnCode)
  {
    this.returnCode = returnCode;
  }

  public String getReturnCode()
  {
    return returnCode;
  }

  public void setGeneratedXmlMessage(String generatedXmlMessage)
  {
    this.generatedXmlMessage = generatedXmlMessage;
  }

  public String getGeneratedXmlMessage()
  {
    return generatedXmlMessage;
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

  public void setLastUpdatedBy(String lastUpdatedBy)
  {
    this.lastUpdatedBy = lastUpdatedBy;
  }

  public String getLastUpdatedBy()
  {
    return lastUpdatedBy;
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
}
