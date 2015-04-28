package com.valspar.interfaces.common.beans;

import com.valspar.interfaces.common.*;
import com.valspar.interfaces.common.utils.DataAccessBean;
import java.sql.*;
import org.apache.commons.lang3.StringUtils;
import org.apache.log4j.Logger;

public class ConnectionBean
{
  private static Logger log4jLogger = Logger.getLogger(ConnectionBean.class);

  private Connection connection;
  private String version;
  private String userName;
  private String logUser;
  private String orgId;
  private String responsibilityId;
  private String applicationId;
  private String dbType;
  private String password;
  private String location;

  public Connection getDuplicateConnection()
  {
    Connection conn = null;
    try
    {
      conn = DataAccessBean.getConnection(getDbName(), getDbSid(), getUserName(), getDbType(), getPassword());
    }
    catch (Exception e)
    {
      log4jLogger.error(e);
    }
    return conn;
  }

  public Connection getConnection()
  {
    return connection;
  }

  public void setConnection(Connection connection)
  {
    this.connection = connection;
  }

  public boolean isWercs()
  {
    return StringUtils.equalsIgnoreCase(version, "WERCS");
  }

  public boolean isOpm11i()
  {
    return StringUtils.equalsIgnoreCase(version, "OPM11i");
  }

  public String getDbName()
  {
    String db = Constants.EMPTY_STRING;

    try
    {
      String url = getConnection().getMetaData().getURL();

      if (getDbType().equalsIgnoreCase("ORACLE"))
      {
        db = url.substring(url.length() - 4, url.length());
      }
      else if (getDbType().equalsIgnoreCase("MICROSOFT"))
      {
        db = StringUtils.substringBetween(url, "//", ":");
      }
      else if (getDbType().equalsIgnoreCase("ANALYTICS"))
      {
        db = StringUtils.substringBetween(url, "//", ":");
      }
    }
    catch (Exception e)
    {
      log4jLogger.error(e);
    }
    return db;
  }

  public String getDbSid()
  {
    String db = Constants.EMPTY_STRING;

    try
    {
      String url = getConnection().getMetaData().getURL();

      if (getDbType().equalsIgnoreCase("ORACLE"))
      {
        // not used for Oracle connections
      }
      else if (getDbType().equalsIgnoreCase("MICROSOFT"))
      {
        db = StringUtils.substringAfterLast(url, "/");
      }
      else if (getDbType().equalsIgnoreCase("ANALYTICS"))
      {
        // not used for Analytics connections
      }
    }
    catch (Exception e)
    {
      log4jLogger.error(e);
    }
    return db;    
  }

  public boolean isNa()
  {
    return StringUtils.startsWith(getDbName(), "NA");
  }

  public boolean isEmeai()
  {
    return StringUtils.startsWith(getDbName(), "IN");
  }
  
  public boolean isAsiaPac()
  {
    return StringUtils.startsWith(getDbName(), "PA");
  }

  public String getAnalyticsOracle11iSource()
  {
    if (this.isNa())
    {
      return "NorthAmerica";
    }
    else if (this.isEmeai())
    {
      return "International";
    }
    else if (this.isAsiaPac())
    {
      return "International";
    }
    return null;
  }

  public String getAnalyticsOracle11iSourceDatabase()
  {
    if (this.isNa())
    {
      return "NAPR";
    }
    else if (this.isEmeai())
    {
      return "INPR";
    }
    else if (this.isAsiaPac())
    {
      return "PAPR";
    }
    return null;
  }

  public void setVersion(String version)
  {
    this.version = version;
  }

  public String getVersion()
  {
    return version;
  }

  public void setLogUser(String logUser)
  {
    this.logUser = logUser;
  }

  public String getLogUser()
  {
    return logUser;
  }

  public void setOrgId(String orgId)
  {
    this.orgId = orgId;
  }

  public String getOrgId()
  {
    return orgId;
  }

  public String getResponsibilityId()
  {
    return responsibilityId;
  }

  public void setResponsibilityId(String newResponsibilityId)
  {
    responsibilityId = newResponsibilityId;
  }

  public String getApplicationId()
  {
    return applicationId;
  }

  public void setApplicationId(String newApplicationId)
  {
    applicationId = newApplicationId;
  }

  public String getDbType()
  {
    return dbType;
  }

  public void setDbType(String dbType)
  {
    this.dbType = dbType;
  }

  public String getPassword()
  {
    return password;
  }

  public void setPassword(String password)
  {
    this.password = password;
  }

  public String getLocation()
  {
    return location;
  }

  public void setLocation(String location)
  {
    this.location = location;
  }

  public void setUserName(String userName)
  {
    this.userName = userName;
  }

  public String getUserName()
  {
    return userName;
  }
}
