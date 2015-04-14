package com.valspar.interfaces.regulatory.dea.program;

import com.valspar.interfaces.common.BaseInterface;
import com.valspar.interfaces.common.beans.ConnectionAccessBean;
import com.valspar.interfaces.common.enums.DataSource;
import com.valspar.interfaces.common.utils.*;
import com.valspar.interfaces.regulatory.dea.beans.DeaBean;
import java.io.FileWriter;
import java.sql.*;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.GregorianCalendar;
import oracle.jdbc.OracleConnection;
import org.apache.log4j.Logger;

public class DeaInterface extends BaseInterface
{
  private static Logger log4jLogger = Logger.getLogger(DeaInterface.class);

  public DeaInterface()
  {
  }

  public void execute()
  {
    log4jLogger.info("DeaInterface now starting - " +  new Date());
   
    int row_count = 0;
    OracleConnection regulatoryConn = null;
    OracleConnection northAmericanConn = null;
    try
    {
      regulatoryConn = (OracleConnection) ConnectionAccessBean.getConnection(DataSource.REGULATORY);
      northAmericanConn = (OracleConnection) ConnectionAccessBean.getConnection(DataSource.NORTHAMERICAN);
      
      DeaBean dea = new DeaBean();
      getDatesThenFormat(regulatoryConn, dea);
      String monthlyDtlRpt = "/data/interfaces/logs/regulatory/DeaInterface/DEA_Summary_" + dea.getRpt_month() + ".csv";
      String dailyDtlRpt = "/data/interfaces/logs/regulatory/DeaInterface/DEA_Detail_" + dea.getRpt_date_time() + ".csv";
      String dailyProdAssRpt = "/data/interfaces/logs/regulatory/DeaInterface/DEA_Assessment_" + dea.getRpt_date_time() + ".csv";
      if (!dea.getTodays_mm().equalsIgnoreCase(dea.getLast_run_mm()))
      {
        row_count = createDetailReport(northAmericanConn, monthlyDtlRpt);
        log4jLogger.info("DEA Summary Report filename: " + monthlyDtlRpt);
        if (dea.getEmail_to() != null)
        {
          if (row_count > 0)
          {
            EmailBean.emailMessage("DEA Summary Report for " + dea.getRpt_month(), " ", dea.getEmail_to(), monthlyDtlRpt); // File to attach
          }
        }
      }
      log4jLogger.info("Now Loading Customers in " + ConnectionUtility.buildDatabaseName(northAmericanConn));
      loadCustomers(northAmericanConn);
      log4jLogger.info("Now Loading Products in " + ConnectionUtility.buildDatabaseName(northAmericanConn));
      loadProducts(regulatoryConn, northAmericanConn);
      log4jLogger.info("Now Loading Orders in " + ConnectionUtility.buildDatabaseName(northAmericanConn));
      loadOrders(northAmericanConn);
      row_count = createDetailReport(northAmericanConn, dailyDtlRpt);
      log4jLogger.info("DEA Detail Report filename: " + dailyDtlRpt);
      if (dea.getEmail_to() != null)
      {
        if (row_count > 0)
          EmailBean.emailMessage("DEA Detail Report", "There are " + row_count + " items to report", dea.getEmail_to(), dailyDtlRpt);
        else
          EmailBean.emailMessage("DEA Detail Report", "There are no items to report", dea.getEmail_to());
      }
      row_count = createProdAssReport(northAmericanConn, dailyProdAssRpt);
      log4jLogger.info("DEA Assessment Report filename: " + dailyProdAssRpt);

      if (dea.getEmail_to() != null)
      {
        if (row_count > 0)
        {
          StringBuffer my_buff = new StringBuffer();
          my_buff.append("There are " + row_count + " items for you to review. ");
          my_buff.append("Determine whether each item is DEA-regulated and \n");
          my_buff.append("notify Regulatory Affairs to enter the status in WERCS ");
          my_buff.append("Regulatory Tracking.");
          EmailBean.emailMessage("DEA Daily Product Assessment Report", my_buff.toString(), dea.getEmail_to(), dailyProdAssRpt);
        }
        else
          EmailBean.emailMessage("DEA Daily Product Assessment Report", "There are no items for you to review today", dea.getEmail_to());
      }

      setLastRunDate(regulatoryConn, dea);
    }
    catch (Exception e)
    {
      log4jLogger.error(e);
    }
    finally
    {
      JDBCUtil.close(regulatoryConn);
      JDBCUtil.close(northAmericanConn);
    }  
  }

