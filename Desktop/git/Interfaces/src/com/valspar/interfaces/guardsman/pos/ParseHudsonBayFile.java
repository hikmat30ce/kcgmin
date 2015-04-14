package com.valspar.interfaces.guardsman.pos;

import com.valspar.interfaces.guardsman.pos.utility.*;
import java.io.*;
import java.util.*;
import com.valspar.interfaces.guardsman.pos.beans.*;
import java.text.SimpleDateFormat;
import oracle.jdbc.OraclePreparedStatement;
import oracle.jdbc.OracleResultSet;
import org.apache.log4j.Logger;

public class ParseHudsonBayFile
{
  static Logger log4jLogger = Logger.getLogger(ParseHudsonBayFile.class.getName());

  public static ArrayList parsedTrans = new ArrayList();
  static String lastTransId = new String();
  static String lastTransType = new String();
  static String headerSku = new String();
  static String headerItemSaAmt = new String();
  static String headerTransId = new String();
  static int counter = 0;

  public ParseHudsonBayFile(PosFileBean pfb)
  {
    try
    {
      this.setlastTransId("");
      this.setLastTransType("");
      FileReader fr = new FileReader(pfb.getFileName());
      BufferedReader br = new BufferedReader(fr);
      String record = new String();
      int lineNum = 0;
      //Read Line
      while ((record = br.readLine()) != null)
      {
        ReadLineBean readLine = new ReadLineBean();
        readLine.setRecord(record);
        readLine.setPipeError(false);
        readLine.setDetailTransIdMissMatchError(false);
        lineNum += 1;
        validateDataRequirements(readLine, pfb);
        if (!readLine.isPipeError()) //Passed Step 1 - Add to Temp Transaction */
        {
          addReadLineToParsedTrans(pfb, readLine);
        }
      }
      addParsedTransToFile(pfb);
    }
    catch (IOException e)
    {
      ExceptionLogger.logException(pfb, "ParseHudsonBayFile", "ParseHudsonBayFile", null, e);
    }
    log4jLogger.info("Number of Transactions: " + counter);
  }

  /*
   * Validates what kind of a line it is and if it's the proper length.
   * It breaks up the valid lines into an array, based on the location within the fixed length string.
   */

