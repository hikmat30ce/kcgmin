package com.valspar.interfaces.guardsman.pos.utility;

import com.valspar.interfaces.guardsman.pos.beans.*;
import java.io.*;
import java.util.*;
import oracle.jdbc.*;
import org.apache.log4j.Logger;

public class Invoice
{
  static Logger log4jLogger = Logger.getLogger(Invoice.class.getName());

  public Invoice(PosFileBean pfb)
  {
    log4jLogger.info("Creating file summary for " + pfb.getRetailerName() + "...\n");
    Iterator i = pfb.getUniqueRetailerMap().values().iterator(); //Send one email for each ERP
    while (i.hasNext())
    {
      String erp = (String) i.next();
      if (pfb.getFileFormat().equals("Pipe 36") || pfb.getFileFormat().equals("Hudson Bay"))
      {
        runPipe36HeaderInvoiceDetailToDB(pfb, erp);
        runPipe36TransInvoiceDetailToDB(pfb, erp);
        runPipe36ItemInvoiceDetailToDB(pfb, erp);

        runPipe36HeaderSalesUpdate(pfb, erp);
        runPipe36HeaderReturnsUpdate(pfb, erp);
        runPipe36ItemSalesUpdate(pfb, erp);
        runPipe36ItemReturnsUpdate(pfb, erp);
        runPipe36TransSalesUpdate(pfb, erp);
        runPipe36TransReturnsUpdate(pfb, erp);
      }
      else if (pfb.getFileFormat().equals("Tab 18"))
      {
        runTab18InvoiceDetailToDB(pfb, erp);
        runTab18InvoiceUpdate(pfb, erp);
      }
      else if (pfb.getFileFormat().equals("POSLITE"))
      {
        runCabinetPlanTotalInvoiceDetailToDB(pfb, erp);
        presortConSaCounts(pfb, erp);
        runReturnsOnDoubleTransCabinetsDetailToDB(pfb, erp);
        cleanDoubleTransReturns(pfb, erp);
        runPosLiteInvoiceDetailToDB(pfb, erp);
        runPosLiteInvoiceUpdate(pfb, erp);
      }

      runErrorDetailToDB(pfb, erp);
      if (pfb.isInvoiceByStore())
      {
        runInvoiceByStore(pfb, erp);
      }
      else
      {
        runInvoice(pfb, erp);
      }
    }
    log4jLogger.info("File summary complete.\n");
  }

  public static void finishInvoice(PosFileBean pfb, String erp, String billTo)
  {
    if (pfb.isPrintPlans())
    {
      runPlanPrintCount(pfb, erp);
    }
    if (pfb.isManualReturns())
    {
      runManualReturn(pfb, erp);
      runManualReturnUpdate(pfb, erp);
    }
    if (!pfb.getFileFormat().equals("POSLITE"))
    {
      if (pfb.isSendInvoiceDetailRpt())
      {
        createInvoiceDetailRpt(pfb, erp, billTo);
      }
    }
    if (!pfb.getFileFormat().equals("POSLITE"))
    {
      sendInvoiceEmail(pfb, erp, billTo);
    }
    writeInvoiceRecords(pfb, erp, billTo);
    pfb.getInvoiceItems().clear();
    log4jLogger.info("Summary complete for " + erp + "-" + billTo + "\n");
  }

  public static String setHdrInvId(PosFileBean pfb)
  {
    String invHdrId = null;
    StringBuilder sb = new StringBuilder();
    sb.append("select sam_pos_invoice_hdr_seq.nextval from dual ");
    OraclePreparedStatement pstmt = null;
    OracleResultSet rs = null;
    try
    {
      pstmt = (OraclePreparedStatement) pfb.getConnection().prepareStatement(sb.toString());
      rs = (OracleResultSet) pstmt.executeQuery();
      if (rs.next())
      {
        invHdrId = rs.getString(1);
      }

    }
    catch (Exception e)
    {
      log4jLogger.error("Error in Invoice.setHdrInvId(): " + e);
    }
    return invHdrId;
  }

  public static void writeHdrRecord(PosFileBean pfb, String hdrId, String retailerERP, String dseq, String store, String billTo)
  {
    String invoiceDesc = new java.util.Date().toString();
    String invoiceStatus = new String();

    if (pfb.getFileExtractStartDt() != null)
    {
      invoiceDesc = pfb.getFileExtractStartDt() + " - " + pfb.getFileExtractEndDt();
    }
    if ("Y".equalsIgnoreCase(pfb.getAutoInvoiceToJba()))
    {
      if (pfb.getFileFormat().equals("POSLITE"))
      {
        invoiceStatus = "A";
      }
      else
      {
        invoiceStatus = "P";
      }
    }

    StringBuilder sb = new StringBuilder();

    sb.append("insert into sam_pos_invoice_hdr ");
    sb.append("(pos_invoice_hdr_id,pos_fh_id,logged_dt,invoice_status,invoice_status_dt,");
    sb.append("invoice_desc,erp_rtlr_no,erp_rtlr_dseq,disc,auto_invoice_to_jba,rtlr_store_no,erp_bill_to,logged_uid) ");
    sb.append("values ");
    sb.append("(?,?,sysdate,?,sysdate,?,?,?,?,?,?,?,");
    if (pfb.getFileFormat().equals("POSLITE"))
    {
      sb.append("'POSLITE')");
    }
    else
    {
      sb.append("'Pos')");
    }
    OraclePreparedStatement pstmt = null;

    try
    {
      pstmt = (OraclePreparedStatement) pfb.getConnection().prepareStatement(sb.toString());
      pstmt.setString(1, hdrId);
      pstmt.setString(2, pfb.getPosFhId());
      pstmt.setString(3, invoiceStatus);
      pstmt.setString(4, invoiceDesc);
      pstmt.setString(5, retailerERP);
      pstmt.setString(6, dseq);
      pstmt.setString(7, pfb.getTallyDisc());
      pstmt.setString(8, pfb.getAutoInvoiceToJba());
      pstmt.setString(9, store);
      pstmt.setString(10, billTo);
      pstmt.executeUpdate();
    }
    catch (Exception e)
    {
      log4jLogger.error("Error in Invoice.writeHdrRecord(): " + e);
    }
  }

  public static void writeInvoiceRecords(PosFileBean pfb, String retailerERP, String billTo)
  {
    String tempDseq = new String();
    String tempStore = new String();
    String posInvHdrId = new String();

    StringBuilder sb = new StringBuilder();

    sb.append("insert into sam_pos_invoice_dtl ");
    sb.append("(pos_invoice_dtl_id,pos_invoice_hdr_id,logged_dt, ");
    sb.append("item_sku,pricing_code,sales,returns,rtlr_store_no,logged_uid) ");
    sb.append("values ");
    sb.append("(sam_pos_invoice_dtl_seq.nextval,?,sysdate,?,?,?,?,?,");
    if (pfb.getFileFormat().equals("POSLITE"))
    {
      sb.append("'POSLITE')");
    }
    else
    {
      sb.append("'Pos')");
    }

    Iterator i = pfb.getInvoiceItems().iterator();

    while (i.hasNext())
    {
      InvoiceLineBean ilb = (InvoiceLineBean) i.next();
      if (!ilb.getErpRtlrDseq().equalsIgnoreCase(tempDseq))
      {
        tempDseq = ilb.getErpRtlrDseq();
        posInvHdrId = setHdrInvId(pfb);
        tempStore = ilb.getStoreNo();
        writeHdrRecord(pfb, posInvHdrId, retailerERP, tempDseq, tempStore, billTo);
      }

      try
      {
        OraclePreparedStatement pstmt = null;
        pstmt = (OraclePreparedStatement) pfb.getConnection().prepareStatement(sb.toString());
        pstmt.setString(1, posInvHdrId);
        pstmt.setString(2, ilb.getItemSku());
        pstmt.setString(3, ilb.getPricingMethod());
        pstmt.setString(4, ilb.getSales());
        pstmt.setString(5, ilb.getReturns());
        pstmt.setString(6, ilb.getStoreNo());
        pstmt.executeUpdate();
      }
      catch (Exception e)
      {
        log4jLogger.error("Error in Invoice.writeDtlRecords(): " + e);
      }
    }
  }

  public static void sendInvoiceEmail(PosFileBean pfb, String retailerERP, String billTo)
  {
    String valspar_email = pfb.getInvoiceEmailAddr();
    StringBuilder message = new StringBuilder();
    String erp_email = (String) pfb.getErpInvoiceEmailMap().get(retailerERP);
    try
    {
      //If there is a specific email address for this ERP than use that instead of the email address from the ftp profile
      if (erp_email != null)
      {
        valspar_email = erp_email;
      }
      SendMail sm = new SendMail();
      sm.setSendTo(valspar_email);
      sm.setSentFrom("valsparpartner@valspar.com");
      sm.setSubject("Guardsman Point of Sale file summary for " + pfb.getRetailerName() + "(" + retailerERP + "-" + billTo + ")");
      message.append("File Summary for " + pfb.getRetailerName() + " (" + retailerERP + "-" + billTo + ") \n\n");
      if (!pfb.getFileFormat().equals("POSLITE"))
      {
        message.append("File Processed: " + pfb.getFileName().substring(pfb.getFileName().lastIndexOf("/") + 1) + "\n\n");
      }
      message.append("Date Processed: " + new java.util.Date() + "\n\n");

      if (pfb.isFileHeader())
      {
        message.append("Extract Date Range: " + pfb.getFileExtractStartDt() + " - " + pfb.getFileExtractEndDt() + "\n\n");
      }

      message.append("##############################################################\n\n");
      if (pfb.isInvoiceByStore())
      {
        addInvoiceByStoreLinesToMsg(pfb, message);
      }
      else
      {
        addInvoiceLinesToMsg(pfb, message);
      }

      if (pfb.isManualReturns())
      {
        addManualReturnsToMsg(pfb, message);
      }

/*
      if (pfb.isPrintPlans())
      {
        addPlanPrintingToMsg(pfb, message);
      }
*/
      if (pfb.isSendInvoiceDetailRpt())
      {
        File file = new File(pfb.getInvoicePath() + "/InvoiceDetail" + retailerERP + "_" + billTo + "_" + pfb.getPosFhId() + ".csv");
        if (file.exists())
        {
          sm.setAttachmentName(pfb.getInvoicePath() + "/InvoiceDetail" + retailerERP + "_" + billTo + "_" + pfb.getPosFhId() + ".csv");
        }
      }
      sm.setMessage(message);
      sm.send();
    }
    catch (Exception e)
    {
      log4jLogger.error("Error in Invoice.sendInvoiceEmail(): " + e);
    }
  }

  public static void addInvoiceLinesToMsg(PosFileBean pfb, StringBuilder message)
  {
    message.append("SALES\tRETURNS\tTOTAL\tDESCRIPTION\n\n");
    Iterator i = pfb.getInvoiceItems().iterator();
    if (!i.hasNext())
    {
      message.append("FILE DATA NOT FOUND.");
    }
    while (i.hasNext())
    {
      InvoiceLineBean ilb = (InvoiceLineBean) i.next();
      message.append(ilb.getSales() + "\t" + ilb.getReturns() + "\t\t" + ilb.getTotal());
      message.append("\t(" + ilb.getPricingMethod() + ") " + ilb.getPricingDescription() + "\n");
    }
  }

