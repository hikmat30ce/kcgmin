package com.valspar.interfaces.clx.order.outboundorder.dao;

import com.valspar.interfaces.clx.common.beans.*;
import com.valspar.interfaces.clx.order.outboundorder.program.OutboundOrder;
import com.valspar.interfaces.common.beans.ConnectionAccessBean;
import com.valspar.interfaces.common.enums.DataSource;
import com.valspar.interfaces.common.servlets.PropertiesServlet;
import com.valspar.interfaces.common.utils.*;
import java.util.*;
import oracle.jdbc.*;
import org.apache.commons.lang3.StringUtils;
import org.apache.log4j.Logger;

public final class OutboundOrderDAO
{
  private static Logger log4jLogger = Logger.getLogger(OutboundOrderDAO.class);

  public static List<OrderStagingBean> fetchOrders(String senderIdKey)
  {
    log4jLogger.info("OutboundOrderDAO.fetchOrders() - Building order staging bean list from 11i...");

    List<OrderStagingBean> orderStagingBeanList = new ArrayList<OrderStagingBean>();

    OracleConnection conn = null;
    OraclePreparedStatement pst = null;
    OracleResultSet rs = null;
    OrderStagingBean orderStagingBean = null;

    try
    {
      conn = (OracleConnection) ConnectionAccessBean.findConnection(senderIdKey);

      StringBuilder sb = new StringBuilder();
      sb.append("SELECT DISTINCT ROWID transId, DELIVERY delivery, TRANSFER_BATCH transferBatch, ORGN_CODE orgnCode, ACTION_CODE actionCode, EVENT_TYPE eventType, CREATION_DATE creationDate, LAST_UPDATE_DATE lastUpdateDate, BOL_PRINT_DATE bolPrintDate ");
      sb.append("  FROM VALSPAR.VCA_CLX_ORDER_STAGE ");
      sb.append(" WHERE STATUS = 'N' ");
      sb.append("   AND CREATION_DATE < SYSDATE - numtodsinterval(30,'second') ");
      sb.append(" ORDER BY DELIVERY, TRANSFER_BATCH, ORGN_CODE, CREATION_DATE, ACTION_CODE ");

      pst = (OraclePreparedStatement) conn.prepareStatement(sb.toString());
      rs = (OracleResultSet) pst.executeQuery();

      while (rs.next())
      {
        orderStagingBean = new OrderStagingBean();
        orderStagingBean.setSenderId(PropertiesServlet.getProperty(senderIdKey));
        orderStagingBean.setSenderIdKey(senderIdKey);
        orderStagingBean.setActionCode(rs.getString("actionCode"));
        orderStagingBean.setDeliveryNumber(rs.getString("delivery"));
        orderStagingBean.setTransferBatch(rs.getString("transferBatch"));
        orderStagingBean.setOrgnCode(rs.getString("orgnCode"));
        orderStagingBean.setEventType(rs.getString("eventType"));
        orderStagingBean.setTransId(rs.getString("transId"));
        orderStagingBean.setCreationDate(rs.getDate("creationDate"));
        orderStagingBean.setLastUpdateDate(rs.getDate("lastUpdateDate"));
        orderStagingBean.setBolPrintDate(rs.getDate("bolPrintDate"));
        orderStagingBean.setStatus("P");
        orderStagingBeanList.add(orderStagingBean);
      }
    }
    catch (Exception e)
    {
      log4jLogger.error(e);
      OutboundOrder.sendOrderNotifcationEmail(orderStagingBean, e);
    }
    finally
    {
      JDBCUtil.close(pst, rs);
      JDBCUtil.close(conn);
    }
    log4jLogger.info("OutboundOrderDAO.fetchOrders() - Finished building order staging bean list.");

    return orderStagingBeanList;
  }

