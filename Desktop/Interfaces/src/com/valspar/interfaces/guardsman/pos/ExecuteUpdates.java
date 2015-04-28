package com.valspar.interfaces.guardsman.pos;

import com.valspar.interfaces.guardsman.pos.beans.*;
import java.util.*;
import oracle.jdbc.*;
import com.valspar.interfaces.guardsman.pos.utility.*;

public class ExecuteUpdates
{
  public ExecuteUpdates(PosFileBean pfb)
  {
    newDetailNewConSA(pfb);
    Iterator i = pfb.getSalesReceipts().iterator(); //Loop through SR's
    while (i.hasNext())
    {
      SalesReceiptBean srb = (SalesReceiptBean) i.next();
      if (srb.getErrors().size() == 0 && srb.isHasUpdate())
      {
        pricingMethodUpdate(pfb, srb);
        Iterator j = srb.getSrHeaders().iterator(); //Find the Update Header
        while (j.hasNext())
        {
          SrHeaderBean srhb = (SrHeaderBean) j.next();
          if (srhb.getTransCode().equals("U"))
          {
            Iterator k = srhb.getSrDetails().iterator(); //Loop through SR Items
            while (k.hasNext())
            {
              SrDetailBean srdb = (SrDetailBean) k.next();
              if (srdb.isUpdateAddition())
              {
                //kah #110510 - check again if transaction is in system or not
                boolean inFile = false;
                inFile = detailInDataBase(pfb, srb, srdb);
                if (!inFile)
                {
                  insertSRDetail(pfb, srb, srdb);
                  //check status of con sa, if not active, reactivate
                  if (!isConSaActive(pfb, srb, srdb))
                  {
                    reactiveConSa(pfb, srb, srdb);
                  }
                } else
                {
                  updateExistingSrItem(pfb, srb, srdb);
                }
              }
              else
              { // Update existing srdb
                updateExistingSrItem(pfb, srb, srdb);
              }
            }
          }
        }
      }
    }
  }

  public static boolean isConSaActive(PosFileBean pfb, SalesReceiptBean srb, SrDetailBean srdb)
  {
    boolean isActive = false;
    
    StringBuilder sb = new StringBuilder();
    sb.append("select sam_con_sa.con_sa_id ");
    sb.append("from sam_con_sa ");
    sb.append("where sam_con_sa.con_sa_id = ? ");
    sb.append("and sam_con_sa.sa_status = 'Active' ");
    OraclePreparedStatement pstmt = null;
    OracleResultSet rs = null;
    try
    {
      pstmt = (OraclePreparedStatement) pfb.getConnection().prepareStatement(sb.toString());
      if (srdb.getPricingCode() != null)
      {
        pstmt.setString(1,srdb.getSamConSaId());
      }
      else if (srdb.getPricingCode() == null)
      {
        pstmt.setString(1, srb.getSamEliteConSAId());
      }
      rs = (OracleResultSet) pstmt.executeQuery();
      if (rs.next())
      {
        isActive = true;
      }
    }
    catch (Exception e)
    {
      ExceptionLogger.logException(pfb, "ExecuteUpdates", "isConSaActive", null, e);
    }
    finally
    {
      TryCleanup.tryCleanup(pfb, pstmt, rs);
    }
    
    return isActive;
  }
  
  public static void reactiveConSa(PosFileBean pfb, SalesReceiptBean srb, SrDetailBean srdb)
  {
    String pricingCode = "";
    String conSaId = "";
    
    StringBuilder sb = new StringBuilder();
    sb.append("update sam_con_sa ");
    sb.append("set sa_status = 'Active', ");
    sb.append("fpp_print_dt = null ");
    sb.append("where sam_con_sa.con_sa_id = ? ");
    OraclePreparedStatement pstmt = null;
    try
    {
      pstmt = (OraclePreparedStatement) pfb.getConnection().prepareStatement(sb.toString());
      if (srdb.getPricingCode() != null)
      {
        pstmt.setString(1,srdb.getSamConSaId());
        pricingCode = srdb.getPricingCode();
        conSaId = srdb.getSamConSaId();
      }
      else if (srdb.getPricingCode() == null)
      {
        pstmt.setString(1, srb.getSamEliteConSAId());
        pricingCode = srb.getPricingMethod();
        conSaId = srb.getSamEliteConSAId();
      }
      pstmt.executeUpdate();
      //invoice dates need to be cleared so newly added items can be invoiced
      if (isTranBasedInv(pfb, pricingCode))
      {
        clearSRItemInvDates(pfb, conSaId, pricingCode);
      }  
    }
    catch (Exception e)
    {
      ExceptionLogger.logException(pfb, "ExecuteUpdates", "clearSRItemInvDates", null, e);
    }
    finally
    {
      TryCleanup.tryCleanup(pfb, pstmt, null);
    }
  }
  
