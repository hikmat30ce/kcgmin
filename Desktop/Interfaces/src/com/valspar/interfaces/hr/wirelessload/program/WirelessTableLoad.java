package com.valspar.interfaces.hr.wirelessload.program;

import com.sun.jersey.api.client.Client;
import com.sun.jersey.api.client.ClientResponse;
import com.sun.jersey.api.client.WebResource;
import com.sun.jersey.api.client.config.ClientConfig;
import com.sun.jersey.api.client.config.DefaultClientConfig;
import com.sun.jersey.api.client.filter.HTTPBasicAuthFilter;
import com.sun.jersey.api.json.JSONConfiguration;
import com.valspar.interfaces.common.BaseInterface;
import com.valspar.interfaces.common.beans.ConnectionAccessBean;
import com.valspar.interfaces.common.enums.DataSource;
import com.valspar.interfaces.common.enums.Domains;
import com.valspar.interfaces.common.hibernate.HibernateUtil;
import com.valspar.interfaces.common.servlets.PropertiesServlet;
import com.valspar.interfaces.common.utils.ADUtilityByDomain;
import com.valspar.interfaces.common.utils.CommonUtility;
import com.valspar.interfaces.common.utils.EmailUtility;
import com.valspar.interfaces.common.utils.JDBCUtil;
import com.valspar.interfaces.hr.wirelessload.beans.CurrentWorkdayMobileNumbersBean;
import com.valspar.interfaces.hr.wirelessload.beans.IntMobileDeviceBean;
import com.valspar.interfaces.hr.wirelessload.beans.PhoneNumberBean;
import com.valspar.workday.reader.WorkdayReader;
import java.util.*;
import javax.naming.*;
import javax.naming.directory.*;
import javax.naming.ldap.*;
import javax.ws.rs.core.MediaType;
import oracle.jdbc.*;
import org.apache.commons.lang3.StringUtils;
import org.apache.log4j.*;
import org.codehaus.jackson.JsonNode;
import org.codehaus.jackson.map.DeserializationConfig;
import org.codehaus.jackson.map.ObjectMapper;
import org.codehaus.jackson.type.TypeReference;
import org.hibernate.*;

public class WirelessTableLoad extends BaseInterface
{
  private StringBuilder emailMessage;
  private static Logger log4jLogger = Logger.getLogger(WirelessTableLoad.class);

  public WirelessTableLoad()
  {
  }

  public void execute()
  {
    this.setEmailMessage(new StringBuilder());

    Date startDate = new Date();
    String startMessage = "The WirelessTableLoad Started at " + startDate + "<br>";
    log4jLogger.info(startMessage.replaceAll("<br>", "\n"));
    emailMessage.append(startMessage);

    List<CurrentWorkdayMobileNumbersBean> currentWokdayMobileNumbersBeanList = buildIntMobileDeviceBeans();
    insertBeans(currentWokdayMobileNumbersBeanList);
    syncUserLineAssignments();
    rebuidOrgHierarchyTable();

    log4jLogger.info("Ending WirelessTableLoad");
    Date endDate = new Date();
    String endMessage = "The WirelessTableLoad Finsihed at " + endDate + "<br>";
    String durationMessage = "The overall interface processing duration was: " + CommonUtility.calculateRunTime(startDate, endDate);
    emailMessage.append(endMessage);
    emailMessage.append(durationMessage);
    log4jLogger.info(endMessage.replaceAll("<br>", "\n"));
    log4jLogger.info(durationMessage.replaceAll("<br>", "\n"));

    StringBuilder emailSubject = new StringBuilder();
    emailSubject.append("The WirelessTableLoad Completed on ");
    emailSubject.append(PropertiesServlet.getProperty("webserver"));
    if (currentWokdayMobileNumbersBeanList != null)
    {
      emailSubject.append(" ");
      emailSubject.append(currentWokdayMobileNumbersBeanList.size());
      emailSubject.append(" Wireless Numbers Loaded");
    }
    EmailUtility.sendNotificationEmail(emailSubject.toString(), emailMessage.toString(), PropertiesServlet.getProperty("wirlesstableload.emailnotifylist"));

  }