  public static void addInvoiceByStoreLinesToMsg(PosFileBean pfb, StringBuilder message)
  {
    message.append("STORE               \tSALES\tRETURNS\tTOTAL\tDESCRIPTION\n\n");

    Collections.sort(pfb.getInvoiceItems());
    Iterator i = pfb.getInvoiceItems().iterator();
    if (!i.hasNext())
    {
      message.append("FILE DATA NOT FOUND.");
    }
    while (i.hasNext())
    {

      InvoiceLineBean ilb = (InvoiceLineBean) i.next();

      //format store to length of 20
      String storeCity = ilb.getStoreCity();
      String storeNo = ilb.getStoreNo();
      String storeDesc = storeNo + " - " + storeCity;
      int storeDescLen = storeDesc.length();
      for (int s = 0; s < 20 - storeDescLen; s++)
      {
        storeDesc += " ";
      }
      storeDesc = storeDesc.substring(0, 20);

      message.append(storeDesc + "\t");
      message.append(ilb.getSales() + "\t" + ilb.getReturns() + "\t\t" + ilb.getTotal());
      message.append("\t(" + ilb.getPricingMethod() + ") " + ilb.getPricingDescription() + "\n");
    }
  }

  public static void addManualReturnsToMsg(PosFileBean pfb, StringBuilder message)
  {
    String tempRtnCnt = null;

    message.append("\n\nMAN RET CNT\t\tDESCRIPTION\n\n");
    Iterator i = pfb.getManualRtnItems().iterator();
    if (!i.hasNext())
    {
      message.append("NO MANUAL RETURN ITEMS.");
    }

    while (i.hasNext())
    {

      ManualReturnBean mrb = (ManualReturnBean) i.next();
      if ("Sale".equalsIgnoreCase(mrb.getType()))
      {
        tempRtnCnt = mrb.getTransRtnCnt();
      }
      else
      {
        tempRtnCnt = mrb.getItemRtnCnt();
      }

      message.append(tempRtnCnt + "\t\t\t(" + mrb.getPricingMethod() + ") " + mrb.getPricingDescription() + "\n");
    }
  }

  /*
  public static void addPlanPrintingToMsg(PosFileBean pfb, StringBuilder message)
  {
    String tempPlanCnt = null;

    //message.append("\n\n#Plans to be printed: " + pfb.getPlanPrintCount() + "\n\n");

    message.append("\n\nTOTAL PLANS\t\tDESCRIPTION\n\n");
    Iterator i = pfb.getPlanPrintingItems().iterator();
    if (!i.hasNext())
    {
      message.append("NO PLANS TO PRINT.");
    }

    while (i.hasNext())
    {

      PlanPrintingBean ppb = (PlanPrintingBean) i.next();

      tempPlanCnt = ppb.getPlanCnt();

      message.append(tempPlanCnt + "\t\t\t(" + ppb.getSaType() + ") " + ppb.getSaTypeDescription() + "\n");
    }
  }
*/

  public static void runTab18InvoiceUpdate(PosFileBean pfb, String retailerERP)
  {
    StringBuilder sb = new StringBuilder();
    sb.append("update sam_con_sa ");
    sb.append("set sam_con_sa.sls_qty_invoiced = sam_con_sa.sls_qty_to_inv, ");
    sb.append("sam_con_sa.sls_qty_to_inv = null, ");
    sb.append("sam_con_sa.rtrn_qty_invoiced = sam_con_sa.rtrn_qty_to_inv, ");
    sb.append("sam_con_sa.rtrn_qty_to_inv = null, ");
    sb.append("sam_con_sa.invoice_dt = sysdate ");
    sb.append("where (sls_qty_to_inv is not null  ");
    sb.append("or rtrn_qty_to_inv is not null) ");
    sb.append("and logged_uid = 'POS' ");
    sb.append("and rtlr_addr_id in  ");
    sb.append("(select rtlr_addr_id from sam_rtlr_addr where erp_rtlr_no = ?) ");
    OraclePreparedStatement pstmt = null;
    try
    {
      pstmt = (OraclePreparedStatement) pfb.getConnection().prepareStatement(sb.toString());
      pstmt.setString(1, retailerERP);
      pstmt.executeUpdate();
    }
    catch (Exception e)
    {
      ExceptionLogger.logException(pfb, "Invoice", "runTab18InvoiceUpdate", null, e);
    }
    finally
    {
      TryCleanup.tryCleanup(pfb, pstmt, null);
    }
  }

  public static void cleanDoubleTransReturns(PosFileBean pfb, String retailerERP)
  {
    StringBuilder sb = new StringBuilder();

    sb.append("    UPDATE sam_con_sa ");
    sb.append("    SET    rtrn_qty_to_inv = NULL ");
    sb.append("    WHERE  sls_qty_to_inv IS NOT NULL ");
    sb.append("           AND rtrn_qty_to_inv IS NOT NULL ");
    sb.append("           AND logged_uid = 'POSLITE' ");
    sb.append("           AND pricing_method = '798' ");
    sb.append("           AND rtlr_addr_id IN (SELECT rtlr_addr_id ");
    sb.append("                                FROM   sam_rtlr_addr ");
    sb.append("                                WHERE  erp_rtlr_no = ?) ");

    OraclePreparedStatement pstmt = null;
    try
    {
      pstmt = (OraclePreparedStatement) pfb.getConnection().prepareStatement(sb.toString());
      pstmt.setString(1, retailerERP);
      pstmt.executeUpdate();
    }
    catch (Exception e)
    {
      ExceptionLogger.logException(pfb, "Invoice", "cleanDoubleTransReturns", null, e);
    }
    finally
    {
      TryCleanup.tryCleanup(pfb, pstmt, null);
    }
  }

  public static void runPosLiteInvoiceUpdate(PosFileBean pfb, String retailerERP)
  {
    StringBuilder sb = new StringBuilder();
    sb.append("UPDATE sam_con_sa ");
    sb.append("SET    sam_con_sa.sls_qty_invoiced = sam_con_sa.sls_qty_to_inv, ");
    sb.append("       sam_con_sa.sls_qty_to_inv = NULL, ");
    sb.append("       sam_con_sa.rtrn_qty_invoiced = ( CASE ");
    sb.append("                                          WHEN ( sam_con_sa.pricing_method = '798' ) ");
    sb.append("                                          THEN sam_con_sa.rtrn_qty_invoiced ");
    sb.append("                                          ELSE sam_con_sa.rtrn_qty_to_inv ");
    sb.append("                                        END ), ");
    sb.append("       sam_con_sa.rtrn_qty_to_inv = NULL, ");
    sb.append("       sam_con_sa.invoice_dt = SYSDATE ");
    sb.append("WHERE  ( sls_qty_to_inv IS NOT NULL ");
    sb.append("          OR rtrn_qty_to_inv IS NOT NULL ) ");
    sb.append("       AND logged_uid = 'POSLITE' ");
    sb.append("       AND rtlr_addr_id IN (SELECT rtlr_addr_id ");
    sb.append("                            FROM   sam_rtlr_addr ");
    sb.append("                            WHERE  erp_rtlr_no = ?) ");
    OraclePreparedStatement pstmt = null;
    try
    {
      pstmt = (OraclePreparedStatement) pfb.getConnection().prepareStatement(sb.toString());
      pstmt.setString(1, retailerERP);
      pstmt.executeUpdate();
    }
    catch (Exception e)
    {
      ExceptionLogger.logException(pfb, "Invoice", "runPosLiteInvoiceUpdate", null, e);
    }
    finally
    {
      TryCleanup.tryCleanup(pfb, pstmt, null);
    }
  }

  public static void runPipe36ItemSalesUpdate(PosFileBean pfb, String retailerERP)
  {
    //only update those where the pricing code is a 'item'
    StringBuilder sb = new StringBuilder();
    sb.append("update sam_con_sls_rcpt_item ");
    sb.append("set invoice_dt = sysdate ");
    sb.append("where invoice_dt is null ");
    sb.append("and pricing_code is not null ");
    sb.append("and sls_rcpt_id in  ");
    sb.append("(select sr.sls_rcpt_id ");
    sb.append("from sam_con_sls_rcpt sr, sam_rtlr_addr rtlr ");
    sb.append("where sr.rtlr_addr_id = rtlr.rtlr_addr_id ");
    sb.append("and rtlr.erp_rtlr_no = ?) ");
    sb.append("and pricing_code in ");
    sb.append("(select sam_rtlr_pricing.rtlr_pricing_id ");
    sb.append("from sam_rtlr_pricing ");
    sb.append("where sam_rtlr_pricing.type = 'Item') ");
    OraclePreparedStatement pstmt = null;
    try
    {
      pstmt = (OraclePreparedStatement) pfb.getConnection().prepareStatement(sb.toString());
      pstmt.setString(1, retailerERP);
      pstmt.executeUpdate();
    }
    catch (Exception e)
    {
      ExceptionLogger.logException(pfb, "Invoice", "runPipe36ItemSalesUpdate", null, e);
    }
    finally
    {
      TryCleanup.tryCleanup(pfb, pstmt, null);
    }
  }

  public static void runPipe36ItemReturnsUpdate(PosFileBean pfb, String retailerERP)
  {
    //only update those where the pricing code is a 'item'
    StringBuilder sb = new StringBuilder();
    sb.append("update sam_con_sls_rcpt_item ");
    sb.append("set invoice_rtrn_dt = sysdate ");
    sb.append("where status = 'R' ");
    sb.append("and invoice_rtrn_dt is null ");
    sb.append("and pricing_code is not null ");
    sb.append("and sls_rcpt_id in  ");
    sb.append("(select sr.sls_rcpt_id ");
    sb.append("from sam_con_sls_rcpt sr, sam_rtlr_addr rtlr ");
    sb.append("where sr.rtlr_addr_id = rtlr.rtlr_addr_id ");
    sb.append("and rtlr.erp_rtlr_no = ?) ");
    sb.append("and pricing_code in ");
    sb.append("(select sam_rtlr_pricing.rtlr_pricing_id ");
    sb.append("from sam_rtlr_pricing ");
    sb.append("where sam_rtlr_pricing.type = 'Item') ");
    OraclePreparedStatement pstmt = null;
    try
    {
      pstmt = (OraclePreparedStatement) pfb.getConnection().prepareStatement(sb.toString());
      pstmt.setString(1, retailerERP);
      pstmt.executeUpdate();
    }
    catch (Exception e)
    {
      ExceptionLogger.logException(pfb, "Invoice", "runPipe36ItemReturnsUpdate", null, e);
    }
    finally
    {
      TryCleanup.tryCleanup(pfb, pstmt, null);
    }
  }

