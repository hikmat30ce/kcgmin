package com.valspar.interfaces.wercs.extractfills.program;

import com.valspar.interfaces.common.BaseInterface;
import com.valspar.interfaces.common.beans.ConnectionAccessBean;
import com.valspar.interfaces.common.enums.DataSource;
import com.valspar.interfaces.wercs.common.etl.ETLUtility;
import com.valspar.interfaces.wercs.common.etl.beans.IaliasBean;
import com.valspar.interfaces.wercs.common.etl.beans.IprocessBean;
import com.valspar.interfaces.common.utils.*;
import com.valspar.interfaces.wercs.extractfills.beans.*;
import java.sql.CallableStatement;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
import java.sql.Types;
import java.util.*;
import oracle.jdbc.OracleConnection;
import oracle.jdbc.OraclePreparedStatement;
import org.apache.log4j.Logger;

public class ExtractFillsInterface extends BaseInterface
{
  private OracleConnection wercsConn = null;
  private static Logger log4jLogger = Logger.getLogger(ExtractFillsInterface.class);

  public ExtractFillsInterface()
  {
  }

  public void execute()
  {
    try
    {
      this.setWercsConn((OracleConnection) ConnectionAccessBean.getConnection(DataSource.WERCS));
      List<ItemBean> itemList = new ArrayList<ItemBean>();
      ArrayList<TranslationBean> translationList = new ArrayList<TranslationBean>();
      for (DataSource datasource: CommonUtility.getERPDataSourceList())
      {
        log4jLogger.info("Populating bulks in VCA_WERCS_EXTRACT_FILLS_QUEUE for " + datasource.getDataSourceLabel());
        populateBulks(datasource);
        log4jLogger.info("Building beans for " + datasource.getDataSourceLabel());
        buildBeans(datasource, itemList);
        log4jLogger.info("Starting the Wercs bulk exists check there are " + itemList.size() + " items to check on.");
        checkBulksExistInWercs(itemList);
        log4jLogger.info("Done with the Wercs bulk exists check there are " + itemList.size() + " after the check.");
        log4jLogger.info("Starting the Wercs bulk match check there are " + itemList.size() + " items to check on.");
        checkBulksMatchInWercs(itemList);
        log4jLogger.info("Done with the Wercs bulk match check there are " + itemList.size() + " after the check.");
        checkStripsMatchInWercs(itemList);
        log4jLogger.info("Done with the Wercs stripped alias match check there are " + itemList.size() + " after the check.");
        log4jLogger.info("Building translation beans for " + datasource.getDataSourceLabel());
        buildTranslationBeans(datasource, translationList);
      }

      log4jLogger.info("There are " + itemList.size() + " that will be added");
      for (ItemBean ib: itemList)
      {
        addToWercs(ib);
      }

      log4jLogger.info("There are " + translationList.size() + " that will be added to the translation table");
      if (!translationList.isEmpty())
      {
        for (TranslationBean tb: translationList)
        {
          String jobId = ETLUtility.getNextJobId(this.getWercsConn());
          IprocessBean iProcessBean = ETLUtility.createIprocessBean(this.getInterfaceName(), ETLUtility.getProcessGroupId(getWercsConn(), "Import Aliases"), new Long("0"), new Long("1"), jobId, null, null);
          IaliasBean ialiasBean = new IaliasBean();
          ialiasBean.setJobId(jobId);
          ialiasBean.setProduct(tb.getProduct());
          ialiasBean.setAlias(tb.getAlias());
          ialiasBean.setAliasName(LanguageUtility.convertDataFromWindowsEncoding(tb.getTranslation(), tb.getIsoLanguage()));
          ialiasBean.setLanguage(tb.getIsoLanguage());
          ialiasBean.setDirection("I");
          ialiasBean.setStatus(new Long(0));
          ialiasBean.setUserInserted(this.getInterfaceName());
          ialiasBean.setDateStampInserted(new Date());
          iProcessBean.getIAliases().add(ialiasBean);
          iProcessBean.setProduct(tb.getProduct());
          updateTranslationStatus(tb, 2);
          ETLUtility.submitIprocess(iProcessBean);
        }
      }
    }
    catch (Exception e)
    {
      log4jLogger.error(e);
    }
    finally
    {
      JDBCUtil.close(this.getWercsConn());
    }
  }

