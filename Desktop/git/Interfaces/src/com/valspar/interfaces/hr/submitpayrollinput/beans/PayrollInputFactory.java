package com.valspar.interfaces.hr.submitpayrollinput.beans;

import org.apache.commons.lang3.*;

public class PayrollInputFactory
{
  public PayrollInputFactory()
  {
  }
  
  public static PayrollInputBean getPayrollInputBean(String fileName)
  {
    if (StringUtils.startsWithIgnoreCase(fileName, "Canada"))
    {
      return new CanadaPayrollInputBean();
    }
    else
    {
      return new USAPayrollInputBean();
    }
  }
}