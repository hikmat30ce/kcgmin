package com.valspar.interfaces.wercs.densitysync.program;

import com.valspar.interfaces.common.BaseInterface;
import com.valspar.interfaces.common.beans.ConnectionAccessBean;
import com.valspar.interfaces.common.enums.DataSource;
import com.valspar.interfaces.common.utils.*;
import com.valspar.interfaces.wercs.common.utils.WercsUtility;
import com.valspar.interfaces.wercs.densitysync.beans.WercsItemBean;
import java.sql.*;
import java.util.*;
import oracle.jdbc.OracleConnection;
import oracle.jdbc.OraclePreparedStatement;
import org.apache.log4j.Logger;

public class DensitySyncInterface extends BaseInterface
{
  private static Logger log4jLogger = Logger.getLogger(DensitySyncInterface.class);

  public DensitySyncInterface()
  {
  }

  public void execute()
  {
    OracleConnection wercsConn = null;
    try
    {
      wercsConn = (OracleConnection) ConnectionAccessBean.getConnection(DataSource.WERCS);
      List<WercsItemBean> ar = buildWercsItemBeans(wercsConn);
      checkItemsWPG(ar, wercsConn);
      if (!ar.isEmpty())
      {
        for (DataSource datasource: CommonUtility.getERPDataSourceList6X()) //TODO
        {
          addLotsCNV(ar, datasource);
        }
        updateVcaDensitySyncQueue(ar, wercsConn);
      }
    }
    catch (Exception e)
    {
      log4jLogger.error(e);
    }
    finally
    {
      JDBCUtil.close(wercsConn);
    }
  }

  private List<WercsItemBean> buildWercsItemBeans(OracleConnection wercsConn)
  {
    List<WercsItemBean> ar = new ArrayList<WercsItemBean>();
    Statement stmt = null;
    ResultSet rs = null;
    try
    {
      StringBuilder sql = new StringBuilder();
      sql.append("SELECT A.ID, A.PRODUCT,  ");
      sql.append("VCA_CALC_DENSITY_FROM_FILL_PCT@TOFM(B.F_ALIAS, A.DENSLB),  ");
      sql.append("VCA_CALC_DENSITY_FROM_FILL_PCT@TOFM(B.F_ALIAS, A.DENKGL),  ");
      sql.append("B.F_ALIAS, B.F_ALIAS_NAME, ");
      sql.append("GET_WERCS_TEXT_CODE(B.F_ALIAS,'BUSGP') as BUSGP ");
      sql.append("FROM  VCA_DENSITY_SYNC_QUEUE A, T_PRODUCT_ALIAS_NAMES B, T_PROD_TEXT C  ");
      sql.append("WHERE A.PRODUCT = B.F_PRODUCT  ");
      sql.append("AND   A.PRODUCT = C.F_PRODUCT(+)  ");
      sql.append("AND   C.F_TEXT_CODE(+) = 'COSTCL02'  ");
      sql.append("AND   A.DATE_PROCESSED IS NULL  ");
      sql.append("AND   A.COMMENTS IS NULL  ");
      sql.append("ORDER BY A.DATE_ADDED  ");

      log4jLogger.info("WERCS Connection = " + ConnectionUtility.buildDatabaseName(wercsConn));
      stmt = wercsConn.createStatement();
      rs = stmt.executeQuery(sql.toString());
      while (rs.next())
      {
        WercsItemBean wb = new WercsItemBean();
        wb.setId(rs.getString(1));
        wb.setProduct(rs.getString(2));
        wb.setDensLb(rs.getString(3));
        wb.setDenKgl(rs.getString(4));
        wb.setAlias(rs.getString(5));
        wb.setAliasName(rs.getString(6));
        wb.setBusinessGroup(rs.getString(7));
        ar.add(wb);
      }
      log4jLogger.info("We have " + ar.size() + " to be processed");
    }
    catch (Exception e)
    {
      log4jLogger.error(e);
    }
    finally
    {
      JDBCUtil.close(stmt, rs);
    }
    return ar;
  }

