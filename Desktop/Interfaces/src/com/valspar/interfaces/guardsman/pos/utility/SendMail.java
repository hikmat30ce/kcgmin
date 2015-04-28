package com.valspar.interfaces.guardsman.pos.utility;

import com.valspar.interfaces.common.servlets.PropertiesServlet;
import java.beans.*;
import java.io.Serializable;
import java.util.*;
import javax.activation.DataHandler;
import javax.activation.FileDataSource;
import javax.mail.*;
import javax.mail.internet.*;
import org.apache.log4j.Logger;

public class SendMail implements Serializable
{
  static Logger log4jLogger = Logger.getLogger(SendMail.class.getName());

  public SendMail()
  {
    mailSession = null;
    mailMessage = null;
    propertySupport = new PropertyChangeSupport(this);
    try
    {
      //ResourceBundle bundle = ResourceBundle.getBundle("com.valspar.interfaces.guardsman.pos.properties.ApplicationResources");
      Properties fmail = new Properties();
      fmail.put("mail.transport.protocol", "smtp");
      fmail.put("mail.smtp.host", PropertiesServlet.getProperty("mailserver"));

      mailSession = Session.getDefaultInstance(fmail, null);
      mailMessage = new MimeMessage(mailSession);
    }
    catch (Exception err)
    {
      System.err.println("SendMail::SendMail:" + err);
    }
  }

  public void addPropertyChangeListener(PropertyChangeListener listener)
  {
    propertySupport.addPropertyChangeListener(listener);
  }

  public void removePropertyChangeListener(PropertyChangeListener listener)
  {
    propertySupport.removePropertyChangeListener(listener);
  }

  public String[] getSendTo()
  {
    return sendTo;
  }

  public void setSendTo(String[] sendTo)
  {
    this.sendTo = sendTo;
  }

  public void setSendTo(StringBuilder buffer)
  {
    setSendTo(buffer.toString());
  }

  public void setSendTo(String buffer)
  {
    StringTokenizer tok = new StringTokenizer(buffer, ";");
    String s[] = new String[tok.countTokens()];
    for (int i = 0; tok.hasMoreTokens(); i++)
      s[i] = tok.nextToken();
    setSendTo(s);
  }

  public void setCc(StringBuilder buffer)
  {
    setCc(buffer.toString());
  }

  public void setCc(String buffer)
  {
    StringTokenizer tok = new StringTokenizer(buffer, ";");
    String s[] = new String[tok.countTokens()];
    for (int i = 0; tok.hasMoreTokens(); i++)
      s[i] = tok.nextToken();
    setCc(s);
  }

  public void setCc(String[] c)
  {
    cc = c;
  }

  public String[] getCc()
  {
    return cc;
  }

  public void setBcc(StringBuilder buffer)
  {
    setBcc(buffer.toString());
  }

  public void setBcc(String buffer)
  {
    StringTokenizer tok = new StringTokenizer(buffer, ";");
    String s[] = new String[tok.countTokens()];
    for (int i = 0; tok.hasMoreTokens(); i++)
      s[i] = tok.nextToken();
    setBcc(s);
  }

  public void setBcc(String[] c)
  {
    bcc = c;
  }

  public String[] getBcc()
  {
    return bcc;
  }

  public String getSentFrom()
  {
    return sentFrom;
  }

  public void setSentFrom(String sentFrom)
  {
    this.sentFrom = sentFrom;
  }

  public StringBuilder getMessage()
  {
    return message;
  }

  public void setMessage(StringBuilder message)
  {
    this.message = message;
  }

  public void send() throws MessagingException, AddressException
  {
    mailMessage.setFrom(new InternetAddress(getSentFrom()));
    mailMessage.setSubject(getSubject());
    
    MimeBodyPart mbp1 = new MimeBodyPart();
    mbp1.setText(getMessage().toString());
    
    Multipart mp = new MimeMultipart();
    mp.addBodyPart(mbp1);

    if (attachmentName != null)
    {
      MimeBodyPart mbp2 = new MimeBodyPart();
      FileDataSource fds = new FileDataSource(attachmentName);
      mbp2.setDataHandler(new DataHandler(fds));
      mbp2.setFileName(fds.getName());
      mp.addBodyPart(mbp2);
    }
    mailMessage.setContent(mp);

    InternetAddress addresses[] = new InternetAddress[getSendTo().length];
    for (int i = 0; i < getSendTo().length; i++)
      addresses[i] = new InternetAddress(getSendTo()[i]);

    if (cc != null && cc.length > 0)
    {
      InternetAddress cCopies[] = new InternetAddress[getCc().length];
      for (int i = 0; i < getCc().length; i++)
        cCopies[i] = new InternetAddress(getCc()[i]);

      mailMessage.setRecipients(javax.mail.Message.RecipientType.CC, cCopies);
    }
    if (bcc != null && bcc.length > 0)
    {
      InternetAddress bCopies[] = new InternetAddress[getBcc().length];
      for (int i = 0; i < getBcc().length; i++)
        bCopies[i] = new InternetAddress(getBcc()[i]);

      mailMessage.setRecipients(javax.mail.Message.RecipientType.BCC, bCopies);
    }
    mailMessage.setRecipients(javax.mail.Message.RecipientType.TO, addresses);
    Transport.send(mailMessage);
  }

  public String getSubject()
  {
    return subject;
  }

  public void setSubject(String subject)
  {
    this.subject = subject;
  }

  public void sendHTML() throws Exception
  {
    mailMessage.setFrom(new InternetAddress(getSentFrom()));
    mailMessage.setSubject(getSubject());
    MimeBodyPart htmlPart = new MimeBodyPart();
    htmlPart.setContent(message.toString(), "text/html");
    Multipart mp = new MimeMultipart();
    mp.addBodyPart(htmlPart);
    InternetAddress addresses[] = new InternetAddress[getSendTo().length];
    for (int i = 0; i < getSendTo().length; i++)
      addresses[i] = new InternetAddress(getSendTo()[i]);

    mailMessage.setRecipients(javax.mail.Message.RecipientType.TO, addresses);
    if (cc != null && cc.length > 0)
    {
      InternetAddress cCopies[] = new InternetAddress[getCc().length];
      for (int i = 0; i < getCc().length; i++)
        cCopies[i] = new InternetAddress(getCc()[i]);

      mailMessage.setRecipients(javax.mail.Message.RecipientType.CC, cCopies);
    }
    if (bcc != null && bcc.length > 0)
    {
      InternetAddress bCopies[] = new InternetAddress[getBcc().length];
      for (int i = 0; i < getBcc().length; i++)
        bCopies[i] = new InternetAddress(getBcc()[i]);

      mailMessage.setRecipients(javax.mail.Message.RecipientType.BCC, bCopies);
    }
    mailMessage.setContent(mp);
    log4jLogger.info("Sending email...");
    Transport.send(mailMessage);
    log4jLogger.info("Email sent...");
  }
  
  public void setAttachmentName(String attachmentName)
  {
    this.attachmentName = attachmentName;
  }

  public String getAttachmentName()
  {
    return attachmentName;
  }

  private PropertyChangeSupport propertySupport;
  private String sendTo[];
  private String sentFrom;
  private StringBuilder message;
  private String subject;
  private Session mailSession;
  private Message mailMessage;
  private String attachmentName;
  protected String cc[];
  protected String bcc[];
}
