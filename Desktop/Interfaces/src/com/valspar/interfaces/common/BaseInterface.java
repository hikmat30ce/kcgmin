package com.valspar.interfaces.common;

import com.valspar.interfaces.common.beans.ConnectionAccessBean;
import com.valspar.interfaces.common.beans.ConnectionBean;
import com.valspar.interfaces.common.beans.InterfaceInfoBean;
import com.valspar.interfaces.common.enums.*;
import com.valspar.interfaces.common.servlets.PropertiesServlet;
import com.valspar.interfaces.common.utils.*;
import com.valspar.interfaces.common.utils.InterfaceThreadManager;
import java.io.File;
import java.util.Date;
import java.sql.*;
import java.text.*;
import java.util.*;
import oracle.jdbc.*;
import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.quartz.*;

public abstract class BaseInterface implements Job
{
  private static Logger log4jLoggerBase = Logger.getLogger(BaseInterface.class);

  private String dbEnvironment;
  private String processId;
  private String interfaceId;
  private String interfaceName;
  private InterfaceInfoBean interfaceInfoBean;

  private OracleConnection logConn;
  private List<ConnectionBean> fromConn = new ArrayList<ConnectionBean>();
  private List<ConnectionBean> toConn = new ArrayList<ConnectionBean>();

  private String notificationEmail;
  private List<String> emailMessages = new ArrayList<String>();

  private static final String DIRECTION_FROM = "from";
  private static final String DIRECTION_TO = "to";

  private Date startDate;
  private static final DateFormat df = new SimpleDateFormat("MM-dd-yyyy-HH-mm-ss");
  private JobExecutionContext context;
  private boolean deleteLogFile;

  public BaseInterface()
  {    
  }  

  public void execute(JobExecutionContext context)
  {
    this.setContext(context);
    init(context.getJobDetail().getKey().getName());

    try
    {
      execute();
    }
    catch (Exception e)
    {
      log4jLoggerBase.error(e);
    }
    finally
    {
      cleanUpETL();
    }
  }

  public abstract void execute();

  protected String getParameterValue(String parameterName)
  {
    JobDataMap jobDataMap = getContext().getJobDetail().getJobDataMap();
    return jobDataMap.getString(parameterName);
  }

  protected String getJobName()
  {
    return getContext().getJobDetail().getKey().getName();
  }

  private void init(String jobKey)
  {
    setInterfaceName(jobKey);
    setStartDate(new Date());

    File logFile = buildLogFileLocation();
    InterfaceInfoBean interfaceInfoBean = new InterfaceInfoBean(logFile);
    setInterfaceInfoBean(interfaceInfoBean);
    InterfaceThreadManager.addInterfaceThread(interfaceInfoBean);

    log4jLoggerBase.info(interfaceName + " Started ...");
    logConn = (OracleConnection) ConnectionAccessBean.getConnection(DataSource.MIDDLEWARE);

    processStart();
    log4jLoggerBase.info("Interface Process ID = " + processId);

    interfaceInfoBean.setProcessId(processId);
  }

  private File buildLogFileLocation()
  {
    String className = this.getClass().getName();
    // --> com.valspar.interfaces.purchasing.cipace.program.CipAceInterface

    className = StringUtils.substringAfter(className, "com.valspar.interfaces.");
    // --> purchasing.cipace.program.CipAceInterface

    className = StringUtils.replace(className, ".program.", ".");
    // --> purchasing.cipace.CipAceInterface

    StringBuilder sb = new StringBuilder();
    sb.append(InterfacesFileAppender.getLogRoot());
    sb.append(File.separator);
    sb.append(StringUtils.replace(className, ".", File.separator));
    sb.append("_");
    sb.append(df.format(startDate));
    sb.append(".log");
    
    return new File (sb.toString()); 
  }