  public static void runInvoice(PosFileBean pfb, String retailerERP)
  {
    String[] billToArray = billToArray(pfb, retailerERP);
    int billToCount = billToCount(pfb, retailerERP);
    
     for (int i = 0; i <= (billToCount - 1); i++)
    {
      StringBuilder sb = new StringBuilder();
      sb.append("SELECT detail.pricing_id ");
      sb.append("     , detail.code ");
      sb.append("     , sam_rtlr_pricing.item_sku ");
      sb.append("     , SUM(sales) ");
      sb.append("     , SUM(returns) ");
      sb.append("     , SUM(sales) - SUM(returns) ");
      sb.append("     , (SELECT MAX(sam_rtlr_addr.erp_rtlr_dseq) ");
      sb.append("          FROM sam_rtlr_addr ");
      sb.append("         WHERE erp_rtlr_no = :erpRtlrNo ");
      sb.append("           AND erp_bill_to = :erpBillTo) dseq ");
      sb.append("  FROM sam_pos_invoice_stored_detail detail ");
      sb.append("     , sam_rtlr_pricing ");
      sb.append("     , sam_rtlr_addr addr ");
      sb.append(" WHERE detail.pricing_id = sam_rtlr_pricing.rtlr_pricing_id ");
      sb.append("   AND detail.pricing_type <> 'Error' ");
      sb.append("   AND addr.rtlr_addr_id = detail.rtlr_addr_id ");
      sb.append("   AND detail.pos_fh_id = :posFhId ");
      sb.append("   AND detail.erp_rtlr_no = :erpRtlrNo ");
      sb.append("   AND detail.erp_bill_to = :erpBillTo ");
      sb.append(" GROUP BY detail.pricing_id ");
      sb.append("     , detail.code ");
      sb.append("     , sam_rtlr_pricing.item_sku ");
      sb.append("     , detail.erp_bill_to ");
      OraclePreparedStatement pstmt = null;
      OracleResultSet rs = null;
      try
      {
        pstmt = (OraclePreparedStatement) pfb.getConnection().prepareStatement(sb.toString());
        pstmt.setStringAtName("posFhId", pfb.getPosFhId());
        pstmt.setStringAtName("erpRtlrNo", retailerERP);
        pstmt.setStringAtName("erpBillTo", billToArray[i]);
        rs = (OracleResultSet) pstmt.executeQuery();
        while (rs.next())
        {
          InvoiceLineBean ilb = new InvoiceLineBean();
          ilb.setPricingMethod(rs.getString(1));
          ilb.setPricingDescription(rs.getString(2));
          ilb.setItemSku(rs.getString(3));
          ilb.setSales(rs.getString(4));
          ilb.setReturns(rs.getString(5));
          ilb.setTotal(rs.getString(6));
          ilb.setErpRtlrDseq(rs.getString(7));
          pfb.getInvoiceItems().add(ilb);
        }
      }
      catch (Exception e)
      {
        ExceptionLogger.logException(pfb, "Invoice", "runInvoice", null, e);
      }
      finally
      {
        TryCleanup.tryCleanup(pfb, pstmt, rs);
      }
      finishInvoice(pfb, retailerERP, billToArray[i]);
    }
  }

  public static void runInvoiceByStore(PosFileBean pfb, String retailerERP)
  {
    String[] billToArray = billToArray(pfb, retailerERP);
    int billToCount = billToCount(pfb, retailerERP);

    for (int i = 0; i <= (billToCount - 1); i++)
    {
      StringBuilder sb = new StringBuilder();
      sb.append(" SELECT detail.dseq ");
      sb.append("      , addr.rtlr_store_no ");
      sb.append("      , addr.NAME ");
      sb.append("      , addr.city ");
      sb.append("      , addr.address1 ");
      sb.append("      , detail.pricing_id ");
      sb.append("      , detail.code ");
      sb.append("      , pricing.item_sku ");
      sb.append("      , SUM(sales) ");
      sb.append("      , SUM(returns) ");
      sb.append("      , SUM(sales) - SUM(returns) total ");
      sb.append("   FROM sam_pos_invoice_stored_detail detail ");
      sb.append("      , sam_rtlr_pricing pricing ");
      sb.append("      , sam_rtlr_addr addr ");
      sb.append("  WHERE detail.pricing_id = pricing.rtlr_pricing_id ");
      sb.append("    AND detail.rtlr_addr_id = addr.rtlr_addr_id ");
      sb.append("    AND detail.dseq = addr.erp_rtlr_dseq ");
      sb.append("    AND detail.pricing_type <> 'Error' ");
      sb.append("    AND detail.pos_fh_id = :posFhId ");
      sb.append("    AND detail.erp_rtlr_no = :erpRtlrNo ");
      sb.append("    AND detail.erp_bill_to = :erpBillTo ");
      sb.append("  GROUP BY detail.dseq ");
      sb.append("      , addr.rtlr_store_no ");
      sb.append("      , addr.NAME ");
      sb.append("      , addr.city ");
      sb.append("      , addr.address1 ");
      sb.append("      , detail.pricing_id ");
      sb.append("      , detail.code ");
      sb.append("      , pricing.item_sku ");
      OraclePreparedStatement pstmt = null;
      OracleResultSet rs = null;
      try
      {
        pstmt = (OraclePreparedStatement) pfb.getConnection().prepareStatement(sb.toString());
        pstmt.setStringAtName("posFhId", pfb.getPosFhId());
        pstmt.setStringAtName("erpRtlrNo", retailerERP);
        pstmt.setStringAtName("erpBillTo", billToArray[i]);
        rs = (OracleResultSet) pstmt.executeQuery();
        while (rs.next())
        {
          InvoiceLineBean ilb = new InvoiceLineBean();
          ilb.setErpRtlrDseq(rs.getString(1));
          ilb.setStoreNo(rs.getString(2));
          ilb.setStoreName(rs.getString(3));
          ilb.setStoreCity(rs.getString(4));
          ilb.setStoreAddr(rs.getString(5));
          ilb.setPricingMethod(rs.getString(6));
          ilb.setPricingDescription(rs.getString(7));
          ilb.setItemSku(rs.getString(8));
          ilb.setSales(rs.getString(9));
          ilb.setReturns(rs.getString(10));
          ilb.setTotal(rs.getString(11));
          pfb.getInvoiceItems().add(ilb);
        }
      }
      catch (Exception e)
      {
        ExceptionLogger.logException(pfb, "Invoice", "runInvoiceByStore", null, e);
      }
      finally
      {
        TryCleanup.tryCleanup(pfb, pstmt, rs);
      }
      finishInvoice(pfb, retailerERP, billToArray[i]);
    }
  }

  public static void runPipe36HeaderSalesUpdate(PosFileBean pfb, String retailerERP)
  {
    StringBuilder sb = new StringBuilder();
    sb.append("update sam_con_sls_rcpt ");
    sb.append("set invoice_dt = sysdate ");
    sb.append("where invoice_dt is null ");
    sb.append("and pricing_method is not null ");
    sb.append("and logged_uid = 'POS' ");
    sb.append("and rtlr_addr_id in ");
    sb.append("(select sam_rtlr_addr.rtlr_addr_id ");
    sb.append("from sam_rtlr_addr ");
    sb.append("where sam_rtlr_addr.erp_rtlr_no = ?) ");
    OraclePreparedStatement pstmt = null;
    try
    {
      pstmt = (OraclePreparedStatement) pfb.getConnection().prepareStatement(sb.toString());
      pstmt.setString(1, retailerERP);
      pstmt.executeUpdate();
    }
    catch (Exception e)
    {
      ExceptionLogger.logException(pfb, "Invoice", "runPipe36HeaderSalesUpdate", null, e);
    }
    finally
    {
      TryCleanup.tryCleanup(pfb, pstmt, null);
    }
  }

  public static void runPipe36HeaderReturnsUpdate(PosFileBean pfb, String retailerERP)
  {
    StringBuilder sb = new StringBuilder();
    sb.append("update sam_con_sls_rcpt ");
    sb.append("set invoice_rtrn_dt = sysdate ");
    sb.append("where invoice_rtrn_dt is null ");
    sb.append("and logged_uid = 'POS' ");
    sb.append("and pricing_method is not null ");
    sb.append("and sls_rcpt_id in (select sr.sls_rcpt_id ");
    sb.append("FROM sam_con_sls_rcpt sr, sam_rtlr_addr,  ");
    sb.append("sam_con_sa ");
    sb.append("WHERE sr.rtlr_addr_id = sam_rtlr_addr.rtlr_addr_id ");
    sb.append("AND sr.sls_rcpt_id = sam_con_sa.sls_rcpt_id ");
    sb.append("AND sam_con_sa.sa_status <> 'Active' ");
    sb.append("AND sr.invoice_rtrn_dt IS NULL ");
    sb.append("AND sr.pricing_method IS NOT NULL ");
    sb.append("AND sr.logged_uid = 'POS' ");
    sb.append("AND sam_rtlr_addr.erp_rtlr_no = ?) ");
    OraclePreparedStatement pstmt = null;
    try
    {
      pstmt = (OraclePreparedStatement) pfb.getConnection().prepareStatement(sb.toString());
      pstmt.setString(1, retailerERP);
      pstmt.executeUpdate();
    }
    catch (Exception e)
    {
      ExceptionLogger.logException(pfb, "Invoice", "runPipe36HeaderSalesUpdate", null, e);
    }
    finally
    {
      TryCleanup.tryCleanup(pfb, pstmt, null);
    }
  }

