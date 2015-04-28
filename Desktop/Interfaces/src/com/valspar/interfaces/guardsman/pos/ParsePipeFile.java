package com.valspar.interfaces.guardsman.pos;

import com.valspar.interfaces.guardsman.pos.utility.*;
import java.io.*;
import java.util.*;
import com.valspar.interfaces.guardsman.pos.beans.*;
import oracle.jdbc.OraclePreparedStatement;
import oracle.jdbc.OracleResultSet;
import org.apache.log4j.Logger;

public class ParsePipeFile
{
  static Logger log4jLogger = Logger.getLogger(ParsePipeFile.class.getName());

  public static ArrayList parsedTrans = new ArrayList();
  static boolean headerLast = false;
  static String lastTransId = new String();
  static int counter = 0;

  public ParsePipeFile(PosFileBean pfb)
  {
    try
    {
      FileReader fr = new FileReader(pfb.getFileName());
      BufferedReader br = new BufferedReader(fr);
      String record = new String();
      int lineNum = 0;
      headerLast = false;
      //Read Line
      while ((record = br.readLine()) != null)
      {
        ReadLineBean readLine = new ReadLineBean();
        readLine.setRecord(record);
        readLine.setPipeError(false);
        readLine.setDetailTransIdMissMatchError(false);
        lineNum += 1;
        StringTokenizer st = new StringTokenizer(record, "|", true);
        validateDataRequirements(pfb, st, readLine);
        if (!readLine.isPipeError()) //Passed Step 1 - Add to Temp Transaction
        {
          addReadLineToParsedTrans(pfb, readLine);
        }
      }
      if (!headerLast) //Add last good parsed transaction to File. 
      {
        addParsedTransToFile(pfb);
      }
      else
      {
        ExceptionLogger.logError("Header with no Detail", "Record: " + lastTransId, "Check for Detail", pfb, null, null);
      }
    }
    catch (IOException e)
    {
      ExceptionLogger.logException(pfb, "ParsePipeFile", "ParsePipeFile", null, e);
    }
    log4jLogger.info("Number of Transactions: " + counter);
  }

  public static void validateDataRequirements(PosFileBean pfb, StringTokenizer inSt, ReadLineBean inReadLineBean)
  {
    String value = new String();
    int numPipes = 0;
    int pos = 1;
    String[] parsedLine = inReadLineBean.getParsedLine();
    while (inSt.hasMoreTokens())
    {
      value = inSt.nextToken();
      if (!value.equals("|"))
      {
        /*if (pos > 36)
        {
          inReadLineBean.setPipeError(true);
          ExceptionLogger.logError("Too many values", "Record: " + inReadLineBean.getRecord(), "Value Count", pfb, null, null);
        }
        else*/
        if (pos <= 36)
        {
          parsedLine[pos] = value.trim();
          if (parsedLine[pos].equalsIgnoreCase(""))
          {
            parsedLine[pos] = null;
          }
        }
      }
      else
      {
        numPipes += 1;
        pos += 1;
      }
    }
    //if (numPipes != 36) //Validation Step 1 - Correct # of pipes
    if (numPipes > 40) //Validation Step 1 - Correct # of pipes
    {
      inReadLineBean.setPipeError(true);
      ExceptionLogger.logError("Incorrect number of delimiters", "Record: " + inReadLineBean.getRecord(), "Validate Data Requirements", pfb, null, null);
    }
    else if (parsedLine[12] == null && "H".equalsIgnoreCase(parsedLine[1]))
    {
      inReadLineBean.setPipeError(true);
      ExceptionLogger.logError("Did not receive Consumer Last Name", "Record: " + inReadLineBean.getRecord(), "Validate Data Requirements", pfb, null, null);
    }
    else if ((parsedLine[3] == null || parsedLine[4] == null) && "FH".equalsIgnoreCase(parsedLine[1]))
    {
      inReadLineBean.setPipeError(true);
      ExceptionLogger.logError("Did not receive required start & end dates", "Record: " + inReadLineBean.getRecord(), "Validate Data Requirements", pfb, null, null);
    }
    }

