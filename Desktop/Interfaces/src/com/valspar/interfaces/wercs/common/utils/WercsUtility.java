package com.valspar.interfaces.wercs.common.utils;

import com.valspar.interfaces.common.Constants;
import com.valspar.interfaces.common.beans.ConnectionAccessBean;
import com.valspar.interfaces.common.enums.DataSource;
import com.valspar.interfaces.common.utils.CommonUtility;
import com.valspar.interfaces.common.utils.JDBCUtil;
import com.valspar.interfaces.common.utils.ValsparLookUps;
import com.valspar.interfaces.wercs.common.beans.BaseProductBean;
import java.math.BigDecimal;
import java.sql.*;
import java.util.*;
import oracle.jdbc.*;
import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;

public class WercsUtility implements Constants
{
  private static Logger log4jLogger = Logger.getLogger(WercsUtility.class);

  public WercsUtility()
  {
  }

  public static ArrayList<String> getDescriptionLanguages(String region)
  {
    OracleConnection wercsConn = (OracleConnection) ConnectionAccessBean.getConnection(DataSource.WERCS);
    ArrayList<String> returnValue = new ArrayList<String>();
    StringBuilder sql = new StringBuilder();
    Statement stmt = null;
    ResultSet rs = null;
    try
    {
      sql.append("select keyfield3 from vca_lookups ");
      sql.append("WHERE KEYFIELD1  = 'PRODUCT_DESCRIPTION_TRANSLATION' ");
      sql.append("and keyfield2 = ");
      sql.append(CommonUtility.toVarchar(region));

      stmt = wercsConn.createStatement();
      rs = stmt.executeQuery(sql.toString());
      while (rs.next())
      {
        returnValue.add(rs.getString(1));
      }
    }
    catch (Exception e)
    {
      log4jLogger.error(e);
    }
    finally
    {
      JDBCUtil.close(stmt, rs);
      JDBCUtil.close(wercsConn);
    }
    return returnValue;
  }

  public static HashMap getProductImportDataAttributes(BaseProductBean pb)
  {
    OracleConnection optivaConn = (OracleConnection) ConnectionAccessBean.getConnection(DataSource.FORMULATION);
    HashMap hm = new HashMap();
    StringBuilder sql = new StringBuilder();
    Statement stmt = null;
    ResultSet rs = null;
    try
    {
      sql.append("SELECT DISTINCT C.WERCS_DATA_CODE, DECODE(B.PVALUE, 'Y', 'Y', 'N', 'N','0 0','00','YES','YES', ROUND(B.PVALUE, C.PRECISION)) ");
      sql.append("FROM FSFORMULA F, FSFORMULATECHPARAM B, VCA_OPTIVA_WERCS_MAPPING_6X C ");
      sql.append("WHERE F.FORMULA_ID = B.FORMULA_ID ");
      sql.append("AND   NVL (FSI.GET_CLASS_XREF (F.CLASS), F.CLASS) = C.FORMULA_CLASS ");
      sql.append("AND   B.PARAM_CODE = C.OPTIVA_DATA_CODE ");
      sql.append("AND C.XREF_MAPPING = 'N' AND C.DATA_TEXT = 'DATA' ");
      sql.append("AND   F.FORMULA_ID = ");
      sql.append(pb.getFormulaId());
      sql.append(" union ");
      sql.append("SELECT DISTINCT C.WERCS_DATA_CODE, L.KEYFIELD4 ");
      sql.append("FROM FSFORMULA F, FSFORMULATECHPARAM B, VCA_OPTIVA_WERCS_MAPPING_6X C, VCA_LOOKUPS_6X L  ");
      sql.append("WHERE F.FORMULA_ID = B.FORMULA_ID  ");
      sql.append("AND   NVL (FSI.GET_CLASS_XREF (F.CLASS), F.CLASS) = NVL(C.FORMULA_CLASS,NVL (FSI.GET_CLASS_XREF (F.CLASS), F.CLASS))  ");
      sql.append("AND   B.PARAM_CODE = C.OPTIVA_DATA_CODE  ");
      sql.append("AND L.KEYFIELD1 = 'OPTIVA WERCS XREF' ");
      sql.append("AND L.KEYFIELD2 = C.OPTIVA_DATA_CODE ");
      sql.append("AND L.KEYFIELD3 = B.PVALUE ");
      sql.append("AND C.XREF_MAPPING = 'Y' AND C.DATA_TEXT = 'DATA' ");
      sql.append("AND   F.FORMULA_ID = ");
      sql.append(pb.getFormulaId());

      stmt = optivaConn.createStatement();
      rs = stmt.executeQuery(sql.toString());
      while (rs.next())
      {
        hm.put(rs.getString(1), rs.getString(2));
      }

      if (hm.isEmpty())
      {
        return hm;
      }

      hm.put(FPF, pb.getFlashF());
      hm.put(FPC, pb.getFlashC());
      hm.put(FRSETCD, pb.getSetCode());
      hm.put(FRMCODE, pb.getFormulaCode());
      hm.put(FVER, pb.getVersion());

      if (pb.getExtension() != null)
      {
        hm.put(EXTN, pb.getExtension());
      }

      if (pb.getSetCode().equalsIgnoreCase(USA) && hm.get("DENSLB") != null)
      {
        BigDecimal bd = new BigDecimal(hm.get(DENSLB).toString());
        bd = bd.divide(new BigDecimal(8.33), 2, 4);
        hm.put(DENKGL, bd.toString());
      }
    }
    catch (Exception e)
    {
      log4jLogger.error("SQL=" + sql.toString(), e);
    }
    finally
    {
      JDBCUtil.close(stmt, rs);
      JDBCUtil.close(optivaConn);
    }
    return hm;
  }

