package com.valspar.interfaces.regulatory.rollup.program;

import com.jacob.activeX.ActiveXComponent;
import com.jacob.com.Dispatch;
import com.valspar.interfaces.common.BaseInterface;
import com.valspar.interfaces.common.beans.ConnectionBean;
import com.valspar.interfaces.common.utils.JDBCUtil;
import com.valspar.interfaces.regulatory.rollup.beans.*;
import java.sql.*;
import java.util.ArrayList;
import oracle.jdbc.OracleCallableStatement;
import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;

public class RollupInterface extends BaseInterface
{
  private static Logger log4jLogger = Logger.getLogger(RollupInterface.class);
  private static ActiveXComponent axc;
  private int optivaToWercsQueueCount = 0;

  public RollupInterface(String interfaceName, String environment)
  {
    super(interfaceName, environment);

    RollupBean rollupBean = null;
    try
    {
      if (!onHold())
      {
        rollupBean = buildRollupBean();
      }
      if (rollupBean != null && !onHold())
      {
        if (!rollupBean.isError())
        {
          log4jLogger.info("Processing Rollup ID " + rollupBean.getRollupId());
          processRollup(rollupBean, environment);
          if (!rollupBean.isError())
          {
            updateQueueStatus(rollupBean, 2);
            addToEmail("Rollup ID " + rollupBean.getRollupId() + " is complete. Please see logs for details.");
          }
          else
          {
            addToEmail("Rollup ID " + rollupBean.getRollupId() + " did not process. Please see logs for details.");
          }
        }
        else
        {
          addToEmail("Rollup ID " + rollupBean.getRollupId() + " did not process. Please see logs for details.");
        }
        addToEmail("");
        addToEmail("Number of products added to Optiva To Wercs queue: " + optivaToWercsQueueCount);
        addToEmail("");
        addToEmail("Rollup Items Requested:");
        for (ProductBean pbean: rollupBean.getRollupList())
        {
          addToEmail(pbean.getProduct());
        }
        if (StringUtils.isNotEmpty(this.getNotificationEmail()))
        {
          this.setNotificationEmail(this.getNotificationEmail() + "," + rollupBean.getEmail());
        }
        else
        {
          this.setNotificationEmail(rollupBean.getEmail());
        }
      }
      else
      {
        log4jLogger.info("There are no Approved rollups to process.");
      }
    }
    catch (Exception e)
    {
      log4jLogger.error(e);
      updateQueueStatus(rollupBean, -99);
    }
    finally
    {
      cleanUp();
    }
  }

  public void execute()
  {
  }

  public static void main(String[] parameters)
  {
    setAxc(new ActiveXComponent("OptivaToWercsJacobProject.clsJACOB_API"));
    new RollupInterface(parameters[0], parameters[1]);
  }

  public RollupBean buildRollupBean()
  {
    log4jLogger.info("Gathering Rollup Data");
    RollupBean rb = null;
    Statement stmt = null;
    ResultSet rs = null;
    try
    {
      StringBuffer sql = new StringBuffer();
      sql.append("  SELECT rollup_id, ");
      sql.append("         rollup_type, ");
      sql.append("         email, ");
      sql.append("         DECODE (OVERRIDE_OTW_DELAY, ");
      sql.append("                 'Y', 0, ");
      sql.append("                 (SELECT PARM_VALUE ");
      sql.append("                    FROM vca_gmd_run_parameters@TOFM ");
      sql.append("                   WHERE parm_name = 'OPTIVA_TO_WERCS_DELAY')) AS otwDelay, ");
      sql.append("         status ");
      sql.append("    FROM vca_rollup_queue ");
      sql.append("   WHERE status in (0,4) ");
      sql.append("ORDER BY date_approved ");

      ConnectionBean cb = getFromConn().get(0);

      stmt = cb.getConnection().createStatement();
      rs = stmt.executeQuery(sql.toString());

      if (rs.next())
      {
        rb = new RollupBean();
        rb.setRollupId(rs.getString(1));
        updateQueueStatus(rb, 1);

        rb.setRollupType(rs.getString(2));
        rb.setEmail(rs.getString(3));
        rb.setOptivaToWercsDelay(rs.getString(4));
        rb.setStatus(rs.getString(5));

        rb.setRollupList(buildRollupList(rb));
        rb.setComponentList(buildComponentList(rb.getRollupId(), rb.getRollupType()));
        rb.setRmBoList(buildRmBoList(rb.getRollupId(), rb.getRollupType()));
        rb.setResinList(buildResinList(rb.getRollupId(), rb.getRollupType()));
        rb.setFinishedGoodsList(buildFinishedGoodsList(rb.getRollupId(), rb.getRollupType()));
        rb.setIntermediateList(buildIntermediateList(rb.getRollupId()));
        if (rb.getIntermediateList().size() > 0)
        {
          rb.setLowestLevel(rb.getIntermediateList().get(0).getLevel());
        }
        else
        {
          rb.setLowestLevel(0);
        }
        rb.setReprocessList(buildReprocessList(rb.getRollupId()));
      }
    }
    catch (Exception e)
    {
      log4jLogger.error(e);
      updateQueueStatus(rb, -99);
    }
    finally
    {
      JDBCUtil.close(stmt, rs);
    }
    return rb;
  }

