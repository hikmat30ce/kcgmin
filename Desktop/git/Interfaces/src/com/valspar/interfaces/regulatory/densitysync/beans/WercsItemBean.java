package com.valspar.interfaces.regulatory.densitysync.beans;

public class WercsItemBean
{

  public WercsItemBean()
  {
  }
  private String aliasName;
  private String product;
  private String id;
  private String density;
  private String densKg;
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
  public String getDensity()
  {
    return density;
  }
  public void setDensity(String density)
  {
    this.density = density;
  }
  public String getDensKg()
  {
    return densKg;
  }
  public void setDensKg(String densKg)
  {
    this.densKg = densKg;
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
}