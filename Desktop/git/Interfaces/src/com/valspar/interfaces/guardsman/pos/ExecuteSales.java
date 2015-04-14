package com.valspar.interfaces.guardsman.pos;

import java.util.*;
import oracle.jdbc.*;
import com.valspar.interfaces.guardsman.pos.beans.*;
import com.valspar.interfaces.guardsman.pos.utility.*;
import org.apache.log4j.Logger;

public class ExecuteSales
{
  static Logger log4jLogger = Logger.getLogger(ExecuteSales.class.getName());

  public ExecuteSales()
  {
  }

  public ExecuteSales(PosFileBean pfb)
  {
    new ReserveIDsForSales(pfb);
    saleInsert(pfb);
  }

  public static void saleInsert(PosFileBean pfb)
  {
    int srbCounter = 0;
    Iterator i = pfb.getSalesReceipts().iterator();
    while (i.hasNext())
    {
      SalesReceiptBean srb = (SalesReceiptBean) i.next();

      if (srb.isHasSale() && srb.getErrors().size() == 0)
      {
        consumerSetup(pfb, srb);
      }
      if (srb.isHasSale() && srb.getSamSrId() == null && srb.getErrors().size() == 0)
      {
        insertSR(pfb, srb); // SR Header        
        Iterator m = srb.getConSAs().iterator(); //Item Con SA's
        while (m.hasNext() && srb.getErrors().size() == 0)
        {
          ConSaBean conSa = (ConSaBean) m.next();
          insertConSA(pfb, srb, conSa);
        }
        Iterator j = srb.getSrHeaders().iterator();
        while (j.hasNext())
        {
          SrHeaderBean srhb = (SrHeaderBean) j.next();
          //if (srhb.getTransCode().equals("S") || srhb.getTransCode().equals("U")) #110510
          if (srhb.getTransCode().equals("S"))
          {
            Iterator k = srhb.getSrDetails().iterator();
            while (k.hasNext() && srb.getErrors().size() == 0)
            {
              SrDetailBean srdb = (SrDetailBean) k.next();
              insertSRDetail(pfb, srb, srdb);
            }
          }
        }
      }
      srbCounter = srbCounter + 1;
      if (srbCounter % 1000 == 0)
      {
        log4jLogger.info("Sale Transaction: " + srbCounter);
      }
    }
    log4jLogger.info("Sale Transaction: " + srbCounter);
  }

  public static void consumerSetup(PosFileBean pfb, SalesReceiptBean srb)
  {
    consumerMatchOrInsert(pfb, srb);
    if (srb.getSamConId() == null)
    {
      insertCon(pfb, srb);
      insertConAddr(pfb, srb);
      if (srb.getPhoneHome() != null && srb.getErrors().size() == 0)
      {
        insertConHomePhone(pfb, srb);
      }
      if (srb.getPhoneWork() != null && srb.getErrors().size() == 0)
      {
        insertConWorkPhone(pfb, srb);
      }
    } else
    {
      updateConAddr(pfb, srb);
    }
  }

  public static void consumerMatchOrInsert(PosFileBean pfb, SalesReceiptBean srb)
  {
    StringBuilder sb = new StringBuilder();
    sb.append("select distinct(sam_con.con_id) ");
    sb.append("from sam_con,sam_con_phone ");
    sb.append("where ");
    sb.append("UPPER(sam_con.last_name)  =  UPPER(?) ");
    sb.append("and UPPER(sam_con.first_name)  =  UPPER(?) ");
    sb.append("and sam_con.con_id = sam_con_phone.con_id ");
    sb.append("and (sam_con_phone.phone = ? or sam_con_phone.phone = ?) ");
    sb.append("and sam_con_phone.phone <> '0000000000' ");

    int matchCount = 0;
    String tempConId = new String();
    OraclePreparedStatement pstmt = null;
    OracleResultSet rs = null;
    try
    {
      pstmt = (OraclePreparedStatement) pfb.getConnection().prepareStatement(sb.toString());
      pstmt.setString(1, srb.getLastName());
      pstmt.setString(2, srb.getFirstName());
      pstmt.setString(3, srb.getPhoneHome());
      pstmt.setString(4, srb.getPhoneWork());
      rs = (OracleResultSet) pstmt.executeQuery();
      while (rs.next())
      {
        tempConId = rs.getString(1);
        matchCount = matchCount + 1;
      }
    }
    catch (Exception e)
    {
      ExceptionLogger.logException(pfb, "ExecuteSales", "consumerMatchOrInsert", "TRANS_ID: " + srb.getTransId(), e);
    }
    finally
    {
      TryCleanup.tryCleanup(pfb, pstmt, rs);
    }
    if (tempConId != null && (matchCount == 1))
    {
      srb.setSamConId(tempConId);
    }
  }

