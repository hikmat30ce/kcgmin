package com.valspar.interfaces.common.beans;

import java.io.File;

public class InterfaceInfoBean
{
  private File logFile;
  private String processId = "-1";

  public InterfaceInfoBean(File logFile)
  {
    this.logFile = logFile;
  }

  public void setProcessId(String processId)
  {
    this.processId = processId;
  }

  public String getProcessId()
  {
    return processId;
  }

  public void setLogFile(File logFile)
  {
    this.logFile = logFile;
  }

  public File getLogFile()
  {
    return logFile;
  }
}
