package com.valspar.interfaces.hr.submitpayrollinput.parser;

import com.valspar.interfaces.hr.submitpayrollinput.beans.PayrollInputBean;
import java.util.*;
import org.apache.commons.lang3.*;

public class CanadaParser extends PayrollInputParser
{
  public CanadaParser()
  {
  }
  
  public void parse(String[] inputLine, PayrollInputBean payrollInputBean, List<PayrollInputBean> payrollInputBeanErrorList)
  {
    if (inputLine.length == 5)
    {
      payrollInputBean.setEmployeeId(inputLine[0]);
      payrollInputBean.setEarningCode(inputLine[1]);
      payrollInputBean.setHours(inputLine[2]);
      payrollInputBean.setShift(inputLine[3]);
      payrollInputBean.setDollars(inputLine[4]);
      payrollInputBean.setValidFormat(true);
    }
    else
    {
      payrollInputBeanErrorList.add(payrollInputBean);
      if(StringUtils.isEmpty(Arrays.toString(inputLine)))
      {
        payrollInputBean.setErrorMessage("Empty row");
      }
      else
      {
        payrollInputBean.setErrorMessage("Invalid format value: " + Arrays.toString(inputLine));
      }          
    }       
  }  

  public String getReferenceID()
  {
    return "003";
  }
}
