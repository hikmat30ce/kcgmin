package com.valspar.interfaces.guardsman.pos.beans;

import com.valspar.interfaces.common.beans.ConnectionAccessBean;
import com.valspar.interfaces.common.enums.DataSource;
import oracle.jdbc.OracleConnection;
import org.apache.log4j.Logger;

public class ConnectionBean
{
  static Logger log4jLogger = Logger.getLogger(ConnectionBean.class.getName());

  public ConnectionBean()
  {
  }

  public OracleConnection openConnection()
  {
    return (OracleConnection)ConnectionAccessBean.getConnection(DataSource.GUARDSMAN);
  }

  public void closeConnection(OracleConnection conn)
  {
    try
    {
      conn.close();
    }
    catch (Exception e)
    {
      log4jLogger.error("Error in Connectionbean.closeConnection(): " + e);
    }
  }
}
