package com.valspar.interfaces.financials.openscan.beans;

import com.valspar.interfaces.common.servlets.*;
import java.io.*;
import org.apache.commons.lang3.*;

public class HSBCImageFileBean
{
  private String fileName;
  
  public HSBCImageFileBean(String fileName)
  {
    this.setFileName(fileName);
  }

  public String getHsbcLocalRootDirectory()
  {
    if (StringUtils.startsWith(this.getFileName(), "CA.W10278"))
    {
      return PropertiesServlet.getProperty("openscan.hsbcCanadaLocalRootDirectory") + StringUtils.substring(this.getFileName(), 0, 10) + StringUtils.substring(this.getFileName(), 15, 21);
    }
    else if (StringUtils.startsWith(this.getFileName(), "CA.W10279"))
    {
      return PropertiesServlet.getProperty("openscan.hsbcUSLocalRootDirectory") + StringUtils.substring(this.getFileName(), 0, 10) + StringUtils.substring(this.getFileName(), 15, 21);
    }
    return null;
  }

  public String getHsbcFTPRootDirectory()
  {
    if (StringUtils.startsWith(this.getFileName(), "CA.W10278"))
    {
      return PropertiesServlet.getProperty("openscan.hsbcCanadaFTPRootDirectory");
    }
    else if (StringUtils.startsWith(this.getFileName(), "CA.W10279"))
    {
      return PropertiesServlet.getProperty("openscan.hsbcUSFTPRootDirectory");
    }
    return null;
  }
  
  public String getHsbcFullFTPRootDirectory()
  {
    return this.getHsbcFTPRootDirectory() + "/" + StringUtils.substringAfterLast(this.getHsbcLocalRootDirectory(), File.separator);
  }

  public void setFileName(String fileName)
  {
    this.fileName = fileName;
  }

  public String getFileName()
  {
    return fileName;
  }
}