  public static void runPipe36HeaderInvoiceDetailToDB(PosFileBean pfb, String retailerERP)
  {
    StringBuilder sb = new StringBuilder();

    sb.append("insert into sam_pos_invoice_stored_detail(erp_rtlr_no, rtlr_addr_id, dseq, erp_bill_to, trans_id, ");
    sb.append("first_name,last_name,delpur_dt,con_sa_id,pricing_id, ");
    sb.append("code, pricing_type, create_dt, pos_fh_id, sales,returns) ");
    sb.append("SELECT rtlr.erp_rtlr_no, rtlr.rtlr_addr_id, rtlr.erp_rtlr_dseq,  nvl(rtlr.erp_bill_to, rtlr.erp_rtlr_dseq), sr.trans_id, con.first_name,  ");
    sb.append("con.last_name, sam_con_sa.delpur_dt, sam_con_sa.con_sa_id, ");
    sb.append("pricing.rtlr_pricing_id, pricing.description code, pricing.type, sysdate, :posFhId,  ");
    sb.append("1 sales, 0 RETURN   ");
    sb.append("FROM sam_con_sls_rcpt sr,  ");
    sb.append("sam_rtlr_addr rtlr,  ");
    sb.append("sam_rtlr_pricing pricing, ");
    sb.append("sam_con con, ");
    sb.append("sam_con_sa ");
    sb.append("WHERE sr.rtlr_addr_id = rtlr.rtlr_addr_id  ");
    sb.append("AND sr.pricing_method = pricing.rtlr_pricing_id  ");
    sb.append("and sr.con_id = con.con_id ");
    sb.append("and sam_con_sa.SLS_RCPT_ID = sr.SLS_RCPT_ID ");
    sb.append("AND pricing.type = 'Sale'  ");
    sb.append("AND sr.invoice_dt IS NULL  ");
    sb.append("AND sr.pricing_method IS NOT NULL  ");
    sb.append("AND sr.logged_uid = 'POS'  ");
    sb.append("AND rtlr.erp_rtlr_no = :erpRtlrNo  ");
    sb.append("UNION ALL ");
    sb.append("SELECT rtlr.erp_rtlr_no, rtlr.rtlr_addr_id, rtlr.erp_rtlr_dseq, nvl(rtlr.erp_bill_to, rtlr.erp_rtlr_dseq), sr.trans_id, con.first_name,  ");
    sb.append("con.last_name, sam_con_sa.delpur_dt, sam_con_sa.con_sa_id,  ");
    sb.append("pricing.rtlr_pricing_id, pricing.description code, pricing.type, sysdate, :posFhId,  ");
    sb.append("0 sales, 1 RETURN ");
    sb.append("FROM sam_con_sls_rcpt sr,  ");
    sb.append("sam_rtlr_addr rtlr,   ");
    sb.append("sam_rtlr_pricing pricing, ");
    sb.append("sam_con con,  ");
    sb.append("sam_con_sa  ");
    sb.append("WHERE sr.rtlr_addr_id = rtlr.rtlr_addr_id  ");
    sb.append("AND sr.pricing_method = pricing.rtlr_pricing_id  ");
    sb.append("AND sr.sls_rcpt_id = sam_con_sa.sls_rcpt_id  ");
    sb.append("and sr.con_id = con.con_id ");
    sb.append("AND sam_con_sa.sa_status = 'R'  ");
    sb.append("AND pricing.type = 'Sale'  ");
    sb.append("AND sr.invoice_rtrn_dt IS NULL  ");
    sb.append("AND sr.pricing_method IS NOT NULL  ");
    sb.append("AND sr.logged_uid = 'POS'  ");
    sb.append("AND rtlr.erp_rtlr_no = :erpRtlrNo  ");

    OraclePreparedStatement pstmt = null;
    OracleResultSet rs = null;
    try
    {
      pstmt = (OraclePreparedStatement) pfb.getConnection().prepareStatement(sb.toString());
      pstmt.setStringAtName("erpRtlrNo", retailerERP);
      pstmt.setStringAtName("posFhId", pfb.getPosFhId());

      pstmt.executeUpdate();
    }
    catch (Exception e)
    {
      ExceptionLogger.logException(pfb, "Invoice", "runPipe36HeaderInvoiceDetailToDB", null, e);
    }
    finally
    {
      TryCleanup.tryCleanup(pfb, pstmt, rs);
    }
  }

  public static void runPipe36TransInvoiceDetailToDB(PosFileBean pfb, String retailerERP)
  {
    StringBuilder sb = new StringBuilder();

    sb.append("insert into sam_pos_invoice_stored_detail(erp_rtlr_no, rtlr_addr_id, dseq, erp_bill_to, trans_id, ");
    sb.append("first_name,last_name,delpur_dt,con_sa_id,pricing_id, ");
    sb.append("code, pricing_type, create_dt, pos_fh_id, sales,returns) ");
    sb.append("SELECT rtlr.erp_rtlr_no, rtlr.rtlr_addr_id, rtlr.erp_rtlr_dseq, nvl(rtlr.erp_bill_to, rtlr.erp_rtlr_dseq), sr.trans_id, con.first_name, ");
    sb.append("con.last_name, sam_con_sa.delpur_dt, sam_con_sa.con_sa_id, ");
    sb.append("pricing.rtlr_pricing_id, pricing.description code, pricing.type, sysdate, :posFhId, ");
    sb.append("COUNT (DISTINCT (sam_con_sa.con_sa_id)) sales, 0 RETURN ");
    sb.append("FROM sam_con_sls_rcpt_item item,sam_con_sls_rcpt sr,  ");
    sb.append("sam_rtlr_addr rtlr,sam_rtlr_pricing pricing, sam_con_sa, sam_con con  ");
    sb.append("WHERE item.pricing_code is not null and item.sls_rcpt_id = sr.sls_rcpt_id  ");
    sb.append("AND sr.rtlr_addr_id = rtlr.rtlr_addr_id  ");
    sb.append("AND item.pricing_code = pricing.rtlr_pricing_id  ");
    sb.append("and item.con_sa_id = sam_con_sa.con_sa_id ");
    sb.append("and sam_con_sa.con_id = con.con_id ");
    sb.append("AND rtlr.erp_rtlr_no = :erpRtlrNo  ");
    sb.append("AND item.logged_uid = 'POS'  ");
    sb.append("AND pricing.type = 'Sale'  ");
    sb.append("group by rtlr.erp_rtlr_no, rtlr.rtlr_addr_id, rtlr.erp_rtlr_dseq, nvl(rtlr.erp_bill_to, rtlr.erp_rtlr_dseq), sr.trans_id, con.first_name,  ");
    sb.append("con.last_name, sam_con_sa.delpur_dt, sam_con_sa.con_sa_id,  ");
    sb.append("pricing.rtlr_pricing_id, pricing.description, pricing.type, sysdate, :posFhId ");
    sb.append("having max(item.invoice_dt) is null ");
    sb.append("UNION ALL ");
    sb.append("SELECT rtlr.erp_rtlr_no, rtlr.rtlr_addr_id, rtlr.erp_rtlr_dseq, nvl(rtlr.erp_bill_to, rtlr.erp_rtlr_dseq), sr.trans_id, con.first_name, ");
    sb.append("con.last_name, sam_con_sa.delpur_dt dlv_dt, sam_con_sa.con_sa_id, ");
    sb.append("pricing.rtlr_pricing_id ID, pricing.description code, pricing.TYPE, sysdate, :posFhId, ");
    sb.append("0 sales, COUNT (DISTINCT (sam_con_sa.con_sa_id)) RETURN ");
    sb.append("FROM sam_con_sls_rcpt_item item,  ");
    sb.append("sam_con_sls_rcpt sr, sam_rtlr_addr rtlr,  ");
    sb.append("sam_rtlr_pricing pricing , sam_con_sa, sam_con con ");
    sb.append("WHERE item.pricing_code is not null and item.sls_rcpt_id = sr.sls_rcpt_id  ");
    sb.append("AND sr.rtlr_addr_id = rtlr.rtlr_addr_id  ");
    sb.append("AND item.pricing_code = pricing.rtlr_pricing_id  ");
    sb.append("and sam_con_sa.con_sa_id = item.con_sa_id  ");
    sb.append("and sam_con_sa.con_id = con.con_id ");
    sb.append("and sam_con_sa.sa_status = 'R' ");
    sb.append("AND rtlr.erp_rtlr_no = :erpRtlrNo ");
    sb.append("AND item.logged_uid = 'POS'  ");
    sb.append("AND item.status = 'R'  ");
    sb.append("AND item.invoice_rtrn_dt IS NULL ");
    sb.append("AND pricing.type = 'Sale' ");
    sb.append("group by rtlr.erp_rtlr_no, rtlr.rtlr_addr_id, rtlr.erp_rtlr_dseq, nvl(rtlr.erp_bill_to, rtlr.erp_rtlr_dseq), sr.trans_id, con.first_name,  ");
    sb.append("con.last_name, sam_con_sa.delpur_dt, sam_con_sa.con_sa_id,  ");
    sb.append("pricing.rtlr_pricing_id, pricing.description, pricing.TYPE, sysdate, :posFhId ");

    OraclePreparedStatement pstmt = null;
    OracleResultSet rs = null;
    try
    {
      pstmt = (OraclePreparedStatement) pfb.getConnection().prepareStatement(sb.toString());
      pstmt.setStringAtName("erpRtlrNo", retailerERP);
      pstmt.setStringAtName("posFhId", pfb.getPosFhId());

      pstmt.executeUpdate();
    }
    catch (Exception e)
    {
      ExceptionLogger.logException(pfb, "Invoice", "runPipe36TransInvoiceDetailToDB", null, e);
    }
    finally
    {
      TryCleanup.tryCleanup(pfb, pstmt, rs);
    }
  }

  public static void runPipe36ItemInvoiceDetailToDB(PosFileBean pfb, String retailerERP)
  {
    StringBuilder sb = new StringBuilder();

    sb.append("insert into sam_pos_invoice_stored_detail(erp_rtlr_no, rtlr_addr_id, dseq, erp_bill_to, trans_id, ");
    sb.append("first_name,last_name,delpur_dt,con_sa_id,pricing_id, ");
    sb.append("code, pricing_type, create_dt, pos_fh_id, sales, returns) ");
    sb.append("SELECT rtlr.erp_rtlr_no, rtlr.rtlr_addr_id, rtlr.erp_rtlr_dseq, nvl(rtlr.erp_bill_to, rtlr.erp_rtlr_dseq), ");
    sb.append("       sr.trans_id, con.first_name, con.last_name, sam_con_sa.delpur_dt, ");
    sb.append("       sam_con_sa.con_sa_id, pricing.rtlr_pricing_id, ");
    sb.append("       pricing.description code, pricing.TYPE, SYSDATE, :posFhId, ");
    sb.append("       item.qty sales, 0 RETURN  ");
    sb.append("  FROM sam_con_sls_rcpt_item item, ");
    sb.append("       sam_con_sls_rcpt sr, ");
    sb.append("       sam_rtlr_addr rtlr, ");
    sb.append("       sam_rtlr_pricing pricing, ");
    sb.append("       sam_con_sa, ");
    sb.append("       sam_con con  ");
    sb.append(" WHERE item.pricing_code IS NOT NULL ");
    sb.append("   AND item.sls_rcpt_id = sr.sls_rcpt_id ");
    sb.append("   AND sr.rtlr_addr_id = rtlr.rtlr_addr_id ");
    sb.append("   AND item.pricing_code = pricing.rtlr_pricing_id ");
    sb.append("   AND item.con_sa_id = sam_con_sa.con_sa_id ");
    sb.append("   AND sam_con_sa.con_id = con.con_id ");
    sb.append("   AND rtlr.erp_rtlr_no = :erpRtlrNo ");
    sb.append("   AND item.invoice_dt IS NULL ");
    sb.append("   AND item.logged_uid = 'POS' ");
    sb.append("   AND pricing.TYPE = 'Item'   ");
    sb.append("UNION ALL ");
    sb.append("SELECT rtlr.erp_rtlr_no, rtlr.rtlr_addr_id, rtlr.erp_rtlr_dseq, nvl(rtlr.erp_bill_to, rtlr.erp_rtlr_dseq), ");
    sb.append("       sr.trans_id, con.first_name, con.last_name, ");
    sb.append("       sam_con_sa.delpur_dt dlv_dt, sam_con_sa.con_sa_id, ");
    sb.append("       pricing.rtlr_pricing_id ID, pricing.description code, pricing.TYPE, ");
    sb.append("       SYSDATE, :posFhId, 0 sales, item.qty returns ");
    sb.append("  FROM sam_con_sls_rcpt_item item, ");
    sb.append("       sam_con_sls_rcpt sr, ");
    sb.append("       sam_rtlr_addr rtlr, ");
    sb.append("       sam_rtlr_pricing pricing, ");
    sb.append("       sam_con_sa,  ");
    sb.append("       sam_con con ");
    sb.append(" WHERE item.pricing_code IS NOT NULL ");
    sb.append("   AND item.sls_rcpt_id = sr.sls_rcpt_id ");
    sb.append("   AND sr.rtlr_addr_id = rtlr.rtlr_addr_id ");
    sb.append("   AND item.pricing_code = pricing.rtlr_pricing_id ");
    sb.append("   AND item.con_sa_id = sam_con_sa.con_sa_id ");
    sb.append("   AND sam_con_sa.con_id = con.con_id ");
    sb.append("   AND rtlr.erp_rtlr_no = :erpRtlrNo ");
    sb.append("   AND item.logged_uid = 'POS' ");
    sb.append("   AND item.status = 'R' ");
    sb.append("   AND item.invoice_rtrn_dt IS NULL ");
    sb.append("   AND pricing.TYPE = 'Item' ");

    OraclePreparedStatement pstmt = null;
    OracleResultSet rs = null;
    try
    {
      pstmt = (OraclePreparedStatement) pfb.getConnection().prepareStatement(sb.toString());
      pstmt.setStringAtName("erpRtlrNo", retailerERP);
      pstmt.setStringAtName("posFhId", pfb.getPosFhId());
      pstmt.executeUpdate();
    }
    catch (Exception e)
    {
      ExceptionLogger.logException(pfb, "Invoice", "runPipe36ItemInvoiceDetailToDB", null, e);
    }
    finally
    {
      TryCleanup.tryCleanup(pfb, pstmt, rs);
    }
  }

