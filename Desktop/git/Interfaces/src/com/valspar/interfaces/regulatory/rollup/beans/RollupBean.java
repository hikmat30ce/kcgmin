package com.valspar.interfaces.regulatory.rollup.beans;

import java.util.ArrayList;

public class RollupBean
{
  private String rollupId;
  private String rollupType;
  private String email;  
  private String status;
  private ArrayList<String> componentList;
  private ArrayList<String> rmBoList;
  private ArrayList<String> resinList;
  private ArrayList<ProductBean> intermediateList;
  private ArrayList<ProductBean> rollupList;
  private ArrayList<String> finishedGoodsList;
  private String optivaToWercsDelay;
  private int lowestLevel;
  private boolean error;
  private ArrayList<String> reprocessList = new ArrayList<String>();
  
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

  public void setRmBoList(ArrayList<String> rmBoList)
  {
    this.rmBoList = rmBoList;
  }

  public ArrayList<String> getRmBoList()
  {
    return rmBoList;
  }

  public void setResinList(ArrayList<String> resinList)
  {
    this.resinList = resinList;
  }

  public ArrayList<String> getResinList()
  {
    return resinList;
  }

  public void setIntermediateList(ArrayList<ProductBean> intermediateList)
  {
    this.intermediateList = intermediateList;
  }

  public ArrayList<ProductBean> getIntermediateList()
  {
    return intermediateList;
  }

  public void setLowestLevel(int lowestLevel)
  {
    this.lowestLevel = lowestLevel;
  }

  public int getLowestLevel()
  {
    return lowestLevel;
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

  public void setFinishedGoodsList(ArrayList<String> finishedGoodsList)
  {
    this.finishedGoodsList = finishedGoodsList;
  }

  public ArrayList<String> getFinishedGoodsList()
  {
    return finishedGoodsList;
  }

  public void setOptivaToWercsDelay(String optivaToWercsDelay)
  {
    this.optivaToWercsDelay = optivaToWercsDelay;
  }

  public String getOptivaToWercsDelay()
  {
    return optivaToWercsDelay;
  }

  public void setStatus(String status)
  {
    this.status = status;
  }

  public String getStatus()
  {
    return status;
  }

  public void setReprocessList(ArrayList<String> reprocessList)
  {
    this.reprocessList = reprocessList;
  }

  public ArrayList<String> getReprocessList()
  {
    return reprocessList;
  }
}
