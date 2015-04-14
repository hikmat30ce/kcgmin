package com.valspar.interfaces.purchasing.eoqproductionusage.program;

import com.valspar.interfaces.common.BaseInterface;
import com.valspar.interfaces.common.beans.ConnectionAccessBean;
import com.valspar.interfaces.common.enums.DataSource;
import com.valspar.interfaces.common.utils.JDBCUtil;
import com.valspar.interfaces.purchasing.eoqproductionusage.beans.UsageBean;
import java.sql.*;
import java.util.*;
import oracle.jdbc.*;
import org.apache.log4j.Logger;

public class EOQProductionUsageInterface extends BaseInterface
{
  private static Logger log4jLogger = Logger.getLogger(EOQProductionUsageInterface.class);

  public EOQProductionUsageInterface()
  {
  }

  public void execute()
  {
    ArrayList<DataSource> toErpDataSourceList = new ArrayList<DataSource>();
    toErpDataSourceList.add(DataSource.NORTHAMERICAN);
    toErpDataSourceList.add(DataSource.EMEAI);
    try
    {
      for (DataSource dataSource: toErpDataSourceList)
      {
        populateUsage(dataSource);
        log4jLogger.info("...done inserting usage data into " + dataSource.getDataSourceLabel());
      }

    }
    catch (Exception e)
    {
      log4jLogger.error(e);
    }
  }

  public void populateUsage(DataSource dataSource)
  {
    OracleConnection conn = null;;
    try
    {
      conn = (OracleConnection) ConnectionAccessBean.getConnection(dataSource);
      log4jLogger.info("Truncating Oracle Usage table " + dataSource.getDataSourceLabel() + "...");
      clearUsageTable(conn);

      log4jLogger.info("Running usage query against analytics " + dataSource.getDataSourceLabel() + "...");
      List<UsageBean> usage = buildUsage(dataSource);

      log4jLogger.info("...usage query finished against analytics, count = " + usage.size() + ".  Inserting into Oracle " + dataSource.getDataSourceLabel() + "...");
      insertUsage(conn, usage);
    }
    catch (Exception e)
    {
      log4jLogger.error(e);
    }
    finally
    {
      JDBCUtil.close(conn);
    }

  }

