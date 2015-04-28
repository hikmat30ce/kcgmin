package com.valspar.interfaces.wercs.rollup.beans;

import java.math.BigDecimal;
import java.util.ArrayList;

public class RollupBean
{
  private String rollupId;
  private String rollupType;
  private String email;
  private String status;
  private ArrayList<String> componentList;
  private ArrayList<String> rmBoIpList;
  private ArrayList<String> postReactedList;
  private ArrayList<ProductBean> rollupList;
  private ArrayList<ProductBean> intFgRepackList;
  private boolean productImport;
  private String productImportDelay;
  private BigDecimal lowestLevel;
  private boolean error;

  public RollupBean()
  {
  }

  public void setRollupId(String rollupId)
  {
    this.rollupId = rollupId;
  }

  public String getRollupId()
  {
    return rollupId;
  }

  public void setRollupType(String rollupType)
  {
    this.rollupType = rollupType;
  }

  public String getRollupType()
  {
    return rollupType;
  }

  public void setEmail(String email)
  {
    this.email = email;
  }

  public String getEmail()
  {
    return email;
  }

  public void setComponentList(ArrayList<String> componentList)
  {
    this.componentList = componentList;
  }

  public ArrayList<String> getComponentList()
  {
    return componentList;
  }

  public void setRollupList(ArrayList<ProductBean> rollupList)
  {
    this.rollupList = rollupList;
  }

  public ArrayList<ProductBean> getRollupList()
  {
    return rollupList;
  }

  public void setError(boolean error)
  {
    this.error = error;
  }

  public boolean isError()
  {
    return error;
  }

  public void setStatus(String status)
  {
    this.status = status;
  }

  public String getStatus()
  {
    return status;
  }

  public void setProductImport(boolean productImport)
  {
    this.productImport = productImport;
  }

  public boolean isProductImport()
  {
    return productImport;
  }

  public void setLowestLevel(BigDecimal lowestLevel)
  {
    this.lowestLevel = lowestLevel;
  }

  public BigDecimal getLowestLevel()
  {
    return lowestLevel;
  }
  
  public void setPostReactedList(ArrayList<String> postReactedList)
  {
    this.postReactedList = postReactedList;
  }

  public ArrayList<String> getPostReactedList()
  {
    return postReactedList;
  }

  public void setRmBoIpList(ArrayList<String> rmBoIpList)
  {
    this.rmBoIpList = rmBoIpList;
  }

  public ArrayList<String> getRmBoIpList()
  {
    return rmBoIpList;
  }

  public void setIntFgRepackList(ArrayList<ProductBean> intFgRepackList)
  {
    this.intFgRepackList = intFgRepackList;
  }

  public ArrayList<ProductBean> getIntFgRepackList()
  {
    return intFgRepackList;
  }

  public void setProductImportDelay(String productImportDelay)
  {
    this.productImportDelay = productImportDelay;
  }

  public String getProductImportDelay()
  {
    return productImportDelay;
  }
}
