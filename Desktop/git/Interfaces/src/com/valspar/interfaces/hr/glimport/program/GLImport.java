package com.valspar.interfaces.hr.glimport.program;

import au.com.bytecode.opencsv.CSVReader;
import com.valspar.interfaces.common.BaseInterface;
import com.valspar.interfaces.common.beans.ConnectionAccessBean;
import com.valspar.interfaces.common.enums.DataSource;
import com.valspar.interfaces.common.servlets.PropertiesServlet;
import com.valspar.interfaces.common.utils.JDBCUtil;
import com.valspar.interfaces.common.utils.NotificationUtility;
import com.valspar.interfaces.hr.glimport.beans.GLBean;
import java.io.*;
import java.sql.CallableStatement;
import java.sql.Types;
import java.text.*;
import java.util.*;
import oracle.jdbc.*;
import org.apache.commons.lang3.*;
import org.apache.log4j.Logger;

public class GLImport extends BaseInterface
{
  private static Logger log4jLogger = Logger.getLogger(GLImport.class);

  public GLImport()
  {
  }

  public void execute()
  {    
    try
    {
      log4jLogger.info("GLImport.startWatchingDirectory() started...");
      File[] files = new File(PropertiesServlet.getProperty("workday.glinputdirectory")).listFiles();

      for (File file: files)
      {
        if (file.isFile())
        {
          log4jLogger.info("Calling processGL for " + file.getCanonicalPath());
          processGL(file);
        }
      }
    }
    catch (Exception e)
    {
      log4jLogger.error(e);
    }    
  }

