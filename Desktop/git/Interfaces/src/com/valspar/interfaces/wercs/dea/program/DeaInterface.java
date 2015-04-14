package com.valspar.interfaces.wercs.dea.program;

import com.valspar.interfaces.common.BaseInterface;
import com.valspar.interfaces.common.beans.ConnectionAccessBean;
import com.valspar.interfaces.common.enums.DataSource;
import com.valspar.interfaces.common.servlets.PropertiesServlet;
import com.valspar.interfaces.common.utils.*;
import com.valspar.interfaces.wercs.dea.beans.DeaBean;
import java.io.FileWriter;
import java.sql.*;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.GregorianCalendar;
import oracle.jdbc.OracleConnection;
import org.apache.log4j.Logger;

public class DeaInterface extends BaseInterface
{
  private static final String reportDir = PropertiesServlet.getProperty("dea.reportDir");
  private static Logger log4jLogger = Logger.getLogger(DeaInterface.class);

  public DeaInterface()
  {
  }

  public void execute()
  {
    log4jLogger.info("DeaInterface now starting - " + new Date());

    int row_count = 0;
    OracleConnection wercsConn = null;
    OracleConnection northAmericanConn = null;
    try
    {
      wercsConn = (OracleConnection) ConnectionAccessBean.getConnection(DataSource.WERCS);
      northAmericanConn = (OracleConnection) ConnectionAccessBean.getConnection(DataSource.NORTHAMERICAN);

      DeaBean deaBean = new DeaBean();
      getDatesThenFormat(deaBean);
      String monthlyDtlRpt = reportDir + "DEA_Summary_" + deaBean.getRptMonth() + ".csv";
      String dailyDtlRpt = reportDir + "DEA_Detail_" + deaBean.getRptDateTime() + ".csv";
      String dailyAssessmentRpt = reportDir + "DEA_Assessment_" + deaBean.getRptDateTime() + ".csv";
      if (!deaBean.getTodaysMm().equalsIgnoreCase(deaBean.getLastRunMm()))
      {
        createDetailReport(northAmericanConn, monthlyDtlRpt, deaBean);
        log4jLogger.info("DEA Summary Report filename: " + monthlyDtlRpt);
        if (deaBean.getEmailTo() != null)
        {
          if (deaBean.doesReportHaveData())
          {
            EmailBean.emailMessage("DEA Summary Report for " + deaBean.getRptMonth(), " ", deaBean.getEmailTo(), monthlyDtlRpt); // File to attach
          }
        }
      }
      log4jLogger.info("Now Loading Customers in " + ConnectionUtility.buildDatabaseName(northAmericanConn));
      loadCustomers(northAmericanConn);
      log4jLogger.info("Now Loading Products in " + ConnectionUtility.buildDatabaseName(northAmericanConn));
      loadProducts(wercsConn, northAmericanConn);
      log4jLogger.info("Now Loading Orders in " + ConnectionUtility.buildDatabaseName(northAmericanConn));
      loadOrders(northAmericanConn);
      createDetailReport(northAmericanConn, dailyDtlRpt, deaBean);
      log4jLogger.info("DEA Detail Report filename: " + dailyDtlRpt);
      if (deaBean.getEmailTo() != null)
      {
        if (deaBean.doesReportHaveData())
          EmailBean.emailMessage("DEA Detail Report", "There are " + deaBean.getRowCount() + " items to report", deaBean.getEmailTo(), dailyDtlRpt);
        else
          EmailBean.emailMessage("DEA Detail Report", "There are no items to report", deaBean.getEmailTo());
      }
      createAssessmentReport(northAmericanConn, dailyAssessmentRpt, deaBean);
      log4jLogger.info("DEA Assessment Report filename: " + dailyAssessmentRpt);

      if (deaBean.getEmailTo() != null)
      {
        if (deaBean.doesReportHaveData())
        {
          StringBuilder sb = new StringBuilder();
          sb.append("There are " + row_count + " items for you to review. ");
          sb.append("Determine whether each item is DEA-regulated and \n");
          sb.append("notify Regulatory Affairs to enter the status in WERCS ");
          sb.append("Regulatory Tracking.");
          EmailBean.emailMessage("DEA Daily Product Assessment Report", sb.toString(), deaBean.getEmailTo(), dailyAssessmentRpt);
        }
        else
          EmailBean.emailMessage("DEA Daily Product Assessment Report", "There are no items for you to review today", deaBean.getEmailTo());
      }
    }
    catch (Exception e)
    {
      log4jLogger.error(e);
    }
    finally
    {
      JDBCUtil.close(wercsConn);
      JDBCUtil.close(northAmericanConn);
    }
  }

