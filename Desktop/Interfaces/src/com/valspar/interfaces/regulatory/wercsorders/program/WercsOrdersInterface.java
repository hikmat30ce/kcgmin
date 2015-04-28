package com.valspar.interfaces.regulatory.wercsorders.program;

import com.valspar.interfaces.common.*;
import com.valspar.interfaces.common.beans.ConnectionAccessBean;
import com.valspar.interfaces.common.enums.DataSource;
import com.valspar.interfaces.common.utils.*;
import com.valspar.interfaces.regulatory.wercsorders.beans.*;
import java.sql.*;
import java.util.*;
import oracle.jdbc.*;
import org.apache.log4j.Logger;

public class WercsOrdersInterface extends BaseInterface
{
  private static Logger log4jLogger = Logger.getLogger(WercsOrdersInterface.class);

  ArrayList regulatedStatesList = new ArrayList();
  ArrayList woodGroupList = new ArrayList();
  private OracleConnection northAmericanConn = null;
  private OracleConnection regulatoryConn = null;

  public WercsOrdersInterface()
  {
  }

  public void execute()
  {
    try
    {
      northAmericanConn = (OracleConnection) ConnectionAccessBean.getConnection(DataSource.NORTHAMERICAN);
      regulatoryConn = (OracleConnection) ConnectionAccessBean.getConnection(DataSource.REGULATORY);
      statusUpdate();

      regulatedStatesList.add(Constants.MN);
      regulatedStatesList.add(Constants.MA);
      regulatedStatesList.add(Constants.CA);
      regulatedStatesList.add(Constants.NJ);
      regulatedStatesList.add(Constants.PA);

      woodGroupList.add(Constants.W);
      woodGroupList.add(Constants.WS);

      ArrayList msdsList = new ArrayList();
      ArrayList regulatedList = new ArrayList();

      setProcessFlags();

      log4jLogger.info("Starting populateTable");
      populateTable(); // New call to method, no need for dates anymore - Russ K
      log4jLogger.info("Ending populateTable");

      regulatedList = loadOrders("VCA_WERCS_NEW_REG_ORDERS_VIEW"); // Russ K - new call to method
      log4jLogger.info("There are " + regulatedList.size() + " Order Beans in the Regulated List for " + ConnectionUtility.buildDatabaseName(northAmericanConn));

      msdsList = loadOrders("VCA_WERCS_MSDS_ORDERS_VIEW"); // Russ K - new call to method
      log4jLogger.info("There are " + msdsList.size() + " Order Beans in the MSDS List for " + ConnectionUtility.buildDatabaseName(northAmericanConn));

      if (msdsList.size() > 0)
      {
        log4jLogger.info("Starting to process the msdsList for " + ConnectionUtility.buildDatabaseName(northAmericanConn));
        insertIntoTCustomers(msdsList);
        log4jLogger.info("insertIntoTCustomers() Completed for " + ConnectionUtility.buildDatabaseName(northAmericanConn));
        insertIntoTCustomerOrders(msdsList);
        log4jLogger.info("insertIntoTCustomerOrders() Completed for " + ConnectionUtility.buildDatabaseName(northAmericanConn));
        updProcessFlags("MSDS_PROCESSED"); // Russ K - New 03/07/2006
      }

      if (regulatedList.size() > 0)
      {
        log4jLogger.info("Starting to process the regulatedList for " + ConnectionUtility.buildDatabaseName(northAmericanConn));
        insertIntoVaTscaDsl(regulatedList);
        log4jLogger.info("insertIntoVaTscaDsl() Completed for " + ConnectionUtility.buildDatabaseName(northAmericanConn));
        insertIntoVaStateRegs(regulatedList);
        log4jLogger.info("insertIntoVaStateRegs() Completed for " + ConnectionUtility.buildDatabaseName(northAmericanConn));
        updProcessFlags("REGULATED_PROCESSED"); // Russ K - New 03/07/2006
      }

      deleteProcessedRows(); // Russ K - New method to delete rows that were processed
      truncateTable("VCA_WERCS_SO_TXNS"); // Russ K - New, truncates table if empty
      truncateTable("VCA_WERCS_PO_TXNS"); // Russ K - New, truncates table if empty
    }
    catch (Exception e)
    {
      log4jLogger.info("For DB " + ConnectionUtility.buildDatabaseName(northAmericanConn), e);
    }
    finally
    {
      changeProcessedFlags();
      updateFAlwaysSend();
      JDBCUtil.close(northAmericanConn);
      JDBCUtil.close(regulatoryConn);
    }
  }

