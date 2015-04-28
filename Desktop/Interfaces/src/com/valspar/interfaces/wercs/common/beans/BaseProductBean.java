package com.valspar.interfaces.wercs.common.beans;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import org.apache.commons.lang.StringUtils;

public class BaseProductBean
{
  private String formulaId;
  private String fProduct;
  private String fProductName;
  private String formulaClass;
  private String groupCode;
  private String businessGroup;
  private String region;
  private String flashF;
  private String flashC;
  private String setCode;
  private HashMap dataCodes;
  private HashMap textCodes;
  private String formulaCode;
  private String version;
  private String extension;
  private ArrayList<String> descriptionLanguages;
  private List<String> aliasList;
  
  public BaseProductBean()
  {
  }

  public void setFormulaId(String formulaId)
  {
    this.formulaId = formulaId;
  }

  public String getFormulaId()
  {
    return formulaId;
  }

  public void setFProduct(String fProduct)
  {
    this.fProduct = fProduct;
  }

  public String getFProduct()
  {
    return fProduct;
  }

  public void setFProductName(String fProductName)
  {
    this.fProductName = fProductName;
  }

  public String getFProductName()
  {
    return fProductName;
  }

  public void setFormulaClass(String formulaClass)
  {
    this.formulaClass = formulaClass;
  }

  public String getFormulaClass()
  {
    return formulaClass;
  }

  public void setBusinessGroup(String businessGroup)
  {
    this.businessGroup = businessGroup;
  }

  public String getBusinessGroup()
  {
    return businessGroup;
  }

  public void setFlashF(String flashF)
  {
    this.flashF = flashF;
  }

  public String getFlashF()
  {
    return flashF;
  }

  public void setFlashC(String flashC)
  {
    this.flashC = flashC;
  }

  public String getFlashC()
  {
    return flashC;
  }

  public void setSetCode(String setCode)
  {
    this.setCode = setCode;
  }

  public String getSetCode()
  {
    return setCode;
  }

  public void setDataCodes(HashMap dataCodes)
  {
    this.dataCodes = dataCodes;
  }

  public HashMap getDataCodes()
  {
    return dataCodes;
  }

  public void setFormulaCode(String formulaCode)
  {
    this.formulaCode = formulaCode;
  }

  public String getFormulaCode()
  {
    return formulaCode;
  }

  public void setVersion(String version)
  {
    this.version = version;
  }

  public String getVersion()
  {
    return version;
  }

  public void setExtension(String extension)
  {
    this.extension = extension;
  }

  public String getExtension()
  {
    return extension;
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

  public void setDescriptionLanguages(ArrayList<String> descriptionLanguages)
  {
    this.descriptionLanguages = descriptionLanguages;
  }

  public ArrayList<String> getDescriptionLanguages()
  {
    return descriptionLanguages;
  }

  public void setGroupCode(String groupCode)
  {
    this.groupCode = groupCode;
  }

  public String getGroupCode()
  {
    return groupCode;
  }

  public void setRegion(String region)
  {
    this.region = region;
  }

  public String getRegion()
  {
      return region;
  }

  public void setTextCodes(HashMap textCodes)
  {
    this.textCodes = textCodes;
  }

  public HashMap getTextCodes()
  {
    return textCodes;
  }

  public void setAliasList(List<String> aliasList)
  {
    this.aliasList = aliasList;
  }

  public List<String> getAliasList()
  {
    return aliasList;
  }
}
