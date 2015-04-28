package com.valspar.interfaces.guardsman.common.beans;

public class PlanetPressResultBean
{
  private boolean success;
  private String filePath;
  private String errorMessage;
  private long msec;

  public void setSuccess(boolean success)
  {
    this.success = success;
  }

  public boolean isSuccess()
  {
    return success;
  }

  public void setFilePath(String filePath)
  {
    this.filePath = filePath;
  }

  public String getFilePath()
  {
    return filePath;
  }

  public void setErrorMessage(String errorMessage)
  {
    this.errorMessage = errorMessage;
  }

  public String getErrorMessage()
  {
    return errorMessage;
  }

  public void setMsec(long msec)
  {
    this.msec = msec;
  }

  public long getMsec()
  {
    return msec;
  }
}
