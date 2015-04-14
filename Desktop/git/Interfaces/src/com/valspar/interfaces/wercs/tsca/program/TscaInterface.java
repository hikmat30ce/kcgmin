package com.valspar.interfaces.wercs.tsca.program;

import com.valspar.interfaces.common.BaseInterface;
import com.valspar.interfaces.common.beans.ConnectionAccessBean;
import com.valspar.interfaces.common.enums.DataSource;
import com.valspar.interfaces.common.utils.*;
import com.valspar.interfaces.wercs.tsca.beans.TscaBean;
import java.sql.*;
import java.util.*;
import oracle.jdbc.*;
import org.apache.commons.lang3.StringUtils;
import org.apache.log4j.Logger;

public class TscaInterface extends BaseInterface
{
  private OracleConnection wercsConn = null;
  private static Logger log4jLogger = Logger.getLogger(TscaInterface.class);

  public TscaInterface()
  {
  }

  public void execute()
  {
    try
    {
      log4jLogger.info("TSCA Interface Started");
      setWercsConn((OracleConnection) ConnectionAccessBean.getConnection(DataSource.WERCS));
      log4jLogger.info("Connected to " + ConnectionUtility.buildDatabaseName(getWercsConn()));
      log4jLogger.info("Selecting TSCA Items");
      processTSCA();
      log4jLogger.info("TSCA Interface Complete!");
    }
    catch (Exception e)
    {
      log4jLogger.error(e);
    }
    finally
    {
      JDBCUtil.close(getWercsConn());
    }
  }

  public void processTSCA()
  {
    try
    {
      for (TscaBean tscaBean: buildTscaBeanList())
      {
        log4jLogger.info("New Component: " + tscaBean.getComponentId() + ", Data Code: " + tscaBean.getDataCode());
        if (!isLetterSent(tscaBean))
        {
          if (isComponentExist(tscaBean, "TSCA1203") || isComponentExist(tscaBean, "TSCA1204"))
          {
            printLetter(tscaBean);
          }
        }
        updateTSCA(tscaBean);
      }
    }
    catch (Exception e)
    {
      log4jLogger.error(e);
    }
  }

