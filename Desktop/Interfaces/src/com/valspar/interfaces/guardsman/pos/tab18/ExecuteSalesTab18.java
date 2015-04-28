package com.valspar.interfaces.guardsman.pos.tab18;

import com.valspar.interfaces.guardsman.pos.*;
import com.valspar.interfaces.guardsman.pos.beans.*;
import com.valspar.interfaces.guardsman.pos.utility.*;
import java.util.*;
import oracle.jdbc.*;
import org.apache.log4j.Logger;

public class ExecuteSalesTab18 extends ExecuteSales
{
  static Logger log4jLogger = Logger.getLogger(ExecuteSalesTab18.class.getName());

  public ExecuteSalesTab18(PosFileBean pfb)
  {
    new ReserveIDsForSales(pfb);
    int srbCounter = 0;
    Iterator i = pfb.getSalesReceipts().iterator();
    while (i.hasNext())
    {
      SalesReceiptBean srb = (SalesReceiptBean)i.next();
      
      if (srb.isHasSale() && srb.getErrors().size() == 0) 
      {
        consumerSetup(pfb,srb);
      }
      if (srb.isHasSale() && srb.getSamSrId() == null && srb.getErrors().size() == 0)
      {      
        Iterator m = srb.getConSAs().iterator(); //Item Con SA's
        while (m.hasNext() && srb.getErrors().size() == 0)
        {
          ConSaBean conSa = (ConSaBean)m.next();
          insertConSA(pfb,srb,conSa);
        }
      }
      srbCounter = srbCounter +1;
      if(srbCounter % 1000 == 0)
      {
        log4jLogger.info("Sale Transaction: " + srbCounter);
      }
    }
    log4jLogger.info("Sale Transaction: " + srbCounter);
  }
  public static void insertConSA(PosFileBean pfb, SalesReceiptBean srb, ConSaBean conSa)
  {
    String pricingType = (String)pfb.getPricingCodeMap().get(srb.getPricingMethod());
    conSa.setSamConSAId(String.valueOf(pfb.getConSANextId()));
    srb.setSamEliteConSAId(conSa.getSamConSAId());
    pfb.setConSANextId(pfb.getConSANextId() + 1);
    StringBuilder sb = new StringBuilder();
    sb.append("insert into sam_con_sa(con_sa_id,con_id,comp_code, ");
    sb.append("delpur_dt,logged_dt,logged_uid,rtlr_addr_id,sa_no,sa_amt, ");
    sb.append("sa_status,sa_type_id,pos_item_qty,pos_trans_id,sls_qty_to_inv,pricing_method,pos_fh_id)values");
    sb.append("(?,?,'USA',to_date(?,'YYMMDD'), ");
    sb.append("sysdate,'POS',?,?,?,'Active',?,?,?,?,?,?) ");   
    OraclePreparedStatement pstmt = null;
    OracleResultSet rs = null;      
    try
    {
      pstmt = (OraclePreparedStatement)pfb.getConnection().prepareStatement(sb.toString());
      pstmt.setString(1, conSa.getSamConSAId());
      pstmt.setString(2, srb.getSamConId());
      pstmt.setString(3,srb.getSaleDt());
      pstmt.setString(4, srb.getSamRtlrAddrId());
      pstmt.setString(5, srb.getSaNo());
      pstmt.setString(6, conSa.getSaAmt());
      pstmt.setString(7, conSa.getSaTypeId());
      pstmt.setString(8, srb.getTab18ItemQty());
      pstmt.setString(9, srb.getTransId());      
      if (pricingType.equals("Sale"))
      { //Header based plans only count as one.
        pstmt.setString(10, "1");  
      }
      else
      {
        pstmt.setString(10, srb.getTab18ItemQty());
      }
      pstmt.setString(11, srb.getPricingMethod());
      pstmt.setString(12, pfb.getPosFhId());
      pstmt.executeUpdate();   
    }
    catch(Exception e)
    {
      ExceptionLogger.logException(pfb, "ExecuteSalesTab18","insertConSA","Trans: " + srb.getTransId(),e);
    }    
    finally
    {
      TryCleanup.tryCleanup(pfb,pstmt,rs);
    }    
  }   
}