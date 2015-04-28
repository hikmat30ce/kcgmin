package com.valspar.interfaces.common.utils;

import com.valspar.interfaces.common.beans.SimpleUserBean;
import com.valspar.interfaces.common.servlets.PropertiesServlet;
import com.valspar.interfaces.hr.submitpayrollinput.beans.PayrollInputBean;
import com.valspar.interfaces.hr.submitpayrollinput.beans.PayrollInputNotificationBean;
import com.valspar.interfaces.sales.dealerbrands.beans.DealerBrandsInputNotificationBean;
import com.valspar.interfaces.sales.lowes.beans.LowesInputNotificationBean;
import freemarker.template.*;
import java.io.*;
import java.util.*;
import javax.servlet.ServletContext;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.mail.*;
import org.apache.log4j.Logger;

public class NotificationUtility
{
  private static Logger log4jLogger = Logger.getLogger(NotificationUtility.class);
  private static String MAIL_SERVER = PropertiesServlet.getProperty("mailserver");
  private static Configuration configuration = new Configuration();

  public NotificationUtility()
  {
  }

  public static void initialize(ServletContext sc)
  {
    configuration.setServletContextForTemplateLoading(sc, "templates");

    String webserver = PropertiesServlet.getProperty("webserver");
    Map<String, Object> rootMap = new HashMap<String, Object>();
    rootMap.put("properties", new HashMap(System.getProperties()));
    rootMap.put("restartTime", new java.util.Date());
    rootMap.put("webserver", webserver);
    String messageBody = buildMessage("restart-notification.ftl", rootMap);

    String emailNotifyList = PropertiesServlet.getProperty("emailnotifylist");

    if (StringUtils.isNotEmpty(emailNotifyList))
    {
      sendHTMEmail(emailNotifyList, messageBody, webserver + " Has Been Restarted", null);
    }
  }

  public static String buildMessage(String template, Map<String, Object> values)
  {
    Writer out = new StringWriter();
    try
    {
      Template t = configuration.getTemplate(template);
      t.process(values, out);
    }
    catch (Exception e)
    {
      log4jLogger.error(e);
    }
    return out.toString();
  }

  private static boolean sendHTMEmail(SimpleUserBean recipient, SimpleUserBean from, String messageBody, String subject, List<EmailAttachment> attachments)
  {
    try
    {
      HtmlEmail email = new HtmlEmail();
      email.setCharset("utf-8");
      email.setHostName(MAIL_SERVER);
      email.addTo(recipient.getEmail(), recipient.getFullName());
      email.setFrom(from.getEmail(), from.getFullName());
      email.setSubject(subject);
      email.setHtmlMsg(messageBody);

      if (attachments != null)
      {
        for (EmailAttachment attachment: attachments)
        {
          email.attach(attachment);
        }
      }

      email.send();
      return true;
    }
    catch (Exception e)
    {
      log4jLogger.error(e);
      return false;
    }
  }

  private static void sendHTMEmail(String recipients, String messageBody, String subject, List<File> attachments)
  {
    try
    {
      HtmlEmail email = new HtmlEmail();
      email.setCharset("utf-8");
      email.setHostName(MAIL_SERVER);
      email.addTo(StringUtils.split(recipients, ","));
      email.setFrom("interfaces@valspar.com");
      email.setSubject(subject);
      email.setContent(messageBody, "text/html");

      if (attachments != null)
      {
        for (File f: attachments)
        {
          email.attach(f);
        }
      }

      email.send();
    }
    catch (Exception e)
    {
      log4jLogger.error(e);
    }
  }

  public static void sendHTMEmail(String[] recipients, String messageBody, String subject, boolean urgent, String filePath)
  {
    try
    {
      HtmlEmail email = new HtmlEmail();
      email.setHostName(MAIL_SERVER);
      email.addTo(recipients);
      email.setFrom("interfaces@valspar.com");
      email.setSubject(subject);
      email.setHtmlMsg(messageBody);

      if (urgent)
      {
        Map<String, String> headerMap = new HashMap<String, String>();
        headerMap.put("X-Priority", "1");
        headerMap.put("X-MSMail-Priority", "High");
        email.setHeaders(headerMap);
      }
      if (org.apache.commons.lang3.StringUtils.isNotEmpty(filePath))
      {
        EmailAttachment attachment = new EmailAttachment();
        attachment.setPath(filePath);
        attachment.setDisposition(EmailAttachment.ATTACHMENT);
        email.attach(attachment);
      }
      email.send();
    }
    catch (Exception e)
    {
      log4jLogger.error(e);
    }
  }

  public static boolean sendNotification(SimpleUserBean recipient, SimpleUserBean from, String subject, String template, Map<String, Object> parameters, List<EmailAttachment> attachments)
  {
    String body = buildMessage(template, parameters);
    return sendHTMEmail(recipient, from, body, subject, attachments);
  }

