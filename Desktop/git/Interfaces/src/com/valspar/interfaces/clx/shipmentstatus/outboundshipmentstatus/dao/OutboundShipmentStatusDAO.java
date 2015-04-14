package com.valspar.interfaces.clx.shipmentstatus.outboundshipmentstatus.dao;

import com.valspar.interfaces.clx.common.beans.*;
import com.valspar.interfaces.clx.shipmentstatus.outboundshipmentstatus.program.OutboundShipmentStatus;
import com.valspar.interfaces.common.beans.ConnectionAccessBean;
import com.valspar.interfaces.common.utils.*;
import java.util.*;
import oracle.jdbc.*;
import org.apache.log4j.Logger;

public final class OutboundShipmentStatusDAO
{
  private static Logger log4jLogger = Logger.getLogger(OutboundShipmentStatusDAO.class);  
  
  public OutboundShipmentStatusDAO()
  {
  }


  public static List<ShipmentStatusStagingBean> fetchShipmentStatusDeliveries(String senderIdKey)
  {
    log4jLogger.info("OutboundShipmentStatusDAO.fetchShipmentStatusDeliveries() - Building order staging bean list from 11i...");

    List<ShipmentStatusStagingBean> shipmentStatusStagingBeanList = new ArrayList<ShipmentStatusStagingBean>();

    OracleConnection conn = null;
    OraclePreparedStatement pst = null;
    OracleResultSet rs = null;
    ShipmentStatusStagingBean shipmentStatusStagingBean = null;

    try
    {
      conn = (OracleConnection) ConnectionAccessBean.findConnection(senderIdKey);

      StringBuilder sb = new StringBuilder();
      sb.append("SELECT DISTINCT ROWID transId, DELIVERY delivery, TRANSFER_BATCH transferBatch, ORGN_CODE orgnCode, ACTION_CODE actionCode, EVENT_TYPE eventType, CREATION_DATE creationDate, LAST_UPDATE_DATE lastUpdateDate ");
      sb.append("  FROM VALSPAR.VCA_CLX_DELIVERY_STAGE ");
      sb.append(" WHERE STATUS = 'N' ");
      sb.append("   AND CREATION_DATE < SYSDATE - numtodsinterval(30,'second') ");
      sb.append(" ORDER BY DELIVERY, TRANSFER_BATCH, ORGN_CODE, CREATION_DATE ");

      pst = (OraclePreparedStatement) conn.prepareStatement(sb.toString());
      rs = (OracleResultSet) pst.executeQuery();

      while (rs.next())
      {
        shipmentStatusStagingBean = new ShipmentStatusStagingBean();
        shipmentStatusStagingBean.setActionCode(rs.getString("actionCode"));
        shipmentStatusStagingBean.setDeliveryNumber(rs.getString("delivery"));
        shipmentStatusStagingBean.setBatchNumber(rs.getString("transferBatch"));
        shipmentStatusStagingBean.setOrgnCode(rs.getString("orgnCode"));
        shipmentStatusStagingBean.setEventType(rs.getString("eventType"));
        shipmentStatusStagingBean.setTransId(rs.getString("transId"));
        shipmentStatusStagingBean.setCreationDate(rs.getDate("creationDate"));
        shipmentStatusStagingBean.setLastUpdateDate(rs.getDate("lastUpdateDate"));
        shipmentStatusStagingBeanList.add(shipmentStatusStagingBean);
      }
    }
    catch (Exception e)
    {
      log4jLogger.error(e);
      OutboundShipmentStatus.sendShipmentStatusNotifcationEmail(shipmentStatusStagingBean, e);
    }
    finally
    {
      JDBCUtil.close(pst, rs);
      JDBCUtil.close(conn);
    }
    log4jLogger.info("OutboundShipmentStatusDAO.fetchShipmentStatusDeliveries() - Finished building delivery staging bean list.");

    return shipmentStatusStagingBeanList;
  }