  public static void addReadLineToParsedTrans(PosFileBean pfb, ReadLineBean inReadLineBean)
  {
    String[] parsedLine = inReadLineBean.getParsedLine();

    if ("H".equalsIgnoreCase(parsedLine[1]))
    {
      if (headerLast)
      {
        ExceptionLogger.logError("Header with no Detail", "Record: " + lastTransId, "Check for Detail", pfb, null, null);
      }

      if (parsedTrans.size() > 0)
      {
        //Add last good parsed transaction to File.
        addParsedTransToFile(pfb);
      }

      // if Cancel record is found do not set headerLast
      if (!"C".equalsIgnoreCase(parsedLine[3]))
      {
        headerLast = true;
      }
      parsedTrans.add(inReadLineBean);
      //check if trans_id in the parse in null
      if (parsedLine[2] != null)
      {
        lastTransId = parsedLine[2];
      }
      //trans_id is null. log the error.
      else
      {
        lastTransId = "headerTransIdWasNull";
        ExceptionLogger.logError("TRANS_ID on Header Line is NULL", "Record: " + inReadLineBean.getRecord(), "Check TRANS_ID", pfb, null, null);
      }
    }
    else if ("D".equalsIgnoreCase(parsedLine[1]))
    {
      //check if trans_id in the parse in null
      if (parsedLine[2] != null)
      {
        //there was a trans_id, check if it is the same as the lastTransId
        if (!parsedLine[2].equals(lastTransId))
        {
          inReadLineBean.setDetailTransIdMissMatchError(true);
          ExceptionLogger.logError("TRANS_ID on Detail Line does not match the Header's TRANS_ID", "Record: " + inReadLineBean.getRecord(), "Check TRANS_ID", pfb, null, null);
        }
        //check if the item is a dupe
        else if (duplicateItemInTransaction(inReadLineBean))
        {
          ExceptionLogger.logError("ITEM_ID invalid. Duplicate found", "Record: " + inReadLineBean.getRecord(), "Duplicate Item check", pfb, null, null);
        }
        else //Detail OK. Matches Header TRANS_ID, add Detail to parsedFile
        {
          parsedTrans.add(inReadLineBean);
        }
      }
      else
      {
        inReadLineBean.setDetailTransIdMissMatchError(true);
        ExceptionLogger.logError("TRANS_ID on Detail Line is NULL", "Record: " + inReadLineBean.getRecord(), "Check TRANS_ID", pfb, null, null);
      }
      headerLast = false;
    }
    else if ("FH".equalsIgnoreCase(parsedLine[1]))
    {
      pfb.setFileHeader(true);
      pfb.setFileRunDt(parsedLine[2]);
      pfb.setFileExtractStartDt(parsedLine[3]);
      pfb.setFileExtractEndDt(parsedLine[4]);
    }
    else
    {
      ExceptionLogger.logError("Unknown TRANS_TYPE", "Record: " + inReadLineBean.getRecord(), "Check TRANS_TYPE", pfb, null, null);
    }
  }

  public static boolean duplicateItemInTransaction(ReadLineBean inReadLineBean)
  {
    boolean duplicate = false;
    String[] parsedLineCheck = inReadLineBean.getParsedLine();
    Iterator i = parsedTrans.iterator();
    while (!duplicate && i.hasNext())
    {
      ReadLineBean rlb = (ReadLineBean) i.next();
      String[] parsedLine = rlb.getParsedLine();
      if (parsedLine[1].equals("D") && parsedLine[21].equals(parsedLineCheck[21]))
      {
        duplicate = true;
      }
    }
    return duplicate;
  }

  public static boolean transactionInFile(PosFileBean pfb)
  {
    boolean transFound = false;
    Iterator i = pfb.getSalesReceipts().iterator();
    while (!transFound && i.hasNext())
    {
      SalesReceiptBean srb = (SalesReceiptBean) i.next();
      Iterator j = parsedTrans.iterator();

      while (!transFound && j.hasNext())
      {
        ReadLineBean readLineBean = (ReadLineBean) j.next();
        String[] pl = readLineBean.getParsedLine();
        if (srb.getTransId() != null)
        {
            if (srb.getPricingMethod() == null)
            {
              if ("H".equals(pl[1]))
              {
              if (srb.getTransId().equals(pl[2]) && pl[6] == null)
              {
                transFound = true;
              }
            }
          }
          else
          {
            if (srb.getTransId().equals(pl[2]) && srb.getPricingMethod().equals(pl[6]))
            {
              transFound = true;
            }
          }
        }
      }
    }
    return transFound;
  }