  private int createDetailReport(OracleConnection northAmericanConn, String outfilename)
  {
    StringBuffer sql = new StringBuffer();
    StringBuffer csv_file = new StringBuffer();
    int mycount = 0;

    sql.append("select replace(decode(a.dea_group,'DEAPRDS','SOLVENT','DEAPRDA','ACID',a.dea_group),',',' ') TYPE, \n");
    sql.append("       replace(o.customer_number,',',' ') customer_number, \n");
    sql.append("       replace(o.customer_name,',',' ') customer_name, \n");
    sql.append("       replace(o.from_whse,',',' ')     from_whse,    \n");
    sql.append("       replace(o.whse_name,',',' ')     whse_name,    \n");
    sql.append("       o.order_number, \n");
    sql.append("       replace(o.ordered_item,',',' ') ordered_item, \n");
    sql.append("       o.line_no, \n");
    sql.append("       a.f_data, \n");
    sql.append("       to_char(o.order_date,'DD-MON-YYYY HH24:MI:SS') order_date, \n");
    sql.append("       to_char(o.schedule_ship_date,'DD-MON-YYYY HH24:MI:SS') schedule_ship_date, \n");
    sql.append("       replace(o.line_status,',',' ') line_status, \n");
    sql.append("       replace(o.login_name,',',' ') login_name, \n");
    sql.append("       replace(o.login_descr,',',' ') login_descr, \n");
    sql.append("       decode(f_stop_code, '3','Y',null) product_reviewed, \n");
    sql.append("       replace(o.bulk_product,',',' ') bulk_product, \n");
    sql.append("       replace(o.order_uom,',',' ') order_uom, \n");
    sql.append("       o.ordered_quantity, \n");
    sql.append("       ROUND(GMICUOM.UOM_CONVERSION(O.item_id,0,o.ordered_quantity,o.order_uom,'GAL',0),2) EST_GAL, \n");
    sql.append("       replace(o.address1,',',' ') address1, \n");
    sql.append("       replace(o.address2,',',' ') address2, \n");
    sql.append("       replace(o.address3,',',' ') address3, \n");
    sql.append("       replace(o.address4,',',' ') address4, \n");
    sql.append("       replace(o.city,',',' ')     city, \n");
    sql.append("       replace(o.postal_code,',',' ') postal_code, \n");
    sql.append("       replace(o.state,',',' ')    state, \n");
    sql.append("       replace(o.province,',',' ') province, \n");
    sql.append("       replace(o.country,',',' ') country \n");
    sql.append("from   valspar.vca_dea_orders o, valspar.vca_dea_products a \n");
    sql.append("where  o.ordered_item = a.f_alias \n");
    sql.append("and    a.dea_group = 'DEAPRDS' \n");
    sql.append("and    a.f_stop_code  <> '1' \n");
    sql.append("and    country not in ('VI','AS','GU','MP','PR') \n");
    sql.append("union  \n");
    sql.append("select replace(decode(a.dea_group,'DEAPRDS','SOLVENT','DEAPRDA','ACID',a.dea_group),',',' ') TYPE, \n");
    sql.append("       replace(o.customer_number,',',' ') customer_number, \n");
    sql.append("	     replace(o.customer_name,',',' ') customer_name, \n");
    sql.append("       replace(o.from_whse,',',' ')     from_whse,    \n");
    sql.append("       replace(o.whse_name,',',' ')     whse_name,    \n");
    sql.append("       o.order_number, \n");
    sql.append("       replace(o.ordered_item,',',' ') ordered_item, \n");
    sql.append("       o.line_no, \n");
    sql.append("       a.f_data, \n");
    sql.append("       to_char(o.order_date,'DD-MON-YYYY HH24:MI:SS') order_date, \n");
    sql.append("       to_char(o.schedule_ship_date,'DD-MON-YYYY HH24:MI:SS') schedule_ship_date, \n");
    sql.append("       replace(o.line_status,',',' ') line_status, \n");
    sql.append("       replace(o.login_name,',',' ') login_name, \n");
    sql.append("       replace(o.login_descr,',',' ') login_descr, \n");
    sql.append("       decode(f_stop_code, '3','Y',null) product_reviewed, \n");
    sql.append("	     replace(o.bulk_product,',',' ') bulk_product, \n");
    sql.append("	     replace(o.order_uom,',',' ') order_uom, \n");
    sql.append("	     o.ordered_quantity, \n");
    sql.append("       ROUND(GMICUOM.UOM_CONVERSION(O.item_id,0,o.ordered_quantity,o.order_uom,'GAL',0),2) EST_GAL, \n");
    sql.append("       replace(o.address1,',',' ') address1, \n");
    sql.append("	     replace(o.address2,',',' ') address2, \n");
    sql.append("	     replace(o.address3,',',' ') address3, \n");
    sql.append("	     replace(o.address4,',',' ') address4, \n");
    sql.append("	     replace(o.city,',',' ')     city, \n");
    sql.append("	     replace(o.postal_code,',',' ') postal_code, \n");
    sql.append("	     replace(o.state,',',' ') state, \n");
    sql.append("	     replace(o.province,',',' ') province, \n");
    sql.append("	     replace(o.country,',',' ') country \n");
    sql.append("from   valspar.vca_dea_orders o, valspar.vca_dea_products a \n");
    sql.append("where  o.ordered_item = a.f_alias \n");
    sql.append("and    a.dea_group = 'DEAPRDA' \n");
    sql.append("and    a.f_stop_code  <> '1' \n");
    sql.append("and    country in ('AR','BO','BR','CL','CO','EC','GF','GY','PA','PE','PY','SR','UY','VE')--acid regulated here \n");

    Statement st = null;
    ResultSet rs = null;
    try
    {
      st = northAmericanConn.createStatement();
      rs = st.executeQuery(sql.toString());
      FileWriter dtl_rpt = new FileWriter(outfilename);
      csv_file.append("TYPE" + ",");
      csv_file.append("CUSTOMER NUMBER" + ",");
      csv_file.append("CUSTOMER NAME" + ",");
      csv_file.append("FROM WHSE" + ",");
      csv_file.append("WHSE NAME" + ",");
      csv_file.append("ORDER NUMBER" + ",");
      csv_file.append("LINE NO" + ",");
      csv_file.append("ORDER DATE" + ",");
      csv_file.append("SCHEDULE SHIP DATE" + ",");
      csv_file.append("LINE STATUS" + ",");
      csv_file.append("LOGIN NAME" + ",");
      csv_file.append("LOGIN DESCR" + ",");
      csv_file.append("PRODUCT REVIEWED" + ",");
      csv_file.append("BULK PRODUCT" + ",");
      csv_file.append("ORDERED ITEM" + ",");
      csv_file.append("ORDER UOM" + ",");
      csv_file.append("ORDER QTY" + ",");
      csv_file.append("EST GAL" + ",");
      csv_file.append("PERCENT ACTIVE" + ",");
      csv_file.append("ADDRESS1" + ",");
      csv_file.append("ADDRESS2" + ",");
      csv_file.append("ADDRESS3" + ",");
      csv_file.append("ADDRESS4" + ",");
      csv_file.append("CITY" + ",");
      csv_file.append("POSTAL CODE" + ",");
      csv_file.append("STATE" + ",");
      csv_file.append("PROVINCE" + ",");
      csv_file.append("COUNTRY \n");

      dtl_rpt.write(csv_file.toString());
      csv_file.setLength(0);

      while (rs.next())
      {
        mycount++;
        if (rs.getString("TYPE") != null)
          csv_file.append(rs.getString("TYPE") + ",");
        else
          csv_file.append(",");
        if (rs.getString("CUSTOMER_NUMBER") != null)
          csv_file.append(rs.getString("CUSTOMER_NUMBER") + ",");
        else
          csv_file.append(",");
        if (rs.getString("CUSTOMER_NAME") != null)
          csv_file.append(rs.getString("CUSTOMER_NAME") + ",");
        else
          csv_file.append(",");
        if (rs.getString("FROM_WHSE") != null)
          csv_file.append(rs.getString("FROM_WHSE") + ",");
        else
          csv_file.append(",");
        if (rs.getString("WHSE_NAME") != null)
          csv_file.append(rs.getString("WHSE_NAME") + ",");
        else
          csv_file.append(",");
        if (rs.getString("ORDER_NUMBER") != null)
          csv_file.append(rs.getString("ORDER_NUMBER") + ",");
        else
          csv_file.append(",");
        if (rs.getString("LINE_NO") != null)
          csv_file.append(rs.getString("LINE_NO") + ",");
        else
          csv_file.append(",");
        if (rs.getString("ORDER_DATE") != null)
          csv_file.append(rs.getString("ORDER_DATE") + ",");
        else
          csv_file.append(",");
        if (rs.getString("SCHEDULE_SHIP_DATE") != null)
          csv_file.append(rs.getString("SCHEDULE_SHIP_DATE") + ",");
        else
          csv_file.append(",");
        if (rs.getString("LINE_STATUS") != null)
          csv_file.append(rs.getString("LINE_STATUS") + ",");
        else
          csv_file.append(",");
        if (rs.getString("LOGIN_NAME") != null)
          csv_file.append(rs.getString("LOGIN_NAME") + ",");
        else
          csv_file.append(",");
        if (rs.getString("LOGIN_DESCR") != null)
          csv_file.append(rs.getString("LOGIN_DESCR") + ",");
        else
          csv_file.append(",");
        if (rs.getString("PRODUCT_REVIEWED") != null)
          csv_file.append(rs.getString("PRODUCT_REVIEWED") + ",");
        else
          csv_file.append(",");
        if (rs.getString("BULK_PRODUCT") != null)
          csv_file.append(rs.getString("BULK_PRODUCT") + ",");
        else
          csv_file.append(",");
        if (rs.getString("ORDERED_ITEM") != null)
          csv_file.append(rs.getString("ORDERED_ITEM") + ",");
        else
          csv_file.append(",");
        if (rs.getString("ORDER_UOM") != null)
          csv_file.append(rs.getString("ORDER_UOM") + ",");
        else
          csv_file.append(",");

        csv_file.append(rs.getString("ORDERED_QUANTITY") + ",");
        csv_file.append(rs.getString("EST_GAL") + ",");

        if (rs.getString("F_DATA") != null)
          csv_file.append(rs.getString("F_DATA") + ",");
        else
          csv_file.append(",");
        if (rs.getString("ADDRESS1") != null)
          csv_file.append(rs.getString("ADDRESS1") + ",");
        else
          csv_file.append(",");
        if (rs.getString("ADDRESS2") != null)
          csv_file.append(rs.getString("ADDRESS2") + ",");
        else
          csv_file.append(",");
        if (rs.getString("ADDRESS3") != null)
          csv_file.append(rs.getString("ADDRESS3") + ",");
        else
          csv_file.append(",");
        if (rs.getString("ADDRESS4") != null)
          csv_file.append(rs.getString("ADDRESS4") + ",");
        else
          csv_file.append(",");
        if (rs.getString("CITY") != null)
          csv_file.append(rs.getString("CITY") + ",");
        else
          csv_file.append(",");
        if (rs.getString("POSTAL_CODE") != null)
          csv_file.append(rs.getString("POSTAL_CODE") + ",");
        else
          csv_file.append(",");
        if (rs.getString("STATE") != null)
          csv_file.append(rs.getString("STATE") + ",");
        else
          csv_file.append(",");
        if (rs.getString("PROVINCE") != null)
          csv_file.append(rs.getString("PROVINCE") + ",");
        else
          csv_file.append(",");
        if (rs.getString("COUNTRY") != null)
          csv_file.append(rs.getString("COUNTRY"));

        csv_file.append("\n");
        dtl_rpt.write(csv_file.toString());
        csv_file.setLength(0);
      }

      dtl_rpt.flush();
      dtl_rpt.close();
      log4jLogger.info("Rows for Detail Report: " + mycount);
    }
    catch (Exception e)
    {
      log4jLogger.error(e);
    }
    finally
    {
      JDBCUtil.close(st, rs);
    }
    return mycount;
  }