  public static void runTab18InvoiceDetailToDB(PosFileBean pfb, String retailerERP)
  {
    StringBuilder sb = new StringBuilder();

    sb.append("INSERT INTO sam_pos_invoice_stored_detail(erp_rtlr_no, rtlr_addr_id, dseq, erp_bill_to, trans_id, ");
    sb.append("first_name,last_name,delpur_dt,con_sa_id,pricing_id, ");
    sb.append("code, pricing_type, create_dt, pos_fh_id, sales, returns) ");
    sb.append("SELECT   sam_rtlr_addr.erp_rtlr_no, sam_rtlr_addr.rtlr_addr_id, sam_rtlr_addr.erp_rtlr_dseq, nvl(sam_rtlr_addr.erp_bill_to, sam_rtlr_addr.erp_rtlr_dseq), ");
    sb.append("         sam_con_sa.pos_trans_id, sam_con.first_name, sam_con.last_name, sam_con_sa.delpur_dt, ");
    sb.append("         sam_con_sa.con_sa_id, sam_rtlr_pricing.rtlr_pricing_id, ");
    sb.append("         sam_rtlr_pricing.description code, sam_rtlr_pricing.TYPE, SYSDATE, :posFhId, ");
    sb.append("         SUM (NVL (sam_con_sa.sls_qty_to_inv, 0)) sales, ");
    sb.append("         SUM (NVL (sam_con_sa.rtrn_qty_to_inv, 0)) returns ");
    sb.append("    FROM sam_con_sa, sam_rtlr_addr, sam_rtlr_pricing, sam_con ");
    sb.append("   WHERE sam_con_sa.rtlr_addr_id = sam_rtlr_addr.rtlr_addr_id ");
    sb.append("     AND sam_con_sa.con_id = sam_con.con_id ");
    sb.append("     AND sam_con_sa.pricing_method = sam_rtlr_pricing.rtlr_pricing_id ");
    sb.append("     AND (sls_qty_to_inv IS NOT NULL OR rtrn_qty_to_inv IS NOT NULL) ");
    sb.append("     AND sam_con_sa.logged_uid = 'POS' ");
    sb.append("     AND sam_rtlr_addr.erp_rtlr_no IN (:erpRtlrNo) ");
    sb.append("GROUP BY sam_rtlr_addr.erp_rtlr_no, ");
    sb.append("         sam_rtlr_addr.rtlr_addr_id, ");
    sb.append("         sam_rtlr_addr.erp_rtlr_dseq, ");
    sb.append("         nvl(sam_rtlr_addr.erp_bill_to, sam_rtlr_addr.erp_rtlr_dseq), ");
    sb.append("         sam_con_sa.pos_trans_id, ");
    sb.append("         sam_con.first_name, ");
    sb.append("         sam_con.last_name, ");
    sb.append("         sam_con_sa.delpur_dt, ");
    sb.append("         sam_con_sa.con_sa_id, ");
    sb.append("         sam_rtlr_pricing.rtlr_pricing_id, ");
    sb.append("         sam_rtlr_pricing.description, ");
    sb.append("         sam_rtlr_pricing.TYPE, ");
    sb.append("         SYSDATE, :posFhId ");

    OraclePreparedStatement pstmt = null;
    OracleResultSet rs = null;
    try
    {
      pstmt = (OraclePreparedStatement) pfb.getConnection().prepareStatement(sb.toString());
      pstmt.setStringAtName("erpRtlrNo", retailerERP);
      pstmt.setStringAtName("posFhId", pfb.getPosFhId());

      pstmt.executeUpdate();
    }
    catch (Exception e)
    {
      ExceptionLogger.logException(pfb, "Invoice", "runTab18InvoiceDetailToDB", null, e);
    }
    finally
    {
      TryCleanup.tryCleanup(pfb, pstmt, rs);
    }
  }

  public static void presortConSaCounts(PosFileBean pfb, String retailerERP)
  {
    StringBuilder sb = new StringBuilder();
    sb.append("UPDATE sam_con_sa ");
    sb.append("SET    sls_qty_to_inv = ( CASE ");
    sb.append("                            WHEN ( Nvl(rtrn_qty_to_inv, 0) = '0' ) THEN ");
    sb.append("                            sls_qty_to_inv ");
    sb.append("                            ELSE ( Nvl(sls_qty_to_inv, 0) + ");
    sb.append("                                   Nvl(sls_qty_invoiced, 0) ");
    sb.append("                                   - rtrn_qty_to_inv ) ");
    sb.append("                          END ), ");
    sb.append("       sls_qty_invoiced = ( CASE ");
    sb.append("                              WHEN ( Nvl(rtrn_qty_to_inv, 0) = '0' ) THEN ");
    sb.append("                              sls_qty_invoiced ");
    sb.append("                              ELSE ( CASE ");
    sb.append("                                       WHEN ( rtrn_qty_to_inv = sls_qty_invoiced ");
    sb.append("                                            ) THEN ");
    sb.append("                                       sls_qty_invoiced ");
    sb.append("                                       ELSE NULL ");
    sb.append("                                     END ) ");
    sb.append("                            END ), ");
    sb.append("       rtrn_qty_to_inv = ( CASE ");
    sb.append("                             WHEN ( Nvl(sls_qty_to_inv, 0) = '0' ) ");
    sb.append("                             THEN sls_qty_invoiced ");
    sb.append("                             ELSE NULL ");
    sb.append("                           END ), ");
    sb.append("      rtrn_qty_invoiced = ( CASE ");
    sb.append("                              WHEN ( Nvl(rtrn_qty_to_inv, 0) = '0' ) ");
    sb.append("                              THEN rtrn_qty_invoiced ");
    sb.append("                              ELSE ( Nvl(rtrn_qty_to_inv, 0) + ");
    sb.append("                                     Nvl(rtrn_qty_invoiced, 0) ) ");
    sb.append("                             END ) ");
    sb.append("WHERE  ( sls_qty_to_inv IS NOT NULL ");
    sb.append("        OR rtrn_qty_to_inv IS NOT NULL ) ");
    sb.append("       AND logged_uid = 'POSLITE' ");
    sb.append("       AND pricing_method = '798' ");
    sb.append("       AND rtlr_addr_id IN (SELECT rtlr_addr_id ");
    sb.append("                            FROM   sam_rtlr_addr ");
    sb.append("                            WHERE  erp_rtlr_no = ?)   ");
    OraclePreparedStatement pstmt = null;
    try
    {
      pstmt = (OraclePreparedStatement) pfb.getConnection().prepareStatement(sb.toString());
      pstmt.setString(1, retailerERP);
      pstmt.executeUpdate();
    }
    catch (Exception e)
    {
      ExceptionLogger.logException(pfb, "Invoice", "presortConSaCounts", null, e);
    }
    finally
    {
      TryCleanup.tryCleanup(pfb, pstmt, null);
    }
  }

