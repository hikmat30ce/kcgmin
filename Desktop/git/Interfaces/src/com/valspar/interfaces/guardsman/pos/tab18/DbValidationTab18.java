package com.valspar.interfaces.guardsman.pos.tab18;

import com.valspar.interfaces.guardsman.pos.*;
import com.valspar.interfaces.guardsman.pos.beans.*;
import com.valspar.interfaces.guardsman.pos.utility.*;
import java.util.*;
import oracle.jdbc.*;
import java.text.SimpleDateFormat;
import org.apache.log4j.Logger;

public class DbValidationTab18 extends DbValidation
{
  static Logger log4jLogger = Logger.getLogger(DbValidationTab18.class.getName());

  public DbValidationTab18(PosFileBean pfb)
  {
    cacheRetailers(pfb);
    verifyStore(pfb);
    cacheSATypes(pfb);
    verifySaType(pfb); //Unique for the Tab 18 format
    verifyUniqueTrans(pfb); //Unique for the Tab 18 format
    cacheStateIDs(pfb);
    cachePricingCodes(pfb);
  }

  public static void verifySaType(PosFileBean pfb)
  {
    Iterator i = pfb.getSalesReceipts().iterator();
    while (i.hasNext())
    {
      SalesReceiptBean srb = (SalesReceiptBean) i.next();
      if (srb.getErrors().size() == 0)
      {
        createConSAs(pfb, srb);
      }
    }
  }

  public static void createConSAs(PosFileBean pfb, SalesReceiptBean srb)
  {
    SimpleDateFormat sdf = new SimpleDateFormat("yyMMdd");
    Iterator i = pfb.getValidSaTypes().iterator();
    boolean saTypeFound = false;
    while (!saTypeFound && i.hasNext())
    {
      SaTypeBean saType = (SaTypeBean) i.next();
      try //Check Dates and ERP #
      {
        if ((srb.getSaleDt() != null) && (saType.getStartDate() != null))
        {
          Date saleDate = sdf.parse(srb.getSaleDt());
          Date startDate = sdf.parse(saType.getStartDate());
          if (!saleDate.before(startDate) && saType.getErpNo().equals(srb.getRetailerNo()) && (saType.getCoverageType().equals(srb.getTab18SAType()) || srb.getTab18SAType().equals("W") && saType.getCoverageType().equals("C")))
          {
            if (srb.getSaTypeId()==null||srb.getSaTypeId().equals(saType.getSaTypeId()))
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
        ExceptionLogger.logException(pfb, "DbValidationTab18", "createConSAs - Invalid Date", srb.getTransId(), e);
      }
      if (saTypeFound) //create a new con sa
      {
        ConSaBean conSa = new ConSaBean();
        conSa.setSaTypeId(saType.getSaTypeId());
        conSa.setCoverageType(srb.getTab18SAType());
        conSa.setSaAmt(srb.getSaAmt());
        srb.getConSAs().add(conSa);
      }
    }
    if (!saTypeFound) //No SA Type was ever found. Add an error.
    {
      ExceptionLogger.logError("Service Agreement Type not found", " SA_TYPE: " + srb.getTab18SAType(), "Con SA Validation", pfb, srb, null);
    }
  }

  public static void verifyUniqueTrans(PosFileBean pfb)
  {
    int srbCounter = 0;
    StringBuilder sb = new StringBuilder();
    /*#113732 kah 11-12-07 - add count of claims logged*/
    sb.append("select sam_con_sa.con_sa_id,count(claim_id) ");
    sb.append("from sam_con_sa,sam_sa_type,sam_rtlr_addr,sam_claim where ");
    sb.append("sam_con_sa.sa_type_id = sam_sa_type.sa_type_id ");
    sb.append("and sam_con_sa.rtlr_addr_id = sam_rtlr_addr.rtlr_addr_id ");
    sb.append("and sam_con_sa.con_sa_id = sam_claim.con_sa_id (+) ");
    sb.append("and sam_con_sa.sa_status <> 'R' ");
    sb.append("and sam_rtlr_addr.erp_rtlr_no = ? ");
    sb.append("and sam_con_sa.pos_trans_id = ? ");
    sb.append("and sam_sa_type.coverage_type = ? ");
    sb.append("group by sam_con_sa.con_sa_id");
    Iterator i = pfb.getSalesReceipts().iterator();
    while (i.hasNext())
    {
      SalesReceiptBean srb = (SalesReceiptBean) i.next();
      if (srb.getErrors().size() == 0)
      {
        String tempConSAId = new String();
        String tempClaimCnt = new String();
        int recordCount = 0;
        OraclePreparedStatement pstmt = null;
        OracleResultSet rs = null;
        try
        {
          pstmt = (OraclePreparedStatement) pfb.getConnection().prepareStatement(sb.toString());
          /*pstmt.setString(1, srb.getSamRtlrAddrId());*/
          pstmt.setString(1, srb.getRetailerNo());
          pstmt.setString(2, srb.getTransId());
          if (srb.getTab18SAType().equals("W"))
          {
            pstmt.setString(3, "C");
          }
          else
          {
            pstmt.setString(3, srb.getTab18SAType());
          }
          rs = (OracleResultSet) pstmt.executeQuery();
          while (rs.next())
          {
            tempConSAId = rs.getString(1);
            tempClaimCnt = rs.getString(2);
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
        if (recordCount > 0 && srb.isHasSale())
        {
          ExceptionLogger.logError("Transaction is not unique", null, "Unique TRANS_ID check", pfb, srb, null);
        }
        else if (recordCount == 1 && !srb.isHasSale())
        {
          srb.setSamEliteConSAId(tempConSAId);
          if (tempClaimCnt.equalsIgnoreCase("0"))
          {
            srb.setHasClaim(false);
          } else
          {
            srb.setHasClaim(true);
          }
          
          /*
           * Using the SRB's EliteConSAID.
           * Reason being that there are NO Sales Receipt Created in the Tab Format and the Con SA ID acts
           * like the SR in the Pipe 36 format. We need to keep this relationship to re-use code.
          */
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

  public static void cachePricingCodes(PosFileBean pfb)
  {
    StringBuilder sb = new StringBuilder();
    sb.append("select rtlr_pricing_id, type from sam_rtlr_pricing");
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
      ExceptionLogger.logException(pfb, "DbValidationTab18", "cachePricingCodes", null, e);
    }
    finally
    {
      TryCleanup.tryCleanup(pfb, pstmt, rs);
    }
  }
}