  private void createDetailReport(OracleConnection northAmericanConn, String outfilename, DeaBean deaBean)
  {
    StringBuilder sql = new StringBuilder();
    StringBuilder csvFile = new StringBuilder();
    int recCount = 0;
    
    sql.append("select replace(decode(a.dea_group,'DEAPRDS','SOLVENT','DEAPRDA','ACID',a.dea_group),',',' ') TYPE,  ");
    sql.append("       replace(o.customer_number,',',' ') customer_number,  ");
    sql.append("       replace(o.customer_name,',',' ') customer_name,  ");
    sql.append("       replace(o.from_whse,',',' ')     from_whse,     ");
    sql.append("       replace(o.whse_name,',',' ')     whse_name,     ");
    sql.append("       o.order_number,  ");
    sql.append("       replace(o.ordered_item,',',' ') ordered_item,  ");
    sql.append("       o.line_no,  ");
    sql.append("       a.f_data,  ");
    sql.append("       to_char(o.order_date,'DD-MON-YYYY HH24:MI:SS') order_date,  ");
    sql.append("       to_char(o.schedule_ship_date,'DD-MON-YYYY HH24:MI:SS') schedule_ship_date,  ");
    sql.append("       replace(o.line_status,',',' ') line_status,  ");
    sql.append("       replace(o.login_name,',',' ') login_name,  ");
    sql.append("       replace(o.login_descr,',',' ') login_descr,  ");
    sql.append("       decode(f_stop_code, '3','Y',null) product_reviewed,  ");
    sql.append("       replace(o.bulk_product,',',' ') bulk_product,  ");
    sql.append("       replace(o.order_uom,',',' ') order_uom,  ");
    sql.append("       o.ordered_quantity,  ");
    sql.append("       ROUND(GMICUOM.UOM_CONVERSION(O.item_id,0,o.ordered_quantity,o.order_uom,'GAL',0),2) EST_GAL,  ");
    sql.append("       replace(o.address1,',',' ') address1,  ");
    sql.append("       replace(o.address2,',',' ') address2,  ");
    sql.append("       replace(o.address3,',',' ') address3,  ");
    sql.append("       replace(o.address4,',',' ') address4,  ");
    sql.append("       replace(o.city,',',' ')     city,  ");
    sql.append("       replace(o.postal_code,',',' ') postal_code,  ");
    sql.append("       replace(o.state,',',' ')    state,  ");
    sql.append("       replace(o.province,',',' ') province,  ");
    sql.append("       replace(o.country,',',' ') country  ");
    sql.append("from   valspar.vca_dea_orders o, valspar.vca_dea_products a  ");
    sql.append("where  o.ordered_item = a.f_alias  ");
    sql.append("and    a.dea_group = 'DEAPRDS'  ");
    sql.append("and    a.f_stop_code  <> '1'  ");
    sql.append("and    country not in ('VI','AS','GU','MP','PR')  ");
    sql.append("union   ");
    sql.append("select replace(decode(a.dea_group,'DEAPRDS','SOLVENT','DEAPRDA','ACID',a.dea_group),',',' ') TYPE,  ");
    sql.append("       replace(o.customer_number,',',' ') customer_number,  ");
    sql.append("             replace(o.customer_name,',',' ') customer_name,  ");
    sql.append("       replace(o.from_whse,',',' ')     from_whse,     ");
    sql.append("       replace(o.whse_name,',',' ')     whse_name,     ");
    sql.append("       o.order_number,  ");
    sql.append("       replace(o.ordered_item,',',' ') ordered_item,  ");
    sql.append("       o.line_no,  ");
    sql.append("       a.f_data,  ");
    sql.append("       to_char(o.order_date,'DD-MON-YYYY HH24:MI:SS') order_date,  ");
    sql.append("       to_char(o.schedule_ship_date,'DD-MON-YYYY HH24:MI:SS') schedule_ship_date,  ");
    sql.append("       replace(o.line_status,',',' ') line_status,  ");
    sql.append("       replace(o.login_name,',',' ') login_name,  ");
    sql.append("       replace(o.login_descr,',',' ') login_descr,  ");
    sql.append("       decode(f_stop_code, '3','Y',null) product_reviewed,  ");
    sql.append("             replace(o.bulk_product,',',' ') bulk_product,  ");
    sql.append("             replace(o.order_uom,',',' ') order_uom,  ");
    sql.append("             o.ordered_quantity,  ");
    sql.append("       ROUND(GMICUOM.UOM_CONVERSION(O.item_id,0,o.ordered_quantity,o.order_uom,'GAL',0),2) EST_GAL,  ");
    sql.append("       replace(o.address1,',',' ') address1,  ");
    sql.append("             replace(o.address2,',',' ') address2,  ");
    sql.append("             replace(o.address3,',',' ') address3,  ");
    sql.append("             replace(o.address4,',',' ') address4,  ");
    sql.append("             replace(o.city,',',' ')     city,  ");
    sql.append("             replace(o.postal_code,',',' ') postal_code,  ");
    sql.append("             replace(o.state,',',' ') state,  ");
    sql.append("             replace(o.province,',',' ') province,  ");
    sql.append("             replace(o.country,',',' ') country  ");
    sql.append("from   valspar.vca_dea_orders o, valspar.vca_dea_products a  ");
    sql.append("where  o.ordered_item = a.f_alias  ");
    sql.append("and    a.dea_group = 'DEAPRDA'  ");
    sql.append("and    a.f_stop_code  <> '1'  ");
    sql.append("and    country in ('AR','BO','BR','CL','CO','EC','GF','GY','PA','PE','PY','SR','UY','VE')--acid regulated here  ");

    Statement st = null;
    ResultSet rs = null;
    try
    {
      st = northAmericanConn.createStatement();
      rs = st.executeQuery(sql.toString());
      FileWriter dtlRpt = new FileWriter(outfilename);
      csvFile.append("TYPE" + ",");
      csvFile.append("CUSTOMER NUMBER" + ",");
      csvFile.append("CUSTOMER NAME" + ",");
      csvFile.append("FROM WHSE" + ",");
      csvFile.append("WHSE NAME" + ",");
      csvFile.append("ORDER NUMBER" + ",");
      csvFile.append("LINE NO" + ",");
      csvFile.append("ORDER DATE" + ",");
      csvFile.append("SCHEDULE SHIP DATE" + ",");
      csvFile.append("LINE STATUS" + ",");
      csvFile.append("LOGIN NAME" + ",");
      csvFile.append("LOGIN DESCR" + ",");
      csvFile.append("PRODUCT REVIEWED" + ",");
      csvFile.append("BULK PRODUCT" + ",");
      csvFile.append("ORDERED ITEM" + ",");
      csvFile.append("ORDER UOM" + ",");
      csvFile.append("ORDER QTY" + ",");
      csvFile.append("EST GAL" + ",");
      csvFile.append("PERCENT ACTIVE" + ",");
      csvFile.append("ADDRESS1" + ",");
      csvFile.append("ADDRESS2" + ",");
      csvFile.append("ADDRESS3" + ",");
      csvFile.append("ADDRESS4" + ",");
      csvFile.append("CITY" + ",");
      csvFile.append("POSTAL CODE" + ",");
      csvFile.append("STATE" + ",");
      csvFile.append("PROVINCE" + ",");
      csvFile.append("COUNTRY \n");

      dtlRpt.write(csvFile.toString());
      csvFile.setLength(0);

      while (rs.next())
      {
        recCount++;
        if (rs.getString("TYPE") != null)
          csvFile.append(rs.getString("TYPE") + ",");
        else
          csvFile.append(",");
        if (rs.getString("CUSTOMER_NUMBER") != null)
          csvFile.append(rs.getString("CUSTOMER_NUMBER") + ",");
        else
          csvFile.append(",");
        if (rs.getString("CUSTOMER_NAME") != null)
          csvFile.append(rs.getString("CUSTOMER_NAME") + ",");
        else
          csvFile.append(",");
        if (rs.getString("FROM_WHSE") != null)
          csvFile.append(rs.getString("FROM_WHSE") + ",");
        else
          csvFile.append(",");
        if (rs.getString("WHSE_NAME") != null)
          csvFile.append(rs.getString("WHSE_NAME") + ",");
        else
          csvFile.append(",");
        if (rs.getString("ORDER_NUMBER") != null)
          csvFile.append(rs.getString("ORDER_NUMBER") + ",");
        else
          csvFile.append(",");
        if (rs.getString("LINE_NO") != null)
          csvFile.append(rs.getString("LINE_NO") + ",");
        else
          csvFile.append(",");
        if (rs.getString("ORDER_DATE") != null)
          csvFile.append(rs.getString("ORDER_DATE") + ",");
        else
          csvFile.append(",");
        if (rs.getString("SCHEDULE_SHIP_DATE") != null)
          csvFile.append(rs.getString("SCHEDULE_SHIP_DATE") + ",");
        else
          csvFile.append(",");
        if (rs.getString("LINE_STATUS") != null)
          csvFile.append(rs.getString("LINE_STATUS") + ",");
        else
          csvFile.append(",");
        if (rs.getString("LOGIN_NAME") != null)
          csvFile.append(rs.getString("LOGIN_NAME") + ",");
        else
          csvFile.append(",");
        if (rs.getString("LOGIN_DESCR") != null)
          csvFile.append(rs.getString("LOGIN_DESCR") + ",");
        else
          csvFile.append(",");
        if (rs.getString("PRODUCT_REVIEWED") != null)
          csvFile.append(rs.getString("PRODUCT_REVIEWED") + ",");
        else
          csvFile.append(",");
        if (rs.getString("BULK_PRODUCT") != null)
          csvFile.append(rs.getString("BULK_PRODUCT") + ",");
        else
          csvFile.append(",");
        if (rs.getString("ORDERED_ITEM") != null)
          csvFile.append(rs.getString("ORDERED_ITEM") + ",");
        else
          csvFile.append(",");
        if (rs.getString("ORDER_UOM") != null)
          csvFile.append(rs.getString("ORDER_UOM") + ",");
        else
          csvFile.append(",");

        csvFile.append(rs.getString("ORDERED_QUANTITY") + ",");
        csvFile.append(rs.getString("EST_GAL") + ",");

        if (rs.getString("F_DATA") != null)
          csvFile.append(rs.getString("F_DATA") + ",");
        else
          csvFile.append(",");
        if (rs.getString("ADDRESS1") != null)
          csvFile.append(rs.getString("ADDRESS1") + ",");
        else
          csvFile.append(",");
        if (rs.getString("ADDRESS2") != null)
          csvFile.append(rs.getString("ADDRESS2") + ",");
        else
          csvFile.append(",");
        if (rs.getString("ADDRESS3") != null)
          csvFile.append(rs.getString("ADDRESS3") + ",");
        else
          csvFile.append(",");
        if (rs.getString("ADDRESS4") != null)
          csvFile.append(rs.getString("ADDRESS4") + ",");
        else
          csvFile.append(",");
        if (rs.getString("CITY") != null)
          csvFile.append(rs.getString("CITY") + ",");
        else
          csvFile.append(",");
        if (rs.getString("POSTAL_CODE") != null)
          csvFile.append(rs.getString("POSTAL_CODE") + ",");
        else
          csvFile.append(",");
        if (rs.getString("STATE") != null)
          csvFile.append(rs.getString("STATE") + ",");
        else
          csvFile.append(",");
        if (rs.getString("PROVINCE") != null)
          csvFile.append(rs.getString("PROVINCE") + ",");
        else
          csvFile.append(",");
        if (rs.getString("COUNTRY") != null)
          csvFile.append(rs.getString("COUNTRY"));

        csvFile.append("\n");
        dtlRpt.write(csvFile.toString());
        csvFile.setLength(0);
      }
      dtlRpt.flush();
      dtlRpt.close();
      log4jLogger.info("Rows for Detail Report: " + recCount);
      deaBean.setRowCount(recCount);
    }
    catch (Exception e)
    {
      log4jLogger.error(e);
    }
    finally
    {
      JDBCUtil.close(st, rs);
    }
  }