  public static void runCabinetPlanTotalInvoiceDetailToDB(PosFileBean pfb, String retailerERP)
  {
    StringBuilder sb = new StringBuilder();

    sb.append("INSERT INTO sam_pos_invoice_stored_detail ");
    sb.append("            (erp_rtlr_no, ");
    sb.append("             rtlr_addr_id, ");
    sb.append("             dseq, ");
    sb.append("             erp_bill_to, ");
    sb.append("             trans_id, ");
    sb.append("             first_name, ");
    sb.append("             last_name, ");
    sb.append("             delpur_dt, ");
    sb.append("             con_sa_id, ");
    sb.append("             pricing_id, ");
    sb.append("             code, ");
    sb.append("             pricing_type, ");
    sb.append("             create_dt, ");
    sb.append("             pos_fh_id, ");
    sb.append("             sales, ");
    sb.append("             returns) ");
    sb.append("SELECT sam_rtlr_addr.erp_rtlr_no    erp_rtlr_no, ");
    sb.append("       sam_rtlr_addr.rtlr_addr_id   rtlr_addr_id, ");
    sb.append("       sam_rtlr_addr.erp_rtlr_dseq  dseq, ");
    sb.append("       nvl(sam_rtlr_addr.erp_bill_to, sam_rtlr_addr.erp_rtlr_dseq), ");
    sb.append("       sam_con_sa.pos_trans_id      trans_id, ");
    sb.append("       sam_con.first_name, ");
    sb.append("       sam_con.last_name, ");
    sb.append("       sam_con_sa.delpur_dt, ");
    sb.append("       sam_con_sa.con_sa_id, ");
    sb.append("       '799'                        pricing_id, ");
    sb.append("       sam_rtlr_pricing.description code, ");
    sb.append("       sam_rtlr_pricing.type        pricing_type, ");
    sb.append("       SYSDATE                      create_dt, ");
    sb.append("       :posFhId                     pos_fh_id, ");
    sb.append("       ( CASE Nvl(sam_con_sa.sls_qty_to_inv, 0) ");
    sb.append("           WHEN 0 THEN 0 ");
    sb.append("           ELSE 1 ");
    sb.append("         END )                      sales, ");
    sb.append("       ( CASE Nvl(sam_con_sa.rtrn_qty_to_inv, 0) ");
    sb.append("           WHEN 0 THEN 0 ");
    sb.append("           ELSE CASE (( Nvl(sam_con_sa.sls_qty_to_inv, 0) + ");
    sb.append("                Nvl(sam_con_sa.sls_qty_invoiced, 0)) ");
    sb.append("                - (Nvl(sam_con_sa.rtrn_qty_to_inv, 0) + ");
    sb.append("                Nvl(sam_con_sa.rtrn_qty_invoiced, ");
    sb.append("                0))) ");
    sb.append("                  WHEN 0 THEN 1 ");
    sb.append("                  ELSE 0 ");
    sb.append("                END ");
    sb.append("         END )                      returns ");
    sb.append("FROM   sam_con_sa, ");
    sb.append("       sam_rtlr_addr, ");
    sb.append("       sam_rtlr_pricing, ");
    sb.append("       sam_con ");
    sb.append("WHERE  sam_con_sa.rtlr_addr_id = sam_rtlr_addr.rtlr_addr_id ");
    sb.append("       AND sam_con_sa.con_id = sam_con.con_id ");
    sb.append("       AND sam_con_sa.pricing_method = sam_rtlr_pricing.rtlr_pricing_id ");
    sb.append("       AND ( sls_qty_to_inv IS NOT NULL ");
    sb.append("              OR rtrn_qty_to_inv IS NOT NULL ) ");
    sb.append("       AND sam_con_sa.logged_uid = 'POSLITE' ");
    sb.append("       AND sam_rtlr_pricing.rtlr_pricing_id = ( '798' ) ");
    sb.append("       AND sam_rtlr_addr.erp_rtlr_no IN ( :erpRtlrNo )   ");

    OraclePreparedStatement pstmt = null;
    OracleResultSet rs = null;
    try
    {
      pstmt = (OraclePreparedStatement) pfb.getConnection().prepareStatement(sb.toString());
      pstmt.setStringAtName("posFhId", pfb.getPosFhId());
      pstmt.setStringAtName("erpRtlrNo", retailerERP);
      pstmt.executeUpdate();
    }
    catch (Exception e)
    {
      ExceptionLogger.logException(pfb, "Invoice", "runCabinetPlanTotalInvoiceDetailToDB", null, e);
    }
    finally
    {
      TryCleanup.tryCleanup(pfb, pstmt, rs);
    }

  }

  public static void runReturnsOnDoubleTransCabinetsDetailToDB(PosFileBean pfb, String retailerERP)
  {
    StringBuilder sb = new StringBuilder();

    sb.append("INSERT INTO sam_pos_invoice_stored_detail ");
    sb.append("            (erp_rtlr_no, ");
    sb.append("             rtlr_addr_id, ");
    sb.append("             dseq, ");
    sb.append("             erp_bill_to, ");
    sb.append("             trans_id, ");
    sb.append("             first_name, ");
    sb.append("             last_name, ");
    sb.append("             delpur_dt, ");
    sb.append("             con_sa_id, ");
    sb.append("             pricing_id, ");
    sb.append("             code, ");
    sb.append("             pricing_type, ");
    sb.append("             create_dt, ");
    sb.append("             pos_fh_id, ");
    sb.append("             sales, ");
    sb.append("             returns) ");
    sb.append("SELECT erp_rtlr_no, ");
    sb.append("       rtlr_addr_id, ");
    sb.append("       dseq, ");
    sb.append("       bill_to, ");
    sb.append("       trans_id, ");
    sb.append("       first_name, ");
    sb.append("       last_name, ");
    sb.append("       delpur_dt, ");
    sb.append("       con_sa_id, ");
    sb.append("       (CASE pricing_id ");
    sb.append("         WHEN 798 THEN CASE Nvl(returns,0) ");
    sb.append("                         WHEN 1 THEN 793 ");
    sb.append("                         WHEN 2 THEN 794 ");
    sb.append("                         WHEN 3 THEN 795 ");
    sb.append("                         WHEN 4 THEN 796 ");
    sb.append("                         WHEN 5 THEN 797 ");
    sb.append("                         ELSE 798 ");
    sb.append("                       END ");
    sb.append("         ELSE pricing_id ");
    sb.append("       END) pricing_id, ");
    sb.append("       code, ");
    sb.append("       type, ");
    sb.append("       create_dt, ");
    sb.append("       pos_fh_id, ");
    sb.append("       sales, ");
    sb.append("       returns ");
    sb.append("FROM ( ");
    sb.append("SELECT sam_rtlr_addr.erp_rtlr_no                 erp_rtlr_no, ");
    sb.append("       sam_rtlr_addr.rtlr_addr_id                rtlr_addr_id, ");
    sb.append("       sam_rtlr_addr.erp_rtlr_dseq               dseq, ");
    sb.append("       nvl(sam_rtlr_addr.erp_bill_to, sam_rtlr_addr.erp_rtlr_dseq) bill_to, ");
    sb.append("       sam_con_sa.pos_trans_id                   trans_id, ");
    sb.append("       sam_con.first_name                        first_name, ");
    sb.append("       sam_con.last_name                         last_name, ");
    sb.append("       sam_con_sa.delpur_dt                      delpur_dt, ");
    sb.append("       sam_con_sa.con_sa_id                      con_sa_id, ");
    sb.append("       sam_rtlr_pricing.rtlr_pricing_id          pricing_id, ");
    sb.append("       sam_rtlr_pricing.description              code, ");
    sb.append("       sam_rtlr_pricing.TYPE                     TYPE, ");
    sb.append("       SYSDATE                                   create_dt, ");
    sb.append("       :posFhId                                  pos_fh_id, ");
    sb.append("       '0'                                       sales, ");
    sb.append("       SUM (Nvl (sam_con_sa.rtrn_qty_to_inv, 0)) returns ");
    sb.append("FROM   sam_con_sa, ");
    sb.append("       sam_rtlr_addr, ");
    sb.append("       sam_rtlr_pricing, ");
    sb.append("       sam_con ");
    sb.append("WHERE  sam_con_sa.rtlr_addr_id = sam_rtlr_addr.rtlr_addr_id ");
    sb.append("       AND sam_con_sa.con_id = sam_con.con_id ");
    sb.append("       AND sam_con_sa.pricing_method = sam_rtlr_pricing.rtlr_pricing_id ");
    sb.append("       AND sls_qty_to_inv IS NOT NULL ");
    sb.append("       AND rtrn_qty_to_inv IS NOT NULL ");
    sb.append("       AND sam_con_sa.logged_uid = 'POSLITE' ");
    sb.append("       AND sam_rtlr_addr.erp_rtlr_no IN ( :erpRtlrNo ) ");
    sb.append("GROUP  BY sam_rtlr_addr.erp_rtlr_no, ");
    sb.append("          sam_rtlr_addr.rtlr_addr_id, ");
    sb.append("          sam_rtlr_addr.erp_rtlr_dseq, ");
    sb.append("          sam_con_sa.pos_trans_id, ");
    sb.append("          nvl(sam_rtlr_addr.erp_bill_to, sam_rtlr_addr.erp_rtlr_dseq), ");
    sb.append("          sam_con.first_name, ");
    sb.append("          sam_con.last_name, ");
    sb.append("          sam_con_sa.delpur_dt, ");
    sb.append("          sam_con_sa.con_sa_id, ");
    sb.append("          sam_rtlr_pricing.rtlr_pricing_id, ");
    sb.append("          sam_rtlr_pricing.description, ");
    sb.append("          sam_rtlr_pricing.TYPE, ");
    sb.append("          SYSDATE, ");
    sb.append("          :posFhId ");
    sb.append(") ");

    OraclePreparedStatement pstmt = null;
    OracleResultSet rs = null;
    try
    {
      pstmt = (OraclePreparedStatement) pfb.getConnection().prepareStatement(sb.toString());
      pstmt.setStringAtName("erpRtlrNo", retailerERP);
      pstmt.setStringAtName("posFhId", pfb.getPosFhId());

      pstmt.executeUpdate();
    }
    catch (Exception e)
    {
      ExceptionLogger.logException(pfb, "Invoice", "runReturnsOnDoubleTransCabinetsDetailToDB", null, e);
    }
    finally
    {
      TryCleanup.tryCleanup(pfb, pstmt, rs);
    }

  }

