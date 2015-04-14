package com.valspar.interfaces.regulatory.lockouts.program;

import com.valspar.interfaces.common.BaseInterface;
import com.valspar.interfaces.common.beans.ConnectionAccessBean;
import com.valspar.interfaces.common.enums.DataSource;
import com.valspar.interfaces.common.utils.*;
import java.sql.*;
import java.text.DateFormat;
import oracle.jdbc.OracleConnection;
import org.apache.log4j.Logger;

public class LockoutsInterface extends BaseInterface
{
  private static Logger log4jLogger = Logger.getLogger(LockoutsInterface.class);

  static int i = 0;
  boolean success = true;
  StringBuffer getPercent;
  StringBuffer getCompList;
  StringBuffer currYrUsage;
  StringBuffer currLfUsage;
  StringBuffer currentYearUsage;
  StringBuffer currentLifeUsage;
  StringBuffer maxThreshY;
  StringBuffer maxThreshP;
  StringBuffer maxThreshL;
  PreparedStatement currYrStmt;
  PreparedStatement currLfStmt;
  PreparedStatement currentYearStmt;
  PreparedStatement currentLifeStmt;
  PreparedStatement maxThreshStmtY;
  PreparedStatement maxThreshStmtP;
  PreparedStatement maxThreshStmtL;
  static String gemmsDB_1 = "";
  static String gemmsDB_2 = "";
  String mapperLine = "";
  String mapperFile = "";
  String errLine = "";
  StringBuffer get_products = new StringBuffer(0);
  Statement get_products_stmt;
  ResultSet get_products_rs;
  ResultSet customerOrders_rs;
  Statement customerOrders_stmt;
  StringBuffer customerOrders = new StringBuffer(0);
  String casNo = " ";
  String componentId = " ";
  String p_qty = " ";
  String year_requested = " ";
  ResultSet search_rs;
  ResultSet update_rs;
  Statement search_stmt;
  Statement update_stmt;
  Statement insert_stmt;
  Statement insert2_stmt;
  StringBuffer search_str = new StringBuffer(0);
  StringBuffer update_str = new StringBuffer(0);
  StringBuffer insert_str = new StringBuffer(0);
  StringBuffer insert2_str = new StringBuffer(0);
  String casNumber = " ";
  String component_Id = " ";
  int row_count = 0;
  StringBuffer update_processed = new StringBuffer(0);
  Statement update_processed_stmt;
  int record_count = 0;
  String updateYearUsage = "";
  String updateLifeUsage = "";
  String checkRec = " ";
  Statement insReadstmt1, insReadstmt2, updReadstmt, updateYearStmt, updateLifeStmt;
  ResultSet updReadset;
  Statement all_components_stmt, component_count_stmt;
  ResultSet all_components_rs, component_count_rs, initialLifeAmt, initialYearAmt, maxRsetY, maxRsetP, maxRsetL;
  Statement insert_lockout_stmt;
  StringBuffer insertRec = new StringBuffer(0);
  private OracleConnection regulatoryConn = null;
  private OracleConnection toERPConn = null;

  public LockoutsInterface()
  {
  }

  public void execute()
  {
    String jndiName = getParameterValue("jndiName");
    startInterface(jndiName);
  }

