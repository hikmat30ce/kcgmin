package com.valspar.interfaces.hr.timeforce.program;

import com.sun.jersey.api.client.*;
import com.sun.jersey.api.client.config.*;
import com.sun.jersey.api.client.filter.*;
import com.sun.jersey.api.json.*;
import com.valspar.interfaces.common.BaseInterface;
import com.valspar.interfaces.common.servlets.PropertiesServlet;
import com.valspar.interfaces.common.utils.*;
import com.valspar.interfaces.hr.timeforce.beans.Employee;
import com.valspar.workday.reader.WorkdayReader;
import java.text.SimpleDateFormat;
import java.util.*;
import org.apache.commons.lang3.*;
import org.apache.log4j.*;
import org.codehaus.jackson.*;
import org.codehaus.jackson.map.*;
import org.codehaus.jackson.type.*;

public final class TimeForceInterface extends BaseInterface
{
  private StringBuilder emailMessage;
  private static Logger log4jLogger = Logger.getLogger(TimeForceInterface.class);

  public TimeForceInterface()
  {
  }

  public void execute()
  {
    try
    {
      this.setEmailMessage(new StringBuilder());
      java.util.Date startDate = new java.util.Date();
      String startMessage = "The Timeforce Interface Started at " + startDate + "<br>";
      log4jLogger.info(startMessage.replaceAll("<br>", "\n"));
      emailMessage.append(startMessage);

      List<Employee> employeeList = buildEmployeeList();
      if (!employeeList.isEmpty())
      {
        String fileLocation = writeFile(employeeList);
        ftpFile(fileLocation);

        String userAssignedFileLocation = writeUserAssignedFile(employeeList);
        ftpFile(userAssignedFileLocation);
      }

      java.util.Date endDate = new java.util.Date();
      String endMessage = "The Timeforce Interface Finsihed at " + endDate + "<br>";
      String durationMessage = "The overall interface processing duration was: " + CommonUtility.calculateRunTime(startDate, endDate);
      emailMessage.append(endMessage);
      emailMessage.append(durationMessage);
      log4jLogger.info(endMessage.replaceAll("<br>", "\n"));
      log4jLogger.info(durationMessage.replaceAll("<br>", "\n"));

      StringBuilder emailSubject = new StringBuilder();
      emailSubject.append("The Timeforce Interface ... on ");
      emailSubject.append(PropertiesServlet.getProperty("webserver"));
      EmailUtility.sendNotificationEmail(emailSubject.toString(), emailMessage.toString(), PropertiesServlet.getProperty("emailnotifylist"));
    }
    catch (Exception e)
    {
      log4jLogger.error(e);
    }
  }
  
