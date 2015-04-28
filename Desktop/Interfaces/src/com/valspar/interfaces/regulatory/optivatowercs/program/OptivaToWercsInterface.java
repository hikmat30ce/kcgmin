package com.valspar.interfaces.regulatory.optivatowercs.program;

import com.jacob.activeX.ActiveXComponent;
import com.jacob.com.Dispatch;
import com.valspar.interfaces.common.*;
import com.valspar.interfaces.common.beans.ConnectionBean;
import com.valspar.interfaces.common.utils.*;
import com.valspar.interfaces.regulatory.optivatowercs.beans.*;
import com.valspar.interfaces.regulatory.optivatowercs.exceptions.PopulateWercsException;
import java.math.BigDecimal;
import java.sql.*;
import java.util.*;
import oracle.jdbc.OracleCallableStatement;
import oracle.sql.*;
import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;

public class OptivaToWercsInterface extends BaseInterface
{
  private static Logger log4jLogger = Logger.getLogger(OptivaToWercsInterface.class);
  private static ActiveXComponent axc;

  public OptivaToWercsInterface(String interfaceName, String environment, String runType)
  {
    super(interfaceName, environment);

    ArrayList productList = null;
    boolean dataMissing = true;

    try
    {
      while (!onHold())
      {
        //added on hold check for change 510556 to prevent hang ups during db bounce.
        productList = new ArrayList();
        if (!hasErrors())
        {
          dataMissing = buildProductBeans(productList, runType);
          if (productList.isEmpty())
          {
            if (!dataMissing)
              break;
          }
          else
          {
            log4jLogger.info("There are " + productList.size() + " products in the list to be processed.");
            productCompare(productList);
            nvvCompare(productList);
            populateWercs(productList, environment, runType);
          }
        }
        log4jLogger.info("The OptivaToWercsInterface has no more to process.");
      }
    }
    catch (Exception e)
    {
      log4jLogger.error(e);
    }
    finally
    {
      cleanUpStatuses(productList);
      cleanUp();
    }
  }

  public void execute()
  {
  }

  public static void main(String[] parameters)
  {
    setAxc(new ActiveXComponent("OptivaToWercsJacobProject.clsJACOB_API"));
    new OptivaToWercsInterface(parameters[0], parameters[1], parameters[2]);
  }

  public boolean buildProductBeans(ArrayList productList, String runType)
  {
    ProductBean pb = null;
    boolean dataMissing = false;

    Statement stmt = null;
    ResultSet rs = null;
    try
    {
      StringBuffer sql = new StringBuffer();
      sql.append("SELECT A.ID, A.FORMULA_CODE, SUBSTR(A.FORMULA_CODE, INSTR(A.FORMULA_CODE, '.', -1, 1)+1, 3), ");
      sql.append("A.VERSION, FORMULA_ID, A.F_PRODUCT, A.F_PRODUCT_NAME, ");
      sql.append("A.FORMULA_CLASS, A.BUSINESS_GROUP, A.FLASHF, A.FLASHC, A.SET_CODE, A.PRIORITY, NVL(A.BYPASS_COMPARE, 0), A.STATUS ");
      sql.append("FROM VCA_OPTIVA_TO_WERCS A, VCA_OPTIVA_TO_WERCS_RUN_TYPE B ");
      sql.append("WHERE A.RUN_TYPE_ID = B.RUN_TYPE_ID ");
      sql.append("AND A.STATUS in (0, -50) ");
      //added date test for incd 500005 p.bruveris
      sql.append("AND A.DATE_ADDED <= SYSDATE ");
      sql.append("AND B.RUN_TYPE_DESC = '");
      sql.append(runType);
      sql.append("' ");
      sql.append("ORDER BY A.PRIORITY, A.DATE_ADDED ");

      ConnectionBean cb = getFromConn().get(0);

      stmt = cb.getConnection().createStatement();
      rs = stmt.executeQuery(sql.toString());

      //    Changed the "while" to an "if" so only one product is picked up at a time.
      //    Once rules are faster then we can switch it back to picking up more than one at a time.
      //      while (rs.next())
      if (rs.next())
      {
        pb = new ProductBean();
        pb.setId(rs.getString(1));
        pb.setFormulaCode(rs.getString(2));
        pb.setExtension(rs.getString(3));
        pb.setVersion(rs.getString(4));
        pb.setFormulaId(rs.getString(5));
        pb.setFProduct(rs.getString(6));
        pb.setFProductName(rs.getString(7));
        pb.setFormulaClass(rs.getString(8));
        pb.setBusinessGroup(rs.getString(9));
        pb.setFlashF(rs.getString(10));
        pb.setFlashC(rs.getString(11));
        pb.setSetCode(rs.getString(12));
        pb.setPriority(rs.getString(13));
        pb.setBypassCompare(rs.getInt(14) == 1? true: false);
        pb.setStatus(rs.getString(15));
        pb.setDescriptionLanguages(getDescriptionLanguages(pb.getBusinessGroup()));        

        updateQueueStatus(pb, 1);

        pb.setDataCodes(getDataCodes(pb));

        if (pb.getDataCodes().isEmpty())
        {
          dataMissing = true;
          throw new PopulateWercsException("buildProductBeans() error: " + pb.getFProduct() + " was not sent to Wercs because formula class '" + pb.getFormulaClass() + "' is not in VCA_OPTIVA_WERCS_MAPPING.");
        }
        String componentsMissing = allComponentsExist(pb);
        if (componentsMissing.length() > 0)
        {
          dataMissing = true;
          throw new PopulateWercsException("buildProductBeans() error: " + componentsMissing);
        }

        String componentError = getComponents(pb);
        if (componentError != null)
          throw new PopulateWercsException(componentError);

        productList.add(pb);
        log4jLogger.info(pb.getFProduct() + " was added to the ArrayList to be processed.");
      }
    }
    catch (PopulateWercsException pwe)
    {
      log4jLogger.error("PopulateWercsException: " + pwe.getExceptionMessage());
      populateLogDetails(pb, pwe.getExceptionMessage());
      updateQueueStatus(pb, -99);
      deleteWercsProduct(pb);
    }
    catch (Exception e)
    {
      log4jLogger.error(e);
    }
    finally
    {
      JDBCUtil.close(stmt, rs);
    }
    return dataMissing;
  }

