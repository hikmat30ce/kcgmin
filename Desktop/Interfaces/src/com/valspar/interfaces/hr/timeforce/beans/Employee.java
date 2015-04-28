package com.valspar.interfaces.hr.timeforce.beans;

import com.valspar.interfaces.hr.timeforce.program.TimeForceInterface;
import java.text.SimpleDateFormat;
import java.util.*;
import org.apache.commons.lang3.*;
import org.apache.log4j.Logger;

public class Employee
{
  private String employeeID;
  private String legalFirstName;
  private String legalMiddleName;
  private String legalLastName;
  private String originalHireDate;
  private String hireDate;
  private String managerCategory;
  private String activeStatus;
  private String terminationDate;
  private String primaryWorkEmail;
  private String userName;
  private String payRateType;
  private String managerEmployeeID;
  private String managerUsername;
  private String timeAdministratorOrg;
  private String location;
  private String exemptStatus;
  private String timeZone;
  private boolean manager;
  private static List<String> locationList = new ArrayList<String>();
  private static Logger log4jLogger = Logger.getLogger(Employee.class);

  public Employee()
  {
  }

  public boolean isTimeForceLocation()
  {
    if (this.getLocationList().isEmpty())
    {
      // Phase 1 Locations
      this.getLocationList().add("USA- Birmingham");
      this.getLocationList().add("USA- Chicago");
      this.getLocationList().add("USA- Garland");
      this.getLocationList().add("USA- Hagerstown");
      this.getLocationList().add("USA- Matteson");
      this.getLocationList().add("USA- Miller Park");
      this.getLocationList().add("USA- Minneapolis Headquarters");
      this.getLocationList().add("USA- Minneapolis VAST Center");
      this.getLocationList().add("USA- Minneapolis Lab");
      this.getLocationList().add("USA- St Paul");
      this.getLocationList().add("USA- Wheeling");

      // Phase 2 Locations
      this.getLocationList().add("USA- Covington");
      this.getLocationList().add("USA- Rochester");
      this.getLocationList().add("USA- Bowling Green");
      this.getLocationList().add("USA- Charlotte");
      this.getLocationList().add("USA- High Point 1717");
      this.getLocationList().add("USA- High Point Brevard");
      this.getLocationList().add("USA- LosAngeles");
      this.getLocationList().add("USA- Sacramento");
      this.getLocationList().add("USA- Pittsburgh");
      this.getLocationList().add("USA- Sewickley");

      // Phase 3 Locations
      this.getLocationList().add("USA- Athens");
      this.getLocationList().add("USA- Kankakee");
      this.getLocationList().add("USA- Lebanon");
      this.getLocationList().add("USA- Louisville");
      this.getLocationList().add("USA- Marengo");
      this.getLocationList().add("USA- Medina");
      this.getLocationList().add("USA- Moline");
      this.getLocationList().add("USA- Mooresville");
      this.getLocationList().add("USA- Rockford");
      this.getLocationList().add("USA- Sanford");
      this.getLocationList().add("USA- Statesville");

// 1-22-2015 - Karen said these Locations might not be a part of phase 3
//      this.getLocationList().add("USA- Grand Rapids");
//      this.getLocationList().add("USA- Salem");

    }

    return this.getLocationList().contains(this.getLocation());
  }

  public void setEmployeeID(String employeeID)
  {
    this.employeeID = employeeID;
  }

  public String getEmployeeID()
  {
    return employeeID;
  }

  public void setLegalFirstName(String legalFirstName)
  {
    this.legalFirstName = legalFirstName;
  }

  public String getLegalFirstName()
  {
    return legalFirstName;
  }

  public void setLegalMiddleName(String legalMiddleName)
  {
    this.legalMiddleName = legalMiddleName;
  }

  public String getLegalMiddleName()
  {
    return legalMiddleName;
  }

  public void setLegalLastName(String legalLastName)
  {
    this.legalLastName = legalLastName;
  }

  public String getLegalLastName()
  {
    return legalLastName;
  }

  public void setOriginalHireDate(String originalHireDate)
  {
    this.originalHireDate = originalHireDate;
  }

  public String getOriginalHireDate()
  {
    return originalHireDate;
  }

