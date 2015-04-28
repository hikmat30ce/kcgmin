package com.valspar.interfaces.guardsman.pos.program;

import com.valspar.interfaces.common.BaseInterface;
import com.valspar.interfaces.guardsman.pos.beans.*;
import com.valspar.interfaces.guardsman.pos.utility.*;
import java.text.SimpleDateFormat;
import java.util.*;
import oracle.jdbc.*;
import org.apache.log4j.Logger;

public class GuardsmanPosLiteInterface extends BaseInterface
{
  static Logger log4jLogger = Logger.getLogger(GuardsmanPosLiteInterface.class.getName());

  public GuardsmanPosLiteInterface()
  {
  }

  public void execute()
  {
    log4jLogger.info("Starting Guardsman POS Lite");
    ConnectionBean cb = new ConnectionBean();
    OracleConnection conn = null;

    try
    {
      conn = cb.openConnection();
      retailerLoop(conn);
      sendSummaryEmail(conn);
    }
    finally
    {
      cb.closeConnection(conn);
    }

    log4jLogger.info("Ending Guardsman POS Lite");
  }

  public static void retailerLoop(OracleConnection conn)
  {
    String retailerArray[] = getRetailerArray(conn);
    String erpRtlrNo = null;
    int arrayLength = retailerArray.length;

    for (int i = 0; i < arrayLength; i++)
    {
      PosFileBean pfb = new PosFileBean();
      pfb.setConnection(conn);
      erpRtlrNo = retailerArray[i];
      pfb.setFileFormat("POSLITE");
      SimpleDateFormat sdf = new SimpleDateFormat("MM'/'d'/'yyyy' at 'hh:mm:ss aaa");
      java.util.Date d = Calendar.getInstance().getTime();
      pfb.setFileRunDt(sdf.format(d));
      pfbBuilder(pfb, erpRtlrNo);
      fetchPosFhId(pfb);
      writePosFhRecord(pfb);
      writeLastInvoiceDate(pfb);
      new Invoice(pfb);
      reportErrors(pfb);
    }
  }

  public static String[] getRetailerArray(OracleConnection conn)
  {

    //new PosFileBean to get a connection
    PosFileBean pfb = new PosFileBean();
    pfb.setConnection(conn);

    /*
     * the retailer array to be returned
     * size based on the getRetailerCount
     */
    String retailerArray[] = new String[getRetailerCount(conn)];

    //iterater to fill each element of the array
    int i = 0;

    //determining week    
    Date dateNow = new Date();
    SimpleDateFormat weekOfMonthFormat = new SimpleDateFormat("W");
    weekOfMonthFormat.format(dateNow);
    String week = ("%" + weekOfMonthFormat.format(dateNow) + "%");

    StringBuilder sb = new StringBuilder();
    sb.append("select erp.erp_rtlr_no ");
    sb.append("from sam_rtlr_ftp ftp, sam_rtlr_erp erp ");
    sb.append("where ftp.rtlr_ftp_id = erp.rtlr_ftp_id ");
    sb.append("and ftp.file_format = 'POSLITE' ");
    sb.append("and ftp.frequency like ?");
    sb.append("order by 1 ");
    OraclePreparedStatement pstmt = null;
    OracleResultSet rs = null;
    try
    {
      pstmt = (OraclePreparedStatement) pfb.getConnection().prepareStatement(sb.toString());
      pstmt.setString(1, week);
      rs = (OracleResultSet) pstmt.executeQuery();
      while (rs.next())
      {
        retailerArray[i] = rs.getString(1);
        i++;
      }
    }
    catch (Exception e)
    {
      log4jLogger.error("Error in PosLiteInvoicing.getRetailerArray(conn): " + e);
    }
    finally
    {
      TryCleanup.tryCleanup(pfb, pstmt, rs);
    }

    return retailerArray;
  }