  public void populateBulks(DataSource datasource)
  {
    CallableStatement cstmt = null;
    OracleConnection conn = null;
    try
    {
      conn = (OracleConnection) ConnectionAccessBean.getConnection(datasource);
      cstmt = conn.prepareCall("{call apps.vca_wercs_pkg.upd_extract_dtl}");
      log4jLogger.info("Calling vca_wercs_pkg.upd_extract_dtl()");
      cstmt.execute();
    }
    catch (Exception e)
    {
      log4jLogger.error(e);
    }
    finally
    {
      JDBCUtil.close(cstmt);
      JDBCUtil.close(conn);
    }
  }

  public void buildBeans(DataSource datasource, List<ItemBean> ar)
  {
    StringBuilder sql = new StringBuilder();
    sql.append("SELECT a.ID, upper(a.alias), upper(a.product), a.description, a.reason, a.customer,   ");
    sql.append("nvl((SELECT PUBLISH_STRIPPED   ");
    sql.append("          FROM va_publishing_rules@to_rg ");
    sql.append("         WHERE bus_group = WERCS.GET_WERCS_TEXT_CODE@to_rg(a.product, 'BUSGP')),0) as PUBLISH_STRIPPED,  ");
    sql.append("        NVL (SUBSTR (A.ALIAS, 1, INSTR (A.ALIAS, '.', -1) - 1), A.ALIAS) as STRIPPED_ALIAS ");
    sql.append("  FROM VALSPAR.VCA_WERCS_EXTRACT_FILLS_QUEUE a ");
    sql.append(" WHERE a.STATUS = 0 ");

    Statement st = null;
    ResultSet rs = null;
    OracleConnection conn = null;
    try
    {
      conn = (OracleConnection) ConnectionAccessBean.getConnection(datasource);
      st = conn.createStatement();
      rs = st.executeQuery(sql.toString());

      while (rs.next())
      {
        ItemBean ib = new ItemBean();
        ib.setId(rs.getString(1));
        ib.setAlias(rs.getString(2));
        ib.setProduct(getBulk(rs.getString(3)));
        ib.setAliasName(rs.getString(4));
        ib.setReason(rs.getString(5));
        ib.setCustomer(rs.getString(6));
        if (rs.getString(7).equalsIgnoreCase("1"))
        {
          ib.setPublishStripped(true);
        }
        ib.setStrippedAlias(rs.getString(8));
        ib.setDatasource(datasource);
        ar.add(ib);
      }
    }
    catch (Exception e)
    {
      log4jLogger.error(e);
    }
    finally
    {
      JDBCUtil.close(st, rs);
      JDBCUtil.close(conn);
    }
  }

