package com.valspar.interfaces.guardsman.pos.utility;

import com.valspar.interfaces.guardsman.pos.beans.*;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.util.Iterator;
import oracle.jdbc.OraclePreparedStatement;
import oracle.jdbc.OracleResultSet;
import org.apache.log4j.Logger;

public class EmailReport
{
  static Logger log4jLogger = Logger.getLogger(EmailReport.class.getName());

  public EmailReport(PosFileBean pfb)
  {
    StringBuilder message = new StringBuilder();
    StringBuilder message2 = new StringBuilder();
    String retailerErp;
    boolean nullErps = false;
    boolean nonNullErps = false;
    boolean nullErpErrors = false;
    boolean nonNullErpErrors = false;
    boolean fileLevelErrors = false;
    
    Iterator i = pfb.getUniqueRetailerMap().values().iterator();
    while (i.hasNext())
    {
      retailerErp = (String) i.next();
      if (retailerErp != null)
      {
         String erp_email = (String) pfb.getErpErrorEmailMap().get(retailerErp);
         if (erp_email == null)
         {
           nullErps = true;
           if (erpErrorsExist(pfb, retailerErp))
             nullErpErrors = true;
         }  
         else 
         {
           nonNullErps = true;
           if (erpErrorsExist(pfb, retailerErp))
              nonNullErpErrors = true;
         }
      }
    }
    // set fileLevelErrors = true if there are ANY file level errors, because those need to be sent
    // to the default email address
    if (!pfb.getErrors().isEmpty())
      fileLevelErrors = true;

    try
    {
      if (nullErpErrors || fileLevelErrors)
      {
        //If errors exist for erps with no specific email, or file level errors exist, send them
        //to the default error email address.
        SendMail sm = null;
        sm = new SendMail();
        message = new StringBuilder();
        sm.setSendTo(pfb.getEmailAddr());
        sm.setSentFrom("valsparpartner@valspar.com");
        sm.setSubject("Guardsman Point of Sale report -- ERRORS OCCURRED:  "+pfb.getRetailerName());
        message.append(pfb.getRetailerName() + " file Processed: " + pfb.getFileName().substring(pfb.getFileName().lastIndexOf("/") + 1) + "\n\n");
        message.append("!########################################################!\n\n");
        message.append("The processed Point of Sale file contained errors.\n\n");
        message.append("Please note that transactions which error are not processed.\n");
        message.append("Failed transactions must be corrected and resubmitted to be registered.\n");
        message.append("Contact Valspar Support with questions.\n\n");
        message.append("!########################################################!\n\n");
        //write out file level errors
        reportFileLevelErrors(pfb, message);
        //write out sales receipt errors for those ERP's w/o a specific email address.
        Iterator j = pfb.getUniqueRetailerMap().values().iterator();
        while (j.hasNext())
        {
          retailerErp = (String) j.next();
          String erp_email = (String) pfb.getErpErrorEmailMap().get(retailerErp);
          if (erp_email == null)
             reportSrErrors(pfb, message, retailerErp);
        }
        if (pfb.isSendInvoiceDetailRpt())
        {
          createErrorRpt(pfb);
          File file = new File(pfb.getErrorPath());      
          if(file.exists())
          {
            sm.setAttachmentName(pfb.getErrorPath());
          }  
        }  
        sm.setMessage(message);
        sm.send();        
      }
      else
        if (nullErps)
        {
          SendMail sm = null;
          sm = new SendMail();
          message = new StringBuilder();
          sm.setSendTo(pfb.getEmailAddr());
          sm.setSentFrom("valsparpartner@valspar.com");
          sm.setSubject("Guardsman Point of Sale report -- Successful:  "+pfb.getRetailerName());
          message.append(pfb.getRetailerName() + " file Processed: " + pfb.getFileName().substring(pfb.getFileName().lastIndexOf("/") + 1) + "\n\n");
          message.append("The Point of Sale transfer was successfully executed at Valspar.\n");
          message.append("We appreciate your business.  Please contact us with any questions.");
          sm.setMessage(message);
          sm.send();
        }
      
      if (nonNullErps)
      {
        Iterator j = pfb.getUniqueRetailerMap().values().iterator();
        while (j.hasNext())
        {
          retailerErp = (String) j.next();
          String erp_email = (String) pfb.getErpErrorEmailMap().get(retailerErp);
          if (erp_email != null)
          {
            if (erpErrorsExist(pfb, retailerErp))
            {
              SendMail sm2 = null;
              sm2 = new SendMail();
              message2 = new StringBuilder();
              sm2.setSendTo(erp_email);
              sm2.setSentFrom("valsparpartner@valspar.com");
              sm2.setSubject("Guardsman Point of Sale report -- ERRORS OCCURRED:  ("+retailerErp+")  "+pfb.getRetailerName());
              message2.append(pfb.getRetailerName() + " file Processed: " + pfb.getFileName().substring(pfb.getFileName().lastIndexOf("/") + 1) + "\n\n");
              message2.append("!########################################################!\n\n");
              message2.append("The processed Point of Sale file contained errors.\n\n");
              message2.append("Please note that transactions which error are not processed.\n");
              message2.append("Failed transactions must be corrected and resubmitted to be registered.\n");
              message2.append("Contact Valspar Support with questions.\n\n");
              message2.append("!########################################################!\n\n");
              reportSrErrors(pfb, message2, retailerErp);
              sm2.setMessage(message2);
              sm2.send();        
            }
            else
            {
              SendMail sm2 = null;
              sm2 = new SendMail();
              message2 = new StringBuilder();
              sm2.setSendTo(erp_email);
              sm2.setSentFrom("valsparpartner@valspar.com");
              sm2.setSubject("Guardsman Point of Sale report -- Successful:  ("+retailerErp+")  "+ pfb.getRetailerName());
              message2.append(pfb.getRetailerName() + " file Processed: " + pfb.getFileName().substring(pfb.getFileName().lastIndexOf("/") + 1) + "\n\n");
              message2.append("The Point of Sale transfer was successfully executed at Valspar.\n");
              message2.append("We appreciate your business.  Please contact us with any questions.");
              sm2.setMessage(message2);
              sm2.send();
            }
          } 
        }
      }
    }
    catch (Exception e)
    {
      log4jLogger.info("Send Mail Error " + e.toString());
    }
    reportAdminErrors(pfb);
  }
  
