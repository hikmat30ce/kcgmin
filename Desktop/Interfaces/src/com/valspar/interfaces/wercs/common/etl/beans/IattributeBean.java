package com.valspar.interfaces.wercs.common.etl.beans;

import java.util.Date;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.SequenceGenerator;
import javax.persistence.Table;

@Entity
@Table(name = "WERCS.I_ATTRIBUTES")

public class IattributeBean
{
  private Long recordKey;
  private String jobId;
  private String prodAliasComp;
  private String product;
  private String casNumber;
  private String componentId;
  private String format;
  private String subformat;
  private String usage;
  private String dataCode;
  private String data;
  private String textCode;
  private String bTextLine;
  private String lTextLine;
  private String language;
  private Long repDataSet;
  private Long repSequence;
  private Long deleteFlag;
  private String userToApply;
  private String direction;
  private Long order;
  private Long status;
  private String remarks;
  private String userUpdated;
  private Date dateStamp;
  private String userInserted;
  private Date dateStampInserted;

  public IattributeBean()
  {
  }

  public void setRecordKey(Long recordKey)
  {
    this.recordKey = recordKey;
  }

  @Id
  @Column(name="F_RECORD_KEY")
  @GeneratedValue(generator = "AttributeIdSeq", 
                      strategy = GenerationType.SEQUENCE)
  @SequenceGenerator(name = "AttributeIdSeq", 
                         sequenceName = "WERCS.I_ATTRIBUTES_SEQ", 
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

  public void setProdAliasComp(String prodAliasComp)
  {
    this.prodAliasComp = prodAliasComp;
  }
  
  @Column(name="F_PROD_ALIAS_COMP")
  public String getProdAliasComp()
  {
    return prodAliasComp;
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

  public void setFormat(String format)
  {
    this.format = format;
  }

  @Column(name="F_FORMAT")
  public String getFormat()
  {
    return format;
  }

  public void setSubformat(String subformat)
  {
    this.subformat = subformat;
  }

  @Column(name="F_SUBFORMAT")
  public String getSubformat()
  {
    return subformat;
  }

  public void setUsage(String usage)
  {
    this.usage = usage;
  }

  @Column(name="F_USAGE")
  public String getUsage()
  {
    return usage;
  }

  public void setDataCode(String dataCode)
  {
    this.dataCode = dataCode;
  }

  @Column(name="F_DATA_CODE")
  public String getDataCode()
  {
    return dataCode;
  }

  public void setData(String data)
  {
    this.data = data;
  }

  @Column(name="F_DATA")
  public String getData()
  {
    return data;
  }

  public void setTextCode(String textCode)
  {
    this.textCode = textCode;
  }

  @Column(name="F_TEXT_CODE")
  public String getTextCode()
  {
    return textCode;
  }

  public void setBTextLine(String bTextLine)
  {
    this.bTextLine = bTextLine;
  }

  @Column(name="F_B_TEXT_LINE")
  public String getBTextLine()
  {
    return bTextLine;
  }

  public void setLTextLine(String lTextLine)
  {
    this.lTextLine = lTextLine;
  }

  @Column(name="F_L_TEXT_LINE")
  public String getLTextLine()
  {
    return lTextLine;
  }

  public void setLanguage(String language)
  {
    this.language = language;
  }

  @Column(name="F_LANGUAGE")
  public String getLanguage()
  {
    return language;
  }

  public void setRepDataSet(Long repDataSet)
  {
    this.repDataSet = repDataSet;
  }

  @Column(name="F_REP_DATASET")
  public Long getRepDataSet()
  {
    return repDataSet;
  }

  public void setRepSequence(Long repSequence)
  {
    this.repSequence = repSequence;
  }

  @Column(name="F_REP_SEQUENCE")
  public Long getRepSequence()
  {
    return repSequence;
  }

  public void setDeleteFlag(Long deleteFlag)
  {
    this.deleteFlag = deleteFlag;
  }

  @Column(name="F_DELETE_FLAG")
  public Long getDeleteFlag()
  {
    return deleteFlag;
  }

  public void setUserToApply(String userToApply)
  {
    this.userToApply = userToApply;
  }

  @Column(name="F_USER_TO_APPLY")
  public String getUserToApply()
  {
    return userToApply;
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

  public void setOrder(Long order)
  {
    this.order = order;
  }

  @Column(name="F_ORDER")
  public Long getOrder()
  {
    return order;
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
