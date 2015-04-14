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
@Table(name = "WERCS.I_PRODUCTS")
public class IproductBean
{
 
  private Long recordKey;
  private String jobId;
  private String product;
  private String productName;
  private String direction;
  private Long status;
  private String remarks;
  private String userUpdated;
  private Date dateStamp;
  private String userInserted;
  private Date dateStampInserted;

  public IproductBean()
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
                         sequenceName = "WERCS.I_PRODUCTS_SEQ", 
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

  public void setProductName(String productName)
  {
    this.productName = productName;
  }

  @Column(name="F_PRODUCT_NAME")
  public String getProductName()
  {
    return productName;
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
