package com.valspar.interfaces.regulatory.optivatodromont.program;

import com.valspar.interfaces.common.BaseInterface;
import com.valspar.interfaces.common.beans.ConnectionAccessBean;
import com.valspar.interfaces.common.enums.DataSource;
import com.valspar.interfaces.common.utils.JDBCUtil;
import com.valspar.interfaces.regulatory.optivatodromont.beans.RowBean;
import java.sql.*;
import java.util.*;
import oracle.jdbc.OracleConnection;
import org.apache.log4j.Logger;

public class OptivaToDromontInterface extends BaseInterface
{
  OracleConnection optivaConn = null;

  private static Logger log4jLogger = Logger.getLogger(OptivaToDromontInterface.class);

  public OptivaToDromontInterface()
  {
  }

  public void execute()
  {
    try
    {
      optivaConn = (OracleConnection) ConnectionAccessBean.getConnection(DataSource.FORMULATION);
      ArrayList<DataSource> toDromontDataSourceList = new ArrayList<DataSource>();
      toDromontDataSourceList.add(DataSource.DROMONTBG);
      toDromontDataSourceList.add(DataSource.DROMONTGARPITT);
      toDromontDataSourceList.add(DataSource.DROMONTKANK);
      for (DataSource ds: toDromontDataSourceList)
      {
        ArrayList ar = new ArrayList();
        cleanUpDromontQueue(ds);
        buildBeans(ar, ds);
        populateDromontSystem(ar, ds);
        updateDromontState(ds);
        updateQueueStatus(ds);
      }
    }
    catch (Exception e)
    {
      log4jLogger.error(e);
    }
    finally
    {
      JDBCUtil.close(optivaConn);
    }
  }

  public void cleanUpDromontQueue(DataSource ds)
  {
    Connection conn = null;
    try
    {
      conn = ConnectionAccessBean.getConnection(ds);
      log4jLogger.info("Updating FdIFFoTrState to 2 for " + ds.getDataSourceLabel());
      StringBuffer sql = new StringBuffer();
      sql.append("update iFFo set FdIFFoTrState = 2 where FdIFFoTrState = 1 and FdIFFoTrErrCod = 0");
      Statement stmt = conn.createStatement();
      stmt.executeUpdate(sql.toString());
      stmt.close();
      log4jLogger.info("Done updating FdIFFoTrState to 2 for " + ds.getDataSourceLabel());
    }
    catch (Exception e)
    {
      log4jLogger.error(e);
    }
    finally
    {
      JDBCUtil.close(conn);
    }
  }

  public void buildBeans(ArrayList ar, DataSource ds)
  {
    try
    {
      log4jLogger.info("Building row beans to send to " + ds.getDataSourceLabel());

      StringBuffer sql = new StringBuffer();
      sql.append("select op.id, hdr.cf_mapper_formula, hdr.description, substr(hdr.version, 3,4), ");
      sql.append("hdr.commenttxt, ing.item_code, ing.material_pct, op.location ");
      sql.append("from vca_optiva_to_dromont op, fsformula hdr, fsformulaingred ing ");
      sql.append("where op.fsformula_id = hdr.formula_id and hdr.formula_id = ing.formula_id ");
      sql.append("and ing.material_pct > 0 and op.status = 0 ");
      sql.append("and op.location = UPPER(REPLACE('");
      sql.append(ds.getDataSourceLabel());
      sql.append("', 'Dromont '))");
      Statement stmt = optivaConn.createStatement();
      ResultSet rs = stmt.executeQuery(sql.toString());

      while (rs.next())
      {
        RowBean rb = new RowBean();
        rb.setId(rs.getString(1));
        rb.setCfMapperFormula(rs.getString(2));
        rb.setDescription(rs.getString(3));
        rb.setVersion(rs.getString(4));
        rb.setCommenttxt(rs.getString(5));
        rb.setItemCode(rs.getString(6));
        rb.setMaterialPct(rs.getString(7));
        rb.setLocation(rs.getString(8));
        ar.add(rb);
      }
      stmt.close();
      rs.close();
      log4jLogger.info("There are " + ar.size() + " row beans to be sent to " + ds.getDataSourceLabel());
    }
    catch (Exception e)
    {
      log4jLogger.error(e);
    }
  }

