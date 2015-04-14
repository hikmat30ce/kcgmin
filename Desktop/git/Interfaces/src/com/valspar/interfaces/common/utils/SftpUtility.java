package com.valspar.interfaces.common.utils;

import java.io.File;
import org.apache.commons.vfs2.FileObject;
import org.apache.commons.vfs2.FileSystemOptions;
import org.apache.commons.vfs2.Selectors;
import org.apache.commons.vfs2.impl.StandardFileSystemManager;
import org.apache.commons.vfs2.provider.sftp.SftpFileSystemConfigBuilder;
import org.apache.log4j.Logger;

public class SftpUtility
{
  private static Logger log4jLogger = Logger.getLogger(SftpUtility.class);

  public static void main(String[] args)
  {
    SftpUtility sftpUtility = new SftpUtility();
    sftpUtility.sftpFile(null, null, null, null, null, null,false);
    sftpUtility.sftpFilesInDirectory(null, null, null, null, null,false);
  }

  public void sftpFile(String server, String ftpUserName, String ftpPassword, String localPath, String fileToFTP, String ftpRemoteDestination, boolean deleteAfterFTP)
  {
    File file = new File(localPath + fileToFTP);
    if (file.exists())
    {
      StandardFileSystemManager manager = initStandardFileSystemManager();
      FileSystemOptions opts = initFileSystemOptions();
      try
      {
        String sftpUri = "sftp://" + ftpUserName + ":" + ftpPassword + "@" + server + "/" + ftpRemoteDestination + "/" + fileToFTP;
        FileObject localFileObject = manager.resolveFile(file.getAbsolutePath());
        FileObject remoteFileObject = manager.resolveFile(sftpUri, opts);
        remoteFileObject.copyFrom(localFileObject, Selectors.SELECT_SELF);
        log4jLogger.info("File transfered from " + file.getAbsolutePath() + " to " + server + " to this path " + ftpRemoteDestination);
        if(deleteAfterFTP)
        {
          file.delete();
        }
      }
      catch (Exception e)
      {
        log4jLogger.error(e);
      }
      finally
      {
        manager.close();
      }
    }
    else
    {
      log4jLogger.error("localPath + fileToFTP =" + localPath + fileToFTP + " " + " is not a file");
    }
  }

  public void sftpFilesInDirectory(String server, String ftpUserName, String ftpPassword, String localPath, String ftpRemoteDestination, boolean deleteAfterFTP)
  {
    File file = new File(localPath);
    if (file.isDirectory())
    {
      StandardFileSystemManager manager = initStandardFileSystemManager();
      FileSystemOptions opts = initFileSystemOptions();
      try
      {
        for (File f: file.listFiles())
        {
          String sftpUri = "sftp://" + ftpUserName + ":" + ftpPassword + "@" + server + "/" + ftpRemoteDestination + "/" + f.getName();
          FileObject localFileObject = manager.resolveFile(f.getAbsolutePath());
          FileObject remoteFileObject = manager.resolveFile(sftpUri, opts);
          remoteFileObject.copyFrom(localFileObject, Selectors.SELECT_SELF);
          log4jLogger.info("File transfered from " + f.getAbsolutePath() + " to " + server + " to this path " + ftpRemoteDestination + "/" + f.getName());
          if(deleteAfterFTP)
          {
            f.delete();
          }
        }
      }
      catch (Exception e)
      {
        log4jLogger.error(e);
      }
      finally
      {
        manager.close();
      }
    }
    else
    {
      log4jLogger.error("localPath =" + localPath + " " + " is not a directory");
    }
  }

  private StandardFileSystemManager initStandardFileSystemManager()
  {
    StandardFileSystemManager manager = new StandardFileSystemManager();
    try
    {
      manager.init();
    }
    catch (Exception e)
    {
      log4jLogger.error(e);
    }
    return manager;
  }

  private FileSystemOptions initFileSystemOptions()
  {
    FileSystemOptions opts = new FileSystemOptions();
    try
    {
      SftpFileSystemConfigBuilder.getInstance().setStrictHostKeyChecking(opts, "no");
      SftpFileSystemConfigBuilder.getInstance().setUserDirIsRoot(opts, true);
      SftpFileSystemConfigBuilder.getInstance().setTimeout(opts, 10000);
    }
    catch (Exception e)
    {
      log4jLogger.error(e);
    }
    return opts;
  }

}