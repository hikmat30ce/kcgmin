package com.valspar.interfaces.common.exceptions;

public class SpreadsheetReadingFatalException extends Exception
{
  public SpreadsheetReadingFatalException(Throwable throwable)
  {
    super(throwable);
  }

  public SpreadsheetReadingFatalException(String string, Throwable throwable)
  {
    super(string, throwable);
  }

  public SpreadsheetReadingFatalException(String string)
  {
    super(string);
  }

  public SpreadsheetReadingFatalException()
  {
    super();
  }
}