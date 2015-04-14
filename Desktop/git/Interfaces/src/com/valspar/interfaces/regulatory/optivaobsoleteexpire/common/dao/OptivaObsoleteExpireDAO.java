package com.valspar.interfaces.regulatory.optivaobsoleteexpire.common.dao;

import com.valspar.interfaces.common.beans.ConnectionAccessBean;
import com.valspar.interfaces.common.enums.DataSource;
import com.valspar.interfaces.common.utils.ConnectionUtility;
import com.valspar.interfaces.common.utils.JDBCUtil;
import com.valspar.interfaces.regulatory.optivaobsoleteexpire.common.beans.ItemBean;
import com.valspar.interfaces.regulatory.optivaobsoleteexpire.common.beans.ValidityRuleBean;
import java.util.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
import java.sql.Types;
import java.util.ArrayList;
import java.util.Calendar;
import oracle.jdbc.OracleCallableStatement;
import oracle.jdbc.OracleConnection;
import oracle.jdbc.OraclePreparedStatement;
import oracle.jdbc.OracleResultSet;
import org.apache.log4j.Logger;
import org.apache.commons.lang.StringUtils;

public class OptivaObsoleteExpireDAO
{
  private static Logger log4jLogger = Logger.getLogger(OptivaObsoleteExpireDAO.class);
  private static Date northAmericaNextPeriodStartDate;
  private static Date emeaiNextPeriodStartDate;
  private static Date asiapacNextPeriodStartDate;

  public OptivaObsoleteExpireDAO()
  {
  }