  public static void runPosLiteInvoiceDetailToDB(PosFileBean pfb, String retailerERP)
  {
    StringBuilder sb = new StringBuilder();

    sb.append("INSERT INTO sam_pos_invoice_stored_detail ");
    sb.append("            (erp_rtlr_no, ");
    sb.append("             rtlr_addr_id, ");
    sb.append("             dseq, ");
    sb.append("             erp_bill_to, ");
    sb.append("             trans_id, ");
    sb.append("             first_name, ");
    sb.append("             last_name, ");
    sb.append("             delpur_dt, ");
    sb.append("             con_sa_id, ");
    sb.append("             pricing_id, ");
    sb.append("             code, ");
    sb.append("             pricing_type, ");
    sb.append("             create_dt, ");
    sb.append("             pos_fh_id, ");
    sb.append("             sales, ");
    sb.append("             returns) ");
    sb.append("SELECT erp_rtlr_no, ");
    sb.append("       rtlr_addr_id, ");
    sb.append("       dseq, ");
    sb.append("       bill_to, ");
    sb.append("       trans_id, ");
    sb.append("       first_name, ");
    sb.append("       last_name, ");
    sb.append("       delpur_dt, ");
    sb.append("       con_sa_id, ");
    sb.append("       (CASE pricing_id ");
    sb.append("         WHEN 798 THEN CASE COALESCE(sales,returns,0) ");
    sb.append("                         WHEN 1 THEN 793 ");
    sb.append("                         WHEN 2 THEN 794 ");
    sb.append("                         WHEN 3 THEN 795 ");
    sb.append("                         WHEN 4 THEN 796 ");
    sb.append("                         WHEN 5 THEN 797 ");
    sb.append("                         ELSE 798 ");
    sb.append("                       END ");
    sb.append("         ELSE pricing_id ");
    sb.append("       END) pricing_id, ");
    sb.append("       code, ");
    sb.append("       type, ");
    sb.append("       create_dt, ");
    sb.append("       pos_fh_id, ");
    sb.append("       sales, ");
    sb.append("       returns ");
    sb.append("FROM ( ");
    sb.append("SELECT sam_rtlr_addr.erp_rtlr_no                 erp_rtlr_no, ");
    sb.append("       sam_rtlr_addr.rtlr_addr_id                rtlr_addr_id, ");
    sb.append("       sam_rtlr_addr.erp_rtlr_dseq               dseq, ");
    sb.append("       nvl(sam_rtlr_addr.erp_bill_to, sam_rtlr_addr.erp_rtlr_dseq) bill_to, ");
    sb.append("       sam_con_sa.pos_trans_id                   trans_id, ");
    sb.append("       sam_con.first_name                        first_name, ");
    sb.append("       sam_con.last_name                         last_name, ");
    sb.append("       sam_con_sa.delpur_dt                      delpur_dt, ");
    sb.append("       sam_con_sa.con_sa_id                      con_sa_id, ");
    sb.append("       sam_rtlr_pricing.rtlr_pricing_id          pricing_id, ");
    sb.append("       sam_rtlr_pricing.description              code, ");
    sb.append("       sam_rtlr_pricing.TYPE                     TYPE, ");
    sb.append("       SYSDATE                                   create_dt, ");
    sb.append("       :posFhId                                  pos_fh_id, ");
    sb.append("       SUM (Nvl (sam_con_sa.sls_qty_to_inv, 0))  sales, ");
    sb.append("       SUM (Nvl (sam_con_sa.rtrn_qty_to_inv, 0)) returns ");
    sb.append("FROM   sam_con_sa, ");
    sb.append("       sam_rtlr_addr, ");
    sb.append("       sam_rtlr_pricing, ");
    sb.append("       sam_con ");
    sb.append("WHERE  sam_con_sa.rtlr_addr_id = sam_rtlr_addr.rtlr_addr_id ");
    sb.append("       AND sam_con_sa.con_id = sam_con.con_id ");
    sb.append("       AND sam_con_sa.pricing_method = sam_rtlr_pricing.rtlr_pricing_id ");
    sb.append("       AND ( sls_qty_to_inv IS NOT NULL ");
    sb.append("              OR rtrn_qty_to_inv IS NOT NULL ) ");
    sb.append("       AND sam_con_sa.logged_uid = 'POSLITE' ");
    sb.append("       AND sam_rtlr_addr.erp_rtlr_no IN ( :erpRtlrNo ) ");
    sb.append("GROUP  BY sam_rtlr_addr.erp_rtlr_no, ");
    sb.append("          sam_rtlr_addr.rtlr_addr_id, ");
    sb.append("          sam_rtlr_addr.erp_rtlr_dseq, ");
    sb.append("          nvl(sam_rtlr_addr.erp_bill_to, sam_rtlr_addr.erp_rtlr_dseq), ");
    sb.append("          sam_con_sa.pos_trans_id, ");
    sb.append("          sam_con.first_name, ");
    sb.append("          sam_con.last_name, ");
    sb.append("          sam_con_sa.delpur_dt, ");
    sb.append("          sam_con_sa.con_sa_id, ");
    sb.append("          sam_rtlr_pricing.rtlr_pricing_id, ");
    sb.append("          sam_rtlr_pricing.description, ");
    sb.append("          sam_rtlr_pricing.TYPE, ");
    sb.append("          SYSDATE, ");
    sb.append("          :posFhId ");
    sb.append(") ");

    OraclePreparedStatement pstmt = null;
    OracleResultSet rs = null;
    try
    {
      pstmt = (OraclePreparedStatement) pfb.getConnection().prepareStatement(sb.toString());
      pstmt.setStringAtName("erpRtlrNo", retailerERP);
      pstmt.setStringAtName("posFhId", pfb.getPosFhId());

      pstmt.executeUpdate();
    }
    catch (Exception e)
    {
      ExceptionLogger.logException(pfb, "Invoice", "runPosLiteInvoiceDetailToDB", null, e);
    }
    finally
    {
      TryCleanup.tryCleanup(pfb, pstmt, rs);
    }

  }

  public static void runErrorDetailToDB(PosFileBean pfb, String retailerERP)
  {
    StringBuilder sb = new StringBuilder();

    sb.append("insert into sam_pos_invoice_stored_detail(erp_rtlr_no, rtlr_addr_id, dseq, trans_id, ");
    sb.append("first_name,last_name,delpur_dt,con_sa_id,pricing_id, ");
    sb.append("code, pricing_type, create_dt, pos_fh_id, sales, returns) ");
    sb.append("SELECT RTLR_ERP_NO, NULL, NULL, transaction_id trans_id, ");
    sb.append("con_first_name first_name, con_last_name last_name, NULL dlv_dt, ");
    sb.append("NULL con_sa_id, pricing_code pricing_id, pos_error_msg code, 'Error', sysdate, :posFhId, ");
    sb.append("null Sales, null RETURN ");
    sb.append("FROM sam_pos_errors ");
    sb.append("WHERE rtlr_erp_no = :erpRtlrNo ");
    sb.append("AND pos_fh_id = :posFhId");

    OraclePreparedStatement pstmt = null;
    OracleResultSet rs = null;
    try
    {
      pstmt = (OraclePreparedStatement) pfb.getConnection().prepareStatement(sb.toString());
      pstmt.setStringAtName("erpRtlrNo", retailerERP);
      pstmt.setStringAtName("posFhId", pfb.getPosFhId());

      pstmt.executeUpdate();
    }
    catch (Exception e)
    {
      ExceptionLogger.logException(pfb, "Invoice", "runErrorDetailToDB", null, e);
    }
    finally
    {
      TryCleanup.tryCleanup(pfb, pstmt, rs);
    }
  }

  public static void runPipe36TransSalesUpdate(PosFileBean pfb, String retailerERP)
  {
    //only update those where the pricing code is a 'Sale'
    StringBuilder sb = new StringBuilder();
    sb.append("update sam_con_sls_rcpt_item ");
    sb.append("set invoice_dt = sysdate ");
    sb.append("where invoice_dt is null ");
    sb.append("and pricing_code is not null ");
    sb.append("and sls_rcpt_id in  ");
    sb.append("(select sr.sls_rcpt_id ");
    sb.append("from sam_con_sls_rcpt sr, sam_rtlr_addr rtlr ");
    sb.append("where sr.rtlr_addr_id = rtlr.rtlr_addr_id ");
    sb.append("and rtlr.erp_rtlr_no = ?) ");
    sb.append("and pricing_code in ");
    sb.append("(select sam_rtlr_pricing.rtlr_pricing_id ");
    sb.append("from sam_rtlr_pricing ");
    sb.append("where sam_rtlr_pricing.type = 'Sale') ");
    OraclePreparedStatement pstmt = null;
    try
    {
      pstmt = (OraclePreparedStatement) pfb.getConnection().prepareStatement(sb.toString());
      pstmt.setString(1, retailerERP);
      pstmt.executeUpdate();
    }
    catch (Exception e)
    {
      ExceptionLogger.logException(pfb, "Invoice", "runPipe36TransSalesUpdate", null, e);
    }
    finally
    {
      TryCleanup.tryCleanup(pfb, pstmt, null);
    }
  }

  public static void runPipe36TransReturnsUpdate(PosFileBean pfb, String retailerERP)
  {
    //only update those where the pricing code is a 'Sale'
    StringBuilder sb = new StringBuilder();
    sb.append("update sam_con_sls_rcpt_item ");
    sb.append("set invoice_rtrn_dt = sysdate ");
    sb.append("where status = 'R' ");
    sb.append("and invoice_rtrn_dt is null ");
    sb.append("and pricing_code is not null ");
    sb.append("and sls_rcpt_id in  ");
    sb.append("(select sr.sls_rcpt_id ");
    sb.append("from sam_con_sls_rcpt sr, sam_rtlr_addr rtlr ");
    sb.append("where sr.rtlr_addr_id = rtlr.rtlr_addr_id ");
    sb.append("and rtlr.erp_rtlr_no = ?) ");
    sb.append("and pricing_code in ");
    sb.append("(select sam_rtlr_pricing.rtlr_pricing_id ");
    sb.append("from sam_rtlr_pricing ");
    sb.append("where sam_rtlr_pricing.type = 'Sale') ");
    OraclePreparedStatement pstmt = null;
    try
    {
      pstmt = (OraclePreparedStatement) pfb.getConnection().prepareStatement(sb.toString());
      pstmt.setString(1, retailerERP);
      pstmt.executeUpdate();
    }
    catch (Exception e)
    {
      ExceptionLogger.logException(pfb, "Invoice", "runPipe36TransReturnsUpdate", null, e);
    }
    finally
    {
      TryCleanup.tryCleanup(pfb, pstmt, null);
    }
  }

  public static void runPlanPrintCount(PosFileBean pfb, String retailerERP)
  {
    StringBuilder sb = new StringBuilder();
    //sb.append("select count(*) ");
    sb.append("select count(*), sam_sa_type.sa_type_id, sam_sa_type.description ");
    sb.append("from sam_con_sa,sam_con_addr,sam_con,sam_sa_type,sam_rtlr_addr,sam_rtlr_ftp, sam_rtlr_erp ");
    sb.append("where sam_con_sa.con_id = sam_con.con_id ");
    sb.append("and sam_con_sa.rtlr_addr_id = sam_rtlr_addr.rtlr_addr_id ");
    sb.append("and sam_con_addr.con_id = sam_con.con_id ");
    sb.append("and sam_con_sa.sa_type_id = sam_sa_type.sa_type_id ");
    sb.append("and sam_rtlr_addr.erp_rtlr_no = sam_rtlr_erp.erp_rtlr_no ");
    sb.append("and sam_rtlr_erp.rtlr_ftp_id = sam_rtlr_ftp.rtlr_ftp_id ");
    sb.append("and sam_con_addr.status = 'Active' ");
    sb.append("and sam_con_sa.sa_status = 'Active' ");
    sb.append("and sam_sa_type.coverage_type <> 'X' ");
    sb.append("and sam_con_addr.type = 'Home' ");
    sb.append("and sam_con_sa.fpp_print_dt is null ");
    sb.append("and sam_con_sa.logged_uid = 'POS' ");
    sb.append("and sam_rtlr_addr.erp_rtlr_no = ? ");
    sb.append("group by sam_sa_type.sa_type_id, sam_sa_type.description");

    OraclePreparedStatement pstmt = null;
    OracleResultSet rs = null;
    try
    {
      pstmt = (OraclePreparedStatement) pfb.getConnection().prepareStatement(sb.toString());
      pstmt.setString(1, retailerERP);
      rs = (OracleResultSet) pstmt.executeQuery();
      while (rs.next())
      {
        //pfb.setPlanPrintCount(rs.getString(1));
        PlanPrintingBean ppb = new PlanPrintingBean();
        ppb.setPlanCnt(rs.getString(1));
        ppb.setSaType(rs.getString(2));
        ppb.setSaTypeDescription(rs.getString(3));
        pfb.getPlanPrintingItems().add(ppb);
      }
    }
    catch (Exception e)
    {
      ExceptionLogger.logException(pfb, "Invoice", "runPlanPrintCount", null, e);
    }
    finally
    {
      TryCleanup.tryCleanup(pfb, pstmt, rs);
    }
  }

