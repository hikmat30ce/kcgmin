package com.valspar.interfaces.regulatory.optivatodromont.beans;

public class RowBean 
{
  private String id;
  private String cfMapperFormula;
  private String description;
  private String version;
  private String commenttxt;
  private String itemCode;
  private String materialPct;
  private String location;

  public RowBean()
  {
  }

  public String getId()
  {
    return id;
  }

  public void setId(String id)
  {
    this.id = id;
  }

  public String getCfMapperFormula()
  {
    return cfMapperFormula;
  }

  public void setCfMapperFormula(String cfMapperFormula)
  {
    this.cfMapperFormula = cfMapperFormula;
  }

  public String getDescription()
  {
    return description;
  }

  public void setDescription(String description)
  {
    this.description = description;
  }

  public String getVersion()
  {
    return version;
  }

  public void setVersion(String version)
  {
    this.version = version;
  }

  public String getCommenttxt()
  {
    return commenttxt;
  }

  public void setCommenttxt(String commenttxt)
  {
    this.commenttxt = commenttxt;
  }

  public String getItemCode()
  {
    return itemCode;
  }

  public void setItemCode(String itemCode)
  {
    this.itemCode = itemCode;
  }

  public String getMaterialPct()
  {
    return materialPct;
  }

  public void setMaterialPct(String materialPct)
  {
    this.materialPct = materialPct;
  }

  public String getLocation()
  {
    return location;
  }

  public void setLocation(String location)
  {
    this.location = location;
  }
}