  public static boolean isTranBasedInv(PosFileBean pfb, String pricingCode)
  {
    boolean isTranBasedInv = false;
    
    StringBuilder sb = new StringBuilder();
    sb.append("select 'x' ");
    sb.append("from sam_rtlr_pricing ");
    sb.append("where rtlr_pricing_id = "+pricingCode+" ");
    sb.append("and type = 'Sale'");
    OraclePreparedStatement pstmt = null;
    OracleResultSet rs = null;
    try
    {
      pstmt = (OraclePreparedStatement) pfb.getConnection().prepareStatement(sb.toString());
      rs = (OracleResultSet) pstmt.executeQuery();
      if (rs.next())
      {
        isTranBasedInv = true;
      }
    }
    catch (Exception e)
    {
      ExceptionLogger.logException(pfb, "ExecuteUpdates", "isTranBasedInv", null, e);
    }
    finally
    {
      TryCleanup.tryCleanup(pfb, pstmt, rs);
    }
    
    return isTranBasedInv;
  }
  
  public static void clearSRItemInvDates(PosFileBean pfb, String conSaId, String pricingCode)
  {
    StringBuilder sb = new StringBuilder();
    sb.append("update sam_con_sls_rcpt_item ");
    sb.append("set invoice_dt = null ");
    sb.append("where logged_uid = 'POS' ");
    sb.append("AND CON_SA_ID = "+conSaId+" ");
    sb.append("and pricing_code = "+pricingCode);

    OraclePreparedStatement pstmt = null;
    try
    {
      pstmt = (OraclePreparedStatement) pfb.getConnection().prepareStatement(sb.toString());
      pstmt.executeUpdate();

    }
    catch (Exception e)
    {
      ExceptionLogger.logException(pfb, "ExecuteUpdates", "clearSRItemInvDates", null, e);
    }
    finally
    {
      TryCleanup.tryCleanup(pfb, pstmt, null);
    }
  }
  
