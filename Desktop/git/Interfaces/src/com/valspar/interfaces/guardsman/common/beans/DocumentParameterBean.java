package com.valspar.interfaces.guardsman.common.beans;

public class DocumentParameterBean
{
  private String name;
  private String value;

  public DocumentParameterBean(String name, String value)
  {
    setName(name);
    setValue(value);
  }

  public void setName(String name)
  {
    this.name = name;
  }

  public String getName()
  {
    return name;
  }

  public void setValue(String value)
  {
    this.value = value;
  }

  public String getValue()
  {
    return value;
  }
}