  public static void validateDataRequirements(ReadLineBean inReadLineBean, PosFileBean pfb)
  {
    String[] parsedLine = inReadLineBean.getParsedLine();
    String hbLine = inReadLineBean.getRecord();
    boolean noHeaderUpdate = false;

    /* FURNITURE FORMAT VARIABLE
     * Furniture format files are easily distinguisable by their length.
     */
    boolean furnitureFormat = (hbLine.length() > 400);

    /* VALID SKUs
     * These variables serve as checks to make sure the
     * SKU that HBC sends is a valid SKU that we check.
     * HBC regularly sends junk data so it is necessary to
     * keep this up to date, even though it rarely changes.
     */
    // mattress skus
    String checkSKU1 = "82300989"; //twin
    String checkSKU2 = "82300997"; //twin xl
    String checkSKU3 = "82301011"; //double
    String checkSKU4 = "82301029"; //double xl
    String checkSKU5 = "82301037"; //queen
    String checkSKU6 = "82301045"; //king
    String checkSKU7 = "82301052"; //cally king
    String checkSKU8 = "05158888"; //old number
    // new matress skus (change 516699)
    String checkSKU34 = "85488669"; //twin
    String checkSKU35 = "85489090"; //twin xl
    String checkSKU36 = "85489105"; //double
    String checkSKU37 = "85489107"; //double xl
    String checkSKU38 = "85489109"; //queen
    String checkSKU39 = "85489113"; //king
    String checkSKU40 = "85489115"; //california king
    // furniture skus
    String checkSKU9 = "83931594"; //below $1000
    String checkSKU10 = "83932785"; //below $1000
    String checkSKU11 = "83932788"; //below $1000
    String checkSKU12 = "83932789"; //below $1000
    String checkSKU13 = "83932790"; //below $1000
    String checkSKU14 = "83932791"; //above $1000
    String checkSKU15 = "83932792"; //above $1000
    String checkSKU16 = "83932793"; //above $1000
    String checkSKU17 = "83932794"; //above $1000
    String checkSKU18 = "83933091"; //above $1000
    String checkSKU19 = "83933093"; //above $1000
    String checkSKU20 = "83933094"; //above $1000
    String checkSKU21 = "83933097"; //above $1000
    // new furniture skus
    String checkSKU22 = "84220657"; //below $1000 -- 7YR TG FURNITURE $0 to$299.99
    String checkSKU23 = "84220658"; //below $1000 -- 7YR TG FURNITURE $300 to $499.99
    String checkSKU24 = "84220659"; //below $1000 -- 7YR TG FURNITURE $500 to $699.99
    String checkSKU25 = "84220660"; //below $1000 -- 7YR TG FURNITURE $700 to $999.99
    String checkSKU26 = "84220661"; //above $1000 -- 7YR TG FURNITURE $1000 to$1499.99
    String checkSKU27 = "84220662"; //above $1000 -- 7YR TG FURNITURE $1500 to $1999.99
    String checkSKU28 = "84220663"; //above $1000 -- 7YR TG FURNITURE $2000 to $2499.99
    String checkSKU29 = "84220665"; //above $1000 -- 7YR TG FURNITURE $2500 to $2999.99
    String checkSKU30 = "84220666"; //above $1000 -- 7YR TG FURNITURE $3000 to $3999.99
    String checkSKU31 = "84220667"; //above $1000 -- 7YR TG FURNITURE $4000 to $4999.99
    String checkSKU32 = "84220668"; //above $1000 -- 7YR TG FURNITURE $5000 to $7999.99
    String checkSKU33 = "84220670"; //above $1000 -- 7YR TG FURNITURE $8000 Plus

    /* LINE LENGTH CHECK (mattress format & furniture format)
     * Is the string long enough to be a sales receipt line?
     * If it isn't go through line checking if it is a file header.
     * End result: if it is too short, throw it away.
     */
    if (hbLine.length() < 386)
    {
      /* CHECK IF LINE IS FILE HEADER
       * If it is, collect date information.
       */
      if ("H".equals(hbLine.substring(0, 1)))
      {
        parsedLine[1] = "FH";
        /* PROCESS DATE
         * If not present, it is defaulted to today's date
         */
        parsedLine[2] = ParseHudsonBayFile.now("yyyyMMdd");
        parsedLine[3] = hbLine.substring(7, 15);
        /* END DATE
         * If not present, it is defaulted to today's date
         */
        parsedLine[4] = ParseHudsonBayFile.now("yyyyMMdd");
      }
      /* CHECK IF LINE IS FILE FOOTER
       * File footers are just thrown out.
       */
      else if ("T".equals(hbLine.substring(0, 1)))
      {
        inReadLineBean.setPipeError(true);
      }
      /* THROW AWAY LINE
       * Line cannot be identified, so throw it away.
       */
      else
      {
        ExceptionLogger.logError("Line is too short and was not identified as file header or footer.", "Record: " + inReadLineBean.getRecord(), "line length check", pfb, null, null);
        inReadLineBean.setPipeError(true);
      }
    }

    /*
     * ELSE -- NOT A SHORTENED LINE
     * The line is over 386 characters, meaning it is probably a valid line.
     * The first two ifs do some essential work then the following if/else if statements are looking for a reason to throw it away.
     * If it passes all of those, the final else if processes the file.
     */
    else
    {
      int[] columnArray = new int[2];
      //if (!("        ".equals(hbLine.substring(20, 28)))) //is it the new format?
      if ("     ".equals(hbLine.substring(1, 6)))
      {
        columnArray[0] = 18; //end of order number (trans_id) and beginning of prime line (item line)
        columnArray[1] = 28; //end sub line (item line)
      }
      else
      {
        columnArray[0] = 14; //end of dealer reference 1 or order line (trans_id) and beginning of prime line or dealer reference 2 (item line)
        columnArray[1] = 20; //end dealer reference 3 or sub line (item line)
      }

      /* DOES THIS HAVE CORRESPONDING HEADER ALREADY IN APPLIX? (furniture format)
       * Check if the header's trans id (old format trans id) matches
       * the trans id on this detail line.
       * If it doesn't see if it exists, if it does make it an update and build the header.
       */
      if (furnitureFormat && ("Y".equals(hbLine.substring(419, 420))) && !(headerTransId.equals(hbLine.substring(6, columnArray[0]))))
      {

        noHeaderUpdate = true;
        String noHeaderTransId = hbLine.substring(6, columnArray[0]).concat(hbLine.substring(413, 419));
        if (inApplix(noHeaderTransId, pfb))
        {
          if ("644".equals(saTypeIdInApplix(noHeaderTransId, pfb)))
          {
            headerSku = "83932791";
          }
          else
          {
            headerSku = "83931594";
          }
          headerItemSaAmt = saAmtInApplix(noHeaderTransId, pfb);
          headerTransId = (hbLine.substring(6, columnArray[0]));
        }
      }

      /* IS THIS LINE A HEADER (furniture format)
     * check if it is a header on the Furniture format line.
     * If there is, grab the SKU and item SA Amt and throw it away.
     */
      if (furnitureFormat && " ".equals(hbLine.substring(419, 420)))
      {
        headerSku = (hbLine.substring(344, 352));
        headerItemSaAmt = hbLine.substring(354, 359) + "." + hbLine.substring(359, 361);
        headerTransId = (hbLine.substring(6, columnArray[0]));
        inReadLineBean.setPipeError(true);
      }
      /* INTERFACE FLAG CHECK (furniture format)
     * check if the line has a interface flag,
     * meaning it has been processed before,
     * if it does throw it away with no error.
     */
      else if (furnitureFormat && "Y".equals(hbLine.substring(420, 421)))
      {
        inReadLineBean.setPipeError(true);
      }
      /* DOES THIS HAVE CORRESPONDING HEADER? (furniture format)
     * Check if the header's trans id (old format trans id) matches
     * the trans id on this detail line. If not leave error message
     * and throw it away.
     */
      else if (furnitureFormat && ("Y".equals(hbLine.substring(419, 420))) && !(headerTransId.equals(hbLine.substring(6, columnArray[0]))))
      {
        ExceptionLogger.logError("New sale detail with no header", "Record: " + inReadLineBean.getRecord(), "header check", pfb, null, null);
        inReadLineBean.setPipeError(true);
      }
      /*
     * CHECK IF SKU IS VALID
     * If it is a matress, derive the result directly from a substring of the line.
     * If it is furniture, derive the result by checking it verse the header SKU.
     * In furniture format, all detail line SKUs are to be ignored.
     * If the SKU is found to be valid proceed into the main parsing section.
     */
      else if ((checkSKU1.equals(hbLine.substring(344, 352))) || (checkSKU2.equals(hbLine.substring(344, 352))) || (checkSKU3.equals(hbLine.substring(344, 352))) || (checkSKU4.equals(hbLine.substring(344, 352))) || (checkSKU5.equals(hbLine.substring(344, 352))) || (checkSKU6.equals(hbLine.substring(344, 352))) || (checkSKU7.equals(hbLine.substring(344, 352))) || (checkSKU8.equals(hbLine.substring(344, 352))) || (checkSKU34.equals(hbLine.substring(344, 352))) || (checkSKU35.equals(hbLine.substring(344, 352))) || (checkSKU36.equals(hbLine.substring(344, 352))) || (checkSKU37.equals(hbLine.substring(344, 352))) || (checkSKU38.equals(hbLine.substring(344, 352))) || (checkSKU39.equals(hbLine.substring(344, 352))) || (checkSKU40.equals(hbLine.substring(344, 352))) || (checkSKU9.equals(headerSku)) || (checkSKU10.equals(headerSku)) || (checkSKU11.equals(headerSku)) || (checkSKU12.equals(headerSku)) || (checkSKU13.equals(headerSku)) || (checkSKU14.equals(headerSku)) || (checkSKU15.equals(headerSku)) || (checkSKU16.equals(headerSku)) || (checkSKU17.equals(headerSku)) || (checkSKU18.equals(headerSku)) || (checkSKU19.equals(headerSku)) || (checkSKU20.equals(headerSku)) || (checkSKU21.equals(headerSku)) || (checkSKU22.equals(headerSku)) || (checkSKU23.equals(headerSku)) || (checkSKU24.equals(headerSku)) || (checkSKU25.equals(headerSku)) || (checkSKU26.equals(headerSku)) || (checkSKU27.equals(headerSku)) || (checkSKU28.equals(headerSku)) || (checkSKU29.equals(headerSku)) || (checkSKU30.equals(headerSku)) || (checkSKU31.equals(headerSku)) || (checkSKU32.equals(headerSku)) || (checkSKU33.equals(headerSku)))
      {
        /* MARK AS HEADER OR DETAIL FOR NATIVE APPLICATION
       * This marks the line as a header or detail while processing.
       * This is NOT the HBC definition of header.
       * Header lines on the furniture format have some info extracted
       * and then are thrown out, so on our side the header is simply
       * the first line of a set of line belonging to the same transaction.
       * If the line is the first line and is marked "header", it is later
       * pulled apart into both a header and a detail.
       */
        if (lastTransId.equals(hbLine.substring(6, columnArray[0])) && lastTransType.equals(hbLine.substring(0, 1)))
        {
          parsedLine[1] = "D";
        }
        else if (furnitureFormat)
        {

          if (lastTransId.equals(hbLine.substring(6, columnArray[0]).concat(hbLine.substring(413, 419))) && lastTransType.equals(hbLine.substring(0, 1)))
          {
            parsedLine[1] = "D";
          }
          else
          {
            parsedLine[1] = "H";
          }
        }
        else
        {
          parsedLine[1] = "H";
        }
        /* Trans ID */
        if (furnitureFormat)
        {
          parsedLine[2] = hbLine.substring(6, columnArray[0]).concat(hbLine.substring(413, 419));
        }
        else
        {
          parsedLine[2] = hbLine.substring(6, columnArray[0]);
        }
        /* Trans Code */
        if ("A".equals(hbLine.substring(0, 1)) && "H".equals(parsedLine[1]))
        {
          //if it is an add, run an sql statement to see if it's been entered into Applix before
          boolean update = false;
          update = inApplix(parsedLine[2], pfb);
          if (!update)
          {
            update = inFile(parsedLine[2], pfb, inReadLineBean);
          }

          if (update)
          {
            parsedLine[3] = "U";
          }
          else
          {
            parsedLine[3] = "S";
          }
        }
        //If it is not an add, check if it is a cancel/return mark it as a return. Skipping cancel, because cancel removes all the lines from the sale.
        else if ("C".equals(hbLine.substring(0, 1)))
        {
          parsedLine[3] = "R";
          inFile(parsedLine[2], pfb, inReadLineBean);
        }
        //Else I have no idea, so just mark it as the first letter in the string.
        else
        {
          parsedLine[3] = hbLine.substring(0, 1);
        }
        /* retailer_no  */
        parsedLine[4] = "2304272";
        /* store_no  */
        parsedLine[5] = hbLine.substring(216, 220);
        /* invoice_no  */
        parsedLine[9] = hbLine.substring(6, columnArray[0]);
        /* control_no  */
        parsedLine[10] = hbLine.substring(28, 35);
        /* sale_dt  */
        if (furnitureFormat)
        {
          parsedLine[11] = hbLine.substring(413, 419);
        }
        else
        {
          parsedLine[11] = hbLine.substring(230, 236);
        }
        /* last_name  */
        parsedLine[12] = hbLine.substring(36, 50).toUpperCase();
        /* first_name  */
        parsedLine[13] = hbLine.substring(51, 65).toUpperCase();
        /* address1  */
        parsedLine[14] = hbLine.substring(66, 95);
        /* address2  */
        parsedLine[15] = hbLine.substring(96, 125);
        /* city  */
        parsedLine[16] = hbLine.substring(126, 145);
        /* state  */
        parsedLine[17] = hbLine.substring(146, 148);
        /* postal_code  */
        parsedLine[18] = hbLine.substring(150, 156);
        /* phone_home  */
        parsedLine[19] = hbLine.substring(156, 166);
        /* phone_work  */
        if ("0000000000".equals(hbLine.substring(166, 176)))
        {
        }
        else
        {
          parsedLine[20] = hbLine.substring(166, 176);
        }
        /* item_id  */
        parsedLine[21] = hbLine.substring(columnArray[0], columnArray[1]);
        /* pricing_code  */
        if (checkSKU1.equals(hbLine.substring(344, 352)))
        {
          parsedLine[22] = "672";
        }
        else if (checkSKU2.equals(hbLine.substring(344, 352)))
        {
          parsedLine[22] = "677";
        }
        else if (checkSKU3.equals(hbLine.substring(344, 352)))
        {
          parsedLine[22] = "673";
        }
        else if (checkSKU4.equals(hbLine.substring(344, 352)))
        {
          parsedLine[22] = "678";
        }
        else if (checkSKU5.equals(hbLine.substring(344, 352)))
        {
          parsedLine[22] = "674";
        }
        else if (checkSKU6.equals(hbLine.substring(344, 352)))
        {
          parsedLine[22] = "675";
        }
        else if (checkSKU7.equals(hbLine.substring(344, 352)))
        {
          parsedLine[22] = "676";
        }
        else if (checkSKU8.equals(hbLine.substring(344, 352)))
        {
          parsedLine[22] = "681";
        }
        else if ((checkSKU34.equals(hbLine.substring(344, 352))) || (checkSKU35.equals(hbLine.substring(344, 352))) || (checkSKU36.equals(hbLine.substring(344, 352))) || (checkSKU37.equals(hbLine.substring(344, 352))) || (checkSKU38.equals(hbLine.substring(344, 352))) || (checkSKU39.equals(hbLine.substring(344, 352))) || (checkSKU40.equals(hbLine.substring(344, 352))))
        {
          parsedLine[22] = "1091";
        }
        else if (checkSKU9.equals(headerSku))
        {
          parsedLine[22] = "779"; //add correct pricing id below $1000
        }
        else if (checkSKU10.equals(headerSku))
        {
          parsedLine[22] = "779"; //add correct pricing id below $1000
        }
        else if (checkSKU11.equals(headerSku))
        {
          parsedLine[22] = "779"; //add correct pricing id below $1000
        }
        else if (checkSKU12.equals(headerSku))
        {
          parsedLine[22] = "779"; //add correct pricing id below $1000
        }
        else if (checkSKU13.equals(headerSku))
        {
          parsedLine[22] = "779"; //add correct pricing id below $1000
        }
        else if (checkSKU14.equals(headerSku))
        {
          parsedLine[22] = "780"; //add correct pricing id above $1000
        }
        else if (checkSKU15.equals(headerSku))
        {
          parsedLine[22] = "780"; //add correct pricing id above $1000
        }
        else if (checkSKU16.equals(headerSku))
        {
          parsedLine[22] = "780"; //add correct pricing id above $1000
        }
        else if (checkSKU17.equals(headerSku))
        {
          parsedLine[22] = "780"; //add correct pricing id above $1000
        }
        else if (checkSKU18.equals(headerSku))
        {
          parsedLine[22] = "780"; //add correct pricing id above $1000
        }
        else if (checkSKU19.equals(headerSku))
        {
          parsedLine[22] = "780"; //add correct pricing id above $1000
        }
        else if (checkSKU20.equals(headerSku))
        {
          parsedLine[22] = "780"; //add correct pricing id above $1000
        }
        else if (checkSKU21.equals(headerSku))
        {
          parsedLine[22] = "780"; //add correct pricing id above $1000
        }
        else if (checkSKU22.equals(headerSku))
        {
          parsedLine[22] = "779"; //add correct pricing id below $1000
        }
        else if (checkSKU23.equals(headerSku))
        {
          parsedLine[22] = "779"; //add correct pricing id below $1000
        }
        else if (checkSKU24.equals(headerSku))
        {
          parsedLine[22] = "779"; //add correct pricing id below $1000
        }
        else if (checkSKU25.equals(headerSku))
        {
          parsedLine[22] = "779"; //add correct pricing id below $1000
        }
        else if (checkSKU26.equals(headerSku))
        {
          parsedLine[22] = "780"; //add correct pricing id above $1000
        }
        else if (checkSKU27.equals(headerSku))
        {
          parsedLine[22] = "780"; //add correct pricing id above $1000
        }
        else if (checkSKU28.equals(headerSku))
        {
          parsedLine[22] = "780"; //add correct pricing id above $1000
        }
        else if (checkSKU29.equals(headerSku))
        {
          parsedLine[22] = "780"; //add correct pricing id above $1000
        }
        else if (checkSKU30.equals(headerSku))
        {
          parsedLine[22] = "780"; //add correct pricing id above $1000
        }
        else if (checkSKU31.equals(headerSku))
        {
          parsedLine[22] = "780"; //add correct pricing id above $1000
        }
        else if (checkSKU32.equals(headerSku))
        {
          parsedLine[22] = "780"; //add correct pricing id above $1000
        }
        else if (checkSKU33.equals(headerSku))
        {
          parsedLine[22] = "780"; //add correct pricing id above $1000
        }
        /* 2ND SKU CHECK ERROR
       * If the SKU has errored out on this else, there is a problem, it should've errored out
       * on the original SKU check and the log error should've been sent to the exception logger
       * at the end of this method.
       * If this is catching errors, you likely need to check to make sure that the code that is
       * matching up HBC SKUs to our SKUs is correct and the same as the SKUs used in the original
       * SKU check.
       */
        else
        {
          if (furnitureFormat)
          {
            ExceptionLogger.logError("Not a valid SKU", "SKU# on header: " + headerSku + " Record: " + inReadLineBean.getRecord(), "2nd SKU check", pfb, null, null);
          }
          else
          {
            ExceptionLogger.logError("Not a valid SKU", "SKU#: " + hbLine.substring(344, 352) + "Record: " + inReadLineBean.getRecord(), "2nd SKU check", pfb, null, null);
          }
          inReadLineBean.setPipeError(true);
        }
        /* sa_type  */
        if (furnitureFormat)
        {
          parsedLine[23] = "A";
        }
        else
        {
          parsedLine[23] = "M";
        }
        /* delivery_dt  */
        if (furnitureFormat)
        {
          if ("000000".equals(hbLine.substring(230, 236)))
          {
            parsedLine[24] = hbLine.substring(411, 419);
          }
          else
          {
            parsedLine[24] = hbLine.substring(230, 236);
          }
        }
        else
        {
          parsedLine[24] = hbLine.substring(230, 236);
        }
        /* qty  */
        parsedLine[25] = "1";
        /* unit_amt  */
        boolean errorCheck = hbLine.substring(305, 312).indexOf("-") > 0;
        if (errorCheck)
        {
          parsedLine[26] = hbLine.substring(305, 312).replace("-", "0");
        }
        else
        {
          parsedLine[26] = hbLine.substring(305, 312);
        }
        /* extended_amt  */
        parsedLine[27] = parsedLine[26];
        /* item_sku  */
        parsedLine[28] = hbLine.substring(295, 303);
        /* manuf_name  */
        parsedLine[29] = hbLine.substring(260, 279);
        /* item_color_style  */
        parsedLine[30] = hbLine.substring(280, 294);
        /* item_description  */
        parsedLine[31] = hbLine.substring(324, 344);
        /* item_sa_amt  */
        if (furnitureFormat)
        {
          parsedLine[32] = headerItemSaAmt;
          // if we add it more than once, then it will become cumulative
          headerItemSaAmt = "0.00";
        }
        else
        {
          parsedLine[32] = hbLine.substring(354, 359) + "." + hbLine.substring(359, 361);
        }
        /* language  */
        if (("QC".equals(parsedLine[17])))
        {
          parsedLine[33] = "FRN";
        }
        else
        {
          parsedLine[33] = "ENG";
        }
        lastTransType = hbLine.substring(0, 1);

        /* UNIQUE FURNITURE FORMAT FIELDS (reference)
        sales order date = hbLine.substring(411, 419);
        detail flag = hbLine.substring(419, 420);
        interfaced flag = hbLine.substring(420, 421);
        file creation date = hbLine.substring(396, 404);
        file sequation no = hbLine.substring(404, 409);
      */

      }
      else
      {
        if (furnitureFormat)
        {
          ExceptionLogger.logError("Not a valid SKU", "SKU# on header: " + headerSku + " Record: " + inReadLineBean.getRecord(), "SKU check", pfb, null, null);
        }
        else
        {
          ExceptionLogger.logError("Not a valid SKU", "SKU#: " + hbLine.substring(344, 352) + "Record: " + inReadLineBean.getRecord(), "SKU check", pfb, null, null);
        }
        inReadLineBean.setPipeError(true);
      }
    }
  }