  public static void insertSRDetail(PosFileBean pfb, SalesReceiptBean srb, SrDetailBean srdb)
  {
    if (srdb.getSamConSaId() != null)
    {
      if (pfb.getUpdateSrItemNextId() <= pfb.getUpdateSrItemLastId())
      {
        srdb.setSamSrItemId(String.valueOf(pfb.getUpdateSrItemNextId()));
        pfb.setUpdateSrItemNextId(pfb.getUpdateSrItemNextId() + 1);
        StringBuilder sb = new StringBuilder();
        sb.append("insert into sam_con_sls_rcpt_item");
        sb.append("(sls_rcpt_item_id,sls_rcpt_id,item_id,pricing_code,con_sa_id,delivered_dt,");
        sb.append("qty,price_amt,sku_no,mfg_id,mfg_other,color_style,description,logged_dt,");
        sb.append("logged_uid,status,furniture_id,item_sa_amt,tally_dt,sa_type,pos_add_on_flg,pos_fh_id,serial_no,ack_no,plan_item_id)values");
        sb.append("(?,?,?,?,?,");
        sb.append("to_date(?,'YYMMDD'),?,?,substr(?,1,32),105,substr(?,1,100),substr(?,1,32),");
        sb.append("substr(?,1,250),sysdate,'POS','A',98,?,sysdate,?,'Y',?,substr(?,1,32),substr(?,1,32),substr(?,1,250))");
        OraclePreparedStatement pstmt = null;
        try
        {
          pstmt = (OraclePreparedStatement) pfb.getConnection().prepareStatement(sb.toString());
          pstmt.setString(1, srdb.getSamSrItemId());
          pstmt.setString(2, srb.getSamSrId());
          pstmt.setString(3, srdb.getItemId());
          pstmt.setString(4, srdb.getPricingCode());
          if (srdb.getPricingCode() != null)
          {
            pstmt.setString(5, srdb.getSamConSaId());
          }
          else if (srdb.getPricingCode() == null)
          {
            pstmt.setString(5, srb.getSamEliteConSAId());
          }
          pstmt.setString(6, srdb.getDeliveryDt());
          pstmt.setString(7, srdb.getQty());
          pstmt.setString(8, srdb.getUnitAmt());
          pstmt.setString(9, srdb.getSkuNo());
          pstmt.setString(10, srdb.getManufName());
          pstmt.setString(11, srdb.getItemColorStyle());
          pstmt.setString(12, srdb.getItemDescription());
          pstmt.setString(13, srdb.getItemSaAmt());
          pstmt.setString(14, srdb.getSaType());
          pstmt.setString(15, pfb.getPosFhId());
          pstmt.setString(16, srb.getSerialNo());
          pstmt.setString(17, srb.getSerialNo());  //put serial_no in ack_no column for display in applix
          pstmt.setString(18, srdb.getPlanItemId());         
          pstmt.executeUpdate();
        }
        catch (Exception e)
        {
          ExceptionLogger.logException(pfb, "ExecuteUpdates", "insertSRDetail", "TRANS_ID: " + srb.getTransId() + "ITEM_ID: " + srdb.getItemId(), e);
        }
        finally
        {
          TryCleanup.tryCleanup(pfb, pstmt, null);
        }
      }
      else
      {
        ExceptionLogger.logError("Record ID Not allocated for Sales Receipt Detail", null, "Update SR Detail Insert", pfb, srb, srdb);
      }
    }
    else
    {
      ExceptionLogger.logError("Service Agreement Type not found", " ITEM_ID: " + srdb.getItemId(), "SR Detail Insert", pfb, srb, srdb);
    }
  }

  public static void newDetailNewConSA(PosFileBean pfb)
  {
    int newItemCount = 0;
    Iterator i = pfb.getSalesReceipts().iterator();
    while (i.hasNext())
    {
      SalesReceiptBean srb = (SalesReceiptBean) i.next();
      //if (srb.getErrors().size() == 0 && !srb.isHasSale() && srb.isHasUpdate()) #110510
      //check if transaction in db or if sale is in file #110510
      if (srb.isHasUpdate())
      {
        boolean tranInDB = false;
        tranInDB = tranInDataBase(pfb, srb);
        //if (srb.isHasUpdate()&&(tranInDB || srb.isHasSale()))
        if (tranInDB || srb.isHasSale())
        {
          if (srb.getErrors().size() == 0 && srb.isHasUpdate())
          {
            Iterator j = srb.getSrHeaders().iterator();
            while (j.hasNext())
            {
              SrHeaderBean srhb = (SrHeaderBean) j.next();
              if (srhb.getTransCode().equals("U"))
              {
                Iterator k = srhb.getSrDetails().iterator();
                while (k.hasNext())
                {
                  SrDetailBean srdb = (SrDetailBean) k.next();
                  boolean inFile = false;
                  inFile = detailInDataBase(pfb, srb, srdb);
                  if (!inFile)
                  {
                    newItemCount += 1;
                    srdb.setUpdateAddition(true);
                    lookForConSA(pfb, srb, srdb);
                  }
                }
              }
            }
          }
        } else
        {
          ExceptionLogger.logError("Original sale transaction was not found for an attempted Update", null, "Execute Update", pfb, srb, null);
        }
      }
    }
    fetchUpdateSrItemIDs(pfb, newItemCount);
  }

  public static void lookForConSA(PosFileBean pfb, SalesReceiptBean srb, SrDetailBean srdb)
  {
    boolean conSaFound = false;
    //if (!srb.isHasSale()) //no sale, look in db first
    //{
      conSaFound = conSAInDataBase(pfb, srb, srdb);
      if (!conSaFound) // Add one then already
      {
        fetchNewConSaId(pfb, srb, srdb);
        insertConSA(pfb, srb, srdb);
      }
    //}
  }

