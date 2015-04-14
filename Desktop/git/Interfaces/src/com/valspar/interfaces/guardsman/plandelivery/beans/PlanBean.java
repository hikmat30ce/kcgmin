package com.valspar.interfaces.guardsman.plandelivery.beans;

import com.valspar.interfaces.common.beans.SimpleUserBean;
import com.valspar.interfaces.common.servlets.PropertiesServlet;
import com.valspar.interfaces.common.utils.FlatFileUtility;
import com.valspar.interfaces.guardsman.plandelivery.enums.PlanActionTaken;
import java.util.*;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.validator.routines.EmailValidator;

public class PlanBean
{
  private String saTypeId;
  private String conSaId;
  private String conId;
  private SimpleUserBean consumer = new SimpleUserBean();
  private String languageCode;
  private String planName;
  private String saId;
  private String erpRetailerNo;
  private Date printDate;
  private String printPlanId;
  private String reprintFlag;
  private String planEmailingFlag;
  private String conAddrId;
  private String address1;
  private String address2;
  private String city;
  private String state;
  private String postalCode;
  private Date uetaNotifyDate;
  private long daysSinceUetaNotified;
  private boolean uetaAccepted;
  private boolean uetaDeclined;
  private PlanActionTaken actionTaken;
  private String planPdfFilePath;
  private EmailValidator emailValidator = EmailValidator.getInstance();

  public boolean isEmailEnabled()
  {
    return StringUtils.equalsIgnoreCase(planEmailingFlag, "Y") && emailValidator.isValid(consumer.getEmail());
  }

  public boolean shouldEmailUetaNotice()
  {
    return uetaNotifyDate == null && isEmailEnabled();
  }

  public boolean shouldEmailPlanPdf()
  {
    return uetaAccepted && isEmailEnabled();
  }

  public boolean shouldPrint()
  {
    return !isEmailEnabled() || uetaDeclined || (!uetaAccepted && daysSinceUetaNotified > 30);
  }

  public String getUetaUrl()
  {
    StringBuilder sb = new StringBuilder();
    sb.append(PropertiesServlet.getProperty("guardsmanplandelivery.uetaUrl"));
    sb.append("?u=");
    sb.append(conId);
    sb.append(conSaId);

    return sb.toString();
  }

  public boolean isOutsourced()
  {
    return StringUtils.isNotEmpty(printPlanId);
  }

  public String getCombinedStreetAddress()
  {
    StringBuilder sb = new StringBuilder();
    if (StringUtils.isNotEmpty(address1))
    {
      sb.append(address1);
      sb.append(" ");
    }
    if (StringUtils.isNotEmpty(address2))
    {
      sb.append(address2);
    }
    
    return StringUtils.trimToEmpty(sb.toString());
  }

  public static List<String> getOutsourceFileHeaderFields()
  {
    List<String> headers = new ArrayList<String>();
    headers.add("doc type");
    headers.add("srf_sa_id");
    headers.add("name");
    headers.add("address1");
    headers.add("address2");
    headers.add("city");
    headers.add("st");
    headers.add("zip");
    headers.add("language");
    headers.add("plan_name");
    headers.add("sa_id");
    headers.add("sa_type_id");
    headers.add("srf_id");
    headers.add("Con SA ID");
    headers.add("Con ID");
    headers.add("con_addr_id");
    headers.add("erp_no");
    headers.add("plan_file");
    headers.add("plan_reprint");
    headers.add("letter_verbiage");
    headers.add("rep_name");
    headers.add("rep_ext");
    headers.add("pages");
    headers.add("claim_id");
    headers.add("first_name");
    headers.add("last_name");
    headers.add("order_no");
    headers.add("rush_order");
    headers.add("fp_1");
    headers.add("dr_1");
    headers.add("fp_2");
    headers.add("dr_2");
    headers.add("fp_3");
    headers.add("dr_3");
    headers.add("fp_4");
    headers.add("dr_4");
    headers.add("fp_5");
    headers.add("dr_5");
    headers.add("fp_6");
    headers.add("dr_6");
    headers.add("fp_7");
    headers.add("dr_7");
    headers.add("fp_8");
    headers.add("dr_8");
    headers.add("fp_9");
    headers.add("dr_9");
    headers.add("fp_10");
    headers.add("dr_10");
    headers.add("status");
    headers.add("err_desc");
    headers.add("proc_date");
    headers.add("new_addr1");
    headers.add("new_addr2");
    headers.add("new_city");
    headers.add("new_state");
    headers.add("new_zip10");
    headers.add("ups_track");

    return headers;
  }