  public static List<OrderLineBean> fetchOrderLines(OrderStagingBean orderStagingBean)
  {
    log4jLogger.info("OutboundOrderDAO.fetchOrderLines() - Building order line beans from 11i");

    List<OrderLineBean> orderLineBeanList = new ArrayList<OrderLineBean>();
    
    OracleConnection conn = null;
    OraclePreparedStatement pst = null;
    OracleResultSet rs = null;

    try
    {
      conn = (OracleConnection) ConnectionAccessBean.findConnection(orderStagingBean.getSenderIdKey());

      StringBuilder sb = new StringBuilder();
      sb.append("SELECT DOC_ID docId, ");
      sb.append("       SENDER_ORG_NAME senderOrgName, ");
      sb.append("       RECEIVER_ORG_ID receiverOrgId, ");
      sb.append("       RECEIVER_ORG_NAME receiverOrgName, ");
      sb.append("       TRANS_ID transId, ");
      sb.append("       TRANS_TYPE transType, ");
      sb.append("       TRANS_DATE transDate, ");
      sb.append("       TIMEZONE timezone, ");
      sb.append("       ACTION_CODE actionCode, ");
      sb.append("       SHIPMENT_ID shipmentId, ");
      sb.append("       DELIVERY_DETAIL_ID deliveryDetailId, ");
      sb.append("       LINE_HAUL_MODE lineHaulMode, ");
      sb.append("       MOVEMENT_TYPE movementType, ");
      sb.append("       PAYMENT_METHOD paymentMethod, ");
      sb.append("       FREIGHT_TERMS_CODE freightTermsCode, ");
      sb.append("       REFERENCE1 reference1, ");
      sb.append("       REFERENCE1_TYPE reference1Type, ");
      sb.append("       REFERENCE2 reference2, ");
      sb.append("       REFERENCE2_TYPE reference2Type, ");
      sb.append("       CASE WHEN DELIVERY_DETAIL_ID IS NOT NULL ");
      sb.append("       THEN ");
      sb.append("         VCA_TMS_COMMON_PKG.GET_GL_REFERENCE3(SS_DEST_LOCATION_ID,DELIVERY_DETAIL_ID, SS_DEST_ORG_ID, SS_BOL, SS_PRODUCT_ID) ");
      sb.append("       ELSE ");
      sb.append("         VCA_TMS_COMMON_PKG.GET_INV_XFER_REFERENCE3(TRANSFER, ORGN_CODE) ");
      sb.append("       END reference3, ");
      sb.append("       REFERENCE3_TYPE reference3Type, ");
      sb.append("       CASE WHEN DELIVERY_DETAIL_ID IS NOT NULL ");
      sb.append("       THEN ");
      sb.append("         VCA_TMS_COMMON_PKG.GET_GL_CODE_STRING(SS_DEST_LOCATION_ID,DELIVERY_DETAIL_ID, SS_DEST_ORG_ID, SS_BOL, SS_PRODUCT_ID) ");
      sb.append("       ELSE ");
      sb.append("          VCA_TMS_COMMON_PKG.GET_INV_XFER_GL_STRING(TRANSFER, ORGN_CODE) ");
      sb.append("       END reference4, ");
      sb.append("       REFERENCE4_TYPE reference4Type, ");
      sb.append("       REFERENCE5 reference5, ");
      sb.append("       REFERENCE5_TYPE reference5Type, ");
      sb.append("       REFERENCE6 reference6, ");
      sb.append("       REFERENCE6_TYPE reference6Type, ");
      sb.append("       REFERENCE7 reference7, ");
      sb.append("       REFERENCE7_TYPE reference7Type, ");
      sb.append("       CASE WHEN DELIVERY_DETAIL_ID IS NOT NULL ");
      sb.append("       THEN ");
      sb.append("         VCA_TMS_COMMON_PKG.GET_GL_REFERENCE8(SS_DEST_LOCATION_ID,DELIVERY_DETAIL_ID, SS_DEST_ORG_ID, SS_BOL, SS_PRODUCT_ID) ");
      sb.append("       ELSE ");
      sb.append("          'INV_XFER' ");
      sb.append("       END reference8, ");
      sb.append("       REFERENCE8_TYPE reference8Type, ");
      sb.append("       REFERENCE9 reference9, ");
      sb.append("       REFERENCE9_TYPE reference9Type, ");
      sb.append("       REFERENCE10 reference10, ");
      sb.append("       REFERENCE10_TYPE reference10Type, ");
      sb.append("       REFERENCE11 reference11, ");
      sb.append("       REFERENCE11_TYPE reference11Type, ");
      sb.append("       REFERENCE12 reference12, ");
      sb.append("       REFERENCE12_TYPE reference12Type, ");
      sb.append("       REFERENCE13 reference13, ");
      sb.append("       REFERENCE13_TYPE reference13Type, ");
      sb.append("       REFERENCE14_TYPE reference14Type, ");
      sb.append("       REFERENCE15 reference15, ");
      sb.append("       REFERENCE15_TYPE reference15Type, ");
      sb.append("       REFERENCE16 reference16, ");
      sb.append("       REFERENCE16_TYPE reference16Type, ");
      sb.append("       REFERENCE17 reference17, ");
      sb.append("       REFERENCE17_TYPE reference17Type, ");
      sb.append("       REFERENCE18 reference18, ");
      sb.append("       REFERENCE18_TYPE reference18Type, ");
      sb.append("       REFERENCE19 reference19, ");
      sb.append("       REFERENCE19_TYPE reference19Type, ");
      sb.append("       REFERENCE20 reference20, ");
      sb.append("       REFERENCE20_TYPE reference20Type, ");
      sb.append("       VCA_TMS_COMMON_PKG.IS_HAZMAT_ITEM_ON_DELIVERY(SS_BOL) attribute1, ");
      sb.append("       ATTRIBUTE2 attribute2, ");
      sb.append("       ATTRIBUTE3 attribute3, ");
      sb.append("       ATTRIBUTE4 attribute4, ");
      sb.append("       ATTRIBUTE5 attribute5, ");
      sb.append("       ATTRIBUTE6 attribute6, ");
      sb.append("       ATTRIBUTE7 attribute7, ");
      sb.append("       ATTRIBUTE8 attribute8, ");
      sb.append("       ATTRIBUTE9 attribute9, ");
      sb.append("       ATTRIBUTE10 attribute10, ");
      sb.append("       ATTRIBUTE11 attribute11, ");
      sb.append("       ATTRIBUTE12 attribute12, ");
      sb.append("       ATTRIBUTE13 attribute13, ");
      sb.append("       ATTRIBUTE14 attribute14, ");
      sb.append("       ATTRIBUTE15 attribute15, ");
      sb.append("       ATTRIBUTE16 attribute16, ");
      sb.append("       ATTRIBUTE17 attribute17, ");
      sb.append("       ATTRIBUTE18 attribute18, ");
      sb.append("       ATTRIBUTE19 attribute19, ");
      sb.append("       ATTRIBUTE20 attribute20, ");
      sb.append("       SERVICE_TYPE serviceType, ");
      sb.append("       EQUIPMENT_DESCRIPTION equipmentDesc, ");
      sb.append("       GROSS_WEIGHT_UOM grossWeightUOM, ");
      sb.append("       GROSS_WEIGHT grossWeight, ");
      sb.append("       SS_BOL bol, ");
      sb.append("       TRANSFER transfer, ");
      sb.append("       ORGN_CODE orgnCode, ");
      sb.append("       SS_PO_NUMBER poNumber, ");
      sb.append("       SS_SALES_ORDER_NUMBER soNumber, ");
      sb.append("       SS_LADING_QTY_UOM ssLadingQtyUOM, ");
      sb.append("       SS_LADING_QTY ssLadingQty, ");
      sb.append("       SS_GROSS_WEIGHT_UOM ssGrossWeightUOM, ");
      sb.append("       SS_GROSS_WEIGHT ssGrossWeight, ");
      sb.append("       SS_VOLUME_UOM volumeUOM, ");
      sb.append("       SS_VOLUME volume, ");
      sb.append("       SS_ORIG_STOP_NUMBER originStopNumber, ");
      sb.append("       SS_ORIG_STOP_REASON originStopReason, ");
      sb.append("       SS_ORIG_LOCATION_NAME originLocationName, ");
      sb.append("       SS_ORIG_LOCATION_ID originLocationId, ");
      sb.append("       SS_ORIG_LOCATION_TYPE originLocationType, ");
      sb.append("       ORIGIN_ORG_NAME originOrgName, ");
      sb.append("       SS_ORIG_ADDRESS1 originAddress1, ");
      sb.append("       SS_ORIG_ADDRESS2 originAddress2, ");
      sb.append("       SS_ORIG_ADDRESS3 originAddress3, ");
      sb.append("       SS_ORIG_ADDRESS4 originAddress4, ");
      sb.append("       SS_ORIG_CITY originCity, ");
      sb.append("       SS_ORIG_STATE_PROVINCE originStateProvince, ");
      sb.append("       SS_ORIG_POSTAL_CODE originPostalCode, ");
      sb.append("       SS_ORIG_COUNTRY originCountry, ");
      sb.append("       NVL(SS_ORIG_PLANNED_FROM_DATE, SYSDATE) originPlannedFromDate, ");
      sb.append("       NVL(SS_ORIG_PLANNED_TO_DATE, SYSDATE) originPlannedToDate, ");
      sb.append("       SS_DEST_STOP_NUMBER destStopNumber, ");
      sb.append("       SS_DEST_STOP_REASON destStopReason, ");
      sb.append("       SS_DEST_LOCATION_NAME destLocationName, ");
      sb.append("       SS_DEST_LOCATION_ID destLocationId, ");
      sb.append("       SS_DEST_LOCATION_TYPE destLocationType, ");
      sb.append("       SS_DEST_ORG_ID destOrgId, ");
      sb.append("       SS_DEST_ADDRESS1 destAddress1, ");
      sb.append("       SS_DEST_ADDRESS2 destAddress2, ");
      sb.append("       SS_DEST_ADDRESS3 destAddress3, ");
      sb.append("       SS_DEST_ADDRESS4 destAddress4, ");
      sb.append("       SS_DEST_CITY destCity, ");
      sb.append("       SS_DEST_STATE_PROVINCE destStateProvince, ");
      sb.append("       SS_DEST_POSTAL_CODE destPostalCode, ");
      sb.append("       SS_DEST_COUNTRY destCountry, ");
      sb.append("       SS_PLANNED_DATE plannedDate, ");
      sb.append("       SS_DEST_NOTE destNote, ");
      sb.append("       SS_PRODUCT_ID productId, ");
      sb.append("       SS_PRODUCT_DESCRIPTION productDesc, ");
      sb.append("       SS_PRODUCT_GROSS_WEIGHT_UOM productGrossWeightUOM, ");
      sb.append("       SS_PRODUCT_GROSS_WEIGHT productGrossWeight, ");
      sb.append("       SS_PRODUCT_LADING_QTY_UOM productLadingQtyUOM, ");
      sb.append("       SS_PRODUCT_LADING_QTY productLadingQty, ");
      sb.append("       LINE_STATUS lineStatus, ");
      sb.append("       HEADER_STATUS headerStatus, ");
      sb.append("       CREATION_DATE intCreateDate, ");
      sb.append("       LAST_UPDATE_DATE intUpdateDate ");
      sb.append("  FROM APPS.VCA_CLX_ORDER_INTERFACE_V ");
      sb.append(" WHERE STATUS = 'P' ");
      sb.append(" AND TRANS_ID = :TRANS_ID ");
      sb.append(" ORDER BY SS_PRODUCT_ID");

      pst = (OraclePreparedStatement) conn.prepareStatement(sb.toString());
      pst.setStringAtName("TRANS_ID", orderStagingBean.getTransId());
      rs = (OracleResultSet) pst.executeQuery();
      String senderOrgId = orderStagingBean.getSenderId();

      while (rs.next())
      {
        OrderLineBean bean = new OrderLineBean();
        bean.setDocId(rs.getString("docId"));
        bean.setSenderOrgId(senderOrgId);
        bean.setSenderOrgName(rs.getString("senderOrgName"));
        bean.setReceiverOrgId(rs.getString("receiverOrgId"));
        bean.setReceiverOrgName(rs.getString("receiverOrgName"));
        bean.setTransId(rs.getString("transId"));
        bean.setTransType(rs.getString("transType"));
        bean.setTransDate(rs.getDate("transDate"));
        bean.setTimeZone(rs.getString("timezone"));
        bean.setActionCode(rs.getString("actionCode"));
        bean.setShipmentId(rs.getString("shipmentId"));
        bean.setLineHaulMode(rs.getString("lineHaulMode"));
        bean.setMovementType(rs.getString("movementType"));
        bean.setReference1(rs.getString("reference1"));
        bean.setReference1Type(rs.getString("reference1Type"));
        bean.setReference2(rs.getString("reference2"));
        bean.setReference2Type(rs.getString("reference2Type"));
        bean.setReference3(rs.getString("reference3"));
        bean.setReference3Type(rs.getString("reference3Type"));
        bean.setReference4(rs.getString("reference4"));
        bean.setReference4Type(rs.getString("reference4Type"));
        bean.setReference5(rs.getString("reference5"));
        bean.setReference5Type(rs.getString("reference5Type"));
        bean.setReference6(rs.getString("reference6"));
        bean.setReference6Type(rs.getString("reference6Type"));
        bean.setReference7(rs.getString("reference7"));
        bean.setReference7Type(rs.getString("reference7Type"));
        bean.setReference8(rs.getString("reference8"));
        bean.setReference8Type(rs.getString("reference8Type"));
        bean.setReference9(rs.getString("reference9"));
        bean.setReference9Type(rs.getString("reference9Type"));
        bean.setReference10(rs.getString("reference10"));
        bean.setReference10Type(rs.getString("reference10Type"));
        bean.setReference11(rs.getString("reference11"));
        bean.setReference11Type(rs.getString("reference11Type"));
        bean.setReference12(rs.getString("reference12"));
        bean.setReference12Type(rs.getString("reference12Type"));
        bean.setReference13(rs.getString("reference13"));
        bean.setReference13Type(rs.getString("reference13Type"));
        bean.setReference14Type(rs.getString("reference14Type"));
        bean.setReference15(rs.getString("reference15"));
        bean.setReference15Type(rs.getString("reference15Type"));
        bean.setReference16(rs.getString("reference16"));
        bean.setReference16Type(rs.getString("reference16Type"));
        bean.setReference17(rs.getString("reference17"));
        bean.setReference17Type(rs.getString("reference17Type"));
        bean.setReference18(rs.getString("reference18"));
        bean.setReference18Type(rs.getString("reference18Type"));
        bean.setReference19(rs.getString("reference19"));
        bean.setReference19Type(rs.getString("reference19Type"));
        bean.setReference20(rs.getString("reference20"));
        bean.setReference20Type(rs.getString("reference20Type"));
        bean.setPaymentMethod(rs.getString("paymentMethod"));
        orderStagingBean.setCarrierSCAC(rs.getString("reference5"));
        bean.setAttribute1(rs.getString("attribute1"));
        bean.setAttribute2(rs.getString("attribute2"));
        bean.setAttribute3(rs.getString("attribute3"));
        bean.setAttribute4(rs.getString("attribute4"));
        bean.setAttribute5(rs.getString("attribute5"));
        bean.setAttribute6(rs.getString("attribute6"));
        bean.setAttribute7(rs.getString("attribute7"));
        bean.setAttribute8(rs.getString("attribute8"));
        bean.setAttribute9(rs.getString("attribute9"));
        bean.setAttribute10(rs.getString("attribute10"));
        bean.setAttribute11(rs.getString("attribute11"));
        bean.setAttribute12(rs.getString("attribute12"));
        bean.setAttribute13(rs.getString("attribute13"));
        bean.setAttribute14(rs.getString("attribute14"));
        bean.setAttribute15(rs.getString("attribute15"));
        bean.setAttribute16(rs.getString("attribute16"));
        bean.setAttribute17(rs.getString("attribute17"));
        bean.setAttribute18(rs.getString("attribute18"));
        bean.setAttribute19(rs.getString("attribute19"));
        bean.setAttribute20(rs.getString("attribute20"));
        bean.setServiceType(rs.getString("serviceType"));        
        if (StringUtils.equalsIgnoreCase(bean.getServiceType(), "EXPEDITE"))
        {
          orderStagingBean.setExpedited(true);
        }
        bean.setEquipmentDescription(rs.getString("equipmentDesc"));
        bean.setGrossWeightUOM(rs.getString("grossWeightUOM"));
        bean.setGrossWeight(rs.getString("grossWeight"));
        bean.setVolumeUOM(rs.getString("volumeUOM"));
        bean.setVolume(rs.getString("volume"));
        bean.setBol(rs.getString("bol"));
        bean.setPoNumber(rs.getString("poNumber"));
        bean.setSalesOrderNumber(rs.getString("soNumber"));
        bean.setLadingQuantityUOM(rs.getString("ssLadingQtyUOM"));
        bean.setLadingQuantity(rs.getString("ssLadingQty"));
        bean.setSubGrossWeightUOM(rs.getString("ssGrossWeightUOM"));
        bean.setSubGrossWeight(rs.getString("ssGrossWeight"));
        bean.setOrigStopNumber(rs.getString("originStopNumber"));
        bean.setOrigStopReason(rs.getString("originStopReason"));
        bean.setOrigLocationName(rs.getString("originLocationName"));
        bean.setOrigLocationId(rs.getString("originLocationId"));
        bean.setOrigLocationType(rs.getString("originLocationType"));
        bean.setOriginOrgId(senderOrgId);
        bean.setOriginOrgName(rs.getString("originOrgName"));
        bean.setOrigAddress1(rs.getString("originAddress1"));
        bean.setOrigAddress2(rs.getString("originAddress2"));
        bean.setOrigAddress3(rs.getString("originAddress3"));
        bean.setOrigAddress4(rs.getString("originAddress4"));
        bean.setOrigCity(rs.getString("originCity"));
        bean.setOrigStateProvince(rs.getString("originStateProvince"));
        bean.setOrigPostalCode(rs.getString("originPostalCode"));
        bean.setOrigCountry(rs.getString("originCountry"));
        bean.setOrigPlannedFromDate(rs.getDate("originPlannedFromDate"));
        bean.setOrigPlannedToDate(rs.getDate("originPlannedToDate"));
        bean.setDestStopNumber(rs.getString("destStopNumber"));
        bean.setDestStopReason(rs.getString("destStopReason"));
        bean.setDestLocationName(rs.getString("destLocationName"));
        bean.setDestLocationId(rs.getString("destLocationId"));
        bean.setDestLocationType(rs.getString("destLocationType"));
        bean.setDestOrgId(rs.getString("destOrgId"));
        bean.setDestAddress1(rs.getString("destAddress1"));
        bean.setDestAddress2(rs.getString("destAddress2"));
        bean.setDestAddress3(rs.getString("destAddress3"));
        bean.setDestAddress4(rs.getString("destAddress4"));
        bean.setDestCity(rs.getString("destCity"));
        bean.setDestStateProvince(rs.getString("destStateProvince"));
        bean.setDestPostalCode(rs.getString("destPostalCode"));
        bean.setDestCountry(rs.getString("destCountry"));
        bean.setPlannedDate(rs.getDate("plannedDate"));
        bean.setDestNote(rs.getString("destNote"));
        bean.setProductId(rs.getString("productId"));
        bean.setProductDescription(rs.getString("productDesc"));
        bean.setProductGrossWeightUom(rs.getString("productGrossWeightUOM"));
        bean.setProductGrossWeight(rs.getString("productGrossWeight"));
        bean.setProductLadingQtyUOM(rs.getString("productLadingQtyUOM"));
        bean.setProductLadingQty(rs.getString("productLadingQty"));
        bean.setProductLadingQty(rs.getString("productLadingQty"));
        bean.setBolPrintDate((rs.getDate("reference11")));
        bean.setTransferNumber(rs.getString("transfer"));
        bean.setOrgnCode(rs.getString("orgnCode"));
        orderLineBeanList.add(bean);
      }
    }
    catch (Exception e)
    {
      log4jLogger.error(e);
    }
    finally
    {
      JDBCUtil.close(pst, rs);
      JDBCUtil.close(conn);
    }
    log4jLogger.info("OutboundOrderDAO.fetchOrderLines() - Finished building order line beans");

    return orderLineBeanList;
  }
  