  public void startInterface(String jndiName)
  {
    DataSource toDataSource = CommonUtility.getDataSourceByJndiName(jndiName);
    try
    {
      if (toDataSource != null)
      {
        regulatoryConn = (OracleConnection) ConnectionAccessBean.getConnection(DataSource.REGULATORY);
        toERPConn = (OracleConnection) ConnectionAccessBean.getConnection(toDataSource);

        log4jLogger.info("LockoutsInterface - WERCS and GEMMS Connected ...");
        DateFormat longTimestamp = DateFormat.getDateTimeInstance(DateFormat.FULL, DateFormat.FULL);
        log4jLogger.info("Before refreshLockoutTable - jose - " + longTimestamp.format(new java.util.Date()));

        if (!refreshLockoutTable())
        {
          log4jLogger.info("LockoutsInterface - Error Deleting Records in VA_TR_LOCKOUT ...");
        }
        log4jLogger.info("After refreshLockoutTable - jose - " + longTimestamp.format(new java.util.Date()));
        log4jLogger.info("Before statusUpdate - jose - " + longTimestamp.format(new java.util.Date()));

        if (!statusUpdate())
        {
          log4jLogger.info("LockoutsInterface - Error in Updating the Product Status ...");
        }
        else
        {
          log4jLogger.info("LockoutsInterface - Product Status Updated ...");
        }

        log4jLogger.info("After statusUpdate - jose - " + longTimestamp.format(new java.util.Date()));
        log4jLogger.info("Before getProducts - jose - " + longTimestamp.format(new java.util.Date()));

        if (!getProducts())
        {
          log4jLogger.info("LockoutsInterface - Error in Updating with Sales/Purchase Orders Totals ...");
        }
        else
        {
          log4jLogger.info("LockoutsInterface - Sales/Purchase Orders Totals Updated ...");
        }

        log4jLogger.info("After getProducts - jose - " + longTimestamp.format(new java.util.Date()));
        log4jLogger.info("Before updateUsage - jose - " + longTimestamp.format(new java.util.Date()));

        if (!updateUsage())
        {
          log4jLogger.info("LockoutsInterface - Error Inserting DSLTRK Regulation Usage ...");
        }
        else
        {
          log4jLogger.info("LockoutsInterface -  DSLTRK Regulation Usage Processed...");
        }

        log4jLogger.info("After updateUsage - jose - " + longTimestamp.format(new java.util.Date()));
        log4jLogger.info("Before processDSLComponents - jose - " + longTimestamp.format(new java.util.Date()));

        if (!processDSLComponents())
        {
          log4jLogger.info("LockoutsInterface - Error in DSL Regulations Processing...");
        }
        else
        {
          log4jLogger.info("LockoutsInterface - DSL Regulations Processed...");
        }

        log4jLogger.info("After processDSLComponents - jose - " + longTimestamp.format(new java.util.Date()));
        log4jLogger.info("Before process_T_PROD_DATA_StateRegulations - jose - " + longTimestamp.format(new java.util.Date()));

        if (!process_T_PROD_DATA_StateRegulations("MN_HVYMTL", "MN", "THVYMTL"))
        {
          log4jLogger.info("Error Processing MN Regulations");
        }
        else
        {
          log4jLogger.info("MN Heavy Metals Processed...");
        }

        log4jLogger.info("After process_T_PROD_DATA_StateRegulations - jose - " + longTimestamp.format(new java.util.Date()));
        log4jLogger.info("Before process_T_REG_PRODUCTS_StateRegulations - jose - " + longTimestamp.format(new java.util.Date()));

        if (!process_T_REG_PRODUCTS_StateRegulations("CA_PEST"))
        {
          log4jLogger.info("Error Processing CA PEST Regulations");
        }
        else
        {
          log4jLogger.info("CA Pest Registration Processed...");
        }

        log4jLogger.info("After process_T_REG_PRODUCTS_StateRegulations - jose - " + longTimestamp.format(new java.util.Date()));
        log4jLogger.info("Before process_T_REG_PRODUCTS_StateRegulations (CA) - jose - " + longTimestamp.format(new java.util.Date()));

        if (!process_T_REG_PRODUCTS_StateRegulations("CA_REST"))
        {
          log4jLogger.info("Error Processing CA REST Regulations");
        }
        else
        {
          log4jLogger.info("CA Rest Registration Processed...");
        }

        log4jLogger.info("After process_T_REG_PRODUCTS_StateRegulations (CA) - jose - " + longTimestamp.format(new java.util.Date()));
        log4jLogger.info("Before populateLockoutCategory - jose - " + longTimestamp.format(new java.util.Date()));
        populateLockoutCategory();
        log4jLogger.info("After populateLockoutCategory - jose - " + longTimestamp.format(new java.util.Date()));
        log4jLogger.info("populateLockoutCategory() complete...");

        log4jLogger.info("Before vca_reg_lockout_log cleanup - " + longTimestamp.format(new java.util.Date()));
        if (!cleanupLockoutLogTable())
        {
          log4jLogger.info("LockoutsInterface - Error Deleting Records in vca_reg_lockout_log ...");
        }
        log4jLogger.info("After vca_reg_lockout_log cleanup - " + longTimestamp.format(new java.util.Date()));
      }
      else
      {
        log4jLogger.error("LockoutsInterface cannot run because the jndiName parameter is not valid.");
      }
    }
    catch (Exception e)
    {
      log4jLogger.error(e);
    }
    finally
    {
      JDBCUtil.close(regulatoryConn);
      JDBCUtil.close(toERPConn);
    }
  }

