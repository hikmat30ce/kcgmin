package com.valspar.interfaces.common.utils;

import java.io.*;
import java.text.*;
import org.apache.commons.lang3.StringUtils;
import java.util.*;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.net.ftp.*;
import org.apache.log4j.Logger;

public final class FtpUtility
{
  private static Logger log4jLogger = Logger.getLogger(FtpUtility.class);

  private FtpUtility()
  {
  }

  public static ArrayList<String> stringToParamListShellCmd(String cmdString)
  {
    ArrayList<String> cmdArrayList = new ArrayList<String>(Arrays.asList(StringUtils.split(cmdString, ',')));
    return cmdArrayList;
  }

  public static void sendFileOrDirectory(String server, String ftpUserName, String ftpPassword, String localPath, String ftpRemoteDestination, String fileExtension)
  {
    try
    {
      FTPClient ftpClient = new FTPClient();
      ftpClient.connect(server, 21);
      if (!FTPReply.isPositiveCompletion(ftpClient.getReplyCode()))
      {
        log4jLogger.error("Not able to connect to the FTP server FTP Reply Code: " + ftpClient.getReplyCode());
      }
      else
      {
        ftpClient.setFileType(FTP.BINARY_FILE_TYPE);
        ftpClient.enterLocalPassiveMode();
        if (ftpClient.login(ftpUserName, ftpPassword))
        {
          upload(new File(localPath), ftpRemoteDestination, ftpClient, fileExtension);
        }
        ftpClient.logout();
        ftpClient.disconnect();
      }
    }
    catch (Exception e)
    {
      log4jLogger.error(e);
    }
  }

  public static void rename(String server, String ftpUserName, String ftpPassword, String ftpRemoteDestinationFrom, String ftpRemoteDestinationTo)
  {
    try
    {
      FTPClient ftpClient = new FTPClient();
      ftpClient.connect(server, 21);
      if (!FTPReply.isPositiveCompletion(ftpClient.getReplyCode()))
      {
        log4jLogger.error("Not able to connect to the FTP server FTP Reply Code: " + ftpClient.getReplyCode());
      }
      else
      {
        ftpClient.setFileType(FTP.BINARY_FILE_TYPE);
        ftpClient.enterLocalPassiveMode();
        if (ftpClient.login(ftpUserName, ftpPassword))
        {
          ftpClient.rename(ftpRemoteDestinationFrom, ftpRemoteDestinationTo);
        }
        ftpClient.logout();
        ftpClient.disconnect();
      }
    }
    catch (Exception e)
    {
      log4jLogger.error(e);
    }
  }

  private static void upload(File localFile, String ftpRemoteDestination, FTPClient ftpClient, String fileExtension) throws IOException
  {
    if (localFile.isDirectory())
    {
      String directoryName = ftpRemoteDestination + "/" + localFile.getName();
      log4jLogger.info("FtpUtility.upload() - directoryName = " + directoryName);
      ftpClient.makeDirectory(directoryName);
      ftpClient.changeWorkingDirectory(directoryName);
      for (File file: localFile.listFiles())
      {
        upload(file, directoryName, ftpClient, fileExtension);
      }
      ftpClient.changeToParentDirectory();
    }
    else
    {
      if (StringUtils.isEmpty(fileExtension) || StringUtils.endsWithIgnoreCase(localFile.getName(), fileExtension))
      {
        InputStream is = null;
        try
        {
          is = new FileInputStream(localFile);
          StringBuilder sb = new StringBuilder();
          sb.append(ftpRemoteDestination);
          sb.append("/");
          sb.append(localFile.getName());
          log4jLogger.info("FtpUtility.upload() - File Name (sb.toString) = " + sb.toString());
          ftpClient.storeFile(sb.toString(), is);
          ftpClient.sendSiteCommand("chmod 777 " + sb.toString());
        }
        catch (Exception e)
        {
          log4jLogger.error(e);
        }
        finally
        {
          IOUtils.closeQuietly(is);
        }
      }
    }
  }

