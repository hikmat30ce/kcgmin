package com.valspar.interfaces.wercs.optivatowercs.program;

import com.valspar.interfaces.common.*;
import com.valspar.interfaces.common.beans.ConnectionAccessBean;
import com.valspar.interfaces.common.enums.DataSource;
import com.valspar.interfaces.wercs.common.etl.ETLUtility;
import com.valspar.interfaces.wercs.common.utils.WercsUtility;
import com.valspar.interfaces.common.utils.*;
import com.valspar.interfaces.wercs.common.etl.beans.IaliasBean;
import com.valspar.interfaces.wercs.common.etl.beans.IattributeBean;
import com.valspar.interfaces.wercs.common.etl.beans.IformulationBean;
import com.valspar.interfaces.wercs.optivatowercs.beans.ProductBean;
import com.valspar.interfaces.wercs.common.etl.beans.IprocessBean;
import com.valspar.interfaces.wercs.common.etl.beans.IproductBean;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.*;
import oracle.jdbc.OracleConnection;
import oracle.jdbc.OraclePreparedStatement;
import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;

public class OptivaToWercsInterface extends BaseInterface
{
  OracleConnection optivaConn = null;
  OracleConnection wercsConn = null;

  private static Logger log4jLogger = Logger.getLogger(OptivaToWercsInterface.class);

  public OptivaToWercsInterface()
  {
  }

  public void execute()
  {
    try
    {
      setOptivaConn((OracleConnection) ConnectionAccessBean.getConnection(DataSource.FORMULATION));
      setWercsConn((OracleConnection) ConnectionAccessBean.getConnection(DataSource.WERCS));

      List<ProductBean> productList = buildProductBeans();
      if (!productList.isEmpty())
      {
        log4jLogger.info("There are " + productList.size() + " products in the list to be processed.");
        productCompare(productList);
        populateWercs(productList);
      }
      log4jLogger.info("The OptivaToWercsInterface has no more to process.");
    }
    catch (Exception e)
    {
      log4jLogger.error(e);
    }
    finally
    {
      JDBCUtil.close(getOptivaConn());
      JDBCUtil.close(getWercsConn());
    }
  }