  public boolean statusUpdate()
  {
    StringBuffer update_status = new StringBuffer(0);
    Statement update_status_stmt;
    try
    {
      update_status.setLength(0);
      update_status.append("update va_tsca_dsl set error_code = 0 where error_code = 1 and alias in " + "(select f_alias from t_product_alias_names)");
      update_status_stmt = regulatoryConn.createStatement();
      update_status_stmt.executeQuery(update_status.toString());
    }
    catch (Exception e)
    {
      log4jLogger.error(e);
      return false;
    }
    return true;
  }

  public boolean getProducts()
  {
    try
    {
      String current_id = " ";
      String current_alias = " ";
      get_products.setLength(0);
      get_products.append("select id, alias from va_tsca_dsl where country_shipped_to = 'CA' and error_code = 0 and dsl_processed = 0 ");
      get_products_stmt = regulatoryConn.createStatement();
      get_products_rs = get_products_stmt.executeQuery(get_products.toString());
      while (get_products_rs.next())
      {
        current_id = get_products_rs.getString(1);
        current_alias = get_products_rs.getString(2);
        log4jLogger.info("PROCESSING ALIAS: " + current_alias + ", ID: " + current_id);
        salesUpdate(current_id, current_alias);
      }
      get_products_rs.close();
      get_products_stmt.close();
    }
    catch (Exception e)
    {
      log4jLogger.error(e);
      return false;
    }
    return true;
  }

  public boolean salesUpdate(String current_id, String current_alias)
  {
    try
    {
      success = true;
      customerOrders.setLength(0);
      customerOrders.append("select b.f_cas_number, b.f_component_id, ((a.quantity * b.f_percent)/100), to_char(a.date_requested, 'YYYY') " + "from t_product_alias_names c, t_prod_comp b, va_tsca_dsl a " + "where b.f_product = c.f_product and c.f_alias = a.alias " + "and a.id = " + current_id + " " + "and not exists " + "(SELECT * FROM T_COMP_DATA " + " WHERE  f_component_id = b.f_component_id " + " AND    f_cas_number   = b.f_cas_number " + " AND    f_data_code    = 'DSLCMP') ");
      customerOrders_stmt = regulatoryConn.createStatement();
      customerOrders_rs = customerOrders_stmt.executeQuery(customerOrders.toString());
      while (customerOrders_rs.next())
      {
        casNo = customerOrders_rs.getString(1);
        componentId = customerOrders_rs.getString(2);
        p_qty = customerOrders_rs.getString(3);
        year_requested = customerOrders_rs.getString(4);
        log4jLogger.info("  PROCESSING ALIAS: " + current_alias + ", ID: " + current_id + ", CAS No: " + casNo + ", ComponentId: " + componentId + ", Quantity: " + p_qty + ", Year: " + year_requested);
        success = updateSalesUsage(casNo, componentId, p_qty, year_requested);
      }
      customerOrders_rs.close();
      customerOrders_stmt.close();
      updateProcessedFlag(current_id);
      return success;
    }
    catch (Exception e)
    {
      log4jLogger.error(e);
      return false;
    }
  }

