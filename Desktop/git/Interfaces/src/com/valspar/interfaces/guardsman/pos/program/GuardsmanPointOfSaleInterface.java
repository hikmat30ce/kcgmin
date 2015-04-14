package com.valspar.interfaces.guardsman.pos.program;

import com.valspar.interfaces.common.BaseInterface;
import com.valspar.interfaces.common.servlets.PropertiesServlet;
import com.valspar.interfaces.guardsman.pos.DbValidation;
import com.valspar.interfaces.guardsman.pos.ExecuteCancels;
import com.valspar.interfaces.guardsman.pos.ExecuteReturns;
import com.valspar.interfaces.guardsman.pos.ExecuteSales;
import com.valspar.interfaces.guardsman.pos.ExecuteUpdates;
import com.valspar.interfaces.guardsman.pos.ParseHudsonBayFile;
import com.valspar.interfaces.guardsman.pos.ParsePipeFile;
import com.valspar.interfaces.guardsman.pos.RetailerSetup;
import com.valspar.interfaces.guardsman.pos.beans.*;
import com.valspar.interfaces.guardsman.pos.tab18.*;
import com.valspar.interfaces.guardsman.pos.utility.*;
import java.io.File;
import java.util.*;
import oracle.jdbc.*;
import org.apache.log4j.*;

public class GuardsmanPointOfSaleInterface extends BaseInterface
{
  private static Logger log4jLogger = Logger.getLogger(GuardsmanPointOfSaleInterface.class);

  public GuardsmanPointOfSaleInterface()
  {
  }

  public void execute()
  {
    log4jLogger.info("Starting Guardsman POS");
    ConnectionBean cb = new ConnectionBean();
    OracleConnection conn = null;

    try
    {
      String scriptHome = PropertiesServlet.getProperty("guardsman.pos.scriptHome");
      String ftpServer = PropertiesServlet.getProperty("guardsman.pos.ftpServer");
      // TODO might be an issue since we're running as oracle now, instead of fsgpos
      log4jLogger.info("Running " + scriptHome + "/guardsman_pos_file_pull2.sh \"" + ftpServer + "\"...");
      String shellOutput = ShellUtility.runScript(scriptHome + "/guardsman_pos_file_pull2.sh", ftpServer);
      log4jLogger.info("   Script output is: " + shellOutput);

      conn = cb.openConnection();
      retailerLoop(conn);
    }
    catch (Exception e)
    {
      log4jLogger.error(e);
    }
    finally
    {
      cb.closeConnection(conn);
    }

    log4jLogger.info("Ending Guardsman POS");
  }

  public static void retailerLoop(OracleConnection conn)
  {
    String root = PropertiesServlet.getProperty("guardsman.pos.root");
    String folder_name = PropertiesServlet.getProperty("guardsman.pos.folder_name");
    String invoice_folder_name = PropertiesServlet.getProperty("guardsman.pos.invoice_folder_name");
    String error_folder_name = PropertiesServlet.getProperty("guardsman.pos.error_folder_name");
    Vector files = new Vector();
    File f = new File(root + folder_name);
    String[] dirList = null;
    String[] dir = f.list();

    for (int i = 0; i < dir.length; i++)
    {
      //hash table contains the time=file information
      Hashtable h = new Hashtable();
      File fl = new File(root + folder_name + "/" + dir[i]);
      dirList = fl.list();
      //timeSort is the array containing the numeric time last modified of every file
	  //the size of the array is the number of files in the directory
      long[] timeSort = new long[(dirList.length)];
      //n is the count variable used to assign the elements of the array as it loops through
      int n = 0;
      //fileTime is the variable for the time of each file, this might not be necessary
      long fileTime = 0;

      //this for loop goes through each file of the directory, x being the loop count
      for (int x = 0; x < dirList.length; x++)
      {
        //validation that the file ends with 'txt' and starts with 'valspar'
        if (dirList[x].toLowerCase().endsWith("txt"))
        {
          if (dirList[x].indexOf("valspar") >= 0)
          {
            //the file is valid so times and file names are assigned in the timeSort and hash
            File testDate = new File(root + folder_name + "/" + dir[i] + "/" + dirList[x]);
            fileTime = new Long(String.valueOf(testDate.lastModified()));
            //if files have same exact time, we need to create a minor difference to avoid errors
            if(fileTime != 0)
            {
              fileTime = fileTime + x;
            }
            h.put(String.valueOf(fileTime), dirList[x]);
            timeSort[n] = fileTime;
            n++;
          }
        }
      }

      //sorts the timeSort array to ensure the files are grabbed in order
      Arrays.sort(timeSort);

	  //for loop to continue cycling until the end of the timeSort array
      for (int rev = 0; rev < timeSort.length; rev++)
      {
        //if time is not 0, it is a valid file
        if (timeSort[rev] != 0)
        {
          files.addElement(h.get(String.valueOf(timeSort[rev])));
          String path = root + folder_name + "/" + dir[i];
          String invoicePath = root + folder_name + "/" + dir[i] + "/" + invoice_folder_name;
          String errorPath = root + folder_name + "/" + dir[i] + "/" + error_folder_name;
          String fileName = (String) h.get(String.valueOf(timeSort[rev]));
          String filePathAndName = path + "/" + fileName;
          File processFile = new File(filePathAndName);

          //the if is probably not needed, but I'm leaving the code as it was
          if (processFile.exists())
          {
            PosFileBean pfb = new PosFileBean();
            pfb.setFileName(filePathAndName);
            pfb.setInvoicePath(invoicePath);
            pfb.setErrorPath(errorPath);
            pfb.setConnection(conn);
            fetchPosFhId(pfb);
            new RetailerSetup(pfb, dir[i]);
            fileRun(pfb); // *** Main POS Method ***
            moveFileToArchive(pfb, path, fileName);
            reportErrors(pfb);
            if (pfb.getRetailerName().equalsIgnoreCase("levitz"))
            {
              runLevitzMattressExport(pfb);
            }
            pfb = null;
          }
        }
      }
    }
  }

