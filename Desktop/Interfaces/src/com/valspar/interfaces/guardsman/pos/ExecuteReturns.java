package com.valspar.interfaces.guardsman.pos;

import com.valspar.interfaces.guardsman.pos.beans.*;
import java.util.*;
import oracle.jdbc.*;
import com.valspar.interfaces.guardsman.pos.utility.*;

public class ExecuteReturns
{
  public ExecuteReturns(PosFileBean pfb)
  {
    reserveIDsForReturn(pfb);
    Iterator i = pfb.getSalesReceipts().iterator();
    while (i.hasNext())
    {
      SalesReceiptBean srb = (SalesReceiptBean) i.next();
      if (srb.getErrors().size() == 0 && srb.isHasReturn())
      {
        Iterator j = srb.getSrHeaders().iterator();
        while (j.hasNext())
        {
          SrHeaderBean srhb = (SrHeaderBean) j.next();
          if (srhb.getTransCode().equals("R"))
          {
            Iterator k = srhb.getSrDetails().iterator();
            while (k.hasNext())
            {
              SrDetailBean srdb = (SrDetailBean) k.next();
              if (detailExistCheck(pfb, srb, srdb))
              { //Verify that a item exists before it can be returned.
                if (srdb.getClaimCnt()== 0)
                {
                  returnItem(pfb, srb, srdb);
                  if ((Integer.parseInt(srdb.getQty()) > 0) && (srdb.getOriginalQty() > Integer.parseInt(srdb.getQty())))
                  { //If the return items qty is not zero but is less than the original, create a new item.
                    createModifiedSalesDetail(pfb, srb, srdb);
                  }
                }
                else
                {
                  ExceptionLogger.logError("Request logged against originating sale, return not allowed.", " ITEM_ID: " + srdb.getItemId() + " PM: " + srb.getPricingMethod() + " PRICING_CODE: " + srdb.getPricingCode() + " QTY: " + srdb.getQty(), "Item Return", pfb, srb, srdb);
                }
              }
              else
              {
                ExceptionLogger.logError("Return failed, active item not found", " ITEM_ID: " + srdb.getItemId() + " PM: " + srb.getPricingMethod() + " PRICING_CODE: " + srdb.getPricingCode() + " QTY: " + srdb.getQty(), "Item Return", pfb, srb, srdb);
              }
            } //Finished returning all items in this SR Transaction.
          }
        }
        new UpdateConSA(pfb, srb);
      } else if (srb.getErrors().size() > 0 && srb.isHasReturn())
      {
        ExceptionLogger.logError("Return not processed due to errors on Transaction.", null, "Item Return", pfb, srb, null);
      }
    }
  }

  public static void reserveIDsForReturn(PosFileBean pfb)
  {
    int numOfRecords = 0;

    Iterator i = pfb.getSalesReceipts().iterator();
    while (i.hasNext())
    {
      SalesReceiptBean srb = (SalesReceiptBean) i.next();
      if (srb.getErrors().size() == 0 && srb.isHasReturn())
      {
        Iterator j = srb.getSrHeaders().iterator();
        while (j.hasNext())
        {
          SrHeaderBean srhb = (SrHeaderBean) j.next();
          if (srhb.getTransCode().equals("R"))
          {
            numOfRecords = numOfRecords + srhb.getSrDetails().size();
          }
        }
      }
    }
    fetchReturnSrItemIDs(pfb, numOfRecords);
  }

  public static void fetchReturnSrItemIDs(PosFileBean pfb, int numOfRecords)
  {
    int lastId = 0;
    int endId = 0;
    int counterFound = 0;
    StringBuilder sb = new StringBuilder();
    sb.append("select last_one from counter where item =?");
    StringBuilder updateSb = new StringBuilder();
    updateSb.append("update counter set last_one = ? where item = ? and last_one = ?");
    OraclePreparedStatement pstmt = null;
    OraclePreparedStatement pstmtUpdate = null;
    OracleResultSet rs = null;
    try
    {
      pstmt = (OraclePreparedStatement) pfb.getConnection().prepareStatement(sb.toString());
      pstmtUpdate = (OraclePreparedStatement) pfb.getConnection().prepareStatement(updateSb.toString());
      pstmt.setString(1, "sam_con_sls_rcpt_item.sls_rcpt_item_id");
      rs = (OracleResultSet) pstmt.executeQuery();
      while (rs.next())
      {
        lastId = rs.getInt(1);
        counterFound += 1;
      }
      if (counterFound == 1)
      {
        endId = lastId + numOfRecords;
        pstmtUpdate.setInt(1, endId);
        pstmtUpdate.setString(2, "sam_con_sls_rcpt_item.sls_rcpt_item_id");
        pstmtUpdate.setInt(3, lastId);
        pstmtUpdate.executeUpdate(); //if this went ok the update pfb
        pfb.setReturnSrItemNextId(lastId + 1);
        pfb.setReturnSrItemLastId(endId);
      }
      else
      {
        ExceptionLogger.logError("Failed to get Return ID's for SR Item", null, "SR Counter Fetch", pfb, null, null);
      }
    }
    catch (Exception e)
    {
      ExceptionLogger.logException(pfb, "ExecuteReturns", "fetchReturnSrItemIDs", null, e);
    }
    finally
    {
      TryCleanup.tryCleanup(pfb, pstmt, rs);
      TryCleanup.tryCleanup(pfb, pstmtUpdate, null);
    }
  }

