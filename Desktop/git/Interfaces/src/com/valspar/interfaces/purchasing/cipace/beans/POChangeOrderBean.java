package com.valspar.interfaces.purchasing.cipace.beans;

import java.math.BigDecimal;
import java.util.*;

public class POChangeOrderBean extends PoHeadersInterfaceBean
{
  private String poChangeOrderAutoId;
  private String authorizedBy;
  private Date changedDate;
  private String statusId;
  private BigDecimal changedQuantity;
  private BigDecimal changedUnitPrice;
  private Date createdOn;
  private String description;
  private BigDecimal latestAmount;
  private BigDecimal latestQuantity;
  private BigDecimal latestUnitPrice;
  private String purchaseOrderLineItemAutoId;
  private BigDecimal variance;

  private int poLineNumber;

  private PurchaseOrderBean purchaseOrder;

  private List<PoLinesInterfaceBean> lines;

  public void compositeFieldsFromPurchaseOrder()
  {
    setProjectId(purchaseOrder.getProjectId());
    setProjectName(purchaseOrder.getProjectName());
    setCipAcePoAutoId(purchaseOrder.getCipAcePoAutoId());
    setCipAcePoNumber(purchaseOrder.getCipAcePoNumber());
    setRevisionNumber(purchaseOrder.getRevisionNumber());
    setCipAceDatabaseName(purchaseOrder.getCipAceDatabaseName());
    setShipToOrganizationCode(purchaseOrder.getShipToOrganizationCode());
    setShipToOrganizationName(purchaseOrder.getShipToOrganizationName());
    setCurrencyCode(purchaseOrder.getCurrencyCode());
    setOrgId(purchaseOrder.getOrgId());
    setVendorId(purchaseOrder.getVendorId());
    setVendorSiteId(purchaseOrder.getVendorSiteId());
    setGlCompanyCode(purchaseOrder.getGlCompanyCode());
    setGlProfitCenterCode(purchaseOrder.getGlProfitCenterCode());
    setGlLocationCode(purchaseOrder.getGlLocationCode());
    setGlCostCenterCode(purchaseOrder.getGlCostCenterCode());
    setGlAccountNumber(purchaseOrder.getGlAccountNumber());
    setGlCategoryCode(purchaseOrder.getGlCategoryCode());
    setGlDepartmentCode(purchaseOrder.getGlDepartmentCode());
    setOraclePoNumber(purchaseOrder.getOraclePoNumber());
    setUserName(purchaseOrder.getUserName());

    lines = new ArrayList<PoLinesInterfaceBean>();

    PurchaseOrderLineBean baseLine = purchaseOrder.getLines().get(0);

    PoLinesInterfaceBean line = new PoLinesInterfaceBean();

    //line.setCipAcePoLineAutoId(baseLine.getCipAcePoLineAutoId());
    line.setLineNumber(poLineNumber);
    line.setUnitPrice(latestUnitPrice);
    line.setQuantity(latestQuantity);
    line.setPromisedDate(baseLine.getPromisedDate());
    line.setItemCategory(baseLine.getItemCategory());
    line.setUom(baseLine.getUom());
    line.setItemDescription(baseLine.getItemDescription());

    lines.add(line);
  }

  public List<PoLinesInterfaceBean> getLines()
  {
    return lines;
  }

  public boolean isCancel()
  {
    return BigDecimal.ZERO.equals(changedQuantity) && BigDecimal.ZERO.equals(changedUnitPrice);
  }

  public void setPoChangeOrderAutoId(String poChangeOrderAutoId)
  {
    this.poChangeOrderAutoId = poChangeOrderAutoId;
  }

  public String getPoChangeOrderAutoId()
  {
    return poChangeOrderAutoId;
  }

  public void setAuthorizedBy(String authorizedBy)
  {
    this.authorizedBy = authorizedBy;
  }

  public String getAuthorizedBy()
  {
    return authorizedBy;
  }

  public void setChangedDate(Date changedDate)
  {
    this.changedDate = changedDate;
  }

  public Date getChangedDate()
  {
    return changedDate;
  }

  public void setStatusId(String statusId)
  {
    this.statusId = statusId;
  }

  public String getStatusId()
  {
    return statusId;
  }

  public void setChangedQuantity(BigDecimal changedQuantity)
  {
    this.changedQuantity = changedQuantity;
  }

  public BigDecimal getChangedQuantity()
  {
    return changedQuantity;
  }

  public void setChangedUnitPrice(BigDecimal changedUnitPrice)
  {
    this.changedUnitPrice = changedUnitPrice;
  }

  public BigDecimal getChangedUnitPrice()
  {
    return changedUnitPrice;
  }

  public void setCreatedOn(Date createdOn)
  {
    this.createdOn = createdOn;
  }

  public Date getCreatedOn()
  {
    return createdOn;
  }

  public void setDescription(String description)
  {
    this.description = description;
  }

  public String getDescription()
  {
    return description;
  }

  public void setLatestAmount(BigDecimal latestAmount)
  {
    this.latestAmount = latestAmount;
  }

  public BigDecimal getLatestAmount()
  {
    return latestAmount;
  }

  public void setLatestQuantity(BigDecimal latestQuantity)
  {
    this.latestQuantity = latestQuantity;
  }

  public BigDecimal getLatestQuantity()
  {
    return latestQuantity;
  }

  public void setLatestUnitPrice(BigDecimal latestUnitPrice)
  {
    this.latestUnitPrice = latestUnitPrice;
  }

  public BigDecimal getLatestUnitPrice()
  {
    return latestUnitPrice;
  }

  public void setPurchaseOrderLineItemAutoId(String purchaseOrderLineItemAutoId)
  {
    this.purchaseOrderLineItemAutoId = purchaseOrderLineItemAutoId;
  }

  public String getPurchaseOrderLineItemAutoId()
  {
    return purchaseOrderLineItemAutoId;
  }

  public void setVariance(BigDecimal variance)
  {
    this.variance = variance;
  }

  public BigDecimal getVariance()
  {
    return variance;
  }

  public void setPoLineNumber(int poLineNumber)
  {
    this.poLineNumber = poLineNumber;
  }

  public int getPoLineNumber()
  {
    return poLineNumber;
  }

  public void setPurchaseOrder(PurchaseOrderBean purchaseOrder)
  {
    this.purchaseOrder = purchaseOrder;
  }

  public PurchaseOrderBean getPurchaseOrder()
  {
    return purchaseOrder;
  }
}
