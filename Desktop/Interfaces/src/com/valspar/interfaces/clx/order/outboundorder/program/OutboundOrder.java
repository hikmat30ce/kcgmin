package com.valspar.interfaces.clx.order.outboundorder.program;

import com.valspar.clx.returnvalue.generated.ReturnValue;
import com.valspar.interfaces.clx.common.api.CLXBaseImportAPI;
import com.valspar.interfaces.clx.common.beans.*;
import com.valspar.interfaces.clx.common.dao.ClxDAO;
import com.valspar.interfaces.clx.order.outboundorder.dao.OutboundOrderDAO;
import com.valspar.interfaces.common.BaseInterface;
import com.valspar.interfaces.common.servlets.PropertiesServlet;
import com.valspar.interfaces.common.utils.NotificationUtility;
import java.util.*;
import org.apache.commons.lang3.StringUtils;
import org.apache.log4j.Logger;

public class OutboundOrder extends BaseInterface
{
  private static Logger log4jLogger = Logger.getLogger(OutboundOrder.class);

  public void execute()
  {
    OrderStagingBean lastOrderStagingBean = null;
    boolean deleteOrderLogFile = true;
    try
    {
      log4jLogger.info("Starting CLX Outbound Order Interface...");
      List<OrderStagingBean> orderStagingBeanList = OutboundOrderDAO.fetchOrders(getParameterValue("senderIdKey"));
      if (!orderStagingBeanList.isEmpty())
      {
        ClxDAO.updateStagingTableForList(orderStagingBeanList);
        for (OrderStagingBean orderStagingBean: orderStagingBeanList)
        {
          deleteOrderLogFile = false;
          log4jLogger.info("Processing transId: " + orderStagingBean.getTransId());
          lastOrderStagingBean = orderStagingBean;
          if (orderStagingBean.isDeleteAction())
          {
            orderStagingBean.setOrderLineBeanList(buildOrderLineBeanForDelete(orderStagingBean));
          }
          else
          {
            orderStagingBean.setOrderLineBeanList(OutboundOrderDAO.fetchOrderLines(orderStagingBean));
          }
          log4jLogger.info("orderLineBeanList size:" + orderStagingBean.getOrderLineBeanList().size());
          if (!orderStagingBean.getOrderLineBeanList().isEmpty())
          {
            if (!orderStagingBean.isDeleteAction())
            {
              OutboundOrderDAO.populateTempControl(orderStagingBean);
            }
            importOrder(orderStagingBean);
          }
          else
          {
            log4jLogger.info("orderLineBeanList is empty!");
            orderStagingBean.setReturnMessage("No order lines found");
            orderStagingBean.setReturnCode("E");
            ClxDAO.updateStagingTableForBean(orderStagingBean);
          }
          log4jLogger.info("Completed processing transId: " + orderStagingBean.getTransId());
        }
      }
    }
    catch (Exception e)
    {
      deleteOrderLogFile = false;
      log4jLogger.error(e);
      lastOrderStagingBean.setReturnMessage("Exception thrown: " + e);
      lastOrderStagingBean.setReturnCode("E");
      ClxDAO.updateStagingTableForBean(lastOrderStagingBean);
      sendOrderNotifcationEmail(lastOrderStagingBean, e);
    }
    finally
    {
      this.setDeleteLogFile(deleteOrderLogFile);
      log4jLogger.info("End CLX Outbound Order Interface.");
    }
  }

  private static void importOrder(OrderStagingBean orderStagingBean)
  {
    log4jLogger.info("Starting OutboundOrder.importOrder()...");

    try
    {
      CLXBaseImportAPI clxBaseImportAPI = new CLXBaseImportAPI();
      orderStagingBean.setGeneratedXmlMessage(OutboundOrderCreator.createOrderXml(orderStagingBean));
      ClxDAO.insertAuditXML(orderStagingBean.getSenderIdKey(), orderStagingBean.getTransId(), null, orderStagingBean.getGeneratedXmlMessage(), "OutboundOrder");
      if (!StringUtils.isEmpty(orderStagingBean.getGeneratedXmlMessage()))
      {
        ReturnValue returnValue = clxBaseImportAPI.importDocument(orderStagingBean.getGeneratedXmlMessage());
        orderStagingBean.setReturnCode(returnValue.getReturnCode());
        orderStagingBean.setReturnMessage(returnValue.getMessage());
      }
      else if (!StringUtils.isEmpty(orderStagingBean.getGeneratedXmlMessage()) || orderStagingBean.isErrorStatus())
      {
        log4jLogger.error("Error in OutboundOrder.importOrder() Trans Id: " + orderStagingBean.getTransId());
        sendOrderNotifcationEmail(orderStagingBean, null);
      }
      else
      {
        if (orderStagingBean.isInventoryTransfer())
        {
          OutboundOrderDAO.updateInventoryTransferMaster(orderStagingBean);
        }
      }
    }
    catch (Exception e)
    {
      log4jLogger.error(e);
      sendOrderNotifcationEmail(orderStagingBean, e);
    }
    finally
    {
      if (orderStagingBean != null && orderStagingBean.getTransId() != null && orderStagingBean.getReturnCode() != null)
      {
        ClxDAO.updateStagingTableForBean(orderStagingBean);  //TODO Think about what to do if they are null...
      }
    }
  }

