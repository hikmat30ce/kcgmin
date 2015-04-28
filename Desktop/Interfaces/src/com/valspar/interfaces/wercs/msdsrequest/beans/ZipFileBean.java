package com.valspar.interfaces.wercs.msdsrequest.beans;

import java.io.File;

public class ZipFileBean
{
  private File fileToZip;
  private String zipFileName;
  
  public ZipFileBean()
  {
    super();
  }

  public void setFileToZip(File fileToZip)
  {
    this.fileToZip = fileToZip;
  }

  public File getFileToZip()
  {
    return fileToZip;
  }

  public void setZipFileName(String zipFileName)
  {
    this.zipFileName = zipFileName;
  }

  public String getZipFileName()
  {
    return zipFileName;
  }
}
