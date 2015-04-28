package com.valspar.interfaces.guardsman.pos.tab18;

import com.valspar.interfaces.guardsman.pos.beans.*;
import com.valspar.interfaces.guardsman.pos.utility.*;
import java.util.*;
import oracle.jdbc.*;

public class ExecuteReturnsTab18
{
  public ExecuteReturnsTab18(PosFileBean pfb)
  {
    Iterator i = pfb.getSalesReceipts().iterator();
    while (i.hasNext())
    {
      SalesReceiptBean srb = (SalesReceiptBean) i.next();
      if (srb.getErrors().size() == 0 && srb.isHasReturn())
      {
        boolean conSAFound = false;
        ConSaBean conSa = new ConSaBean();
        Iterator j = srb.getConSAs().iterator();
        while (!conSAFound && j.hasNext()) //There will only be one ConSA
        {
          conSa = (ConSaBean) j.next();
          conSAFound = true;
        }
        findConSaIdForReturn(pfb, srb, conSa);
      }
    }
  }

  public static void findConSaIdForReturn(PosFileBean pfb, SalesReceiptBean srb, ConSaBean conSa)
  {
    if (srb.getSamEliteConSAId() == null)
    {
      /*
       * If the originating sale was in this same file, it would have been
       * inserted into the database during the ExecuteSalesTab18.
       * This means that the initial check during
       * DbValidationTab18.verifyUniqueTrans hasn't yet found the sale.
       * So we need to look once more.
       */
      boolean inFile = false;
      Iterator i = pfb.getSalesReceipts().iterator();
      while (!inFile && i.hasNext())
      {
        SalesReceiptBean tempSrb = (SalesReceiptBean) i.next();
        if (tempSrb.getErrors().size() == 0 && tempSrb.isHasSale() && tempSrb.getTransId().equals(srb.getTransId()) && tempSrb.getTab18SAType().equals(srb.getTab18SAType()))
        {
          conSa.setSamConSAId(tempSrb.getSamEliteConSAId());
          srb.setSamEliteConSAId(tempSrb.getSamEliteConSAId());
          inFile = true;
        }
      }
    }
    // Should have the ConSa at this point.
    /*if (srb.getSamEliteConSAId() != null)*/
    if (srb.getSamEliteConSAId() != null && !srb.isHasClaim())
    {
      adjustConSA(pfb, srb, conSa);
    }
    else if(srb.isHasClaim())
    {
      ExceptionLogger.logError("Request logged against originating sale, return not allowed.", " SA_TYPE: " + srb.getTab18SAType() + " PRICING_CODE: " + srb.getPricingMethod() + " QTY: " + srb.getTab18ItemQty(), "Return Item Exist Check", pfb, srb, null);      
    }
    else
    {
      ExceptionLogger.logError("Originating Sale not found.", " SA_TYPE: " + srb.getTab18SAType() + " PRICING_CODE: " + srb.getPricingMethod() + " QTY: " + srb.getTab18ItemQty(), "Return Item Exist Check", pfb, srb, null);
    }
  }

  public static void adjustConSA(PosFileBean pfb, SalesReceiptBean srb, ConSaBean conSa)
  {
    String pricingType = (String) pfb.getPricingCodeMap().get(srb.getPricingMethod());
    if (pricingType.equals("Sale"))
    {
      updateHeaderLevelConSa(pfb, srb, conSa);
      updateHeaderLevelRtrnQty(pfb, srb, conSa);
    }
    else
    {
      updateItemLevelConSa(pfb, srb, conSa);
      updateConSaStatus(pfb, srb, conSa);
    }
  }

