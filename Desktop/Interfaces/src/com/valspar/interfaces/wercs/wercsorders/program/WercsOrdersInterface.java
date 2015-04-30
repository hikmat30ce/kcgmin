package com.valspar.interfaces.wercs.wercsorders.program;

import com.valspar.interfaces.common.*;
import com.valspar.interfaces.common.beans.ConnectionAccessBean;
import com.valspar.interfaces.common.enums.DataSource;
import com.valspar.interfaces.common.hibernate.HibernateUtil;
import com.valspar.interfaces.common.utils.*;
import com.valspar.interfaces.wercs.wercsorders.beans.*;
import java.sql.CallableStatement;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
import java.sql.Types;
import java.util.*;
import oracle.jdbc.*;
import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.hibernate.Session;

public class WercsOrdersInterface extends BaseInterface
{
  private static Logger log4jLogger = Logger.getLogger(WercsOrdersInterface.class);

  private OracleConnection northAmericanConn = null;
  private OracleConnection wercsConn = null;

  public WercsOrdersInterface()
  {
  }

  public void execute()
  {
    try
    {
      setNorthAmericanConn((OracleConnection) ConnectionAccessBean.getConnection(DataSource.NORTHAMERICAN));
      setWercsConn((OracleConnection) ConnectionAccessBean.getConnection(DataSource.WERCS));
      statusUpdate();

      List regulatedStatesList = new ArrayList();
      regulatedStatesList.add(Constants.MN);
      regulatedStatesList.add(Constants.MA);
      regulatedStatesList.add(Constants.CA);
      regulatedStatesList.add(Constants.NJ);
      regulatedStatesList.add(Constants.PA);

      List<IorderBean> iOrdersList = new ArrayList<IorderBean>();
      List<OrderBean> regulatedList = new ArrayList<OrderBean>();

      setProcessFlags();

      log4jLogger.info("Starting populateTable");
      populateTable();
      log4jLogger.info("Ending populateTable");

      regulatedList = loadOrders();
      log4jLogger.info("There are " + regulatedList.size() + " Order Beans in the Regulated List for " + ConnectionUtility.buildDatabaseName(getNorthAmericanConn()));

      iOrdersList = loadIOrders();
      log4jLogger.info("There are " + iOrdersList.size() + " Order Beans in the MSDS List for " + ConnectionUtility.buildDatabaseName(getNorthAmericanConn()));

      String dbName = ConnectionUtility.buildDatabaseName(getNorthAmericanConn());
      if (!iOrdersList.isEmpty())
      {
        log4jLogger.info("Starting to process the msdsList for " + dbName);
        insertIntoTCustomers(iOrdersList);
        log4jLogger.info("insertIntoTCustomers() Completed for " + dbName);
        insertIntoIOrders(iOrdersList);
        log4jLogger.info("insertIntoTCustomerOrders() Completed for " + dbName);
        updProcessFlags("MSDS_PROCESSED");
      }

      if (!regulatedList.isEmpty())
      {
        log4jLogger.info("Starting to process the regulatedList for " + dbName);
        insertIntoVaTscaDsl(regulatedList);
        log4jLogger.info("insertIntoVaTscaDsl() Completed for " + dbName);
        insertIntoVaStateRegs(regulatedList, regulatedStatesList);
        log4jLogger.info("insertIntoVaStateRegs() Completed for " + dbName);
        //    updProcessFlags("REGULATED_PROCESSED"); //TODO  JW remove comments// Russ K - New 03/07/2006
      }
      //TODO  JW the 3 comment out lines need to put back after tests  //TODO  JW remove comments
      /*
      deleteProcessedRows(); // Russ K - New method to delete rows that were processed
      truncateTable("VCA_WERCS_SO_TXNS"); // Russ K - New, truncates table if empty
      truncateTable("VCA_WERCS_PO_TXNS"); // Russ K - New, truncates table if empty
*/
    }
    catch (Exception e)
    {
      log4jLogger.error(e);
    }
    finally
    {
      JDBCUtil.close(getNorthAmericanConn());
      JDBCUtil.close(getWercsConn());
    }
  }