  public ArrayList<String> buildReprocessList(String rollupId)
  {
    log4jLogger.info("Building ReProcess List");
    ArrayList<String> reprocessList = new ArrayList<String>();
    Statement stmt = null;
    ResultSet rs = null;
    try
    {
      StringBuffer sb = new StringBuffer();

      sb.append("select step_id ");
      sb.append("from vca_rollup_process_steps ");
      sb.append("where rollup_id = ");
      sb.append(rollupId);
      sb.append(" order by rollup_step_id ");

      ConnectionBean cb = getFromConn().get(0);
      stmt = cb.getConnection().createStatement();
      rs = stmt.executeQuery(sb.toString());
      while (rs.next())
      {
        reprocessList.add(rs.getString(1));
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
    return reprocessList;
  }

  public ArrayList<String> buildComponentList(String rollupId, String rollupType)
  {
    log4jLogger.info("Building Component List");
    ArrayList<String> componentList = new ArrayList<String>();
    Statement stmt = null;
    ResultSet rs = null;
    try
    {
      StringBuffer sb = new StringBuffer();
      sb.append("select distinct c.f_component_id ");
      sb.append("from vca_rollup_items i, t_prod_comp c, t_product_alias_names a ");
      if (rollupType.equalsIgnoreCase("STANDARD"))
      {
        sb.append(", t_components c1 ");
      }
      sb.append("where c.f_product = a.f_product ");
      sb.append("and a.f_alias = i.rollup_item ");
      if (rollupType.equalsIgnoreCase("STANDARD"))
      {
        sb.append("and c1.f_component_id = a.f_product ");
      }
      sb.append("and rollup_id = ");
      sb.append(rollupId);

      ConnectionBean cb = getFromConn().get(0);
      stmt = cb.getConnection().createStatement();
      rs = stmt.executeQuery(sb.toString());
      while (rs.next())
      {
        componentList.add(rs.getString(1));
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
    return componentList;
  }

  public ArrayList<String> buildRmBoList(String rollupId, String rollupType)
  {
    log4jLogger.info("Building RM/Buyout List");
    ArrayList<String> rmBoList = new ArrayList<String>();
    Statement stmt = null;
    ResultSet rs = null;

    try
    {
      StringBuffer sb = new StringBuffer();
      if (rollupType.equalsIgnoreCase("STANDARD"))
      {
        sb.append("SELECT DISTINCT b.f_product, ");
        sb.append(" CASE WHEN get_wercs_data(b.f_product, 'CSTCL') = 'R' AND get_wercs_data(b.f_product, 'FRMCLS') IN ('R','G2') THEN 1 ELSE 0 END AS RESIN ");
        sb.append(" FROM t_components a, t_prod_comp b, t_prod_data c, vca_rollup_items i, t_product_alias_names an ");
        sb.append(" WHERE a.f_component_id = an.f_product  ");
        sb.append(" and i.rollup_item = an.f_alias ");
        sb.append(" and b.f_component_id = a.f_component_id ");
        sb.append(" and b.f_cas_number = b.f_cas_number ");
        sb.append(" and b.f_product = c.f_product  ");
        sb.append(" and c.f_data_code = 'CSTCL' ");
        sb.append(" AND c.f_data in ('R','B') ");
        sb.append(" and i.rollup_id = ");
        sb.append(rollupId);
        sb.append(" union ");
        sb.append("SELECT DISTINCT an.f_product, ");
        sb.append(" CASE WHEN get_wercs_data(b.f_product, 'CSTCL') = 'R' AND get_wercs_data(b.f_product, 'FRMCLS') IN ('R','G2') THEN 1 ELSE 0 END AS RESIN ");
        sb.append(" FROM vca_rollup_items i, t_product_alias_names an, t_prod_data b ");
        sb.append(" WHERE i.rollup_item = an.f_alias ");
        sb.append(" and b.f_product = an.f_product ");
        sb.append(" and b.f_data_code = 'FRMCLS' ");
        sb.append(" and b.f_data in ('R','G2') ");
        sb.append(" and i.rollup_id = ");
        sb.append(rollupId);
      }
      else
      {
        sb.append("select distinct b.f_product,   ");
        sb.append("CASE WHEN get_wercs_data(b.f_product, 'CSTCL') = 'R' AND get_wercs_data(b.f_product, 'FRMCLS') IN ('R','G2') THEN 1 ELSE 0 END AS RESIN ");
        sb.append("from vca_rollup_items i,  t_product_alias_names an, t_prod_with_input_prod a, t_prod_data b   ");
        sb.append("where an.f_alias = i.rollup_item   ");
        sb.append("and a.f_input_product = b.f_product   ");
        sb.append("and b.f_data_code = 'CSTCL'   ");
        sb.append("and b.f_data in ('R','B')   ");
        sb.append("and a.f_product = an.f_product   ");
        sb.append("and an.f_alias not like 'C%-%'   ");
        sb.append("and rollup_id = ");
        sb.append(rollupId);
        sb.append(" union  ");
        sb.append("select distinct b.f_product,  ");
        sb.append("CASE WHEN get_wercs_data(b.f_product, 'CSTCL') = 'R' AND get_wercs_data(b.f_product, 'FRMCLS') IN ('R','G2') THEN 1 ELSE 0 END AS RESIN ");
        sb.append("from t_prod_data b, t_product_alias_names an, vca_rollup_items i  ");
        sb.append("where b.f_data_code = 'CSTCL'  ");
        sb.append("and b.f_data in ('R','B')  ");
        sb.append("and b.f_product = an.f_product  ");
        sb.append("and an.f_alias = i.rollup_item  ");
        sb.append("and rollup_id = ");
        sb.append(rollupId);
      }
      sb.append(" ORDER BY 2 ");
      ConnectionBean cb = getFromConn().get(0);
      stmt = cb.getConnection().createStatement();
      rs = stmt.executeQuery(sb.toString());

      while (rs.next())
      {
        rmBoList.add(rs.getString(1));
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
    return rmBoList;
  }

  public ArrayList<String> buildResinList(String rollupId, String rollupType)
  {
    log4jLogger.info("Building Resin List");
    ArrayList<String> resinList = new ArrayList<String>();
    Statement stmt = null;
    ResultSet rs = null;
    try
    {
      StringBuffer sb = new StringBuffer();
      if (rollupType.equalsIgnoreCase("STANDARD"))
      {
        sb.append("SELECT DISTINCT b.f_product ");
        sb.append("FROM t_components a, ");
        sb.append("t_prod_comp b, ");
        sb.append("t_prod_data c, ");
        sb.append("t_prod_data c2,  ");
        sb.append("vca_rollup_items i, ");
        sb.append("t_product_alias_names an ");
        sb.append("WHERE     a.f_component_id = an.f_product ");
        sb.append("AND i.rollup_item = an.f_alias ");
        sb.append("AND b.f_component_id = a.f_component_id ");
        sb.append("AND b.f_cas_number = b.f_cas_number ");
        sb.append("AND b.f_product = c.f_product ");
        sb.append("AND c.f_data_code = 'CSTCL' ");
        sb.append("AND c.f_data IN ('R', 'B') ");
        sb.append("AND b.f_product = c2.f_product ");
        sb.append("AND c2.f_data_code = 'FRMCLS' ");
        sb.append("AND c2.f_data IN ('R', 'G2') ");
        sb.append("AND i.rollup_id =  ");
        sb.append(rollupId);
        sb.append("UNION ");
        sb.append("SELECT DISTINCT an.f_product ");
        sb.append("FROM vca_rollup_items i, t_product_alias_names an, t_prod_data b ");
        sb.append("WHERE     i.rollup_item = an.f_alias ");
        sb.append("AND b.f_product = an.f_product ");
        sb.append("AND b.f_data_code = 'FRMCLS' ");
        sb.append("AND b.f_data IN ('R', 'G2') ");
        sb.append("AND i.rollup_id =  ");
        sb.append(rollupId);
        sb.append("UNION ");
        sb.append("SELECT DISTINCT a.f_product ");
        sb.append("  FROM vca_rollup_items i, ");
        sb.append("       t_prod_with_input_prod a, ");
        sb.append("       t_prod_data b, ");
        sb.append("       t_product_alias_names an ");
        sb.append(" WHERE     a.f_product = b.f_product ");
        sb.append("       AND a.f_input_product = an.f_product ");
        sb.append("       AND i.rollup_item = an.f_alias ");
        sb.append("       AND b.f_data_code = 'FRMCLS' ");
        sb.append("       AND b.f_data IN ('R', 'G2') ");
        sb.append("AND i.rollup_id =  ");
        sb.append(rollupId);
      }
      else
      {
        sb.append("select distinct b.f_product ");
        sb.append("from vca_rollup_items i,  t_product_alias_names an, t_prod_with_input_prod a, t_prod_data b, t_prod_data c ");
        sb.append("where an.f_alias = i.rollup_item ");
        sb.append("and a.f_input_product = b.f_product ");
        sb.append("and b.f_data_code = 'CSTCL' ");
        sb.append("and b.f_data in ('R','B') ");
        sb.append("and b.f_product = c.f_product ");
        sb.append("and c.f_data_code = 'FRMCLS' ");
        sb.append("and c.f_data in ('R','G2') ");
        sb.append("and a.f_product = an.f_product ");
        sb.append("and an.f_alias not like 'C%-%' ");
        sb.append("and rollup_id = ");
        sb.append(rollupId);
        sb.append(" union   ");
        sb.append("select distinct b.f_product  ");
        sb.append("from vca_rollup_items i,  t_product_alias_names an, t_prod_data b, t_prod_data c  ");
        sb.append("where an.f_alias = i.rollup_item  ");
        sb.append("and an.f_product = b.f_product  ");
        sb.append("and b.f_data_code = 'CSTCL'  ");
        sb.append("and b.f_data in ('R','B')  ");
        sb.append("and b.f_product = c.f_product  ");
        sb.append("and c.f_data_code = 'FRMCLS'  ");
        sb.append("and c.f_data in ('R','G2')  ");
        sb.append("and an.f_alias not like 'C%-%'  ");
        sb.append("and rollup_id = ");
        sb.append(rollupId);
      }

      ConnectionBean cb = getFromConn().get(0);
      stmt = cb.getConnection().createStatement();
      rs = stmt.executeQuery(sb.toString());

      while (rs.next())
      {
        resinList.add(rs.getString(1));
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
    return resinList;
  }

  public ArrayList<ProductBean> buildRollupList(RollupBean rb)
  {
    log4jLogger.info("Building Rollup Item Input List");
    ArrayList<ProductBean> rollupList = new ArrayList<ProductBean>();
    Statement stmt = null;
    ResultSet rs = null;
    try
    {
      StringBuffer sb = new StringBuffer();
      sb.append("select distinct a.f_product, a.f_alias, a.f_alias_name, i.rollup_item_id, ");
      sb.append("get_wercs_data(a.f_product, 'CSTCL') costClass,  ");
      sb.append("get_wercs_data(a.f_product, 'FRMCLS') formClass ");
      sb.append("from vca_rollup_items i, t_product_alias_names a ");
      sb.append("where a.f_alias = i.rollup_item ");
      sb.append("and rollup_id = ");
      sb.append(rb.getRollupId());

      ConnectionBean cb = getFromConn().get(0);
      stmt = cb.getConnection().createStatement();
      rs = stmt.executeQuery(sb.toString());

      while (rs.next())
      {
        ProductBean pbean = new ProductBean();
        pbean.setProduct(rs.getString(1));
        pbean.setAlias(rs.getString(2));
        pbean.setAliasName(rs.getString(3));
        pbean.setRollupItemId(rs.getString(4));
        pbean.setCostClass(rs.getString(5));
        pbean.setMsdsList(buildMsdsList(pbean.getRollupItemId()));
        rollupList.add(pbean);
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
    return rollupList;
  }

  public ArrayList<String> buildFinishedGoodsList(String rollupId, String rollupType)
  {
    log4jLogger.info("Building Finished Goods List");
    ArrayList<String> finishedGoodList = new ArrayList<String>();
    Statement stmt = null;
    ResultSet rs = null;
    try
    {
      StringBuffer sb = new StringBuffer();
      if (rollupType.equalsIgnoreCase("STANDARD"))
      {      
        sb.append("SELECT DISTINCT a.f_product ");
        sb.append("  FROM t_prod_comp a, ");
        sb.append("       vca_rollup_items i, ");
        sb.append("       t_product_alias_names an, ");
        sb.append("       FSITEM@TOFM fi, ");
        sb.append("       FSFORMULA@TOFM ff ");
        sb.append(" WHERE     a.f_component_id = an.f_product ");
        sb.append("       AND a.f_product = fi.item_code(+)   ");
        sb.append("       AND fi.formula_id = ff.formula_id(+) ");
        sb.append("       and nvl(ff.status_ind,0) <> 999 ");
        sb.append("       AND i.rollup_item = an.f_alias ");
        sb.append("       AND (   get_wercs_data (a.f_product, 'CSTCL') IS NULL ");
        sb.append("            OR get_wercs_data (a.f_product, 'CSTCL') NOT IN ('R', 'B', 'C')) ");
        sb.append("       AND i.rollup_id = ");
        sb.append(rollupId);
        sb.append("UNION ");
        sb.append("SELECT DISTINCT a.f_product ");
        sb.append("  FROM vca_rollup_items i, ");
        sb.append("       t_product_alias_names an, ");
        sb.append("       t_prod_with_input_prod a, ");
        sb.append("       FSITEM@TOFM fi, ");
        sb.append("       FSFORMULA@TOFM ff ");
        sb.append(" WHERE     i.rollup_item = an.f_alias ");
        sb.append("       AND a.f_product = fi.item_code(+)   ");
        sb.append("       AND fi.formula_id = ff.formula_id(+) ");
        sb.append("       and nvl(ff.status_ind,0) <> 999 ");
        sb.append("       AND a.f_input_product = an.f_product ");
        sb.append("       AND (   get_wercs_data (a.f_product, 'CSTCL') IS NULL ");
        sb.append("            OR get_wercs_data (a.f_product, 'CSTCL') NOT IN ('R', 'B', 'C')) ");
        sb.append("       AND i.rollup_id = ");
        sb.append(rollupId);
        sb.append("UNION ");
        sb.append("SELECT DISTINCT a.f_product ");
        sb.append("  FROM (SELECT DISTINCT a.f_product ");
        sb.append("          FROM vca_rollup_items i, ");
        sb.append("               t_prod_with_input_prod a, ");
        sb.append("               t_prod_data b, ");
        sb.append("               t_product_alias_names an ");
        sb.append("         WHERE     a.f_product = b.f_product ");
        sb.append("               AND a.f_input_product = an.f_product ");
        sb.append("               AND i.rollup_item = an.f_alias ");
        sb.append("               AND b.f_data_code = 'FRMCLS' ");
        sb.append("               AND b.f_data IN ('R', 'G2') ");
        sb.append("               AND i.rollup_id = ");
        sb.append(rollupId);
        sb.append(") r, ");
        sb.append("       t_prod_with_input_prod a, ");
        sb.append("       FSITEM@TOFM fi, ");
        sb.append("       FSFORMULA@TOFM ff ");
        sb.append(" WHERE     a.f_input_product = r.f_product ");
        sb.append("       AND a.f_product = fi.item_code(+) ");
        sb.append("       AND fi.formula_id = ff.formula_id(+) ");
        sb.append("       and nvl(ff.status_ind,0) <> 999 ");
        sb.append("       AND (   get_wercs_data (a.f_product, 'CSTCL') IS NULL ");
        sb.append("            OR get_wercs_data (a.f_product, 'CSTCL') NOT IN ('R', 'B', 'C')) ");
      }
      else
      {
        sb.append("SELECT DISTINCT a.f_product, ff.status_ind ");
        sb.append("  FROM vca_rollup_items i, ");
        sb.append("       t_product_alias_names a, ");
        sb.append("       FSITEM@TOFM fi, ");
        sb.append("       FSFORMULA@TOFM ff ");
        sb.append(" WHERE     a.f_alias = i.rollup_item ");
        sb.append("       AND a.f_product = fi.item_code (+) ");
        sb.append("       AND fi.formula_id = ff.formula_id (+) ");
        sb.append("       and nvl(ff.status_ind,0) <> 999 ");
        sb.append("       AND (   get_wercs_data (a.f_product, 'CSTCL') IS NULL ");
        sb.append("            OR get_wercs_data (a.f_product, 'CSTCL') NOT IN ('R', 'B', 'C')) ");
        sb.append("       AND i.rollup_id = ");
        sb.append(rollupId);
      }

      ConnectionBean cb = getFromConn().get(0);
      stmt = cb.getConnection().createStatement();
      rs = stmt.executeQuery(sb.toString());

      while (rs.next())
      {
        finishedGoodList.add(rs.getString(1));
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
    return finishedGoodList;
  }

  public ArrayList<MsdsBean> buildMsdsList(String rollupItemId)
  {
    ArrayList<MsdsBean> msdsList = new ArrayList<MsdsBean>();
    Statement stmt = null;
    ResultSet rs = null;
    try
    {
      StringBuffer sb = new StringBuffer();
      sb.append("select subformat, language  ");
      sb.append("from vca_rollup_item_msds ");
      sb.append("where rollup_item_id = ");
      sb.append(rollupItemId);

      ConnectionBean cb = getFromConn().get(0);
      stmt = cb.getConnection().createStatement();
      rs = stmt.executeQuery(sb.toString());

      while (rs.next())
      {
        MsdsBean msdsBean = new MsdsBean();
        msdsBean.setSubFormat(rs.getString(1));
        msdsBean.setLanguage(rs.getString(2));
        msdsList.add(msdsBean);
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
    return msdsList;
  }

  public ArrayList<ProductBean> buildIntermediateList(String rollupId)
  {
    log4jLogger.info("Building Intermediate List");
    ArrayList<ProductBean> intermediateList = new ArrayList<ProductBean>();
    ArrayList<String> itemCodeList = new ArrayList<String>();
    populateIntermediateStagingTable(rollupId);
    Statement stmt = null;
    ResultSet rs = null;
    try
    {
      StringBuffer sb = new StringBuffer();

      sb.append("select distinct item, item_level ");
      sb.append("from vca_rollup_int_staging ");
      sb.append("where rollup_id =  ");
      sb.append(rollupId);
      sb.append(" order by item_level desc ");

      ConnectionBean cb = getFromConn().get(0);
      stmt = cb.getConnection().createStatement();
      rs = stmt.executeQuery(sb.toString());

      while (rs.next())
      {
        String itemCode = rs.getString(1);
        if (itemCodeList.indexOf(itemCode) == -1)
        {
          ProductBean iBean = new ProductBean();
          iBean.setProduct(itemCode);
          iBean.setLevel(rs.getInt(2));
          intermediateList.add(iBean);
          itemCodeList.add(itemCode);
        }
      }
      cleanupIntermediateStagingTable(rollupId);
    }
    catch (Exception e)
    {
      log4jLogger.error(e);
    }
    finally
    {
      JDBCUtil.close(stmt, rs);
    }
    return intermediateList;
  }

  public void populateIntermediateStagingTable(String rollupId)
  {
    ConnectionBean cb = getFromConn().get(0);
    OracleCallableStatement cst = null;

    StringBuffer sb = new StringBuffer();
    sb.append("{CALL vca_rollup_pkg.get_ingr_list(?)}");

    try
    {
      cst = (OracleCallableStatement) cb.getConnection().prepareCall(sb.toString());
      cst.setString(1, rollupId);
      cst.execute();
    }
    catch (Exception e)
    {
      log4jLogger.error(e);
    }
    finally
    {
      JDBCUtil.close(cst);
    }
  }

  public void cleanupIntermediateStagingTable(String rollupId)
  {
    ConnectionBean cb = getFromConn().get(0);
    OracleCallableStatement cst = null;

    StringBuffer sb = new StringBuffer();
    sb.append("delete from vca_rollup_int_staging where rollup_id = ?");

    try
    {
      cst = (OracleCallableStatement) cb.getConnection().prepareCall(sb.toString());
      cst.setString(1, rollupId);
      cst.execute();
    }
    catch (Exception e)
    {
      log4jLogger.error(e);
    }
    finally
    {
      JDBCUtil.close(cst);
    }
  }

  public void processRollup(RollupBean rollupBean, String environment)
  {
    try
    {
      callCustomAG("TESTEU");
      if (rollupBean.getRollupType().equalsIgnoreCase("SIR"))
      {
        if (rollupBean.getReprocessList().isEmpty() || rollupBean.getReprocessList().contains("3149_comp"))
        {
          String status3149 = insertStatus(rollupBean, "3149_comp");
          for (String component: rollupBean.getComponentList())
          {
            if (onHold())
              throw new Exception("Process put on hold");
            ruleWriter("3149", component);
          }
          updateStatusComplete(status3149, rollupBean.getRollupId());
        }
      }

      if (rollupBean.getReprocessList().isEmpty() || rollupBean.getReprocessList().contains("ag_rmbo"))
      {
        String statusAg = insertStatus(rollupBean, "ag_rmbo");
        for (String product: rollupBean.getRmBoList())
        {
          if (onHold())
            throw new Exception("Process put on hold");
          callCustomAG(product);
        }
        updateStatusComplete(statusAg, rollupBean.getRollupId());
      }

      if (rollupBean.getReprocessList().isEmpty() || rollupBean.getReprocessList().contains("eu_rmbo"))
      {
        String statusEU = insertStatus(rollupBean, "eu_rmbo");
        for (String product: rollupBean.getRmBoList())
        {
          if (onHold())
            throw new Exception("Process put on hold");
          eUWizardAPI(product);
        }
        updateStatusComplete(statusEU, rollupBean.getRollupId());
      }

      if (rollupBean.getReprocessList().isEmpty() || rollupBean.getReprocessList().contains("3151_rmbo"))
      {
        String status3151 = insertStatus(rollupBean, "3151_rmbo");
        for (String product: rollupBean.getRmBoList())
        {
          if (onHold())
            throw new Exception("Process put on hold");
          ruleWriter("3151", product);
        }
        updateStatusComplete(status3151, rollupBean.getRollupId());
      }

      if (rollupBean.getReprocessList().isEmpty() || rollupBean.getReprocessList().contains("dot_calc_resin"))
      {
        String statusDotCalc = insertStatus(rollupBean, "dot_calc_resin");
        for (String product: rollupBean.getResinList())
        {
          if (onHold())
            throw new Exception("Process put on hold");
          callDotCalc(product, environment);
        }
        updateStatusComplete(statusDotCalc, rollupBean.getRollupId());
      }

      if (rollupBean.getReprocessList().isEmpty() || rollupBean.getReprocessList().contains("2467_resin"))
      {
        String status2467 = insertStatus(rollupBean, "2467_resin");
        for (String product: rollupBean.getResinList())
        {
          if (onHold())
            throw new Exception("Process put on hold");
          ruleWriter("2467", product);
        }
        updateStatusComplete(status2467, rollupBean.getRollupId());
      }

      if (rollupBean.getReprocessList().isEmpty() || rollupBean.getReprocessList().contains("ghs_rmbo"))
      {
        String statusGHS = insertStatus(rollupBean, "ghs_rmbo");
        for (String product: rollupBean.getRmBoList())
        {
          if (onHold())
            throw new Exception("Process put on hold");
          gHSComInterfaceAPI(product);
        }
        updateStatusComplete(statusGHS, rollupBean.getRollupId());
      }

      if (rollupBean.getReprocessList().isEmpty() || rollupBean.getReprocessList().contains("3592_rmbo"))
      {
        String status3592 = insertStatus(rollupBean, "3592_rmbo");
        for (String product: rollupBean.getRmBoList())
        {
          if (onHold())
            throw new Exception("Process put on hold");
          ruleWriter("3592", product);
        }
        updateStatusComplete(status3592, rollupBean.getRollupId());
      }

      if (rollupBean.getReprocessList().isEmpty() || rollupBean.getReprocessList().contains("3672_rmbo"))
      {
        String status3672Rmbo = insertStatus(rollupBean, "3672_rmbo");
        for (String product: rollupBean.getRmBoList())
        {
          if (onHold())
            throw new Exception("Process put on hold");
          ruleWriter("3672", product);
        }
        updateStatusComplete(status3672Rmbo, rollupBean.getRollupId());
      }

      if (rollupBean.getReprocessList().isEmpty() || rollupBean.getReprocessList().contains("3672_resin"))
      {
        String status3672Resin = insertStatus(rollupBean, "3672_resin");
        for (String product: rollupBean.getResinList())
        {
          if (onHold())
            throw new Exception("Process put on hold");
          ruleWriter("3672", product);
        }
        updateStatusComplete(status3672Resin, rollupBean.getRollupId());
      }

      if (rollupBean.getReprocessList().isEmpty() || rollupBean.getReprocessList().contains("3672_comp"))
      {
        String status3672Comp = insertStatus(rollupBean, "3672_comp");
        for (String product: rollupBean.getComponentList())
        {
          if (onHold())
            throw new Exception("Process put on hold");
          ruleWriter("3672", product);
        }
        updateStatusComplete(status3672Comp, rollupBean.getRollupId());
      }

      if (rollupBean.getRollupType().equalsIgnoreCase("STANDARD"))
      {
        if (rollupBean.getReprocessList().isEmpty() || rollupBean.getReprocessList().contains("standard_msds"))
        {
          if (onHold())
            throw new Exception("Process put on hold");
          String statusStandardMsds = insertStatus(rollupBean, "standard_msds");
          addToPublishingQueue(rollupBean.getRmBoList());
          addToPublishingQueue(rollupBean.getResinList());
          updateStatusComplete(statusStandardMsds, rollupBean.getRollupId());
        }
      }

      if (rollupBean.getReprocessList().isEmpty() || rollupBean.getReprocessList().contains("otw_int"))
      {
        if (onHold())
          throw new Exception("Process put on hold");
        String statusIntOtoW = insertStatus(rollupBean, "otw_int");
        addIntermediateToOptivaQueueBatch(rollupBean);
        updateStatusComplete(statusIntOtoW, rollupBean.getRollupId());
      }

      if (rollupBean.getReprocessList().isEmpty() || rollupBean.getReprocessList().contains("otw_fg"))
      {
        if (onHold())
          throw new Exception("Process put on hold");
        String statusFgOtw = insertStatus(rollupBean, "otw_fg");
        addFGToOptivaQueueBatch(rollupBean);
        updateStatusComplete(statusFgOtw, rollupBean.getRollupId());
      }

      if (rollupBean.getRollupType().equalsIgnoreCase("SIR"))
      {
        if (rollupBean.getReprocessList().isEmpty() || rollupBean.getReprocessList().contains("sir_msds"))
        {
          if (onHold())
            throw new Exception("Process put on hold");
          String statusSirMsds = insertStatus(rollupBean, "sir_msds");
          addSirToPublishingQueue(rollupBean);
          updateStatusComplete(statusSirMsds, rollupBean.getRollupId());
        }
      }
    }
    catch (Exception e)
    {
      log4jLogger.error(e);
      updateQueueStatus(rollupBean, -99);
    }
  }

  public void ruleWriter(String rule, String product)
  {
    Object axcObj = axc.getObject();
    try
    {
      String ruleWriterReturn = Dispatch.call(axcObj, "callRuleWriter", product, rule).toString();
      if (ruleWriterReturn.equals("0"))
        log4jLogger.info("RuleWriter Group " + rule + " completed sucessfully for " + product);
      else
        log4jLogger.error("PopulateWercsException:RuleWriter error for rule group " + rule + " for " + product + ": " + ruleWriterReturn);
    }
    catch (Exception e)
    {
      log4jLogger.error(e);
    }
  }

  public void callCustomAG(String product)
  {
    Object axcObj = axc.getObject();
    try
    {
      String autoGenerateReturn = Dispatch.call(axcObj, "CallCustomAG", product).toString();
      if (autoGenerateReturn.equals("0"))
        log4jLogger.info("CustomAG completed sucessfully for product " + product);
      else
        log4jLogger.error("PopulateWercsException: CustomAG error for product " + product + ".  Check the ingredients in Optiva: " + autoGenerateReturn);
    }
    catch (Exception e)
    {
      log4jLogger.error(e);
    }
  }

  public void eUWizardAPI(String product)
  {
    Object axcObj = axc.getObject();
    try
    {
      String euWizardReturn = Dispatch.call(axcObj, "EUWizardAPI", product).toString();
      if (euWizardReturn.equals("0"))
        log4jLogger.info("EUWizard completed sucessfully for product " + product);
      else
        log4jLogger.error("PopulateWercsException: EUWizard error for product " + product + ": " + euWizardReturn);
    }
    catch (Exception e)
    {
      log4jLogger.error(e);
    }
  }

  public void gHSComInterfaceAPI(String product)
  {
    Object axcObj = axc.getObject();
    try
    {
      String ghsComInterfaceAPIReturn = Dispatch.call(axcObj, "GHSComInterfaceAPI", product).toString();
      if (ghsComInterfaceAPIReturn.equals("0"))
        log4jLogger.info("GHSComInterfaceAPI completed sucessfully for product " + product);
      else
        log4jLogger.error("PopulateWercsException:GHSComInterfaceAPI error for product " + product + ": " + ghsComInterfaceAPIReturn);
    }
    catch (Exception e)
    {
      log4jLogger.error(e);
    }
  }

  public void callDotCalc(String product, String environment)
  {
    try
    {
     /* DotCalcInterface dc = new DotCalcInterface("DotCalcInterface", environment);
      String dcReturn = dc.runDotCalcInterface(product);
      if (dcReturn == null)
        log4jLogger.info("DotCalcInterface completed sucessfully for product " + product);
      else
        log4jLogger.error("DotCalcInterface error for product " + product + ": " + dcReturn);*/
    }
    catch (Exception e)
    {
      log4jLogger.error(e);
    }
  }

  public void addIntermediateToOptivaQueueBatch(RollupBean rb)
  {
    int priority = 1;
    ConnectionBean cb = getToConn().get(0);

    StringBuilder updateSb = new StringBuilder();
    updateSb.append("UPDATE vca_optiva_to_wercs SET status = 5 ");
    updateSb.append("WHERE formula_code = ? ");
    updateSb.append("AND version <> ? ");
    updateSb.append("AND status = 0");

    StringBuilder sb = new StringBuilder();
    sb.append("INSERT INTO VCA_OPTIVA_TO_WERCS ");
    sb.append("   SELECT VCA_OPTIVA_TO_WERCS_SEQ.NEXTVAL, ");
    sb.append("          ?, ?, ?, ?, 2, ?, '0', ");
    sb.append("          NULL, NULL, NULL, ");
    sb.append("          ? + SYSDATE, ");
    sb.append("          'ROLLUP', ");
    sb.append("          ? + SYSDATE, ");
    sb.append("          'ROLLUP', ");
    sb.append("          ?, ?, ?, ?, ?, ?, ");
    sb.append("          'ROLLUP', ");
    sb.append(rb.getRollupId());
    sb.append("    , '1' ");   
    sb.append("     FROM DUAL ");

    CallableStatement updateCstmt = null;
    CallableStatement cstmt = null;

    try
    {
      updateCstmt = cb.getConnection().prepareCall(updateSb.toString());
      cstmt = cb.getConnection().prepareCall(sb.toString());

      for (ProductBean iBean: rb.getIntermediateList())
      {
        try
        {
          priority = rb.getLowestLevel() - iBean.getLevel() + 3; //Start with priority 3 instead of 1
          setupOtoWCallableStatement(iBean.getProduct(), priority, rb.getOptivaToWercsDelay(), cstmt, updateCstmt);
        }
        catch (Exception e)
        {
          log4jLogger.error("Product: " + iBean.getProduct(), e);
        }
      }
      updateCstmt.executeBatch();
      cstmt.executeBatch();
    }
    catch (Exception e)
    {
      log4jLogger.error("Could not add to OptivaToWercs queue for Rollup: " + rb.getRollupId(), e);
    }
    finally
    {
      JDBCUtil.close(updateCstmt);
      JDBCUtil.close(cstmt);
    }
  }

  public void addFGToOptivaQueueBatch(RollupBean rb)
  {
    int priority = rb.getLowestLevel() + 3;
    ConnectionBean cb = getToConn().get(0);

    StringBuilder updateSb = new StringBuilder();
    updateSb.append("UPDATE vca_optiva_to_wercs SET status = 5 ");
    updateSb.append("WHERE formula_code = ? ");
    updateSb.append("AND version <> ? ");
    updateSb.append("AND status = 0");

    StringBuilder sb = new StringBuilder();
    sb.append("INSERT INTO VCA_OPTIVA_TO_WERCS ");
    sb.append("   SELECT VCA_OPTIVA_TO_WERCS_SEQ.NEXTVAL, ");
    sb.append("          ?, ?, ?, ?, 2, ?, '0', ");
    sb.append("          NULL, NULL, NULL, ");
    sb.append("          ? + SYSDATE, ");
    sb.append("          'ROLLUP', ");
    sb.append("          ? + SYSDATE, ");
    sb.append("          'ROLLUP', ");
    sb.append("          ?, ?, ?, ?, ?, ?, ");
    sb.append("          'ROLLUP', ");
    sb.append(rb.getRollupId());
    sb.append("    , '1' ");   
    sb.append("     FROM DUAL ");

    CallableStatement updateCstmt = null;
    CallableStatement cstmt = null;

    try
    {
      updateCstmt = cb.getConnection().prepareCall(updateSb.toString());
      cstmt = cb.getConnection().prepareCall(sb.toString());

      for (String finishedGood: rb.getFinishedGoodsList())
      {
        try
        {
          setupOtoWCallableStatement(finishedGood, priority, rb.getOptivaToWercsDelay(), cstmt, updateCstmt);
        }
        catch (Exception e)
        {
          log4jLogger.error("Finished Good: " + finishedGood, e);
        }
      }

      updateCstmt.executeBatch();
      cstmt.executeBatch();
    }
    catch (Exception e)
    {
      log4jLogger.error("Could not add to OptivaToWercs queue for Rollup: " + rb.getRollupId(), e);
    }
    finally
    {
      JDBCUtil.close(updateCstmt);
      JDBCUtil.close(cstmt);
    }
  }

  public void setupOtoWCallableStatement(String itemCode, int priority, String otwDelay, CallableStatement cstmt, CallableStatement updateCstmt)
  {
    Statement stmt = null;
    ResultSet rs = null;
    try
    {
      StringBuffer sb = new StringBuffer();
      sb.append("SELECT FORMULA_CODE, ");
      sb.append("       VERSION, ");
      sb.append("       I.FORMULA_ID, ");
      sb.append("       I.ITEM_CODE, ");
      sb.append("       F.DESCRIPTION, ");
      sb.append("       NVL(FSI.GET_CLASS_XREF(F.CLASS),F.CLASS) AS CLASS, ");
      sb.append("       nvl(FSI.GET_BUS_GRP_XREF(F.GROUP_CODE), F.GROUP_CODE)  ");
      sb.append("          GROUP_CODE, ");
      sb.append("          nvl(FSI.GET_SET_CODE_XREF(F.group_code), 'USA') as SET_CODE, ");
      sb.append("       NVL(CF_FLASH_POINT,ROUND((TP2.PVALUE*1.8)+32,0)) as FLASHF, ");
      sb.append("       NVL(ROUND((5/9)*(CF_FLASH_POINT-32),0),TP2.PVALUE) as FLASHC ");
      sb.append("  FROM FSFORMULA F, FSITEM I, FSFORMULATECHPARAM TP, FSFORMULATECHPARAM TP2 ");
      sb.append(" WHERE     I.FORMULA_ID = F.FORMULA_ID ");
      sb.append("       AND F.ITEM_CODE = I.ITEM_CODE ");
      sb.append("       AND TP.FORMULA_ID(+) = F.FORMULA_ID ");
      sb.append("       AND TP.PARAM_CODE(+) = 'DEACTIVE' ");
      sb.append("       AND NVL (TP.PVALUE, 0) = 0 ");
      sb.append("       AND F.CLASS NOT IN ('G2', 'J1', 'J2', 'R', 'WF', 'OC', 'OL','J') ");
      sb.append("       AND TP2.FORMULA_ID(+) = F.FORMULA_ID ");
      sb.append("       AND TP2.PARAM_CODE(+) = 'FL-POINT-C' ");
      sb.append("       AND I.ITEM_CODE = '");
      sb.append(itemCode);
      sb.append("'");

      ConnectionBean cb = getToConn().get(0);

      stmt = cb.getConnection().createStatement();
      rs = stmt.executeQuery(sb.toString());

      if (rs.next())
      {
        if (StringUtils.isEmpty(rs.getString("FLASHF")) || StringUtils.isEmpty(rs.getString("FLASHC")))
        {
          log4jLogger.error("No Flashf or Flashc, Product will not be added to OptivaToWercs queue: " + itemCode);
        }
        else
        {
          if ("OF".equals(rs.getString(6)))
          {
            priority++;
          }
          updateCstmt.setString(1, rs.getString("FORMULA_CODE"));
          updateCstmt.setString(2, rs.getString("VERSION"));
          updateCstmt.addBatch();

          cstmt.setString(1, rs.getString("FORMULA_CODE"));
          cstmt.setString(2, rs.getString("VERSION"));
          cstmt.setString(3, rs.getString("FORMULA_ID"));
          cstmt.setString(4, rs.getString("ITEM_CODE"));
          cstmt.setInt(5, priority);
          cstmt.setString(6, otwDelay);
          cstmt.setString(7, otwDelay);
          cstmt.setString(8, rs.getString("DESCRIPTION"));
          cstmt.setString(9, rs.getString("CLASS"));
          cstmt.setString(10, rs.getString("GROUP_CODE"));
          cstmt.setString(11, rs.getString("FLASHF"));
          cstmt.setString(12, rs.getString("FLASHC"));
          cstmt.setString(13, rs.getString("SET_CODE"));
          cstmt.addBatch();
          optivaToWercsQueueCount++;
          log4jLogger.info("Product will be added to OptivaToWercs queue: " + itemCode);
        }
      }
      else
      {
        log4jLogger.error("Product will not be added to OptivaToWercs queue: " + itemCode);
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
  }

  public void addSirToPublishingQueue(RollupBean rb)
  {
    ConnectionBean cb = getFromConn().get(0);
    StringBuilder sb = new StringBuilder();
    sb.append("INSERT INTO VA_PUBLISHING_QUEUE ");
    sb.append("            SELECT   VA_PUBLISHING_QUEUE_SEQ.NEXTVAL, ");
    sb.append("                     ?, 'VAL',?,?,?,?,1,'ROLLUP',0,");
    sb.append("                     'ROLLUP', ");
    sb.append("                     SYSDATE ");
    sb.append("              FROM   DUAL ");
    sb.append("             WHERE   NOT EXISTS ");
    sb.append("                        (SELECT   * ");
    sb.append("                           FROM   VA_PUBLISHING_QUEUE ");
    sb.append("                          WHERE       PRODUCT = ? ");
    sb.append("                                  AND FORMAT = 'VAL' ");
    sb.append("                                  AND SUBFORMAT = ? ");
    sb.append("                                  AND LANGUAGE = ? ");
    sb.append("                                  AND ALIAS = ? ");
    sb.append("                                  AND STATUS = 0)");

    PreparedStatement cstmt = null;
    try
    {
      cstmt = cb.getConnection().prepareStatement(sb.toString());

      for (ProductBean pBean: rb.getRollupList())
      {
        try
        {
          if (pBean.getMsdsList().size() > 0)
          {
            for (MsdsBean msdsBean: pBean.getMsdsList())
            {
              cstmt.setString(1, pBean.getProduct());
              cstmt.setString(2, msdsBean.getSubFormat());
              cstmt.setString(3, msdsBean.getLanguage());
              cstmt.setString(4, pBean.getAlias());
              cstmt.setString(5, pBean.getAliasName());
              cstmt.setString(6, pBean.getProduct());
              cstmt.setString(7, msdsBean.getSubFormat());
              cstmt.setString(8, msdsBean.getLanguage());
              cstmt.setString(9, pBean.getAlias());
              cstmt.addBatch();

              log4jLogger.info("Alias " + pBean.getAlias() + ", Msds " + msdsBean.getSubFormat() + "/" + msdsBean.getLanguage() + " will be added to the publishing queue");
            }
          }
        }
        catch (Exception e)
        {
          log4jLogger.error("Alias: " + pBean.getAlias(), e);
        }
      }
      cstmt.executeBatch();
    }
    catch (Exception e)
    {
      log4jLogger.error("Could not add to publishing queue for Rollup: " + rb.getRollupId(), e);
    }
    finally
    {
      JDBCUtil.close(cstmt);
    }
  }

  public void addToPublishingQueue(ArrayList<String> productList)
  {
    ConnectionBean cb = getFromConn().get(0);
    StringBuilder sb = new StringBuilder();
    sb.append("INSERT INTO VA_PUBLISHING_QUEUE  ");
    sb.append("SELECT VA_PUBLISHING_QUEUE_SEQ.NEXTVAL,  ");
    sb.append("       a.f_product,  ");
    sb.append("       B.f_format,  ");
    sb.append("       B.f_subformat,  ");
    sb.append("       B.f_language,  ");
    sb.append("       A.f_alias,  ");
    sb.append("       A.f_alias_name,  ");
    sb.append("       2,'MASSLOAD',0,  ");
    sb.append("       'ROLLUP',  ");
    sb.append("       SYSDATE  ");
    sb.append("  FROM T_PRODUCT_ALIAS_NAMES A, T_PDF_MSDS B  ");
    sb.append(" WHERE     A.F_ALIAS = B.F_PRODUCT  ");
    sb.append("       AND B.F_AUTHORIZED <> 0  ");
    sb.append("       AND A.F_PRODUCT = (SELECT F_PRODUCT  ");
    sb.append("                            FROM T_PRODUCT_ALIAS_NAMES  ");
    sb.append("                           WHERE F_ALIAS = ?)  ");
    sb.append("       AND NOT EXISTS  ");
    sb.append("                  (SELECT *  ");
    sb.append("                     FROM VA_PUBLISHING_QUEUE  ");
    sb.append("                    WHERE     PRODUCT = a.f_product  ");
    sb.append("                          AND FORMAT = B.f_format  ");
    sb.append("                          AND SUBFORMAT = B.f_subformat  ");
    sb.append("                          AND LANGUAGE = B.f_language  ");
    sb.append("                          AND ALIAS = A.f_alias  ");
    sb.append("                          AND STATUS = 0) ");

    PreparedStatement cstmt = null;
    try
    {
      cstmt = cb.getConnection().prepareStatement(sb.toString());

      for (String product: productList)
      {
        try
        {
          cstmt.setString(1, product);
          cstmt.addBatch();

          log4jLogger.info("Alias " + product + " will be added to the publishing queue");
        }
        catch (Exception e)
        {
          log4jLogger.error("Alias: " + product, e);
        }
      }
      cstmt.executeBatch();
    }
    catch (Exception e)
    {
      log4jLogger.error("Could not add to publishing queue for Rollup", e);
    }
    finally
    {
      JDBCUtil.close(cstmt);
    }
  }

  public void updateQueueStatus(RollupBean rb, int status)
  {
    Statement stmt = null;
    try
    {
      if (status == -99)
      {
        rb.setError(true);
      }
      StringBuffer sql = new StringBuffer();
      sql.append("UPDATE vca_rollup_queue SET STATUS = ");
      sql.append(status);

      if (status == 1)
      {
        sql.append(", START_TIME = SYSDATE ");
        sql.append(", PROCESS_ID = ");
        sql.append(getProcessId());
      }
      else
      {
        sql.append(", END_TIME = SYSDATE ");
      }

      sql.append(" WHERE ROLLUP_ID = ");
      sql.append(rb.getRollupId());

      ConnectionBean cb = getFromConn().get(0);
      stmt = cb.getConnection().createStatement();
      stmt.executeUpdate(sql.toString());
      log4jLogger.info("The status in vca_rollup_queue for Rollup " + rb.getRollupId() + " was updated to a " + status);
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

  public String insertStatus(RollupBean rollupBean, String stepId)
  {
    String rollupStatusId = getNextRollupStatusId();
    CallableStatement cstmt = null;

    try
    {
      StringBuffer sql = new StringBuffer();
      sql.append("insert into vca_rollup_status(rollup_status_id, rollup_id, step_id, start_time, reprocess) values  ");
      sql.append("(?, ?, ?, sysdate, ?) ");

      ConnectionBean cb = getFromConn().get(0);

      cstmt = cb.getConnection().prepareCall(sql.toString());
      cstmt.setString(1, rollupStatusId);
      cstmt.setString(2, rollupBean.getRollupId());
      cstmt.setString(3, stepId);
      if (rollupBean.getStatus().equalsIgnoreCase("4"))
      {
        cstmt.setString(4, "Y");
      }
      else
      {
        cstmt.setString(4, "N");
      }
      cstmt.execute();
    }
    catch (Exception e)
    {
      log4jLogger.error(e);
    }
    finally
    {
      JDBCUtil.close(cstmt);
    }
    return rollupStatusId;
  }

  public void updateStatusComplete(String rollupStatusId, String rollupId)
  {
    Statement stmt = null;
    Statement stmt2 = null;
    try
    {
      StringBuffer sql = new StringBuffer();
      sql.append("UPDATE vca_rollup_status SET end_time = sysdate ");
      sql.append("WHERE rollup_status_id = ");
      sql.append(rollupStatusId);

      ConnectionBean cb = getFromConn().get(0);
      stmt = cb.getConnection().createStatement();
      stmt.executeUpdate(sql.toString());

      StringBuffer sql2 = new StringBuffer();
      sql2.append("delete from vca_rollup_process_steps ");
      sql2.append(" where rollup_id = ");
      sql2.append(rollupId);
      sql2.append(" and step_id = (select step_id from vca_rollup_status where rollup_status_id = ");
      sql2.append(rollupStatusId);
      sql2.append(") ");

      stmt2 = cb.getConnection().createStatement();
      stmt2.executeUpdate(sql2.toString());
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

  public String getNextRollupStatusId()
  {
    String nextId = null;
    Statement stmt = null;
    ResultSet rs = null;

    try
    {
      StringBuffer sb = new StringBuffer();
      sb.append("select VCA_ROLLUP_STATUS_SEQ.nextval from dual  ");

      ConnectionBean cb = getFromConn().get(0);
      stmt = cb.getConnection().createStatement();
      rs = stmt.executeQuery(sb.toString());

      if (rs.next())
      {
        nextId = rs.getString(1);
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
    return nextId;
  }

  public static void setAxc(ActiveXComponent newAxc)
  {
    axc = newAxc;
  }
}
