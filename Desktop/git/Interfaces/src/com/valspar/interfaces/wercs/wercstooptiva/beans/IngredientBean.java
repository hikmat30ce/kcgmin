package com.valspar.interfaces.wercs.wercstooptiva.beans;

public class IngredientBean
{
  private String lineId;
  private String product;
  private String percent;
  public String getLineId()
  {
    return lineId;
  }
  public void setLineId(String lineId)
  {
    this.lineId = lineId;
  }
  public void setProduct(String product)
  {
    this.product = product;
  }
  public String getProduct()
  {
    return product;
  }
  public void setPercent(String percent)
  {
    this.percent = percent;
  }
  public String getPercent()
  {
    return percent;
  }
}