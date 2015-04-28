package com.valspar.interfaces.hr.workdayadsync.program;

import com.sun.image.codec.jpeg.*;
import com.sun.jersey.api.client.Client;
import com.sun.jersey.api.client.ClientResponse;
import com.sun.jersey.api.client.WebResource;
import com.sun.jersey.api.client.config.ClientConfig;
import com.sun.jersey.api.client.config.DefaultClientConfig;
import com.sun.jersey.api.client.filter.HTTPBasicAuthFilter;
import com.sun.jersey.api.json.JSONConfiguration;
import com.valspar.interfaces.common.BaseInterface;
import com.valspar.interfaces.common.api.*;
import com.valspar.interfaces.common.enums.*;
import com.valspar.interfaces.common.servlets.PropertiesServlet;
import com.valspar.interfaces.common.utils.*;
import com.valspar.interfaces.hr.workdayadsync.beans.ADUserBean;
import com.valspar.interfaces.hr.workdayadsync.beans.Employee;
import com.valspar.workday.reader.WorkdayReader;
import java.awt.image.*;
import java.io.*;
import java.util.*;
import javax.imageio.*;
import javax.naming.*;
import javax.naming.directory.*;
import javax.naming.ldap.*;
import javax.ws.rs.core.MediaType;
import org.apache.commons.io.*;
import org.apache.commons.lang3.*;
import org.apache.log4j.Logger;
import org.codehaus.jackson.JsonNode;
import org.codehaus.jackson.map.DeserializationConfig;
import org.codehaus.jackson.map.ObjectMapper;
import org.codehaus.jackson.type.TypeReference;

public class WorkdayADSync extends BaseInterface
{
  private Map<String, ADUserBean> adUserBeanMap;  
  private HumanResourcesAPI humanResourcesAPI;
  private List<Employee> employeeList;
  private List<String> noncompliantImageList;
  private List<ADUserBean> nonMatchingEmailAddressList;
  private StringBuilder emailMessage;
  private static Logger log4jLogger = Logger.getLogger(WorkdayADSync.class);
  private static final String NON_COMPLIANT_IMAGE_DIRECTORY = PropertiesServlet.getProperty("workday.datadirectory")+File.separator+"noncompliant_images"+File.separator;
  
  public WorkdayADSync()
  {
  }

  public void execute()
  {
    try
    {
      this.setEmailMessage(new StringBuilder());
      this.setNoncompliantImageList(new ArrayList<String>());
      this.setNonMatchingEmailAddressList(new ArrayList<ADUserBean>());
      setHumanResourcesAPI(new HumanResourcesAPI());
      java.util.Date startDate = new java.util.Date();
      String startMessage = "The ADSync Interface Started at "+startDate+"<br>";
      log4jLogger.info(startMessage.replaceAll("<br>", "\n"));
      emailMessage.append(startMessage);
      FileUtils.cleanDirectory(new File(NON_COMPLIANT_IMAGE_DIRECTORY));
      int originalNumberOfActiveEmployees = 0;
      this.setEmployeeList(buildEmployeeListFromWorkday());
      originalNumberOfActiveEmployees = this.getEmployeeList().size();
      setAdUserBeanMap(lookupUsers());
      removeUsersNotFoundInAnyAD();
      updateActiveDirectory();
      printNoncompliantImages();
      updateNonmatchingEmailAddresses();
      
      java.util.Date endDate = new java.util.Date();
      String endMessage = "The ADSync Interface Finsihed at "+ endDate + "<br>";
      String durationMessage = "The overall interface processing duration was: "+CommonUtility.calculateRunTime(startDate,endDate);
      emailMessage.append(endMessage);
      emailMessage.append(durationMessage);
      log4jLogger.info(endMessage.replaceAll("<br>", "\n"));
      log4jLogger.info(durationMessage.replaceAll("<br>", "\n"));
      
      StringBuilder emailSubject = new StringBuilder();
      emailSubject.append("The ADSync Interface Ran for ");
      emailSubject.append(originalNumberOfActiveEmployees);
      emailSubject.append(" Active Employees on ");
      emailSubject.append(PropertiesServlet.getProperty("webserver"));
      EmailUtility.sendNotificationEmail(emailSubject.toString(), emailMessage.toString(),PropertiesServlet.getProperty("workday.adsyncemailnotifylist"));
    }
    catch (Exception e)
    {
      log4jLogger.error(e);
    }
  }
  