  public void statusUpdate()
  {
    CallableStatement cstmt = null;
    try
    {
      log4jLogger.info("Starting statusUpdate()");
      cstmt = getWercsConn().prepareCall("{call UPDATE_ORDER_STATUS()}");
      cstmt.execute();
      log4jLogger.info("Done with statusUpdate()");
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

  public void populateTable()
  {
    CallableStatement cstmt = null;
    try
    {
      cstmt = getNorthAmericanConn().prepareCall("{call apps.vca_wercs_pkg_6x.load_vca_wercs_orders_wk(?)}");
      log4jLogger.info("Calling vca_wercs_pkg_6x.load_vca_wercs_orders_wk()"); // REMOVE
      cstmt.registerOutParameter(1, Types.VARCHAR);
      cstmt.execute();
      String error = cstmt.getString(1);

      if (error != null)
        log4jLogger.error("Error Calling apps.vca_wercs_pkg.load_vca_wercs_orders_wk(): " + error);
      else
        log4jLogger.info("Done calling vca_wercs_pkg.load_vca_wercs_orders_wk()"); // REMOVE
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

  public ArrayList<OrderBean> loadOrders()
  {
    ArrayList<OrderBean> ordersList = new ArrayList<OrderBean>();
    OracleStatement loadOrdersStmt = null;
    ResultSet loadOrdersRs = null;
    try
    {
      StringBuilder loadOrdersQuery = new StringBuilder();
      loadOrdersQuery.append("SELECT FROM_WHSE, SHIPCUST_ID, ITEM_NO, MSDS_TYPE, CUST_ORDER, QUANTITY_INVOICED, ");
      loadOrdersQuery.append("STATE_SHIPPED_TO, COUNTRY_SHIPPED_TO, COUNTRY_SHIPPED_FROM, ORDER_TYPE, INV_TYPE, ");
      loadOrdersQuery.append("SHIP_ADDRESS ");
      loadOrdersQuery.append("FROM VCA_WERCS_NEW_REG_ORDERS_VIEW ");

      log4jLogger.info("STARTED SELECTING SALES ORDERS FROM VCA_WERCS_NEW_REG_ORDERS_VIEW in " + ConnectionUtility.buildDatabaseName(getNorthAmericanConn()));
      loadOrdersStmt = (OracleStatement) getNorthAmericanConn().createStatement();
      loadOrdersStmt.setFetchSize(500);
      loadOrdersRs = loadOrdersStmt.executeQuery(loadOrdersQuery.toString());
      log4jLogger.info("DONE SELECTING SALES ORDERS FROM VCA_WERCS_NEW_REG_ORDERS_VIEW in " + ConnectionUtility.buildDatabaseName(getNorthAmericanConn()));

      while (loadOrdersRs.next())
      {
        OrderBean ob = new OrderBean();
        ob.setPlant(loadOrdersRs.getString(1));
        ob.setCustomerId(loadOrdersRs.getString(2));
        ob.setAlias(loadOrdersRs.getString(3));
        ob.setErrorCode(Constants.ZERO);
        ob.setCustOrder(loadOrdersRs.getString(5));
        ob.setQuantity(loadOrdersRs.getString(6));
        ob.setShipToState(loadOrdersRs.getString(7));
        ob.setShipToCountry(loadOrdersRs.getString(8));
        ob.setShipFromCountry(loadOrdersRs.getString(9));
        ordersList.add(ob);
        log4jLogger.info("CustOrder = " + ob.getCustOrder() + ", CustomerId = " + ob.getCustomerId() + ", Alias = " + ob.getAlias());
      }
    }
    catch (Exception e)
    {
      log4jLogger.error(e);
    }
    finally
    {
      JDBCUtil.close(loadOrdersStmt, loadOrdersRs);
    }
    return ordersList;
  }

  public ArrayList<IorderBean> loadIOrders()
  {
    ArrayList<IorderBean> iOrdersList = new ArrayList<IorderBean>();
    OracleStatement stmt = null;
    ResultSet rs = null;
    try
    {
      StringBuilder sb = new StringBuilder();
      sb.append("SELECT from_whse, ");
      sb.append("       shipcust_id, ");
      sb.append("       item_no, ");
      sb.append("       msds_type, ");
      sb.append("       cust_order, ");
      sb.append("       quantity_invoiced, ");
      sb.append("       b.ADDR1 as cust_name, ");
      sb.append("       b.addr3, ");
      sb.append("       b.addr4, ");
      sb.append("       b.city, ");
      sb.append("       b.state, ");
      sb.append("       b.zip, ");
      sb.append("       b.country, ");
      sb.append("       b.email, ");
      sb.append("       inv_type, ");
      sb.append("       a.ship_address ");
      sb.append("  FROM valspar.vca_wercs_orders_wk a, VCA_WERCS_MSDS_ADDRESSES_VIEW b ");
      sb.append("  where a.shipcust_id = b.cust_id (+) ");
      
      String printer = getPrinter();

      log4jLogger.info("STARTED SELECTING SALES ORDERS FROM VCA_WERCS_MSDS_ORDERS_VIEW in " + ConnectionUtility.buildDatabaseName(getNorthAmericanConn()));
      stmt = (OracleStatement) getNorthAmericanConn().createStatement();
      stmt.setFetchSize(500);
      rs = stmt.executeQuery(sb.toString());
      log4jLogger.info("DONE SELECTING SALES ORDERS FROM VCA_WERCS_MSDS_ORDERS_VIEW in " + ConnectionUtility.buildDatabaseName(getNorthAmericanConn()));

      while (rs.next())
      {
        IorderBean iob = new IorderBean();
        // iob.setPlant(rs.getString(1));
        iob.setPlant("WERCS");
        iob.setCustomerId(rs.getString(2));
        iob.setAddressType("00");
        iob.setCustomerType("C");
        iob.setProduct(rs.getString(3));
        ///iob.setFormat(Constants.VAL_FORMAT);
        iob.setFormat("MTR");
        iob.setSubformat(rs.getString(4));
        iob.setCustOrder(rs.getString(5));
        iob.setQuantity(rs.getLong(6));
        iob.setStdWgt(rs.getLong(6));
        iob.setCustomerName(rs.getString(7));
        iob.setAddress1(rs.getString(8));
        iob.setAddress2(rs.getString(9));
        iob.setCity(rs.getString(10));
        iob.setState(rs.getString(11));
        iob.setZipCode(rs.getString(12));
        iob.setInventoryType(rs.getString(15));
        iob.setCustom1(rs.getString(16));
        iob.setCont(Constants.ZERO);
        iob.setCustom2(ConnectionUtility.buildDatabaseName(getNorthAmericanConn()));
        iob.setUom(Constants.LB);
        iob.setAttentionLine(Constants.EMPTY_STRING);
        iob.setNumCopies(new Long(1));
        iob.setLanguage(Constants.EN_LANG);
        iob.setCountryCode(findWercsCountryCodeFromName(rs.getString(13)));
        iob.setStatus(new Long(0));
        String email = rs.getString(14);
        if(StringUtils.isEmpty(email))
        {
          iob.setDestination("PRINTER:" + printer);   //"PRINTER:\\\\minneapolis30\\min9naps2"); //TODO  need change to PROD string
        }
        else
        {
          iob.setDestination("EMAIL:" + email);
        }

        iOrdersList.add(iob);
        if (iob.getCountryCode().equalsIgnoreCase("CAN") && iob.getSubformat().equalsIgnoreCase("CAN") && (iob.getState().equalsIgnoreCase("QC") || iob.getState().equalsIgnoreCase("QUEBEC")))
        {
          IorderBean obClone = (IorderBean) iob.clone(); //for Quebec Canada, CAN subformat, we send EN and CF msds
          obClone.setLanguage("CF");
          iOrdersList.add(obClone);
        }
        log4jLogger.info("CustOrder = " + iob.getCustOrder() + ", CustomerId = " + iob.getCustomerId() + ", Alias = " + iob.getProduct() + ", Subformat = " + iob.getSubformat());
      }
    }
    catch (Exception e)
    {
      log4jLogger.error(e);
    }
    finally
    {
      JDBCUtil.close(stmt, rs);
    }
    return iOrdersList;
  }

  public void setProcessFlags()
  {
    int regPoNumRows = 0;
    int regSoNumRows = 0;
    int msdsNumRows = 0;
    int totRows = 0;
    Statement stmtA = null;
    Statement stmtB = null;
    Statement stmtC = null;
    CallableStatement cstmt = null;

    try
    {
      StringBuilder sql = new StringBuilder();
      sql.append("UPDATE VALSPAR.VCA_WERCS_SO_TXNS ");
      sql.append("SET REGULATED_PROCESSED = 'I' ");
      sql.append("WHERE REGULATED_PROCESSED IS NULL ");

      stmtA = getNorthAmericanConn().createStatement();
      regSoNumRows = stmtA.executeUpdate(sql.toString());

      StringBuilder sqlb = new StringBuilder();
      sqlb.append("UPDATE VALSPAR.VCA_WERCS_SO_TXNS ");
      sqlb.append("SET MSDS_PROCESSED = 'C' ");
      sqlb.append("WHERE MSDS_PROCESSED IS NULL ");

      stmtB = getNorthAmericanConn().createStatement();
      stmtB.executeUpdate(sqlb.toString());

      cstmt = getNorthAmericanConn().prepareCall("{call VCA_WERCS_PKG_6X.FLAG_VCA_WERCS_SO_TXNS(?,?,?)}");
      cstmt.registerOutParameter(1, Types.VARCHAR);
      cstmt.registerOutParameter(2, Types.VARCHAR);
      cstmt.registerOutParameter(3, Types.NUMERIC);
      cstmt.execute();

      log4jLogger.info("Status returned from VCA_WERCS_PKG.FLAG_VCA_WERCS_SO_TXNS " + cstmt.getString(1));

      msdsNumRows = +cstmt.getInt(3);

      if (!cstmt.getString(1).equalsIgnoreCase("S"))
        log4jLogger.error("Status returned from VCA_WERCS_PKG.FLAG_VCA_WERCS_SO_TXNS " + cstmt.getString(2));

      StringBuilder sql2 = new StringBuilder();
      sql2.append("UPDATE VALSPAR.VCA_WERCS_PO_TXNS ");
      sql2.append("SET REGULATED_PROCESSED = 'I'  ");
      sql2.append("WHERE REGULATED_PROCESSED IS NULL ");

      stmtC = getNorthAmericanConn().createStatement();
      regPoNumRows = stmtC.executeUpdate(sql2.toString());

      totRows = regSoNumRows + msdsNumRows + regPoNumRows;
      log4jLogger.info("########## Regulated S.O. Rows: " + regSoNumRows + " ##########");
      log4jLogger.info("########## MSDS Order Rows:     " + msdsNumRows + " ##########");
      log4jLogger.info("########## Regulated P.O. Rows: " + regPoNumRows + " ##########");
      log4jLogger.info("########## Rows to Process:     " + totRows + " ##########");
    }
    catch (Exception e)
    {
      log4jLogger.error(e);
    }
    finally
    {
      JDBCUtil.close(stmtA);
      JDBCUtil.close(stmtB);
      JDBCUtil.close(stmtC);
      JDBCUtil.close(cstmt);
    }
  }

  public void truncateTable(String tableName)
  {
    StringBuilder sql = new StringBuilder();
    StringBuilder sql2 = new StringBuilder();
    sql.append("SELECT COUNT('x') FROM  VALSPAR.");
    sql.append(tableName);

    int numRows = 0;
    Statement stmt = null;
    ResultSet rs = null;
    Statement stmt2 = null;
    try
    {
      stmt = getNorthAmericanConn().createStatement();
      rs = stmt.executeQuery(sql.toString());
      if (rs.next())
      {
        numRows = rs.getInt(1);
        log4jLogger.info("Number of rows left in table: " + numRows);
      }

      if (numRows <= 0)
      {
        sql2.append("TRUNCATE TABLE VALSPAR.");
        sql2.append(tableName);

        stmt2 = getNorthAmericanConn().createStatement();
        stmt2.executeUpdate(sql2.toString());
        log4jLogger.info("Table: VALSPAR." + tableName + " has been truncated");
      }
    }
    catch (Exception e)
    {
      log4jLogger.error(e);
    }
    finally
    {
      JDBCUtil.close(stmt, rs);
      JDBCUtil.close(stmt2);
    }
  }

  public void deleteProcessedRows()
  {
    int soNumRows = 0;
    int poNumRows = 0;
    int totRows = 0;

    StringBuilder sql = new StringBuilder();
    StringBuilder sql2 = new StringBuilder();
    Statement stmt = null;
    Statement stmt2 = null;
    try
    {
      sql.append("DELETE FROM VALSPAR.VCA_WERCS_SO_TXNS ");
      sql.append("WHERE MSDS_PROCESSED = 'C' ");
      sql.append("AND  REGULATED_PROCESSED = 'C' ");

      stmt = getNorthAmericanConn().createStatement();

      soNumRows = stmt.executeUpdate(sql.toString());

      sql2.append("DELETE FROM VALSPAR.VCA_WERCS_PO_TXNS ");
      sql2.append("WHERE REGULATED_PROCESSED = 'C' ");

      stmt2 = getNorthAmericanConn().createStatement();

      poNumRows = stmt2.executeUpdate(sql2.toString());

      totRows = soNumRows + poNumRows;

      log4jLogger.info("Order Rows Deleted: " + totRows);
    }
    catch (Exception e)
    {
      log4jLogger.error(e);
    }
    finally
    {
      JDBCUtil.close(stmt);
      JDBCUtil.close(stmt2);
    }
  }

  private void insertIntoTCustomers(List<IorderBean> ar)
  {
    String lastCustomerId = null;
    PreparedStatement insertIntoTCustomersPstmt = null;
    try
    {
      StringBuilder sb = new StringBuilder();
      sb.append("INSERT INTO T_CUSTOMERS (f_customer_id, ");
      sb.append("                         f_customer_name, ");
      sb.append("                         f_customer_type, ");
      sb.append("                         f_date_entered, ");
      sb.append("                         f_tax_id, ");
      sb.append("                         f_sic, ");
      sb.append("                         f_date_stamp, ");
      sb.append("                         f_user_updated) ");
      sb.append("   SELECT ?, ");
      sb.append("          ' ', ");
      sb.append("          'RQ', ");
      sb.append("          trunc(SYSDATE) - 1, ");
      sb.append("          ' ', ");
      sb.append("          ' ', ");
      sb.append("          SYSDATE - 1, ");
      sb.append("          NULL ");
      sb.append("     FROM DUAL ");
      sb.append("    WHERE NOT EXISTS ");
      sb.append("             (SELECT 'x' ");
      sb.append("                FROM t_customers ");
      sb.append("               WHERE f_customer_id = ?) ");

      insertIntoTCustomersPstmt = getWercsConn().prepareStatement(sb.toString());

      for (IorderBean iob: ar)
      {
        lastCustomerId = iob.getCustomerId();
        if (iob.getInventoryType().equalsIgnoreCase(Constants.FG))
        {
          insertIntoTCustomersPstmt.setString(1, iob.getCustomerId());
          insertIntoTCustomersPstmt.setString(2, iob.getCustomerId());
          int count = insertIntoTCustomersPstmt.executeUpdate();
          if (count == 1)
          {
            log4jLogger.info("Inserting into T_CUSTOMERS -- f_customer_id =  " + iob.getCustomerId());
          }
          getWercsConn().commit();
        }
      }
    }
    catch (Exception e)
    {
      log4jLogger.error("for f_customer_id = " + lastCustomerId + ", db = " + ConnectionUtility.buildDatabaseName(getWercsConn()), e);
    }
    finally
    {
      JDBCUtil.close(insertIntoTCustomersPstmt);
    }
  }

  private void insertIntoIOrders(List<IorderBean> list)
  {
    Session session = HibernateUtil.getHibernateSessionAndBeginTransaction(DataSource.WERCS);
    try
    {
      for (IorderBean iOrderBean: (list))
      {
        iOrderBean.setDateStampInserted(new Date()); //TODO, need to only add based on last send date restriction, need to figure this out yet
        //TODO see INSERT_INTO_T_CUST_ORDERS_PROC
        session.save(iOrderBean);
      }
    }
    catch (Exception e)
    {
      log4jLogger.error("db = " + ConnectionUtility.buildDatabaseName(getWercsConn()), e);
    }
    finally
    {
      HibernateUtil.closeHibernateSessionAndCommitTransaction(session);
    }
  }

  private void insertIntoVaTscaDsl(List<OrderBean> ar)
  {
    PreparedStatement insertIntoVaTscaDslPstmt = null;
    try
    {
      insertIntoVaTscaDslPstmt = getWercsConn().prepareStatement("INSERT INTO VA_TSCA_DSL VALUES (VA_TSCA_DSL_SEQ.NEXTVAL, ?,?,?,?,?,?, SYSDATE-1, 0, 0, ?)");
      Iterator i = ar.iterator();
      for (OrderBean orderBean: ar)
      {
        insertIntoVaTscaDslPstmt.setString(1, orderBean.getPlant());
        insertIntoVaTscaDslPstmt.setString(2, orderBean.getAlias());
        insertIntoVaTscaDslPstmt.setString(3, orderBean.getQuantity());
        insertIntoVaTscaDslPstmt.setString(4, orderBean.getCustomerId());
        insertIntoVaTscaDslPstmt.setString(5, findWercsCountryCodeFrom11iCode(orderBean.getShipToCountry()));
        insertIntoVaTscaDslPstmt.setString(6, orderBean.getCustOrder());
        insertIntoVaTscaDslPstmt.setString(7, orderBean.getErrorCode());

        if (orderBean.getShipFromCountry().equalsIgnoreCase(Constants.US) && !orderBean.getShipToCountry().equalsIgnoreCase(Constants.US))
        {
          log4jLogger.info("Inserting into VA_TSCA_DSL -- CustOrder = " + orderBean.getCustOrder() + ", Alias = " + orderBean.getAlias() + ", db = " + ConnectionUtility.buildDatabaseName(getWercsConn()));
          insertIntoVaTscaDslPstmt.addBatch();
        }
      }
      insertIntoVaTscaDslPstmt.executeBatch();
      getWercsConn().commit();
    }
    catch (Exception e)
    {
      log4jLogger.error(e);
    }
    finally
    {
      JDBCUtil.close(insertIntoVaTscaDslPstmt);
    }
  }

  private void insertIntoVaStateRegs(List<OrderBean> ar, List regulatedStatesList)
  {
    PreparedStatement insertIntoVaStateRegsPstmt = null;
    try
    {
      insertIntoVaStateRegsPstmt = getWercsConn().prepareStatement("INSERT INTO VA_STATE_REGS VALUES (VA_STATE_REGS_SEQ.NEXTVAL,?,?,?,?,?,?, SYSDATE-1, 0, ?, 0)");
      for (OrderBean orderBean: ar)
      {
        insertIntoVaStateRegsPstmt.setString(1, orderBean.getPlant());
        insertIntoVaStateRegsPstmt.setString(2, orderBean.getAlias());
        insertIntoVaStateRegsPstmt.setString(3, orderBean.getQuantity());
        insertIntoVaStateRegsPstmt.setString(4, orderBean.getCustomerId());
        insertIntoVaStateRegsPstmt.setString(5, orderBean.getShipToState());
        insertIntoVaStateRegsPstmt.setString(6, orderBean.getCustOrder());
        insertIntoVaStateRegsPstmt.setString(7, orderBean.getErrorCode());

        if (regulatedStatesList.contains(orderBean.getShipToState().toUpperCase()) && orderBean.getShipToCountry().equalsIgnoreCase(Constants.US))
        {
          log4jLogger.info("Inserting into VA_STATE_REGS -- CustOrder = " + orderBean.getCustOrder() + ", Alias = " + orderBean.getAlias() + ", db = " + ConnectionUtility.buildDatabaseName(getWercsConn()));
          insertIntoVaStateRegsPstmt.addBatch();
        }
      }
      insertIntoVaStateRegsPstmt.executeBatch();
      getWercsConn().commit();
    }
    catch (Exception e)
    {
      log4jLogger.error(e);
    }
    finally
    {
      JDBCUtil.close(insertIntoVaStateRegsPstmt);
    }
  }

  public void updProcessFlags(String colToUpdate)
  {
    StringBuilder sql = new StringBuilder();
    StringBuilder sql2 = new StringBuilder();
    log4jLogger.info("In updProcessFlags: " + colToUpdate);
    Statement stmt = null;
    Statement stmt2 = null;
    try
    {
      if (colToUpdate.equalsIgnoreCase("MSDS_PROCESSED"))
      {
        log4jLogger.info("Setting MSDS_PROCESSED = C");
        sql.append("UPDATE VALSPAR.VCA_WERCS_SO_TXNS ");
        sql.append("SET " + colToUpdate + " = 'C' ");
        sql.append("WHERE " + colToUpdate + " = 'I' ");

        stmt = getNorthAmericanConn().createStatement();
        stmt.executeUpdate(sql.toString());
      }
      else if (colToUpdate.equalsIgnoreCase("REGULATED_PROCESSED"))
      {
        log4jLogger.info("Setting REGULATED_PROCESSED = C");
        sql.append("UPDATE VALSPAR.VCA_WERCS_SO_TXNS ");
        sql.append("SET " + colToUpdate + " = 'C' ");
        sql.append("WHERE " + colToUpdate + " = 'I' ");

        stmt = getNorthAmericanConn().createStatement();
        stmt.executeUpdate(sql.toString());

        sql2.append("UPDATE VALSPAR.VCA_WERCS_PO_TXNS ");
        sql2.append("SET " + colToUpdate + " = 'C' ");
        sql2.append("WHERE " + colToUpdate + " = 'I' ");

        stmt2 = getNorthAmericanConn().createStatement();
        stmt2.executeUpdate(sql2.toString());
      }
    }
    catch (Exception e)
    {
      log4jLogger.error(e);
    }
    finally
    {
      JDBCUtil.close(stmt);
      JDBCUtil.close(stmt2);
    }
  }

  public String findWercsCountryCodeFrom11iCode(String countryCode11i)
  {
    StringBuilder sb = new StringBuilder();
    sb.append("select keyfield3 from vca_lookups where application = 'Valspar Custom Application' ");
    sb.append("and keyfield1 = 'COUNTRY_CODE' and keyfield2 = ? ");
    return ValsparLookUps.queryForSingleValueLeaveConnectionOpen(getWercsConn(), sb.toString(), countryCode11i);
  }

  public String findWercsCountryCodeFromName(String countryName)
  {
    String sql = "select f_country_code from t_countries c where upper(f_country_name) = upper(?)";
    return ValsparLookUps.queryForSingleValueLeaveConnectionOpen(getWercsConn(), sql, countryName);
  }

  private String getPrinter()
  {
    String sql = "select keyfield2 from vca_lookups where application = 'Valspar Custom Application' and keyfield1 = 'ORDER_PRINTER'";
    return ValsparLookUps.queryForSingleValueLeaveConnectionOpen(getWercsConn(), sql);
  }

  public void setNorthAmericanConn(OracleConnection northAmericanConn)
  {
    this.northAmericanConn = northAmericanConn;
  }

  public OracleConnection getNorthAmericanConn()
  {
    return northAmericanConn;
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
