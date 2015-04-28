package com.valspar.interfaces.guardsman.pos;

import java.util.*;
import oracle.jdbc.*;
import com.valspar.interfaces.guardsman.pos.beans.*;
import com.valspar.interfaces.guardsman.pos.utility.*;

public class ReserveIDsForSales 
{
  public ReserveIDsForSales()
  { 
  }
  
  public ReserveIDsForSales(PosFileBean pfb)
  {    
    int tempSrCount = 0;
    int tempSrItemCount = 0;
    int tempConCount = 0;
    int tempConPhoneCount = 0;
    int tempConAddrCount = 0;
    int tempConSaCount = 0;
    
    Iterator i = pfb.getSalesReceipts().iterator();
    while (i.hasNext())
    {
      SalesReceiptBean srb = (SalesReceiptBean)i.next();
      if (srb.isHasSale() && srb.getSamSrId() == null && srb.getErrors().size() == 0)
      {
        tempSrCount += 1; //reserve an Id for the SR
        if (srb.getSamConId() == null) //Consumer not previously found
        {
          tempConCount += 1;
          tempConAddrCount += 1;
          if (srb.getPhoneHome() != null)
          {
            tempConPhoneCount += 1; //reserve an ID for the Home Phone
          }
          if (srb.getPhoneWork() != null)
          {
            tempConPhoneCount += 1; //reserve an ID for the Work Phone
          }
        }
        
        Iterator j = srb.getSrHeaders().iterator();
        while (j.hasNext())
        {
          SrHeaderBean srhb = (SrHeaderBean)j.next();
          if (srhb.getTransCode().equals("S") || srhb.getTransCode().equals("U"))
          {
            Iterator k = srhb.getSrDetails().iterator();
            while (k.hasNext())
            {
              k.next();
              tempSrItemCount += 1;
            }
          }
        }
      
        Iterator m = srb.getConSAs().iterator(); //Item Con SA's
        while (m.hasNext())
        {
          m.next();
          tempConSaCount += 1;
        }
        if (srb.getSamSaTypeId() != null) //Elite Con SA
        {
          tempConSaCount += 1;
        }
      }
    }    
    if (tempConCount > 0)
    {
      fetchConIDs(pfb,tempConCount);
    }
    if (tempConAddrCount > 0)
    {
      fetchConAddrIDs(pfb,tempConAddrCount);
    }
    if (tempConPhoneCount > 0)
    {
      fetchConPhoneIDs(pfb,tempConPhoneCount);
    }
    if (tempSrCount > 0)
    {
      fetchSRIDs(pfb,tempSrCount);
    }
    if (tempSrItemCount > 0)
    {
      fetchSRItemIDs(pfb,tempSrItemCount);
    }
    if (tempConSaCount > 0)
    {
      fetchConSaIDs(pfb,tempConSaCount);
    }
  }

  
  public static void fetchSRIDs(PosFileBean pfb, int numOfRecords)
  {
    int lastId = 0; 
    int endId = 0;
    int counterFound = 0;
    StringBuilder sb = new StringBuilder();
    sb.append("select last_one from counter where item =?");
    StringBuilder updateSb = new StringBuilder();
    updateSb.append("update counter set last_one = ? where item = ? and last_one = ?");
    OraclePreparedStatement pstmtUpdate = null;
    OraclePreparedStatement pstmt = null;    
    OracleResultSet rs = null;
    try
    {
      pstmt = (OraclePreparedStatement)pfb.getConnection().prepareStatement(sb.toString());
      pstmtUpdate = (OraclePreparedStatement)pfb.getConnection().prepareStatement(updateSb.toString());
      pstmt.setString(1, "sam_con_sls_rcpt.sls_rcpt_id");
      rs = (OracleResultSet)pstmt.executeQuery();        
      while (rs.next()) 
      {
        lastId = rs.getInt(1);
        counterFound +=1;
      }
      if (counterFound ==1)
      {
        endId = lastId + numOfRecords;
        pstmtUpdate.setInt(1, endId);
        pstmtUpdate.setString(2, "sam_con_sls_rcpt.sls_rcpt_id");
        pstmtUpdate.setInt(3, lastId);
        pstmtUpdate.executeUpdate(); //if this went ok the update pfb  
        //This should return 1 if it updated one row.
        pfb.setSrNextId(lastId + 1);
        pfb.setSrLastId(endId);
      }
      else
      {
        ExceptionLogger.logError("Failed to get Counter for SR",null,"SR Counter Fetch",pfb,null,null);
      }
    }
    catch (Exception e)
    {
      ExceptionLogger.logException(pfb, "ReserveIDsForSales","fetchSRIDs",null,e);
    }
    finally
    {
      TryCleanup.tryCleanup(pfb,pstmt,rs);
      TryCleanup.tryCleanup(pfb,pstmtUpdate,null);
    }
  }