  public boolean updateUsage()
  {
    try
    {
      success = true;
      search_str.setLength(0);
      search_str.append("select f_cas_number, f_component_id " + "from t_reg_comp_compliance " + "where f_regulation = 'DSLTRK' " + "and f_comp_comply_type = 'MAXY' " + "and f_comp_comply_data_num = '.01' ");
      search_stmt = regulatoryConn.createStatement();
      search_rs = search_stmt.executeQuery(search_str.toString());
      while (search_rs.next())
      {
        casNumber = search_rs.getString(1);
        component_Id = search_rs.getString(2);
        update_str.setLength(0);
        update_str.append("select count(*) " + "from t_usage_totals_comp " + "where f_cas_number = '" + casNumber + "' " + "and f_comp_id = '" + component_Id + "' ");
        update_stmt = regulatoryConn.createStatement();
        update_rs = update_stmt.executeQuery(update_str.toString());
        if (update_rs.next())
        {
          row_count = update_rs.getInt(1);
        }
        update_rs.close();
        update_stmt.close();
        if (row_count == 0)
        {
          try
          {
            log4jLogger.info("    Inserted for DSL Usage  : " + component_Id);
            insert_str.setLength(0);
            insert_str.append("insert into t_usage_totals_comp values " + "( '" + casNumber + "','" + component_Id + "','007', TO_CHAR(SYSDATE-1, 'YYYY') ,'YEAR',0,0,0,0,0,0,0,0,0,0,0,0,0, " + "  '.01',NULL,NULL,NULL, 'Not Sold') ");
            insert_stmt = regulatoryConn.createStatement();
            insert_stmt.executeQuery(insert_str.toString());
            insert_stmt.close();
            insert2_str.setLength(0);
            insert2_str.append("insert into t_usage_totals_comp values " + "( '" + casNumber + "','" + component_Id + "','007','0' ,'LIFE',0,0,0,0,0,0,0,0,0,0,0,0,0, " + "  '.01',NULL,NULL,NULL, 'Not Sold') ");
            insert2_stmt = regulatoryConn.createStatement();
            insert2_stmt.executeQuery(insert2_str.toString());
            insert2_stmt.close();
          }
          catch (Exception e)
          {
            log4jLogger.error("Already Exsits in T_USAGE_TOTALS_COMP for Component " + component_Id, e);
            return false;
          }
        }
      }
      search_rs.close();
      search_stmt.close();
      return success;
    }
    catch (Exception e)
    {
      log4jLogger.error(e);
      return false;
    }
  }

  public boolean updateSalesUsage(String casNo, String componentId, String quantity, String year_requested)
  {
    success = true;
    try
    {
      checkRec = " SELECT COUNT(*) FROM T_USAGE_TOTALS_COMP " + " WHERE  F_COMP_ID = '" + componentId + "' " + " AND    F_USAGE_YEAR = '" + year_requested + "'";
      updReadstmt = regulatoryConn.createStatement();
      updReadset = updReadstmt.executeQuery(checkRec);
      if (updReadset.next())
      {
        record_count = updReadset.getInt(1);
      }
      updReadset.close();
      updReadstmt.close();
      if (record_count == 0)
      {
        try
        {
          String insUsage1 = " INSERT INTO T_USAGE_TOTALS_COMP " + " VALUES ( '" + casNo + "' , '" + componentId + "' , " + "          ' ', '" + year_requested + "', 'YEAR' ,0,0,0,0,0,0,0,0,0,0,0,0,0, " + quantity + ", null, null, null, null ) ";
          insReadstmt1 = regulatoryConn.createStatement();
          insReadstmt1.executeQuery(insUsage1);
          insReadstmt1.close();
          checkRec = " SELECT COUNT(*) FROM T_USAGE_TOTALS_COMP " + " WHERE  F_COMP_ID = '" + componentId + "' " + " AND    F_USAGE_TYPE = 'LIFE'";
          updReadstmt = regulatoryConn.createStatement();
          updReadset = updReadstmt.executeQuery(checkRec);
          if (updReadset.next())
          {
            record_count = updReadset.getInt(1);
          }
          updReadset.close();
          updReadstmt.close();
          if (record_count == 0)
          {
            String insUsage2 = " INSERT INTO T_USAGE_TOTALS_COMP " + " VALUES ( '" + casNo + "' , '" + componentId + "' , " + "          ' ', '0000', 'LIFE' ,0,0,0,0,0,0,0,0,0,0,0,0,0, " + quantity + ", null, null, null, null ) ";
            insReadstmt2 = regulatoryConn.createStatement();
            insReadstmt2.executeUpdate(insUsage2);
            insReadstmt2.close();
            log4jLogger.info("    Inserted Order Usage Totals for : " + componentId);
          }
          else
          {
            try
            {
              updateLifeUsage = " UPDATE t_usage_totals_comp " + " SET    f_ytd = f_ytd + " + quantity + " " + " WHERE  f_comp_id    =  '" + componentId + "' " + " AND    f_usage_type = 'LIFE'";
              updateLifeStmt = regulatoryConn.createStatement();
              updateLifeStmt.executeUpdate(updateLifeUsage);
              updateLifeStmt.close();
              log4jLogger.info("    Updated Order Usage Totals for : " + componentId);
            }
            catch (Exception e)
            {
              log4jLogger.error("Updating Order Details in T_USAGE_TOTALS_COMP for Component " + componentId, e);
              return false;
            }
          }
        }
        catch (Exception e)
        {
          log4jLogger.error(e);
          return false;
        }
      }
      else
      {
        try
        {
          updateYearUsage = " UPDATE t_usage_totals_comp " + " SET    f_ytd = f_ytd + " + quantity + " " + " WHERE  f_comp_id    =  '" + componentId + "' " + " AND    (f_usage_year = '" + year_requested + "' " + " OR     f_usage_type = 'LIFE')";
          updateYearStmt = regulatoryConn.createStatement();
          updateYearStmt.executeUpdate(updateYearUsage);
          updateYearStmt.close();
          log4jLogger.info("    Updated Order Usage Totals for : " + componentId);
        }
        catch (Exception e)
        {
          log4jLogger.error("Updating Order Details in T_USAGE_TOTALS_COMP for Component " + componentId, e);
          return false;
        }
      }
      return success;
    }
    catch (Exception e)
    {
      log4jLogger.error("Reading T_USAGE_TOTALS_COMP while Updating Usage Totals for " + componentId, e);
      return false;
    }
  }