  public static void runManualReturn(PosFileBean pfb, String retailerERP)
  {
    StringBuilder sb = new StringBuilder();

    sb.append("SELECT nvl(pricing_code,pricing_method) as Pricing, ");
    sb.append("p.description, p.type, ");
    sb.append("sum(qty) as Item_cnt, count(*) as Sale_cnt ");
    sb.append("FROM sam_pos_errors, sam_rtlr_pricing p ");
    sb.append("where p.rtlr_pricing_id = nvl(pricing_code,pricing_method) ");
    sb.append("and lower(validation_step) like '%return%' ");
    sb.append("and sam_pos_errors.rtlr_erp_no = ? ");
    sb.append("and sam_pos_errors.report_dt is null ");
    sb.append("group by nvl(pricing_code,pricing_method),p.description, p.type ");
    sb.append("order by nvl(pricing_code,pricing_method) ");
    OraclePreparedStatement pstmt = null;
    OracleResultSet rs = null;
    try
    {
      pstmt = (OraclePreparedStatement) pfb.getConnection().prepareStatement(sb.toString());
      pstmt.setString(1, retailerERP);
      rs = (OracleResultSet) pstmt.executeQuery();
      while (rs.next())
      {
        ManualReturnBean mrb = new ManualReturnBean();
        mrb.setPricingMethod(rs.getString(1));
        mrb.setPricingDescription(rs.getString(2));
        mrb.setType(rs.getString(3));
        mrb.setItemRtnCnt(rs.getString(4));
        mrb.setTransRtnCnt(rs.getString(5));
        pfb.getManualRtnItems().add(mrb);
      }
    }
    catch (Exception e)
    {
      ExceptionLogger.logException(pfb, "Invoice", "runManualReturn", null, e);
    }
    finally
    {
      TryCleanup.tryCleanup(pfb, pstmt, rs);
    }
  }

  public static void runManualReturnUpdate(PosFileBean pfb, String retailerERP)
  {
    StringBuilder sb = new StringBuilder();
    sb.append("update sam_pos_errors ");
    sb.append("set report_dt = sysdate ");
    sb.append("where report_dt is null ");
    sb.append("and lower(validation_step) like '%return%' ");
    sb.append("and rtlr_erp_no = ? ");
    OraclePreparedStatement pstmt = null;
    try
    {
      pstmt = (OraclePreparedStatement) pfb.getConnection().prepareStatement(sb.toString());
      pstmt.setString(1, retailerERP);
      pstmt.executeUpdate();
    }
    catch (Exception e)
    {
      ExceptionLogger.logException(pfb, "Invoice", "runManualReturnUpdate", null, e);
    }
    finally
    {
      TryCleanup.tryCleanup(pfb, pstmt, null);
    }
  }

  public static void createInvoiceDetailRpt(PosFileBean pfb, String retailerERP, String billTo)
  {

    File file = new File(pfb.getInvoicePath());
    boolean dirExists = file.exists();
    if (!dirExists)
    {
      file.mkdirs();
    }

    StringBuilder sb = new StringBuilder();
    sb.append("select 'header','','', 'Rtlr ERP No,Process Date, Trans Id,First Name,Last Name,Del/Pur Date,Con SA Id,Store No,Pricing Id,Pricing Desc,Sale,Return'  ");
    sb.append("from dual  ");
    sb.append("union all  ");
    sb.append("SELECT 'detail',det.erp_rtlr_no, det.trans_id, ''''||det.erp_rtlr_no||''','||create_dt||','||det.TRANS_ID||','||replace(det.FIRST_NAME,',')||','||replace(det.LAST_NAME,',')  ");
    sb.append("||','||det.DELPUR_DT||','|| det.CON_SA_ID||','''||  ");
    sb.append("r.RTLR_STORE_NO||''','|| det.PRICING_ID||','||replace(det.CODE,',')||','||sales||','||returns||','||decode(det.PRICING_TYPE,'Error','Error') as outLine  ");
    sb.append("FROM SAM_POS_INVOICE_STORED_DETAIL det, sam_rtlr_addr r  ");
    sb.append("where det.rtlr_addr_id = r.rtlr_addr_id (+)  ");
    sb.append("and pos_fh_id = :posFhId  ");
    sb.append("and det.erp_rtlr_no = :erpRtlrNo  ");
    sb.append("and det.erp_bill_to = :billTo  ");
    sb.append("order by 1 desc, 2, 3 ");

    OraclePreparedStatement pstmt = null;
    OracleResultSet rs = null;
    try
    {
      BufferedWriter out = new BufferedWriter(new FileWriter(pfb.getInvoicePath() + "/InvoiceDetail" + retailerERP + "_" + billTo + "_" + pfb.getPosFhId() + ".csv"));
      pstmt = (OraclePreparedStatement) pfb.getConnection().prepareStatement(sb.toString());
      pstmt.setStringAtName("posFhId", pfb.getPosFhId());
      pstmt.setStringAtName("erpRtlrNo", retailerERP);
      pstmt.setStringAtName("billTo", billTo);
      rs = (OracleResultSet) pstmt.executeQuery();
      while (rs.next())
      {
        out.write(rs.getString(4));
        out.newLine();
      }
      out.close();
    }
    catch (Exception e)
    {
      ExceptionLogger.logException(pfb, "Invoice", "createInvoiceDetailRpt", null, e);
    }
    finally
    {
      TryCleanup.tryCleanup(pfb, pstmt, rs);
    }
  }

  private static int billToCount(PosFileBean pfb, String retailerERP)
  {
    int billToCount = 0;

    StringBuilder sb = new StringBuilder();
    sb.append("SELECT count(unique(nvl(addr.erp_bill_to, addr.erp_rtlr_dseq))) ");
    sb.append("  FROM sam_pos_invoice_stored_detail detail ");
    sb.append("     , sam_rtlr_pricing pricing ");
    sb.append("     , sam_rtlr_addr addr ");
    sb.append(" WHERE detail.pricing_id = pricing.rtlr_pricing_id ");
    sb.append("   AND detail.rtlr_addr_id = addr.rtlr_addr_id ");
    sb.append("   AND detail.dseq = addr.erp_rtlr_dseq ");
    sb.append("   AND detail.pricing_type <> 'Error' ");
    sb.append("   AND detail.pos_fh_id = :posFhId ");
    sb.append("   AND detail.erp_rtlr_no = :erpRtlrNo ");

    OraclePreparedStatement pstmt = null;
    OracleResultSet rs = null;
    try
    {
      pstmt = (OraclePreparedStatement) pfb.getConnection().prepareStatement(sb.toString());
      pstmt.setStringAtName("posFhId", pfb.getPosFhId());
      pstmt.setStringAtName("erpRtlrNo", retailerERP);
      rs = (OracleResultSet) pstmt.executeQuery();
      while (rs.next())
      {
        billToCount = (rs.getInt(1));
      }
    }
    catch (Exception e)
    {
      ExceptionLogger.logException(pfb, "Invoice", "billToCount", null, e);
    }
    finally
    {
      TryCleanup.tryCleanup(pfb, pstmt, rs);
    }
    return billToCount;
  }

  private static String[] billToArray(PosFileBean pfb, String retailerERP)
  {
    String[] billToArray = new String[billToCount(pfb, retailerERP)];

    StringBuilder sb = new StringBuilder();
    sb.append("SELECT unique(nvl(addr.erp_bill_to, addr.erp_rtlr_dseq)) ");
    sb.append("  FROM sam_pos_invoice_stored_detail detail ");
    sb.append("     , sam_rtlr_pricing pricing ");
    sb.append("     , sam_rtlr_addr addr ");
    sb.append(" WHERE detail.pricing_id = pricing.rtlr_pricing_id ");
    sb.append("   AND detail.rtlr_addr_id = addr.rtlr_addr_id ");
    sb.append("   AND detail.dseq = addr.erp_rtlr_dseq ");
    sb.append("   AND detail.pricing_type <> 'Error' ");
    sb.append("   AND detail.pos_fh_id = :posFhId ");
    sb.append("   AND detail.erp_rtlr_no = :erpRtlrNo ");
    OraclePreparedStatement pstmt = null;
    OracleResultSet rs = null;
    try
    {
      pstmt = (OraclePreparedStatement) pfb.getConnection().prepareStatement(sb.toString());
      pstmt.setStringAtName("posFhId", pfb.getPosFhId());
      pstmt.setStringAtName("erpRtlrNo", retailerERP);
      rs = (OracleResultSet) pstmt.executeQuery();
      int i = 0;
      while (rs.next())
      {
        billToArray[i] = (rs.getString(1));
        i++;
      }
    }
    catch (Exception e)
    {
      ExceptionLogger.logException(pfb, "Invoice", "billToArray", null, e);
    }
    finally
    {
      TryCleanup.tryCleanup(pfb, pstmt, rs);
    }
    return billToArray;
  }

  /* private static boolean transCheck(PosFileBean pfb, String retailerERP, String erpBillTo)
  {
    boolean transCheck = false;
    int transCount = 0;
    StringBuilder sb = new StringBuilder();
    sb.append(" SELECT (SUM(detail.sales) + SUM(detail.returns)) total ");
    sb.append("   FROM sam_pos_invoice_stored_detail detail ");
    sb.append("      , sam_rtlr_pricing pricing ");
    sb.append("      , sam_rtlr_addr addr ");
    sb.append("  WHERE detail.pricing_id = pricing.rtlr_pricing_id ");
    sb.append("    AND detail.rtlr_addr_id = addr.rtlr_addr_id ");
    sb.append("    AND detail.dseq = addr.erp_rtlr_dseq ");
    sb.append("    AND detail.pricing_type <> 'Error' ");
    sb.append("    AND detail.pos_fh_id = :posFhId ");
    sb.append("    AND detail.erp_rtlr_no = :erpRtlrNo ");      (SELECT MAX(sam_rtlr_addr.erp_rtlr_dseq) 
          FROM sam_rtlr_addr 
         WHERE erp_rtlr_no = :erpRtlrNo 
           AND addr.erp_bill_to = sam_rtlr_addr.erp_bill_to) dseq 
    sb.append("    AND addr.erp_bill_to = :erpBillTo ");
    sb.append("  GROUP BY detail.dseq ");
    sb.append("      , addr.rtlr_store_no ");
    sb.append("      , addr.NAME ");
    sb.append("      , addr.city ");
    sb.append("      , addr.address1 ");
    sb.append("      , detail.pricing_id ");
    sb.append("      , detail.code ");
    sb.append("      , pricing.item_sku ");
    OraclePreparedStatement pstmt = null;
    OracleResultSet rs = null;
    try
    {
      pstmt = (OraclePreparedStatement) pfb.getConnection().prepareStatement(sb.toString());
      pstmt.setStringAtName("posFhId", pfb.getPosFhId());
      pstmt.setStringAtName("erpRtlrNo", retailerERP);
      pstmt.setStringAtName("erpBillTo", erpBillTo);
      rs = (OracleResultSet) pstmt.executeQuery();
      while (rs.next())
      {
        transCount = (rs.getInt(1));
      }
    }
    catch (Exception e)
    {
      ExceptionLogger.logException(pfb, "Invoice", "runInvoiceByStore", null, e);
    }
    finally
    {
      TryCleanup.tryCleanup(pfb, pstmt, rs);
    }
    if (transCount > 0)
    {
      transCheck = true;
    }
    return transCheck;
  }*/
}

