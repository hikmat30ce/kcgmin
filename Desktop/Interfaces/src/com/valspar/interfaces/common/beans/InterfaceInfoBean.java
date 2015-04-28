package com.valspar.interfaces.common.beans;

public class InterfaceInfoBean
{
  private String logFileLocation;
  private String processId = "-1";

  public InterfaceInfoBean(String logFileLocation)
  {
    this.logFileLocation = logFileLocation;
  }

  public void setProcessId(String processId)
  {
    this.processId = processId;
  }

  public String getProcessId()
  {
    return processId;
  }

  public void setLogFileLocation(String logFileLocation)
  {
    this.logFileLocation = logFileLocation;
  }

  public String getLogFileLocation()
  {
    return logFileLocation;
  }
}