  private void createAssessmentReport(OracleConnection northAmericanConn, String outfilename, DeaBean deaBean)
  {
    StringBuilder sql = new StringBuilder();
    StringBuilder csvFile = new StringBuilder();
    int recCount = 0;

    sql.append("select replace(f_product,',',' ') f_product, ");
    sql.append("       replace(dea_group,',',' ') dea_group, ");
    sql.append("       replace(f_data,',',' ') percent, ");
    sql.append("       replace(iim.item_desc1,',',' ') item_desc1 ");
    sql.append("from   valspar.vca_dea_products vdp, ");
    sql.append("       ic_item_mst iim, ");
    sql.append("       valspar.vca_dea_orders vdo ");
    sql.append("where  f_stop_code = 'N' ");
    sql.append("and    vdp.f_product = iim.item_no ");
    sql.append("and    vdp.f_product = vdo.bulk_product  ");
    sql.append("group  by f_product,  dea_group,  f_data,  iim.item_desc1 ");
    sql.append("order  by f_product, dea_group ");

    Statement st = null;
    ResultSet rs = null;

    try
    {
      st = northAmericanConn.createStatement();
      rs = st.executeQuery(sql.toString());
      FileWriter dtlRpt = new FileWriter(outfilename);
      csvFile.append("F_PRODUCT" + ",");
      csvFile.append("DEA_GROUP" + ",");
      csvFile.append("PERCENT" + ",");
      csvFile.append("ITEM_DESC1" + "\n");
      dtlRpt.write(csvFile.toString());
      csvFile.setLength(0);
      while (rs.next())
      {
        recCount++;
        if (rs.getString("F_PRODUCT") != null)
          csvFile.append(rs.getString("F_PRODUCT") + ",");
        else
          csvFile.append(",");
        if (rs.getString("DEA_GROUP") != null)
          csvFile.append(rs.getString("DEA_GROUP") + ",");
        else
          csvFile.append(",");
        if (rs.getString("PERCENT") != null)
          csvFile.append(rs.getString("PERCENT") + ",");
        else
          csvFile.append(",");
        if (rs.getString("ITEM_DESC1") != null)
          csvFile.append(rs.getString("ITEM_DESC1") + "\n");
        else
          csvFile.append(",");
        dtlRpt.write(csvFile.toString());
        csvFile.setLength(0);
      }
      dtlRpt.flush();
      dtlRpt.close();
      log4jLogger.info("Rows for Product Assessment Report: " + recCount);
      deaBean.setRowCount(recCount);
    }
    catch (Exception e)
    {
      log4jLogger.error(e);
    }
    finally
    {
      JDBCUtil.close(st, rs);
    }
  }

