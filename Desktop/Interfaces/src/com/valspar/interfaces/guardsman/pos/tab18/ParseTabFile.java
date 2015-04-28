package com.valspar.interfaces.guardsman.pos.tab18;

import com.valspar.interfaces.guardsman.pos.program.GuardsmanPointOfSaleInterface;
import com.valspar.interfaces.guardsman.pos.beans.*;
import com.valspar.interfaces.guardsman.pos.utility.*;
import java.io.*;
import java.util.*;
import oracle.jdbc.OraclePreparedStatement;
import oracle.jdbc.OracleResultSet;
import org.apache.log4j.Logger;

public class ParseTabFile
{
  static Logger log4jLogger = Logger.getLogger(ParseTabFile.class.getName());

  public ParseTabFile(PosFileBean pfb)
  {
    try
    {
      FileReader fr = new FileReader(pfb.getFileName());
      BufferedReader br = new BufferedReader(fr);
      String record = new String();
      int lineNum = 0;
      while ((record = br.readLine()) != null)
      {
        ReadLineBean readLineBean = new ReadLineBean();
        readLineBean.setRecord(record);
        readLineBean.setPipeError(false);
        readLineBean.setDetailTransIdMissMatchError(false);
        lineNum += 1;
        StringTokenizer st = new StringTokenizer(readLineBean.getRecord(), "\t", true); // "\t" is the Tab.
        validateTabs(pfb, st, readLineBean);
        if (!readLineBean.isPipeError() && !duplicateSale(pfb, readLineBean)) //Passed Step 1 - Add to Temp Transaction
        {
          addReadLineToFile(pfb, readLineBean);
        }
      }
      log4jLogger.info("Number of Transactions: " + lineNum);
    }
    catch (IOException e)
    {
      ExceptionLogger.logException(pfb, "ParseTabFile", "ParseTabFile", null, e);
    }
  }

  public static void validateTabs(PosFileBean pfb, StringTokenizer st, ReadLineBean readLineBean)
  {
    String value = new String();
    int position = 1;
    String[] parsedLine = readLineBean.getParsedLine();
    while (st.hasMoreTokens())
    {
      value = st.nextToken();
      if (!value.equals("\t"))
      {
        if (position <= 21)
        {
          parsedLine[position] = value;
        }
        else
        {
          readLineBean.setPipeError(true);
          ExceptionLogger.logError("Too many values", "Record: " + readLineBean.getRecord(), "Value Count", pfb, null, null);
        }
      }
      else
      {
        position += 1;
      }
    }
  }

  public static boolean duplicateSale(PosFileBean pfb, ReadLineBean readLineBean)
  {
    boolean saleFound = false;
    String[] pl = readLineBean.getParsedLine();
    Iterator i = pfb.getSalesReceipts().iterator();
    while (!saleFound && i.hasNext())
    {
      SalesReceiptBean srb = (SalesReceiptBean) i.next();
      
      if ("S".equals(pl[2]) && srb.getTransId() == pl[1])
      {
        saleFound = true;
        ExceptionLogger.logError("Transaction is not unique", pl[1], "Transaction Validation", pfb, null, null);
      }
    }

    return saleFound;
  }

  public static void addReadLineToFile(PosFileBean pfb, ReadLineBean readLineBean)
  {
    SalesReceiptBean srb = new SalesReceiptBean();
    String[] pl = readLineBean.getParsedLine();
    if ("S".equals(pl[2]))
    {
      srb.setHasSale(true);
    }
    else if ("R".equals(pl[2]))
    {
      srb.setHasReturn(true);
    }
    else if ("U".equals(pl[2]))
    {
      srb.setHasUpdate(true);
    }
    
    if ("FH".equals(pl[1]))
    {
      pfb.setFileHeader(true);
      pfb.setFileRunDt(pl[2]);
      pfb.setFileExtractStartDt(pl[3]);
      pfb.setFileExtractEndDt(pl[4]);
    } else
    {
      srb.setTransId(pl[1]);
      srb.setRetailerNo(pl[3]);
      srb.setStoreNo(pl[4]);
      srb.setPricingMethod(pl[5]);
      srb.setSaAmt(pl[6]);
      srb.setSaleDt(pl[7]);
      if (pfb.getRtlrFtpId().equals("19"))
      {
        readLineBean.setParsedName(pl[8]);
        srb.setLastName(readLineBean.getParsedLastName());
        srb.setFirstName(readLineBean.getParsedFirstName());
      } else
      {
        srb.setLastName(pl[8]);
        srb.setFirstName(pl[9]);
      }
      srb.setAddress1(pl[10]);
      srb.setAddress2(pl[11]);
      srb.setCity(pl[12]);
      if (pl[13]!=null) 
      {
        srb.setState(pl[13].toUpperCase());
      }
      srb.setPostalCode(pl[14]);
      srb.setPhoneHome(readLineBean.scrubPhone(pl[15]));
      srb.setPhoneWork(readLineBean.scrubPhone(pl[16]));
      srb.setEmail(pl[20]);
      srb.setTab18SAType(pl[17]);
      srb.setTab18ItemQty(pl[18]);
      srb.setLanguage(pl[19]);
      checkForRequiredFields(pfb, srb);
      pfb.getSalesReceipts().add(srb);
      srb.setSaTypeId(getSATypeID(srb.getPricingMethod(),pfb));
    }
  }

  public static void checkForRequiredFields(PosFileBean pfb, SalesReceiptBean srb)
  {
    if (srb.getTransId() == null || srb.getRetailerNo() == null || srb.getStoreNo() == null || srb.getPricingMethod() == null || srb.getSaAmt() == null || srb.getSaleDt() == null || srb.getTab18SAType() == null || srb.getTab18ItemQty() == null)
    {
      ExceptionLogger.logError("Required field(s) not found", null, "Required Field Check", pfb, srb, null);
    }
  }
  
  public static String getSATypeID(String pricingID,PosFileBean pfb)
  {
    String saTypeId = null;
    
    StringBuilder sb = new StringBuilder();
    sb.append("select sam_rtlr_pricing.sa_type_id ");
    sb.append("from sam_rtlr_pricing ");
    sb.append("where rtlr_pricing_id = ?");
    
    OraclePreparedStatement pstmt = null;
    OracleResultSet rs = null;
    try
    {
      pstmt = (OraclePreparedStatement) pfb.getConnection().prepareStatement(sb.toString());
      pstmt.setString(1, pricingID);
      rs = (OracleResultSet) pstmt.executeQuery();
      while (rs.next())
      {
        saTypeId = rs.getString(1);
      }
    }
    catch (Exception e)
    {
      ExceptionLogger.logException(pfb, "ParseTabFile", "getSATypeID", null, e);
    }
    finally
    {
      TryCleanup.tryCleanup(pfb, pstmt, rs);
    }
    
    return saTypeId;
  }

}
