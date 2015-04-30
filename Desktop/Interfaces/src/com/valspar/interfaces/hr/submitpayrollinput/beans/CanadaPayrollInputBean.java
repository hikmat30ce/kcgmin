package com.valspar.interfaces.hr.submitpayrollinput.beans;

import javax.xml.datatype.*;
import org.apache.commons.lang3.*;

public class CanadaPayrollInputBean extends PayrollInputBean
{
  public CanadaPayrollInputBean()
  {
  }
  
  public String getShift()
  {
    if (StringUtils.isEmpty(shift) && StringUtils.isNotEmpty(this.getDollars()))
    {
      return "R";
    }
    return shift;
  }
  
  public String getRunCategory()
  {
     return "003";
  }
 
}