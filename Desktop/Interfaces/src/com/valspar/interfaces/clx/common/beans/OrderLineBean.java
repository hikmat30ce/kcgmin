package com.valspar.interfaces.clx.common.beans;

import java.util.Date;
import org.apache.commons.lang3.StringUtils;
 
public class OrderLineBean
{
  private String docId;
  private String senderOrgId;
  private String senderOrgName;
  private String receiverOrgId;
  private String receiverOrgName;
  private Date transDate;
  private String transId;
  private String transType;
  private String timeZone;
  private String actionCode;
  private String shipmentId;
  private String lineHaulMode;
  private String movementType;
  private String paymentMethod;
  private String reference1;
  private String reference1Type;
  private String reference2;
  private String reference2Type;
  private String reference3;
  private String reference3Type;
  private String reference4;
  private String reference4Type;
  private String reference5;
  private String reference5Type;
  private String reference6;
  private String reference6Type;
  private String reference7;
  private String reference7Type;
  private String reference8;
  private String reference8Type;
  private String reference9;
  private String reference9Type;
  private String reference10;
  private String reference10Type;
  private String reference11;
  private String reference11Type;
  private String reference12;
  private String reference12Type;
  private String reference13;
  private String reference13Type;
  private String reference14;
  private String reference14Type;
  private String reference15;
  private String reference15Type;
  private String reference16;
  private String reference16Type;
  private String reference17;
  private String reference17Type;
  private String reference18;
  private String reference18Type;
  private String reference19;
  private String reference19Type;
  private String reference20;
  private String reference20Type;
  private String attribute1;
  private String attribute2;
  private String attribute3;
  private String attribute4;
  private String attribute5;
  private String attribute6;
  private String attribute7;
  private String attribute8;
  private String attribute9;
  private String attribute10;
  private String attribute11;
  private String attribute12;
  private String attribute13;
  private String attribute14;
  private String attribute15;
  private String attribute16;
  private String attribute17;
  private String attribute18;
  private String attribute19;
  private String attribute20;
  private String serviceType;
  private String equipmentDescription;
  private String bol;
  private String poNumber;
  private String salesOrderNumber;
  private String ladingQuantity;
  private String ladingQuantityUOM;
  private String grossWeight;
  private String grossWeightUOM;
  private String volume;
  private String volumeUOM;
  private String origStopNumber;
  private String origStopReason;
  private String origLocationName;
  private String origLocationId;
  private String origLocationType;
  private String originOrgId;
  private String originOrgName;
  private String origAddress1;
  private String origAddress2;
  private String origAddress3;
  private String origAddress4;
  private String origCity;
  private String origStateProvince;
  private String origPostalCode;
  private String origCountry;
  private Date origPlannedFromDate;
  private Date origPlannedToDate;
  private String destStopNumber;
  private String destStopReason;
  private String destLocationName;
  private String destLocationId;
  private String destLocationType;
  private String destOrgId;
  private String destOrgName;
  private String shipToFromLocationId;
  private String destAddress1;
  private String destAddress2;
  private String destAddress3;
  private String destAddress4;
  private String destCity;
  private String destStateProvince;
  private String destPostalCode;
  private String destCountry;
  private Date plannedDate;
  private String destNote;
  private String productId;
  private String productDescription;
  private String productGrossWeightUom;
  private String productGrossWeight;
  private String productLadingQtyUOM;
  private String productLadingQty;
  private String subLadingQuantity;
  private String subLadingQuantityUOM;
  private String subGrossWeight;
  private String subGrossWeightUOM;
  private Date bolPrintDate;
  private String transferNumber;
  private String orgnCode;
  private String returnMessage;
  private String returnCode;
  
  public OrderLineBean()
  {
  }

  public void setSenderOrgId(String senderOrgId)
  {
    this.senderOrgId = senderOrgId;
  }

  public String getSenderOrgId()
  {
    return senderOrgId;
  }

  public void setSenderOrgName(String senderOrgName)
  {
    this.senderOrgName = senderOrgName;
  }

  public String getSenderOrgName()
  {
    return senderOrgName;
  }

  public void setReceiverOrgId(String receiverOrgId)
  {
    this.receiverOrgId = receiverOrgId;
  }

  public String getReceiverOrgId()
  {
    return receiverOrgId;
  }

  public void setReceiverOrgName(String receiverOrgName)
  {
    this.receiverOrgName = receiverOrgName;
  }

  public String getReceiverOrgName()
  {
    return receiverOrgName;
  }

