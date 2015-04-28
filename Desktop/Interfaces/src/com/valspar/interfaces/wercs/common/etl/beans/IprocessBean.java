package com.valspar.interfaces.wercs.common.etl.beans;

import java.util.Date;
import java.util.HashSet;
import java.util.Set;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.OneToMany;
import javax.persistence.SequenceGenerator;
import javax.persistence.Table;
import javax.persistence.Transient;
import org.hibernate.annotations.Cascade;
import org.hibernate.annotations.Fetch;
import org.hibernate.annotations.FetchMode;

@Entity
@Table(name = "WERCS.I_PROCESS")

public class IprocessBean
{
  private Long recordKey;
  private String jobId;
  private Long processGroupId;
  private Long currentProcess;
  private String product;
  private String casNumber;
  private String componentId;
  private String languages;
  private String format;
  private String subformat;
  private String plant;
  private String authorization;
  private String suppliers;
  private String revisionType;
  private Long priority;
  private Long status;
  private String remarks;
  private String userUpdated;
  private Date dateStamp;
  private String userInserted;
  private Date dateStampInserted;
  private String dateStampDelay;
  private Set<IproductBean> iProducts = new HashSet<IproductBean>();
  private Set<IaliasBean> iAliases = new HashSet<IaliasBean>();
  private Set<IattributeBean> iAttributes = new HashSet<IattributeBean>();
  private Set<IformulationBean> iFormulations = new HashSet<IformulationBean>();

  public IprocessBean()
  {
  }

  public void setRecordKey(Long recordKey)
  {
    this.recordKey = recordKey;
  }

  @Id
  @Column(name="F_RECORD_KEY")  
  @GeneratedValue(generator = "ProcessIdSeq", 
                      strategy = GenerationType.SEQUENCE)
  @SequenceGenerator(name = "ProcessIdSeq", 
                         sequenceName = "WERCS.I_PROCESSES_SEQ", 
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

  public void setProcessGroupId(Long processGroupId)
  {
    this.processGroupId = processGroupId;
  }

  @Column(name="F_PROCESS_GROUP_ID") 
  public Long getProcessGroupId()
  {
    return processGroupId;
  }

  public void setCurrentProcess(Long currentProcess)
  {
    this.currentProcess = currentProcess;
  }

  @Column(name="F_CURRENT_PROCESS") 
  public Long getCurrentProcess()
  {
    return currentProcess;
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

  public void setLanguages(String languages)
  {
    this.languages = languages;
  }

  @Column(name="F_LANGUAGES") 
  public String getLanguages()
  {
    return languages;
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

  public void setPlant(String plant)
  {
    this.plant = plant;
  }

  @Column(name="F_PLANT") 
  public String getPlant()
  {
    return plant;
  }

  public void setAuthorization(String authorization)
  {
    this.authorization = authorization;
  }

  @Column(name="F_AUTHORIZATION") 
  public String getAuthorization()
  {
    return authorization;
  }

  public void setSuppliers(String suppliers)
  {
    this.suppliers = suppliers;
  }

  @Column(name="F_SUPPLIERS") 
  public String getSuppliers()
  {
    return suppliers;
  }

  public void setRevisionType(String revisionType)
  {
    this.revisionType = revisionType;
  }

  @Column(name="F_REVISION_TYPE")
  public String getRevisionType()
  {
    return revisionType;
  }

  public void setPriority(Long priority)
  {
    this.priority = priority;
  }

  @Column(name="F_PRIORITY") 
  public Long getPriority()
  {
    return priority;
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

  public void setIProducts(Set<IproductBean> iProducts)
  {
    this.iProducts = iProducts;
  }

  @OneToMany(orphanRemoval = true, mappedBy = "jobId", fetch = FetchType.EAGER)
  @Cascade({ org.hibernate.annotations.CascadeType.ALL })
  @Fetch(FetchMode.SELECT)
  public Set<IproductBean> getIProducts()
  {
    return iProducts;
  }

  public void setIAttributes(Set<IattributeBean> iAttributes)
  {
    this.iAttributes = iAttributes;
  }

  @OneToMany(orphanRemoval = true, mappedBy = "jobId", fetch = FetchType.EAGER)
  @Cascade({ org.hibernate.annotations.CascadeType.ALL })
  @Fetch(FetchMode.SELECT)
  public Set<IattributeBean> getIAttributes()
  {
    return iAttributes;
  }

  public void setIFormulations(Set<IformulationBean> iFormulations)
  {
    this.iFormulations = iFormulations;
  }

  //@Transient
  @OneToMany(orphanRemoval = true, mappedBy = "jobId", fetch = FetchType.EAGER)
  @Cascade({ org.hibernate.annotations.CascadeType.ALL })
  @Fetch(FetchMode.SELECT)
  public Set<IformulationBean> getIFormulations()
  {
    return iFormulations;
  }

  public void setIAliases(Set<IaliasBean> iAliases)
  {
    this.iAliases = iAliases;
  }

  @OneToMany(orphanRemoval = true, mappedBy = "jobId", fetch = FetchType.EAGER)
  @Cascade({ org.hibernate.annotations.CascadeType.ALL })
  @Fetch(FetchMode.SELECT)
  public Set<IaliasBean> getIAliases()
  {
    return iAliases;
  }

  public void setDateStampDelay(String dateStampDelay)
  {
    this.dateStampDelay = dateStampDelay;
  }

  @Transient
  public String getDateStampDelay()
  {
    return dateStampDelay;
  }
}
