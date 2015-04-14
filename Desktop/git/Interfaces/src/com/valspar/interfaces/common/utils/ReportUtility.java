package com.valspar.interfaces.common.utils;

import com.valspar.interfaces.common.servlets.PropertiesServlet;
import java.util.*;
import org.apache.commons.lang.StringUtils;

public abstract class ReportUtility
{
  private String outputDirectory;
  private String directoryName;
  private String customFilename;
  private String fileExtension;
  private String finalFileName;

  private String buildReportFilename()
  {
    if (StringUtils.isNotEmpty(customFilename))
    {
      return customFilename;
    }
    else
    {
      return "Report";
    }
  }

  protected void buildFileName(String extension)
  {
    StringBuilder sb = new StringBuilder();

    sb.append(buildReportFilename());

    if (StringUtils.isEmpty(customFilename))
    {
      String rand = String.valueOf(new Random().nextInt(100000));
      sb.append(rand);
    }

    this.finalFileName = sb.toString();
    this.fileExtension = extension;
  }

  public static String getFileWritePath(String filename)
  {
    StringBuilder sb = new StringBuilder();
    sb.append(PropertiesServlet.getProperty("reportDir"));
    //sb.append("c:\\data\\interfaces\\reports\\");
    sb.append(filename);
    return sb.toString();
  }

  public String getFileWritePath()
  {
    return getFileWritePath(this.finalFileName + this.fileExtension);
  }

  public static String buildFileViewPath(String filename)
  {
    StringBuilder sb = new StringBuilder();
    sb.append(PropertiesServlet.getProperty("webserver"));
    sb.append("/interfaces");
    sb.append(buildRelativeFileViewPath(filename));

    return sb.toString();
  }

  public static String buildRelativeFileViewPath(String filename)
  {
    StringBuilder sb = new StringBuilder();
    sb.append("/reports/");
    sb.append(filename);
   return sb.toString();
  }

  public String getFileViewPath()
  {
    return buildFileViewPath(this.finalFileName + this.fileExtension);
  }

  public String getRelativeFileViewPath()
  {
    return buildRelativeFileViewPath(this.finalFileName + this.fileExtension);
  }

  public abstract boolean isReportEmpty();

 
  public void setOutputDirectory(String outputDirectory)
  {
    this.outputDirectory = outputDirectory;
  }

  public String getOutputDirectory()
  {
    return outputDirectory;
  }

  public void setDirectoryName(String directoryName)
  {
    this.directoryName = directoryName;
  }

  public String getDirectoryName()
  {
    return directoryName;
  }

  public void setCustomFilename(String customFilename)
  {
    this.customFilename = customFilename;
  }

  public String getCustomFilename()
  {
    return customFilename;
  }

  public void setFileExtension(String fileExtension)
  {
    this.fileExtension = fileExtension;
  }

  public String getFileExtension()
  {
    return fileExtension;
  }

}
