package com.valspar.interfaces.guardsman.techportalusersync.beans;

public class TechUserBean
{
  private String userName;
  private String password;
  private String firstName;
  private String lastName;
  private String fullName;
  private String email;
  private boolean expired;

  public void setUserName(String userName)
  {
    this.userName = userName;
  }

  public String getUserName()
  {
    return userName;
  }

  public void setPassword(String password)
  {
    this.password = password;
  }

  public String getPassword()
  {
    return password;
  }

  public void setFirstName(String firstName)
  {
    this.firstName = firstName;
  }

  public String getFirstName()
  {
    return firstName;
  }

  public void setLastName(String lastName)
  {
    this.lastName = lastName;
  }

  public String getLastName()
  {
    return lastName;
  }

  public void setFullName(String fullName)
  {
    this.fullName = fullName;
  }

  public String getFullName()
  {
    return fullName;
  }

  public void setExpired(boolean expired)
  {
    this.expired = expired;
  }

  public boolean isExpired()
  {
    return expired;
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
