package com.valspar.interfaces.common.utils;

import com.valspar.interfaces.common.servlets.PropertiesServlet;
import commonj.work.*;
import java.text.*;
import java.util.*;
import javax.mail.*;
import javax.mail.internet.*;
import org.apache.log4j.Logger;

public class EmailUtility implements Work
{
  private static Logger log4jLogger = Logger.getLogger(EmailUtility.class);

  public EmailUtility()
  {
  }

  public void run()
  {
    sendEmail();
  }

  public static void main(String[] args)
  {
    EmailUtility eu = new EmailUtility();
    eu.sendEmail();
  }

  public void sendEmail()
  {
    try
    {
      StringBuilder message = new StringBuilder();
      String webserver = PropertiesServlet.getProperty("webserver");
      String mailServer = PropertiesServlet.getProperty("mailserver");
      String emailnotifylist = PropertiesServlet.getProperty("emailnotifylist");
      String fromEmail = "interfaces@valspar.com";
      String subject = webserver + " Has Been Restarted";
      if (emailnotifylist != null && emailnotifylist.length() > 0)
      {
        DateFormat df = new SimpleDateFormat("MM/dd/yyyy 'at' HH:mm:ss");
        Date dt = new Date();
        String time = df.format(dt);
        Properties p = System.getProperties();
        message.append("<html><head><title>");
        message.append(webserver);
        message.append(" Restarted</title></head><body bgcolor=\"#FFFFFF\"><font face = arial size = 2>");
        message.append(webserver);
        message.append(" Has Been Restarted<br><b>Time of Restart was ");
        message.append(time);
        message.append(" </b><br><br>");
        Set s = p.keySet();
        Iterator i = s.iterator();
        message.append("<table><tr bgcolor = #DDDDDD><td><font size = 2 face = arial><b>System Key</b></td><td><font size = 2 face = arial><b>System Value</b></td></tr><tr>");
        while (i.hasNext())
        {
          message.append("<tr><td><font size = 2 face = arial>");
          String key = (String) i.next();
          message.append(key);
          message.append("</td><td><font size = 2 face = arial>");
          message.append((String) p.getProperty(key));
          message.append("</td></tr><tr>");
        }
        message.append("</table>");
        message.append("</body></html>");
        java.util.Properties prop = new java.util.Properties();
        prop.put("mail.smtp.host", mailServer);
        Session mail_Session = Session.getDefaultInstance(prop, null);
        InternetAddress fromAddress = new InternetAddress(fromEmail);
        
        MimeMessage myMessage = new MimeMessage(mail_Session);
        myMessage.setFrom(fromAddress);
        myMessage.setRecipients(Message.RecipientType.TO, InternetAddress.parse(emailnotifylist));
        myMessage.setSentDate(new java.util.Date());
        myMessage.setSubject(subject);
        myMessage.setContent(message.toString(), "text/html");
   
        Transport.send(myMessage);
      }
    }
    catch (Exception e)
    {
      log4jLogger.error(e);
    }
  }
  
  public static void sendNotificationEmail(String subject, String message, String emailNotifyList)
  {    
    try
    {
      String mailServer = PropertiesServlet.getProperty("mailserver");
      
      String fromEmail = "interfaces@valspar.com";
      if (emailNotifyList != null && emailNotifyList.length() > 0)
      {
        java.util.Properties prop = new java.util.Properties();
        prop.put("mail.smtp.host", mailServer);
        Session mail_Session = Session.getDefaultInstance(prop, null);
        InternetAddress fromAddress = new InternetAddress(fromEmail);
        
        MimeMessage myMessage = new MimeMessage(mail_Session);
        myMessage.setFrom(fromAddress);
        myMessage.setRecipients(Message.RecipientType.TO, InternetAddress.parse(emailNotifyList));
        myMessage.setSentDate(new java.util.Date());
        myMessage.setSubject(subject);
        myMessage.setContent(message, "text/html; charset=utf-8");
   
        Transport.send(myMessage);
      }
    }
    catch (Exception e)
    {
      log4jLogger.error(e);
    }
  }

  public void release()
  {
  }

  public boolean isDaemon()
  {
    return false;
  }
}