  public static List<String> getInternalFileHeaderFields()
  {
    List<String> headers = new ArrayList<String>();
    
    headers.add("doc type");
    headers.add("srf_id-con_sa_id-con_id");
    headers.add("name");
    headers.add("address1");
    headers.add("address2");
    headers.add("city st zip");
    headers.add("language");
    headers.add("plan name");
    headers.add("furniture");
    headers.add("email");
    headers.add("sa id");
    headers.add("srf_id");
    headers.add("con_sa_id");
    headers.add("con_id");
    headers.add("erp_rtlr_no");

    return headers;
  }

  public void writeOutsourceRecord(FlatFileUtility flatFileUtility)
  {
    List<String> fields = new ArrayList<String>();
    
    fields.add("PLAN");
    fields.add("0-" + StringUtils.trimToEmpty(conSaId) + "-" + StringUtils.trimToEmpty(conId));
    fields.add(StringUtils.trimToEmpty(consumer.getFirstName()) + " " + StringUtils.trimToEmpty(consumer.getLastName()));
    fields.add(StringUtils.trimToEmpty(address1));
    fields.add(StringUtils.trimToEmpty(address2));
    fields.add(StringUtils.trimToEmpty(city));
    fields.add(StringUtils.trimToEmpty(state));
    fields.add(StringUtils.trimToEmpty(postalCode));
    fields.add(StringUtils.trimToEmpty(languageCode));
    fields.add(StringUtils.trimToEmpty(planName));
    fields.add(StringUtils.trimToEmpty(saId));
    fields.add(StringUtils.trimToEmpty(saTypeId));
    fields.add("0");
    fields.add(StringUtils.trimToEmpty(conSaId));
    fields.add(StringUtils.trimToEmpty(conId));
    fields.add(StringUtils.trimToEmpty(conAddrId));
    fields.add(StringUtils.trimToEmpty(erpRetailerNo));
    fields.add(StringUtils.trimToEmpty(printPlanId));
    fields.add(StringUtils.trimToEmpty(reprintFlag));
    fields.add("");
    fields.add("");
    fields.add("");
    fields.add("");
    fields.add("");
    fields.add("");
    fields.add("");
    fields.add("");
    fields.add("");
    fields.add(""); 

    flatFileUtility.writeLine(fields);
  }

  public void writeInternalRecord(FlatFileUtility flatFileUtility)
  {
    flatFileUtility.writeLine(String.valueOf((char)12) + "PLAN");
    flatFileUtility.writeLine("0-" + StringUtils.trimToEmpty(conSaId) + StringUtils.trimToEmpty(conId));
    flatFileUtility.writeLine("");
    flatFileUtility.writeLine("");
    flatFileUtility.writeLine(StringUtils.repeat(" ", 61) + StringUtils.trimToEmpty(consumer.getFirstName()) + " " + StringUtils.trimToEmpty(consumer.getLastName()));
    flatFileUtility.writeLine(StringUtils.repeat(" ", 61) + StringUtils.trimToEmpty(getCombinedStreetAddress()));
    flatFileUtility.writeLine(StringUtils.rightPad(StringUtils.trimToEmpty(languageCode), 61, " ") + StringUtils.trimToEmpty(city) + " " + StringUtils.trimToEmpty(state) + "  " + StringUtils.trimToEmpty(postalCode));
    flatFileUtility.writeLine(StringUtils.trimToEmpty(planName));
    flatFileUtility.writeLine("");
    flatFileUtility.writeLine("");
    flatFileUtility.writeLine(StringUtils.trimToEmpty(saId));
    flatFileUtility.writeLine("0");
    flatFileUtility.writeLine(StringUtils.trimToEmpty(conSaId));
    flatFileUtility.writeLine(StringUtils.trimToEmpty(conId));
    flatFileUtility.writeLine(StringUtils.trimToEmpty(erpRetailerNo));
  }

