package com.valspar.interfaces.common.utils;

import com.valspar.interfaces.common.interfaces.ValsparWork;
import commonj.work.*;
import java.util.*;
import javax.naming.InitialContext;
import org.apache.log4j.Logger;

public class WorkManagerUtility
{
  private static Logger log4jLogger = Logger.getLogger(WorkManagerUtility.class);
  private static WorkManager workManager = null;
  private static Map<WorkItem, Work> activeWork = new HashMap<WorkItem, Work>();

  private WorkManagerUtility()
  {
  }

  public static WorkManager getWorkManager()
  {
    if (workManager == null)
    {
      try
      {
        InitialContext ctx = new InitialContext();
        workManager = (WorkManager) ctx.lookup("java:comp/env/wm/interfacesworkmanager");
      }
      catch (Exception e)
      {
        log4jLogger.error(e);
      }
    }

    return workManager;
  }

  private static void pruneActiveWork()
  {
    Iterator<Map.Entry<WorkItem, Work>> i = activeWork.entrySet().iterator();

    while (i.hasNext())
    {
      Map.Entry<WorkItem, Work> entry = i.next();
      WorkItem workItem = entry.getKey();
      if (workItem.getStatus() == WorkEvent.WORK_COMPLETED || workItem.getStatus() == WorkEvent.WORK_REJECTED)
      {
        i.remove();
      }
    }
  }

  private static void addWorkToList(WorkItem workItem, Work work)
  {
    if (work instanceof ValsparWork)
    {
      ((ValsparWork)work).setStartTime(new Date());
    }

    synchronized (activeWork)
    {
      activeWork.put(workItem, work);
      pruneActiveWork();
    }
  }

  public static List<Work> getActiveWork()
  {
    synchronized (activeWork)
    {
      pruneActiveWork();

      List<Work> ar = new ArrayList<Work>();
      ar.addAll(activeWork.values());

      return ar;
    }
  }

  public static WorkItem startAsync(Work work)
  {
    WorkItem workItem = null;

    try
    {
      workItem = getWorkManager().schedule(work);

      if (workItem.getStatus() == WorkEvent.WORK_ACCEPTED || workItem.getStatus() == WorkEvent.WORK_STARTED)
      {
        addWorkToList(workItem, work);
      }
    }
    catch (Exception e)
    {
      log4jLogger.error(e);
    }

    return workItem;
  }

  public static boolean tryRunInteractive(WorkItem workItem) throws InterruptedException
  {
    List<WorkItem> ar = new ArrayList<WorkItem>();
    ar.add(workItem);

    return getWorkManager().waitForAll(ar, 270000); // 4.5 minutes
  }

  public static void waitForWorkIndefinite(List<WorkItem> workItems) throws InterruptedException
  {
    WorkManager wm = WorkManagerUtility.getWorkManager();
    wm.waitForAll(workItems, WorkManager.INDEFINITE);
  }

  public static void runParallelAndWait(List<? extends Work> workList) throws InterruptedException
  {
    List<WorkItem> workItems = new ArrayList<WorkItem>();

    for (Work work: workList)
    {
      WorkItem workItem = startAsync(work);

      if (workItem != null)
      {
        workItems.add(workItem);
      }
    }

    if (!workItems.isEmpty())
    {
      waitForWorkIndefinite(workItems);
    }
  }
}
