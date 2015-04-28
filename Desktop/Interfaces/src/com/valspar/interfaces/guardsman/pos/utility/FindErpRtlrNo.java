package com.valspar.interfaces.guardsman.pos.utility;

import com.valspar.interfaces.guardsman.pos.beans.ConnectionBean;
import oracle.jdbc.OracleConnection;
import oracle.jdbc.OraclePreparedStatement;
import oracle.jdbc.OracleResultSet;

public class FindErpRtlrNo
{
  public static String findErpRtlrNo(String RetailerId)
  {
    String correctedRetailerNo = "";
    ConnectionBean cb = new ConnectionBean();
    OracleConnection conn = null;

    try
    {
      conn = cb.openConnection();
      if (queryLegacyErpCount(RetailerId, conn) > 0)
      {
        correctedRetailerNo = queryErpRtlrNo(RetailerId, conn);
      }
      else
      {
        correctedRetailerNo = RetailerId;
      }
    }
    finally
    {
      cb.closeConnection(conn);
    }

    return correctedRetailerNo;
  }

  public static String findBillTo(String RetailerNo, String StoreNo)
  {
    String retailerBillTo = "";
    ConnectionBean cb = new ConnectionBean();
    OracleConnection conn = null;

    try
    {
      conn = cb.openConnection();
      retailerBillTo = queryBillTo(RetailerNo, StoreNo, conn);
    }
    finally
    {
      cb.closeConnection(conn);
    }

    return retailerBillTo;
  }

  private static int queryLegacyErpCount(String LegacyerpRtlrNo, OracleConnection conn)
  {
    int retailerCount = 0;
    StringBuilder sb = new StringBuilder();
    sb.append("select count(new_cust) ");
    sb.append("from i_cust_xref ");
    sb.append("where legacy_cust = :erpRtlrNo ");
    OraclePreparedStatement pstmt = null;
    OracleResultSet rs = null;
    try
    {
      pstmt = (OraclePreparedStatement) conn.prepareStatement(sb.toString());
      pstmt.setStringAtName("erpRtlrNo", LegacyerpRtlrNo);
      rs = (OracleResultSet) pstmt.executeQuery();
      while (rs.next())
      {
        retailerCount = rs.getInt(1);
      }
    }
    catch (Exception e)
    {
      System.out.println("Exception on FindErpRtlrNo.queryLegacyErpCount " + LegacyerpRtlrNo);
    }
    finally
    {
      TryCleanup.tryCleanup(pstmt, rs);
    }
    return retailerCount;
  }

  public static String queryErpRtlrNo(String legacyErpRtlrNo, OracleConnection conn)
  {
    String oracleRtlrNo = "";
    StringBuilder sb = new StringBuilder();
    sb.append("select max(new_cust) ");
    sb.append("from i_cust_xref ");
    sb.append("where legacy_cust = :erpRtlrNo ");
    OraclePreparedStatement pstmt = null;
    OracleResultSet rs = null;
    try
    {
      pstmt = (OraclePreparedStatement) conn.prepareStatement(sb.toString());
      pstmt.setStringAtName("erpRtlrNo", legacyErpRtlrNo);
      rs = (OracleResultSet) pstmt.executeQuery();
      while (rs.next())
      {
        oracleRtlrNo = rs.getString(1);
      }
    }
    catch (Exception e)
    {
      System.out.println("Exception on FindErpRtlrNo.queryerpRtlrNo, erpRtlrNo: " + legacyErpRtlrNo + " " + e);

    }
    finally
    {
      TryCleanup.tryCleanup(pstmt, rs);
    }
    return oracleRtlrNo;
  }
  
  private static String queryBillTo(String erpRtlrNo, String storeNo, OracleConnection conn)
  {
    String oracleBillTo = "";
    StringBuilder sb = new StringBuilder();
    sb.append("select ((case ");
    sb.append("         when erp_bill_to is null ");
    sb.append("         then erp_rtlr_dseq ");
    sb.append("         else erp_bill_to ");
    sb.append("         end))");
    sb.append("  from sam_rtlr_addr ");
    sb.append(" where erp_rtlr_no = :erpRtlrNo ");
    sb.append("   and rtlr_store_no = :storeNo ");
    OraclePreparedStatement pstmt = null;
    OracleResultSet rs = null;
    try
    {
      pstmt = (OraclePreparedStatement) conn.prepareStatement(sb.toString());
      pstmt.setStringAtName("erpRtlrNo", erpRtlrNo);
      pstmt.setStringAtName("storeNo", storeNo);
      rs = (OracleResultSet) pstmt.executeQuery();
      while (rs.next())
      {
        oracleBillTo = rs.getString(1);
      }
    }
    catch (Exception e)
    {
      System.out.println("Exception on FindErpRtlrNoAndBillTo.queryBillTo erpRtlrNo: " + erpRtlrNo + " StoreNo: " + storeNo + e);
    }
    finally
    {
      TryCleanup.tryCleanup(pstmt, rs);
    }
    return oracleBillTo;
  }
}
