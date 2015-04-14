package com.valspar.interfaces.wercs.msdsrequest.program;

import com.valspar.interfaces.common.BaseInterface;
import com.valspar.interfaces.common.beans.ConnectionAccessBean;
import com.valspar.interfaces.common.enums.DataSource;
import com.valspar.interfaces.common.servlets.PropertiesServlet;
import com.valspar.interfaces.common.utils.*;
import com.valspar.interfaces.wercs.msdsrequest.beans.*;
import java.io.*;
import java.sql.*;
import java.util.ArrayList;
import oracle.jdbc.*;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;

public class MsdsRequestInterface extends BaseInterface
{
  private static Logger log4jLogger = Logger.getLogger(MsdsRequestInterface.class);
  private OracleConnection wercsConn = null;

  public MsdsRequestInterface()
  {
  }

  public void execute()
  {
    try
    {
      setWercsConn((OracleConnection) ConnectionAccessBean.getConnection(DataSource.WERCS));
      ArrayList<RequestBean> requestBeanList = buildRequestBeans();
      log4jLogger.info("There are " + requestBeanList.size() + " requests to process.");
      if (!requestBeanList.isEmpty())
      {
        processRequestBeans(requestBeanList);
      }
      log4jLogger.info("The MsdsRequestInterface has completed processing.");
    }
    catch (Exception e)
    {
      log4jLogger.error(e);
    }
    finally
    {
      JDBCUtil.close(getWercsConn());
    }
  }

  public ArrayList<RequestBean> buildRequestBeans()
  {
    ArrayList<RequestBean> requestBeanList = new ArrayList<RequestBean>();
    Statement stmt = null;
    ResultSet rs = null;
    try
    {
      StringBuilder sql = new StringBuilder();
      sql.append("select request_id, request_type, extract_from, email, delivery_method, added_by, date_added, TO_CHAR(publish_date,'MM/DD/YYYY'), ");
      sql.append("(select customer_name from vca_msds_customer where customer_id = vca_msds_request_queue.customer_id)  ");
      sql.append("from VCA_MSDS_REQUEST_QUEUE ");
      sql.append("where status = 0  ");
      sql.append("order by date_added ");

      stmt = getWercsConn().createStatement();
      rs = stmt.executeQuery(sql.toString());

      while (rs.next())
      {
        RequestBean rb = new RequestBean();
        rb.setRequestId(rs.getString(1));
        rb.setRequestType(rs.getString(2));
        rb.setExtractFrom(rs.getString(3));
        rb.setEmail(rs.getString(4));
        rb.setDeliveryMethod(rs.getString(5));
        rb.setAddedBy(rs.getString(6));
        rb.setDateAdded(rs.getString(7));
        rb.setPublishDate(rs.getString(8));
        rb.setCustomerName(rs.getString(9));
        requestBeanList.add(rb);
        updateQueueStatus(rb, 1);
      }
    }
    catch (Exception e)
    {
      log4jLogger.error(e);
    }
    finally
    {
      JDBCUtil.close(stmt, rs);
    }
    return requestBeanList;
  }

  public void processRequestBeans(ArrayList<RequestBean> requestBeanList)
  {
    log4jLogger.info("Starting to process the next " + requestBeanList.size() + " requests from VCA_MSDS_REQUEST_QUEUE...");
    for (RequestBean requestBean: requestBeanList)
    {
      if (StringUtils.equalsIgnoreCase("MassMSDS", requestBean.getRequestType()) || StringUtils.equalsIgnoreCase("CustomerMsds", requestBean.getRequestType()))
      {
        writePDFFiles(requestBean);
      }
      else if (StringUtils.equalsIgnoreCase("QuickFDS", requestBean.getRequestType()))
      {
        writeQuickFdsPDFFiles(requestBean);
      }
      StringBuilder emailMessage = new StringBuilder();
      if (StringUtils.equalsIgnoreCase(requestBean.getDeliveryMethod(), "email"))
      {
        if (!requestBean.getFileList().isEmpty())
        {
          String zipFileName = requestBean.getRequestType() + requestBean.getRequestId() + ".zip";
          ZipUtility.zipFiles(requestBean);
          emailMessage.append(getProperty("webserver") + "/interfaces/reports/" + zipFileName);
        }
        else
        {
          emailMessage.append("Your request did not return any results.");
        }
      }
      if (StringUtils.equalsIgnoreCase(requestBean.getDeliveryMethod(), "ftp") && StringUtils.equalsIgnoreCase(requestBean.getRequestType(), "QuickFDS"))
      {
        emailMessage.append("Start Date: ");
        emailMessage.append(requestBean.getPublishDate());
        emailMessage.append("\r\n");
        emailMessage.append("\r\n");
        emailMessage.append("Your QuickFDS request has been processed. See logs for details on files created. ");
      }
      if (requestBean.getEmail() != null)
      {
        String emailSubject = "";
        if (StringUtils.equalsIgnoreCase("CustomerMsds", requestBean.getRequestType()))
        {
          emailSubject = requestBean.getRequestType() + " - " + requestBean.getCustomerName();
        }
        else
        {
          emailSubject = requestBean.getRequestType();
        }
        EmailBean.emailMessage(emailSubject, emailMessage.toString(), requestBean.getEmail());
      }
      updateQueueStatus(requestBean, 2);
    }
    log4jLogger.info("Done processing group.  Going to pick up more from VCA_MSDS_REQUEST_QUEUE.");
  }

