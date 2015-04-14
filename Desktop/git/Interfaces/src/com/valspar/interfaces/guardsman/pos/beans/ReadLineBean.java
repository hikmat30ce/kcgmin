package com.valspar.interfaces.guardsman.pos.beans;

public class ReadLineBean implements Cloneable
{
  String record = new String();
  boolean pipeError;
  String[] parsedLine = new String[37];
  boolean detailTransIdMissMatchError;
  String parsedLastName = new String();
  String parsedFirstName = new String();
  boolean inSaleFile;

  public ReadLineBean()
  {
  }

  public String getRecord()
  {
    return record;
  }

  public void setRecord(String record)
  {
    this.record = record;
  }

  public boolean isPipeError()
  {
    return pipeError;
  }

  public void setPipeError(boolean pipeError)
  {
    this.pipeError = pipeError;
  }

  public String[] getParsedLine()
  {
    return parsedLine;
  }

  public void setParsedLine(String[] parsedLine)
  {
    this.parsedLine = parsedLine;
  }

  public boolean isDetailTransIdMissMatchError()
  {
    return detailTransIdMissMatchError;
  }

  public void setDetailTransIdMissMatchError(boolean detailTransIdMissMatchError)
  {
    this.detailTransIdMissMatchError = detailTransIdMissMatchError;
  }
  
  public void setParsedName(String name)
  {
    int commaLoc;
    if (name != null && !name.equalsIgnoreCase(""))
    {
      commaLoc = name.indexOf(',');
      if (commaLoc >=0)
      {
        this.parsedLastName = name.substring(0,commaLoc);
        this.parsedFirstName = name.substring(commaLoc+1);
      } 
    }
  }

  public String getParsedLastName()
  {
    return parsedLastName;
  }

  public String getParsedFirstName()
  {
    return parsedFirstName;
  }
  
  public String scrubPhone(String phone)
  {
    String scrubbedPhone = phone;
    if (scrubbedPhone != null && !scrubbedPhone.equalsIgnoreCase(""))
    {
      scrubbedPhone = scrubbedPhone.replaceAll("[^0-9]","");
    }
    
    if (scrubbedPhone != null && !scrubbedPhone.equalsIgnoreCase("") && scrubbedPhone.length() > 10)
    {
      scrubbedPhone = scrubbedPhone.substring(0,10);
    }
    
    if (scrubbedPhone != null && scrubbedPhone.equalsIgnoreCase(""))
    {
      scrubbedPhone = null;
    }
    return scrubbedPhone;
  }
  
  public Object clone()
  {
    try
    {
      return super.clone();
    }
    catch (CloneNotSupportedException e)
    {
      System.out.println("Cloning error: " + e);
    }
    return null;
  }

  public void setInSaleFile(boolean inSaleFile)
  {
    this.inSaleFile = inSaleFile;
  }

  public boolean isInSaleFile()
  {
    return inSaleFile;
  }
}
