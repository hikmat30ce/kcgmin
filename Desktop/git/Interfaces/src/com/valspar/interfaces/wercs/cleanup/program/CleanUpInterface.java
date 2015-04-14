package com.valspar.interfaces.wercs.cleanup.program;

import com.valspar.interfaces.common.BaseInterface;
import com.valspar.interfaces.common.beans.ConnectionAccessBean;
import com.valspar.interfaces.common.enums.DataSource;
import com.valspar.interfaces.common.utils.JDBCUtil;
import java.io.File;
import java.sql.*;
import java.text.*;
import oracle.jdbc.*;
import org.apache.log4j.Logger;

public class CleanUpInterface extends BaseInterface
{
  private static Logger log4jLogger = Logger.getLogger(CleanUpInterface.class);

  public CleanUpInterface()
  {
  }

  public void execute()
  {
    try
    {
      cleanUpWercsDatabase();
      cleanUpQuartzLogs();
      cleanUpValAppsRetainedReports();
      cleanUpTscaTabel();
    }
    catch (Exception e)
    {
      log4jLogger.error(e);
    }
  }

  public void cleanUpWercsDatabase()
  {
    log4jLogger.info("Running WERCS_CLEANUP_INTERFACE_PROC in " + DataSource.WERCS.getDataSourceLabel() + "...");
    OracleConnection conn = null;
    CallableStatement cstmt = null;
    try
    {
      conn = (OracleConnection) ConnectionAccessBean.getConnection(DataSource.WERCS);
      cstmt = conn.prepareCall("{call WERCS_CLEANUP_INTERFACE_PROC(?)}");
      cstmt.registerOutParameter(1, Types.VARCHAR);
      cstmt.execute();
      if (cstmt.getString(1) != null)
        log4jLogger.info("Error Running WERCS_CLEANUP_INTERFACE_PROC in " + DataSource.WERCS.getDataSourceLabel() + ": " + cstmt.getString(1));
      else
        log4jLogger.info("Done Running WERCS_CLEANUP_INTERFACE_PROC in " + DataSource.WERCS.getDataSourceLabel());
    }
    catch (Exception e)
    {
      log4jLogger.error(e);
    }
    finally
    {
      JDBCUtil.close(cstmt);
      JDBCUtil.close(conn);
    }
  }

  public void cleanUpQuartzLogs()
  {
    log4jLogger.info("Cleaning up Quartz logs over 14 days old " + DataSource.MIDDLEWARE.getDataSourceLabel() + "...");
    OracleConnection conn = null;
    Statement stmt1 = null;
    Statement stmt2 = null;
    try
    {
      conn = (OracleConnection) ConnectionAccessBean.getConnection(DataSource.MIDDLEWARE);
      stmt1 = conn.createStatement();
      stmt1.executeQuery("DELETE FROM quartz.vca_schedule_process_err WHERE DATE_LOGGED < SYSDATE - 14");
      log4jLogger.info("Done deleting from vca_schedule_process_err in " + DataSource.MIDDLEWARE.getDataSourceLabel());

      stmt2 = conn.createStatement();
      stmt2.executeQuery("DELETE FROM quartz.vca_schedule_process WHERE START_DATE < SYSDATE - 14");
      log4jLogger.info("Done deleting from vca_schedule_process in " + DataSource.MIDDLEWARE.getDataSourceLabel());
    }
    catch (Exception e)
    {
      log4jLogger.error(e);
    }
    finally
    {
      JDBCUtil.close(stmt1);
      JDBCUtil.close(stmt2);
      JDBCUtil.close(conn);
    }
  }

  public void cleanUpValAppsRetainedReports()
  {
    log4jLogger.info("Cleaning up ValApps retained reports...");
    OracleConnection conn = null;
    OraclePreparedStatement pst = null;
    OracleResultSet rs = null;

    try
    {
      StringBuilder sb = new StringBuilder();
      sb.append("select expire_date ");
      sb.append("from valapps.audit_trail ");
      sb.append("where report_path = :report_path ");

      conn = (OracleConnection) ConnectionAccessBean.getConnection(DataSource.MIDDLEWARE);
      pst = (OraclePreparedStatement) conn.prepareStatement(sb.toString());

      File retainFolder = new File("/data/soa_weblogic_11g/valappsrf/reports/retain");

      if (retainFolder == null || !retainFolder.exists())
      {
        log4jLogger.error("Could not find ValApps reports retain folder!  Exiting.");
        return;
      }

      java.util.Date now = new java.util.Date();
      DateFormat df = new SimpleDateFormat("dd-MMM-yyyy");

      for (File file: retainFolder.listFiles())
      {
        pst.clearParameters();
        pst.setStringAtName("report_path", file.getPath());

        rs = (OracleResultSet)pst.executeQuery();

        if (!rs.next())
        {
          log4jLogger.error("No audit trail record found for " + file.getPath() + "!");
        }
        else
        {
          java.util.Date expireDate = rs.getTimestamp("expire_date");
          if (now.compareTo(expireDate) > 0)
          {
            log4jLogger.info("Deleting " + file.getPath() + ", expired on " + df.format(expireDate));
            file.delete();
          }
          else
          {
            log4jLogger.info("Found " + file.getPath() + ", but it's OK, doesn't expire until " + df.format(expireDate));
          }
        }
        
        JDBCUtil.close(rs);
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
  
  public void cleanUpTscaTabel()
  {
    log4jLogger.info("Cleaning up va_tsca_dsl table over 14 days old " + DataSource.WERCS.getDataSourceLabel() + "...");
    OracleConnection conn = null;
    Statement stmt1 = null;
    try
    {
      conn = (OracleConnection) ConnectionAccessBean.getConnection(DataSource.WERCS);
      stmt1 = conn.createStatement();
      stmt1.executeQuery("DELETE FROM wercs.va_tsca_dsl WHERE DATE_REQUESTED < SYSDATE - 14");
      log4jLogger.info("Done deleting from va_tsca_dsl. ");
    }
    catch (Exception e)
    {
      log4jLogger.error(e);
    }
    finally
    {
      JDBCUtil.close(stmt1);
      JDBCUtil.close(conn);
    }
  }
}