  public static void updateHeaderLevelConSa(PosFileBean pfb, SalesReceiptBean srb, ConSaBean conSa)
  {
    StringBuilder sb = new StringBuilder();
    sb.append("update sam_con_sa ");
    sb.append("set pos_item_qty = nvl(pos_item_qty,0) - ?, ");
    sb.append("sa_amt = nvl(sa_amt,0) - ?, ");
    sb.append("change_dt = sysdate, ");
    sb.append("change_uid = 'POS' ");
    sb.append("where con_sa_id = ?");
    OraclePreparedStatement pstmt = null;
    try
    {
      pstmt = (OraclePreparedStatement) pfb.getConnection().prepareStatement(sb.toString());
      pstmt.setString(1, srb.getTab18ItemQty());
      pstmt.setString(2, conSa.getSaAmt());
      pstmt.setString(3, srb.getSamEliteConSAId());
      pstmt.executeUpdate();
    }
    catch (Exception e)
    {
      ExceptionLogger.logException(pfb, "ExecuteReturnsTab18", "updateItemLevelConSa", "TRANS_ID: " + srb.getTransId(), e);
    }
    finally
    {
      TryCleanup.tryCleanup(pfb, pstmt, null);
    }
  }

  public static void updateHeaderLevelRtrnQty(PosFileBean pfb, SalesReceiptBean srb, ConSaBean conSa)
  {
    StringBuilder sb = new StringBuilder();
    sb.append("update sam_con_sa ");
    sb.append("set rtrn_qty_to_inv = 1, ");
    sb.append("sa_status = 'R' ");
    sb.append("where con_sa_id = ? ");
    sb.append("and pos_item_qty = 0 ");
    OraclePreparedStatement pstmt = null;
    try
    {
      pstmt = (OraclePreparedStatement) pfb.getConnection().prepareStatement(sb.toString());
      pstmt.setString(1, srb.getSamEliteConSAId());
      pstmt.executeUpdate();
    }
    catch (Exception e)
    {
      ExceptionLogger.logException(pfb, "ExecuteReturnsTab18", "updateHeaderLevelRtrnQty", "TRANS_ID: " + srb.getTransId(), e);
    }
    finally
    {
      TryCleanup.tryCleanup(pfb, pstmt, null);
    }
  }

  public static void updateItemLevelConSa(PosFileBean pfb, SalesReceiptBean srb, ConSaBean conSa)
  {
    StringBuilder sb = new StringBuilder();
    sb.append("update sam_con_sa ");
    sb.append("set pos_item_qty = nvl(pos_item_qty,0) - ?, ");
    sb.append("rtrn_qty_to_inv = nvl(rtrn_qty_to_inv,0) + ?, ");
    sb.append("sa_amt = nvl(sa_amt,0) - ?, ");
    sb.append("change_dt = sysdate, ");
    sb.append("change_uid = 'POS' ");
    sb.append("where con_sa_id = ?");
    OraclePreparedStatement pstmt = null;
    try
    {
      pstmt = (OraclePreparedStatement) pfb.getConnection().prepareStatement(sb.toString());
      pstmt.setString(1, srb.getTab18ItemQty());
      pstmt.setString(2, srb.getTab18ItemQty());
      pstmt.setString(3, conSa.getSaAmt());
      pstmt.setString(4, srb.getSamEliteConSAId());
      pstmt.executeUpdate();
    }
    catch (Exception e)
    {
      ExceptionLogger.logException(pfb, "ExecuteReturnsTab18", "updateItemLevelConSa", "TRANS_ID: " + srb.getTransId(), e);
    }
    finally
    {
      TryCleanup.tryCleanup(pfb, pstmt, null);
    }
  }

  public static void updateConSaStatus(PosFileBean pfb, SalesReceiptBean srb, ConSaBean conSa)
  {
    StringBuilder sb = new StringBuilder();
    sb.append("update sam_con_sa ");
    sb.append("set sa_status = 'R' ");
    sb.append("where con_sa_id = ? ");
    sb.append("and pos_item_qty = 0 ");
    OraclePreparedStatement pstmt = null;
    try
    {
      pstmt = (OraclePreparedStatement) pfb.getConnection().prepareStatement(sb.toString());
      pstmt.setString(1, srb.getSamEliteConSAId());
      pstmt.executeUpdate();
    }
    catch (Exception e)
    {
      ExceptionLogger.logException(pfb, "ExecuteReturnsTab18", "updateConSaStatus", "TRANS_ID: " + srb.getTransId(), e);
    }
    finally
    {
      TryCleanup.tryCleanup(pfb, pstmt, null);
    }
  }
}