  public static void populateTempControl(OrderStagingBean orderStagingBean)
  {
    log4jLogger.info("OutboundOrderDAO.populateTempControl() - Getting temperature control data from Regulatory");
    OracleConnection conn = null;
    OraclePreparedStatement pst = null;
    OracleResultSet rs = null;

    try
    {
      conn = (OracleConnection) ConnectionAccessBean.getConnection(DataSource.REGULATORY);

      StringBuilder sb = new StringBuilder();
      sb.append("SELECT B.F_DATA fData, B.F_DATA_CODE fDataCode ");
      sb.append("  FROM T_PRODUCT_ALIAS_NAMES A, ");
      sb.append("       T_PROD_DATA B ");
      sb.append(" WHERE A.F_PRODUCT = B.F_PRODUCT ");
      sb.append("   AND B.F_DATA_CODE IN ('STRABV', 'STRBLW') ");
      sb.append("   AND A.F_ALIAS IN (");
      sb.append(CommonUtility.stringToInListForVarchar(orderStagingBean.getProductIdList()));
      sb.append("                    ) ");
      sb.append(" ORDER BY B.F_DATA_CODE");
      
      pst = (OraclePreparedStatement) conn.prepareStatement(sb.toString());
      rs = (OracleResultSet) pst.executeQuery();
      if (rs.next())
      {
        orderStagingBean.setTempControlled(true);
      }
    }
    catch (Exception e)
    {
      log4jLogger.error(e);
    }
    finally
    {
      JDBCUtil.close(pst, rs);
      JDBCUtil.close(conn);
    }
    log4jLogger.info("OutboundOrderDAO.populateTempControl() - Finished populating temperature control data on staging beans");
  }
  
