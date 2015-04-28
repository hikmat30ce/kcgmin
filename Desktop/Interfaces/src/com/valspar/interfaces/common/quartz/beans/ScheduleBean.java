package  com.valspar.interfaces.common.quartz.beans;

import com.valspar.interfaces.common.servlets.PropertiesServlet;
import java.util.*;
import javax.persistence.*;
import org.apache.commons.lang.StringUtils;
import org.hibernate.annotations.Cascade;
import org.hibernate.annotations.Fetch;
import org.hibernate.annotations.FetchMode;

@Entity
@Table(name = "QUARTZ.VCA_SCHEDULE")
public class ScheduleBean
{
  private Long scheduleId;
  private ApplicationBean applicationBean = new ApplicationBean();
  private String jobKey;
  private String jobGroup;
  private String triggerKey;
  private String triggerGroup;
  private String className;
  private String requestor;
  private String cronExpression;
  private boolean activeFlag;
  private String createdBy;
  private Date creationDate;
  private String lastUpdatedBy;
  private Date lastUpdatedDate;
  private Set<ParameterBean> parameters = new HashSet<ParameterBean>();
  private Set<ScheduleCalendarBean> scheduleCalendars = new HashSet<ScheduleCalendarBean>();
  private Date todayDate = new Date();
  private String userComment;
  private boolean OverrideProduction;

  public ScheduleBean()
  {
  }

  public void setScheduleId(Long scheduleId)
  {
    this.scheduleId = scheduleId;
  }

  @Id
  @Column(name = "SCHEDULE_ID")
  @GeneratedValue(generator = "ScheduleIdSeq", strategy = GenerationType.SEQUENCE)
  @SequenceGenerator(name = "ScheduleIdSeq", sequenceName = "QUARTZ.VCA_SCHEDULE_SEQ", allocationSize = 1)
  public Long getScheduleId()
  {
    return scheduleId;
  }

  public void setApplicationBean(ApplicationBean applicationBean)
  {
    this.applicationBean = applicationBean;
  }

  @OneToOne
  @JoinColumn(name = "APPLICATION_ID")
  public ApplicationBean getApplicationBean()
  {
    return applicationBean;
  }

  public void setJobKey(String jobKey)
  {
    this.jobKey = jobKey;
  }

  @Column(name = "JOB_KEY")
  public String getJobKey()
  {
    return jobKey;
  }

  public void setJobGroup(String jobGroup)
  {
    this.jobGroup = jobGroup;
  }

  @Column(name = "JOB_GROUP")
  public String getJobGroup()
  {
    return jobGroup;
  }

  public void setTriggerKey(String triggerKey)
  {
    this.triggerKey = triggerKey;
  }

  @Column(name = "TRIGGER_KEY")
  public String getTriggerKey()
  {
    return triggerKey;
  }

  public void setTriggerGroup(String triggerGroup)
  {
    this.triggerGroup = triggerGroup;
  }

  @Column(name = "TRIGGER_GROUP")
  public String getTriggerGroup()
  {
    return triggerGroup;
  }

  public void setClassName(String className)
  {
    this.className = className;
  }

  @Column(name = "CLASS_NAME")
  public String getClassName()
  {
    return className;
  }

  public void setRequestor(String requestor)
  {
    this.requestor = requestor;
  }

  @Column(name = "REQUESTOR")
  public String getRequestor()
  {
    return requestor;
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

  public void setActiveFlag(boolean activeFlag)
  {
    this.activeFlag = activeFlag;
  }

  @Column(name = "ACTIVE_FLAG")
  public boolean isActiveFlag()
  {
    return activeFlag;
  }

  public void setCreatedBy(String createdBy)
  {
    this.createdBy = createdBy;
  }

  @Column(name = "CREATED_BY")
  public String getCreatedBy()
  {
    return createdBy;
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

  public void setLastUpdatedBy(String lastUpdatedBy)
  {
    this.lastUpdatedBy = lastUpdatedBy;
  }

  @Column(name = "LAST_UPDATED_BY")
  public String getLastUpdatedBy()
  {
    return lastUpdatedBy;
  }

  public void setLastUpdatedDate(Date lastUpdatedDate)
  {
    this.lastUpdatedDate = lastUpdatedDate;
  }

  @Column(name = "LAST_UPDATED_DATE")
  public Date getLastUpdatedDate()
  {
    return lastUpdatedDate;
  }

  public void setParameters(Set<ParameterBean> parameters)
  {
    this.parameters = parameters;
  }

  @OneToMany(orphanRemoval = true, mappedBy = "scheduleId", fetch = FetchType.EAGER)
  @Cascade({ org.hibernate.annotations.CascadeType.ALL })
  @Fetch(FetchMode.SELECT)
  public Set<ParameterBean> getParameters()
  {
    return parameters;
  }

  @Transient
  public List<ParameterBean> getParametersAsList()
  {
    List<ParameterBean> parameterList = new ArrayList<ParameterBean>();
    parameterList.addAll(parameters);
    return parameterList;
  }
  
  @Transient
  public void addParameter(ParameterBean parameterBean)
  {
    parameterBean.setScheduleBean(this);
    getParameters().add(parameterBean);    
  }

  @Transient  
  public String getImageSource()
  { 
    if(this.isActiveFlag())
    {
      return "checkmark.gif";
    }
    else
    {
      return "exclamation.png";
    }
  }
  
  public void setScheduleCalendars(Set<ScheduleCalendarBean> scheduleCalendars)
  {
    this.scheduleCalendars = scheduleCalendars;
  }

  @OneToMany(orphanRemoval = true, mappedBy = "scheduleId", fetch = FetchType.EAGER)
  @Cascade({ org.hibernate.annotations.CascadeType.ALL })
  @Fetch(FetchMode.SELECT)
  public Set<ScheduleCalendarBean> getScheduleCalendars()
  {
    return scheduleCalendars;
  }

  @Transient
  public List<ScheduleCalendarBean> getScheduleCalendarAsList()
  {
    List<ScheduleCalendarBean> scheduleCalendarList = new ArrayList<ScheduleCalendarBean>();
    TreeSet<ScheduleCalendarBean> scheduleCalendarTree = new TreeSet<ScheduleCalendarBean>(scheduleCalendars);
    scheduleCalendarTree.addAll(scheduleCalendars);
    scheduleCalendarList.addAll(scheduleCalendarTree);
    return  scheduleCalendarList;
  }

  @Transient
  public ScheduleCalendarBean getNextScheduleCalendar()
  {
    for (ScheduleCalendarBean scheduleCalendarBean : getScheduleCalendarAsList() )
    {
      if (scheduleCalendarBean.getScheduleDate().after(todayDate))
      {
        return scheduleCalendarBean;
      }
    }
    return null;
  }
  
  public void setUserComment(String userComment)
  {
    this.userComment = userComment;
  }

  @Column(name = "USER_COMMENT")
  public String getUserComment()
  {
    return userComment;
  }

  public void setOverrideProduction(boolean OverrideProduction)
  {
    this.OverrideProduction = OverrideProduction;
  }

  @Column(name = "OVERRIDE_PRODUCTION")
  public boolean isOverrideProduction()
  {
    return OverrideProduction;
  }

  @Transient
  public boolean isRunnableInEnvironment()
  {
    if (PropertiesServlet.isProduction() || (this.isOverrideProduction() && !(StringUtils.contains(System.getProperty("os.name"), "Windows"))))
    {
      return true;
    }
    return false;
  }

}