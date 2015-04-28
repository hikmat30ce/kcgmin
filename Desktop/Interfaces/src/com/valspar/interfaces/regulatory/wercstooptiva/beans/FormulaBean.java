package com.valspar.interfaces.regulatory.wercstooptiva.beans;

import java.util.*;

public class FormulaBean
{
  private String product;
  private String aliasName;
  private java.util.ArrayList ingredients = new ArrayList();
  private boolean oneHundredpercentOfItself;
  private String id;
  private boolean singleIngredientFormula;
  private String costClass;
  private boolean componentBased;
  
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
  public void setIngredients(ArrayList ingredients)
  {
    this.ingredients = ingredients;
  }
  public ArrayList getIngredients()
  {
    return ingredients;
  }
  public void setOneHundredpercentOfItself(boolean oneHundredpercentOfItself)
  {
    this.oneHundredpercentOfItself = oneHundredpercentOfItself;
  }
  public boolean isOneHundredpercentOfItself()
  {
    return oneHundredpercentOfItself;
  }

  public String getId()
  {
    return id;
  }

  public void setId(String id)
  {
    this.id = id;
  }

  public void setSingleIngredientFormula(boolean singleIngredientFormula)
  {
    this.singleIngredientFormula = singleIngredientFormula;
  }

  public boolean isSingleIngredientFormula()
  {
    return singleIngredientFormula;
  }

  public void setCostClass(String costClass)
  {
    this.costClass = costClass;
  }

  public String getCostClass()
  {
    return costClass;
  }

  public void setComponentBased(boolean componentBased)
  {
    this.componentBased = componentBased;
  }

  public boolean isComponentBased()
  {
    return componentBased;
  }
}