  public static void fileRun(PosFileBean pfb)
  {
    if (pfb.getFileFormat().equals("Pipe 36"))
    {
      processPipe36File(pfb);
    }
    else if (pfb.getFileFormat().equals("Tab 18"))
    {
      processTab18File(pfb);
    }
    else if (pfb.getFileFormat().equals("Hudson Bay"))
    {
      processHudsonBayFile(pfb);
    }
    writePosFhRecord(pfb);
    log4jLogger.info("Finished Guardsman POS: " + new java.util.Date());
  }

  public static void processPipe36File(PosFileBean pfb)
  {
    new ParsePipeFile(pfb);
    new DbValidation(pfb);
    log4jLogger.info("Executing Sales");
    new ExecuteSales(pfb);
    log4jLogger.info("Executing Updates");
    new ExecuteUpdates(pfb);
    log4jLogger.info("Executing Returns");
    new ExecuteReturns(pfb);
    log4jLogger.info("Executing Cancels");
    new ExecuteCancels(pfb);
  }

  public static void processTab18File(PosFileBean pfb)
  {
    new ParseTabFile(pfb);
    new DbValidationTab18(pfb);
    log4jLogger.info("Executing Sales");
    new ExecuteSalesTab18(pfb);
    log4jLogger.info("Executing Updates");
    new ExecuteUpdatesTab18(pfb);
    log4jLogger.info("Executing Returns");
    new ExecuteReturnsTab18(pfb);
  }

  public static void processHudsonBayFile(PosFileBean pfb)
  {
    new ParseHudsonBayFile(pfb);
    new DbValidation(pfb);
    log4jLogger.info("Executing Sales");
    new ExecuteSales(pfb);
    log4jLogger.info("Executing Updates");
    new ExecuteUpdates(pfb);
    log4jLogger.info("Executing Returns");
    new ExecuteReturns(pfb);
    log4jLogger.info("Executing Cancels");
    new ExecuteCancels(pfb);
  }

  public static void reportErrors(PosFileBean pfb)
  {
    int errorCount = 0;
    Iterator k = pfb.getErrors().iterator(); //file errors
    while (k.hasNext())
    {
      errorCount = errorCount + 1;
      ErrorBean errorBean = (ErrorBean) k.next();
      log4jLogger.error("Error: " + errorCount + ") " + errorBean.getErrorMsg() + ".  When: " + errorBean.getValidationStep() + ".\n" + errorBean.getRecord());
    }

    Iterator m = pfb.getAdminMessages().iterator();
    while (m.hasNext())
    {
      errorCount = errorCount + 1;
      AdminMsgBean amb = (AdminMsgBean) m.next();
      log4jLogger.error("Admin Error: " + errorCount + ") " + amb.getProgramLocation() + " Msg: " + amb.getMessage() + " Item: " + amb.getItem());
    }

    Iterator i = pfb.getSalesReceipts().iterator(); //SR errors
    while (i.hasNext())
    {
      SalesReceiptBean srb = (SalesReceiptBean) i.next();
      if (srb.getErrors().size() > 0)
      {
        Iterator j = srb.getErrors().iterator();
        while (j.hasNext())
        {
          errorCount = errorCount + 1;
          ErrorBean errorBean = (ErrorBean) j.next();
          log4jLogger.error("Error: " + errorCount + ") " + errorBean.getErrorMsg() + " / " + errorBean.getRecord());
        }
      }
    }
    if (pfb.isAutoInvoice())
    {
      new Invoice(pfb);
    }
    new EmailReport(pfb);
  }

