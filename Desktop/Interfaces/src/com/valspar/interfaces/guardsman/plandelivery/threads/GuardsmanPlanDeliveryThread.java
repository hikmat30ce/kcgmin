package com.valspar.interfaces.guardsman.plandelivery.threads;

import com.valspar.interfaces.common.beans.*;
import com.valspar.interfaces.common.utils.*;
import com.valspar.interfaces.common.utils.InterfaceThreadManager;
import com.valspar.interfaces.guardsman.common.beans.*;
import com.valspar.interfaces.guardsman.common.utility.PlanetPressUtility;
import com.valspar.interfaces.guardsman.plandelivery.beans.PlanBean;
import com.valspar.interfaces.guardsman.plandelivery.dao.GuardsmanPlanDeliveryDAO;
import com.valspar.interfaces.guardsman.plandelivery.enums.*;
import commonj.work.Work;
import java.io.File;
import java.util.*;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.mail.EmailAttachment;
import org.apache.log4j.Logger;

public class GuardsmanPlanDeliveryThread implements Work
{
  private static Logger log4jLogger = Logger.getLogger(GuardsmanPlanDeliveryThread.class);

  private InterfaceInfoBean interfaceInfo;
  private String workerId;
  private ProcessingMode mode;

  private boolean inProduction;
  private List<PlanBean> plansToProcess;
  private List<PlanBean> uetaNotifiedPlans;
  private List<PlanBean> plansForInternalPrinting;
  private List<PlanBean> plansForOutsourcePrinting;
  private List<PlanBean> pdfEmailedPlans;
  private Map<PlanBean, PlanetPressResultBean> erroredPlans;

  private int processCount = 0;

  public GuardsmanPlanDeliveryThread(InterfaceInfoBean interfaceInfo, String workerId, ProcessingMode mode)
  {
    this.interfaceInfo = interfaceInfo;
    this.workerId = workerId;
    this.mode = mode;
  }

  public void run()
  {
    InterfaceThreadManager.addInterfaceThread(interfaceInfo);

    try
    {
      while (true)
      {
        boolean success = false;
        PlanBean plan = null;

        try
        {
          plan = plansToProcess.remove(0);
        }
        catch (IndexOutOfBoundsException e)
        {
          break;
        }

        processCount++;

        try
        {
          if (mode == ProcessingMode.PDF_DELIVERY)
          {
            if (plan.shouldEmailPlanPdf())
            {
              PlanetPressResultBean ppResult = generatePDF(plan);

              success = ppResult.isSuccess();
              long length = -1;

              if (success)
              {
                File f = new File(plan.getPlanPdfFilePath());

                if (f.exists())
                {
                  length = f.length();

                  // plan has to be bigger than 20kb
                  if (length < 20 * 1024)
                  {
                    success = false;
                    ppResult.setErrorMessage("PDF generated but appears to be incomplete");
                  }
                }
                else
                {
                  success = false;
                  ppResult.setErrorMessage("PlanetPress returned success but PDF was not found");
                }
              }

              if (success)
              {
                success = emailPlan(plan);

                if (!success)
                {
                  ppResult.setErrorMessage("Unable to send plan PDF email to recipient");
                }
              }

              if (success)
              {
                pdfEmailedPlans.add(plan);
                plan.setActionTaken(PlanActionTaken.PDF_EMAILED);
              }
              else
              {
                erroredPlans.put(plan, ppResult);
              }
            }
          }
          else if (mode == ProcessingMode.FULL)
          {
            if (plan.shouldEmailUetaNotice())
            {
              success = emailUetaNotice(plan);
              PlanetPressResultBean ppResult = null;

              if (!success)
              {
                ppResult = new PlanetPressResultBean();
                ppResult.setErrorMessage("Unable to send UETA notification email to recipient");
              }

              if (success)
              {
                uetaNotifiedPlans.add(plan);
                plan.setActionTaken(PlanActionTaken.UETA_EMAILED);
              }
              else
              {
                erroredPlans.put(plan, ppResult);
              }
            }
            else if (plan.shouldPrint())
            {
              plan.setActionTaken(PlanActionTaken.PRINTED);

              if (plan.isOutsourced())
              {
                plansForOutsourcePrinting.add(plan);
                success = true;
              }
              else
              {
                plansForInternalPrinting.add(plan);
                success = true;
              }
            }
          }
        }
        catch (Exception e)
        {
          log4jLogger.error(e);
          success = false;
        }

        if (success)
        {
          GuardsmanPlanDeliveryDAO.updateApplix(plan);
        }
      }
    }
    catch (Exception e)
    {
      log4jLogger.error(e);
    }
    finally
    {
      InterfaceThreadManager.removeInterfaceThread();
    }
  }

  private PlanetPressResultBean generatePDF(PlanBean plan)
  {
    plan.setPlanPdfFilePath(CommonUtility.getDataDirectoryPath() + "guardsman_fpp_" + plan.getConSaId() + ".pdf");

    List<DocumentParameterBean> parms = new ArrayList<DocumentParameterBean>();

    parms.add(new DocumentParameterBean("SA_NUMBER", plan.getConSaId()));
    parms.add(new DocumentParameterBean("ERP_RTLR_NO", plan.getErpRetailerNo()));
    parms.add(new DocumentParameterBean("FULL_NAME", plan.getConsumer().getFullName()));
    parms.add(new DocumentParameterBean("ADDRESS", plan.getCombinedStreetAddress()));
    parms.add(new DocumentParameterBean("CITY_STATE_ZIP", plan.getCity() + " " + plan.getState() + " " + plan.getPostalCode()));
    parms.add(new DocumentParameterBean("POSTAL_ZIP", StringUtils.replace(plan.getPostalCode(), "-", "")));
    parms.add(new DocumentParameterBean("PLAN_NAME", plan.getPlanName()));
    parms.add(new DocumentParameterBean("LANGUAGE_CODE", plan.getLanguageCode()));
    parms.add(new DocumentParameterBean("ERROR_MESSAGE", null));

    //log4jLogger.info(workerId + ": Generating plan " + plan.getPlanName() + ", language=" + plan.getLanguageCode());
    return PlanetPressUtility.generatePdf("vserverpdfplans2", parms, "ERROR_MESSAGE", plan.getPlanPdfFilePath());
  }