  public static void returnItem(PosFileBean pfb, SalesReceiptBean srb, SrDetailBean srdb)
  {
    StringBuilder sb = new StringBuilder();
    sb.append("update ");
    sb.append("sam_con_sls_rcpt_item ");
    sb.append("set sam_con_sls_rcpt_item.status = 'R', ");
    sb.append("sam_con_sls_rcpt_item.tally_dt =  sysdate ");
    sb.append("where sam_con_sls_rcpt_item.item_id = ? ");
    sb.append("and sam_con_sls_rcpt_item.sls_rcpt_id = ? ");
    OraclePreparedStatement pstmt = null;
    try
    {
      pstmt = (OraclePreparedStatement) pfb.getConnection().prepareStatement(sb.toString());
      pstmt.setString(1, srdb.getItemId());
      pstmt.setString(2, srb.getSamSrId());
      pstmt.executeUpdate();
    }
    catch (Exception e)
    {
      ExceptionLogger.logException(pfb, "ExecuteReturns", "returnItem", "TRANS_ID: " + srb.getTransId() + "ITEM_ID: " + srdb.getItemId(), e);
    }
    finally
    {
      TryCleanup.tryCleanup(pfb, pstmt, null);
    }
  }

  public static boolean detailExistCheck(PosFileBean pfb, SalesReceiptBean srb, SrDetailBean returnDetail)
  {
    boolean inFile = false;
    Iterator i = srb.getSrHeaders().iterator();
    while (i.hasNext())
    {
      SrHeaderBean srhb = (SrHeaderBean) i.next();
      if (srhb.getTransCode().equals("S"))
      {
        Iterator j = srhb.getSrDetails().iterator();
        while (j.hasNext() && !inFile)
        {
          SrDetailBean saleDetail = (SrDetailBean) j.next();
          if (saleDetail.getItemId().equals(returnDetail.getItemId()))
          {
            returnDetail.setSamSrItemId(saleDetail.getSamSrItemId());
            returnDetail.setOriginalQty(Integer.parseInt(saleDetail.getQty()));
            returnDetail.setClaimCnt(0);
            inFile = true;
          }
        }
      }
    }
    if (!inFile)
    {
      inFile = detailInDataBase(pfb, srb, returnDetail);
    }
    return inFile;
  }

