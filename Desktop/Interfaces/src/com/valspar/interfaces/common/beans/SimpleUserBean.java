package com.valspar.interfaces.common.beans;

import java.io.Serializable;
import org.apache.commons.lang.StringUtils;

public class SimpleUserBean implements Comparable, Serializable
{
  private String userName;
  private String firstName;
  private String lastName;
  private String fullName;
  private String email;

  public SimpleUserBean()
  {
  }

  public boolean equals(Object obj)
  {
    if (obj instanceof SimpleUserBean)
    {
      SimpleUserBean s2 = (SimpleUserBean)obj;
      return StringUtils.equalsIgnoreCase(getFullName(), s2.getFullName());
    }
    else
    {
      return false;
    }
  }

  public int compareTo(Object o2)
  {
    SimpleUserBean s2 = (SimpleUserBean) o2;

    if (getFullNameLastFirst() != null && s2.getFullNameLastFirst() != null)
    {
      return getFullNameLastFirst().toUpperCase().compareTo(s2.getFullNameLastFirst().toUpperCase());
    }
    else if (getFullName() != null && s2.getFullName() != null)
    {
      return getFullName().toUpperCase().compareTo(s2.getFullName().toUpperCase());
    }
    else
    {
      return -1;
    }
  }

  public String getFullNameAndEmail()
  {
    return getFullName() + " (" + getEmail() + ")";
  }

  public void setFullName(String fullName)
  {
    this.fullName = fullName;
  }

  public String getFullName()
  {
    if (StringUtils.isEmpty(firstName) && StringUtils.isEmpty(lastName))
    {
      return fullName;
    }
    else
    {
      StringBuilder sb = new StringBuilder();

      if (StringUtils.isNotEmpty(firstName))
      {
        sb.append(firstName);
      }
      if (StringUtils.isNotEmpty(lastName))
      {
        if (sb.length() > 0)
        {
          sb.append(" ");
        }
        sb.append(lastName);
      }

      return sb.toString();
    }
  }

  public String getFullNameLastFirst()
  {
    StringBuilder sb = new StringBuilder();

    if (StringUtils.isNotEmpty(lastName))
    {
      sb.append(lastName);
    }
    if (StringUtils.isNotEmpty(firstName))
    {
      if (sb.length() > 0)
      {
        sb.append(", ");
      }
      sb.append(firstName);
    }

    if (sb.length() > 0)
    {
      return sb.toString();
    }
    else
    {
      return getFullName();
    }
  }

  public void setUserName(String userName)
  {
    this.userName = userName;
  }

  public String getUserName()
  {
    return userName;
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

  public void setEmail(String email)
  {
    this.email = email;
  }

  public String getEmail()
  {
    return email;
  }
}