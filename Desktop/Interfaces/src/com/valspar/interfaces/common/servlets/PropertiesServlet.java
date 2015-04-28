package com.valspar.interfaces.common.servlets;

import com.valspar.interfaces.common.utils.CommonUtility;
import java.io.*;
import java.util.Properties;
import javax.servlet.*;
import javax.servlet.http.*;
import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;

public class PropertiesServlet extends HttpServlet
{
  private static Properties properties;
  private static Properties secureProperties;
  private static Logger log4jLogger = Logger.getLogger(PropertiesServlet.class);

  public PropertiesServlet()
  {
    super();
  }

  public void init(ServletConfig servletconfig) throws ServletException
  {
    super.init(servletconfig);
    reloadProperties();
  }

  public void service(HttpServletRequest request, HttpServletResponse response) throws ServletException, java.io.IOException
  {
    reloadProperties();
    return;
  }

  public static void reloadProperties()
  {
    log4jLogger.info("Loading properties");
    FileInputStream fis = null;

    try
    {
      String fileLocation = null;

      if (StringUtils.contains(System.getProperty("os.name"), "Windows"))
      {
        fileLocation = "C:\\properties";
      }
      else
      {
        fileLocation = "/com/valspar/soa_weblogic_11g/interfaces/conf";
      }

      properties = new Properties();

      File f = new File(fileLocation, "interfaces.properties");
      if (f.exists())
      {
        fis = new FileInputStream(f);
        properties.load(fis);
        CommonUtility.close(fis);
      }

      secureProperties = new Properties();

      f = new File(fileLocation, "interfaces_secure.properties");
      if (f.exists())
      {
        fis = new FileInputStream(f);
        secureProperties.load(fis);
        CommonUtility.close(fis);
      }
    }
    catch (Exception e)
    {
      log4jLogger.error(e);
    }
    finally
    {
      CommonUtility.close(fis);
    }
  }

  public static String getProperty(String key)
  {
    if (properties.containsKey(key))
    {
      return properties.getProperty(key);
    }
    else if (secureProperties.containsKey(key))
    {
      return secureProperties.getProperty(key);
    }

    return null;
  }
  public static boolean isProduction()
  {
    return org.apache.commons.lang3.StringUtils.equalsIgnoreCase(PropertiesServlet.getProperty("environment"), "production");
  }

}