  public static int getRetailerCount(OracleConnection conn)
  {
    //new PosFileBean to get a connection
    PosFileBean pfb = new PosFileBean();
    pfb.setConnection(conn);


    //the counter to be returned
    int retailerCount = 0;

    //determining week    
    Date dateNow = new Date();
    SimpleDateFormat weekOfMonthFormat = new SimpleDateFormat("W");
    weekOfMonthFormat.format(dateNow);
    String week = ("%" + weekOfMonthFormat.format(dateNow) + "%");

    //build sql statement
    StringBuilder sb = new StringBuilder();
    sb.append("select count(erp.erp_rtlr_no) ");
    sb.append("from sam_rtlr_ftp ftp, sam_rtlr_erp erp ");
    sb.append("where ftp.rtlr_ftp_id = erp.rtlr_ftp_id ");
    sb.append("and ftp.file_format = 'POSLITE' ");
    sb.append("and ftp.frequency like ? ");

    OraclePreparedStatement pstmt = null;
    OracleResultSet rs = null;
    try
    {
      pstmt = (OraclePreparedStatement) pfb.getConnection().prepareStatement(sb.toString());
      pstmt.setString(1, week);
      rs = (OracleResultSet) pstmt.executeQuery();
      while (rs.next())
      {
        retailerCount = rs.getInt(1);
      }
    }
    catch (Exception e)
    {
      log4jLogger.error("Error in PosLiteInvoicing.getRetailerCount(conn): " + e);
    }
    finally
    {
      TryCleanup.tryCleanup(pfb, pstmt, rs);
    }
    return retailerCount;
  }

  public static void pfbBuilder(PosFileBean pfb, String erpRtlrNo)
  {

    StringBuilder sb = new StringBuilder();
    sb.append("SELECT erp.erp_rtlr_no, ftp.description, ftp.email, ftp.invoice_email, ");
    sb.append("     ftp.status, ftp.auto_invoice_flg, ftp.plan_printing_flg, ");
    sb.append("     ftp.man_return_flg, ftp.invoice_by_store, ftp.rtlr_ftp_id, ");
    sb.append("     ftp.auto_invoice_to_jba, tally_disc, send_invoice_detail_rpt, ");
    sb.append("     TO_CHAR(ftp.last_invoice,'YYMMDD'), ");
    sb.append("     TO_CHAR(SYSDATE,'YYMMDD')");
    sb.append("FROM sam_rtlr_ftp ftp, sam_rtlr_erp erp ");
    sb.append("WHERE ftp.rtlr_ftp_id = erp.rtlr_ftp_id ");
    sb.append("AND ftp.file_format = 'POSLITE' ");
    sb.append("AND erp.erp_rtlr_no = ? ");

    OraclePreparedStatement pstmt = null;
    OracleResultSet rs = null;
    try
    {
      pstmt = (OraclePreparedStatement) pfb.getConnection().prepareStatement(sb.toString());
      pstmt.setString(1, erpRtlrNo);
      rs = (OracleResultSet) pstmt.executeQuery();
      while (rs.next())
      {
        pfb.getUniqueRetailerMap().put(rs.getString(1), rs.getString(1));
        pfb.setRetailerName(rs.getString(2));
        pfb.setEmailAddr(rs.getString(3));
        pfb.setInvoiceEmailAddr(rs.getString(4));
        if (rs.getString(5).equalsIgnoreCase("A"))
        {
          pfb.setRetailerActive(true);
        }
        if (rs.getString(6) != null)
        {
          if (rs.getString(6).equalsIgnoreCase("Y"))
          {
            pfb.setAutoInvoice(true);
          }
        }
        if (rs.getString(7) != null)
        {
          if (rs.getString(7).equalsIgnoreCase("Y"))
          {
            pfb.setPrintPlans(true);
          }
        }
        if (rs.getString(8) != null)
        {
          if (rs.getString(8).equalsIgnoreCase("Y"))
          {
            pfb.setManualReturns(true);
          }
        }
        /* ALWAYS INVOICE BY ERP
        if (rs.getString(9) != null)
        {
          if (rs.getString(9).equalsIgnoreCase("Y"))
          {
            pfb.setInvoiceByStore(true);
          }
        }
        */
        pfb.setRtlrFtpId(rs.getString(10));
        pfb.setAutoInvoiceToJba("Y"); //rs.getString(11));
        pfb.setTallyDisc(rs.getString(12));

        if (rs.getString(13) != null)
        {
          if (rs.getString(13).equalsIgnoreCase("Y"))
          {
            pfb.setSendInvoiceDetailRpt(true);
          }
        }
        //pfb header info
        pfb.setFileHeader(true);
        pfb.setFileRunDt(rs.getString(14));
        pfb.setFileExtractStartDt(rs.getString(14));
        pfb.setFileExtractEndDt(rs.getString(15));
      }
    }
    catch (Exception e)
    {
      ExceptionLogger.logException(pfb, "PosLiteInvoicing", "pfbBuilder", null, e);
    }
    finally
    {
      TryCleanup.tryCleanup(pfb, pstmt, rs);
    }
  }

