package com.valspar.interfaces.common.quartz;

import com.valspar.interfaces.common.enums.DataSource;
import com.valspar.interfaces.common.hibernate.HibernateUtil;
import com.valspar.interfaces.common.quartz.beans.ParameterBean;
import com.valspar.interfaces.common.quartz.beans.ScheduleBean;
import com.valspar.interfaces.common.quartz.beans.ScheduleCalendarBean;
import com.valspar.interfaces.common.servlets.PropertiesServlet;
import java.util.*;
import org.apache.commons.lang3.*;
import org.apache.log4j.Logger;
import org.hibernate.Query;
import org.hibernate.Session;
import static org.quartz.CronScheduleBuilder.cronSchedule;
import static org.quartz.JobBuilder.newJob;
import org.quartz.*;
import static org.quartz.TriggerBuilder.newTrigger;
import org.quartz.impl.StdSchedulerFactory;

public class ValsparQuartzScheduler
{
  private static Scheduler scheduler;
  private static Logger log4jLogger = Logger.getLogger(ValsparQuartzScheduler.class);

  public ValsparQuartzScheduler()
  {
  }

  public static void setUpScheduler()
  {
    try
    {
      Scheduler scheduler = getScheduler();
      scheduler.clear();

      for (ScheduleBean scheduleBean: buildScheduleBeans())
      {
        String cronEval = null;
        try
        {
          Class c = Class.forName(scheduleBean.getClassName());
          JobDetail job = newJob(c).withIdentity(scheduleBean.getJobKey(), scheduleBean.getJobGroup()).storeDurably().build();
          for (ParameterBean parameterBean: scheduleBean.getParametersAsList())
          {
            job.getJobDataMap().put(parameterBean.getParameterName(), parameterBean.getParameterValue());
          }

          ScheduleCalendarBean scheduleCalendarBean = scheduleBean.getNextScheduleCalendar();
          if (scheduleCalendarBean != null)
          {
            cronEval = scheduleCalendarBean.getCronExpression();
          }
          else
          {
            cronEval = scheduleBean.getCronExpression();
          }
          if (scheduleBean.isRunnableInEnvironment())
          {
            if (cronEval == null)
            {
              log4jLogger.error("No Schedule for Job Key " + scheduleBean.getJobKey());
              scheduler.addJob(job, false);
            }
            else
            {
              Trigger trigger = newTrigger().withIdentity(scheduleBean.getTriggerKey(), scheduleBean.getTriggerGroup()).withSchedule(cronSchedule(cronEval)).build();
              scheduler.scheduleJob(job, trigger);
            }
          }
          else
          {
            scheduler.addJob(job, false);
          }
        }
        catch (Exception e)
        {
          log4jLogger.error(e);
        }
      }
    }
    catch (Exception e)
    {
      log4jLogger.error(e);
    }
  }

  public static void startScheduler()
  {
    try
    {
      getScheduler().start();
    }
    catch (Exception e)
    {

      log4jLogger.error(e);
    }
  }

  public static List<ScheduleBean> buildScheduleBeans()
  {
    List<ScheduleBean> scheduleList = new ArrayList<ScheduleBean>();
    Session session = HibernateUtil.getHibernateSession(DataSource.MIDDLEWARE);

    StringBuilder sb = new StringBuilder();
    sb.append("SELECT scheduleBean ");
    sb.append("FROM ScheduleBean scheduleBean ");
    sb.append("WHERE scheduleBean.applicationBean.application = :APPLICATION ");
    sb.append("  AND scheduleBean.activeFlag = true ");
    sb.append("ORDER BY scheduleBean.jobKey ");

    Query query = session.createQuery(sb.toString());
    query.setParameter("APPLICATION", "INTERFACES");

    scheduleList = query.list();
    session.close();
    return scheduleList;
  }

  public static void runJob(String jobKey, String jobGroup) throws SchedulerException
  {
    JobKey jk = new JobKey(jobKey, jobGroup);
    ValsparQuartzScheduler.getScheduler().triggerJob(jk);
  }

  public static void setScheduler(Scheduler scheduler)
  {
    ValsparQuartzScheduler.scheduler = scheduler;
  }

  public static Scheduler getScheduler()
  {
    if (scheduler == null)
    {
      try
      {
        SchedulerFactory schedulerFactory = new StdSchedulerFactory();
        setScheduler(schedulerFactory.getScheduler());
      }
      catch (Exception e)
      {
        log4jLogger.error(e);
      }
    }
    return scheduler;
  }
}