  public static List<ShipmentStatusLineBean> fetchShipmentStatusDeliveryLines(ShipmentStatusStagingBean shipmentStatusStagingBean)
  {
    log4jLogger.info("OutboundShipmentStatusDAO.fetchShipmentStatusDeliveryLines() - Building shipment status lines from 11i");

    List<ShipmentStatusLineBean> shipmentStatuses = new ArrayList<ShipmentStatusLineBean>();

    OracleConnection conn = null;
    OraclePreparedStatement pst = null;
    OracleResultSet rs = null;

    try
    {
      conn = (OracleConnection) ConnectionAccessBean.findConnection(shipmentStatusStagingBean.getSenderIdKey());

      StringBuilder sb = new StringBuilder();
      sb.append("SELECT DOC_ID docId, ");
      sb.append("       SENDER_ORG_NAME senderOrgName, ");
      sb.append("       RECEIVER_ORG_ID receiverOrgId, ");
      sb.append("       RECEIVER_ORG_NAME receiverOrgName, ");
      sb.append("       TRANS_ID transId, ");
      sb.append("       TRANS_TYPE transType, ");
      sb.append("       TRANS_DATE transDate, ");
      sb.append("       TIMEZONE timezone, ");
      sb.append("       SHIPMENT_ID shipmentId, ");
      sb.append("       SS_BOL bol, ");
//      sb.append("       TRANSFER_NO trasfer, ");
 //     sb.append("       ORGN_CODE orgnCode, ");
      sb.append("       REFERENCE1 reference1, ");
      sb.append("       REFERENCE1_TYPE reference1Type, ");
      sb.append("       REFERENCE2 reference2, ");
      sb.append("       REFERENCE2_TYPE reference2Type, ");
      sb.append("       EQUIPMENT_DESCRIPTION equipmentDesc, ");
      sb.append("       TRAILER_NUMBER trailerNumber, ");
      sb.append("       SHIPMENT_STATUS_CODE shipmentStatusCode, ");
      sb.append("       SHIPMENT_STATUS_REASON shipmentStatusReason, ");
      sb.append("       TRANS_DATE shipmentStatusTime, ");
      sb.append("       'N' headerStatus, ");
      sb.append("       CREATION_DATE intCreateDate, ");
      sb.append("       LAST_UPDATE_DATE intUpdateDate ");
      sb.append("  FROM APPS.VCA_CLX_OUTBOUND_SHIP_STATUS_V  ");
      sb.append(" WHERE TRANS_ID = :TRANS_ID ");

      pst = (OraclePreparedStatement) conn.prepareStatement(sb.toString());
      pst.setStringAtName("TRANS_ID", shipmentStatusStagingBean.getTransId());
      rs = (OracleResultSet) pst.executeQuery();
      String senderOrgId = shipmentStatusStagingBean.getSenderId();

      while (rs.next())
      {
        ShipmentStatusLineBean bean = new ShipmentStatusLineBean();
        bean.setDocId(rs.getString("docId"));
        bean.setSenderOrgId(senderOrgId);
        bean.setSenderOrgName(rs.getString("senderOrgName"));
        bean.setReceiverOrgId(rs.getString("receiverOrgId"));
        bean.setReceiverOrgName(rs.getString("receiverOrgName"));
        bean.setTransId(rs.getString("transId"));
        bean.setTransType(rs.getString("transType"));
        bean.setTransDate(rs.getDate("transDate"));
        bean.setTimeZone(rs.getString("timezone"));
        bean.setShipmentId(rs.getString("shipmentId"));
        bean.setReference1(rs.getString("reference1"));
        bean.setReference1Type(rs.getString("reference1Type"));
        bean.setReference2(rs.getString("reference2"));
        bean.setReference2Type(rs.getString("reference2Type"));
        bean.setEquipmentDescription(rs.getString("equipmentDesc"));
        bean.setTrailerNumber(rs.getString("trailerNumber"));
        bean.setTrailerNumber(rs.getString("trailerNumber"));
        bean.setShipmentStatusCode(rs.getString("shipmentStatusCode"));
        bean.setShipmentStatusReason(rs.getString("shipmentStatusReason"));
        bean.setShipmentStatusTime(rs.getDate("shipmentStatusTime"));
     
        shipmentStatuses.add(bean);
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
    return shipmentStatuses;
  }

  public static String updateShipmentStatusStagingTable(ShipmentStatusStagingBean shipmentStatusStagingBean)
  {
    log4jLogger.info("OutboundShipmentStatusDAO.updateShipmentStatusStagingTable() - Updating delivery staging table... ");
    String returnMessage = null;
    OracleConnection conn = (OracleConnection) ConnectionAccessBean.findConnection(shipmentStatusStagingBean.getSenderIdKey());
    OraclePreparedStatement pst = null;
    String erpUserId = OracleAppsUtility.getERPUserId(conn, "TMS");

    try
    {
      StringBuilder sb = new StringBuilder();
      sb.append("UPDATE VALSPAR.VCA_CLX_DELIVERY_STAGE ");
      sb.append("   SET STATUS = :STATUS, ");
      sb.append("       LAST_UPDATED_BY = :ERP_USER_ID, ");
      sb.append("       LAST_UPDATE_DATE = SYSDATE, ");
      sb.append("       ERROR_MESSAGE = SUBSTR(:ERROR_MESSAGE, 1, 2000) ");
      sb.append(" WHERE ROWID = :ROW_ID ");

      pst = (OraclePreparedStatement) conn.prepareStatement(sb.toString());
      pst.setStringAtName("ROW_ID", shipmentStatusStagingBean.getTransId());
      pst.setStringAtName("STATUS", shipmentStatusStagingBean.getInterfaceStatusCode());
      pst.setStringAtName("ERP_USER_ID", erpUserId);
      pst.setStringAtName("ERROR_MESSAGE", shipmentStatusStagingBean.getReturnMessage());
      pst.executeUpdate();
    }
    catch (Exception e)
    {
      returnMessage = "ERROR - OutboundShipmentStatusDAO.updateShipmentStatusStagingTable() " + e;
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
