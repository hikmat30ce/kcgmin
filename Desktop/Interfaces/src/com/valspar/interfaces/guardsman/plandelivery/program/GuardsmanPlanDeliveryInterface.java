package com.valspar.interfaces.guardsman.plandelivery.program;

import com.valspar.interfaces.common.BaseInterface;
import com.valspar.interfaces.common.beans.*;
import com.valspar.interfaces.common.enums.DataSource;
import com.valspar.interfaces.common.servlets.PropertiesServlet;
import com.valspar.interfaces.common.utils.*;
import com.valspar.interfaces.guardsman.common.beans.PlanetPressResultBean;
import com.valspar.interfaces.guardsman.plandelivery.beans.PlanBean;
import com.valspar.interfaces.guardsman.plandelivery.dao.GuardsmanPlanDeliveryDAO;
import com.valspar.interfaces.guardsman.plandelivery.enums.ProcessingMode;
import com.valspar.interfaces.guardsman.plandelivery.threads.GuardsmanPlanDeliveryThread;
import commonj.work.Work;
import java.text.*;
import java.util.*;
import oracle.jdbc.OracleConnection;
import org.apache.commons.lang3.StringUtils;
import org.apache.log4j.Logger;

public class GuardsmanPlanDeliveryInterface extends BaseInterface
{
  private static Logger log4jLogger = Logger.getLogger(GuardsmanPlanDeliveryInterface.class);
  private int workerThreadCount = 3;
  private List<PlanBean> plansToProcess = Collections.synchronizedList(new ArrayList<PlanBean>());
  private List<PlanBean> uetaNotifiedPlans = Collections.synchronizedList(new ArrayList<PlanBean>());
  private List<PlanBean> pdfEmailedPlans = Collections.synchronizedList(new ArrayList<PlanBean>());
  private List<PlanBean> plansForInternalPrinting = Collections.synchronizedList(new ArrayList<PlanBean>());
  private List<PlanBean> plansForOutsourcePrinting = Collections.synchronizedList(new ArrayList<PlanBean>());
  private Map<PlanBean, PlanetPressResultBean> erroredPlans = Collections.synchronizedMap(new HashMap<PlanBean, PlanetPressResultBean>());
  private ProcessingMode mode;

  // This interface replaces the following PL/SQL jobs from APLX: sams.sam_fpp_export, sams.sam_fpp_outsource_export

  public GuardsmanPlanDeliveryInterface()
  {
  }

  public void execute()
  {
    OracleConnection conn = null;

    try
    {
      conn = (OracleConnection) ConnectionAccessBean.getConnection(DataSource.GUARDSMAN);

      debug("");
      debug("----------------------------------------------------------------------------------------------------------------------------------------------------------------------");
      debug("START Process new protection plans");
      debug("----------------------------------------------------------------------------------------------------------------------------------------------------------------------");
      debug("");

      String modeString = getParameterValue("MODE");
      
      if (StringUtils.isEmpty(modeString))
      {
        debug("ERROR! MODE parameter was not provided!");
        return;        
      }

      mode = ProcessingMode.valueOf(modeString);
      
      if (mode == null)
      {
        debug("ERROR! Mode not recognized: " + mode);
        return;
      }

      debug("Mode: " + mode);
      processPlans();
      debug("DONE Processing new protection plans");
      
      if (mode == ProcessingMode.FULL)
      {
        debug("  UETA notifications sent:                      " + uetaNotifiedPlans.size());
        debug("  FPP records extracted for internal printing:  " + plansForInternalPrinting.size());
        debug("  FPP records extracted for outsource printing: " + plansForOutsourcePrinting.size());
        debug("  FPP errors:                                   " + erroredPlans.size());

        debug("");
        debug("----------------------------------------------------------------------------------------------------------------------------------------------------------------------");
        debug("START Create Overstock ASN");
        debug("----------------------------------------------------------------------------------------------------------------------------------------------------------------------");
        debug("");
        GuardsmanPlanDeliveryDAO.createOverstockASN();
        debug("DONE Creating Overstock ASN");

        sendCompletionNotification();
      }
      else if (mode == ProcessingMode.PDF_DELIVERY)
      {
        debug("  PDF Plan emails sent: " + pdfEmailedPlans.size());        
      }
    }
    catch (Exception e)
    {
      log4jLogger.error(e);
    }
    finally
    {
      JDBCUtil.close(conn);
    }
  }

  public void debug(String message)
  {
    log4jLogger.info(message);
  }

