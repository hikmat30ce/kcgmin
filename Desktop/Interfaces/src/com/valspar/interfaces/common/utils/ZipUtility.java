package com.valspar.interfaces.common.utils;

import com.valspar.interfaces.regulatory.msdsrequest.beans.RequestBean;
import java.io.*;
import java.util.zip.*;
import org.apache.commons.lang3.StringUtils;
import org.apache.log4j.Logger;

public final class ZipUtility
{
  private static Logger log4jLogger = Logger.getLogger(ZipUtility.class);

  private ZipUtility()
  {
  }

  public static void zipFiles(RequestBean requestBean)
  {
    byte[] buf = new byte[1024];
    try
    {
      ZipOutputStream out = new ZipOutputStream(new FileOutputStream(CommonUtility.getDataDirectoryPath() + requestBean.getRequestType() + requestBean.getRequestId() + ".zip"));

      for (int i = 0; i < requestBean.getFileList().size(); i++)
      {
        FileInputStream in = new FileInputStream(requestBean.getFileList().get(i).getFileToZip());
        out.putNextEntry(new ZipEntry(requestBean.getFileList().get(i).getZipFileName()));
        int len;
        while ((len = in.read(buf)) > 0)
        {
          out.write(buf, 0, len);
        }
        out.flush();

        out.closeEntry();
        in.close();
        requestBean.getFileList().get(i).getFileToZip().delete();
      }
      out.close();
    }
    catch (Exception e)
    {
      log4jLogger.error(e);
    }
  }
  
  public static void zipFiles(com.valspar.interfaces.wercs.msdsrequest.beans.RequestBean requestBean)
  {
    byte[] buf = new byte[1024];
    try
    {
      ZipOutputStream out = new ZipOutputStream(new FileOutputStream(CommonUtility.getDataDirectoryPath() + requestBean.getRequestType() + requestBean.getRequestId() + ".zip"));

      for (int i = 0; i < requestBean.getFileList().size(); i++)
      {
        FileInputStream in = new FileInputStream(requestBean.getFileList().get(i).getFileToZip());
        out.putNextEntry(new ZipEntry(requestBean.getFileList().get(i).getZipFileName()));
        int len;
        while ((len = in.read(buf)) > 0)
        {
          out.write(buf, 0, len);
        }
        out.flush();

        out.closeEntry();
        in.close();
        requestBean.getFileList().get(i).getFileToZip().delete();
      }
      out.close();
    }
    catch (Exception e)
    {
      log4jLogger.error(e);
    }
  }
  
  public static void unZipFile(String zipFile, String outputFolder)
  {
    byte[] buffer = new byte[1024];

    try
    {
      File folder = new File(outputFolder);
      if (!folder.exists())
      {
        folder.mkdir();
      }

      ZipInputStream zipInputStream = new ZipInputStream(new FileInputStream(zipFile));
      
      ZipEntry zipEntry = zipInputStream.getNextEntry();
      while (zipEntry != null)
      {
        String fileName = zipEntry.getName();
        File newFile = new File(outputFolder + File.separator + fileName);

        if (StringUtils.endsWithIgnoreCase(fileName, "/"))
        {
          newFile.mkdir();
        }
        else
        {
          FileOutputStream fos = new FileOutputStream(newFile);

          int length;
          while ((length = zipInputStream.read(buffer)) > 0)
          {
            fos.write(buffer, 0, length);
          }
          fos.close();
        }
        zipEntry = zipInputStream.getNextEntry();
      }
      zipInputStream.closeEntry();
      zipInputStream.close();
    }
    catch (Exception e)
    {
      log4jLogger.error(e);
    }
  }
}