  public static void insertConSA(PosFileBean pfb, SalesReceiptBean srb, SrDetailBean srdb)
  {
    boolean conSaFound = false;
    Iterator i = srb.getConSAs().iterator(); //Find this item's Con SA ID
    while (!conSaFound && i.hasNext())
    {
      ConSaBean conSa = (ConSaBean) i.next();
      if (conSa.getCoverageType().equals(srdb.getSaType()))
      {
        conSaFound = true;
        StringBuilder sb = new StringBuilder();
        sb.append("insert into sam_con_sa(con_sa_id,con_id,comp_code, ");
        sb.append("delpur_dt,logged_dt,logged_uid,rtlr_addr_id,sa_no,sa_amt, ");
        sb.append("sa_status,sa_type_id,sls_rcpt_id)values");
        sb.append("(?,?,'USA',to_date(?,'YYMMDD'), ");
        sb.append("sysdate,'POS',?,?,?,'Active',?,?) ");
        OraclePreparedStatement pstmt = null;
        try
        {
          pstmt = (OraclePreparedStatement) pfb.getConnection().prepareStatement(sb.toString());
          pstmt.setString(1, srdb.getSamConSaId());
          pstmt.setString(2, srb.getSamConId());
          pstmt.setString(3,conSa.getDelPurDt());
          pstmt.setString(4, srb.getSamRtlrAddrId());
          pstmt.setString(5, srb.getSaNo());
          if (srdb.getPricingCode() == null)
          {
            pstmt.setString(6, srb.getSaAmt());
          }
          else
          {
            pstmt.setString(6, srdb.getItemSaAmt());
          }
          pstmt.setString(7, conSa.getSaTypeId());
          pstmt.setString(8, srb.getSamSrId());
          pstmt.executeUpdate();
        }
        catch (Exception e)
        {
          ExceptionLogger.logException(pfb, "ExecuteUpdates", "insertConSA", "TRANS_ID: " + srb.getTransId() + "ITEM_ID: " + srdb.getItemId(), e);
        }
        finally
        {
          TryCleanup.tryCleanup(pfb, pstmt, null);
        }
      }
    }
    if (!conSaFound)
    {
      ExceptionLogger.logError("Consumer Service Agreement not found", " ITEM_ID" + srdb.getItemId(), "Update Con SA insert", pfb, srb, srdb);
    }
  }

  public static boolean conSAInDataBase(PosFileBean pfb, SalesReceiptBean srb, SrDetailBean srdb)
  {
    String conSAId = null;
    boolean conSAFound = false;

    StringBuilder sb = new StringBuilder();
    sb.append("select distinct item.con_sa_id ");
    sb.append("from sam_con_sls_rcpt sr ");
    sb.append("inner join sam_con_sls_rcpt_item item ");
    sb.append("  on sr.sls_rcpt_id = item.sls_rcpt_id ");
    sb.append("  and (item.pricing_code is null or ");
    sb.append("       item.pricing_code = :pricing_code) ");
    sb.append("inner join sam_con_sa sa ");
    sb.append("  on sr.sls_rcpt_id = sa.sls_rcpt_id ");
    sb.append("  and (item.con_sa_id = sa.con_sa_id ");
    sb.append("     or item.con_sa_id is null) ");
    sb.append("inner join sam_sa_type st ");
    sb.append("  on sa.sa_type_id = st.sa_type_id ");
    sb.append("  and st.coverage_type = :coverage_type ");
    sb.append("where sr.sls_rcpt_id = :sls_rcpt_id ");

    OraclePreparedStatement pstmt = null;
    OracleResultSet rs = null;
    try
    {
      pstmt = (OraclePreparedStatement) pfb.getConnection().prepareStatement(sb.toString());
      pstmt.setStringAtName("sls_rcpt_id", srb.getSamSrId());
      if (srdb.getSaType().equals("W"))
      {
        pstmt.setStringAtName("coverage_type", "C");
      }
      else
      {
        pstmt.setStringAtName("coverage_type", srdb.getSaType());
      }
      pstmt.setStringAtName("pricing_code", srdb.getPricingCode());

      rs = (OracleResultSet) pstmt.executeQuery();
      while (rs.next())
      {
        conSAId = rs.getString(1);
      }
    }
    catch (Exception e)
    {
      ExceptionLogger.logException(pfb, "ExecuteUpdates", "conSAInDataBase", "TRANS_ID: " + srb.getTransId() + "ITEM_ID: " + srdb.getItemId(), e);
    }
    finally
    {
      TryCleanup.tryCleanup(pfb, pstmt, rs);
    }
    if (conSAId != null)
    {
      conSAFound = true;
      if (srdb.getPricingCode() == null) //(srb.getSamEliteConSAId() == null)
      {
        srb.setSamEliteConSAId(conSAId);
      }
      srdb.setSamConSaId(conSAId);
    }
    return conSAFound;
  }

