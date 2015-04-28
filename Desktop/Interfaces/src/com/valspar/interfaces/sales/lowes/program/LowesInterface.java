package com.valspar.interfaces.sales.lowes.program;

import com.valspar.interfaces.common.BaseInterface;
import com.valspar.interfaces.common.servlets.PropertiesServlet;
import com.valspar.interfaces.common.utils.NotificationUtility;
import com.valspar.interfaces.sales.lowes.beans.LowesInputNotificationBean;
import com.valspar.interfaces.sales.lowes.dao.LowesDAO;
import java.util.Date;
import org.apache.log4j.Logger;

public class LowesInterface extends BaseInterface
{
  private static Logger log4jLogger = Logger.getLogger(LowesInterface.class);   

  public LowesInterface()
  {
  }

  public void execute()
  {
    log4jLogger.info("lowes interface starting ");
    LowesInputNotificationBean lowesInputNotificationBean = new LowesInputNotificationBean();
    LowesDAO.loadAccountsAndContacts(lowesInputNotificationBean);
    Date endDate = new Date();
    lowesInputNotificationBean.setServer(PropertiesServlet.getProperty("webserver"));
    lowesInputNotificationBean.setEndDate(endDate);
    NotificationUtility.sendLowesNotifcationEmail(lowesInputNotificationBean);
    log4jLogger.info("lowes interface complete");
  }
}
