package com.valspar.interfaces.regulatory.dot.program;

import com.valspar.interfaces.common.*;
import com.valspar.interfaces.common.beans.ConnectionAccessBean;
import com.valspar.interfaces.common.enums.DataSource;
import com.valspar.interfaces.common.utils.*;
import com.valspar.interfaces.regulatory.dot.beans.*;
import java.sql.*;
import java.util.*;
import oracle.jdbc.*;
import org.apache.commons.lang3.*;
import org.apache.log4j.Logger;

public class DotInterfaceBuildBeansThread implements Runnable, Constants
{
  private static Logger log4jLogger = Logger.getLogger(DotInterfaceBuildBeansThread.class);

  private Connection conn;
  private String id;
  private String bulk;
  private String alias;
  private String container;
  private ArrayList ar;
  private HashMap idMap;
  private ArrayList<DataSource> toErpDatasourceList = new ArrayList<DataSource>();
  private ArrayList itemDbList = new ArrayList();
  private String type;

  public DotInterfaceBuildBeansThread(Connection conn, String id, String bulk, String alias, String container, ArrayList ar, HashMap idMap, ArrayList<DataSource> toErpDatasources, ArrayList itemDbList, String type)
  {
    this.setConn(conn);
    this.setId(id);
    this.setBulk(bulk);
    this.setAlias(alias);
    this.setContainer(container);
    this.setAr(ar);
    this.setIdMap(idMap);
    this.setToErpDatasourceList(toErpDatasources);
    this.setItemDbList(itemDbList);
    this.setType(type);
  }

  public void run()
  {
    if (StringUtils.equalsIgnoreCase(this.getType(), "LAND"))
    {
      buildProductLandBean(id, bulk, alias, container, ar, idMap);
    }
    else if (StringUtils.equalsIgnoreCase(this.getType(), "AIR"))
    {
      buildProductAirBean(id, bulk, alias, container, ar, idMap);
    }
    else if (StringUtils.equalsIgnoreCase(this.getType(), "WATER"))
    {
      buildProductWaterBean(id, bulk, alias, container, ar, idMap);
    }
  }

  private void buildProductLandBean(String id, String bulk, String alias, String container, ArrayList ar, HashMap idMap)
  {
    try
    {
      ProductBean lb = new ProductBean();
      lb.setDataCodes(getDataCodeValues(bulk, LAND_DATA_CODES));
      lb.setUnNumber(lb.getData(TRUN));
      lb.setEuUnNumber(lb.getData(TRADUN));
      lb.setPackingGroup(lb.getData(TRPKG));
      lb.setEuPackingGroup(lb.getData(TRADPCK));
      lb.setId(id);
      lb.setProductNumber(alias);
      lb.setShipMethod(LAND);
      lb.setHazIngr3(getNosIngredients(lb));
      lb.setEuHazIngr1(lb.getData(TRADEH1));
      lb.setEuHazIngr2(lb.getData(TRADEH2));
      lb.setBulk(bulk);
      lb.setContainer(container);
      populateItemDbList(lb);
      lb.setImdgPage(null);
      lb.setIataCode(null);
      lb.setAdrNumber(lb.getData(TRADCL));
      lb.setTransCount(ZERO);
      lb.setDeleteMark(ZERO);
      lb.setHazQty1(ZERO);
      lb.setHazQty2(ZERO);
      lb.setHazQty3(ZERO);
      lb.setEuHazQty1(ZERO);
      lb.setEuHazQty2(ZERO);
      lb.setUnNumSeq(null);
      lb.setZLabel(null);
      lb.setLimitedQuantity(getLimitedQty(lb));
      lb.setBulkFlag(isBulk(lb));
      lb.setViscosityException(getViscosityEx(lb));
      lb.setItemDbList(this.getItemDbList());

      ar.add(lb);
    }
    catch (Exception e)
    {
      log4jLogger.error(e);
    }
  }

