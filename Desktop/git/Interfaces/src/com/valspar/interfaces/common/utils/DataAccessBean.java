package com.valspar.interfaces.common.utils;

import java.sql.*;
import java.util.*;
import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;

public class DataAccessBean
{
  private static Logger log4jLogger = Logger.getLogger(DataAccessBean.class);

  private static boolean oracleDriverRegistered;
  private static boolean microsoftDriverRegistered;
  private static boolean analyticsDriverRegistered;

  private DataAccessBean()
  {
  }

  public static Connection getConnection(String dbName, String sid, String user, String dbType, String password)
  {
    Connection outConn = null;

    try
    {
      if (StringUtils.isEmpty(dbName))
      {
        throw new Exception("Database name is null");
      }
      if (StringUtils.isEmpty(user))
      {
        throw new Exception("User is null");
      }

      dbName = StringUtils.upperCase(dbName);
      user = StringUtils.upperCase(user);        

      if (StringUtils.equalsIgnoreCase(dbType, "MICROSOFT"))
      {
        if (!microsoftDriverRegistered)
        {
          registerDriver(dbType);
        }

        StringBuilder sb = new StringBuilder();
        sb.append("jdbc:jtds:sqlserver://");
        sb.append(dbName);
        sb.append(":1433/");
        sb.append(sid);
        log4jLogger.info("Getting connection to SQL Server database " + dbName + "/" + sid + " as " + user);
        outConn = DriverManager.getConnection(sb.toString(), user, password);
      }
      else if (StringUtils.equalsIgnoreCase(dbType, "ANALYTICS"))
      {
        if (!analyticsDriverRegistered)
        {
          registerDriver(dbType);
        }

        StringBuilder sb = new StringBuilder();
        sb.append("jdbc:oraclebi://");
        sb.append(dbName);
        sb.append(":9703/RpcClientExpirationTime=86400;");

        log4jLogger.info("Getting connection to Analytics database " + dbName + " as " + user);
        outConn = DriverManager.getConnection(sb.toString(), user, password);
      }
      else if (StringUtils.equalsIgnoreCase(dbType, "ORACLE"))
      {
        if (!oracleDriverRegistered)
        {
          registerDriver(dbType);
        }

        Connection conn = null;
        PreparedStatement pst = null;
        ResultSet rs = null;

        try
        {
          conn = DriverManager.getConnection("jdbc:oracle:thin:@misp-db.valspar.local:1532:MISP", "custom", "custom");
          pst = conn.prepareStatement("SELECT GETDBINFO(?,?,'',5) FROM DUAL");
          pst.setString(1, dbName);
          pst.setString(2, user);

          rs = pst.executeQuery();
          if (rs.next())
          {
            log4jLogger.info("Getting connection to Oracle database " + dbName + " as " + user);
            outConn = DriverManager.getConnection(rs.getString(1));
          }
          else
          {
            log4jLogger.info("Error getting connection for Oracle database " + dbName + " as " + user + ".  Please contact the DBA team to setup this database/user combination in the SQLPlus driver");
          }
        }
        catch (Exception e)
        {
          log4jLogger.error(e);
        }
        finally
        {
          JDBCUtil.close(pst, rs);
          JDBCUtil.close(conn);
        }
      }
      else
      {
        throw new Exception("Unknown database type [" + dbType + "]");
      }

      if (outConn != null)
      {
        outConn.setAutoCommit(true);
      }
    }
    catch (Exception e)
    {
      log4jLogger.error(e);
    }

    return outConn;
  }

  private static boolean registerDriver(String dbType)
  {
    try
    {
      if (StringUtils.equalsIgnoreCase(dbType, "ORACLE"))
      {
        DriverManager.registerDriver(new oracle.jdbc.driver.OracleDriver());
        oracleDriverRegistered = true;
        log4jLogger.info("Oracle Driver Registered");
      }
      else if (StringUtils.equalsIgnoreCase(dbType, "MICROSOFT"))
      {
        //DriverManager.registerDriver(new com.microsoft.jdbc.sqlserver.SQLServerDriver());
        DriverManager.registerDriver(new net.sourceforge.jtds.jdbc.Driver());
        microsoftDriverRegistered = true;
        log4jLogger.info("Microsoft Driver Registered");
      }
      else if (StringUtils.equalsIgnoreCase(dbType, "ANALYTICS"))
      {
        DriverManager.registerDriver(new oracle.bi.jdbc.AnaJdbcDriver());
        analyticsDriverRegistered = true;
        log4jLogger.info("Analytics Driver Registered");
      }
      else
      {
        throw new Exception("Unknown database type [" + dbType + "]");
      }
      return true;
    }
    catch (Exception e)
    {
      log4jLogger.error(e);
      return false;
    }
  }

  public static void closeConnectionList(List<Connection> connectionList)
  {
    Iterator<Connection> i = connectionList.iterator();
    while (i.hasNext())
    {
      try
      {
        Connection conn = i.next();
        JDBCUtil.close(conn);
        i.remove();
      }
      catch (Exception e)
      {
        log4jLogger.error(e);
      }
    }
  }
}