  public static boolean detailInDataBase(PosFileBean pfb, SalesReceiptBean srb, SrDetailBean updateDetail)
  {
    boolean inFile = false;
    int cnt = 0;
    StringBuilder sb = new StringBuilder();
    sb.append("select count(sam_con_sls_rcpt_item.sls_rcpt_item_id) ");
    sb.append("from sam_con_sls_rcpt_item,sam_con_sls_rcpt ");
    sb.append("where ");
    sb.append("sam_con_sls_rcpt_item.item_id = ?  ");
    sb.append("and sam_con_sls_rcpt_item.sls_rcpt_id = sam_con_sls_rcpt.sls_rcpt_id ");
    sb.append("and sam_con_sls_rcpt.trans_id = ? ");
    sb.append("and sam_con_sls_rcpt.rtlr_addr_id = ? ");
    sb.append("and sam_con_sls_rcpt_item.status <> 'R' ");
    OraclePreparedStatement pstmt = null;
    OracleResultSet rs = null;
    try
    {
      pstmt = (OraclePreparedStatement) pfb.getConnection().prepareStatement(sb.toString());
      pstmt.setString(1, updateDetail.getItemId());
      pstmt.setString(2, srb.getTransId());
      pstmt.setString(3, srb.getSamRtlrAddrId());
      rs = (OracleResultSet) pstmt.executeQuery();
      while (rs.next())
      {
        cnt = rs.getInt(1);
      }
    }
    catch (Exception e)
    {
      ExceptionLogger.logException(pfb, "ExecuteUpdates", "detailInDataBase", "TRANS_ID: " + srb.getTransId() + "ITEM_ID: " + updateDetail.getItemId(), e);
    }
    finally
    {
      TryCleanup.tryCleanup(pfb, pstmt, rs);
    }
    if (cnt == 1)
    {
      inFile = true;
    }
    else if (cnt > 1)
    {
      ExceptionLogger.logError("No unique match for Update Detail", " ITEM_ID: " + updateDetail.getItemId(), "Update Item Check", pfb, srb, updateDetail);
    }
    return inFile;
  }

