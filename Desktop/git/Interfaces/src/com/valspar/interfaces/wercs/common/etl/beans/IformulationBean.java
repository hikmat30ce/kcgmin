package com.valspar.interfaces.wercs.common.etl.beans;

import java.math.BigDecimal;
import java.util.Date;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.SequenceGenerator;
import javax.persistence.Table;
@Entity
@Table(name = "WERCS.I_FORMULATIONS")

public class IformulationBean
{
   private Long recordKey;
   private String jobId;
   private String product;
   private String inputProduct;
   private String casNumber;
   private String componentId;
   private String tradeSecretName;
   private String tradeSecretFlag;
   private BigDecimal percentage;
   private String percentRange;
   private String productUom;
   private String lineUom;
   private BigDecimal lineQuantity;
   private BigDecimal productQuantity;
   private BigDecimal hazardFlag;
   private String model;
   private String modelDesc;
   private Date order;
   private String direction;
   private Long status;
   private String remarks;
   private String userUpdated;
   private Date dateStamp;
   private String userInserted;
   private Date dateStampInserted;
   
  public IformulationBean()
  {
  }

  public void setRecordKey(Long recordKey)
  {
    this.recordKey = recordKey;
  }

  @Id
  @Column(name="F_RECORD_KEY")
  @GeneratedValue(generator = "FormulationIdSeq", 
                      strategy = GenerationType.SEQUENCE)
  @SequenceGenerator(name = "FormulationIdSeq", 
                         sequenceName = "WERCS.I_FORMULATIONS_SEQ", 
                         allocationSize = 1)
  public Long getRecordKey()
  {
    return recordKey;
  }

  public void setJobId(String jobId)
  {
    this.jobId = jobId;
  }

  @Column(name="F_JOB_ID")
  public String getJobId()
  {
    return jobId;
  }

  public void setProduct(String product)
  {
    this.product = product;
  }

  @Column(name="F_PRODUCT")
  public String getProduct()
  {
    return product;
  }

  public void setInputProduct(String inputProduct)
  {
    this.inputProduct = inputProduct;
  }

  @Column(name="F_INPUT_PRODUCT")
  public String getInputProduct()
  {
    return inputProduct;
  }

  public void setCasNumber(String casNumber)
  {
    this.casNumber = casNumber;
  }

  @Column(name="F_CAS_NUMBER")
  public String getCasNumber()
  {
    return casNumber;
  }

  public void setComponentId(String componentId)
  {
    this.componentId = componentId;
  }

  @Column(name="F_COMPONENT_ID")
  public String getComponentId()
  {
    return componentId;
  }

  public void setTradeSecretName(String tradeSecretName)
  {
    this.tradeSecretName = tradeSecretName;
  }

  @Column(name="F_TRADE_SECRET_NAME")
  public String getTradeSecretName()
  {
    return tradeSecretName;
  }

  public void setTradeSecretFlag(String tradeSecretFlag)
  {
    this.tradeSecretFlag = tradeSecretFlag;
  }

  @Column(name="F_TRADE_SECRET_FLAG")
  public String getTradeSecretFlag()
  {
    return tradeSecretFlag;
  }

  public void setPercentage(BigDecimal percentage)
  {
    this.percentage = percentage;
  }

  @Column(name="F_PERCENTAGE")
  public BigDecimal getPercentage()
  {
    return percentage;
  }

  public void setPercentRange(String percentRange)
  {
    this.percentRange = percentRange;
  }

  @Column(name="F_PERCENT_RANGE")
  public String getPercentRange()
  {
    return percentRange;
  }

  public void setProductUom(String productUom)
  {
    this.productUom = productUom;
  }

  @Column(name="F_PRODUCT_UOM")
  public String getProductUom()
  {
    return productUom;
  }

  public void setLineUom(String lineUom)
  {
    this.lineUom = lineUom;
  }

  @Column(name="F_LINE_UOM")
  public String getLineUom()
  {
    return lineUom;
  }

  public void setLineQuantity(BigDecimal lineQuantity)
  {
    this.lineQuantity = lineQuantity;
  }

  @Column(name="F_LINE_QUANTITY")
  public BigDecimal getLineQuantity()
  {
    return lineQuantity;
  }

  public void setProductQuantity(BigDecimal productQuantity)
  {
    this.productQuantity = productQuantity;
  }

  @Column(name="F_PRODUCT_QUANTITY")
  public BigDecimal getProductQuantity()
  {
    return productQuantity;
  }

  public void setHazardFlag(BigDecimal hazardFlag)
  {
    this.hazardFlag = hazardFlag;
  }

  @Column(name="F_HAZARD_FLAG")
  public BigDecimal getHazardFlag()
  {
    return hazardFlag;
  }

  public void setModel(String model)
  {
    this.model = model;
  }

  @Column(name="F_MODEL")
  public String getModel()
  {
    return model;
  }

  public void setModelDesc(String modelDesc)
  {
    this.modelDesc = modelDesc;
  }

  @Column(name="F_MODEL_DESC")
  public String getModelDesc()
  {
    return modelDesc;
  }

  public void setOrder(Date order)
  {
    this.order = order;
  }

  @Column(name="F_ORDER")
  public Date getOrder()
  {
    return order;
  }

  public void setDirection(String direction)
  {
    this.direction = direction;
  }

  @Column(name="F_DIRECTION")
  public String getDirection()
  {
    return direction;
  }

  public void setStatus(Long status)
  {
    this.status = status;
  }

  @Column(name="F_STATUS")
  public Long getStatus()
  {
    return status;
  }

  public void setRemarks(String remarks)
  {
    this.remarks = remarks;
  }

  @Column(name="F_REMARKS")
  public String getRemarks()
  {
    return remarks;
  }

  public void setUserUpdated(String userUpdated)
  {
    this.userUpdated = userUpdated;
  }

  @Column(name="F_USER_UPDATED")
  public String getUserUpdated()
  {
    return userUpdated;
  }

  public void setDateStamp(Date dateStamp)
  {
    this.dateStamp = dateStamp;
  }

  @Column(name="F_DATE_STAMP")
  public Date getDateStamp()
  {
    return dateStamp;
  }

  public void setUserInserted(String userInserted)
  {
    this.userInserted = userInserted;
  }

  @Column(name="F_USER_INSERTED")
  public String getUserInserted()
  {
    return userInserted;
  }

  public void setDateStampInserted(Date dateStampInserted)
  {
    this.dateStampInserted = dateStampInserted;
  }

  @Column(name="F_DATE_STAMP_INSERTED")
  public Date getDateStampInserted()
  {
    return dateStampInserted;
  }
}