  private void updateProcessedFlag(String current_id)
  {
    try
    {
      update_processed.setLength(0);
      update_processed.append("update va_tsca_dsl set dsl_processed = 1 where id = " + current_id);
      update_processed_stmt = regulatoryConn.createStatement();
      update_processed_stmt.executeUpdate(update_processed.toString());
      update_processed_stmt.close();
    }
    catch (Exception e)
    {
      log4jLogger.error(e);
    }
  }

  public boolean refreshLockoutTable()
  {
    String deleteRecords = "TRUNCATE TABLE VALSPAR.VA_TR_LOCKOUT";
    try
    {
      Statement stmt = toERPConn.createStatement();
      log4jLogger.info("Truncating VA_TR_LOCKOUT in " + ConnectionUtility.buildDatabaseName(toERPConn));
      stmt.executeUpdate(deleteRecords);
      stmt.close();
    }
    catch (Exception e)
    {
      log4jLogger.error("Deleting Records in VA_TR_LOCKOUT in Database " + ConnectionUtility.buildDatabaseName(toERPConn), e);
      return false;
    }
    return true;
  }

  public boolean processDSLComponents()
  {
    success = true;
    int rcount = 0;
    String compId = " ";
    String component_count = " ";
    StringBuffer currentYearUsage = new StringBuffer(0);
    StringBuffer currentLifeUsage = new StringBuffer(0);
    StringBuffer maxThreshY = new StringBuffer(0);
    StringBuffer maxThreshP = new StringBuffer(0);
    StringBuffer maxThreshL = new StringBuffer(0);
    double initialYAmt = 0.0;
    double initialLAmt = 0.0;
    double maxYamt = 0.0;
    double maxLamt = 0.0;
    double maxPamt = 0.0;
    double newAmt = 0.0;
    String all_components = "SELECT DISTINCT A.F_COMP_ID " + " FROM   T_USAGE_TOTALS_COMP A " + " WHERE F_USAGE_YEAR = TO_CHAR(SYSDATE-1, 'YYYY') " + " AND NOT EXISTS " + " (SELECT * FROM T_COMP_DATA " + "  WHERE  f_component_id = a.f_comp_id " + "  AND    f_data_code    = 'DSLCMP') ";
    try
    {
      all_components_stmt = regulatoryConn.createStatement();
      all_components_rs = all_components_stmt.executeQuery(all_components);
      while (all_components_rs.next())
      {
        compId = all_components_rs.getString(1);
        component_count = " SELECT count(*) " + " FROM   t_reg_comp_compliance " + " WHERE  f_regulation = 'DSLTRK' " + " AND    f_component_id = '" + compId + "' ";
        rcount = 0;
        component_count_stmt = regulatoryConn.createStatement();
        component_count_rs = component_count_stmt.executeQuery(component_count);
        if (component_count_rs.next())
        {
          rcount = component_count_rs.getInt(1);
        }
        component_count_rs.close();
        component_count_stmt.close();
        log4jLogger.info("DSL Component : " + compId);
        if (rcount == 0)
        {
          insertLockout(compId, "DSL");
        }
        else
        {
          initialYAmt = 0.0;
          initialLAmt = 0.0;
          maxYamt = 0.0;
          maxLamt = 0.0;
          maxPamt = 0.0;
          newAmt = 0.0;
          try
          {
            currentYearUsage.setLength(0);
            currentYearUsage.append("SELECT F_YTD FROM T_USAGE_TOTALS_COMP WHERE F_USAGE_YEAR = TO_CHAR(SYSDATE-1, 'YYYY') AND F_COMP_ID = ?");
            currentYearStmt = regulatoryConn.prepareStatement(currentYearUsage.toString());
            currentYearStmt.setString(1, compId);
            initialYearAmt = currentYearStmt.executeQuery();
            if (initialYearAmt.next())
              initialYAmt = (new Double(initialYearAmt.getString(1))).doubleValue();
            else
              log4jLogger.info("No year usage was found for component " + compId);
            initialYearAmt.close();
            currentYearStmt.close();
          }
          catch (Exception e)
          {
            currentYearStmt.close();
            log4jLogger.error("component " + compId, e);
          }
          try
          {
            currentLifeUsage.setLength(0);
            currentLifeUsage.append("SELECT F_YTD FROM T_USAGE_TOTALS_COMP WHERE F_USAGE_TYPE = 'LIFE' AND F_COMP_ID = ?");
            currentLifeStmt = regulatoryConn.prepareStatement(currentLifeUsage.toString());
            currentLifeStmt.setString(1, compId);
            initialLifeAmt = currentLifeStmt.executeQuery();
            if (initialLifeAmt.next())
              initialLAmt = (new Double(initialLifeAmt.getString(1))).doubleValue();
            initialLifeAmt.close();
            currentLifeStmt.close();
          }
          catch (Exception e)
          {
            currentLifeStmt.close();
            log4jLogger.error("component " + compId, e);
          }
          try
          {
            maxThreshY.setLength(0);
            maxThreshY.append("SELECT F_COMP_COMPLY_DATA_NUM FROM T_REG_COMP_COMPLIANCE WHERE F_COMP_COMPLY_TYPE = 'MAXY' AND F_COMPONENT_ID = ?");
            maxThreshStmtY = regulatoryConn.prepareStatement(maxThreshY.toString());
            maxThreshStmtY.setString(1, compId);
            maxRsetY = maxThreshStmtY.executeQuery();
            if (maxRsetY.next())
              maxYamt = (new Double(maxRsetY.getString(1))).doubleValue();
            maxRsetY.close();
            maxThreshStmtY.close();
          }
          catch (Exception e)
          {
            maxThreshStmtY.close();
            log4jLogger.error("component " + compId, e);
          }
          try
          {
            maxThreshP.setLength(0);
            maxThreshP.append("SELECT F_COMP_COMPLY_DATA_NUM FROM T_REG_COMP_COMPLIANCE WHERE F_COMP_COMPLY_TYPE = 'TPCT' AND F_COMPONENT_ID = ?");
            maxThreshStmtP = regulatoryConn.prepareStatement(maxThreshP.toString());
            maxThreshStmtP.setString(1, compId);
            maxRsetP = maxThreshStmtP.executeQuery();
            while (maxRsetP.next())
              maxPamt = (new Double(maxRsetP.getString(1))).doubleValue();
            maxRsetP.close();
            maxThreshStmtP.close();
          }
          catch (Exception e)
          {
            maxThreshStmtP.close();
            log4jLogger.error("component " + compId, e);
          }
          if ((maxYamt != 0.0) && (maxPamt != 0.0))
          {
            newAmt = ((maxYamt * maxPamt) / 100);
          }
          if (initialYAmt >= newAmt)
          {
            success = insertLockout(compId, "DSL");
          }
          else
          {
            try
            {
              maxThreshL.setLength(0);
              maxThreshL.append("SELECT F_COMP_COMPLY_DATA_NUM FROM T_REG_COMP_COMPLIANCE WHERE F_COMP_COMPLY_TYPE = 'MAXT' AND F_COMPONENT_ID = ?");
              maxThreshStmtL = regulatoryConn.prepareStatement(maxThreshL.toString());
              maxThreshStmtL.setString(1, compId);
              maxRsetL = maxThreshStmtL.executeQuery();
              while (maxRsetL.next())
                maxLamt = (new Double(maxRsetL.getString(1))).doubleValue();
              maxRsetL.close();
              maxThreshStmtL.close();
            }
            catch (Exception e)
            {
              maxThreshStmtL.close();
              log4jLogger.error("component " + compId, e);
            }
            if (initialLAmt > maxLamt)
              success = insertLockout(compId, "DSL");
          }
        }
      }
      all_components_rs.close();
      all_components_stmt.close();
      return success;
    }
    catch (Exception e)
    {
      log4jLogger.error("component " + compId, e);
      return false;
    }
  }

