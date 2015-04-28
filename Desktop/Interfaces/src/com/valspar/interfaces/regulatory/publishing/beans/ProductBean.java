package com.valspar.interfaces.regulatory.publishing.beans;

public class ProductBean 
{
  String product;
  String format;
  String subformat;
  String language;
  String alias;
  String aliasName;
  String user;
  String id;

  public ProductBean()
  {
  }

  public String getProduct()
  {
    return product;
  }

  public void setProduct(String newProduct)
  {
    product = newProduct;
  }

  public String getFormat()
  {
    return format;
  }

  public void setFormat(String newFormat)
  {
    format = newFormat;
  }

  public String getSubformat()
  {
    return subformat;
  }

  public void setSubformat(String newSubformat)
  {
    subformat = newSubformat;
  }

  public String getLanguage()
  {
    return language;
  }

  public void setLanguage(String newLanguage)
  {
    language = newLanguage;
  }

  public String getAlias()
  {
    return alias;
  }

  public void setAlias(String newAlias)
  {
    alias = newAlias;
  }

  public String getAliasName()
  {
    return aliasName;
  }

  public void setAliasName(String newAliasName)
  {
    aliasName = newAliasName;
  }

  public String getUser()
  {
    return user;
  }

  public void setUser(String newUser)
  {
    user = newUser;
  }

  public String getId()
  {
    return id;
  }

  public void setId(String newId)
  {
    id = newId;
  }
}