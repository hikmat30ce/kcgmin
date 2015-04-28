package com.valspar.interfaces.hr.submitpayrollinput.beans;

import com.valspar.interfaces.common.utils.NotificationUtility;
import java.text.*;
import java.util.*;

public class PayrollInputNotificationBean
{
  private Date startDate;
  private Date endDate;
  private int rowCount;
  private int errorCount;
  private String fileName;
  
  public PayrollInputNotificationBean()
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

  public void setFileName(String fileName)
  {
    this.fileName = fileName;
  }

  public String getFileName()
  {
    return fileName;
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
    return dateFormat.format(new Date()) + this.getFileName();
  }
}