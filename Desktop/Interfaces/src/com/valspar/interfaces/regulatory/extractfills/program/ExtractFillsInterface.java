package com.valspar.interfaces.regulatory.extractfills.program;

import com.valspar.interfaces.common.BaseInterface;
import com.valspar.interfaces.common.beans.ConnectionAccessBean;
import com.valspar.interfaces.common.enums.DataSource;
import com.valspar.interfaces.common.utils.*;
import com.valspar.interfaces.regulatory.extractfills.beans.*;
import java.sql.*;
import java.util.*;
import oracle.jdbc.OracleConnection;
import org.apache.log4j.Logger;

public class ExtractFillsInterface extends BaseInterface
{
  private OracleConnection regulatoryConn = null;
  private static Logger log4jLogger = Logger.getLogger(ExtractFillsInterface.class);

  public ExtractFillsInterface()
  {
  }

  public void execute()
  {
    try
    {
      this.setRegulatoryConn((OracleConnection) ConnectionAccessBean.getConnection(DataSource.REGULATORY));
      ArrayList<DataSource> fromERPDataSourceList = new ArrayList<DataSource>();
      fromERPDataSourceList.add(DataSource.NORTHAMERICAN);
      fromERPDataSourceList.add(DataSource.EMEAI);
      fromERPDataSourceList.add(DataSource.ASIAPAC);
      ArrayList itemList = new ArrayList();
      ArrayList<TranslationBean> translationList = new ArrayList<TranslationBean>();

      for (DataSource datasource: fromERPDataSourceList)
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
      Iterator ibIterator = itemList.iterator();

      while (ibIterator.hasNext())
      {
        ItemBean ib = (ItemBean) ibIterator.next();
        addToWercs(ib);
      }

      log4jLogger.info("There are " + translationList.size() + " that will be added to the translation table");
      for (TranslationBean tb: translationList)
      {
        String success = LanguageUtility.updateWercsDescriptionTranslation(tb.getAlias(), tb.getTranslation(), tb.getIsoLanguage(), this.getRegulatoryConn());
        if (success.equalsIgnoreCase("success"))
        {
          deleteFromTranslationQueue(tb);
        }
        else
        {
          updateTranslationStatus(tb, -99);
        }
      }
    }
    catch (Exception e)
    {
      log4jLogger.error(e);
    }
    finally
    {
      JDBCUtil.close(this.getRegulatoryConn());
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

  public void buildBeans(DataSource datasource, ArrayList ar)
  {
    StringBuffer sql = new StringBuffer();
    sql.append("SELECT a.ID, upper(a.alias), upper(a.product), a.description, a.reason, a.customer,   ");
    sql.append("nvl((SELECT PUBLISH_STRIPPED   ");
    sql.append("          FROM va_publishing_rules@torg ");
    sql.append("         WHERE bus_group = get_wercs_data@torg(a.product, 'BUSGP')),0) as PUBLISH_STRIPPED,  ");
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

  public void checkBulksExistInWercs(ArrayList ar)
  {
    PreparedStatement pstmt = null;
    ResultSet rs = null;
    StringBuffer sql = new StringBuffer();
    sql.append("SELECT F_PRODUCT FROM T_PRODUCT_ALIAS_NAMES WHERE F_PRODUCT = ? ");

    try
    {
      pstmt = this.getRegulatoryConn().prepareStatement(sql.toString());
      Iterator i = ar.iterator();
      while (i.hasNext())
      {
        ItemBean ib = (ItemBean) i.next();
        pstmt.setString(1, ib.getProduct());
        rs = pstmt.executeQuery();

        if (!rs.next())
        {
          StringBuffer error = new StringBuffer();
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

  public void checkBulksMatchInWercs(ArrayList ar)
  {
    PreparedStatement pstmt = null;
    ResultSet rs = null;
    try
    {
      Iterator i = ar.iterator();
      StringBuffer sql = new StringBuffer();
      sql.append("SELECT F_PRODUCT FROM T_PRODUCT_ALIAS_NAMES WHERE F_ALIAS = ? ");
      pstmt = this.getRegulatoryConn().prepareStatement(sql.toString());

      while (i.hasNext())
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
            StringBuffer badAssociationError = new StringBuffer();
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

  public void checkStripsMatchInWercs(ArrayList ar)
  {
    PreparedStatement pstmt = null;
    ResultSet rs = null;

    try
    {
      Iterator i = ar.iterator();
      StringBuffer sql = new StringBuffer();
      sql.append(" SELECT F_PRODUCT FROM T_PRODUCT_ALIAS_NAMES WHERE F_ALIAS = ? ");
      pstmt = this.getRegulatoryConn().prepareStatement(sql.toString());

      while (i.hasNext())
      {
        ItemBean ib = (ItemBean) i.next();
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
              StringBuffer badAssociationError = new StringBuffer();
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
    StringBuffer sql = new StringBuffer();
    sql.append("select b.item_id, a.description, b.item_no, c.iso, c.oracle ");
    sql.append("from VCA_WERCS_TRANSLATION_QUEUE a, ic_item_mst b, vca_language_mappings@torg c ");
    sql.append("where a.id = b.item_id ");
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
      String command = "{call WERCS.WERCS_ADD_NEW_ALIAS_PROC(?,?,?,?,?,?,?)}";
      cstmt = this.getRegulatoryConn().prepareCall(command);
      cstmt.setString(1, ib.getProduct().toUpperCase());
      cstmt.setString(2, ib.getAlias().toUpperCase());
      cstmt.setString(3, ib.getAliasName().toUpperCase());
      cstmt.setString(4, ib.getReason());
      cstmt.setString(5, ib.getCustomer());
      cstmt.setString(6, ib.getDatasource().getAnalyticsDataSource());
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
    String f_product = null;

    try
    {
      StringBuffer sql = new StringBuffer();
      sql.append("select GET_PRODUCT('");
      sql.append(alias);
      sql.append("') from dual");

      st = this.getRegulatoryConn().createStatement();
      rs = st.executeQuery(sql.toString());

      if (rs.next())
      {
        f_product = rs.getString(1);
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
    return f_product;
  }

  private void updateStatus(ItemBean ib, int status)
  {
    Statement stmt = null;
    OracleConnection conn = null;
    try
    {
      conn = (OracleConnection) ConnectionAccessBean.getConnection(ib.getDatasource());
      log4jLogger.info("Setting the status = " + status + " for Product " + ib.getProduct() + " and alias " + ib.getAlias());
      StringBuffer sql = new StringBuffer();
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
      StringBuffer sql = new StringBuffer();
      sql.append("update VALSPAR.VCA_WERCS_TRANSLATION_QUEUE ");
      sql.append("set status = ");
      sql.append(status);
      sql.append(" where id = ");
      sql.append(tb.getItemId());
      sql.append(" and language = '");
      sql.append(tb.getOracleLanguage());
      sql.append("'");

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
    Statement stmt = null;
    OracleConnection conn = null;
    try
    {
      conn = (OracleConnection) ConnectionAccessBean.getConnection(ib.getDatasource());
      log4jLogger.info("Deleting Product " + ib.getProduct() + " and alias " + ib.getAlias() + " from extract fills queue");
      StringBuffer sql = new StringBuffer();
      sql.append("delete from VALSPAR.VCA_WERCS_EXTRACT_FILLS_QUEUE where id = ");
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

  private void deleteFromTranslationQueue(TranslationBean tb)
  {
    Statement stmt = null;
    OracleConnection conn = null;
    try
    {
      conn = (OracleConnection) ConnectionAccessBean.getConnection(tb.getDatasource());
      StringBuffer sql = new StringBuffer();
      sql.append("delete from VALSPAR.VCA_WERCS_TRANSLATION_QUEUE where id = ");
      sql.append(tb.getItemId());
      sql.append(" and language = '");
      sql.append(tb.getOracleLanguage());
      sql.append("'");

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

  public void setRegulatoryConn(OracleConnection regulatoryConn)
  {
    this.regulatoryConn = regulatoryConn;
  }

  public OracleConnection getRegulatoryConn()
  {
    return regulatoryConn;
  }
}
