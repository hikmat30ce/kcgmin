package com.valspar.interfaces.regulatory.dea.beans;

public class DeaBean 
{

  java.util.Date todays_date;             // Todays date
  String todays_mm;                       // Todays MM
  String last_run_date;                   // Date this program was last run
  String last_run_mm;                     // Month (MM) this program was last run
  String rpt_date_time;                   // Date-time stamp for file names
  String rpt_month;                       // Month to be appended to month end report
  String email_to;                        // Contains email addresses of who to send report to

  public DeaBean()
  {
  }

 
 

  public void setTodays_date(java.util.Date todays_date)
  {
    this.todays_date = todays_date;
  }


  public java.util.Date getTodays_date()
  {
    return todays_date;
  }


  public void setTodays_mm(String todays_mm)
  {
    this.todays_mm = todays_mm;
  }


  public String getTodays_mm()
  {
    return todays_mm;
  }


  public void setLast_run_date(String last_run_date)
  {
    this.last_run_date = last_run_date;
  }


  public String getLast_run_date()
  {
    return last_run_date;
  }


  public void setLast_run_mm(String last_run_mm)
  {
    this.last_run_mm = last_run_mm;
  }


  public String getLast_run_mm()
  {
    return last_run_mm;
  }


  public void setEmail_to(String email_to)
  {
    this.email_to = email_to;
  }


  public String getEmail_to()
  {
    return email_to;
  }


  public void setRpt_date_time(String rpt_date_time)
  {
    this.rpt_date_time = rpt_date_time;
  }


  public String getRpt_date_time()
  {
    return rpt_date_time;
  }


  public void setRpt_month(String rpt_month)
  {
    this.rpt_month = rpt_month;
  }


  public String getRpt_month()
  {
    return rpt_month;
  }


 
}