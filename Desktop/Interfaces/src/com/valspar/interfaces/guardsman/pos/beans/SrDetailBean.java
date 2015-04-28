package com.valspar.interfaces.guardsman.pos.beans;

public class SrDetailBean
{
  String deliveryDt;
  String extendedPrice;
  String itemColorStyle;
  String itemDescription;
  String itemId;
  String itemSaAmt;
  String manufName;
  String pricingCode;
  String skuNo;
  String qty;
  String saType;
  String saTypeId;
  String unitAmt;
  String samSrItemId;
  String samConSaId;
  boolean updateAddition;
  int originalQty;
  int claimCnt;
  String planItemId;

  public SrDetailBean()
  {
  }

  public String getDeliveryDt()
  {
    return deliveryDt;
  }

  public void setDeliveryDt(String deliveryDt)
  {
    this.deliveryDt = deliveryDt;
  }

  public String getExtendedPrice()
  {
    return extendedPrice;
  }

  public void setExtendedPrice(String extendedPrice)
  {
    this.extendedPrice = extendedPrice;
  }

  public String getItemColorStyle()
  {
    return itemColorStyle;
  }

  public void setItemColorStyle(String itemColorStyle)
  {
    this.itemColorStyle = itemColorStyle;
  }

  public String getItemDescription()
  {
    return itemDescription;
  }

  public void setItemDescription(String itemDescription)
  {
    this.itemDescription = itemDescription;
  }

  public String getItemId()
  {
    return itemId;
  }

  public void setItemId(String itemId)
  {
    this.itemId = itemId;
  }

  public String getItemSaAmt()
  {
    return itemSaAmt;
  }

  public void setItemSaAmt(String itemSaAmt)
  {
    this.itemSaAmt = itemSaAmt;
  }

  public String getPlanItemId()
  {
    return planItemId;
  }

  public void setPlanItemId(String planItemId)
  {
    this.planItemId = planItemId;
  }

  public String getManufName()
  {
    return manufName;
  }

  public void setManufName(String manufName)
  {
    this.manufName = manufName;
  }

  public String getPricingCode()
  {
    return pricingCode;
  }

  public void setPricingCode(String pricingCode)
  {
    this.pricingCode = pricingCode;
  }

  public String getSkuNo()
  {
    return skuNo;
  }

  public void setSkuNo(String skuNo)
  {
    this.skuNo = skuNo;
  }

  public String getQty()
  {
    return qty;
  }

  public void setQty(String qty)
  {
    this.qty = qty;
  }

  public String getSaType()
  {
    return saType;
  }

  public void setSaType(String saType)
  {
    this.saType = saType;
  }

  public String getUnitAmt()
  {
    return unitAmt;
  }

  public void setUnitAmt(String unitAmt)
  {
    this.unitAmt = unitAmt;
  }

  public String getSamSrItemId()
  {
    return samSrItemId;
  }

  public void setSamSrItemId(String samSrItemId)
  {
    this.samSrItemId = samSrItemId;
  }

  public String getSamConSaId()
  {
    return samConSaId;
  }

  public void setSamConSaId(String samConSaId)
  {
    this.samConSaId = samConSaId;
  }

  public boolean isUpdateAddition()
  {
    return updateAddition;
  }

  public void setUpdateAddition(boolean updateAddition)
  {
    this.updateAddition = updateAddition;
  }

  public int getOriginalQty()
  {
    return originalQty;
  }

  public void setOriginalQty(int originalQty)
  {
    this.originalQty = originalQty;
  }

  public void setSaTypeId(String saTypeId)
  {
    this.saTypeId = saTypeId;
  }

  public String getSaTypeId()
  {
    return saTypeId;
  }

  public void setClaimCnt(int claimCnt)
  {
    this.claimCnt = claimCnt;
  }

  public int getClaimCnt()
  {
    return claimCnt;
  }
}