  private int createProdAssReport(OracleConnection northAmericanConn, String outfilename)
  {
    StringBuffer sql = new StringBuffer();
    StringBuffer csv_file = new StringBuffer();
    int mycount = 0;

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
      FileWriter dtl_rpt = new FileWriter(outfilename);
      csv_file.append("F_PRODUCT" + ",");
      csv_file.append("DEA_GROUP" + ",");
      csv_file.append("PERCENT" + ",");
      csv_file.append("ITEM_DESC1" + "\n");
      dtl_rpt.write(csv_file.toString());
      csv_file.setLength(0);
      while (rs.next())
      {
        mycount++;
        if (rs.getString("F_PRODUCT") != null)
          csv_file.append(rs.getString("F_PRODUCT") + ",");
        else
          csv_file.append(",");
        if (rs.getString("DEA_GROUP") != null)
          csv_file.append(rs.getString("DEA_GROUP") + ",");
        else
          csv_file.append(",");
        if (rs.getString("PERCENT") != null)
          csv_file.append(rs.getString("PERCENT") + ",");
        else
          csv_file.append(",");
        if (rs.getString("ITEM_DESC1") != null)
          csv_file.append(rs.getString("ITEM_DESC1") + "\n");
        else
          csv_file.append(",");
        dtl_rpt.write(csv_file.toString());
        csv_file.setLength(0);
      }
      dtl_rpt.flush();
      dtl_rpt.close();
      log4jLogger.info("Rows for Product Assessment Report: " + mycount);
    }
    catch (Exception e)
    {
      log4jLogger.error(e);
    }
    finally
    {
      JDBCUtil.close(st, rs);
    }
    return mycount;
  }

  private void getDatesThenFormat(OracleConnection regulatoryConn, DeaBean dea)
  {
    try
    {
      dea.setTodays_date(new java.util.Date());
      SimpleDateFormat sdf = new SimpleDateFormat("MM");
      dea.setTodays_mm(sdf.format(dea.getTodays_date()));
      log4jLogger.info("Todays Month is: " + dea.getTodays_mm());
      getLastRunDate(regulatoryConn, dea);
      log4jLogger.info("Date of last run: " + dea.getLast_run_date());
      Date last_run_date = null;
      SimpleDateFormat df = new SimpleDateFormat("dd-MMM-yyyy kk:mm:ss");
      last_run_date = df.parse(dea.getLast_run_date());
      dea.setLast_run_mm(sdf.format(last_run_date));
      log4jLogger.info("MM of last run: " + dea.getLast_run_mm());
      SimpleDateFormat rpt_sdf = new SimpleDateFormat("MMddyyHHmmss");
      dea.setRpt_date_time(rpt_sdf.format(dea.getTodays_date()));

      GregorianCalendar mycalendar = new GregorianCalendar();
      mycalendar.add(mycalendar.MONTH, -1);
      Date rpt_month = mycalendar.getTime();
      SimpleDateFormat rpt_df = new SimpleDateFormat("MMMM");
      dea.setRpt_month(rpt_df.format(rpt_month));
      log4jLogger.info("Calculated  Month: " + dea.getRpt_month());
    }
    catch (Exception e)
    {
      log4jLogger.error(e);
    }
  }

  private void getLastRunDate(OracleConnection regulatoryConn, DeaBean dea)
  {
    StringBuffer sql = new StringBuffer();
    sql.append("SELECT TO_CHAR( (NVL(b.START_DATE,SYSDATE)),'DD-MON-YYYY HH24:MI:SS') LAST_RUN_DATE,  ");
    sql.append("       NOTIFICATION_EMAIL ");
    sql.append("FROM   VA_REG_INTERFACES a, VA_REG_INTERFACES_RUN_ENV b ");
    sql.append("WHERE  a.INTERFACE_NAME = 'DeaInterface' ");
    sql.append("AND    a.INTERFACE_ID = b.INTERFACE_ID ");

    Statement st = null;
    ResultSet rs = null;

    try
    {
      st = regulatoryConn.createStatement();
      rs = st.executeQuery(sql.toString());
      if (rs.next())
      {
        dea.setLast_run_date(rs.getString("LAST_RUN_DATE"));
        dea.setEmail_to(rs.getString("NOTIFICATION_EMAIL"));
      }
    }
    catch (Exception e)
    {
      log4jLogger.error(e);
    }
    finally
    {
      JDBCUtil.close(st, rs);
    }
    if (dea.getEmail_to() == null)
      log4jLogger.error("There are no email addresses in WERCS.VA_REG_INTERFACES for DeaInterface");
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

  private void loadProducts(OracleConnection regulatoryConn, OracleConnection northAmericanConn)
  {
    StringBuffer sql = new StringBuffer();
    StringBuffer sql_ins = new StringBuffer();
    int rows_read = 0;
    int rows_inserted = 0;
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
      log4jLogger.error("while truncating table VALSPAR.VCA_DEA_PRODUCTS", e);
    }
    finally
    {
      JDBCUtil.close(cstmt);
    }

    CallableStatement cstmt2 = null;
    try
    {
      st = regulatoryConn.createStatement();
      rs = st.executeQuery(sql.toString());
      cstmt2 = northAmericanConn.prepareCall(sql_ins.toString());
      while (rs.next())
      {
        rows_read++;
        cstmt2.setString(1, rs.getString("F_PRODUCT"));
        cstmt2.setString(2, rs.getString("F_ALIAS"));
        cstmt2.setString(3, rs.getString("F_ALIAS_NAME"));
        cstmt2.setString(4, rs.getString("F_DATA_CODE"));
        cstmt2.setString(5, rs.getString("F_DATA"));
        cstmt2.setString(6, rs.getString("F_STOP_CODE"));
        cstmt2.executeUpdate();
        rows_inserted++;
      }
      log4jLogger.info("Rows read: " + rows_read + " Rows inserted: " + rows_inserted);
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

  private void setLastRunDate(OracleConnection regulatoryConn, DeaBean dea)
  {
    SimpleDateFormat sdf = new SimpleDateFormat("dd-MMM-yyyy kk:mm:ss");
    String oracle_date = sdf.format(dea.getTodays_date());
    Statement stmt = null;
    try
    {
      StringBuffer sql = new StringBuffer();
      sql.append("UPDATE VA_REG_INTERFACES_RUN_ENV ");
      sql.append("SET START_DATE = TO_DATE('");
      sql.append(oracle_date + "','DD-MON-YYYY HH24:MI:SS') ");
      sql.append("WHERE INTERFACE_ID = (SELECT INTERFACE_ID ");
      sql.append("FROM VA_REG_INTERFACES WHERE INTERFACE_NAME = 'DeaInterface') ");

      stmt = regulatoryConn.createStatement();
      stmt.executeUpdate(sql.toString());
    }
    catch (Exception e)
    {
      log4jLogger.error(e);
    }
    finally
    {
      JDBCUtil.close(stmt);
    }
  }
}