  public static SalesReceiptBean getSRBForTrans(PosFileBean pfb)
  {
    boolean srbFound = false;
    SalesReceiptBean outGoingSalesReceiptBean = new SalesReceiptBean();
    Iterator i = pfb.getSalesReceipts().iterator();
    while (!srbFound && i.hasNext())
    {
      SalesReceiptBean srb = (SalesReceiptBean) i.next();
      if (srb.getTransId() != null)
      {

        if (srb.getTransId().equals(lastTransId))
        {
          srbFound = true;
          outGoingSalesReceiptBean = srb;
        }
      }
    }
    return outGoingSalesReceiptBean;
  }

  public static void addParsedTransToFile(PosFileBean pfb)
  {
    if (!transactionInFile(pfb)) //Create new SR and append it to the Hashmap.
    {
      SalesReceiptBean srb = new SalesReceiptBean();
      srb.setHasSale(false);
      srb.setHasReturn(false);
      srb.setHasUpdate(false);
      SrHeaderBean srHeaderBean = new SrHeaderBean();
      Iterator i = parsedTrans.iterator();
      while (i.hasNext())
      {
        ReadLineBean readLineBean = (ReadLineBean) i.next();
        String[] pl = readLineBean.getParsedLine();
        if (readLineBean.getParsedLine()[1].equals("H"))
        {

          // Build the SR");
          srb.setTransId(pl[2]);
          srb.setRetailerNo(pl[4]);
          srb.setStoreNo(pl[4],pl[5]);
          srb.setPricingMethod(pl[6]);
          srb.setSaNo(pl[7]);
          srb.setSaAmt(pl[8]);
          srb.setInvoiceNo(pl[9]);
          srb.setControlNo(pl[10]);
          srb.setSaleDt(pl[11]);
          srb.setLastName(pl[12]);
          srb.setFirstName(pl[13]);
          srb.setAddress1(pl[14]);
          srb.setAddress2(pl[15]);
          srb.setCity(pl[16]);
          if (pl[17] != null)
          {
            srb.setState(pl[17].toUpperCase());
          }
          srb.setPostalCode(pl[18]);
          srb.setPhoneHome(readLineBean.scrubPhone(pl[19]));
          srb.setPhoneWork(readLineBean.scrubPhone(pl[20]));
          srb.setLanguage(pl[33]);
          srb.setSerialNo(pl[34]);
          srb.setEmail(pl[36]);
          if (srb.getPricingMethod() != null)
          {
            srb.setSaTypeId(getSATypeID(srb.getPricingMethod(), pfb));
          }

          if (pl[3].equals("S"))
          {
            srb.setHasSale(true);
          }
          else if (pl[3].equals("R"))
          {
            srb.setHasReturn(true);
          }
          else if (pl[3].equals("U"))
          {
            srb.setHasUpdate(true);
          }
          else if (pl[3].equals("C"))
          {
            srb.setHasCancel(true);
          }
          srHeaderBean.setTransCode(pl[3]); //Build the SR Header
          srb.addSrHeader(srHeaderBean);
        }
        else //Build Detail
        {
          if (validateSrDetail(pfb, srb, pl))
          {
            SrDetailBean srdb = new SrDetailBean();
            srdb.setItemId(pl[21]);
            srdb.setPricingCode(pl[22]);
            srdb.setSaType(pl[23]);
            srdb.setDeliveryDt(pl[24]);
            srdb.setQty(pl[25]);
            srdb.setUnitAmt(pl[26]);
            srdb.setExtendedPrice(pl[27]);
            srdb.setSkuNo(pl[28]);
            srdb.setManufName(pl[29]);
            srdb.setItemColorStyle(pl[30]);
            srdb.setItemDescription(pl[31]);
            srdb.setItemSaAmt(pl[32]);
            srdb.setPlanItemId(pl[35]);
            if (srdb.getPricingCode() != null)
            {
              srdb.setSaTypeId(getSATypeID(srdb.getPricingCode(), pfb));
            }
            else
            {
              srdb.setSaTypeId(srb.getSaTypeId());
            }
            srHeaderBean.addSrDetail(srdb);
            srHeaderBean.setHasItems(true);
          }
        }
      }
      pfb.getSalesReceipts().add(srb);
    }
    else
    {
      createSrHeaders(pfb, getSRBForTrans(pfb));
    }
    parsedTrans.clear();
    counter += 1;
  }

