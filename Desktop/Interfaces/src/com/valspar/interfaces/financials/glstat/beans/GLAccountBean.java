package com.valspar.interfaces.financials.glstat.beans;

public class GLAccountBean
{
  private String glAccount;
  private String folderName;
  private String columnName;
  
  public GLAccountBean()
  {
  }

  public void setGlAccount(String glAccount)
  {
    this.glAccount = glAccount;
  }

  public String getGlAccount()
  {
    return glAccount;
  }

  public void setFolderName(String folderName)
  {
    this.folderName = folderName;
  }

  public String getFolderName()
  {
    return folderName;
  }

  public void setColumnName(String columnName)
  {
    this.columnName = columnName;
  }

  public String getColumnName()
  {
    return columnName;
  }
}
