package com.valspar.interfaces.regulatory.optivatowercs.beans;

import java.util.*;

public class ProductBean
{
  private String id;
  private String formulaId;
  private String fProduct;
  private String fProductName;
  private String formulaClass;
  private String businessGroup;
  private String flashF;
  private String flashC;
  private String setCode;
  private HashMap dataCodes;
  private ArrayList componentBeans;
  private boolean sameProduct;
  private String priority;
  private String formulaCode;
  private String version;
  private String extension;
  private boolean bypassCompare;
  private String optivaVs;
  private String wercsVs;
  private String status;
  private ArrayList<String> descriptionLanguages;

  public ProductBean()
  {
  }

  public String getId()
  {
    return id;
  }

  public void setId(String newId)
  {
    id = newId;
  }

  public String getFormulaId()
  {
    return formulaId;
  }

  public void setFormulaId(String newFormulaId)
  {
    formulaId = newFormulaId;
  }

  public String getFProduct()
  {
    return fProduct;
  }

  public void setFProduct(String newFProduct)
  {
    fProduct = newFProduct;
  }

  public String getFProductName()
  {
    return fProductName;
  }

  public void setFProductName(String newFProductName)
  {
    fProductName = newFProductName;
  }

  public String getFormulaClass()
  {
    return formulaClass;
  }

  public void setFormulaClass(String newFormulaClass)
  {
    formulaClass = newFormulaClass;
  }

  public String getBusinessGroup()
  {
    return businessGroup;
  }

  public void setBusinessGroup(String newBusinessGroup)
  {
    businessGroup = newBusinessGroup;
  }

  public String getFlashF()
  {
    return flashF;
  }

  public void setFlashF(String newFlashF)
  {
    flashF = newFlashF;
  }

  public String getFlashC()
  {
    return flashC;
  }

  public void setFlashC(String newFlashC)
  {
    flashC = newFlashC;
  }

  public String getSetCode()
  {
    return setCode;
  }

  public void setSetCode(String newSetCode)
  {
    setCode = newSetCode;
  }

  public boolean isDataCodeExist(String dataCode)
  {
    if (getDataCodes().get(dataCode) == null)
      return false;
    else
      return true;
  }

  public String getDataCodeValue(String dataCode)
  {
    if (getDataCodes().get(dataCode) == null)
      return null;
    else
      return (String) getDataCodes().get(dataCode);
  }

  public HashMap getDataCodes()
  {
    return dataCodes;
  }

  public void setDataCodes(HashMap newDataCodes)
  {
    dataCodes = newDataCodes;
  }

  public ArrayList getComponentBeans()
  {
    return componentBeans;
  }

  public void setComponentBeans(ArrayList newComponentBeans)
  {
    componentBeans = newComponentBeans;
  }

  public boolean isSameProduct()
  {
    return sameProduct;
  }

  public void setSameProduct(boolean newSameProduct)
  {
    sameProduct = newSameProduct;
  }

  public String getPriority()
  {
    return priority;
  }

  public void setPriority(String newPriority)
  {
    priority = newPriority;
  }

  public String getFormulaCode()
  {
    return formulaCode;
  }

  public void setFormulaCode(String newFormulaCode)
  {
    formulaCode = newFormulaCode;
  }

  public String getVersion()
  {
    return version;
  }

  public void setVersion(String newVersion)
  {
    version = newVersion;
  }

  public String getExtension()
  {
    return extension;
  }

  public void setExtension(String newExtension)
  {
    extension = newExtension;
  }

  public boolean isBypassCompare()
  {
    return bypassCompare;
  }

  public void setBypassCompare(boolean bypassCompare)
  {
    this.bypassCompare = bypassCompare;
  }

  public String toStringComponentBean()
  {
    StringBuffer sb = new StringBuffer();
    sb.append("\n");
    sb.append(" -- ComponentBean -- ");
    sb.append("\n");

    Iterator i = this.getComponentBeans().iterator();
    while (i.hasNext())
    {
      ComponentBean cb = (ComponentBean)i.next();
      sb.append("description = ");
      sb.append(cb.getDescription());
      sb.append("\n");
      sb.append("cas = ");
      sb.append(cb.getCas());
      sb.append("\n");
      sb.append("componentId = ");
      sb.append(cb.getComponentId());
      sb.append("\n");
      sb.append("percent = ");
      sb.append(cb.getPercent());
      sb.append("\n");
      sb.append("combineByCas = ");
      sb.append(cb.isCombineByCas());
      sb.append("\n");
    }

    return sb.toString();
  }

  public void setOptivaVs(String optivaVs)
  {
    this.optivaVs = optivaVs;
  }

  public String getOptivaVs()
  {
    return optivaVs;
  }

  public void setWercsVs(String wercsVs)
  {
    this.wercsVs = wercsVs;
  }

  public String getWercsVs()
  {
    return wercsVs;
  }

  public void setStatus(String status)
  {
    this.status = status;
  }

  public String getStatus()
  {
    return status;
  }

  public void setDescriptionLanguages(ArrayList<String> descriptionLanguages)
  {
    this.descriptionLanguages = descriptionLanguages;
  }

  public ArrayList<String> getDescriptionLanguages()
  {
    return descriptionLanguages;
  }
}
