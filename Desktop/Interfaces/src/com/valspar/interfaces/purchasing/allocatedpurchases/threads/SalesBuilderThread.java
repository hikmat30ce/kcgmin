package com.valspar.interfaces.purchasing.allocatedpurchases.threads;

import com.valspar.interfaces.common.beans.*;
import com.valspar.interfaces.common.enums.DataSource;
import com.valspar.interfaces.common.utils.*;
import com.valspar.interfaces.common.utils.InterfaceThreadManager;
import com.valspar.interfaces.purchasing.allocatedpurchases.beans.SalesBean;
import java.sql.*;
import oracle.jdbc.*;
import org.apache.log4j.Logger;

public class SalesBuilderThread extends BaseBuilderThread
{
  private static Logger log4jLogger = Logger.getLogger(SalesBuilderThread.class);
  private DataSource dataSource;

  public SalesBuilderThread(InterfaceInfoBean interfaceinfo, DataSource dataSource)
  {
    setInterfaceInfo(interfaceinfo);
    this.dataSource = dataSource;
  }

  public void run()
  {
    InterfaceThreadManager.addInterfaceThread(getInterfaceInfo());

    try
    {
      transferSales();
    }
    catch (Exception e)
    {
      log4jLogger.error(e);
    }
    finally
    {
      InterfaceThreadManager.removeInterfaceThread();
    }
  }

