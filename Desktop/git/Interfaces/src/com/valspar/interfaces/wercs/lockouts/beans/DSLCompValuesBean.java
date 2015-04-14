package com.valspar.interfaces.wercs.lockouts.beans;

public class DSLCompValuesBean
{
  private String componentId;
  private double initYearAmt = 0.0;
  private double initLifeAmt = 0.0;
  private double maxYearAmt = 0.0;
  private double maxLifeAmt = 0.0;
  private double maxTpctAmt = 0.0;
  private double newAmt = 0.0;

  public DSLCompValuesBean()
  {
  }

  public void setComponentId(String componentId)
  {
    this.componentId = componentId;
  }

  public String getComponentId()
  {
    return componentId;
  }

  public void setInitYearAmt(double initYearAmt)
  {
    this.initYearAmt = initYearAmt;
  }

  public double getInitYearAmt()
  {
    return initYearAmt;
  }

  public void setInitLifeAmt(double initLifeAmt)
  {
    this.initLifeAmt = initLifeAmt;
  }

  public double getInitLifeAmt()
  {
    return initLifeAmt;
  }

  public void setMaxYearAmt(double maxYearAmt)
  {
    this.maxYearAmt = maxYearAmt;
  }

  public double getMaxYearAmt()
  {
    return maxYearAmt;
  }

  public void setMaxLifeAmt(double maxLifeAmt)
  {
    this.maxLifeAmt = maxLifeAmt;
  }

  public double getMaxLifeAmt()
  {
    return maxLifeAmt;
  }

  public void setMaxTpctAmt(double maxTpctAmt)
  {
    this.maxTpctAmt = maxTpctAmt;
  }

  public double getMaxTpctAmt()
  {
    return maxTpctAmt;
  }

  public void setNewAmt(double newAmt)
  {
    this.newAmt = newAmt;
  }

  public double getNewAmt()
  {
    return newAmt;
  }
}