  public static void insertCon(PosFileBean pfb, SalesReceiptBean srb)
  {
    String langCode = "1";
    if ("ESP".equalsIgnoreCase(srb.getLanguage()))
    {
      langCode = "2";
    } else if ("FRN".equalsIgnoreCase(srb.getLanguage()))
    {
      langCode = "3";
    }
    
    srb.setSamConId(String.valueOf(pfb.getConNextId()));
    pfb.setConNextId(pfb.getConNextId() + 1);
    StringBuilder sb = new StringBuilder();
    sb.append("insert into sam_con ");
    sb.append("(con_id,comp_code,first_name,last_name,logged_dt,logged_uid,status,language_id,email) ");
    sb.append("values ");
    sb.append("(?,'USA',?,?,sysdate,'POS','A',?,?)");
    OraclePreparedStatement pstmt = null;
    OracleResultSet rs = null;
    try
    {
      pstmt = (OraclePreparedStatement) pfb.getConnection().prepareStatement(sb.toString());
      pstmt.setString(1, srb.getSamConId());
      pstmt.setString(2, srb.getFirstName());
      pstmt.setString(3, srb.getLastName());
      pstmt.setString(4,langCode);
      pstmt.setString(5, srb.getEmail());
      pstmt.executeUpdate();
    }
    catch (Exception e)
    {
      ExceptionLogger.logException(pfb, "ExecuteSales", "insertCon", "Trans: " + srb.getTransId(), e);
    }
    finally
    {
      TryCleanup.tryCleanup(pfb, pstmt, rs);
    }
  }

  public static void insertConAddr(PosFileBean pfb, SalesReceiptBean srb)
  {
    srb.setSamAddrId(String.valueOf(pfb.getConAddrNextId()));
    pfb.setConAddrNextId(pfb.getConAddrNextId() + 1);
    StringBuilder sb = new StringBuilder();
    sb.append("insert into sam_con_addr ");
    sb.append("(con_addr_id,con_id,address1,address2,city,state_id,postal_code,country, ");
    sb.append("status,type,logged_dt,logged_uid) ");
    sb.append("values ");
    sb.append("(?,?,?,?,?,?,?,?,");
    sb.append("'Active','Home',sysdate,'POS')");
    OraclePreparedStatement pstmt = null;
    OracleResultSet rs = null;
    try
    {
      pstmt = (OraclePreparedStatement) pfb.getConnection().prepareStatement(sb.toString());
      pstmt.setString(1, srb.getSamAddrId());
      pstmt.setString(2, srb.getSamConId());
      pstmt.setString(3, srb.getAddress1());
      pstmt.setString(4, srb.getAddress2());
      pstmt.setString(5, srb.getCity());
      pstmt.setString(6, (String) pfb.getStateIdMap().get(srb.getState()));
      pstmt.setString(7, srb.getPostalCode());
      pstmt.setString(8, srb.getRtlrCountry());
      pstmt.executeUpdate();
    }
    catch (Exception e)
    {
      ExceptionLogger.logException(pfb, "ExecuteSales", "insertConAddr", "Trans: " + srb.getTransId(), e);
    }
    finally
    {
      TryCleanup.tryCleanup(pfb, pstmt, rs);
    }
  }
  
