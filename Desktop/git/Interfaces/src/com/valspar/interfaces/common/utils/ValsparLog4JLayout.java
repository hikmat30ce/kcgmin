package com.valspar.interfaces.common.utils;

import java.io.*;
import java.lang.reflect.*;
import java.text.*;
import java.util.Date;
import org.apache.commons.lang.*;
import org.apache.log4j.*;
import org.apache.log4j.spi.*;

public class ValsparLog4JLayout extends Layout
{
  private static DateFormat dFormat = new SimpleDateFormat("dd-MMM-yyyy HH:mm:ss");

  public ValsparLog4JLayout()
  {
    super();
  }

  public boolean ignoresThrowable()
  {
    return false;
  }

  private static String getStackTrace(Throwable t)
  {
    StringWriter sw = new StringWriter();
    PrintWriter pw = new PrintWriter(sw);
    t.printStackTrace(pw);
    return sw.toString();
  }

  private static String getStackTrace(Throwable t, int maxLines)
  {
    String fullStackTrace = getStackTrace(t);
    String[] lines = StringUtils.split(fullStackTrace, "\r\n");

    if (lines.length > maxLines)
    {
      String[] topStackTrace = (String[]) ArrayUtils.subarray(lines, 0, maxLines - 1);

      StringBuilder sb = new StringBuilder();
      sb.append(StringUtils.join(topStackTrace, "\r\n"));
      sb.append("\r\n        ... ");
      sb.append(lines.length - maxLines);
      sb.append(" more");

      return sb.toString();
    }
    else
    {
      return fullStackTrace;
    }
  }

  private static String formatThrowable(LoggingEvent event, Throwable t)
  {
    StringBuilder sb = new StringBuilder();
    StackTraceElement ste = null;
    String loggerClassName = event.getLoggerName();

    if (loggerClassName.startsWith("com.valspar."))
    {
      for (StackTraceElement el: t.getStackTrace())
      {
        String stackTraceElementClassName = el.getClassName();

        if (StringUtils.equals(stackTraceElementClassName, loggerClassName))
        {
          ste = el;
          break;
        }
      }

      if (ste != null)
      {
        sb.append("Exception in ");
        sb.append(ste);
        sb.append(": ");
        sb.append(StringUtils.chomp(t.toString()));
      }
      else
      {
        sb.append("Exception occurred, could not match stack frame.  Full stack trace follows:");
      }
    }

    if (ste == null)
    {
      sb.append("\n");
      sb.append(StringUtils.chomp(getStackTrace(t)));
    }
    else if (t instanceof InvocationTargetException)
    {
      sb.append("\nCaused by:");
      sb.append(StringUtils.chomp(getStackTrace(t.getCause(), 5)));
    }

    return sb.toString();
  }

  public static String formatEvent(LoggingEvent event)
  {
    Object message = event.getMessage();

    StringBuilder sb = new StringBuilder();

    if (message instanceof Throwable)
    {
      sb.append(formatThrowable(event, (Throwable) message));
    }
    else
    {
      ThrowableInformation ti = event.getThrowableInformation();

      if (ti != null)
      {
        Throwable t = ti.getThrowable();

        if (t != null)
        {
          sb.append(formatThrowable(event, t));

          if (message != null)
          {
            sb.append("\n==> Additional Info: ");
          }
        }
      }

      if (message != null)
      {
        sb.append(StringUtils.chomp(message.toString()));
      }
    }

    return sb.toString();
  }

  public String format(LoggingEvent event)
  {
    StringBuilder sb = new StringBuilder();

    if (event.getLevel() == Level.ERROR)
    {
      sb.append("\n");
    }

    sb.append(StringUtils.rightPad(event.getLevel().toString(), 5));
    sb.append(" ");
    sb.append(dFormat.format(new Date()));
    sb.append(" - ");
    sb.append(formatEvent(event));
    sb.append("\n");
    return sb.toString();
  }

  public void activateOptions()
  {
  }
}
