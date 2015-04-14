package com.valspar.interfaces.common.quartz.beans;

import java.util.Date;
import javax.persistence.*;
import org.apache.commons.lang.builder.EqualsBuilder;
import org.apache.commons.lang.builder.HashCodeBuilder;

@Entity
@Table(name = "QUARTZ.VCA_APPLICATION")

public class ApplicationBean
{
  private Long applicationId;
  private String application;
  private String createdBy;
  private Date creationDate;
  private String lastUpdatedBy;
  private Date lastUpdatedDate;
  
  public ApplicationBean()
  {
  }

  public void setApplicationId(Long applicationId)
  {
    this.applicationId = applicationId;
  }

  @Id
  @Column(name="APPLICATION_ID")  
  @GeneratedValue(generator = "ApplicationIdSeq", 
                      strategy = GenerationType.SEQUENCE)
  @SequenceGenerator(name = "ApplicationIdSeq", 
                         sequenceName = "QUARTZ.VCA_APPLICATION_SEQ", 
                         allocationSize = 1)
  public Long getApplicationId()
  {
    return applicationId;
  }

  public void setApplication(String application)
  {
    this.application = application;
  }

  @Column(name="APPLICATION" )
   public String getApplication()
  {
    return application;
  }

  public void setCreatedBy(String createdBy)
  {
    this.createdBy = createdBy;
  }

  @Column(name="CREATED_BY")
  public String getCreatedBy()
  {
    return createdBy;
  }

  public void setCreationDate(Date creationDate)
  {
    this.creationDate = creationDate;
  }

  @Column(name="CREATION_DATE")
  public Date getCreationDate()
  {
    return creationDate;
  }

  public void setLastUpdatedBy(String lastUpdatedBy)
  {
    this.lastUpdatedBy = lastUpdatedBy;
  }

  @Column(name="LAST_UPDATED_BY")
  public String getLastUpdatedBy()
  {
    return lastUpdatedBy;
  }

  public void setLastUpdatedDate(Date lastUpdatedDate)
  {
    this.lastUpdatedDate = lastUpdatedDate;
  }
  
  @Column(name="LAST_UPDATED_DATE")
  public Date getLastUpdatedDate()
  {
    return lastUpdatedDate;
  }

  @Transient   
  public boolean equals(Object obj) 
  {
    return EqualsBuilder.reflectionEquals(this, obj);
  }

  @Transient   
  public int hashCode() 
  {
    return HashCodeBuilder.reflectionHashCode(this);
  }
}