  public static void updateConAddr(PosFileBean pfb, SalesReceiptBean srb)
  {
    lookupConAddrIdHome(pfb,srb);
    
    if (srb.getSamAddrId() != null)
    {
      StringBuilder sb = new StringBuilder();
      sb.append("update sam_con_addr ");
      sb.append("set address1 = ?,address2 = ?,city = ?,state_id = ?,postal_code = ?,country = ?, ");
      sb.append("change_dt = sysdate,change_uid = 'POS' ");
      sb.append("where sam_con_addr.con_addr_id = ? ");
      OraclePreparedStatement pstmt = null;
      OracleResultSet rs = null;
      try
      {
        pstmt = (OraclePreparedStatement) pfb.getConnection().prepareStatement(sb.toString());
        pstmt.setString(1, srb.getAddress1());
        pstmt.setString(2, srb.getAddress2());
        pstmt.setString(3, srb.getCity());
        pstmt.setString(4, (String) pfb.getStateIdMap().get(srb.getState()));
        pstmt.setString(5, srb.getPostalCode());
        pstmt.setString(6, srb.getRtlrCountry());
        pstmt.setString(7, srb.getSamAddrId());
        pstmt.executeUpdate();
      }
      catch (Exception e)
      {
        ExceptionLogger.logException(pfb, "ExecuteSales", "updateConAddr", "Trans: " + srb.getTransId(), e);
      }
      finally
      {
        TryCleanup.tryCleanup(pfb, pstmt, rs);
      }
    }
  }

  public static void lookupConAddrIdHome(PosFileBean pfb, SalesReceiptBean srb)
  {
    StringBuilder sb = new StringBuilder();
    sb.append("select sam_con_addr.con_addr_id ");
    sb.append("from sam_con_addr ");
    sb.append("where ");
    sb.append("sam_con_addr.con_id = ? ");
    sb.append("and sam_con_addr.type  =  'Home' ");
    sb.append("and sam_con_addr.status = 'Active' ");
    
    String conAddrIdHome = new String();
    OraclePreparedStatement pstmt = null;
    OracleResultSet rs = null;
    try
    {
      pstmt = (OraclePreparedStatement) pfb.getConnection().prepareStatement(sb.toString());
      pstmt.setString(1, srb.getSamConId());
      rs = (OracleResultSet) pstmt.executeQuery();
      if (rs.next())
      {
        conAddrIdHome = rs.getString(1);
      }
    }
    catch (Exception e)
    {
      ExceptionLogger.logException(pfb, "ExecuteSales", "getConAddrIdHome", "TRANS_ID: " + srb.getTransId(), e);
    }
    finally
    {
      TryCleanup.tryCleanup(pfb, pstmt, rs);
    }
    if (conAddrIdHome != null)
    {
      srb.setSamAddrId(conAddrIdHome);
    }
  }
  
  public static void insertConHomePhone(PosFileBean pfb, SalesReceiptBean srb)
  {
    srb.setSamPhoneHomeId(String.valueOf(pfb.getConPhoneNextId()));
    pfb.setConPhoneNextId(pfb.getConPhoneNextId() + 1);
    StringBuilder sbHome = new StringBuilder();
    sbHome.append("insert into sam_con_phone ");
    sbHome.append("(con_phone_id,con_id,phone,status,type,change_dt,change_uid) ");
    sbHome.append("values ");
    sbHome.append("(?,?,?,'A',?,sysdate,'POS')");
    OraclePreparedStatement pstmt = null;
    OracleResultSet rs = null;
    try
    {
      pstmt = (OraclePreparedStatement) pfb.getConnection().prepareStatement(sbHome.toString());
      pstmt.setString(1, srb.getSamPhoneHomeId());
      pstmt.setString(2, srb.getSamConId());
      pstmt.setString(3, srb.getPhoneHome());
      pstmt.setString(4, "Home Phone");
      pstmt.executeUpdate();
    }
    catch (Exception e)
    {
      ExceptionLogger.logException(pfb, "ExecuteSales", "insertConHomePhone", "Trans: " + srb.getTransId(), e);
    }
    finally
    {
      TryCleanup.tryCleanup(pfb, pstmt, rs);
    }
  }