  public static void sendOrderNotifcationEmail(OrderStagingBean orderStagingBean, Exception ex)
  {
    try
    {
      boolean urgent = true;
      Map<String, Object> rootMap = new HashMap<String, Object>();
      rootMap.put("orderStagingBean", orderStagingBean);
      rootMap.put("exception", ex);
      String messageBody = NotificationUtility.buildMessage("clxorder-notification.ftl", rootMap);
      StringBuilder sb = new StringBuilder();
      sb.append("Error in CLX Outbound Order Interface for Delivery/Transfer Batch: ");
      if (orderStagingBean != null)
      {
        sb.append(orderStagingBean.getDeliveryNumber());
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

  private static List<OrderLineBean> buildOrderLineBeanForDelete(OrderStagingBean orderStagingBean)
  {
    List<OrderLineBean> orderLineBeanList = new ArrayList<OrderLineBean>();
    String senderOrgId = orderStagingBean.getSenderId();

    OrderLineBean orderLineBean = new OrderLineBean();
    orderLineBean.setDocId(orderStagingBean.getTransId());
    orderLineBean.setSenderOrgId(senderOrgId);
    orderLineBean.setSenderOrgName("Valspar");
    orderLineBean.setReceiverOrgId("048599448");
    orderLineBean.setReceiverOrgName("Sterling TMS");
    orderLineBean.setTransId(orderStagingBean.getTransId());
    orderLineBean.setTransDate(orderStagingBean.getCreationDate());
    orderLineBean.setTimeZone("LT");
    orderLineBean.setActionCode(orderStagingBean.getActionCode());
    orderLineBean.setShipmentId("AUTOGEN");
    orderLineBean.setLineHaulMode("ANY");
    orderLineBean.setMovementType("OUTBOUND");
    orderLineBean.setPaymentMethod("PREPAID");
    orderLineBean.setServiceType("NORMAL");
    orderLineBean.setEquipmentDescription("TF");
    orderLineBean.setGrossWeightUOM("POUNDS");
    orderLineBean.setGrossWeight("0");
    orderLineBean.setVolumeUOM("CUBIC_FEET");
    orderLineBean.setVolume("0");
    orderLineBean.setBol(orderStagingBean.getDeliveryNumber());
    orderLineBean.setPoNumber("0");
    orderLineBean.setLadingQuantityUOM("PIECES");
    orderLineBean.setLadingQuantity("0");
    orderLineBean.setSubGrossWeightUOM("POUNDS");
    orderLineBean.setSubGrossWeight("0");
    orderLineBean.setOrigStopNumber("1");
    orderLineBean.setOrigStopReason("COMPLETE_LOAD");
    orderLineBean.setOrigLocationId("DELORIGIN");
    orderLineBean.setOrigLocationType("ZZ");
    orderLineBean.setOriginOrgId(senderOrgId);
    orderLineBean.setOriginOrgName("Valspar");
    orderLineBean.setOrigPlannedFromDate(orderStagingBean.getCreationDate());
    orderLineBean.setOrigPlannedToDate(orderStagingBean.getCreationDate());
    orderLineBean.setDestStopNumber("2");
    orderLineBean.setDestStopReason("COMPLETE_UNLOAD");
    orderLineBean.setDestLocationId("DELDESTINATION");
    orderLineBean.setDestLocationType("ZZ");
    orderLineBeanList.add(orderLineBean);

    return orderLineBeanList;
  }
}
