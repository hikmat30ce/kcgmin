package com.valspar.interfaces.guardsman.pos;

import com.valspar.interfaces.guardsman.pos.beans.*;
import com.valspar.interfaces.guardsman.pos.utility.*;
import oracle.jdbc.*;

/*
     * For the srb that has been modified by either an Update or Return in the
     * transaction, the ConSA must be updated to reflect the changes.
     *    If a ConSA is Elite or Case Goods then it should be set to 'Inactive'
     * if all the related items have been returned.
     *    If a ConSA is an Itemized plan and active items remain,
     * the SA Amount should be modified to be the sum of the remaining items.
     *    If a ConSA is an Itemized pland and no active items remain,
     * Inactivate the Con SA.
     */
public class UpdateConSA
{
  public UpdateConSA(PosFileBean pfb, SalesReceiptBean srb)
  {
    String conSaId = new String();
    String saAmt = new String();
    String coverageType = new String();

    StringBuilder sb = new StringBuilder();
    sb.append("SELECT sam_con_sa.con_sa_id, sam_con_sa.sa_amt, sam_sa_type.coverage_type ");
    sb.append("FROM sam_con_sa, sam_sa_type ");
    sb.append("WHERE sam_con_sa.sa_type_id = sam_sa_type.sa_type_id ");
    sb.append("AND sam_con_sa.sls_rcpt_id = ? ");
    OraclePreparedStatement pstmt = null;
    OracleResultSet rs = null;
    try
    {
      pstmt = (OraclePreparedStatement) pfb.getConnection().prepareStatement(sb.toString());
      pstmt.setString(1, srb.getSamSrId());
      rs = (OracleResultSet) pstmt.executeQuery();
      while (rs.next())
      {
        conSaId = rs.getString(1);
        saAmt = rs.getString(2);
        coverageType = rs.getString(3);
        fullReturnOrUpdate(pfb, srb, conSaId, saAmt, coverageType);
      }
    }
    catch (Exception e)
    {
      ExceptionLogger.logException(pfb, "UpdateConSA", "UpdateConSA", "TRANS_ID: " + srb.getTransId(), e);
    }
    finally
    {
      TryCleanup.tryCleanup(pfb, pstmt, rs);
    }
  }

  public static void fullReturnOrUpdate(PosFileBean pfb, SalesReceiptBean srb, String conSaId, String saAmt, String coverageType)
  {
    int qty = 0;
    String itemSaAmt = null;
    StringBuilder sb = new StringBuilder();
    sb.append("select sum(item.qty), sum(item.item_sa_amt) ");
    sb.append("from sam_con_sls_rcpt_item item,sam_con_sls_rcpt,sam_rtlr_addr ");
    sb.append("where sam_con_sls_rcpt.sls_rcpt_id = item.sls_rcpt_id ");
    sb.append("and sam_con_sls_rcpt.rtlr_addr_id = sam_rtlr_addr.rtlr_addr_id ");
    sb.append("and sam_con_sls_rcpt.sls_rcpt_id = ? ");
    sb.append("and sam_rtlr_addr.erp_rtlr_no = ? ");
    sb.append("and item.con_sa_id = ?  ");
    sb.append("and item.status = 'A'  ");
    OraclePreparedStatement pstmt = null;
    OracleResultSet rs = null;
    try
    {
      pstmt = (OraclePreparedStatement) pfb.getConnection().prepareStatement(sb.toString());
      pstmt.setString(1, srb.getSamSrId());
      pstmt.setString(2, srb.getRetailerNo());
      pstmt.setString(3, conSaId);
      rs = (OracleResultSet) pstmt.executeQuery();
      while (rs.next())
      {
        qty = rs.getInt(1);
        itemSaAmt = rs.getString(2);
      }
    }
    catch (Exception e)
    {
      ExceptionLogger.logException(pfb, "UpdateConSA", "UpdateConSA", "Con SA ID: " + conSaId, e);
    }
    finally
    {
      TryCleanup.tryCleanup(pfb, pstmt, rs);
    }

    if (qty == 0)
    {
      inactivateConSA(pfb, conSaId);
      if ((coverageType.equalsIgnoreCase("A")) || (coverageType.equalsIgnoreCase("C")))
      {
        returnSalesReceiptHeader(pfb, conSaId);
      }
    }
    else if (qty > 0)
    {
      changeConSaPrice(pfb, conSaId, itemSaAmt);
    }
  }

  public static void changeConSaPrice(PosFileBean pfb, String conSaId, String itemSaAmt)
  {
    StringBuilder sb = new StringBuilder();
    sb.append("update sam_con_sa set sa_amt = ? , ");
    sb.append("change_dt = sysdate, change_uid = 'POS' where con_sa_id = ?");
    OraclePreparedStatement pstmt = null;
    try
    {
      pstmt = (OraclePreparedStatement) pfb.getConnection().prepareStatement(sb.toString());
      pstmt.setString(1, itemSaAmt);
      pstmt.setString(2, conSaId);
      pstmt.executeUpdate();
    }
    catch (Exception e)
    {
      ExceptionLogger.logException(pfb, "UpdateConSA", "changeConSaPrice", "Con SA Id: " + conSaId + " Item SA Amt: " + itemSaAmt, e);
    }
    finally
    {
      TryCleanup.tryCleanup(pfb, pstmt, null);
    }
  }

  public static void inactivateConSA(PosFileBean pfb, String conSaId)
  {
    StringBuilder sb = new StringBuilder();
    sb.append("update sam_con_sa set sa_amt = 0, sa_status = 'R', change_dt = sysdate, change_uid = 'POS' where con_sa_id = ?");
    OraclePreparedStatement pstmt = null;
    try
    {
      pstmt = (OraclePreparedStatement) pfb.getConnection().prepareStatement(sb.toString());
      pstmt.setString(1, conSaId);
      pstmt.executeUpdate();
    }
    catch (Exception e)
    {
      ExceptionLogger.logException(pfb, "UpdateConSA", "inactivateConSA", "Con SA Id: " + conSaId, e);
    }
    finally
    {
      TryCleanup.tryCleanup(pfb, pstmt, null);
    }
  }

  public static void returnSalesReceiptHeader(PosFileBean pfb, String conSaId)
  {
    StringBuilder sb = new StringBuilder();
    sb.append("update sam_con_sls_rcpt set tally_dt = sysdate, ");
    sb.append("status = 'R' where sls_rcpt_id = (select sam_con_sa.sls_rcpt_id ");
    sb.append("from sam_con_sa where con_sa_id = ?)");
    OraclePreparedStatement pstmt = null;
    try
    {
      pstmt = (OraclePreparedStatement) pfb.getConnection().prepareStatement(sb.toString());
      pstmt.setString(1, conSaId);
      pstmt.executeUpdate();
    }
    catch (Exception e)
    {
      ExceptionLogger.logException(pfb, "UpdateConSA", "returnSalesReceiptHeader", "Con SA Id: " + conSaId, e);
    }
    finally
    {
      TryCleanup.tryCleanup(pfb, pstmt, null);
    }
  }
}
