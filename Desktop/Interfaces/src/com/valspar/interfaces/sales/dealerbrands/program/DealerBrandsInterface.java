package com.valspar.interfaces.sales.dealerbrands.program;

import com.valspar.interfaces.common.BaseInterface;
import com.valspar.interfaces.common.servlets.PropertiesServlet;
import com.valspar.interfaces.common.utils.NotificationUtility;
import com.valspar.interfaces.sales.dealerbrands.beans.DealerBrandsInputNotificationBean;
import com.valspar.interfaces.sales.dealerbrands.dao.DealerBrandsDAO;
import java.util.Date;
import org.apache.log4j.Logger;

public class DealerBrandsInterface extends BaseInterface 
{
  private static Logger log4jLogger = Logger.getLogger(DealerBrandsInterface.class);     
 
  public DealerBrandsInterface()
  {
  }

  public void execute()
  {
    DealerBrandsInputNotificationBean dealerBrandsInputNotificationBean = new DealerBrandsInputNotificationBean();
    log4jLogger.info("dealer brands interface starting ");
    Date startDate = new Date();
    dealerBrandsInputNotificationBean.setStartDate(startDate);
    DealerBrandsDAO.dealerBrandsAccounts(dealerBrandsInputNotificationBean);
    Date endDate = new Date();
    dealerBrandsInputNotificationBean.setEndDate(endDate);
    dealerBrandsInputNotificationBean.setServer(PropertiesServlet.getProperty("webserver"));
    NotificationUtility.sendDealerBrandsNotifcationEmail(dealerBrandsInputNotificationBean);
    log4jLogger.info("dealer brands interface ended ");
  }
}
