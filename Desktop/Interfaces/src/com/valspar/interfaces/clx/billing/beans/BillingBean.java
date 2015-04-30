package com.valspar.interfaces.clx.billing.beans;

import java.math.BigDecimal;
import java.util.Date;
import javax.persistence.*;

@Entity
@Table(name = "VALSPAR.CLX_FREIGHT_INTERFACE")
public class BillingBean
{
  private Long docId;
  private String direction;   
  private String tmsShipmentId;
  private BigDecimal freightAmount;
  private String currency;
  private String shipFromWhse;
  private String shipToWhse;
  private String custVendNumber;
  private String custVendSiteNumber;
  private String custVendName;
  private String shipmentNumber;
  private String actionCode = "ORIGINAL";
  private String origName;
  private String origCity;
  private String origState;
  private String origZip;
  private String destName;
  private String destCity;
  private String destState;
  private String destZip;
  private String glAccount;
  private String glSource;
  private String documentNumber;
  private String scac;
  private String proNumber;
  private String masterTrip;
  private Date dateShipped;
  private Date dateDelivered;
  private String serviceType;
  private Date transactionDate;
  private String taxReference1;
  private String taxReference1Type;
  private String taxReference2;
  private String taxReference2Type;
  private String taxReference3;
  private String taxReference3Type;
  private String taxReference4;
  private String taxReference4Type;
  private Date creationDate;
  private Date lastUpdateDate;
  private BigDecimal createdBy;
  private BigDecimal lastUpdatedBy;
  private String fileName;
  private Date glProcessingDate;
  private Date vendavoProcessingDate;
  private String status = "N";
  private String errorMessage;
  
  public BillingBean()
  {
  }

  public void setDocId(Long docId)
  {
    this.docId = docId;
  }

  @Id
  @Column(name = "DOC_ID")
  @GeneratedValue(generator = "docIdSeq", strategy = GenerationType.SEQUENCE)
  @SequenceGenerator(name = "docIdSeq", sequenceName = "VALSPAR.CLX_FREIGHT_PAY_SEQ", allocationSize = 1)
  public Long getDocId()
  {
    return docId;
  }
  
  public void setDirection(String direction)
  {
    this.direction = direction;
  }

  @Column(name = "DIRECTION")
  public String getDirection()
  {
    return direction;
  }

  public void setTmsShipmentId(String tmsShipmentId)
  {
    this.tmsShipmentId = tmsShipmentId;
  }

  @Column(name = "TMS_SHIPMENT_ID")
  public String getTmsShipmentId()
  {
    return tmsShipmentId;
  }

  public void setFreightAmount(BigDecimal freightAmount)
  {
    this.freightAmount = freightAmount;
  }

  @Column(name = "FREIGHT_AMOUNT")
  public BigDecimal getFreightAmount()
  {
    return freightAmount;
  }

  public void setCurrency(String currency)
  {
    this.currency = currency;
  }

  @Column(name = "CURRENCY")
  public String getCurrency()
  {
    return currency;
  }

  public void setShipFromWhse(String shipFromWhse)
  {
    this.shipFromWhse = shipFromWhse;
  }

  @Column(name = "SHIP_FROM_WHSE")
  public String getShipFromWhse()
  {
    return shipFromWhse;
  }

  public void setShipToWhse(String shipToWhse)
  {
    this.shipToWhse = shipToWhse;
  }

  @Column(name = "SHIP_TO_WHSE")
  public String getShipToWhse()
  {
    return shipToWhse;
  }

  public void setCustVendNumber(String custVendNumber)
  {
    this.custVendNumber = custVendNumber;
  }

  @Column(name = "CUST_VEND_NUMBER")
  public String getCustVendNumber()
  {
    return custVendNumber;
  }

  public void setCustVendSiteNumber(String custVendSiteNumber)
  {
    this.custVendSiteNumber = custVendSiteNumber;
  }

  @Column(name = "CUST_VEND_SITE_NUMBER")
  public String getCustVendSiteNumber()
  {
    return custVendSiteNumber;
  }

  public void setCustVendName(String custVendName)
  {
    this.custVendName = custVendName;
  }

