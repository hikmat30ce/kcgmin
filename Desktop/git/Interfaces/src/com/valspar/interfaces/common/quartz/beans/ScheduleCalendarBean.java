package com.valspar.interfaces.common.quartz.beans;

import java.util.Date;
import javax.persistence.*;

@Entity
@Table(name = "QUARTZ.VCA_SCHEDULE_CALENDAR")
public class ScheduleCalendarBean implements Comparable<ScheduleCalendarBean>
{
  private Long scheduleCalendarId;
  private Long scheduleId;
  private String cronExpression;
  private Date scheduleDate;
  private String createdBy;
  private Date creationDate;
  private String lastUpdatedBy;
  private Date lastUpdatedDate;
  private ScheduleBean scheduleBean;

  public ScheduleCalendarBean()
  {
  }

  public void setScheduleCalendarId(Long scheduleCalendarId)
  {
    this.scheduleCalendarId = scheduleCalendarId;
  }

  @Id
  @Column(name="SCHEDULE_CALENDAR_ID")  
  @GeneratedValue(generator = "ScheduleCalendarIdSeq", 
                      strategy = GenerationType.SEQUENCE)
  @SequenceGenerator(name = "ScheduleCalendarIdSeq", 
                         sequenceName = "QUARTZ.VCA_SCHEDULE_CALENDAR_SEQ", 
                         allocationSize = 1)
  public Long getScheduleCalendarId()
  {
    return scheduleCalendarId;
  }

  public void setScheduleDate(Date scheduleDate)
  {
    this.scheduleDate = scheduleDate;
  }

  @Column(name="SCHEDULE_DATE", nullable = false )
   public Date getScheduleDate()
  {
    return scheduleDate;
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
  
  public void setCronExpression(String cronExpression)
  {
    this.cronExpression = cronExpression;
  }

  @Column(name = "CRON_EXPRESSION")
  public String getCronExpression()
  {
    return cronExpression;
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
  
  public int compareTo(ScheduleCalendarBean o)
  {
    return this.scheduleDate.compareTo(o.scheduleDate);
  }
}
