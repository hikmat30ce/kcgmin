package com.valspar.interfaces.common.utils;

import java.sql.CallableStatement;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
import org.apache.log4j.Logger;

public class JDBCUtil
{
  private static Logger log4jLogger = Logger.getLogger(JDBCUtil.class);

  public JDBCUtil()
  {
  }

  public static void close(PreparedStatement preparedStatement, ResultSet resultSet)
  {
    try
    {
      if (preparedStatement != null)
      {
        preparedStatement.close();
      }
    }
    catch (Exception e)
    {
      log4jLogger.error(e);
    }

    try
    {
      if (resultSet != null)
      {
        resultSet.close();
      }
    }
    catch (Exception e)
    {
      log4jLogger.error(e);
    }

    finally
    {
      preparedStatement = null;
      resultSet = null;
    }
  }

  public static void close(Statement statement, ResultSet resultSet)
  {
    try
    {
      if (statement != null)
      {
        statement.close();
      }
    }
    catch (Exception e)
    {
      log4jLogger.error(e);
    }

    try
    {
      if (resultSet != null)
      {
        resultSet.close();
      }
    }
    catch (Exception e)
    {
      log4jLogger.error(e);
    }

    finally
    {
      statement = null;
      resultSet = null;
    }
  }

  public static void close(Statement statement)
  {
    try
    {
      if (statement != null)
      {
        statement.close();
      }
    }
    catch (Exception e)
    {
      log4jLogger.error(e);
    }
    finally
    {
      statement = null;
    }
  }

  public static void close(PreparedStatement preparedStatement)
  {
    try
    {
      if (preparedStatement != null)
      {
        preparedStatement.close();
      }
    }
    catch (Exception e)
    {
      log4jLogger.error(e);
    }
    finally
    {
      preparedStatement = null;
    }
  }

  public static void close(CallableStatement callableStatement)
  {
    try
    {
      if (callableStatement != null)
      {
        callableStatement.close();
      }
    }
    catch (Exception e)
    {
      log4jLogger.error(e);
    }
    finally
    {
      callableStatement = null;
    }
  }

  public static void close(ResultSet resultSet)
  {
    try
    {
      if (resultSet != null)
      {
        resultSet.close();
      }
    }
    catch (Exception e)
    {
      log4jLogger.error(e);
    }

    finally
    {
      resultSet = null;
    }
  }

  public static void close(Connection conn)
  {
    try
    {
      if (conn != null)
      {
        conn.close();
      }
    }
    catch (Exception e)
    {
      log4jLogger.error(e);
    }
    finally
    {
      conn = null;
    }
  }

  public static void rollBack(Connection connection)
  {
    try
    {
      connection.rollback();
    }
    catch (Exception e)
    {
      log4jLogger.error(e);
    }
  }

  public static void autoCommit(Connection connection)
  {
    try
    {
      connection.setAutoCommit(true);
    }
    catch (Exception e)
    {
      log4jLogger.error(e);
    }
  }

  public static oracle.sql.DATE getDATE(java.util.Date date)
  {
    if (date == null)
    {
      return null;
    }
    else
    {
      return new oracle.sql.DATE(new java.sql.Timestamp(date.getTime()));
    }
  }
}