  public void setTransDate(Date transDate)
  {
    this.transDate = transDate;
  }

  public Date getTransDate()
  {
    return transDate;
  }

  public void setTimeZone(String timeZone)
  {
    this.timeZone = timeZone;
  }

  public String getTimeZone()
  {
    return timeZone;
  }

  public void setActionCode(String actionCode)
  {
    this.actionCode = actionCode;
  }

  public String getActionCode()
  {
    return actionCode;
  }

  public void setShipmentId(String shipmentId)
  {
    this.shipmentId = shipmentId;
  }

  public String getShipmentId()
  {
    return shipmentId;
  }

  public void setLineHaulMode(String lineHaulMode)
  {
    this.lineHaulMode = lineHaulMode;
  }

  public String getLineHaulMode()
  {
    return lineHaulMode;
  }

  public void setMovementType(String movementType)
  {
    this.movementType = movementType;
  }

  public String getMovementType()
  {
    return movementType;
  }

  public void setPaymentMethod(String paymentMethod)
  {
    this.paymentMethod = paymentMethod;
  }

  public String getPaymentMethod()
  {
    if (StringUtils.equalsIgnoreCase(this.getReference8(), "WHSE XFER"))
    {
      return "PREPAID";
    }
    else
    {
      return this.paymentMethod;
    }
  }

  public void setReference1(String reference1)
  {
    this.reference1 = reference1;
  }

  public String getReference1()
  {
    return reference1;
  }

  public void setReference1Type(String reference1Type)
  {
    this.reference1Type = reference1Type;
  }

  public String getReference1Type()
  {
    return reference1Type;
  }

  public void setReference2(String reference2)
  {
    this.reference2 = reference2;
  }

  public String getReference2()
  {
    return reference2;
  }

  public void setReference2Type(String reference2Type)
  {
    this.reference2Type = reference2Type;
  }

  public String getReference2Type()
  {
    return reference2Type;
  }

  public void setReference3(String reference3)
  {
    this.reference3 = reference3;
  }

  public String getReference3()
  {
    return reference3;
  }

  public void setReference3Type(String reference3Type)
  {
    this.reference3Type = reference3Type;
  }

  public String getReference3Type()
  {
    return reference3Type;
  }

  public void setReference4(String reference4)
  {
    this.reference4 = reference4;
  }

  public String getReference4()
  {
    return reference4;
  }

  public void setReference4Type(String reference4Type)
  {
    this.reference4Type = reference4Type;
  }

  public String getReference4Type()
  {
    return reference4Type;
  }

  public void setReference5(String reference5)
  {
    this.reference5 = reference5;
  }

  public String getReference5()
  {
    return reference5;
  }

  public void setReference5Type(String reference5Type)
  {
    this.reference5Type = reference5Type;
  }

  public String getReference5Type()
  {
    return reference5Type;
  }

  public void setReference6(String reference6)
  {
    this.reference6 = reference6;
  }

  public String getReference6()
  {
    return reference6;
  }

  public void setReference6Type(String reference6Type)
  {
    this.reference6Type = reference6Type;
  }

  public String getReference6Type()
  {
    return reference6Type;
  }

  public void setReference7(String reference7)
  {
    this.reference7 = reference7;
  }

  public String getReference7()
  {
    return reference7;
  }

  public void setReference7Type(String reference7Type)
  {
    this.reference7Type = reference7Type;
  }

  public String getReference7Type()
  {
    return reference7Type;
  }

  public void setReference8(String reference8)
  {
    this.reference8 = reference8;
  }

  public String getReference8()
  {
    return reference8;
  }

  public void setReference8Type(String reference8Type)
  {
    this.reference8Type = reference8Type;
  }

  public String getReference8Type()
  {
    return reference8Type;
  }

  public void setReference9(String reference9)
  {
    this.reference9 = reference9;
  }

  public String getReference9()
  {
    return reference9;
  }

  public void setReference9Type(String reference9Type)
  {
    this.reference9Type = reference9Type;
  }

  public String getReference9Type()
  {
    return reference9Type;
  }

  public void setReference10(String reference10)
  {
    this.reference10 = reference10;
  }

  public String getReference10()
  {
    return reference10;
  }

  public void setReference10Type(String reference10Type)
  {
    this.reference10Type = reference10Type;
  }

  public String getReference10Type()
  {
    return reference10Type;
  }

  public void setReference11(String reference11)
  {
    this.reference11 = reference11;
  }

