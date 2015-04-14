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
@Table(name = "WERCS.I_ALIASES")

public class IaliasBean
{
  private Long recordKey;
  private String jobId;
  private String product;
  private String alias;
  private String aliasName;
  private String language;
  private String direction;
  private Long status;
  private String remarks;
  private String userUpdated;
  private Date dateStamp;
  private String userInserted;
  private Date dateStampInserted;

  public IaliasBean()
  {
  }

  public void setRecordKey(Long recordKey)
  {
    this.recordKey = recordKey;
  }

  @Id
  @Column(name="F_RECORD_KEY")  
  @GeneratedValue(generator = "ProductIdSeq", 
                      strategy = GenerationType.SEQUENCE)
  @SequenceGenerator(name = "ProductIdSeq", 
                         sequenceName = "WERCS.I_ALIASES_SEQ", 
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

  public void setAlias(String alias)
  {
    this.alias = alias;
  }

  @Column(name="F_ALIAS")  
  public String getAlias()
  {
    return alias;
  }

  public void setAliasName(String aliasName)
  {
    this.aliasName = aliasName;
  }

  @Column(name="F_ALIAS_NAME")  
  public String getAliasName()
  {
    return aliasName;
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
