package com.valspar.interfaces.regulatory.optivacosting.program;

import com.valspar.interfaces.common.BaseInterface;
import com.valspar.interfaces.common.beans.ConnectionAccessBean;
import com.valspar.interfaces.common.enums.DataSource;
import com.valspar.interfaces.common.utils.*;
import com.valspar.interfaces.regulatory.optivacosting.beans.ProductBean;
import java.sql.*;
import java.util.ArrayList;
import oracle.jdbc.OracleConnection;
import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;

public class OptivaCostingInterface extends BaseInterface
{
  private OracleConnection northAmericanConn = null;
  private OracleConnection optivaConn = null;
  private static Logger log4jLogger = Logger.getLogger(OptivaCostingInterface.class);

  public OptivaCostingInterface()
  {
  }

  public void execute()
  {
    try
    {
      northAmericanConn = (OracleConnection) ConnectionAccessBean.getConnection(DataSource.NORTHAMERICAN);
      optivaConn = (OracleConnection) ConnectionAccessBean.getConnection(DataSource.FORMULATION);
      ArrayList<ProductBean> ar = new ArrayList<ProductBean>();
      updateFsiItemClass();
      getOptivaCost("vca_optiva_actconv_costing", ar, "SA", "EC$", "SAC", "EPSACTCONV");
      getOptivaCost("vca_optiva_actconv_costing", ar, "VB", "US$", "SAC", "VALACTCONV");

      getOptivaCost("vca_optiva_stdconv_costing", ar, "SD", "EC$", "STND", "EPSSTDCONV");
      getOptivaCost("vca_optiva_stdconv_costing", ar, "VS", "US$", "STND", "VALSTDCONV");

      getOptivaCost("vca_optiva_actmat_costing", ar, "VB", "US$", "SAC", "VALACTRM");
      getOptivaCost("vca_optiva_actmat_costing", ar, "SA", "EC$", "SAC", "EPSACTRM");

      getOptivaCost("vca_optiva_stdmat_costing", ar, "SD", "EC$", "STND", "EPSSTDRM");
      getOptivaCost("vca_optiva_stdmat_costing", ar, "VS", "US$", "STND", "VALSTDRM");

      getOptivaCost("vca_optiva_actrmint_costing", ar, "VB", "US$", "SAC", "VALACTRM");
      getOptivaCost("vca_optiva_actrmint_costing", ar, "SA", "EC$", "SAC", "EPSACTRM");

      getOptivaCost("vca_optiva_stdrmint_costing", ar, "SD", "EC$", "STND", "EPSSTDRM");
      getOptivaCost("vca_optiva_stdrmint_costing", ar, "VS", "US$", "STND", "VALSTDRM");

      getSACCost(ar, "US$", "VAL", "VB%", "VALSACRM");

      getSACCost(ar, "EC$", "EPS", "SA%", "EPSSACRM");

      getSACCost(ar, "CN$", "CAD", "CB%", "CANSACRM");

      updateOptivaCosts(ar);
    }
    catch (Exception e)
    {
      log4jLogger.error(e);
    }
    finally
    {
      this.getEmailMessages().addAll(fetchErrorMessages());
      JDBCUtil.close(northAmericanConn);
      JDBCUtil.close(optivaConn);
    }
  }

  public void getOptivaCost(String viewName, ArrayList<ProductBean> ar, String calendar, String warehouse, String cost_method, String optiva_param)
  {
    Statement stmt = null;
    ResultSet rs = null;
    try
    {
      StringBuffer sql = new StringBuffer();
      sql.append("select item_no, cost_value, item_um from ");
      sql.append(viewName);
      sql.append(" where calendar_code like '");
      sql.append(calendar);
      sql.append("%' and whse_code = '");
      sql.append(warehouse);
      sql.append("' and cost_mthd_code = '");
      sql.append(cost_method);
      sql.append("' and cost_value is not null ");
      log4jLogger.info("" + viewName + " sql = " + sql);
      stmt = northAmericanConn.createStatement();
      rs = stmt.executeQuery(sql.toString());
      while (rs.next())
      {
        ProductBean pb = new ProductBean();
        pb.setProduct(rs.getString(1));
        pb.setCost(rs.getString(2));
        pb.setItemUm(rs.getString(3));
        pb.setOptivaParam(optiva_param);
        pb.setViewName(viewName);
        ar.add(pb);
      }
    }
    catch (Exception e)
    {
      log4jLogger.error("for " + viewName, e);
    }
    finally
    {
      JDBCUtil.close(stmt, rs);
    }
  }

