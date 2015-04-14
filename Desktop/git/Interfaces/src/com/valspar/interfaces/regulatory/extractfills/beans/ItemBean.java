package com.valspar.interfaces.regulatory.extractfills.beans;

import com.valspar.interfaces.common.enums.DataSource;
import com.valspar.interfaces.common.utils.CommonUtility;

public class ItemBean
{
  private String id;
  private String alias;
  private String product;
  private String aliasName;
  private String reason;
  private String customer;
  private boolean publishStripped = false;
  private String strippedAlias;
  private DataSource datasource;

  public String getId()
  {
    return id;
  }

  public void setId(String newId)
  {
    id = newId;
  }

  public void setAlias(String alias)
  {
    this.alias = alias;
  }

  public String getAlias()
  {
    return alias;
  }

  public void setProduct(String product)
  {
    this.product = product;
  }

  public String getProduct()
  {
    return product;
  }

  public void setAliasName(String aliasName)
  {
    this.aliasName = aliasName;
  }

  public String getAliasName()
  {
    return CommonUtility.nvl(aliasName);
  }

  public void setReason(String reason)
  {
    this.reason = reason;
  }

  public String getReason()
  {
    return reason;
  }

  public void setCustomer(String customer)
  {
    this.customer = customer;
  }

  public String getCustomer()
  {
    return customer;
  }

  public void setPublishStripped(boolean publishStripped)
  {
    this.publishStripped = publishStripped;
  }

  public boolean isPublishStripped()
  {
    return publishStripped;
  }

  public void setStrippedAlias(String strippedAlias)
  {
    this.strippedAlias = strippedAlias;
  }

  public String getStrippedAlias()
  {
    return strippedAlias;
  }

  public void setDatasource(DataSource datasource)
  {
    this.datasource = datasource;
  }

  public DataSource getDatasource()
  {
    return datasource;
  }
}
