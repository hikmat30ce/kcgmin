package com.valspar.interfaces.guardsman.techportalusersync.utility;

import com.valspar.interfaces.common.servlets.PropertiesServlet;
import com.valspar.interfaces.guardsman.techportalusersync.beans.TechUserBean;
import java.util.Hashtable;
import javax.naming.*;
import javax.naming.directory.*;
import javax.naming.ldap.InitialLdapContext;
import org.apache.commons.lang3.StringUtils;
import org.apache.log4j.*;

public class OIDUtility
{
  private static Logger log4jLogger = Logger.getLogger(OIDUtility.class);

  private static String HOST = PropertiesServlet.getProperty("guardsmantechportalusers.oidServer");
  private static String PORT = PropertiesServlet.getProperty("guardsmantechportalusers.oidPort");
  private static String BASE_DN = PropertiesServlet.getProperty("guardsmantechportalusers.baseDN");
  private static final String OID_USERNAME = PropertiesServlet.getProperty("guardsmantechportalusers.oidUsername");
  private static final String OID_PASSWORD = PropertiesServlet.getProperty("guardsmantechportalusers.oidPassword");

  public static void main(String[] args)
  {
    PropertyConfigurator.configureAndWatch("C:\\properties\\interfaces_log4j.properties");

    TechUserBean user = new TechUserBean();
    user.setUserName("cmsoxf1");
    user.setEmail("cms.tester1@valspar.com");
    user.setFullName("James Ballywho");
    user.setFirstName("James");
    user.setLastName("Ballywho");
    user.setPassword("tucows2");

    OIDUtility.addUser(user);
    //OIDUtility.updateUser(user, "13cows");
    //OIDUtility.removeUser(user);
    /*
    String dn = findUser(user);

    if (StringUtils.isEmpty(dn))
    {
      log4jLogger.error("User " + user.getUserName() + " not found!");
    }
    else
    {
      log4jLogger.info("Found user, dn=" + dn);
    }
*/
  }

  public static boolean syncUser(TechUserBean user)
  {
    try
    {
      String dn = findUser(user);
  
      if (StringUtils.isNotEmpty(dn))
      {
        if (user.isExpired())
        {
          return removeUser(user);
        }
        else
        {
          return updateUser(user);
        }
      }
      else if (!user.isExpired())
      {
        return addUser(user);
      }
  
      log4jLogger.info("No action taken");
    }
    catch (Exception e)
    {
      log4jLogger.error(e);
    }

    return false;
  }

  private static boolean addUser(TechUserBean user)
  {
    InitialLdapContext conn = null;

    try
    {
      if (StringUtils.isEmpty(user.getUserName()))
      {
        log4jLogger.error("Username cannot be empty!");
        return false;
      }

      String dn = findUser(user);
      if (StringUtils.isNotEmpty(dn))
      {
        log4jLogger.error("User " + user.getUserName() + " already exists!");
        return false;
      }

      dn = "uid=" + user.getUserName() + ", ou=People, " + BASE_DN;

      Attributes attrs = buildAttributes(user);

      Attribute objectClass = new BasicAttribute("objectclass");

      String[] objectClassList = new String[] { "inetOrgPerson", "organizationalPerson", "orclUser", "pilotObject", "person", "ValsparPeople", "top" };

      for (String objectClassValue: objectClassList)
      {
        objectClass.add(objectClassValue);
      }
      attrs.put(objectClass);

      conn = getOIDConnection();

      log4jLogger.info("Adding user " + user.getUserName() + " (" + user.getFullName() + ")");
      conn.createSubcontext(dn, attrs);

      log4jLogger.info("SUCCESS!");
      return true;
    }
    catch (Exception e)
    {
      log4jLogger.error(e);
    }
    finally
    {
      closeLdapConnection(conn);
    }

    return false;
  }

  private static boolean updateUser(TechUserBean user)
  {
    InitialLdapContext conn = null;

    try
    {
      String dn = findUser(user);

      if (StringUtils.isEmpty(dn))
      {
        log4jLogger.error("User " + user.getUserName() + " does not exist!");
        return false;
      }

      Attributes attributes = buildAttributes(user);

      conn = getOIDConnection();

      log4jLogger.info("Updating user " + user.getUserName() + " (" + user.getFullName() + ")");
      conn.modifyAttributes(dn, DirContext.REPLACE_ATTRIBUTE, attributes);

      log4jLogger.info("SUCCESS!");
      return true;
    }
    catch (Exception e)
    {
      log4jLogger.error(e);
    }
    finally
    {
      closeLdapConnection(conn);
    }

    return false;
  }

  private static boolean removeUser(TechUserBean user)
  {
    InitialDirContext conn = null;

    try
    {
      String dn = findUser(user);

      if (StringUtils.isEmpty(dn))
      {
        log4jLogger.error("User " + user.getUserName() + " not found!");
        return false;
      }

      conn = getOIDConnection();

      log4jLogger.info("Deleting User " + user.getUserName());
      conn.destroySubcontext(dn);

      log4jLogger.info("SUCCESS!");
      return true;
    }
    catch (Exception e)
    {
      log4jLogger.error(e);
    }
    finally
    {
      closeLdapConnection(conn);
    }

    return false;
  }

  private static Attributes buildAttributes(TechUserBean user)
  {
    Attributes attrs = new BasicAttributes();

    attrs.put("cn", user.getFullName());
    attrs.put("employeetype", "external");
    attrs.put("givenname", user.getFirstName());
    attrs.put("initials", StringUtils.lowerCase(StringUtils.substring(user.getFirstName(), 0, 1) + StringUtils.substring(user.getLastName(), 0, 1)));
    attrs.put("mail", user.getEmail());
    attrs.put("sn", user.getLastName());
    attrs.put("uid", user.getUserName());
    attrs.put("nsroledn", "cn=FSG_CLAIM, ou=Furniture Solutions Group, " + BASE_DN);
    attrs.put("userpassword", user.getPassword());

    return attrs;
  }

  private static String findUser(TechUserBean user)
  {
    String[] attributeNames = { "uid", "nsroledn" };

    SearchControls controls = new SearchControls();
    controls.setSearchScope(SearchControls.SUBTREE_SCOPE);
    controls.setReturningAttributes(attributeNames);

    InitialLdapContext conn = null;
    NamingEnumeration<SearchResult> results = null;

    try
    {
      conn = getOIDConnection();
      results = conn.search("ou=People," + BASE_DN, "uid=" + user.getUserName(), controls);

      if (results.hasMore())
      {
        SearchResult searchResult = results.next();
        String nsRole = null;

        Attributes attributes = searchResult.getAttributes();

        Attribute attribute = attributes.get("nsroledn");
        if (attribute != null)
        {
          nsRole = (String) attribute.get();
        }

        if (!StringUtils.equalsIgnoreCase(nsRole, "cn=FSG_CLAIM, ou=Furniture Solutions Group, " + BASE_DN))
        {
          throw new RuntimeException("Username " + user.getUserName() + " is already taken!");
        }

        return searchResult.getNameInNamespace();
      }
    }
    catch (NamingException e)
    {
      log4jLogger.error(e);
    }
    finally
    {
      closeLdapConnection(conn);
    }

    return null;
  }

  private static InitialLdapContext getOIDConnection()
  {
    return getConnection(HOST, PORT, OID_USERNAME, OID_PASSWORD, true);
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
}
