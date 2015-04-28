package com.valspar.interfaces.guardsman.pos.utility;

import com.valspar.interfaces.guardsman.pos.program.GuardsmanPointOfSaleInterface;
import com.valspar.interfaces.guardsman.pos.beans.*;
import java.util.*;
import org.apache.log4j.Logger;

public class FileReport
{
  static Logger log4jLogger = Logger.getLogger(FileReport.class.getName());

  public FileReport(PosFileBean pfb)
  {
    StringBuilder notify = new StringBuilder();

    try
    {
      if (1 == 1)
      {
        notify.append("The Point of Sale transfer was successfully executed at Valspar.\n");
        notify.append("We appreciate your business.  Please contact us with any questions.");
        //notify.append(logMessage.toString());  
      }
      else
      {
        //notify.append("!########################################################!\n\n");
        notify.append("Errors occurred in the Point of Sale file that was processed.\n\n");
        notify.append("Please resend a correct version of the file to Valspar\n");
        notify.append("or contact Valspar Support with questions\n\n");
        //notify.append("!########################################################!\n\n");

        //notify.append(logMessage.toString());
      }
    }
    catch (Exception e)
    {
      log4jLogger.error("Error in FileReport.FileReport(): " + e.toString());
    }
  }

  public static void reportErrors(PosFileBean pfb)
  {
    int errorCount = 0;
    Iterator k = pfb.getErrors().iterator(); //file errors
    while (k.hasNext())
    {
      errorCount = errorCount + 1;
      ErrorBean errorBean = (ErrorBean) k.next();
      log4jLogger.error("Error: " + errorCount + ") " + errorBean.getErrorMsg() + ".  When: " + errorBean.getValidationStep() + ".\n" + errorBean.getRecord());
    }

    Iterator i = pfb.getSalesReceipts().iterator(); //SR errors
    while (i.hasNext())
    {
      SalesReceiptBean srb = (SalesReceiptBean) i.next();
      if (srb.getErrors().size() > 0)
      {
        Iterator j = srb.getErrors().iterator();
        while (j.hasNext())
        {
          errorCount = errorCount + 1;
          ErrorBean errorBean = (ErrorBean) j.next();
          log4jLogger.error("Error: " + errorCount + ") " + errorBean.getErrorMsg() + " / " + errorBean.getRecord());
        }
      }
    }
  }
}