  private boolean insertLockout(String componentId, String regulation)
  {
    success = true;
    insertRec.setLength(0);
    insertRec.append(" INSERT INTO VALSPAR.va_tr_lockout ");
    insertRec.append("   (SELECT b.f_alias, '" + regulation + "', 'YES', sysdate ");
    insertRec.append("    FROM   t_prod_comp@TORG a, t_product_alias_names@TORG b ");
    insertRec.append("    WHERE  a.f_component_id = '" + componentId + "' ");
    insertRec.append("    AND    a.f_product = b.f_product ");
    insertRec.append("    AND    a.f_percent > 0 ");
    insertRec.append("    AND    NOT EXISTS ");
    insertRec.append("           (SELECT * FROM VALSPAR.va_tr_lockout ");
    insertRec.append("            WHERE F_PRODUCT = b.f_alias)) ");
    int err_counter = 0;
    try
    {
      insert_lockout_stmt = toERPConn.createStatement();
      insert_lockout_stmt.executeUpdate(insertRec.toString());
      insert_lockout_stmt.close();
      log4jLogger.info("Lockout inserted for all aliases containing " + componentId + " in " + ConnectionUtility.buildDatabaseName(toERPConn));
    }
    catch (Exception e)
    {
      err_counter++;
      log4jLogger.error(e);
    }
    if (err_counter > 0)
      success = false;
    return success;
  }

