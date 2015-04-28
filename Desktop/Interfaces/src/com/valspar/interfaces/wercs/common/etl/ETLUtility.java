package com.valspar.interfaces.wercs.common.etl;

import com.valspar.interfaces.common.Constants;
import com.valspar.interfaces.common.beans.ConnectionAccessBean;
import com.valspar.interfaces.common.enums.DataSource;
import com.valspar.interfaces.common.hibernate.HibernateUtil;
import com.valspar.interfaces.wercs.common.etl.beans.IaliasBean;
import com.valspar.interfaces.wercs.common.etl.beans.IattributeBean;
import com.valspar.interfaces.wercs.common.etl.beans.IformulationBean;
import com.valspar.interfaces.wercs.common.etl.beans.IprocessBean;
import com.valspar.interfaces.wercs.common.etl.beans.IproductBean;
import com.valspar.interfaces.common.utils.JDBCUtil;
import com.valspar.interfaces.common.utils.LanguageUtility;
import com.valspar.interfaces.wercs.common.beans.BaseProductBean;
import com.valspar.interfaces.wercs.common.utils.WercsUtility;
import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import oracle.jdbc.OracleConnection;
import oracle.jdbc.OraclePreparedStatement;
import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.hibernate.Session;

public class ETLUtility implements Constants
{

  private static Logger log4jLogger = Logger.getLogger(ETLUtility.class);

  public ETLUtility()
  {
  }

  public static void submitIprocess(IprocessBean iProcessBean)
  {
    Session session = HibernateUtil.getHibernateSessionAndBeginTransaction(DataSource.WERCS);
    iProcessBean.setDateStampInserted(new Date());
    session.save(iProcessBean);
    HibernateUtil.closeHibernateSessionAndCommitTransaction(session);
  }

  public static void submitIprocessJDBC(Connection conn, ArrayList list)
  {
    for (IprocessBean iProcessBean: (ArrayList<IprocessBean>) list)
    {
      if (iProcessBean.getIProducts() != null && !iProcessBean.getIProducts().isEmpty())
      {
        insertIproduct(conn, iProcessBean.getIProducts());
      }
      if (iProcessBean.getIAliases() != null && !iProcessBean.getIAliases().isEmpty())
      {
        insertIalias(conn, iProcessBean.getIAliases());
      }
      if (iProcessBean.getIAttributes() != null && !iProcessBean.getIAttributes().isEmpty())
      {
        insertIattribute(conn, iProcessBean.getIAttributes());
      }
      if (iProcessBean.getIFormulations() != null && !iProcessBean.getIFormulations().isEmpty())
      {
        insertIformulation(conn, iProcessBean.getIFormulations());
      }
      insertIprocess(conn, iProcessBean);
    }
  }