  public static void fetchSRItemIDs(PosFileBean pfb, int numOfRecords)
  {
    int lastId = 0; 
    int endId = 0;
    int counterFound = 0;
    StringBuilder sb = new StringBuilder();
    sb.append("select last_one from counter where item =?");
    StringBuilder updateSb = new StringBuilder();
    updateSb.append("update counter set last_one = ? where item = ? and last_one = ?");
    OraclePreparedStatement pstmtUpdate = null;
    OraclePreparedStatement pstmt = null;    
    OracleResultSet rs = null;    
    try
    {
      pstmt = (OraclePreparedStatement)pfb.getConnection().prepareStatement(sb.toString());
      pstmtUpdate = (OraclePreparedStatement)pfb.getConnection().prepareStatement(updateSb.toString());
      pstmt.setString(1, "sam_con_sls_rcpt_item.sls_rcpt_item_id");
      rs = (OracleResultSet)pstmt.executeQuery();        
      while (rs.next()) 
      {
        lastId = rs.getInt(1);
        counterFound +=1;
      }
      if (counterFound ==1)
      {
        endId = lastId + numOfRecords;
        pstmtUpdate.setInt(1, endId);
        pstmtUpdate.setString(2, "sam_con_sls_rcpt_item.sls_rcpt_item_id");
        pstmtUpdate.setInt(3, lastId);
        pstmtUpdate.executeUpdate(); //if this went ok the update pfb
        pfb.setSrItemNextId(lastId + 1);
        pfb.setSrItemLastId(endId);
      }
      else
      {
        ExceptionLogger.logError("Failed to get Counter for SR Items",null,"SR Item Counter Fetch",pfb,null,null);
      } 
    }
    catch (Exception e)
    {
      ExceptionLogger.logException(pfb, "ReserveIDsForSales","fetchSRItemIDs",null,e);
    }
    finally
    {
      TryCleanup.tryCleanup(pfb,pstmt,rs);
      TryCleanup.tryCleanup(pfb,pstmtUpdate,null);
    }
  }  
  
  public static void fetchConIDs(PosFileBean pfb, int numOfRecords)
  {
    int lastId = 0; 
    int endId = 0;
    int counterFound = 0;
    StringBuilder sb = new StringBuilder();
    sb.append("select last_one from counter where item =?");
    StringBuilder updateSb = new StringBuilder();
    updateSb.append("update counter set last_one = ? where item = ? and last_one = ?");
    OraclePreparedStatement pstmtUpdate = null;
    OraclePreparedStatement pstmt = null;    
    OracleResultSet rs = null;
    try
    {
      pstmt = (OraclePreparedStatement)pfb.getConnection().prepareStatement(sb.toString());
      pstmtUpdate = (OraclePreparedStatement)pfb.getConnection().prepareStatement(updateSb.toString());
      pstmt.setString(1, "sam_con.con_id");
      rs = (OracleResultSet)pstmt.executeQuery();        
      while (rs.next()) 
      {
        lastId = rs.getInt(1);
        counterFound +=1;
      }
      if (counterFound ==1)
      {
        endId = lastId + numOfRecords;
        pstmtUpdate.setInt(1, endId);
        pstmtUpdate.setString(2, "sam_con.con_id");
        pstmtUpdate.setInt(3, lastId);
        pstmtUpdate.executeUpdate(); 
        pfb.setConNextId(lastId + 1);
        pfb.setConLastId(endId);
      }
      else
      {
        ExceptionLogger.logError("Failed to get counter for Consumer",null,"Consumer Counter Fetch",pfb,null,null);
      }
    }
    catch (Exception e)
    {
      ExceptionLogger.logException(pfb, "ReserveIDsForSales","fetchConIDs",null,e);
    }
    finally
    {
      TryCleanup.tryCleanup(pfb,pstmt,rs);
      TryCleanup.tryCleanup(pfb,pstmtUpdate,null);
    }
  }  
  
  public static void fetchConAddrIDs(PosFileBean pfb, int numOfRecords)
  {
    int lastId = 0; 
    int endId = 0;
    int counterFound = 0;
    StringBuilder sb = new StringBuilder();
    sb.append("select last_one from counter where item =?");
    StringBuilder updateSb = new StringBuilder();
    updateSb.append("update counter set last_one = ? where item = ? and last_one = ?");
    OraclePreparedStatement pstmtUpdate = null;
    OraclePreparedStatement pstmt = null;    
    OracleResultSet rs = null;
    try
    {
      pstmt = (OraclePreparedStatement)pfb.getConnection().prepareStatement(sb.toString());
      pstmtUpdate = (OraclePreparedStatement)pfb.getConnection().prepareStatement(updateSb.toString());
      pstmt.setString(1, "sam_con_addr.con_addr_id");
      rs = (OracleResultSet)pstmt.executeQuery();        
      while (rs.next()) 
      {
        lastId = rs.getInt(1);
        counterFound +=1;
      }
      if (counterFound ==1)
      {
        endId = lastId + numOfRecords;
        pstmtUpdate.setInt(1, endId);
        pstmtUpdate.setString(2, "sam_con_addr.con_addr_id");
        pstmtUpdate.setInt(3, lastId);
        pstmtUpdate.executeUpdate(); 
        pfb.setConAddrNextId(lastId + 1);
        pfb.setConAddrLastId(endId);
      }
      else
      {
        ExceptionLogger.logError("Failed to get counter for Consumer Address",null,"Consumer Address Counter Fetch",pfb,null,null);
      }
    }
    catch (Exception e)
    {
      ExceptionLogger.logException(pfb, "ReserveIDsForSales","fetchConAddrIDs",null,e);
    }    
    finally
    {
      TryCleanup.tryCleanup(pfb,pstmt,rs);
      TryCleanup.tryCleanup(pfb,pstmtUpdate,null);
    }
  }
 