  private void getDatesThenFormat(DeaBean dea)
  {
    try
    {
      dea.setTodaysDate(new java.util.Date());
      SimpleDateFormat sdf = new SimpleDateFormat("MM");
      dea.setTodaysMm(sdf.format(dea.getTodaysDate()));
      log4jLogger.info("Todays Month is: " + dea.getTodaysMm());
      getLastRunDate(dea);
      log4jLogger.info("Date of last run: " + dea.getLastRunDate());
      Date lastRunDate = null;
      SimpleDateFormat df = new SimpleDateFormat("dd-MMM-yyyy kk:mm:ss");
      lastRunDate = df.parse(dea.getLastRunDate());
      dea.setLastRunMm(sdf.format(lastRunDate));
      log4jLogger.info("MM of last run: " + dea.getLastRunMm());
      SimpleDateFormat rptSdf = new SimpleDateFormat("MMddyyHHmmss");
      dea.setRptDateTime(rptSdf.format(dea.getTodaysDate()));

      GregorianCalendar mycalendar = new GregorianCalendar();
      mycalendar.add(mycalendar.MONTH, -1);
      Date rptMonth = mycalendar.getTime();
      SimpleDateFormat rptDf = new SimpleDateFormat("MMMM");
      dea.setRptMonth(rptDf.format(rptMonth));
      log4jLogger.info("Calculated  Month: " + dea.getRptMonth());
    }
    catch (Exception e)
    {
      log4jLogger.error(e);
    }
  }