  private void buildProductAirBean(String id, String bulk, String alias, String container, ArrayList ar, HashMap idMap)
  {
    try
    {
      ProductBean ab = new ProductBean();
      ab.setDataCodes(getDataCodeValues(bulk, AIR_DATA_CODES));
      ab.setUnNumber(ab.getData(TRAUN));
      ab.setEuUnNumber(ab.getData(TRIAUN));
      ab.setPackingGroup(ab.getData(TRAPK));
      ab.setEuPackingGroup(ab.getData(TRIAPG));
      ab.setId(id);
      ab.setProductNumber(alias);
      ab.setShipMethod(AIR);
      ab.setHazIngr3(getNosIngredients(ab));
      ab.setEuHazIngr1(ab.getData(TRIAEH1));
      ab.setEuHazIngr2(ab.getData(TRIAEH2));
      ab.setBulk(bulk);
      ab.setContainer(container);
      populateItemDbList(ab);
      ab.setImdgPage(null);
      ab.setIataCode(ab.getData(TRIACL));
      ab.setAdrNumber(null);
      ab.setTransCount(ZERO);
      ab.setDeleteMark(ZERO);
      ab.setHazQty1(ZERO);
      ab.setHazQty2(ZERO);
      ab.setHazQty3(ZERO);
      ab.setEuHazQty1(ZERO);
      ab.setEuHazQty2(ZERO);
      ab.setUnNumSeq(null);
      ab.setZLabel(null);
      ab.setLimitedQuantity(getLimitedQty(ab));
      ab.setBulkFlag(isBulk(ab));
      ab.setViscosityException(getViscosityEx(ab));
      ab.setItemDbList(this.getItemDbList());

      ar.add(ab);
    }
    catch (Exception e)
    {
      log4jLogger.error(e);
    }
  }

  private void buildProductWaterBean(String id, String bulk, String alias, String container, ArrayList ar, HashMap idMap)
  {
    try
    {
      ProductBean wb = new ProductBean();
      wb.setDataCodes(getDataCodeValues(bulk, WATR_DATA_CODES));
      wb.setUnNumber(wb.getData(TRWUN));
      wb.setEuUnNumber(wb.getData(TRIMUN));
      wb.setPackingGroup(wb.getData(TRWPK));
      wb.setEuPackingGroup(wb.getData(TRIMPG));
      wb.setId(id);
      wb.setProductNumber(alias);
      wb.setShipMethod(WATR);
      wb.setHazIngr3(getNosIngredients(wb));
      wb.setEuHazIngr1(wb.getData(TRIMEH1));
      wb.setEuHazIngr2(wb.getData(TRIMEH2));
      wb.setBulk(bulk);
      wb.setContainer(container);
      populateItemDbList(wb);
      wb.setImdgPage(wb.getData(TRIMCL));
      wb.setIataCode(null);
      wb.setAdrNumber(null);
      wb.setTransCount(ZERO);
      wb.setDeleteMark(ZERO);
      wb.setHazQty1(ZERO);
      wb.setHazQty2(ZERO);
      wb.setHazQty3(ZERO);
      wb.setEuHazQty1(ZERO);
      wb.setEuHazQty2(ZERO);
      wb.setUnNumSeq(null);
      wb.setZLabel(null);
      wb.setLimitedQuantity(getLimitedQty(wb));
      wb.setBulkFlag(isBulk(wb));
      wb.setViscosityException(getViscosityEx(wb));
      wb.setItemDbList(this.getItemDbList());

      ar.add(wb);
    }
    catch (Exception e)
    {
      log4jLogger.error(e);
    }
  }

