package com.valspar.interfaces.wercs.wercstooptiva.beans;

import java.util.*;
import org.apache.commons.lang3.StringUtils;

public class ItemBean
{
  private String product;
  private String aliasName;
  private String busgp;
  private String cas;
  private String statusInd;
  private Map<String, DataCodeBean> strow = new HashMap<String, DataCodeBean>();
  private Map<String, DataCodeBean> tp0row = new HashMap<String, DataCodeBean>();
  private Map<String, DataCodeBean> tp1row = new HashMap<String, DataCodeBean>();
  private String chemName;
  private String rmgDesc; 
  
  public String getProduct()
  {
    return product;
  }

  public void setProduct(String product)
  {
    this.product = product;
  }

  public void setAliasName(String aliasName)
  {
    this.aliasName = aliasName;
  }

  public String getAliasName()
  {
    return aliasName;
  }

  public void setBusgp(String busgp)
  {
    this.busgp = busgp;
  }

  public String getBusgp()
  {
    return busgp;
  }

  public void setCas(String cas)
  {
    this.cas = cas;
  }

  public String getCas()
  {
    return cas;
  }

  public void setStatusInd(String statusInd)
  {
    this.statusInd = statusInd;
  }

  public boolean isComponent()
  {
    if (StringUtils.startsWithIgnoreCase(getProduct(), "C") && StringUtils.containsIgnoreCase(getProduct(), "-"))
    {
      return true;
    }
    else
    {
      return false;
    }
  }

  public String getStatusInd()
  {
    if (isComponent())
    {
      return "99";
    }
    else
    {
      return "400";
    }
  }

  public void setChemName(String chemName)
  {
    this.chemName = chemName;
  }

  public String getChemName()
  {
    return chemName;
  }

  public void setRmgDesc(String rmgDesc)
  {
    this.rmgDesc = rmgDesc;
  }

  public String getRmgDesc()
  {
    return rmgDesc;
  }

  public void setStrow(Map<String, DataCodeBean> strow)
  {
    this.strow = strow;
  }

  public Map<String, DataCodeBean> getStrow()
  {
    return strow;
  }

  public void setTp0row(Map<String, DataCodeBean> tp0row)
  {
    this.tp0row = tp0row;
  }

  public Map<String, DataCodeBean> getTp0row()
  {
    return tp0row;
  }

  public void setTp1row(Map<String, DataCodeBean> tp1row)
  {
    this.tp1row = tp1row;
  }

  public Map<String, DataCodeBean> getTp1row()
  {
    return tp1row;
  }
}
