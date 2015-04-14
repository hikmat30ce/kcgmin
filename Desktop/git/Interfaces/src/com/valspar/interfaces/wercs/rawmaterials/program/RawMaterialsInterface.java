package com.valspar.interfaces.wercs.rawmaterials.program;

import com.valspar.interfaces.common.BaseInterface;
import com.valspar.interfaces.common.beans.ConnectionAccessBean;
import com.valspar.interfaces.common.enums.DataSource;
import com.valspar.interfaces.common.utils.*;
import com.valspar.interfaces.wercs.common.utils.WercsUtility;
import com.valspar.interfaces.wercs.rawmaterials.beans.WercsItemBean;
import java.sql.*;
import java.util.*;
import oracle.jdbc.OracleConnection;
import oracle.jdbc.OraclePreparedStatement;
import org.apache.log4j.Logger;

public class RawMaterialsInterface extends BaseInterface
{
  private OracleConnection wercsConn = null;
  private static Logger log4jLogger = Logger.getLogger(RawMaterialsInterface.class);

  public RawMaterialsInterface()
  {
  }

  public void execute()
  {
    try
    {
      setWercsConn((OracleConnection) ConnectionAccessBean.getConnection(DataSource.WERCS));
      ArrayList<WercsItemBean> ar = buildWercsItemBeans();
      checkItemsWPG(ar);
      for (DataSource datasource: CommonUtility.getERPDataSourceList())
      {
        addItems(ar, datasource);
        addLots(ar, datasource, "0");
        addLotsCNV(ar, datasource);
      }
      updateVcaRmQueue(ar);
    }
    catch (Exception e)
    {
      log4jLogger.error(e);
    }
    finally
    {
      JDBCUtil.close(getWercsConn());
    }
  }