  public HashMap getDataCodeValues(String product, String[] dataCodes)
  {
    HashMap hm = new HashMap();
    StringBuffer sql = new StringBuffer();

    Statement stmt = null;
    ResultSet rs = null;

    try
    {
      Connection conn = this.getConn();

      sql.append("select b.f_data_code,b.f_data from t_product_alias_names a, t_prod_data b ");
      sql.append("where a.f_product = b.f_product ");
      sql.append("and b.f_data_code in (");
      for (int i = 0; i < dataCodes.length; i++)
      {
        sql.append("'");
        sql.append(dataCodes[i]);
        sql.append("'");
        if (i != dataCodes.length - 1)
          sql.append(", ");

        hm.put(dataCodes[i], null);
      }
      sql.append(") and a.f_alias = '");
      sql.append(product);
      sql.append("' ");

      stmt = conn.createStatement();
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

  private String getNosIngredients(ProductBean pb)
  {
    String returnString = "";
    String ingred1 = null;
    String ingred2 = null;

    if (StringUtils.equalsIgnoreCase(pb.getShipMethod(), LAND))
    {
      ingred1 = pb.getData(TRLNG1);
      ingred2 = pb.getData(TRLNG2);
    }
    else if (StringUtils.equalsIgnoreCase(pb.getShipMethod(), AIR))
    {
      ingred1 = pb.getData(TRANG1);
      ingred2 = pb.getData(TRANG2);
    }
    else if (StringUtils.equalsIgnoreCase(pb.getShipMethod(), WATR))
    {
      ingred1 = pb.getData(TRWNG1);
      ingred2 = pb.getData(TRWNG2);
    }

    int ingred1Length = StringUtils.length(ingred1);
    int ingred2Length = StringUtils.length(ingred2);

    if (StringUtils.isNotEmpty(ingred1) && StringUtils.isNotEmpty(ingred2))
    {
      if (ingred1Length + ingred2Length >= 60)
      {
        if (ingred1Length >= 30 && ingred2Length < 30)
        {
          ingred1 = StringUtils.substring(ingred1, 0, 59 - ingred2Length);
        }
        else if (ingred1Length < 30 && ingred2Length >= 30)
        {
          ingred2 = StringUtils.substring(ingred2, 0, 59 - ingred1Length);
        }
        else
        {
          ingred1 = StringUtils.substring(ingred1, 0, 30);
          ingred2 = StringUtils.substring(ingred2, 0, 29);
        }
      }
      returnString = ingred1 + "," + ingred2;
    }
    else if (StringUtils.isNotEmpty(ingred1))
    {
      returnString = StringUtils.substring(ingred1, 0, 60);
    }
    else if (StringUtils.isNotEmpty(ingred2))
    {
      returnString = StringUtils.substring(ingred2, 0, 60);
    }

    return returnString;
  }

  private void populateItemDbList(ProductBean pb)
  {
    ArrayList ar = new ArrayList();
    try
    {
      for (DataSource datasource: this.getToErpDatasourceList())
      {
        StringBuffer sql = new StringBuffer();
        sql.append("select distinct a.item_id, b.inventory_item_id ");
        sql.append("from ic_item_mst a, mtl_system_items b ");
        sql.append("where a.item_no  = ? ");
        sql.append("and   b.segment1 = ? ");

        PreparedStatement populateItemDbMapPstmt = null;
        ResultSet populateItemDbMapRs = null;
        OracleConnection conn = null;

        try
        {
          conn = (OracleConnection) ConnectionAccessBean.getConnection(datasource);
          populateItemDbMapPstmt = conn.prepareStatement(sql.toString());

          populateItemDbMapPstmt.setString(1, pb.getProductNumber());
          populateItemDbMapPstmt.setString(2, pb.getProductNumber());
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
          log4jLogger.error("While loop in " + ConnectionUtility.buildDatabaseName(conn), e);
        }
        finally
        {
          JDBCUtil.close(populateItemDbMapPstmt, populateItemDbMapRs);
          JDBCUtil.close(conn);
        }
      }
      pb.setItemDbList(ar);
    }
    catch (Exception e)
    {
      log4jLogger.error(e);
    }
  }

  private String getLimitedQty(ProductBean pb)
  {
    String limitedQty = ZERO;
    PreparedStatement getLimitQtyPstmt = null;
    ResultSet getLimitQtyRs = null;
    try
    {
      Connection conn = this.getConn();
      getLimitQtyPstmt = conn.prepareStatement("select * from va_limited_qty where un_number = ? and pkg_group = ? and ? like extension");

      String unNumber = pb.getUnNumber() != null? pb.getUnNumber(): EMPTY_STRING;
      String pkgGroup = pb.getPackingGroup() != null? pb.getPackingGroup(): EMPTY_STRING;
      String prodExt = pb.getProductExtension() != null? pb.getProductExtension(): EMPTY_STRING;

      getLimitQtyPstmt.setString(1, unNumber);
      getLimitQtyPstmt.setString(2, pkgGroup);
      getLimitQtyPstmt.setString(3, prodExt);

      getLimitQtyRs = getLimitQtyPstmt.executeQuery();

      if (getLimitQtyRs.next())
        limitedQty = ONE;

      if (pb.getShipMethod().equalsIgnoreCase(LAND))
      {
        if ((limitedQty.equalsIgnoreCase(ONE)) && (getLimitQtyRs.getString(7) != null))
        {
          pb.setUnNumber(getLimitQtyRs.getString(7));
          pb.setShippingName(getLimitQtyRs.getString(8));
          pb.setShipMethod(LAND);
          pb.setHazardClass(EMPTY_STRING);
          pb.setPackingGroup(EMPTY_STRING);
          limitedQty = ZERO;
        }
      }
    }
    catch (Exception e)
    {
      log4jLogger.error("for product " + pb.getProductNumber(), e);
    }
    finally
    {
      JDBCUtil.close(getLimitQtyPstmt, getLimitQtyRs);
    }
    return limitedQty;
  }

  private String getViscosityEx(ProductBean pb)
  {
    String viscostityEx = ZERO;

    if (pb.getData(TRMPOL) != null || !StringUtils.equalsIgnoreCase(pb.getShipMethod(), AIR))
    {
      Statement stmt = null;
      ResultSet rs = null;
      try
      {
        Connection conn = this.getConn();

        StringBuffer sql = new StringBuffer();
        sql.append("select GET_VISCOSITY_EXCEPTION('");
        sql.append(pb.getBulk());
        sql.append("', '");
        sql.append(pb.getShipMethod());
        sql.append("', '");
        sql.append(pb.getProductExtension());
        //sql.append("', 'N') from dual");
        sql.append("', '");
        if (pb.getData(TRMPOL) != null)
          sql.append("Y");
        else
          sql.append("N");
        sql.append("') from dual");

        stmt = conn.createStatement();
        ((OracleStatement) stmt).defineColumnType(1, Types.VARCHAR);
        rs = stmt.executeQuery(sql.toString());
        if (rs.next())
          viscostityEx = rs.getString(1);
      }
      catch (Exception e)
      {
        log4jLogger.error("for product " + pb.getProductNumber(), e);
      }
      finally
      {
        JDBCUtil.close(stmt, rs);
      }
    }
    return viscostityEx;
  }

  private boolean isBulk(ProductBean pb)
  {
    boolean isBulkValue = false;
    PreparedStatement isBulkPstmt = null;
    ResultSet isBulkRs = null;
    try
    {
      Connection conn = this.getConn();

      int qty = 0;

      isBulkPstmt = conn.prepareStatement("select qty from va_container_qty where ? like extension and uom = 'GAL' order by wild_card");

      isBulkPstmt.setString(1, pb.getProductExtension() != null? pb.getProductExtension(): EMPTY_STRING);
      isBulkRs = isBulkPstmt.executeQuery();

      if (isBulkRs.next())
        qty = isBulkRs.getInt(1);

      if (qty > 120)
        isBulkValue = true;
    }
    catch (Exception e)
    {
      log4jLogger.error("for product " + pb.getProductNumber(), e);
    }
    finally
    {
      JDBCUtil.close(isBulkPstmt, isBulkRs);
    }
    return isBulkValue;
  }

  public void setConn(Connection conn)
  {
    this.conn = conn;
  }

  public Connection getConn()
  {
    return conn;
  }

  public void setId(String id)
  {
    this.id = id;
  }

  public String getId()
  {
    return id;
  }

  public void setBulk(String bulk)
  {
    this.bulk = bulk;
  }

  public String getBulk()
  {
    return bulk;
  }

  public void setAlias(String alias)
  {
    this.alias = alias;
  }

  public String getAlias()
  {
    return alias;
  }

  public void setContainer(String container)
  {
    this.container = container;
  }

  public String getContainer()
  {
    return container;
  }

  public void setAr(ArrayList ar)
  {
    this.ar = ar;
  }

  public ArrayList getAr()
  {
    return ar;
  }

  public void setIdMap(HashMap idMap)
  {
    this.idMap = idMap;
  }

  public HashMap getIdMap()
  {
    return idMap;
  }

  public void setItemDbList(ArrayList itemDbList)
  {
    this.itemDbList = itemDbList;
  }

  public ArrayList getItemDbList()
  {
    return itemDbList;
  }

  public void setType(String type)
  {
    this.type = type;
  }

  public String getType()
  {
    return type;
  }

  public void setToErpDatasourceList(ArrayList<DataSource> toErpDatasourceList)
  {
    this.toErpDatasourceList = toErpDatasourceList;
  }

  public ArrayList<DataSource> getToErpDatasourceList()
  {
    return toErpDatasourceList;
  }
}