  public BaseInterface(String interfaceName, String dbEnvironment)
  {
    setInterfaceName(interfaceName);
    setDbEnvironment(dbEnvironment);

    try
    {
      log4jLoggerBase.info(interfaceName + " Started ...");

      logConn = (OracleConnection)DataAccessBean.getConnection(dbEnvironment, null, "WERCS", "ORACLE", null);

      processStart();
      log4jLoggerBase.info("Interface Process ID = " + processId);

      loadConnections(DIRECTION_FROM);
      loadConnections(DIRECTION_TO);
    }
    catch (Exception e)
    {
      log4jLoggerBase.error(e);
    }
  }

  private void processStart()
  {
    PreparedStatement pst = null;
    ResultSet rs = null;
    OraclePreparedStatement pst2 = null;

    try
    {
      StringBuilder sb = new StringBuilder();
      sb.append("SELECT schedule_id, notification_email, quartz.vca_schedule_process_SEQ.NEXTVAL PROCESS_ID  ");
      sb.append("FROM quartz.vca_schedule  ");
      sb.append("WHERE job_key = ? ");

      pst = logConn.prepareStatement(sb.toString());
      pst.setString(1, interfaceName);
      rs = pst.executeQuery();
      if (rs.next())
      {
        setInterfaceId(rs.getString("schedule_id"));
        setNotificationEmail(rs.getString("notification_email"));
        setProcessId(rs.getString("PROCESS_ID"));
      }

      sb = new StringBuilder();
      sb.append("INSERT INTO quartz.vca_schedule_process (");
      sb.append("   PROCESS_ID, schedule_id, START_DATE, log_file) ");
      sb.append("VALUES (");
      sb.append("   ?, ?, ?, ?)");

      pst2 = (OraclePreparedStatement)logConn.prepareStatement(sb.toString());
      pst2.setString(1, processId);
      pst2.setString(2, interfaceId);
      pst2.setDATE(3, JDBCUtil.getDATE(startDate));
      pst2.setString(4, getInterfaceInfoBean().getLogFile().getCanonicalPath());
      pst2.executeUpdate();
    }
    catch (Exception e)
    {
      log4jLoggerBase.error(e);
    }
    finally
    {
      JDBCUtil.close(pst, rs);
      JDBCUtil.close(pst2);
    }
  }

  private void loadConnections(String direction)
  {
    PreparedStatement pst = null;
    ResultSet rs = null;

    try
    {
      String dbColumnPrefix = null;
      if (StringUtils.equalsIgnoreCase(dbEnvironment, "REGP"))
      {
        dbColumnPrefix = "production";
      }
      else if (StringUtils.equalsIgnoreCase(dbEnvironment, "RSUP"))
      {
        dbColumnPrefix = "test";
      }
      else if (StringUtils.equalsIgnoreCase(dbEnvironment, "RDEV"))
      {
        dbColumnPrefix = "dev";
      }
      else if (StringUtils.equalsIgnoreCase(dbEnvironment, "RPRJ"))
      {
        dbColumnPrefix = "project";
      }

      StringBuilder sb = new StringBuilder();
      sb.append("select distinct ");
      sb.append("       a.");
      sb.append(dbColumnPrefix);
      sb.append("_db db_name, a.");
      sb.append(dbColumnPrefix);
      sb.append("_db_sid db_sid, ");
      sb.append("       a.user_name, a.version, a.log_user, ");
      sb.append("       a.db_type, a.password, a.location ");
      sb.append("from va_reg_databases a, va_reg_interface_databases b ");
      sb.append("where a.db_id = b.");
      sb.append(direction);
      sb.append("_db_id ");
      sb.append("  and b.interface_id = ? ");
      sb.append("  and b.active = 1");

      pst = logConn.prepareStatement(sb.toString());
      pst.setString(1, interfaceId);
      rs = pst.executeQuery();

      while (rs.next())
      {
        ConnectionBean cb = new ConnectionBean();
        String dbName = rs.getString("db_name");
        String dbSid = rs.getString("db_sid");
        cb.setUserName(rs.getString("user_name"));
        cb.setLogUser(rs.getString("log_user"));
        cb.setDbType(rs.getString("db_type"));
        cb.setPassword(rs.getString("password"));
        cb.setLocation(rs.getString("location"));
        cb.setConnection(DataAccessBean.getConnection(dbName, dbSid, cb.getUserName(), cb.getDbType(), cb.getPassword()));
        cb.setVersion(rs.getString("version"));

        if (cb.getConnection() == null)
        {
          log4jLoggerBase.error("Error loading connection.  Connection is null for DB: " + dbName);
        }
        else if (direction.equalsIgnoreCase(DIRECTION_FROM))
        {
          fromConn.add(cb);
        }
        else if (direction.equalsIgnoreCase(DIRECTION_TO))
        {
          toConn.add(cb);
        }
      }
    }
    catch (Exception e)
    {
      log4jLoggerBase.error(e);
    }
    finally
    {
      JDBCUtil.close(pst, rs);
    }
  }

