package com.valspar.interfaces.guardsman.pos;

import com.valspar.interfaces.guardsman.pos.beans.*;
import com.valspar.interfaces.guardsman.pos.utility.*;
import oracle.jdbc.*;
import org.apache.log4j.Logger;

public class RetailerSetup
{
  static Logger log4jLogger = Logger.getLogger(RetailerSetup.class.getName());

  public RetailerSetup(PosFileBean pfb, String folder)
  {
    log4jLogger.info("Retailer: " + folder);
    StringBuilder sb = new StringBuilder();
    sb.append("select description,email,invoice_email,status,file_format, ");
    sb.append("auto_invoice_flg,plan_printing_flg,man_return_flg, invoice_by_store, rtlr_ftp_id, ");
    sb.append("auto_invoice_to_jba, tally_disc, send_invoice_detail_rpt ");
    sb.append("from sam_rtlr_ftp where folder = ?");
    OraclePreparedStatement pstmt = null;
    OracleResultSet rs = null;
    try
    {
      pstmt = (OraclePreparedStatement) pfb.getConnection().prepareStatement(sb.toString());
      pstmt.setString(1, folder);
      rs = (OracleResultSet) pstmt.executeQuery();
      while (rs.next())
      {
        pfb.setRetailerName(rs.getString(1));
        pfb.setEmailAddr(rs.getString(2));
        pfb.setInvoiceEmailAddr(rs.getString(3));
        if (rs.getString(4) != null)
        {
          if (rs.getString(4).equalsIgnoreCase("A"))
          {
            pfb.setRetailerActive(true);
          }
        }
        pfb.setFileFormat(rs.getString(5));
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
        if (rs.getString(9) != null)
        {
          if (rs.getString(9).equalsIgnoreCase("Y"))
          {
            pfb.setInvoiceByStore(true);
          }
        }
        pfb.setRtlrFtpId(rs.getString(10));
        pfb.setAutoInvoiceToJba(rs.getString(11));
        pfb.setTallyDisc(rs.getString(12));
        
        if (rs.getString(13) != null)
        {
          if (rs.getString(13).equalsIgnoreCase("Y"))
          {
            pfb.setSendInvoiceDetailRpt(true);
          }
        }
      }
    }
    catch (Exception e)
    {
      ExceptionLogger.logException(pfb, "DbValidation", "cacheRetailers", null, e);
    }
    finally
    {
      TryCleanup.tryCleanup(pfb, pstmt, rs);
    }
  }
}
