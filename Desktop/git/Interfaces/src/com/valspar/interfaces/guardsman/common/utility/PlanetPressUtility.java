package com.valspar.interfaces.guardsman.common.utility;

import com.valspar.interfaces.common.servlets.PropertiesServlet;
import com.valspar.interfaces.common.utils.CommonUtility;
import com.valspar.interfaces.guardsman.common.beans.*;
import java.io.*;
import java.util.*;
import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.tempuri.*;
import remotetypecls.*;

public final class PlanetPressUtility
{
  private static Logger log4jLogger = Logger.getLogger(PlanetPressUtility.class);

  private PlanetPressUtility()
  {
  }

  public static PlanetPressResultBean generatePdf(String workflowName, List<DocumentParameterBean> parameters, String errorMessageParameterName, String outputFilePath)
  {
    PlanetPressResultBean ppResult = new PlanetPressResultBean();
    ppResult.setSuccess(false);

    try
    {
      ISoapActPortClient client = new ISoapActPortClient();
      client.setEndpoint(PropertiesServlet.getProperty("planetpress.endpoint"));

      TSubmitJobInfStruc inf = new TSubmitJobInfStruc();

      int numVariables = parameters.size();

      TVariableUnit[] vars = new TVariableUnit[numVariables];

      ListIterator li = parameters.listIterator();

      while (li.hasNext())
      {
        DocumentParameterBean parm = (DocumentParameterBean) li.next();

        TVariableUnit var = new TVariableUnit();
        var.setVariableName("%{" + parm.getName() + "}");
        var.setVariableValue(parm.getValue());

        vars[li.previousIndex()] = var;
      }

      inf.setSubmitOriginalFileName("notused.pdf");
      inf.setSubmitSOAPActionName(workflowName);
      inf.setSubmitVariableList(vars);

      String documentFilename = "NOTUSED";

      long msec = new Date().getTime();

      String username = PropertiesServlet.getProperty("planetpress.username");
      String password = PropertiesServlet.getProperty("planetpress.password");
      TSubmitJobResult result = client.submitJob(documentFilename.getBytes(), inf, true, username, password);

      ppResult.setSuccess(result.getSubmitSuccess() == 0);
      ppResult.setMsec(new Date().getTime() - msec);

      if (!ppResult.isSuccess())
      {
        StringBuilder sb = new StringBuilder();
        sb.append("Error calling PlanetPress web service:\n");
        sb.append("   submission return code:    " + result.getSubmitSuccess() + "\n");
        sb.append("   submission return message: " + result.getSubmitMessage() + "\n");
        sb.append("   endpoint:                  " + PropertiesServlet.getProperty("planetpress.endpoint") + "\n");
        sb.append("   workflow name:             " + workflowName + "\n");
        sb.append("   soap username:             " + username + "\n");
        log4jLogger.error(sb.toString());
        ppResult.setErrorMessage("PlanetPress SOAP call failed.  Workflow: " + workflowName + ", Error Code: " + result.getSubmitSuccess() + ", Error Message: " + result.getSubmitMessage());
      }
      else
      {
        if (StringUtils.isNotEmpty(errorMessageParameterName))
        {
          TSubmitJobInfStruc infReturn = result.getSubmitJobInfStruc();
          TVariableUnit[] varsReturn = infReturn.getSubmitVariableList();

          String errorMessageVariableName = "%{" + errorMessageParameterName + "}";

          for (TVariableUnit varReturn: varsReturn)
          {
            if (StringUtils.equals(varReturn.getVariableName(), errorMessageVariableName))
            {
              String errorValue = varReturn.getVariableValue();

              if (StringUtils.isNotEmpty(errorValue) && !StringUtils.equalsIgnoreCase(errorValue, "SUCCESSFUL"))
              {
                ppResult.setSuccess(false);
                ppResult.setErrorMessage(errorValue);
              }
              break;
            }
          }
        }

        if (ppResult.isSuccess())
        {
          FileOutputStream fos = null;
          BufferedOutputStream buf = null;
  
          try
          {
            fos = new FileOutputStream(outputFilePath);
            buf = new BufferedOutputStream(fos);
            buf.write(result.getSubmitResultFile());
            ppResult.setFilePath(outputFilePath);
          }
          catch (Exception e)
          {
            log4jLogger.error(e);
            ppResult.setSuccess(false);
            ppResult.setErrorMessage(e.getMessage());
          }
          finally
          {
            CommonUtility.close(buf);
            CommonUtility.close(fos);
          }
        }
      }
    }
    catch (Exception e)
    {
      log4jLogger.error(e);
      ppResult.setSuccess(false);
      ppResult.setErrorMessage(e.getMessage());
    }

    return ppResult;
  }
}
