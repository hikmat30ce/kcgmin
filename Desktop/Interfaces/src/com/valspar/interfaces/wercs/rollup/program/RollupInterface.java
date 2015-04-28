package com.valspar.interfaces.wercs.rollup.program;

import com.valspar.interfaces.common.BaseInterface;
import com.valspar.interfaces.common.beans.ConnectionAccessBean;
import com.valspar.interfaces.common.enums.DataSource;
import com.valspar.interfaces.wercs.common.etl.ETLUtility;
import com.valspar.interfaces.wercs.common.etl.beans.IprocessBean;
import com.valspar.interfaces.common.utils.JDBCUtil;
import com.valspar.interfaces.common.utils.ValsparLookUps;
import com.valspar.interfaces.wercs.common.utils.WercsUtility;
import com.valspar.interfaces.wercs.rollup.beans.MsdsBean;
import com.valspar.interfaces.wercs.rollup.beans.ProductBean;
import com.valspar.interfaces.wercs.rollup.beans.RollupBean;
import java.math.BigDecimal;
import java.sql.*;
import java.sql.CallableStatement;
import java.util.ArrayList;
import java.util.List;
import oracle.jdbc.*;
import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;

public class RollupInterface extends BaseInterface
{
  private static Logger log4jLogger = Logger.getLogger(RollupInterface.class);
  private OracleConnection wercsConn = null;

  public RollupInterface()
  {
  }

