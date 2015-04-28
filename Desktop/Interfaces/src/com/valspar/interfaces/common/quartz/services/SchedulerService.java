package com.valspar.interfaces.common.quartz.services;

import com.valspar.interfaces.common.quartz.ValsparQuartzScheduler;
import com.valspar.interfaces.common.quartz.beans.ScheduleBean;
import java.util.List;
import javax.ws.rs.*;
import org.apache.log4j.Logger;

@Path("/scheduler")
public class SchedulerService
{
  private static Logger log4jLogger = Logger.getLogger(SchedulerService.class);

  @GET
  @Path("jobs")
  @Produces("application/json")
  public List<ScheduleBean> getJobs()
  {
    return ValsparQuartzScheduler.buildScheduleBeans();
  }

  @POST
  @Path("runjob")
  public void runJob(@FormParam("jobkey")
    String jobKey, @FormParam("jobgroup")
    String jobGroup)
  {
    try
    {
      log4jLogger.info("Running job (key=" + jobKey + ", group=" + jobGroup + ")...");
      ValsparQuartzScheduler.runJob(jobKey, jobGroup);
    }
    catch (Exception e)
    {
      log4jLogger.error(e);
    }
  }

  @POST
  @Path("refresh")
  public void refresh()
  {
    try
    {
      log4jLogger.info("Refreshing Quartz Scheduler...");
      ValsparQuartzScheduler.setUpScheduler();
    }
    catch (Exception e)
    {
      log4jLogger.error(e);
    }
  }

  @POST
  @Path("validate")
  @Produces("application/json")
  public String validate(@FormParam("classname") String classname)
  {
    try
    {
      Class c = Class.forName(classname);

      Object o = c.newInstance();

      if (o == null)
      {
        return "The class you specified, " + classname + " could not be instantiated.  Please verify your entry.";
      }
      else
      {
        if (!(o instanceof org.quartz.Job))
        {
          return "The class you specified, " + classname + " was found but does not implement the org.quartz.Job interface so it cannot be scheduled.";
        }
      }

      return "SUCCESS";
    }
    catch (ClassNotFoundException e)
    {
      return "The class you specified, " + classname + " could not be found.  Please verify your entry.";
    }
    catch (Exception e)
    {
      return "The class you specified, " + classname + " could not be instantiated.  Please verify your entry.";
    }
  }
}
