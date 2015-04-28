package com.valspar.interfaces.regulatory.optivatowercs.exceptions;

import com.valspar.interfaces.regulatory.exceptions.RegulatoryException;

public class PopulateWercsException extends RegulatoryException
{
  String exceptionMessage;

  public PopulateWercsException()
  {
  }

  public PopulateWercsException(String inException)
  {
    this.setExceptionMessage(inException);
  }

  public String getExceptionMessage()
  {
    return exceptionMessage;
  }

  public void setExceptionMessage(String newExceptionMessage)
  {
    exceptionMessage = newExceptionMessage;
  }
}