  public void setHireDate(String hireDate)
  {
    this.hireDate = hireDate;
  }

  public String getHireDate()
  {
    return hireDate;
  }

  public void setManagerCategory(String managerCategory)
  {
    this.managerCategory = managerCategory;
  }

  public String getManagerCategory()
  {
    return managerCategory;
  }

  public void setActiveStatus(String activeStatus)
  {
    this.activeStatus = activeStatus;
  }

  public String getActiveStatus()
  {
    try
    {
      if (StringUtils.equalsIgnoreCase(activeStatus, "0"))
      {
        SimpleDateFormat dt = new SimpleDateFormat("yyyy-MM-dd");
        Date terminationDate = dt.parse(this.getTerminationDate());
        Date today = new Date();
        
        long diff = today.getTime() - terminationDate.getTime();

        if (diff / (1000 * 60 * 60 * 24) >= 15)
        {
          return "0";
        }
      }
    }
    catch (Exception e)
    {
      log4jLogger.error(e);
    }
    return "1";
  }

  public void setPrimaryWorkEmail(String primaryWorkEmail)
  {
    this.primaryWorkEmail = primaryWorkEmail;
  }

  public String getPrimaryWorkEmail()
  {
    return primaryWorkEmail;
  }

  public void setUserName(String userName)
  {
    this.userName = userName;
  }

  public String getUserName()
  {
    return userName;
  }

  public void setPayRateType(String payRateType)
  {
    this.payRateType = payRateType;
  }

  public String getPayRateType()
  {
    return payRateType;
  }

  public void setManagerEmployeeID(String managerEmployeeID)
  {
    this.managerEmployeeID = managerEmployeeID;
  }

  public String getManagerEmployeeID()
  {
    return managerEmployeeID;
  }

  public void setManagerUsername(String managerUsername)
  {
    this.managerUsername = managerUsername;
  }

  public String getManagerUsername()
  {
    return managerUsername;
  }

  public void setTimeAdministratorOrg(String timeAdministratorOrg)
  {
    this.timeAdministratorOrg = timeAdministratorOrg;
  }

  public String getTimeAdministratorOrg()
  {
    return timeAdministratorOrg;
  }

  public void setLocation(String location)
  {
    this.location = location;
  }

  public String getLocation()
  {
    return location;
  }

  public void setExemptStatus(String exemptStatus)
  {
    this.exemptStatus = exemptStatus;
  }

  public String getExemptStatus()
  {
    return exemptStatus;
  }

  public void setTimeZone(String timeZone)
  {
    this.timeZone = timeZone;
  }

  public String getTimeZone()
  {
    if (StringUtils.equalsIgnoreCase(timeZone, "Atlantic Time (Halifax)"))
    {
      return "2";
    }
    if (StringUtils.equalsIgnoreCase(timeZone, "Eastern Time (New York)") || StringUtils.equalsIgnoreCase(timeZone, "Eastern Time (Indiana)"))
    {
      return "1";
    }
    if (StringUtils.equalsIgnoreCase(timeZone, "Central Time (Chicago)"))
    {
      return "0";
    }
    if (StringUtils.equalsIgnoreCase(timeZone, "Mountain Time (Arizona)") || StringUtils.equalsIgnoreCase(timeZone, "Mountain Time (Denver)"))
    {
      return "-1";
    }
    if (StringUtils.equalsIgnoreCase(timeZone, "Pacific Time (San Francisco)"))
    {
      return "-2";
    }
    if (StringUtils.equalsIgnoreCase(timeZone, "Alaska Time (Anchorage)"))
    {
      return "-3";
    }
    if (StringUtils.equalsIgnoreCase(timeZone, "Hawaii Time (Honolulu)"))
    {
      return "-5";
    }

    return timeZone;
  }

  public static void setLocationList(List<String> locationList)
  {
    Employee.locationList = locationList;
  }

  public static List<String> getLocationList()
  {
    return locationList;
  }

  public void setManager(boolean manager)
  {
    this.manager = manager;
  }

  public boolean isManager()
  {
    return manager;
  }

  public void setTerminationDate(String terminationDate)
  {
    this.terminationDate = terminationDate;
  }

  public String getTerminationDate()
  {
    return terminationDate;
  }
  }