  public static void fetchPosFhId(PosFileBean pfb)
  {
    String tempId = "0";
    int counterFound = 0;
    StringBuilder sb = new StringBuilder();
    sb.append("select last_one + 1 from counter where item =?");
    StringBuilder updateSb = new StringBuilder();
    updateSb.append("update counter set last_one = ? where item = ?");
    OraclePreparedStatement pstmtUpdate = null;
    OraclePreparedStatement pstmt = null;
    OracleResultSet rs = null;
    try
    {
      pstmt = (OraclePreparedStatement) pfb.getConnection().prepareStatement(sb.toString());
      pstmtUpdate = (OraclePreparedStatement) pfb.getConnection().prepareStatement(updateSb.toString());
      pstmt.setString(1, "sam_pos_fh.pos_fh_id");
      rs = (OracleResultSet) pstmt.executeQuery();
      while (rs.next())
      {
        tempId = rs.getString(1);
        counterFound += 1;
      }
      if (counterFound == 1)
      {
        pstmtUpdate.setString(1, tempId);
        pstmtUpdate.setString(2, "sam_pos_fh.pos_fh_id");
        pstmtUpdate.executeUpdate();
        pfb.setPosFhId(tempId);
      }
      else
      {
        ExceptionLogger.logError("Failed to get counter for POS_FH_ID", null, "fetchPosFhId", pfb, null, null);
      }
    }
    catch (Exception e)
    {
      ExceptionLogger.logException(pfb, "PosLiteInvoicing", "fetchPosFhId", null, e);
    }
    finally
    {
      TryCleanup.tryCleanup(pfb, pstmt, rs);
      TryCleanup.tryCleanup(pfb, pstmtUpdate, null);
    }
  }

  public static void writePosFhRecord(PosFileBean pfb)
  {

    StringBuilder sb = new StringBuilder();
    sb.append("INSERT INTO sam_pos_fh");
    sb.append("  (pos_fh_id,");
    sb.append("   logged_dt,");
    sb.append("   logged_uid,");
    sb.append("   file_extract_end_dt,");
    sb.append("   file_extract_start_dt,");
    sb.append("   file_name,");
    sb.append("   file_run_dt,");
    sb.append("   rtlr_ftp_id)");
    sb.append("VALUES   (?,");
    sb.append("   SYSDATE,");
    sb.append("   'POSLITE',");
    sb.append("   TO_CHAR(SYSDATE,'YYMMDD'),");
    sb.append("   TO_CHAR((SELECT last_invoice");
    sb.append("            FROM   sam_rtlr_ftp");
    sb.append("            WHERE  rtlr_ftp_id = ?),'YYMMDD'),");
    sb.append("   'PosLite',");
    sb.append("   To_char(SYSDATE,'yyyymmdd'),");
    sb.append("   ?) ");
    OraclePreparedStatement pstmt = null;
    try
    {
      pstmt = (OraclePreparedStatement) pfb.getConnection().prepareStatement(sb.toString());

      pstmt.setString(1, pfb.getPosFhId());
      pstmt.setString(2, pfb.getRtlrFtpId());
      pstmt.setString(3, pfb.getRtlrFtpId());
      pstmt.executeUpdate();
    }
    catch (Exception e)
    {
      ExceptionLogger.logException(pfb, "PosLiteInvoicing", "writePosFhRecord", null, e);
    }
    finally
    {
      TryCleanup.closePreparedStatement(pfb, pstmt);
      log4jLogger.info("File Header Information written to Applix: " + new java.util.Date());
    }
  }

