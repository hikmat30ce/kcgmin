package com.valspar.interfaces.regulatory.publishing.program;

import com.jacob.activeX.ActiveXComponent;
import com.jacob.com.Dispatch;
import com.valspar.interfaces.common.BaseInterface;
import com.valspar.interfaces.common.beans.ConnectionBean;
import com.valspar.interfaces.common.utils.JDBCUtil;
import com.valspar.interfaces.regulatory.publishing.beans.ProductBean;
import java.sql.*;
import java.util.*;
import org.apache.log4j.Logger;

public class PublishingInterface extends BaseInterface
{
  private static Logger log4jLogger = Logger.getLogger(PublishingInterface.class);
  private ActiveXComponent axc;

  public PublishingInterface(String interfaceName, String environment, String runType)
  {
    super(interfaceName, environment);

    try
    {
      axc = new ActiveXComponent("OptivaToWercsJacobProject.clsJACOB_API");
      updateStatus();

      while (!onHold())
      {
        ArrayList productList = new ArrayList();
        buildProductBeans(productList, runType);
        log4jLogger.info("There are " + productList.size() + " products to be published.");
        if (productList.isEmpty())
          break;

        checkProductBeans(productList);
        publishProductBeans(productList);
      }
      log4jLogger.info("The PublishingInterface has no more to publish.");
    }
    catch (Exception e)
    {
      log4jLogger.error(e);
    }
    finally
    {
      cleanUp();
    }
  }

  public void execute()
  {    
  }

  public static void main(String[] parameters)
  {
    new PublishingInterface(parameters[0], parameters[1], parameters[2]);
  }