  public String getReference11()
  {
    return reference11;
  }

  public void setReference11Type(String reference11Type)
  {
    this.reference11Type = reference11Type;
  }

  public String getReference11Type()
  {
    return reference11Type;
  }

  public void setReference12(String reference12)
  {
    this.reference12 = reference12;
  }

  public String getReference12()
  {
    return reference12;
  }

  public void setReference12Type(String reference12Type)
  {
    this.reference12Type = reference12Type;
  }

  public String getReference12Type()
  {
    return reference12Type;
  }

  public void setReference13(String reference13)
  {
    this.reference13 = reference13;
  }

  public String getReference13()
  {
    return reference13;
  }

  public void setReference13Type(String reference13Type)
  {
    this.reference13Type = reference13Type;
  }

  public String getReference13Type()
  {
    return reference13Type;
  }

  public void setReference14(String reference14)
  {
    this.reference14 = reference14;
  }

  public String getReference14()
  {
    return reference14;
  }

  public void setReference14Type(String reference14Type)
  {
    this.reference14Type = reference14Type;
  }

  public String getReference14Type()
  {
    return reference14Type;
  }

  public void setReference15(String reference15)
  {
    this.reference15 = reference15;
  }

  public String getReference15()
  {
    return reference15;
  }

  public void setReference15Type(String reference15Type)
  {
    this.reference15Type = reference15Type;
  }

  public String getReference15Type()
  {
    return reference15Type;
  }

  public void setReference16(String reference16)
  {
    this.reference16 = reference16;
  }

  public String getReference16()
  {
    return reference16;
  }

  public void setReference16Type(String reference16Type)
  {
    this.reference16Type = reference16Type;
  }

  public String getReference16Type()
  {
    return reference16Type;
  }

  public void setReference17(String reference17)
  {
    this.reference17 = reference17;
  }

  public String getReference17()
  {
    return reference17;
  }

  public void setReference17Type(String reference17Type)
  {
    this.reference17Type = reference17Type;
  }

  public String getReference17Type()
  {
    return reference17Type;
  }

  public void setReference18(String reference18)
  {
    this.reference18 = reference18;
  }

  public String getReference18()
  {
    return reference18;
  }

  public void setReference18Type(String reference18Type)
  {
    this.reference18Type = reference18Type;
  }

  public String getReference18Type()
  {
    return reference18Type;
  }

  public void setReference19(String reference19)
  {
    this.reference19 = reference19;
  }

  public String getReference19()
  {
    return reference19;
  }

  public void setReference19Type(String reference19Type)
  {
    this.reference19Type = reference19Type;
  }

  public String getReference19Type()
  {
    return reference19Type;
  }

  public void setReference20(String reference20)
  {
    this.reference20 = reference20;
  }

  public String getReference20()
  {
    return reference20;
  }

  public void setReference20Type(String reference20Type)
  {
    this.reference20Type = reference20Type;
  }

  public String getReference20Type()
  {
    return reference20Type;
  }

  public void setAttribute1(String attribute1)
  {
    this.attribute1 = attribute1;
  }

  public String getAttribute1()
  {
    return attribute1;
  }

  public void setServiceType(String serviceType)
  {
    this.serviceType = serviceType;
  }

  public String getServiceType()
  {
    return serviceType;
  }

  public void setEquipmentDescription(String equipmentDescription)
  {
    this.equipmentDescription = equipmentDescription;
  }

  public String getEquipmentDescription()
  {
    return equipmentDescription;
  }

  public void setGrossWeightUOM(String grossWeightUOM)
  {
    this.grossWeightUOM = grossWeightUOM;
  }

  public String getGrossWeightUOM()
  {
    return grossWeightUOM;
  }

  public void setBol(String bol)
  {
    this.bol = bol;
  }

  public String getBol()
  {
    return bol;
  }

  public void setPoNumber(String poNumber)
  {
    this.poNumber = poNumber;
  }

  public String getPoNumber()
  {
    return poNumber;
  }

  public void setSalesOrderNumber(String salesOrderNumber)
  {
    this.salesOrderNumber = salesOrderNumber;
  }

  public String getSalesOrderNumber()
  {
    return salesOrderNumber;
  }

  public void setLadingQuantity(String ladingQuantity)
  {
    this.ladingQuantity = ladingQuantity;
  }

  public String getLadingQuantity()
  {
    return ladingQuantity;
  }

  public void setGrossWeight(String grossWeight)
  {
    this.grossWeight = grossWeight;
  }

