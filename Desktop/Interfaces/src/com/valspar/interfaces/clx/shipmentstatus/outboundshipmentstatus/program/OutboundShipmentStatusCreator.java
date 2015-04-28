package com.valspar.interfaces.clx.shipmentstatus.outboundshipmentstatus.program;

import com.valspar.interfaces.clx.common.beans.*;
import com.valspar.clx.shipmentstatus.outboundshipmentstatus.generated.*;
import com.valspar.interfaces.common.utils.DateUtility;
import java.io.ByteArrayOutputStream;
import javax.xml.bind.*;
import org.apache.log4j.Logger;

public class OutboundShipmentStatusCreator
{
  private static Logger log4jLogger = Logger.getLogger(OutboundShipmentStatusCreator.class);

  public static String createShipmentStatusXml(ShipmentStatusStagingBean shipmentStatusStagingBean)
  {
    ShipmentStatusLineBean shipmentStatusLineBean = shipmentStatusStagingBean.getShipmentStatusLineBeanList().get(0);
    ObjectFactory myObjFact = new ObjectFactory();
    ShipmentStatusTransaction shipmentStatusTransaction = myObjFact.createShipmentStatusTransaction();
    TransactionInformation transactionHeader = new TransactionInformation();
    shipmentStatusTransaction.setTransactionInformation(transactionHeader);
    DocId docId = new DocId();
    docId.setContent(shipmentStatusLineBean.getDocId());
    transactionHeader.setDocId(docId);
    transactionHeader.setVersion("2.0.0");

    Sender sender = new Sender();
    Organization senderOrg = new Organization();
    OrganizationId senderOrgId = new OrganizationId();
    OrganizationName senderOrgName = new OrganizationName();
    senderOrgId.setContent(shipmentStatusLineBean.getSenderOrgId());
    senderOrg.setOrganizationId(senderOrgId);
    senderOrgName.setContent(shipmentStatusLineBean.getSenderOrgName());
    senderOrg.setOrganizationName(senderOrgName);
    sender.setOrganization(senderOrg);
    transactionHeader.setSender(sender);

    Receiver receiver = new Receiver();
    Organization receiverOrg = new Organization();
    OrganizationId receiverOrgId = new OrganizationId();
    OrganizationName receiverOrgName = new OrganizationName();
    receiverOrgId.setContent(shipmentStatusLineBean.getReceiverOrgId());
    receiverOrg.setOrganizationId(receiverOrgId);
    receiverOrgName.setContent(shipmentStatusLineBean.getReceiverOrgName());
    receiverOrg.setOrganizationName(receiverOrgName);
    receiver.setOrganization(receiverOrg);
    transactionHeader.setReceiver(receiver);

    TransactionHistory history = new TransactionHistory();
    Transaction historyTransaction = new Transaction();

    Date transDate = new Date();
    Day transDay = new Day();
    Month transMonth = new Month();
    Year transYear = new Year();
    transDay.setContent(DateUtility.getDayOfMonth(shipmentStatusLineBean.getTransDate()));
    transDate.setDay(transDay);
    transMonth.setContent(DateUtility.getMonth(shipmentStatusLineBean.getTransDate()));
    transDate.setMonth(transMonth);
    transYear.setContent(DateUtility.getYear(shipmentStatusLineBean.getTransDate()));
    transDate.setYear(transYear);
    historyTransaction.setDate(transDate);

    Time transTime = new Time();
    Hour transHour = new Hour();
    Minute transMinute = new Minute();
    Second transSecond = new Second();
    TimeZone transTimeZone = new TimeZone();
    transHour.setContent(DateUtility.getHourOfDay(shipmentStatusLineBean.getTransDate()));
    transTime.setHour(transHour);
    transMinute.setContent(DateUtility.getMinute(shipmentStatusLineBean.getTransDate()));
    transTime.setMinute(transMinute);
    transSecond.setContent(DateUtility.getSecond(shipmentStatusLineBean.getTransDate()));
    transTime.setSecond(transSecond);
    transTimeZone.setContent(shipmentStatusLineBean.getTimeZone());
    transTime.setTimeZone(transTimeZone);
    historyTransaction.setTime(transTime);

    Sender historySender = new Sender();
    Organization historySenderOrg = new Organization();
    OrganizationId historySenderOrgId = new OrganizationId();
    OrganizationName historySenderOrgName = new OrganizationName();
    historySenderOrgId.setContent(shipmentStatusLineBean.getSenderOrgId());
    historySenderOrg.setOrganizationId(historySenderOrgId);
    historySenderOrgName.setContent(shipmentStatusLineBean.getSenderOrgName());
    historySenderOrg.setOrganizationName(historySenderOrgName);
    historySender.setOrganization(historySenderOrg);
    historyTransaction.setSender(historySender);

    Receiver historyReceiver = new Receiver();
    Organization historyReceiverOrg = new Organization();
    OrganizationId historyReceiverOrgId = new OrganizationId();
    OrganizationName historyReceiverOrgName = new OrganizationName();
    historyReceiverOrgId.setContent(shipmentStatusLineBean.getReceiverOrgId());
    historyReceiverOrg.setOrganizationId(historyReceiverOrgId);
    historyReceiverOrgName.setContent(shipmentStatusLineBean.getReceiverOrgName());
    historyReceiverOrg.setOrganizationName(historyReceiverOrgName);
    historyReceiver.setOrganization(historyReceiverOrg);
    historyTransaction.setReceiver(historyReceiver);

    history.getTransaction().add(historyTransaction);
    transactionHeader.setTransactionHistory(history);

    ShipmentStatus shipmentStatus = new ShipmentStatus();
    TransactionId orderTransactionId = new TransactionId();
    orderTransactionId.setContent(shipmentStatusLineBean.getTransId());
    shipmentStatus.setTransactionId(orderTransactionId);

    if (shipmentStatusLineBean.getTransType() != null)
    {
      TransactionType orderTransactionType = new TransactionType();
      orderTransactionType.setContent(shipmentStatusLineBean.getTransType());
      shipmentStatus.setTransactionType(orderTransactionType);
    }

    ShipmentId shipmentId = new ShipmentId();
    shipmentId.setContent(shipmentStatusLineBean.getShipmentId());
    shipmentStatus.setShipmentId(shipmentId);

    if (shipmentStatusLineBean.getTrailerNumber() != null && shipmentStatusLineBean.getEquipmentDescription() != null)
    {
      Equipment equipment = new Equipment();
      EquipmentDescription equipmentDescription = new EquipmentDescription();
      equipmentDescription.setContent(shipmentStatusLineBean.getEquipmentDescription());
      equipment.setEquipmentDescription(equipmentDescription);
      TrailerNumber trailerNumber = new TrailerNumber();
      trailerNumber.setContent(shipmentStatusLineBean.getTrailerNumber());
      equipment.setTrailerNumber(trailerNumber);
      shipmentStatus.setEquipment(equipment);
    }

    if (shipmentStatusLineBean.getReference1() != null)
    {
      OtherReference reference1 = new OtherReference();
      Id referenceId1 = new Id();
      Type referenceType1 = new Type();
      referenceId1.setContent(shipmentStatusLineBean.getReference1());
      referenceType1.setContent(shipmentStatusLineBean.getReference1Type());
      reference1.setId(referenceId1);
      reference1.setType(referenceType1);
      shipmentStatus.getOtherReference().add(reference1);
    }

    if (shipmentStatusLineBean.getReference2() != null)
    {
      OtherReference reference2 = new OtherReference();
      Id referenceId2 = new Id();
      Type referenceType2 = new Type();
      referenceId2.setContent(shipmentStatusLineBean.getReference2());
      referenceType2.setContent(shipmentStatusLineBean.getReference2Type());
      reference2.setId(referenceId2);
      reference2.setType(referenceType2);
      shipmentStatus.getOtherReference().add(reference2);
    }

    Shipper shipper = new Shipper();
    OrganizationId shipperOrgId = new OrganizationId();
    OrganizationName shipperOrgName = new OrganizationName();
    shipperOrgId.setContent(shipmentStatusLineBean.getSenderOrgId());
    shipperOrgName.setContent(shipmentStatusLineBean.getSenderOrgName());
    shipper.setOrganizationId(shipperOrgId);
    shipper.setOrganizationName(shipperOrgName);
    shipmentStatus.setShipper(shipper);

    ShipmentStatusTime shipmentStatusTime = new ShipmentStatusTime();
    Date statusDate = new Date();
    Time statusTime = new Time();
    Day statusDay = new Day();
    Month statusMonth = new Month();
    Year statusYear = new Year();
    Hour statusHour = new Hour();
    Minute statusMinute = new Minute();
    Second statusSecond = new Second();
    TimeZone statusTimeZone = new TimeZone();
    statusDay.setContent(DateUtility.getDayOfMonth(shipmentStatusLineBean.getShipmentStatusTime()));
    statusDate.setDay(statusDay);
    statusMonth.setContent(DateUtility.getMonth(shipmentStatusLineBean.getShipmentStatusTime()));
    statusDate.setMonth(statusMonth);
    statusYear.setContent(DateUtility.getYear(shipmentStatusLineBean.getShipmentStatusTime()));
    statusDate.setYear(statusYear);
    shipmentStatusTime.setDate(statusDate);
    statusHour.setContent(DateUtility.getHourOfDay(shipmentStatusLineBean.getShipmentStatusTime()));
    statusTime.setHour(statusHour);
    statusMinute.setContent(DateUtility.getMinute(shipmentStatusLineBean.getShipmentStatusTime()));
    statusTime.setMinute(statusMinute);
    statusSecond.setContent(DateUtility.getSecond(shipmentStatusLineBean.getShipmentStatusTime()));
    statusTime.setSecond(statusSecond);
    statusTimeZone.setContent(shipmentStatusLineBean.getTimeZone());
    statusTime.setTimeZone(statusTimeZone);
    shipmentStatusTime.setTime(statusTime);

    ShipmentStatusCode shipmentStatusCode = new ShipmentStatusCode();
    shipmentStatusCode.setValue(shipmentStatusLineBean.getShipmentStatusCode());

    ShipmentStatusReason shipmentStatusReason = new ShipmentStatusReason();
    shipmentStatusReason.setContent(shipmentStatusLineBean.getShipmentStatusReason());

    ShipmentStatusDetail shipmentStatusDetail = new ShipmentStatusDetail();
    shipmentStatusDetail.setShipmentStatusCode(shipmentStatusCode);
    shipmentStatusDetail.setShipmentStatusReason(shipmentStatusReason);
    shipmentStatusDetail.setShipmentStatusTime(shipmentStatusTime);
    shipmentStatus.setShipmentStatusDetail(shipmentStatusDetail);

    shipmentStatusTransaction.getShipmentStatus().add(shipmentStatus);

    StringBuilder sb = new StringBuilder();
    try
    {
      JAXBContext jaxbContext = JAXBContext.newInstance(ShipmentStatusTransaction.class);
      Marshaller jaxbMarshaller = jaxbContext.createMarshaller();
      jaxbMarshaller.setProperty(Marshaller.JAXB_FRAGMENT, true);
      jaxbMarshaller.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, true);
      ByteArrayOutputStream baos = new ByteArrayOutputStream();
      jaxbMarshaller.marshal(shipmentStatusTransaction, baos);
      sb.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n");
      sb.append("<!DOCTYPE ShipmentStatusTransaction PUBLIC \"ShipmentStatus\" \"ShipmentStatus.dtd\">\n");
      sb.append(baos.toString("UTF-8"));
    }
    catch (Exception e)
    {
      log4jLogger.error(e);
      OutboundShipmentStatus.sendShipmentStatusNotifcationEmail(shipmentStatusStagingBean, e);
    }

    String xmlMessage = sb.toString();
    log4jLogger.info("OutboundShipmentStatusCreator.createShipmentStatusXml XML message is: " + xmlMessage);
    return xmlMessage;
  }
}