  public static boolean detailInDataBase(PosFileBean pfb, SalesReceiptBean srb, SrDetailBean returnDetail)
  {
    boolean inDataBase = false;
    int cnt = 0;
    StringBuilder sb = new StringBuilder();
    sb.append("select sam_con_sls_rcpt_item.qty, sam_con_sls_rcpt_item.sls_rcpt_item_id,count(sam_claim.claim_id) ");
    sb.append("from sam_con_sls_rcpt_item,sam_con_sls_rcpt,sam_rtlr_addr,sam_claim ");
    sb.append("where ");
    sb.append("sam_con_sls_rcpt_item.item_id = ?  ");
    sb.append("and sam_con_sls_rcpt.rtlr_addr_id = sam_rtlr_addr.rtlr_addr_id ");
    sb.append("and sam_con_sls_rcpt_item.sls_rcpt_id = sam_con_sls_rcpt.sls_rcpt_id ");
    sb.append("and sam_con_sls_rcpt_item.con_sa_id = sam_claim.con_sa_id (+) ");
    sb.append("and sam_con_sls_rcpt.trans_id = ? ");
    sb.append("and sam_rtlr_addr.erp_rtlr_no = ? ");
    sb.append("and sam_con_sls_rcpt.sls_rcpt_id = ? ");
    sb.append("and sam_con_sls_rcpt_item.status <> 'R' ");
    sb.append("group by sam_con_sls_rcpt_item.qty, sam_con_sls_rcpt_item.sls_rcpt_item_id ");
    OraclePreparedStatement pstmt = null;
    OracleResultSet rs = null;
    try
    {
      pstmt = (OraclePreparedStatement) pfb.getConnection().prepareStatement(sb.toString());
      pstmt.setString(1, returnDetail.getItemId());
      pstmt.setString(2, srb.getTransId());
      pstmt.setString(3, srb.getRetailerNo());
      pstmt.setString(4, srb.getSamSrId());
      rs = (OracleResultSet) pstmt.executeQuery();
      while (rs.next())
      {
        cnt++;
        returnDetail.setOriginalQty(rs.getInt(1));
        returnDetail.setSamSrItemId(rs.getString(2));
        returnDetail.setClaimCnt(rs.getInt(3));
      }
    }
    catch (Exception e)
    {
      ExceptionLogger.logException(pfb, "ExecuteReturns", "detailInDataBase", "TRANS_ID: " + srb.getTransId() + "ITEM_ID: " + returnDetail.getItemId(), e);
    }
    finally
    {
      TryCleanup.tryCleanup(pfb, pstmt, rs);
    }
    if (cnt == 1)
    {
      inDataBase = true;
    }
    else if (cnt > 1)
    {
      ExceptionLogger.logError("Original not found when Return was attempted", " ITEM_ID: " + returnDetail.getItemId() +", "+ cnt + " records found.", "Return Item Exist Check", pfb, srb, returnDetail);
    }
    return inDataBase;
  }

  public static void createModifiedSalesDetail(PosFileBean pfb, SalesReceiptBean srb, SrDetailBean srdb)
  {
    // Create a new SR Item based on the original. Adjust the QTY according the the remaining given in the file.    
    if (pfb.getReturnSrItemNextId() <= pfb.getReturnSrItemLastId())
    {
      StringBuilder sb = new StringBuilder();
      sb.append("insert into sam_con_sls_rcpt_item( ");
      sb.append("ack_no,color_style,control_no,description, ");
      sb.append("discount,furniture_other,mfg_id,mfg_other, ");
      sb.append("notes,price_amt,product_no,qty, ");
      sb.append("serial_no,sku_no,sls_rcpt_id,furniture_id, ");
      sb.append("pricing_code,item_id,sa_type,delivered_dt, ");
      sb.append("con_sa_id,item_sa_amt,tally_dt,pos_add_on_flg, ");
      sb.append("logged_dt,logged_uid,status,sls_rcpt_item_id)  ");
      sb.append("select ");
      sb.append("ack_no,color_style,control_no,description, ");
      sb.append("discount,furniture_other,mfg_id,mfg_other, ");
      sb.append("notes,price_amt,product_no,?, ");
      sb.append("serial_no,sku_no,sls_rcpt_id,furniture_id, ");
      sb.append("pricing_code,item_id,sa_type,delivered_dt, ");
      sb.append("con_sa_id,item_sa_amt,sysdate,'Y',sysdate,'POS','A',? ");
      sb.append("from sam_con_sls_rcpt_item where sls_rcpt_item_id = ? ");
      OraclePreparedStatement pstmt = null;
      try
      {
        pstmt = (OraclePreparedStatement) pfb.getConnection().prepareStatement(sb.toString());
        pstmt.setString(1, srdb.getQty());
        pstmt.setString(2, String.valueOf(pfb.getReturnSrItemNextId()));
        pstmt.setString(3, srdb.getSamSrItemId());
        pstmt.executeUpdate();
      }
      catch (Exception e)
      {
        ExceptionLogger.logException(pfb, "ExecuteReturns", "createModifiedSalesDetail", "TRANS_ID: " + srb.getTransId() + "ITEM_ID: " + srdb.getItemId(), e);
      }
      finally
      {
        TryCleanup.tryCleanup(pfb, pstmt, null);
      }
      pfb.setReturnSrItemNextId(pfb.getReturnSrItemNextId() + 1);
    }
    else
    {
      ExceptionLogger.logError("Record ID Not allocated for SR Detail", null, "SR Detail Modified insert", pfb, srb, srdb);
    }
  }
}
