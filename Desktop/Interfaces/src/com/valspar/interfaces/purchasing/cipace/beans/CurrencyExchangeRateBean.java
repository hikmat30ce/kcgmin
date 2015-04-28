package com.valspar.interfaces.purchasing.cipace.beans;

import java.math.BigDecimal;

public class CurrencyExchangeRateBean
{
  private String fromCurrencyCode;
  private String fromCurrencyGuid;
  private String toCurrencyCode;
  private String toCurrencyGuid;
  private BigDecimal rate;
  private String periodName;
  private String note;

  public void setFromCurrencyCode(String fromCurrencyCode)
  {
    this.fromCurrencyCode = fromCurrencyCode;
  }

  public String getFromCurrencyCode()
  {
    return fromCurrencyCode;
  }

  public void setToCurrencyCode(String toCurrencyCode)
  {
    this.toCurrencyCode = toCurrencyCode;
  }

  public String getToCurrencyCode()
  {
    return toCurrencyCode;
  }

  public void setRate(BigDecimal rate)
  {
    this.rate = rate;
  }

  public BigDecimal getRate()
  {
    return rate;
  }

  public void setPeriodName(String periodName)
  {
    this.periodName = periodName;
  }

  public String getPeriodName()
  {
    return periodName;
  }

  public void setFromCurrencyGuid(String fromCurrencyGuid)
  {
    this.fromCurrencyGuid = fromCurrencyGuid;
  }

  public String getFromCurrencyGuid()
  {
    return fromCurrencyGuid;
  }

  public void setToCurrencyGuid(String toCurrencyGuid)
  {
    this.toCurrencyGuid = toCurrencyGuid;
  }

  public String getToCurrencyGuid()
  {
    return toCurrencyGuid;
  }

  public void setNote(String note)
  {
    this.note = note;
  }

  public String getNote()
  {
    return note;
  }
}
