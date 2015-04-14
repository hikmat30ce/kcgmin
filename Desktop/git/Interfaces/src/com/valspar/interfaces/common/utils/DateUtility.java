package com.valspar.interfaces.common.utils;

import java.util.*;

public class DateUtility
{
  public DateUtility()
  {
  }
  
  public static Date getDate(Date date)
  {
    if (date == null)
    {
      return null;
    }
    else
    {
      return new Date();
    }
  }
  
  public static String getDayOfMonth(Date date)
  {
    Calendar calendar = Calendar.getInstance();
    calendar.setTime(date);
    return String.valueOf(calendar.get(Calendar.DAY_OF_MONTH));
  }

  public static String getMonth(Date date)
  {
    Calendar calendar = Calendar.getInstance();
    calendar.setTime(date);
    return String.valueOf(calendar.get(Calendar.MONTH)+1);
  }
  
  public static String getYear(Date date)
  {
    Calendar calendar = Calendar.getInstance();
    calendar.setTime(date);
    return String.valueOf(calendar.get(Calendar.YEAR));
  }
  
  public static String getHourOfDay(Date date)
  {
    Calendar calendar = Calendar.getInstance();
    calendar.setTime(date);
    return String.valueOf(calendar.get(Calendar.HOUR_OF_DAY));
  }
  
  public static String getMinute(Date date)
  {
    Calendar calendar = Calendar.getInstance();
    calendar.setTime(date);
    return String.valueOf(calendar.get(Calendar.MINUTE));
  }
  
  public static String getSecond(Date date)
  {
    Calendar calendar = Calendar.getInstance();
    calendar.setTime(date);
    return String.valueOf(calendar.get(Calendar.SECOND));
  }  
}
