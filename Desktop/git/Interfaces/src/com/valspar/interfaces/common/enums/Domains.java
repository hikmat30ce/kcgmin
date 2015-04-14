package com.valspar.interfaces.common.enums;

public enum Domains
{
  CORPORATE("CORPORATE", "corporatedc", "389", "DC=corporate,DC=root,DC=corp"),
  EUROPE("EUROPE", "europedc", "389", "DC=europe,DC=root,DC=corp"),
  ASIA("ASIA", "asia-pacdc", "389", "DC=asia-pac,DC=root,DC=corp");

  private String displayName;
  private String server;
  private String port;
  private String searchPath;

  Domains(String displayName, String server, String port, String searchPath)
  {
    this.displayName = displayName;
    this.server = server;
    this.port = port;
    this.searchPath = searchPath;
  }

  public String getServer()
  {
    return this.server;
  }

  public String getPort()
  {
    return this.port;
  }

  public String getSearchPath()
  {
    return this.searchPath;
  }
  
  public String getLDAPConnectString()
  {
    StringBuilder sb = new StringBuilder();
    sb.append("ldap://");
    sb.append(getServer());
    sb.append(":");
    sb.append(getPort());    
    return sb.toString();
  }

  public String getDisplayName()
  {
    return displayName;
  }
}