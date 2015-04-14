package com.valspar.interfaces.regulatory.msdsrequest.beans;

public class IndexBean
{
  private String product;
  private String description;
  private String upcCode;
  private String revDate;
  private String language;
  private String fileName;
  
  public IndexBean()
  {
  }

  public void setProduct(String product)
  {
    this.product = product;
  }

  public String getProduct()
  {
    return product;
  }

  public void setDescription(String description)
  {
    this.description = description;
  }

  public String getDescription()
  {
    return description;
  }

  public void setUpcCode(String upcCode)
  {
    this.upcCode = upcCode;
  }

  public String getUpcCode()
  {
    return upcCode;
  }

  public void setRevDate(String revDate)
  {
    this.revDate = revDate;
  }

  public String getRevDate()
  {
    return revDate;
  }

  public void setLanguage(String language)
  {
    this.language = language;
  }

  public String getLanguage()
  {
    return language;
  }

  public void setFileName(String fileName)
  {
    this.fileName = fileName;
  }

  public String getFileName()
  {
    return fileName;
  }
}
