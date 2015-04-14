package com.valspar.interfaces.common.utils;

import com.valspar.interfaces.common.beans.SimpleUserBean;
import java.util.Hashtable;
import javax.naming.*;
import javax.naming.directory.*;
import javax.naming.ldap.InitialLdapContext;
import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;

public final class ADUtility
{
  private static Logger log4jLogger = Logger.getLogger(ADUtility.class);

  private static final String AD_GC_SERVER_NAME = "globalcat.corporate.root.corp";
  private static final String AD_GC_SERVER_PORT = "3268";
  private static final String AD_GC_SEARCH_PATH = "DC=root,DC=corp";
  private static final String AD_USERNAME = "CN=NETEGRITY LDAP USER LOGIN,OU=Admin Users & Groups,OU=IT,OU=Minneapolis,OU=North America,DC=corporate,DC=root,DC=corp";
  private static final String AD_PASSWORD = "?2000net";
  private static final String[] USER_ATTRIBUTES = { "givenName", "sn", "mail", "sAMAccountName" };

  public static SimpleUserBean buildSimpleUserBeanFromUserName(String userName)
  {
    SimpleUserBean simpleUserBean = null;
    SearchControls controls = new SearchControls();
    controls.setSearchScope(SearchControls.SUBTREE_SCOPE);
    controls.setReturningAttributes(USER_ATTRIBUTES);

    InitialLdapContext context = null;
    NamingEnumeration<SearchResult> results = null;

    try
    {
      context = getADConnection();
      results = context.search(AD_GC_SEARCH_PATH, "(&(objectCategory=person)(objectClass=user)(sAMAccountName=" + userName + "))", controls);

      if (results.hasMore())
      {
        SearchResult sr = results.next();
        simpleUserBean = buildSimpleUserBean(sr.getAttributes());
      }
    }
    catch (Exception e)
    {
      log4jLogger.error(e);
    }
    finally
    {
      closeLdapConnection(context);
      close(results);
    }

    return simpleUserBean;
  }

  private static SimpleUserBean buildSimpleUserBean(Attributes attrs) throws NamingException
  {
    SimpleUserBean simpleUserBean = null;

    if (attrs != null)
    {
      simpleUserBean = new SimpleUserBean();
      Attribute firstNameAttribute = attrs.get("givenName");
      Attribute lastNameAttribute = attrs.get("sn");
      Attribute emailAttribute = attrs.get("mail");
      Attribute usernameAttribute = attrs.get("sAMAccountName");

      if (firstNameAttribute != null)
      {
        simpleUserBean.setFirstName(StringUtils.capitalize(StringUtils.lowerCase((String) firstNameAttribute.get())));
      }
      if (lastNameAttribute != null)
      {
        simpleUserBean.setLastName(StringUtils.capitalize(StringUtils.lowerCase((String) lastNameAttribute.get())));
      }
      if (emailAttribute != null)
      {
        simpleUserBean.setEmail((String) emailAttribute.get());
      }
      if (usernameAttribute != null)
      {
        simpleUserBean.setUserName(StringUtils.lowerCase((String) usernameAttribute.get()));
      }
    }

    return simpleUserBean;
  }

  private static InitialLdapContext getADConnection()
  {
    return getConnection(AD_GC_SERVER_NAME, AD_GC_SERVER_PORT, AD_USERNAME, AD_PASSWORD, true);
  }

  private static InitialLdapContext getConnection(String server, String port, String username, String password, boolean logErrors)
  {
    InitialLdapContext ctx = null;

    try
    {
      Hashtable env = new Hashtable();
      env.put(Context.INITIAL_CONTEXT_FACTORY, "com.sun.jndi.ldap.LdapCtxFactory");
      env.put(Context.SECURITY_AUTHENTICATION, "simple");
      env.put(Context.SECURITY_PRINCIPAL, username);
      env.put(Context.SECURITY_CREDENTIALS, password);
      env.put(Context.PROVIDER_URL, "ldap://" + server + ":" + port);
      env.put(Context.REFERRAL, "follow");
      env.put("java.naming.ldap.attributes.binary", "tokenGroups");

      ctx = new InitialLdapContext(env, null);
    }
    catch (Exception e)
    {
      if (logErrors)
      {
        log4jLogger.error(e);
      }
      else
      {
        /*  Don't put entries in the log for this exception, these are
         *    all user errors of some type, not system errors:
            525     user not found
            52e     invalid credentials
            530     not permitted to logon at this time
            531     not permitted to logon at this workstation
            532     password expired
            533     account disabled
            701     account expired
            773     user must reset password
            775     user account locked
         */
        ;
      }
    }
    return ctx;
  }

  private static void closeLdapConnection(Context context)
  {
    try
    {
      if (context != null)
      {
        context.close();
      }
    }
    catch (Exception e)
    {
      log4jLogger.error(e);
    }
  }

  private static void close(NamingEnumeration ne)
  {
    try
    {
      if (ne != null)
      {
        ne.close();
      }
    }
    catch (Exception e)
    {
      log4jLogger.error(e);
    }
  }
}
