package com.valspar.interfaces.common.utils;

import com.valspar.interfaces.common.beans.*;
import com.valspar.interfaces.common.enums.DataSource;
import java.text.*;
import java.util.Calendar;
import oracle.jdbc.*;
import org.apache.log4j.*;
import org.apache.log4j.spi.LoggingEvent;

public class ValsparLog4JJDBCAppender extends AppenderSkeleton
{
  private static Logger logger = Logger.getLogger(ValsparLog4JJDBCAppender.class);
  private static final DateFormat df = new SimpleDateFormat("dd-MMM-yyyy HH:mm:ss");

  private Appender fileAppender;

  public ValsparLog4JJDBCAppender()
  {
    super();
    fileAppender = Logger.getRootLogger().getAppender("FILE");
  }

  protected void append(LoggingEvent loggingEvent)
  {
    InterfaceInfoBean interfaceInfo = InterfaceThreadManager.getActiveInterfaceInfo();

    if (interfaceInfo != null)
    {
      OracleConnection conn = null;
      OraclePreparedStatement pst = null;

      try
      {
        StringBuilder sb = new StringBuilder();
        sb.append("INSERT INTO quartz.vca_schedule_process_err ");
        sb.append("   (ERR_ID, PROCESS_ID, MESSAGE, DATE_LOGGED) ");
        sb.append("VALUES ");
        sb.append("   (quartz.vca_schedule_process_err_SEQ.NEXTVAL, ?, ?, SYSDATE) ");

        conn = (OracleConnection) ConnectionAccessBean.getConnection(DataSource.MIDDLEWARE);
        pst = (OraclePreparedStatement) conn.prepareStatement(sb.toString());
        pst.setString(1, interfaceInfo.getProcessId());
        pst.setString(2, ValsparLog4JLayout.formatEvent(loggingEvent));
        pst.execute();
      }
      catch (Exception e)
      {
        try
        {
          LoggingEvent event = new LoggingEvent(ValsparLog4JJDBCAppender.class.getName(), logger, Level.ERROR, null, e);
          fileAppender.doAppend(event);
        }
        catch (Exception e2)
        {
          System.out.println(df.format(Calendar.getInstance().getTime()) + " - ValsparLog4JJDBCAppender.append():  " + e + ".  And could not log to InterfacesFileAppender.  That exception was: " + e2);
        }
      }
      finally
      {
        JDBCUtil.close(pst);
        JDBCUtil.close(conn);
      }
    }
  }

  public boolean requiresLayout()
  {
    return false;
  }

  public void close()
  {
  }
}