  private void getLastRunDate(DeaBean dea)
  {
    OracleConnection middlewareConn = null;
    StringBuilder sb = new StringBuilder();

    sb.append("  SELECT TO_CHAR ( (MAX (a.end_date)), 'DD-MON-YYYY HH24:MI:SS') LAST_RUN_DATE, ");
    sb.append("         b.notification_email ");
    sb.append("    FROM quartz.vca_schedule_process a, quartz.vca_schedule b ");
    sb.append("   WHERE     a.schedule_id = b.schedule_id ");
    sb.append("         AND a.schedule_id = (SELECT schedule_id ");
    sb.append("                                FROM quartz.vca_schedule ");
    sb.append("                               WHERE job_key = 'Dea') ");
    sb.append("GROUP BY b.notification_email ");

    Statement st = null;
    ResultSet rs = null;

    try
    {
      middlewareConn = (OracleConnection) ConnectionAccessBean.getConnection(DataSource.MIDDLEWARE);
      st = middlewareConn.createStatement();
      rs = st.executeQuery(sb.toString());
      if (rs.next())
      {
        dea.setLastRunDate(rs.getString("LAST_RUN_DATE"));
        dea.setEmailTo(rs.getString("NOTIFICATION_EMAIL"));
      }
    }
    catch (Exception e)
    {
      log4jLogger.error(e);
    }
    finally
    {
      JDBCUtil.close(st, rs);
      JDBCUtil.close(middlewareConn);
    }
    if (dea.getEmailTo() == null)
      log4jLogger.error("There are no email addresses in quartz.vca_schedule for Dea");
  }