  public static void addReadLineToParsedTrans(PosFileBean pfb, ReadLineBean inReadLineBean)
  {
    String[] parsedLine = inReadLineBean.getParsedLine();

    if ("H".equalsIgnoreCase(parsedLine[1]))
    {
      if (parsedTrans.size() > 0)
      {
        addParsedTransToFile(pfb); //Add last good parsed transaction to File.
      }
      parsedTrans.add(inReadLineBean);
      lastTransId = parsedLine[2];
      //add detail line for returns and updates if they are in same file as associated sale
      inFile(parsedLine[2], pfb, inReadLineBean);
      if ((parsedLine[3].equals("R") || parsedLine[3].equals("U")) && inReadLineBean.isInSaleFile())
      {
        ReadLineBean rlbCopy = (ReadLineBean) inReadLineBean.clone();
        String[] plCopy = (String[]) rlbCopy.getParsedLine().clone();
        plCopy[1] = "D";
        rlbCopy.setParsedLine(plCopy);
        parsedTrans.add(rlbCopy);
      }
    }
    else if ("D".equalsIgnoreCase(parsedLine[1]))
    {
      if (!parsedLine[2].equals(lastTransId))
      {
        inReadLineBean.setDetailTransIdMissMatchError(true);
        ExceptionLogger.logError("Transaction ID matching error", "Record: " + inReadLineBean.getRecord(), "TRANS_ID check", pfb, null, null);
      }
      else //Detail OK. Matches Header TRANS_ID, add Detail to parsedFile
      {
        parsedTrans.add(inReadLineBean);
      }
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
      ExceptionLogger.logError("Unknown TRANS_TYPE", "Record: " + inReadLineBean.getRecord(), "TRANS_TYPE check", pfb, null, null);
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

        if (srb.getTransId().equals(pl[2]))
        {
          transFound = true;
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
      if (srb.getTransId().equals(lastTransId))
      {
        srbFound = true;
        outGoingSalesReceiptBean = srb;
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
          // Build the SR
          srb.setTransId(pl[2]);
          srb.setRetailerNo(pl[4]);
          srb.setStoreNo(pl[5].trim());
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

          srHeaderBean.setTransCode(pl[3]); //Build the SR Header
          srb.addSrHeader(srHeaderBean);

          //Build Detail
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
        else //Build Detail Only
        {
          if (pl[3].equals("S") || pl[3].equals("U"))
          {
            srb.setSaleDt(pl[11]);
          }
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
        ExceptionLogger.logError("Detail line does not have a pricing code", " ITEM_ID: " + parsedLine[21], "SA TYPE validation", pfb, srb, null);
      }
    }
    // Check for null SA_TYPE
    if (parsedLine[23] == null)
    {
      passedTest = false;
      ExceptionLogger.logError("Detail Line does not have a service agreement type", " ITEM_ID: " + parsedLine[21], "SA TYPE validation", pfb, srb, null);
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
            if (pl[3].equals("S") || pl[3].equals("U"))
            {
              srb.setSaleDt(pl[11]);
            }
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

  public String getlastTransId()
  {
    return lastTransId;
  }

  public void setlastTransId(String lastTransId)
  {
    this.lastTransId = lastTransId;
  }

  private void setLastTransType(String lastTransType)
  {
    this.lastTransType = lastTransType;
  }

  //creates today's date for file header, since HudBay's doesn't have one.

  public static String now(String dateFormat)
  {
    Calendar cal = Calendar.getInstance();
    SimpleDateFormat sdf = new SimpleDateFormat(dateFormat);
    return sdf.format(cal.getTime());
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
      ExceptionLogger.logException(pfb, "ParseHudsonBayFile", "getSATypeID", null, e);
    }
    finally
    {
      TryCleanup.tryCleanup(pfb, pstmt, rs);
    }
    return saTypeId;
  }

  public static boolean inApplix(String transId, PosFileBean pfb)
  {
    boolean transFound = false;

    StringBuilder sb = new StringBuilder();
    sb.append("select sam_con_sls_rcpt.sls_rcpt_id, sam_con_sls_rcpt.con_id ");
    sb.append("from sam_con_sls_rcpt,sam_rtlr_addr ");
    sb.append("where sam_con_sls_rcpt.rtlr_addr_id = sam_rtlr_addr.rtlr_addr_id ");
    sb.append("and sam_rtlr_addr.erp_rtlr_no in ('2304272', '04272') ");
    sb.append("and sam_con_sls_rcpt.trans_id = ? ");
    OraclePreparedStatement pstmt = null;
    OracleResultSet rs = null;
    try
    {
      pstmt = (OraclePreparedStatement) pfb.getConnection().prepareStatement(sb.toString());
      pstmt.setString(1, transId);
      rs = (OracleResultSet) pstmt.executeQuery();
      if (rs.next())
      {
        transFound = true;
      }
    }
    catch (Exception e)
    {
      ExceptionLogger.logException(pfb, "ParseHudsonBayFile", "inApplix", null, e);
    }
    finally
    {
      TryCleanup.tryCleanup(pfb, pstmt, rs);
    }

    return transFound;
  }

  public static String saAmtInApplix(String transId, PosFileBean pfb)
  {
    String saAmt = null;

    StringBuilder sb = new StringBuilder();
    sb.append("SELECT consa.sa_amt ");
    sb.append("FROM   sam_con_sls_rcpt rcpt, ");
    sb.append("       sam_con_sa consa ");
    sb.append("WHERE  rcpt.trans_id = ? ");
    sb.append("AND    consa.sls_rcpt_id = rcpt.sls_rcpt_id ");

    OraclePreparedStatement pstmt = null;
    OracleResultSet rs = null;
    try
    {
      pstmt = (OraclePreparedStatement) pfb.getConnection().prepareStatement(sb.toString());
      pstmt.setString(1, transId);
      rs = (OracleResultSet) pstmt.executeQuery();
      while (rs.next())
      {
        saAmt = rs.getString(1);
      }
    }
    catch (Exception e)
    {
      ExceptionLogger.logException(pfb, "ParseHudsonBayFile", "getSATypeID", null, e);
    }
    finally
    {
      TryCleanup.tryCleanup(pfb, pstmt, rs);
    }
    return saAmt;
  }

  public static String saTypeIdInApplix(String transId, PosFileBean pfb)
  {
    String saTypeId = null;

    StringBuilder sb = new StringBuilder();
    sb.append("SELECT consa.sa_type_id ");
    sb.append("FROM   sam_con_sls_rcpt rcpt, ");
    sb.append("       sam_con_sa consa ");
    sb.append("WHERE  rcpt.trans_id = ? ");
    sb.append("AND    consa.sls_rcpt_id = rcpt.sls_rcpt_id ");

    OraclePreparedStatement pstmt = null;
    OracleResultSet rs = null;
    try
    {
      pstmt = (OraclePreparedStatement) pfb.getConnection().prepareStatement(sb.toString());
      pstmt.setString(1, transId);
      rs = (OracleResultSet) pstmt.executeQuery();
      while (rs.next())
      {
        saTypeId = rs.getString(1);
      }
    }
    catch (Exception e)
    {
      ExceptionLogger.logException(pfb, "ParseHudsonBayFile", "getSATypeID", null, e);
    }
    finally
    {
      TryCleanup.tryCleanup(pfb, pstmt, rs);
    }
    return saTypeId;
  }

  public static boolean inFile(String transId, PosFileBean pfb, ReadLineBean inReadLineBean)
  {
    boolean transFound = false;

    Iterator i = pfb.getSalesReceipts().iterator();
    while (!transFound && i.hasNext())
    {
      SalesReceiptBean srb = (SalesReceiptBean) i.next();
      if (srb.getTransId().equals(transId))
      {
        transFound = true;
        inReadLineBean.setInSaleFile(true);
      }
    }
    return transFound;
  }

}
