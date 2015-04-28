package com.valspar.interfaces.guardsman.pos.utility;

import com.valspar.interfaces.guardsman.pos.beans.*;
import java.sql.SQLException;
import java.util.StringTokenizer;
import oracle.jdbc.*;
import org.apache.log4j.Logger;

public class ExceptionLogger
{
  static Logger log4jLogger = Logger.getLogger(ExceptionLogger.class.getName());

  public static void logException(PosFileBean pfb, String inClassName, String inMethodName, String inItem, Exception inException)
  {
    AdminMsgBean amb = new AdminMsgBean();
    amb.setMessage("Exception: " + inException);
    amb.setProgramLocation("Class: " + inClassName + " Method: " + inMethodName);
    amb.setItem(inItem);
    pfb.getAdminMessages().add(amb);
  }

  public static void logError(String message, String record, String spot, PosFileBean pfb, SalesReceiptBean srb, SrDetailBean srdb)
  {
    ErrorBean eb = new ErrorBean();
    eb.setErrorMsg(message);
    eb.setValidationStep(spot);
    eb.setPos_file_name(pfb.getFileName());
    if (srb == null)
    {
      eb.setRecord(record);
      pfb.getErrors().add(eb);
    }
    else
    {
      if (record != null)
      {
        eb.setRecord("TRANS_ID: " + srb.getTransId() + record);
      }
      else
      {
        eb.setRecord("TRANS_ID: " + srb.getTransId());
      }
      srb.getErrors().add(eb);

      eb.setTransID(srb.getTransId());
      eb.setConFirstName(srb.getFirstName());
      eb.setConLastName(srb.getLastName());
      eb.setPricingMethod(srb.getPricingMethod());
      eb.setRtlr_erp_no(srb.getRetailerNo());

      if (srdb != null)
      {
        eb.setPricingCode(srdb.getPricingCode());
        eb.setQty(srdb.getQty());
      }

      if (srb.getTab18ItemQty() != null)
      {
        eb.setQty(srb.getTab18ItemQty());
      }
    }
    writeError(eb, pfb);
  }

  private static void writeError(ErrorBean eb, PosFileBean pfb)
  {
    OraclePreparedStatement pstmt = null;

    StringBuilder sb = new StringBuilder();
    sb.append("insert into sam_pos_errors ");
    sb.append("(pos_error_id,transaction_id,con_last_name,con_first_name,");
    sb.append("pos_error_msg,pricing_method,validation_step,logged_dt,");
    sb.append("pricing_code,qty,pos_file_name,rtlr_erp_no,pos_fh_id,record)");
    sb.append("values ");
    sb.append("(sam_pos_errors_id_seq.nextval,?,?,?,?,?,?,sysdate,?,?,?,?,?,?)");

    try
    {
      pstmt = (OraclePreparedStatement) pfb.getConnection().prepareStatement(sb.toString());
      pstmt.setString(1, eb.getTransID());
      pstmt.setString(2, eb.getConLastName());
      pstmt.setString(3, eb.getConFirstName());
      pstmt.setString(4, eb.getErrorMsg());
      pstmt.setString(5, eb.getPricingMethod());
      pstmt.setString(6, eb.getValidationStep());
      pstmt.setString(7, eb.getPricingCode());
      pstmt.setString(8, eb.getQty());
      pstmt.setString(9, eb.getPos_file_name());
      pstmt.setString(10, eb.getRtlr_erp_no());
      pstmt.setString(11, pfb.getPosFhId());
      pstmt.setString(12, eb.getRecord());

      pstmt.executeUpdate();

      pstmt.close();
    }
    catch (SQLException e)
    {
      log4jLogger.error("Error in ExceptionLogger.writeError(): " + e);
    }
  }
}