  private void loadCustomers(OracleConnection northAmericanConn)
  {
    CallableStatement cstmt = null;

    try
    {
      cstmt = northAmericanConn.prepareCall("{call VCA_DEA_PKG.LOAD_DEA_CUSTOMER_TABLE(?,?,?)}");
      cstmt.registerOutParameter(1, Types.VARCHAR);
      cstmt.registerOutParameter(2, Types.VARCHAR);
      cstmt.registerOutParameter(3, Types.NUMERIC);
      cstmt.execute();

      log4jLogger.info("Rows Loaded into VALSPAR.VCA_DEA_CUSTOMERS: " + cstmt.getInt(3));
      if (!cstmt.getString(1).equalsIgnoreCase("S"))
        log4jLogger.error("Status returned from VCA_DEA_PKG.LOAD_CUSTOMER_TABLE " + cstmt.getString(2));
    }
    catch (Exception e)
    {
      log4jLogger.error(e);
    }
    finally
    {
      JDBCUtil.close(cstmt);
    }
  }

  private void loadOrders(OracleConnection northAmericanConn)
  {
    CallableStatement cstmt = null;

    try
    {
      cstmt = northAmericanConn.prepareCall("{call VCA_DEA_PKG.LOAD_DEA_ORDERS_TABLE(?,?,?)}");
      cstmt.registerOutParameter(1, Types.VARCHAR);
      cstmt.registerOutParameter(2, Types.VARCHAR);
      cstmt.registerOutParameter(3, Types.NUMERIC);
      cstmt.execute();
      log4jLogger.info("Rows Loaded into VALSPAR.VCA_DEA_ORDERS: " + cstmt.getInt(3));
      if (!cstmt.getString(1).equalsIgnoreCase("S"))
      {
        log4jLogger.error("Status returned from VCA_DEA_PKG.LOAD_ORDERS_TABLE " + cstmt.getString(2));
      }
    }
    catch (Exception e)
    {
      log4jLogger.error(e);
    }
    finally
    {
      JDBCUtil.close(cstmt);
    }
  }