  public List<Employee> buildEmployeeListFromWorkday()
  {
    log4jLogger.info("ADSync.buildEmployeeListFromWorkday() - Starting to build employee list from Workday");
    List<Employee> returnList = new ArrayList<Employee>();

    try
    {
      ClientConfig clientConfig = new DefaultClientConfig();
      clientConfig.getFeatures().put(JSONConfiguration.FEATURE_POJO_MAPPING, Boolean.TRUE);
      Client client = Client.create(clientConfig);
      client.addFilter(new HTTPBasicAuthFilter(PropertiesServlet.getProperty("workday.username"), WorkdayReader.readProperty(PropertiesServlet.getProperty("workday.apikey"))));
 
      WebResource webResource = client.resource("https://" + PropertiesServlet.getProperty("workday.server") + "/ccx/service/customreport2/" + PropertiesServlet.getProperty("workday.tenant") + "/integrationOwner/Int_Active_Directory?format=json");


      String adsyncemployeeid = PropertiesServlet.getProperty("workday.adsyncemployeeid");
      if (StringUtils.isNotEmpty(adsyncemployeeid))
      {
        webResource = webResource.queryParam("Employee_ID", adsyncemployeeid);
      }

      WebResource.Builder request = webResource.getRequestBuilder(); 
      request.accept(MediaType.APPLICATION_JSON_TYPE);
      request.type(MediaType.APPLICATION_JSON_TYPE);

      ClientResponse response = request.get(ClientResponse.class);

      if (response.getStatus() != 200)
      {
        log4jLogger.info("ADSync.buildEmployeeListFromWorkday() - Failed : HTTP error code : " + response.getStatus());
      }
      else
      {
        ObjectMapper mapper = new ObjectMapper();
        mapper.configure(DeserializationConfig.Feature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        JsonNode node = mapper.readTree(response.getEntity(String.class)).get("Report_Entry");
 
        for (Employee employee: (List<Employee>) mapper.readValue(node.traverse(), new TypeReference<List<Employee>>(){}))
        {
          returnList.add(employee);
        }
  
        log4jLogger.info("ADSync.buildEmployeeListFromWorkday() - The size of the employee list is "+returnList.size());
        response.close();
      }
    }
    catch (Exception e)
    {
      log4jLogger.error(e);
    }
    log4jLogger.info("ADSync.buildEmployeeListFromWorkday() - done building employee list");
    return returnList;
  }

  private Map<String, ADUserBean> lookupUsers()
  {
    log4jLogger.info("ADSync.lookupUsers()");

    Map<String, ADUserBean> adUserBeanMap = new HashMap<String, ADUserBean>();
  
    SearchControls controls = new SearchControls();
    controls.setSearchScope(SearchControls.SUBTREE_SCOPE);
    controls.setReturningAttributes(new String[]{ "sAMAccountName", "distinguishedName", "mail" });
    
    StringBuilder searchString = new StringBuilder();
    searchString.append("(&(objectCategory=person)(objectClass=user)(|");

    for (Employee employee: getEmployeeList())
    {
      adUserBeanMap.put(employee.getUserName().toUpperCase(), null);
      searchString.append("(sAMAccountName=");
      searchString.append(employee.getUserName().toUpperCase());
      searchString.append(")");
    }
    searchString.append("))");

    for (Domains domain: Domains.values())
    {
      InitialLdapContext ctx = null;

      try
      {
        ctx = ADUtilityByDomain.getADConnection(domain);
        byte[] cookie = null;
        ctx.setRequestControls(new Control[]{ new PagedResultsControl(999, Control.CRITICAL) });       

        do
        {
          NamingEnumeration<SearchResult> results = ctx.search("", searchString.toString(), controls);
          while (results.hasMoreElements())
          {
            SearchResult sr = results.next();
            String userName = (String) sr.getAttributes().get("sAMAccountName").get();
            String dn = (String) sr.getAttributes().get("distinguishedName").get();
            String email = null;
            if (sr.getAttributes().get("mail") != null)
            {
              email = (String) sr.getAttributes().get("mail").get();
            }
            ADUserBean adBean = new ADUserBean();
            adBean.setDomain(domain);
            adBean.setDn(dn);
            adBean.setEmail(email);
            if (userName != null)
            {
              userName = userName.toUpperCase();
            }
            adUserBeanMap.put(userName, adBean);
          }
          results.close();
          Control[] ctl = ctx.getResponseControls();
          if (controls != null)
          {
            for (int i = 0; i < ctl.length; i++)
            {
              if (ctl[i] instanceof PagedResultsResponseControl)
              {
                PagedResultsResponseControl prrc = (PagedResultsResponseControl) ctl[i];
                cookie = prrc.getCookie();                
              }
            }
          }
          ctx.setRequestControls(new Control[] { new PagedResultsControl(999, cookie, Control.CRITICAL) });
        }
        while (cookie != null);
      }
      catch (Exception e)
      {
        log4jLogger.error(e);
      }
      finally
      {
        ADUtilityByDomain.closeLdapConnection(ctx);
      } 
    }
    return adUserBeanMap;
  }

  private void printNoncompliantImages()
  {
    if(!getNoncompliantImageList().isEmpty())
    {
      String imageMessage = "These users have a non standard image size or format in Workday:<br>";
      log4jLogger.info(imageMessage.replaceAll("<br>", "\n"));
      emailMessage.append(imageMessage);
    }    
    for(String text: getNoncompliantImageList())
    {
      log4jLogger.info(text);
      emailMessage.append(text);
      emailMessage.append("<br>");
    }
  }

  private void updateNonmatchingEmailAddresses()
  {
    log4jLogger.info("Starting updateNonmatchingEmailAddresses()");
    if(!getNonMatchingEmailAddressList().isEmpty())
    {
      String nonMatchingEmailAddressesMessage = "<br> " + getNonMatchingEmailAddressList().size() + " user(s) have non matching email addresses in Workday:<br>";
      log4jLogger.info(nonMatchingEmailAddressesMessage.replaceAll("<br>", "\n"));
      emailMessage.append(nonMatchingEmailAddressesMessage);
    }    
    for(ADUserBean adUserBean:getNonMatchingEmailAddressList())
    {
      String updateEmailAddressText = adUserBean.getEmployeeId() + " - AD has a different email address than Workday.  The email address in Workday will be updated to " + adUserBean.getEmail() + ". ";
      String updateEmailAddressError = getHumanResourcesAPI().updateEmailAddress(adUserBean.getEmployeeId(), adUserBean.getEmail());

      log4jLogger.info(updateEmailAddressText);
      emailMessage.append(updateEmailAddressText);
      if (StringUtils.isNotEmpty(updateEmailAddressError))
      {
        emailMessage.append("<br>");
        emailMessage.append(adUserBean.getEmployeeId());
        emailMessage.append(" - Error Updating Email Address: ");
        emailMessage.append(updateEmailAddressError);
      }
      emailMessage.append("<br>");
    }
    emailMessage.append("<br>");
    log4jLogger.info("Ending updateNonmatchingEmailAddresses()");
  }

  private void removeUsersNotFoundInAnyAD()
  {
    String userMessage = "<br>Users not found in any of the Active Directory domains searched:<br>";
    log4jLogger.info(userMessage.replaceAll("<br>", "\n"));
    emailMessage.append(userMessage);
    int notFoundCount = 0;
    Iterator i = getAdUserBeanMap().keySet().iterator();
    while (i.hasNext())
    {
      String userName = (String) i.next();
      ADUserBean adUserBean = getAdUserBeanMap().get(userName);
      if (adUserBean == null)
      {
        notFoundCount++;
        i.remove();
      }
    }

    Iterator i2 = this.getEmployeeList().iterator();
    while (i2.hasNext())
    {
      Employee employee = (Employee) i2.next();
      if (getAdUserBeanMap().get(employee.getUserName().toUpperCase()) == null)
      {
        StringBuilder missingMessage = new StringBuilder();
        missingMessage.append(employee.getLegalFirstName());
        missingMessage.append(" ");
        missingMessage.append(employee.getLegalLastName());
        missingMessage.append(" (");
        missingMessage.append(employee.getUserName().toUpperCase());
        missingMessage.append("-");
        missingMessage.append(employee.getEmployeeID());
        missingMessage.append(") ");
        missingMessage.append(employee.getBusinessTitle());
        missingMessage.append(" ");
        missingMessage.append(employee.getCostCenterHierarchyName());

        emailMessage.append(missingMessage);
        emailMessage.append("<br>");
        log4jLogger.info(missingMessage);
        
        i2.remove();
      }
    }
    String notFoundMessage = "A total of " + notFoundCount + " users were not found in any of the Active Directory domains searched.<br><br>";
    emailMessage.append(notFoundMessage);
    log4jLogger.info(notFoundMessage.replaceAll("<br>", "\n"));
  }

  private void updateActiveDirectory()
  {
    int imageCount = 0;
    InitialLdapContext ctx = null;
    Map<String, InitialLdapContext> connectionMap = new HashMap<String, InitialLdapContext>();
    for (Employee employee: getEmployeeList())
    {
      ADUserBean adUserBean = getAdUserBeanMap().get(employee.getUserName().toUpperCase());
      Domains domain = adUserBean.getDomain();
      
      byte[] employeeImage = null;
      if (employee.getBase64ImageData() != null)
      {
        if (employee.getImageFileName() != null && employee.getImageFileName().toLowerCase().contains(".jpg"))
        {
          imageCount++;
          employeeImage = employee.getBase64ImageData();
        }
        else
        {          
          getNoncompliantImageList().add("Employee image is not in jpg format for user " + employee.getUserName().toUpperCase() + "-" + employee.getEmployeeID() + " file name is " + employee.getImageFileName());
          getHumanResourcesAPI().writeImageEmployeeImageToPath(employee.getEmployeeID(), NON_COMPLIANT_IMAGE_DIRECTORY, employeeImage);
        }
      }
      byte[] employeeImageResized = null;
      if (employeeImage != null)
      {
        employeeImageResized = resizeImage(employee, employeeImage, 96, 96);
      }
      if (connectionMap.get(domain.getServer()) == null)
      {
        ctx = ADUtilityByDomain.getADConnection(domain);
        connectionMap.put(domain.getServer(), ctx);
      }
      else
      {
        ctx = connectionMap.get(domain.getServer());
      }

      try
      {
        SearchControls constrains = new SearchControls();
        constrains.setSearchScope(SearchControls.SUBTREE_SCOPE);

        StringBuilder sb = new StringBuilder();
        sb.append("(&(objectCategory=person)(objectClass=user)(|");
        sb.append("(sAMAccountName=");
        sb.append(employee.getUserName().toUpperCase());
        sb.append(")");
        sb.append("))");

        NamingEnumeration results = ctx.search("", sb.toString(), constrains);
        SearchResult sr = (SearchResult) results.next();
        log4jLogger.info("Updating user " + sr.getName());
        String workPhone = employee.getPrimaryWorkPhone();
        String workPhoneVoIP = employee.getPrimaryVoipPhone();
        if(StringUtils.isNotEmpty(workPhoneVoIP) && workPhoneVoIP.startsWith("+1"))
        {
          workPhoneVoIP = workPhoneVoIP.substring(2, workPhoneVoIP.length());
        }
        
        String businessTitle = employee.getBusinessTitle();
        String manager = null;
        
        ADUserBean managerADUserBean = getAdUserBeanMap().get(employee.getManagerUserName().toUpperCase());
        if (managerADUserBean != null)
        {
          manager = managerADUserBean.getDn();
        }
        String employeeID = employee.getEmployeeID();
        
        adUserBean.setEmployeeId(employeeID);
        String workdayWorkEmailAddress = employee.getPrimaryWorkEmail();
        if (!StringUtils.equalsIgnoreCase(adUserBean.getEmail(), workdayWorkEmailAddress))
        {
          this.getNonMatchingEmailAddressList().add(adUserBean);
        }

        Attributes attributes = new BasicAttributes();

        if (employeeImageResized != null)
        {
          attributes.put(new BasicAttribute("thumbnailPhoto", employeeImageResized));
        }
        if(StringUtils.isNotEmpty(employeeID) && employeeID.length() < 17)
        {
          attributes.put(new BasicAttribute("employeeID", employeeID));
        }
        if (StringUtils.isNotEmpty(workPhone))
        {
          attributes.put(new BasicAttribute("telephoneNumber", workPhone));
        }
        if (StringUtils.isNotEmpty(workPhoneVoIP))
        {
          attributes.put(new BasicAttribute("ipPhone", workPhoneVoIP));
        }
        if (StringUtils.isNotEmpty(workPhoneVoIP))
        {
          attributes.put(new BasicAttribute("otherTelephone", workPhoneVoIP));
        }
        if (StringUtils.isNotEmpty(businessTitle))
        {
          attributes.put(new BasicAttribute("title", businessTitle));
        }
        if (StringUtils.isNotEmpty(manager))
        {
          attributes.put(new BasicAttribute("manager", manager));
        }
        
        String streetAddress = findWorkStreetAddress(employee);
        if(StringUtils.isNotEmpty(streetAddress))
        {
          attributes.put(new BasicAttribute("streetAddress", streetAddress));
        }
        if(StringUtils.isNotEmpty(employee.getWorkAddressCity()))
        {
          attributes.put(new BasicAttribute("l", employee.getWorkAddressCity()));
        }
        if(StringUtils.isNotEmpty(employee.getWorkAddressState()))
        {
          attributes.put(new BasicAttribute("st", employee.getWorkAddressState()));
        }
        if(StringUtils.isNotEmpty(employee.getWorkAddressPostalCode()))
        {
          attributes.put(new BasicAttribute("postalCode", employee.getWorkAddressPostalCode()));
        }

        if(StringUtils.isNotEmpty(employee.getWorkAddressCountry()))
        {
          attributes.put(new BasicAttribute("c", employee.getWorkAddressCountry()));
        }
        if(StringUtils.isNotEmpty(employee.getRegion()))
        {
          attributes.put(new BasicAttribute("co", employee.getRegion()));
        }
        if(StringUtils.isNotEmpty(employee.getJobFamily()))
        {
          attributes.put(new BasicAttribute("department", employee.getJobFamily()));
        }
        if (StringUtils.isNotEmpty(employee.getCostCenterHierarchyName()))
        {
          attributes.put(new BasicAttribute("division", employee.getCostCenterHierarchyName()));
        }

        if (StringUtils.isNotEmpty(employee.getSupervisoryOrganizationName()))
        {
          System.out.println(StringUtils.substring(employee.getSupervisoryOrganizationName(), 0, 64));
          attributes.put(new BasicAttribute("o", StringUtils.substring(employee.getSupervisoryOrganizationName(), 0, 64) ));
        }
        ctx.modifyAttributes(sr.getName(), DirContext.REPLACE_ATTRIBUTE, attributes);
      }
      catch (Exception e)
      {
        log4jLogger.error(e);
        log4jLogger.error("Exception in updateActiveDirectory user name = " + employee.getUserName() + ", employee ID = " + employee.getEmployeeID());
        emailMessage.append("Exception updating Active Directory for user name = " + employee.getUserName() + ", employee ID = " + employee.getEmployeeID() + ", Error = " + e.toString() + "<br>");
      }
    }
    String employeeImageCountMessage = "<br>There are "+imageCount+" employees with images in Workday.<br>";
    emailMessage.append(employeeImageCountMessage);
    log4jLogger.info(employeeImageCountMessage.replaceAll("<br>", "\n"));
    ADUtilityByDomain.closeAllLdapConnections(connectionMap);
  }

  private String findWorkStreetAddress(Employee employee)
  {
    StringBuilder sb = new StringBuilder();

    if (StringUtils.equalsIgnoreCase(employee.getWorkAddressLine1(), "home office"))
    {
      sb.append("Home Office");
    }
    else
    {
      if (StringUtils.isNotEmpty(employee.getWorkAddressLine1()))
      {
        sb.append(employee.getWorkAddressLine1());
        sb.append("\n");
      }
      if (StringUtils.isNotEmpty(employee.getWorkAddressLine2()))
      {
        sb.append(employee.getWorkAddressLine2());
        sb.append("\n");
      }
      if (StringUtils.isNotEmpty(employee.getWorkAddressLine3()))
      {
        sb.append(employee.getWorkAddressLine3());
        sb.append("\n");
      }
      if (StringUtils.isNotEmpty(employee.getWorkAddressLine4()))
      {
        sb.append(employee.getWorkAddressLine4());
        sb.append("\n");
      }
    }
    return sb.toString();
  }

  private byte[] resizeImage(Employee employee, byte[] employeeImageBytes, int width, int height)
  {
    ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
    try
    {
      BufferedImage bufferedImage = ImageIO.read(new ByteArrayInputStream(employeeImageBytes));
      if (bufferedImage.getHeight() != 200 || bufferedImage.getWidth() != 200)
      {
        String imageStatement = employee.getUserName().toUpperCase() + "-" + employee.getEmployeeID() + " (Height " + bufferedImage.getHeight() + " Width " + bufferedImage.getWidth()+") file name is " + employee.getImageFileName();
        getNoncompliantImageList().add(imageStatement);
        getHumanResourcesAPI().writeImageEmployeeImageToPath(employee.getEmployeeID(), NON_COMPLIANT_IMAGE_DIRECTORY, employeeImageBytes);
      }
      double thumbRatio = (double) width / (double) height;
      int imageWidth = bufferedImage.getWidth(null);
      int imageHeight = bufferedImage.getHeight(null);
      double imageRatio = (double) imageWidth / (double) imageHeight;
      if (thumbRatio < imageRatio)
      {
        height = (int) (width / imageRatio);
      }
      else
      {
        width = (int) (height * imageRatio);
      }

      BufferedImage thumbImage = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
      java.awt.Graphics2D graphics2D = thumbImage.createGraphics();
      graphics2D.setRenderingHint(java.awt.RenderingHints.KEY_INTERPOLATION, java.awt.RenderingHints.VALUE_INTERPOLATION_BILINEAR);
      graphics2D.drawImage(bufferedImage, 0, 0, width, height, null);

      JPEGImageEncoder encoder = JPEGCodec.createJPEGEncoder(byteArrayOutputStream);
      JPEGEncodeParam param = encoder.getDefaultJPEGEncodeParam(thumbImage);
      param.setQuality(1.0f, false);
      encoder.setJPEGEncodeParam(param);
      encoder.encode(thumbImage);
    }
    catch (Exception e)
    {
      log4jLogger.error(e);
    }
    return byteArrayOutputStream.toByteArray();
  }

  public void setEmployeeList(List<Employee> employeeList)
  {
    this.employeeList = employeeList;
  }

  public List<Employee> getEmployeeList()
  {
    return employeeList;
  }

  public void setNonMatchingEmailAddressList(List<ADUserBean> nonMatchingEmailAddressList)
  {
    this.nonMatchingEmailAddressList = nonMatchingEmailAddressList;
  }

  public List<ADUserBean> getNonMatchingEmailAddressList()
  {
    return nonMatchingEmailAddressList;
  }

  public void setEmailMessage(StringBuilder emailMessage)
  {
    this.emailMessage = emailMessage;
  }

  public StringBuilder getEmailMessage()
  {
    return emailMessage;
  }

  public void setHumanResourcesAPI(HumanResourcesAPI humanResourcesAPI)
  {
    this.humanResourcesAPI = humanResourcesAPI;
  }

  public HumanResourcesAPI getHumanResourcesAPI()
  {
    return humanResourcesAPI;
  }

  public void setAdUserBeanMap(Map<String, ADUserBean> adUserBeanMap)
  {
    this.adUserBeanMap = adUserBeanMap;
  }

  public Map<String, ADUserBean> getAdUserBeanMap()
  {
    return adUserBeanMap;
  }

  public void setNoncompliantImageList(List<String> noncompliantImageList)
  {
    this.noncompliantImageList = noncompliantImageList;
  }

  public List<String> getNoncompliantImageList()
  {
    return noncompliantImageList;
  }
}