  public static void processGL(File file)
  {
    List<GLBean> glBeanList = new ArrayList<GLBean>();
    OracleConnection conn = null;
    try
    {
      Thread.sleep(5 * 1000);
      File archivedFile = archiveFile(file);
      CSVReader reader = new CSVReader(new FileReader(archivedFile.getCanonicalPath()));
      List<String[]> readerList = reader.readAll();
      for (String[] inputLine: readerList)
      {
        GLBean glBean = new GLBean();
        glBean.setStatus(inputLine[0]);
        glBean.setCurrencyCode(inputLine[1]);
        glBean.setCreatedBy(inputLine[2]);
        glBean.setActualFlag(inputLine[3]);
        glBean.setJeUserCategoryName(inputLine[4]);
        glBean.setJeUserSourceName(inputLine[5]);
        glBean.setSegment1(inputLine[6]);
        glBean.setSegment2(inputLine[7]);
        glBean.setSegment3(inputLine[8]);
        glBean.setSegment4(inputLine[9]);
        glBean.setSegment5(inputLine[10]);
        glBean.setSegment6(inputLine[11]);
        glBean.setSegment7(inputLine[12]);
        glBean.setEnteredDr(inputLine[13]);
        glBean.setEnteredCr(inputLine[14]);
        glBean.setDescription(inputLine[15]);
        glBean.setPeriodName(inputLine[16]);
        glBeanList.add(glBean);
      }

      conn = (OracleConnection)ConnectionAccessBean.getConnection(DataSource.NORTHAMERICAN);
      callAppsInitialize(conn);
      String errorMessage = populateGLInterface(conn, glBeanList);
      String emailList[] = StringUtils.split(PropertiesServlet.getProperty("workday.glimportemailnotifylist"),",");

      if (StringUtils.isNotEmpty(errorMessage))
      {
        NotificationUtility.sendHTMEmail(emailList, errorMessage, "Error Loading Payroll Journal Entry", true, null);
      }
      else
      {
        String runID = queryRunID(conn);
        log4jLogger.info("Run ID is " + runID);

        insertIntoGLInterfaceControl(conn, runID);

        String requestId = callFNDRequestSumbitRequest(conn, runID);
        String emailBody = callFNDConcurrentWaitForRequest(conn, requestId);
        deleteGLInterfaceControl(conn, runID);
        createJournalEntryFile(conn);
        NotificationUtility.sendHTMEmail(emailList, emailBody, "Payroll Journal Entry - Canada", false, "/gloutput/JournalEntryCanada.csv");
      }

      reader.close();
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
  
  public static String populateGLInterface(OracleConnection conn, List<GLBean> glBeanList)
  {    
    OraclePreparedStatement pst = null;
    try
    {
      StringBuilder sb = new StringBuilder();
      sb.append(" INSERT into gl_interface  ( ");
      sb.append(" status, ");
      sb.append(" set_of_books_id, ");
      sb.append(" accounting_date, ");
      sb.append(" currency_code, ");
      sb.append(" date_created, ");
      sb.append(" created_by, ");
      sb.append(" actual_flag, ");
      sb.append(" user_je_category_name, ");
      sb.append(" user_je_source_name , ");
      sb.append(" segment1, ");
      sb.append(" segment2, ");
      sb.append(" segment3, ");
      sb.append(" segment4, ");
      sb.append(" segment5, ");
      sb.append(" segment6, ");
      sb.append(" segment7, ");
      sb.append("  entered_dr, ");
      sb.append(" entered_cr, ");
      sb.append(" reference4, ");
      sb.append(" period_name) ");
      sb.append("VALUES (:STATUS, :SET_OF_BOOKS_ID,SYSDATE,:CURRENCY_CODE,SYSDATE,:CREATED_BY, :ACTUAL_FLAG,");
      sb.append(":USER_JE_CATEGORY_NAME, :USER_JE_SOURCE_NAME, :SEGMENT1, :SEGMENT2, :SEGMENT3, :SEGMENT4, :SEGMENT5, :SEGMENT6, :SEGMENT7, :ENTERED_DR, :ENTERED_CR, :DESCRIPTION, :PERIOD_NAME) ");
     
      pst = (OraclePreparedStatement) conn.prepareStatement(sb.toString());

      for (GLBean glBean:glBeanList)
      {
        pst.setStringAtName("STATUS", glBean.getStatus());
        pst.setStringAtName("SET_OF_BOOKS_ID", "42");
        pst.setStringAtName("CURRENCY_CODE", glBean.getCurrencyCode());
        pst.setStringAtName("CREATED_BY", glBean.getCreatedBy());
        pst.setStringAtName("ACTUAL_FLAG", glBean.getActualFlag());
        pst.setStringAtName("USER_JE_CATEGORY_NAME", glBean.getJeUserCategoryName());
        pst.setStringAtName("USER_JE_SOURCE_NAME", glBean.getJeUserSourceName());
        pst.setStringAtName("SEGMENT1", glBean.getSegment1());
        pst.setStringAtName("SEGMENT2", glBean.getSegment2());
        pst.setStringAtName("SEGMENT3", glBean.getSegment3());
        pst.setStringAtName("SEGMENT4", glBean.getSegment4());
        pst.setStringAtName("SEGMENT5", glBean.getSegment5());
        pst.setStringAtName("SEGMENT6", glBean.getSegment6());
        pst.setStringAtName("SEGMENT7", glBean.getSegment7());
        pst.setStringAtName("ENTERED_DR", glBean.getEnteredDr());
        pst.setStringAtName("ENTERED_CR", glBean.getEnteredCr());
        pst.setStringAtName("DESCRIPTION", glBean.getDescription());
        pst.setStringAtName("PERIOD_NAME", glBean.getPeriodName());
        
        pst.addBatch();
        pst.clearParameters();
      }
      pst.executeBatch();
    }
    catch (Exception e)
    {
      log4jLogger.error(e);
      return e.getMessage();
    }
    finally
    {
      JDBCUtil.close(pst);
    }
    return null;
  }

  public static File archiveFile(File file)
  {
    File archivedFile = null;
    try
    {
      DateFormat dateFormat = new SimpleDateFormat("M-dd-yyyy_k-m-s");
      StringBuilder sb = new StringBuilder();
      sb.append(PropertiesServlet.getProperty("workday.glinputdirectory"));
      sb.append(File.separator);
      sb.append("archive");
      sb.append(File.separator);
      sb.append(StringUtils.substringAfterLast(file.getCanonicalPath(), File.separator));
      sb.append("_processed_on_");
      sb.append(dateFormat.format(new Date()));
      archivedFile = new File(sb.toString());
      file.renameTo(archivedFile);
    }
    catch (Exception e)
    {
      log4jLogger.error(e);
    }
    return archivedFile;
  }

  public static String callAppsInitialize(OracleConnection conn)
  {
    try
    {
      String procedure = "{call FND_GLOBAL.APPS_INITIALIZE(?, ?, ?)}";
      OracleCallableStatement cstmt = (OracleCallableStatement) conn.prepareCall(procedure);
      cstmt.setString("user_id", "1118");
      cstmt.setString("resp_id", "50158");
      cstmt.setString("resp_appl_id", "101");
      cstmt.execute();
    }
    catch (Exception e)
    {
      log4jLogger.error(e);
    }
    return null;
  }

  public static String queryRunID(OracleConnection conn)
  {
    OracleStatement st = null;
    OracleResultSet rs = null;
    String runID = null;

    try
    {
      StringBuilder sb = new StringBuilder();
      sb.append("SELECT gl_interface_control_s.NEXTVAL FROM dual");
      st = (OracleStatement) conn.createStatement();
      rs = (OracleResultSet) st.executeQuery(sb.toString());
      if (rs.next())
      {
        runID = rs.getString(1);
      }
    }
    catch (Exception e)
    {
      log4jLogger.error(e);
    }
    finally
    {
      JDBCUtil.close(st, rs);
    }
    return runID;
  }

  public static void insertIntoGLInterfaceControl(OracleConnection conn, String runID)
  {
    OraclePreparedStatement pst = null;
    try
    {
      StringBuilder sb = new StringBuilder();
      sb.append("INSERT into gl_interface_control (JE_SOURCE_NAME,STATUS,INTERFACE_RUN_ID,GROUP_ID,SET_OF_BOOKS_ID) ");
      sb.append("VALUES (:JE_SOURCE_NAME,:STATUS,:INTERFACE_RUN_ID,:GROUP_ID,:SET_OF_BOOKS_ID) ");

      pst = (OraclePreparedStatement) conn.prepareStatement(sb.toString());
      pst.setStringAtName("JE_SOURCE_NAME", "Payroll");
      pst.setStringAtName("STATUS", "S");
      pst.setStringAtName("INTERFACE_RUN_ID", runID);
      pst.setStringAtName("GROUP_ID", null);
      pst.setStringAtName("SET_OF_BOOKS_ID", "42");
      pst.executeUpdate();
    }
    catch (Exception e)
    {
      log4jLogger.error(e);
    }
    finally
    {
      JDBCUtil.close(pst);
    }
  }

  public static String callFNDRequestSumbitRequest(OracleConnection conn, String runID)
  {
    String requestId = null;
    try
    {
      StringBuilder sb = new StringBuilder();
      sb.append("begin ");
      sb.append("? := FND_REQUEST.SUBMIT_REQUEST( ");
      sb.append("              application => 'SQLGL',  ");
      sb.append("              program     => 'GLLEZL',  ");
      sb.append("              description => 'Journal Import', ");
      sb.append("              argument1   =>  '");
      sb.append(runID);
      sb.append("',            argument2   => ''||42, ");
      sb.append("              argument3   => ''||'Y', ");
      sb.append("              argument4   => '', ");
      sb.append("              argument5   => '', ");
      sb.append("              argument6   => ''||'Y', ");
      sb.append("              argument7   => ''||'N' ); ");
      sb.append("end; ");

      log4jLogger.info(sb.toString());
      OracleCallableStatement cstmt = (OracleCallableStatement) conn.prepareCall(sb.toString());
      cstmt.registerOutParameter(1, Types.NUMERIC);

      cstmt.execute();
      requestId = cstmt.getString(1);
      log4jLogger.info("Request ID is " + requestId);
    }
    catch (Exception e)
    {
      log4jLogger.error(e);
    }
    return requestId;
  }

  public static String callFNDConcurrentWaitForRequest(OracleConnection conn, String requestId)
  {
    CallableStatement cst = null;
    OracleResultSet rs = null;
    StringBuilder emailBody = new StringBuilder();
    String message = null;
    
    try
    {
      StringBuilder sb = new StringBuilder();
      sb.append("declare ");
      sb.append("resultBool BOOLEAN; ");
      sb.append("resultVarchar VARCHAR2(2); ");
      sb.append("begin ");
      sb.append("resultBool :=FND_CONCURRENT.WAIT_FOR_REQUEST(?,?,?,?,?,?,?,?); ");
      sb.append("if resultBool then resultVarchar := 'T'; else resultVarchar :='F'; end if; ");
      sb.append("? := resultVarchar; " );
      sb.append("end; ");
      cst = conn.prepareCall(sb.toString());
      cst.setString(1, requestId);
      cst.setString(2, "02");
      cst.setString(3, "0");
      cst.registerOutParameter(4, java.sql.Types.VARCHAR);
      cst.registerOutParameter(5, java.sql.Types.VARCHAR);
      cst.registerOutParameter(6, java.sql.Types.VARCHAR);
      cst.registerOutParameter(7, java.sql.Types.VARCHAR);
      cst.registerOutParameter(8, java.sql.Types.VARCHAR);
      cst.registerOutParameter(9, java.sql.Types.VARCHAR);
      cst.execute();
      log4jLogger.info("phase "+cst.getString(4));
      log4jLogger.info("status "+cst.getString(5));
      log4jLogger.info("dev_phase "+cst.getString(6));
      log4jLogger.info("dev_status "+cst.getString(7));
      message = cst.getString(8);
      log4jLogger.info("message "+message);
      log4jLogger.info("Return Status "+cst.getString(9));
      
      emailBody.append("Request ID: ");
      emailBody.append(requestId);
      emailBody.append("<br>");
      emailBody.append("Phase: ");
      emailBody.append(cst.getString(4));
      emailBody.append("<br>");
      emailBody.append("Status: ");
      emailBody.append(cst.getString(5));
      emailBody.append("<br>");
      if (StringUtils.isNotEmpty(message))
      {
        emailBody.append("Message: ");
        emailBody.append(message);
        emailBody.append("<br>");
      }
    }
    catch (Exception e)
    {
      log4jLogger.error(e);
    }
    finally
    {
      JDBCUtil.close(cst, rs);
    }

    return emailBody.toString();
  }  

  public static void deleteGLInterfaceControl(OracleConnection conn, String runID)
  {
    OraclePreparedStatement pst = null;
    try
    {
      StringBuilder sb = new StringBuilder();
      sb.append("DELETE FROM GL_INTERFACE_CONTROL ");
      sb.append("WHERE je_source_name = 'Payroll' AND interface_run_id = :RUN_ID");

      pst = (OraclePreparedStatement) conn.prepareStatement(sb.toString());
      pst.setStringAtName("RUN_ID", runID);
      pst.executeUpdate();
    }
    catch (Exception e)
    {
      log4jLogger.error(e);
    }
    finally
    {
      JDBCUtil.close(pst);
    }
  }

  public static File createJournalEntryFile(OracleConnection conn)
  {
    File journalEntryFile = new File(PropertiesServlet.getProperty("workday.gloutputdirectory")+File.separator+"JournalEntryCanada.csv");
    OracleStatement st = null;
    OracleResultSet rs = null;
    try
    {
      StringBuilder sb = new StringBuilder();
      sb.append("SELECT g.name||','||l.entered_dr||','||l.entered_cr||','|| ");
      sb.append("c.segment1||'-'||c.segment2||'-'||c.segment3||'-'||c.segment4||'-'|| ");
      sb.append("c.segment5||'-'||c.segment6||'-'||c.segment7||','|| ");
      sb.append("g.currency_code||','||l.je_line_num||','||g.period_name ");
      sb.append("FROM   gl_code_combinations c, gl_je_lines l, gl_je_headers g ");
      sb.append("WHERE  g.je_batch_id = (SELECT max(je_batch_id) "); 
      sb.append("FROM   GL_JE_BATCHES ");
      sb.append("WHERE  name like 'Payroll%') ");
      sb.append("AND    g.je_header_id = l.je_header_id ");
      sb.append("AND    c.code_combination_id = l.code_combination_id ");
      sb.append("ORDER BY g.currency_code desc, g.name, c.segment5 ");
      
      st = (OracleStatement) conn.createStatement();
      rs = (OracleResultSet) st.executeQuery(sb.toString());
      
      StringBuilder fileText = new StringBuilder();
      while (rs.next())
      {
        fileText.append(rs.getString(1));
        fileText.append("\\n");
      }

      FileWriter fw = new FileWriter(journalEntryFile);
      fw.write(StringEscapeUtils.unescapeJava(fileText.toString()));
      fw.close();
    }
    catch (Exception e)
    {
      log4jLogger.error(e);
    }
    finally
    {
      JDBCUtil.close(st, rs);
    }
    return journalEntryFile;
  }
}