  private void processPlans()
  {
    debug("Looking for Guardsman plans to process...");
    List<PlanBean> plans = GuardsmanPlanDeliveryDAO.fetchPlans();
    debug("..." + plans.size() + " found");

    debug("Populating addresses...");
    GuardsmanPlanDeliveryDAO.populateAddresses(plans);

    debug("Creating " + workerThreadCount + " workers to process plans...");

    List<Work> workers = new ArrayList<Work>();
    plansToProcess.addAll(plans);

    for (int i = 1; i <= workerThreadCount; i++)
    {
      GuardsmanPlanDeliveryThread worker = new GuardsmanPlanDeliveryThread(getInterfaceInfoBean(), String.valueOf(i), mode);
      worker.setInProduction(isInProduction());
      worker.setPlansToProcess(plansToProcess);
      worker.setUetaNotifiedPlans(uetaNotifiedPlans);
      worker.setPlansForInternalPrinting(plansForInternalPrinting);
      worker.setPlansForOutsorcePrinting(plansForOutsourcePrinting);
      worker.setPdfEmailedPlans(pdfEmailedPlans);
      worker.setErroredPlans(erroredPlans);

      workers.add(worker);
    }

    debug("Starting worker threads...");

    try
    {
      WorkManagerUtility.runParallelAndWait(workers);
    }
    catch (Exception e)
    {
      log4jLogger.error(e);
    }

    debug("Worker threads are done processing plans");

    if (mode == ProcessingMode.FULL)
    {
      DateFormat df = new SimpleDateFormat("yyMMdd_HHmm");
  
      debug("Writing outsource plan file...");
  
      FlatFileUtility outsourcedPlanWriter = new FlatFileUtility(".txt", "\t", "\n");
      outsourcedPlanWriter.setCustomFilename("fpp_pos_" + df.format(new Date()) + "_orig");
      outsourcedPlanWriter.writeLine(PlanBean.getOutsourceFileHeaderFields());
  
      for (PlanBean plan: plansForOutsourcePrinting)
      {
        plan.writeOutsourceRecord(outsourcedPlanWriter);
      }
  
      outsourcedPlanWriter.close();
  
      debug("Writing internal plan file...");
  
      FlatFileUtility internalPlanWriter = new FlatFileUtility(".txt", "\t", "\n");
      internalPlanWriter.setCustomFilename("pw_fpp_" + df.format(new Date()));
      internalPlanWriter.writeLine(PlanBean.getInternalFileHeaderFields());
  
      for (PlanBean plan: plansForInternalPrinting)
      {
        plan.writeInternalRecord(internalPlanWriter);
      }
  
      internalPlanWriter.close();      

      String serverName = PropertiesServlet.getProperty("guardsmanplandelivery.ftpServer");
      String destinationDirectory = PropertiesServlet.getProperty("guardsmanplandelivery.ftpPath");

      if (!plansForInternalPrinting.isEmpty())
      {
        log4jLogger.info("FTP sending internal plan file " + internalPlanWriter.getFileWritePath() + " to " + serverName + ":" + destinationDirectory);
        FtpUtility.sendFileOrDirectory(serverName, "autoapps", PropertiesServlet.getProperty("autoapps.password"), internalPlanWriter.getFileWritePath(), destinationDirectory, null);
      }

      if (!plansForOutsourcePrinting.isEmpty())
      {
        log4jLogger.info("FTP sending outsource plan file " + outsourcedPlanWriter.getFileWritePath() + " to " + serverName + ":" + destinationDirectory);
        FtpUtility.sendFileOrDirectory(serverName, "autoapps", PropertiesServlet.getProperty("autoapps.password"), outsourcedPlanWriter.getFileWritePath(), destinationDirectory, null);
      }
    }

    if (!erroredPlans.isEmpty())
    {
      sendErrorsNotification();
    }
  }

  private boolean sendErrorsNotification()
  {
    SimpleUserBean from = new SimpleUserBean();
    from.setFullName("Guardsman FPP Delivery Interface");
    from.setEmail("interfaces@valspar.com");

    Map<String, Object> values = new HashMap<String, Object>();
    values.put("errors", erroredPlans);

    SimpleUserBean recipient = new SimpleUserBean();
    recipient.setFullName("Guardsman FPP Delivery Admins");
    recipient.setEmail("protectionplans@guardsman.com");

    if (!isInProduction())
    {
      recipient.setEmail("rmurphy2@valspar.com");
    }

    if (!NotificationUtility.sendNotification(recipient, from, "Guardsman FPP plan emailing errors", "guardsmanplandelivery-errors.ftl", values, null))
    {
      log4jLogger.error("Unable to send plan errors notification");
      return false;
    }
    
    return true;
  }

  private boolean sendCompletionNotification()
  {
    SimpleUserBean from = new SimpleUserBean();
    from.setFullName("Guardsman FPP Delivery Interface");
    from.setEmail("interfaces@valspar.com");

    Map<String, Object> values = new HashMap<String, Object>();
    values.put("uetaNotificationsEmailed", uetaNotifiedPlans.size());
    values.put("recordCountInternal", plansForInternalPrinting.size());
    values.put("recordCountOutsource", plansForOutsourcePrinting.size());
    values.put("errorCount", erroredPlans.size());

    SimpleUserBean recipient = new SimpleUserBean();
    recipient.setFullName("Guardsman FPP Delivery Admins");

    if (isInProduction())
    {
      recipient.setEmail("GUARDSMANFPP@valspar.com");
    }
    else
    {
      recipient.setEmail("rmurphy2@valspar.com");
    }

    if (!NotificationUtility.sendNotification(recipient, from, "Guardsman FPP Delivery Interface has completed", "guardsmanplandelivery-completion_notification.ftl", values, null))
    {
      log4jLogger.error("Unable to send completion notification");
      return false;
    }

    return true;
  }
}