  private ArrayList<WercsItemBean> buildWercsItemBeans()
  {
    ArrayList<WercsItemBean> ar = new ArrayList<WercsItemBean>();
    Statement stmt = null;
    ResultSet rs = null;
    try
    {
      StringBuilder sql = new StringBuilder();
      sql.append("SELECT A.ID, B.F_PRODUCT, B.F_ALIAS_NAME, DECODE(C.F_TEXT_CODE, 'COSTCL02', 'DUMMY_BUYOUT', 'DUMMY') ");
      sql.append("FROM  VCA_RM_QUEUE A, T_PRODUCT_ALIAS_NAMES B, T_PROD_TEXT C ");
      sql.append("WHERE A.PRODUCT = B.F_ALIAS ");
      sql.append("AND   B.F_PRODUCT = C.F_PRODUCT(+) ");
      sql.append("AND   C.F_TEXT_CODE(+) = 'COSTCL02' ");
      sql.append("AND   A.DATE_PROCESSED IS NULL ");
      sql.append("AND   A.COMMENTS IS NULL ");
      sql.append("ORDER BY A.DATE_ADDED ");

      log4jLogger.info("WERCS Connection = " + ConnectionUtility.buildDatabaseName(getWercsConn()));
      stmt = getWercsConn().createStatement();
      rs = stmt.executeQuery(sql.toString());
      while (rs.next())
      {
        WercsItemBean wb = new WercsItemBean();
        wb.setId(rs.getString(1));
        wb.setProduct(rs.getString(2));
        wb.setAliasName(rs.getString(3));
        wb.setDummyType(rs.getString(4));
        ar.add(wb);
      }
      log4jLogger.info("We have " + ar.size() + " to process");
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

  public void checkItemsWPG(ArrayList<WercsItemBean> ar)
  {
    log4jLogger.info("Starting to verify that every item has DENSLB and DENKGL in WERCS.");
    Iterator i = ar.iterator();
    while (i.hasNext())
    {
      WercsItemBean wb = (WercsItemBean) i.next();
      try
      {
        if (!WercsUtility.doesItemsWPGExist(wb.getProduct()))
        {
          log4jLogger.error("Error in checkItemsWPG " + wb.getProduct() + " does not have DENSLB and DENKGL in WERCS so we will not add item.");
          EmailBean.emailMessage(wb.getProduct() + " does not have DENSLB and/or DENKGL in Wercs", wb.getProduct() + " does not have DENSLB and/or DENKGL in Wercs so it failed in the raw materials interface.  Please add these datacodes to Wercs and it will automatically be processed the next time the raw materials interface runs.", getNotificationEmail()); // Email addresses
          i.remove();
        }
      }
      catch (Exception e)
      {
        log4jLogger.error("DB Name is " + ConnectionUtility.buildDatabaseName(getWercsConn()) + " Product: " + wb.getProduct(), e);
      }
    }
    log4jLogger.info("Done verifying that every item has DENSLB and DENKGL");
  }

  private void addItems(ArrayList<WercsItemBean> ar, DataSource datasource)
  {
    CallableStatement cstmt = null;
    OracleConnection conn = null;
    try
    {
      conn = (OracleConnection) ConnectionAccessBean.getConnection(datasource);
      log4jLogger.info("Starting to add items for " + ConnectionUtility.buildDatabaseName(conn));
      cstmt = conn.prepareCall("{call VCA_ITEM_CREATE_WRAPPER(?,?,?,?,?)}");
      for (WercsItemBean wb: ar)
      {
        cstmt.clearParameters();
        cstmt.setString(1, wb.getProduct());
        cstmt.setString(2, wb.getAliasName());
        cstmt.setString(3, datasource.getLogUser());
        cstmt.setString(4, wb.getDummyType());
        cstmt.registerOutParameter(5, Types.VARCHAR);
        cstmt.execute();
        if (cstmt.getString(5) != null)
        {
          log4jLogger.info("Message in RawMaterialsInterface.addItems() " + cstmt.getString(5));
        }
      }
      log4jLogger.info("Done adding items for " + ConnectionUtility.buildDatabaseName(conn));
    }
    catch (Exception e)
    {
      log4jLogger.error("DB Name is " + datasource.getDataSourceLabel(), e);
    }
    finally
    {
      JDBCUtil.close(cstmt);
      JDBCUtil.close(conn);
    }
  }

  private void addLots(ArrayList<WercsItemBean> ar, DataSource datasource, String lotType)
  {
    CallableStatement cstmt = null;
    OracleConnection conn = null;
    try
    {
      conn = (OracleConnection) ConnectionAccessBean.getConnection(datasource);
      log4jLogger.info("Starting to add lots for " + ConnectionUtility.buildDatabaseName(conn));
      cstmt = conn.prepareCall("{call VCA_LOT_CREATE_WRAPPER(?,?,?,?)}");
      for (WercsItemBean wb: ar)
      {
        cstmt.setString(1, datasource.getLogUser());
        cstmt.setString(2, lotType);
        cstmt.setString(3, wb.getProduct());
        cstmt.registerOutParameter(4, Types.VARCHAR);
        cstmt.execute();
        if (cstmt.getString(4) != null)
        {
          log4jLogger.info("Message in RawMaterialsInterface.addLots() " + cstmt.getString(4));
        }
      }
      log4jLogger.info("Done adding lots for " + ConnectionUtility.buildDatabaseName(conn));
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

  private void addLotsCNV(ArrayList<WercsItemBean> ar, DataSource datasource)
  {
    CallableStatement cstmt = null;
    OracleConnection conn = null;
    try
    {
      conn = (OracleConnection) ConnectionAccessBean.getConnection(datasource);
      log4jLogger.info("Starting to add lots cnv for " + ConnectionUtility.buildDatabaseName(conn));
      cstmt = conn.prepareCall("{call VCA_ITEM_LOT_CONV_WRAPPER(?,?,?)}");
      for (WercsItemBean wb: ar)
      {
        cstmt.setString(1, datasource.getLogUser());
        cstmt.setString(2, wb.getProduct());
        cstmt.registerOutParameter(3, Types.VARCHAR);
        cstmt.execute();
        if (cstmt.getString(3) != null)
        {
          log4jLogger.info("Message in RawMaterialsInterface.addLotsCNV() " + cstmt.getString(3));
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

  private void updateVcaRmQueue(ArrayList<WercsItemBean> ar)
  {
    log4jLogger.info("Starting to update Wercs statuses");
    StringBuilder sql = new StringBuilder();
    sql.append("UPDATE VCA_RM_QUEUE SET DATE_PROCESSED = SYSDATE WHERE ID = :ID ");
    for (WercsItemBean wb: ar)
    {
      OraclePreparedStatement pst = null;
      try
      {
        pst = (OraclePreparedStatement) getWercsConn().prepareStatement(sql.toString());
        pst.setStringAtName("ID", wb.getId());
        pst.executeUpdate();
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
    log4jLogger.info("Done updating Wercs statuses");
  }

  public void setWercsConn(OracleConnection wercsConn)
  {
    this.wercsConn = wercsConn;
  }

  public OracleConnection getWercsConn()
  {
    return wercsConn;
  }
}