  public void execute()
  {
    RollupBean rollupBean = null;
    try
    {
      setWercsConn((OracleConnection) ConnectionAccessBean.getConnection(DataSource.WERCS));
      rollupBean = buildRollupBean();
      if (rollupBean != null)
      {
        if (!rollupBean.isError())
        {
          log4jLogger.info("Processing Rollup ID " + rollupBean.getRollupId());
          processRollup(rollupBean);
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
        addToEmail("Rollup Items Requested:");
        for (ProductBean pbean: rollupBean.getRollupList())
        {
          addToEmail(pbean.getFProduct());
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
      JDBCUtil.close(getWercsConn());
    }
  }

  public RollupBean buildRollupBean()
  {
    log4jLogger.info("Gathering Rollup Data");
    RollupBean rb = null;
    Statement stmt = null;
    ResultSet rs = null;
    try
    {
      StringBuilder sql = new StringBuilder();
      sql.append("  SELECT rollup_id, ");
      sql.append("         rollup_type, ");
      sql.append("         email, ");
      sql.append("         status, ");
      sql.append("         nvl(product_import,0), ");
      sql.append("         (SELECT PARM_VALUE ");
      sql.append("          FROM vca_gmd_run_parameters@TOFM ");
      sql.append("          WHERE parm_name = 'OPTIVA_TO_WERCS_DELAY') AS otwDelay ");
      sql.append("    FROM vca_rollup_queue ");
      sql.append("   WHERE status = 0 ");
      sql.append("ORDER BY date_approved ");

      stmt = getWercsConn().createStatement();
      rs = stmt.executeQuery(sql.toString());

      if (rs.next())
      {
        rb = new RollupBean();
        rb.setRollupId(rs.getString(1));
        updateQueueStatus(rb, 1);

        rb.setRollupType(rs.getString(2));
        rb.setEmail(rs.getString(3));
        rb.setStatus(rs.getString(4));
        if (rs.getString(5).equalsIgnoreCase("Y"))
        {
          rb.setProductImport(true);
        }
        rb.setProductImportDelay(rs.getString(6));
        rb.setRollupList(buildRollupList(rb));
        rb.setComponentList(buildComponentList(rb.getRollupId(), rb.getRollupType()));
        rb.setRmBoIpList(new ArrayList<String>());
        buildRmList(rb.getRollupId(), rb.getRollupType(), rb.getRmBoIpList());
        buildBuyoutList(rb.getRollupId(), rb.getRollupType(), rb.getRmBoIpList());
        buildIpList(rb.getRollupId(), rb.getRollupType(), rb.getRmBoIpList());
        if (rb.getRmBoIpList().isEmpty())
        {
          rb.getRmBoIpList().add("TESTRM");
        }
        rb.setPostReactedList(buildPostReactedList(rb.getRollupId(), rb.getRollupType()));
        rb.setIntFgRepackList(new ArrayList<ProductBean>());
        buildIntermediateList(rb.getRollupId(), rb.getIntFgRepackList());
        if (rb.getIntFgRepackList().size() > 0)
        {
          rb.setLowestLevel(rb.getIntFgRepackList().get(0).getLevel());
        }
        else
        {
          rb.setLowestLevel(new BigDecimal("0"));
        }
        buildFgRepackList(rb.getRollupId(), rb.getRollupType(), rb.getIntFgRepackList());
        if (rb.getIntFgRepackList().isEmpty())
        {
          ProductBean testFg = new ProductBean();
          testFg.setFProduct("TESTFG");
          rb.getIntFgRepackList().add(testFg);
        }
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

  public ArrayList<String> buildComponentList(String rollupId, String rollupType)
  {
    log4jLogger.info("Building Component List");
    ArrayList<String> componentList = new ArrayList<String>();
    Statement stmt = null;
    ResultSet rs = null;
    try
    {
      StringBuilder sb = new StringBuilder();
      if (rollupType.equalsIgnoreCase("STANDARD"))
      {
        sb.append("select distinct c.f_component_id ");
        sb.append("from vca_rollup_items i, t_prod_comp c, t_product_alias_names a, t_prod_text t, t_components c1 ");
        sb.append("where c.f_product = a.f_product ");
        sb.append("and a.f_alias = i.rollup_item ");
        sb.append("and t.f_product = c.f_component_id and t.f_data_code = 'COSTCL' and t.f_text_code = 'COSTCL06' ");
        ;
        sb.append("and rollup_id = ");
        sb.append(rollupId);
      }
      else
      {
        sb.append("select distinct c.f_component_id  ");
        sb.append("from vca_rollup_items i, t_components c, t_product_alias_names a, t_prod_text t  ");
        sb.append("where c.f_component_id = a.f_product  ");
        sb.append("and a.f_alias = i.rollup_item  ");
        sb.append("and t.f_product = c.f_component_id and t.f_data_code = 'COSTCL' and t.f_text_code = 'COSTCL06'  ");
        sb.append("and rollup_id = ");
        sb.append(rollupId);
      }

      stmt = getWercsConn().createStatement();
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
    if (componentList.isEmpty())
    {
      componentList.add("TESTCOMP");
    }
    return componentList;
  }

  public void buildRmList(String rollupId, String rollupType, ArrayList<String> rmBoIpList)
  {
    log4jLogger.info("Building RM List");
    Statement stmt = null;
    ResultSet rs = null;

    try
    {
      StringBuilder sb = new StringBuilder();
      if (rollupType.equalsIgnoreCase("STANDARD"))
      {
        sb.append("SELECT DISTINCT b.f_product ");
        sb.append(" FROM t_components a, t_prod_comp b, t_prod_text c, vca_rollup_items i, t_product_alias_names an ");
        sb.append(" WHERE a.f_component_id = an.f_product  ");
        sb.append(" and i.rollup_item = an.f_alias ");
        sb.append(" and b.f_component_id = a.f_component_id ");
        sb.append(" and b.f_cas_number = b.f_cas_number ");
        sb.append(" and b.f_product = c.f_product  ");
        sb.append(" and c.f_data_code = 'COSTCL' ");
        sb.append(" AND c.f_text_code = 'COSTCL01' ");
        sb.append(" and i.rollup_id = ");
        sb.append(rollupId);
      }
      else
      {
        sb.append("select distinct b.f_product  ");
        sb.append("from t_prod_text b, t_product_alias_names an, vca_rollup_items i  ");
        sb.append("where b.f_data_code = 'COSTCL'  ");
        sb.append("and b.f_text_code = 'COSTCL01'  ");
        sb.append(" and b.f_product = an.f_product ");
        sb.append("and an.f_alias = i.rollup_item  ");
        sb.append("and rollup_id = ");
        sb.append(rollupId);
      }
      stmt = getWercsConn().createStatement();
      rs = stmt.executeQuery(sb.toString());

      while (rs.next())
      {
        rmBoIpList.add(rs.getString(1));
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

  public void buildBuyoutList(String rollupId, String rollupType, ArrayList<String> rmBoIpList)
  {
    log4jLogger.info("Building Buyout List");
    Statement stmt = null;
    ResultSet rs = null;

    try
    {
      StringBuilder sb = new StringBuilder();
      if (rollupType.equalsIgnoreCase("STANDARD"))
      {
        sb.append("SELECT DISTINCT b.f_product ");
        sb.append(" FROM t_components a, t_prod_comp b, t_prod_text c, vca_rollup_items i, t_product_alias_names an ");
        sb.append(" WHERE a.f_component_id = an.f_product  ");
        sb.append(" and i.rollup_item = an.f_alias ");
        sb.append(" and b.f_component_id = a.f_component_id ");
        sb.append(" and b.f_cas_number = b.f_cas_number ");
        sb.append(" and b.f_product = c.f_product  ");
        sb.append(" and c.f_data_code = 'COSTCL' ");
        sb.append(" AND c.f_text_code = 'COSTCL02' ");
        sb.append(" and i.rollup_id = ");
        sb.append(rollupId);
      }
      else
      {
        sb.append("select distinct b.f_product  ");
        sb.append("from t_prod_text b, t_product_alias_names an, vca_rollup_items i  ");
        sb.append("where b.f_data_code = 'COSTCL'  ");
        sb.append("and b.f_text_code = 'COSTCL02'  ");
        sb.append("and b.f_product = an.f_product  ");
        sb.append("and an.f_alias = i.rollup_item  ");
        sb.append("and rollup_id = ");
        sb.append(rollupId);
      }
      stmt = getWercsConn().createStatement();
      rs = stmt.executeQuery(sb.toString());

      while (rs.next())
      {
        rmBoIpList.add(rs.getString(1));
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

  public void buildIpList(String rollupId, String rollupType, ArrayList<String> rmBoIpList)
  {
    log4jLogger.info("Building IP List");
    Statement stmt = null;
    ResultSet rs = null;

    try
    {
      StringBuilder sb = new StringBuilder();
      if (rollupType.equalsIgnoreCase("STANDARD"))
      {
        sb.append("SELECT DISTINCT b.f_product ");
        sb.append(" FROM t_components a, t_prod_comp b, t_prod_text c, vca_rollup_items i, t_product_alias_names an ");
        sb.append(" WHERE a.f_component_id = an.f_product  ");
        sb.append(" and i.rollup_item = an.f_alias ");
        sb.append(" and b.f_component_id = a.f_component_id ");
        sb.append(" and b.f_cas_number = b.f_cas_number ");
        sb.append(" and b.f_product = c.f_product  ");
        sb.append(" and c.f_data_code = 'IPSTAT' ");
        sb.append(" AND b.f_product like 'RMX-%' ");
        sb.append(" and i.rollup_id = ");
        sb.append(rollupId);
      }
      else
      {
        sb.append("select distinct b.f_product  ");
        sb.append("from t_prod_text b, t_product_alias_names an, vca_rollup_items i  ");
        sb.append("where b.f_data_code = 'IPSTAT'  ");
        sb.append("and b.f_product like 'RMX-%'  ");
        sb.append("and b.f_product = an.f_product  ");
        sb.append("and an.f_alias = i.rollup_item  ");
        sb.append("and rollup_id = ");
        sb.append(rollupId);
      }
      stmt = getWercsConn().createStatement();
      rs = stmt.executeQuery(sb.toString());

      while (rs.next())
      {
        rmBoIpList.add(rs.getString(1));
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

  public ArrayList<String> buildPostReactedList(String rollupId, String rollupType)
  {
    log4jLogger.info("Building Post Reacted List");
    ArrayList<String> resinList = new ArrayList<String>();
    Statement stmt = null;
    ResultSet rs = null;
    try
    {
      StringBuilder sb = new StringBuilder();
      if (rollupType.equalsIgnoreCase("STANDARD"))
      {
        sb.append("SELECT DISTINCT b.f_product  ");
        sb.append("FROM t_components a,  ");
        sb.append("t_prod_comp b,  ");
        sb.append("t_prod_text t,   ");
        sb.append("vca_rollup_items i,  ");
        sb.append("t_product_alias_names an  ");
        sb.append("WHERE     a.f_component_id = an.f_product  ");
        sb.append("AND i.rollup_item = an.f_alias  ");
        sb.append("AND b.f_component_id = a.f_component_id  ");
        sb.append("AND b.f_cas_number = b.f_cas_number  ");
        sb.append("AND b.f_product = t.f_product  ");
        sb.append("AND t.f_data_code = 'COSTCL'  ");
        sb.append("AND t.f_text_code IN ('COSTCL05','COSTCL08') ");
        sb.append("AND i.rollup_id =  ");
        sb.append(rollupId);
        sb.append("UNION  ");
        sb.append("SELECT DISTINCT an.f_product  ");
        sb.append("FROM vca_rollup_items i, t_product_alias_names an, t_prod_text t  ");
        sb.append("WHERE     i.rollup_item = an.f_alias  ");
        sb.append("AND t.f_product = an.f_product  ");
        sb.append("AND t.f_data_code = 'COSTCL'  ");
        sb.append("       AND t.f_text_code IN ('COSTCL05','COSTCL08') ");
        sb.append("AND i.rollup_id = ");
        sb.append(rollupId);
        sb.append("UNION  ");
        sb.append("SELECT DISTINCT a.f_product  ");
        sb.append("  FROM vca_rollup_items i,  ");
        sb.append("       t_prod_with_input_prod a,  ");
        sb.append("       t_prod_text t,  ");
        sb.append("       t_product_alias_names an  ");
        sb.append(" WHERE     a.f_product = t.f_product  ");
        sb.append("       AND a.f_input_product = an.f_product  ");
        sb.append("       AND i.rollup_item = an.f_alias  ");
        sb.append("       AND t.f_data_code = 'COSTCL'  ");
        sb.append("       AND t.f_text_code IN ('COSTCL05','COSTCL08') ");
        sb.append("AND i.rollup_id =  ");
        sb.append(rollupId);
      }
      else
      {
        sb.append("select distinct c.f_product  ");
        sb.append("from vca_rollup_items i,  t_product_alias_names an, t_prod_text c  ");
        sb.append("where an.f_alias = i.rollup_item ");
        sb.append("and an.f_product = c.f_product  ");
        sb.append("and c.f_data_code = 'COSTCL'  ");
        sb.append("AND c.f_text_code IN ('COSTCL05','COSTCL08')  ");
        sb.append("and an.f_alias not like 'C%-%' ");
        sb.append("and rollup_id = ");
        sb.append(rollupId);
      }

      stmt = getWercsConn().createStatement();
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
    if (resinList.isEmpty())
    {
      resinList.add("TESTRESIN");
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
      StringBuilder sb = new StringBuilder();
      sb.append("select distinct a.f_product, a.f_alias_name, i.rollup_item_id, ");
      sb.append("                (SELECT 'x' ");
      sb.append("                   FROM t_prod_text ");
      sb.append("                  WHERE f_product = a.f_product AND f_text_code = 'PRDSTAT3') as obsolete ");
      sb.append("from vca_rollup_items i, t_product_alias_names a ");
      sb.append("where a.f_alias = i.rollup_item ");
      sb.append("and rollup_id = ");
      sb.append(rb.getRollupId());

      stmt = getWercsConn().createStatement();
      rs = stmt.executeQuery(sb.toString());

      while (rs.next())
      {
        ProductBean pbean = new ProductBean();
        pbean.setFProduct(rs.getString(1));
        pbean.setFProductName(rs.getString(2));
        pbean.setRollupItemId(rs.getString(3));
        if (rs.getString(4) != null && rs.getString(4).equalsIgnoreCase("x"))
        {
          pbean.setObsolete(true);
        }
        pbean.setMsdsList(buildMsdsList(pbean.getRollupItemId()));
        pbean.setAliasList(populateAlias(pbean));
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

  public void buildFgRepackList(String rollupId, String rollupType, ArrayList<ProductBean> intFgRepackList)
  {
    log4jLogger.info("Building Finished Goods and Repack List");
    Statement stmt = null;
    ResultSet rs = null;
    try
    {
      StringBuilder sb = new StringBuilder();
      if (rollupType.equalsIgnoreCase("STANDARD"))
      {
        sb.append("SELECT DISTINCT ");
        sb.append("       a.f_product, ");
        sb.append("       NVL (FSI.GET_CLASS_XREF@TOFM (FF.CLASS), FF.CLASS) AS formula_class, ");
        sb.append("       NVL (CF_FLASH_POINT, ROUND ( (TP.PVALUE * 1.8) + 32, 0)) AS FLASHF, ");
        sb.append("       NVL (ROUND ( (5 / 9) * (CF_FLASH_POINT - 32), 0), TP.PVALUE) AS FLASHC, ");
        sb.append("       ff.formula_id, ff.formula_code, ff.version, SUBSTR(ff.formula_code, INSTR(ff.formula_code, '.', -1, 1)+1, 3) as extension, ");
        sb.append("       NVL (FSI.GET_SET_CODE_XREF@tofm(ff.group_code), 'USA') as setcode, ");
        sb.append("       NVL (FSI.GET_BUS_GRP_XREF@tofm(ff.GROUP_CODE), fi.GROUP_CODE) as busgroup ");
        sb.append("  FROM vca_rollup_items i, ");
        sb.append("       t_product_alias_names an, ");
        sb.append("       t_prod_with_input_prod a, ");
        sb.append("       t_prod_text t, ");
        sb.append("       FSITEM@TOFM fi, ");
        sb.append("       FSFORMULA@TOFM ff, ");
        sb.append("       FSFORMULATECHPARAM@TOFM TP ");
        sb.append(" WHERE     i.rollup_item = an.f_alias ");
        sb.append("       AND a.f_product = fi.item_code(+)   ");
        sb.append("       AND fi.formula_id = ff.formula_id(+) ");
        sb.append("       AND NVL (ff.status_ind, 0) <> 999 ");
        sb.append("       AND TP.FORMULA_ID(+) = FF.FORMULA_ID ");
        sb.append("       AND TP.PARAM_CODE(+) = 'FL-POINT-C' ");
        sb.append("       AND a.f_input_product = an.f_product ");
        sb.append("       AND a.f_product = t.f_product ");
        sb.append("       and t.f_data_code = 'COSTCL' and t.f_text_code IN ('COSTCL03', 'COSTCL07')  ");
        sb.append("       AND i.rollup_id = ");
        sb.append(rollupId);
        sb.append("UNION ");
        sb.append("SELECT DISTINCT a.f_product,  ");
        sb.append("       NVL (FSI.GET_CLASS_XREF@TOFM (FF.CLASS), FF.CLASS) AS formula_class, ");
        sb.append("       NVL (CF_FLASH_POINT, ROUND ( (TP.PVALUE * 1.8) + 32, 0)) AS FLASHF, ");
        sb.append("       NVL (ROUND ( (5 / 9) * (CF_FLASH_POINT - 32), 0), TP.PVALUE) AS FLASHC, ");
        sb.append("       ff.formula_id, ff.formula_code, ff.version, SUBSTR(ff.formula_code, INSTR(ff.formula_code, '.', -1, 1)+1, 3) as extension, ");
        sb.append("       NVL (FSI.GET_SET_CODE_XREF@tofm(ff.group_code), 'USA') as setcode, ");
        sb.append("       NVL (FSI.GET_BUS_GRP_XREF@tofm(ff.GROUP_CODE), fi.GROUP_CODE) as busgroup ");
        sb.append("  FROM (SELECT DISTINCT a.f_product ");
        sb.append("          FROM vca_rollup_items i, ");
        sb.append("               t_prod_with_input_prod a, ");
        sb.append("               t_product_alias_names an, ");
        sb.append("               t_prod_text t  ");
        sb.append("         WHERE     a.f_input_product = an.f_product  ");
        sb.append("               AND i.rollup_item = an.f_alias ");
        sb.append("               and a.f_product = t.f_product ");
        sb.append("               and t.f_data_code = 'COSTCL' and t.f_text_code IN ('COSTCL05','COSTCL08') ");
        sb.append("               AND i.rollup_id = ");
        sb.append(rollupId);
        sb.append(") r, ");
        sb.append("       t_prod_with_input_prod a, ");
        sb.append("       t_prod_text t, ");
        sb.append("       FSITEM@TOFM fi, ");
        sb.append("       FSFORMULA@TOFM ff,  ");
        sb.append("       FSFORMULATECHPARAM@TOFM TP ");
        sb.append(" WHERE     a.f_input_product = r.f_product ");
        sb.append("       AND a.f_product = fi.item_code(+) ");
        sb.append("       AND fi.formula_id = ff.formula_id(+) ");
        sb.append("       and nvl(ff.status_ind,0) <> 999 ");
        sb.append("       AND TP.FORMULA_ID(+) = FF.FORMULA_ID ");
        sb.append("       AND TP.PARAM_CODE(+) = 'FL-POINT-C' ");
        sb.append("       and a.f_product = t.f_product ");
        sb.append("       and t.f_data_code = 'COSTCL' and t.f_text_code IN ('COSTCL03','COSTCL07') ");
      }
      else
      {
        sb.append("SELECT DISTINCT ");
        sb.append("       a.f_product, ");
        sb.append("       NVL (FSI.GET_CLASS_XREF@TOFM (FF.CLASS), FF.CLASS) AS formula_class, ");
        sb.append("       NVL (CF_FLASH_POINT, ROUND ( (TP.PVALUE * 1.8) + 32, 0)) AS FLASHF, ");
        sb.append("       NVL (ROUND ( (5 / 9) * (CF_FLASH_POINT - 32), 0), TP.PVALUE) AS FLASHC, ");
        sb.append("       ff.formula_id, ff.formula_code, ff.version, SUBSTR(ff.formula_code, INSTR(ff.formula_code, '.', -1, 1)+1, 3) as extension, ");
        sb.append("       NVL (FSI.GET_SET_CODE_XREF@tofm(ff.group_code), 'USA') as setcode, ");
        sb.append("       NVL (FSI.GET_BUS_GRP_XREF@tofm(ff.GROUP_CODE), fi.GROUP_CODE) as busgroup ");
        sb.append("  FROM vca_rollup_items i, ");
        sb.append("       t_product_alias_names a, ");
        sb.append("       t_prod_text t, ");
        sb.append("       FSITEM@TOFM fi, ");
        sb.append("       FSFORMULA@TOFM ff, ");
        sb.append("       FSFORMULATECHPARAM@TOFM TP ");
        sb.append(" WHERE     a.f_alias = i.rollup_item ");
        sb.append("       AND a.f_product = fi.item_code (+) ");
        sb.append("       AND fi.formula_id = ff.formula_id (+) ");
        sb.append("       AND NVL (ff.status_ind, 0) <> 999 ");
        sb.append("       AND TP.FORMULA_ID(+) = FF.FORMULA_ID ");
        sb.append("       AND TP.PARAM_CODE(+) = 'FL-POINT-C' ");
        sb.append("       AND a.f_product = t.f_product ");
        sb.append("       AND t.f_data_code = 'COSTCL' ");
        sb.append("       AND t.f_text_code IN ('COSTCL03', 'COSTCL07') ");
        sb.append("       AND i.rollup_id = ");
        sb.append(rollupId);
      }

      stmt = getWercsConn().createStatement();
      rs = stmt.executeQuery(sb.toString());

      while (rs.next())
      {
        ProductBean pBean = new ProductBean();
        pBean.setFProduct(rs.getString(1));
        pBean.setFormulaClass(rs.getString(2));
        pBean.setFlashF(rs.getString(3));
        pBean.setFlashC(rs.getString(4));
        pBean.setFormulaId(rs.getString(5));
        pBean.setFormulaCode(rs.getString(6));
        pBean.setVersion(rs.getString(7));
        pBean.setExtension(rs.getString(8));
        pBean.setSetCode(rs.getString(9));
        pBean.setBusinessGroup(rs.getString(10));
        intFgRepackList.add(pBean);
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

  public ArrayList<MsdsBean> buildMsdsList(String rollupItemId)
  {
    ArrayList<MsdsBean> msdsList = new ArrayList<MsdsBean>();
    Statement stmt = null;
    ResultSet rs = null;
    try
    {
      StringBuilder sb = new StringBuilder();
      sb.append("select subformat, language from vca_rollup_item_msds ");
      sb.append("where rollup_item_id = ");
      sb.append(rollupItemId);
      stmt = getWercsConn().createStatement();
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

  public void buildIntermediateList(String rollupId, ArrayList<ProductBean> intFgRepackList)
  {
    log4jLogger.info("Building Intermediate List");
    ArrayList<String> itemCodeList = new ArrayList<String>();
    populateIntermediateStagingTable(rollupId);
    Statement stmt = null;
    ResultSet rs = null;
    try
    {
      StringBuilder sb = new StringBuilder();

      sb.append("select distinct item, item_level, flashf, flashc, formula_class, formula_id, business_group, set_code, formula_code, version, extension ");
      sb.append("from vca_rollup_int_staging ");
      sb.append("where rollup_id =  ");
      sb.append(rollupId);
      sb.append(" order by item_level desc ");

      stmt = getWercsConn().createStatement();
      rs = stmt.executeQuery(sb.toString());

      while (rs.next())
      {
        String itemCode = rs.getString(1);
        if (itemCodeList.indexOf(itemCode) == -1)
        {
          ProductBean iBean = new ProductBean();
          iBean.setFProduct(itemCode);
          iBean.setLevel(new BigDecimal(rs.getString(2)));
          iBean.setFlashF(rs.getString(3));
          iBean.setFlashC(rs.getString(4));
          iBean.setFormulaClass(rs.getString(5));
          iBean.setFormulaId(rs.getString(6));
          iBean.setBusinessGroup(rs.getString(7));
          iBean.setSetCode(rs.getString(8));
          iBean.setFormulaCode(rs.getString(9));
          iBean.setVersion(rs.getString(10));
          iBean.setExtension(rs.getString(11));
          iBean.setIntermediate(true);
          intFgRepackList.add(iBean);
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
  }

  public void populateIntermediateStagingTable(String rollupId)
  {
    OracleCallableStatement cst = null;
    try
    {
      cst = (OracleCallableStatement) getWercsConn().prepareCall("{CALL vca_rollup_pkg.get_ingr_list(?)}");
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
    OracleCallableStatement cst = null;
    try
    {
      cst = (OracleCallableStatement) getWercsConn().prepareCall("delete from vca_rollup_int_staging where rollup_id = ?");
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

  public void processRollup(RollupBean rollupBean)
  {
    String jobId = ETLUtility.getNextJobId(getWercsConn());
    log4jLogger.info(jobId + " created for rollup: " + rollupBean.getRollupId());
    ArrayList<IprocessBean> processBeanList = new ArrayList<IprocessBean>();
    try
    {
      int ctr;
      Long priority;
      Long status = new Long("0");
      priority = new Long("1");
      ctr = 0;
      for (String component: rollupBean.getComponentList())
      {
        ctr++;
        if (ctr == rollupBean.getComponentList().size())
        {
          priority = new Long("9");
        }
        processBeanList.add(ETLUtility.createIprocessBean(rollupBean.getRollupId(), ETLUtility.getProcessGroupId(getWercsConn(), "RollupComponent1"), status, priority, jobId, component, "0"));
      }
      status = new Long("5");

      priority = new Long("1");
      ctr = 0;
      for (String product: rollupBean.getRmBoIpList())
      {
        ctr++;
        if (ctr == rollupBean.getRmBoIpList().size())
        {
          priority = new Long("9");
        }
        processBeanList.add(ETLUtility.createIprocessBean(rollupBean.getRollupId(), ETLUtility.getProcessGroupId(getWercsConn(), "RollupRM1"), status, priority, jobId, product, "0"));
      }

      status = new Long("5");
      priority = new Long("1");
      ctr = 0;
      for (String product: rollupBean.getPostReactedList())
      {
        ctr++;
        if (ctr == rollupBean.getPostReactedList().size())
        {
          priority = new Long("9");
        }
        processBeanList.add(ETLUtility.createIprocessBean(rollupBean.getRollupId(), ETLUtility.getProcessGroupId(getWercsConn(), "RollupResin1"), status, priority, jobId, product, "0"));
      }

      for (ProductBean pBean: rollupBean.getRollupList())
      {
        if (pBean.getMsdsList().size() > 0)
        {
          if (!pBean.isObsolete())
          {
            for (MsdsBean msdsBean: pBean.getMsdsList())
            {
              processBeanList.add(ETLUtility.createIprocessPublishingBean(pBean.getFProduct(), "MTR", msdsBean.getSubFormat(), msdsBean.getLanguage(), rollupBean.getRollupId(), new Long(1), new Long(5), jobId));
            }
          }
        }
      }
     // processCostCL01Products(rollupBean, jobId);
      for (String product: rollupBean.getRmBoIpList())
      {
        addToPublishingQueue(product, processBeanList, jobId, rollupBean.getRollupId(), false);
      }
      for (String product: rollupBean.getPostReactedList())
      {
        addToPublishingQueue(product, processBeanList, jobId, rollupBean.getRollupId(), false);
      }
      for (ProductBean productBean: rollupBean.getIntFgRepackList())
      {
        addToPublishingQueue(productBean.getFProduct(), processBeanList, jobId, rollupBean.getRollupId(), false);
      }

      for (ProductBean pBean: rollupBean.getRollupList())
      {
        for (String alias: pBean.getAliasList())
        {
          addToPublishingQueue(alias, processBeanList, jobId, rollupBean.getRollupId(), true);
        }
      }
      if (rollupBean.isProductImport())
      {
        addToProductImportQueue(rollupBean, processBeanList);
      }
      else
      {
        for (ProductBean productBean: rollupBean.getIntFgRepackList())
        {
          processBeanList.add(ETLUtility.createIprocessBean(rollupBean.getRollupId(), ETLUtility.getProcessGroupId(getWercsConn(), "RollupFG1"), status, new Long("9"), jobId, productBean.getFProduct(), "0"));
        }
      }

      ETLUtility.submitIprocessJDBC(getWercsConn(), processBeanList);
    }
    catch (Exception e)
    {
      log4jLogger.error(e);
      updateQueueStatus(rollupBean, -99);
    }
  }

  public void addToProductImportQueue(RollupBean rb, ArrayList<IprocessBean> processBeanList)
  {
    BigDecimal priority = new BigDecimal("1");

    StringBuilder updateSb = new StringBuilder();
    updateSb.append("update i_process ");
    updateSb.append("set f_status = ? ");
    updateSb.append(" WHERE     f_PRODUCT = ? ");
    updateSb.append("       AND f_process_group_id = ");
    updateSb.append("              (SELECT f_process_group_id ");
    updateSb.append("                 FROM i_process_groups ");
    updateSb.append("                WHERE f_process_group_name = 'Product Import') ");
    updateSb.append("       AND f_STATUS in (0,5) ");

    CallableStatement updateCstmt = null;

    try
    {
      updateCstmt = getWercsConn().prepareCall(updateSb.toString());

      for (ProductBean pBean: rb.getIntFgRepackList())
      {
        try
        {
          if (StringUtils.isEmpty(pBean.getFlashF()) || StringUtils.isEmpty(pBean.getFlashC()))
          {
            log4jLogger.error("No Flashf or Flashc, record will not be added to ProductImport queue: " + pBean.getFProduct());
          }
          else
          {
            if ("OF".equals(pBean.getFormulaClass()))
            {
              priority = priority.add(new BigDecimal("1"));
            }
            if (pBean.isIntermediate())
            {
              priority = rb.getLowestLevel().subtract(pBean.getLevel()).add(new BigDecimal("3"));
            }
            pBean.setDataCodes(WercsUtility.getProductImportDataAttributes(pBean));
            if (!pBean.getDataCodes().isEmpty())
            {
              IprocessBean iProcessBean = ETLUtility.createIprocessBean(rb.getRollupId(), ETLUtility.getProcessGroupId(getWercsConn(), "Product Import"), new Long("5"), new Long(priority.toString()), ETLUtility.getNextJobId(getWercsConn()), pBean.getFProduct(), rb.getProductImportDelay());
              iProcessBean.setIProducts(ETLUtility.populateIproductBean(pBean, iProcessBean.getJobId(), rb.getRollupId()));
              iProcessBean.setIFormulations(ETLUtility.populateIformulationBeans(getWercsConn(), pBean, iProcessBean.getJobId(), rb.getRollupId()));
              iProcessBean.setIAttributes(ETLUtility.populateIattributeBeans(pBean, iProcessBean.getJobId(), rb.getRollupId(), true));
              ETLUtility.populatePublishingData(iProcessBean, pBean, null);
              processBeanList.add(iProcessBean);
              updateCstmt.setString(1, "7");
              updateCstmt.setString(2, pBean.getFProduct());
              updateCstmt.addBatch();
            }
            else
            {
              log4jLogger.info(pBean.getFProduct() + " formula class '" + pBean.getFormulaClass() + "' is not in VCA_OPTIVA_WERCS_MAPPING");
            }
          }
        }
        catch (Exception e)
        {
          log4jLogger.error("Product: " + pBean.getFProduct(), e);
        }
      }
      updateCstmt.executeBatch();
    }
    catch (Exception e)
    {
      log4jLogger.error("Could not add to Product Import queue for Rollup: " + rb.getRollupId(), e);
    }
    finally
    {
      JDBCUtil.close(updateCstmt);
    }
  }

  public void addToPublishingQueue(String product, ArrayList<IprocessBean> processBeanList, String jobId, String rollupId, boolean aliasFlag)
  {
    StringBuilder sb = new StringBuilder();
    if (aliasFlag)
    {
      sb.append("select b.f_format,b.f_subformat,b.f_language, ");
      sb.append("(SELECT 'x' ");
      sb.append("                   FROM t_prod_text a1, t_product_alias_names b1 ");
      sb.append("                  WHERE a1.f_product = b1.f_product AND a1.f_text_code = 'PRDSTAT3'  and b1.f_alias =  :PRODUCT) as obsolete ");
      sb.append("from t_product_alias_names a,t_pdf_msds b ");
      sb.append("where a.f_alias = b.f_product and a.f_product != a.f_alias and b.f_authorized = 3 ");
      sb.append("and  b.f_product = :PRODUCT ");
    }
    else
    {
      sb.append("SELECT A.F_FORMAT, A.F_SUBFORMAT, A.F_LANGUAGE, ");
      sb.append("(SELECT 'x' ");
      sb.append("                   FROM t_prod_text a1, t_product_alias_names b1 ");
      sb.append("                  WHERE a1.f_product = b1.f_product AND a1.f_text_code = 'PRDSTAT3'  and b1.f_alias =  :PRODUCT) as obsolete ");
      sb.append("  FROM WERCS.VCA_PRODUCT_MSDS_TYPES_V A, T_ALIAS_LANGUAGE_NAMES B ");
      sb.append(" WHERE     a.F_alias = b.F_ALIAS(+) ");
      sb.append("       AND a.F_LANGUAGE = b.F_LANGUAGE(+) ");
      sb.append("       AND A.f_alias = :PRODUCT ");
      sb.append("       AND A.F_AUTO_PUBLISH = 'Y' ");
    }
   
    OraclePreparedStatement pst = null;
    ResultSet rs = null;
    try
    {
      pst = (OraclePreparedStatement) getWercsConn().prepareStatement(sb.toString());
      pst.setStringAtName("PRODUCT", product);
      try
      {
        rs = pst.executeQuery();
        while (rs.next())
        {
          if (rs.getString(4) == null)
          {
            processBeanList.add(ETLUtility.createIprocessPublishingBean(product, rs.getString(1), rs.getString(2), rs.getString(3), rollupId, new Long(1), new Long(5), jobId));
          }
        }
      }
      catch (Exception e)
      {
        log4jLogger.error("Alias: " + product, e);
      }
    }
    catch (Exception e)
    {
      log4jLogger.error("Could not add to publishing queue for Rollup", e);
    }
    finally
    {
      JDBCUtil.close(pst, rs);
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
      StringBuilder sql = new StringBuilder();
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

      stmt = getWercsConn().createStatement();
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

  private List<String> populateAlias(ProductBean productBean)
  {
    OraclePreparedStatement pst = null;
    ResultSet rs = null;
    List<String> aliasList = new ArrayList<String>();
    try
    {
      StringBuilder sb = new StringBuilder();
      sb.append("select distinct a.f_alias   from t_product_alias_names a, t_pdf_msds b ");
      sb.append("where  a.f_alias = b.f_product  and b.f_authorized = 3 ");
      sb.append("and a.f_product != a.f_alias  and a.f_product = :PRODUCT ");

      pst = (OraclePreparedStatement) getWercsConn().prepareStatement(sb.toString());
      pst.setStringAtName("PRODUCT", productBean.getFProduct());
      rs = pst.executeQuery();
      while (rs.next())
      {
        aliasList.add(rs.getString(1));
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
    return aliasList;
  }

  private void processCostCL01Products(RollupBean rollupBean, String jobId)
  {
    ArrayList<IprocessBean> processBeanList = new ArrayList<IprocessBean>();
    for(ProductBean productBean: rollupBean.getRollupList())
    {
      String isCostClass01 = ValsparLookUps.queryForSingleValueLeaveConnectionOpen(getWercsConn(), "SELECT 'X' FROM T_PROD_TEXT WHERE  F_PRODUCT = ? AND F_TEXT_CODE = 'COSTCL01' ", productBean.getFProduct());
      if(StringUtils.isNotEmpty(isCostClass01))
      {
        processBeanList.add(ETLUtility.createIprocessPublishingBean(productBean.getFProduct(), "MTR", "SRRM", "EN", rollupBean.getRollupId(), new Long(1), new Long(5), jobId));
      }
    }
    if(!processBeanList.isEmpty())
    {
      ETLUtility.submitIprocessJDBC(getWercsConn(), processBeanList);
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
}
