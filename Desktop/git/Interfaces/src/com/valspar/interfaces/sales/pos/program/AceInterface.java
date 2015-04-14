package com.valspar.interfaces.sales.pos.program;

import com.valspar.interfaces.common.BaseInterface;
import com.valspar.interfaces.common.servlets.PropertiesServlet;
import com.valspar.interfaces.common.utils.FtpUtility;
import org.apache.commons.lang3.StringUtils;
import org.apache.log4j.Logger;

public class AceInterface extends BaseInterface
{
  private static Logger log4jLogger = Logger.getLogger(AceInterface.class);
  private static final String posFTPServer = PropertiesServlet.getProperty("pos.posFTPServer");
  private static final String localRootDirectory = PropertiesServlet.getProperty("pos.posAcelocalRootDirectory");

  public AceInterface()
  {
  }

  public void execute()
  {
    String interfaceName = getParameterValue("interfaceName");

    log4jLogger.info("POS AceInterface started for ACE " + interfaceName);
    if (StringUtils.equalsIgnoreCase(interfaceName, "DAILY"))
    {
      dailyFiles();
    }
    else if (StringUtils.equalsIgnoreCase(interfaceName, "STORE"))
    {
      storeFiles();
    }
    else
    {
      log4jLogger.error("POS AceInterface.startInterface(): Interface Name does not exist: " + interfaceName);
    }
    log4jLogger.info("POS AceInterface finished for ACE " + interfaceName + "!");
  }

  private void dailyFiles()
  {
    String posFTPRemoteFileLocation = PropertiesServlet.getProperty("pos.posFTPRemoteFileLocation");
    String ftpUser = PropertiesServlet.getProperty("pos.posAceDailyFTPUser");
    String ftpPassword = PropertiesServlet.getProperty("pos.posAceDailyFTPPassword");
    FtpUtility.retrieveFileWithTimeStamp(posFTPServer, ftpUser, ftpPassword, posFTPRemoteFileLocation, localRootDirectory);
  }

  private void storeFiles()
  {
    String posAceStoreFTPRemoteFileLocation = PropertiesServlet.getProperty("pos.posAceStoreFTPRemoteFileLocation");
    String ftpUser = PropertiesServlet.getProperty("pos.posAceStoreFTPUser");
    String ftpPassword = PropertiesServlet.getProperty("pos.posAceStoreFTPPassword");
    FtpUtility.retrieveFileWithTimeStamp(posFTPServer, ftpUser, ftpPassword, posAceStoreFTPRemoteFileLocation, localRootDirectory);
  }
}

