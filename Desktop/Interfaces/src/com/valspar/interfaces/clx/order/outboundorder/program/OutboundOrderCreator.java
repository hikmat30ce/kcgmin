package com.valspar.interfaces.clx.order.outboundorder.program;

import com.valspar.clx.order.outboundorder.generated.*;
import com.valspar.interfaces.clx.common.beans.*;
import com.valspar.interfaces.common.utils.DateUtility;
import java.io.ByteArrayOutputStream;
import java.util.ListIterator;
import javax.xml.bind.*;
import org.apache.commons.lang3.StringUtils;
import org.apache.log4j.Logger;

public class OutboundOrderCreator
{
  private static Logger log4jLogger = Logger.getLogger(OutboundOrderCreator.class);

  public static String createOrderXml(OrderStagingBean orderStagingBean)
  {
    String xmlMessage = null;
    try
    {
      OrderLineBean orderLineBean = orderStagingBean.getOrderLineBeanList().get(0);
      ObjectFactory myObjFact = new ObjectFactory();
      OrderTransaction orderTransaction = myObjFact.createOrderTransaction();
      TransactionInformation transactionHeader = new TransactionInformation();
      orderTransaction.setTransactionInformation(transactionHeader);
      DocId docId = new DocId();
      docId.setContent(orderLineBean.getDocId());
      transactionHeader.setDocId(docId);
      transactionHeader.setVersion("2.0.0");

      Sender sender = new Sender();
      Organization senderOrg = new Organization();
      OrganizationId senderOrgId = new OrganizationId();
      OrganizationName senderOrgName = new OrganizationName();
      senderOrgId.setContent(orderLineBean.getSenderOrgId());
      senderOrg.setOrganizationId(senderOrgId);
      senderOrgName.setContent(orderLineBean.getSenderOrgName());
      senderOrg.setOrganizationName(senderOrgName);
      sender.setOrganization(senderOrg);
      transactionHeader.setSender(sender);

      Receiver receiver = new Receiver();
      Organization receiverOrg = new Organization();
      OrganizationId receiverOrgId = new OrganizationId();
      OrganizationName receiverOrgName = new OrganizationName();
      receiverOrgId.setContent(orderLineBean.getReceiverOrgId());
      receiverOrg.setOrganizationId(receiverOrgId);
      receiverOrgName.setContent(orderLineBean.getReceiverOrgName());
      receiverOrg.setOrganizationName(receiverOrgName);
      receiver.setOrganization(receiverOrg);
      transactionHeader.setReceiver(receiver);

      TransactionHistory history = new TransactionHistory();
      Transaction historyTransaction = new Transaction();

      Date transDate = new Date();
      Day transDay = new Day();
      Month transMonth = new Month();
      Year transYear = new Year();
      transDay.setContent(DateUtility.getDayOfMonth(orderLineBean.getTransDate()));
      transDate.setDay(transDay);
      transMonth.setContent(DateUtility.getMonth(orderLineBean.getTransDate()));
      transDate.setMonth(transMonth);
      transYear.setContent(DateUtility.getYear(orderLineBean.getTransDate()));
      transDate.setYear(transYear);
      historyTransaction.setDate(transDate);

      Time transTime = new Time();
      Hour transHour = new Hour();
      Minute transMinute = new Minute();
      Second transSecond = new Second();
      TimeZone transTimeZone = new TimeZone();
      transHour.setContent(DateUtility.getHourOfDay(orderLineBean.getTransDate()));
      transTime.setHour(transHour);
      transMinute.setContent(DateUtility.getMinute(orderLineBean.getTransDate()));
      transTime.setMinute(transMinute);
      transSecond.setContent(DateUtility.getSecond(orderLineBean.getTransDate()));
      transTime.setSecond(transSecond);
      transTimeZone.setContent(orderLineBean.getTimeZone());
      transTime.setTimeZone(transTimeZone);
      historyTransaction.setTime(transTime);

      Sender historySender = new Sender();
      Organization historySenderOrg = new Organization();
      OrganizationId historySenderOrgId = new OrganizationId();
      OrganizationName historySenderOrgName = new OrganizationName();
      historySenderOrgId.setContent(orderLineBean.getSenderOrgId());
      historySenderOrg.setOrganizationId(historySenderOrgId);
      historySenderOrgName.setContent(orderLineBean.getSenderOrgName());
      historySenderOrg.setOrganizationName(historySenderOrgName);
      historySender.setOrganization(historySenderOrg);
      historyTransaction.setSender(historySender);

      Receiver historyReceiver = new Receiver();
      Organization historyReceiverOrg = new Organization();
      OrganizationId historyReceiverOrgId = new OrganizationId();
      OrganizationName historyReceiverOrgName = new OrganizationName();
      historyReceiverOrgId.setContent(orderLineBean.getReceiverOrgId());
      historyReceiverOrg.setOrganizationId(historyReceiverOrgId);
      historyReceiverOrgName.setContent(orderLineBean.getReceiverOrgName());
      historyReceiverOrg.setOrganizationName(historyReceiverOrgName);
      historyReceiver.setOrganization(historyReceiverOrg);
      historyTransaction.setReceiver(historyReceiver);
      history.getTransaction().add(historyTransaction);
      transactionHeader.setTransactionHistory(history);

      Order order = new Order();
      SubShipment subShipment = new SubShipment();
      TransactionId orderTransactionId = new TransactionId();
      orderTransactionId.setContent(orderLineBean.getTransId());
      order.setTransactionId(orderTransactionId);
      TransactionType orderTransactionType = new TransactionType();
      orderTransactionType.setContent(orderLineBean.getTransType());
      order.setTransactionType(orderTransactionType);
      ShipmentId shipmentId = new ShipmentId();
      shipmentId.setContent(orderLineBean.getShipmentId());
      order.setShipmentId(shipmentId);

      ActionCode actionCode = new ActionCode();
      actionCode.setValue(orderLineBean.getActionCode());
      order.setActionCode(actionCode);

      Equipment equipment = new Equipment();
      EquipmentDescription equipmentDescription = new EquipmentDescription();
      if (StringUtils.equalsIgnoreCase(orderLineBean.getEquipmentDescription(), "Y"))
      {
        equipmentDescription.setContent("TJ");
      }
      else if (orderStagingBean.isTempControlled())
      {
        equipmentDescription.setContent("RT");
      }
      else
      {
        equipmentDescription.setContent("TF");
      }
      equipment.setEquipmentDescription(equipmentDescription);
      order.setEquipment(equipment);

      if (orderLineBean.getAttribute1() != null)
      {
        UserDefinedAccessorialAttribute hazmatAttribute = new UserDefinedAccessorialAttribute();
        hazmatAttribute.setContent(orderLineBean.getAttribute1());
        order.getUserDefinedAccessorialAttribute().add(hazmatAttribute);
        subShipment.getUserDefinedAccessorialAttribute().add(hazmatAttribute);
        buildOtherReference("Y", orderLineBean.getReference10Type(), order, subShipment);
      }

      GrossWeight grossWeight = new GrossWeight();
      grossWeight.setUnitOfMeasure(orderLineBean.getGrossWeightUOM());
      grossWeight.setContent(orderLineBean.getGrossWeight());
      order.setGrossWeight(grossWeight);

      LineHaulMode lineHaulMode = new LineHaulMode();
      lineHaulMode.setValue(orderLineBean.getLineHaulMode());
      order.setLineHaulMode(lineHaulMode);

      MovementType movementType = new MovementType();
      movementType.setValue(orderLineBean.getMovementType());
      order.setMovementType(movementType);

      PaymentMethod paymentMethod = new PaymentMethod();
      paymentMethod.setValue(orderLineBean.getPaymentMethod());
      order.setPaymentMethod(paymentMethod);

      OrganizationId carrierOrgId = new OrganizationId();
      carrierOrgId.setContent(orderStagingBean.getCarrierSCAC());
      Carrier carrier = new Carrier();
      carrier.setOrganizationId(carrierOrgId);
      order.setCarrier(carrier);

      buildOtherReference(orderLineBean.getReference1(), orderLineBean.getReference1Type(), order, null);
      buildOtherReference(orderLineBean.getReference2(), orderLineBean.getReference2Type(), order, null);
      buildOtherReference(orderLineBean.getReference5(), orderLineBean.getReference5Type(), order, null);
      buildOtherReference(orderLineBean.getReference6(), orderLineBean.getReference6Type(), order, null);
      buildOtherReference(orderLineBean.getReference7(), orderLineBean.getReference7Type(), order, subShipment);
      buildOtherReference(orderLineBean.getReference8(), orderLineBean.getReference8Type(), order, subShipment);
      if (orderStagingBean.isTempControlled())
      {
        buildOtherReference("Y", orderLineBean.getReference14Type(), order, subShipment);
      }
      buildOtherReference(orderLineBean.getReference16(), orderLineBean.getReference16Type(), order, subShipment);
      buildOtherReference(orderLineBean.getReference17(), orderLineBean.getReference17Type(), order, subShipment);
      
      ServiceType serviceType = new ServiceType();
      if (orderStagingBean.isExpedited())
      {
        buildOtherReference("EXPEDITE", orderLineBean.getReference9Type(), order, subShipment);
        serviceType.setValue("EXPEDITE");
        InternalShipmentPriority internalShipmentPriority = new InternalShipmentPriority();
        internalShipmentPriority.setContent("EXPEDITE");
        order.setInternalShipmentPriority(internalShipmentPriority);
      }
      else
      {
        serviceType.setValue(orderLineBean.getServiceType());
      }
      order.setServiceType(serviceType);

      Shipper shipper = new Shipper();
      OrganizationId shipperOrgId = new OrganizationId();
      OrganizationName shipperOrgName = new OrganizationName();
      shipperOrgId.setContent(orderLineBean.getOriginOrgId());
      shipperOrgName.setContent(orderLineBean.getOriginOrgName());
      shipper.setOrganizationId(shipperOrgId);
      shipper.setOrganizationName(shipperOrgName);
      order.setShipper(shipper);

      BOL bol = new BOL();
      bol.setContent(orderLineBean.getBol());
      subShipment.setBOL(bol);

      PONumber poNumber = new PONumber();
      poNumber.setContent(orderLineBean.getPoNumber());
      subShipment.getPONumber().add(poNumber);

      GrossWeight ssGrossWeight = new GrossWeight();
      ssGrossWeight.setUnitOfMeasure(orderLineBean.getSubGrossWeightUOM());
      ssGrossWeight.setContent(orderLineBean.getSubGrossWeight());
      subShipment.setGrossWeight(ssGrossWeight);

      LadingQuantity ssLadingQty = new LadingQuantity();
      ssLadingQty.setUnitOfMeasure(orderLineBean.getLadingQuantityUOM());
      ssLadingQty.setContent(orderLineBean.getLadingQuantity());
      subShipment.setLadingQuantity(ssLadingQty);

      ListIterator listIterator = orderStagingBean.getOrderLineBeanList().listIterator();
      while (listIterator.hasNext())
      {
        OrderLineBean currentOrderLineBean = (OrderLineBean) listIterator.next();
        if (orderStagingBean.getOrderLineBeanList().size() != 1 && listIterator.hasNext())
        {
          OrderLineBean nextOrderLineBean = orderStagingBean.getOrderLineBeanList().get(listIterator.nextIndex());
          if (!StringUtils.equalsIgnoreCase(currentOrderLineBean.getSalesOrderNumber(), nextOrderLineBean.getSalesOrderNumber()))
          {
            SalesOrderNumber salesOrderNumber = new SalesOrderNumber();
            salesOrderNumber.setContent(currentOrderLineBean.getSalesOrderNumber());
            subShipment.getSalesOrderNumber().add(salesOrderNumber);
          }
        }
        else
        {
          SalesOrderNumber salesOrderNumber = new SalesOrderNumber();
          salesOrderNumber.setContent(orderLineBean.getSalesOrderNumber());
          subShipment.getSalesOrderNumber().add(salesOrderNumber);
        }

        if (!orderLineBean.isDelete())
        {
          ShippingUnit ssShippingUnit = new ShippingUnit();
          ProductLineItem productLineItem = new ProductLineItem();

          GrossWeight itemGrossWeight = new GrossWeight();
          itemGrossWeight.setUnitOfMeasure(currentOrderLineBean.getProductGrossWeightUom());
          itemGrossWeight.setContent(currentOrderLineBean.getProductGrossWeight());
          productLineItem.setGrossWeight(itemGrossWeight);

          LadingQuantity itemLadingQty = new LadingQuantity();
          itemLadingQty.setUnitOfMeasure(currentOrderLineBean.getProductLadingQtyUOM());
          itemLadingQty.setContent(currentOrderLineBean.getProductLadingQty());
          productLineItem.setLadingQuantity(itemLadingQty);

          ProductId productId = new ProductId();
          ProductDescription productDescription = new ProductDescription();
          productId.setContent(currentOrderLineBean.getProductId());
          productDescription.setContent(currentOrderLineBean.getProductDescription());
          productLineItem.setProductId(productId);
          productLineItem.setProductDescription(productDescription);
          UserDefinedReference userDefinedReference15 = new UserDefinedReference();
          Id userDefinedReference15Id = new Id();
          userDefinedReference15Id.setContent(currentOrderLineBean.getReference15());
          userDefinedReference15.setId(userDefinedReference15Id);
          Type userDefinedReference15Type = new Type();
          userDefinedReference15Type.setContent(currentOrderLineBean.getReference15Type());
          userDefinedReference15.setType(userDefinedReference15Type);
          productLineItem.getUserDefinedReference().add(userDefinedReference15);
          ssShippingUnit.getProductLineItem().add(productLineItem);
          subShipment.getShippingUnit().add(ssShippingUnit);
        }
        buildOtherReference(orderLineBean.getReference3(), orderLineBean.getReference3Type(), null, subShipment);
        buildOtherReference(orderLineBean.getReference4(), orderLineBean.getReference4Type(), order, subShipment);
        buildOtherReference(orderLineBean.getReference11(), orderLineBean.getReference11Type(), order, subShipment);
        buildOtherReference(orderLineBean.getReference12(), orderLineBean.getReference12Type(), order, subShipment);
        buildOtherReference(orderLineBean.getReference13(), orderLineBean.getReference13Type(), order, subShipment);
      }

      Volume volume = new Volume();
      volume.setUnitOfMeasure(orderLineBean.getVolumeUOM());
      volume.setContent(orderLineBean.getVolume());
      subShipment.setVolume(volume);

      Origin ssOrigin = new Origin();
      if (!orderLineBean.isDelete())
      {
        Address origAddress = buildAddress(orderLineBean.getOrigAddress1(), orderLineBean.getOrigAddress2(), orderLineBean.getOrigAddress3(), orderLineBean.getOrigAddress4(), orderLineBean.getOrigCity(), orderLineBean.getOrigStateProvince(), orderLineBean.getOrigPostalCode(), orderLineBean.getOrigCountry());
        ssOrigin.setAddress(origAddress);
      }
      ssOrigin.setLocation(buildLocation(orderLineBean.getOrigLocationId(),orderLineBean.getOrigLocationName(),orderLineBean.getOrigLocationType()));
      ssOrigin.setPlanned(buildPlanned(orderLineBean.getOrigPlannedFromDate(), orderLineBean.getOrigPlannedToDate()));

      StopNumber origStopNumber = new StopNumber();
      origStopNumber.setContent(orderLineBean.getOrigStopNumber());
      ssOrigin.setStopNumber(origStopNumber);
      StopReasonCode origStopReasonCode = new StopReasonCode();
      origStopReasonCode.setValue(orderLineBean.getOrigStopReason());
      ssOrigin.setStopReasonCode(origStopReasonCode);
      subShipment.setOrigin(ssOrigin);

      Destination destination = new Destination();
      if (!orderLineBean.isDelete())
      {
        Address destAddress = buildAddress(orderLineBean.getDestAddress1(), orderLineBean.getDestAddress2(), orderLineBean.getDestAddress3(), orderLineBean.getDestAddress4(), orderLineBean.getDestCity(), orderLineBean.getDestStateProvince(), orderLineBean.getDestPostalCode(), orderLineBean.getDestCountry());
        destination.setAddress(destAddress);
      }
      destination.setLocation(buildLocation(orderLineBean.getDestLocationId(),orderLineBean.getDestLocationName(),orderLineBean.getDestLocationType()));

      StopNumber destStopNumber = new StopNumber();
      destStopNumber.setContent(orderLineBean.getDestStopNumber());
      destination.setStopNumber(destStopNumber);
      StopReasonCode destStopReasonCode = new StopReasonCode();
      destStopReasonCode.setValue(orderLineBean.getDestStopReason());
      destination.setStopReasonCode(destStopReasonCode);
      Note destinationNote = new Note();
      destinationNote.setContent(orderLineBean.getDestNote());
      destination.getNote().add(destinationNote);
      subShipment.setDestination(destination);
      order.setSubShipment(subShipment);
      orderTransaction.getOrder().add(order);

      StringBuilder sb = new StringBuilder();
      JAXBContext jaxbContext = JAXBContext.newInstance(OrderTransaction.class);
      Marshaller jaxbMarshaller = jaxbContext.createMarshaller();
      jaxbMarshaller.setProperty(Marshaller.JAXB_FRAGMENT, true);
      jaxbMarshaller.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, true);
      ByteArrayOutputStream baos = new ByteArrayOutputStream();
      jaxbMarshaller.marshal(orderTransaction, baos);
      sb.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n");
      sb.append("<!DOCTYPE OrderTransaction PUBLIC \"Order\" \"Order.dtd\">\n");
      sb.append(baos.toString("UTF-8"));
      xmlMessage = sb.toString();
  }
  catch (Exception e)
  {
    log4jLogger.error(e);
    OutboundOrder.sendOrderNotifcationEmail(orderStagingBean, e);
  }
    return xmlMessage;
  }

  private static void buildOtherReference(String id, String type, Order order, SubShipment subShipment)
  {
    if (id != null)
    {
      OtherReference otherReference = new OtherReference();
      Id otherReferenceId = new Id();
      Type otherReferenceType = new Type();
      otherReferenceId.setContent(id);
      otherReferenceType.setContent(type);
      otherReference.setId(otherReferenceId);
      otherReference.setType(otherReferenceType);
      if (order != null)
      {
        order.getOtherReference().add(otherReference);
      }
      if (subShipment != null)
      {
        subShipment.getOtherReference().add(otherReference);
      }
    }
  }
  
  private static Address buildAddress(String addr1, String addr2, String addr3, String addr4, String city, String state, String postalCode, String country)
  {
    Address address = new Address();
    Address1 address1 = new Address1();
    Address2 address2 = new Address2();
    Address3 address3 = new Address3();
    Address4 address4 = new Address4();
    City retCity = new City();
    StateProvince retState = new StateProvince();
    PostalCode retPostalCode = new PostalCode();
    Country retCountry = new Country();
    address1.setContent(addr1);
    address2.setContent(addr2);
    address3.setContent(addr3);
    address4.setContent(addr4);
    retCity.setContent(city);
    retState.setContent(state);
    retPostalCode.setContent(postalCode);
    retCountry.setContent(country);
    address.setAddress1(address1);
    address.setAddress2(address2);
    address.setAddress3(address3);
    address.setAddress4(address4);
    address.setCity(retCity);
    address.setStateProvince(retState);
    address.setPostalCode(retPostalCode);
    address.setCountry(retCountry);
    return address;
  }
  
  private static Location buildLocation(String locationId, String locationName, String locationType)
  {
    Location location = new Location();
    Id retLocationId = new Id();
    Name retLocationName = new Name();
    Type retLocationType = new Type();
    retLocationId.setContent(locationId);
    retLocationName.setContent(locationName);
    retLocationType.setContent(locationType);
    location.setId(retLocationId);
    location.setName(retLocationName);
    location.setType(retLocationType);
    
    return location;
  }
  
  private static Planned buildPlanned(java.util.Date fromDate, java.util.Date toDate)
  {
    Planned planned = new Planned();
    From plannedFrom = new From();
    Date plannedFromDate = new Date();
    Day plannedFromDay = new Day();
    Month plannedFromMonth = new Month();
    Year plannedFromYear = new Year();
    plannedFromDay.setContent(DateUtility.getDayOfMonth(fromDate));
    plannedFromDate.setDay(plannedFromDay);
    plannedFromMonth.setContent(DateUtility.getMonth(fromDate));
    plannedFromDate.setMonth(plannedFromMonth);
    plannedFromYear.setContent(DateUtility.getYear(fromDate));
    plannedFromDate.setYear(plannedFromYear);
    plannedFrom.setDate(plannedFromDate);
    planned.getContent().add(plannedFrom);
    
    To plannedTo = new To();
    Date plannedToDate = new Date();
    Day plannedToDay = new Day();
    Month plannedToMonth = new Month();
    Year plannedToYear = new Year();
    plannedToDay.setContent(DateUtility.getDayOfMonth(toDate));
    plannedToDate.setDay(plannedToDay);
    plannedToMonth.setContent(DateUtility.getMonth(toDate));
    plannedToDate.setMonth(plannedToMonth);
    plannedToYear.setContent(DateUtility.getYear(toDate));
    plannedToDate.setYear(plannedToYear);
    plannedTo.setDate(plannedToDate);
    planned.getContent().add(plannedTo);
    return planned;
  }
}
