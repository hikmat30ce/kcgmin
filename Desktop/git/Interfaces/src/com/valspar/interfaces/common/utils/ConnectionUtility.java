package com.valspar.interfaces.common.utils;

import com.valspar.interfaces.common.beans.*;
import com.valspar.interfaces.common.enums.DataSource;
import java.sql.Connection;
import java.util.*;
import org.apache.commons.lang3.StringUtils;

public final class ConnectionUtility
{
  private ConnectionUtility()
  {
  }

  public static String buildDatabaseName(Connection conn)
  {
    String url = null;
    String returnValue = "";

    try
    {
      url = conn.getMetaData().getURL();

      if (url != null)
      {
        url = url.toUpperCase();
        returnValue = StringUtils.substringAfterLast(url, "/");

        if (StringUtils.isNotEmpty(returnValue))
        {
          returnValue = StringUtils.substringBefore(returnValue, ":");
        }
        else
        {
          returnValue = StringUtils.substringAfterLast(url, ":");
        }

        if (StringUtils.containsIgnoreCase(url, "sqlserver"))
        {
          String host = StringUtils.substringBetween(url, "://", ":");

          if (StringUtils.isNotEmpty(host) && !StringUtils.equalsIgnoreCase(host, returnValue))
          {
            returnValue = returnValue + " (" + host + ")";
          }
        }
      }
    }
    catch (Exception ex)
    {
      ;
    }

    if (StringUtils.isEmpty(returnValue))
    {
      returnValue = "-";
    }

    return returnValue;
  }

  public static List<DatabaseStatusBean> buildAllDBStatusBeans()
  {
    List<DatabaseStatusBean> dbStatusBeans = new ArrayList<DatabaseStatusBean>();

    for (DataSource datasource: DataSource.values())
    {
      dbStatusBeans.add(buildDBStatusBean(datasource));
    }

    return dbStatusBeans;
  }

  public static DatabaseStatusBean buildDBStatusBean(DataSource ds)
  {
    DatabaseStatusBean bean = new DatabaseStatusBean();
    bean.setDataSourceName(ds.getDataSourceLabel());

    Connection conn = null;

    try
    {
      if (ds == DataSource.ANALYTICS)
      {
        bean.setDatabaseName("(not checked)");
      }
      else
      {
        conn = ConnectionAccessBean.getConnection(ds, false);

        if (conn == null)
        {
          bean.setDatabaseName("-");
          bean.setDatabaseStatus(false);
        }
        else
        {
          bean.setDatabaseName(buildDatabaseName(conn));
        }

        if (StringUtils.isNotEmpty(bean.getDatabaseName()) && !StringUtils.equals(bean.getDatabaseName(), "-"))
        {
          bean.setDatabaseStatus(true);
        }
      }
    }
    catch (Exception e)
    {
      bean.setDatabaseName("-");
      bean.setDatabaseStatus(false);
    }
    finally
    {
      JDBCUtil.close(conn);
    }

    return bean;
  }
}
