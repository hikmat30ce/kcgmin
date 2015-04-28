package com.valspar.interfaces.wercs.densitysync.beans;

public class WercsItemBean
{

  public WercsItemBean()
  {
  }
  private String aliasName;
  private String product;
  private String id;
  private String densLb;
  private String denKgl;
  private String alias;
  private String businessGroup;
  
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
  public String getId()
  {
    return id;
  }
  public void setId(String id)
  {
    this.id = id;
  }

  public String getAlias()
  {
    return alias;
  }
  public void setAlias(String alias)
  {
    this.alias = alias;
  }
  public String getBusinessGroup()
  {
    return businessGroup;
  }
  public void setBusinessGroup(String businessGroup)
  {
    this.businessGroup = businessGroup;
  }

  public void setDensLb(String densLb)
  {
    this.densLb = densLb;
  }

  public String getDensLb()
  {
    return densLb;
  }

  public void setDenKgl(String denKgl)
  {
    this.denKgl = denKgl;
  }

  public String getDenKgl()
  {
    return denKgl;
  }
}
