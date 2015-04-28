package com.valspar.interfaces.hr.submitpayrollinput.beans;

import org.apache.commons.lang3.*;

public abstract class PayrollInputBean
{
  private String employeeId;
  private String earningCode;
  private String hours;
  protected String shift = "1";
  private String dollars;
  private String costCenterID;
  private String payrollResponseID;
  private String errorMessage;
  private boolean validFormat;
  private IntCurrentPayPeriodBean intCurrentPayPeriodBean;
  
  public PayrollInputBean()
  {
  }
  
  public abstract String getRunCategory();
  public abstract String getShift();

  public void setEmployeeId(String employeeId)
  {
    this.employeeId = employeeId;
  }

  public String getEmployeeId()
  {
    return StringUtils.leftPad(employeeId, 9, "0");
  }

  public void setEarningCode(String earningCode)
  {
    this.earningCode = earningCode;
  }

  public String getEarningCode()
  {
    return earningCode;
  }

  public void setHours(String hours)
  {
    this.hours = hours;
  }

  public String getHours()
  {
    return hours;
  }

  public void setShift(String shift)
  {
    this.shift = shift;
  }

  public void setDollars(String dollars)
  {
    this.dollars = dollars;
  }

  public String getDollars()
  {
    return dollars;
  }

  public void setCostCenterID(String costCenterID)
  {
    this.costCenterID = costCenterID;
  }

  public String getCostCenterID()
  {
    return StringUtils.trim(costCenterID);
  }

  public void setErrorMessage(String errorMessage)
  {
    this.errorMessage = errorMessage;
  }

  public String getErrorMessage()
  {
    return errorMessage;
  }

  public void setPayrollResponseID(String payrollResponseID)
  {
    this.payrollResponseID = payrollResponseID;
  }

  public String getPayrollResponseID()
  {
    return payrollResponseID;
  }

  public void setValidFormat(boolean validFormat)
  {
    this.validFormat = validFormat;
  }

  public boolean isValidFormat()
  {
    return validFormat;
  }

  public void setIntCurrentPayPeriodBean(IntCurrentPayPeriodBean intCurrentPayPeriodBean)
  {
    this.intCurrentPayPeriodBean = intCurrentPayPeriodBean;
  }

  public IntCurrentPayPeriodBean getIntCurrentPayPeriodBean()
  {
    return intCurrentPayPeriodBean;
  }
}