  public void checkItemsWPG(List<WercsItemBean> wercsItemBeanList, OracleConnection wercsConn)
  {
    log4jLogger.info("Starting to verify that every item has DENSLB and DENKGL in WERCS.");
    Iterator i = wercsItemBeanList.iterator();
    while (i.hasNext())
    {
      WercsItemBean wb = (WercsItemBean) i.next();
      try
      {
        if (!WercsUtility.doesItemsWPGExist(wb.getProduct()))
        {
          log4jLogger.error("Error in checkItemsWPG " + wb.getProduct() + " does not have DENSLB and DENKGL in WERCS so we will not add item.");
          EmailBean.emailMessage(wb.getProduct() + " does not have DENSLB and/or DENKGL in Wercs", wb.getProduct() + " does not have DENSLB and/or DENKGL in Wercs so it failed in the density sync interface.  Please add these datacodes to Wercs and it will automatically be processed the next time the density sync interface runs.", getNotificationEmail()); // Email addresses
          i.remove();
        }
      }
      catch (Exception e)
      {
        log4jLogger.error("DB Name is " + ConnectionUtility.buildDatabaseName(wercsConn) + " item=" + wb.getProduct(), e);
      }
    }
    log4jLogger.info("Done verifying that every item has DENSLB and DENKGL");
  }

  private void addLotsCNV(List<WercsItemBean> wercsItemBeanList, DataSource datasource)
  {
    CallableStatement cstmt = null;
    OracleConnection conn = null;
    try
    {
      conn = (OracleConnection) ConnectionAccessBean.getConnection(datasource);
      log4jLogger.info("Starting to add lots cnv for " + ConnectionUtility.buildDatabaseName(conn));
      cstmt = conn.prepareCall("{call VCA_IC_ITEM_CNV_WRAPPER_6X(?,?,?,?,?,?)}");
      for (WercsItemBean wb: wercsItemBeanList)
      {
        try
        {
          cstmt.clearParameters();
          cstmt.setString(1, datasource.getLogUser());
          cstmt.setString(2, wb.getAlias());
          if (datasource.getInstanceCodeOf11i().equalsIgnoreCase("NA"))
          {
            cstmt.setString(3, wb.getDensLb());
            cstmt.setString(4, "LB");
          }
          else
          {
            cstmt.setString(3, wb.getDenKgl());
            cstmt.setString(4, "KG");
          }
          cstmt.setString(5, wb.getBusinessGroup());
          cstmt.registerOutParameter(6, Types.VARCHAR);
          cstmt.execute();
          if (cstmt.getString(6) != null)
          {
            log4jLogger.info("Message in DensitySyncInterface.addLotsCNV() " + cstmt.getString(6));
          }
        }
        catch (Exception e)
        {
          log4jLogger.error("item=" + wb.getProduct(), e);
        }
      }
      log4jLogger.info("Done with add lots cnv for " + ConnectionUtility.buildDatabaseName(conn));
    }
    catch (Exception e)
    {
      log4jLogger.error(e);
    }
    finally
    {
      JDBCUtil.close(cstmt);
      JDBCUtil.close(conn);
    }
  }

  private void updateVcaDensitySyncQueue(List<WercsItemBean> wercsItemBeanList, OracleConnection wercsConn)
  {
    OraclePreparedStatement pst = null;
    try
    {
      log4jLogger.info("Starting to update Wercs statuses");
      pst = (OraclePreparedStatement) wercsConn.prepareStatement("UPDATE VCA_DENSITY_SYNC_QUEUE SET DATE_PROCESSED = SYSDATE WHERE ID = :ID ");
      for (WercsItemBean wb: wercsItemBeanList)
      {
        try
        {
          pst.clearParameters();
          pst.setStringAtName("ID", wb.getId());
          pst.executeUpdate();
        }
        catch (Exception e)
        {
          log4jLogger.error(e);
        }
      }
      log4jLogger.info("Done updating Wercs statuses");
    }
    catch (Exception e)
    {
      log4jLogger.error(e);
    }
    finally
    {
      JDBCUtil.close(pst);
    }
  }
}
