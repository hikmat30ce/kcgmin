package com.valspar.interfaces.guardsman.pos.utility;

import com.valspar.interfaces.guardsman.pos.beans.PosFileBean;
import oracle.jdbc.*;
import org.apache.log4j.Logger;

public class TryCleanup
{
  private static Logger log4jLogger = Logger.getLogger(TryCleanup.class);

  public static void tryCleanup(PosFileBean pfb, OraclePreparedStatement pstmt, OracleResultSet rs)
  {
    closePreparedStatement(pfb, pstmt);
    closeResultSet(pfb, rs);
  }

  public static void tryCleanup(OraclePreparedStatement pstmt, OracleResultSet rs)
  {
    closePreparedStatement(pstmt);
    closeResultSet(rs);
  }
  
  public static void closePreparedStatement(PosFileBean pfb, OraclePreparedStatement pstmt)
  {
    if (pstmt != null)
    {
      try
      {
        pstmt.close();
        /* Making the prepared statement equal to null makes the 
         * prepared statement eligible for garbage collection.
         */
        pstmt = null;
      }
      catch (Exception e)
      {
        ExceptionLogger.logException(pfb, "TryCleanup", "closePreparedStatement", "N/A", e);
      }
    }
  }

  /* Overloading method to account for queries not using a pfb. At the time this is written, the only
   * query not using a pfb is in the FindErpRtlrNo class.
   * The only difference between this and the original closePreparedStatement is that the catch
   * doesn't use the ExceptionLogger.
   */
  public static void closePreparedStatement(OraclePreparedStatement pstmt)
  {
    if (pstmt != null)
    {
      try
      {
        pstmt.close();
        pstmt = null;
      }
      catch (Exception e)
      {
        log4jLogger.error("TryCleanup: closePreparedStatement -- new method: " + e);
      }
    }
  }

  public static void closeResultSet(PosFileBean pfb, OracleResultSet rs)
  {
    if (rs != null)
    {
      try
      {
        rs.close();
        /* Making the result set equal to null makes the 
         * result set eligible for garbage collection.
         */
        rs = null;
      }
      catch (Exception e)
      {
        ExceptionLogger.logException(pfb, "TryCleanup", "closeResultSet", "N/A", e);
      }
    }
  }
  
  /* Overloading method to account for queries not using a pfb. At the time this is written, the only
   * query not using a pfb is in the FindErpRtlrNo class.
   * The only difference between this and the original closePreparedStatement is that the catch
   * doesn't use the ExceptionLogger.
   */
  public static void closeResultSet(OracleResultSet rs)
  {
    if (rs != null)
    {
      try
      {
        rs.close();
        rs = null;
      }
      catch (Exception e)
      {
        log4jLogger.error("TryCleanup: closeResultSet -- new method: " + e);
      }
    }
  }
}