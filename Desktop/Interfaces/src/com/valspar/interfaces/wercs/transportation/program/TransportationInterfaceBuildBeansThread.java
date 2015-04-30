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
import org.apache.commons.lang3.*;
import org.apache.log4j.Logger;

public class TransportationInterfaceBuildBeansThread implements Runnable, Constants
{
  private static Logger log4jLogger = Logger.getLogger(TransportationInterfaceBuildBeansThread.class);

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

  public TransportationInterfaceBuildBeansThread(Connection conn, String id, String bulk, String alias, String container, ArrayList ar, HashMap idMap, ArrayList<DataSource> toErpDatasources, ArrayList itemDbList, String type)
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
      buildProductLandBean(getId(), getBulk(), getAlias(), getContainer(), getAr());
    }
    else if (StringUtils.equalsIgnoreCase(this.getType(), "AIR"))
    {
      buildProductAirBean(getId(), getBulk(), getAlias(), getContainer(), getAr());
    }
    else if (StringUtils.equalsIgnoreCase(this.getType(), "WATER"))
    {
      buildProductWaterBean(getId(), getBulk(), getAlias(), getContainer(), getAr());
    }
  }

  private void buildProductLandBean(String id, String bulk, String alias, String container, ArrayList ar)
  {
    try
    {
      ProductBean lb = new ProductBean();
      lb.setDataCodes(getDataCodeValues(bulk, TRANS_LAND_DATA_CODES));
      lb.setTextCodes(getTextCodeValues(bulk, TRANS_LAND_TEXT_CODES));
      if (isTextCodeExist(bulk, "NRREG001"))
      {
        lb.setUnNumber("NRREG");
        lb.setEuUnNumber("NRREG");
        lb.setShippingName("NOT REGULATED");
        lb.setEuShippingName("NOT REGULATED");
      }
      else
      {
        lb.setUnNumber(lb.getData(UND));
        lb.setEuUnNumber(lb.getData(UNA));
        lb.setShippingName(getTextPhrase(lb.getText(PSND)));
        lb.setEuShippingName(lb.getText(PSNA));
      }
      lb.setHazardClass(lb.getData(HCD));
      lb.setEuHazardClass(lb.getData(HCA));
      lb.setSubsidaryRisk(lb.getData(SCD));
      lb.setEuSubsidaryRisk(lb.getData(SCA));
      lb.setPackingGroup(lb.getData(PGD));
      lb.setEuPackingGroup(lb.getData(PGA));
      lb.setErgCode(lb.getData(DERGN));
      lb.setHazLabel1(lb.getData(DOTHL1));
      lb.setEuHazLabel1(lb.getData(ADRHL1));
      lb.setHazLabel2(lb.getData(DOTHL2));
      lb.setEuHazLabel2(lb.getData(ADRHL2));
      lb.setHazLabel3(lb.getData(DOTHL3));
      lb.setEuHazLabel3(lb.getData(ADRHL3));
      lb.setId(id);
      lb.setProductNumber(alias);
      lb.setShipMethod(LAND);
      lb.setHazIngr3(getNosIngredients(lb));
      lb.setEuHazIngr1(lb.getData(TND1));
      lb.setEuHazIngr2(lb.getData(TND2));
      lb.setBulk(bulk);
      lb.setContainer(container);
      populateItemDbList(lb);
      lb.setImdgPage(null);
      lb.setIataCode(null);
      lb.setAdrNumber(lb.getData(HCA));
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
      //lb.setLimitedQuantity(lb.getData(ALQ));
      lb.setBulkFlag(isBulk(lb));
      lb.setViscosityException(getViscosityEx(lb, "NA"));
      lb.setEuViscosityException(getViscosityEx(lb, ""));
      lb.setItemDbList(this.getItemDbList());

      ar.add(lb);
    }
    catch (Exception e)
    {
      log4jLogger.error(e);
    }
  }

  private void buildProductAirBean(String id, String bulk, String alias, String container, ArrayList ar)
  {
    try
    {
      ProductBean ab = new ProductBean();
      ab.setDataCodes(getDataCodeValues(bulk, TRANS_AIR_DATA_CODES));
      ab.setTextCodes(getTextCodeValues(bulk, TRANS_AIR_TEXT_CODES));
      if (isTextCodeExist(bulk, "NRREG001"))
      {
        ab.setUnNumber("NRREG");
        ab.setEuUnNumber("NRREG");
        ab.setShippingName("NOT REGULATED");
        ab.setEuShippingName("NOT REGULATED");
      }
      else
      {
        ab.setUnNumber(ab.getData(UNI));
        ab.setEuUnNumber(ab.getData(UNI));
        ab.setShippingName(getTextPhrase(ab.getText(PSNI)));
        ab.setEuShippingName(ab.getText(PSNI));
      }
      ab.setHazardClass(ab.getData(HCI));
      ab.setEuHazardClass(ab.getData(HCI));
      ab.setSubsidaryRisk(ab.getData(SCI));
      ab.setEuSubsidaryRisk(ab.getData(SCI));
      ab.setPackingGroup(ab.getData(PGI));
      ab.setEuPackingGroup(ab.getData(PGI));
      // ab.setErgCode(ab.getData(ERGC));
      ab.setHazLabel1(ab.getData(IATAHL1));
      ab.setEuHazLabel1(ab.getData(IATAHL1));
      ab.setHazLabel2(ab.getData(IATAHL2));
      ab.setEuHazLabel2(ab.getData(IATAHL2));
      ab.setHazLabel3(ab.getData(IATAHL3));
      ab.setEuHazLabel3(ab.getData(IATAHL3));
      ab.setId(id);
      ab.setProductNumber(alias);
      ab.setShipMethod(AIR);
      ab.setHazIngr3(getNosIngredients(ab));
      ab.setEuHazIngr1(ab.getData(TND1));
      ab.setEuHazIngr2(ab.getData(TND2));
      ab.setBulk(bulk);
      ab.setContainer(container);
      populateItemDbList(ab);
      ab.setImdgPage(null);
      ab.setIataCode(ab.getData(HCI));
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
      //  ab.setLimitedQuantity(ab.getData(ITLQ));
      ab.setLimitedQuantity(getLimitedQty(ab));
      ab.setBulkFlag(isBulk(ab));
      ab.setViscosityException(ZERO);
      ab.setEuViscosityException(ZERO);
      ab.setItemDbList(this.getItemDbList());

      ar.add(ab);
    }
    catch (Exception e)
    {
      log4jLogger.error(e);
    }
  }

  private void buildProductWaterBean(String id, String bulk, String alias, String container, ArrayList ar)
  {
    try
    {
      ProductBean wb = new ProductBean();
      wb.setDataCodes(getDataCodeValues(bulk, TRANS_WATR_DATA_CODES));
      wb.setTextCodes(getTextCodeValues(bulk, TRANS_WATR_TEXT_CODES));

      if (isTextCodeExist(bulk, "NRREG001"))
      {
        wb.setUnNumber("NRREG");
        wb.setEuUnNumber("NRREG");
        wb.setShippingName("NOT REGULATED");
        wb.setEuShippingName("NOT REGULATED");
      }
      else
      {
        wb.setUnNumber(wb.getData(UNM));
        wb.setEuUnNumber(wb.getData(UNM));
        wb.setShippingName(getTextPhrase(wb.getText(PSNM)));
        wb.setEuShippingName(wb.getText(PSNM));
      }
      wb.setHazardClass(wb.getData(HCM));
      wb.setEuHazardClass(wb.getData(HCM));
      wb.setSubsidaryRisk(wb.getData(SCM));
      wb.setEuSubsidaryRisk(wb.getData(SCM));
      wb.setPackingGroup(wb.getData(PGM));
      wb.setEuPackingGroup(wb.getData(PGM));
      //wb.setErgCode(wb.getData(ERGC));
      wb.setHazLabel1(wb.getData(IMDGHL1));
      wb.setEuHazLabel1(wb.getData(IMDGHL1));
      wb.setHazLabel2(wb.getData(IMDGHL2));
      wb.setEuHazLabel2(wb.getData(IMDGHL2));
      wb.setHazLabel3(wb.getData(IMDGHL3));
      wb.setEuHazLabel3(wb.getData(IMDGHL3));
      wb.setId(id);
      wb.setProductNumber(alias);
      wb.setShipMethod(WATR);
      wb.setHazIngr3(getNosIngredients(wb));
      wb.setEuHazIngr1(wb.getData(TND1));
      wb.setEuHazIngr2(wb.getData(TND2));
      wb.setBulk(bulk);
      wb.setContainer(container);
      populateItemDbList(wb);
      wb.setImdgPage(wb.getData(HCM));
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
      // wb.setLimitedQuantity(wb.getData(IMLQ));
      wb.setLimitedQuantity(getLimitedQty(wb));
      wb.setBulkFlag(isBulk(wb));
      wb.setViscosityException(getViscosityEx(wb, "NA"));
      wb.setEuViscosityException(getViscosityEx(wb, ""));
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
    StringBuilder sql = new StringBuilder();
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
        sql.append(CommonUtility.toVarchar(dataCodes[i]));
        if (i != dataCodes.length - 1)
          sql.append(", ");
        hm.put(dataCodes[i], null);
      }
      sql.append(") and a.f_alias = ");
      sql.append(CommonUtility.toVarchar(product));

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

  public HashMap getTextCodeValues(String product, String[] textDataCodes)
  {
    HashMap hm = new HashMap();
    StringBuilder sql = new StringBuilder();

    Statement stmt = null;
    ResultSet rs = null;

    try
    {
      Connection conn = this.getConn();

      sql.append("select b.f_data_code,b.f_text_code from t_product_alias_names a, t_prod_text b ");
      sql.append("where a.f_product = b.f_product ");
      sql.append("and b.f_data_code in (");
      for (int i = 0; i < textDataCodes.length; i++)
      {
        sql.append(CommonUtility.toVarchar(textDataCodes[i]));
        if (i != textDataCodes.length - 1)
          sql.append(", ");
        hm.put(textDataCodes[i], null);
      }
      sql.append(") and a.f_alias = ");
      sql.append(CommonUtility.toVarchar(product));
      stmt = conn.createStatement();
      rs = stmt.executeQuery(sql.toString());

      while (rs.next())
      {
        String f_data_code = rs.getString(1);
        String f_text_code = rs.getString(2);
        hm.put(f_data_code, f_text_code);
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
    String ingred1 = pb.getData(TN1);
    ;
    String ingred2 = pb.getData(TN2);
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
        StringBuilder sql = new StringBuilder();
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

  private String getViscosityEx(ProductBean pb, String region11i)
  {
    String viscostityEx = ZERO;

    Statement stmt = null;
    ResultSet rs = null;
    try
    {
      Connection conn = this.getConn();

      StringBuilder sql = new StringBuilder();
      sql.append("select GET_VISCOSITY_EXCEPTION(");
      sql.append(CommonUtility.toVarchar(pb.getBulk()));
      sql.append(", ");
      sql.append(CommonUtility.toVarchar(pb.getShipMethod()));
      sql.append(", ");
      sql.append(CommonUtility.toVarchar(pb.getProductExtension()));
      sql.append(", ");
      sql.append(CommonUtility.toVarchar(region11i));
      sql.append(") from dual");

      stmt = conn.createStatement();
      ((OracleStatement) stmt).defineColumnType(1, Types.VARCHAR);
      rs = stmt.executeQuery(sql.toString());
      if (rs.next())
      {
        if (rs.getInt(1) > 0)
        {
          viscostityEx = Constants.ONE;
        }
      }
      //  viscostityEx = rs.getString(1);
    }
    catch (Exception e)
    {
      log4jLogger.error("for product " + pb.getProductNumber(), e);
    }
    finally
    {
      JDBCUtil.close(stmt, rs);
    }
    return viscostityEx;
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
          /* pb.setUnNumber(getLimitQtyRs.getString(7));
          pb.setShippingName(getLimitQtyRs.getString(8));
          pb.setShipMethod(LAND);
          pb.setHazardClass(EMPTY_STRING);
          pb.setPackingGroup(EMPTY_STRING);*/
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

  public static String getTextPhrase(String textCode)
  {
    OracleConnection wercsConn = (OracleConnection) ConnectionAccessBean.getConnection(DataSource.WERCS);
    StringBuilder sb = new StringBuilder();
    sb.append("select b.f_phrase from t_phrase_linkage a,t_phrase_translations b ");
    sb.append("where a.f_phrase_id = b.f_phrase_id and a.f_text_code = ? ");
    sb.append(" and b.f_language = 'EN' ");
    return ValsparLookUps.queryForSingleValue(wercsConn, sb.toString(), textCode);
  }

  private boolean isTextCodeExist(String product, String textCode)
  {
    OracleConnection wercsConn = (OracleConnection) ConnectionAccessBean.getConnection(DataSource.WERCS);
    StringBuilder sb = new StringBuilder();
    sb.append("SELECT 'X' FROM   T_PROD_TEXT ");
    sb.append("WHERE  F_TEXT_CODE = ");
    sb.append(CommonUtility.toVarchar(textCode));
    sb.append(" and F_PRODUCT = ");
    sb.append(CommonUtility.toVarchar(product));
    String existFlag = ValsparLookUps.queryForSingleValue(wercsConn, sb.toString());
    if (StringUtils.isEmpty(existFlag))
    {
      return false;
    }
    else
    {
      return true;
    }
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
