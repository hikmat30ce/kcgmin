package com.valspar.interfaces.guardsman.pos.beans;

public class AdminMsgBean
{
  String programLocation;
  String message;
  String Item;

  public AdminMsgBean()
  {
  }

  public String getProgramLocation()
  {
    return programLocation;
  }

  public void setProgramLocation(String programLocation)
  {
    this.programLocation = programLocation;
  }

  public String getMessage()
  {
    return message;
  }

  public void setMessage(String message)
  {
    this.message = message;
  }

  public String getItem()
  {
    return Item;
  }

  public void setItem(String Item)
  {
    this.Item = Item;
  }
}
