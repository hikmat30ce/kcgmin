package com.valspar.interfaces.hr.submitpayrollinput.beans;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.GregorianCalendar;
import javax.xml.datatype.DatatypeFactory;
import javax.xml.datatype.XMLGregorianCalendar;
import org.apache.log4j.Logger;

public class IntCurrentPayPeriodBean
{
  private static Logger log4jLogger = Logger.getLogger(IntCurrentPayPeriodBean.class);

  private String periodStartDate;
  private String periodEndDate;
  
  public IntCurrentPayPeriodBean()
  {
  }

  public void setPeriodStartDate(String periodStartDate)
  {
    this.periodStartDate = periodStartDate;
  }

  public XMLGregorianCalendar getPeriodStartDate()
  {
    XMLGregorianCalendar xmlDate = null;
    try
    {
      DateFormat df=new SimpleDateFormat("yyyy-MM-dd");
      GregorianCalendar cal = new GregorianCalendar();
      cal.setTime(df.parse(periodStartDate));
      xmlDate = DatatypeFactory.newInstance().newXMLGregorianCalendarDate(cal.get(Calendar.YEAR), cal.get(Calendar.MONTH)+1, cal.get(Calendar.DAY_OF_MONTH)+1, cal.getTimeZone().LONG).normalize();
    }
    catch (Exception e)
    {
      log4jLogger.error(e);
    }
    return xmlDate;
  }

  public void setPeriodEndDate(String periodEndDate)
  {
    this.periodEndDate = periodEndDate;
  }

  public XMLGregorianCalendar getPeriodEndDate()
  {
    XMLGregorianCalendar xmlDate = null;
    try
    {
      DateFormat df=new SimpleDateFormat("yyyy-MM-dd");
      GregorianCalendar cal = new GregorianCalendar();
      cal.setTime(df.parse(periodEndDate));
      xmlDate = DatatypeFactory.newInstance().newXMLGregorianCalendarDate(cal.get(Calendar.YEAR), cal.get(Calendar.MONTH)+1, cal.get(Calendar.DAY_OF_MONTH)+1, cal.getTimeZone().LONG).normalize();
    }
    catch (Exception e)
    {
      log4jLogger.error(e);
    }
    return xmlDate;
  }
}