  public String getGrossWeight()
  {
    return grossWeight;
  }

  public void setVolume(String volume)
  {
    this.volume = volume;
  }

  public String getVolume()
  {
    return volume;
  }

  public void setOrigStopNumber(String origStopNumber)
  {
    this.origStopNumber = origStopNumber;
  }

  public String getOrigStopNumber()
  {
    return origStopNumber;
  }

  public void setOrigStopReason(String origStopReason)
  {
    this.origStopReason = origStopReason;
  }

  public String getOrigStopReason()
  {
    return origStopReason;
  }

  public void setOrigLocationName(String origLocationName)
  {
    this.origLocationName = origLocationName;
  }

  public String getOrigLocationName()
  {
    return origLocationName;
  }

  public void setOrigLocationId(String origLocationId)
  {
    this.origLocationId = origLocationId;
  }

  public String getOrigLocationId()
  {
    return origLocationId;
  }

  public void setOrigLocationType(String origLocationType)
  {
    this.origLocationType = origLocationType;
  }

  public String getOrigLocationType()
  {
    return origLocationType;
  }

  public void setOrigAddress1(String origAddress1)
  {
    this.origAddress1 = origAddress1;
  }

  public String getOrigAddress1()
  {
    return origAddress1;
  }

  public void setOrigCity(String origCity)
  {
    this.origCity = origCity;
  }

  public String getOrigCity()
  {
    return origCity;
  }

  public void setOrigStateProvince(String origStateProvince)
  {
    this.origStateProvince = origStateProvince;
  }

  public String getOrigStateProvince()
  {
    return origStateProvince;
  }

  public void setOrigPostalCode(String origPostalCode)
  {
    this.origPostalCode = origPostalCode;
  }

  public String getOrigPostalCode()
  {
    return origPostalCode;
  }

  public void setOrigCountry(String origCountry)
  {
    this.origCountry = origCountry;
  }

  public String getOrigCountry()
  {
    return origCountry;
  }

  public void setOrigPlannedFromDate(Date origPlannedFromDate)
  {
    this.origPlannedFromDate = origPlannedFromDate;
  }

  public Date getOrigPlannedFromDate()
  {
    return origPlannedFromDate;
  }

  public void setOrigPlannedToDate(Date origPlannedToDate)
  {
    this.origPlannedToDate = origPlannedToDate;
  }

  public Date getOrigPlannedToDate()
  {
    return origPlannedToDate;
  }

  public void setDestStopNumber(String destStopNumber)
  {
    this.destStopNumber = destStopNumber;
  }

  public String getDestStopNumber()
  {
    return destStopNumber;
  }

  public void setDestStopReason(String destStopReason)
  {
    this.destStopReason = destStopReason;
  }

  public String getDestStopReason()
  {
    return destStopReason;
  }

  public void setDestLocationName(String destLocationName)
  {
    this.destLocationName = destLocationName;
  }

  public String getDestLocationName()
  {
    return destLocationName;
  }

  public void setDestLocationId(String destLocationId)
  {
    this.destLocationId = destLocationId;
  }

  public String getDestLocationId()
  {
    return destLocationId;
  }

  public void setDestLocationType(String destLocationType)
  {
    this.destLocationType = destLocationType;
  }

  public String getDestLocationType()
  {
    return destLocationType;
  }

  public void setDestAddress1(String destAddress1)
  {
    this.destAddress1 = destAddress1;
  }

  public String getDestAddress1()
  {
    return destAddress1;
  }

  public void setDestCity(String destCity)
  {
    this.destCity = destCity;
  }

  public String getDestCity()
  {
    return destCity;
  }

  public void setDestStateProvince(String destStateProvince)
  {
    this.destStateProvince = destStateProvince;
  }

  public String getDestStateProvince()
  {
    return destStateProvince;
  }

  public void setDestPostalCode(String destPostalCode)
  {
    this.destPostalCode = destPostalCode;
  }

  public String getDestPostalCode()
  {
    return destPostalCode;
  }

  public void setDestCountry(String destCountry)
  {
    this.destCountry = destCountry;
  }

  public String getDestCountry()
  {
    return destCountry;
  }

  public void setPlannedDate(Date plannedDate)
  {
    this.plannedDate = plannedDate;
  }

  public Date getPlannedDate()
  {
    return plannedDate;
  }

  public void setDestNote(String destNote)
  {
    this.destNote = destNote;
  }

  public String getDestNote()
  {
    return destNote;
  }

