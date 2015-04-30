package com.valspar.interfaces.financials.openscan.beans;

import com.valspar.interfaces.common.servlets.*;
import java.io.*;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import org.apache.commons.lang3.*;

public class HSBCImageFileBean
{
  private String fileName;
  private Date startDate;
  private static final DateFormat df = new SimpleDateFormat("HHmmss");

  public HSBCImageFileBean(String fileName, Date startDate)
  {
    this.setFileName(fileName);
    this.setStartDate(startDate);  
  }

  public String getHsbcLocalRootDirectory()
  {
    String hourminsec = df.format(startDate);
    if (StringUtils.startsWith(this.getFileName(), "CA.W10278"))
    {
      return PropertiesServlet.getProperty("openscan.hsbcCanadaLocalRootDirectory") + StringUtils.substring(this.getFileName(), 0, 10) + StringUtils.substring(this.getFileName(), 15, 21) + hourminsec;
    }
    else if (StringUtils.startsWith(this.getFileName(), "CA.W10279"))
    {
      return PropertiesServlet.getProperty("openscan.hsbcUSLocalRootDirectory") + StringUtils.substring(this.getFileName(), 0, 10) + StringUtils.substring(this.getFileName(), 15, 21) + hourminsec;
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

  public void setStartDate(Date startDate)
  {
    this.startDate = startDate;
  }

  public Date getStartDate()
  {
    return startDate;
  }
}
