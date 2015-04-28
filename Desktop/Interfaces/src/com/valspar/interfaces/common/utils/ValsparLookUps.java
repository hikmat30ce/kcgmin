package com.valspar.interfaces.common.utils;

import com.valspar.interfaces.common.beans.ConnectionAccessBean;
import com.valspar.interfaces.common.enums.DataSource;
import java.sql.*;
import java.util.*;
import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;

public final class ValsparLookUps
{
  private static Logger log4jLogger = Logger.getLogger(ValsparLookUps.class);

  private ValsparLookUps()
  {
  }

  public static String queryForSingleValue(DataSource ds, String query, String... parameters)
  {
    Connection conn = ConnectionAccessBean.getConnection(ds);

    try
    {
      return queryForSingleValueLeaveConnectionOpen(conn, query, parameters);
    }
    finally
    {
      JDBCUtil.close(conn);
    }

  }
  
  public static String queryForSingleValue(Connection conn, String query, String... parameters)
  {
    try
    {
      return queryForSingleValueLeaveConnectionOpen(conn, query, parameters);
    }
    finally
    {
      JDBCUtil.close(conn);
    }
  }

  public static String queryForSingleValueLeaveConnectionOpen(Connection conn, String query, String... parameters)
  {
    String value = null;
    PreparedStatement pst = null;
    ResultSet rs = null;

    try
    {
      pst = conn.prepareStatement(query);

      int i=1;
      for (String parameter : parameters)
      {
        pst.setString(i, parameter);
        i++;
      }

      rs = pst.executeQuery();
      if (rs.next())
      {
        value = rs.getString(1);
      }
    }
    catch (Exception e)
    {
      log4jLogger.error("Query: " + query + "\nParameters: " + StringUtils.join(parameters, ","), e);
    }
    finally
    {
      JDBCUtil.close(pst, rs);
    }
    return value;
  }

  public static List<String> queryStringList(DataSource dataSource, String query, String... parameters)
  {
    Connection conn = ConnectionAccessBean.getConnection(dataSource);

    try
    {
      return queryStringListLeaveConnectionOpen(conn, query, parameters);
    }
    finally
    {
      JDBCUtil.close(conn);
    }
  }

  public static List<String> queryStringListLeaveConnectionOpen(Connection conn, String query, String... parameters)
  {
    PreparedStatement pst = null;
    ResultSet rs = null;

    List<String> ar = new ArrayList<String>();

    try
    {
      pst = conn.prepareStatement(query);

      int i = 1;
      for (String parameter: parameters)
      {
        pst.setString(i, parameter);
        i++;
      }

      rs = pst.executeQuery();

      while (rs.next())
      {
        ar.add(rs.getString(1));
      }
    }
    catch (Exception e)
    {
      log4jLogger.error(e);
    }
    finally
    {
      JDBCUtil.close(pst, rs);
    }

    return ar;
  }
}