  public void statusUpdate()
  {
    CallableStatement cstmt = null;
    try
    {
      log4jLogger.info("Starting statusUpdate()");
      cstmt = regulatoryConn.prepareCall("{call UPDATE_ORDER_STATUS()}");
      cstmt.execute();
      log4jLogger.info("Done with statusUpdate()");
    }
    catch (Exception e)
    {
      log4jLogger.error("for db " + ConnectionUtility.buildDatabaseName(regulatoryConn), e);
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
      cstmt = northAmericanConn.prepareCall("{call apps.vca_wercs_pkg.load_vca_wercs_orders_wk(?)}");
      log4jLogger.info("Calling vca_wercs_pkg.load_vca_wercs_orders_wk()"); // REMOVE
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

  public ArrayList loadOrders(String view)
  {
    ArrayList ordersList = new ArrayList();
    OracleStatement loadOrdersStmt = null;
    ResultSet loadOrdersRs = null;
    try
    {
      StringBuffer loadOrdersQuery = new StringBuffer();
      loadOrdersQuery.append("SELECT FROM_WHSE, SHIPCUST_ID, ITEM_NO, MSDS_TYPE, CUST_ORDER, QUANTITY_INVOICED, ");
      loadOrdersQuery.append("STATE_SHIPPED_TO, COUNTRY_SHIPPED_TO, COUNTRY_SHIPPED_FROM, ORDER_TYPE, INV_TYPE, ");
      loadOrdersQuery.append("SHIP_ADDRESS ");

      loadOrdersQuery.append("FROM ");
      loadOrdersQuery.append(view);
      log4jLogger.info("STARTED SELECTING SALES ORDERS FROM " + view + " in " + ConnectionUtility.buildDatabaseName(northAmericanConn));
      loadOrdersStmt = (OracleStatement) northAmericanConn.createStatement();
      loadOrdersStmt.setFetchSize(500);
      loadOrdersRs = loadOrdersStmt.executeQuery(loadOrdersQuery.toString());
      log4jLogger.info("DONE    SELECTING SALES ORDERS FROM " + view + " in " + ConnectionUtility.buildDatabaseName(northAmericanConn));

      while (loadOrdersRs.next())
      {
        OrderBean ob = new OrderBean();
        ob.setPlant(loadOrdersRs.getString(1));
        ob.setCustomerId(loadOrdersRs.getString(2));
        ob.setAlias(loadOrdersRs.getString(3));
        ob.setPublishedAlias(getPublishedAlias(ob.getAlias()));
        ob.setMsdsType(loadOrdersRs.getString(4));
        ob.setProduct(getProduct(ob));

        if (view.equalsIgnoreCase("VCA_WERCS_MSDS_ORDERS_VIEW"))
        {
          ob.setErrorCode(Constants.ZERO);
          ob.setProcessedFlag(populateProcessedFlag(ob));
        }
        else
        {
          if (ob.getProduct().equals(Constants.EMPTY_STRING))
          {
            ob.setErrorCode(Constants.ONE);
            ob.setProcessedFlag(Constants.ZERO);
          }
          else
          {
            ob.setErrorCode(Constants.ZERO);
            ob.setProcessedFlag(Constants.ZERO);
          }
        }

        ob.setCustOrder(loadOrdersRs.getString(5));
        ob.setQuantity(loadOrdersRs.getString(6));
        ob.setStandardWeight(loadOrdersRs.getString(6));
        ob.setShipToState(loadOrdersRs.getString(7));
        ob.setShipToCountry(loadOrdersRs.getString(8));
        ob.setShipFromCountry(loadOrdersRs.getString(9));
        ob.setOrderType(loadOrdersRs.getString(10));
        ob.setInventoryType(loadOrdersRs.getString(11));
        ob.setShipAddress(loadOrdersRs.getString(12));

        ob.setAddressBean(populateAddressBean(ob));
        ob.setBusinessGroup(populateBusinessGroup(ob));
        ob.setDb(ConnectionUtility.buildDatabaseName(northAmericanConn));

        ob.setFormat(Constants.VAL_FORMAT);
        ob.setLanguage(Constants.EN_LANG);
        ob.setUom(Constants.LB);
        ob.setAttentionLine(Constants.EMPTY_STRING);
        ob.setNumCopies(Constants.ONE);
        ob.setContainer(Constants.ZERO);
        ob.setDestination(Constants.ZERO);

        ordersList.add(ob);

        if (view.equalsIgnoreCase("VCA_WERCS_MSDS_ORDERS_VIEW"))
        {
          Iterator ai = ob.getAddressBean().iterator();
          boolean continueLoop = true;
          while (ai.hasNext() && continueLoop)
          {
            AddressBean ab = (AddressBean) ai.next();
            if (ab.getCountryName().equalsIgnoreCase("CANADA") && ob.getMsdsType().equalsIgnoreCase("CAN") && (ab.getState().equalsIgnoreCase("QC") || ab.getState().equalsIgnoreCase("QUEBEC")))
            {
              OrderBean obClone = (OrderBean) ob.clone();
              obClone.setLanguage("CF");
              ordersList.add(obClone);
              continueLoop = false;
            }
          }
        }

        log4jLogger.info("CustOrder = " + ob.getCustOrder() + ", CustomerId = " + ob.getCustomerId() + ", Alias = " + ob.getAlias() + ", MsdsType = " + ob.getMsdsType() + ", ProcessedFlag = " + ob.getProcessedFlag());

      }
    }
    catch (Exception e)
    {
      log4jLogger.error("for view " + view + " and db " + ConnectionUtility.buildDatabaseName(northAmericanConn), e);
    }
    finally
    {
      JDBCUtil.close(loadOrdersStmt, loadOrdersRs);
    }
    return ordersList;
  }

  private String getProduct(OrderBean ob)
  {
    String product = null;
    Statement stmt = null;
    ResultSet rs = null;
    try
    {
      StringBuffer getProductQuery = new StringBuffer();
      getProductQuery.append("select GET_PRODUCT('");
      getProductQuery.append(ob.getAlias());
      getProductQuery.append("') from dual");

      stmt = regulatoryConn.createStatement();
      ((OracleStatement) stmt).defineColumnType(1, Types.VARCHAR);
      rs = stmt.executeQuery(getProductQuery.toString());
      if (rs.next())
        product = rs.getString(1);
    }
    catch (Exception e)
    {
      log4jLogger.error("for db " + ConnectionUtility.buildDatabaseName(regulatoryConn), e);
    }
    finally
    {
      JDBCUtil.close(stmt, rs);
    }
    return product;
  }

  private String getPublishedAlias(String alias)
  {
    String publishedAlias = null;
    Statement stmt = null;
    ResultSet rs = null;
    try
    {
      StringBuffer getPublishedAliasQuery = new StringBuffer();
      getPublishedAliasQuery.append("select GET_PUBLISHED_ALIAS('");
      getPublishedAliasQuery.append(alias);
      getPublishedAliasQuery.append("') from dual");

      stmt = regulatoryConn.createStatement();
      ((OracleStatement) stmt).defineColumnType(1, Types.VARCHAR);
      rs = stmt.executeQuery(getPublishedAliasQuery.toString());
      if (rs.next())
        publishedAlias = rs.getString(1);
    }
    catch (Exception e)
    {
      log4jLogger.error("for db " + ConnectionUtility.buildDatabaseName(regulatoryConn), e);
    }
    finally
    {
      JDBCUtil.close(stmt, rs);
    }
    return publishedAlias;
  }

  private String populateProcessedFlag(OrderBean ob)
  {
    String flag = null;
    Statement stmt = null;
    ResultSet rs = null;
    try
    {
      StringBuffer populateProcessedFlagQuery = new StringBuffer();
      populateProcessedFlagQuery.append("select GET_MSDS_ORDER_STATUS('");
      populateProcessedFlagQuery.append(ob.getAlias());
      populateProcessedFlagQuery.append("', '");
      populateProcessedFlagQuery.append(getProduct(ob));
      populateProcessedFlagQuery.append("', '");
      populateProcessedFlagQuery.append(getPublishedAlias(ob.getAlias()));
      populateProcessedFlagQuery.append("', '");
      populateProcessedFlagQuery.append(ob.getMsdsType());
      populateProcessedFlagQuery.append("') from dual");

      stmt = regulatoryConn.createStatement();
      ((OracleStatement) stmt).defineColumnType(1, Types.VARCHAR);
      rs = stmt.executeQuery(populateProcessedFlagQuery.toString());
      if (rs.next())
        flag = rs.getString(1);
    }
    catch (Exception e)
    {
      log4jLogger.error("for db " + ConnectionUtility.buildDatabaseName(regulatoryConn), e);
    }
    finally
    {
      JDBCUtil.close(stmt, rs);
    }
    return flag;
  }

  private String populateBusinessGroup(OrderBean ob)
  {
    String businessGroup = null;
    Statement stmt = null;
    ResultSet rs = null;
    try
    {
      StringBuffer populateProcessedFlagQuery = new StringBuffer();
      populateProcessedFlagQuery.append("select GET_WERCS_DATA('");
      populateProcessedFlagQuery.append(ob.getProduct());
      populateProcessedFlagQuery.append("', '");
      populateProcessedFlagQuery.append(Constants.BUSGP);
      populateProcessedFlagQuery.append("') from dual");

      stmt = regulatoryConn.createStatement();
      ((OracleStatement) stmt).defineColumnType(1, Types.VARCHAR);
      rs = stmt.executeQuery(populateProcessedFlagQuery.toString());
      if (rs.next())
        businessGroup = rs.getString(1);
    }
    catch (Exception e)
    {
      log4jLogger.error("for db " + ConnectionUtility.buildDatabaseName(regulatoryConn), e);
    }
    finally
    {
      JDBCUtil.close(stmt, rs);
    }
    return businessGroup;
  }

  /* ****************************************************************************
 * * Method to update the rows in the custom tables VALSPAR.VCA_WERCS_SO_TXNS
 * * and VALSPAR.VCA_WERCS_PO_TXNS to
 * * indicate which rows are being processed. We set them to an 'I' here to
 * * indicated "IN PROCESS".
 * * New - Russ K 03/07/2006
 * ****************************************************************************
 */

  public void setProcessFlags()
  {
    int reg_po_num_rows = 0;
    int reg_so_num_rows = 0;
    int msds_num_rows = 0;
    int tot_rows = 0;
    Statement stmtA = null;
    Statement stmtB = null;
    Statement stmtC = null;
    CallableStatement cstmt = null;

    try
    {
      StringBuffer sql = new StringBuffer();
      sql.append("UPDATE VALSPAR.VCA_WERCS_SO_TXNS \n");
      sql.append("SET REGULATED_PROCESSED = 'I' \n");
      sql.append("WHERE REGULATED_PROCESSED IS NULL ");

      stmtA = northAmericanConn.createStatement();
      reg_so_num_rows = stmtA.executeUpdate(sql.toString());

      sql.setLength(0);
      sql.append("UPDATE VALSPAR.VCA_WERCS_SO_TXNS \n");
      sql.append("SET MSDS_PROCESSED = 'C' \n");
      sql.append("WHERE MSDS_PROCESSED IS NULL ");

      stmtB = northAmericanConn.createStatement();
      stmtB.executeUpdate(sql.toString());

      cstmt = northAmericanConn.prepareCall("{call VCA_WERCS_PKG.FLAG_VCA_WERCS_SO_TXNS(?,?,?)}");
      cstmt.registerOutParameter(1, Types.VARCHAR);
      cstmt.registerOutParameter(2, Types.VARCHAR);
      cstmt.registerOutParameter(3, Types.NUMERIC);
      cstmt.execute();

      log4jLogger.info("Status returned from VCA_WERCS_PKG.setProcessFlags " + cstmt.getString(1));

      msds_num_rows = +cstmt.getInt(3);

      if (!cstmt.getString(1).equalsIgnoreCase("S"))
        log4jLogger.error("Status returned from VCA_WERCS_PKG.FLAG_VCA_WERCS_SO_TXNS " + cstmt.getString(2));

      StringBuffer sql2 = new StringBuffer();
      sql2.append("UPDATE VALSPAR.VCA_WERCS_PO_TXNS \n");
      sql2.append("SET REGULATED_PROCESSED = 'I'  \n");
      sql2.append("WHERE REGULATED_PROCESSED IS NULL ");

      stmtC = northAmericanConn.createStatement();
      reg_po_num_rows = stmtC.executeUpdate(sql2.toString());

      tot_rows = reg_so_num_rows + msds_num_rows + reg_po_num_rows;
      log4jLogger.info("########## Regulated S.O. Rows: " + reg_so_num_rows + " ##########");
      log4jLogger.info("########## MSDS Order Rows:     " + msds_num_rows + " ##########");
      log4jLogger.info("########## Regulated P.O. Rows: " + reg_po_num_rows + " ##########");
      log4jLogger.info("########## Rows to Process:     " + tot_rows + " ##########");
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
    StringBuffer sql = new StringBuffer();
    StringBuffer sql2 = new StringBuffer();
    sql.append("SELECT COUNT('x') FROM  VALSPAR.");
    sql.append(tableName);

    int num_rows = 0;
    Statement stmt = null;
    ResultSet rs = null;
    Statement stmt2 = null;
    try
    {
      stmt = northAmericanConn.createStatement();
      rs = stmt.executeQuery(sql.toString());
      if (rs.next())
      {
        num_rows = rs.getInt(1);
        log4jLogger.info("Number of rows left in table: " + num_rows);
      }

      if (num_rows <= 0)
      {
        sql2.append("TRUNCATE TABLE VALSPAR.");
        sql2.append(tableName);

        stmt2 = northAmericanConn.createStatement();
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
    int so_num_rows = 0;
    int po_num_rows = 0;
    int tot_rows = 0;

    StringBuffer sql = new StringBuffer();
    StringBuffer sql2 = new StringBuffer();
    Statement stmt = null;
    Statement stmt2 = null;
    try
    {
      sql.append("DELETE FROM VALSPAR.VCA_WERCS_SO_TXNS \n");
      sql.append("WHERE MSDS_PROCESSED = 'C' \n");
      sql.append("AND  REGULATED_PROCESSED = 'C' ");

      stmt = northAmericanConn.createStatement();

      so_num_rows = stmt.executeUpdate(sql.toString());

      sql2.append("DELETE FROM VALSPAR.VCA_WERCS_PO_TXNS \n");
      sql2.append("WHERE REGULATED_PROCESSED = 'C' ");

      stmt2 = northAmericanConn.createStatement();

      po_num_rows = stmt2.executeUpdate(sql2.toString());

      tot_rows = so_num_rows + po_num_rows;

      log4jLogger.info("Order Rows Deleted: " + tot_rows);
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

  private ArrayList populateAddressBean(OrderBean ob)
  {
    ArrayList ar = new ArrayList();
    PreparedStatement populateAddressBeanPstmt = null;
    ResultSet populateAddressBeanRs = null;
    try
    {
      populateAddressBeanPstmt = northAmericanConn.prepareStatement("SELECT ADDR1, ADDR2, ADDR3, ADDR4, CITY, STATE, ZIP, COUNTRY, EMAIL FROM VCA_WERCS_MSDS_ADDRESSES_VIEW WHERE cust_id = ?");
      populateAddressBeanPstmt.setString(1, ob.getCustomerId());
      populateAddressBeanRs = populateAddressBeanPstmt.executeQuery();
      while (populateAddressBeanRs.next())
      {
        AddressBean ab = new AddressBean();
        String addr1 = populateAddressBeanRs.getString(1);
        String addr2 = populateAddressBeanRs.getString(2);
        String addr3 = populateAddressBeanRs.getString(3);
        String addr4 = populateAddressBeanRs.getString(4);

        StringBuffer shipAddress = new StringBuffer();
        if (addr1.compareTo(" ") != 0)
        {
          shipAddress.append(addr1);
        }
        if (addr2.compareTo(" ") != 0)
        {
          shipAddress.append(Constants.RTF_RETURN_CHAR);
          shipAddress.append(addr2);
        }
        if (addr3.compareTo(" ") != 0)
        {
          shipAddress.append(Constants.RTF_RETURN_CHAR);
          shipAddress.append(addr3);
        }
        if (addr4.compareTo(" ") != 0)
        {
          shipAddress.append(Constants.RTF_RETURN_CHAR);
          shipAddress.append(addr4);
        }

        ab.setAddress(shipAddress.toString());
        ab.setCity(populateAddressBeanRs.getString(5));
        ab.setState(populateAddressBeanRs.getString(6));
        ab.setZip(populateAddressBeanRs.getString(7));
        ab.setCountryName(populateAddressBeanRs.getString(8));
        ab.setEmail(populateAddressBeanRs.getString(9));

        ar.add(ab);
      }
    }
    catch (Exception e)
    {
      log4jLogger.error("for db " + ConnectionUtility.buildDatabaseName(northAmericanConn), e);
    }
    finally
    {
      JDBCUtil.close(populateAddressBeanPstmt, populateAddressBeanRs);
    }
    return ar;
  }

  private void insertIntoTCustomers(ArrayList ar)
  {
    OrderBean ob = null;
    PreparedStatement insertIntoTCustomersPstmt = null;
    try
    {
      insertIntoTCustomersPstmt = regulatoryConn.prepareStatement("INSERT INTO T_CUSTOMERS VALUES " + "(?," + " ' '," + " 'RQ'," + " SYSDATE-1," + " ' '," + " ' '," + " SYSDATE-1," + " null)"); // F_USER_UPDATED

      Iterator i = ar.iterator();
      while (i.hasNext())
      {
        ob = (OrderBean) i.next();

        if (ob.getInventoryType().equalsIgnoreCase(Constants.FG))
        {
          Statement customerCountStmt = null;
          ResultSet customerCountRs = null;
          try
          {
            StringBuffer customerCount = new StringBuffer();
            customerCount.append("select count(*) from t_customers where f_customer_id = '");
            customerCount.append(ob.getCustomerId());
            customerCount.append("'");
            customerCountStmt = regulatoryConn.createStatement();
            customerCountRs = customerCountStmt.executeQuery(customerCount.toString());
            customerCountRs.next();
            int count = customerCountRs.getInt(1);

            if (count == 0)
            {
              insertIntoTCustomersPstmt.setString(1, ob.getCustomerId());
              log4jLogger.info("Inserting into T_CUSTOMERS -- f_customer_id =  " + ob.getCustomerId());
              insertIntoTCustomersPstmt.executeUpdate();
              regulatoryConn.commit();
            }
          }
          catch (Exception e)
          {
            log4jLogger.error("in loop for f_customer_id = " + ob.getCustomerId() + ", db = " + ConnectionUtility.buildDatabaseName(regulatoryConn), e);
          }
          finally
          {
            JDBCUtil.close(customerCountStmt, customerCountRs);
          }
        }
      }
    }
    catch (Exception e)
    {
      log4jLogger.error("for f_customer_id = " + ob.getCustomerId() + ", db = " + ConnectionUtility.buildDatabaseName(regulatoryConn), e);
    }
    finally
    {
      JDBCUtil.close(insertIntoTCustomersPstmt);
    }
  }

  private void insertIntoTCustomerOrders(ArrayList ar)
  {
    OrderBean ob = null;
    try
    {
      String command = "{call INSERT_INTO_T_CUST_ORDERS_PROC(?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)}";

      Iterator i = ar.iterator();
      while (i.hasNext())
      {
        ob = (OrderBean) i.next();

        if (ob.getInventoryType().equalsIgnoreCase(Constants.FG))
        {
          Iterator ai = ob.getAddressBean().iterator();
          while (ai.hasNext())
          {
            AddressBean ab = (AddressBean) ai.next();

            if (ab.getEmail() != null)
              ob.setDestination("EMAIL:" + ab.getEmail());
            else
              ob.setDestination(Constants.ZERO);

            CallableStatement cstmt = regulatoryConn.prepareCall(command);
            try
            {
              cstmt.setString(1, ob.getPlant());
              cstmt.setString(2, ob.getCustomerId());
              cstmt.setString(3, ob.getProductGroup());
              cstmt.setString(4, ob.getFormat());
              cstmt.setString(5, ob.getLanguage());
              cstmt.setString(6, ab.getAddress());
              cstmt.setString(7, ab.getCity());
              cstmt.setString(8, ab.getState());
              cstmt.setString(9, ab.getZip());
              cstmt.setString(10, ob.getAttentionLine());
              cstmt.setString(11, ab.getCountryName());
              cstmt.setString(12, ob.getNumCopies());
              cstmt.setString(13, ob.getProcessedFlag());
              cstmt.setString(14, ob.getCustOrder());
              cstmt.setString(15, ob.getQuantity());
              cstmt.setString(16, ob.getUom());
              cstmt.setString(17, ob.getContainer());
              cstmt.setString(18, ob.getStandardWeight());
              cstmt.setString(19, ob.getMsdsType());
              cstmt.setString(20, ob.getDestination());
              cstmt.setString(21, ob.getPublishedAlias());
              cstmt.setString(22, ob.getProduct());
              cstmt.setString(23, ob.getDb());
              cstmt.setString(24, ob.getOrderType());
              cstmt.setString(25, ob.getPlant());
              cstmt.setString(26, ob.getSubstitutions());
              cstmt.setString(27, ob.getAlwaysSend());
              cstmt.setString(28, ob.getShipAddress());
              cstmt.registerOutParameter(29, Types.VARCHAR);
              cstmt.execute();
              String error = cstmt.getString(29);

              if (error == null)
                log4jLogger.info("Call to INSERT_INTO_T_CUST_ORDERS_PROC successful -- Cust Order = " + ob.getCustOrder() + ", Alias = " + ob.getPublishedAlias() + ", DB = " + ob.getDb());
              else
                log4jLogger.error("Call to INSERT_INTO_T_CUST_ORDERS_PROC not successful -- Cust Order = " + ob.getCustOrder() + ", Alias = " + ob.getPublishedAlias() + ", DB = " + ob.getDb() + ", error = " + error);
            }
            catch (Exception e)
            {
              log4jLogger.error("Cust Order = " + ob.getCustOrder() + " for db = " + ConnectionUtility.buildDatabaseName(regulatoryConn), e);
            }
            finally
            {
              JDBCUtil.close(cstmt);
            }
          }
        }
      }
      regulatoryConn.commit();
    }
    catch (Exception e)
    {
      log4jLogger.error("db = " + ConnectionUtility.buildDatabaseName(regulatoryConn), e);
    }
  }

  private void insertIntoVaTscaDsl(ArrayList ar)
  {
    OrderBean ob = null;
    PreparedStatement insertIntoVaTscaDslPstmt = null;
    try
    {
      insertIntoVaTscaDslPstmt = regulatoryConn.prepareStatement("INSERT INTO VA_TSCA_DSL VALUES " + "(VA_TSCA_DSL_SEQ.NEXTVAL," + " ?," + " ?," + " ?," + " ?," + " ?," + " ?," + " SYSDATE-1," + " 0," + " 0," + " ?)"); // ERROR_CODE

      Iterator i = ar.iterator();
      while (i.hasNext())
      {
        ob = (OrderBean) i.next();

        insertIntoVaTscaDslPstmt.setString(1, ob.getPlant());
        insertIntoVaTscaDslPstmt.setString(2, ob.getAlias());
        insertIntoVaTscaDslPstmt.setString(3, ob.getQuantity());
        insertIntoVaTscaDslPstmt.setString(4, ob.getCustomerId());
        insertIntoVaTscaDslPstmt.setString(5, ob.getShipToCountry());
        insertIntoVaTscaDslPstmt.setString(6, ob.getCustOrder());
        insertIntoVaTscaDslPstmt.setString(7, ob.getErrorCode());

        if (ob.getShipFromCountry().equalsIgnoreCase(Constants.US) && !ob.getShipToCountry().equalsIgnoreCase(Constants.US))
        {
          log4jLogger.info("Inserting into VA_TSCA_DSL -- CustOrder = " + ob.getCustOrder() + ", Alias = " + ob.getAlias() + ", db = " + ConnectionUtility.buildDatabaseName(regulatoryConn));
          insertIntoVaTscaDslPstmt.addBatch();
        }
      }
      insertIntoVaTscaDslPstmt.executeBatch();
      regulatoryConn.commit();
    }
    catch (Exception e)
    {
      log4jLogger.error("db = " + ConnectionUtility.buildDatabaseName(regulatoryConn), e);
    }
    finally
    {
      JDBCUtil.close(insertIntoVaTscaDslPstmt);
    }
  }

  private void insertIntoVaStateRegs(ArrayList ar)
  {
    OrderBean ob = null;
    PreparedStatement insertIntoVaStateRegsPstmt = null;
    try
    {
      insertIntoVaStateRegsPstmt = regulatoryConn.prepareStatement("INSERT INTO VA_STATE_REGS VALUES " + "(VA_STATE_REGS_SEQ.NEXTVAL," + " ?," + " ?," + " ?," + " ?," + " ?," + " ?," + " SYSDATE-1," + " 0," + " ?," + " 0)"); // PROCESSED

      Iterator i = ar.iterator();
      while (i.hasNext())
      {
        ob = (OrderBean) i.next();

        insertIntoVaStateRegsPstmt.setString(1, ob.getPlant());
        insertIntoVaStateRegsPstmt.setString(2, ob.getAlias());
        insertIntoVaStateRegsPstmt.setString(3, ob.getQuantity());
        insertIntoVaStateRegsPstmt.setString(4, ob.getCustomerId());
        insertIntoVaStateRegsPstmt.setString(5, ob.getShipToState());
        insertIntoVaStateRegsPstmt.setString(6, ob.getCustOrder());
        insertIntoVaStateRegsPstmt.setString(7, ob.getErrorCode());

        if (regulatedStatesList.contains(ob.getShipToState().toUpperCase()) && ob.getShipToCountry().equalsIgnoreCase(Constants.US))
        {
          log4jLogger.info("Inserting into VA_STATE_REGS -- CustOrder = " + ob.getCustOrder() + ", Alias = " + ob.getAlias() + ", db = " + ConnectionUtility.buildDatabaseName(regulatoryConn));
          insertIntoVaStateRegsPstmt.addBatch();
        }
      }
      insertIntoVaStateRegsPstmt.executeBatch();
      regulatoryConn.commit();
    }
    catch (Exception e)
    {
      log4jLogger.error("db = " + ConnectionUtility.buildDatabaseName(regulatoryConn), e);
    }
    finally
    {
      JDBCUtil.close(insertIntoVaStateRegsPstmt);
    }
  }

  public void changeProcessedFlags()
  {
    CallableStatement cstmt = null;
    try
    {
      log4jLogger.info("Starting changeProcessedFlags()");
      cstmt = regulatoryConn.prepareCall("{call UPDATE_T_CUSTOMER_ORDERS(?)}");
      cstmt.registerOutParameter(1, Types.VARCHAR);
      cstmt.execute();

      String error = cstmt.getString(1);
      if (error != null)
        log4jLogger.error("Error in UPDATE_T_CUSTOMER_ORDERS(): " + error);

      log4jLogger.info("Done with changeProcessedFlags()");
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

  public void updateFAlwaysSend()
  {
    CallableStatement cstmt = null;
    try
    {
      log4jLogger.info("Starting updateFAlwaysSend()");
      cstmt = regulatoryConn.prepareCall("{call UPD_F_ALWAYS_SEND(?)}");
      cstmt.registerOutParameter(1, Types.VARCHAR);
      cstmt.execute();

      String error = cstmt.getString(1);
      if (error != null)
        log4jLogger.error("Error in UPD_F_ALWAYS_SEND(): " + error);

      log4jLogger.info("Done with updateFAlwaysSend()");
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

  public void updProcessFlags(String colToUpdate)
  {
    StringBuffer sql = new StringBuffer();
    StringBuffer sql2 = new StringBuffer();
    log4jLogger.info("In updProcessFlags: " + colToUpdate);
    Statement stmt = null;
    Statement stmt2 = null;
    try
    {
      if (colToUpdate.equalsIgnoreCase("MSDS_PROCESSED"))
      {
        log4jLogger.info("Setting MSDS_PROCESSED = C");
        sql.append("UPDATE VALSPAR.VCA_WERCS_SO_TXNS \n");
        sql.append("SET " + colToUpdate + " = 'C' \n");
        sql.append("WHERE " + colToUpdate + " = 'I' ");

        stmt = northAmericanConn.createStatement();
        stmt.executeUpdate(sql.toString());
      }
      else if (colToUpdate.equalsIgnoreCase("REGULATED_PROCESSED"))
      {
        log4jLogger.info("Setting REGULATED_PROCESSED = C");
        sql.append("UPDATE VALSPAR.VCA_WERCS_SO_TXNS \n");
        sql.append("SET " + colToUpdate + " = 'C' \n");
        sql.append("WHERE " + colToUpdate + " = 'I' ");

        stmt = northAmericanConn.createStatement();
        stmt.executeUpdate(sql.toString());

        sql2.append("UPDATE VALSPAR.VCA_WERCS_PO_TXNS \n");
        sql2.append("SET " + colToUpdate + " = 'C' \n");
        sql2.append("WHERE " + colToUpdate + " = 'I' ");

        stmt2 = northAmericanConn.createStatement();
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

}
