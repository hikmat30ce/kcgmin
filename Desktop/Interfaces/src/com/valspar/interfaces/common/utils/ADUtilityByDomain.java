package com.valspar.interfaces.common.utils;

import com.valspar.interfaces.common.enums.Domains;
import java.util.*;
import javax.naming.*;
import javax.naming.ldap.*;
import org.apache.log4j.*;

public class ADUtilityByDomain
{
  private static Logger log4jLogger = Logger.getLogger(ADUtility.class);

  public static InitialLdapContext getADConnection(Domains domain)
  {
    return getConnection(domain, "CN=ADSYNCSERVICE,OU=Admin Users & Groups,OU=IT,OU=Minneapolis,OU=North America,DC=corporate,DC=root,DC=corp", "?2012workday", true);
  }

  private static InitialLdapContext getConnection(Domains domain, String username, String password, boolean logErrors)
  {
    InitialLdapContext ctx = null;

    try
    {
      Hashtable env = new Hashtable();
      env.put(Context.INITIAL_CONTEXT_FACTORY, "com.sun.jndi.ldap.LdapCtxFactory");
      env.put(Context.SECURITY_AUTHENTICATION, "simple");
      env.put(Context.SECURITY_PRINCIPAL, username);
      env.put(Context.SECURITY_CREDENTIALS, password);
      env.put(Context.PROVIDER_URL, "ldap://" + domain.getServer() + ":" + domain.getPort() + "/" + domain.getSearchPath());
      env.put(Context.REFERRAL, "follow");
      env.put("java.naming.ldap.attributes.binary", "jpegPhoto");

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

  public static void closeLdapConnection(Context context)
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

  public static void closeAllLdapConnections(Map<String, InitialLdapContext> connectionMap)
  {
    Iterator i = connectionMap.keySet().iterator();
    while (i.hasNext())
    {
      String key = (String)i.next();
      InitialLdapContext ctx = connectionMap.get(key);
      try
      {
        if (ctx != null)
        {
          ctx.close();
        }
      }
      catch (Exception e)
      {
        log4jLogger.error(e);
      }
    }
  }

}