  public static void reportFileLevelErrors(PosFileBean pfb, StringBuilder message)
  {
    int errorCount = 0;
    Iterator k = pfb.getErrors().iterator(); //file errors
    while (k.hasNext())
    {
      ErrorBean errorBean = (ErrorBean) k.next();
      errorCount = errorCount + 1;
      message.append("Error: " + errorCount + ") " + errorBean.getErrorMsg() + ".  When: " + errorBean.getValidationStep() + ".\n" + errorBean.getRecord() + "\n");
    }
  }

  public static void reportSrErrors(PosFileBean pfb, StringBuilder message, String inErp)
  {
    int errorCount = 0;
    Iterator i = pfb.getSalesReceipts().iterator(); //SR errors
    while (i.hasNext())
    {
      SalesReceiptBean srb = (SalesReceiptBean) i.next();
      //  If the RetailerNo (erp_no) in file was null then just output the error 
      if (srb.getRetailerNo() == null)
      {
        if (srb.getErrors().size() > 0)
        {
          Iterator j = srb.getErrors().iterator();
          while (j.hasNext())
          {
            errorCount = errorCount + 1;
            ErrorBean errorBean = (ErrorBean) j.next();
            message.append("Error: " + errorCount + ") " + errorBean.getErrorMsg() + " / " + errorBean.getRecord() + ".\n");
          }
        }
      }
      else
      {
         //  If the RetailerNo (erp_no) in file was not null then only output the error 
         //  if it matches the ERP that we are creating an error message for.
         if (srb.getRetailerNo().equals(inErp))
         {
           if (srb.getErrors().size() > 0)
           {
             Iterator j = srb.getErrors().iterator();
             while (j.hasNext())
             {
               errorCount = errorCount + 1;
               ErrorBean errorBean = (ErrorBean) j.next();
               message.append("Error: " + errorCount + ") " + errorBean.getErrorMsg() + " / " + errorBean.getRecord() + ".\n");
             }
           }
         }
      }
    }
  }
  