  private List<TscaBean> buildTscaBeanList()
  {
    OraclePreparedStatement pst = null;
    ResultSet rs = null;
    List<TscaBean> ar = new ArrayList<TscaBean>();
    StringBuilder sb = new StringBuilder();
    sb.append("SELECT a.id, nvl(c.f_country_name, ' '), ");
    sb.append("a.alias, to_char(a.date_requested, 'DD-MON-YYYY'), ");
    sb.append("to_char(to_date(a.date_requested)+1, 'DD-MON-YYYY'), ");
    sb.append("nvl(b.f_company_name, ' '), nvl(b.f_address, ' '), ");
    sb.append("nvl(b.f_city, ' '), nvl(b.f_state, ' '), nvl(b.f_zip_code, ' '), ");
    sb.append("nvl(d.f_product_name, ' '), ");
    sb.append("e.f_component_id, e.f_cas_number, nvl(e.f_chem_name, ' '), ");
    sb.append("f.f_data_code, f.f_text_code, ");
    sb.append("e.f_percent, ");
    sb.append("e.f_product ");
    sb.append("FROM t_product_alias_names g, ");
    sb.append("t_comp_text f, ");
    sb.append("t_prod_comp e, ");
    sb.append("t_products  d, ");
    sb.append("t_countries c, ");
    sb.append("t_plant_defaults b, ");
    sb.append("va_tsca_dsl a ");
    sb.append("WHERE  f.f_data_code = 'TSCA12' ");
    sb.append("AND    f.f_text_code in ('TSCA1201', 'TSCA1202') ");
    sb.append("AND    a.alias = g.f_alias ");
    sb.append("AND    f.f_component_id = e.f_component_id ");
    sb.append("AND    g.f_product = e.f_product ");
    sb.append("AND    g.f_product = d.f_product ");
    sb.append("AND    a.country_shipped_to = c.f_country_code ");
    sb.append("AND    a.plant = b.f_plant ");
    sb.append("AND    a.tsca_processed = 0 ");
    sb.append("AND    a.error_code = 0 ");

    try
    {
      pst = (OraclePreparedStatement) getWercsConn().prepareStatement(sb.toString());
      rs = pst.executeQuery();
      while (rs.next())
      {
        TscaBean tscaBean = new TscaBean();
        tscaBean.setVaTscaDslId(rs.getString(1));
        tscaBean.setCountryShippedTo(rs.getString(2));
        tscaBean.setAlias(rs.getString(3));
        tscaBean.setDateRequested(rs.getString(4));
        tscaBean.setExportDate(rs.getString(5));
        tscaBean.setCompanyName(rs.getString(6));
        tscaBean.setAddress(rs.getString(7));
        tscaBean.setCity(rs.getString(8));
        tscaBean.setState(rs.getString(9));
        tscaBean.setZip(rs.getString(10));
        tscaBean.setTradeName(rs.getString(11));
        tscaBean.setComponentId(rs.getString(12));
        tscaBean.setCasNumber(rs.getString(13));
        tscaBean.setChemName(rs.getString(14));
        tscaBean.setDataCode(rs.getString(15));
        tscaBean.setSection(rs.getString(16));
        tscaBean.setChemPercent(rs.getString(17));
        tscaBean.setProductId(rs.getString(18));
        ar.add(tscaBean);
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
    return ar;
  }

  public boolean isLetterSent(TscaBean tscaBean)
  {
    OraclePreparedStatement pst = null;
    ResultSet rs = null;
    boolean existFlag = false;
    StringBuilder sb = new StringBuilder();
    sb.append("SELECT 'X' FROM va_doc_hist WHERE  cas = :CAS_NUMBER ");
    sb.append("AND country_shipped_to = :COUNTRY_SHIP_TO ");
    if (StringUtils.equalsIgnoreCase(tscaBean.getSection(), "TSCA1201"))
    {
      sb.append("AND  to_char(notification_date, 'YYYY') = to_char(sysdate,'YYYY') ");
    }
    try
    {
      log4jLogger.info("New Component: " + tscaBean.getComponentId() + ", Data Code: " + tscaBean.getDataCode());
      pst = (OraclePreparedStatement) getWercsConn().prepareStatement(sb.toString());
      pst.setStringAtName("CAS_NUMBER", tscaBean.getCasNumber());
      pst.setStringAtName("COUNTRY_SHIP_TO", tscaBean.getCountryShippedTo());
      rs = pst.executeQuery();
      if (rs.next())
      {
        existFlag = true;
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

  private boolean isComponentExist(TscaBean tscaBean, String textCode)
  {
    OraclePreparedStatement pst = null;
    ResultSet rs = null;
    boolean componentFlag = false;
    StringBuilder sb = new StringBuilder();
    sb.append("select a.f_component_id from t_prod_comp a, t_comp_text b where a.f_component_id = b.f_component_id ");
    sb.append("and a.f_component_id = :COMPID and a.f_product = :PRODUCT and b.f_text_code = :TEXTCODE ");

    try
    {
      pst = (OraclePreparedStatement) getWercsConn().prepareStatement(sb.toString());
      pst.setStringAtName("COMPID", tscaBean.getComponentId());
      pst.setStringAtName("PRODUCT", tscaBean.getProductId());
      pst.setStringAtName("TEXTCODE", textCode);
      rs = pst.executeQuery();
      if (rs.next())
      {
        if ((StringUtils.equalsIgnoreCase(textCode, "TSCA1203") && Double.parseDouble(tscaBean.getChemPercent()) >= 1.0) || (StringUtils.equalsIgnoreCase(textCode, "TSCA1204") && Double.parseDouble(tscaBean.getChemPercent()) >= 0.1))
        {
          componentFlag = true;
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
    return componentFlag;
  }

  public void printLetter(TscaBean tscaBean)
  {
    String letter = findLetterText();
    letter = letter.replaceAll("%CAS%", tscaBean.getCasNumber());
    letter = letter.replaceAll("%CHEM NAME%", tscaBean.getChemName());
    letter = letter.replaceAll("%TRADE NAME%", tscaBean.getTradeName());
    letter = letter.replaceAll("%PRODUCT%", tscaBean.getProductId());
    letter = letter.replaceAll("%COMPANY NAME%", tscaBean.getCompanyName());
    letter = letter.replaceAll("%ADDRESS1%", tscaBean.getAddress());
    letter = letter.replaceAll("%ADDRESS2%", tscaBean.getCity() + ", " + tscaBean.getState() + " " + tscaBean.getZip());
    letter = letter.replaceAll("%COUNTRY%", tscaBean.getCountryShippedTo());
    letter = letter.replaceAll("%DATE%", tscaBean.getDateRequested());
    letter = letter.replaceAll("%TSCA12A%", tscaBean.getSection());

    log4jLogger.info("Letter Sent, component: " + tscaBean.getComponentId());
    insertVaDocHist(tscaBean);
    try
    {

      EmailBean.emailMessage("TSCA12B Export Notification Letter for Cas Number: " + tscaBean.getCasNumber() + " Chem Name: " + tscaBean.getChemName() + " Component Id: " + tscaBean.getComponentId() + " Section: " + tscaBean.getSection(), letter, this.getNotificationEmail());
    }
    catch (Exception e)
    {
      log4jLogger.error(e);
    }
  }

  private void updateTSCA(TscaBean tscaBean)
  {
    OraclePreparedStatement pst = null;
    try
    {
      pst = (OraclePreparedStatement) getWercsConn().prepareStatement("UPDATE va_tsca_dsl SET tsca_processed = 1 WHERE  id = :ID");
      pst.setStringAtName("ID", tscaBean.getVaTscaDslId());
      pst.executeUpdate();
      log4jLogger.info("setting tsca_processed = 1 for id = " + tscaBean.getVaTscaDslId());
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

  public void insertVaDocHist(TscaBean tscaBean)
  {
    OraclePreparedStatement pst = null;
    StringBuilder sb = new StringBuilder();
    sb.append("INSERT INTO VA_DOC_HIST ");
    sb.append("(DOC_HIST_ID, REFERENCE_CODE, NOTIFICATION_DATE, CAS, RM_CODE, CHEM_NAME, COUNTRY_SHIPPED_TO, ");
    sb.append("  SHIP_DATE, SECTION, DATE_ADDED, DATE_MODIFIED, ADDED_BY, MODIFIED_BY, DELETE_MARK) ");
    sb.append("VALUES (VA_DOC_HIST_SEQ.NEXTVAL, 'tsca12bprog', SYSDATE, :casNumber, :componentId, :chemName, ");
    sb.append(":countryShippedTo, TO_DATE(:dateRequested,'DD-MON-YYYY'), :section,  ");
    sb.append("SYSDATE, SYSDATE, 'TSCA', 'TSCA', 0 )");
    try
    {
      pst = (OraclePreparedStatement) getWercsConn().prepareStatement(sb.toString());
      pst.setStringAtName("casNumber", tscaBean.getCasNumber());
      pst.setStringAtName("componentId", tscaBean.getComponentId());
      pst.setStringAtName("chemName", tscaBean.getChemName());
      pst.setStringAtName("countryShippedTo", tscaBean.getCountryShippedTo());
      pst.setStringAtName("dateRequested", tscaBean.getDateRequested());
      pst.setStringAtName("section", tscaBean.getSection());
      pst.executeUpdate();
      log4jLogger.info("Insert into VA_DOC_HIST , component: " + tscaBean.getComponentId());
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

  public String findLetterText()
  {
    OraclePreparedStatement pst = null;
    ResultSet rs = null;
    String letterText = null;
    StringBuilder sb = new StringBuilder();
    sb.append("select b.f_phrase from t_phrase_linkage a,t_phrase_translations b ");
    sb.append("where a.f_phrase_id = b.f_phrase_id ");
    sb.append("and a.f_text_code = 'VALLTR01' and b.f_language = 'EN' ");

    try
    {
      pst = (OraclePreparedStatement) getWercsConn().prepareStatement(sb.toString());
      rs = pst.executeQuery();
      if (rs.next())
      {
        letterText = rs.getString(1);
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
    return letterText;
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