  private boolean emailPlan(PlanBean plan)
  {
    SimpleUserBean from = new SimpleUserBean();
    from.setFullName("Guardsman");
    from.setEmail("protectionplans@guardsman.com");

    Map<String, Object> values = new HashMap<String, Object>();
    values.put("consumerFullName", plan.getConsumer().getFullName());

    List<EmailAttachment> attachments = new ArrayList<EmailAttachment>();
    EmailAttachment planPdf = new EmailAttachment();
    planPdf.setPath(plan.getPlanPdfFilePath());
    planPdf.setName("ProtectionPlan.pdf");
    attachments.add(planPdf);

    EmailAttachment howToRequestService = new EmailAttachment();
    howToRequestService.setURL(this.getClass().getResource("../resources/HOW TO REQUEST SERVICE.pdf"));
    howToRequestService.setName("HOW TO REQUEST SERVICE.pdf");
    attachments.add(howToRequestService);

    //debug("Emailing " + planPdf.getName() + " to " + plan.getConsumer().getFullNameAndEmail() + "...");
    boolean shouldSendEmail = inProduction;

    if (!inProduction)
    {
      shouldSendEmail = true;
      plan.getConsumer().setEmail("rmurphy2@valspar.com");
    }

    if (shouldSendEmail)
    {
      if (!NotificationUtility.sendNotification(plan.getConsumer(), from, "Your New Guardsman Furniture Protection Plan (ID " + plan.getConSaId() + ")", "guardsmanplandelivery-protection_plan.ftl", values, attachments))
      {
        log4jLogger.error("Unable to email PDF to " + plan.getConsumer().getFullNameAndEmail());
        return false;
      }
    }

    return true;
  }

  private boolean emailUetaNotice(PlanBean plan)
  {
    SimpleUserBean from = new SimpleUserBean();
    from.setFullName("Guardsman");
    from.setEmail("protectionplans@guardsman.com");

    Map<String, Object> values = new HashMap<String, Object>();
    values.put("consumerFullName", plan.getConsumer().getFullName());
    values.put("uetaUrl", plan.getUetaUrl());

    List<EmailAttachment> attachments = new ArrayList<EmailAttachment>();

    //debug("Emailing " + planPdf.getName() + " to " + plan.getConsumer().getFullNameAndEmail() + "...");
    boolean shouldSendEmail = inProduction;

    if (!inProduction)
    {
      shouldSendEmail = true;
      plan.getConsumer().setEmail("rmurphy2@valspar.com");
    }

    if (shouldSendEmail)
    {
      if (!NotificationUtility.sendNotification(plan.getConsumer(), from, "IMPORTANT/ACTION REQUIRED: Your New Guardsman Furniture Protection Plan (ID " + plan.getConSaId() + ")", "guardsmanplandelivery-ueta_notice.ftl", values, attachments))
      {
        log4jLogger.error("Unable to email UETA notice to " + plan.getConsumer().getFullNameAndEmail());
        return false;
      }
    }

    return true;
  }

  public void release()
  {
  }

  public boolean isDaemon()
  {
    return false;
  }

  public void setPlansToProcess(List<PlanBean> plansToProcess)
  {
    this.plansToProcess = plansToProcess;
  }

  public List<PlanBean> getPlansToProcess()
  {
    return plansToProcess;
  }

  public void setPlansForInternalPrinting(List<PlanBean> plansForInternalPrinting)
  {
    this.plansForInternalPrinting = plansForInternalPrinting;
  }

  public List<PlanBean> getPlansForInternalPrinting()
  {
    return plansForInternalPrinting;
  }

  public void setPlansForOutsorcePrinting(List<PlanBean> plansForOutsourcePrinting)
  {
    this.plansForOutsourcePrinting = plansForOutsourcePrinting;
  }

  public List<PlanBean> getPlansForOutsourcePrinting()
  {
    return plansForOutsourcePrinting;
  }

  public void setInProduction(boolean inProduction)
  {
    this.inProduction = inProduction;
  }

  public boolean isInProduction()
  {
    return inProduction;
  }

  public void setErroredPlans(Map<PlanBean, PlanetPressResultBean> erroredPlans)
  {
    this.erroredPlans = erroredPlans;
  }

  public Map<PlanBean, PlanetPressResultBean> getErroredPlans()
  {
    return erroredPlans;
  }

  public void setUetaNotifiedPlans(List<PlanBean> uetaNotifiedPlans)
  {
    this.uetaNotifiedPlans = uetaNotifiedPlans;
  }

  public List<PlanBean> getUetaNotifiedPlans()
  {
    return uetaNotifiedPlans;
  }

  public void setPdfEmailedPlans(List<PlanBean> pdfEmailedPlans)
  {
    this.pdfEmailedPlans = pdfEmailedPlans;
  }

  public List<PlanBean> getPdfEmailedPlans()
  {
    return pdfEmailedPlans;
  }
}
