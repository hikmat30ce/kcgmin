package com.valspar.interfaces.common.servlets;

import com.valspar.interfaces.common.hibernate.HibernateUtil;
import com.valspar.interfaces.common.quartz.ValsparQuartzScheduler;
import com.valspar.interfaces.common.quartz.beans.ScheduleBean;
import com.valspar.interfaces.common.utils.*;
import java.io.*;
import java.util.*;
import javax.servlet.*;
import javax.servlet.http.*;
import org.apache.commons.lang.StringUtils;
import org.apache.log4j.*;

public class StartUpServlet extends HttpServlet
{
  private static final String CONTENT_TYPE = "text/html; charset=UTF-8";
  private static Logger log4jLogger = Logger.getLogger(StartUpServlet.class);

  public void init(ServletConfig config) throws ServletException
  {
    super.init(config);
    String fileLocation = null;

    try
    {
      if (StringUtils.contains(System.getProperty("os.name"), "Windows"))
      {
        fileLocation = "C:\\properties\\";
      }
      else
      {
        fileLocation = "/com/valspar/soa_weblogic_11g/interfaces/conf/";
      }
      PropertyConfigurator.configureAndWatch(fileLocation + "interfaces_log4j.properties");
      log4jLogger.info("Starting Interfaces...");

      HibernateUtil.initialize();

      ValsparQuartzScheduler.setUpScheduler();
      ValsparQuartzScheduler.startScheduler();

      NotificationUtility.initialize(config.getServletContext());
    }
    catch (Exception e)
    {
      log4jLogger.error(e);
    }
  }

  public void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException
  {
    response.setContentType(CONTENT_TYPE);

    Map<String, Object> values = new HashMap<String, Object>();
    renderPage(response.getWriter(), values);
  }

  public void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException
  {
    try
    {
      response.setContentType(CONTENT_TYPE);

      Map<String, Object> values = new HashMap<String, Object>();
      String jobKey = request.getParameter("JOB_KEY");

      if (StringUtils.isNotEmpty(jobKey))
      {
        values.put("jobKey", jobKey);
      }

      if (StringUtils.isNotEmpty(request.getParameter("REFRESH_SCHEDULER")))
      {
        ValsparQuartzScheduler.setUpScheduler();
        values.put("message", new Date() + ": Scheduler refreshed.");
      }
      else if (StringUtils.isNotEmpty(request.getParameter("RUN_INTERFACE")))
      {
        String jobGroup = "";

        for (ScheduleBean sbean: ValsparQuartzScheduler.buildScheduleBeans())
        {
          if (sbean.getJobKey().equalsIgnoreCase(jobKey))
          {
            jobGroup = sbean.getJobGroup();
          }
        }

        if (jobKey != null && jobGroup != null)
        {
          PropertiesServlet.reloadProperties();
          ValsparQuartzScheduler.runJob(jobKey, jobGroup);
          values.put("message", new Date() + ":  " + jobKey + " has been started.");
        }
      }

      renderPage(response.getWriter(), values);
    }
    catch (Exception e)
    {
      log4jLogger.error(e);
    }
  }

  public void renderPage(PrintWriter out, Map<String, Object> values)
  {
    try
    {
      values.put("interfaces", ValsparQuartzScheduler.buildScheduleBeans());
      values.put("dbStatusBeans", ConnectionUtility.buildAllDBStatusBeans());
      values.put("runningJobs", ValsparQuartzScheduler.getScheduler().getCurrentlyExecutingJobs());

      String output = NotificationUtility.buildMessage("control-panel.ftl", values);
      out.write(output);
      out.close();
    }
    catch (Exception e)
    {
      log4jLogger.error(e);
    }
  }
}