  public void checkBulksExistInWercs(List<ItemBean> ar)
  {
    PreparedStatement pstmt = null;
    ResultSet rs = null;
    StringBuilder sql = new StringBuilder();
    sql.append("SELECT F_PRODUCT FROM T_PRODUCT_ALIAS_NAMES WHERE F_PRODUCT = ? ");

    try
    {
      pstmt = this.getWercsConn().prepareStatement(sql.toString());
      Iterator i = ar.iterator();
      while (i.hasNext())
      {
        ItemBean ib = (ItemBean) i.next();
        pstmt.setString(1, ib.getProduct());
        rs = pstmt.executeQuery();
        if (!rs.next())
        {
          StringBuilder error = new StringBuilder();
          error.append("Error: Tried to add a new fill ");
          error.append(ib.getAlias());
          error.append(" associated to bulk ");
          error.append(ib.getProduct());
          error.append(". This bulk does not exist in WERCS so it will not be added.");
          log4jLogger.info(error.toString());
          updateStatus(ib, -99);
          i.remove();
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

  public void checkBulksMatchInWercs(List<ItemBean> ar)
  {
    PreparedStatement pstmt = null;
    ResultSet rs = null;
    try
    {
      pstmt = this.getWercsConn().prepareStatement("SELECT F_PRODUCT FROM T_PRODUCT_ALIAS_NAMES WHERE F_ALIAS = ? ");
      Iterator i = ar.iterator();
      while (i.hasNext())
      {
        try
        {
          ItemBean ib = (ItemBean) i.next();
          pstmt.clearParameters();
          pstmt.setString(1, ib.getAlias());
          rs = pstmt.executeQuery();

          if (rs.next())
          {
            String f_product = rs.getString(1);

            if (((ib.getProduct() != null) && (f_product != null)) && !(ib.getProduct().equalsIgnoreCase(f_product)))
            {
              StringBuilder badAssociationError = new StringBuilder();
              badAssociationError.append("Tried to add a new fill ");
              badAssociationError.append(ib.getAlias());
              badAssociationError.append(" associated to bulk ");
              badAssociationError.append(ib.getProduct());
              badAssociationError.append(". This fill is associated to ");
              badAssociationError.append(f_product);
              badAssociationError.append(" in Wercs. This item will not be added.");
              log4jLogger.info(badAssociationError.toString());
              updateStatus(ib, -99);
              i.remove();
            }
          }
        }
        catch (Exception e)
        {
          log4jLogger.error(e);
        }
        finally
        {
          JDBCUtil.close(rs);
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

  public void checkStripsMatchInWercs(List<ItemBean> ar)
  {
    PreparedStatement pstmt = null;
    ResultSet rs = null;
    try
    {
      pstmt = this.getWercsConn().prepareStatement(" SELECT F_PRODUCT FROM T_PRODUCT_ALIAS_NAMES WHERE F_ALIAS = ? ");
      Iterator i = ar.iterator();
      while (i.hasNext())
      {
        ItemBean ib = (ItemBean) i.next();
        try
        {
          if (ib.isPublishStripped())
          {
            pstmt.clearParameters();
            pstmt.setString(1, ib.getStrippedAlias());
            rs = pstmt.executeQuery();
            if (rs.next())
            {
              String f_product = rs.getString(1);

              if (((ib.getProduct() != null) && (f_product != null)) && !(ib.getProduct().equalsIgnoreCase(f_product)))
              {
                StringBuilder badAssociationError = new StringBuilder();
                badAssociationError.append("Tried to add a new stripped alias ");
                badAssociationError.append(ib.getStrippedAlias());
                badAssociationError.append(" associated to bulk ");
                badAssociationError.append(ib.getProduct());
                badAssociationError.append(". This stripped alias is associated to ");
                badAssociationError.append(f_product);
                badAssociationError.append(" in Wercs. This item will not be added.");
                log4jLogger.info(badAssociationError.toString());
                updateStatus(ib, -99);
                i.remove();
              }
            }
          }
        }
        catch (Exception e)
        {
          log4jLogger.error(e);
        }
        finally
        {
          JDBCUtil.close(rs);
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

  public void buildTranslationBeans(DataSource datasource, ArrayList<TranslationBean> ar)
  {
    StringBuilder sql = new StringBuilder();
    sql.append("select b.item_id, a.description, b.item_no, c.iso, c.oracle, pa.f_product  ");
    sql.append("from VCA_WERCS_TRANSLATION_QUEUE a, ic_item_mst b, vca_language_mappings@to_rg c, t_product_alias_names@to_rg pa  ");
    sql.append("where a.id = b.item_id ");
    sql.append("and b.item_no = pa.f_alias  ");
    sql.append("and a.language = c.oracle ");
    sql.append("and a.status = 0 ");

    Statement st = null;
    ResultSet rs = null;
    OracleConnection conn = null;
    try
    {
      conn = (OracleConnection) ConnectionAccessBean.getConnection(datasource);
      st = conn.createStatement();
      rs = st.executeQuery(sql.toString());

      while (rs.next())
      {
        TranslationBean tb = new TranslationBean();
        tb.setItemId(rs.getString(1));
        tb.setTranslation(rs.getString(2));
        tb.setAlias(rs.getString(3));
        tb.setIsoLanguage(rs.getString(4));
        tb.setOracleLanguage(rs.getString(5));
        tb.setDatasource(datasource);
        tb.setProduct(rs.getString(6));
        ar.add(tb);
      }
    }
    catch (Exception e)
    {
      log4jLogger.error(e);
    }
    finally
    {
      JDBCUtil.close(st, rs);
      JDBCUtil.close(conn);
    }
  }

  public void addToWercs(ItemBean ib)
  {
    CallableStatement cstmt = null;
    try
    {
      cstmt = this.getWercsConn().prepareCall( "{call WERCS.WERCS_ADD_NEW_ALIAS_PROC(?,?,?,?,?,?,?)}");
      cstmt.setString(1, ib.getProduct().toUpperCase());
      cstmt.setString(2, ib.getAlias().toUpperCase());
      cstmt.setString(3, ib.getAliasName().toUpperCase());
      cstmt.setString(4, ib.getReason());
      cstmt.setString(5, ib.getCustomer());
      cstmt.setString(6, this.getInterfaceName());
      cstmt.registerOutParameter(7, Types.VARCHAR);

      log4jLogger.info("Calling WERCS_ADD_NEW_ALIAS_PROC() for alias " + ib.getAlias());
      cstmt.execute();
      String error = cstmt.getString(7);
      if (error != null)
        log4jLogger.error("Error Calling WERCS_ADD_NEW_ALIAS_PROC() for alias " + ib.getAlias() + ": " + error);
      else
        deleteFromQueue(ib);

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

  private String getBulk(String alias)
  {
    Statement st = null;
    ResultSet rs = null;
    String product = null;

    try
    {
      StringBuilder sql = new StringBuilder();
      sql.append("select GET_PRODUCT(");
      sql.append(CommonUtility.toVarchar(alias));
      sql.append(") from dual");

      st = this.getWercsConn().createStatement();
      rs = st.executeQuery(sql.toString());

      if (rs.next())
      {
        product = rs.getString(1);
      }
    }
    catch (Exception e)
    {
      log4jLogger.error(e);
    }
    finally
    {
      JDBCUtil.close(st, rs);
    }
    return product;
  }

  private void updateStatus(ItemBean ib, int status)
  {
    OracleConnection conn = null;
    Statement stmt = null;
    try
    {
      conn = (OracleConnection) ConnectionAccessBean.getConnection(ib.getDatasource());
      log4jLogger.info("Setting the status = " + status + " for Product " + ib.getProduct() + " and alias " + ib.getAlias());
      StringBuilder sql = new StringBuilder();
      sql.append("update VALSPAR.VCA_WERCS_EXTRACT_FILLS_QUEUE ");
      sql.append("set status = ");
      sql.append(status);
      sql.append(" where id = ");
      sql.append(ib.getId());

      stmt = conn.createStatement();
      stmt.executeUpdate(sql.toString());
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
  }

  private void updateTranslationStatus(TranslationBean tb, int status)
  {
    Statement stmt = null;
    OracleConnection conn = null;
    try
    {
      conn = (OracleConnection) ConnectionAccessBean.getConnection(tb.getDatasource());
      StringBuilder sql = new StringBuilder();
      sql.append("update VALSPAR.VCA_WERCS_TRANSLATION_QUEUE ");
      sql.append("set status = ");
      sql.append(status);
      sql.append(" where id = ");
      sql.append(tb.getItemId());
      sql.append(" and language = ");
      sql.append(CommonUtility.toVarchar(tb.getOracleLanguage()));

      stmt = conn.createStatement();
      stmt.executeUpdate(sql.toString());
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
  }

  private void deleteFromQueue(ItemBean ib)
  {
    OracleConnection conn = null;
    OraclePreparedStatement pst = null;
    try
    {
      conn = (OracleConnection) ConnectionAccessBean.getConnection(ib.getDatasource());
      log4jLogger.info("Deleting Product " + ib.getProduct() + " and alias " + ib.getAlias() + " from extract fills queue");
      pst = (OraclePreparedStatement) conn.prepareStatement("delete from VALSPAR.VCA_WERCS_EXTRACT_FILLS_QUEUE where id = :ID ");
      pst.setStringAtName("ID", ib.getId());
      pst.executeUpdate();
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
