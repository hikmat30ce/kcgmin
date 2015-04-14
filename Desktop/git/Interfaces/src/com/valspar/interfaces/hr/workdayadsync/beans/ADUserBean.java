package com.valspar.interfaces.hr.workdayadsync.beans;

import com.valspar.interfaces.common.enums.Domains;

public class ADUserBean
{
  private Domains domain;
  private String dn;
  private String employeeId;
  private String email;

  public ADUserBean()
  {
  }

  public void setDomain(Domains domain)
  {
    this.domain = domain;
  }

  public Domains getDomain()
  {
    return domain;
  }

  public void setDn(String dn)
  {
    this.dn = dn;
  }

  public String getDn()
  {
    return dn;
  }

  public void setEmployeeId(String employeeId)
  {
    this.employeeId = employeeId;
  }

  public String getEmployeeId()
  {
    return employeeId;
  }

  public void setEmail(String email)
  {
    this.email = email;
  }

  public String getEmail()
  {
    return email;
  }
}