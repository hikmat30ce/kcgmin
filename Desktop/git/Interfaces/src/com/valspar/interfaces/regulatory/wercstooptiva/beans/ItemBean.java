package com.valspar.interfaces.regulatory.wercstooptiva.beans;

import java.util.*;
import org.apache.commons.lang3.StringUtils;

public class ItemBean
{
  private String product;
  private String aliasName;
  private String busgp;
  private String cas;
  private String statusInd;
  private java.util.HashMap strow = new HashMap();
  private java.util.HashMap tp0row = new HashMap();
  private java.util.HashMap tp1row = new HashMap();
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

  public void setTp0row(java.util.HashMap tp0row)
  {
    this.tp0row = tp0row;
  }

  public java.util.HashMap getTp0row()
  {
    return tp0row;
  }

  public void setTp1row(java.util.HashMap tp1row)
  {
    this.tp1row = tp1row;
  }

  public java.util.HashMap getTp1row()
  {
    return tp1row;
  }

  public void setStrow(java.util.HashMap strow)
  {
    this.strow = strow;
  }

  public java.util.HashMap getStrow()
  {
    return strow;
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
}