  public static HashMap getProductImportTextAttributes(BaseProductBean pb)
  {
    OracleConnection optivaConn = (OracleConnection) ConnectionAccessBean.getConnection(DataSource.FORMULATION);
    HashMap hm = new HashMap();
    StringBuilder sql = new StringBuilder();
    Statement stmt = null;
    ResultSet rs = null;
    try
    {
      sql.append("SELECT DISTINCT C.WERCS_DATA_CODE, DECODE(B.PVALUE, 'Y', 'Y', 'N', 'N','0 0','00', ROUND(B.PVALUE, C.PRECISION)) ");
      sql.append("FROM FSFORMULA F, FSFORMULATECHPARAM B, VCA_OPTIVA_WERCS_MAPPING_6X C ");
      sql.append("WHERE F.FORMULA_ID = B.FORMULA_ID ");
      sql.append("AND   NVL (FSI.GET_CLASS_XREF (F.CLASS), F.CLASS) = C.FORMULA_CLASS ");
      sql.append("AND   B.PARAM_CODE = C.OPTIVA_DATA_CODE ");
      sql.append("AND C.XREF_MAPPING = 'N' AND C.DATA_TEXT = 'TEXT' ");
      sql.append("AND   F.FORMULA_ID = ");
      sql.append(pb.getFormulaId());
      sql.append(" union ");
      sql.append("SELECT DISTINCT C.WERCS_DATA_CODE, L.KEYFIELD4 ");
      sql.append("FROM FSFORMULA F, FSFORMULATECHPARAM B, VCA_OPTIVA_WERCS_MAPPING_6X C, VCA_LOOKUPS_6X L  ");
      sql.append("WHERE F.FORMULA_ID = B.FORMULA_ID  ");
      sql.append("AND   NVL (FSI.GET_CLASS_XREF (F.CLASS), F.CLASS) = NVL(C.FORMULA_CLASS,NVL (FSI.GET_CLASS_XREF (F.CLASS), F.CLASS))  ");
      sql.append("AND   B.PARAM_CODE = C.OPTIVA_DATA_CODE  ");
      sql.append("AND L.KEYFIELD1 = 'OPTIVA WERCS XREF' ");
      sql.append("AND L.KEYFIELD2 = C.OPTIVA_DATA_CODE ");
      sql.append("AND L.KEYFIELD3 = B.PVALUE ");
      sql.append("AND C.XREF_MAPPING = 'Y' AND C.DATA_TEXT = 'TEXT' ");
      sql.append("AND   F.FORMULA_ID = ");
      sql.append(pb.getFormulaId());

      stmt = optivaConn.createStatement();
      rs = stmt.executeQuery(sql.toString());
      while (rs.next())
      {
        hm.put(rs.getString(1), rs.getString(2));
      }

      hm.put(BUSGP, pb.getBusinessGroup());
      hm.put(REGION, pb.getRegion());
      hm.put(FRMCT, pb.getFormulaClass());
    }
    catch (Exception e)
    {
      log4jLogger.error("SQL=" + sql.toString(), e);
    }
    finally
    {
      JDBCUtil.close(stmt, rs);
      JDBCUtil.close(optivaConn);
    }
    return hm;
  }

  public static boolean doesItemsWPGExist(String product)
  {
    log4jLogger.info("Starting to verify that every item has DENSLB and DENKGL in WERCS.");
    OracleConnection conn = (OracleConnection) ConnectionAccessBean.getConnection(DataSource.WERCS);
    OraclePreparedStatement pst = null;
    ResultSet rs = null;
    boolean existFlag = false;

    StringBuilder sql = new StringBuilder();
    sql.append("SELECT COUNT('X') ");
    sql.append("FROM T_PROD_DATA ");
    sql.append("WHERE F_PRODUCT =  :PRODUCT ");
    sql.append("AND F_DATA_CODE IN('DENSLB', 'DENKGL')");
    try
    {
      pst = (OraclePreparedStatement) conn.prepareStatement(sql.toString());
      pst.setStringAtName("PRODUCT", product);
      rs = pst.executeQuery();
      if (rs.next())
      {
        if (rs.getInt(1) == 2)
        {
          existFlag = true;
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
      JDBCUtil.close(conn);
    }
    return existFlag;
  }
  
  public static String getWercsFPlant(String language, String format, String subformat)
  {
    OracleConnection conn = (OracleConnection) ConnectionAccessBean.getConnection(DataSource.WERCS);
    String plant = null;
    StringBuilder sb = new StringBuilder();
    sb.append("select f_plant from va_publishing_groups where f_language = ? ");
    sb.append("and f_format = ? and f_subformat = ? and rownum = 1 ");
    plant =  ValsparLookUps.queryForSingleValueLeaveConnectionOpen(conn, sb.toString(), language, format, subformat);
    if(StringUtils.isEmpty(plant))
    {
      StringBuilder sb1 = new StringBuilder();
      sb1.append("select f_plant from t_pdf_msds where f_language = ? ");
      sb1.append("and f_format = ? and f_subformat = ? and rownum = 1 ");
      plant =  ValsparLookUps.queryForSingleValueLeaveConnectionOpen(conn, sb1.toString(), language, format, subformat);      
    }
    if(StringUtils.isEmpty(plant))
    {
      plant = "WERCS";
    }
    JDBCUtil.close(conn);
    return plant;
  }
}