  public List<CurrentWorkdayMobileNumbersBean> buildIntMobileDeviceBeans()
  {
    log4jLogger.info("buildIntMobileDeviceBeans() - Starting to build beans from Workday");
    List<CurrentWorkdayMobileNumbersBean> currentWokdayMobileNumbersBeanList = new ArrayList<CurrentWorkdayMobileNumbersBean>();

    try
    {
      ClientConfig clientConfig = new DefaultClientConfig();
      clientConfig.getFeatures().put(JSONConfiguration.FEATURE_POJO_MAPPING, Boolean.TRUE);
      Client client = Client.create(clientConfig);
      client.addFilter(new HTTPBasicAuthFilter(PropertiesServlet.getProperty("workday.username"), WorkdayReader.readProperty(PropertiesServlet.getProperty("workday.apikey"))));

      WebResource webResource = client.resource("https://" + PropertiesServlet.getProperty("workday.server") + "/ccx/service/customreport2/" + PropertiesServlet.getProperty("workday.tenant") + "/integrationOwner/Int_Mobile_Devices?format=json");

      WebResource.Builder request = webResource.getRequestBuilder();
      request.accept(MediaType.APPLICATION_JSON_TYPE);
      request.type(MediaType.APPLICATION_JSON_TYPE);
      ClientResponse response = request.get(ClientResponse.class);

      if (response.getStatus() != 200)
      {
        log4jLogger.info("buildIntMobileDeviceBeans() - Failed : HTTP error code : " + response.getStatus());
      }
      else
      {
        ObjectMapper mapper = new ObjectMapper();
        mapper.configure(DeserializationConfig.Feature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        JsonNode node = mapper.readTree(response.getEntity(String.class)).get("Report_Entry");

        for (IntMobileDeviceBean intMobileDeviceBean: (List<IntMobileDeviceBean>) mapper.readValue(node.traverse(), new TypeReference<List<IntMobileDeviceBean>>()
          {
          }))
        {

          for (PhoneNumberBean phoneNumberBean: intMobileDeviceBean.getPhoneNumbers())
          {
            CurrentWorkdayMobileNumbersBean currentWorkdayMobileNumbersBean = new CurrentWorkdayMobileNumbersBean();
            currentWorkdayMobileNumbersBean.setCategory(phoneNumberBean.getPhoneType());
            currentWorkdayMobileNumbersBean.setCostCenterName(intMobileDeviceBean.getCostCenterName());
            currentWorkdayMobileNumbersBean.setEmailAddress(intMobileDeviceBean.getEmailAddress());
            currentWorkdayMobileNumbersBean.setEmployeeId(intMobileDeviceBean.getEmployeeId());
            currentWorkdayMobileNumbersBean.setFirstName(intMobileDeviceBean.getFirstName());
            currentWorkdayMobileNumbersBean.setLastName(intMobileDeviceBean.getLastName());
            currentWorkdayMobileNumbersBean.setJobTitle(intMobileDeviceBean.getJobTitle());
            currentWorkdayMobileNumbersBean.setLocation(intMobileDeviceBean.getLocation());
            currentWorkdayMobileNumbersBean.setCountryISOCode(intMobileDeviceBean.getCountryIsoCode());
            currentWorkdayMobileNumbersBean.setUserName(StringUtils.upperCase(intMobileDeviceBean.getUserName()));
            currentWorkdayMobileNumbersBean.setInternationalPhoneCode(phoneNumberBean.getInternationalPhoneCode());
            currentWorkdayMobileNumbersBean.setCreationDate(new Date());
            currentWorkdayMobileNumbersBean.setJobFamily(intMobileDeviceBean.getJobFamily());
            String mobileNumber = phoneNumberBean.getMobileNumber();
            if (mobileNumber != null)
            {
              mobileNumber = mobileNumber.replaceAll("[^\\d]", "");
            }
            currentWorkdayMobileNumbersBean.setMobileNumber(mobileNumber);
            currentWorkdayMobileNumbersBean.setCostCenterID(intMobileDeviceBean.getCostCenterID());
            currentWokdayMobileNumbersBeanList.add(currentWorkdayMobileNumbersBean);
          }
        }

        log4jLogger.info("buildIntMobileDeviceBeans() - The size of the wireless number list is " + currentWokdayMobileNumbersBeanList.size());
        response.close();
      }
    }
    catch (Exception e)
    {
      log4jLogger.error(e);
    }
    log4jLogger.info("buildIntMobileDeviceBeans() - done building wireless number list");
    return currentWokdayMobileNumbersBeanList;
  }

  /*
  public List<CurrentWorkdayMobileNumbersBean> buildCurrentWokdayMobileNumbersBeans()
  {
    List<CurrentWorkdayMobileNumbersBean> currentWokdayMobileNumbersBeanList = new ArrayList<CurrentWorkdayMobileNumbersBean>();
    ReportClient reportClient = new ReportClient(PropertiesServlet.getProperty("workdayserver"), PropertiesServlet.getProperty("workdayuser"), PropertiesServlet.getProperty("workdaypassword"));
    ReportDataType reportDataType = reportClient.listWirelessPhoneNumbers();
    CurrentWorkdayMobileNumbersBean currentWorkdayMobileNumbersBean = null;
    for(ReportEntryType reportEntryType: reportDataType.getReportEntry())
    {
      for(PhoneNumbersType phoneNumbersType: reportEntryType.getPhoneNumbers())
      {
        currentWorkdayMobileNumbersBean = new CurrentWorkdayMobileNumbersBean();
        currentWorkdayMobileNumbersBean.setCategory(phoneNumbersType.getPhoneType().getDescriptor());
        if(reportEntryType.getCostCenterName() != null && !reportEntryType.getCostCenterName().isEmpty())
        {
          OrganizationObjectType organizationObjectType = reportEntryType.getCostCenterName().get(0);
          currentWorkdayMobileNumbersBean.setCostCenterName(organizationObjectType.getDescriptor());
        }
        currentWorkdayMobileNumbersBean.setEmailAddress(reportEntryType.getEmailAddress());
        currentWorkdayMobileNumbersBean.setEmployeeId(reportEntryType.getEmployeeId());
        currentWorkdayMobileNumbersBean.setFirstName(reportEntryType.getFirstName());
        currentWorkdayMobileNumbersBean.setLastName(reportEntryType.getLastName());
        currentWorkdayMobileNumbersBean.setJobTitle(reportEntryType.getJobTitle());
        currentWorkdayMobileNumbersBean.setLocation(reportEntryType.getLocation().getDescriptor());
        currentWorkdayMobileNumbersBean.setCountryISOCode(reportEntryType.getPrimaryWorkAddressCountry().getCountryIsoCode());
        currentWorkdayMobileNumbersBean.setUserName(StringUtils.upperCase(reportEntryType.getUserName()));
        currentWorkdayMobileNumbersBean.setInternationalPhoneCode(phoneNumbersType.getInternationalPhoneCode());
        currentWorkdayMobileNumbersBean.setCreationDate(new Date());
        if(reportEntryType.getJobFamily() != null && !reportEntryType.getJobFamily().isEmpty())
        {
          currentWorkdayMobileNumbersBean.setMajorFamily(reportEntryType.getJobFamily().get(0).getMajorFamily());
        }
        String mobileNumber = phoneNumbersType.getMobileNumber();
        if(mobileNumber != null)
        {
          mobileNumber = mobileNumber.replaceAll( "[^\\d]", "" );
        }
        currentWorkdayMobileNumbersBean.setMobileNumber(mobileNumber);
        currentWokdayMobileNumbersBeanList.add(currentWorkdayMobileNumbersBean);
        currentWorkdayMobileNumbersBean.setCostCenterID(reportEntryType.getCostCenterID());
      }
    }
    return currentWokdayMobileNumbersBeanList;
  }
*/

  public static String lookupUserAttributeByUserName(Domains domain, String userName, String attributeName)
  {
    String attributeValue = null;
    NamingEnumeration<SearchResult> results = null;
    InitialDirContext ctx = null;

    try
    {
      ctx = ADUtilityByDomain.getADConnection(domain);
      SearchControls controls = new SearchControls();
      controls.setSearchScope(SearchControls.SUBTREE_SCOPE);
      controls.setReturningAttributes(new String[]
          { attributeName });
      results = ctx.search("", "(&(objectCategory=person)(objectClass=user)(sAMAccountName=" + userName + "))", controls);

      if (results.hasMore())
      {
        SearchResult searchResult = results.next();
        Attributes attributes = searchResult.getAttributes();

        Attribute attribute = attributes.get(attributeName);
        if (attribute != null)
        {
          attributeValue = (String) attribute.get();
        }
      }
      results.close();
    }
    catch (Exception e)
    {
      log4jLogger.error(e);
    }
    finally
    {
      try
      {
        ctx.close();
      }
      catch (Exception e)
      {
        log4jLogger.error(e);
      }
    }
    return attributeValue;
  }

  public void saveCurrentWorkdayMobileNumbersBeanList(List<CurrentWorkdayMobileNumbersBean> currentWorkdayMobileNumbersBeanList)
  {
    Session session = HibernateUtil.getHibernateSession(DataSource.MIDDLEWARE);
    for (CurrentWorkdayMobileNumbersBean currentWorkdayMobileNumbersBean: currentWorkdayMobileNumbersBeanList)
    {
      try
      {
        session.save(currentWorkdayMobileNumbersBean);
      }
      catch (Exception e)
      {
        log4jLogger.error("Error in saveCurrentWorkdayMobileNumbersBeanList():  Employee ID = " + currentWorkdayMobileNumbersBean.getEmployeeId());
        log4jLogger.error(e);
      }
    }
    HibernateUtil.closeHibernateSession(session);
  }

  private static void insertBeans(List<CurrentWorkdayMobileNumbersBean> currentWokdayMobileNumbersBeanList)
    {
      OracleConnection middlewareConn = (OracleConnection) ConnectionAccessBean.getConnection(DataSource.MIDDLEWARE);

      OraclePreparedStatement pst = null;
      OraclePreparedStatement pst2 = null;

      try
      {
        middlewareConn.setAutoCommit(false);

        StringBuilder sb2 = new StringBuilder();
        sb2.append("DELETE FROM MOBILE.CURRENT_WORKDAY_MOBILE_NUMBERS");
        pst2 = (OraclePreparedStatement) middlewareConn.prepareStatement(sb2.toString());
        pst2.execute();

        StringBuilder sb = new StringBuilder();
        sb.append("INSERT INTO  MOBILE.CURRENT_WORKDAY_MOBILE_NUMBERS( ");
        sb.append("  WORKDAY_MOBILE_NUMBER_ID, ");
        sb.append("  USERNAME, ");
        sb.append("  FIRST_NAME, ");
        sb.append("  LAST_NAME, ");
        sb.append("  JOB_TITLE, ");
        sb.append("  COUNTRY_ISO_CODE, ");
        sb.append("  INTERNATIONAL_PHONE_CODE, ");
        sb.append("  MOBILE_NUMBER, ");
        sb.append("  CATEGORY, ");
        sb.append("  EMPLOYEE_ID, ");
        sb.append("  MAJOR_FAMILY, ");
        sb.append("  LOCATION, ");
        sb.append("  COST_CENTER_NAME, ");
        sb.append("  EMAIL_ADDRESS, ");
        sb.append("  CREATION_DATE, ");
        sb.append("  GL_STRING, ");
        sb.append("  TERMINATION_DATE) ");
        sb.append("VALUES ( ");
        sb.append("   MOBILE.workday_mobile_number_id_seq.nextval, ");
        sb.append("   :USERNAME, ");
        sb.append("   :FIRST_NAME, ");
        sb.append("   :LAST_NAME, ");
        sb.append("   :JOB_TITLE, ");
        sb.append("   :COUNTRY_ISO_CODE, ");
        sb.append("   :INTERNATIONAL_PHONE_CODE, ");
        sb.append("   :MOBILE_NUMBER,  ");
        sb.append("   :CATEGORY,  ");
        sb.append("   :EMPLOYEE_ID,  ");
        sb.append("   :MAJOR_FAMILY,  ");
        sb.append("   :LOCATION,  ");
        sb.append("   :COST_CENTER_NAME,  ");
        sb.append("   :EMAIL_ADDRESS, ");
        sb.append("   :CREATION_DATE,  ");
        sb.append("   :GL_STRING,  ");
        sb.append("   :TERMINATION_DATE) ");

        pst = (OraclePreparedStatement) middlewareConn.prepareStatement(sb.toString());

        for (CurrentWorkdayMobileNumbersBean currentWorkdayMobileNumbersBean:currentWokdayMobileNumbersBeanList)
        {
          pst.setStringAtName("USERNAME", currentWorkdayMobileNumbersBean.getUserName());
          pst.setStringAtName("FIRST_NAME", currentWorkdayMobileNumbersBean.getFirstName());
          pst.setStringAtName("LAST_NAME", currentWorkdayMobileNumbersBean.getLastName());
          pst.setStringAtName("JOB_TITLE", currentWorkdayMobileNumbersBean.getJobTitle());
          pst.setStringAtName("COUNTRY_ISO_CODE", currentWorkdayMobileNumbersBean.getCountryISOCode());
          pst.setStringAtName("INTERNATIONAL_PHONE_CODE", currentWorkdayMobileNumbersBean.getInternationalPhoneCode());
          pst.setStringAtName("MOBILE_NUMBER", currentWorkdayMobileNumbersBean.getMobileNumber());
          pst.setStringAtName("CATEGORY", currentWorkdayMobileNumbersBean.getCategory());
          pst.setStringAtName("EMPLOYEE_ID", currentWorkdayMobileNumbersBean.getEmployeeId());
          pst.setStringAtName("MAJOR_FAMILY", currentWorkdayMobileNumbersBean.getJobFamily());
          pst.setStringAtName("LOCATION", currentWorkdayMobileNumbersBean.getLocation());
          pst.setStringAtName("COST_CENTER_NAME", currentWorkdayMobileNumbersBean.getCostCenterName());
          pst.setStringAtName("EMAIL_ADDRESS", currentWorkdayMobileNumbersBean.getEmailAddress());
          pst.setDATEAtName("CREATION_DATE", JDBCUtil.getDATE(currentWorkdayMobileNumbersBean.getCreationDate()));
          pst.setStringAtName("GL_STRING", currentWorkdayMobileNumbersBean.getCostCenterID());
          pst.setDATEAtName("TERMINATION_DATE", JDBCUtil.getDATE(currentWorkdayMobileNumbersBean.getTerminationDate()));
          pst.execute();
        }
        middlewareConn.commit();
      }
      catch (Exception e)
      {
        log4jLogger.error(e);
        JDBCUtil.rollBack(middlewareConn);
      }
      finally
      {
        JDBCUtil.autoCommit(middlewareConn);
        JDBCUtil.close(pst);
        JDBCUtil.close(pst2);
      }
    }

  public void syncUserLineAssignments()
  {
    Session session = HibernateUtil.getHibernateSession(DataSource.MIDDLEWARE);

    try
    {
      session.createSQLQuery("{call MOBILE.COMMON_PKG.sync_user_line_assignments}").executeUpdate();
    }
    catch (Exception e)
    {
      log4jLogger.error(e);
    }
    finally
    {
      session.close();
    }
  }

  public void rebuidOrgHierarchyTable()
  {
    Session session = HibernateUtil.getHibernateSession(DataSource.MIDDLEWARE);
    Transaction transaction = session.beginTransaction();

    try
    {
      rebuildOrgHierarchy(Domains.CORPORATE, session);
      rebuildOrgHierarchy(Domains.EUROPE, session);
      rebuildOrgHierarchy(Domains.ASIA, session);

      StringBuilder sb = new StringBuilder();
      sb.append("merge into mobile.current_org_hierarchy dest ");
      sb.append("using ( select dn, employee_id ");
      sb.append("        from mobile.current_org_hierarchy ");
      sb.append("        where dn is not null ");
      sb.append("          and employee_id is not null) src ");
      sb.append("on (DEST.MANAGER_DN = src.dn) ");
      sb.append("when matched then update set ");
      sb.append("  manager_id = src.employee_id ");
      sb.append("where dest.manager_dn is not null ");
      sb.append("  and dest.manager_dn != dest.dn ");

      session.createSQLQuery(sb.toString()).executeUpdate();
      transaction.commit();
    }
    catch (Exception e)
    {
      log4jLogger.error(e);
      transaction.rollback();
    }
    finally
    {
      session.close();
    }
  }

  private void rebuildOrgHierarchy(Domains domain, Session session)
  {
    InitialLdapContext ctx = ADUtilityByDomain.getADConnection(domain);

    try
    {
      byte[] cookie = null;
      ctx.setRequestControls(new Control[]
          { new PagedResultsControl(999, Control.CRITICAL) });

      SearchControls searchCtls = new SearchControls();
      searchCtls.setSearchScope(SearchControls.SUBTREE_SCOPE);
      String returnedAtts[] =
      { "distinguishedName", "employeeID", "sAMAccountName", "givenName", "sn", "manager", "mail" };
      searchCtls.setReturningAttributes(returnedAtts);

      StringBuilder sb = new StringBuilder();
      sb.append("DELETE FROM MOBILE.CURRENT_ORG_HIERARCHY ");
      sb.append("WHERE DOMAIN = :DOMAIN ");

      Query deleteQuery = session.createSQLQuery(sb.toString());
      deleteQuery.setString("DOMAIN", domain.getDisplayName());
      deleteQuery.executeUpdate();

      sb = new StringBuilder();
      sb.append("INSERT INTO MOBILE.CURRENT_ORG_HIERARCHY ( ");
      sb.append("   DOMAIN, USERNAME, EMPLOYEE_ID, FIRST_NAME,  ");
      sb.append("   LAST_NAME, DN, MANAGER_DN, EMAIL)  ");
      sb.append("VALUES (");
      sb.append("   :DOMAIN, :USERNAME, :EMPLOYEE_ID, :FIRST_NAME,  ");
      sb.append("   :LAST_NAME, :DN, :MANAGER_DN, :EMAIL)  ");

      Query insertQuery = session.createSQLQuery(sb.toString());

      StringBuilder sbFilter = new StringBuilder();
      sbFilter.append("(&(objectCategory=person)(objectClass=user)(employeeID=*))");

      int batchCount = 0;

      do
      {
        NamingEnumeration<SearchResult> answer = ctx.search("", sbFilter.toString(), searchCtls);
        while (answer.hasMoreElements())
        {
          SearchResult sr = answer.next();
          Attributes attr = sr.getAttributes();

          Attribute usernameAttribute = attr.get("sAMAccountName");
          if (usernameAttribute == null)
          {
            log4jLogger.error("Found a record in AD with no username!");
          }
          else
          {
            Attribute employeeIdAttribute = attr.get("employeeID");
            Attribute firstNameAttribute = attr.get("givenName");
            Attribute lastNameAttribute = attr.get("sn");
            Attribute distinguishedNameAttribute = attr.get("distinguishedName");
            Attribute managerAttribute = attr.get("manager");
            Attribute emailAttribute = attr.get("mail");

            String employeeId = (employeeIdAttribute != null)? (String) employeeIdAttribute.get(): null;
            String firstName = (firstNameAttribute != null)? (String) firstNameAttribute.get(): null;
            String lastName = (lastNameAttribute != null)? (String) lastNameAttribute.get(): null;
            String distinguishedName = (distinguishedNameAttribute != null)? (String) distinguishedNameAttribute.get(): null;
            String managerDistinguishedName = (managerAttribute != null)? (String) managerAttribute.get(): null;
            String email = (emailAttribute != null)? (String) emailAttribute.get(): null;

            insertQuery.setString("DOMAIN", domain.getDisplayName());
            insertQuery.setString("USERNAME", (String) usernameAttribute.get());
            insertQuery.setString("EMPLOYEE_ID", employeeId);
            insertQuery.setString("FIRST_NAME", firstName);
            insertQuery.setString("LAST_NAME", lastName);
            insertQuery.setString("DN", distinguishedName);
            insertQuery.setString("MANAGER_DN", managerDistinguishedName);
            insertQuery.setString("EMAIL", email);

            insertQuery.executeUpdate();
            if (++batchCount % 20 == 0)
            {
              session.flush();
              session.clear();
            }
          }
        }

        Control[] controls = ctx.getResponseControls();

        if (controls != null)
        {
          for (int i = 0; i < controls.length; i++)
          {
            if (controls[i] instanceof PagedResultsResponseControl)
            {
              PagedResultsResponseControl prrc = (PagedResultsResponseControl) controls[i];
              cookie = prrc.getCookie();
            }
          }
        }
        ctx.setRequestControls(new Control[]
            { new PagedResultsControl(999, cookie, Control.CRITICAL) });
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

  public void setEmailMessage(StringBuilder emailMessage)
  {
    this.emailMessage = emailMessage;
  }

  public StringBuilder getEmailMessage()
  {
    return emailMessage;
  }
}
