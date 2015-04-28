package com.valspar.interfaces.regulatory.rawmaterials.beans;

public class WercsItemBean
{

  public WercsItemBean()
  {
  }
  private String aliasName;
  private String product;
  private String dummyType;
  private String id;
  public String getAliasName()
  {
    return aliasName;
  }
  public void setAliasName(String aliasName)
  {
    this.aliasName = aliasName;
  }
  public void setProduct(String product)
  {
    this.product = product;
  }
  public String getProduct()
  {
    return product;
  }
  public void setDummyType(String dummyType)
  {
    this.dummyType = dummyType;
  }
  public String getDummyType()
  {
    return dummyType;
  }

  public String getId()
  {
    return id;
  }

  public void setId(String id)
  {
    this.id = id;
  }
}