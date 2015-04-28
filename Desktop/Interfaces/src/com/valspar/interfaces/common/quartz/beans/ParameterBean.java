package com.valspar.interfaces.common.quartz.beans;

import java.util.Date;
import javax.persistence.*;
import com.valspar.interfaces.common.quartz.beans.*;

@Entity
@Table(name = "QUARTZ.VCA_SCHEDULE_PARAMETERS")
public class ParameterBean
{ 
  private Long parameterId;
  private Long scheduleId;
  private String parameterName;
  private String parameterValue;
  private String createdBy;
  private Date creationDate;
  private String lastUpdatedBy;
  private Date lastUpdatedDate;
  private ScheduleBean scheduleBean;
  
  public ParameterBean()
  {
  }


  public void setParameterId(Long parameterId)
  {
    this.parameterId = parameterId;
  }

  @Id
  @Column(name="PARAMETER_ID")  
  @GeneratedValue(generator = "ParameterIdSeq", 
                      strategy = GenerationType.SEQUENCE)
  @SequenceGenerator(name = "ParameterIdSeq", 
                         sequenceName = "QUARTZ.VCA_SCHEDULE_PARAMETERS_SEQ", 
                         allocationSize = 1)
  public Long getParameterId()
  {
    return parameterId;
  }

  public void setScheduleId(Long scheduleId)
  {
      this.scheduleId = scheduleId;
  }
  
 @Column(name="SCHEDULE_ID", nullable = false)
  public Long getScheduleId()
  {
    if(scheduleBean == null)
    {
      return scheduleId;
    }
    else
    {
      return scheduleBean.getScheduleId();
    }
  }

  public void setParameterName(String parameterName)
  {
    this.parameterName = parameterName;
  }

  @Column(name="PARAMETER_NAME")
  public String getParameterName()
  {
    return parameterName;
  }

  public void setParameterValue(String parameterValue)
  {
    this.parameterValue = parameterValue;
  }

  @Column(name="PARAMETER_VALUE")
  public String getParameterValue()
  {
    return parameterValue;
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

  public void setScheduleBean(ScheduleBean scheduleBean)
  {
    this.scheduleBean = scheduleBean;
  }

@Transient
  public ScheduleBean getScheduleBean()
  {
    return scheduleBean;
  }
}
