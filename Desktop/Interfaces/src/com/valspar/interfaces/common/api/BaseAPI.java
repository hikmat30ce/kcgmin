package com.valspar.interfaces.common.api;

import com.valspar.interfaces.common.servlets.PropertiesServlet;
import com.valspar.workday.reader.WorkdayReader;
import java.util.*;
import javax.xml.ws.*;
import weblogic.wsee.jws.jaxws.owsm.*;
import weblogic.wsee.security.unt.*;
import weblogic.xml.crypto.wss.*;
import weblogic.xml.crypto.wss.provider.*;

public class BaseAPI
{
  private static final String workdayServer = PropertiesServlet.getProperty("workday.server");
  private static final String workdayUser = PropertiesServlet.getProperty("workday.username");
  private static final String workdayPassword = WorkdayReader.readProperty(PropertiesServlet.getProperty("workday.apikey"));
  private static final String tenant = PropertiesServlet.getProperty("workday.tenant");
  private static final SecurityPoliciesFeature securityFeatures = new SecurityPoliciesFeature(new String[] { "oracle/wss_username_token_client_policy" });
  
  public void bindCredentials(Object port, String wsdlURL)
  {
    BindingProvider bindingProvider = (BindingProvider) port;
    Map<String, Object> reqContext = bindingProvider.getRequestContext();
    List<CredentialProvider> credProviders = new ArrayList<CredentialProvider>();
    credProviders.add(new ClientUNTCredentialProvider(this.getWorkdayUser().getBytes(), this.getWorkdayPassword().getBytes()));
    reqContext.put(WSSecurityContext.CREDENTIAL_PROVIDER_LIST, credProviders);
    reqContext.put(BindingProvider.USERNAME_PROPERTY, this.getWorkdayUser() + "@" + this.getTenant());
    reqContext.put(BindingProvider.PASSWORD_PROPERTY, this.getWorkdayPassword());
    reqContext.put(BindingProvider.ENDPOINT_ADDRESS_PROPERTY,wsdlURL);
  }

  public static String getWorkdayServer()
  {
    return workdayServer;
  }

  public static String getWorkdayUser()
  {
    return workdayUser;
  }

  public static String getWorkdayPassword()
  {
    return workdayPassword;
  }

  public static SecurityPoliciesFeature getSecurityFeatures()
  {
    return securityFeatures;
  }

  public static String getTenant()
  {
    return tenant;
  }
}
