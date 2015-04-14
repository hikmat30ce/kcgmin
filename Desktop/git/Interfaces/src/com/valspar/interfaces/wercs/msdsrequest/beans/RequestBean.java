package com.valspar.interfaces.wercs.msdsrequest.beans;

import java.util.ArrayList;

public class RequestBean
{
  private String requestId;
  private String requestType;
  private String extractFrom;
  private String email;
  private String deliveryMethod;
  private String addedBy;
  private String dateAdded;
  private String publishDate;
  private String customerName;
  private ArrayList<String> idList = new ArrayList<String>();
  private ArrayList<ZipFileBean> fileList = new ArrayList<ZipFileBean>();
  private ArrayList<String> ftpDir = new ArrayList<String>();
  private ArrayList<IndexBean> index = new ArrayList<IndexBean>();
  
  public RequestBean()
  {
    super();
  }

  public void setRequestId(String requestId)
  {
    this.requestId = requestId;
  }

  public String getRequestId()
  {
    return requestId;
  }

  public void setRequestType(String requestType)
  {
    this.requestType = requestType;
  }

  public String getRequestType()
  {
    return requestType;
  }

  public void setExtractFrom(String extractFrom)
  {
    this.extractFrom = extractFrom;
  }

  public String getExtractFrom()
  {
    return extractFrom;
  }

  public void setEmail(String email)
  {
    this.email = email;
  }

  public String getEmail()
  {
    return email;
  }

  public void setDeliveryMethod(String deliveryMethod)
  {
    this.deliveryMethod = deliveryMethod;
  }

  public String getDeliveryMethod()
  {
    return deliveryMethod;
  }

  public void setAddedBy(String addedBy)
  {
    this.addedBy = addedBy;
  }

  public String getAddedBy()
  {
    return addedBy;
  }

  public void setDateAdded(String dateAdded)
  {
    this.dateAdded = dateAdded;
  }

  public String getDateAdded()
  {
    return dateAdded;
  }

  public void setFileList(ArrayList<ZipFileBean> fileList)
  {
    this.fileList = fileList;
  }

  public ArrayList<ZipFileBean> getFileList()
  {
    return fileList;
  }

  public void setIdList(ArrayList<String> idList)
  {
    this.idList = idList;
  }

  public ArrayList<String> getIdList()
  {
    return idList;
  }

  public void setFtpDir(ArrayList<String> ftpDir)
  {
    this.ftpDir = ftpDir;
  }

  public ArrayList<String> getFtpDir()
  {
    return ftpDir;
  }

  public void setPublishDate(String publishDate)
  {
    this.publishDate = publishDate;
  }

  public String getPublishDate()
  {
    return publishDate;
  }

  public void setCustomerName(String customerName)
  {
    this.customerName = customerName;
  }

  public String getCustomerName()
  {
    return customerName;
  }

  public void setIndex(ArrayList<IndexBean> index)
  {
    this.index = index;
  }

  public ArrayList<IndexBean> getIndex()
  {
    return index;
  }
}