  private void transferSales()
  {
    Connection sourceConn = null;
    OracleConnection targetConn = null;
    PreparedStatement sourcePst = null;
    OraclePreparedStatement targetPst = null;
    ResultSet rs = null;
    long rec = 0L;
    String oracleSource = dataSource.getAnalyticsDataSource();

    try
    {
      StringBuilder sb = new StringBuilder();

      sb.append("INSERT INTO ANNUAL_SALES ( ");
      sb.append("   PRODUCT_NO, PRODUCT_DESC, ");
      sb.append("   UOM, PRODUCT_TYPE,  ");
      sb.append("   NET_QTY, CUSTOMER_GL_CLASS, CUSTOMER_GL_CLASS_NAME,  ");
      sb.append("   PROFIT_CENTER_CODE, PROFIT_CENTER_NAME, ");
      sb.append("   COMPANY_CODE, COMPANY_NAME, ");
      sb.append("   TOTAL_CORP_CODE, TOTAL_CORP_DESC,  ");
      sb.append("   BUSINESS_UNIT_CODE, BUSINESS_UNIT_NAME, SOURCE_INSTANCE)  ");
      sb.append("VALUES ( ");
      sb.append("   :PRODUCT_NO, :PRODUCT_DESC, ");
      sb.append("   :UOM, :PRODUCT_TYPE, ");
      sb.append("   :NET_QTY, :CUSTOMER_GL_CLASS, :CUSTOMER_GL_CLASS_NAME,  ");
      sb.append("   :PROFIT_CENTER_CODE, :PROFIT_CENTER_NAME, ");
      sb.append("   :COMPANY_CODE, :COMPANY_NAME, ");
      sb.append("   :TOTAL_CORP_CODE, :TOTAL_CORP_DESC,  ");
      sb.append("   :BUSINESS_UNIT_CODE, :BUSINESS_UNIT_NAME, :SOURCE_INSTANCE)  ");

      targetConn = (OracleConnection) ConnectionAccessBean.getConnection(DataSource.RMINDEX);
      targetPst = (OraclePreparedStatement) targetConn.prepareStatement(sb.toString());

      sb = new StringBuilder();
      sb.append("SELECT EVALUATE ('/*+ NO_CPU_COSTING */ %1', Time.\"Fiscal Month\") sneaky_hint, ");
      sb.append("       \"- Product Attributes\".\"Product Number\" product_number, ");
      sb.append("       \"- Product Attributes\".\"Product Description\" product_desc, ");
      sb.append("       \"- Product Attributes\".\"Base UOM\" uom, ");
      sb.append("       \"- Other Attributes\".\"Product Type\" product_type,  ");
      sb.append("       \"Equivalent Quantity\".\"Net Quantity\" net_quantity, ");
      sb.append("       \"Ship To Location\".\"Ship To Customer GL Class\" customer_gl_class, ");
      sb.append("       \"Ship To Location\".\"Ship To Customer GL Class Name\" customer_gl_class_name, ");
      sb.append("       \"GL Account\".\"Profit Center Code\" profit_center_code, ");
      sb.append("       \"GL Account\".\"Profit Center Description\" profit_center_name, ");
      sb.append("       \"GL Account\".\"Company Code\" company_code, ");
      sb.append("       \"GL Account\".\"Company Code Description\" company_name, ");
      sb.append("       \"Business Unit\".\"Total Corp Code\" total_corp_code, ");
      sb.append("       \"Business Unit\".\"Total Corp\" total_corp_desc, ");
      sb.append("       \"Business Unit\".\"Business Unit Code\" business_unit_code, ");
      sb.append("       \"Business Unit\".\"Business Unit\" business_unit_name ");
      sb.append("FROM \"Sales - Orders, Backlog and Invoices\" ");
      sb.append("WHERE (\"GL Account\".\"Account Category Code\" = '5000') ");
      sb.append("  AND (\"Invoice Details\".\"Revenue Flag\" = 'Y') ");
      sb.append("  AND (\"Invoice Details\".\"Sales Invoice Lines Transaction Type\" = 'Standard Invoice') ");
      sb.append("  AND (Time.\"Fiscal Month\" > VALUEOF (\"FISCAL_PERIOD_LY_PREVIOUS\")) ");
      sb.append("  AND (Time.\"Fiscal Month\" <= VALUEOF (\"PREVIOUS_FSCL_MONTH\")) ");
      sb.append("  AND (\"Invoice Details\".\"Oracle Source\" = '" + oracleSource + "') ");
      sb.append("  AND (Time.\"Fiscal Calendar\" = '" + dataSource.getAnalyticsFiscalCalendar() + "')  ");

      sourceConn = ConnectionAccessBean.getConnection(DataSource.ANALYTICS);
      sourcePst = sourceConn.prepareStatement(sb.toString());

      log4jLogger.info("SALES - Running sales query against Analytics for " + oracleSource + "...");
      rs = sourcePst.executeQuery();
      log4jLogger.info("SALES - Query finished.  Streaming data from Analytics (" + oracleSource + ") to " + DataSource.RMINDEX.getDataSourceLabel() + "...");

      while (rs.next())
      {
        SalesBean bean = new SalesBean();
        bean.setProductNumber(rs.getString("product_number"));
        bean.setProductDesc(rs.getString("product_desc"));
        bean.setUom(rs.getString("uom"));
        bean.setProductType(rs.getString("product_type"));
        bean.setQuantity(rs.getString("net_quantity"));
        bean.setCustomerGlClass(rs.getString("customer_gl_class"));
        bean.setCustomerGlClassName(rs.getString("customer_gl_class_name"));
        bean.setProfitCenterCode(rs.getString("profit_center_code"));
        bean.setProfitCenterName(rs.getString("profit_center_name"));
        bean.setCompanyCode(rs.getString("company_code"));
        bean.setCompanyName(rs.getString("company_name"));
        bean.setTotalCorpCode(rs.getString("total_corp_code"));
        bean.setTotalCorpDesc(rs.getString("total_corp_desc"));
        bean.setBusinessUnitCode(rs.getString("business_unit_code"));
        bean.setBusinessUnitName(rs.getString("business_unit_name"));
        bean.setOracleSource(oracleSource);

        targetPst.setStringAtName("PRODUCT_NO", bean.getProductNumber());
        targetPst.setStringAtName("PRODUCT_DESC", bean.getProductDesc());
        targetPst.setStringAtName("UOM", bean.getUom());
        targetPst.setStringAtName("PRODUCT_TYPE", bean.getProductType());
        targetPst.setStringAtName("NET_QTY", bean.getQuantity());
        targetPst.setStringAtName("CUSTOMER_GL_CLASS", bean.getCustomerGlClass());
        targetPst.setStringAtName("CUSTOMER_GL_CLASS_NAME", bean.getCustomerGlClassName());
        targetPst.setStringAtName("PROFIT_CENTER_CODE", bean.getProfitCenterCode());
        targetPst.setStringAtName("PROFIT_CENTER_NAME", bean.getProfitCenterName());
        targetPst.setStringAtName("COMPANY_CODE", bean.getCompanyCode());
        targetPst.setStringAtName("COMPANY_NAME", bean.getCompanyName());
        targetPst.setStringAtName("TOTAL_CORP_CODE", bean.getTotalCorpCode());
        targetPst.setStringAtName("TOTAL_CORP_DESC", bean.getTotalCorpDesc());
        targetPst.setStringAtName("BUSINESS_UNIT_CODE", bean.getBusinessUnitCode());
        targetPst.setStringAtName("BUSINESS_UNIT_NAME", bean.getBusinessUnitName());
        targetPst.setStringAtName("SOURCE_INSTANCE", bean.getOracleSource());
        targetPst.addBatch();

        rec += 1L;

        if (rec % 1000L == 0L)
        {
          targetPst.executeBatch();
        }
      }

      targetPst.executeBatch();
    }
    catch (Exception e)
    {
      log4jLogger.error("SALES (" + oracleSource + ")", e);
      setError(e);
    }
    finally
    {
      JDBCUtil.close(sourcePst, rs);
      JDBCUtil.close(targetPst);
      JDBCUtil.close(sourceConn);
      JDBCUtil.close(targetConn);
    }

    log4jLogger.info("SALES - Done transferring sales data from Analytics (" + oracleSource + "), count = " + rec);
  }
}