  public static ArrayList<ValidityRuleBean> buildValidityRuleBeans(DataSource ds)
  {
    ArrayList<ValidityRuleBean> ar = new ArrayList<ValidityRuleBean>();
    Statement stmt = null;
    ResultSet rs = null;
    OracleConnection conn = null;
    try
    {
      conn = (OracleConnection) ConnectionAccessBean.getConnection(ds);
      StringBuffer sb = new StringBuffer();
      sb.append("SELECT RECIPE_VALIDITY_RULE_ID, ORGN_CODE ");
      sb.append("  FROM (  SELECT GBH.RECIPE_VALIDITY_RULE_ID, ");
      sb.append("                 MAX (GBH.ACTUAL_CMPLT_DATE) LAST_PROD_DATE, ");
      sb.append("                 VEVC.DAYS_FROM_LAST_PROD, ");
      sb.append("                 VEVC.ORGN_CODE ");
      sb.append("            FROM APPS.GMD_RECIPE_VALIDITY_RULES GRVR, ");
      sb.append("                 APPS.GME_BATCH_HEADER GBH, ");
      sb.append("                 APPS.GMD_RECIPES GR, ");
      sb.append("                 APPS.IC_ITEM_MST IIM, ");
      sb.append("                 APPS.FM_FORM_MST FFM, ");
      sb.append("                 VALSPAR.VCA_EXPIRE_VR_CONFIG VEVC ");
      sb.append("           WHERE     GRVR.ORGN_CODE = VEVC.ORGN_CODE ");
      sb.append("                 AND GRVR.DELETE_MARK = 0 ");
      sb.append("                 AND GRVR.END_DATE > SYSDATE + 30 ");
      sb.append("                 AND GRVR.RECIPE_VALIDITY_RULE_ID = GBH.RECIPE_VALIDITY_RULE_ID ");
      sb.append("                 AND GBH.PLANT_CODE = VEVC.ORGN_CODE ");
      sb.append("                 AND GRVR.ITEM_ID = IIM.ITEM_ID ");
      sb.append("                 AND GRVR.RECIPE_ID = GR.RECIPE_ID ");
      sb.append("                 AND GR.FORMULA_ID = FFM.FORMULA_ID ");
      sb.append("                 AND UPPER(NVL(VEVC.ACTIVE,'N')) = 'Y' ");
      sb.append("                 AND (   VEVC.ORGN_CODE NOT IN ('860', '960', '360') ");
      sb.append("                      OR IIM.ITEMCOST_CLASS <> 'FG') ");
      sb.append("                 AND (   VEVC.ORGN_CODE NOT IN ('860', '960', '360') ");
      sb.append("                      OR FFM.FORMULA_NO NOT LIKE '%T') ");
      sb.append("        GROUP BY GBH.RECIPE_VALIDITY_RULE_ID, VEVC.DAYS_FROM_LAST_PROD, VEVC.ORGN_CODE) ");
      sb.append(" WHERE (  TO_DATE (SYSDATE, 'DD/MM/YYYY') - TO_DATE (LAST_PROD_DATE, 'DD/MM/YYYY')) > DAYS_FROM_LAST_PROD ");      
      sb.append("UNION ");
      sb.append("SELECT GRVR.RECIPE_VALIDITY_RULE_ID, VEVC.ORGN_CODE ");
      sb.append("  FROM APPS.GMD_RECIPE_VALIDITY_RULES GRVR, ");
      sb.append("       APPS.GMD_RECIPES GR, ");
      sb.append("       APPS.IC_ITEM_MST IIM, ");
      sb.append("       APPS.FM_FORM_MST FFM, ");
      sb.append("       VALSPAR.VCA_EXPIRE_VR_CONFIG VEVC ");
      sb.append(" WHERE     GRVR.DELETE_MARK = 0 ");
      sb.append("       AND GRVR.ORGN_CODE = VEVC.ORGN_CODE ");
      sb.append("       AND GRVR.ITEM_ID = IIM.ITEM_ID ");
      sb.append("       AND GRVR.RECIPE_ID = GR.RECIPE_ID ");
      sb.append("       AND GR.FORMULA_ID = FFM.FORMULA_ID ");
      sb.append("       AND UPPER(NVL(VEVC.ACTIVE,'N')) = 'Y' ");
      sb.append("       AND (   VEVC.ORGN_CODE NOT IN ('860', '960', '360') ");
      sb.append("            OR IIM.ITEMCOST_CLASS <> 'FG') ");
      sb.append("       AND (   VEVC.ORGN_CODE NOT IN ('860', '960', '360') ");
      sb.append("            OR FFM.FORMULA_NO NOT LIKE '%T') ");
      sb.append("       AND GRVR.END_DATE > SYSDATE + 30 ");
      sb.append("       AND GRVR.RECIPE_VALIDITY_RULE_ID NOT IN ");
      sb.append("              (SELECT DISTINCT GBH2.RECIPE_VALIDITY_RULE_ID ");
      sb.append("                 FROM APPS.GME_BATCH_HEADER GBH2 ");
      sb.append("                WHERE GBH2.PLANT_CODE = VEVC.ORGN_CODE) ");
      sb.append("       AND (  TO_DATE (SYSDATE, 'DD/MM/YYYY') - TO_DATE (GRVR.CREATION_DATE, 'DD/MM/YYYY')) > VEVC.DAYS_FROM_CREATION ");

      log4jLogger.info("11i Connection = " + ConnectionUtility.buildDatabaseName(conn));
      stmt = conn.createStatement();
      rs = stmt.executeQuery(sb.toString());
      log4jLogger.info("Query completed.  Building Beans ");
      while (rs.next())
      {
        ValidityRuleBean vrb = new ValidityRuleBean();
        vrb.setRuleId(rs.getString(1));
        vrb.setOrgnCode(rs.getString(2));

        ar.add(vrb);
        if (ar.size() % 100 == 0)
        {
          log4jLogger.info("Building Validity Rules list...  Current list size " + ar.size());
        }
      }
      log4jLogger.info("We have " + ar.size() + " Validity Rules to expire");
    }
    catch (Exception e)
    {
      log4jLogger.error("Exception occured in ExpireValidityRulesInterface.buildValidityRuleBeans() " + e);
    }
    finally
    {
      JDBCUtil.close(stmt, rs);
      JDBCUtil.close(conn);
    }
    return ar;
  }

