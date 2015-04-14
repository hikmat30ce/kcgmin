 package com.valspar.interfaces.regulatory.dotcalc.program;

import com.valspar.interfaces.common.*;
import com.valspar.interfaces.common.beans.ConnectionAccessBean;
import com.valspar.interfaces.common.enums.DataSource;
import com.valspar.interfaces.common.utils.*;
import com.valspar.interfaces.regulatory.dotcalc.beans.ProductBean;
import java.math.BigDecimal;
import java.sql.*;
import java.util.*;
import oracle.jdbc.*;
import oracle.sql.*;
import org.apache.log4j.Logger;

public class DotCalcInterface extends BaseInterface
{
  private static Logger log4jLogger = Logger.getLogger(DotCalcInterface.class);

  public DotCalcInterface()
  {
  }
 
  public void execute()
  {
    String inProductGroup = getParameterValue("inProductGroup");
    String returnString = null;
    ArrayList productList = new ArrayList();
    OracleConnection regulatoryConn = null;

    try
    {
      regulatoryConn = (OracleConnection) ConnectionAccessBean.getConnection(DataSource.REGULATORY);
      buildProductList(inProductGroup, productList, regulatoryConn);

      if (!productList.isEmpty())
      {
        Iterator productIterator = productList.iterator();
        while (productIterator.hasNext())
        {
          String product = (String) productIterator.next();
          ProductBean pb = new ProductBean();
          pb.setProduct(product);
          getFormulaClass(pb, regulatoryConn);

          if (pb.getFormulaClass() != null && !pb.getFormulaClass().equalsIgnoreCase(Constants.A))
          {
            log4jLogger.info("Starting the DotCalcInterface for product " + product);
            deleteData(pb, regulatoryConn);
            deleteText(pb, regulatoryConn);
            getDotRules(pb, regulatoryConn);
            calcNewCorrPct(pb, regulatoryConn);
            processVaDotRules(pb, regulatoryConn, Constants.LAND);
            processVaDotRules(pb, regulatoryConn, Constants.NONLAND);
            processVaDotRules(pb, regulatoryConn, Constants.EURO);
            getHazardClasses(pb, regulatoryConn);
            getEuHazardClasses(pb, regulatoryConn);
            getNosComponents(pb, regulatoryConn);
            getEuNosComponents(pb, regulatoryConn);
            getMarinePollutants(pb, regulatoryConn);
            getDotDetails(pb, regulatoryConn, Constants.USA);
            getDotDetails(pb, regulatoryConn, Constants.EURO);
            insertData(pb, regulatoryConn);
            if ((pb.getCorrosiveReview() != null) && (pb.getCorrosiveReview().equalsIgnoreCase("Y")) && (!("YES".equalsIgnoreCase(pb.getInactive()))) && (!("B".equalsIgnoreCase(pb.getFormulaClass()))))
              if (pb.getCorrosiveReview() != null && pb.getCorrosiveReview().equalsIgnoreCase(Constants.Y))
                insertText(pb, regulatoryConn, Constants.CORREV03, Constants.CORREV);
            if ((pb.getData(Constants.TRHAZ) != null && pb.getData(Constants.TRHAZ).toString().equalsIgnoreCase(Constants.EIGHT)) || (pb.getData(Constants.TRSUB) != null && pb.getData(Constants.TRSUB).toString().equalsIgnoreCase(Constants.EIGHT)))
              insertText(pb, regulatoryConn, Constants.WHMCLS13, Constants.WHMCLS);
            addToDotQueue(product, regulatoryConn);
            log4jLogger.info("DotCalcInterface has completed for product " + product);

          }
          else
          {
            log4jLogger.error("DotCalcInterface did not process for product " + product + " because it's formula class is " + pb.getFormulaClass());
          }
        }
      }
      else
      {
        log4jLogger.error("Error in runDotCalcInterface(): There are no products in T_PROD_GROUPING WHERE F_PRODUCT_GROUP = " + inProductGroup);
        returnString = "Error in runDotCalcInterface(): There are no products in T_PROD_GROUPING WHERE F_PRODUCT_GROUP = " + inProductGroup;
      }
    }
    catch (Exception e)
    {
      log4jLogger.error("product group " + inProductGroup, e);
      returnString = "Error in DotCalcInterface() for product group " + inProductGroup + ": " + e;
    }
    finally
    {
      JDBCUtil.close(regulatoryConn);
    }

    //return returnString;
  } 

  public void buildProductList(String inProductGroup, ArrayList productList, OracleConnection conn)
  {
    Statement stmt = null;
    ResultSet rs = null;
    try
    {
      StringBuffer sql = new StringBuffer();
      sql.append("SELECT F_PRODUCT FROM T_PROD_GROUPING WHERE F_PRODUCT_GROUP = '");
      sql.append(inProductGroup);
      sql.append("'");
      stmt = conn.createStatement();
      rs = stmt.executeQuery(sql.toString());
      while (rs.next())
      {
        productList.add(rs.getString(1));
      }
    }
    catch (Exception e)
    {
      log4jLogger.error("product group " + inProductGroup, e);
    }
    finally
    {
      JDBCUtil.close(stmt, rs);
    }
  }