  public boolean process_T_PROD_DATA_StateRegulations(String regulation, String state, String data_code)
  {
    String product = "";
    StringBuffer stateRegulation = new StringBuffer(0);
    try
    {
      stateRegulation.append("select /*+ index(c,T_PROD_DATA_KEY) */  distinct b.f_product ");
      stateRegulation.append("from t_prod_data c, t_product_alias_names b ");
      stateRegulation.append("where b.f_product = c.f_product ");
      stateRegulation.append("and   c.f_data_code = ?");
      stateRegulation.append("and   c.f_data > 1");
      stateRegulation.append("and   not exists ");
      stateRegulation.append(" (SELECT f_alias FROM va_reg_bypasses");
      stateRegulation.append("  WHERE  f_alias = b.f_alias");
      stateRegulation.append("  AND    f_regulation = ?)");
      PreparedStatement stateRegulation_pstmt = regulatoryConn.prepareStatement(stateRegulation.toString());
      stateRegulation_pstmt.setString(1, data_code);
      if (regulation.equalsIgnoreCase("MN_HVYMTL"))
        stateRegulation_pstmt.setString(2, "MN_HVYM");
      else
        stateRegulation_pstmt.setString(2, regulation);
      log4jLogger.info("Getting products for Regulation: " + regulation + ", State: " + state + ", Data Code: " + data_code);
      ResultSet stateRegulation_rs = stateRegulation_pstmt.executeQuery();

      if (state.compareTo("MN") == 0)
      {
        while (stateRegulation_rs.next())
        {
          product = stateRegulation_rs.getString(1);
          get_all_aliases_and_insert(product, regulation);
        }
      }
    }
    catch (Exception e)
    {
      log4jLogger.error(e);
      return false;
    }
    return true;
  }

