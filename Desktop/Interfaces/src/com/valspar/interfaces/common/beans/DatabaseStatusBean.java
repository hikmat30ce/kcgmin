package com.valspar.interfaces.common.beans;

public class DatabaseStatusBean
{
  private String dataSourceName;
  private String databaseName;
  private Boolean databaseStatus;

  public DatabaseStatusBean()
  {
  }

  public void setDatabaseName(String databaseName)
  {
    this.databaseName = databaseName;
  }

  public String getDatabaseName()
  {
    return databaseName;
  }

  public void setDataSourceName(String dataSourceName)
  {
    this.dataSourceName = dataSourceName;
  }

  public String getDataSourceName()
  {
    return dataSourceName;
  }

  public void setDatabaseStatus(Boolean databaseStatus)
  {
    this.databaseStatus = databaseStatus;
  }

  public Boolean getDatabaseStatus()
  {
    return databaseStatus;
  }

  public String getImageSource()
  {
    if (databaseStatus == null)
    {
      return "";
    }
    else if (databaseStatus)
    {
      return "arrow-up.gif";
    }
    else
    {
      return "arrow-down.gif";
    }
  }
}
