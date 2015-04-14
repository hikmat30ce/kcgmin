package com.valspar.interfaces.wercs.lockouts.program;

import com.valspar.interfaces.common.BaseInterface;
import com.valspar.interfaces.common.beans.ConnectionAccessBean;
import com.valspar.interfaces.common.enums.DataSource;
import com.valspar.interfaces.common.utils.*;
import com.valspar.interfaces.wercs.lockouts.beans.DSLCompValuesBean;
import com.valspar.interfaces.wercs.lockouts.beans.LockoutsBean;
import java.sql.*;
import oracle.jdbc.OracleConnection;
import oracle.jdbc.OraclePreparedStatement;
import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;

public class LockoutsInterface extends BaseInterface
{
  private OracleConnection wercsConn = null;
  private OracleConnection toERPConn = null;
  private static Logger log4jLogger = Logger.getLogger(LockoutsInterface.class);

  public LockoutsInterface()
  {
  }

  public void execute()
  {
    String instance = getParameterValue("instance");
    startInterface(instance);
  }

  public void startInterface(String instance)
  {
    DataSource toDataSource = CommonUtility.getDataSourceBy11iInstance(instance);
    try
    {
      if (toDataSource != null)
      {
        setWercsConn((OracleConnection) ConnectionAccessBean.getConnection(DataSource.WERCS));
        setToERPConn((OracleConnection) ConnectionAccessBean.getConnection(toDataSource));

        log4jLogger.info("LockoutsInterface - WERCS and ERP(" + instance + ") Connected ...");
        log4jLogger.info("Before refreshLockoutTable");
        refreshLockoutTable();
        log4jLogger.info("After refreshLockoutTable ");
        log4jLogger.info("Before statusUpdate");
        updateStatus();
        log4jLogger.info("After statusUpdate ");

        log4jLogger.info("Before getProducts ");
        getProducts();
        log4jLogger.info("After getProducts  ");

        log4jLogger.info("Before updateUsage ");
        updateUsage();
        log4jLogger.info("After updateUsage  ");

        log4jLogger.info("Before processDSLComponents ");
        processDSLComponents();
        log4jLogger.info("After processDSLComponents ");

        log4jLogger.info("Before process_T_PROD_DATA_StateRegulations ");
        processTProdDDataStateRegulations("MN_HVYMTL", "MN", "THVYMTL");
        log4jLogger.info("After process_T_PROD_DATA_StateRegulations ");

        log4jLogger.info("Before process_T_REG_PRODUCTS_StateRegulations ");
        processTRegProductsStateRegulations("CA_PEST");
        log4jLogger.info("After process_T_REG_PRODUCTS_StateRegulations ");

        log4jLogger.info("Before process_T_REG_PRODUCTS_StateRegulations (CA) ");
        processTRegProductsStateRegulations("CA_REST");
        log4jLogger.info("After process_T_REG_PRODUCTS_StateRegulations (CA) ");

        log4jLogger.info("Before populateLockoutCategory ");
        populateLockoutCategory();
        log4jLogger.info("After populateLockoutCategory  ");

        log4jLogger.info("Before vca_reg_lockout_log cleanup - ");
        cleanupLockoutLogTable();
        log4jLogger.info("After vca_reg_lockout_log cleanup - ");
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
      JDBCUtil.close(getWercsConn());
      JDBCUtil.close(getToERPConn());
    }
  }

  public void updateStatus()
  {
    OraclePreparedStatement pst = null;
    StringBuilder sb = new StringBuilder();
    sb.append("update va_tsca_dsl set error_code = 0 where error_code = 1 and alias in ( ");
    sb.append("select f_alias from t_product_alias_names) ");
    try
    {
      pst = (OraclePreparedStatement) getWercsConn().prepareStatement(sb.toString());
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

  public void getProducts()
  {
    OraclePreparedStatement pst = null;
    ResultSet rs = null;
    StringBuilder sb = new StringBuilder();
    sb.append("select id, alias from va_tsca_dsl where country_shipped_to = 'CAN' ");
    sb.append("and error_code = 0 and dsl_processed = 0 ");
    try
    {
      pst = (OraclePreparedStatement) getWercsConn().prepareStatement(sb.toString());
      rs = pst.executeQuery();
      while (rs.next())
      {
        log4jLogger.info("PROCESSING ALIAS: " + rs.getString(2) + ", ID: " + rs.getString(1));
        updateSales(rs.getString(1), rs.getString(2));
      }
    }
    catch (Exception e)
    {
      log4jLogger.error(e);
    }
    finally
    {
      JDBCUtil.close(pst, rs);
    }
  }

  public void updateSales(String currentId, String currentAlias)
  {
    OraclePreparedStatement pst = null;
    ResultSet rs = null;
    StringBuilder sb = new StringBuilder();
    sb.append("select b.f_cas_number, b.f_component_id, ( (a.quantity * b.f_percent) / 100) qty , to_char (a.date_requested, 'YYYY') year_requested ");
    sb.append("from t_product_alias_names c, t_prod_comp b, va_tsca_dsl a ");
    sb.append("where b.f_product = c.f_product and c.f_alias = a.alias and a.id = :CURRENTID ");
    sb.append("and not exists (select 'X' from t_comp_data ");
    sb.append("    where f_component_id = b.f_component_id and f_cas_number = b.f_cas_number and f_data_code = 'DSLCMP') ");
    try
    {
      pst = (OraclePreparedStatement) getWercsConn().prepareStatement(sb.toString());
      pst.setStringAtName("CURRENTID", currentId);
      rs = pst.executeQuery();
      while (rs.next())
      {
        LockoutsBean lockoutsBean = new LockoutsBean();
        lockoutsBean.setCasNumber(rs.getString(1));
        lockoutsBean.setComponentId(rs.getString(2));
        lockoutsBean.setQuantity(rs.getString(3));
        lockoutsBean.setYearRequested(rs.getString(4));
        log4jLogger.info("PROCESSING ALIAS: " + currentAlias + ", ID: " + currentId + " " + lockoutsBean.toString());
        updateSalesUsage(lockoutsBean);
      }
      updateProcessedFlag(currentId);
    }
    catch (Exception e)
    {
      log4jLogger.error(e);
    }
    finally
    {
      JDBCUtil.close(pst, rs);
    }
  }

  public void updateUsage()
  {
    OraclePreparedStatement pst = null;
    ResultSet rs = null;
    StringBuilder sb = new StringBuilder();
    sb.append("select f_cas_number, f_component_id from t_reg_comp_compliance ");
    sb.append("where f_regulation = 'DSLTRK' and f_comp_comply_type = 'MAXY' and f_comp_comply_data_num = '.01' ");
    try
    {
      pst = (OraclePreparedStatement) getWercsConn().prepareStatement(sb.toString());
      rs = pst.executeQuery();
      while (rs.next())
      {
        LockoutsBean lockoutsBean = new LockoutsBean();
        lockoutsBean.setCasNumber(rs.getString(1));
        lockoutsBean.setComponentId(rs.getString(2));
        if (!checkUsageTotalsComp(lockoutsBean, "DSL"))
        {
          log4jLogger.info("    Inserted for DSL Usage  : " + lockoutsBean.getComponentId());
          insertUsageTotalsComp(lockoutsBean, "YEAR", true);
          insertUsageTotalsComp(lockoutsBean, "LIFE", true);
        }
      }
    }
    catch (Exception e)
    {
      log4jLogger.error(e);
    }
    finally
    {
      JDBCUtil.close(pst, rs);
    }
  }

  public void updateSalesUsage(LockoutsBean lockooutsBean)
  {
    if (checkUsageTotalsComp(lockooutsBean, "YEAR"))
    {
      updateUsageTotalsComp(lockooutsBean, "YEARANDLIFE");
    }
    else
    {
      insertUsageTotalsComp(lockooutsBean, "YEAR", false);
      if (checkUsageTotalsComp(lockooutsBean, "LIFE"))
      {
        updateUsageTotalsComp(lockooutsBean, "LIFE");
      }
      {
        insertUsageTotalsComp(lockooutsBean, "LIFE", false);
      }
    }
  }

  private void updateProcessedFlag(String currentId)
  {
    OraclePreparedStatement pst = null;
    ResultSet rs = null;
    try
    {
      pst = (OraclePreparedStatement) getWercsConn().prepareStatement("update va_tsca_dsl set dsl_processed = 1 where id = :CURRENTID ");
      pst.setStringAtName("CURRENTID", currentId);
      pst.executeUpdate();
    }
    catch (Exception e)
    {
      log4jLogger.error(e);
    }
    finally
    {
      JDBCUtil.close(pst, rs);
    }
  }

  public void refreshLockoutTable()
  {
    Statement stmt = null;
    try
    {
      stmt = getToERPConn().createStatement(); 
      log4jLogger.info("Truncating VA_TR_LOCKOUT in " + ConnectionUtility.buildDatabaseName(getToERPConn()));
      stmt.executeUpdate("TRUNCATE TABLE VALSPAR.VA_TR_LOCKOUT");
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

  public void processDSLComponents()
  {
    OraclePreparedStatement pst = null;
    ResultSet rs = null;
    StringBuilder sb = new StringBuilder();
    sb.append("select distinct a.f_comp_id from t_usage_totals_comp a where f_usage_year = to_char (sysdate - 1, 'YYYY') ");
    sb.append("and not exists (select * from t_comp_data where f_component_id = a.f_comp_id and f_data_code = 'DSLCMP') ");

    try
    {
      pst = (OraclePreparedStatement) getWercsConn().prepareStatement(sb.toString());
      rs = pst.executeQuery();
      while (rs.next())
      {
        String compId = rs.getString(1);
        log4jLogger.info("DSL Component : " + compId);
        checkDSLComponentAndInsert(compId);
      }
    }
    catch (Exception e)
    {
      log4jLogger.error(e);
    }
    finally
    {
      JDBCUtil.close(pst, rs);
    }
  }

  public void checkDSLComponentAndInsert(String compId)
  {
    OraclePreparedStatement pst = null;
    ResultSet rs = null;
    try
    {
      pst = (OraclePreparedStatement) getWercsConn().prepareStatement("select 'X' from t_reg_comp_compliance where  f_regulation = 'DSLTRK' and f_component_id = :COMPID ");
      pst.setStringAtName("COMPID", compId);
      rs = pst.executeQuery();
      if (rs.next())
      {
        findDSLComponentValuesDoInsert(compId);
      }
      else
      {
        insertLockout(compId, "DSL");
      }
    }
    catch (Exception e)
    {
      log4jLogger.error(e);
    }
    finally
    {
      JDBCUtil.close(pst, rs);
    }
  }

  public void findDSLComponentValuesDoInsert(String compId)
  {
    OraclePreparedStatement pst = null;
    ResultSet rs = null;
    StringBuilder sb = new StringBuilder();
    sb.append("select ");
    sb.append("(select f_ytd from t_usage_totals_comp where f_usage_year = to_char(sysdate-1, 'YYYY') and f_comp_id = :COMPID) init_year_amt, ");
    sb.append("(select f_ytd from t_usage_totals_comp where f_usage_type = 'LIFE' and f_comp_id = :COMPID) init_life_amt, ");
    sb.append("(select max(f_comp_comply_data_num) from t_reg_comp_compliance where f_comp_comply_type = 'MAXY' and f_component_id = :COMPID) max_year_amt, ");
    sb.append("(select max(f_comp_comply_data_num) from t_reg_comp_compliance where f_comp_comply_type = 'TPCT' and f_component_id = :COMPID) max_tpct_amt, ");
    sb.append("(select max(f_comp_comply_data_num) from t_reg_comp_compliance where f_comp_comply_type = 'MAXT' and f_component_id = :COMPID) max_life_amt ");
    sb.append("from dual ");

    try
    {
      pst = (OraclePreparedStatement) getWercsConn().prepareStatement(sb.toString());
      rs = pst.executeQuery();
      if (rs.next())
      {
        DSLCompValuesBean dslCompValuesBean = new DSLCompValuesBean();
        dslCompValuesBean.setComponentId(compId);
        dslCompValuesBean.setInitYearAmt(rs.getDouble("init_year_amt"));
        dslCompValuesBean.setInitLifeAmt(rs.getDouble("init_life_amt"));
        double maxYearAmt = rs.getDouble("max_year_amt");
        double maxTpctAmt = rs.getDouble("max_tpct_amt");
        dslCompValuesBean.setMaxYearAmt(maxYearAmt);
        dslCompValuesBean.setMaxTpctAmt(maxTpctAmt);
        dslCompValuesBean.setMaxLifeAmt(rs.getDouble("max_life_amt"));
        if ((maxYearAmt != 0.0) && (maxTpctAmt != 0.0))
        {
          if ((dslCompValuesBean.getInitYearAmt() > maxYearAmt * maxTpctAmt / 100) || (dslCompValuesBean.getInitYearAmt() > dslCompValuesBean.getMaxLifeAmt()))
          {
            insertLockout(compId, "DSL");
          }
        }
      }
    }
    catch (Exception e)
    {
      log4jLogger.error(e);
    }
    finally
    {
      JDBCUtil.close(pst, rs);
    }
  }

  private void insertLockout(String componentId, String regulation)
  {
    OraclePreparedStatement pst = null;
    StringBuilder sb = new StringBuilder();
    sb.append("insert into valspar.va_tr_lockout ");
    sb.append("(select b.f_alias, :REGULATION, 'YES', sysdate ");
    sb.append("from t_prod_comp@to_rg a, t_product_alias_names@to_rg b ");
    sb.append("where a.f_component_id = :COMPID ");
    sb.append("and a.f_product = b.f_product and a.f_percent > 0 ");
    sb.append("and not exists (select 'X' from valspar.va_tr_lockout where f_product = b.f_alias)) ");
    try
    {
      pst = (OraclePreparedStatement) getToERPConn().prepareStatement(sb.toString());
      pst.setStringAtName("REGULATION", regulation);
      pst.setStringAtName("COMPID", componentId);
      pst.executeUpdate();
      log4jLogger.info("Lockout inserted for all aliases containing " + componentId + " in " + ConnectionUtility.buildDatabaseName(getToERPConn()));
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

  public void processTProdDDataStateRegulations(String regulation, String state, String dataCode)
  {
    OraclePreparedStatement pst = null;
    ResultSet rs = null;
    StringBuilder sb = new StringBuilder();
    sb.append("select /*+ index(c,T_PROD_DATA_KEY) */  distinct b.f_product ");
    sb.append("from t_prod_data c, t_product_alias_names b ");
    sb.append("where b.f_product = c.f_product ");
    sb.append("and   c.f_data_code = :DATACODE ");
    sb.append("and   c.f_data > 1");
    sb.append("and   not exists ");
    sb.append(" (SELECT f_alias FROM va_reg_bypasses");
    sb.append("  WHERE  f_alias = b.f_alias");
    sb.append("  AND    f_regulation = :REGULATION)");
    try
    {
      pst = (OraclePreparedStatement) getWercsConn().prepareStatement(sb.toString());
      pst.setStringAtName("DATACODE", dataCode);
      if (StringUtils.equalsIgnoreCase(regulation, "MN_HVYMTL"))
      {
        pst.setStringAtName("REGULATION", "MN_HVYM");
      }
      else
      {
        pst.setStringAtName("REGULATION", regulation);
      }
      rs = pst.executeQuery();
      log4jLogger.info("Getting products for Regulation: " + regulation + ", State: " + state + ", Data Code: " + dataCode);

      if (StringUtils.equalsIgnoreCase(state, "MN"))
      {
        while (rs.next())
        {
          insertVaTrLockout(rs.getString(1), regulation);
        }
      }
    }
    catch (Exception e)
    {
      log4jLogger.error(e);
    }
    finally
    {
      JDBCUtil.close(pst, rs);
    }
  }

  public void processTRegProductsStateRegulations(String regulation)
  {
    OraclePreparedStatement pst = null;
    ResultSet rs = null;
    StringBuilder sb = new StringBuilder();
    sb.append("select distinct b.f_alias from t_reg_products a, t_product_alias_names b  ");
    sb.append("where a.f_regulation = :REGULATION and b.f_product = a.f_product ");
    sb.append(" and   not exists ");
    sb.append(" (SELECT 'X' FROM va_reg_bypasses ");
    sb.append("  WHERE  f_alias = a.f_product ");
    sb.append("  AND    f_regulation = :REGULATION )");
    try
    {
      pst = (OraclePreparedStatement) getWercsConn().prepareStatement(sb.toString());
      pst.setStringAtName("REGULATION", regulation);
      rs = pst.executeQuery();
      while (rs.next())
      {
        insertVaTrLockout(rs.getString(1), regulation);
      }
    }
    catch (Exception e)
    {
      log4jLogger.error(e);
    }
    finally
    {
      JDBCUtil.close(pst, rs);
    }
  }

  private void insertVaTrLockout(String alias, String regulation)
  {
    OraclePreparedStatement pst = null;
    try
    {
      log4jLogger.info("Inserting lockout for " + alias + " with " + regulation + " regulation.");
      pst = (OraclePreparedStatement) getToERPConn().prepareStatement("insert into valspar.va_tr_lockout values(:ALIAS, :REGULATION,'YES', SYSDATE) ");
      pst.setStringAtName("ALIAS", alias);
      pst.setStringAtName("REGULATION", regulation);
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

  public void populateLockoutCategory()
  {
    CallableStatement cstmt = null;
    try
    {
      String command = "{call vca_reg_lockout.Populate_lockout_category()}";
      cstmt = getToERPConn().prepareCall(command);
      cstmt.execute();
      cstmt.close();
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

  public void cleanupLockoutLogTable()
  {
    OraclePreparedStatement pst = null;
    try
    {
      log4jLogger.info("Deleting Records in vca_reg_lockout_log in " + ConnectionUtility.buildDatabaseName(getToERPConn()));
      pst = (OraclePreparedStatement) getToERPConn().prepareStatement("DELETE FROM valspar.vca_reg_lockout_log WHERE date_created < SYSDATE - 180 ");
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

  public boolean checkUsageTotalsComp(LockoutsBean lockooutsBean, String usageType)
  {
    boolean existFlag = true;
    OraclePreparedStatement pst = null;
    ResultSet rs = null;
    StringBuilder sb = new StringBuilder();
    sb.append("select 'X' from t_usage_totals_comp ");
    if (StringUtils.equalsIgnoreCase(usageType, "DSL"))
    {
      sb.append("where  f_cas_number = :CASNO' ");
      sb.append("and  f_comp_id = :COMPID ");
    }
    else
    {
      sb.append("where  f_comp_id = :COMPID ");
    }
    if (StringUtils.equalsIgnoreCase(usageType, "YEAR"))
    {
      sb.append("and f_usage_year = :YEAR ");
    }
    else if (StringUtils.equalsIgnoreCase(usageType, "LIFE"))
    {
      sb.append("and f_usage_type = 'LIFE' ");
    }

    try
    {
      pst = (OraclePreparedStatement) getWercsConn().prepareStatement(sb.toString());
      pst.setStringAtName("COMPID", lockooutsBean.getComponentId());
      if (StringUtils.equalsIgnoreCase(usageType, "YEAR"))
      {
        pst.setStringAtName("YEAR", lockooutsBean.getYearRequested());
      }
      if (StringUtils.equalsIgnoreCase(usageType, "DSL"))
      {
        pst.setStringAtName("CASNO", lockooutsBean.getCasNumber());
      }
      rs = pst.executeQuery();
      if (!rs.next())
      {
        existFlag = false;
      }
    }
    catch (Exception e)
    {
      log4jLogger.error(e);
    }
    finally
    {
      JDBCUtil.close(pst, rs);
    }
    return existFlag;
  }

  public void insertUsageTotalsComp(LockoutsBean lockoutsBean, String usageType, boolean dslUsage)
  {
    OraclePreparedStatement pst = null;
    StringBuilder sb = new StringBuilder();
    sb.append("insert into t_usage_totals_comp(f_cas_number, f_comp_id, f_plant_code, ");
    if (dslUsage)
    {
      sb.append("f_comment, ");
    }
    sb.append("f_usage_year, f_usage_type, f_ytd) ");
    if (dslUsage)
    {
      sb.append("values(:CASNO, :COMPID, '007', 'Not Sold', "); //:YEAR, :USAGETYPE, :QTY)  ");
      if (StringUtils.equalsIgnoreCase(usageType, "YEAR"))
      {
        sb.append("to_char(sysdate-1, 'YYYY'), "); //:YEAR, :USAGETYPE, :QTY)  ");
      }
      else
      {
        sb.append("'0', ");
      }
      sb.append(":USAGETYPE, '0.01')  ");
    }
    else
    {
      sb.append("values(:CASNO, :COMPID, ' ', :YEAR, :USAGETYPE, :QTY) ");
    }

    try
    {
      pst = (OraclePreparedStatement) getWercsConn().prepareStatement(sb.toString());
      pst.setStringAtName("CASNO", lockoutsBean.getCasNumber());
      pst.setStringAtName("COMPID", lockoutsBean.getComponentId());
      pst.setStringAtName("YEAR", lockoutsBean.getYearRequested());
      pst.setStringAtName("USAGETYPE", usageType);
      pst.setStringAtName("QTY", lockoutsBean.getQuantity());
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

  public void updateUsageTotalsComp(LockoutsBean lockooutsBean, String usageType)
  {
    OraclePreparedStatement pst = null;
    StringBuilder sb = new StringBuilder();
    sb.append("update t_usage_totals_comp set f_ytd = f_ytd + :QTY ");
    sb.append("where  f_comp_id = :COMPID ");
    if (StringUtils.equalsIgnoreCase(usageType, "LIFE"))
    {
      sb.append("and f_usage_type = 'LIFE' ");
    }
    else
    {
      sb.append("and (f_usage_year = :YEAR or f_usage_type = 'LIFE') ");
    }
    try
    {
      pst = (OraclePreparedStatement) getWercsConn().prepareStatement(sb.toString());
      pst.setStringAtName("COMPID", lockooutsBean.getComponentId());
      if (!StringUtils.equalsIgnoreCase(usageType, "LIFE"))
      {
        pst.setStringAtName("YEAR", lockooutsBean.getYearRequested());
      }
      pst.setStringAtName("QTY", lockooutsBean.getQuantity());
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

  public void setWercsConn(OracleConnection wercsConn)
  {
    this.wercsConn = wercsConn;
  }

  public OracleConnection getWercsConn()
  {
    return wercsConn;
  }

  public void setToERPConn(OracleConnection toERPConn)
  {
    this.toERPConn = toERPConn;
  }

  public OracleConnection getToERPConn()
  {
    return toERPConn;
  }
}