  @Column(name = "CUST_VEND_NAME")
  public String getCustVendName()
  {
    return custVendName;
  }

  public void setShipmentNumber(String shipmentNumber)
  {
    this.shipmentNumber = shipmentNumber;
  }

  @Column(name = "SHIPMENT_NUMBER")
  public String getShipmentNumber()
  {
    return shipmentNumber;
  }

  public void setActionCode(String actionCode)
  {
    this.actionCode = actionCode;
  }

  @Column(name = "ACTION_CODE")
  public String getActionCode()
  {
    return actionCode;
  }

  public void setOrigName(String origName)
  {
    this.origName = origName;
  }

  @Column(name = "ORIG_NAME")
  public String getOrigName()
  {
    return origName;
  }

  public void setOrigCity(String origCity)
  {
    this.origCity = origCity;
  }

  @Column(name = "ORIG_CITY")
  public String getOrigCity()
  {
    return origCity;
  }

  public void setOrigState(String origState)
  {
    this.origState = origState;
  }

  @Column(name = "ORIG_STATE")
  public String getOrigState()
  {
    return origState;
  }

  public void setOrigZip(String origZip)
  {
    this.origZip = origZip;
  }

  @Column(name = "ORIG_ZIP")
  public String getOrigZip()
  {
    return origZip;
  }

  public void setDestName(String destName)
  {
    this.destName = destName;
  }

  @Column(name = "DEST_NAME")
  public String getDestName()
  {
    return destName;
  }

  public void setDestCity(String destCity)
  {
    this.destCity = destCity;
  }

  @Column(name = "DEST_CITY")
  public String getDestCity()
  {
    return destCity;
  }

  public void setDestState(String destState)
  {
    this.destState = destState;
  }

  @Column(name = "DEST_STATE")
  public String getDestState()
  {
    return destState;
  }

  public void setDestZip(String destZip)
  {
    this.destZip = destZip;
  }

  @Column(name = "DEST_ZIP")
  public String getDestZip()
  {
    return destZip;
  }

  public void setGlAccount(String glAccount)
  {
    this.glAccount = glAccount;
  }

  @Column(name = "GL_ACCOUNT")
  public String getGlAccount()
  {
    return glAccount;
  }

  public void setGlSource(String glSource)
  {
    this.glSource = glSource;
  }

  @Column(name = "GL_SOURCE")
  public String getGlSource()
  {
    return glSource;
  }

  public void setDocumentNumber(String documentNumber)
  {
    this.documentNumber = documentNumber;
  }

  @Column(name = "DOCUMENT_NUMBER")
  public String getDocumentNumber()
  {
    return documentNumber;
  }

  public void setScac(String scac)
  {
    this.scac = scac;
  }

  @Column(name = "SCAC")
  public String getScac()
  {
    return scac;
  }

  public void setProNumber(String proNumber)
  {
    this.proNumber = proNumber;
  }

  @Column(name = "PRO_NUMBER")
  public String getProNumber()
  {
    return proNumber;
  }

  public void setMasterTrip(String masterTrip)
  {
    this.masterTrip = masterTrip;
  }

  @Column(name = "MASTER_TRIP")
  public String getMasterTrip()
  {
    return masterTrip;
  }

  public void setDateShipped(Date dateShipped)
  {
    this.dateShipped = dateShipped;
  }

  @Column(name = "DATE_SHIPPED")
  public Date getDateShipped()
  {
    return dateShipped;
  }

  public void setDateDelivered(Date dateDelivered)
  {
    this.dateDelivered = dateDelivered;
  }

  @Column(name = "DATE_DELIVERED")
  public Date getDateDelivered()
  {
    return dateDelivered;
  }

  public void setServiceType(String serviceType)
  {
    this.serviceType = serviceType;
  }

  @Column(name = "SERVICE_TYPE")
  public String getServiceType()
  {
    return serviceType;
  }

  public void setTransactionDate(Date transactionDate)
  {
    this.transactionDate = transactionDate;
  }

  @Column(name = "TRANSACTION_DATE ")
  public Date getTransactionDate()
  {
    return transactionDate;
  }

