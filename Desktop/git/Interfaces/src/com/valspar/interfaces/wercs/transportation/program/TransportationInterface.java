package com.valspar.interfaces.wercs.transportation.program;

import com.valspar.interfaces.common.*;
import com.valspar.interfaces.common.beans.ConnectionAccessBean;
import com.valspar.interfaces.common.enums.DataSource;
import com.valspar.interfaces.common.utils.*;
import com.valspar.interfaces.wercs.transportation.beans.ItemIdBean;
import com.valspar.interfaces.wercs.transportation.beans.ProductBean;
import java.sql.*;
import java.util.*;
import oracle.jdbc.*;
import org.apache.commons.lang3.StringUtils;
import org.apache.log4j.Logger;

public class TransportationInterface extends BaseInterface
{
  private static Logger log4jLogger = Logger.getLogger(TransportationInterface.class);
  private OracleConnection wercsConn;
  private ArrayList<DataSource> toErpDatasources = new ArrayList<DataSource>();

  public TransportationInterface()
  {
  }

  public void execute()
  {
     HashMap idMap = new HashMap();
     ArrayList beanList = new ArrayList();
    try
    {
      this.setWercsConn((OracleConnection) ConnectionAccessBean.getConnection(DataSource.WERCS));
      this.setToErpDatasources( (ArrayList<DataSource>)CommonUtility.getERPDataSourceList());
      buildProductBeans(idMap, beanList);
      if (!beanList.isEmpty())
      {
        setRq(beanList);
        writeTransportation(beanList, idMap);
        log4jLogger.info("Done processing the TransportationInterface.");
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

  public void buildProductBeans(HashMap idMap, ArrayList inBeanList)
  {
    OracleStatement buildProductBeansStmt = null;
    ResultSet buildProductBeansRs = null;

    try
    {
      StringBuilder sb = new StringBuilder();
      // First get all of the dual level products
      sb.append("SELECT distinct a.id, a.product, b.f_alias, NULL ");
      sb.append("from vca_transportation_queue a, t_product_alias_names b ");
      sb.append("where a.product = b.f_product ");
      sb.append("and   nvl(b.f_reason, ' ') != 'NODOT' "); 
      sb.append("AND   a.container IS NULL ");
      sb.append("AND   a.date_processed IS NULL ");
      sb.append("AND   a.comments IS NULL ");
     // sb.append("and id= 3682522 ");
      sb.append("UNION ");
      // Second get all of the first time single level products (with container) from the sales order
      sb.append("SELECT distinct  id, product, product, container ");
      sb.append("FROM vca_transportation_queue ");
      sb.append("WHERE container IS NOT NULL ");
      sb.append("AND   date_processed IS NULL ");
      sb.append("AND   comments IS NULL ");
      sb.append("UNION ");
      // Third get all of the re-authorized single level products for NA (get containers from TR_ITEM_MST)
      sb.append("SELECT  distinct a.id, a.product, a.product, c.container ");
      sb.append("FROM vca_transportation_queue a, ic_item_mst@tona b, tr_item_mst@tona c ");
      sb.append("WHERE a.product = b.item_no ");
      sb.append("AND   b.item_id = c.item_id ");
      sb.append("AND   b.itemcost_class LIKE '%SL' ");
      sb.append("AND   a.container IS NULL ");
      sb.append("AND   a.date_processed IS NULL ");
      sb.append("AND   a.comments IS NULL ");
      sb.append("UNION ");
      // Fourth get all of the re-authorized single level products for INTL (get containers from TR_ITEM_MST) - added 04/14/08 kah
      sb.append("SELECT  distinct a.id, a.product, a.product, c.container ");
      sb.append("FROM vca_transportation_queue a, ic_item_mst@toin b, tr_item_mst@toin c ");
      sb.append("WHERE a.product = b.item_no ");
      sb.append("AND   b.item_id = c.item_id ");
      sb.append("AND   b.itemcost_class LIKE '%SL' ");
      sb.append("AND   a.container IS NULL ");
      sb.append("AND   a.date_processed IS NULL ");
      sb.append("AND   a.comments IS NULL ");
      sb.append("UNION ");
      // Fifth get all of the re-authorized single level products for ASIAPAC (get containers from TR_ITEM_MST)
      sb.append("SELECT  distinct a.id, a.product, a.product, c.container ");
      sb.append("FROM vca_transportation_queue a, ic_item_mst@topa b, tr_item_mst@topa c ");
      sb.append("WHERE a.product = b.item_no ");
      sb.append("AND   b.item_id = c.item_id ");
      sb.append("AND   b.itemcost_class LIKE '%SL' ");
      sb.append("AND   a.container IS NULL ");
      sb.append("AND   a.date_processed IS NULL ");
      sb.append("AND   a.comments IS NULL ");


      buildProductBeansStmt = (OracleStatement) this.getWercsConn().createStatement();
      log4jLogger.info("Selecting products to be processed...");
      log4jLogger.info(sb.toString());
      buildProductBeansRs = buildProductBeansStmt.executeQuery(sb.toString());
      log4jLogger.info("Done selecting products to be processed.  Starting to build Product Beans...");

      ThreadGroup tg = new ThreadGroup("TransportationInterface");

      ArrayList<Connection> connectionList = new ArrayList<Connection>();
      Connection landConnection = ConnectionAccessBean.getConnection(DataSource.WERCS);
      connectionList.add(landConnection);
      Connection airConnection = ConnectionAccessBean.getConnection(DataSource.WERCS);
      connectionList.add(airConnection);
      Connection waterConnection = ConnectionAccessBean.getConnection(DataSource.WERCS);
      connectionList.add(waterConnection);

      while (buildProductBeansRs.next())
      {
        String id = buildProductBeansRs.getString(1);
        String bulk = buildProductBeansRs.getString(2);
        String alias = buildProductBeansRs.getString(3);
        String container = buildProductBeansRs.getString(4);

        ArrayList itemDbList = populateItemDbList(alias);

        if (itemDbList.isEmpty())
        {
          updateVcaTransportationQueue(id, false, "NOT IN 11I YET");
        }
        else
        {
          idMap.put(id, null);

          log4jLogger.info("Building beans for alias = " + alias + ", container = " + container);

          Thread landThread = new Thread(tg, new TransportationInterfaceBuildBeansThread(landConnection, id, bulk, alias, container, inBeanList, idMap, this.getToErpDatasources(), itemDbList, "LAND"), "TransportationInterfaceBuildBeans");
          landThread.start();
          Thread airThread = new Thread(tg, new TransportationInterfaceBuildBeansThread(airConnection, id, bulk, alias, container, inBeanList, idMap, this.getToErpDatasources(), itemDbList, "AIR"), "TransportationInterfaceBuildBeans");
          airThread.start();
          Thread waterThread = new Thread(tg, new TransportationInterfaceBuildBeansThread(waterConnection, id, bulk, alias, container, inBeanList, idMap, this.getToErpDatasources(), itemDbList, "WATER"), "TransportationInterfaceBuildBeans");
          waterThread.start();
        }

        while (tg.activeCount() > 0)
        {
        }
      }

      log4jLogger.info("Closing connectionList after WHILE loop");
      DataAccessBean.closeConnectionList(connectionList);

      log4jLogger.info("There are " + inBeanList.size() + " Product Beans in the ArrayList to be processed.");
    }
    catch (Exception e)
    {
      log4jLogger.error(e);
    }
    finally
    {
      JDBCUtil.close(buildProductBeansStmt, buildProductBeansRs);
    }
  }

  public HashMap getDataCodeValues(String product, String[] dataCodes)
  {
    HashMap hm = new HashMap();
    StringBuilder sql = new StringBuilder();
    Statement stmt = null;
    ResultSet rs = null;
    try
    {
      sql.append("select b.f_data_code,b.f_data from t_product_alias_names a, t_prod_data b ");
      sql.append("where a.f_product = b.f_product ");
      sql.append("and b.f_data_code in (");
      for (int i = 0; i < dataCodes.length; i++)
      {
        sql.append(CommonUtility.toVarchar(dataCodes[i]));
        if (i != dataCodes.length - 1)
          sql.append(", ");

        hm.put(dataCodes[i], null);
      }
      sql.append(") and a.f_alias = ");
      sql.append(CommonUtility.toVarchar(product));
 
      stmt = this.getWercsConn().createStatement();
      rs = stmt.executeQuery(sql.toString());

      while (rs.next())
      {
        String f_data_code = rs.getString(1);
        String f_data = rs.getString(2);
        hm.put(f_data_code, f_data);
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
    return hm;
  }

  private ArrayList populateItemDbList(String alias)
  {
    ArrayList ar = new ArrayList();

    try
    {
      StringBuilder sql = new StringBuilder();
      sql.append("select distinct a.item_id, b.inventory_item_id ");
      sql.append("from ic_item_mst a, mtl_system_items b ");
      sql.append("where a.item_no  = ? ");
      sql.append("and   b.segment1 = ? ");

      for (DataSource datasource: this.getToErpDatasources())
      {
        PreparedStatement populateItemDbMapPstmt = null;
        ResultSet populateItemDbMapRs = null;
        OracleConnection conn = null;

        try
        {
          conn = (OracleConnection) ConnectionAccessBean.getConnection(datasource);
          populateItemDbMapPstmt = conn.prepareStatement(sql.toString());

          populateItemDbMapPstmt.setString(1, alias);
          populateItemDbMapPstmt.setString(2, alias);
          populateItemDbMapRs = populateItemDbMapPstmt.executeQuery();

          if (populateItemDbMapRs.next())
          {
            ItemIdBean itemBean = new ItemIdBean();
            itemBean.setDatasource(datasource);
            itemBean.setItemId(populateItemDbMapRs.getString(1));
            itemBean.setInventoryItemId(populateItemDbMapRs.getString(2));
            ar.add(itemBean);
          }
        }
        catch (Exception e)
        {
          log4jLogger.error("while loop in " + ConnectionUtility.buildDatabaseName(conn), e);
        }
        finally
        {
          JDBCUtil.close(populateItemDbMapPstmt, populateItemDbMapRs);
          JDBCUtil.close(conn);
        }
      }
    }
    catch (Exception e)
    {
      log4jLogger.error(e);
    }
    return ar;
  }

  private void setRq(ArrayList inBeanList)
  {
    PreparedStatement setRqPstmt = null;
    ResultSet setRqRs = null;
    try
    {
      ProductBean pb = null;
      Iterator beanIterator = inBeanList.iterator();

      StringBuilder sql = new StringBuilder();
      sql.append("select b.f_chem_name, (b.f_percent/100) * ?, TO_NUMBER(c.f_data) ");
      sql.append("from t_comp_data c, t_prod_comp b, t_product_alias_names a ");
      sql.append("where a.f_product = b.f_product and b.f_component_id = c.f_component_id ");
      sql.append("and c.f_data_code = 'CERCRQ' and a.f_alias = ? ");
      sql.append("and b.f_percent <> 0  order by 2 desc ");

      setRqPstmt = this.getWercsConn().prepareStatement(sql.toString());

      while (beanIterator.hasNext())
      {
        pb = (ProductBean) beanIterator.next();
        setRqPstmt.setInt(1, getFillQty(pb));
        setRqPstmt.setString(2, pb.getProductNumber());
        setRqRs = setRqPstmt.executeQuery();

        while (setRqRs.next())
        {
          if (setRqRs.getInt(2) >= setRqRs.getInt(3))
          {
            if (pb.getHazIngr1() == null)
            {
              pb.setHazIngr1(setRqRs.getString(1));
              pb.setHazQty1(setRqRs.getString(2));
            }
            else if (pb.getHazIngr2() == null)
            {
              pb.setHazIngr2(setRqRs.getString(1));
              pb.setHazQty2(setRqRs.getString(2));
            }
            setEnvHaz(pb);
            pb.setRqFlag(true);
          }
        }
        if (!pb.isRqFlag())
          setNonBulk(pb);
      }
    }
    catch (Exception e)
    {
      log4jLogger.error(e);
    }
    finally
    {
      JDBCUtil.close(setRqPstmt, setRqRs);
    }
  }

  private void writeTransportation(ArrayList inBeanList, HashMap idMap)
  {
    ProductBean pb = null;
    ItemIdBean itemBean = null;
    HashMap callableStatementMap = new HashMap();
    ArrayList<Connection> erpConnectionsList = new ArrayList<Connection>();
    for (DataSource datasource: this.getToErpDatasources())
    {
      erpConnectionsList.add(ConnectionAccessBean.getConnection(datasource));
    }

    try
    {
      String command = "begin VCA_WERCS_POPULATE_TR_ITEM_MST(?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?); end;";
      Iterator beanIterator = inBeanList.iterator();

      for (Connection connection: erpConnectionsList)
      {
        callableStatementMap.put(StringUtils.substring(ConnectionUtility.buildDatabaseName(connection), 0, 2), connection.prepareCall(command));
      }

      while (beanIterator.hasNext())
      {
        pb = (ProductBean) beanIterator.next();
        Iterator connIterator = pb.getItemDbList().iterator();

        while (connIterator.hasNext())
        {
          itemBean = (ItemIdBean) connIterator.next();
          setupCallableStatement(pb, itemBean, (CallableStatement) callableStatementMap.get(itemBean.getDatasource().getInstanceCodeOf11i()), idMap);
        }
        if (idMap.containsKey(pb.getId()))
        {
          updateVcaTransportationQueue(pb.getId(), true, null);
          idMap.remove(pb.getId());
        }
      }

      Iterator i = callableStatementMap.keySet().iterator();

     while (i.hasNext())
      {
        String instanceCodeOf11i = (String) i.next();
        CallableStatement cstmt = (CallableStatement) callableStatementMap.get(instanceCodeOf11i);
        log4jLogger.info("Starting CallableStatement.executeBatch() for " + instanceCodeOf11i);
        cstmt.executeBatch();
        log4jLogger.info("Finished CallableStatement.executeBatch() for " + instanceCodeOf11i);
      }
    }
    catch (Exception e)
    {
      log4jLogger.error("Inserting/Updating TRANSPORTATION for item number " + pb.getProductNumber() + ", ship method = " + pb.getShipMethod() + ", DB = " + itemBean.getDatasource().getInstanceCodeOf11i(), e);
    }
    finally
    {
      Iterator i = callableStatementMap.keySet().iterator();
      while (i.hasNext())
      {
        String databaseName = (String) i.next();
        CallableStatement cstmt = (CallableStatement) callableStatementMap.get(databaseName);
        JDBCUtil.close(cstmt);
        for (Connection connection: erpConnectionsList)
        {
          JDBCUtil.close(connection);
        }
        log4jLogger.info("Finished closing CallableStatement for " + databaseName);
      }
    }
  }

  private void setupCallableStatement(ProductBean pb, ItemIdBean itemBean, CallableStatement cstmt,  HashMap idMap)
  {
    try
    {
      boolean unNumberExist = true;
      if ((itemBean.isEMEAI() || itemBean.isASIAPAC()) && StringUtils.isEmpty(pb.getEuUnNumber()))
      {
        unNumberExist = false;
        log4jLogger.error("Product: " + pb.getProductNumber() + ", Ship Method: " + pb.getShipMethod() + " is missing UN Number for EMEAI/ASIAPAC");
      }
      if (itemBean.isNA() && StringUtils.isEmpty(pb.getUnNumber()))
      {
        unNumberExist = false;
        log4jLogger.error("Product: " + pb.getProductNumber() + ", Ship Method: " + pb.getShipMethod() + " is missing UN Number for NA");
      }

      if (unNumberExist)
      {
        populateHazLabel(pb, false, false);
        cstmt.setString(1, itemBean.getItemId());
        cstmt.setString(2, pb.getShipMethod());
        cstmt.setString(3, pb.getUnNumber());
        cstmt.setString(4, pb.getLimitedQuantity());
        cstmt.setString(5, pb.getIataCode());
        cstmt.setString(6, pb.getPackingGroup());
        cstmt.setString(7, pb.getData(Constants.TRPASS));
        cstmt.setString(8, pb.getData(Constants.TRCARG));
        if (itemBean.isNA())
        {
          if (pb.getText(Constants.DMPT) != null)
          {
            cstmt.setString(9, "YES");
          }
          else
          {
            cstmt.setString(9, "");
          }
        }
        else
        {
          if (pb.getText(Constants.IMPT) != null)
          {
            cstmt.setString(9, "YES");
          }
          else
          {
            cstmt.setString(9, "");
          }
        }

        if (!itemBean.isEMEAI() && pb.getShipMethod().equalsIgnoreCase("LAND"))
        {
          cstmt.setString(10, "");
        }
        else if ("UN3082".equalsIgnoreCase(pb.getEuUnNumber()) && (itemBean.isEMEAI() || itemBean.isASIAPAC()))
        {
          if (pb.getData(Constants.TRMING) != null && (pb.getData(Constants.TRMING).equalsIgnoreCase(pb.getEuHazIngr1()) || pb.getData(Constants.TRMING).equalsIgnoreCase(pb.getEuHazIngr2()) || pb.getData(Constants.TRMING).equalsIgnoreCase(pb.getHazIngr3())))
          {
            if (pb.getData(Constants.TRMING2) == null || (pb.getData(Constants.TRMING2) != null && (pb.getData(Constants.TRMING2).equalsIgnoreCase(pb.getEuHazIngr1()) || pb.getData(Constants.TRMING2).equalsIgnoreCase(pb.getEuHazIngr2()) || pb.getData(Constants.TRMING2).equalsIgnoreCase(pb.getHazIngr3()))))
            {
              cstmt.setString(10, "");
            }
            else
            {
              cstmt.setString(10, pb.getData(Constants.TRMING2));
            }
          }
          else
          {
            if (pb.getData(Constants.TRMING2) != null)
            {
              cstmt.setString(10, pb.getData(Constants.TRMING) + ", " + pb.getData(Constants.TRMING2));
            }
            else
            {
              cstmt.setString(10, pb.getData(Constants.TRMING));
            }
          }
        }
        else if ("UN3082".equalsIgnoreCase(pb.getUnNumber()) && !itemBean.isEMEAI() && !itemBean.isASIAPAC())
        {
          if (pb.getData(Constants.TRMING) != null && (pb.getData(Constants.TRMING).equalsIgnoreCase(pb.getHazIngr1()) || pb.getData(Constants.TRMING).equalsIgnoreCase(pb.getHazIngr2()) || pb.getData(Constants.TRMING).equalsIgnoreCase(pb.getHazIngr3())))
          {
            if (pb.getData(Constants.TRMING2) == null || (pb.getData(Constants.TRMING2) != null && (pb.getData(Constants.TRMING2).equalsIgnoreCase(pb.getHazIngr1()) || pb.getData(Constants.TRMING2).equalsIgnoreCase(pb.getHazIngr2()) || pb.getData(Constants.TRMING2).equalsIgnoreCase(pb.getHazIngr3()))))
            {
              cstmt.setString(10, "");
            }
            else
            {
              cstmt.setString(10, pb.getData(Constants.TRMING2));
            }
          }
          else
          {
            if (pb.getData(Constants.TRMING2) != null)
            {
              cstmt.setString(10, pb.getData(Constants.TRMING) + ", " + pb.getData(Constants.TRMING2));
            }
            else
            {
              cstmt.setString(10, pb.getData(Constants.TRMING));
            }
          }
        }
        else if (pb.getData(Constants.TRMING2) != null)
        {
          cstmt.setString(10, pb.getData(Constants.TRMING) + ", " + pb.getData(Constants.TRMING2));
        }
        else
        {
          cstmt.setString(10, pb.getData(Constants.TRMING));
        }
        
        if (itemBean.isEMEAI() || itemBean.isASIAPAC())
        {
          cstmt.setString(11, pb.getEuHazIngr1());
          cstmt.setString(12, pb.getEuHazIngr2());
        }
        else
        {
          cstmt.setString(11, pb.getHazIngr1());
          cstmt.setString(12, pb.getHazIngr2());
        }

        cstmt.setString(13, pb.getHazIngr3());
        if (itemBean.isEMEAI() || itemBean.isASIAPAC())
        {
          cstmt.setString(14, pb.getEuHazQty1());
          cstmt.setString(15, pb.getEuHazQty2());
        }
        else
        {
          cstmt.setString(14, pb.getHazQty1());
          cstmt.setString(15, pb.getHazQty2());
        }

        cstmt.setString(16, pb.getHazQty3());
        cstmt.setString(17, pb.getData(Constants.TRNBUN));

        //If it's Intl then use Celsius, else use Fahrenheit
        if (itemBean.isEMEAI() || itemBean.isASIAPAC())
          cstmt.setString(18, pb.getData(Constants.FPC));
        else
          cstmt.setString(18, pb.getData(Constants.FPF));
        cstmt.setString(19, pb.getAdrNumber());
        cstmt.setString(20, pb.getMfagNo());
        if(pb.getShipMethod().equalsIgnoreCase("WATR"))
          cstmt.setString(21, pb.getData(Constants.EMS));
        else
          cstmt.setString(21, " ");
        cstmt.setString(22, pb.getData(Constants.SEC));
        cstmt.setString(23, pb.getImdgPage());
        cstmt.setString(24, pb.getTransCount());
        cstmt.setString(25, itemBean.getDatasource().getLogUser());
        cstmt.setString(26, itemBean.getDatasource().getLogUser());
        cstmt.setString(27, pb.getDeleteMark());
        cstmt.setString(28, pb.getUnNumSeq());
        cstmt.setString(29, pb.getZLabel());
        cstmt.setString(30, itemBean.getInventoryItemId());
        cstmt.setString(31, pb.getShippingName());
        cstmt.setString(32, pb.getHazardClass());
        cstmt.setString(33, pb.getSubsidaryRisk());
        cstmt.setString(34, pb.getErgCode());
        cstmt.setString(35, pb.getHazLabel1());
        cstmt.setString(36, pb.getHazLabel2());
        cstmt.setString(37, pb.getHazLabel3());
        cstmt.setString(38, itemBean.getDatasource().getLogUser());
        cstmt.setString(39, itemBean.getDatasource().getLogUser());
        cstmt.setString(40, itemBean.getDatasource().getLogUser());
        cstmt.setString(41, pb.getData(Constants.VOCGL));
        cstmt.setString(42, pb.getEuUnNumber() != null? pb.getEuUnNumber(): pb.getUnNumber());
        cstmt.setString(43, pb.getEuPackingGroup());
        cstmt.setString(44, pb.getData(Constants.TRADSP));
        cstmt.setString(45, pb.getViscosityException());
        cstmt.setString(46, pb.getData(Constants.ENVHAZA));
        cstmt.setString(47, pb.getData(Constants.ENVHAZB));
        cstmt.setString(48, pb.getContainer());
        cstmt.setString(49, pb.getData(Constants.VOCPCT));
        if(pb.getShipMethod().equalsIgnoreCase("LAND") || pb.getShipMethod().equalsIgnoreCase("WATR"))
          cstmt.setString(50, pb.getData(Constants.TRCA));
        else
          cstmt.setString(50, " ");
        cstmt.setString(51, pb.getData(Constants.MIR));
        cstmt.setString(52, pb.getId());
       cstmt.addBatch();
        log4jLogger.info("Added to the batch to be processed: item_id = " + itemBean.getItemId() + ", item number = " + pb.getProductNumber() + ", ship method = " + pb.getShipMethod() + ", container = " + pb.getContainer() + ", DB = " + itemBean.getDatasource().getInstanceCodeOf11i());
      }
    }
    catch (Exception e)
    {
      log4jLogger.error("Inserting/Updating TRANSPORTATION for item number " + pb.getProductNumber() + ", ship method = " + pb.getShipMethod() + ", container = " + pb.getContainer() + ", DB = " + itemBean.getDatasource().getInstanceCodeOf11i() + ", Bulk " + pb.getBulk() + " will not get marked as complete.", e);
      idMap.remove(pb.getId());
      updateVcaTransportationQueue(pb.getId(), false, "Error in writeTransportation() Inserting/Updating TRANSPORTATION for item number " + pb.getProductNumber() + ", ship method = " + pb.getShipMethod() + ", container = " + pb.getContainer() + ", DB = " + itemBean.getDatasource().getInstanceCodeOf11i() + ", Error = " + e + " Bulk " + pb.getBulk() + " will not get marked as complete.");
    }
  }

  private void updateVcaTransportationQueue(String id, boolean transporationComplete, String comments)
  {
    Statement stmt = null;
    try
    {
      stmt = this.getWercsConn().createStatement();

      StringBuilder sql = new StringBuilder();
      sql.append("UPDATE VCA_TRANSPORTATION_QUEUE SET ");
      if (transporationComplete)
        sql.append("DATE_PROCESSED = SYSDATE");
      else
      {
        sql.append("COMMENTS = '");
        sql.append(comments);
        sql.append("'");
      }
      sql.append(" WHERE ID = ");
      sql.append(id);

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

  private int getFillQty(ProductBean pb)
  {
    int fillQty = 0;
    PreparedStatement getFillQtyPstmt = null;
    ResultSet getFillQtyRs = null;
    try
    {
      StringBuilder sb = new StringBuilder();
      sb.append("select ROUND(TO_NUMBER(DECODE(a.uom, 'GAL', b.f_data * a.qty, a.qty)),2) ");
      sb.append("from t_product_alias_names c, t_prod_data b, va_container_qty a ");
      sb.append("where c.f_product = b.f_product ");
      sb.append("and   b.f_data_code = 'DENSLB' ");
      sb.append("and   ? like a.extension ");
      sb.append("and   c.f_alias = ? ");
      sb.append("order by a.wild_card ");

      getFillQtyPstmt = this.getWercsConn().prepareStatement(sb.toString());

      getFillQtyPstmt.setString(1, pb.getProductExtension() != null? pb.getProductExtension(): Constants.EMPTY_STRING);
      getFillQtyPstmt.setString(2, pb.getProductNumber());
      getFillQtyRs = getFillQtyPstmt.executeQuery();

      if (getFillQtyRs.next())
        fillQty = getFillQtyRs.getInt(1);

    }
    catch (Exception e)
    {
      log4jLogger.error("item " + pb.getProductNumber(), e);
    }
    finally
    {
      JDBCUtil.close(getFillQtyPstmt, getFillQtyRs);
    }
    return fillQty;
  }

  private void setEnvHaz(ProductBean pb) //TODO
  {
    try
    {
      if (StringUtils.endsWith(pb.getUnNumber(), "CO"))
        pb.setData(Constants.TRNBUN, pb.getUnNumber());
      if (StringUtils.startsWith(pb.getUnNumber(), "NR"))
      {
        pb.setUnNumber("UN3082");
        pb.setPackingGroup("III");
        populateHazLabel(pb, false, false);
      }
    }
    catch (Exception e)
    {
      log4jLogger.error(e);
    }
  }

  private void setNonBulk(ProductBean pb) //TODO
  {
    try
    {
      if (StringUtils.endsWith(pb.getUnNumber(), "CO") && !pb.isBulkFlag() && pb.getData(Constants.TRNBUN) != null)
      {
        pb.setUnNumber(pb.getData(Constants.TRNBUN));
        pb.setPackingGroup(null);
        pb.setHazardClass(null);
        pb.setSubsidaryRisk(null);
        pb.setErgCode(null);
        pb.setHazLabel1(null);
        pb.setHazLabel2(null);
        pb.setHazLabel3(null);
      }
    }
    catch (Exception e)
    {
      log4jLogger.error("product " + pb.getProductNumber(), e);
    }
  }
  
  private void populateHazLabel(ProductBean pb, boolean isEMEAIInstance, boolean isASIAPACInstance)
  {
    PreparedStatement populateVaDotsMstDataPstmt = null;
    ResultSet populateVaDotsMstDataRs = null;
    try
    {
      StringBuilder sb = new StringBuilder();
      sb.append("select haz_label1, haz_label2, haz_label3 ");
      sb.append("from va_dots_mst ");
      sb.append("where un_number = ? and ship_mthd = ? and language = 'EN'");

      populateVaDotsMstDataPstmt = this.getWercsConn().prepareStatement(sb.toString());

      if (isEMEAIInstance || isASIAPACInstance)
      {
        populateVaDotsMstDataPstmt.setString(1, pb.getEuUnNumber());
      }
      else
      {
        populateVaDotsMstDataPstmt.setString(1, pb.getUnNumber());
      }
      populateVaDotsMstDataPstmt.setString(2, pb.getShipMethod());

      populateVaDotsMstDataRs = populateVaDotsMstDataPstmt.executeQuery();
      if (populateVaDotsMstDataRs.next())
      {
        pb.setHazLabel1(populateVaDotsMstDataRs.getString(1));
        pb.setHazLabel2(populateVaDotsMstDataRs.getString(2));
        pb.setHazLabel3(populateVaDotsMstDataRs.getString(3));
      }
      else
      {
        if (isEMEAIInstance || isASIAPACInstance)
        {
          log4jLogger.error("EU UN Number - " + pb.getEuUnNumber() + " does not exist in VA_DOTS_MST.");
        }
        else
        {
          log4jLogger.error("US UN Number - " + pb.getUnNumber() + " does not exist in VA_DOTS_MST.");
        }
      }
    }
    catch (Exception e)
    {
      log4jLogger.error(e);
    }
    finally
    {
      JDBCUtil.close(populateVaDotsMstDataPstmt, populateVaDotsMstDataRs);
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

  public void setToErpDatasources(ArrayList<DataSource> toErpDatasources)
  {
    this.toErpDatasources = toErpDatasources;
  }

  public ArrayList<DataSource> getToErpDatasources()
  {
    return toErpDatasources;
  }
}