  public static boolean tranInDataBase(PosFileBean pfb, SalesReceiptBean srb)
  {
    boolean tranInDB = false;
    int cnt = 0;
    StringBuilder sb = new StringBuilder();
    sb.append("select count(sam_con_sls_rcpt.sls_rcpt_id) ");
    sb.append("from sam_con_sls_rcpt ");
    sb.append("where sam_con_sls_rcpt.trans_id = ? ");
    sb.append("and sam_con_sls_rcpt.rtlr_addr_id = ? ");
    OraclePreparedStatement pstmt = null;
    OracleResultSet rs = null;
    try
    {
      pstmt = (OraclePreparedStatement) pfb.getConnection().prepareStatement(sb.toString());
      pstmt.setString(1, srb.getTransId());
      pstmt.setString(2, srb.getSamRtlrAddrId());
      rs = (OracleResultSet) pstmt.executeQuery();
      while (rs.next())
      {
        cnt = rs.getInt(1);
      }
    }
    catch (Exception e)
    {
      ExceptionLogger.logException(pfb, "ExecuteUpdates", "tranInDataBase", "TRANS_ID: " + srb.getTransId(), e);
    }
    finally
    {
      TryCleanup.tryCleanup(pfb, pstmt, rs);
    }
    if (cnt >= 1)
    {
      tranInDB = true;
    }
    return tranInDB;
  }
  public static void fetchUpdateSrItemIDs(PosFileBean pfb, int numOfRecords)
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
        pfb.setUpdateSrItemNextId(lastId + 1);
        pfb.setUpdateSrItemLastId(endId);
      }
      else
      {
        ExceptionLogger.logError("Failed to get Counter for SR Item", null, "SR Counter Fetch", pfb, null, null);
      }
    }
    catch (Exception e)
    {
      ExceptionLogger.logException(pfb, "ExecuteUpdates", "fetchUpdateSrItemIDs", null, e);
    }
    finally
    {
      TryCleanup.tryCleanup(pfb, pstmt, rs);
      TryCleanup.tryCleanup(pfb, pstmtUpdate, null);
    }
  }

  public static void fetchNewConSaId(PosFileBean pfb, SalesReceiptBean srb, SrDetailBean srdb)
  {
    int lastOne = 0;
    int counterFound = 0;
    StringBuilder sb = new StringBuilder();
    sb.append("select last_one from counter where item = 'sam_con_sa.con_sa_id' ");
    StringBuilder updateSb = new StringBuilder();
    updateSb.append("update counter set last_one = ? where item = 'sam_con_sa.con_sa_id' ");
    OraclePreparedStatement pstmt = null;
    OraclePreparedStatement pstmtUpdate = null;
    OracleResultSet rs = null;
    try
    {
      pstmt = (OraclePreparedStatement) pfb.getConnection().prepareStatement(sb.toString());
      pstmtUpdate = (OraclePreparedStatement) pfb.getConnection().prepareStatement(updateSb.toString());
      rs = (OracleResultSet) pstmt.executeQuery();
      while (rs.next())
      {
        lastOne = rs.getInt(1);
        counterFound += 1;
      }
      if (counterFound == 1)
      {
        lastOne += 1;
        pstmtUpdate.setInt(1, lastOne);
        pstmtUpdate.executeUpdate();
        if (srdb.getPricingCode() == null && srb.getSamEliteConSAId() == null)
        {
          srb.setSamEliteConSAId(String.valueOf(lastOne));
        }
        srdb.setSamConSaId(String.valueOf(lastOne));
      }
      else
      {
        ExceptionLogger.logError("Failed to get ID for Consumer SA", null, "Consumer SA Counter Fetch", pfb, srb, null);
      }
    }
    catch (Exception e)
    {
      ExceptionLogger.logException(pfb, "ExecuteUpdates", "fetchNewConSaId", "TRANS_ID: " + srb.getTransId() + "ITEM_ID: " + srdb.getItemId(), e);
    }
    finally
    {
      TryCleanup.tryCleanup(pfb, pstmt, rs);
      TryCleanup.tryCleanup(pfb, pstmtUpdate, null);
    }
  }

  public static void updateExistingSrItem(PosFileBean pfb, SalesReceiptBean srb, SrDetailBean srdb)
  {
    StringBuilder sb = new StringBuilder();
    sb.append("update ");
    sb.append("sam_con_sls_rcpt_item ");
    sb.append("set sam_con_sls_rcpt_item.delivered_dt = to_date(?,'YYMMDD'), ");
    sb.append("sam_con_sls_rcpt_item.item_sa_amt = ?, ");
    sb.append("sam_con_sls_rcpt_item.change_dt = sysdate, ");
    sb.append("sam_con_sls_rcpt_item.change_uid = 'POS' ");
    sb.append("where sam_con_sls_rcpt_item.item_id = ? ");
    sb.append("and sam_con_sls_rcpt_item.sls_rcpt_id = ");
    sb.append("(select sam_con_sls_rcpt.sls_rcpt_id from sam_con_sls_rcpt ");
    sb.append("where sam_con_sls_rcpt.rtlr_addr_id = ? ");
    sb.append("and sam_con_sls_rcpt.trans_id = ? ");
    sb.append("and sam_con_sls_rcpt.sls_rcpt_id = ?) ");
    OraclePreparedStatement pstmt = null;
    try
    {
      pstmt = (OraclePreparedStatement) pfb.getConnection().prepareStatement(sb.toString());
      pstmt.setString(1, srdb.getDeliveryDt());
      pstmt.setString(2, srdb.getItemSaAmt());
      pstmt.setString(3, srdb.getItemId());
      pstmt.setString(4, srb.getSamRtlrAddrId());
      pstmt.setString(5, srb.getTransId());
      pstmt.setString(6, srb.getSamSrId());
      pstmt.executeUpdate();
    }
    catch (Exception e)
    {
      ExceptionLogger.logException(pfb, "ExecuteUpdates", "updateExistingSrItem", "TRANS_ID: " + srb.getTransId() + "ITEM_ID: " + srdb.getItemId(), e);
    }
    finally
    {
      TryCleanup.tryCleanup(pfb, pstmt, null);
    }
  }

  public static void pricingMethodUpdate(PosFileBean pfb, SalesReceiptBean srb)
  {
    int cnt = 0;
    int pricingMethod = 0;
    if (srb.getPricingMethod() != null)
    {
      StringBuilder sb = new StringBuilder();
      sb.append("select sam_con_sls_rcpt.pricing_method ");
      sb.append("from sam_con_sls_rcpt ");
      sb.append("where sam_con_sls_rcpt.sls_rcpt_id = ? ");
      OraclePreparedStatement pstmt = null;
      OracleResultSet rs = null;
      try
      {
        pstmt = (OraclePreparedStatement) pfb.getConnection().prepareStatement(sb.toString());
        pstmt.setString(1, srb.getSamSrId());
        rs = (OracleResultSet) pstmt.executeQuery();
        while (rs.next())
        {
          cnt = +1;
          pricingMethod = rs.getInt(1);
        }
      }
      catch (Exception e)
      {
        ExceptionLogger.logException(pfb, "ExecuteUpdates", "pricingMethodUpdate Q1", "TRANS_ID: " + srb.getTransId(), e);
      }
      finally
      {
        TryCleanup.tryCleanup(pfb, pstmt, rs);
      }
      if (cnt == 0) // Originating Sale not found!!
      {
        ExceptionLogger.logError("Original sale transaction was not found for an attempted Update", null, "Sales Receipt Update", pfb, srb, null);
      }
    }
    if (pricingMethod > 0 || (!srb.isHasSale() && srb.getPricingMethod() != null))
    {
      StringBuilder sbUpdate = new StringBuilder();
      sbUpdate.append("update ");
      sbUpdate.append("sam_con_sls_rcpt ");
      sbUpdate.append("set sam_con_sls_rcpt.pricing_method = ?, ");
      sbUpdate.append("sam_con_sls_rcpt.change_dt = sysdate, ");
      sbUpdate.append("sam_con_sls_rcpt.change_uid = 'POS' ");
      sbUpdate.append("where sam_con_sls_rcpt.rtlr_addr_id = ? ");
      sbUpdate.append("and sam_con_sls_rcpt.trans_id = ? ");
      sbUpdate.append("and sam_con_sls_rcpt.sls_rcpt_id = ? ");
      OraclePreparedStatement pstmtUpdate = null;
      try
      {
        pstmtUpdate = (OraclePreparedStatement) pfb.getConnection().prepareStatement(sbUpdate.toString());
        pstmtUpdate.setString(1, srb.getPricingMethod());
        pstmtUpdate.setString(2, srb.getSamRtlrAddrId());
        pstmtUpdate.setString(3, srb.getTransId());
        pstmtUpdate.setString(4, srb.getSamSrId());
        pstmtUpdate.executeUpdate();
      }
      catch (Exception e)
      {
        ExceptionLogger.logException(pfb, "ExecuteUpdates", "pricingMethodUpdate Q2", "TRANS_ID: " + srb.getTransId(), e);
      }
      finally
      {
        TryCleanup.tryCleanup(pfb, pstmtUpdate, null);
      }
    }
  }
}