  public void getFormulaClass(ProductBean pb, OracleConnection conn)
  {
    Statement stmt = null;
    ResultSet rs = null;
    try
    {
      StringBuffer sql = new StringBuffer();
      sql.append("SELECT GET_WERCS_DATA('");
      sql.append(pb.getProduct());
      sql.append("', 'FRMCLS') FROM DUAL");
      stmt = conn.createStatement();
      rs = stmt.executeQuery(sql.toString());
      if (rs.next())
      {
        pb.setFormulaClass(rs.getString(1));
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

  public void getInactive(ProductBean pb, OracleConnection conn)
  {
    Statement stmt = null;
    ResultSet rs = null;
    try
    {
      StringBuffer sql = new StringBuffer();
      sql.append("SELECT GET_WERCS_DATA('");
      sql.append(pb.getProduct());
      sql.append("', 'INACTV') FROM DUAL");
      stmt = conn.createStatement();
      rs = stmt.executeQuery(sql.toString());
      if (rs.next())
      {
        pb.setInactive(rs.getString(1));
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

  public void calcNewCorrPct(ProductBean pb, OracleConnection conn)
  {
    String newCorrPct = getNewCorrPct(pb.getProduct(), conn);
    if (newCorrPct != null && newCorrPct != "")
    {
      PreparedStatement pstmt = null;
      try
      {
        StringBuffer sb = new StringBuffer();

        sb.append("MERGE INTO t_prod_data d ");
        sb.append("   USING (SELECT ? AS f_product ");
        sb.append("            FROM DUAL) b ");
        sb.append("   ON (d.f_data_code = 'CORRPCT' AND d.f_product = b.f_product) ");
        sb.append("   WHEN MATCHED THEN ");
        sb.append("      UPDATE ");
        sb.append("         SET f_data = ?, f_user_updated = 'DOTCALC', ");
        sb.append("             f_date_stamp = SYSDATE ");
        sb.append("   WHEN NOT MATCHED THEN ");
        sb.append("      INSERT (f_product, f_data_code, f_data, f_user_updated, f_date_stamp) ");
        sb.append("      VALUES (?, 'CORRPCT', ?, 'DOTCALC', SYSDATE) ");

        pstmt = conn.prepareStatement(sb.toString());
        pstmt.setString(1, pb.getProduct());
        pstmt.setString(2, newCorrPct);
        pstmt.setString(3, pb.getProduct());
        pstmt.setString(4, newCorrPct);
        pstmt.executeUpdate();
      }
      catch (Exception e)
      {
        log4jLogger.error(e);
      }
      finally
      {
        JDBCUtil.close(pstmt);
      }
    }
  }

  public String getNewCorrPct(String product, OracleConnection conn)
  {
    String returnValue = "";
    Statement stmt = null;
    ResultSet rs = null;
    try
    {
      StringBuffer sql = new StringBuffer();

      sql.append("SELECT   ROUND(SUM (c.f_percent * NVL ((SELECT f_data ");
      sql.append("                        FROM t_comp_data ");
      sql.append("                       WHERE f_data_code = 'CORRRNK' ");
      sql.append("                         AND f_component_id = c.f_component_id),2)) / 5,2) AS new_corrpct ");
      sql.append("  FROM t_prod_comp c, t_comp_text ct ");
      sql.append(" WHERE c.f_component_id = ct.f_component_id ");
      sql.append("   AND c.f_product = '");
      sql.append(product);
      sql.append("'   AND ct.f_data_code = 'SIGNAL' ");
      sql.append("   AND ct.f_text_code = 'SIGNAL11' ");

      stmt = conn.createStatement();
      rs = stmt.executeQuery(sql.toString());

      if (rs.next())
      {
        returnValue = rs.getString(1);
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

    return returnValue;
  }

  public void getDotRules(ProductBean pb, OracleConnection conn)
  {
    Statement stmt = null;
    ResultSet rs = null;
    try
    {
      StringBuffer sql = new StringBuffer();
      sql.append("Select f_text_code from T_PROD_TEXT WHERE F_PRODUCT = '");
      sql.append(pb.getProduct());
      sql.append("' AND F_DATA_CODE = 'DOTRUL'");
      stmt = conn.createStatement();
      rs = stmt.executeQuery(sql.toString());
      while (rs.next())
      {
        pb.getDotRuleList().add(rs.getString(1));
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

  public void deleteData(ProductBean pb, OracleConnection conn)
  {
    Statement stmt = null;
    try
    {
      StringBuffer sql = new StringBuffer();
      sql.append("DELETE FROM T_PROD_DATA WHERE F_PRODUCT = '");
      sql.append(pb.getProduct());
      sql.append("' AND F_DATA_CODE LIKE 'TR%' ");
      sql.append("  AND F_DATA_CODE <> 'TRMPOL' ");
      stmt = conn.createStatement();
      stmt.executeUpdate(sql.toString());
      log4jLogger.info("Deleted old TR datacodes for product " + pb.getProduct());
    }
    catch (Exception e)
    {
      log4jLogger.error("product " + pb.getProduct(), e);
    }
    finally
    {
      JDBCUtil.close(stmt);
    }
  }

  public void deleteText(ProductBean pb, OracleConnection conn)
  {
    Statement stmt = null;
    try
    {
      StringBuffer sql = new StringBuffer();
      sql.append("DELETE FROM T_PROD_TEXT WHERE F_PRODUCT = '");
      sql.append(pb.getProduct());
      sql.append("' AND F_TEXT_CODE IN ");
      sql.append("('WHMCLS13', 'CORREV03')");
      stmt = conn.createStatement();
      stmt.executeUpdate(sql.toString());
      log4jLogger.info("Deleted old textcodes for product " + pb.getProduct());
    }
    catch (Exception e)
    {
      log4jLogger.error("product " + pb.getProduct(), e);
    }
    finally
    {
      JDBCUtil.close(stmt);
    }
  }

  public void processVaDotRules(ProductBean pb, OracleConnection conn, String type)
  {
    Statement stmt = null;
    ResultSet rs = null;
    try
    {
      boolean noMatch = true;
      StringBuffer sql = new StringBuffer();
      sql.append("SELECT A.F_RULE_NO, A.F_BULK_UNNO, A.F_NON_BULK_UNNO, A.F_BULK_PKG_GROUP, ");
      sql.append("A.F_EXCEPTION_CONTAINS, A.F_EXCEPTION, A.F_CORREV ");
      sql.append("FROM VA_DOT_RULES A, VA_DOT_CATEGORY B ");
      sql.append("WHERE A.F_ID = B.F_ID ");
      sql.append("AND   B.F_TEXT_KEY = GET_WERCS_DATA('");
      sql.append(pb.getProduct());
      sql.append("', 'FRMCLS') AND   A.F_TYPE = '");
      sql.append(type);
      sql.append("' AND   GET_WERCS_DATA('");
      sql.append(pb.getProduct());
      sql.append("', 'FLASHPT') BETWEEN A.F_FLASHPOINT_MIN AND A.F_FLASHPOINT_MAX ");
      sql.append("AND   GET_WERCS_DATA('");
      sql.append(pb.getProduct());
      sql.append("', 'CORRPCT') BETWEEN A.F_CORROSIVE_MIN AND A.F_CORROSIVE_MAX ");
      sql.append("AND   GET_WERCS_DATA('");
      sql.append(pb.getProduct());
      sql.append("', 'NVMPCT') BETWEEN A.F_NVM_MIN AND A.F_NVM_MAX ");
      sql.append("ORDER BY A.F_ID ");
      stmt = conn.createStatement();
      rs = stmt.executeQuery(sql.toString());
      while (rs.next() && noMatch)
      {
        String contains = rs.getString(5);
        String exception = rs.getString(6);
        if ((exception != null && contains != null) && ((pb.getDotRuleList().contains(exception) && contains.equalsIgnoreCase(Constants.FALSE)) || (!(pb.getDotRuleList().contains(exception)) && contains.equalsIgnoreCase(Constants.TRUE))))
        {
          log4jLogger.info("Rule number " + rs.getString(1) + " does not match because product = " + pb.getProduct() + ", F_EXCEPTION_CONTAINS = " + contains + ", F_EXCEPTION = " + exception);
        }
        else
        {
          // We have a match, so set noMatch equal to false
          noMatch = false;
          if (type.equalsIgnoreCase(Constants.LAND))
          {
            pb.setMatchingRuleNumber(rs.getString(1));
            pb.addDataCode(Constants.TRUN, rs.getString(2));
            pb.addDataCode(Constants.TRUNL, stripUnNumber(rs.getString(2)));
            pb.addDataCode(Constants.TRNBUN, rs.getString(3));
            pb.addDataCode(Constants.TRPKG, rs.getString(4));
            pb.setCorrosiveReview(rs.getString(7));
          }
          else if (type.equalsIgnoreCase(Constants.NONLAND))
          {
            pb.addDataCode(Constants.TRAUN, rs.getString(2));
            pb.addDataCode(Constants.TRWUN, rs.getString(2));
            pb.addDataCode(Constants.TRUNA, stripUnNumber(rs.getString(2)));
            pb.addDataCode(Constants.TRUNW, stripUnNumber(rs.getString(2)));
            pb.addDataCode(Constants.TRAPK, rs.getString(4));
            pb.addDataCode(Constants.TRWPK, rs.getString(4));
          }
          else if (type.equalsIgnoreCase(Constants.EURO))
          {
            //pb.addDataCode(TRUNUK, rs.getString(2));
            pb.setEuMatchingRuleNumber(rs.getString(1));
            pb.addDataCode(Constants.TRIAUN, stripUnNumber(rs.getString(2)));
            pb.addDataCode(Constants.TRIMUN, stripUnNumber(rs.getString(2)));
            pb.addDataCode(Constants.TRADUN, stripUnNumber(rs.getString(2)));
            pb.addDataCode(Constants.TRIAPG, rs.getString(4));
            pb.addDataCode(Constants.TRIMPG, rs.getString(4));
            pb.addDataCode(Constants.TRADPCK, rs.getString(4));
            pb.setCorrosiveReview(rs.getString(7));
          }
        }
      }
      log4jLogger.info("processVaDotRules() complete for product " + pb.getProduct() + ", type = " + type);
    }
    catch (Exception e)
    {
      log4jLogger.error("product " + pb.getProduct(), e);
    }
    finally
    {
      JDBCUtil.close(stmt, rs);
    }
  }

  public void getHazardClasses(ProductBean pb, OracleConnection conn)
  {
    Statement stmt = null;
    ResultSet rs = null;
    try
    {
      StringBuffer sql = new StringBuffer();
      sql.append("SELECT F_HAZARD_CLASS FROM VA_DOT_HAZCLASS WHERE F_ID = ");
      sql.append(pb.getMatchingRuleNumber());
      sql.append(" ORDER BY to_number(F_HAZARD_CLASS) ");
      stmt = conn.createStatement();
      rs = stmt.executeQuery(sql.toString());
      while (rs.next())
      {
        if (pb.getFirstHazardClass() == null)
          pb.setFirstHazardClass(rs.getString(1));
        else if (pb.getSecondHazardClass() == null)
          pb.setSecondHazardClass(rs.getString(1));
      }
      log4jLogger.info("getHazardClasses() complete for product " + pb.getProduct());
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

  public void getEuHazardClasses(ProductBean pb, OracleConnection conn)
  {
    Statement stmt = null;
    ResultSet rs = null;
    try
    {
      StringBuffer sql = new StringBuffer();
      sql.append("SELECT F_HAZARD_CLASS FROM VA_DOT_HAZCLASS WHERE F_ID = ");
      sql.append(pb.getEuMatchingRuleNumber());
      sql.append(" ORDER BY to_number(F_HAZARD_CLASS) ");
      stmt = conn.createStatement();
      rs = stmt.executeQuery(sql.toString());
      while (rs.next())
      {
        if (pb.getEuFirstHazardClass() == null)
          pb.setEuFirstHazardClass(rs.getString(1));
        else if (pb.getEuSecondHazardClass() == null)
          pb.setEuSecondHazardClass(rs.getString(1));
      }
      log4jLogger.info("getEuHazardClasses() complete for product " + pb.getProduct());
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

  public void getNosComponents(ProductBean pb, OracleConnection conn)
  {
    if (pb.getData(Constants.TRAUN) == null || pb.getData(Constants.TRAUN).equals("NA1993CO") || pb.getData(Constants.TRAUN).equals("UN1760") || pb.getData(Constants.TRAUN).equals("UN1993") || pb.getData(Constants.TRAUN).equals("UN2920") || pb.getData(Constants.TRAUN).equals("UN2924") || pb.getData(Constants.TRAUN).equals("UN3082") || pb.getData(Constants.TRAUN).equals("UN3264") || pb.getData(Constants.TRAUN).equals("UN3266"))
    {
      PreparedStatement hazClassThreePstmt = null;
      ResultSet hazClassThreeRs = null;
      PreparedStatement hazClassEightPstmt = null;
      ResultSet hazClassEightRs = null;
      PreparedStatement multHazClassPstmt = null;
      ResultSet multHazClassRs = null;
      try
      {
        StringBuffer hazClassThreeSql = new StringBuffer();
        hazClassThreeSql.append("SELECT a.f_chem_name ");
        hazClassThreeSql.append("FROM t_prod_comp a, t_comp_data b, t_comp_data c ");
        hazClassThreeSql.append("WHERE a.f_component_id = b.f_component_id ");
        hazClassThreeSql.append("and a.f_cas_number = b.f_cas_number ");
        hazClassThreeSql.append("and a.f_component_id = c.f_component_id ");
        hazClassThreeSql.append("and a.f_cas_number = c.f_cas_number ");
        hazClassThreeSql.append("and b.f_data_code = 'CTRHAZ' ");
        hazClassThreeSql.append("and b.f_data = ? ");
        hazClassThreeSql.append("and c.f_data_code = 'CFLASH' ");
        hazClassThreeSql.append("AND a.f_product = ? ");
        hazClassThreeSql.append("and to_number(c.f_data) < 141 ");
        hazClassThreeSql.append("and to_number(a.f_percent) > 1 ");
        hazClassThreeSql.append("order by to_number(c.f_data) ");

        StringBuffer hazClassEightSql = new StringBuffer();
        hazClassEightSql.append("SELECT a.f_chem_name ");
        hazClassEightSql.append("FROM t_prod_comp a, t_comp_data b ");
        hazClassEightSql.append("WHERE a.f_component_id = b.f_component_id ");
        hazClassEightSql.append("and a.f_cas_number = b.f_cas_number ");
        hazClassEightSql.append("and b.f_data_code = 'CORRRNK' ");
        hazClassEightSql.append("AND a.f_product = ? ");
        hazClassEightSql.append("and to_number(a.f_percent) >= .95 ");
        hazClassEightSql.append("and to_number(b.f_data) > 0 ");
        hazClassEightSql.append("order by to_number(b.f_data) desc ");

        StringBuffer multHazClassSql = new StringBuffer();
        multHazClassSql.append("SELECT a.f_chem_name ");
        multHazClassSql.append("FROM t_prod_comp a, t_comp_data b, t_comp_data c ");
        multHazClassSql.append("WHERE a.f_component_id = b.f_component_id ");
        multHazClassSql.append("and a.f_cas_number = b.f_cas_number ");
        multHazClassSql.append("and a.f_component_id = c.f_component_id ");
        multHazClassSql.append("and a.f_cas_number = c.f_cas_number ");
        multHazClassSql.append("and b.f_data_code = 'CTRHAZ' ");
        multHazClassSql.append("and b.f_data = ? ");
        multHazClassSql.append("and c.f_data_code = 'CFLASH' ");
        multHazClassSql.append("AND a.f_product = ? ");
        multHazClassSql.append("and to_number(c.f_data) < 141 ");
        multHazClassSql.append("and to_number(a.f_percent) >= .95 ");
        multHazClassSql.append("order by to_number(c.f_data) ");

        if (pb.getFirstHazardClass() != null && pb.getSecondHazardClass() == null)
        {
          if (pb.getFirstHazardClass().equalsIgnoreCase(Constants.THREE))
          {
            hazClassThreePstmt = conn.prepareStatement(hazClassThreeSql.toString());
            hazClassThreePstmt.setString(1, pb.getFirstHazardClass());
            hazClassThreePstmt.setString(2, pb.getProduct());
            hazClassThreeRs = hazClassThreePstmt.executeQuery();
            while (hazClassThreeRs.next())
            {
              if (pb.getData(Constants.TRLNG1) == null)
                pb.addDataCode(Constants.TRLNG1, hazClassThreeRs.getString(1));
              else if (pb.getData(Constants.TRLNG2) == null)
                pb.addDataCode(Constants.TRLNG2, hazClassThreeRs.getString(1));
              if (pb.getData(Constants.TRANG1) == null)
                pb.addDataCode(Constants.TRANG1, hazClassThreeRs.getString(1));
              else if (pb.getData(Constants.TRANG2) == null)
                pb.addDataCode(Constants.TRANG2, hazClassThreeRs.getString(1));
              if (pb.getData(Constants.TRWNG1) == null)
                pb.addDataCode(Constants.TRWNG1, hazClassThreeRs.getString(1));
              else if (pb.getData(Constants.TRWNG2) == null)
                pb.addDataCode(Constants.TRWNG2, hazClassThreeRs.getString(1));
            }
          }
          else if (pb.getFirstHazardClass().equalsIgnoreCase(Constants.EIGHT))
          {
            hazClassEightPstmt = conn.prepareStatement(hazClassEightSql.toString());
            hazClassEightPstmt.setString(1, pb.getProduct());
            hazClassEightRs = hazClassEightPstmt.executeQuery();
            while (hazClassEightRs.next())
            {
              if (pb.getData(Constants.TRLNG1) == null)
                pb.addDataCode(Constants.TRLNG1, hazClassEightRs.getString(1));
              else if (pb.getData(Constants.TRLNG2) == null)
                pb.addDataCode(Constants.TRLNG2, hazClassEightRs.getString(1));
              if (pb.getData(Constants.TRANG1) == null)
                pb.addDataCode(Constants.TRANG1, hazClassEightRs.getString(1));
              else if (pb.getData(Constants.TRANG2) == null)
                pb.addDataCode(Constants.TRANG2, hazClassEightRs.getString(1));
              if (pb.getData(Constants.TRWNG1) == null)
                pb.addDataCode(Constants.TRWNG1, hazClassEightRs.getString(1));
              else if (pb.getData(Constants.TRWNG2) == null)
                pb.addDataCode(Constants.TRWNG2, hazClassEightRs.getString(1));
            }
          }
        }
        else if (pb.getFirstHazardClass() != null && pb.getSecondHazardClass() != null)
        {
          // Process First Hazardous Chemical
          multHazClassPstmt = conn.prepareStatement(multHazClassSql.toString());
          multHazClassPstmt.setString(1, pb.getFirstHazardClass());
          multHazClassPstmt.setString(2, pb.getProduct());
          multHazClassRs = multHazClassPstmt.executeQuery();
          if (multHazClassRs.next())
          {
            if (pb.getData(Constants.TRLNG1) == null)
              pb.addDataCode(Constants.TRLNG1, multHazClassRs.getString(1));
            if (pb.getData(Constants.TRANG1) == null)
              pb.addDataCode(Constants.TRANG1, multHazClassRs.getString(1));
            if (pb.getData(Constants.TRWNG1) == null)
              pb.addDataCode(Constants.TRWNG1, multHazClassRs.getString(1));
          }
          // Process Second Hazardous Chemical
          hazClassEightPstmt = conn.prepareStatement(hazClassEightSql.toString());
          hazClassEightPstmt.setString(1, pb.getProduct());
          hazClassEightRs = hazClassEightPstmt.executeQuery();
          if (hazClassEightRs.next())
          {
            if (pb.getData(Constants.TRLNG2) == null)
              pb.addDataCode(Constants.TRLNG2, hazClassEightRs.getString(1));
            if (pb.getData(Constants.TRANG2) == null)
              pb.addDataCode(Constants.TRANG2, hazClassEightRs.getString(1));
            if (pb.getData(Constants.TRWNG2) == null)
              pb.addDataCode(Constants.TRWNG2, hazClassEightRs.getString(1));
          }
        }
        log4jLogger.info("getNosComponents() complete for product " + pb.getProduct());
      }
      catch (Exception e)
      {
        log4jLogger.error(e);
      }
      finally
      {
        JDBCUtil.close(hazClassThreePstmt, hazClassThreeRs);
        JDBCUtil.close(hazClassEightPstmt, hazClassEightRs);
        JDBCUtil.close(multHazClassPstmt, multHazClassRs);
      }
    }
    else
    {
      log4jLogger.info("getNosComponents() skipped for product " + pb.getProduct() + ", un number " + pb.getData(Constants.TRAUN));
    }
  }

  public void getEuNosComponents(ProductBean pb, OracleConnection conn)
  {
    if (pb.getData(Constants.TRADUN) == null || pb.getData(Constants.TRADUN).equals("NA1993CO") || pb.getData(Constants.TRADUN).equals("UN1760") || pb.getData(Constants.TRADUN).equals("UN1993") || pb.getData(Constants.TRADUN).equals("UN2920") || pb.getData(Constants.TRADUN).equals("UN2924") || pb.getData(Constants.TRADUN).equals("UN3082") || pb.getData(Constants.TRADUN).equals("UN3264") || pb.getData(Constants.TRADUN).equals("UN3266"))
    {
      PreparedStatement hazClassNinePstmt = null;
      ResultSet hazClassNineRs = null;
      try
      {
        StringBuffer hazClassNineSql = new StringBuffer();
        hazClassNineSql.append("select eecclas, f_chem_name, f_percent from ");
        hazClassNineSql.append("(select '50' as eecclas, c.f_chem_name, c.f_percent ");
        hazClassNineSql.append("from t_prod_text t, t_prod_comp c, t_comp_data a ");
        hazClassNineSql.append("where t.f_product = c.f_product ");
        hazClassNineSql.append("and a.f_component_id = c.f_component_id ");
        hazClassNineSql.append("and f_text_code IN ('RISK0050','RISK0051','RISKC066','RISKC067') ");
        hazClassNineSql.append("and a.f_data_code = 'EECCLAS' ");
        hazClassNineSql.append("and a.f_data like '%R50%' ");
        hazClassNineSql.append("and c.f_product = ? ");
        hazClassNineSql.append("and c.f_percent > 1 ");
        hazClassNineSql.append("order by c.f_percent desc) ");
        hazClassNineSql.append("where rownum < 3 ");
        hazClassNineSql.append("union ");
        hazClassNineSql.append("select eecclas, f_chem_name, f_percent from ");
        hazClassNineSql.append("(select '51' as eecclas, c.f_chem_name, c.f_percent ");
        hazClassNineSql.append("from t_prod_text t, t_prod_comp c, t_comp_data a ");
        hazClassNineSql.append("where t.f_product = c.f_product ");
        hazClassNineSql.append("and a.f_component_id = c.f_component_id ");
        hazClassNineSql.append("and f_text_code IN ('RISK0050','RISK0051','RISKC066','RISKC067') ");
        hazClassNineSql.append("and a.f_data_code = 'EECCLAS' ");
        hazClassNineSql.append("and a.f_data like '%R51%' ");
        hazClassNineSql.append("and c.f_product = ? ");
        hazClassNineSql.append("and c.f_percent > 1 ");
        hazClassNineSql.append("order by f_percent desc) ");
        hazClassNineSql.append("where rownum < 3 ");
        hazClassNineSql.append("order by 1, f_percent desc ");

        if (pb.getEuFirstHazardClass() != null && pb.getEuFirstHazardClass().equalsIgnoreCase(Constants.NINE))
        {
          hazClassNinePstmt = conn.prepareStatement(hazClassNineSql.toString());
          hazClassNinePstmt.setString(1, pb.getProduct());
          hazClassNinePstmt.setString(2, pb.getProduct());
          hazClassNineRs = hazClassNinePstmt.executeQuery();
          String eecclas50one = "";
          String eecclas50two = "";
          String eecclas51one = "";
          String eecclas51two = "";

          while (hazClassNineRs.next())
          {
            BigDecimal hazPct = new BigDecimal(hazClassNineRs.getString(3));
            if (hazClassNineRs.getString(1).equalsIgnoreCase("50"))
            {
              if (eecclas50one == "")
              {
                eecclas50one = hazClassNineRs.getString(2);
              }
              else
              {
                eecclas50two = hazClassNineRs.getString(2);
              }
            }
            else
            {
              if (eecclas51one == "" & hazPct.compareTo(new BigDecimal("15")) == 1)
              {
                eecclas51one = hazClassNineRs.getString(2);
              }
              else if (eecclas51two == "")
              {
                eecclas51two = hazClassNineRs.getString(2);
              }
            }
          }

          if (eecclas50one != "")
          {
            if (pb.getData(Constants.TRADEH1) == null)
            {
              pb.addDataCode(Constants.TRADEH1, eecclas50one);
              pb.addDataCode(Constants.TRIAEH1, eecclas50one);
              pb.addDataCode(Constants.TRIMEH1, eecclas50one);
            }
            else if (pb.getData(Constants.TRADEH2) == null)
            {
              pb.addDataCode(Constants.TRADEH2, eecclas50one);
              pb.addDataCode(Constants.TRIAEH2, eecclas50one);
              pb.addDataCode(Constants.TRIMEH2, eecclas50one);
            }
          }
          if (eecclas51one != "")
          {
            if (pb.getData(Constants.TRADEH1) == null)
            {
              pb.addDataCode(Constants.TRADEH1, eecclas51one);
              pb.addDataCode(Constants.TRIAEH1, eecclas51one);
              pb.addDataCode(Constants.TRIMEH1, eecclas51one);
            }
            else if (pb.getData(Constants.TRADEH2) == null)
            {
              pb.addDataCode(Constants.TRADEH2, eecclas51one);
              pb.addDataCode(Constants.TRIAEH2, eecclas51one);
              pb.addDataCode(Constants.TRIMEH2, eecclas51one);
            }
          }
          if (eecclas50two != "")
          {
            if (pb.getData(Constants.TRADEH1) == null)
            {
              pb.addDataCode(Constants.TRADEH1, eecclas50two);
              pb.addDataCode(Constants.TRIAEH1, eecclas50two);
              pb.addDataCode(Constants.TRIMEH1, eecclas50two);
            }
            else if (pb.getData(Constants.TRADEH2) == null)
            {
              pb.addDataCode(Constants.TRADEH2, eecclas50two);
              pb.addDataCode(Constants.TRIAEH2, eecclas50two);
              pb.addDataCode(Constants.TRIMEH2, eecclas50two);
            }
          }
          if (eecclas51two != "")
          {
            if (pb.getData(Constants.TRADEH1) == null)
            {
              pb.addDataCode(Constants.TRADEH1, eecclas51two);
              pb.addDataCode(Constants.TRIAEH1, eecclas51two);
              pb.addDataCode(Constants.TRIMEH1, eecclas51two);
            }
            else if (pb.getData(Constants.TRADEH2) == null)
            {
              pb.addDataCode(Constants.TRADEH2, eecclas51two);
              pb.addDataCode(Constants.TRIAEH2, eecclas51two);
              pb.addDataCode(Constants.TRIMEH2, eecclas51two);
            }
          }
        }
        log4jLogger.info("getEuNosComponents() complete for product " + pb.getProduct());
      }
      catch (Exception e)
      {
        log4jLogger.error(e);
      }
      finally
      {
        JDBCUtil.close(hazClassNinePstmt, hazClassNineRs);
      }
    }
    else
    {
      log4jLogger.info("getEuNosComponents() skipped for product " + pb.getProduct() + ", un number " + pb.getData(Constants.TRADUN));
    }
  }

  public void getMarinePollutants(ProductBean pb, OracleConnection conn)
  {
    PreparedStatement pstmt = null;
    ResultSet rs = null;
    try
    {
      StringBuffer sql = new StringBuffer();
      sql.append("select eecclas, f_chem_name, f_percent from ");
      sql.append("(select '50' as eecclas, c.f_chem_name, c.f_percent ");
      sql.append("from t_prod_text t, t_prod_comp c, t_comp_data a, t_prod_data d ");
      sql.append("where t.f_product = c.f_product ");
      sql.append("and t.f_product = d.f_product and d.f_data_code = 'TRMPOL' ");
      sql.append("and a.f_component_id = c.f_component_id ");
      sql.append("and f_text_code IN ('RISK0050','RISK0051','RISKC066','RISKC067') ");
      sql.append("and a.f_data_code = 'EECCLAS' ");
      sql.append("and a.f_data like '%R50%' ");
      sql.append("and upper(c.f_chem_name) NOT like '%AMMONI%' ");
      sql.append("and c.f_product = ? ");
      sql.append("and c.f_percent > 1 ");
      sql.append("order by c.f_percent desc) ");
      sql.append("where rownum < 3 ");
      sql.append("union ");
      sql.append("select eecclas, f_chem_name, f_percent from ");
      sql.append("(select '51' as eecclas, c.f_chem_name, c.f_percent ");
      sql.append("from t_prod_text t, t_prod_comp c, t_comp_data a, t_prod_data d ");
      sql.append("where t.f_product = c.f_product ");
      sql.append("and t.f_product = d.f_product and d.f_data_code = 'TRMPOL' ");
      sql.append("and a.f_component_id = c.f_component_id ");
      sql.append("and f_text_code IN ('RISK0050','RISK0051','RISKC066','RISKC067') ");
      sql.append("and a.f_data_code = 'EECCLAS' ");
      sql.append("and a.f_data like '%R51%' ");
      sql.append("and upper(c.f_chem_name) NOT like '%AMMONI%' ");
      sql.append("and c.f_product = ? ");
      sql.append("and c.f_percent > 1 ");
      sql.append("order by f_percent desc) ");
      sql.append("where rownum < 3 ");
      sql.append("order by 1, f_percent desc ");

      pstmt = conn.prepareStatement(sql.toString());
      pstmt.setString(1, pb.getProduct());
      pstmt.setString(2, pb.getProduct());
      rs = pstmt.executeQuery();
      String eecclas50one = "";
      String eecclas50two = "";
      String eecclas51one = "";
      String eecclas51two = "";

      while (rs.next())
      {
        BigDecimal hazPct = new BigDecimal(rs.getString(3));
        if (rs.getString(1).equalsIgnoreCase("50"))
        {
          if (eecclas50one == "")
          {
            eecclas50one = rs.getString(2);
          }
          else
          {
            eecclas50two = rs.getString(2);
          }
        }
        else
        {
          if (eecclas51one == "" & hazPct.compareTo(new BigDecimal("15")) == 1)
          {
            eecclas51one = rs.getString(2);
          }
          else if (eecclas51two == "")
          {
            eecclas51two = rs.getString(2);
          }
        }
      }

      if (eecclas50one != "")
      {
        if (pb.getData(Constants.TRMING) == null)
        {
          pb.addDataCode(Constants.TRMING, eecclas50one);
        }
        else if (pb.getData(Constants.TRMING2) == null)
        {
          pb.addDataCode(Constants.TRMING2, eecclas50one);
        }
      }
      if (eecclas51one != "")
      {
        if (pb.getData(Constants.TRMING) == null)
        {
          pb.addDataCode(Constants.TRMING, eecclas51one);
        }
        else if (pb.getData(Constants.TRMING2) == null)
        {
          pb.addDataCode(Constants.TRMING2, eecclas51one);
        }
      }
      if (eecclas50two != "")
      {
        if (pb.getData(Constants.TRMING) == null)
        {
          pb.addDataCode(Constants.TRMING, eecclas50two);
        }
        else if (pb.getData(Constants.TRMING2) == null)
        {
          pb.addDataCode(Constants.TRMING2, eecclas50two);
        }
      }
      if (eecclas51two != "")
      {
        if (pb.getData(Constants.TRMING) == null)
        {
          pb.addDataCode(Constants.TRMING, eecclas51two);
        }
        else if (pb.getData(Constants.TRMING2) == null)
        {
          pb.addDataCode(Constants.TRMING2, eecclas51two);
        }
      }
      log4jLogger.info("getMarinePollutants() complete for product " + pb.getProduct());
    }
    catch (Exception e)
    {
      log4jLogger.error(e);
    }
    finally
    {
      JDBCUtil.close(pstmt, rs);
    }
  }

  public void getDotDetails(ProductBean pb, OracleConnection conn, String setCode)
  {
    Statement stmt = null;
    ResultSet rs = null;
    try
    {
      StringBuffer sql = new StringBuffer();
      sql.append("SELECT A.F_DATA_CODE, (SELECT SHIPPING_NAME FROM VA_DOTS_MST WHERE UN_NUMBER = '");
      sql.append(getUnNumber(pb, setCode, Constants.LAND));
      sql.append("' AND SHIP_MTHD = A.SHIP_MTHD AND LANGUAGE = 'EN') FROM VA_DOTS_MAPPING A WHERE A.FRSETCD = '");
      sql.append(setCode);
      sql.append("' AND A.CODE_TYPE = 'SHIPPING_NAME' AND A.SHIP_MTHD = 'LAND' ");
      sql.append("UNION SELECT A.F_DATA_CODE, (SELECT HAZARD_CLASS FROM VA_DOTS_MST WHERE UN_NUMBER = '");
      sql.append(getUnNumber(pb, setCode, Constants.LAND));
      sql.append("' AND SHIP_MTHD = A.SHIP_MTHD AND LANGUAGE = 'EN') FROM VA_DOTS_MAPPING A WHERE A.FRSETCD = '");
      sql.append(setCode);
      sql.append("' AND A.CODE_TYPE = 'HAZARD_CLASS' AND A.SHIP_MTHD = 'LAND' ");
      sql.append("UNION SELECT A.F_DATA_CODE, (SELECT SUBSIDARY_RISK FROM VA_DOTS_MST WHERE UN_NUMBER = '");
      sql.append(getUnNumber(pb, setCode, Constants.LAND));
      sql.append("' AND SHIP_MTHD = A.SHIP_MTHD AND LANGUAGE = 'EN') FROM VA_DOTS_MAPPING A WHERE A.FRSETCD = '");
      sql.append(setCode);
      sql.append("' AND A.CODE_TYPE = 'SUBSIDARY_RISK' AND A.SHIP_MTHD = 'LAND' ");
      sql.append("UNION SELECT A.F_DATA_CODE, (SELECT SHIPPING_NAME FROM VA_DOTS_MST WHERE UN_NUMBER = '");
      sql.append(getUnNumber(pb, setCode, Constants.AIR));
      sql.append("' AND SHIP_MTHD = A.SHIP_MTHD AND LANGUAGE = 'EN') FROM VA_DOTS_MAPPING A WHERE A.FRSETCD = '");
      sql.append(setCode);
      sql.append("' AND A.CODE_TYPE = 'SHIPPING_NAME' AND A.SHIP_MTHD = 'AIR' ");
      sql.append("UNION SELECT A.F_DATA_CODE, (SELECT HAZARD_CLASS FROM VA_DOTS_MST WHERE UN_NUMBER = '");
      sql.append(getUnNumber(pb, setCode, Constants.AIR));
      sql.append("' AND SHIP_MTHD = A.SHIP_MTHD AND LANGUAGE = 'EN') FROM VA_DOTS_MAPPING A WHERE A.FRSETCD = '");
      sql.append(setCode);
      sql.append("' AND A.CODE_TYPE = 'HAZARD_CLASS' AND A.SHIP_MTHD = 'AIR' ");
      sql.append("UNION SELECT A.F_DATA_CODE, (SELECT SUBSIDARY_RISK FROM VA_DOTS_MST WHERE UN_NUMBER = '");
      sql.append(getUnNumber(pb, setCode, Constants.AIR));
      sql.append("' AND SHIP_MTHD = A.SHIP_MTHD AND LANGUAGE = 'EN') FROM VA_DOTS_MAPPING A WHERE A.FRSETCD = '");
      sql.append(setCode);
      sql.append("' AND A.CODE_TYPE = 'SUBSIDARY_RISK' AND A.SHIP_MTHD = 'AIR' ");
      sql.append("UNION SELECT A.F_DATA_CODE, (SELECT SHIPPING_NAME FROM VA_DOTS_MST WHERE UN_NUMBER = '");
      sql.append(getUnNumber(pb, setCode, Constants.WATR));
      sql.append("' AND SHIP_MTHD = A.SHIP_MTHD AND LANGUAGE = 'EN') FROM VA_DOTS_MAPPING A WHERE A.FRSETCD = '");
      sql.append(setCode);
      sql.append("' AND A.CODE_TYPE = 'SHIPPING_NAME' AND A.SHIP_MTHD = 'WATR' ");
      sql.append("UNION SELECT A.F_DATA_CODE, (SELECT HAZARD_CLASS FROM VA_DOTS_MST WHERE UN_NUMBER = '");
      sql.append(getUnNumber(pb, setCode, Constants.WATR));
      sql.append("' AND SHIP_MTHD = A.SHIP_MTHD AND LANGUAGE = 'EN') FROM VA_DOTS_MAPPING A WHERE A.FRSETCD = '");
      sql.append(setCode);
      sql.append("' AND A.CODE_TYPE = 'HAZARD_CLASS' AND A.SHIP_MTHD = 'WATR' ");
      sql.append("UNION SELECT A.F_DATA_CODE, (SELECT SUBSIDARY_RISK FROM VA_DOTS_MST WHERE UN_NUMBER = '");
      sql.append(getUnNumber(pb, setCode, Constants.WATR));
      sql.append("' AND SHIP_MTHD = A.SHIP_MTHD AND LANGUAGE = 'EN') FROM VA_DOTS_MAPPING A WHERE A.FRSETCD = '");
      sql.append(setCode);
      sql.append("' AND A.CODE_TYPE = 'SUBSIDARY_RISK' AND A.SHIP_MTHD = 'WATR' ");
      stmt = conn.createStatement();
      rs = stmt.executeQuery(sql.toString());
      while (rs.next())
      {
        pb.addDataCode(rs.getString(1), rs.getString(2));
      }
      log4jLogger.info("getDotDetails() complete for product = " + pb.getProduct() + ", setCode = " + setCode);
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

  public void insertData(ProductBean pb, OracleConnection conn)
  {
    OracleCallableStatement cstmt = null;
    try
    {
      ArrayDescriptor arrayDesc = ArrayDescriptor.createDescriptor("ARRAY_OF_MAP_OBJ", conn);
      String[][] objArray = new String[pb.getDataCodes().size()][2];
      objArray = Conversion.hashMapToArray(pb.getDataCodes());
      ARRAY oracleArray = new ARRAY(arrayDesc, conn, objArray);
      cstmt = (OracleCallableStatement) conn.prepareCall("{call WERCS_UPDATE_PRODUCT_DATA(?,?,?,?)}");
      cstmt.setString(1, pb.getProduct());
      cstmt.setString(2, Constants.DOTCALC);
      cstmt.setArray(3, oracleArray);
      cstmt.registerOutParameter(4, Types.VARCHAR);
      cstmt.execute();
      if (cstmt.getString(4) != null)
      {
        log4jLogger.error("WERCS_UPDATE_PRODUCT_DATA() message: " + cstmt.getString(4));
      }
      else
        log4jLogger.info("insertData() complete for product " + pb.getProduct());
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

  public void insertText(ProductBean pb, OracleConnection conn, String textCode, String dataCode)
  {
    CallableStatement cstmt = null;
    try
    {
      cstmt = conn.prepareCall("{call WERCS_UPDATE_PRODUCT_TEXT(?,?,?,?)}");
      cstmt.setString(1, pb.getProduct());
      cstmt.setString(2, textCode);
      cstmt.setString(3, dataCode);
      cstmt.registerOutParameter(4, Types.VARCHAR);
      cstmt.execute();
      if (cstmt.getString(4) != null)
        log4jLogger.error("Error in insertText(): " + cstmt.getString(4));
      else
        log4jLogger.info("insertText() complete for product " + pb.getProduct());
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

  public String getUnNumber(ProductBean pb, String setCode, String shipMthd)
  {
    String fDataCode = null;
    try
    {
      if (setCode.equalsIgnoreCase(Constants.USA))
      {
        if (shipMthd.equalsIgnoreCase(Constants.LAND))
          fDataCode = Constants.TRUN;
        else if (shipMthd.equalsIgnoreCase(Constants.AIR))
          fDataCode = Constants.TRAUN;
        else if (shipMthd.equalsIgnoreCase(Constants.WATR))
          fDataCode = Constants.TRWUN;
      }
      else if (setCode.equalsIgnoreCase(Constants.EURO))
      {
        if (shipMthd.equalsIgnoreCase(Constants.LAND))
          //fDataCode = TRUNUK;
          fDataCode = Constants.TRADUN;
        else if (shipMthd.equalsIgnoreCase(Constants.AIR))
          fDataCode = Constants.TRIAUN;
        else if (shipMthd.equalsIgnoreCase(Constants.WATR))
          fDataCode = Constants.TRIMUN;
      }
    }
    catch (Exception e)
    {
      log4jLogger.error("fDataCode = " + fDataCode, e);
    }
    return pb.getData(fDataCode);
  }

  public String stripUnNumber(String unNumber)
  {
    try
    {
      if (unNumber.length() > 6)
        unNumber = unNumber.substring(0, 6);
    }
    catch (Exception e)
    {
      log4jLogger.error(e);
    }
    return unNumber;
  }

  public void addToDotQueue(String product, OracleConnection conn)
  {
    PreparedStatement stmt = null;
    try
    {
      StringBuffer sql = new StringBuffer();
      sql.append("INSERT INTO VCA_DOT_QUEUE (PRODUCT, ADDED_BY, DATE_ADDED) VALUES (?, 'DOTCALC', SYSDATE)");

      stmt = conn.prepareStatement(sql.toString());
      stmt.setString(1, product);
      stmt.executeUpdate();
      log4jLogger.info("Insert Product to VCA_DOT_QUEUE " + product);
    }
    catch (Exception e)
    {
      log4jLogger.error("product " + product, e);
    }
    finally
    {
      JDBCUtil.close(stmt);
    }
  }
}