  public void buildProductBeans(ArrayList productList, String runType)
  {
    Statement stmt = null;
    ResultSet rs = null;
    try
    {
      StringBuffer sql = new StringBuffer();
      sql.append("SELECT * FROM ");
      sql.append("(SELECT * FROM VA_PUBLISHING_QUEUE where RUN_TYPE = '");
      sql.append(runType);
      sql.append("' AND STATUS = 0 ORDER BY PRIORITY, ADDED_DATE) ");
      sql.append("WHERE ROWNUM <= 30 ");

      ConnectionBean cb = getFromConn().get(0);
      stmt = cb.getConnection().createStatement();
      rs = stmt.executeQuery(sql.toString());

      while (rs.next())
      {
        ProductBean pb = new ProductBean();
        pb.setId(rs.getString(1));
        pb.setProduct(rs.getString(2));
        pb.setFormat(rs.getString(3));
        pb.setSubformat(rs.getString(4));
        pb.setLanguage(rs.getString(5));
        pb.setAlias(rs.getString(6));
        pb.setAliasName(rs.getString(7));
        pb.setUser(rs.getString(10));
        productList.add(pb);
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

  public void updateStatus()
  {
    try
    {
      StringBuffer productMissingUpdateSql = new StringBuffer();
      productMissingUpdateSql.append("update va_publishing_queue a set a.status = 0 where a.status = -99 and exists ");
      productMissingUpdateSql.append("(select * from t_product_alias_names where f_alias = a.alias and f_product = a.product)");

      StringBuffer authorizationMissingUpdateSql = new StringBuffer();
      authorizationMissingUpdateSql.append("update va_publishing_queue a set a.status = 0 where a.status = -77 and exists ");
      authorizationMissingUpdateSql.append("(SELECT * FROM T_PROD_FORMAT_AUTH B, T_PROD_FORMULATION_AUTH C ");
      authorizationMissingUpdateSql.append("WHERE B.F_PRODUCT = C.F_PRODUCT ");
      authorizationMissingUpdateSql.append("AND   B.F_FORMAT_AUTH = 1 ");
      authorizationMissingUpdateSql.append("AND   C.F_FORMULATION_AUTH = 1 ");
      authorizationMissingUpdateSql.append("AND   B.F_PRODUCT = A.PRODUCT)");

      ConnectionBean cb = getFromConn().get(0);
      cb.getConnection().createStatement().executeUpdate(productMissingUpdateSql.toString());
      cb.getConnection().createStatement().executeUpdate(authorizationMissingUpdateSql.toString());
    }
    catch (Exception e)
    {
      log4jLogger.error(e);
    }
  }

  public void checkProductBeans(ArrayList productList)
  {
    try
    {
      Iterator i = productList.iterator();

      while (i.hasNext())
      {
        ProductBean pb = (ProductBean) i.next();
        if (!checkProductInWercs(pb))
          i.remove();
        else if (!checkProductAuthorization(pb))
          i.remove();
        else if (!checkMsdsApprovalFlag(pb))
          i.remove();
        else if (!checkProductObsolete(pb))
          i.remove();
      }
    }
    catch (Exception e)
    {
      log4jLogger.error(e);
    }
  }

  private boolean checkProductInWercs(ProductBean pb)
  {
    boolean returnValue = true;
    Statement stmt = null;
    ResultSet rs = null;
    try
    {
      StringBuffer sql = new StringBuffer();
      sql.append("select * from t_product_alias_names where f_alias = '");
      sql.append(pb.getAlias());
      sql.append("' and f_product = '");
      sql.append(pb.getProduct());
      sql.append("'");

      ConnectionBean cb = getFromConn().get(0);
      stmt = cb.getConnection().createStatement();
      rs = stmt.executeQuery(sql.toString());
      if (!rs.next())
      {
        updateVaPublishingQueueStatus(pb, "-99");
        log4jLogger.error("Product: " + pb.getProduct() + ", Alias: " + pb.getAlias() + " combination is not in T_PRODUCT_ALIAS_NAMES.");
        returnValue = false;
      }
    }
    catch (Exception e)
    {
      log4jLogger.error("for alias: " + pb.getAlias() + ", product: " + pb.getProduct(), e);
    }
    finally
    {
      JDBCUtil.close(stmt, rs);
    }
    return returnValue;
  }

  private boolean checkProductAuthorization(ProductBean pb)
  {
    boolean returnValue = true;
    Statement stmt = null;
    ResultSet rs = null;
    try
    {
      StringBuffer sql = new StringBuffer();
      sql.append("select * from t_prod_format_auth a, t_prod_formulation_auth b ");
      sql.append("where a.f_product = b.f_product ");
      sql.append("and a.f_format_auth = 1 ");
      sql.append("and b.f_formulation_auth = 1 ");
      sql.append("and a.f_product = '");
      sql.append(pb.getProduct());
      sql.append("'");

      ConnectionBean cb = getFromConn().get(0);
      stmt = cb.getConnection().createStatement();
      rs = stmt.executeQuery(sql.toString());

      if (!rs.next())
      {
        updateVaPublishingQueueStatus(pb, "-77");
        log4jLogger.error("Product " + pb.getProduct() + " is not authorized in T_PROD_FORMAT_AUTH and T_PROD_FORMULATION_AUTH.");
        returnValue = false;
      }
    }
    catch (Exception e)
    {
      log4jLogger.error("for product: " + pb.getProduct(), e);
    }
    finally
    {
      JDBCUtil.close(stmt, rs);
    }
    return returnValue;
  }

  private boolean checkMsdsApprovalFlag(ProductBean pb)
  {
    boolean returnValue = true;
    PreparedStatement pstmt = null;
    ResultSet rs = null;
    try
    {
      StringBuffer sql = new StringBuffer();
      sql.append("select distinct a.F_APPROVAL_FLAG ");
      sql.append("from va_publishing_groups a, t_prod_data b ");
      sql.append("where a.F_SUBFORMAT = ? ");
      sql.append("and ((a.F_APPROVAL_FLAG IS NULL) or (b.f_data_code = a.f_approval_flag)) ");
      sql.append("and   b.F_PRODUCT = ? ");

      ConnectionBean cb = getFromConn().get(0);
      pstmt = cb.getConnection().prepareCall(sql.toString());
      pstmt.setString(1, pb.getSubformat());
      pstmt.setString(2, pb.getProduct());

      rs = pstmt.executeQuery();

      if (!rs.next())
      {
        updateVaPublishingQueueStatus(pb, "-55");
        log4jLogger.error("Product " + pb.getProduct() + ", Subformat " + pb.getSubformat() + " does not have the correct approval flag.");
        returnValue = false;
      }
    }
    catch (Exception e)
    {
      log4jLogger.error("for product: " + pb.getProduct() + ", Subformat " + pb.getSubformat(), e);
    }
    finally
    {
      JDBCUtil.close(pstmt, rs);
    }
    return returnValue;
  }

  private boolean checkProductObsolete(ProductBean pb)
  {
    boolean returnValue = true;
    Statement stmt = null;
    ResultSet rs = null;
    try
    {
      StringBuffer sql = new StringBuffer();
      sql.append("select * from t_prod_text where f_product = '");
      sql.append(pb.getProduct());
      sql.append("' and f_text_code = 'PRDSTAT3'");

      ConnectionBean cb = getFromConn().get(0);
      stmt = cb.getConnection().createStatement();
      rs = stmt.executeQuery(sql.toString());
      if (rs.next())
      {
        updateVaPublishingQueueStatus(pb, "-44");
        log4jLogger.error("Product: " + pb.getProduct() + ", Alias: " + pb.getAlias() + " is an obsolete formula in Optiva.");
        returnValue = false;
      }
    }
    catch (Exception e)
    {
      log4jLogger.error("for alias: " + pb.getAlias() + ", product: " + pb.getProduct(), e);
    }
    finally
    {
      JDBCUtil.close(stmt, rs);
    }
    return returnValue;
  }

  private void updateVaPublishingQueueStatus(ProductBean pb, String status)
  {
    Statement stmt = null;
    try
    {
      StringBuffer sql = new StringBuffer();
      sql.append("update va_publishing_queue set status = ");
      sql.append(status);
      sql.append(" where id = ");
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

  public void publishProductBeans(ArrayList productList)
  {
    try
    {
      Object axcObj = axc.getObject();
      Iterator i = productList.iterator();
      log4jLogger.info("Starting to publish the next " + productList.size() + " products from VA_PUBLISHING_QUEUE...");

      while (i.hasNext())
      {
        ProductBean pb = (ProductBean) i.next();
        log4jLogger.info("Publishing: Product = " + pb.getProduct() + ", Alias = " + pb.getAlias() + ", Subformat = " + pb.getSubformat() + ", Language = " + pb.getLanguage());
        if (pb.getSubformat().equalsIgnoreCase("USA") || pb.getSubformat().equalsIgnoreCase("EPU") || pb.getSubformat().equalsIgnoreCase("CCAU"))
        {
          setTradeSecret(pb);
        }
        if (!pb.getSubformat().equalsIgnoreCase("PDS") && !pb.getSubformat().equalsIgnoreCase("CMP"))
        {
          insertPrintFlags(pb);
        }
        String callPublishReturn = Dispatch.call(axcObj, "callPublish", pb.getProduct(), pb.getFormat(), pb.getSubformat(), pb.getLanguage(), pb.getAlias(), pb.getAliasName()).toString();
        if (pb.getSubformat().equalsIgnoreCase("USA") || pb.getSubformat().equalsIgnoreCase("EPU") || pb.getSubformat().equalsIgnoreCase("CCAU"))
        {
          removeTradeSecret(pb);
        }
        if (!callPublishReturn.equals("0"))
          log4jLogger.error("callPublish error: Product = " + pb.getProduct() + ", Alias = " + pb.getAlias() + ", Subformat = " + pb.getSubformat() + ", Language = " + pb.getLanguage() + ", callPublishReturn: " + callPublishReturn);
        else
          removeFromPublishingQueue(pb);
      }
      log4jLogger.info("Done publishing group.  Going to pick up more from VA_PUBLISHING_QUEUE.");
    }
    catch (Exception e)
    {
      log4jLogger.error(e);
    }
  }

  public void removeFromPublishingQueue(ProductBean pb)
  {
    PreparedStatement pstmt = null;
    try
    {
      ConnectionBean cb = getFromConn().get(0);
      pstmt = cb.getConnection().prepareCall("delete from va_publishing_queue where id = ?");
      pstmt.setString(1, pb.getId());
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

  public void setTradeSecret(ProductBean pb)
  {
    PreparedStatement pstmt = null;
    try
    {
      ConnectionBean cb = getFromConn().get(0);
      StringBuffer sql = new StringBuffer();
      sql.append("UPDATE T_PROD_COMP A ");
      sql.append("SET A.F_TRADE_SECRET_FLAG = 1 ");
      sql.append("where A.F_PRODUCT = ? ");
      sql.append("AND EXISTS ");
      sql.append(" (SELECT * FROM T_COMPONENTS_ALIAS B ");
      sql.append(" WHERE A.F_COMPONENT_ID = B.F_COMPONENT_ID ");
      sql.append(" AND A.F_CAS_NUMBER   = B.F_CAS_NUMBER ");
      sql.append(" AND B.F_LANGUAGE = 'EN' ");
      sql.append(" AND   ( B.F_TRADE_SECRET_NAME  LIKE 'PROP%' OR  B.F_TRADE_SECRET_NAME  LIKE 'SUP%') ");
      sql.append(")");
      pstmt = cb.getConnection().prepareCall(sql.toString());
      pstmt.setString(1, pb.getProduct());
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

  public void removeTradeSecret(ProductBean pb)
  {
    PreparedStatement pstmt = null;
    try
    {
      ConnectionBean cb = getFromConn().get(0);
      StringBuffer sql = new StringBuffer();
      sql.append("UPDATE T_PROD_COMP A ");
      sql.append("SET A.F_TRADE_SECRET_FLAG = 0 ");
      sql.append("where A.F_PRODUCT = ? ");

      pstmt = cb.getConnection().prepareCall(sql.toString());
      pstmt.setString(1, pb.getProduct());
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

  public void insertPrintFlags(ProductBean pb)
  {
    PreparedStatement pstmt = null;
    try
    {
      ConnectionBean cb = getFromConn().get(0);
      StringBuffer sql = new StringBuffer();
      sql.append("INSERT INTO T_PRINT_FLAGS ");
      sql.append("(F_PRODUCT, F_FORMAT, F_CAS_NUMBER, F_COMPONENT_ID, F_PRINT_FLAG, F_DATE_STAMP, F_SUBFORMAT, F_USER_UPDATED) ");
      sql.append("SELECT A.F_PRODUCT, ? , A.F_CAS_NUMBER, A.F_COMPONENT_ID, 0, SYSDATE, ? ,'WERCS' ");
      sql.append("FROM T_PROD_COMP A ");
      sql.append("WHERE NOT EXISTS (SELECT 'x' FROM T_PRINT_FLAGS B ");
      sql.append("                  WHERE A.F_PRODUCT = B.F_PRODUCT ");
      sql.append("                  AND A.F_COMPONENT_ID = B.F_COMPONENT_ID ");
      sql.append("                  AND A.F_CAS_NUMBER = B.F_CAS_NUMBER ");
      sql.append("                  AND B.F_SUBFORMAT = ? ) ");
      sql.append("AND A.F_PRODUCT = ? ");

      pstmt = cb.getConnection().prepareCall(sql.toString());
      pstmt.setString(1, pb.getFormat());
      pstmt.setString(2, pb.getSubformat());
      pstmt.setString(3, pb.getSubformat());
      pstmt.setString(4, pb.getProduct());
      log4jLogger.info(pstmt.executeUpdate() + " new print flags were added to " + pb.getProduct());
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