  private static void writeLastInvoiceDate(PosFileBean pfb)
  {
    StringBuilder sb = new StringBuilder();
    sb.append("UPDATE sam_rtlr_ftp ");
    sb.append("SET last_invoice = sysdate ");
    sb.append("WHERE rtlr_ftp_id = ? ");
    OraclePreparedStatement pstmt = null;
    try
    {
      pstmt = (OraclePreparedStatement) pfb.getConnection().prepareStatement(sb.toString());
      pstmt.setString(1, pfb.getRtlrFtpId());
      pstmt.executeUpdate();
    }
    catch (Exception e)
    {
      ExceptionLogger.logException(pfb, "PosLiteInvoicing", "writeLastInvoiceDate", null, e);
    }
    finally
    {
      TryCleanup.closePreparedStatement(pfb, pstmt);
      log4jLogger.info("File Header Information written to Applix: " + new java.util.Date());
    }
  }

  private static void sendSummaryEmail(OracleConnection conn)
  {
    StringBuilder message = new StringBuilder();
    Date dateNow = new Date();
    SimpleDateFormat dateFormat = new SimpleDateFormat("MMM dd, yyyy");
    SimpleDateFormat dateTimeFormat = new SimpleDateFormat("MMM dd, yyyy hh:mma z");
    try
    {
      SendMail sm = new SendMail();
      sm.setSendTo("gr_retcon@valspar.com");
      sm.setSentFrom("valsparpartner@valspar.com");
      sm.setSubject("Guardsman Pos Lite summary. " + dateFormat.format(dateNow));
      message.append("\n");
      message.append("Guardsman Retailer Connections summary.\n");
      message.append(dateTimeFormat.format(dateNow) + "\n\n");

      addInvoiceDetail(message, conn);

      sm.setMessage(message);
      sm.send();
    }
    catch (Exception e)
    {
      log4jLogger.error("Error in PosLiteInvoicing.sendSummaryEmail(conn): " + e);
    }

  }

