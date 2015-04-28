package com.valspar.interfaces.common.utils;

import com.valspar.interfaces.common.servlets.PropertiesServlet;
import java.io.File;
import java.util.Date;
import javax.activation.*;
import javax.mail.*;
import javax.mail.internet.*;
import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;

public class EmailBean
{
  private static Logger log4jLogger = Logger.getLogger(EmailBean.class);

  public EmailBean()
  {
    super();
  }

  public static boolean emailMessage(String subject, String message, String toAdress, String fileToAttach)
  {
    try
    {
      return emailMessage(new InternetAddress("isreg@valspar.com", "Valspar Regulatory Interface"), subject, message, toAdress, fileToAttach);
    }
    catch (Exception e)
    {
      log4jLogger.error(e);
      return false;
    }
  }

  public static boolean emailMessage(InternetAddress fromAddress, String subject, String message, String toAdress, String fileToAttach)
  {
    try
    {
      java.util.Properties mailProperties = new java.util.Properties();
      mailProperties.put("mail.transport.protocol", "smtp");
      mailProperties.put("mail.smtp.host", PropertiesServlet.getProperty("mailserver"));
      Session session = Session.getDefaultInstance(mailProperties, null);
      Message msg = new MimeMessage(session);
      msg.setFrom(fromAddress);
      msg.setRecipients(Message.RecipientType.TO, InternetAddress.parse(toAdress, false));
      msg.setSubject(subject);

      // Create the message part
      BodyPart messageBodyPart = new MimeBodyPart();

      // Fill the message
      messageBodyPart.setText(message);

      Multipart multipart = new MimeMultipart();
      multipart.addBodyPart(messageBodyPart);

      // Part two is attachment
      messageBodyPart = new MimeBodyPart();
      DataSource source = new FileDataSource(fileToAttach);
      messageBodyPart.setDataHandler(new DataHandler(source));
      messageBodyPart.setFileName(StringUtils.substringAfterLast(fileToAttach, File.separator));
      multipart.addBodyPart(messageBodyPart);
      msg.setContent(multipart);
      msg.setSentDate(new Date());

      Transport.send(msg);
    }
    catch (Exception e)
    {
      log4jLogger.error(e);
      return false;
    }
    return true;
  }

  public static boolean emailMessage(String subject, String message, String toAdress)
  {
    if (toAdress != null)
    {
      try
      {
        java.util.Properties mailProperties = new java.util.Properties();
        mailProperties.put("mail.transport.protocol", "smtp");
        //mailProperties.put("mail.smtp.host", PropertiesServlet.getProperty("mailserver"));
        mailProperties.put("mail.smtp.host", "smtp1.valspar.com");
        Session session = Session.getDefaultInstance(mailProperties, null);
        Message msg = new MimeMessage(session);
        msg.setFrom(new InternetAddress("isreg@valspar.com", "Valspar Regulatory Interface"));
        msg.setRecipients(Message.RecipientType.TO, InternetAddress.parse(toAdress, false));
        msg.setSubject(subject);
        msg.setText(message);
        Transport.send(msg);
      }
      catch (Exception e)
      {
        log4jLogger.error(e);
        return false;
      }
    }
    return true;
  }
}