  public void setProductId(String productId)
  {
    this.productId = productId;
  }

  public String getProductId()
  {
    return productId;
  }

  public void setProductDescription(String productDescription)
  {
    this.productDescription = productDescription;
  }

  public String getProductDescription()
  {
    return productDescription;
  }

  public void setSubLadingQuantity(String subLadingQuantity)
  {
    this.subLadingQuantity = subLadingQuantity;
  }

  public String getSubLadingQuantity()
  {
    return subLadingQuantity;
  }

  public void setSubGrossWeight(String subGrossWeight)
  {
    this.subGrossWeight = subGrossWeight;
  }

  public String getSubGrossWeight()
  {
    return subGrossWeight;
  }

  public void setAttribute2(String attribute2)
  {
    this.attribute2 = attribute2;
  }

  public String getAttribute2()
  {
    return attribute2;
  }

  public void setAttribute3(String attribute3)
  {
    this.attribute3 = attribute3;
  }

  public String getAttribute3()
  {
    return attribute3;
  }

  public void setAttribute4(String attribute4)
  {
    this.attribute4 = attribute4;
  }

  public String getAttribute4()
  {
    return attribute4;
  }

  public void setAttribute5(String attribute5)
  {
    this.attribute5 = attribute5;
  }

  public String getAttribute5()
  {
    return attribute5;
  }

  public void setAttribute6(String attribute6)
  {
    this.attribute6 = attribute6;
  }

  public String getAttribute6()
  {
    return attribute6;
  }

  public void setAttribute7(String attribute7)
  {
    this.attribute7 = attribute7;
  }

  public String getAttribute7()
  {
    return attribute7;
  }

  public void setAttribute8(String attribute8)
  {
    this.attribute8 = attribute8;
  }

  public String getAttribute8()
  {
    return attribute8;
  }

  public void setAttribute9(String attribute9)
  {
    this.attribute9 = attribute9;
  }

  public String getAttribute9()
  {
    return attribute9;
  }

  public void setAttribute10(String attribute10)
  {
    this.attribute10 = attribute10;
  }

  public String getAttribute10()
  {
    return attribute10;
  }

  public void setAttribute11(String attribute11)
  {
    this.attribute11 = attribute11;
  }

  public String getAttribute11()
  {
    return attribute11;
  }

  public void setAttribute12(String attribute12)
  {
    this.attribute12 = attribute12;
  }

  public String getAttribute12()
  {
    return attribute12;
  }

  public void setAttribute13(String attribute13)
  {
    this.attribute13 = attribute13;
  }

  public String getAttribute13()
  {
    return attribute13;
  }

  public void setAttribute14(String attribute14)
  {
    this.attribute14 = attribute14;
  }

  public String getAttribute14()
  {
    return attribute14;
  }

  public void setAttribute15(String attribute15)
  {
    this.attribute15 = attribute15;
  }

  public String getAttribute15()
  {
    return attribute15;
  }

  public void setAttribute16(String attribute16)
  {
    this.attribute16 = attribute16;
  }

  public String getAttribute16()
  {
    return attribute16;
  }

  public void setAttribute17(String attribute17)
  {
    this.attribute17 = attribute17;
  }

  public String getAttribute17()
  {
    return attribute17;
  }

  public void setAttribute18(String attribute18)
  {
    this.attribute18 = attribute18;
  }

  public String getAttribute18()
  {
    return attribute18;
  }

  public void setAttribute19(String attribute19)
  {
    this.attribute19 = attribute19;
  }

  public String getAttribute19()
  {
    return attribute19;
  }

  public void setAttribute20(String attribute20)
  {
    this.attribute20 = attribute20;
  }

  public String getAttribute20()
  {
    return attribute20;
  }

  public void setTransId(String transId)
  {
    this.transId = transId;
  }

  public String getTransId()
  {
    return transId;
  }

  public void setTransType(String transType)
  {
    this.transType = transType;
  }

  public String getTransType()
  {
    return transType;
  }

  public void setSubLadingQuantityUOM(String subLadingQuantityUOM)
  {
    this.subLadingQuantityUOM = subLadingQuantityUOM;
  }

  public String getSubLadingQuantityUOM()
  {
    return subLadingQuantityUOM;
  }

  public void setSubGrossWeightUOM(String subGrossWeightUOM)
  {
    this.subGrossWeightUOM = subGrossWeightUOM;
  }

  public String getSubGrossWeightUOM()
  {
    return subGrossWeightUOM;
  }

