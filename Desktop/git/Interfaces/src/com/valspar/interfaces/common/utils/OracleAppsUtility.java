package com.valspar.interfaces.common.utils;

import java.sql.*;
import oracle.jdbc.*;
import org.apache.log4j.Logger;

public final class OracleAppsUtility
{
  private static Logger log4jLogger = Logger.getLogger(OracleAppsUtility.class);

  private OracleAppsUtility()
  {
  }

  public static void appsInitialize(OracleConnection conn, String userId, String responsibilityId, String applicationId) throws Exception
  {
    OracleCallableStatement cstmt = null;
    try
    {
      String procedure = "{call fnd_global.apps_initialize (:user_id, :responsibility_id, :application_id)}";
      cstmt = (OracleCallableStatement) conn.prepareCall(procedure);
      cstmt.setStringAtName("user_id", userId);
      cstmt.setStringAtName("responsibility_id", responsibilityId);
      cstmt.setStringAtName("application_id", applicationId);
      cstmt.execute();
    }
    finally
    {
      JDBCUtil.close(cstmt);
    }
  }

  public static String getLastErrorMessage(Connection conn)
  {
    return ValsparLookUps.queryForSingleValueLeaveConnectionOpen(conn, "select fnd_message.get from dual");
  }

  public static int submitConcurrentRequest(Connection conn, String application, String program, String description, String startTime, boolean subRequest, String... arguments) throws Exception
  {
    OracleCallableStatement cst = null;

    try
    {
      //  v_request_id := APPS.FND_REQUEST.SUBMIT_REQUEST('PO','POXPOPDOI','','',FALSE,'','STANDARD','','N','','INITIATE APPROVAL','',v_po_batch_id,'','');

      StringBuilder sb = new StringBuilder();
      sb.append("declare ");
      sb.append("   subrequest boolean := ");
      sb.append(subRequest ? "true;" : "false;");
      sb.append("begin ");
      sb.append("   ? := APPS.FND_REQUEST.SUBMIT_REQUEST(?,?,?,?,subrequest,");
      sb.append("?,?,?,?,?,?,?,?,?,?, ");
      sb.append("?,?,?,?,?,?,?,?,?,?, ");
      sb.append("?,?,?,?,?,?,?,?,?,?, ");
      sb.append("?,?,?,?,?,?,?,?,?,?, ");
      sb.append("?,?,?,?,?,?,?,?,?,?, ");
      sb.append("?,?,?,?,?,?,?,?,?,?, ");
      sb.append("?,?,?,?,?,?,?,?,?,?, ");
      sb.append("?,?,?,?,?,?,?,?,?,?, ");
      sb.append("?,?,?,?,?,?,?,?,?,?, ");
      sb.append("?,?,?,?,?,?,?,?,?,?); ");
      sb.append("end; ");

      cst = (OracleCallableStatement) conn.prepareCall(sb.toString());

      int i = 1;
      cst.registerOutParameter(i++, Types.INTEGER);
      cst.setString(i++, application);
      cst.setString(i++, program);
      cst.setString(i++, description);
      cst.setString(i++, startTime);

      for (String argument: arguments)
      {
        cst.setString(i++, argument);
      }

      while (i <= 105)
      {
        cst.setString(i++, String.valueOf((char)0));
      }

      cst.execute();
      conn.commit();

      return cst.getInt(1);
    }
    finally
    {
      JDBCUtil.close(cst);
    }
  }

  public static boolean waitForConcurrentRequest(Connection conn, int requestId, int pollIntervalSeconds) throws Exception
  {
    OracleCallableStatement cst = null;

    try
    {
      //  v_wait_flag := FND_CONCURRENT.WAIT_FOR_REQUEST(v_request_id,10,0,v_phase,v_status,v_dev_phase,v_dev_status,v_message);

      StringBuilder sb = new StringBuilder();
      sb.append("declare ");
      sb.append("   x boolean; ");
      sb.append("begin ");
      sb.append("   x := APPS.FND_CONCURRENT.WAIT_FOR_REQUEST(?,?,?,?,?,?,?,?); ");
      sb.append("end;");

      cst = (OracleCallableStatement) conn.prepareCall(sb.toString());
      int i = 1;
      //cst.registerOutParameter(i++, Types.BOOLEAN);
      cst.setInt(i++, requestId);
      cst.setInt(i++, pollIntervalSeconds); //polling interval
      cst.setInt(i++, 0); // max wait time
      cst.registerOutParameter(i++, Types.VARCHAR);
      cst.registerOutParameter(i++, Types.VARCHAR);
      cst.registerOutParameter(i++, Types.VARCHAR);
      cst.registerOutParameter(i++, Types.VARCHAR);
      cst.registerOutParameter(i++, Types.VARCHAR);
      cst.execute();

      //String phase = cst.getString(4);
      //String status = cst.getString(5);
      String devPhase = cst.getString(6);
      String devStatus = cst.getString(7);
      String message = cst.getString(8);

      if ("COMPLETE".equals(devPhase) && "NORMAL".equals(devStatus))
      {
        return true;
      }
      else
      {
        log4jLogger.error("Error in OracleAppsUtility.waitForConcurrentRequest, request " + requestId + " did not complete successfully.  Error = " + message);
        return false;
      }
    }
    finally
    {
      JDBCUtil.close(cst);
    }
  }
  
  public static String getERPUserId(Connection conn, String userName)
  {
    return ValsparLookUps.queryForSingleValueLeaveConnectionOpen(conn, "select user_id from fnd_user where upper(user_name) = upper(?)", userName);
  }
}