  public List<ProductBean> buildProductBeans()
  {
    Statement stmt = null;
    ResultSet rs = null;
    List<ProductBean> productList = new ArrayList<ProductBean>();

    try
    {
      StringBuilder sql = new StringBuilder();
      sql.append("SELECT * FROM ");
      sql.append("(SELECT A.ID, A.FORMULA_CODE, SUBSTR(A.FORMULA_CODE, INSTR(A.FORMULA_CODE, '.', -1, 1)+1, 3) Extension, ");
      sql.append("A.VERSION, A.FORMULA_ID, A.F_PRODUCT, A.F_PRODUCT_NAME, ");
      sql.append("L3.KEYFIELD3 AS FRMCT, B.GROUP_CODE, A.FLASHF, A.FLASHC, A.SET_CODE, A.PRIORITY, NVL(A.BYPASS_COMPARE, 0) BYPASS, A.STATUS, ");
      sql.append("L1.KEYFIELD3 AS BUSGP, L2.KEYFIELD3 AS REGION ");
      sql.append("FROM VCA_OPTIVA_TO_WERCS_6X A, FSFORMULA B, VCA_LOOKUPS_6X L1, VCA_LOOKUPS_6X L2, VCA_LOOKUPS_6X L3 ");
      sql.append("WHERE A.FORMULA_ID = B.FORMULA_ID ");
      sql.append("AND L1.KEYFIELD1 = 'OPTIVA WERCS BUSGP XREF'  ");
      sql.append("AND L1.KEYFIELD2 = B.GROUP_CODE ");
      sql.append("AND L2.KEYFIELD1 = 'OPTIVA WERCS REGION XREF'  ");
      sql.append("AND L2.KEYFIELD2 = B.GROUP_CODE ");
      sql.append("AND L3.KEYFIELD1 = 'OPTIVA FORMULA CLASS XREF'  ");
      sql.append("AND L3.KEYFIELD2 = A.FORMULA_CLASS ");
      sql.append("AND A.STATUS in (0, -50) ");
      sql.append("AND A.DATE_ADDED <= SYSDATE ");
      sql.append("ORDER BY A.PRIORITY, A.DATE_ADDED) ");

      stmt = getOptivaConn().createStatement();
      rs = stmt.executeQuery(sql.toString());

      boolean validProduct;
      while (rs.next())
      {
        validProduct = true;
        ProductBean pb = new ProductBean();
        pb.setId(rs.getString(1));
        System.out.println("id: " + pb.getId());
        pb.setFormulaCode(rs.getString(2));
        pb.setExtension(rs.getString(3));
        pb.setVersion(rs.getString(4));
        pb.setFormulaId(rs.getString(5));
        pb.setFProduct(rs.getString(6));
        pb.setFProductName(rs.getString(7));
        pb.setFormulaClass(rs.getString(8));
        pb.setGroupCode(rs.getString(9));
        pb.setFlashF(rs.getString(10));
        pb.setFlashC(rs.getString(11));
        pb.setSetCode(rs.getString(12));
        pb.setPriority(rs.getString(13));
        pb.setBypassCompare(rs.getInt(14) == 1? true: false);
        pb.setStatus(rs.getString(15));
        pb.setBusinessGroup(rs.getString(16));
        pb.setRegion(rs.getString(17));
        if (StringUtils.equalsIgnoreCase(pb.getGroupCode(), "EURO") && StringUtils.equalsIgnoreCase(pb.getExtension(), "1G0"))
        {
          pb.setRegion("REGION10");
        }
        if (StringUtils.equalsIgnoreCase(pb.getGroupCode(), "EURO") && StringUtils.equalsIgnoreCase(pb.getExtension(), "1F0"))
        {
          pb.setRegion("REGION11");
        }
        pb.setDescriptionLanguages(WercsUtility.getDescriptionLanguages(pb.getRegion()));

        updateQueueStatus(pb, 1);

        pb.setDataCodes(WercsUtility.getProductImportDataAttributes(pb));
        pb.setTextCodes(WercsUtility.getProductImportTextAttributes(pb));

        if (pb.getDataCodes().isEmpty())
        {
          validProduct = false;
          log4jLogger.info(pb.getFProduct() + " formula class '" + pb.getFormulaClass() + "' is not in VCA_OPTIVA_WERCS_MAPPING");
        }
        if (!allComponentsExist(pb))
        {
          validProduct = false;
          log4jLogger.info(pb.getFProduct() + " has components missing in T_PRODUCT_ALIAS_NAMES - product will not be processed");
        }
        if (validProduct)
        {
          productList.add(populateAlias(pb));
          log4jLogger.info(pb.getFProduct() + " was added to the ArrayList to be processed.");
        }
        else
        {
          updateQueueStatus(pb, -99);
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
    return productList;
  }

  public boolean allComponentsExist(ProductBean pb)
  {
    boolean returnValue = true;
    Statement stmt = null;
    ResultSet rs = null;
    try
    {
      StringBuilder sql = new StringBuilder();
      sql.append("select a.item_code from fsformulaingred@TOFM a ");
      sql.append("where a.formula_id = ");
      sql.append(pb.getFormulaId());
      sql.append(" and a.material_pct > 0 ");
      sql.append("and not exists (select * from t_product_alias_names where f_alias = a.item_code) ");

      stmt = getWercsConn().createStatement();
      rs = stmt.executeQuery(sql.toString());
      while (rs.next())
      {
        returnValue = false;
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
    return returnValue;
  }

  public void productCompare(List<ProductBean> productList)
  {
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
          StringBuilder sql = new StringBuilder();
          sql.append("select WERCS_OPTIVA_FORMULA_COMPARE(");
          sql.append(CommonUtility.toVarchar(pb.getFProduct()));
          sql.append(", 'WERC', ");
          sql.append(CommonUtility.toVarchar((String) pb.getDataCodes().get("DENSLB")));
          sql.append(", ");
          sql.append(CommonUtility.toVarchar((String) pb.getDataCodes().get("DENKGL")));
          sql.append(", ");
          sql.append(CommonUtility.toVarchar(pb.getFlashF()));
          sql.append(", ");
          sql.append(CommonUtility.toVarchar(pb.getBusinessGroup()));
          sql.append(", ");
          sql.append(CommonUtility.toVarchar(pb.getFormulaClass()));
          sql.append(", '");
          if (pb.isDataCodeExist("MALV1"))
          {
            sql.append(pb.getDataCodeValue("MALV1"));
          }
          sql.append("', '");
          if (pb.isDataCodeExist("MALV2"))
          {
            sql.append(pb.getDataCodeValue("MALV2"));
          }
          sql.append("', '");
          if (pb.isDataCodeExist("NO_YTAL"))
          {
            sql.append(pb.getDataCodeValue("NO_YTAL"));
          }
          sql.append("', '");
          if (pb.isDataCodeExist("NO_YLGP"))
          {
            sql.append(pb.getDataCodeValue("NO_YLGP"));
          }
          sql.append("', '");

          if (pb.isDataCodeExist("DE_BP31"))
          {
            sql.append(pb.getDataCodeValue("DE_BP31"));
          }
          sql.append("', '");
          if (pb.isDataCodeExist("VISCOST") && pb.getSetCode().equalsIgnoreCase("EURO"))
          {
            sql.append(pb.getDataCodeValue("VISCOST"));
          }
          sql.append("', ");
          sql.append(CommonUtility.toVarchar(pb.getSetCode()));
          sql.append(") from dual");

          log4jLogger.info(sql.toString());
          Statement stmt = null;
          ResultSet rs = null;

          try
          {
            stmt = getWercsConn().createStatement();
            rs = stmt.executeQuery(sql.toString());
            if (rs.next())
            {
              if (rs.getString(1).equalsIgnoreCase("TRUE"))
              {
                pb.setSameProduct(true);
                componentCompare(pb);
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
        if ((pb.getDataCodes().get("DENSLB") == "null") || (pb.getDataCodes().get("DENSLB") == null) || (pb.getDataCodes().get("DENKGL") == "null") || (pb.getDataCodes().get("DENKGL") == null))
        {
          log4jLogger.info("DENSLB and/or DENKGL not found for " + pb.getFProduct() + ". Cannot add to DensitySyncQueue.");
        }
        else
        {
          if (checkDataCodeChange(pb, "DENSLB") || checkDataCodeChange(pb, "DENKGL"))
          {
            log4jLogger.info("DENSLB and/or DENKGL changed for " + pb.getFProduct() + ". Add to DensitySyncQueue.");
            insertIntoDensitySyncQueue(pb);
          }
          else
          {
            log4jLogger.info("DENSLB and/or DENKGL did not change for " + pb.getFProduct() + ". Don't add to DensitySyncQueue.");
          }
        }
      }
    }
    catch (Exception e)
    {
      log4jLogger.error(e);
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
        StringBuilder sql = new StringBuilder();
        sql.append("select decode(ROUND(NVL(");
        sql.append(CommonUtility.toVarchar((String) pb.getDataCodes().get(dataCode)));
        sql.append(" ,0),2), ROUND(NVL(f_data,0),2), 'SAME', 'DIFF')");
        sql.append("from t_prod_data ");
        sql.append("where f_product = ");
        sql.append(CommonUtility.toVarchar(pb.getFProduct()));
        sql.append(" and f_data_code = ");
        sql.append(CommonUtility.toVarchar(dataCode));

        stmt = getWercsConn().createStatement();
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
      StringBuilder sql = new StringBuilder();
      sql.append("INSERT INTO VCA_DENSITY_SYNC_QUEUE ");
      sql.append("SELECT 0, ");
      sql.append(CommonUtility.toVarchar(pb.getFProduct()));
      sql.append(", ");
      sql.append(CommonUtility.toVarchar((String) pb.getDataCodes().get("DENSLB")));
      sql.append(", ");
      sql.append(CommonUtility.toVarchar((String) pb.getDataCodes().get("DENKGL")));
      sql.append(", 'WERC', ");
      sql.append("SYSDATE, NULL, NULL FROM DUAL ");
      sql.append("WHERE NOT EXISTS ");
      sql.append("(SELECT * FROM VCA_DENSITY_SYNC_QUEUE ");
      sql.append("WHERE PRODUCT = ");
      sql.append(CommonUtility.toVarchar(pb.getFProduct()));
      sql.append("  AND DATE_PROCESSED IS NULL) ");

      stmt = getWercsConn().createStatement();
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

    PreparedStatement componentsPstmt = null;
    ResultSet componentsRs = null;
    try
    {
      StringBuilder componentsSql = new StringBuilder();
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

      componentsPstmt = getWercsConn().prepareStatement(componentsSql.toString());
      componentsPstmt.setString(1, pb.getFormulaId());
      componentsRs = componentsPstmt.executeQuery();

      StringBuilder sql = new StringBuilder();
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
          pstmt = getWercsConn().prepareStatement(sql.toString());
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

  public void populateWercs(List<ProductBean> productList)
  {
    try
    {
      for (ProductBean pb: productList)
      {
        IprocessBean iProcessBean = ETLUtility.createIprocessBean(this.getInterfaceName(), ETLUtility.getProcessGroupId(getWercsConn(), "Product Import"), new Long(0), new Long(pb.getPriority()), ETLUtility.getNextJobId(getWercsConn()), pb.getFProduct(), "0", null);
        ArrayList<IprocessBean> translationList = new ArrayList<IprocessBean>();
        log4jLogger.info(iProcessBean.getJobId() + " created for product: " + pb.getFProduct());
        if (pb.isSameProduct())
        {
          if (newDescription(pb))
          {
            iProcessBean.setIProducts(ETLUtility.populateIproductBean(pb, iProcessBean.getJobId(), this.getInterfaceName()));
            iProcessBean.setIAliases(ETLUtility.populateIaliasBean(pb, iProcessBean.getJobId(), this.getInterfaceName()));
            for (String language: pb.getDescriptionLanguages())
            {
              String jobId = ETLUtility.getNextJobId(getWercsConn());
              IprocessBean importAliasProcessBean = ETLUtility.createIprocessBean(this.getInterfaceName(), ETLUtility.getProcessGroupId(getWercsConn(), "Import Aliases"), new Long("0"), new Long("1"), jobId, pb.getFProduct(), null, null);
              IaliasBean ialiasBean = new IaliasBean();
              ialiasBean.setJobId(jobId);
              ialiasBean.setProduct(pb.getFProduct());
              ialiasBean.setAlias(pb.getFProduct());
              ialiasBean.setAliasName(LanguageUtility.convertDataFromWindowsEncoding(pb.getFProductName(), language));
              ialiasBean.setLanguage(language);
              ialiasBean.setDirection("I");
              ialiasBean.setStatus(new Long(0));
              ialiasBean.setUserInserted(this.getInterfaceName());
              ialiasBean.setDateStampInserted(new Date());
              importAliasProcessBean.getIAliases().add(ialiasBean);
              translationList.add(importAliasProcessBean);
            }
          }
          iProcessBean.setIAttributes(ETLUtility.populateIattributeBeans(pb, iProcessBean.getJobId(), this.getInterfaceName(), false));
        }
        else
        {
          iProcessBean.setIProducts(ETLUtility.populateIproductBean(pb, iProcessBean.getJobId(), this.getInterfaceName()));
          iProcessBean.setIAliases(ETLUtility.populateIaliasBean(pb, iProcessBean.getJobId(), this.getInterfaceName()));
          iProcessBean.setIFormulations(ETLUtility.populateIformulationBeans(getWercsConn(), pb, iProcessBean.getJobId(), this.getInterfaceName()));
          iProcessBean.setIAttributes(ETLUtility.populateIattributeBeans(pb, iProcessBean.getJobId(), this.getInterfaceName(), true));
          for (String language: pb.getDescriptionLanguages())
          {
            String jobId = ETLUtility.getNextJobId(getWercsConn());
            IprocessBean importAliasProcessBean = ETLUtility.createIprocessBean(this.getInterfaceName(), ETLUtility.getProcessGroupId(getWercsConn(), "Import Aliases"), new Long("0"), new Long("1"), jobId, pb.getFProduct(), null, null);
            IaliasBean ialiasBean = new IaliasBean();
            ialiasBean.setJobId(jobId);
            ialiasBean.setProduct(pb.getFProduct());
            ialiasBean.setAlias(pb.getFProduct());
            ialiasBean.setAliasName(LanguageUtility.convertDataFromWindowsEncoding(pb.getFProductName(), language));
            ialiasBean.setLanguage(language);
            ialiasBean.setDirection("I");
            ialiasBean.setStatus(new Long(0));
            ialiasBean.setUserInserted(this.getInterfaceName());
            ialiasBean.setDateStampInserted(new Date());
            importAliasProcessBean.getIAliases().add(ialiasBean);
            translationList.add(importAliasProcessBean);
          }
        }
        ETLUtility.populatePublishingData(iProcessBean, pb, null);
        ETLUtility.submitIprocess(iProcessBean);
        for (IprocessBean translationBean: translationList)
        {
          ETLUtility.submitIprocess(translationBean);
        }

        publishingAlias(iProcessBean, pb);

        updateQueueStatus(pb, 2);
      }
    }
    catch (Exception e)
    {
      log4jLogger.error(e);
    }
  }

  private boolean newDescription(ProductBean pb)
  {
    PreparedStatement pstmt = null;
    ResultSet rs = null;
    boolean returnValue = false;
    try
    {
      pstmt = getWercsConn().prepareStatement("SELECT f_product FROM T_PRODUCTS WHERE F_PRODUCT = ? AND F_PRODUCT_NAME = ? ");
      pstmt.setString(1, pb.getFProduct());
      pstmt.setString(2, pb.getFProductName());
      rs = pstmt.executeQuery();
      if (rs.next())
      {
        returnValue = true;
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
    return returnValue;
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

      StringBuilder sql = new StringBuilder();
      sql.append("UPDATE VCA_OPTIVA_TO_WERCS_6X SET STATUS = ");
      sql.append(status);

      if (status == 1)
        sql.append(", START_TIME = SYSDATE, LOG_DETAILS = '' ");
      else
        sql.append(", END_TIME = SYSDATE");

      sql.append(", DATE_MODIFIED = SYSDATE WHERE ID = ");
      sql.append(pb.getId());

      stmt = getOptivaConn().createStatement();
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

  private ProductBean populateAlias(ProductBean productBean)
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
      productBean.setAliasList(aliasList);
    }
    catch (Exception e)
    {
      log4jLogger.error(e);
    }
    finally
    {
      JDBCUtil.close(pst, rs);
    }
    return productBean;
  }

  private void publishingAlias(IprocessBean ipBean, ProductBean productBean)
  {
    ipBean.setProcessGroupId(ETLUtility.getProcessGroupId(getWercsConn(), "ImportPublishDocument"));
    ipBean.setPriority(new Long("1"));
    ipBean.setStatus(new Long("5"));
    ipBean.setJobId(ipBean.getJobId() + "P");
    for (String alias: productBean.getAliasList())
    {
      ipBean.setIAliases(new HashSet<IaliasBean>());
      ipBean.setIProducts(new HashSet<IproductBean>());
      ipBean.setIAttributes(new HashSet<IattributeBean>());
      ipBean.setIFormulations(new HashSet<IformulationBean>());
      ipBean.setProduct(alias);
      ETLUtility.populatePublishingData(ipBean, productBean, alias);
      ETLUtility.submitIprocess(ipBean);
    }
  }

  public void setOptivaConn(OracleConnection optivaConn)
  {
    this.optivaConn = optivaConn;
  }

  public OracleConnection getOptivaConn()
  {
    return optivaConn;
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