  public void setPlanPdfFilePath(String planPdfFilePath)
  {
    this.planPdfFilePath = planPdfFilePath;
  }

  public String getPlanPdfFilePath()
  {
    return planPdfFilePath;
  }

  public void setSaTypeId(String saTypeId)
  {
    this.saTypeId = saTypeId;
  }

  public String getSaTypeId()
  {
    return saTypeId;
  }

  public void setConSaId(String conSaId)
  {
    this.conSaId = conSaId;
  }

  public String getConSaId()
  {
    return conSaId;
  }

  public void setConId(String conId)
  {
    this.conId = conId;
  }

  public String getConId()
  {
    return conId;
  }

  public void setLanguageCode(String languageCode)
  {
    this.languageCode = languageCode;
  }

  public String getLanguageCode()
  {
    return languageCode;
  }

  public void setPlanName(String planName)
  {
    this.planName = planName;
  }

  public String getPlanName()
  {
    return planName;
  }

  public void setSaId(String saId)
  {
    this.saId = saId;
  }

  public String getSaId()
  {
    return saId;
  }

  public void setErpRetailerNo(String erpRetailerNo)
  {
    this.erpRetailerNo = erpRetailerNo;
  }

  public String getErpRetailerNo()
  {
    return erpRetailerNo;
  }

  public void setPrintDate(Date printDate)
  {
    this.printDate = printDate;
  }

  public Date getPrintDate()
  {
    return printDate;
  }

  public void setReprintFlag(String reprintFlag)
  {
    this.reprintFlag = reprintFlag;
  }

  public String getReprintFlag()
  {
    return reprintFlag;
  }

  public void setCity(String city)
  {
    this.city = city;
  }

  public String getCity()
  {
    return city;
  }

  public void setState(String state)
  {
    this.state = state;
  }

  public String getState()
  {
    return state;
  }

  public void setPostalCode(String postalCode)
  {
    this.postalCode = postalCode;
  }

  public String getPostalCode()
  {
    return postalCode;
  }

  public void setConsumer(SimpleUserBean consumer)
  {
    this.consumer = consumer;
  }

  public SimpleUserBean getConsumer()
  {
    return consumer;
  }

  public void setPrintPlanId(String printPlanId)
  {
    this.printPlanId = printPlanId;
  }

  public String getPrintPlanId()
  {
    return printPlanId;
  }

  public void setAddress1(String address1)
  {
    this.address1 = address1;
  }

  public String getAddress1()
  {
    return address1;
  }

  public void setAddress2(String address2)
  {
    this.address2 = address2;
  }

  public String getAddress2()
  {
    return address2;
  }

  public void setConAddrId(String conAddrId)
  {
    this.conAddrId = conAddrId;
  }

  public String getConAddrId()
  {
    return conAddrId;
  }

  public void setPlanEmailingFlag(String planEmailingFlag)
  {
    this.planEmailingFlag = planEmailingFlag;
  }

  public String getPlanEmailingFlag()
  {
    return planEmailingFlag;
  }

  public void setUetaAccepted(boolean uetaAccepted)
  {
    this.uetaAccepted = uetaAccepted;
  }

  public boolean isUetaAccepted()
  {
    return uetaAccepted;
  }

  public void setUetaDeclined(boolean uetaDeclined)
  {
    this.uetaDeclined = uetaDeclined;
  }

  public boolean isUetaDeclined()
  {
    return uetaDeclined;
  }

  public void setUetaNotifyDate(Date uetaNotifyDate)
  {
    this.uetaNotifyDate = uetaNotifyDate;
  }

  public Date getUetaNotifyDate()
  {
    return uetaNotifyDate;
  }

  public void setDaysSinceUetaNotified(long daysSinceUetaNotified)
  {
    this.daysSinceUetaNotified = daysSinceUetaNotified;
  }

  public long getDaysSinceUetaNotified()
  {
    return daysSinceUetaNotified;
  }

  public void setActionTaken(PlanActionTaken actionTaken)
  {
    this.actionTaken = actionTaken;
  }

  public PlanActionTaken getActionTaken()
  {
    return actionTaken;
  }
}