  public static void writePosFhRecord(PosFileBean pfb)
  {
    StringBuilder sb = new StringBuilder();
    sb.append("insert into sam_pos_fh");
    sb.append("(pos_fh_id,logged_dt,logged_uid,file_extract_end_dt,");
    sb.append("file_extract_start_dt,file_name,file_run_dt,rtlr_ftp_id)values");
    sb.append("(?,sysdate,'POS',?,");
    sb.append("?,?,?,?)");
    OraclePreparedStatement pstmt = null;
    try
    {
      pstmt = (OraclePreparedStatement) pfb.getConnection().prepareStatement(sb.toString());

      pstmt.setString(1, pfb.getPosFhId());
      pstmt.setString(2, pfb.getFileExtractEndDt());
      pstmt.setString(3, pfb.getFileExtractStartDt());
      pstmt.setString(4, pfb.getFileName());
      pstmt.setString(5, pfb.getFileRunDt());
      pstmt.setString(6, pfb.getRtlrFtpId());
      pstmt.executeUpdate();
    }
    catch (Exception e)
    {
      ExceptionLogger.logException(pfb, "PointOfSale", "writePosFhRecord", null, e);
    }
    finally
    {
      TryCleanup.closePreparedStatement(pfb, pstmt);
      log4jLogger.info("File Header Information written to Applix: " + new java.util.Date());
    }
  }

  public static void fetchPosFhId(PosFileBean pfb)
  {
    String tempId = "0";
    int counterFound = 0;
    StringBuilder sb = new StringBuilder();
    sb.append("select last_one + 1 from counter where item =?");
    StringBuilder updateSb = new StringBuilder();
    updateSb.append("update counter set last_one = ? where item = ?");
    OraclePreparedStatement pstmtUpdate = null;
    OraclePreparedStatement pstmt = null;
    OracleResultSet rs = null;
    try
    {
      pstmt = (OraclePreparedStatement) pfb.getConnection().prepareStatement(sb.toString());
      pstmtUpdate = (OraclePreparedStatement) pfb.getConnection().prepareStatement(updateSb.toString());
      pstmt.setString(1, "sam_pos_fh.pos_fh_id");
      rs = (OracleResultSet) pstmt.executeQuery();
      while (rs.next())
      {
        tempId = rs.getString(1);
        counterFound += 1;
      }
      if (counterFound == 1)
      {
        pstmtUpdate.setString(1, tempId);
        pstmtUpdate.setString(2, "sam_pos_fh.pos_fh_id");
        pstmtUpdate.executeUpdate();
        pfb.setPosFhId(tempId);
      }
      else
      {
        ExceptionLogger.logError("Failed to get counter for POS_FH_ID", null, "fetchPosFhId", pfb, null, null);
      }
    }
    catch (Exception e)
    {
      ExceptionLogger.logException(pfb, "ReserveIDsForSales", "fetchPosFhId", null, e);
    }
    finally
    {
      TryCleanup.tryCleanup(pfb, pstmt, rs);
      TryCleanup.tryCleanup(pfb, pstmtUpdate, null);
    }
  }

  public static void moveFileToArchive(PosFileBean pfb, String path, String fileName)
  {
    File processedFile = new File(pfb.getFileName());
    String archivePath = path + "/archive/" + fileName;
    File archiveFile = new File(archivePath);

    if (processedFile.renameTo(archiveFile))
    {
      log4jLogger.info("File " + archivePath + " has been archived.");
    }
    else
    {
      log4jLogger.info("File " + archivePath + " has NOT been archived. Problem occurred during the move.");
    }
  }

  public static void runLevitzMattressExport(PosFileBean pfb)
  {
    log4jLogger.info("Begin runLevitzMattressExport");
    StringBuilder sb = new StringBuilder();
    sb.append("begin sam_mattress_pad_levitz;end;");
    OraclePreparedStatement pstmt = null;
    try
    {
      pstmt = (OraclePreparedStatement) pfb.getConnection().prepareStatement(sb.toString());
      pstmt.execute();
    }
    catch (Exception e)
    {
      ExceptionLogger.logException(pfb, "PointOfSale", "runLevitzMattressExport", null, e);
    }
    finally
    {
      TryCleanup.tryCleanup(pfb, pstmt, null);
    }
    log4jLogger.info("End runLevitzMattressExport");
  }
}
