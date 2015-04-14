package com.valspar.interfaces.common.beans;

import com.sforce.soap.enterprise.Connector;
import com.sforce.soap.enterprise.EnterpriseConnection;
import com.sforce.ws.ConnectorConfig;
import com.valspar.interfaces.common.servlets.PropertiesServlet;
import com.valspar.interfaces.common.utils.JDBCUtil;
import de.simplicit.vjdbc.VirtualDriver;
import java.sql.*;
import java.util.Properties;
import javax.naming.*;
import javax.sql.DataSource;
import oracle.jdbc.*;
import org.apache.commons.lang3.StringUtils;
import org.apache.log4j.Logger;

public final class ConnectionAccessBean
{
  private static boolean oracleDriverRegistered;
  private static boolean sqlServerDriverRegistered;
  private static boolean analyticsDriverRegistered;
  private static Logger log4jLogger = Logger.getLogger(ConnectionAccessBean.class);

  private ConnectionAccessBean()
  {
  }

  public static Connection getConnection(com.valspar.interfaces.common.enums.DataSource ds)
  {
    return getConnection(ds, true);
  }

  public static Connection getConnection(com.valspar.interfaces.common.enums.DataSource ds, boolean logExceptions)
  {
    Connection conn = null;

    switch (ds.getDbConnectionType())
    {
      case ANALYTICS:
        conn = getAnalyticsConnection(logExceptions);
        break;

      case ORACLE:
        conn = getOracleConnection(ds, logExceptions);
        break;

      case SQL_SERVER:
        conn = getSqlServerConnection(ds, logExceptions);
        break;

      default:
        log4jLogger.error("Data Source " + ds.getDataSourceLabel() + " is using an unsupported connection type!");
    }

    return conn;
  }

  public static Connection findConnection(String senderId)
  {
    Connection conn = null;
    if (org.apache.commons.lang.StringUtils.equalsIgnoreCase(senderId, "clx.apds.dunsnumber"))
    {
      conn = getOracleConnection(com.valspar.interfaces.common.enums.DataSource.ASIAPAC, true);
    }
    else if (org.apache.commons.lang.StringUtils.equalsIgnoreCase(senderId, "clx.inds.dunsnumber"))
    {
      conn = getOracleConnection(com.valspar.interfaces.common.enums.DataSource.EMEAI, true);       
    }
    else if (org.apache.commons.lang.StringUtils.equalsIgnoreCase(senderId, "clx.nads.dunsnumber"))
    {
      conn = getOracleConnection(com.valspar.interfaces.common.enums.DataSource.NORTHAMERICAN, true);
    }
    else
    {
      log4jLogger.error("ERROR - No ERP connection found for Sender Id: " + senderId);
    }

    return conn;
  }

  private static Connection getAnalyticsConnection(boolean logExceptions)
  {
    Connection outConn = null;

    try
    {
      Context initialContext = new InitialContext();
      if (!analyticsDriverRegistered)
      {
        registerAnalyticsDriver();
      }
      DataSource dataSource = (DataSource) initialContext.lookup("jdbc/analytics");
      outConn = dataSource.getConnection();
    }
    catch (Exception e)
    {
      if (logExceptions)
      {
        log4jLogger.error(e);
      }
      JDBCUtil.close(outConn);
    }
    return outConn;
  }

  private static OracleConnection getOracleConnection(com.valspar.interfaces.common.enums.DataSource ds, boolean logExceptions)
  {
    OracleConnection outConn = null;

    try
    {
      Context initialContext = new InitialContext();
      if (!oracleDriverRegistered)
      {
        registerOracleDriver();
      }
      DataSource dataSource = (DataSource) initialContext.lookup(ds.getWebLogicDataSource());
      outConn = (OracleConnection) dataSource.getConnection();
      outConn.setAutoCommit(true);
      destroyPackageState(outConn);
    }
    catch (Exception e)
    {
      if (logExceptions)
      {
        log4jLogger.error("Requested DataSource: " + ds, e);
      }
      JDBCUtil.close(outConn);
    }
    return outConn;
  }

  private static Connection getSqlServerConnection(com.valspar.interfaces.common.enums.DataSource ds, boolean logExceptions)
  {
    Connection outConn = null;

    try
    {
      Context initialContext = new InitialContext();
      if (!sqlServerDriverRegistered)
      {
        registerSqlServerDriver();
      }
      DataSource dataSource = (DataSource) initialContext.lookup(ds.getWebLogicDataSource());
      outConn = dataSource.getConnection();
    }
    catch (Exception e)
    {
      if (logExceptions)
      {
        log4jLogger.error("Requested DataSource: " + ds, e);
      }
      JDBCUtil.close(outConn);
    }
    return outConn;
  }

  public static Connection getSasConnection()
  {
    Connection conn = null;

    try
    {
      Class.forName("de.simplicit.vjdbc.VirtualDriver").newInstance();
      String rmiHost = PropertiesServlet.getProperty("guardsmantechportalusers.sasServer");

      String fullUrl = "jdbc:vjdbc:rmi://" + rmiHost + "/VJdbc,sas";

      //conn = DriverManager.getConnection(fullUrl);
      conn = new VirtualDriver().connect(fullUrl, new Properties());
    }
    catch (Exception e)
    {
      log4jLogger.error(e);
    }

    return conn;
  }

  private static boolean registerOracleDriver()
  {
    try
    {
      DriverManager.registerDriver(new oracle.jdbc.OracleDriver());
    }
    catch (Exception e)
    {
      return false;
    }
    oracleDriverRegistered = true;
    return true;
  }

  private static boolean registerAnalyticsDriver()
  {
    try
    {
      DriverManager.registerDriver(new oracle.bi.jdbc.AnaJdbcDriver());
    }
    catch (Exception e)
    {
      return false;
    }
    analyticsDriverRegistered = true;
    return true;
  }

  public static void destroyPackageState(OracleConnection inConn)
  {
    OracleCallableStatement cstmt = null;
    try
    {
      String procedure = "{call dbms_session.reset_package}";
      cstmt = (OracleCallableStatement) inConn.prepareCall(procedure);
      cstmt.execute();
    }
    catch (Exception e)
    {
      log4jLogger.error(e);
    }
    finally
    {
      JDBCUtil.close(cstmt);
    }
  }

  private static boolean registerSqlServerDriver()
  {
    try
    {
      DriverManager.registerDriver(new net.sourceforge.jtds.jdbc.Driver());
    }
    catch (Exception e)
    {
      return false;
    }
    sqlServerDriverRegistered = true;
    return true;
  }

  public static EnterpriseConnection getSFDCConnection()
  {
    EnterpriseConnection conn = null;

    try
    {
      String endpoint = PropertiesServlet.getProperty("salesforceserver") + StringUtils.substringAfter(Connector.END_POINT, "salesforce.com");
      ConnectorConfig config = new ConnectorConfig();
      config.setAuthEndpoint(endpoint);
      config.setServiceEndpoint(endpoint);
      config.setUsername(PropertiesServlet.getProperty("salesforce_username"));
      config.setPassword(PropertiesServlet.getProperty("salesforce_password"));
      conn = Connector.newConnection(config);
    }
    catch (Exception e)
    {
      log4jLogger.error(e);
    }

    return conn;
  }
}