  public static boolean retrieveFile(String server, String ftpUserName, String ftpPassword, String ftpRemoteFileLocation, String localFileDestination, boolean deleteAfterFTP)
  {
    File f = null;
    try
    {
      FTPClient ftp = new FTPClient();
      ftp.connect(server, 21);
      if (!FTPReply.isPositiveCompletion(ftp.getReplyCode()))
      {
        log4jLogger.error("Not able to connect to the FTP server FTP Reply Code: " + ftp.getReplyCode());
      }
      else
      {
        ftp.setFileType(FTP.BINARY_FILE_TYPE);
        ftp.enterLocalPassiveMode();
        if (ftp.login(ftpUserName, ftpPassword))
        {
          InputStream is = ftp.retrieveFileStream(ftpRemoteFileLocation);
          if (is != null)
          {
            f = new File(localFileDestination);
            FileUtils.copyInputStreamToFile(is, new File(localFileDestination));
            is.close();
          }
          if (deleteAfterFTP)
          {
            ftp.deleteFile(ftpRemoteFileLocation);
          }
        }
        ftp.logout();
        ftp.disconnect();
      }
    }
    catch (Exception e)
    {
      log4jLogger.error(e);
    }
    if (f == null || !f.exists())
    {
      return false;
    }
    else
    {
      return true;
    }
  }

  public static byte[] retrieveFile(String server, String ftpUserName, String ftpPassword, String ftpRemoteFileLocation, boolean deleteAfterFTP)
  {
    byte[] bytes = null;

    try
    {
      FTPClient ftp = new FTPClient();
      ftp.connect(server, 21);
      if (!FTPReply.isPositiveCompletion(ftp.getReplyCode()))
      {
        log4jLogger.error("Not able to connect to the FTP server FTP Reply Code: " + ftp.getReplyCode());
      }
      else
      {
        ftp.setFileType(FTP.BINARY_FILE_TYPE);
        ftp.enterLocalPassiveMode();
        if (ftp.login(ftpUserName, ftpPassword))
        {
          InputStream is = ftp.retrieveFileStream(ftpRemoteFileLocation);
          if (is != null)
          {
            bytes = IOUtils.toByteArray(is);
            is.close();
          }
          if (deleteAfterFTP)
          {
            ftp.deleteFile(ftpRemoteFileLocation);
          }
        }
        ftp.logout();
        ftp.disconnect();
      }
    }
    catch (Exception e)
    {
      log4jLogger.error(e);
    }
    return bytes;
  }

  public static FTPFile[] listFiles(String server, String ftpUserName, String ftpPassword, String ftpRemoteFileLocation, FTPFileFilter filter)
  {
    FTPFile[] fileArray = null;
    try
    {
      FTPClient ftp = new FTPClient();
      ftp.connect(server, 21);
      if (!FTPReply.isPositiveCompletion(ftp.getReplyCode()))
      {
        log4jLogger.error("Not able to connect to the FTP server FTP Reply Code: " + ftp.getReplyCode());
      }
      else
      {
        ftp.setFileType(FTP.BINARY_FILE_TYPE);
        ftp.enterLocalPassiveMode();
        if (ftp.login(ftpUserName, ftpPassword))
        {
          fileArray = ftp.listFiles(ftpRemoteFileLocation);
        }
        ftp.logout();
        ftp.disconnect();
      }
    }
    catch (Exception e)
    {
      log4jLogger.error(e);
    }
    return fileArray;
  }

  public static void retrieveFileWithTimeStamp(String ftpServer, String ftpUserName, String ftpPassword, String ftpRemoteFileLocation, String localRootDirectory)
  {
    FTPFile[] fileArray = FtpUtility.listFiles(ftpServer, ftpUserName, ftpPassword, ftpRemoteFileLocation, null);
    for (int i = 0; i < fileArray.length; i++)
    {
      FTPFile file = fileArray[i];
      String fileName = file.getName();
      DateFormat df = new SimpleDateFormat("yyyyMMddHHmmss");
      String runDate = df.format(Calendar.getInstance().getTime());
      int len = fileName.lastIndexOf('.');
      fileName = fileName.substring(0, len) + "-" + runDate + fileName.substring(len);
      retrieveFile(ftpServer, ftpUserName, ftpPassword, ftpRemoteFileLocation + file.getName(), localRootDirectory + fileName, true);
    }
  }
}