  public boolean hasErrors()
  {
    boolean hasErrorsReturn = false;
    Statement stmt = null;
    ResultSet rs = null;

    try
    {

      StringBuilder sql = new StringBuilder();
      sql.append("select count(status) as failed from ");
      sql.append("(select b.* from vca_optiva_to_wercs b ");
      sql.append("where end_time is not null order by end_time desc) ");
      sql.append("where rownum <= 250 having status = -99 group by status ");

      ConnectionBean cb = getFromConn().get(0);
      stmt = cb.getConnection().createStatement();
      rs = stmt.executeQuery(sql.toString());

      if (rs.next())
      {
        if (rs.getInt(1) == 250)
        {
          hasErrorsReturn = true;
          log4jLogger.error("OptivaToWercsInterface - There have been " + rs.getInt(1) + " transfer errors in a row");
          EmailBean.emailMessage("CRITICAL-MSG:OptivaToWercsInterface - There have been " + rs.getInt(1) + " transfer errors in a row", "OptivaToWercsInterface - There have been " + rs.getInt(1) + " transfer errors in a row", getNotificationEmail());
          log4jLogger.info("Starting to sleep");
          Thread.sleep(86400000);
          log4jLogger.info("Done sleeping");
        }
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
    return hasErrorsReturn;
  }

  public HashMap getDataCodes(ProductBean pb)
  {
    HashMap hm = new HashMap();
    StringBuffer sql = new StringBuffer();
    Statement stmt = null;
    ResultSet rs = null;
    try
    {
      // sql.append("SELECT DISTINCT C.WERCS_DATA_CODE, DECODE(B.PVALUE, 'Y', 'Y', 'N', 'N', ROUND(B.PVALUE, C.PRECISION)) ");
      sql.append("SELECT DISTINCT C.WERCS_DATA_CODE, DECODE(B.PVALUE, 'Y', 'Y', 'N', 'N','0 0','00', ROUND(B.PVALUE, C.PRECISION)) ");
      sql.append("FROM VCA_OPTIVA_TO_WERCS A, FSFORMULATECHPARAM B, VCA_OPTIVA_WERCS_MAPPING C ");
      sql.append("WHERE A.FORMULA_ID = B.FORMULA_ID ");
      sql.append("AND   A.FORMULA_CLASS = C.FORMULA_CLASS ");
      sql.append("AND   B.PARAM_CODE = C.OPTIVA_DATA_CODE ");
      sql.append("AND   A.FORMULA_ID = ");
      sql.append(pb.getFormulaId());

      ConnectionBean cb = getFromConn().get(0);
      stmt = cb.getConnection().createStatement();
      rs = stmt.executeQuery(sql.toString());
      while (rs.next())
      {
        hm.put(rs.getString(1), rs.getString(2));
      }

      if (hm.isEmpty())
        return hm;

      hm.put(Constants.FRMCLS, pb.getFormulaClass());
      hm.put(Constants.BUSGP, pb.getBusinessGroup());
      hm.put(Constants.FLASHPT, pb.getFlashF());
      hm.put(Constants.FLASHC, pb.getFlashC());
      hm.put(Constants.FRSETCD, pb.getSetCode());
      hm.put(Constants.FRMCODE, pb.getFormulaCode());
      hm.put(Constants.VERSION, pb.getVersion());

      if (pb.getExtension() != null)
        hm.put(Constants.EXTN, pb.getExtension());

      if (pb.getSetCode().equalsIgnoreCase(Constants.USA))
      {
        BigDecimal bd = new BigDecimal(hm.get(Constants.DENSITY).toString());
        bd = bd.divide(new BigDecimal(8.33), 2, 4);
        hm.put(Constants.DENSKG, bd.toString());
      }
    }
    catch (Exception e)
    {
      log4jLogger.error("SQL=" + sql.toString(), e);
    }
    finally
    {
      JDBCUtil.close(stmt, rs);
    }
    return hm;
  }
    
  public ArrayList<String> getDescriptionLanguages(String businessGroup)
  {
    ArrayList<String> returnValue = new ArrayList<String>();
    StringBuffer sql = new StringBuffer();
    Statement stmt = null;
    ResultSet rs = null;
    try
    {
      sql.append("select keyfield3 from vca_lookups ");
      sql.append("WHERE KEYFIELD1  = 'PRODUCT_DESCRIPTION_TRANSLATION' ");
      sql.append("and keyfield2 = '");
      sql.append(businessGroup);
      sql.append("' ");

      ConnectionBean cb = getToConn().get(0);
      stmt = cb.getConnection().createStatement();
      rs = stmt.executeQuery(sql.toString());
      while (rs.next())
      {
        returnValue.add(rs.getString(1));
      }
    }
    catch (Exception e)
    {
      log4jLogger.error("SQL=" + sql.toString(), e);
    }
    finally
    {
      JDBCUtil.close(stmt, rs);
    }
    return returnValue;
  }

  public String allComponentsExist(ProductBean pb)
  {
    StringBuffer returnStringBuffer = new StringBuffer();
    Statement stmt = null;
    ResultSet rs = null;
    try
    {
      StringBuffer sql = new StringBuffer();
      sql.append("select a.item_code from fsformulaingred@TOFM a ");
      sql.append("where a.formula_id = ");
      sql.append(pb.getFormulaId());
      sql.append(" and a.material_pct > 0 ");
      sql.append("and not exists (select * from t_product_alias_names where f_alias = a.item_code) ");

      ConnectionBean cb = getToConn().get(0);
      stmt = cb.getConnection().createStatement();
      rs = stmt.executeQuery(sql.toString());
      while (rs.next())
      {
        returnStringBuffer.append("Component " + rs.getString(1) + " (in product " + pb.getFProduct() + ") is not in T_PRODUCT_ALIAS_NAMES in WERCS. ");
        log4jLogger.error("Component " + rs.getString(1) + " (in product " + pb.getFProduct() + ") is not in T_PRODUCT_ALIAS_NAMES in WERCS.");
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
    return returnStringBuffer.toString();
  }

  public String getComponents(ProductBean pb)
  {
    String returnString = null;
    ArrayList ar = new ArrayList();
    Statement stmt = null;
    ResultSet rs = null;
    try
    {
      StringBuffer sql = new StringBuffer();

      sql.append("select b.f_product, ROUND(SUM(a.material_pct), 4), B.F_ALIAS_NAME ");
      sql.append("from fsformulaingred@TOFM A,  T_PRODUCT_ALIAS_NAMES B ");
      sql.append("where a.item_code = b.f_alias ");
      sql.append("and   A.material_pct > 0 ");
      sql.append("and   A.formula_id = ");
      sql.append(pb.getFormulaId());
      sql.append("group by b.f_product, b.f_alias_name ");

      ConnectionBean cb = getToConn().get(0);
      stmt = cb.getConnection().createStatement();

      rs = stmt.executeQuery(sql.toString());
      while (rs.next())
      {
        ComponentBean compBean = new ComponentBean();
        compBean.setComponentId(rs.getString(1));
        compBean.setPercent(rs.getString(2));
        compBean.setDescription(rs.getString(3));

        if (compBean.getDescription().length() > 60)
        {
          returnString = "getComponents() error:  Chemical Name is " + compBean.getDescription().length() + " characters which is greater than the 60 character limit.  Product: " + pb.getFProduct() + ", Component: " + compBean.getComponentId() + ", Chemical Name: " + compBean.getDescription();
          break;
        }
        ar.add(compBean);
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
    pb.setComponentBeans(ar);
    return returnString;
  }

  public void productCompare(ArrayList productList)
  {
    ConnectionBean cb = getToConn().get(0);
    try
    {
      Iterator i = productList.iterator();
      while (i.hasNext())
      {
        ProductBean pb = (ProductBean) i.next();

        if (pb.isBypassCompare())
        {
          pb.setSameProduct(false);
          log4jLogger.info("Bypassing compare for product " + pb.getFProduct());
        }
        else
        {

          StringBuffer sql = new StringBuffer();
          sql.append("select WERCS_OPTIVA_FORMULA_COMPARE('");
          sql.append(pb.getFProduct());
          sql.append("', '");
          sql.append(cb.getLogUser());
          sql.append("', '");
          sql.append((String) pb.getDataCodes().get("DENSITY"));
          sql.append("', '");
          //added DENSKG for incd 500005 p.bruveris
          sql.append((String) pb.getDataCodes().get("DENSKG"));
          sql.append("', '");
          sql.append(pb.getFlashF());
          sql.append("', '");
          sql.append(pb.getBusinessGroup());
          sql.append("', '");
          sql.append(pb.getFormulaClass());
          sql.append("', '");

          if (pb.isDataCodeExist("MALV1"))
            sql.append(pb.getDataCodeValue("MALV1"));

          sql.append("', '");

          if (pb.isDataCodeExist("MALV2"))
            sql.append(pb.getDataCodeValue("MALV2"));

          sql.append("', '");

          if (pb.isDataCodeExist("NO_YTAL"))
            sql.append(pb.getDataCodeValue("NO_YTAL"));

          sql.append("', '");

          if (pb.isDataCodeExist("NO_YLGP"))
            sql.append(pb.getDataCodeValue("NO_YLGP"));

          sql.append("', '");

          if (pb.isDataCodeExist("DE_BP31"))
            sql.append(pb.getDataCodeValue("DE_BP31"));

          sql.append("', '");

          if (pb.isDataCodeExist("VISCOST") && pb.getSetCode().equalsIgnoreCase("EURO"))
            sql.append(pb.getDataCodeValue("VISCOST"));

          sql.append("', '");

          sql.append(pb.getSetCode());
          sql.append("') from dual");

          log4jLogger.info(sql.toString());
          Statement stmt = null;
          ResultSet rs = null;

          try
          {
            stmt = cb.getConnection().createStatement();
            rs = stmt.executeQuery(sql.toString());
            if (rs.next())
            {
              if (rs.getString(1).equalsIgnoreCase("TRUE"))
              {
                pb.setSameProduct(true);
                componentCompare(pb);
                if (pb.isSameProduct())
                  populateLogDetails(pb, "Product " + pb.getFProduct() + " has not changed from the previous version.");
              }
              else
              {
                pb.setSameProduct(false);
              }
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
        //incd 500005 p.bruveris
        if ((pb.getDataCodes().get("DENSITY") == "null") || (pb.getDataCodes().get("DENSITY") == null) || (pb.getDataCodes().get("DENSKG") == "null") || (pb.getDataCodes().get("DENSKG") == null))
        {
          log4jLogger.info("Density and/or Denskg not found for " + pb.getFProduct() + ". Cannot add to DensitySyncQueue.");
        }
        else
        {
          // If Density or Denskg is changed then insert into queue.
          if (checkDataCodeChange(pb, "DENSITY") || checkDataCodeChange(pb, "DENSKG"))
          {
            log4jLogger.info("Density and/or Denskg changed for " + pb.getFProduct() + ". Add to DensitySyncQueue.");
            insertIntoDensitySyncQueue(pb);
          }
          else
          {
            log4jLogger.info("Density and/or Denskg did not change for " + pb.getFProduct() + ". Don't add to DensitySyncQueue.");
          }
        }
        //end incd 500005 changes
      }
    }
    catch (Exception e)
    {
      log4jLogger.error(e);
    }
  }

  public void nvvCompare(ArrayList productList)
  {
    ConnectionBean cb = getToConn().get(0);
    PreparedStatement pstmt = null;
    ResultSet rs = null;
    try
    {
      Iterator i = productList.iterator();
      while (i.hasNext())
      {
        ProductBean pb = (ProductBean) i.next();

        StringBuffer sql = new StringBuffer();
        sql.append("select tp.pvalue, p.f_data ");
        sql.append("from fsformulatechparam@TOFM tp, fsitem@tofm i, t_prod_data p, ");
        sql.append("valspar.vca_lookups@tona l ");
        sql.append("where tp.formula_id = i.formula_id ");
        sql.append("and i.item_code = p.f_product ");
        sql.append("and l.keyfield2 = '");
        sql.append(pb.getBusinessGroup());
        sql.append("' ");
        sql.append("and tp.param_code = 'NVV' ");
        sql.append("and p.f_data_code = 'NVVPCT' ");
        sql.append("and l.keyfield1 = 'NVV_CHANGE_PCT' ");
        sql.append("and p.f_product = '");
        sql.append(pb.getFProduct());
        sql.append("' ");
        sql.append("and abs(tp.pvalue - p.f_data) >= l.keyfield3");

        pstmt = cb.getConnection().prepareStatement(sql.toString());
        rs = pstmt.executeQuery(sql.toString());
        if (rs.next())
        {
          pb.setOptivaVs(rs.getString(1));
          pb.setWercsVs(rs.getString(2));
          insertIntoNvvCompareQueue(pb);
        }
      }
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

  public void insertIntoNvvCompareQueue(ProductBean pb)
  {
    Statement stmt = null;
    try
    {
      ConnectionBean cb = getToConn().get(0);
      StringBuffer sql = new StringBuffer();
      sql.append("MERGE INTO vca_nvv_compare a ");
      sql.append("USING (SELECT '" + pb.getFProduct() + "' PRODUCT_NO FROM DUAL) b ");
      sql.append("ON (a.PRODUCT_NO = b.PRODUCT_NO) ");
      sql.append("WHEN MATCHED THEN ");
      sql.append("UPDATE ");
      sql.append("SET formula_id = " + pb.getFormulaId() + ", ");
      sql.append("optiva_pct = " + pb.getOptivaVs() + ", ");
      sql.append("wercs_pct = " + pb.getWercsVs() + ", ");
      sql.append("business_group = '" + pb.getBusinessGroup() + "', ");
      sql.append("date_added = sysdate ");
      sql.append("WHEN NOT MATCHED THEN ");
      sql.append("INSERT (a.product_no, a.formula_id, a.optiva_pct, a.wercs_pct, a.business_group, a.date_added) ");
      sql.append("VALUES ('" + pb.getFProduct() + "', " + pb.getFormulaId() + ", " + pb.getOptivaVs() + ", " + pb.getWercsVs() + ", '" + pb.getBusinessGroup() + "', sysdate) ");
      log4jLogger.info(sql.toString());

      stmt = cb.getConnection().createStatement();
      stmt.executeUpdate(sql.toString());
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

  public boolean checkDataCodeChange(ProductBean pb, String dataCode)
  {
    boolean dataCodeChanged = false;
    if (!pb.getFProduct().startsWith("T-")) //this was in the function wercs_optiva_formula_compare
    {
      Statement stmt = null;
      ResultSet rs = null;

      try
      {
        StringBuffer sql = new StringBuffer();
        sql.append("select decode(ROUND(NVL('");
        sql.append((String) pb.getDataCodes().get(dataCode));
        sql.append("' ,0),2), ROUND(NVL(f_data,0),2), 'SAME', 'DIFF')");
        sql.append("from t_prod_data ");
        sql.append("where f_product = '");
        sql.append(pb.getFProduct());
        sql.append("' and f_data_code = '");
        sql.append(dataCode);
        sql.append("'");

        ConnectionBean cb = getToConn().get(0);
        stmt = cb.getConnection().createStatement();
        rs = stmt.executeQuery(sql.toString());

        if (rs.next())
        {
          if (rs.getString(1).equalsIgnoreCase("DIFF"))
            dataCodeChanged = true;
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
    return dataCodeChanged;
  }

  public void insertIntoDensitySyncQueue(ProductBean pb)
  {
    Statement stmt = null;
    try
    {
      ConnectionBean cb = getToConn().get(0);
      StringBuffer sql = new StringBuffer();
      sql.append("INSERT INTO VCA_DENSITY_SYNC_QUEUE ");
      sql.append("SELECT 0, '");
      sql.append(pb.getFProduct());
      sql.append("', '");
      sql.append((String) pb.getDataCodes().get("DENSITY"));
      sql.append("', '");
      sql.append((String) pb.getDataCodes().get("DENSKG"));
      sql.append("', '");
      sql.append(cb.getLogUser());
      sql.append("', ");
      sql.append("SYSDATE, NULL, NULL FROM DUAL ");
      sql.append("WHERE NOT EXISTS ");
      sql.append("(SELECT * FROM VCA_DENSITY_SYNC_QUEUE ");
      sql.append("WHERE PRODUCT = '");
      sql.append(pb.getFProduct());
      sql.append("'  AND DATE_PROCESSED IS NULL) ");

      stmt = cb.getConnection().createStatement();
      stmt.executeUpdate(sql.toString());
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

  public void componentCompare(ProductBean pb)
  {
    boolean sameComponent = true;

    ConnectionBean cb = getToConn().get(0);
    PreparedStatement componentsPstmt = null;
    ResultSet componentsRs = null;
    try
    {
      StringBuffer componentsSql = new StringBuffer();
      componentsSql.append("select c.F_CAS_NUMBER, c.F_COMPONENT_ID, c.F_CHEM_NAME, d.f_data, SUM(ROUND(F_PERCENT * (a.material_pct/100),4)) ");
      componentsSql.append("from fsformulaingred@TOFM A,  T_PRODUCT_ALIAS_NAMES B, T_PROD_COMP C, ");
      componentsSql.append("(select f_data, f_product from T_PROD_DATA where f_data_code = 'NOCASCB') D ");
      componentsSql.append("where a.item_code = b.f_alias ");
      componentsSql.append("and   b.f_product = c.f_product ");
      componentsSql.append("and   d.f_product(+) = c.F_COMPONENT_ID ");
      componentsSql.append("and   A.material_pct > 0 ");
      componentsSql.append("and   A.formula_id = ? ");
      componentsSql.append("group by c.F_CAS_NUMBER, c.F_COMPONENT_ID, c.F_CHEM_NAME, d.f_data ");
      componentsSql.append("order by c.F_CAS_NUMBER, c.F_COMPONENT_ID DESC ");

      componentsPstmt = cb.getConnection().prepareStatement(componentsSql.toString());
      componentsPstmt.setString(1, pb.getFormulaId());
      componentsRs = componentsPstmt.executeQuery();

      StringBuffer sql = new StringBuffer();
      sql.append("SELECT * FROM T_COMPONENT_RANGES A, T_PROD_COMP B ");
      sql.append("WHERE B.F_PRODUCT = ? ");
      sql.append("AND   B.F_COMPONENT_ID = ? ");
      sql.append("AND   B.F_CAS_NUMBER = ? ");
      sql.append("AND   A.F_FORMAT = 'VAL' AND A.F_SUBFORMAT = 'USA' ");
      sql.append("AND   B.F_PERCENT BETWEEN A.F_IN_RANGE_LOW AND A.F_IN_RANGE_HIGH ");
      sql.append("AND   ? BETWEEN F_IN_RANGE_LOW AND F_IN_RANGE_HIGH ");

      PreparedStatement pstmt = null;
      ResultSet rs = null;
      while (componentsRs.next() && sameComponent)
      {
        try
        {
          pstmt = cb.getConnection().prepareStatement(sql.toString());
          pstmt.setString(1, pb.getFProduct());
          pstmt.setString(2, componentsRs.getString(2));
          pstmt.setString(3, componentsRs.getString(1));
          pstmt.setString(4, componentsRs.getString(5));
          rs = pstmt.executeQuery();

          if (!rs.next())
          {
            pb.setSameProduct(false);
            sameComponent = false;
          }
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
    }
    catch (Exception e)
    {
      log4jLogger.error(e);
    }
    finally
    {
      JDBCUtil.close(componentsPstmt, componentsRs);
    }
  }

  public void populateWercs(ArrayList productList, String environment, String runType)
  {
    Object axcObj = axc.getObject();
    ProductBean pb = null;

    try
    {
      Iterator i = productList.iterator();
      while (i.hasNext())
      {
        try
        {
          pb = (ProductBean) i.next();
          log4jLogger.info("Product " + pb.getFProduct() + " is now being processed");
          if (pb.isSameProduct())
          {
            log4jLogger.info("Product " + pb.getFProduct() + " has not changed.  Physical properties are being updated.");

            String uppReturn = updatePhysicalProperties(pb);
            if (uppReturn == null)
              log4jLogger.info("updatePhysicalProperties completed sucessfully for product " + pb.getFProduct());
            else
              throw new PopulateWercsException("updatePhysicalProperties error for product " + pb.getFProduct() + ": " + uppReturn);

            if (newDescription(pb))
            {
              String updReturn = updateProductDescription(pb);
              if (updReturn == null)
                log4jLogger.info("updateProductDescription completed sucessfully for product " + pb.getFProduct());
              else
                throw new PopulateWercsException("updateProductDescription error for product " + pb.getFProduct() + ": " + updReturn);

              addToPublishingQueue(pb, runType);
            }
            else
            {
              republishCpds(pb, runType);
            }
          }
          else
          {
            log4jLogger.info("Product " + pb.getFProduct() + " has changed and will be brought into Wercs.");
            deleteWercsProduct(pb);

            String addProductReturn = Dispatch.call(axcObj, "addProductAPI", pb.getFProduct(), pb.getFProductName()).toString();
            if (addProductReturn.equals("-1"))
              log4jLogger.info("addProduct completed sucessfully for product " + pb.getFProduct());
            else
              throw new PopulateWercsException("addProduct error for product " + pb.getFProduct() + ": " + addProductReturn);

            String updReturn = updateProductDescription(pb);
            if (updReturn == null)
              log4jLogger.info("updateProductDescription completed sucessfully for product " + pb.getFProduct());
            else
              throw new PopulateWercsException("updateProductDescription error for product " + pb.getFProduct() + ": " + updReturn);

            String lwcReturn = loadWercsComponents(pb);
            if (lwcReturn == null)
              log4jLogger.info("loadWercsComponents completed sucessfully for product " + pb.getFProduct());
            else
              throw new PopulateWercsException("loadWercsComponents error for product " + pb.getFProduct() + ": " + lwcReturn);

            String uppReturn = updatePhysicalProperties(pb);
            if (uppReturn == null)
              log4jLogger.info("updatePhysicalProperties completed sucessfully for product " + pb.getFProduct());
            else
              throw new PopulateWercsException("updatePhysicalProperties error for product " + pb.getFProduct() + ": " + uppReturn);

            log4jLogger.info("Starting AutoGenerate for product " + pb.getFProduct());
            String autoGenerateReturn = Dispatch.call(axcObj, "AutoGenerateAPI", pb.getFProduct()).toString();
            if (autoGenerateReturn.equals("0"))
              log4jLogger.info("AutoGenerate completed sucessfully for product " + pb.getFProduct());
            else
              throw new PopulateWercsException("AutoGenerate error for product " + pb.getFProduct() + ".  Check the ingredients in Optiva: " + autoGenerateReturn);

            log4jLogger.info("Starting EUWizardAPI");
            String euWizardReturn = Dispatch.call(axcObj, "EUWizardAPI", pb.getFProduct()).toString();
            if (euWizardReturn.equals("0"))
              log4jLogger.info("EUWizard completed sucessfully for product " + pb.getFProduct() + ".");
            else
              throw new PopulateWercsException("EUWizard error for product " + pb.getFProduct() + ": " + euWizardReturn);

            log4jLogger.info("Starting callRuleWriter for rule group 3189");
            String ruleWriter3189Return = Dispatch.call(axcObj, "callRuleWriter", pb.getFProduct(), "3189").toString();
            if (ruleWriter3189Return.equals("0"))
              log4jLogger.info("RuleWriter Group 3189 completed sucessfully for product " + pb.getFProduct());
            else
              throw new PopulateWercsException("RuleWriter error for rule group 2667 for product " + pb.getFProduct() + ": " + ruleWriter3189Return);

            /*log4jLogger.info("Starting DotCalcInterface");
            DotCalcInterface dc = new DotCalcInterface("DotCalcInterface", environment);
            String dcReturn = dc.runDotCalcInterface(pb.getFProduct());
            if (dcReturn == null)
              log4jLogger.info("DotCalcInterface completed sucessfully for product " + pb.getFProduct());
            else
              throw new PopulateWercsException("DotCalcInterface error for product " + pb.getFProduct() + ": " + dcReturn);*/

            log4jLogger.info("Starting callRuleWriter for rule group 2467");
            String ruleWriter2467Return = Dispatch.call(axcObj, "callRuleWriter", pb.getFProduct(), "2467").toString();
            if (ruleWriter2467Return.equals("0"))
              log4jLogger.info("RuleWriter Group 2467 completed sucessfully for product " + pb.getFProduct());
            else
              throw new PopulateWercsException("RuleWriter error for rule group 2467 for product " + pb.getFProduct() + ": " + ruleWriter2467Return);

            log4jLogger.info("Starting GHSComInterfaceAPI");
            String ghsComInterfaceAPIReturn = Dispatch.call(axcObj, "GHSComInterfaceAPI", pb.getFProduct()).toString();
            if (ghsComInterfaceAPIReturn.equals("0"))
              log4jLogger.info("GHSComInterfaceAPI completed sucessfully for product " + pb.getFProduct() + ".");
            else
              throw new PopulateWercsException("GHSComInterfaceAPI error for product " + pb.getFProduct() + ": " + ghsComInterfaceAPIReturn);

            log4jLogger.info("Starting callRuleWriter for rule group 3592");
            String ruleWriter3592Return = Dispatch.call(axcObj, "callRuleWriter", pb.getFProduct(), "3592").toString();
            if (ruleWriter3592Return.equals("0"))
              log4jLogger.info("RuleWriter Group 3592 completed sucessfully for product " + pb.getFProduct());
            else
              throw new PopulateWercsException("RuleWriter error for rule group 3592 for product " + pb.getFProduct() + ": " + ruleWriter3592Return);

            log4jLogger.info("Starting updProdFormatAuth");
            String updProdFormatAuthReturn = Dispatch.call(axcObj, "updProdFormatAuth", pb.getFProduct()).toString();
            if (updProdFormatAuthReturn.equals("0"))
              log4jLogger.info("updProdFormatAuth completed sucessfully for product " + pb.getFProduct());
            else
              throw new PopulateWercsException("updProdFormatAuth error for product " + pb.getFProduct() + ": " + updProdFormatAuthReturn);

            log4jLogger.info("Starting updProdFormulaAuth");
            String updProdFormulaAuthReturn = Dispatch.call(axcObj, "updProdFormulaAuth", pb.getFProduct()).toString();
            if (updProdFormulaAuthReturn.equals("0"))
              log4jLogger.info("updProdFormulaAuth completed sucessfully for product " + pb.getFProduct());
            else
              throw new PopulateWercsException("updProdFormatAuth error for product " + pb.getFProduct() + ": " + updProdFormulaAuthReturn);

            log4jLogger.info("Starting updProdFormulaRev");
            String updProdFormulaRevReturn = Dispatch.call(axcObj, "updProdFormulaRev", pb.getFProduct()).toString();
            if (updProdFormulaRevReturn.equals("0"))
              log4jLogger.info("updProdFormulaRev completed sucessfully for product " + pb.getFProduct());
            else
              throw new PopulateWercsException("updProdFormulaRev error for product " + pb.getFProduct() + ": " + updProdFormulaRevReturn);
            
            addToPublishingQueue(pb, runType);
          }

          updateQueueStatus(pb, 2);
        }
        catch (PopulateWercsException pwe)
        {
          log4jLogger.error("PopulateWercsException: " + pwe.getExceptionMessage() + pb.toStringComponentBean());
          populateLogDetails(pb, pwe.getExceptionMessage() + pb.toStringComponentBean());
          updateQueueStatus(pb, -99);
          deleteWercsProduct(pb);
        }
      }
    }
    catch (Exception e)
    {
      log4jLogger.error(e);
    }
  }

  public String updatePhysicalProperties(ProductBean pb)
  {
    String returnValue = null;
    ConnectionBean cb = getToConn().get(0);
    OracleCallableStatement cstmt = null;
    
    try
    {
      ArrayDescriptor arrayDesc = ArrayDescriptor.createDescriptor("ARRAY_OF_MAP_OBJ", cb.getConnection());
      String[][] objArray = new String[pb.getDataCodes().size()][2];
      objArray = Conversion.hashMapToArray(pb.getDataCodes());
      ARRAY oracleArray = new ARRAY(arrayDesc, cb.getConnection(), objArray);
      cstmt = (OracleCallableStatement) cb.getConnection().prepareCall("{call WERCS_UPDATE_PRODUCT_DATA(?,?,?,?)}");

      cstmt.setString(1, pb.getFProduct());
      cstmt.setString(2, Constants.OPTIVA_USER);
      cstmt.setArray(3, oracleArray);
      cstmt.registerOutParameter(4, Types.VARCHAR);
      cstmt.execute();

      if (cstmt.getString(4) != null)
      {
        returnValue = cstmt.getString(4);
      }
      else
        log4jLogger.info("Updated physical properties for product " + pb.getFProduct());
    }
    catch (Exception e)
    {
      log4jLogger.error(e);
      returnValue = e.getMessage();
    }
    finally
    {
      JDBCUtil.close(cstmt);
    }
    return returnValue;
  }

  private boolean newDescription(ProductBean pb)
  {
    boolean returnValue = false;
    Statement stmt = null;
    ResultSet rs = null;

    try
    {
      ConnectionBean cb = getToConn().get(0);
      StringBuffer sql = new StringBuffer();
      sql.append("SELECT NEW_DESCRIPTION('");
      sql.append(pb.getFProduct());
      sql.append("', '");
      sql.append(pb.getFProductName());
      sql.append("') FROM DUAL");

      stmt = cb.getConnection().createStatement();
      rs = stmt.executeQuery(sql.toString());
      if (rs.next())
        if (rs.getString(1).equalsIgnoreCase("TRUE"))
          returnValue = true;
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

  public void deleteWercsProduct(ProductBean pb)
  {
    ConnectionBean cb = getToConn().get(0);
    CallableStatement cstmt = null;
    try
    {
      cstmt = cb.getConnection().prepareCall("{call OPTIVA_TO_WERCS_DELETE_PRODUCT(?,?)}");
      cstmt.setString(1, pb.getFProduct());
      cstmt.registerOutParameter(2, Types.VARCHAR);
      cstmt.execute();

      if (cstmt.getString(2) != null)
        log4jLogger.error("Error in deleteWercsProduct(): " + cstmt.getString(2));
      else
        log4jLogger.info("Product " + pb.getFProduct() + " was deleted from Wercs");
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

  public String updateProductDescription(ProductBean pb)
  {
    String returnValue = null;
    ConnectionBean cb = getToConn().get(0);
    CallableStatement cstmt = null;
    try
    {
      cstmt = cb.getConnection().prepareCall("{call WERCS_UPDATE_PRODUCT_DESC(?,?,?,?)}");
      cstmt.setString(1, pb.getFProduct());
      cstmt.setString(2, pb.getFProductName());
      cstmt.setString(3, Constants.OPTIVA_USER);
      cstmt.registerOutParameter(4, Types.VARCHAR);
      cstmt.execute();

      if (cstmt.getString(4) != null)
        returnValue = "Error in updateProductDescription(): " + cstmt.getString(4);
      else
        log4jLogger.info("The description for Product " + pb.getFProduct() + " was updated to " + pb.getFProductName());
      
      for (String language: pb.getDescriptionLanguages())
      {
        //LanguageUtility.updateWercsDescriptionTranslation(pb.getFProduct(), pb.getFProductName(), language, getToConn().get(0));
        //The above line is in the current code on the windows machines, commented out here for compilation reasons in the new project.  
        //the windows interfaces will be removed from this project and replaced with the version under wercs when we upgrade
      }
    }
    catch (Exception e)
    {
      log4jLogger.error(e);
    }
    finally
    {
      JDBCUtil.close(cstmt);
    }
    return returnValue;
  }

  public String loadWercsComponents(ProductBean pb)
  {
    String returnValue = null;
    ConnectionBean cb = getToConn().get(0);
    OracleCallableStatement cstmt = null;
    try
    {
      ArrayDescriptor arrayDesc = ArrayDescriptor.createDescriptor("ARRAY_OF_INPUT_PRODUCT", cb.getConnection());
      String[][] objArray = new String[pb.getComponentBeans().size()][3];
      objArray = Conversion.arrayListToArray(pb.getComponentBeans());
      ARRAY oracleArray = new ARRAY(arrayDesc, cb.getConnection(), objArray);
      cstmt = (OracleCallableStatement) cb.getConnection().prepareCall("{call WERCS_ADD_INPUT_PRODUCTS(?,?,?,?)}");
      cstmt.setString(1, pb.getFProduct());
      cstmt.setArray(2, oracleArray);
      cstmt.setString(3, Constants.OPTIVA_USER);
      cstmt.registerOutParameter(4, Types.VARCHAR);
      cstmt.execute();

      if (cstmt.getString(4) != null)
      {
        returnValue = cstmt.getString(4);
      }
      else
        log4jLogger.info("Components for product " + pb.getFProduct() + " have sucessfully been loaded into T_PROD_WITH_INPUT_PROD");
    }
    catch (Exception e)
    {
      log4jLogger.error("for product " + pb.getFProduct(), e);
      returnValue = e.getMessage();
    }
    finally
    {
      JDBCUtil.close(cstmt);
    }
    return returnValue;
  }

  public void addToPublishingQueue(ProductBean pb, String runType)
  {
    ConnectionBean cb = getToConn().get(0);
    CallableStatement cstmt = null;
    try
    {
      cstmt = cb.getConnection().prepareCall("{call WERCS_PUBLISH_PRODUCT_PROC(?,?,?,?,?,?,?)}");
      cstmt.setString(1, pb.getFProduct());
      cstmt.setString(2, pb.getFProduct());
      cstmt.setString(3, pb.getFProductName());
      cstmt.setString(4, pb.getPriority());
      cstmt.setString(5, runType);
      cstmt.setString(6, Constants.OPTIVA_USER);
      cstmt.registerOutParameter(7, Types.VARCHAR);
      cstmt.execute();

      if (cstmt.getString(7) != null)
        log4jLogger.error("Error in addToPublishingQueue(): " + cstmt.getString(7));
      else
        log4jLogger.info("Product " + pb.getFProduct() + " successfully added to the publishing queue");
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

  public void republishCpds(ProductBean pb, String runType)
  {
    Statement stmt = null;
    try
    {
      StringBuffer sql = new StringBuffer();
      sql.append("INSERT INTO VA_PUBLISHING_QUEUE ");
      sql.append("SELECT VA_PUBLISHING_QUEUE_SEQ.NEXTVAL, F_PRODUCT, ");
      sql.append("F_FORMAT, F_SUBFORMAT, F_LANGUAGE, F_PRODUCT, F_PRODUCT_NAME, ");
      sql.append(pb.getPriority());
      sql.append(", '");
      sql.append(runType);
      sql.append("', 0, '");
      sql.append(Constants.OPTIVA_USER);
      sql.append("', SYSDATE ");
      sql.append("FROM T_PDF_MSDS ");
      sql.append("WHERE F_PRODUCT = '");
      sql.append(pb.getFProduct());
      sql.append("' AND F_SUBFORMAT in ('PDS', 'CMP') ");
      sql.append("AND F_AUTHORIZED <> 0 ");

      ConnectionBean cb = getToConn().get(0);
      stmt = cb.getConnection().createStatement();
      stmt.executeUpdate(sql.toString());
      log4jLogger.info("Repulishing the CPDS for product " + pb.getFProduct());
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

  public void updateQueueStatus(ProductBean pb, int status)
  {
    Statement stmt = null;
    try
    {
      if (!StringUtils.equalsIgnoreCase(pb.getStatus(), "-50") && status == -99)
      {
        status = -50;
      }

      StringBuffer sql = new StringBuffer();
      sql.append("UPDATE VCA_OPTIVA_TO_WERCS SET STATUS = ");
      sql.append(status);

      if (status == 1)
        sql.append(", START_TIME = SYSDATE, LOG_DETAILS = '' ");
      else
        sql.append(", END_TIME = SYSDATE");

      sql.append(", DATE_MODIFIED = SYSDATE WHERE ID = ");
      sql.append(pb.getId());

      ConnectionBean cb = getFromConn().get(0);
      stmt = cb.getConnection().createStatement();
      stmt.executeUpdate(sql.toString());
      log4jLogger.info("The status in VCA_OPTIVA_TO_WERCS for Product " + pb.getFProduct() + " was updated to a " + status);
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

  public void populateLogDetails(ProductBean pb, String error)
  {
    Statement stmt = null;
    try
    {
      log4jLogger.info(error);
      StringBuffer sql = new StringBuffer();
      sql.append("update VCA_OPTIVA_TO_WERCS set LOG_DETAILS = LOG_DETAILS || '");
      sql.append(error);
      sql.append("  ', DATE_MODIFIED = SYSDATE where id = ");
      sql.append(pb.getId());

      ConnectionBean cb = getFromConn().get(0);
      stmt = cb.getConnection().createStatement();
      stmt.executeUpdate(sql.toString());
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

  public void cleanUpStatuses(ArrayList productList)
  {
    if (productList != null && productList.size() > 0)
    {
      Statement stmt = null;
      try
      {
        StringBuffer sql = new StringBuffer();
        sql.append("UPDATE VCA_OPTIVA_TO_WERCS SET STATUS = 0 WHERE STATUS = 1 AND F_PRODUCT IN (");

        Iterator i = productList.iterator();

        while (i.hasNext())
        {
          ProductBean pb = (ProductBean) i.next();
          sql.append("'");
          sql.append(pb.getFProduct());
          sql.append("',");
        }
        sql = new StringBuffer(sql.substring(0, sql.length() - 1));
        sql.append(")");

        ConnectionBean cb = getFromConn().get(0);
        stmt = cb.getConnection().createStatement();
        stmt.executeUpdate(sql.toString());
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
  }

  public static void setAxc(ActiveXComponent newAxc)
  {
    axc = newAxc;
  }
}