  private void loadProducts(OracleConnection wercsConn, OracleConnection northAmericanConn)
  {
    StringBuilder sql = new StringBuilder();
    StringBuilder sql_ins = new StringBuilder();
    int rowsRead = 0;
    int rowsInserted = 0;
    sql.append("SELECT P.F_PRODUCT, A.F_ALIAS, A.F_ALIAS_NAME, P.F_DATA_CODE, P.F_DATA, NVL(R.F_STOP_CODE,'N') F_STOP_CODE ");
    sql.append("FROM T_PROD_DATA P, T_PRODUCT_ALIAS_NAMES A, T_REG_PRODUCTS R ");
    sql.append("WHERE P.F_PRODUCT = A.F_PRODUCT ");
    sql.append("AND P.F_PRODUCT = R.F_PRODUCT(+) ");
    sql.append("AND R.F_REGULATION(+) = 'DEA_REG' ");
    sql.append("AND P.F_DATA_CODE IN ('DEAPRDS','DEAPRDA')");
    
    sql_ins.append("INSERT INTO VALSPAR.VCA_DEA_PRODUCTS ");
    sql_ins.append("(F_PRODUCT, F_ALIAS, F_ALIAS_NAME, DEA_GROUP, F_DATA, F_STOP_CODE) ");
    sql_ins.append("VALUES (?,?,?,?,?,?) ");

    CallableStatement cstmt = null;
    Statement st = null;
    ResultSet rs = null;

    try
    {
      cstmt = northAmericanConn.prepareCall("TRUNCATE TABLE VALSPAR.VCA_DEA_PRODUCTS");
      cstmt.executeUpdate();
    }
    catch (Exception e)
    {
      log4jLogger.error(e);
    }
    finally
    {
      JDBCUtil.close(cstmt);
    }

    CallableStatement cstmt2 = null;
    try
    {
      st = wercsConn.createStatement();
      rs = st.executeQuery(sql.toString());
      cstmt2 = northAmericanConn.prepareCall(sql_ins.toString());
      while (rs.next())
      {
        rowsRead++;
        cstmt2.setString(1, rs.getString("F_PRODUCT"));
        cstmt2.setString(2, rs.getString("F_ALIAS"));
        cstmt2.setString(3, rs.getString("F_ALIAS_NAME"));
        cstmt2.setString(4, rs.getString("F_DATA_CODE"));
        cstmt2.setString(5, rs.getString("F_DATA"));
        cstmt2.setString(6, rs.getString("F_STOP_CODE"));
        cstmt2.executeUpdate();
        rowsInserted++;
      }
      log4jLogger.info("Rows read: " + rowsRead + " Rows inserted: " + rowsInserted);
    }
    catch (Exception e)
    {
      log4jLogger.error(e);
    }
    finally
    {
      JDBCUtil.close(st, rs);
      JDBCUtil.close(cstmt2);
    }
  }
}