  public List<Employee> buildEmployeeList()
  {
    log4jLogger.info("Timeforce.buildEmployeeList() - starting to build employee list");
    List<Employee> employeeList = new ArrayList<Employee>();
    List<Employee> returnList = new ArrayList<Employee>();

    try
    {
      ClientConfig clientConfig = new DefaultClientConfig();
      clientConfig.getFeatures().put(JSONConfiguration.FEATURE_POJO_MAPPING, Boolean.TRUE);
      Client client = Client.create(clientConfig);
      client.addFilter(new HTTPBasicAuthFilter(PropertiesServlet.getProperty("workday.username"), WorkdayReader.readProperty(PropertiesServlet.getProperty("workday.apikey"))));

      WebResource webResource = client.resource("https://" + PropertiesServlet.getProperty("workday.server") + "/ccx/service/customreport2/" + PropertiesServlet.getProperty("workday.tenant") + "/integrationOwner/Int_Timeforce?format=json");
      ClientResponse response = webResource.accept("application/json").type("application/json").get(ClientResponse.class);

      if (response.getStatus() != 200)
      {
        log4jLogger.info("Failed : HTTP error code : " + response.getStatus());
      }
      else
      {
        ObjectMapper mapper = new ObjectMapper();
        mapper.configure(DeserializationConfig.Feature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        JsonNode node = mapper.readTree(response.getEntity(String.class)).get("Report_Entry");
        employeeList = (List<Employee>) mapper.readValue(node.traverse(), new TypeReference<List<Employee>>(){});

        Map<String, Employee> fullEmployeeMap = new HashMap<String, Employee>();
        for (Employee emp: employeeList)
        {
          fullEmployeeMap.put(emp.getEmployeeID(), emp);
        }
 
        for (Employee employee: employeeList)
        {
          if (StringUtils.equalsIgnoreCase(employee.getPayRateType(), "Hourly") && employee.isTimeForceLocation())
          {
            returnList.add(employee);
            Employee employeeManager = fullEmployeeMap.get(employee.getManagerEmployeeID());
            if (employeeManager != null)
            {
              employeeManager.setManager(true);
              if (!returnList.contains(employeeManager))
              {
                returnList.add(employeeManager);
              }
            }
            else
            {
              log4jLogger.error("Timeforce.buildEmployeeList() Couldn't find manager for employee id " + employee.getEmployeeID() + " employee name " + employee.getLegalFirstName() + " " + employee.getLegalLastName());
            }
          }
        }
        response.close();
      }
    }
    catch (Exception e)
    {
      log4jLogger.error(e);
    }
    log4jLogger.info("Timeforce.buildEmployeeList() - done building employee list");
    return returnList;
  }

  public String writeFile(List<Employee> employeeList)
  {
    FlatFileUtility flatFileUtility = new FlatFileUtility(".csv", null, "\r\n");
    flatFileUtility.setCustomFilename("EmployeeList");
    try
    {
      log4jLogger.info("Timeforce.writeFile() - starting to write file");

      for (Employee employee: employeeList)
      {
        String employeeId = StringUtils.stripStart(employee.getEmployeeID(), "0");
        StringBuilder data = new StringBuilder();
        data.append(employeeId);
        data.append(",");
        data.append(employee.getLegalFirstName());
        data.append(",");
        data.append(employee.getLegalLastName());
        data.append(",");
        data.append(CommonUtility.nvl(employee.getLegalMiddleName(), ""));
        data.append(",");
        data.append(employee.getOriginalHireDate());
        data.append(",");
        data.append(employee.getHireDate());
        data.append(",");
        if (StringUtils.equalsIgnoreCase(employee.getManagerCategory(), "Individual Contributor"))
        {
          data.append("0");
        }
        else
        {
          data.append("1");
        }
        data.append(",");
        data.append(employee.getActiveStatus());
        data.append(",");
        if (StringUtils.isNotEmpty(employee.getPrimaryWorkEmail()))
        {
          data.append(employee.getPrimaryWorkEmail());
        }
        else
        {
          data.append(employeeId + "@valsparnoemail.com");
        }
        data.append(",");
        data.append(employee.getUserName());
        data.append(",");
        data.append("0");
        data.append(",");
        data.append("Regular");
        data.append(",");
        data.append("Hire");
        data.append(",");
        data.append("Hourly");
        data.append(",");
        if(StringUtils.equalsIgnoreCase(employee.getPayRateType(), "Salary"))
        {
          data.append("Exempt Salary");
        }
        else
        {
          data.append(employee.getPayRateType());
        }
        data.append(",");
        data.append(StringUtils.stripStart(CommonUtility.nvl(employee.getManagerEmployeeID(), ""), "0"));
        data.append(",");
        data.append("1234");
        data.append(",");
        data.append("Employee");
        data.append(",");
        if(StringUtils.startsWith(employee.getUserName(), "EMP"))
        {
          data.append("0");
        }
        else
        {
          data.append("1");
        }
        data.append(",");
        data.append(employee.getManagerUsername());
        data.append(",");
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
        data.append(sdf.format(new Date()));
        data.append(",");
        if(StringUtils.equalsIgnoreCase(employee.getTimeAdministratorOrg(),"Location Time Administrator"))
        {
          data.append("Administrator");
          data.append(" - ");
          data.append(employee.getLocation());
        }
        else if(StringUtils.equalsIgnoreCase(employee.getTimeAdministratorOrg(),"All Locations Time Administrator"))
        {
          data.append("Administrator All");
        }
        else if (StringUtils.equalsIgnoreCase(employee.getManagerCategory(), "Individual Contributor"))
        {
          if(StringUtils.equalsIgnoreCase(employee.getExemptStatus(),"Non Exempt"))
          {
            data.append("Employee");
            data.append(" - ");
            data.append(employee.getLocation());
            data.append(" - ");
            data.append("Non-Exempt");
          }
          else if(StringUtils.equalsIgnoreCase(employee.getExemptStatus(),"Hourly"))
          {
            data.append("Employee");
            data.append(" - ");
            data.append(employee.getLocation());
            data.append(" - ");
            data.append("Hourly");
          }
        }
        else if (StringUtils.equalsIgnoreCase(employee.getManagerCategory(), "Manager") || StringUtils.equalsIgnoreCase(employee.getManagerCategory(), "Manager's Manager"))
        {
          if(StringUtils.equalsIgnoreCase(employee.getExemptStatus(),"Non Exempt"))
          {
            data.append("Manager");
            data.append(" - ");
            data.append(employee.getLocation());
            data.append(" - ");
            data.append("Non-Exempt");
          }
          if(StringUtils.equalsIgnoreCase(employee.getExemptStatus(),"Exempt"))
          {
            data.append("Manager");
            data.append(" - ");
            data.append(employee.getLocation());
          }
        }
        data.append(",");
        data.append("1");
        
        data.append(",");
        data.append(employee.getTimeZone());

        flatFileUtility.writeLine(data.toString());
      }
      flatFileUtility.close();
      log4jLogger.info("Timeforce.writeFile() - done writing file");
    }
    catch (Exception e)
    {
      log4jLogger.info(e);
    }
    return flatFileUtility.getFileWritePath();
  }

  public String writeUserAssignedFile(List<Employee> employeeList)
  {
    FlatFileUtility flatFileUtility = new FlatFileUtility(".csv", null, "\r\n");
    flatFileUtility.setCustomFilename("UserAssigned");
    try
    {
      log4jLogger.info("Timeforce.writeUserAssignedFile() - starting to write file");

      for (Employee employee: employeeList)
      {
        if (employee.isManager())
        {
          StringBuilder data = new StringBuilder();
          data.append(employee.getUserName());
          data.append(",");
          data.append(StringUtils.stripStart(CommonUtility.nvl(employee.getEmployeeID(), ""), "0"));
          flatFileUtility.writeLine(data.toString());
        }
      }
      flatFileUtility.close();
      log4jLogger.info("Timeforce.writeUserAssignedFile() - done writing file");
    }
    catch (Exception e)
    {
      log4jLogger.info(e);
    }
    return flatFileUtility.getFileWritePath();
  }

  public void ftpFile(String fileLocation)
  {
    log4jLogger.info("Timeforce.ftpFile() - starting to ftp file");
    FtpUtility.sendFileOrDirectory(PropertiesServlet.getProperty("timeforce.server"), PropertiesServlet.getProperty("timeforce.user"), PropertiesServlet.getProperty("timeforce.password"), fileLocation, "", null);
    log4jLogger.info("Timeforce.ftpFile() - done ftping file");
  }

  public void setEmailMessage(StringBuilder emailMessage)
  {
    this.emailMessage = emailMessage;
  }

  public StringBuilder getEmailMessage()
  {
    return emailMessage;
  }
}