  public boolean process_T_REG_PRODUCTS_StateRegulations(String regulation)
  {
    String product = "";
    StringBuffer stateRegulation = new StringBuffer(0);
    stateRegulation.append("select a.f_product from t_reg_products a where a.f_regulation = '" + regulation + "' ");
    stateRegulation.append(" and   not exists ");
    stateRegulation.append(" (SELECT f_alias FROM va_reg_bypasses ");
    stateRegulation.append("  WHERE  f_alias = a.f_product ");
    stateRegulation.append("  AND    f_regulation = '" + regulation + "' )");
    try
    {
      PreparedStatement stateRegulation_pstmt = regulatoryConn.prepareStatement(stateRegulation.toString());
      ResultSet stateRegulation_rs = stateRegulation_pstmt.executeQuery();
      while (stateRegulation_rs.next())
      {
        product = stateRegulation_rs.getString(1);
        get_all_aliases_and_insert(product, regulation);
      }
      stateRegulation_rs.close();
      stateRegulation_pstmt.close();
    }
    catch (Exception e)
    {
      log4jLogger.error(e);
      return false;
    }
    return true;
  }

  private boolean get_all_aliases_and_insert(String product, String regulation)
  {
    try
    {
      String alias = "";
      PreparedStatement get_all_aliases_pstmt = regulatoryConn.prepareStatement("select f_alias from t_product_alias_names where f_product = ?");
      get_all_aliases_pstmt.setString(1, product);
      ResultSet get_all_aliases_rs = get_all_aliases_pstmt.executeQuery();
      while (get_all_aliases_rs.next())
      {
        alias = get_all_aliases_rs.getString(1);
        checkAndInsert(alias, regulation);
      }
      get_all_aliases_rs.close();
      get_all_aliases_pstmt.close();
    }
    catch (Exception e)
    {
      log4jLogger.error(e);
      return false;
    }
    return true;
  }

  private boolean checkAndInsert(String alias, String regulation)
  {
    try
    {
      Statement stmt;
      String insert = "INSERT INTO VALSPAR.VA_TR_LOCKOUT VALUES ('" + alias + "','" + regulation + "','YES', SYSDATE)";
      log4jLogger.info("Inserting lockout for " + alias + " with " + regulation + " regulation.");
      stmt = toERPConn.createStatement();
      stmt.executeUpdate(insert);
      stmt.close();
      return true;
    }
    catch (Exception e)
    {
      log4jLogger.error(e);
      return false;
    }
  }

  public void populateLockoutCategory()
  {
    try
    {
      String command = "{call vca_reg_lockout.Populate_lockout_category()}";
      CallableStatement cstmt = toERPConn.prepareCall(command);
      cstmt.execute();
      cstmt.close();
    }
    catch (Exception e)
    {
      log4jLogger.error(e);
    }
  }

  public boolean cleanupLockoutLogTable()
  {
    String deleteRecords = "DELETE FROM valspar.vca_reg_lockout_log WHERE date_created < SYSDATE - 180";
    try
    {
      Statement stmt = toERPConn.createStatement();
      log4jLogger.info("Deleting Records in vca_reg_lockout_log in " + ConnectionUtility.buildDatabaseName(toERPConn));
      stmt.executeUpdate(deleteRecords);
      stmt.close();
    }
    catch (Exception e)
    {
      log4jLogger.error("Deleting Records in vca_reg_lockout_log in Database " + ConnectionUtility.buildDatabaseName(toERPConn), e);
      return false;
    }
    return true;
  }
}
