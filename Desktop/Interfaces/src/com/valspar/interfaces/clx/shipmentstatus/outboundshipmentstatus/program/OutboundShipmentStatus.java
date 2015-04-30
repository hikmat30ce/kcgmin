package com.valspar.interfaces.clx.shipmentstatus.outboundshipmentstatus.program;

import com.valspar.clx.returnvalue.generated.ReturnValue;
import com.valspar.interfaces.clx.common.api.CLXBaseImportAPI;
import com.valspar.interfaces.clx.common.beans.*;
import com.valspar.interfaces.clx.common.dao.ClxDAO;
import com.valspar.interfaces.clx.shipmentstatus.outboundshipmentstatus.dao.OutboundShipmentStatusDAO;
import com.valspar.interfaces.common.BaseInterface;
import com.valspar.interfaces.common.servlets.PropertiesServlet;
import com.valspar.interfaces.common.utils.NotificationUtility;
import java.util.*;
import org.apache.log4j.Logger;

public class OutboundShipmentStatus extends BaseInterface
{
  private static Logger log4jLogger = Logger.getLogger(OutboundShipmentStatus.class);

  public void execute()
  {
    ShipmentStatusStagingBean lastShipmentStatusStagingBean = null;
    boolean deleteShipmentStatusLogFile = true;
    try
    {
      log4jLogger.info("Starting CLX Outbound Shipment Status Interface...");
      List<ShipmentStatusStagingBean> shipmentStatusStagingBeanList = OutboundShipmentStatusDAO.fetchShipmentStatusDeliveries(getParameterValue("senderIdKey"));
      if (!shipmentStatusStagingBeanList.isEmpty())
      { 
        ClxDAO.updateStagingTableForList(shipmentStatusStagingBeanList);
        for (ShipmentStatusStagingBean shipmentStatusStagingBean: shipmentStatusStagingBeanList)
        {
          deleteShipmentStatusLogFile = false;
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
            ClxDAO.updateStagingTableForBean(shipmentStatusStagingBean);
          }
          log4jLogger.info("Completed processing transId: " + shipmentStatusStagingBean.getTransId());
        }
      }
    }
    catch (Exception e)
    {
      deleteShipmentStatusLogFile = false;
      log4jLogger.error(e);
      sendShipmentStatusNotifcationEmail(lastShipmentStatusStagingBean, e);
    }
    finally
    {
      this.setDeleteLogFile(deleteShipmentStatusLogFile);
      log4jLogger.info("End CLX Outbound Shipment Status Interface.");
    }
  }

  private void importShipmentStatus(ShipmentStatusStagingBean shipmentStatusStagingBean)
  {
    try
    {
      CLXBaseImportAPI clxBaseImportAPI = new CLXBaseImportAPI();

      if (shipmentStatusStagingBean.getShipmentStatusLineBeanList().get(0).getShipmentId() != null)
      {
        shipmentStatusStagingBean.setGeneratedXmlMessage(OutboundShipmentStatusCreator.createShipmentStatusXml(shipmentStatusStagingBean));
        ClxDAO.insertAuditXML(shipmentStatusStagingBean.getSenderIdKey(), shipmentStatusStagingBean.getTransId(), null, shipmentStatusStagingBean.getGeneratedXmlMessage(), "OutboundShipmentStatus");
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
        ClxDAO.updateStagingTableForBean(shipmentStatusStagingBean);
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
