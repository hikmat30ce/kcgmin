package com.valspar.interfaces.common.api;

import com.valspar.workday.payroll.*;
import com.valspar.workday.payroll.types.*;
import java.net.*;
import javax.xml.namespace.*;

public class PayrollAPI extends BaseAPI
{
  private PayrollPort payrollPort;

  public SubmitPayrollInputResponseType submitPayroll(SubmitPayrollInputRequestType submitPayrollInputRequestType) throws Exception
  {
    return getPayrollPort().submitPayrollInput(submitPayrollInputRequestType);
  }

  public PayrollPort getPayrollPort() throws Exception
  {
    if(payrollPort == null)
    {  
      String wsdlURL = "https://" + this.getWorkdayServer() + "/ccx/service/"+this.getTenant()+"/Payroll/v19?wsdl";
      PayrollService payrollService = new PayrollService(new URL(wsdlURL), new QName("urn:com.workday/bsvc/Payroll", "PayrollService"));
      payrollPort = payrollService.getPayroll(getSecurityFeatures());
      bindCredentials(payrollPort,wsdlURL);
      return payrollPort;
    }
    else
    {
      return payrollPort;
    }
  }

  public void setPayrollPort(PayrollPort payrollPort)
  {
    this.payrollPort = payrollPort;
  }
}