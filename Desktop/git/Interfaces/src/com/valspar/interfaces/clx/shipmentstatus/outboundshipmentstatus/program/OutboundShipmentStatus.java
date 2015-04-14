package com.valspar.interfaces.clx.shipmentstatus.outboundshipmentstatus.program;

import com.valspar.clx.returnvalue.generated.ReturnValue;
import com.valspar.interfaces.clx.common.api.CLXBaseImportAPI;
import com.valspar.interfaces.clx.common.beans.ShipmentStatusStagingBean;
import com.valspar.interfaces.clx.shipmentstatus.outboundshipmentstatus.dao.OutboundShipmentStatusDAO;
import com.valspar.interfaces.common.BaseInterface;
import com.valspar.interfaces.common.servlets.PropertiesServlet;
import com.valspar.interfaces.common.utils.NotificationUtility;
import java.util.*;
import org.apache.log4j.Logger;

public class OutboundShipmentStatus extends BaseInterface
{
  private static Logger log4jLogger = Logger.getLogger(OutboundShipmentStatus.class);

  public OutboundShipmentStatus()
  {
  }

  public void execute()
  {
    log4jLogger.info("Starting CLX Outbound Shipment Status Interface...");
    String senderIdKey = getParameterValue("senderIdKey");
    ShipmentStatusStagingBean lastShipmentStatusStagingBean = new ShipmentStatusStagingBean();

    try
    {
      for (ShipmentStatusStagingBean shipmentStatusStagingBean: OutboundShipmentStatusDAO.fetchShipmentStatusDeliveries(senderIdKey))
      {
        shipmentStatusStagingBean.setSenderId(PropertiesServlet.getProperty(senderIdKey));
        shipmentStatusStagingBean.setSenderIdKey(senderIdKey);
        lastShipmentStatusStagingBean = shipmentStatusStagingBean;
        log4jLogger.info("Processing transId" + shipmentStatusStagingBean.getTransId());
        shipmentStatusStagingBean.setShipmentStatusLineBeanList(OutboundShipmentStatusDAO.fetchShipmentStatusDeliveryLines(shipmentStatusStagingBean));
        log4jLogger.info("shipmentStatusLineBeanList size:" + shipmentStatusStagingBean.getShipmentStatusLineBeanList().size());
        if (!shipmentStatusStagingBean.getShipmentStatusLineBeanList().isEmpty())
        {
          importShipmentStatus(shipmentStatusStagingBean);
        }
        else
        {
          log4jLogger.error("shipmentStatusLineBeanList is empty! TransId:" + shipmentStatusStagingBean.getTransId());
          shipmentStatusStagingBean.setReturnMessage("No shipment status lines found");
          shipmentStatusStagingBean.setReturnCode("E");
          OutboundShipmentStatusDAO.updateShipmentStatusStagingTable(shipmentStatusStagingBean);
        }
        log4jLogger.info("Completed processing transId: " + shipmentStatusStagingBean.getTransId());
      }
    }
    catch (Exception e)
    {
      log4jLogger.error(e);
      sendShipmentStatusNotifcationEmail(lastShipmentStatusStagingBean, e);
    }
    log4jLogger.info("End CLX Outbound Shipment Status Interface.");
  }

  private void importShipmentStatus(ShipmentStatusStagingBean shipmentStatusStagingBean)
  {
    try
    {
      CLXBaseImportAPI clxBaseImportAPI = new CLXBaseImportAPI();
      
      if (shipmentStatusStagingBean.getShipmentStatusLineBeanList().get(0).getShipmentId() != null)
      {
        shipmentStatusStagingBean.setGeneratedXmlMessage(OutboundShipmentStatusCreator.createShipmentStatusXml(shipmentStatusStagingBean));
        ReturnValue returnValue = clxBaseImportAPI.importDocument(shipmentStatusStagingBean.getGeneratedXmlMessage());
        shipmentStatusStagingBean.setReturnCode(returnValue.getReturnCode());
        shipmentStatusStagingBean.setReturnMessage(returnValue.getMessage());
        if (shipmentStatusStagingBean.isErrorStatus())
        {
          log4jLogger.error("Error in OutboundShipmentStatus.importShipmentStatus(). TransId: " + shipmentStatusStagingBean.getTransId());
          sendShipmentStatusNotifcationEmail(null, null);
        }
      }
      else
      {
        log4jLogger.info("Ignoring non-TMS shipment with null shipment Id. TransId: " + shipmentStatusStagingBean.getTransId());
        shipmentStatusStagingBean.setReturnMessage("No shipment Id found");
        shipmentStatusStagingBean.setReturnCode("E");
      }
    }
    catch (Exception e)
    {
      log4jLogger.error(e);
      sendShipmentStatusNotifcationEmail(shipmentStatusStagingBean, e);
    }
    finally
    {
      if (shipmentStatusStagingBean != null && shipmentStatusStagingBean.getTransId() != null && shipmentStatusStagingBean.getReturnCode() != null)
      {
        OutboundShipmentStatusDAO.updateShipmentStatusStagingTable(shipmentStatusStagingBean);
      }
    }
  }

  public static void sendShipmentStatusNotifcationEmail(ShipmentStatusStagingBean shipmentStatusStagingBean, Exception ex)
  {
    try
    {
      boolean urgent = true;
      Map<String, Object> rootMap = new HashMap<String, Object>();
      rootMap.put("shipmentStatusStagingBean", shipmentStatusStagingBean);
      rootMap.put("exception", ex);
      String messageBody = NotificationUtility.buildMessage("clxshipmentstatus-notification.ftl", rootMap);
      StringBuilder sb = new StringBuilder();
      sb.append("Error occured in CLX Outbound Shipment Status Interface ");
      sb.append(" for Delivery/Transfer Batch: ");
      if (shipmentStatusStagingBean != null)
      {
        sb.append(shipmentStatusStagingBean.getDeliveryOrTransferBatch());
      }
      sb.append(" on ");
      sb.append(PropertiesServlet.getProperty("webserver"));
      NotificationUtility.sendHTMEmail(new String[]
          { PropertiesServlet.getProperty("clx.notifylist") }, messageBody, sb.toString(), urgent, null);

    }
    catch (Exception e)
    {
      log4jLogger.error(e);
    }
  }
}