  public void getSACCost(ArrayList<ProductBean> ar, String whse_code, String orgn_code, String calendar, String optiva_param)
  {
    PreparedStatement stmt = null;
    ResultSet rs = null;
    try
    {
      StringBuffer sql = new StringBuffer();
      sql.append("  SELECT DISTINCT M.ITEM_NO, ROUND (G.ACCTG_COST, 4) ");
      sql.append("    FROM APPS.IC_ITEM_MST M, APPS.GL_ITEM_CST G ");
      sql.append("   WHERE     M.ITEM_ID = G.ITEM_ID ");
      sql.append("         AND G.WHSE_CODE = ? ");
      sql.append("         AND G.ORGN_CODE = ? ");
      sql.append("         AND G.PERIOD_CODE IN ");
      sql.append("                (  SELECT DISTINCT PERIOD_CODE ");
      sql.append("                     FROM APPS.CM_CLDR_DTL CM_CLDR_DTL1 ");
      sql.append("                    WHERE SYSDATE < END_DATE AND SYSDATE > START_DATE ");
      sql.append("                 GROUP BY PERIOD_CODE) ");
      sql.append("         AND G.CALENDAR_CODE IN ");
      sql.append("                (SELECT DISTINCT CALENDAR_CODE ");
      sql.append("                   FROM APPS.CM_CLDR_DTL CM_CLDR_DTL2 ");
      sql.append("                  WHERE     CALENDAR_CODE LIKE ? ");
      sql.append("                        AND SYSDATE < END_DATE ");
      sql.append("                        AND SYSDATE > START_DATE) ");
      sql.append("         AND M.ITEMCOST_CLASS NOT IN ('4000W55M', 'LC96', 'MA', 'MRO') ");
      sql.append("         AND G.LAST_UPDATE_DATE > SYSDATE - 1 ");
      sql.append("GROUP BY M.ITEM_NO, ");
      sql.append("         G.ACCTG_COST, ");
      sql.append("         M.ITEM_UM, ");
      sql.append("         G.PERIOD_CODE, ");
      sql.append("         G.CALENDAR_CODE    ");
      stmt = northAmericanConn.prepareStatement(sql.toString());
      stmt.setString(1, whse_code);
      stmt.setString(2, orgn_code);
      stmt.setString(3, calendar);
      log4jLogger.info("sql = " + sql);
      rs = stmt.executeQuery();
      while (rs.next())
      {
        ProductBean pb = new ProductBean();
        pb.setProduct(rs.getString(1));
        pb.setCost(rs.getString(2));
        pb.setOptivaParam(optiva_param);
        ar.add(pb);
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

  private void updateOptivaCosts(ArrayList<ProductBean> ar)
  {
    try
    {
      log4jLogger.info("Updating Item costs in Optiva...");
      for (ProductBean pb: ar)
      {
        boolean updateCost = false;

        StringBuffer sb = new StringBuffer();
        sb.append("select formula_id, decode(component_ind,1,'FORMULA','ITEM'), ");
        sb.append("       DECODE ( ");
        sb.append("          component_ind, ");
        sb.append("          1, (SELECT pvalue ");
        sb.append("                FROM fsformulatechparam ");
        sb.append("               WHERE formula_id = a.formula_id AND param_code = ?), ");
        sb.append("          (SELECT pvalue ");
        sb.append("             FROM fsitemtechparam ");
        sb.append("            WHERE item_code = a.item_code AND param_code = ?)) ");
        sb.append("          AS currentOptivaCost ");
        sb.append("from fsitem a ");
        sb.append("where item_code = ? ");
        sb.append("and logical_delete = 0 ");
        sb.append("and status_ind = 400 ");
        if (!StringUtils.isEmpty(pb.getViewName()))
        {
          sb.append("and uom_code = ? ");

          if (pb.getViewName().equalsIgnoreCase("vca_optiva_actconv_costing") || pb.getViewName().equalsIgnoreCase("vca_optiva_stdconv_costing"))
          {
            sb.append("and class = 'INGRED' ");
            sb.append("and component_ind = 1");
          }

          if (pb.getViewName().equalsIgnoreCase("vca_optiva_actrmint_costing") || pb.getViewName().equalsIgnoreCase("vca_optiva_stdrmint_costing"))
          {
            sb.append("and component_ind = 1");
          }

          if (pb.getViewName().equalsIgnoreCase("vca_optiva_actmat_costing") || pb.getViewName().equalsIgnoreCase("vca_optiva_stdmat_costing"))
          {
            sb.append("and component_ind in (8,2)");
          }
        }

        PreparedStatement stmt = null;
        ResultSet rs = null;
        try
        {
          stmt = optivaConn.prepareStatement(sb.toString());
          stmt.setString(1, pb.getOptivaParam());
          stmt.setString(2, pb.getOptivaParam());
          stmt.setString(3, pb.getProduct());
          if (!StringUtils.isEmpty(pb.getViewName()))
          {
            stmt.setString(4, pb.getItemUm());
          }

          rs = stmt.executeQuery();
          if (rs.next())
          {
            if (!pb.getCost().equalsIgnoreCase(rs.getString(3)))
            {
              pb.setFormulaID(rs.getString(1));
              pb.setType(rs.getString(2));
              updateCost = true;
            }
          }
        }
        catch (Exception e)
        {
          log4jLogger.error("for product : " + pb.getProduct(), e);
        }
        finally
        {
          JDBCUtil.close(stmt, rs);
        }

        if (updateCost)
        {
          if (pb.getType().equalsIgnoreCase("FORMULA"))
          {
            if (!pb.getFormulaId().equals("0"))
            {
              updateFormulaCost(pb);
            }
          }
          else
          {
            updateItemCost(pb);
          }
        }
      }
      log4jLogger.info("Finished updating Item costs in Optiva...");
    }
    catch (Exception e)
    {
      log4jLogger.error(e);
    }
  }

  private void updateItemCost(ProductBean pb)
  {
    StringBuffer sb = new StringBuffer();

    sb.append("MERGE INTO fsi.fsitemtechparam a ");
    sb.append("     USING (SELECT 'x' FROM DUAL) b ");
    sb.append("        ON (a.item_code = ? AND a.param_code = ?) ");
    sb.append("WHEN MATCHED ");
    sb.append("THEN ");
    sb.append("   UPDATE SET pvalue = ? ");
    if (pb.getOptivaParam().endsWith("SACRM"))
    {
      sb.append("     , calc_level = ? ");
    }
    sb.append("WHEN NOT MATCHED ");
    sb.append("THEN ");
    sb.append("   INSERT     (ITEM_CODE, ");
    sb.append("               PARAM_CODE, ");
    sb.append("               CALC_LEVEL, ");
    sb.append("               PVALUE) ");
    sb.append("       VALUES (?, ");
    sb.append("               ?, ");
    sb.append("               ?, ");
    sb.append("               ?) ");

    PreparedStatement stmt = null;
    try
    {
      stmt = optivaConn.prepareStatement(sb.toString());
      stmt.setString(1, pb.getProduct());
      stmt.setString(2, pb.getOptivaParam());
      stmt.setString(3, pb.getCost());
      if (pb.getOptivaParam().endsWith("SACRM"))
      {
        stmt.setString(4, "1");
        stmt.setString(5, pb.getProduct());
        stmt.setString(6, pb.getOptivaParam());
        stmt.setString(7, "1");
        stmt.setString(8, pb.getCost());
      }
      else
      {
        stmt.setString(4, pb.getProduct());
        stmt.setString(5, pb.getOptivaParam());
        stmt.setString(6, "0");
        stmt.setString(7, pb.getCost());
      }

      stmt.executeUpdate();
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

  private void updateFormulaCost(ProductBean pb)
  {
    StringBuffer sb = new StringBuffer();
    sb.append("MERGE INTO fsi.fsformulatechparam a ");
    sb.append("     USING (SELECT 'x' FROM DUAL) b ");
    sb.append("        ON (a.formula_id = ? AND a.param_code = ?) ");
    sb.append("WHEN MATCHED ");
    sb.append("THEN ");
    sb.append("   UPDATE SET pvalue = ? ");
    if (pb.getOptivaParam().endsWith("SACRM"))
    {
      sb.append("     , calc_level = ? ");
    }
    sb.append("WHEN NOT MATCHED ");
    sb.append("THEN ");
    sb.append("   INSERT     (formula_id, ");
    sb.append("               PARAM_CODE, ");
    sb.append("               rollup_ind, ");
    sb.append("               CALC_LEVEL, ");
    sb.append("               PVALUE) ");
    sb.append("       VALUES (?, ");
    sb.append("               ?, ");
    sb.append("               0, ");
    sb.append("               ?, ");
    sb.append("               ?) ");

    PreparedStatement stmt = null;
    try
    {
      stmt = optivaConn.prepareStatement(sb.toString());
      stmt.setString(1, pb.getFormulaId());
      stmt.setString(2, pb.getOptivaParam());
      stmt.setString(3, pb.getCost());
      if (pb.getOptivaParam().endsWith("SACRM"))
      {
        stmt.setString(4, "1");
        stmt.setString(5, pb.getFormulaId());
        stmt.setString(6, pb.getOptivaParam());
        stmt.setString(7, "1");
        stmt.setString(8, pb.getCost());
      }
      else
      {
        stmt.setString(4, pb.getFormulaId());
        stmt.setString(5, pb.getOptivaParam());
        stmt.setString(6, "0");
        stmt.setString(7, pb.getCost());
      }

      stmt.executeUpdate();
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

  private void updateFsiItemClass()
  {
    CallableStatement cstmt = null;
    try
    {
      String procedure = "{call fsi_item_class_update(?)}";
      cstmt = optivaConn.prepareCall(procedure);
      cstmt.registerOutParameter(1, Types.VARCHAR);
      cstmt.execute();
      log4jLogger.info("Updating fsi item update class for " + ConnectionUtility.buildDatabaseName(optivaConn));
      String v_str_out = cstmt.getString(1);
      if (v_str_out != null)
        log4jLogger.info("Stored procedure call in updateFsiItemClass(): " + v_str_out);
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
}