  public static ArrayList<ItemBean> buildItemBeans(String recordsToProcess)
  {
    ArrayList<ItemBean> ar = new ArrayList<ItemBean>();
    Statement stmt = null;
    ResultSet rs = null;
    OracleConnection conn = null;
    try
    {
      conn = (OracleConnection) ConnectionAccessBean.getConnection(DataSource.FORMULATION);
      StringBuffer sb = new StringBuffer();
      sb.append("SELECT items.item_code, items.formula_id, ");
      sb.append("       (SELECT 'NAPR' ");
      sb.append("          FROM apps.ic_item_mst@sutona.valspar.com ");
      sb.append("         WHERE item_no = items.item_code) ");
      sb.append("          AS napr, ");
      sb.append("       (SELECT 'PAPR' ");
      sb.append("          FROM apps.ic_item_mst@sutopa.valspar.com ");
      sb.append("         WHERE item_no = items.item_code) ");
      sb.append("          AS papr, ");
      sb.append("       (SELECT 'INPR' ");
      sb.append("          FROM apps.ic_item_mst@sutoin.valspar.com ");
      sb.append("         WHERE item_no = items.item_code) ");
      sb.append("          AS inpr, ");
      sb.append("       (SELECT F_PRODUCT ");
      sb.append("          FROM T_PRODUCT_ALIAS_NAMES@TORG ");
      sb.append("         WHERE F_PRODUCT = ITEMS.ITEM_CODE AND f_alias = f_product) ");
      sb.append("          AS bulk ");
      sb.append("  FROM (SELECT f.item_code, f.formula_id ");
      sb.append("          FROM fsformula f ");
      sb.append("         WHERE     f.status_ind IN (402, 999) ");
      sb.append("               AND f.creation_date < (SYSDATE - 730) ");
      sb.append("               AND f.class <> 'J' and f.version not like '%.%' ");
      sb.append("        MINUS ");
      sb.append("        SELECT f.item_code, f.formula_id ");
      sb.append("          FROM fsformula f ");
      sb.append("         WHERE     f.status_ind IN (402, 999) ");
      sb.append("               AND f.creation_date >= (SYSDATE - 730) ");
      sb.append("               AND f.class <> 'J') items            "); //items=2 year old formulas
      sb.append(" WHERE     NOT EXISTS         "); //check NAPR for transactions in the last 2 years
      sb.append("                  (SELECT 'x' ");
      sb.append("                     FROM apps.ic_tran_pnd@sutona.valspar.com a, ");
      sb.append("                          apps.ic_item_mst_b@sutona.valspar.com b ");
      sb.append("                    WHERE     a.item_id = b.item_id ");
      sb.append("                          AND a.trans_date >= (SYSDATE - 730) ");
      sb.append("                          AND b.itemcost_class NOT IN ");
      sb.append("                                 ('FILL', ");
      sb.append("                                  'MRP', ");
      sb.append("                                  'MA', ");
      sb.append("                                  'SL_CONT', ");
      sb.append("                                  'MRO', ");
      sb.append("                                  'LC96') ");
      sb.append("                          AND b.planning_class NOT IN ");
      sb.append("                                 ('MER-AIDS', ");
      sb.append("                                  'MTO', ");
      sb.append("                                  'F-CAPS', ");
      sb.append("                                  'F-BOTTLE', ");
      sb.append("                                  'F-MISC', ");
      sb.append("                                  'LABELS') ");
      sb.append("                          AND b.purch_class NOT IN ");
      sb.append("                                 ('BROCHURE', ");
      sb.append("                                  'CARTON', ");
      sb.append("                                  'CHIP STR', ");
      sb.append("                                  'COLR CRD', ");
      sb.append("                                  'DIS MED', ");
      sb.append("                                  'DIS/EASY', ");
      sb.append("                                  'MRO', ");
      sb.append("                                  'PALLETS') ");
      sb.append("                          AND b.item_no = items.item_code) ");
      sb.append("       AND NOT EXISTS                                "); //check NAPR for inventory
      sb.append("                  (SELECT 'x' ");
      sb.append("                     FROM ic_item_mst@sutona.valspar.com im, ");
      sb.append("                          ic_loct_inv@sutona.valspar.com ii ");
      sb.append("                    WHERE     im.item_id = ii.item_id ");
      sb.append("                          AND im.item_no = items.item_code) ");
      sb.append("       AND NOT EXISTS         "); //check PAPR for transactions in the last 2 years
      sb.append("                  (SELECT 'x' ");
      sb.append("                     FROM apps.ic_tran_pnd@sutopa.valspar.com a, ");
      sb.append("                          apps.ic_item_mst_b@sutopa.valspar.com b ");
      sb.append("                    WHERE     a.item_id = b.item_id ");
      sb.append("                          AND a.trans_date >= (SYSDATE - 730) ");
      sb.append("                          AND b.itemcost_class NOT IN ");
      sb.append("                                 ('FILL', ");
      sb.append("                                  'MRP', ");
      sb.append("                                  'MA', ");
      sb.append("                                  'SL_CONT', ");
      sb.append("                                  'MRO', ");
      sb.append("                                  'LC96') ");
      sb.append("                          AND b.planning_class NOT IN ");
      sb.append("                                 ('MER-AIDS', ");
      sb.append("                                  'MTO', ");
      sb.append("                                  'F-CAPS', ");
      sb.append("                                  'F-BOTTLE', ");
      sb.append("                                  'F-MISC', ");
      sb.append("                                  'LABELS') ");
      sb.append("                          AND b.item_no = items.item_code) ");
      sb.append("       AND NOT EXISTS                               "); //check PAPR for inventory
      sb.append("                  (SELECT 'x' ");
      sb.append("                     FROM ic_item_mst@sutopa.valspar.com im, ");
      sb.append("                          ic_loct_inv@sutopa.valspar.com ii ");
      sb.append("                    WHERE     im.item_id = ii.item_id ");
      sb.append("                          AND im.item_no = items.item_code) ");
      sb.append("       AND NOT EXISTS         "); //check INPR for transactions in the last 2 years
      sb.append("                  (SELECT 'x' ");
      sb.append("                     FROM apps.ic_tran_pnd@sutoin.valspar.com a, ");
      sb.append("                          apps.ic_item_mst_b@sutoin.valspar.com b ");
      sb.append("                    WHERE     a.item_id = b.item_id ");
      sb.append("                          AND a.trans_date >= (SYSDATE - 730) ");
      sb.append("                          AND b.itemcost_class NOT IN ");
      sb.append("                                 ('FILL', ");
      sb.append("                                  'MRP', ");
      sb.append("                                  'MA', ");
      sb.append("                                  'SL_CONT', ");
      sb.append("                                  'MRO', ");
      sb.append("                                  'LC96') ");
      sb.append("                          AND b.planning_class NOT IN ");
      sb.append("                                 ('MER-AIDS', ");
      sb.append("                                  'MTO', ");
      sb.append("                                  'F-CAPS', ");
      sb.append("                                  'F-BOTTLE', ");
      sb.append("                                  'F-MISC', ");
      sb.append("                                  'LABELS') ");
      sb.append("                          AND b.item_no = items.item_code) ");
      sb.append("       AND NOT EXISTS                                "); //check INPR for inventory
      sb.append("                  (SELECT 'x' ");
      sb.append("                     FROM ic_item_mst@sutoin.valspar.com im, ");
      sb.append("                          ic_loct_inv@sutoin.valspar.com ii ");
      sb.append("                    WHERE     im.item_id = ii.item_id ");
      sb.append("                          AND im.item_no = items.item_code) ");
      sb.append("       AND NOT EXISTS ");
      sb.append("                  (SELECT 'x' ");
      sb.append("                     FROM fsformulasets ");
      sb.append("                    WHERE     formula_id = items.formula_id ");
      sb.append("                          AND set_code = 'FMLA_OBS') "); // HASNT ALREADY BEEN PICKED UP BY THIS PROGRAM TO BE FLAGGED AS OBSOLETE
      sb.append("       AND EXISTS                                    "); // EXISTS SOMEWHERE IN 11I
      sb.append("              (SELECT 'NAPR' ");
      sb.append("                 FROM apps.ic_item_mst@sutona.valspar.com ");
      sb.append("                WHERE item_no = items.item_code ");
      sb.append("               UNION ");
      sb.append("               SELECT 'PAPR' ");
      sb.append("                 FROM apps.ic_item_mst@sutopa.valspar.com ");
      sb.append("                WHERE item_no = items.item_code ");
      sb.append("               UNION ");
      sb.append("               SELECT 'INPR' ");
      sb.append("                 FROM apps.ic_item_mst@sutoin.valspar.com ");
      sb.append("                WHERE item_no = items.item_code) ");
      if (!StringUtils.equalsIgnoreCase(recordsToProcess, "ALL"))
      {
        sb.append("    and rownum <= ");
        sb.append(recordsToProcess);
      }

      log4jLogger.info("Optiva Connection = " + ConnectionUtility.buildDatabaseName(conn));
      stmt = conn.createStatement();
      rs = stmt.executeQuery(sb.toString());
      log4jLogger.info("Query completed.  Building Beans ");
      while (rs.next())
      {
        ItemBean ib = new ItemBean();
        ib.setItemNumber(rs.getString(1));
        ib.setFormulaId(rs.getString(2));
        if (rs.getString(3) != null)
        {
          ib.setNorthAmerica(true);
          ib.setNorthAmericaValidityRules(OptivaObsoleteExpireDAO.buildValidityRules(DataSource.NORTHAMERICAN, ib.getItemNumber(), northAmericaNextPeriodStartDate));
        }
        if (rs.getString(4) != null)
        {
          ib.setAsiapac(true);
          ib.setAsiapacValidityRules(OptivaObsoleteExpireDAO.buildValidityRules(DataSource.ASIAPAC, ib.getItemNumber(), asiapacNextPeriodStartDate));
        }
        if (rs.getString(5) != null)
        {
          ib.setEmeai(true);
          ib.setEmeaiValidityRules(OptivaObsoleteExpireDAO.buildValidityRules(DataSource.EMEAI, ib.getItemNumber(), emeaiNextPeriodStartDate));
        }
        if (rs.getString(6) != null)
        {
          ib.setBulk(true);
        }
        ar.add(ib);
        if (ar.size() % 10000 == 0)
        {
          log4jLogger.info("Building list...  Current list size " + ar.size());
        }
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
      JDBCUtil.close(conn);
    }
    return ar;
  }

  public static void updateOptivaFormula(String formulaId)
  {
    StringBuilder formulaUpdate = new StringBuilder();
    formulaUpdate.append("UPDATE fsformula ");
    formulaUpdate.append("   SET status_ind = 999, ");
    formulaUpdate.append("       approval_code = 'OBSOLETE', ");
    formulaUpdate.append("       modify_by = 'CONV', ");
    formulaUpdate.append("       modify_date = SYSDATE ");
    formulaUpdate.append(" WHERE formula_id = ? ");
    formulaUpdate.append("   AND status_ind <> 999  ");
    
    StringBuilder setCodeInsert = new StringBuilder();
    setCodeInsert.append("INSERT INTO fsformulasets (formula_id, set_code) ");
    setCodeInsert.append("   SELECT ?, 'FMLA_OBS' ");
    setCodeInsert.append("     FROM DUAL ");
    setCodeInsert.append("    WHERE NOT EXISTS ");
    setCodeInsert.append("             (SELECT 'x' ");
    setCodeInsert.append("                FROM fsformulasets ");
    setCodeInsert.append("               WHERE formula_id = ? AND set_code = 'FMLA_OBS') ");

    PreparedStatement pstmt1 = null;
    PreparedStatement pstmt2 = null;
    OracleConnection conn = null;

    try
    {
      conn = (OracleConnection) ConnectionAccessBean.getConnection(DataSource.FORMULATION);
      pstmt1 = conn.prepareStatement(formulaUpdate.toString());
      pstmt1.setString(1, formulaId);
      pstmt1.execute();
      
      pstmt2 = conn.prepareStatement(setCodeInsert.toString());
      pstmt2.setString(1, formulaId);
      pstmt2.setString(2, formulaId);
      pstmt2.execute();
    }
    catch (Exception e)
    {
      log4jLogger.error(e);
    }
    finally
    {
      JDBCUtil.close(pstmt1);
      JDBCUtil.close(pstmt2);
      JDBCUtil.close(conn);
    }
  }

  public static void update11iValidityRule(DataSource ds, String ruleId, String orgnCode)
  {
    //update11iValidityRule will do the following
    //#1  INSERT 11I TEXT
    //#2  EXPIRE VALIDITY
    //#3  UPDATE FSXGMFORMEFF
      
    OracleCallableStatement cst = null;
    OracleConnection conn = null;
    String status = "";

    String procedure = "{call APPS.VCA_EXPIRE_VALIDITY_RULE (?, ?, ?, ?)}";
    try
    {
      conn = (OracleConnection) ConnectionAccessBean.getConnection(ds);
      cst = (OracleCallableStatement) conn.prepareCall(procedure);
      cst.setString(1, ruleId);
      cst.setString(2, orgnCode);
      if (ConnectionUtility.buildDatabaseName(conn).startsWith("NA"))
      {
        cst.setDATE(3, JDBCUtil.getDATE(northAmericaNextPeriodStartDate));
      }
      else if (ConnectionUtility.buildDatabaseName(conn).startsWith("PA"))
      {
        cst.setDATE(3, JDBCUtil.getDATE(asiapacNextPeriodStartDate));
      }
      else
      {
        cst.setDATE(3, JDBCUtil.getDATE(emeaiNextPeriodStartDate));
      }
      cst.registerOutParameter(4, Types.VARCHAR);
      cst.executeUpdate();
      status = cst.getString(4);
      if (!status.equalsIgnoreCase("SUCCESS"))
      {
        log4jLogger.error("Error updating validity rule on " + ConnectionUtility.buildDatabaseName(conn) + " Rule ID: " + ruleId + " Orgn Code: " + orgnCode + " SQLERRM: " + status);
      }
    }
    catch (Exception e)
    {
      log4jLogger.error(e);
    }
    finally
    {
      JDBCUtil.close(cst);
      JDBCUtil.close(conn);
    }
  }

  public static Date getNextPeriodStartDate(DataSource ds)
  {
    Statement stmt = null;
    ResultSet rs = null;
    OracleConnection conn = null;
    Date nextPeriodStartDate = Calendar.getInstance().getTime();

    try
    {
      conn = (OracleConnection) ConnectionAccessBean.getConnection(ds);
      StringBuilder sb = new StringBuilder();
      sb.append("SELECT start_date  ");
      sb.append("             FROM gl_periods  ");
      sb.append("            WHERE     start_date > SYSDATE  ");
      sb.append("                  AND start_date <= ADD_MONTHS (SYSDATE, 1)  ");
      if (ConnectionUtility.buildDatabaseName(conn).startsWith("NA"))
      {
        sb.append("                  AND period_type = 1 ");
      }
      else
      {
        sb.append("                  AND period_type = '21' ");
      }
      sb.append("                  AND period_name NOT LIKE 'BEG-%'  ");
      sb.append("                  AND period_name NOT LIKE 'END-%' ");

      stmt = conn.createStatement();
      rs = stmt.executeQuery(sb.toString());
      while (rs.next())
      {
        nextPeriodStartDate = rs.getDate(1);
      }
    }
    catch (Exception e)
    {
      log4jLogger.error(e);
    }
    finally
    {
      JDBCUtil.close(stmt);
      JDBCUtil.close(conn);
    }
    log4jLogger.info(ds.getDataSourceLabel() + " next period start date: " + nextPeriodStartDate);
    return nextPeriodStartDate;
  }

  public static void updateWercs(String itemNumber)
  {
    StringBuilder sb = new StringBuilder();
    sb.append("INSERT INTO T_PROD_TEXT (F_PRODUCT, ");
    sb.append("                                        F_FORMAT, ");
    sb.append("                                        F_TEXT_CODE, ");
    sb.append("                                        F_DATE_STAMP, ");
    sb.append("                                        F_DATA_CODE, ");
    sb.append("                                        F_COUNTER, ");
    sb.append("                                        F_USER_UPDATED) ");
    sb.append("                    select F_PRODUCT, ");
    sb.append("                            'VAL', ");
    sb.append("                            'PRDSTAT3', ");
    sb.append("                            SYSDATE, ");
    sb.append("                            'PRDSTAT', ");
    sb.append("                            '999999', ");
    sb.append("                            'WERCS' from T_PRODUCTS ");
    sb.append("                            where F_PRODUCT = ? AND not exists  ");
    sb.append("                            (select 'x' from t_prod_text ");
    sb.append("where f_text_code = 'PRDSTAT3' and f_product = ?) ");

    PreparedStatement pstmt = null;
    OracleConnection conn = null;

    try
    {
      conn = (OracleConnection) ConnectionAccessBean.getConnection(DataSource.REGULATORY);
      pstmt = conn.prepareStatement(sb.toString());
      pstmt.setString(1, itemNumber);
      pstmt.setString(2, itemNumber);
      pstmt.execute();
    }
    catch (Exception e)
    {
      log4jLogger.error(e);
    }
    finally
    {
      JDBCUtil.close(pstmt);
      JDBCUtil.close(conn);
    }
  }

  public static ArrayList<ValidityRuleBean> buildValidityRules(DataSource ds, String itemNumber, Date endDate)
  {
    ArrayList<ValidityRuleBean> ar = new ArrayList<ValidityRuleBean>();
    OraclePreparedStatement pst = null;
    OracleResultSet rs = null;
    OracleConnection conn = null;

    StringBuilder sb = new StringBuilder();
    sb.append("SELECT recipe_validity_rule_id, orgn_code FROM gmd_recipe_validity_rules  ");
    sb.append(" WHERE item_id = (SELECT item_id  ");
    sb.append("                    FROM apps.ic_item_mst  ");
    sb.append("                   WHERE item_no = ?)  ");
    sb.append("                   AND END_DATE > ? ");

    try
    {
      conn = (OracleConnection) ConnectionAccessBean.getConnection(ds);
      pst = (OraclePreparedStatement) conn.prepareStatement(sb.toString());
      pst.setString(1, itemNumber);
      pst.setDATE(2, JDBCUtil.getDATE(endDate));
      rs = (OracleResultSet) pst.executeQuery();

      while (rs.next())
      {
        ValidityRuleBean vrb = new ValidityRuleBean();
        vrb.setRuleId(rs.getString(1));
        vrb.setOrgnCode(rs.getString(2));
        ar.add(vrb);
      }
    }
    catch (Exception e)
    {
      log4jLogger.error(e);
    }
    finally
    {
      JDBCUtil.close(pst);
      JDBCUtil.close(conn);
    }
    return ar;
  }

  public static void setNorthAmericaNextPeriodStartDate(Date northAmericaNextPeriodStartDate)
  {
    OptivaObsoleteExpireDAO.northAmericaNextPeriodStartDate = northAmericaNextPeriodStartDate;
  }

  public static Date getNorthAmericaNextPeriodStartDate()
  {
    return northAmericaNextPeriodStartDate;
  }

  public static void setEmeaiNextPeriodStartDate(Date emeaiNextPeriodStartDate)
  {
    OptivaObsoleteExpireDAO.emeaiNextPeriodStartDate = emeaiNextPeriodStartDate;
  }

  public static Date getEmeaiNextPeriodStartDate()
  {
    return emeaiNextPeriodStartDate;
  }

  public static void setAsiapacNextPeriodStartDate(Date asiapacNextPeriodStartDate)
  {
    OptivaObsoleteExpireDAO.asiapacNextPeriodStartDate = asiapacNextPeriodStartDate;
  }

  public static Date getAsiapacNextPeriodStartDate()
  {
    return asiapacNextPeriodStartDate;
  }
}
