package com.valspar.interfaces.sales.dealerbrands.beans;

import com.sforce.soap.enterprise.sobject.Account;
import com.valspar.interfaces.common.utils.NotificationUtility;
import java.text.*;
import java.util.*;

public class DealerBrandsInputNotificationBean
{
  private Date startDate;
  private Date endDate;
  private int rowCount; 
  private int errorCount;
  private String server;
  private  List<Account> errorAccountList=new ArrayList<Account>();
  public DealerBrandsInputNotificationBean()
  {
    this.setStartDate(new Date());
  }

  public void setStartDate(Date startDate)
  {
    this.startDate = startDate;
  }

  public Date getStartDate()
  {
    return startDate;
  }

  public void setEndDate(Date endDate)
  {
    this.endDate = endDate;
  }

  public Date getEndDate()
  {
    return endDate;
  }

  public void setRowCount(int rowCount)
  {
    this.rowCount = rowCount;
  }

  public int getRowCount()
  {
    return rowCount;
  }

  public void setErrorCount(int errorCount)
  {
    this.errorCount = errorCount;
  }

  public int getErrorCount()
  {
    return errorCount;
  }

  public String getDuration()
  {
    return NotificationUtility.getRunTimeDuration(this.getStartDate(), this.getEndDate());
  }
  
  public String getBatchId()
  {
    DateFormat dateFormat = new SimpleDateFormat("yy-MM-dd_");
    return dateFormat.format(new Date());
  }

  public void setErrorAccountList(List<Account> errorAccountList)
  {
    this.errorAccountList = errorAccountList;
  }

  public List<Account> getErrorAccountList()
  {
    return errorAccountList;
  }

  public void setServer(String server)
  {
    this.server = server;
  }

  public String getServer()
  {
    return server;
  }
}

