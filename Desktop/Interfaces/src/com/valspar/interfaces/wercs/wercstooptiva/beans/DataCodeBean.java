package com.valspar.interfaces.wercs.wercstooptiva.beans;

import org.apache.log4j.Logger;

public class DataCodeBean implements Cloneable
{
  private static Logger log4jLogger = Logger.getLogger(DataCodeBean.class);

  private String table;
  private String defaultValue;
  private String product;
  private String optivaDataCode;
  private String wercsDataCode;
  private String value;
  private boolean returnActualValue;
  private String defaultPositiveValue;
  private boolean dataCodeFound;

  public void setTable(String table)
  {
    this.table = table;
  }

  public String getTable()
  {
    return table;
  }

  public void setDefaultValue(String defaultValue)
  {
    this.defaultValue = defaultValue;
  }

  public String getDefaultValue()
  {
    return defaultValue;
  }

  public void setProduct(String product)
  {
    this.product = product;
  }

  public String getProduct()
  {
    return product;
  }

  public void setOptivaDataCode(String optivaDataCode)
  {
    this.optivaDataCode = optivaDataCode;
  }

  public String getOptivaDataCode()
  {
    return optivaDataCode;
  }

  public void setWercsDataCode(String wercsDataCode)
  {
    this.wercsDataCode = wercsDataCode;
  }

  public String getWercsDataCode()
  {
    return wercsDataCode;
  }

  public void setValue(String value)
  {
    this.value = value;
  }

  public String getValue()
  {
    return value;
  }

  public Object clone()
  {
    try
    {
      return super.clone();
    }
    catch (CloneNotSupportedException e)
    {
      log4jLogger.error(e);
    }
    return null;
  }

  public void setReturnActualValue(boolean returnActualValue)
  {
    this.returnActualValue = returnActualValue;
  }

  public boolean isReturnActualValue()
  {
    return returnActualValue;
  }

  public void setDefaultPositiveValue(String defaultPositiveValue)
  {
    this.defaultPositiveValue = defaultPositiveValue;
  }

  public String getDefaultPositiveValue()
  {
    return defaultPositiveValue;
  }

  public void setDataCodeFound(boolean dataCodeFound)
  {
    this.dataCodeFound = dataCodeFound;
  }

  public boolean isDataCodeFound()
  {
    return dataCodeFound;
  }
}
