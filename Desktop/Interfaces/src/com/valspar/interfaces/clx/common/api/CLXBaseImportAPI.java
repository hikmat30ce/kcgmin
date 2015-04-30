package com.valspar.interfaces.clx.common.api;

import com.valspar.clx.*;
import com.valspar.clx.returnvalue.generated.*;
import com.valspar.interfaces.common.servlets.PropertiesServlet;
import java.io.*;
import java.net.*;
import java.rmi.RemoteException;
import java.security.cert.X509Certificate;
import java.util.*;
import javax.xml.bind.*;
import javax.xml.rpc.Stub;
import javax.xml.transform.*;
import javax.xml.transform.stream.*;
import javax.xml.ws.BindingProvider;
import org.apache.commons.lang3.*;
import org.apache.log4j.Logger;
import weblogic.wsee.async.AsyncPreCallContext;
import weblogic.wsee.connection.transport.http.HttpTransportInfo;
import weblogic.wsee.security.bst.ClientBSTCredentialProvider;
import weblogic.wsee.security.unt.ClientUNTCredentialProvider;
import weblogic.xml.crypto.wss.WSSecurityContext;
import weblogic.xml.crypto.wss.provider.CredentialProvider;

public class CLXBaseImportAPI
{
  private IImportWebService _port;
  private List<CredentialProvider> _credProviders;
  private static AsyncPreCallContext asyncPreCallContext;
  private static final String clxServer = PropertiesServlet.getProperty("clx.server");
  private static final String clxUser = PropertiesServlet.getProperty("clx.user");
  private static final String clxPassword = PropertiesServlet.getProperty("clx.password");
  private static final String clxAccountNumber = PropertiesServlet.getProperty("clx.accountnumber");
  private static final String clxEndpointURL = PropertiesServlet.getProperty("clx.endpointurl");
  private static Logger log4jLogger = Logger.getLogger(CLXBaseImportAPI.class);

  public CLXBaseImportAPI()
  {
    try
    {
      ImportWebService service = new ImportWebService_Impl();
      _port = service.getIImportWebServicePort();
      getIImportWebService();
      setPortCredentialProviderList();
      setEndpoint(getClxEndpointURL());
    }
    catch (Exception e)
    {
      log4jLogger.error("Exception in CLXBaseImportAPI " + e);
    }

  }

  public IImportWebService getIImportWebService()
  {
    if (_port == null)
    {
      try
      {
        CLXBaseImportAPI clxBaseImportAPI = new CLXBaseImportAPI();
        clxBaseImportAPI.bindCredentials(_port);
      }
      catch (Exception e)
      {
        log4jLogger.error("Exception in CLXOutboundOrderAPI.getIImportWebService() " + e);
      }
      return _port;
    }
    else
    {
      return _port;
    }
  }

  public void bindCredentials(Object port)
  {
    try
    {
      BindingProvider bindingProvider = (BindingProvider) port;
      CredentialProvider credProvider = new ClientUNTCredentialProvider(getClxUser().getBytes(), getClxPassword().getBytes());
      addCredentialProvider(credProvider);
      asyncPreCallContext = (AsyncPreCallContext) bindingProvider.getRequestContext();
      asyncPreCallContext.setProperty(WSSecurityContext.CREDENTIAL_PROVIDER_LIST, getCredentialProviderList());
      asyncPreCallContext.setProperty(BindingProvider.USERNAME_PROPERTY, getClxUser());
      asyncPreCallContext.setProperty(BindingProvider.PASSWORD_PROPERTY, getClxPassword());
    }
    catch (Exception e)
    {
      log4jLogger.error("Exception in CLXBaseImportAPI.bindCredentials " + e);
    }
  }

  public IImportWebService getPort()
  {
    return _port;
  }

  public String getEndpoint()
  {
    return (String) ((Stub) getPort())._getProperty(Stub.ENDPOINT_ADDRESS_PROPERTY);
  }

  public void setEndpoint(String endpoint)
  {
    ((Stub) getPort())._setProperty(Stub.ENDPOINT_ADDRESS_PROPERTY, endpoint);
  }

  public List<CredentialProvider> getCredentialProviderList()
  {
    if (_credProviders == null)
      _credProviders = new ArrayList<CredentialProvider>();

    return _credProviders;
  }

  public void addCredentialProvider(CredentialProvider cp)
  {
    getCredentialProviderList().add(cp);
  }

