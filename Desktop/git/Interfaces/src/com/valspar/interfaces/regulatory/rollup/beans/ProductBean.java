package com.valspar.interfaces.regulatory.rollup.beans;

import java.util.ArrayList;

public class ProductBean
{
  private String product;
  private String alias;
  private String aliasName;
  private String rollupItemId;
  private int level;
  private String costClass;
  private ArrayList<MsdsBean> msdsList = new ArrayList<MsdsBean>();
  
  public ProductBean()
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

  public void setLevel(int level)
  {
    this.level = level;
  }

  public int getLevel()
  {
    return level;
  }

  public void setCostClass(String costClass)
  {
    this.costClass = costClass;
  }

  public String getCostClass()
  {
    return costClass;
  }

  public void setAlias(String alias)
  {
    this.alias = alias;
  }

  public String getAlias()
  {
    return alias;
  }

  public void setMsdsList(ArrayList<MsdsBean> msdsList)
  {
    this.msdsList = msdsList;
  }

  public ArrayList<MsdsBean> getMsdsList()
  {
    return msdsList;
  }

  public void setRollupItemId(String rollupItemId)
  {
    this.rollupItemId = rollupItemId;
  }

  public String getRollupItemId()
  {
    return rollupItemId;
  }

  public void setAliasName(String aliasName)
  {
    this.aliasName = aliasName;
  }

  public String getAliasName()
  {
    return aliasName;
  }
}
