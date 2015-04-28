package com.valspar.interfaces.regulatory.densitysync.program;

import com.valspar.interfaces.common.BaseInterface;
import com.valspar.interfaces.common.beans.ConnectionAccessBean;
import com.valspar.interfaces.common.enums.DataSource;
import com.valspar.interfaces.common.utils.*;
import com.valspar.interfaces.regulatory.densitysync.beans.WercsItemBean;
import java.sql.*;
import java.util.*;
import oracle.jdbc.OracleConnection;
import org.apache.log4j.Logger;

public class DensitySyncInterface extends BaseInterface
{
  private static Logger log4jLogger = Logger.getLogger(DensitySyncInterface.class);

  public DensitySyncInterface()
  {
  }

  public void execute()
  {
    OracleConnection regulatoryConn = null;
    ArrayList<DataSource> toErpDataSources = new ArrayList<DataSource>();
    try
    {
      regulatoryConn = (OracleConnection) ConnectionAccessBean.getConnection(DataSource.REGULATORY);
      toErpDataSources.add(DataSource.NORTHAMERICAN);
      toErpDataSources.add(DataSource.EMEAI);
      toErpDataSources.add(DataSource.ASIAPAC);

      ArrayList ar = buildWercsItemBeans(regulatoryConn);
      checkItemsWPG(ar, regulatoryConn);
      for (DataSource datasource: toErpDataSources)
      {
        addLotsCNV(ar, datasource);
      }
      updateVcaDensitySyncQueue(ar, regulatoryConn);
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

  private ArrayList buildWercsItemBeans(OracleConnection regulatoryConn)
  {
    ArrayList ar = new ArrayList();
    Statement stmt = null;
    ResultSet rs = null;
    try
    {
      StringBuffer sql = new StringBuffer();

      sql.append("SELECT A.ID, A.PRODUCT,  ");
      sql.append("VCA_CALC_DENSITY_FROM_FILL_PCT@TOFM(B.F_ALIAS, A.DENSITY),  ");
      sql.append("VCA_CALC_DENSITY_FROM_FILL_PCT@TOFM(B.F_ALIAS, A.DENSKG),  ");
      sql.append("B.F_ALIAS, B.F_ALIAS_NAME, D.F_DATA ");
      sql.append("FROM  VCA_DENSITY_SYNC_QUEUE A, T_PRODUCT_ALIAS_NAMES B, T_PROD_TEXT C, T_PROD_DATA D  ");
      sql.append("WHERE A.PRODUCT = B.F_PRODUCT  ");
      sql.append("AND   A.PRODUCT = C.F_PRODUCT(+)  ");
      sql.append("AND   C.F_TEXT_CODE(+) = 'COSTCL02'  ");
      sql.append("AND   D.F_DATA_CODE = 'BUSGP'  ");
      sql.append("AND   D.F_PRODUCT = A.PRODUCT  ");
      sql.append("AND   A.DATE_PROCESSED IS NULL  ");
      sql.append("AND   A.COMMENTS IS NULL  ");
      sql.append("ORDER BY A.DATE_ADDED  ");

      log4jLogger.info("WERCS Connection = " + ConnectionUtility.buildDatabaseName(regulatoryConn));
      stmt = regulatoryConn.createStatement();
      rs = stmt.executeQuery(sql.toString());
      while (rs.next())
      {
        WercsItemBean wb = new WercsItemBean();
        wb.setId(rs.getString(1));
        wb.setProduct(rs.getString(2));
        wb.setDensity(rs.getString(3));
        wb.setDensKg(rs.getString(4));
        wb.setAlias(rs.getString(5));
        wb.setAliasName(rs.getString(6));
        wb.setBusinessGroup(rs.getString(7));
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

  public void checkItemsWPG(ArrayList ar, OracleConnection regulatoryConn)
  {
    log4jLogger.info("Starting to verify that every item has DENSITY and DENSKG in WERCS.");
    Statement st = null;
    ResultSet rs = null;

    Iterator i = ar.iterator();
    while (i.hasNext())
    {
      WercsItemBean wb = (WercsItemBean) i.next();
      StringBuffer sql = new StringBuffer();
      sql.append("SELECT COUNT(*) ");
      sql.append("FROM T_PROD_DATA ");
      sql.append("WHERE F_PRODUCT IN ('");
      sql.append(wb.getProduct());
      sql.append("') ");
      sql.append("AND F_DATA_CODE IN('DENSITY', 'DENSKG')");
      try
      {
        st = regulatoryConn.createStatement();
        rs = st.executeQuery(sql.toString());
        if (rs.next())
        {
          String num = rs.getString(1);
          if (!num.equalsIgnoreCase("2"))
          {
            log4jLogger.error("Error in checkItemsWPG " + wb.getProduct() + " does not have DENSITY and DENSKG in WERCS so we will not add item.");
            EmailBean.emailMessage(wb.getProduct() + " does not have DENSITY and/or DENSKG in Wercs", wb.getProduct() + " does not have DENSITY and/or DENSKG in Wercs so it failed in the density sync interface.  Please add these datacodes to Wercs and it will automatically be processed the next time the density sync interface runs.", getNotificationEmail()); // Email addresses
            i.remove();
          }
        }
      }
      catch (Exception e)
      {
        log4jLogger.error("DB Name is " + ConnectionUtility.buildDatabaseName(regulatoryConn) + " item=" + wb.getProduct(), e);
      }
      finally
      {
        JDBCUtil.close(st, rs);
      }
    }
    log4jLogger.info("Done verifying that every item has DENSITY and DENSKG");
  }

  private void addLotsCNV(ArrayList ar, DataSource datasource)
  {
    CallableStatement cstmt = null;
    OracleConnection conn = null;
    try
    {
      conn = (OracleConnection) ConnectionAccessBean.getConnection(datasource);
      log4jLogger.info("Starting to add lots cnv for " + ConnectionUtility.buildDatabaseName(conn));
      cstmt = conn.prepareCall("{call VCA_IC_ITEM_CNV_WRAPPER(?,?,?,?,?,?)}");
      Iterator i = ar.iterator();
      while (i.hasNext())
      {
        WercsItemBean wb = null;
        try
        {
          wb = (WercsItemBean) i.next();
          cstmt.setString(1, datasource.getLogUser());
          cstmt.setString(2, wb.getAlias());
          if (datasource.getInstanceCodeOf11i().equalsIgnoreCase("NA"))
          {
            cstmt.setString(3, wb.getDensity());
            cstmt.setString(4, "LB");
          }
          else
          {
            cstmt.setString(3, wb.getDensKg());
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

  private void updateVcaDensitySyncQueue(ArrayList ar, OracleConnection regulatoryConn)
  {
    Statement stmt = null;
    try
    {
      log4jLogger.info("Starting to update Wercs statuses");
      stmt = regulatoryConn.createStatement();
      Iterator i = ar.iterator();
      while (i.hasNext())
      {
        WercsItemBean wb = (WercsItemBean) i.next();
        StringBuffer sql = new StringBuffer();
        sql.append("UPDATE VCA_DENSITY_SYNC_QUEUE SET DATE_PROCESSED = SYSDATE WHERE ID = ");
        sql.append(wb.getId());
        stmt.executeUpdate(sql.toString());
      }
      log4jLogger.info("Done updating Wercs statuses");
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
}