  public void writePDFFiles(RequestBean rb)
  {
    PreparedStatement pstmt = null;
    OracleResultSet rs = null;
    StringBuilder sb = new StringBuilder();
    sb.append("SELECT replace(replace(C.PRODUCT,'/'),'\\'), DECODE(A.F_SUBFORMAT,'PDS','CPDS',A.F_SUBFORMAT), A.F_LANGUAGE, ");
    sb.append("F_PRODUCT_NAME, to_char(F_DATE_REVISED,'MM/DD/YY'), (select upc_code from ic_item_mst@tona where item_no = C.product) as upc, ");
    sb.append("f_pdf ");

    if (rb.getExtractFrom().equalsIgnoreCase("Archive"))
    {
      sb.append("FROM WERCS.T_PDF_MSDS@RHST.VALSPAR.COM A, ");
    }
    else
    {
      sb.append("FROM WERCS.T_PDF_MSDS A, ");
    }
    sb.append("wercs.vca_msds_request_queue b, ");
    sb.append("WERCS.VCA_MSDS_REQUEST_PRODUCTS C,   ");
    sb.append("vca_msds_customer d, vca_msds_subformat e, vca_msds_language f ");
    sb.append("WHERE C.REQUEST_ID = ? ");
    sb.append("and b.request_id = c.request_id ");
    sb.append("and b.customer_id = d.customer_id (+)  ");
    sb.append("and d.customer_id = e.customer_id (+) ");
    sb.append("and e.subformat_id = f.subformat_id (+)  ");
    sb.append("and a.f_pdf is not null ");
    sb.append("and A.F_SUBFORMAT = NVL(e.subformat,A.F_SUBFORMAT) ");
    sb.append("and a.f_language = nvl(F.SUBFORMAT_LANGUAGE, a.f_language) ");
    sb.append("AND A.F_PRODUCT = get_published_alias(C.PRODUCT) ");
    if (rb.getExtractFrom().equalsIgnoreCase("Valapps"))
    {
      sb.append("AND F_AUTHORIZED <> 0 ");
    }
    sb.append("AND a.f_date_stamp = ");
    sb.append("                (SELECT MAX (f_date_stamp) ");
    if (rb.getExtractFrom().equalsIgnoreCase("Archive"))
    {
      sb.append("                   FROM t_pdf_msds@RHST.VALSPAR.COM ");
    }
    else
    {
      sb.append("                   FROM t_pdf_msds ");
    }
    sb.append("                  WHERE     f_product = a.f_product ");
    sb.append("                        AND f_subformat = a.f_subformat ");
    sb.append("                        AND f_language = a.f_language) ");
    sb.append("ORDER BY A.F_PRODUCT, A.F_SUBFORMAT ASC, A.F_DATE_STAMP desc ");

    try
    {
      pstmt = getWercsConn().prepareStatement(sb.toString());
      pstmt.setString(1, rb.getRequestId());
      rs = (OracleResultSet) pstmt.executeQuery();

      while (rs.next())
      {
        String product = rs.getString(1);
        String subformat = rs.getString(2);
        String language = rs.getString(3);
        String description = rs.getString(4);
        String revDate = rs.getString(5);
        String upcCode = rs.getString(6);
        String pdfFileName = rb.getRequestId() + product + subformat + language + ".pdf";
        String zipFileName = subformat + "_" + product + ".pdf";
        File pdfFile = new File(CommonUtility.getDataDirectoryPath() + File.separator + pdfFileName);
        FileUtils.copyInputStreamToFile(rs.getBinaryStream(7), pdfFile);

        if (rb.getIdList().indexOf(pdfFileName) == -1)
        {
          ZipFileBean zfBean = new ZipFileBean();
          zfBean.setFileToZip(pdfFile);
          zfBean.setZipFileName(subformat + File.separator + language + File.separator + zipFileName);
          rb.getIdList().add(pdfFileName);
          rb.getFileList().add(zfBean);

          IndexBean iBean = new IndexBean();
          iBean.setProduct(product);
          iBean.setDescription(description);
          iBean.setLanguage(language);
          iBean.setRevDate(revDate);
          iBean.setUpcCode(upcCode);
          iBean.setFileName(zipFileName);
          rb.getIndex().add(iBean);
        }
      }
      if (StringUtils.equalsIgnoreCase("CustomerMsds", rb.getRequestType()) && rb.getIndex().size() > 0)
      {
        createIndexFile(rb);
      }
    }
    catch (Exception e)
    {
      log4jLogger.error(e);
    }
    finally
    {
      JDBCUtil.close(pstmt, rs);
    }
  }

