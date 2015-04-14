package com.valspar.interfaces.purchasing.eoqproductionusage.beans;

public class UsageBean
{
  private String whseCode;
  private String itemNo;
  private String fiscalMonth;
  private String usageLB;
  private String usageGAL;
  private String usageKG;
  private String usageLTR;
  private String dataSource;

  public UsageBean()
  {

  }

  public void setWhseCode(String whseCode)
  {
    this.whseCode = whseCode;
  }

  public String getWhseCode()
  {
    return whseCode;
  }

  public void setFiscalMonth(String fiscalMonth)
  {
    this.fiscalMonth = fiscalMonth;
  }

  public String getFiscalMonth()
  {
    return fiscalMonth;
  }

  public void setUsageLB(String usageLB)
  {
    this.usageLB = usageLB;
  }

  public String getUsageLB()
  {
    return usageLB;
  }

  public void setUsageGAL(String usageGAL)
  {
    this.usageGAL = usageGAL;
  }

  public String getUsageGAL()
  {
    return usageGAL;
  }

  public void setUsageKG(String usageKG)
  {
    this.usageKG = usageKG;
  }

  public String getUsageKG()
  {
    return usageKG;
  }

  public void setUsageLTR(String usageLTR)
  {
    this.usageLTR = usageLTR;
  }

  public String getUsageLTR()
  {
    return usageLTR;
  }

  public void setDataSource(String dataSource)
  {
    this.dataSource = dataSource;
  }

  public String getDataSource()
  {
    return dataSource;
  }

  public void setItemNo(String itemNo)
  {
    this.itemNo = itemNo;
  }

  public String getItemNo()
  {
    return itemNo;
  }
}