  public static void insertConWorkPhone(PosFileBean pfb, SalesReceiptBean srb)
  {
    srb.setSamPhoneWorkId(String.valueOf(pfb.getConPhoneNextId()));
    pfb.setConPhoneNextId(pfb.getConPhoneNextId() + 1);
    StringBuilder sbWork = new StringBuilder();
    sbWork.append("insert into sam_con_phone ");
    sbWork.append("(con_phone_id,con_id,phone,status,type,change_dt,change_uid) ");
    sbWork.append("values ");
    sbWork.append("(?,?,?,'A',?,sysdate,'POS')");
    OraclePreparedStatement pstmt = null;
    OracleResultSet rs = null;
    try
    {
      pstmt = (OraclePreparedStatement) pfb.getConnection().prepareStatement(sbWork.toString());
      pstmt.setString(1, srb.getSamPhoneWorkId());
      pstmt.setString(2, srb.getSamConId());
      pstmt.setString(3, srb.getPhoneWork());
      pstmt.setString(4, "Work Phone");
      pstmt.executeUpdate();
    }
    catch (Exception e)
    {
      ExceptionLogger.logException(pfb, "ExecuteSales", "insertConWorkPhone", "Trans: " + srb.getTransId(), e);
    }
    finally
    {
      TryCleanup.tryCleanup(pfb, pstmt, rs);
    }
  }

  public static void insertSR(PosFileBean pfb, SalesReceiptBean srb)
  {
    srb.setSamSrId(String.valueOf(pfb.getSrNextId()));
    pfb.setSrNextId(pfb.getSrNextId() + 1);
    StringBuilder sb = new StringBuilder();
    sb.append("insert into sam_con_sls_rcpt");
    sb.append("(sls_rcpt_id,logged_dt,logged_uid,received_dt,");
    sb.append("sls_rcpt_dt,sls_rcpt_no,source,status,comp_code,control_no,");
    sb.append("form,trans_id,pricing_method,con_id,rtlr_addr_id,");
    sb.append("delivered_dt,tally_dt,con_phone_id,con_addr_id)values");
    sb.append("(?,sysdate,'POS',sysdate,to_date(?,'YYMMDD'),?,'EDI',");
    sb.append("'C','USA',?,'SAM_SR_MASTER',");
    sb.append("?,?,?,?,sysdate,sysdate,?,?)");
    OraclePreparedStatement pstmt = null;
    OracleResultSet rs = null;
    try
    {
      pstmt = (OraclePreparedStatement) pfb.getConnection().prepareStatement(sb.toString());
      pstmt.setString(1, srb.getSamSrId());
      pstmt.setString(2, srb.getSaleDt());
      pstmt.setString(3, srb.getInvoiceNo());
      pstmt.setString(4, srb.getControlNo());
      pstmt.setString(5, srb.getTransId());
      pstmt.setString(6, srb.getPricingMethod());
      pstmt.setString(7, srb.getSamConId());
      pstmt.setString(8, srb.getSamRtlrAddrId());
      pstmt.setString(9, srb.getSamPhoneHomeId());
      pstmt.setString(10, srb.getSamAddrId());
      pstmt.executeUpdate();
    }
    catch (Exception e)
    {
      ExceptionLogger.logException(pfb, "ExecuteSales", "insertSR", "Trans: " + srb.getTransId(), e);
    }
    finally
    {
      TryCleanup.tryCleanup(pfb, pstmt, rs);
    }
  }

  public static void insertConSA(PosFileBean pfb, SalesReceiptBean srb, ConSaBean conSa)
  {
    conSa.setSamConSAId(String.valueOf(pfb.getConSANextId()));
    pfb.setConSANextId(pfb.getConSANextId() + 1);
    StringBuilder sb = new StringBuilder();
    sb.append("insert into sam_con_sa(con_sa_id,con_id,comp_code, ");
    sb.append("delpur_dt,logged_dt,logged_uid,rtlr_addr_id,sa_no,sa_amt, ");
    sb.append("sa_status,sa_type_id,sls_rcpt_id,pos_item_qty,serial_no)values");
    sb.append("(?,?,'USA',to_date(?,'YYMMDD'), ");
    sb.append("sysdate,'POS',?,?,?,'Active',?,?,?,substr(?,1,32)) ");
    OraclePreparedStatement pstmt = null;
    OracleResultSet rs = null;
    try
    {
      pstmt = (OraclePreparedStatement) pfb.getConnection().prepareStatement(sb.toString());
      pstmt.setString(1, conSa.getSamConSAId());
      pstmt.setString(2, srb.getSamConId());
      pstmt.setString(3,conSa.getDelPurDt());
      pstmt.setString(4, srb.getSamRtlrAddrId());
      pstmt.setString(5, srb.getSaNo());
      pstmt.setString(6, conSa.getSaAmt());
      pstmt.setString(7, conSa.getSaTypeId());
      pstmt.setString(8, srb.getSamSrId());
      pstmt.setString(9, srb.getTab18ItemQty());
      pstmt.setString(10, srb.getSerialNo());
      pstmt.executeUpdate();
    }
    catch (Exception e)
    {
      ExceptionLogger.logException(pfb, "ExecuteSales", "insertConSA", "Trans: " + srb.getTransId(), e);
    }
    finally
    {
      TryCleanup.tryCleanup(pfb, pstmt, rs);
    }
  }