  private static void addInvoiceDetail(StringBuilder message, OracleConnection conn)
  {
    //variables for determining how much blank space after each retailer or description
    int stringFillLength1;
    int stringFillLength2;

    //invoice deatail header        
    message.append("ERP#  RETAILER                                  ");
    message.append("PRC DESCRIPTION                                 ");
    message.append("SALE\tRET\tNET\n");
    message.append("-----------------------------------------------------------------------------------------------------------------\n");

    //new PosFileBean to get a connection
    PosFileBean pfb = new PosFileBean();
    pfb.setConnection(conn);

    //build the query
    StringBuilder sb = new StringBuilder();
    sb.append("SELECT   substr((hdr.erp_rtlr_no||' '||rtlr.description),1,48) AS rtlr, ");
    sb.append("         substr((dtl.pricing_code||' '||prc.description),1,48) AS code, ");
    sb.append("         sum(dtl.sales), ");
    sb.append("         sum(dtl.returns), ");
    sb.append("         (sum(dtl.sales) - sum(dtl.returns)) AS net ");
    sb.append("FROM     sam_pos_invoice_dtl dtl, ");
    sb.append("         sam_pos_invoice_hdr hdr, ");
    sb.append("         sam_pos_fh fh, ");
    sb.append("         sam_rtlr_ftp rtlr, ");
    sb.append("         sam_rtlr_pricing prc ");
    sb.append("WHERE    hdr.pos_invoice_hdr_id = dtl.pos_invoice_hdr_id ");
    sb.append("  AND    hdr.pos_fh_id = fh.pos_fh_id ");
    sb.append("  AND    fh.rtlr_ftp_id = rtlr.rtlr_ftp_id ");
    sb.append("  AND    prc.rtlr_pricing_id = dtl.pricing_code ");
    sb.append("  AND    hdr.logged_dt > SYSDATE - .25 ");
    sb.append("  AND    hdr.invoice_status = 'A' ");
    sb.append("  AND    hdr.logged_uid = 'POSLITE' ");
    sb.append("GROUP BY hdr.erp_rtlr_no, ");
    sb.append("         rtlr.description, ");
    sb.append("         dtl.pricing_code, ");
    sb.append("         prc.description ");
    sb.append("ORDER BY hdr.erp_rtlr_no ");

    OraclePreparedStatement pstmt = null;
    OracleResultSet rs = null;
    try
    {
      pstmt = (OraclePreparedStatement) pfb.getConnection().prepareStatement(sb.toString());
      rs = (OracleResultSet) pstmt.executeQuery();
      while (rs.next())

      {
        // Retailer info
        message.append(rs.getString(1));

        // Create filler to keep columns straight
        stringFillLength1 = (48 - rs.getString(1).length());
        if (stringFillLength1 < 0)
        {
          stringFillLength1 = 0;
        }
        char[] stringFill1 = new char[stringFillLength1];
        Arrays.fill(stringFill1, ' ');
        String filler1 = new String(stringFill1);
        message.append(filler1);

        // tab and pricing code & description
        message.append(rs.getString(2));

        // Create filler to keep columns straight
        stringFillLength2 = (48 - rs.getString(2).length());
        if (stringFillLength2 < 0)
        {
          stringFillLength2 = 0;
        }
        char[] stringFill2 = new char[stringFillLength2];
        Arrays.fill(stringFill2, ' ');
        String filler2 = new String(stringFill2);
        message.append(filler2);

        // number of sales and tab
        message.append(rs.getString(3));
        message.append("\t");

        // number of returns and tab
        message.append(rs.getString(4));
        message.append("\t");

        // net sales and next line
        message.append(rs.getString(5));
        message.append("\n");
      }
    }
    catch (Exception e)
    {
      log4jLogger.error("Error in PosLiteInvoicing.addInvoiceDetail(message, conn): " + e);
    }
    finally
    {
      TryCleanup.tryCleanup(pfb, pstmt, rs);
    }
  }

  public static void reportErrors(PosFileBean pfb)
  {
    int errorCount = 0;
    if (pfb.getAdminMessages().size() > 0)
    {
      StringBuilder errorMessage = new StringBuilder();
      try
      {
        SendMail sm = new SendMail();
        sm.setSendTo("helpdeskgrandrapids@valspar.com");
        sm.setSentFrom("valsparpartner@valspar.com");
        sm.setSubject("Guardsman PosLite Exception Report");
        errorMessage.append(pfb.getRetailerName() + "\n\n");

        Iterator m = pfb.getAdminMessages().iterator();
        while (m.hasNext())
        {
          errorCount = errorCount + 1;
          AdminMsgBean amb = (AdminMsgBean) m.next();
          errorMessage.append("Admin Error: " + errorCount + ") " + amb.getProgramLocation() + " Msg: " + amb.getMessage() + " Item: " + amb.getItem() + "\n\n");
        }
        sm.setMessage(errorMessage);
        sm.send();
      }
      catch (Exception e)
      {
        log4jLogger.error("Send Mail Error " + e.toString());
      }
    }
  }
}
