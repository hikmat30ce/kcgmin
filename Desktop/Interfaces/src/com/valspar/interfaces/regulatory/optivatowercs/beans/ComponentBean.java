package com.valspar.interfaces.regulatory.optivatowercs.beans;

public class ComponentBean
{
  private String description;
  private String cas;
  private String componentId;
  private String percent;
  private boolean combineByCas;

  public ComponentBean()
  {
  }

  public String getDescription()
  {
    return description;
  }

  public void setDescription(String newDescription)
  {
    description = newDescription;
  }

  public String getCas()
  {
    return cas;
  }

  public void setCas(String newCas)
  {
    cas = newCas;
  }

  public String getComponentId()
  {
    return componentId;
  }

  public void setComponentId(String newComponentId)
  {
    componentId = newComponentId;
  }

  public String getPercent()
  {
    return percent;
  }

  public void setPercent(String newPercent)
  {
    percent = newPercent;
  }

  public boolean isCombineByCas()
  {
    return combineByCas;
  }

  public void setCombineByCas(boolean newCombineByCas)
  {
    combineByCas = newCombineByCas;
  }
}