  public static void fetchConPhoneIDs(PosFileBean pfb, int numOfRecords)
  {
    int lastId = 0; 
    int endId = 0;
    int counterFound = 0;
    StringBuilder sb = new StringBuilder();
    sb.append("select last_one from counter where item =?");
    StringBuilder updateSb = new StringBuilder();
    updateSb.append("update counter set last_one = ? where item = ? and last_one = ?");
    OraclePreparedStatement pstmtUpdate = null;
    OraclePreparedStatement pstmt = null;    
    OracleResultSet rs = null;
    try
    {
      pstmt = (OraclePreparedStatement)pfb.getConnection().prepareStatement(sb.toString());
      pstmtUpdate = (OraclePreparedStatement)pfb.getConnection().prepareStatement(updateSb.toString());
      pstmt.setString(1, "sam_con_phone.con_phone_id");
      rs = (OracleResultSet)pstmt.executeQuery();        
      while (rs.next()) 
      {
        lastId = rs.getInt(1);
        counterFound +=1;
      }
      if (counterFound ==1)
      {
        endId = lastId + numOfRecords;
        pstmtUpdate.setInt(1, endId);
        pstmtUpdate.setString(2, "sam_con_phone.con_phone_id");
        pstmtUpdate.setInt(3, lastId);
        pstmtUpdate.executeUpdate(); 
        pfb.setConPhoneNextId(lastId + 1);
        pfb.setConPhoneLastId(endId);
      }
      else
      {
        ExceptionLogger.logError("Failed to get counter for Consumer Phone",null,"Consumer Phone Counter Fetch",pfb,null,null);
      }
    }
    catch (Exception e)
    {
      ExceptionLogger.logException(pfb, "ReserveIDsForSales","fetchConPhoneIDs",null,e);
    }    
    finally
    {
      TryCleanup.tryCleanup(pfb,pstmt,rs);
      TryCleanup.tryCleanup(pfb,pstmtUpdate,null);
    }  
  }
 
  public static void fetchConSaIDs(PosFileBean pfb, int numOfRecords)
  {
    int lastId = 0; 
    int endId = 0;
    int counterFound = 0;
    StringBuilder sb = new StringBuilder();
    sb.append("select last_one from counter where item =?");
    StringBuilder updateSb = new StringBuilder();
    updateSb.append("update counter set last_one = ? where item = ? and last_one = ?");
    OraclePreparedStatement pstmtUpdate = null;
    OraclePreparedStatement pstmt = null;    
    OracleResultSet rs = null;    
    try
    {
      pstmt = (OraclePreparedStatement)pfb.getConnection().prepareStatement(sb.toString());
      pstmtUpdate = (OraclePreparedStatement)pfb.getConnection().prepareStatement(updateSb.toString());
      pstmt.setString(1, "sam_con_sa.con_sa_id");
      rs = (OracleResultSet)pstmt.executeQuery();        
      while (rs.next()) 
      {
        lastId = rs.getInt(1);
        counterFound +=1;
      }
      if (counterFound ==1)
      {
        endId = lastId + numOfRecords;
        pstmtUpdate.setInt(1, endId);
        pstmtUpdate.setString(2, "sam_con_sa.con_sa_id");
        pstmtUpdate.setInt(3, lastId);
        pstmtUpdate.executeUpdate(); 
        pfb.setConSANextId(lastId + 1);
        pfb.setConSALastId(endId);
      }
      else
      {
        ExceptionLogger.logError("Failed to get counter for Consumer SA",null,"Consumer SA Counter Fetch",pfb,null,null);
      }
    }
    catch (Exception e)
    {
      ExceptionLogger.logException(pfb, "ReserveIDsForSales","fetchConSaIDs",null,e);
    }    
    finally
    {
      TryCleanup.tryCleanup(pfb,pstmt,rs);
      TryCleanup.tryCleanup(pfb,pstmtUpdate,null);
    }  
  }
}