  public static boolean erpErrorsExist(PosFileBean pfb, String inErp)   
  {
    Iterator i = pfb.getSalesReceipts().iterator(); //SR errors
    while (i.hasNext())
    {
      SalesReceiptBean srb = (SalesReceiptBean) i.next();
      if (srb.getRetailerNo() != null)
      {
         if ( (srb.getRetailerNo().equals(inErp)) || (srb.getRetailerNo() == null && inErp == "NULL") )
         {
           if (srb.getErrors().size() > 0)
              return true;
         }
      }
    }
    return false;
  }

  public static void reportAdminErrors(PosFileBean pfb)
  {
    int errorCount = 0;
    if (pfb.getAdminMessages().size() > 0)
    {
      StringBuilder adminMessage = new StringBuilder();
      String adminEmail = new String();
      adminEmail = "helpdeskgrandrapids@valspar.com";
      try
      {
        SendMail sm = new SendMail();
        sm.setSendTo(adminEmail);
        sm.setSentFrom("valsparpartner@valspar.com");
        sm.setSubject("Guardsman Point of Sale Exception Report");
        adminMessage.append(pfb.getRetailerName() + " file Processed: " + pfb.getFileName().substring(pfb.getFileName().lastIndexOf("/") + 1) + "\n\n");

        Iterator m = pfb.getAdminMessages().iterator();
        while (m.hasNext())
        {
          errorCount = errorCount + 1;
          AdminMsgBean amb = (AdminMsgBean) m.next();
          adminMessage.append("Admin Error: " + errorCount + ") " + amb.getProgramLocation() + " Msg: " + amb.getMessage() + " Item: " + amb.getItem() + "\n\n");
        }
        sm.setMessage(adminMessage);
        sm.send();
      }
      catch (Exception e)
      {
        log4jLogger.error("Send Mail Error " + e.toString());
      }
    }
  }
  
  public static void createErrorRpt(PosFileBean pfb)
  {
    
    File file=new File(pfb.getErrorPath());
    boolean dirExists = file.exists();
    if (!dirExists)
    {
      file.mkdirs();  
    }
    
    String errorFileName = pfb.getErrorPath()+"/Error"+pfb.getPosFhId()+".csv";
    pfb.setErrorPath(errorFileName);
    
    StringBuilder sb = new StringBuilder();

    sb.append("select 'detail',pos_error_msg||','||validation_step||','||replace(record,',')   ");
    sb.append("FROM SAM_POS_errors ");
    sb.append("where pos_fh_id = :posFhId ");
    sb.append("and rtlr_erp_no is null ");


    OraclePreparedStatement pstmt = null;
    OracleResultSet rs = null;
    try
    {
      pstmt = (OraclePreparedStatement) pfb.getConnection().prepareStatement(sb.toString());
      pstmt.setStringAtName("posFhId", pfb.getPosFhId());
      rs = (OracleResultSet) pstmt.executeQuery();
      while (rs.next())
      {
        BufferedWriter out = new BufferedWriter(new FileWriter(pfb.getErrorPath(),true));
        out.write(rs.getString(2));
        out.newLine();
        out.close();
      }
    }
    catch (Exception e)
    {
      ExceptionLogger.logException(pfb, "EmailReport", "createErrorRpt", null, e);
    }
    finally
    {
      TryCleanup.tryCleanup(pfb, pstmt, rs);
    }
  }
}
