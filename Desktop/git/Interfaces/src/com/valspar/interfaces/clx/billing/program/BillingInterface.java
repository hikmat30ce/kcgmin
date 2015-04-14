package com.valspar.interfaces.clx.billing.program;

import com.valspar.interfaces.clx.billing.beans.BillingBean;
import com.valspar.interfaces.clx.billing.dao.BillingDAO;
import com.valspar.interfaces.common.BaseInterface;
import com.valspar.interfaces.common.beans.ConnectionAccessBean;
import com.valspar.interfaces.common.enums.DataSource;
import com.valspar.interfaces.common.servlets.PropertiesServlet;
import com.valspar.interfaces.common.utils.*;
import java.util.*;
import org.apache.commons.net.ftp.FTPFile;
import org.apache.log4j.Logger;

public class BillingInterface extends BaseInterface
{
  private static Logger log4jLogger = Logger.getLogger(BillingInterface.class);
  private static final String clxFTPServer = PropertiesServlet.getProperty("clx.FTPServer");
  private static final String FileLocation = PropertiesServlet.getProperty("clx.fileOutgoingLocation");
  private static final String localRootDirectory = PropertiesServlet.getProperty("clx.localRootDirectory");
  private static final String ftpUsername = PropertiesServlet.getProperty("clx.FTPUser");
  private static final String ftpPassword = PropertiesServlet.getProperty("clx.FTPPassword");

  public BillingInterface()
  {
  }

  public void execute()
  {
    String interfaceName = getParameterValue("interfaceName");
    String instance = getParameterValue("instance");
    String remoteFileLocation = '/' + instance + FileLocation;
    String localFileLocation = localRootDirectory + instance + FileLocation;
    DataSource ds = CommonUtility.getDataSourceBy11iInstance(getParameterValue("instance"));
    String userId = ValsparLookUps.queryForSingleValue(ConnectionAccessBean.getConnection(ds), "select user_id from fnd_user where upper(user_name) = upper(?)", "TMS");

    log4jLogger.info("CLX Billing started for " + interfaceName);
    FTPFile[] fileArray = FtpUtility.listFiles(clxFTPServer, ftpUsername, ftpPassword, remoteFileLocation, null);
    for (int i = 0; i < fileArray.length; i++)
    {
      String orgFileName = null;
      try
      {
        FTPFile file = fileArray[i];
        orgFileName = file.getName();
        String runDate = CommonUtility.getFormattedDate("yyyyMMddHHmmss");  
        int len = orgFileName.lastIndexOf('.');
        String fileName = orgFileName.substring(0, len) + "-" + runDate + orgFileName.substring(len);
        FtpUtility.retrieveFile(clxFTPServer, ftpUsername, ftpPassword, remoteFileLocation + file.getName(), localFileLocation + fileName, false);
        List<BillingBean> billingBeanList = BillingDAO.buildBillingBeanList(localFileLocation + fileName, orgFileName.substring(0, orgFileName.lastIndexOf('_')), userId);
        if(!billingBeanList.isEmpty())
        {
          BillingDAO.saveBillingBeanList(billingBeanList, ds);
        }
      }
      catch (Exception e)
      {
        log4jLogger.error(e);
      }
      finally
      {
        log4jLogger.info("CLX Billing finished for " + orgFileName + "!");
      }
    }
  }
}
