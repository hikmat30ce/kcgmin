package com.valspar.interfaces.guardsman.pos;

import java.text.SimpleDateFormat;
import java.util.*;
import oracle.jdbc.*;
import com.valspar.interfaces.guardsman.pos.beans.*;
import com.valspar.interfaces.guardsman.pos.utility.*;
import org.apache.log4j.Logger;

public class DbValidation
{
  static Logger log4jLogger = Logger.getLogger(DbValidation.class.getName());

  public DbValidation()
  {
  }

  public DbValidation(PosFileBean pfb)
  {
    cacheRetailers(pfb);
    verifyStore(pfb);
    cacheSATypes(pfb);
    cachePricingMethods(pfb);
    verifySaType(pfb);
    verifyUniqueTrans(pfb);
    cacheStateIDs(pfb);
  }

  public static void cacheRetailers(PosFileBean pfb)
  {
    Iterator i = pfb.getSalesReceipts().iterator();
    while (i.hasNext())
    {
      SalesReceiptBean srb = (SalesReceiptBean) i.next();
      if (pfb.getUniqueRetailerMap().get(srb.getRetailerNo()) == null)
      {
        pfb.getUniqueRetailerMap().put(srb.getRetailerNo(), srb.getRetailerNo());
      }
    }
    //For each retailerNo (ERP #) retreive all active locations.
    String erpList = "";
    Iterator j = pfb.getUniqueRetailerMap().values().iterator();
    while (j.hasNext())
    {
      String erp = (String) j.next();
      erpList = erpList + "," + "'" + erp + "'";
    }

    erpList = erpList.replaceFirst(",", "");
    StringBuilder sb = new StringBuilder();
    sb.append("select rtlr_addr_id,rtlr_store_no, erp_rtlr_no, country ");
    sb.append("from sam_rtlr_addr ");
    sb.append("where rtlr_store_no is not null and erp_rtlr_no in (");
    sb.append(erpList);
    sb.append(")");
    OraclePreparedStatement pstmt = null;
    OracleResultSet rs = null;
    try
    {
      pstmt = (OraclePreparedStatement) pfb.getConnection().prepareStatement(sb.toString());
      rs = (OracleResultSet) pstmt.executeQuery();
      while (rs.next())
      {
        String key = rs.getString(3) + rs.getString(2);
        RetailerBean rb = new RetailerBean();
        rb.setSamRtlrAddrId(rs.getString(1));
        rb.setStoreNo(rs.getString(2));
        rb.setRetailerNo(rs.getString(3));
        rb.setRtlrCountry(rs.getString(4));
        pfb.getRetailerMap().put(key, rb);
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

    //For each retailerNo (ERP #) retreive ERP specific email address for errors and invoices.
    //    String erpList = new String();
    //    Iterator j = pfb.getUniqueRetailerMap().values().iterator();
    //    while (j.hasNext())
    //    {
    //      String erp = (String) j.next();
    //      erpList = erpList + "," + "'" + erp + "'";
    //    }

    //    erpList = erpList.replaceFirst(",", "");
    StringBuilder sb2 = new StringBuilder();
    sb2.append("select erp_rtlr_no, erp_invoice_email, erp_error_email ");
    sb2.append("from sam_rtlr_erp ");
    sb2.append("where erp_rtlr_no in (");
    sb2.append(erpList);
    sb2.append(")");
    pstmt = null;
    rs = null;
    try
    {
      pstmt = (OraclePreparedStatement) pfb.getConnection().prepareStatement(sb2.toString());
      rs = (OracleResultSet) pstmt.executeQuery();
      while (rs.next())
      {
        pfb.getErpInvoiceEmailMap().put(rs.getString(1), rs.getString(2));
        pfb.getErpErrorEmailMap().put(rs.getString(1), rs.getString(3));
      }
    }
    catch (Exception e)
    {
      ExceptionLogger.logException(pfb, "DbValidation", "cacheRetailers email addresses", null, e);
    }
    finally
    {
      TryCleanup.tryCleanup(pfb, pstmt, rs);
    }

  }

  public static ArrayList cacheSATypes(PosFileBean pfb)
  {
    String erpList = "";
    ArrayList validSaTypes = new ArrayList();
    Iterator i = pfb.getUniqueRetailerMap().values().iterator();
    while (i.hasNext()) //For each retailerNo (ERP #) retreive all active locations.
    {
      String retailerErp = (String) i.next();
      erpList = erpList + "," + "'" + retailerErp + "'";
    }
    erpList = erpList.replaceFirst(",", "");
    StringBuilder sb = new StringBuilder();
    sb.append("select sam_sa_type.sa_type_id, sam_rtlr_sa.erp_rtlr_no, sam_sa_type.coverage_type,");
    sb.append(" to_char(sam_rtlr_sa.sold_start_dt,'YYMMDD'), to_char(sam_rtlr_sa.sold_end_dt,'YYMMDD') from sam_rtlr_sa,sam_sa_type ");
    sb.append("where sam_rtlr_sa.sa_type_id = sam_sa_type.sa_type_id and sam_rtlr_sa.status = 'A'");
    sb.append(" and sam_sa_type.status = 'A' and sam_rtlr_sa.erp_rtlr_no in (");
    sb.append(erpList);
    sb.append(")");
    OraclePreparedStatement pstmt = null;
    OracleResultSet rs = null;
    try
    {
      pstmt = (OraclePreparedStatement) pfb.getConnection().prepareStatement(sb.toString());
      rs = (OracleResultSet) pstmt.executeQuery();
      while (rs.next())
      {
        SaTypeBean stb = new SaTypeBean();
        stb.setSaTypeId(rs.getString(1));
        stb.setErpNo(rs.getString(2));
        stb.setCoverageType(rs.getString(3));
        stb.setStartDate(rs.getString(4));
        stb.setEndDate(rs.getString(5));
        pfb.getValidSaTypes().add(stb);
      }
    }
    catch (Exception e)
    {
      ExceptionLogger.logException(pfb, "ExecuteSales", "cacheSATypes", null, e);
    }
    finally
    {
      TryCleanup.tryCleanup(pfb, pstmt, rs);
    }
    return validSaTypes;
  }

  public static void cachePricingMethods(PosFileBean pfb)
  {
    StringBuilder sb = new StringBuilder();
    sb.append("select rtlr_pricing_id, coverage_type from sam_rtlr_pricing ");
    sb.append("where type = 'Sale' and coverage_type is not null");
    OraclePreparedStatement pstmt = null;
    OracleResultSet rs = null;
    try
    {
      pstmt = (OraclePreparedStatement) pfb.getConnection().prepareStatement(sb.toString());
      rs = (OracleResultSet) pstmt.executeQuery();
      while (rs.next())
      {
        pfb.getPricingCodeMap().put(rs.getString(1), rs.getString(2));
      }
    }
    catch (Exception e)
    {
      ExceptionLogger.logException(pfb, "ExecuteSales", "cachePricingMethods", null, e);
    }
    finally
    {
      TryCleanup.tryCleanup(pfb, pstmt, rs);
    }
  }

  public static void cacheStateIDs(PosFileBean pfb)
  {
    StringBuilder sb = new StringBuilder();
    sb.append("select state,state_id from sam_lu_state where status = 'A' order by state");
    OraclePreparedStatement pstmt = null;
    OracleResultSet rs = null;
    try
    {
      pstmt = (OraclePreparedStatement) pfb.getConnection().prepareStatement(sb.toString());
      rs = (OracleResultSet) pstmt.executeQuery();
      while (rs.next())
      {
        pfb.getStateIdMap().put(rs.getString(1), rs.getString(2));
      }
    }
    catch (Exception e)
    {
      ExceptionLogger.logException(pfb, "ExecuteSales", "cacheStateIDs", null, e);
    }
    finally
    {
      TryCleanup.tryCleanup(pfb, pstmt, rs);
    }
  }

  public static void verifyStore(PosFileBean pfb)
  {
    boolean rtlrFound;
    rtlrFound = false;
    Iterator i = pfb.getSalesReceipts().iterator();
    while (i.hasNext())
    {
      SalesReceiptBean srb = (SalesReceiptBean) i.next();
      if (srb.getErrors().size() == 0)
      {
        if (pfb.getRetailerMap().get(srb.getRetailerNo() + srb.getStoreNo()) != null)
        {
          RetailerBean rb = (RetailerBean) pfb.getRetailerMap().get(srb.getRetailerNo() + srb.getStoreNo());
          srb.setSamRtlrAddrId(rb.getSamRtlrAddrId());
          srb.setRtlrCountry(rb.getRtlrCountry());
          rtlrFound = true;
        }
        else
        {
          ExceptionLogger.logError("Retailer Location not found", " / Retailer: " + srb.getRetailerNo() + " / Store: " + srb.getStoreNo(), "Retailer validation", pfb, srb, null);
        }
      }
      rtlrFound = false; //reset for the nest srb
    }
  }

  public static void verifySaType(PosFileBean pfb)
  {
    Iterator i = pfb.getSalesReceipts().iterator();
    while (i.hasNext())
    {
      SalesReceiptBean srb = (SalesReceiptBean) i.next();

      if (srb.getErrors().size() == 0 && (srb.isHasSale() || srb.isHasUpdate()))
      {
        //go by needed sa types
        if (srb.getPricingMethod() != null && srb.isHasSale()) //header level SA Type
        {
          verifyEliteSAType(pfb, srb, pfb.getValidSaTypes());
          verifyEliteItemsOnSale(pfb, srb);
        }
        createConSAs(pfb, srb, pfb.getValidSaTypes());
      }
    }
  }

  public static void verifyEliteSAType(PosFileBean pfb, SalesReceiptBean srb, ArrayList validSaTypes)
  {
    /*
     * Find the SA Type ID that goes to the Elite Sale's Pricing Method.
     * Check the active dates.
     */
    boolean saTypeFound;
    saTypeFound = false;
    SimpleDateFormat sdf = new SimpleDateFormat("yyMMdd");
    //Assigned to the type assiciated with that Pricing Method.
    String coverageType = (String) pfb.getPricingCodeMap().get(srb.getPricingMethod());
    Iterator j = validSaTypes.iterator();
    while (!saTypeFound && j.hasNext())
    {
      SaTypeBean saType = (SaTypeBean) j.next();
      try //Check Dates and ERP #
      {
        if ((srb.getSaleDt() != null) && (saType.getStartDate() != null))
        {
          Date saleDate = sdf.parse(srb.getSaleDt());
          Date startDate = sdf.parse(saType.getStartDate());
          if (!saleDate.before(startDate) && saType.getErpNo().equals(srb.getRetailerNo()))
          {
            if (saType.getCoverageType().equals(coverageType))
            {
              if (srb.getSaTypeId() == null || srb.getSaTypeId().equals(saType.getSaTypeId()))
              {
                saTypeFound = true;
              }
            }
            // Check out the end date too.
            if (saTypeFound && saType.getEndDate() != null) //If there is an end date add that condition
            {
              Date endDate = sdf.parse(saType.getEndDate());
              if (saleDate.after(endDate)) //add or equals
              {
                saTypeFound = false;
              }
            }
          }
        }
      }
      catch (Exception e)
      {
        ExceptionLogger.logException(pfb, "DbValidation", "verifyEliteSAType", "Invalid Date ", e);
      }
      if (saTypeFound)
      {
        srb.setSamSaTypeId(saType.getSaTypeId());
      }
    }

    if (!saTypeFound) //No SA Type was found. Add an error.
    {
      ExceptionLogger.logError("Elite Service Agreement Type not found", " PRICING_METHOD: " + srb.getPricingMethod(), "SA Validation", pfb, srb, null);
    }
  }

  public static void createConSAs(PosFileBean pfb, SalesReceiptBean srb, ArrayList validSaTypes)
  {
    Iterator i = srb.getSrHeaders().iterator();
    while (i.hasNext())
    {
      SrHeaderBean srHeader = (SrHeaderBean) i.next();
      Iterator j = srHeader.getSrDetails().iterator();
      while (j.hasNext() && (srHeader.getTransCode().equals("S") || srHeader.getTransCode().equals("U"))) //Sweep through srDetails
      {
        SrDetailBean srdb = (SrDetailBean) j.next();
        if (srdb.getSaType() != null)
        {
          boolean saTypeFound;
          saTypeFound = false;
          Iterator k = srb.getConSAs().iterator(); //Look for an existing
          while (k.hasNext())
          {
            ConSaBean conSa = (ConSaBean) k.next();
            if (srdb.getSaType().equals(conSa.getCoverageType()) && (srdb.getSaTypeId() == null || srdb.getSaTypeId().equals(conSa.getSaTypeId()))&& (srdb.getPricingCode() == null || srdb.getPricingCode().equals(conSa.getPricingCode()))) //add to the con sa
            {
              saTypeFound = true;
              if (srdb.getPricingCode() != null) //For itemized con sa's, add up the item prices.
              {
                try
                {
                  double conSaAmt = 0;
                  double itemSaAmt = 0;
                  if (conSa.getSaAmt() != null)
                  {
                    conSaAmt = Double.parseDouble(conSa.getSaAmt());
                  }
                  else
                  {
                    conSaAmt = 0;
                  }
                  if (srdb.getItemSaAmt() != null)
                  {
                    itemSaAmt = Double.parseDouble(srdb.getItemSaAmt());
                  }
                  else
                  {
                    itemSaAmt = 0;
                  }
                  conSaAmt = conSaAmt + itemSaAmt;
                  conSa.setSaAmt(String.valueOf(conSaAmt));
                }
                catch (Exception e)
                {
                  ExceptionLogger.logException(pfb, "DbValidation", "createConSAs - Con SA Addition", srb.getTransId(), e);
                }
              }
            }
          }
          if (!saTypeFound) //Create a new con sa if you can find one a valid SA Type.
          {
            SimpleDateFormat sdf = new SimpleDateFormat("yyMMdd");
            Iterator m = validSaTypes.iterator();
            while (!saTypeFound && m.hasNext())
            {
              SaTypeBean saType = (SaTypeBean) m.next();
              try //Check Dates and ERP #
              {
                if ((srb.getSaleDt() != null) && (saType.getStartDate() != null))
                {
                  Date saleDate = sdf.parse(srb.getSaleDt());
                  Date startDate = sdf.parse(saType.getStartDate());
                  if (!saleDate.before(startDate) && saType.getErpNo().equals(srb.getRetailerNo()) && (saType.getCoverageType().equals(srdb.getSaType()) || srdb.getSaType().equals("W") && saType.getCoverageType().equals("C")))
                  {
                    if (srdb.getSaTypeId() == null || srdb.getSaTypeId().equals(saType.getSaTypeId()))
                    {
                      saTypeFound = true;
                    }
                  }
                  if (saType.getEndDate() != null) //If there is an end date add that condition
                  {
                    Date endDate = sdf.parse(saType.getEndDate());
                    if (saleDate.after(endDate)) //add or equals
                    {
                      saTypeFound = false;
                    }
                  }
                }
              }
              catch (Exception e)
              {
                ExceptionLogger.logException(pfb, "DbValidation", "createConSAs - Invalid Date", srb.getTransId(), e);
              }
              if (saTypeFound) //create a new con sa
              {
                ConSaBean conSa = new ConSaBean();
                conSa.setSaTypeId(saType.getSaTypeId());
                conSa.setPricingCode(srdb.getPricingCode());
                conSa.setCoverageType(srdb.getSaType());
                conSa.setDelPurDt(srdb.getDeliveryDt());
                if (srdb.getPricingCode() == null)
                {
                  conSa.setSaAmt(srb.getSaAmt());
                }
                else
                {
                  conSa.setSaAmt(srdb.getItemSaAmt());
                }
                srb.getConSAs().add(conSa);
              }
            }
            if (!saTypeFound) //No SA Type was ever found. Add an error.
            {
              ExceptionLogger.logError("Service Agreement Type not found", " ITEM_ID: " + srdb.getItemId() + " SA_TYPE: " + srdb.getSaType(), "SA Validation", pfb, srb, null);
            }
          } //end of new Con SA logic
        }
      } //end of sr detail loop
    }
  }

  public static void verifyUniqueTrans(PosFileBean pfb)
  {
    int srbCounter = 0;
    StringBuilder sb = new StringBuilder();
    Iterator i = pfb.getSalesReceipts().iterator();
    while (i.hasNext())
    {
      SalesReceiptBean srb = (SalesReceiptBean) i.next();
      sb.setLength(0);
      sb.append("select sam_con_sls_rcpt.sls_rcpt_id, sam_con_sls_rcpt.con_id "); //added con_id 08-10-06
      sb.append("from sam_con_sls_rcpt,sam_rtlr_addr ");
      sb.append("where sam_con_sls_rcpt.rtlr_addr_id = sam_rtlr_addr.rtlr_addr_id ");
      sb.append("and sam_con_sls_rcpt.trans_id = ? ");
      sb.append("and sam_rtlr_addr.erp_rtlr_no = ? ");
      if (srb.getPricingMethod() == null)
      {
        sb.append("and sam_con_sls_rcpt.pricing_method IS NULL ");
      }
      else
      {
        sb.append("and sam_con_sls_rcpt.pricing_method = ? ");
      }
      if (srb.getErrors().size() == 0) //returns?
      {
        String tempSrId = "";
        String tempConId = ""; //added 08-10-06
        int recordCount = 0;

        OraclePreparedStatement pstmt = null;
        OracleResultSet rs = null;
        try
        {
          pstmt = (OraclePreparedStatement) pfb.getConnection().prepareStatement(sb.toString());
          pstmt.setString(1, srb.getTransId());
          pstmt.setString(2, srb.getRetailerNo());
          if (srb.getPricingMethod() != null)
          {
            pstmt.setString(3, srb.getPricingMethod());
          }
          rs = (OracleResultSet) pstmt.executeQuery();
          while (rs.next())
          {
            tempSrId = rs.getString(1);
            tempConId = rs.getString(2); //added 08-10-06
            recordCount = recordCount + 1;
          }
        }
        catch (Exception e)
        {
          ExceptionLogger.logException(pfb, "DbValidation", "verifyUniqueTrans", null, e);
        }
        finally
        {
          TryCleanup.tryCleanup(pfb, pstmt, rs);
        }
        if (recordCount == 1 && srb.isHasSale())
        {
          ExceptionLogger.logError("Transaction is not unique", null, "Unique TRANS_ID check", pfb, srb, null);
        }
        else if (recordCount == 1 && !srb.isHasSale())
        {
          srb.setSamSrId(tempSrId);
          srb.setSamConId(tempConId); //added 08-10-06
        }
        else if (recordCount > 1)
        {
          ExceptionLogger.logError("Multiple Transactions found", null, "Unique TRANS_ID check", pfb, srb, null);
        }
        srbCounter = srbCounter + 1;
        if (srbCounter % 1000 == 0)
        {
          log4jLogger.info("Sales Receipt validation. Record: " + srbCounter);
        }
      }
    }
    log4jLogger.info("Sales Receipt total: " + srbCounter);
  }

  public static void verifyEliteItemsOnSale(PosFileBean pfb, SalesReceiptBean srb)
  {
    boolean eliteItemFound = false;
    Iterator i = srb.getSrHeaders().iterator();
    while (!eliteItemFound && i.hasNext())
    {
      SrHeaderBean srHeader = (SrHeaderBean) i.next();
      Iterator j = srHeader.getSrDetails().iterator();
      while (!eliteItemFound && j.hasNext()) //Sweep through srDetails
      {
        SrDetailBean srDetail = (SrDetailBean) j.next();
        if ((srDetail.getPricingCode() == null))
        {
          eliteItemFound = true;
        }
      }
    }
    if (!eliteItemFound)
    {
      ExceptionLogger.logError("No Elite item found where PRICING_METHOD exists", "PRICING_METHOD: " + srb.getPricingMethod(), "SA Validation", pfb, srb, null);
    }
  }
}
