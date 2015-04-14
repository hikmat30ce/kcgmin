package com.valspar.interfaces.regulatory.rollup.beans;

public class MsdsBean
{
  private String subFormat;
  private String language;
  
  public MsdsBean()
  {
  }

  public void setSubFormat(String subFormat)
  {
    this.subFormat = subFormat;
  }

  public String getSubFormat()
  {
    return subFormat;
  }

  public void setLanguage(String language)
  {
    this.language = language;
  }

  public String getLanguage()
  {
    return language;
  }
}
