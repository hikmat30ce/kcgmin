package com.valspar.interfaces.guardsman.pos.tab18;

import com.valspar.interfaces.guardsman.pos.beans.*;
import com.valspar.interfaces.guardsman.pos.utility.*;
import java.util.*;
import oracle.jdbc.*;

public class ExecuteUpdatesTab18 
{
  /*
   * ONLY Two things can be updated. The SA_AMT and Qty
   */  

  public ExecuteUpdatesTab18(PosFileBean pfb)
  {
    Iterator i = pfb.getSalesReceipts().iterator();
    while (i.hasNext())
    {
      SalesReceiptBean srb = (SalesReceiptBean)i.next();      
      if (srb.getErrors().size() == 0 && srb.isHasUpdate())
      {
        ConSaBean conSa = new ConSaBean();
        Iterator j = srb.getConSAs().iterator(); //Item Con SA's
        while (j.hasNext() && srb.getErrors().size() == 0)
        {
          conSa = (ConSaBean)j.next();
        }
        findConSAForUpdate(pfb,srb,conSa);
      }
    }
  }
  
  public static void findConSAForUpdate(PosFileBean pfb, SalesReceiptBean srb,ConSaBean conSa)
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
        SalesReceiptBean tempSrb = (SalesReceiptBean)i.next();      
        if (tempSrb.getErrors().size() == 0 
            && tempSrb.isHasSale() 
            && tempSrb.getTransId().equals(srb.getTransId()) 
            && tempSrb.getTab18SAType().equals(srb.getTab18SAType()))
        {
          conSa.setSamConSAId(tempSrb.getSamEliteConSAId());
          srb.setSamEliteConSAId(tempSrb.getSamEliteConSAId());
          inFile = true;
        }
      }
    }
    // Should have the ConSa at this point.
    if (srb.getSamEliteConSAId() != null)
    {
      conSAUpdate(pfb,srb);
    }
    else
    {   
      ExceptionLogger.logError("Originating Sale not found."," TRANS_ID: " + srb.getTransId()
      + " SA_TYPE: " + srb.getTab18SAType(),"Validate Consumer Service Agreement",pfb,srb,null);
    }
  }
  
  public static void conSAUpdate(PosFileBean pfb, SalesReceiptBean srb)
  {
    String pricingType = (String)pfb.getPricingCodeMap().get(srb.getPricingMethod());
    StringBuilder sb = new StringBuilder();
    sb.append("update sam_con_sa set sa_amt = nvl(sa_amt,0) + ?, ");
    sb.append("pos_item_qty = nvl(pos_item_qty,0) + ?, ");
    sb.append("sls_qty_to_inv = nvl(sls_qty_to_inv,0) + ?, ");    
    sb.append("change_dt = sysdate, change_uid = 'POS' where con_sa_id = ?");
    OraclePreparedStatement pstmt = null;
    try
    {
      pstmt = (OraclePreparedStatement)pfb.getConnection().prepareStatement(sb.toString());
      pstmt.setString(1, srb.getSaAmt());
      pstmt.setString(2, srb.getTab18ItemQty());
      if (pricingType.equals("Sale"))
      { //Header based plans do not update the sales quantity to invoice
        pstmt.setString(3, "0");  
      }
      else
      {
        pstmt.setString(3, srb.getTab18ItemQty());
      }
      pstmt.setString(4, srb.getSamEliteConSAId());  
      pstmt.executeUpdate();
    }
    catch (Exception e)
    {
      ExceptionLogger.logException(pfb, "ExecuteUpdatesTab18","ConSAUpdate","Con SA Id: " + srb.getSamSrId(),e);
    }
    finally
    {
      TryCleanup.tryCleanup(pfb,pstmt,null);
    }
  }
}