  protected boolean onHold()
  {
    boolean onHoldReturn = false;
    PreparedStatement pst = null;
    ResultSet rs = null;
    try
    {
      StringBuilder sql = new StringBuilder();
      sql.append("select on_hold ");
      sql.append("from VA_REG_INTERFACES_RUN_ENV ");
      sql.append("where interface_id = ? ");

      pst = logConn.prepareStatement(sql.toString());
      pst.setString(1, interfaceId);

      rs = pst.executeQuery();

      if (rs.next())
      {
        if (StringUtils.equalsIgnoreCase(rs.getString(1), "Y"))
        {
          onHoldReturn = true;
        }
      }
    }
    catch (Exception e)
    {
      log4jLoggerBase.error(e);
    }
    finally
    {
      JDBCUtil.close(pst, rs);
    }
    return onHoldReturn;
  }

  protected List<String> fetchErrorMessages()
  {
    List<String> errorMessages = new ArrayList<String>();

    PreparedStatement pst = null;
    ResultSet rs = null;

    try
    {
      StringBuilder sb = new StringBuilder();
      sb.append("SELECT MESSAGE ");
      sb.append("FROM quartz.vca_schedule_process_err ");
      sb.append("WHERE PROCESS_ID = ? ");
      sb.append("ORDER BY ERR_ID ");

      pst = logConn.prepareStatement(sb.toString());
      pst.setString(1, processId);
      rs = pst.executeQuery();

      while (rs.next())
      {
        errorMessages.add(rs.getString("message"));
      }
    }
    catch (Exception e)
    {
      log4jLoggerBase.error(e);
    }
    finally
    {
      JDBCUtil.close(pst, rs);
    }

    return errorMessages;
  }

  protected void addToEmail(String message)
  {
    emailMessages.add(message);
  }

  private void sendEmailMessages()
  {
    if (!emailMessages.isEmpty() && getNotificationEmail() != null)
    {
      StringBuilder message = new StringBuilder();
      for (String emsg: emailMessages)
      {
        message.append(emsg);
        message.append("\n\n");
      }
      EmailBean.emailMessage("Message from the " + getInterfaceName(), message.toString(), getNotificationEmail());
    }
  }

  public boolean isInProduction()
  {
    return StringUtils.equalsIgnoreCase(PropertiesServlet.getProperty("environment"), "production");
  }

  private void processStop()
  {
    PreparedStatement pst = null;
    try
    {
      StringBuilder sb = new StringBuilder();
      sb.append("UPDATE quartz.vca_schedule_process ");
      sb.append("SET END_DATE = SYSDATE ");
      sb.append("WHERE PROCESS_ID = ? ");

      pst = logConn.prepareStatement(sb.toString());
      pst.setString(1, processId);
      pst.executeUpdate();
    }
    catch (Exception e)
    {
      log4jLoggerBase.error(e);
    }
    finally
    {
      JDBCUtil.close(pst);
    }
  }

