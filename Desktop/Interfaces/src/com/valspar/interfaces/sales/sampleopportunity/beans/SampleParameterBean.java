package com.valspar.interfaces.sales.sampleopportunity.beans;

public class SampleParameterBean
{
  private String id;
  private String shipTo;
  private String product;
  private String closeDate;
  private String recordType;
  private String recordTypeId;
  private String ownerId;
  
  public SampleParameterBean()
  {
  }

  public void setShipTo(String shipTo)
  {
    this.shipTo = shipTo;
  }

  public String getShipTo()
  {
    return shipTo;
  }
  
  public void setProduct(String product)
  {
    this.product = product;
  }

  public String getProduct()
  {
    return product;
  }

  public void setCloseDate(String closeDate)
  {
    this.closeDate = closeDate;
  }

  public String getCloseDate()
  {
    return closeDate;
  }

  public void setId(String id)
  {
    this.id = id;
  }

  public String getId()
  {
    return id;
  }

  public void setRecordType(String recordType)
  {
    this.recordType = recordType;
  }

  public String getRecordType()
  {
    return recordType;
  }

  public void setRecordTypeId(String recordTypeId)
  {
    this.recordTypeId = recordTypeId;
  }

  public String getRecordTypeId()
  {
    return recordTypeId;
  }

  public void setOwnerId(String ownerId)
  {
    this.ownerId = ownerId;
  }

  public String getOwnerId()
  {
    return ownerId;
  }
}
