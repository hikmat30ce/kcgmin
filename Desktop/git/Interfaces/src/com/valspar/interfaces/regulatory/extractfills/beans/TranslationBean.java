package com.valspar.interfaces.regulatory.extractfills.beans;

import com.valspar.interfaces.common.enums.DataSource;

public class TranslationBean
{
  private String itemId;
  private String translation;
  private String alias;
  private String isoLanguage;
  private String oracleLanguage;
  private DataSource datasource;
  
  public TranslationBean()
  {
  }

  public void setTranslation(String translation)
  {
    this.translation = translation;
  }

  public String getTranslation()
  {
    return translation;
  }

  public void setAlias(String alias)
  {
    this.alias = alias;
  }

  public String getAlias()
  {
    return alias;
  }

  public void setIsoLanguage(String isoLanguage)
  {
    this.isoLanguage = isoLanguage;
  }

  public String getIsoLanguage()
  {
    return isoLanguage;
  }

  public void setItemId(String itemId)
  {
    this.itemId = itemId;
  }

  public String getItemId()
  {
    return itemId;
  }

  public void setOracleLanguage(String oracleLanguage)
  {
    this.oracleLanguage = oracleLanguage;
  }

  public String getOracleLanguage()
  {
    return oracleLanguage;
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