  public void setLadingQuantityUOM(String ladingQuantityUOM)
  {
    this.ladingQuantityUOM = ladingQuantityUOM;
  }

  public String getLadingQuantityUOM()
  {
    return ladingQuantityUOM;
  }

  public void setVolumeUOM(String volumeUOM)
  {
    this.volumeUOM = volumeUOM;
  }

  public String getVolumeUOM()
  {
    return volumeUOM;
  }

  public void setProductGrossWeightUom(String productGrossWeightUom)
  {
    this.productGrossWeightUom = productGrossWeightUom;
  }

  public String getProductGrossWeightUom()
  {
    return productGrossWeightUom;
  }

  public void setProductGrossWeight(String productGrossWeight)
  {
    this.productGrossWeight = productGrossWeight;
  }

  public String getProductGrossWeight()
  {
    return productGrossWeight;
  }

  public void setProductLadingQtyUOM(String productLadingQtyUOM)
  {
    this.productLadingQtyUOM = productLadingQtyUOM;
  }

  public String getProductLadingQtyUOM()
  {
    return productLadingQtyUOM;
  }

  public void setProductLadingQty(String productLadingQty)
  {
    this.productLadingQty = productLadingQty;
  }

  public String getProductLadingQty()
  {
    return productLadingQty;
  }

  public void setDocId(String docId)
  {
    this.docId = docId;
  }

  public String getDocId()
  {
    return docId;
  }

  public void setShipToFromLocationId(String shipToFromLocationId)
  {
    this.shipToFromLocationId = shipToFromLocationId;
  }

  public String getShipToFromLocationId()
  {
    return shipToFromLocationId;
  }

  public void setOriginOrgId(String originOrgId)
  {
    this.originOrgId = originOrgId;
  }

  public String getOriginOrgId()
  {
    return originOrgId;
  }

  public void setOriginOrgName(String originOrgName)
  {
    this.originOrgName = originOrgName;
  }

  public String getOriginOrgName()
  {
    return originOrgName;
  }

  public void setDestOrgId(String destOrgId)
  {
    this.destOrgId = destOrgId;
  }

  public String getDestOrgId()
  {
    return destOrgId;
  }

  public void setDestOrgName(String destOrgName)
  {
    this.destOrgName = destOrgName;
  }

  public String getDestOrgName()
  {
    return destOrgName;
  }

  public void setDestAddress2(String destAddress2)
  {
    this.destAddress2 = destAddress2;
  }

  public String getDestAddress2()
  {
    return destAddress2;
  }

  public void setDestAddress3(String destAddress3)
  {
    this.destAddress3 = destAddress3;
  }

  public String getDestAddress3()
  {
    return destAddress3;
  }

  public void setDestAddress4(String destAddress4)
  {
    this.destAddress4 = destAddress4;
  }

  public String getDestAddress4()
  {
    return destAddress4;
  }

  public void setOrigAddress2(String origAddress2)
  {
    this.origAddress2 = origAddress2;
  }

  public String getOrigAddress2()
  {
    return origAddress2;
  }

  public void setOrigAddress3(String origAddress3)
  {
    this.origAddress3 = origAddress3;
  }

  public String getOrigAddress3()
  {
    return origAddress3;
  }

  public void setOrigAddress4(String origAddress4)
  {
    this.origAddress4 = origAddress4;
  }

  public String getOrigAddress4()
  {
    return origAddress4;
  }

  public void setReturnCode(String returnCode)
  {
    this.returnCode = returnCode;
  }

  public String getReturnCode()
  {
    return returnCode;
  }

  public void setReturnMessage(String returnMessage)
  {
    this.returnMessage = returnMessage;
  }

  public String getReturnMessage()
  {
    return returnMessage;
  }
  
  public void setBolPrintDate(Date bolPrintDate)
  {
    this.bolPrintDate = bolPrintDate;
  }

  public Date getBolPrintDate()
  {
    return bolPrintDate;
  }

  public void setTransferNumber(String transferNumber)
  {
    this.transferNumber = transferNumber;
  }

  public String getTransferNumber()
  {
    return transferNumber;
  }

  public void setOrgnCode(String orgnCode)
  {
    this.orgnCode = orgnCode;
  }

  public String getOrgnCode()
  {
    return orgnCode;
  }

  public boolean isDelete()
  {
    if (StringUtils.equalsIgnoreCase(this.getActionCode(), "DELETE"))
    {
      return true;
    }
    else
    {
      return false;
    }
  }
}