  public static boolean validateSrDetail(PosFileBean pfb, SalesReceiptBean srb, String[] parsedLine)
  {
    boolean passedTest = true;
    // Check for null PRICING_CODE
    if (parsedLine[22] == null)
    {
      if (srb.getPricingMethod() == null)
      {
        passedTest = false;
        ExceptionLogger.logError("Detail Line does not have a PRICING_CODE (position 22) ", " ITEM_ID: " + parsedLine[21], "SA TYPE validation", pfb, srb, null);
      }
    }
    // Check for null SA_TYPE
    if (parsedLine[23] == null)
    {
      passedTest = false;
      ExceptionLogger.logError("Detail Line does not have a SA_TYPE (position 23) ", " ITEM_ID: " + parsedLine[21], "SA TYPE validation", pfb, srb, null);
    }
    return passedTest;
  }

  public static void createSrHeaders(PosFileBean pfb, SalesReceiptBean srb)
  {
    String lastTransCode = new String();
    Iterator i = parsedTrans.iterator();
    while (i.hasNext()) //Go through the Parsed Trans
    {
      ReadLineBean readLineBean = (ReadLineBean) i.next();
      String[] pl = readLineBean.getParsedLine();
      if (pl[1].equals("H")) //Look for an existing header, else add it.
      {
        lastTransCode = pl[3];
        boolean headerFound = false;
        Iterator j = srb.getSrHeaders().iterator();
        while (j.hasNext() && !headerFound) //Verify that this type of Header does not already exist
        {
          SrHeaderBean srhb = (SrHeaderBean) j.next();
          if (srhb.getTransCode().equals(pl[3]))
          {
            headerFound = true;
          }
        }
        if (!headerFound)
        {
          SrHeaderBean newSrhb = new SrHeaderBean();
          newSrhb.setTransCode(pl[3]);
          srb.addSrHeader(newSrhb);
          if (pl[3].equals("S"))
          {
            srb.setHasSale(true);
          }
          else if (pl[3].equals("R"))
          {
            srb.setHasReturn(true);
          }
          else if (pl[3].equals("C"))
          {
            srb.setHasCancel(true);
          }
          else if (pl[3].equals("U"))
          {
            srb.setHasUpdate(true);
            srb.setPricingMethod(pl[6]);
            if (srb.getPricingMethod() != null)
            {
              srb.setSaTypeId(getSATypeID(srb.getPricingMethod(), pfb));
            }
          }
        }
      } //End of Header
      else if (pl[1].equals("D"))
      {
        boolean detailHeaderFound = false;
        Iterator k = srb.getSrHeaders().iterator();
        while (k.hasNext() && !detailHeaderFound)
        {
          SrHeaderBean srhb = (SrHeaderBean) k.next();
          if (srhb.getTransCode().equals(lastTransCode))
          {
            detailHeaderFound = true;
            SrDetailBean srdb = new SrDetailBean();
            srdb.setItemId(pl[21]);
            srdb.setPricingCode(pl[22]);
            srdb.setSaType(pl[23]);
            srdb.setDeliveryDt(pl[24]);
            srdb.setQty(pl[25]);
            srdb.setUnitAmt(pl[26]);
            srdb.setExtendedPrice(pl[27]);
            srdb.setSkuNo(pl[28]);
            srdb.setManufName(pl[29]);
            srdb.setItemColorStyle(pl[30]);
            srdb.setItemDescription(pl[31]);
            srdb.setItemSaAmt(pl[32]);
            srdb.setPlanItemId(pl[35]);
            if (srdb.getPricingCode() != null)
            {
              srdb.setSaTypeId(getSATypeID(srdb.getPricingCode(), pfb));
            }
            else
            {
              srdb.setSaTypeId(srb.getSaTypeId());
            }
            srhb.addSrDetail(srdb);
            srhb.setHasItems(true);
          }
        }
      } //End of Detail
    } //End of Trans Loop
  }

  public boolean isHeaderLast()
  {
    return headerLast;
  }

  public void setHeaderLast(boolean headerLast)
  {
    this.headerLast = headerLast;
  }

  public String getlastTransId()
  {
    return lastTransId;
  }

  public void setlastTransId(String lastTransId)
  {
    this.lastTransId = lastTransId;
  }

  public static String getSATypeID(String pricingID, PosFileBean pfb)
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
      ExceptionLogger.logException(pfb, "ParsePipeFile", "getSATypeID", null, e);
    }
    finally
    {
      TryCleanup.tryCleanup(pfb, pstmt, rs);
    }

    return saTypeId;
  }
}