  public static void insertSRDetail(PosFileBean pfb, SalesReceiptBean srb, SrDetailBean srdb)
  {
    srdb.setSamSrItemId(String.valueOf(pfb.getSrItemNextId()));
    pfb.setSrItemNextId(pfb.getSrItemNextId() + 1);

    Iterator i = srb.getConSAs().iterator(); //Find this item's Con SA ID
    while (i.hasNext())
    {
      ConSaBean conSa = (ConSaBean) i.next();
      if (srdb.getSamConSaId() == null && conSa.getCoverageType().equals(srdb.getSaType()) && (srdb.getPricingCode() == null || srdb.getPricingCode().equals(conSa.getPricingCode())))
      {
        srdb.setSamConSaId(conSa.getSamConSAId());
      }
    }
    if (srdb.getSamConSaId() == null && srb.getSamEliteConSAId() != null && srdb.getPricingCode() == null) //Populate for Elite Con SA
    {
      srdb.setSamConSaId(srb.getSamEliteConSAId());
    }
    if (srdb.getSamConSaId() != null)
    {
      StringBuilder sb = new StringBuilder();
      sb.append("insert into sam_con_sls_rcpt_item");
      sb.append("(sls_rcpt_item_id,sls_rcpt_id,item_id,pricing_code,con_sa_id,delivered_dt,");
      sb.append("qty,price_amt,sku_no,mfg_id,mfg_other,color_style,description,logged_dt,");
      sb.append("logged_uid,status,furniture_id,item_sa_amt,tally_dt,sa_type,pos_fh_id,serial_no,ack_no,plan_item_id)values");
      sb.append("(?,?,?,?,?,");
      sb.append("to_date(?,'YYMMDD'),?,?,substr(?,1,32),105,substr(?,1,100),substr(?,1,32),");
      sb.append("substr(?,1,250),sysdate,'POS','A',98,?,sysdate,?,?,substr(?,1,32),substr(?,1,32),substr(?,1,250))");
      OraclePreparedStatement pstmt = null;
      try
      {
        pstmt = (OraclePreparedStatement) pfb.getConnection().prepareStatement(sb.toString());
        pstmt.setString(1, srdb.getSamSrItemId());
        pstmt.setString(2, srb.getSamSrId());
        pstmt.setString(3, srdb.getItemId());
        pstmt.setString(4, srdb.getPricingCode());
        pstmt.setString(5, srdb.getSamConSaId());
        pstmt.setString(6, srdb.getDeliveryDt());
        pstmt.setString(7, srdb.getQty());
        pstmt.setString(8, srdb.getUnitAmt());
        pstmt.setString(9, srdb.getSkuNo());
        pstmt.setString(10, srdb.getManufName());
        pstmt.setString(11, srdb.getItemColorStyle());
        if (srdb.getItemDescription() != null)
        {
           if (srdb.getItemDescription().length() > 250)
              pstmt.setString(12, srdb.getItemDescription().substring(0,250));
           else
             pstmt.setString(12, srdb.getItemDescription());
        }
        else
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
        ExceptionLogger.logException(pfb, "ExecuteSales", "insertSrDetail", "Trans: " + srb.getTransId() + "Item: " + srdb.getItemId(), e);
      }
      finally
      {
        TryCleanup.closePreparedStatement(pfb, pstmt);
      }
    }
    else
    {
      ExceptionLogger.logError("Service Agreement Type not found", " ITEM_ID: " + srdb.getItemId(), "SR Detail Insert", pfb, srb, srdb);
    }
  }
}