  public void createIndexFile(RequestBean rb)
  {
    try
    {
      File writePath = new File(CommonUtility.getDataDirectoryPath());
      String indexFileName = rb.getRequestId() + "index.csv";
      BufferedWriter out = new BufferedWriter(new FileWriter(writePath + File.separator + indexFileName));
      out.write("Product #, Description, UPC Code, Date Revised, Language, Filename");
      out.newLine();

      for (IndexBean iBean: rb.getIndex())
      {
        out.write(iBean.getProduct());
        out.write(",");
        out.write(iBean.getDescription());
        out.write(",");
        if (iBean.getUpcCode() != null)
        {
          out.write(iBean.getUpcCode());
        }
        else
        {
          out.write(" ");
        }
        out.write(",");
        out.write(iBean.getRevDate());
        out.write(",");
        out.write(iBean.getLanguage());
        out.write(",");
        out.write(iBean.getFileName());
        out.newLine();
      }

      out.close();

      ZipFileBean zfBean = new ZipFileBean();
      zfBean.setFileToZip(new File(writePath + File.separator + indexFileName));
      zfBean.setZipFileName("index.csv");
      rb.getFileList().add(zfBean);
    }
    catch (Exception e)
    {
      log4jLogger.error(e);
    }
  }

  public void writeQuickFdsPDFFiles(RequestBean rb)
  {
    OracleResultSet rs = null;
    PreparedStatement pstmt = null;

    StringBuilder sb = new StringBuilder();
    sb.append("  SELECT REPLACE (REPLACE (A.F_PRODUCT, '/'), '\\'), ");
    sb.append("         A.F_SUBFORMAT, ");
    sb.append("         A.F_LANGUAGE, ");
    sb.append("         F_PDF ");
    sb.append("    FROM t_pdf_msds a, ");
    sb.append("         t_product_alias_names n, ");
    sb.append("         t_prod_text t, ");
    sb.append("         vca_msds_request_queue q, ");
    sb.append("         vca_lookups v1, ");
    sb.append("         vca_lookups v2 ");
    sb.append("   WHERE     a.f_product = n.f_product ");
    sb.append("         AND n.f_product = t.f_product ");
    sb.append("         AND q.request_id = ? ");
    sb.append("         AND v1.keyfield1 = 'QUICKFDS_SUBFORMAT' ");
    sb.append("         AND v2.keyfield1 = 'QUICKFDS_REGION' ");
    sb.append("         AND a.F_AUTHORIZED <> 0 ");
    sb.append("         AND t.f_data_code = 'REGION' ");
    sb.append("         AND t.f_text_code = v2.keyfield2 ");
    sb.append("         AND a.f_subformat = v1.keyfield2 ");
    sb.append("         AND (   get_wercs_text_code (n.f_alias, 'FRMCT') <> 'FRMCT014' ");
    sb.append("              OR get_wercs_text_code (n.f_alias, 'FRMCT') IS NULL) ");
    sb.append("         AND n.f_alias NOT LIKE '%.%' ");
    sb.append("         AND n.f_product NOT LIKE '92%' ");
    sb.append("         AND n.f_product NOT LIKE 'T-%' ");
    sb.append("         AND TRUNC (a.f_published_date) >= q.publish_date ");
    sb.append("         AND TRUNC (a.f_published_date) <= NVL (q.publish_end_date, SYSDATE) ");
    sb.append("         AND f_pdf IS NOT NULL ");
    sb.append("         AND a.f_date_stamp = ");
    sb.append("                (SELECT MAX (f_date_stamp) ");
    sb.append("                   FROM t_pdf_msds ");
    sb.append("                  WHERE     f_product = a.f_product ");
    sb.append("                        AND f_subformat = a.f_subformat ");
    sb.append("                        AND f_language = a.f_language) ");
    sb.append("ORDER BY A.F_SUBFORMAT, A.F_LANGUAGE ASC, A.F_DATE_STAMP DESC ");

    try
    {
      pstmt = getWercsConn().prepareStatement(sb.toString());
      pstmt.setString(1, rb.getRequestId());
      rs = (OracleResultSet) pstmt.executeQuery();
      String subformatDirectoryString = "";
      String prevSubformatDirectoryString = "";
      while (rs.next())
      {
        String product = rs.getString(1);
        String subformat = rs.getString(2);
        String language = rs.getString(3);
        String pdfFileName = product + ".pdf";
        subformatDirectoryString = subformat + language;

        //If writing to new subformat directory, ftp previous directory to quickfds and delete off server
        if (!prevSubformatDirectoryString.equalsIgnoreCase(subformatDirectoryString))
        {
          if (!StringUtils.isEmpty(prevSubformatDirectoryString))
          {
            File fromDirectory = new File(CommonUtility.getDataDirectoryPath() + File.separator + "quickfds" + File.separator + prevSubformatDirectoryString + File.separator + rb.getRequestId());
            String toPath = getProperty("fds_ftp_dir") + File.separator + prevSubformatDirectoryString;
            //  FtpUtility.ftpMultipleFiles(getProperty("fds_ftp_user"), fromDirectory.toString(), ".pdf", getProperty("fds_ftp_server"), toPath);
            FtpUtility.sendFileOrDirectory(getProperty("fds_ftp_server"), "autoapps", PropertiesServlet.getProperty("autoapps.password"), fromDirectory.toString(), toPath, ".pdf");
            log4jLogger.info("Ftp'd to directory " + toPath);
            deleteDir(fromDirectory);
            log4jLogger.info("Deleted directory " + fromDirectory);
          }
          prevSubformatDirectoryString = subformatDirectoryString;
        }
        File subFormatDirectory = new File(CommonUtility.getDataDirectoryPath() + File.separator + "quickfds" + File.separator + subformatDirectoryString);
        if (subFormatDirectory.mkdirs())
        {
          subFormatDirectory.setWritable(true, false);
          subFormatDirectory.setReadable(true, false);
        }
        File writeDirectory = new File(subFormatDirectory + File.separator + rb.getRequestId());
        writeDirectory.mkdirs();
        FileUtils.copyInputStreamToFile(rs.getBinaryStream(4), new File(writeDirectory + File.separator + pdfFileName));
        log4jLogger.info("Wrote msds: product = " + product + ", subformat = " + subformat + ", language = " + language);
      }
      if (!StringUtils.isEmpty(prevSubformatDirectoryString))
      {
        File fromDirectory = new File(CommonUtility.getDataDirectoryPath() + File.separator + "quickfds" + File.separator + prevSubformatDirectoryString + File.separator + rb.getRequestId());
        String toPath = getProperty("fds_ftp_dir") + File.separator + prevSubformatDirectoryString;
        // FtpUtility.ftpMultipleFiles(getProperty("fds_ftp_user"), fromDirectory.toString(), ".pdf", getProperty("fds_ftp_server"), toPath);
        FtpUtility.sendFileOrDirectory(getProperty("fds_ftp_server"), "autoapps", PropertiesServlet.getProperty("autoapps.password"), fromDirectory.toString(), toPath, ".pdf");
        log4jLogger.info("Ftp'd to directory " + toPath);
        deleteDir(fromDirectory);
        log4jLogger.info("Deleted directory " + fromDirectory);
      }
    }
    catch (Exception e)
    {
      log4jLogger.error(e);
    }
    finally
    {
      JDBCUtil.close(pstmt, rs);
    }
  }

  public void updateQueueStatus(RequestBean rb, int status)
  {
    Statement stmt = null;
    try
    {
      StringBuilder sql = new StringBuilder();
      sql.append("update VCA_MSDS_REQUEST_QUEUE set STATUS = ");
      sql.append(status);
      if (status == 1)
        sql.append(", START_TIME = SYSDATE ");
      else
        sql.append(", END_TIME = SYSDATE ");

      sql.append("WHERE REQUEST_ID = ");
      sql.append(rb.getRequestId());

      stmt = getWercsConn().createStatement();
      stmt.executeUpdate(sql.toString());
    }
    catch (Exception e)
    {
      log4jLogger.error(e);
    }
    finally
    {
      JDBCUtil.close(stmt);
    }
  }

  public static boolean deleteDir(File dir)
  {
    if (dir.isDirectory())
    {
      String[] children = dir.list();
      for (int i = 0; i < children.length; i++)
      {
        boolean success = deleteDir(new File(dir, children[i]));
        if (!success)
        {
          return false;
        }
      }
    }
    return dir.delete();
  }

  public void setWercsConn(OracleConnection wercsConn)
  {
    this.wercsConn = wercsConn;
  }

  public OracleConnection getWercsConn()
  {
    return wercsConn;
  }
}
