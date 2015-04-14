package com.valspar.interfaces.hr.submitpayrollinput.beans;

import javax.xml.datatype.*;
import org.apache.commons.lang3.*;

public class USAPayrollInputBean extends PayrollInputBean
{
  public USAPayrollInputBean()
  {
  }
  
  public String getShift()
  {
    return StringUtils.defaultIfEmpty(shift, "1");
  }
  
  public String getRunCategory()
  {
      return "001";
  }
}