  protected void cleanUp()
  {
    try
    {
      List<ConnectionBean> allConnections = new ArrayList<ConnectionBean>();
      allConnections.addAll(toConn);
      allConnections.addAll(fromConn);

      for (ConnectionBean cb: allConnections)
      {
        if (!cb.getConnection().getAutoCommit())
        {
          cb.getConnection().commit();
        }
        String dbName = cb.getDbName();
        JDBCUtil.close(cb.getConnection());
        log4jLoggerBase.info(dbName + " connection was sucessfully closed.");
      }
      processStop();
      sendEmailMessages();

      JDBCUtil.close(logConn);
      log4jLoggerBase.info("Logging connection was sucessfully closed.");

      log4jLoggerBase.info(getInterfaceName() + " Complete.");
    }
    catch (Exception e)
    {
      log4jLoggerBase.error(e);
    }
  }

  private void cleanUpETL()
  {
    try
    {
      processStop();
      sendEmailMessages();
      JDBCUtil.close(logConn);
      log4jLoggerBase.info("Logging connection was sucessfully closed.");
      log4jLoggerBase.info(getInterfaceName() + " Complete.");
    }
    catch (Exception e)
    {
      log4jLoggerBase.error(e);
    }
    finally
    {
      InterfaceThreadManager.removeInterfaceThread();
      if (this.isDeleteLogFile())
      {
        this.getInterfaceInfoBean().getLogFile().delete();
      }
    }
  }

  public String getProperty(String key)
  {
    return PropertiesServlet.getProperty(key);
  }

  public void setInterfaceName(String interfaceName)
  {
    this.interfaceName = interfaceName;
  }

  public String getInterfaceName()
  {
    return interfaceName;
  }

  public void setFromConn(List<ConnectionBean> fromConn)
  {
    this.fromConn = fromConn;
  }

  public List<ConnectionBean> getFromConn()
  {
    return fromConn;
  }

  public void setToConn(List<ConnectionBean> toConn)
  {
    this.toConn = toConn;
  }

  public List<ConnectionBean> getToConn()
  {
    return toConn;
  }

  public void setDbEnvironment(String dbEnvironment)
  {
    this.dbEnvironment = dbEnvironment;
  }

  public String getDbEnvironment()
  {
    return dbEnvironment;
  }

  public void setEmailMessages(List<String> emailMessages)
  {
    this.emailMessages = emailMessages;
  }

  public List<String> getEmailMessages()
  {
    return emailMessages;
  }

  public void setNotificationEmail(String notificationEmail)
  {
    this.notificationEmail = notificationEmail;
  }

  public String getNotificationEmail()
  {
    return notificationEmail;
  }

  public String getProcessId()
  {
    return processId;
  }

  public void setProcessId(String processId)
  {
    this.processId = processId;
  }

  public void setInterfaceId(String interfaceId)
  {
    this.interfaceId = interfaceId;
  }

  public String getInterfaceId()
  {
    return interfaceId;
  }

  public void setStartDate(Date startDate)
  {
    this.startDate = startDate;
  }

  public Date getStartDate()
  {
    return startDate;
  }

  private void setContext(JobExecutionContext context)
  {
    this.context = context;
  }

  protected JobExecutionContext getContext()
  {
    return context;
  }

  public void setDeleteLogFile(boolean deleteLogFile)
  {
    this.deleteLogFile = deleteLogFile;
  }

  public boolean isDeleteLogFile()
  {
    return deleteLogFile;
  }

  public void setInterfaceInfoBean(InterfaceInfoBean interfaceInfoBean)
  {
    this.interfaceInfoBean = interfaceInfoBean;
  }

  public InterfaceInfoBean getInterfaceInfoBean()
  {
    return interfaceInfoBean;
  }
}