  public static String updateInventoryTransferMaster(OrderStagingBean orderStagingBean)
  {
    log4jLogger.info("OutboundOrderDAO.updateInventoryTransferMaster() - Updating IC_XFER_MST... ");
    String returnMessage = null;
    OracleConnection conn = (OracleConnection) ConnectionAccessBean.findConnection(orderStagingBean.getSenderIdKey());
    OraclePreparedStatement pst = null;
    String erpUserId = OracleAppsUtility.getERPUserId(conn, "TMS");

    try
    {
      StringBuilder sb = new StringBuilder();
      sb.append("UPDATE GMI.IC_XFER_MST ");
      sb.append("   SET ATTRIBUTE19 = 'Y', ");
      sb.append("       LAST_UPDATED_BY = :ERP_USER_ID, ");
      sb.append("       LAST_UPDATE_DATE = SYSDATE ");
      sb.append(" WHERE TRANSFER_BATCH = :TRANSFER_BATCH ");
      sb.append("   AND ORGN_CODE = :ORGN_CODE ");

      pst = (OraclePreparedStatement) conn.prepareStatement(sb.toString());
      pst.setStringAtName("TRANSFER_BATCH", orderStagingBean.getTransferBatch());
      pst.setStringAtName("ORGN_CODE", orderStagingBean.getOrgnCode());
      pst.setStringAtName("ERP_USER_ID", erpUserId);
      pst.executeUpdate();
    }
    catch (Exception e)
    {
      returnMessage = "ERROR - OutboundOrderDAO.updateInventoryTransferMaster() " + e;
      log4jLogger.error(returnMessage);
    }
    finally
    {
      JDBCUtil.close(pst);
      JDBCUtil.close(conn);
    }
    return returnMessage;
  }
}