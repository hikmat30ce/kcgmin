package com.valspar.interfaces.common.enums;

public enum DataSource
{
  ANALYTICS("AnalyticsDS", "jdbc/analytics", "Analytics", DbConnectionType.ANALYTICS, null, null, null, null, null, null, null, null),
  ASIAPAC("AsiaPacDS", "jdbc/asiapac", "Asia-Pac", DbConnectionType.ORACLE, "PA", "topa", "PAPR", null, null, "International", "1152", null),
  CIPACE("CipAceDS", "jdbc/cipace", "CIP Ace", DbConnectionType.SQL_SERVER, null, null, null, null, null, null, null, null),
  DEALERBRANDS("DealerBrandsDS", "jdbc/dealerbrands", "DealerBrands", DbConnectionType.ORACLE, null, null, null, null, null, null, null, null),
  DROMONTBG("DromontBGDS", "jdbc/dromontBG", "Dromont Bowling Green", DbConnectionType.SQL_SERVER, null, null, null, null, null, null, null, null),
  DROMONTKANK("DromontKankDS", "jdbc/dromontKank", "Dromont Kankakee", DbConnectionType.SQL_SERVER, null, null, null, null, null, null, null, null),
  DROMONTGARPITT("DromontGarPittDS", "jdbc/dromontGarPitt", "Dromont Gar-Pitt", DbConnectionType.SQL_SERVER, null, null, null, null, null, null, null, null),
  EMEAI("InternationalDS", "jdbc/in", "EMEAI", DbConnectionType.ORACLE, "IN", "toin", "INPR", null, "RPTOIN", "International", "1152", null),
  FORMULATION("FormulationDS", "jdbc/formulation", "Formulation", DbConnectionType.ORACLE, null, null, null, null, null, null, null, null),
  GUARDSMAN("GuardsmanRetailerDS", "jdbc/guardsmanretailer", "Guardsman Retailer", DbConnectionType.ORACLE, null, null, null, null, null, null, null, null),
  JDA("JdaDs", "jdbc/jda", "JDA", DbConnectionType.ORACLE, null, null, null, null, null, null, null, null),
  LOWESBIDS("LowesDS", "jdbc/lowesbi", "Lowes", DbConnectionType.ORACLE, null, null, null, null, null, null, null, null),
  MIDDLEWARE(null, "jdbc/middleware", "Middleware", DbConnectionType.ORACLE, null, null, null, "middleware.hibernate.cfg.xml", null, null, null, null),
  NORTHAMERICAN("NorthAmericanDS", "jdbc/na", "North American", DbConnectionType.ORACLE, "NA", "tona", "NAPR", "northamerican.hibernate.cfg.xml", "TRG", "North America", "1122", "050736453"),
  REGULATORY("RegulatoryDS", "jdbc/reg", "Regulatory", DbConnectionType.ORACLE, null, null, null, null, null, null, null, null),
  RMINDEX("RMIndexDS", "jdbc/rmi", "RMIndex", DbConnectionType.ORACLE, null, null, null, null, null, null, null, null),
  WERCS("WercsDS", "jdbc/wercs", "Wercs", DbConnectionType.ORACLE, null, null, null, "wercs.hibernate.cfg.xml", null, null, null, null);

  private String bIPublisherdataSource;
  private String webLogicDataSource;
  private String dataSourceLabel;
  private DbConnectionType dbConnectionType;
  private String instanceCodeOf11i;
  private String databaseLinkName;
  private String analyticsDataSource;
  private String hibernateConfigLocation;
  private String redPrairie11iDbLinkName;
  private String analyticsFiscalCalendar;
  private String logUser;
  private String clxSenderId;

  DataSource(String biPublisherdataSource, String webLogicDataSource, String dataSourceLabel, DbConnectionType dbConnectionType, String instanceCodeOf11i, String databaseLinkName, String analyticsDataSource, String hibernateConfigLocation, String redPrairie11iDbLinkName, String analyticsFiscalCalendar, String logUser, String clxSenderId)
  {
    this.bIPublisherdataSource = biPublisherdataSource;
    this.webLogicDataSource = webLogicDataSource;
    this.dataSourceLabel = dataSourceLabel;
    this.dbConnectionType = dbConnectionType;
    this.instanceCodeOf11i = instanceCodeOf11i;
    this.databaseLinkName = databaseLinkName;
    this.analyticsDataSource = analyticsDataSource;
    this.hibernateConfigLocation = hibernateConfigLocation;
    this.redPrairie11iDbLinkName = redPrairie11iDbLinkName;
    this.analyticsFiscalCalendar = analyticsFiscalCalendar;
    this.logUser = logUser;
    this.clxSenderId = clxSenderId;
  }

  public String getBIPublisherDataSource()
  {
    return bIPublisherdataSource;
  }

  public String getWebLogicDataSource()
  {
    return webLogicDataSource;
  }

  public String getDataSourceLabel()
  {
    return dataSourceLabel;
  }

  public String getInstanceCodeOf11i()
  {
    return instanceCodeOf11i;
  }

  public String getDatabaseLinkName()
  {
    return databaseLinkName;
  }

  public String getAnalyticsDataSource()
  {
    return analyticsDataSource;
  }

  public DbConnectionType getDbConnectionType()
  {
    return dbConnectionType;
  }

  public String getHibernateConfigLocation()
  {
    return hibernateConfigLocation;
  }

  public String getRedPrairie11iDbLinkName()
  {
    return redPrairie11iDbLinkName;
  }

  public String getAnalyticsFiscalCalendar()
  {
    return analyticsFiscalCalendar;
  }

  public String getLogUser()
  {
    return logUser;
  }

  public String getClxSenderId()
  {
    return clxSenderId;
  }
}

