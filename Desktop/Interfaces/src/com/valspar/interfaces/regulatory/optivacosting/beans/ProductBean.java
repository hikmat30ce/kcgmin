package com.valspar.interfaces.regulatory.optivacosting.beans;

public class ProductBean
{
  private String product;
  private String cost;
  private String itemUm;
  private String optivaParam;
  private String viewName;
  private String type;
  private String formulaId;

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

  public String getCost()
  {
    return cost;
  }

  public void setCost(String cost)
  {
    this.cost = cost;
  }

  public String getOptivaParam()
  {
    return optivaParam;
  }

  public void setOptivaParam(String optivaParam)
  {
    this.optivaParam = optivaParam;
  }

  public void setViewName(String viewName)
  {
    this.viewName = viewName;
  }

  public String getViewName()
  {
    return viewName;
  }

  public void setItemUm(String itemUm)
  {
    this.itemUm = itemUm;
  }

  public String getItemUm()
  {
    return itemUm;
  }

  public void setType(String type)
  {
    this.type = type;
  }

  public String getType()
  {
    return type;
  }

  public void setFormulaID(String formulaId)
  {
    this.formulaId = formulaId;
  }

  public String getFormulaId()
  {
    return formulaId;
  }
}
