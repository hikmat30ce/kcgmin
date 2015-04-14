package com.valspar.interfaces.common.utils;

import com.valspar.interfaces.common.beans.InterfaceInfoBean;
import java.io.*;
import java.text.*;
import java.util.Calendar;
import org.apache.commons.lang.StringUtils;
import org.apache.log4j.DailyRollingFileAppender;
import org.apache.log4j.spi.LoggingEvent;

public class InterfacesFileAppender extends DailyRollingFileAppender
{
  private static String logRoot;
  private static final DateFormat df = new SimpleDateFormat("dd-MMM-yyyy HH:mm:ss");

  @Override
  public void activateOptions()
  {
    super.activateOptions();

    setLogRoot(StringUtils.substringBeforeLast(getFile(), File.separator));
  }

  public void append(LoggingEvent loggingEvent)
  {
    InterfaceInfoBean interfaceInfo = InterfaceThreadManager.getActiveInterfaceInfo();

    if (interfaceInfo != null)
    {
      File f = new File(interfaceInfo.getLogFileLocation());
      f.getParentFile().mkdirs();

      FileOutputStream fos = null;
      PrintWriter pw = null;
      try
      {
        fos = new FileOutputStream(f, true);
        pw = new PrintWriter(fos, true);

        String line = layout.format(loggingEvent);
        pw.write(line);
        return;
      }
      catch (Exception e)
      {
        System.out.println(df.format(Calendar.getInstance().getTime()) + " - InterfacesFileAppender.append(): Error writing to specific log file, writing to general log file.  Exception is: " + e);
      }
      finally
      {
        CommonUtility.close(pw);
        CommonUtility.close(fos);
      }
    }

    super.append(loggingEvent);
  }

  public static void setLogRoot(String logRoot)
  {
    InterfacesFileAppender.logRoot = logRoot;
  }

  public static String getLogRoot()
  {
    return logRoot;
  }
}