  public static void insertIprocess(Connection conn, IprocessBean iProcessBean)
  {
    PreparedStatement pst = null;
    StringBuilder sb = new StringBuilder();
    sb.append("INSERT INTO i_process (f_job_id, ");
    sb.append("                       f_process_group_id, ");
    sb.append("                       f_product, ");
    sb.append("                       f_cas_number, ");
    sb.append("                       f_component_id, ");
    sb.append("                       f_languages, ");
    sb.append("                       f_format, ");
    sb.append("                       f_subformat, ");
    sb.append("                       f_plant, ");
    sb.append("                       f_authorization, ");
    sb.append("                       f_suppliers, ");
    sb.append("                       f_revision_type, ");
    sb.append("                       f_priority, ");
    sb.append("                       f_status, ");
    sb.append("                       f_user_inserted, ");
    sb.append("                       f_date_stamp_inserted) ");
    sb.append("     VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,SYSDATE+nvl(?,0))");

    try
    {
      pst = conn.prepareStatement(sb.toString());
      pst.setString(1, iProcessBean.getJobId());
      pst.setLong(2, iProcessBean.getProcessGroupId());
      pst.setString(3, iProcessBean.getProduct());
      pst.setString(4, iProcessBean.getCasNumber());
      pst.setString(5, iProcessBean.getComponentId());
      pst.setString(6, iProcessBean.getLanguages());
      pst.setString(7, iProcessBean.getFormat());
      pst.setString(8, iProcessBean.getSubformat());
      pst.setString(9, iProcessBean.getPlant());
      pst.setString(10, iProcessBean.getAuthorization());
      pst.setString(11, iProcessBean.getSuppliers());
      pst.setString(12, iProcessBean.getRevisionType());
      pst.setLong(13, iProcessBean.getPriority());
      pst.setLong(14, iProcessBean.getStatus());
      pst.setString(15, iProcessBean.getUserInserted());
      pst.setString(16, iProcessBean.getDateStampDelay());
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

  public static void insertIproduct(Connection conn, Set<IproductBean> productBeans)
  {
    PreparedStatement pst = null;
    StringBuilder sb = new StringBuilder();
    sb.append("INSERT INTO i_products (f_job_id, ");
    sb.append("                        f_product, ");
    sb.append("                        f_product_name, ");
    sb.append("                        f_direction, ");
    sb.append("                        f_status, ");
    sb.append("                        f_user_inserted, ");
    sb.append("                        f_date_stamp_inserted) ");
    sb.append("     VALUES (?,?,?,?,?,?,SYSDATE)");

    try
    {
      pst = conn.prepareStatement(sb.toString());
      for (IproductBean ipBean: productBeans)
      {
        pst.setString(1, ipBean.getJobId());
        pst.setString(2, ipBean.getProduct());
        pst.setString(3, ipBean.getProductName());
        pst.setString(4, ipBean.getDirection());
        pst.setLong(5, ipBean.getStatus());
        pst.setString(6, ipBean.getUserInserted());
        pst.addBatch();
      }
      pst.executeBatch();
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

  public static void insertIalias(Connection conn, Set<IaliasBean> aliasBeans)
  {
    PreparedStatement pst = null;
    StringBuilder sb = new StringBuilder();
    sb.append("INSERT INTO i_aliases (f_job_id, ");
    sb.append("                        f_product, ");
    sb.append("                        f_alias, ");
    sb.append("                        f_alias_name, ");
    sb.append("                        f_language, ");
    sb.append("                        f_direction, ");
    sb.append("                        f_status, ");
    sb.append("                        f_user_inserted, ");
    sb.append("                        f_date_stamp_inserted) ");
    sb.append("     VALUES (?,?,?,?,?,?,?,?,SYSDATE)");

    try
    {
      pst = conn.prepareStatement(sb.toString());
      for (IaliasBean iaBean: (ArrayList<IaliasBean>) aliasBeans)
      {
        pst.setString(1, iaBean.getJobId());
        pst.setString(2, iaBean.getProduct());
        pst.setString(3, iaBean.getAlias());
        pst.setString(4, iaBean.getAliasName());
        pst.setString(5, iaBean.getLanguage());
        pst.setString(6, iaBean.getDirection());
        pst.setLong(7, iaBean.getStatus());
        pst.setString(8, iaBean.getUserInserted());
        pst.addBatch();
      }
      pst.executeBatch();
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

  public static void insertIformulation(Connection conn, Set<IformulationBean> formulationBeans)
  {
    OraclePreparedStatement pst = null;
    StringBuilder sb = new StringBuilder();
    sb.append("INSERT INTO i_formulations (f_job_id, ");
    sb.append("                            f_product, ");
    sb.append("                            f_input_product, ");
    sb.append("                            f_cas_number, ");
    sb.append("                            f_component_id, ");
    sb.append("                            f_trade_secret_name, ");
    sb.append("                            f_trade_secret_flag, ");
    sb.append("                            f_percentage, ");
    sb.append("                            f_percent_range, ");
    sb.append("                            f_product_uom, ");
    sb.append("                            f_line_uom, ");
    sb.append("                            f_line_quantity, ");
    sb.append("                            f_product_quantity, ");
    sb.append("                            f_hazard_flag, ");
    sb.append("                            f_model, ");
    sb.append("                            f_model_desc, ");
    sb.append("                            f_order, ");
    sb.append("                            f_direction, ");
    sb.append("                            f_status, ");
    sb.append("                            f_user_inserted, ");
    sb.append("                            f_date_stamp_inserted) ");
    sb.append("     VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,SYSDATE)");

    try
    {
      pst = (OraclePreparedStatement) conn.prepareStatement(sb.toString());
      for (IformulationBean ifBean: formulationBeans)
      {
        pst.setString(1, ifBean.getJobId());
        pst.setString(2, ifBean.getProduct());
        pst.setString(3, ifBean.getInputProduct());
        pst.setString(4, ifBean.getCasNumber());
        pst.setString(5, ifBean.getComponentId());
        pst.setString(6, ifBean.getTradeSecretName());
        pst.setString(7, ifBean.getTradeSecretFlag());
        pst.setBigDecimal(8, ifBean.getPercentage());
        pst.setString(9, ifBean.getPercentRange());
        pst.setString(10, ifBean.getProductUom());
        pst.setString(11, ifBean.getLineUom());
        pst.setBigDecimal(12, ifBean.getLineQuantity());
        pst.setBigDecimal(13, ifBean.getProductQuantity());
        pst.setBigDecimal(14, ifBean.getHazardFlag());
        pst.setString(15, ifBean.getModel());
        pst.setString(16, ifBean.getModelDesc());
        pst.setDATE(17, JDBCUtil.getDATE(ifBean.getOrder()));
        pst.setString(18, ifBean.getDirection());
        pst.setLong(19, ifBean.getStatus());
        pst.setString(20, ifBean.getUserInserted());
        pst.addBatch();
      }
      pst.executeBatch();
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

  public static void insertIattribute(Connection conn, Set<IattributeBean> attributeBeans)
  {
    PreparedStatement pst = null;

    StringBuilder sb = new StringBuilder();
    sb.append("INSERT INTO i_attributes (f_job_id, ");
    sb.append("                          f_prod_alias_comp, ");
    sb.append("                          f_product, ");
    sb.append("                          f_cas_number, ");
    sb.append("                          f_component_id, ");
    sb.append("                          f_format, ");
    sb.append("                          f_subformat, ");
    sb.append("                          f_usage, ");
    sb.append("                          f_data_code, ");
    sb.append("                          f_data, ");
    sb.append("                          f_text_code, ");
    sb.append("                          f_b_text_line, ");
    sb.append("                          f_l_text_line, ");
    sb.append("                          f_language, ");
    sb.append("                          f_rep_dataset, ");
    sb.append("                          f_rep_sequence, ");
    sb.append("                          f_delete_flag, ");
    sb.append("                          f_user_to_apply, ");
    sb.append("                          f_direction, ");
    sb.append("                          f_order, ");
    sb.append("                          f_status, ");
    sb.append("                          f_user_inserted, ");
    sb.append("                          f_date_stamp_inserted) ");
    sb.append("     VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,SYSDATE)");

    try
    {
      pst = conn.prepareStatement(sb.toString());
      for (IattributeBean iaBean: attributeBeans)
      {
        pst.setString(1, iaBean.getJobId());
        pst.setString(2, iaBean.getProdAliasComp());
        pst.setString(3, iaBean.getProduct());
        pst.setString(4, iaBean.getCasNumber());
        pst.setString(5, iaBean.getComponentId());
        pst.setString(6, iaBean.getFormat());
        pst.setString(7, iaBean.getSubformat());
        pst.setString(8, iaBean.getUsage());
        pst.setString(9, iaBean.getDataCode());
        pst.setString(10, iaBean.getData());
        pst.setString(11, iaBean.getTextCode());
        pst.setString(12, iaBean.getBTextLine());
        pst.setString(13, iaBean.getLTextLine());
        pst.setString(14, iaBean.getLanguage());
        pst.setLong(15, iaBean.getRepDataSet());
        pst.setLong(16, iaBean.getRepSequence());
        pst.setLong(17, iaBean.getDeleteFlag());
        pst.setString(18, iaBean.getUserToApply());
        pst.setString(19, iaBean.getDirection());
        pst.setLong(20, iaBean.getOrder());
        pst.setLong(21, iaBean.getStatus());
        pst.setString(22, iaBean.getUserInserted());
        pst.addBatch();
      }
      pst.executeBatch();
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

  public static IprocessBean createIprocessBean(String userInserted, Long processGroupId, Long status, Long priority, String jobId, String product, String dateStampDelay)
  {
    IprocessBean iProcessBean = new IprocessBean();
    iProcessBean.setJobId(jobId);
    iProcessBean.setProcessGroupId(processGroupId);
    iProcessBean.setLanguages("EN");
    iProcessBean.setFormat("MTR");
    iProcessBean.setSubformat("DATA");
    iProcessBean.setPlant("WERCS");
    iProcessBean.setPriority(priority);
    iProcessBean.setStatus(status);
    iProcessBean.setUserInserted(userInserted);
    if (dateStampDelay != null)
    {
      iProcessBean.setDateStampDelay(dateStampDelay);
    }
    if (product != null)
    {
      iProcessBean.setProduct(product);
    }
    return iProcessBean;
  }

  public static IprocessBean createIprocessPublishingBean(String product, String format, String subformat, String languages, String userInserted, Long priority, Long status, String jobId)
  {
    IprocessBean iProcessBean = new IprocessBean();
    iProcessBean.setJobId(jobId);
    iProcessBean.setProcessGroupId(new Long(3));
    iProcessBean.setProduct(product);
    iProcessBean.setLanguages(languages);
    iProcessBean.setFormat(format);
    iProcessBean.setPlant(WercsUtility.getWercsFPlant(languages, format, subformat));
    //iProcessBean.setPlant("WERCS");
    iProcessBean.setRevisionType("1");
    iProcessBean.setSubformat(subformat);
    iProcessBean.setPriority(priority);
    iProcessBean.setStatus(status);
    iProcessBean.setAuthorization("3");
    iProcessBean.setUserInserted(userInserted);
    return iProcessBean;
  }

  public static Set<IproductBean> populateIproductBean(BaseProductBean pb, String jobId, String userInserted)
  {
    Set<IproductBean> beanList = new HashSet<IproductBean>();
    IproductBean iproductBean = new IproductBean();
    iproductBean.setJobId(jobId);
    iproductBean.setProduct(pb.getFProduct());
    iproductBean.setProductName(pb.getFProductName());
    iproductBean.setDirection("I");
    iproductBean.setStatus(new Long(0));
    iproductBean.setUserInserted(userInserted);
    iproductBean.setDateStampInserted(new Date());
    beanList.add(iproductBean);
    return beanList;
  }

  public static Set<IaliasBean> populateIaliasBean(BaseProductBean pb, String jobId, String userInserted)
  {
    Set<IaliasBean> beanList = new HashSet<IaliasBean>();

    for (String language: pb.getDescriptionLanguages())
    {
      IaliasBean ialiasBean = new IaliasBean();
      ialiasBean.setJobId(jobId);
      ialiasBean.setProduct(pb.getFProduct());
      ialiasBean.setAlias(pb.getFProduct());
      ialiasBean.setAliasName(LanguageUtility.convertDataFromWindowsEncoding(pb.getFProductName(), language));
      ialiasBean.setLanguage(language);
      ialiasBean.setDirection("I");
      ialiasBean.setStatus(new Long("0"));
      ialiasBean.setUserInserted(userInserted);
      ialiasBean.setDateStampInserted(new Date());
      beanList.add(ialiasBean);
    }
    return beanList;
  }

  public static Set<IformulationBean> populateIformulationBeans(OracleConnection wercsConn, BaseProductBean pb, String jobId, String userInserted)
  {
    Set<IformulationBean> beanList = new HashSet<IformulationBean>();
    Statement stmt = null;
    ResultSet rs = null;
    try
    {
      StringBuilder sb = new StringBuilder();

      sb.append("select b.f_product, ROUND(SUM(a.material_pct), 4) ");
      sb.append("from fsformulaingred@TOFM A,  T_PRODUCT_ALIAS_NAMES B ");
      sb.append("where a.item_code = b.f_alias ");
      sb.append("and   A.material_pct > 0 ");
      sb.append("and   A.formula_id = ");
      sb.append(pb.getFormulaId());
      sb.append("group by b.f_product ");

      stmt = wercsConn.createStatement();

      rs = stmt.executeQuery(sb.toString());
      while (rs.next())
      {
        IformulationBean iFormulationBean = new IformulationBean();
        iFormulationBean.setJobId(jobId);
        iFormulationBean.setProduct(pb.getFProduct());
        iFormulationBean.setInputProduct(rs.getString(1));
        iFormulationBean.setPercentage(new BigDecimal(rs.getString(2)));
        iFormulationBean.setDirection("I");
        iFormulationBean.setStatus(new Long(0));
        iFormulationBean.setUserInserted(userInserted);
        iFormulationBean.setDateStampInserted(new Date());
        beanList.add(iFormulationBean);
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
    return beanList;
  }

  public static Set<IattributeBean> populateIattributeBeans(BaseProductBean pb, String jobId, String userInserted, boolean deleteExisting)
  {
    Set<IattributeBean> beanList = new HashSet<IattributeBean>();

    if (deleteExisting)
    {
      OracleConnection wercsConn = (OracleConnection) ConnectionAccessBean.getConnection(DataSource.WERCS);
      OraclePreparedStatement pst = null;
      ResultSet rs = null;
      try
      {
        StringBuilder sb = new StringBuilder();
        sb.append("select 'PVAL',f_data_code, f_data ");
        sb.append("from t_prod_data ");
        sb.append("where F_PRODUCT = :product  ");
        sb.append("UNION ");
        sb.append("select 'PTXT', F_DATA_CODE, F_TEXT_CODE ");
        sb.append("from t_prod_text ");
        sb.append("where F_PRODUCT = :product ");

        pst = (OraclePreparedStatement) wercsConn.prepareStatement(sb.toString());
        pst.setStringAtName("product", pb.getFProduct());
        rs = pst.executeQuery();
        while (rs.next())
        {
          String dataCode = rs.getString(2);
          if (!pb.getDataCodes().containsKey(dataCode)) //only delete if not being updated by optivatowercs
          {
            IattributeBean iAttributeBean = new IattributeBean();
            iAttributeBean.setJobId(jobId);
            iAttributeBean.setProdAliasComp("P");
            iAttributeBean.setProduct(pb.getFProduct());
            iAttributeBean.setUsage(rs.getString(1));
            iAttributeBean.setDataCode(dataCode);
            if (StringUtils.equalsIgnoreCase(rs.getString(1), "PVAL"))
            {
              iAttributeBean.setData(rs.getString(3));
            }
            else
            {
              iAttributeBean.setTextCode(rs.getString(3));
            }
            iAttributeBean.setDirection("I");
            iAttributeBean.setDeleteFlag(new Long(1));
            iAttributeBean.setStatus(new Long(0));
            iAttributeBean.setUserInserted(userInserted);
            iAttributeBean.setDateStampInserted(new Date());
            beanList.add(iAttributeBean);
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
        JDBCUtil.close(wercsConn);
      }
    }

    Set dataSet = pb.getDataCodes().entrySet();
    Iterator iData = dataSet.iterator();
    while (iData.hasNext())
    {
      Map.Entry entry = (Map.Entry) iData.next();
      IattributeBean iAttributeBean = new IattributeBean();
      iAttributeBean.setJobId(jobId);
      iAttributeBean.setProdAliasComp("P");
      iAttributeBean.setProduct(pb.getFProduct());
      iAttributeBean.setUsage("PVAL");
      iAttributeBean.setDataCode((String) entry.getKey());
      iAttributeBean.setData((String) entry.getValue());
      iAttributeBean.setDirection("I");
      iAttributeBean.setStatus(new Long(0));
      iAttributeBean.setUserInserted(userInserted);
      iAttributeBean.setDateStampInserted(new Date());
      beanList.add(iAttributeBean);
    }

    Set textSet = pb.getTextCodes().entrySet();
    Iterator iText = textSet.iterator();
    while (iText.hasNext())
    {
      Map.Entry entry = (Map.Entry) iText.next();
      IattributeBean iAttributeBean = new IattributeBean();
      iAttributeBean.setJobId(jobId);
      iAttributeBean.setProdAliasComp("P");
      iAttributeBean.setProduct(pb.getFProduct());
      iAttributeBean.setUsage("PTXT");
      iAttributeBean.setDataCode((String) entry.getKey());
      iAttributeBean.setTextCode((String) entry.getValue());
      iAttributeBean.setFormat("MTR");
      iAttributeBean.setDirection("I");
      iAttributeBean.setStatus(new Long(0));
      iAttributeBean.setUserInserted(userInserted);
      iAttributeBean.setDateStampInserted(new Date());
      beanList.add(iAttributeBean);
    }
    return beanList;
  }

/*  public static void populatePublishingData(IprocessBean iProcessBean, BaseProductBean productBean)
  {
    OracleConnection wercsConn = (OracleConnection) ConnectionAccessBean.getConnection(DataSource.WERCS);
    OraclePreparedStatement pst = null;
    ResultSet rs = null;
    StringBuilder publishFormat = new StringBuilder();
    StringBuilder publishSubFormat = new StringBuilder();
    StringBuilder publishLanguage = new StringBuilder();
    StringBuilder publishAuth = new StringBuilder();
    StringBuilder publishPlant = new StringBuilder();
    StringBuilder publishRevisionType = new StringBuilder();

    try
    {
      StringBuilder sb = new StringBuilder();
      sb.append("select distinct FORMAT, SUBFORMAT, LANGUAGE, PLANT  ");
      sb.append("from table(vca_subf_lang_pub_pkg.get_subf_lang_list(:alias, :region, :busgp)) a ");
      sb.append("WHERE   NOT EXISTS ");
      sb.append("                        (SELECT   * ");
      sb.append("                           FROM   i_process ");
      sb.append("                          WHERE       F_PRODUCT = :alias ");
      sb.append("                                  AND F_FORMAT like '%'||a.FORMAT||'%' ");
      sb.append("                                  AND F_SUBFORMAT like '%'||a.SUBFORMAT||'%' ");
      sb.append("                                  AND F_LANGUAGES LIKE '%'||a.LANGUAGE||'%' ");
      sb.append("                                  AND F_STATUS = 0) ");

      pst = (OraclePreparedStatement) wercsConn.prepareStatement(sb.toString());
      pst.setStringAtName("alias", iProcessBean.getProduct());
      pst.setStringAtName("region", productBean.getRegion());
      pst.setStringAtName("busgp", productBean.getBusinessGroup());
      rs = pst.executeQuery();
      while (rs.next())
      {
        if (StringUtils.isNotEmpty(publishFormat.toString()))
        {
          publishFormat.append("|");
          publishSubFormat.append("|");
          publishLanguage.append("|");
          publishAuth.append("|");
          publishPlant.append("|");
          publishRevisionType.append("|");
        }
        publishFormat.append(rs.getString(1));
        publishSubFormat.append(rs.getString(2));
        publishLanguage.append(rs.getString(3));
        publishAuth.append("3");
        publishPlant.append(rs.getString(4));
        publishRevisionType.append("1");
      }

      iProcessBean.setFormat(publishFormat.toString());
      iProcessBean.setSubformat(publishSubFormat.toString());
      iProcessBean.setLanguages(publishLanguage.toString());
      iProcessBean.setAuthorization(publishAuth.toString());
      iProcessBean.setPlant(publishPlant.toString());
      iProcessBean.setRevisionType(publishRevisionType.toString());

    }
    catch (Exception e)
    {
      log4jLogger.error(e);
    }
    finally
    {
      JDBCUtil.close(pst);
      JDBCUtil.close(wercsConn);
    }
  }*/

  public static void populatePublishingData(IprocessBean iProcessBean, BaseProductBean productBean, String alias)
  {
    OracleConnection wercsConn = (OracleConnection) ConnectionAccessBean.getConnection(DataSource.WERCS);
    OraclePreparedStatement pst = null;
    ResultSet rs = null;
    try
    {
      StringBuilder publishFormat = new StringBuilder();
      StringBuilder publishSubFormat = new StringBuilder();
      StringBuilder publishLanguage = new StringBuilder();
      StringBuilder publishAuth = new StringBuilder();
      StringBuilder publishPlant = new StringBuilder();
      StringBuilder publishRevisionType = new StringBuilder();

      StringBuilder sb = new StringBuilder();
      sb.append("select distinct FORMAT, SUBFORMAT, LANGUAGE, PLANT  ");
      sb.append("from table(vca_subf_lang_pub_pkg.get_subf_lang_list(:alias, :region, :busgp)) a ");
    /*  sb.append("WHERE   NOT EXISTS ");
      sb.append("                        (SELECT   * ");
      sb.append("                           FROM   i_process ");
      sb.append("                          WHERE       F_PRODUCT = :alias ");
      sb.append("                                  AND F_FORMAT like '%'||a.FORMAT||'%' ");
      sb.append("                                  AND F_SUBFORMAT like '%'||a.SUBFORMAT||'%' ");
      sb.append("                                  AND F_LANGUAGES LIKE '%'||a.LANGUAGE||'%' ");
      sb.append("                                  AND F_STATUS = 0) ");*/

      pst = (OraclePreparedStatement) wercsConn.prepareStatement(sb.toString());
      if (StringUtils.isEmpty(alias))
      {
        pst.setStringAtName("alias", iProcessBean.getProduct());
      }
      else
      {
        pst.setStringAtName("alias", alias);
      }
      pst.setStringAtName("region", productBean.getRegion());
      pst.setStringAtName("busgp", productBean.getBusinessGroup());
      rs = pst.executeQuery();
      while (rs.next())
      {
        if (StringUtils.isNotEmpty(publishFormat.toString()))
        {
          publishFormat.append("|");
          publishSubFormat.append("|");
          publishLanguage.append("|");
          publishAuth.append("|");
          publishPlant.append("|");
          publishRevisionType.append("|");
        }
        publishFormat.append(rs.getString(1));
        publishSubFormat.append(rs.getString(2));
        publishLanguage.append(rs.getString(3));
        publishAuth.append("3");
        publishPlant.append(rs.getString(4));
        publishRevisionType.append("1");
      }

      iProcessBean.setFormat(publishFormat.toString());
      iProcessBean.setSubformat(publishSubFormat.toString());
      iProcessBean.setLanguages(publishLanguage.toString());
      iProcessBean.setAuthorization(publishAuth.toString());
      iProcessBean.setPlant(publishPlant.toString());
      iProcessBean.setRevisionType(publishRevisionType.toString());

    }
    catch (Exception e)
    {
      log4jLogger.error(e);
    }
    finally
    {
      JDBCUtil.close(pst);
      JDBCUtil.close(wercsConn);
    }
  }

  public static String getNextJobId(Connection conn)
  {
    Statement stmt = null;
    ResultSet rs = null;

    try
    {
      StringBuilder sb = new StringBuilder();
      sb.append("select WERCS.VCA_JOB_ID_SEQ.nextval ");
      sb.append("from dual ");

      stmt = conn.createStatement();
      rs = stmt.executeQuery(sb.toString());
      if (rs.next())
      {
        return rs.getString(1);
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
    return null;
  }

  public static Long getProcessGroupId(Connection conn, String processGroupName)
  {
    PreparedStatement pstmt = null;
    ResultSet rs = null;

    try
    {
      StringBuilder sb = new StringBuilder();
      sb.append("select f_process_group_id ");
      sb.append(" from i_process_groups ");
      sb.append(" where f_process_group_name = ? ");

      pstmt = conn.prepareStatement(sb.toString());
      pstmt.setString(1, processGroupName);
      rs = pstmt.executeQuery();
      if (rs.next())
      {
        return new Long(rs.getString(1));
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
    return null;
  }
}