  public void setPortCredentialProviderList()
  {
    ((Stub) getPort())._setProperty(WSSecurityContext.CREDENTIAL_PROVIDER_LIST, getCredentialProviderList());
  }

  public void addUNTCredentialProvider(String username, String password)
  {
    CredentialProvider cp = new ClientUNTCredentialProvider(username.getBytes(), password.getBytes());
    addCredentialProvider(cp);
  }

  public void addBSTCredentialProvider(String clientKeyStore, String clientKeyStorePass, String clientKeyAlias, String clientKeyPass, X509Certificate serverCert) throws Exception
  {
    CredentialProvider cp = new ClientBSTCredentialProvider(clientKeyStore, clientKeyStorePass, clientKeyAlias, clientKeyPass, "JKS", serverCert);
    addCredentialProvider(cp);
  }

  public void setProxyServerInfo(String proxyHost, int proxyPort, String username, String password)
  {
    Proxy p = new Proxy(Proxy.Type.HTTP, new InetSocketAddress(proxyHost, proxyPort));
    HttpTransportInfo info = new HttpTransportInfo();
    info.setProxy(p);

    ((Stub) getPort())._setProperty("weblogic.wsee.connection.transportinfo", info);

    if (username != null)
      ((Stub) getPort())._setProperty("weblogic.webservice.client.proxyusername", username);

    if (password != null)
      ((Stub) getPort())._setProperty("weblogic.webservice.client.proxypassword", password);
  }

  public boolean getMaintainSession()
  {
    return ((Boolean) ((Stub) getPort())._getProperty(Stub.SESSION_MAINTAIN_PROPERTY)).booleanValue();
  }

  public void setMaintainSession(boolean maintainSession)
  {
    ((Stub) getPort())._setProperty(Stub.SESSION_MAINTAIN_PROPERTY, Boolean.valueOf(maintainSession));
  }

  public ReturnValue importDocument(String xmlString) throws RemoteException
  {
    ReturnValue returnValue = new ReturnValue();
    String returnXMLString = importDocument(getClxAccountNumber(), getClxUser(), getClxPassword(), xmlString);
    returnXMLString = returnXMLString.replace("<!DOCTYPE ReturnValue PUBLIC \"ReturnValue\" \"ReturnValue.dtd\" >", "");
    String message = StringUtils.substringBetween(returnXMLString, "<Message>", "</Message>");
    if (message != null)
    {
      returnXMLString = returnXMLString.replace(message, "");
      message = StringUtils.substringBetween(message, "<Description>", "</Description>");
    }
    returnValue = (ReturnValue) unMarshalXmlString(returnXMLString);
    returnValue.setMessage(message);

    return returnValue;
  }

  private Object unMarshalXmlString(String xmlString)
  {
    Object obj = null;
    try
    {
      JAXBContext jaxbContext = JAXBContext.newInstance(ObjectFactory.class.getPackage().getName(), ObjectFactory.class.getClassLoader());
      Unmarshaller jaxbUnMarshaller = jaxbContext.createUnmarshaller();
      obj = jaxbUnMarshaller.unmarshal(new StreamSource(new StringReader(xmlString)));
    }
    catch (Exception e)
    {
      log4jLogger.error(e);
      log4jLogger.error("Unable to unmarshal this xml: " + xmlString);
    }
    return obj;
  }

  public String importDocument(String String_1, String String_2, String String_3, String String_4) throws RemoteException
  {
    return getPort().importDocument(String_1, String_2, String_3, String_4);
  }

  public void importDocumentAsync(AsyncPreCallContext apc, String String_1, String String_2, String String_3, String String_4) throws RemoteException
  {
    getPort().importDocumentAsync(apc, String_1, String_2, String_3, String_4);
  }

  public static String getClxServer()
  {
    return clxServer;
  }

  public static String getClxUser()
  {
    return clxUser;
  }

  public static String getClxPassword()
  {
    return clxPassword;
  }

  public static String getClxAccountNumber()
  {
    return clxAccountNumber;
  }

  public static void setAsyncPreCallContext(AsyncPreCallContext asyncPreCallContext)
  {
    CLXBaseImportAPI.asyncPreCallContext = asyncPreCallContext;
  }

  public static AsyncPreCallContext getAsyncPreCallContext()
  {
    return asyncPreCallContext;
  }

  public static String getClxEndpointURL()
  {
    return clxEndpointURL;
  }
}
