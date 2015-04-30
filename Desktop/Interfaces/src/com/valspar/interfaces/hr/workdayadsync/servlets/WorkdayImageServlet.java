package com.valspar.interfaces.hr.workdayadsync.servlets;

import com.valspar.interfaces.common.api.HumanResourcesAPI;
import java.io.BufferedOutputStream;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.Date;
import javax.servlet.ServletException;
import javax.servlet.http.*;
import org.apache.commons.io.IOUtils;
import org.apache.log4j.Logger;

public class WorkdayImageServlet extends HttpServlet
{

  private static Logger log4jLogger = Logger.getLogger(WorkdayImageServlet.class);

  public WorkdayImageServlet()
  {
  }

  public void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException
  {
    String employeeID = request.getParameter("employeeID");
    BufferedOutputStream bos = null;
    ByteArrayInputStream bai = null;

    try
    {
      HumanResourcesAPI humanResourcesAPI = new HumanResourcesAPI();
      byte[] employeeImage = humanResourcesAPI.getEmployeeImage(employeeID);
      if (employeeImage != null)
      {
        bai = new ByteArrayInputStream(employeeImage);
        response.setContentType("image/jpeg");
        bos = new BufferedOutputStream(response.getOutputStream());
        IOUtils.write(IOUtils.toByteArray(bai), bos);
        IOUtils.closeQuietly(bos);
      }
      else
      {
        response.setContentType("text/html");
        PrintWriter writer = response.getWriter();
        writer.println("<html><body>No Image Found for Employee ID " + employeeID + "</body></html>");
      }
    }
    catch (Exception e)
    {
      log4jLogger.error(e);
    }
  }
}