  public static void sendNotification(String recipients, String subject, String template, Map<String, Object> parameters, List<File> attachments)
  {
    String body = buildMessage(template, parameters);
    sendHTMEmail(recipients, body, subject, attachments);
  }

  public static void sendLowesNotifcationEmail(LowesInputNotificationBean lowesInputNotificationBean)
  {
    try
    {
      boolean urgent = false;
      Map<String, Object> rootMap = new HashMap<String, Object>();
      rootMap.put("lowesInputNotificationBean", lowesInputNotificationBean);
      String messageBody = buildMessage("lowes-notification.ftl", rootMap);
      StringBuilder sb = new StringBuilder();
      sb.append("Lowes Account Sync has been run: ");
      sb.append(lowesInputNotificationBean.getErrorAccountList().size());
      sb.append(" accounts were in error on ");
      sb.append(lowesInputNotificationBean.getServer());
      sendHTMEmail(new String[] { "SFDCLOWESUSERS@valspar.com" }, messageBody, sb.toString(), urgent, null);

    }
    catch (Exception e)
    {
      log4jLogger.error(e);
    }
  }

  public static void sendDealerBrandsNotifcationEmail(DealerBrandsInputNotificationBean dealerBrandsInputNotificationBean)
  {
    try
    {
      boolean urgent = false;
      Map<String, Object> rootMap = new HashMap<String, Object>();
      rootMap.put("dealerBrandsInputNotificationBean", dealerBrandsInputNotificationBean);
      String messageBody = buildMessage("dealerbrands-notification.ftl", rootMap);
      StringBuilder sb = new StringBuilder();
      sb.append("Dealer Brands Account Sync has been run: ");
      sb.append(dealerBrandsInputNotificationBean.getErrorAccountList().size());
      sb.append(" accounts were in error on ");
      sb.append(dealerBrandsInputNotificationBean.getServer());
      sendHTMEmail(new String[] { "SFDCCONSUMERBRANDSUSERS@valspar.com" }, messageBody, sb.toString(), urgent, null);

    }
    catch (Exception e)
    {
      log4jLogger.error(e);
    }
  }

  public static String getRunTimeDuration(Date startTime, Date endTime)
  {
    long diff = endTime.getTime() - startTime.getTime();
    diff = diff / 1000;
    String format = String.format("%%0%dd", 2);
    String seconds = String.format(format, diff % 60);
    String minutes = String.format(format, (diff % 3600) / 60);
    String hours = String.format(format, diff / 3600);

    return hours + ":" + minutes + ":" + seconds;
  }
  
  public static void sendPayrollNoticticationEmail(PayrollInputNotificationBean payrollInputNotificationBean, List<PayrollInputBean> payrollInputBeanErrorList)
  {
    try
    {
      boolean urgent = false;
      Map<String, Object> rootMap = new HashMap<String, Object>();
      rootMap.put("payrollInputErrorBeanList", payrollInputBeanErrorList);
      rootMap.put("payrollInputNotificationBean", payrollInputNotificationBean);
      
      String messageBody = buildMessage("payroll-notification.ftl", rootMap);

      StringBuilder sb = new StringBuilder();
      sb.append(payrollInputNotificationBean.getFileName());
      sb.append(": ");
      sb.append(payrollInputNotificationBean.getRowCount());
      sb.append(" row(s) were processed. ");
      if(!payrollInputBeanErrorList.isEmpty())
      {
        sb.append(payrollInputNotificationBean.getErrorCount());
        sb.append(" row(s) erred. ");
        urgent = true;
      }
      sb.append("Server: ");
      sb.append(PropertiesServlet.getProperty("webserver"));
      
      sendHTMEmail(StringUtils.split(PropertiesServlet.getProperty("workday.submitpayrollinputemailnotifylist"),","), messageBody, sb.toString(), urgent, null);
    }
    catch (Exception e)
    {
      log4jLogger.error(e);
    }
  }

  public static void sendPayrollNoticticationFailureEmail(PayrollInputNotificationBean payrollInputNotificationBean, Exception exception)
  {
    try
    {
      Map<String, Object> rootMap = new HashMap<String, Object>();
      rootMap.put("payrollInputNotificationBean", payrollInputNotificationBean);
      rootMap.put("exception", exception);
      
      String messageBody = buildMessage("payroll-failure-notification.ftl", rootMap);

      StringBuilder sb = new StringBuilder();
      sb.append("ERROR IN FILE: ");
      sb.append(payrollInputNotificationBean.getFileName());
      sb.append(", Server: ");
      sb.append(PropertiesServlet.getProperty("webserver"));
      sendHTMEmail(StringUtils.split(PropertiesServlet.getProperty("submitpayrollinputemailnotifylist"),","), messageBody, sb.toString(), true, null);
    }
    catch (Exception e)
    {
      log4jLogger.error(e);
    }
  }
  

}