  public void setCreationDate(Date creationDate)
  {
    this.creationDate = creationDate;
  }

  @Column(name = "CREATION_DATE")
  public Date getCreationDate()
  {
    return creationDate;
  }

  public void setLastUpdateDate(Date lastUpdateDate)
  {
    this.lastUpdateDate = lastUpdateDate;
  }

  @Column(name = "LAST_UPDATE_DATE")
  public Date getLastUpdateDate()
  {
    return lastUpdateDate;
  }

  public void setCreatedBy(BigDecimal createdBy)
  {
    this.createdBy = createdBy;
  }

  @Column(name = "CREATED_BY")
  public BigDecimal getCreatedBy()
  {
    return createdBy;
  }

  public void setLastUpdatedBy(BigDecimal lastUpdatedBy)
  {
    this.lastUpdatedBy = lastUpdatedBy;
  }

  @Column(name = "LAST_UPDATED_BY")
  public BigDecimal getLastUpdatedBy()
  {
    return lastUpdatedBy;
  }

  public void setFileName(String fileName)
  {
    this.fileName = fileName;
  }

  @Column(name = "FILE_NAME")
  public String getFileName()
  {
    return fileName;
  }

  public void setGlProcessingDate(Date glProcessingDate)
  {
    this.glProcessingDate = glProcessingDate;
  }

  @Column(name = "GL_PROCESSING_DATE")
  public Date getGlProcessingDate()
  {
    return glProcessingDate;
  }

  public void setVendavoProcessingDate(Date vendavoProcessingDate)
  {
    this.vendavoProcessingDate = vendavoProcessingDate;
  }

  @Column(name = "VENDAVO_PROCESSING_DATE")
  public Date getVendavoProcessingDate()
  {
    return vendavoProcessingDate;
  }

  public void setStatus(String status)
  {
    this.status = status;
  }

  @Column(name = "STATUS")
  public String getStatus()
  {
    return status;
  }

  public void setErrorMessage(String errorMessage)
  {
    this.errorMessage = errorMessage;
  }

  @Column(name = "ERROR_MESSAGE")
  public String getErrorMessage()
  {
    return errorMessage;
  }

  public void setTaxReference1(String taxReference1)
  {
    this.taxReference1 = taxReference1;
  }
  @Column(name = "TAX_REFERENCE1")
  public String getTaxReference1()
  {
    return taxReference1;
  }

  public void setTaxReference1Type(String taxReference1Type)
  {
    this.taxReference1Type = taxReference1Type;
  }
  @Column(name = "TAX_REFERENCE1_TYPE")
  public String getTaxReference1Type()
  {
    return taxReference1Type;
  }

  public void setTaxReference2(String taxReference2)
  {
    this.taxReference2 = taxReference2;
  }
  @Column(name = "TAX_REFERENCE2")
  public String getTaxReference2()
  {
    return taxReference2;
  }

  public void setTaxReference2Type(String taxReference2Type)
  {
    this.taxReference2Type = taxReference2Type;
  }
  @Column(name = "TAX_REFERENCE2_TYPE")
  public String getTaxReference2Type()
  {
    return taxReference2Type;
  }

  public void setTaxReference3(String taxReference3)
  {
    this.taxReference3 = taxReference3;
  }
  @Column(name = "TAX_REFERENCE3")
  public String getTaxReference3()
  {
    return taxReference3;
  }

  public void setTaxReference3Type(String taxReference3Type)
  {
    this.taxReference3Type = taxReference3Type;
  }
  @Column(name = "TAX_REFERENCE3_TYPE")
  public String getTaxReference3Type()
  {
    return taxReference3Type;
  }

  public void setTaxReference4(String taxReference4)
  {
    this.taxReference4 = taxReference4;
  }
  @Column(name = "TAX_REFERENCE4")
  public String getTaxReference4()
  {
    return taxReference4;
  }

  public void setTaxReference4Type(String taxReference4Type)
  {
    this.taxReference4Type = taxReference4Type;
  }
  @Column(name = "TAX_REFERENCE4_TYPE")
  public String getTaxReference4Type()
  {
    return taxReference4Type;
  }
}
