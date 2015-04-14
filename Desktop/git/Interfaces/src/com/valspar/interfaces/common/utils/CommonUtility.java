package com.valspar.interfaces.common.utils;

import com.valspar.interfaces.common.Constants;
import com.valspar.interfaces.common.enums.DataSource;
import java.io.*;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import org.apache.log4j.Logger;
import com.google.i18n.phonenumbers.PhoneNumberUtil;
import com.google.i18n.phonenumbers.Phonenumber;
import org.apache.commons.lang.StringUtils;
import com.valspar.interfaces.common.servlets.PropertiesServlet;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public final class CommonUtility
{
  private static Logger log4jLogger = Logger.getLogger(CommonUtility.class);

  private CommonUtility()
  {
  }

  public static String nvl(String temp)
  {
    if (temp == null)
    {
      return Constants.EMPTY_STRING;
    }
    else
    {
      return temp;
    }
  }

  public static void close(Closeable c)
  {
    if (c != null)
    {
      try
      {
        c.close();
      }
      catch (IOException e)
      {
        log4jLogger.error(e);
      }
    }
  }

  public static String getDataDirectoryPath()
  {
   return PropertiesServlet.getProperty("reportDir");
    }

  public static String getFormattedDate(String dateFormat)
  {
    java.util.Date d = Calendar.getInstance().getTime();
    SimpleDateFormat sdf = new SimpleDateFormat(dateFormat);
    return sdf.format(d);
  }
  
  public static Date getFormattedDate(String dateFormat, Date d)
  {
    Date newDate = d;
    try
    {
      SimpleDateFormat sdf = new SimpleDateFormat(dateFormat);
      newDate = sdf.parse(sdf.format(d));
    }
    catch (Exception e)
    {
      log4jLogger.error(e);
    }
    return newDate;
  }
  
  public static Date getFormattedDate(String dateFormat, String d)
  {
    Date newDate = null;
    try
    {
      SimpleDateFormat sdf = new SimpleDateFormat(dateFormat);
      newDate = sdf.parse(d);
    }
    catch (Exception e)
    {
      log4jLogger.error(e);
    }
    return newDate;
  }
 
  public static DataSource getDataSourceByJndiName(String jndiName)
  {
    for (DataSource ds : DataSource.values()) 
    {
      if (ds.getWebLogicDataSource().equalsIgnoreCase(jndiName))
      {
        return ds;
      }
    }
    return null;
  }
  
  public static DataSource getDataSourceBy11iInstance(String instance)
  {
    for (DataSource dataSource: DataSource.values())
    {
      if (dataSource.getInstanceCodeOf11i() != null && dataSource.getInstanceCodeOf11i().equalsIgnoreCase(instance))
      {
        return dataSource;
      }
    }
    return null;
  }

 public static String calculateRunTime(java.util.Date startDate, java.util.Date endDate)
  {
    String duration = null;  
    long diff = endDate.getTime()-startDate.getTime();      
    diff = diff/1000;  
    String format = String.format("%%0%dd", 2);  
    String seconds = String.format(format, diff % 60);  
    String minutes = String.format(format, (diff % 3600) / 60);  
    String hours = String.format(format, diff / 3600);  
    duration =  hours + ":" + minutes + ":" + seconds;  
    return duration;   
  }
public static String convertPhoneNumberToCountryFormat(String countryCode, String phoneNumber)
  {
    PhoneNumberUtil.PhoneNumberFormat format = null;
    if (countryCode != null && countryCode.equalsIgnoreCase("US"))
    {
      format = PhoneNumberUtil.PhoneNumberFormat.NATIONAL;
    }
    else
    {
      format = PhoneNumberUtil.PhoneNumberFormat.INTERNATIONAL;
    }

    try
    {
      PhoneNumberUtil phoneUtil = PhoneNumberUtil.getInstance();
      if (countryCode != null && phoneNumber != null && phoneUtil.isPossibleNumber(phoneNumber, countryCode))
      {
        Phonenumber.PhoneNumber result = phoneUtil.parse(phoneNumber, countryCode);
        phoneNumber = phoneUtil.format(result, format);
      }
    }
    catch (Exception e)
    {
      //Left intentionally blank. We don't want to log bad phone number formats to the log file.
      ;
    }
    return phoneNumber;
  }

  public static String nvl(String testValue, String ifNullValue)
  {
    String returnValue = testValue;

    if (StringUtils.isBlank(testValue))
    {
      returnValue = ifNullValue;
}

    return returnValue;
  }
  
  public static boolean isNumeric(String number)
  {
    try
    {
      Double.parseDouble(number);
    }
    catch (Exception e)
    {
      return false;
    }
    return true;
  }
  
  public static String toVarchar(String value)
  {
    return String.format("'%s'", value);
  }
  
  public static List<DataSource> getERPDataSourceList()
  {
    List<DataSource> dsList = new ArrayList<DataSource>();
    dsList.add(DataSource.NORTHAMERICAN);
    dsList.add(DataSource.ASIAPAC);
    dsList.add(DataSource.EMEAI);
    return dsList;
  }
}
