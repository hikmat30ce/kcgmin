package com.valspar.interfaces.guardsman.common.beans;

public class FileBean
{
  private String physicalPath;
  private String webPath;
  private String filename;

  public void setFilename(String filename)
  {
    this.filename = filename;
  }

  public String getFilename()
  {
    return filename;
  }

  public void setPhysicalPath(String physicalPath)
  {
    this.physicalPath = physicalPath;
  }

  public String getPhysicalPath()
  {
    return physicalPath;
  }

  public void setWebPath(String webPath)
  {
    this.webPath = webPath;
  }

  public String getWebPath()
  {
    return webPath;
  }
}