  private List<UsageBean> buildUsage(DataSource dataSource)
  {
    PreparedStatement pst = null;
    ResultSet rs = null;
    Connection analyticsConn = null;
    List<UsageBean> beans = new ArrayList<UsageBean>();
  
    try
    {
      analyticsConn = ConnectionAccessBean.getConnection(DataSource.ANALYTICS);
      StringBuilder sb = new StringBuilder();
      sb.append("SELECT   Time.\"Fiscal Month\" fiscal_month, ");
      sb.append("\"Inventory Organization\".Warehouse whse_code, ");
      sb.append("\"- Product Attributes\".\"Product Number\" item_no, ");
      sb.append("\"- Month\".\"Qty-LB\" usage_lb, ");
      sb.append("\"- Month\".\"Qty-GAL\" usage_gal, ");
      sb.append("\"- Month\".\"Qty-KG\" usage_kg, ");
      sb.append("\"- Month\".\"Qty-LTR\" usage_ltr, ");
      sb.append("\"Transaction Attributes\".\"Oracle Source\" data_source ");
      sb.append("FROM   Inventory ");
      sb.append("WHERE (\"- Activity Other\".\"Activity Type\" = 'Batches - Ingredient') ");
      sb.append("AND (\"Inventory Organization\".\"Consignment Indicator\" = 'N') ");
      sb.append("AND (\"Time Current Period Codes\".\"Current Fiscal Month Code\" IN  ");
      sb.append("('Previous', ");
      sb.append("'Previous-1', ");
      sb.append("'Previous-2', ");
      sb.append("'Previous-3', ");
      sb.append("'Previous-4', ");
      sb.append("'Previous-5', ");
      sb.append("'Previous-6', ");
      sb.append("'Previous-7', ");
      sb.append("'Previous-8', ");
      sb.append("'Previous-9', ");
      sb.append("'Previous-10', ");
      sb.append("'Previous-11')) ");
      sb.append("AND (\"Transaction Attributes\".\"Oracle Source\" = '");
      sb.append(dataSource.getAnalyticsDataSource());
      sb.append("') ");

      if (dataSource.getDataSourceLabel().equalsIgnoreCase("North American"))
      {
        sb.append("AND (Time.\"Fiscal Calendar\" = 'North America') ");
      }
      else if (dataSource.getDataSourceLabel().equalsIgnoreCase("EMEAI"))
      {
        sb.append("AND (Time.\"Fiscal Calendar\" = 'International') ");
      }
      else
      {
        log4jLogger.info("Error: invalid database for Analytics query:" + dataSource.getDataSourceLabel());
        throw new Exception("Invalid database for analytics query:" + dataSource.getDataSourceLabel());
      }
      sb.append("ORDER BY 1, 2, 3, 8 ");

      pst = analyticsConn.prepareStatement(sb.toString());
      rs = pst.executeQuery();

      while (rs.next())
      {
        UsageBean bean = new UsageBean();
        bean.setFiscalMonth(rs.getString("fiscal_month"));
        bean.setWhseCode(rs.getString("whse_code"));
        bean.setItemNo(rs.getString("item_no"));
        bean.setUsageLB(rs.getString("usage_lb"));
        bean.setUsageGAL(rs.getString("usage_gal"));
        bean.setUsageKG(rs.getString("usage_kg"));
        bean.setUsageLTR(rs.getString("usage_ltr"));
        bean.setDataSource(rs.getString("data_source"));
        beans.add(bean);
      }
    }
    catch (Exception e)
    {
      log4jLogger.error(e);
    }
    finally
    {
      JDBCUtil.close(pst, rs);
      JDBCUtil.close(analyticsConn);
    }
    return beans;
  }

  private void clearUsageTable(OracleConnection conn)
  {
    OraclePreparedStatement pst = null;

    try
    {
      pst = (OraclePreparedStatement) conn.prepareStatement("TRUNCATE TABLE VALSPAR.VCA_EOQ_PRODUCTION_USAGE ");
      pst.execute();
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

  private void insertUsage(OracleConnection conn, List<UsageBean> usage)
  {
    OraclePreparedStatement pst = null;

    try
    {
      StringBuilder sb = new StringBuilder();

      sb.append("INSERT INTO VALSPAR.VCA_EOQ_PRODUCTION_USAGE (FISCAL_MONTH, ");
      sb.append("WHSE_CODE, ");
      sb.append("ITEM_NO, ");
      sb.append("USAGE_LB, ");
      sb.append("USAGE_GAL, ");
      sb.append("USAGE_KG, ");
      sb.append("USAGE_LTR, ");
      sb.append("DATA_SOURCE) ");
      sb.append("VALUES (TO_DATE( :FISCAL_MONTH , 'YYYY / MM'), ");
      sb.append(":WAREHOUSE, ");
      sb.append(":ITEM_NO, ");
      sb.append(":USAGE_LB, ");
      sb.append(":USAGE_GAL, ");
      sb.append(":USAGE_KG, ");
      sb.append(":USAGE_LTR, ");
      sb.append(":DATA_SOURCE) ");

      pst = (OraclePreparedStatement) conn.prepareStatement(sb.toString());

      for (UsageBean bean: usage)
      {
        pst.setString(1, bean.getFiscalMonth());
        pst.setString(2, bean.getWhseCode());
        pst.setString(3, bean.getItemNo());
        pst.setString(4, bean.getUsageLB());
        pst.setString(5, bean.getUsageGAL());
        pst.setString(6, bean.getUsageKG());
        pst.setString(7, bean.getUsageLTR());
        pst.setString(8, bean.getDataSource());
        pst.addBatch();
      }
      pst.executeBatch();
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