  public void populateDromontSystem(ArrayList ar, DataSource ds)
  {
    Connection conn = null;
    try
    {
      conn = ConnectionAccessBean.getConnection(ds);
      log4jLogger.info("Starting to insert row beans to " + ds.getDataSourceLabel());
      Iterator i = ar.iterator();
      while (i.hasNext())
      {
        RowBean rb = null;
        try
        {
          rb = (RowBean) i.next();
          StringBuffer sql = new StringBuffer();
          sql.append("insert into iFFo (FdIFFoFoHeCod, FdIFFoFoHeDes, ");
          sql.append("FdIFFoFoHeRel, FdIFFoFoHeRna, FdIFFoSerieCod, FdIFFoCodTyp, FdIFFoFoLiCod, ");
          sql.append("FdIFFoFoLiQty, FdIFFoTrState, FdIFFoTrErrCod, FdIFFoTrErrDes) values (");
          sql.append("'");
          sql.append(rb.getCfMapperFormula());
          sql.append("', '");
          sql.append(rb.getDescription());
          sql.append("', '");
          sql.append(rb.getVersion());
          sql.append("', '");
          sql.append(rb.getCommenttxt());
          sql.append("', 'Serie1', '0', '");
          sql.append(rb.getItemCode());
          sql.append("', ");
          sql.append(rb.getMaterialPct());
          sql.append(", -1, '', '')");
          Statement stmt = conn.createStatement();
          stmt.executeUpdate(sql.toString());
          stmt.close();
        }
        catch (Exception e)
        {
          log4jLogger.error("formula = '" + rb.getCfMapperFormula() + "', item code = '" + rb.getItemCode() + "'", e);
        }
      }
      log4jLogger.info("Done inserting row beans to " + ds.getDataSourceLabel());
    }
    catch (Exception e)
    {
      log4jLogger.error(e);
    }
    finally
    {
      JDBCUtil.close(conn);
    }
  }
  
  public void updateDromontState(DataSource ds)
  {
    Connection conn = null;
    try
    {
      conn = ConnectionAccessBean.getConnection(ds);
      log4jLogger.info("Updating FdIFFoTrState to 0 for " + ds.getDataSourceLabel());
      StringBuffer sql = new StringBuffer();
      sql.append("update iFFo set FdIFFoTrState = 0 where FdIFFoTrState = -1");
      Statement stmt = conn.createStatement();
      stmt.executeUpdate(sql.toString());
      stmt.close();
      log4jLogger.info("Done updating FdIFFoTrState to 0 for " + ds.getDataSourceLabel());
    }
    catch (Exception e)
    {
      log4jLogger.error(e);
    }
    finally
    {
      JDBCUtil.close(conn);
    }
  }

  public void updateQueueStatus(DataSource ds)
  {
    try
    {
      log4jLogger.info("Updating the Queue Status to 1 for " + ds.getDataSourceLabel());
      StringBuffer sql = new StringBuffer();
      sql.append("update VCA_OPTIVA_TO_DROMONT set status = 1 where status = 0 ");
      sql.append("and location = UPPER(REPLACE('");
      sql.append(ds.getDataSourceLabel());
      sql.append("', 'Dromont '))");
      Statement stmt = optivaConn.createStatement();
      stmt.executeUpdate(sql.toString());
      stmt.close();
      log4jLogger.info("Done updating the Queue Status to 1 for " + ds.getDataSourceLabel());
    }
    catch (Exception e)
    {
      log4jLogger.error(e);
    }
  }
}
