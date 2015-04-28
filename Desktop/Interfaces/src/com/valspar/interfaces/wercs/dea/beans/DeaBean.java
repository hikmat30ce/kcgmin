package com.valspar.interfaces.wercs.dea.beans;

import java.util.Date;

public class DeaBean 
{
  private Date  todaysDate;             // Todays date
  private String todaysMm;                       // Todays MM
  private String lastRunDate;                   // Date this program was last run
  private String lastRunMm;                     // Month (MM) this program was last run
  private String rptDateTime;                   // Date-time stamp for file names
  private String rptMonth;                       // Month to be appended to month end report
  private String emailTo;                        // Contains email addresses of who to send report to
  private int rowCount;

  public DeaBean()
  {
  }

  public void setTodaysDate(java.util.Date todaysDate)
  {
    this.todaysDate = todaysDate;
  }

  public java.util.Date getTodaysDate()
  {
    return todaysDate;
  }

  public void setTodaysMm(String todaysMm)
  {
    this.todaysMm = todaysMm;
  }

  public String getTodaysMm()
  {
    return todaysMm;
  }

  public void setLastRunDate(String lastRunDate)
  {
    this.lastRunDate = lastRunDate;
  }

  public String getLastRunDate()
  {
    return lastRunDate;
  }

  public void setLastRunMm(String lastRunMm)
  {
    this.lastRunMm = lastRunMm;
  }

  public String getLastRunMm()
  {
    return lastRunMm;
  }

  public void setEmailTo(String emailTo)
  {
    this.emailTo = emailTo;
  }

  public String getEmailTo()
  {
    return emailTo;
  }

  public void setRptDateTime(String rptDateTime)
  {
    this.rptDateTime = rptDateTime;
  }

  public String getRptDateTime()
  {
    return rptDateTime;
  }

  public void setRptMonth(String rptMonth)
  {
    this.rptMonth = rptMonth;
  }

  public String getRptMonth()
  {
    return rptMonth;
  }
  
  public boolean doesReportHaveData()
  {
   if(getRowCount() >0)
    {
      return true;
    }
    else
    {
      return false;
    }
  }
 
  public void setRowCount(int rowCount)
  {
    this.rowCount = rowCount;
  }

  public int getRowCount()
  {
    return rowCount;
  }
}
