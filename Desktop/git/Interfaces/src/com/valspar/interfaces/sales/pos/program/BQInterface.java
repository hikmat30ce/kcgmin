package com.valspar.interfaces.sales.pos.program;

import com.valspar.interfaces.common.BaseInterface;
import com.valspar.interfaces.common.servlets.PropertiesServlet;
import com.valspar.interfaces.common.utils.FtpUtility;
import org.apache.log4j.Logger;

public class BQInterface extends BaseInterface
{
  private static Logger log4jLogger = Logger.getLogger(BQInterface.class);
  private static final String posFTPServer = PropertiesServlet.getProperty("pos.posFTPServer");
  private static final String posFTPRemoteFileLocation = PropertiesServlet.getProperty("pos.posFTPRemoteFileLocation");
  private static final String localRootDirectory = PropertiesServlet.getProperty("pos.posBqlocalRootDirectory");
  private static final String ftpUser = PropertiesServlet.getProperty("pos.posBqFTPUser");
  private static final String ftpPassword = PropertiesServlet.getProperty("pos.posBqFTPPassword");
  
  public BQInterface()
  {
  }
  
  public void execute()
  {
    log4jLogger.info("POS BQInterface Started");
    FtpUtility.retrieveFileWithTimeStamp(posFTPServer, ftpUser, ftpPassword, posFTPRemoteFileLocation, localRootDirectory);
    log4jLogger.info("POS BQInterface finished!");
  }
}
