package com.valspar.interfaces.wercs.wercsorders.beans;

import java.util.Date;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.SequenceGenerator;
import javax.persistence.Table;
import javax.persistence.Transient;
import org.apache.log4j.Logger;

@Entity
@Table(name = "WERCS.I_ORDERS")
public class IorderBean implements Cloneable
{
  private static Logger log4jLogger = Logger.getLogger(IorderBean.class);
  
  private Long recordKey;
  private String jobId;
  private String plant;
  private String customerId;
  private String addressType;
  private String customerType;  
  private String product;
  private String customerName;
  private String address1;
  private String address2;
  private String city;
  private String state;
  private String zipCode;
  private String countryCode;
  private String attentionLine;
  private String destination;
  private Long numCopies;
  private String dateRequested;
  private String timeRequested;
  private Long alwaysSend;
  private String substitutions;
  private String custOrder;
  private String telephone;
  private String fax;
  private String language;
  private String format;
  private String subformat;
  private Long quantity;
  private String uom;
  private String cont;
  private Long stdWgt;
  private String custom1;
  private String custom2;
  private String custom3;
  private String custom4;
  private String custom5;
  private Long status;  
  private String remarks;
  private String userUpdated;
  private Date dateStamp;
  private String userInserted;
  private Date dateStampInserted;
  private String inventoryType;
  
  public IorderBean()
  {
  }

  public void setRecordKey(Long recordKey)
  {
    this.recordKey = recordKey;
  }

