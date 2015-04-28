package com.valspar.interfaces.regulatory.rawmaterials.program;

import com.valspar.interfaces.common.BaseInterface;
import com.valspar.interfaces.common.beans.ConnectionAccessBean;
import com.valspar.interfaces.common.enums.DataSource;
import com.valspar.interfaces.common.utils.*;
import com.valspar.interfaces.regulatory.rawmaterials.beans.WercsItemBean;
import java.sql.*;
import java.util.*;
import oracle.jdbc.OracleConnection;
import org.apache.log4j.Logger;

public class RawMaterialsInterface extends BaseInterface
{
  private OracleConnection regulatoryConn = null;
  private ArrayList<DataSource> toERPDatasourceList = new ArrayList<DataSource>();
  private static Logger log4jLogger = Logger.getLogger(RawMaterialsInterface.class);

  public RawMaterialsInterface()
  {
  }

  public void execute()
  {
    try
    {
      regulatoryConn = (OracleConnection) ConnectionAccessBean.getConnection(DataSource.REGULATORY);
      toERPDatasourceList.add(DataSource.NORTHAMERICAN);
      toERPDatasourceList.add(DataSource.ASIAPAC);
      toERPDatasourceList.add(DataSource.EMEAI);
      ArrayList ar = buildWercsItemBeans();
      checkItemsWPG(ar);
      for (DataSource datasource: toERPDatasourceList)
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
      JDBCUtil.close(regulatoryConn);
    }
  }

  private ArrayList buildWercsItemBeans()
  {
    ArrayList ar = new ArrayList();
    Statement stmt = null;
    ResultSet rs = null;
    try
    {
      StringBuffer sql = new StringBuffer();
      sql.append("SELECT A.ID, B.F_PRODUCT, B.F_ALIAS_NAME, DECODE(C.F_TEXT_CODE, 'COSTCL02', 'DUMMY_BUYOUT', 'DUMMY') ");
      sql.append("FROM  VCA_RM_QUEUE A, T_PRODUCT_ALIAS_NAMES B, T_PROD_TEXT C ");
      sql.append("WHERE A.PRODUCT = B.F_ALIAS ");
      sql.append("AND   B.F_PRODUCT = C.F_PRODUCT(+) ");
      sql.append("AND   C.F_TEXT_CODE(+) = 'COSTCL02' ");
      sql.append("AND   A.DATE_PROCESSED IS NULL ");
      sql.append("AND   A.COMMENTS IS NULL ");
      sql.append("ORDER BY A.DATE_ADDED ");

      log4jLogger.info("WERCS Connection = " + ConnectionUtility.buildDatabaseName(regulatoryConn));
      stmt = regulatoryConn.createStatement();
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

  public void checkItemsWPG(ArrayList ar)
  {
    log4jLogger.info("Starting to verify that every item has DENITY and DENSKG in WERCS.");
    Iterator i = ar.iterator();
    while (i.hasNext())
    {
      WercsItemBean wb = (WercsItemBean) i.next();
      Statement st = null;
      ResultSet rs = null;
      try
      {
        StringBuffer sql = new StringBuffer();
        sql.append("SELECT COUNT(*) ");
        sql.append("FROM T_PROD_DATA ");
        sql.append("WHERE F_PRODUCT IN ('");
        sql.append(wb.getProduct());
        sql.append("') ");
        sql.append("AND F_DATA_CODE IN('DENSITY', 'DENSKG')");
        st = regulatoryConn.createStatement();
        rs = st.executeQuery(sql.toString());
        if (rs.next())
        {
          String num = rs.getString(1);
          if (!num.equalsIgnoreCase("2"))
          {
            log4jLogger.error("Error in checkItemsWPG " + wb.getProduct() + " does not have DENSITY and DENSKG in WERCS so we will not add item.");
            EmailBean.emailMessage(wb.getProduct() + " does not have DENSITY and/or DENSKG in Wercs", wb.getProduct() + " does not have DENSITY and/or DENSKG in Wercs so it failed in the raw materials interface.  Please add these datacodes to Wercs and it will automatically be processed the next time the raw materials interface runs.", getNotificationEmail()); // Email addresses
            i.remove();
          }
        }
      }
      catch (Exception e)
      {
        log4jLogger.error("DB Name is " + ConnectionUtility.buildDatabaseName(regulatoryConn) + " Product: " + wb.getProduct(), e);
      }
      finally
      {
        JDBCUtil.close(st, rs);
      }
    }
    log4jLogger.info("Done verifying that every item has DENITY and DENSKG");
  }

  private void addItems(ArrayList ar, DataSource datasource)
  {
    CallableStatement cstmt = null;
    OracleConnection conn = null;
    try
    {
      conn = (OracleConnection) ConnectionAccessBean.getConnection(datasource);
      log4jLogger.info("Starting to add items for " + ConnectionUtility.buildDatabaseName(conn));
      cstmt = conn.prepareCall("{call VCA_ITEM_CREATE_WRAPPER(?,?,?,?,?)}");
      Iterator i = ar.iterator();
      while (i.hasNext())
      {
        WercsItemBean wb = (WercsItemBean) i.next();
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

  private void addLots(ArrayList ar, DataSource datasource, String lotType)
  {
    CallableStatement cstmt = null;
    OracleConnection conn = null;
    try
    {
      conn = (OracleConnection) ConnectionAccessBean.getConnection(datasource);
      log4jLogger.info("Starting to add lots for " + ConnectionUtility.buildDatabaseName(conn));
      cstmt =conn.prepareCall("{call VCA_LOT_CREATE_WRAPPER(?,?,?,?)}");
      Iterator i = ar.iterator();
      while (i.hasNext())
      {
        WercsItemBean wb = (WercsItemBean) i.next();
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

  private void addLotsCNV(ArrayList ar, DataSource datasource)
  {
    CallableStatement cstmt = null;
    OracleConnection conn = null;
    try
    {
      conn = (OracleConnection) ConnectionAccessBean.getConnection(datasource);
      log4jLogger.info("Starting to add lots cnv for " + ConnectionUtility.buildDatabaseName(conn));
      cstmt = conn.prepareCall("{call VCA_ITEM_LOT_CONV_WRAPPER(?,?,?)}");
      Iterator i = ar.iterator();
      while (i.hasNext())
      {
        WercsItemBean wb = (WercsItemBean) i.next();
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

  private void updateVcaRmQueue(ArrayList ar)
  {
    log4jLogger.info("Starting to update Wercs statuses");
    Iterator i = ar.iterator();
    while (i.hasNext())
    {
      Statement stmt = null;
      try
      {
        WercsItemBean wb = (WercsItemBean) i.next();
        stmt = regulatoryConn.createStatement();
        StringBuffer sql = new StringBuffer();
        sql.append("UPDATE VCA_RM_QUEUE SET DATE_PROCESSED = SYSDATE WHERE ID = ");
        sql.append(wb.getId());
        stmt.executeUpdate(sql.toString());
      }
      catch (SQLException e)
      {
        log4jLogger.error(e);
      }
      finally
      {
        JDBCUtil.close(stmt);
      }
    }
    log4jLogger.info("Done updating Wercs statuses");
  }
}
