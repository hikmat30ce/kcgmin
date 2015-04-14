package com.valspar.interfaces.guardsman.pos;

import com.valspar.interfaces.guardsman.pos.beans.*;
import java.util.*;
import oracle.jdbc.*;
import com.valspar.interfaces.guardsman.pos.utility.*;

public class ExecuteCancels
{
  public ExecuteCancels(PosFileBean pfb)
  {
    Iterator i = pfb.getSalesReceipts().iterator();
    while (i.hasNext())
    {
      SalesReceiptBean srb = (SalesReceiptBean) i.next();
      if (srb.getErrors().size() == 0 && srb.isHasCancel())
      {
           Iterator j = srb.getSrHeaders().iterator();
           while (j.hasNext())
           {
             SrHeaderBean srhb = (SrHeaderBean) j.next();
             if (srhb.getTransCode().equals("C"))
             {
                 if (transactionExistCheck(pfb, srb))
                 { //Verify that transaction exists before it can be returned.
                   if (srb.isHasClaim())
                   {
                     ExceptionLogger.logError("Request logged against originating sale, cancel not allowed."," ", "Item Cancel", pfb, srb, null);
                   }
                   else
                   {
                     returnTransaction(pfb, srb);
                   }
                 }
                 else
                 {
                   ExceptionLogger.logError("Cancel failed, active transaction not found"," " , "Item Cancel", pfb, srb, null);
                 }
             } //Finished returning all items in this SR Transaction.
           }
           new UpdateConSA(pfb, srb);
      } 
      else if (srb.getErrors().size() > 0 && srb.isHasCancel())
      {
        ExceptionLogger.logError("Cancel not processed due to errors on Transaction.", null, "Item Return", pfb, srb, null);
      }
    }
  }


  public static void returnTransaction(PosFileBean pfb, SalesReceiptBean srb)
  {
    StringBuilder sb = new StringBuilder();
    sb.append("update ");
    sb.append("sam_con_sls_rcpt_item ");
    sb.append("set sam_con_sls_rcpt_item.status = 'R', ");
    sb.append("sam_con_sls_rcpt_item.tally_dt =  sysdate ");
    sb.append("where sam_con_sls_rcpt_item.sls_rcpt_id = ");
    sb.append("(select sam_con_sls_rcpt.sls_rcpt_id ");
    sb.append("from sam_con_sls_rcpt,sam_rtlr_addr ");    
    sb.append("where sam_con_sls_rcpt.rtlr_addr_id = sam_rtlr_addr.rtlr_addr_id ");
    sb.append("and sam_rtlr_addr.erp_rtlr_no = ? ");    
    sb.append("and sam_con_sls_rcpt.trans_id = ? ");
    if (srb.getPricingMethod() == null)
    {
      sb.append("and sam_con_sls_rcpt.pricing_method IS NULL) ");
    }
    else
    {
      sb.append("and sam_con_sls_rcpt.pricing_method = ?) ");
    }
    OraclePreparedStatement pstmt = null;
    try
    {
      pstmt = (OraclePreparedStatement) pfb.getConnection().prepareStatement(sb.toString());
      pstmt.setString(1, srb.getRetailerNo());
      pstmt.setString(2, srb.getTransId());
      if (srb.getPricingMethod() != null)
      {
        pstmt.setString(3, srb.getPricingMethod());
      }
      pstmt.executeUpdate();
    }
    catch (Exception e)
    {
      ExceptionLogger.logException(pfb, "ExecuteCancels", "returnTransaction", "TRANS_ID: " + srb.getTransId() , e);
    }
    finally
    {
      TryCleanup.tryCleanup(pfb, pstmt, null);
    }
  }

  public static boolean transactionExistCheck(PosFileBean pfb, SalesReceiptBean srb)
  {
    boolean inFile = false;
    if (srb.isHasSale() && srb.getErrors().size() == 0)
    {
      return true;     
    }
    else 
    {
      return transactionInDataBase(pfb, srb);
    }
  }

  public static boolean transactionInDataBase(PosFileBean pfb, SalesReceiptBean srb)
  {
    boolean inDataBase = false;
    String tempClaimCnt = new String();
    int cnt = 0;
    StringBuilder sb = new StringBuilder();
    sb.append("select sam_con_sls_rcpt_item.qty, sam_con_sls_rcpt_item.sls_rcpt_item_id,count(sam_claim.claim_id) ");
    sb.append("from sam_con_sls_rcpt_item,sam_con_sls_rcpt,sam_rtlr_addr,sam_claim ");
    sb.append("where ");
    sb.append("sam_con_sls_rcpt.rtlr_addr_id = sam_rtlr_addr.rtlr_addr_id ");
    sb.append("and sam_con_sls_rcpt_item.sls_rcpt_id = sam_con_sls_rcpt.sls_rcpt_id ");
    sb.append("and sam_con_sls_rcpt_item.con_sa_id = sam_claim.con_sa_id (+) ");
    sb.append("and sam_con_sls_rcpt.trans_id = ? ");
    sb.append("and sam_rtlr_addr.erp_rtlr_no = ? ");
    sb.append("and sam_con_sls_rcpt_item.status <> 'R' ");
    sb.append("group by sam_con_sls_rcpt_item.qty, sam_con_sls_rcpt_item.sls_rcpt_item_id ");
    OraclePreparedStatement pstmt = null;
    OracleResultSet rs = null;
    try
    {
      pstmt = (OraclePreparedStatement) pfb.getConnection().prepareStatement(sb.toString());
      pstmt.setString(1, srb.getTransId());
      pstmt.setString(2, srb.getRetailerNo());
      rs = (OracleResultSet) pstmt.executeQuery();
      while (rs.next())
      {
        cnt++;
        if (rs.getInt(3) > 0)
        {
          srb.setHasClaim(true);
        }
        else
        {
          srb.setHasClaim(false);
        }
      }
    }
    catch (Exception e)
    {
      ExceptionLogger.logException(pfb, "ExecuteCancels", "transactionInDataBase", "TRANS_ID: " + srb.getTransId(), e);
    }
    finally
    {
      TryCleanup.tryCleanup(pfb, pstmt, rs);
    }
    if (cnt >= 1)
    {
      inDataBase = true;
    }
    return inDataBase;
  }

}
