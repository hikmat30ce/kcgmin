package com.valspar.interfaces.regulatory.dotcalc.beans;

import java.util.*;

public class ProductBean
{
  private String product;
  private String firstHazardClass;
  private String secondHazardClass;
  private String corrosiveReview;
  private String matchingRuleNumber;
  private String formulaClass;
  private String inactive;
  private ArrayList dotRuleList = new ArrayList();
  private HashMap dataCodes = new HashMap();
  private String euMatchingRuleNumber;
  private String euFirstHazardClass;
  private String euSecondHazardClass;

  public ProductBean()
  {
  }

  public String getProduct()
  {
    return product;
  }

  public void setProduct(String product)
  {
    this.product = product;
  }

  public void addDataCode(String dataCode, String data)
  {
    getDataCodes().put(dataCode, data);
  }

  public String getData(String fDataCode)
  {
    Object o = getDataCodes().get(fDataCode);

    if (o != null)
      return (String) o;
    else
    return null;
  }

  public String getFirstHazardClass()
  {
    return firstHazardClass;
  }

  public void setFirstHazardClass(String firstHazardClass)
  {
    this.firstHazardClass = firstHazardClass;
  }

  public String getSecondHazardClass()
  {
    return secondHazardClass;
  }

  public void setSecondHazardClass(String secondHazardClass)
  {
    this.secondHazardClass = secondHazardClass;
  }

  public String getCorrosiveReview()
  {
    return corrosiveReview;
  }

  public void setCorrosiveReview(String corrosiveReview)
  {
    this.corrosiveReview = corrosiveReview;
  }

  public String getMatchingRuleNumber()
  {
    return matchingRuleNumber;
  }

  public void setMatchingRuleNumber(String matchingRuleNumber)
  {
    this.matchingRuleNumber = matchingRuleNumber;
  }

  public String getFormulaClass()
  {
    return formulaClass;
  }

  public void setFormulaClass(String formulaClass)
  {
    this.formulaClass = formulaClass;
  }

  public HashMap getDataCodes()
  {
    return dataCodes;
  }

  public void setDataCodes(HashMap dataCodes)
  {
    this.dataCodes = dataCodes;
  }

  public void setDotRuleList(ArrayList dotRuleList)
  {
    this.dotRuleList = dotRuleList;
  }

  public ArrayList getDotRuleList()
  {
    return dotRuleList;
  }

  public void setInactive(String inactive)
  {
    this.inactive = inactive;
  }

  public String getInactive()
  {
    return inactive;
  }

  public void setEuMatchingRuleNumber(String euMatchingRuleNumber)
  {
    this.euMatchingRuleNumber = euMatchingRuleNumber;
  }

  public String getEuMatchingRuleNumber()
  {
    return euMatchingRuleNumber;
  }

  public void setEuFirstHazardClass(String euFirstHazardClass)
  {
    this.euFirstHazardClass = euFirstHazardClass;
  }

  public String getEuFirstHazardClass()
  {
    return euFirstHazardClass;
  }

  public void setEuSecondHazardClass(String euSecondHazardClass)
  {
    this.euSecondHazardClass = euSecondHazardClass;
  }

  public String getEuSecondHazardClass()
  {
    return euSecondHazardClass;
  }
}