  @Id
  @Column(name="F_RECORD_KEY")  
  @GeneratedValue(generator = "OrderIdSeq", 
                      strategy = GenerationType.SEQUENCE)
  @SequenceGenerator(name = "OrderIdSeq", 
                         sequenceName = "WERCS.I_ORDERS_SEQ", 
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

  public void setPlant(String plant)
  {
    this.plant = plant;
  }

  @Column(name="F_PLANT")  
  public String getPlant()
  {
    return plant;
  }

  public void setCustomerId(String customerId)
  {
    this.customerId = customerId;
  }

  @Column(name="F_CUSTOMER_ID")  
  public String getCustomerId()
  {
    return customerId;
  }

  public void setAddressType(String addressType)
  {
    this.addressType = addressType;
  }

  @Column(name="F_ADDRESS_TYPE") 
  public String getAddressType()
  {
    return addressType;
  }

  public void setCustomerType(String customerType)
  {
    this.customerType = customerType;
  }

  @Column(name="F_CUSTOMER_TYPE")  
  public String getCustomerType()
  {
    return customerType;
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

  public void setCustomerName(String customerName)
  {
    this.customerName = customerName;
  }

  @Column(name="F_CUSTOMER_NAME")  
  public String getCustomerName()
  {
    return customerName;
  }

  public void setAddress1(String address1)
  {
    this.address1 = address1;
  }

  @Column(name="F_ADDRESS1")  
  public String getAddress1()
  {
    return address1;
  }

  public void setAddress2(String address2)
  {
    this.address2 = address2;
  }

  @Column(name="F_ADDRESS2")  
  public String getAddress2()
  {
    return address2;
  }

  public void setCity(String city)
  {
    this.city = city;
  }

  @Column(name="F_CITY")  
  public String getCity()
  {
    return city;
  }

  public void setState(String state)
  {
    this.state = state;
  }

  @Column(name="F_STATE")  
  public String getState()
  {
    return state;
  }

  public void setZipCode(String zipCode)
  {
    this.zipCode = zipCode;
  }

  @Column(name="F_ZIP_CODE")  
  public String getZipCode()
  {
    return zipCode;
  }

  public void setCountryCode(String countryCode)
  {
    this.countryCode = countryCode;
  }

  @Column(name="F_COUNTRY_CODE")  
  public String getCountryCode()
  {
    return countryCode;
  }

  public void setAttentionLine(String attentionLine)
  {
    this.attentionLine = attentionLine;
  }

  @Column(name="F_ATTENTION_LINE")  
  public String getAttentionLine()
  {
    return attentionLine;
  }

  public void setDestination(String destination)
  {
    this.destination = destination;
  }

  @Column(name="F_DESTINATION")  
  public String getDestination()
  {
    return destination;
  }

  public void setNumCopies(Long numCopies)
  {
    this.numCopies = numCopies;
  }

  @Column(name="F_NUM_COPIES")  
  public Long getNumCopies()
  {
    return numCopies;
  }

  public void setDateRequested(String dateRequested)
  {
    this.dateRequested = dateRequested;
  }

  @Column(name="F_DATE_REQUESTED")  
  public String getDateRequested()
  {
    return dateRequested;
  }

  public void setTimeRequested(String timeRequested)
  {
    this.timeRequested = timeRequested;
  }

  @Column(name="F_TIME_REQUESTED")  
  public String getTimeRequested()
  {
    return timeRequested;
  }

  public void setAlwaysSend(Long alwaysSend)
  {
    this.alwaysSend = alwaysSend;
  }

  @Column(name="F_ALWAYS_SEND")  
  public Long getAlwaysSend()
  {
    return alwaysSend;
  }

  public void setSubstitutions(String substitutions)
  {
    this.substitutions = substitutions;
  }

  @Column(name="F_SUBSTITUTIONS")  
  public String getSubstitutions()
  {
    return substitutions;
  }

  public void setCustOrder(String custOrder)
  {
    this.custOrder = custOrder;
  }

  @Column(name="F_CUST_ORDER")  
  public String getCustOrder()
  {
    return custOrder;
  }

  public void setTelephone(String telephone)
  {
    this.telephone = telephone;
  }

  @Column(name="F_TELEPHONE")  
  public String getTelephone()
  {
    return telephone;
  }

  public void setFax(String fax)
  {
    this.fax = fax;
  }

  @Column(name="F_FAX")  
  public String getFax()
  {
    return fax;
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

  public void setQuantity(Long quantity)
  {
    this.quantity = quantity;
  }

  @Column(name="F_QUANTITY")  
  public Long getQuantity()
  {
    return quantity;
  }

  public void setUom(String uom)
  {
    this.uom = uom;
  }

  @Column(name="F_UOM")  
  public String getUom()
  {
    return uom;
  }

  public void setCont(String cont)
  {
    this.cont = cont;
  }

  @Column(name="F_CONT")  
  public String getCont()
  {
    return cont;
  }

  public void setStdWgt(Long stdWgt)
  {
    this.stdWgt = stdWgt;
  }

  @Column(name="F_STD_WGT")  
  public Long getStdWgt()
  {
    return stdWgt;
  }

  public void setCustom1(String custom1)
  {
    this.custom1 = custom1;
  }

  @Column(name="F_CUSTOM1")  
  public String getCustom1()
  {
    return custom1;
  }

  public void setCustom2(String custom2)
  {
    this.custom2 = custom2;
  }

  @Column(name="F_CUSTOM2")  
  public String getCustom2()
  {
    return custom2;
  }

  public void setCustom3(String custom3)
  {
    this.custom3 = custom3;
  }

  @Column(name="F_CUSTOM3")  
  public String getCustom3()
  {
    return custom3;
  }

  public void setCustom4(String custom4)
  {
    this.custom4 = custom4;
  }

  @Column(name="F_CUSTOM4")  
  public String getCustom4()
  {
    return custom4;
  }

  public void setCustom5(String custom5)
  {
    this.custom5 = custom5;
  }

  @Column(name="F_CUSTOM5")  
  public String getCustom5()
  {
    return custom5;
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

  public void setInventoryType(String inventoryType)
  {
    this.inventoryType = inventoryType;
  }

  @Transient
  public String getInventoryType()
  {
    return inventoryType;
  }

  public Object clone()
  {
    try
    {
      return super.clone();
    }
    catch (CloneNotSupportedException e)
    {
      log4jLogger.error(e);
    }
    return null;
  }
}
