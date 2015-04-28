package com.valspar.interfaces.purchasing.cipace.beans;

import java.math.BigDecimal;
import java.util.*;
import org.apache.commons.lang.StringUtils;

public abstract class PoHeadersInterfaceBean
{
  private String projectId;
  private String projectName;
  private String cipAcePoAutoId;

  private String oraclePoHeaderId;
  private String cipAceDatabaseName;
  private String oracleDatabaseName;
  private String currencyCode;
  private String revisionNumber;
  private String shipToOrganizationCode;
  private String shipToOrganizationName;

  private String interfaceHeaderId;
  private String poBatchId;
  private String orgName;
  private String orgId;
  private String agentId;
  private String vendorId;
  private String vendorSiteId;
  private String shipToLocationId;
  private String billToLocationId;

  private String glCompanyCode;
  private String glProfitCenterCode;
  private String glLocationCode;
  private String glCostCenterCode;
  private String glAccountNumber;
  private String glCategoryCode;
  private String glDepartmentCode;
  private String glLocal;

  private BigDecimal rate;

  private String cipAcePoNumber;
  private String oraclePoNumber;

  private String userName;

  private boolean importFailed;
  private List<String> errorMessages = new ArrayList<String>();

  public String getAction()
  {
    return "ORIGINAL";
  }

  public abstract List<? extends PoLinesInterfaceBean> getLines();

  public void addFatalErrorMessage(String message)
  {
    importFailed = true;
    errorMessages.add(message);
  }

  public void addNonFatalErrorMessage(String message)
  {
    errorMessages.add(message);
  }

  public String getSubmissionErrorMessage()
  {
    if (errorMessages.isEmpty())
    {
      return "";
    }
    else
    {
      return errorMessages.toString();
    }
  }

  public boolean isImportSuccessful()
  {
    return StringUtils.isNotEmpty(oraclePoNumber);
  }

  public boolean hasErrorMessages()
  {
    return !errorMessages.isEmpty();
  }

  public String getComments()
  {
    return "CIPAce Project# " + projectId;
  }

  public String getPoPdfFilenamePattern()
  {
    // PO_165_1471253_0_US.pdf

    StringBuilder sb = new StringBuilder();
    sb.append("PO_");
    sb.append(orgId);
    sb.append("_");
    sb.append(oraclePoNumber);
    sb.append("_");
    sb.append(revisionNumber);
    sb.append("_%.pdf");

    return sb.toString();
  }

  public void setPoBatchId(String poBatchId)
  {
    this.poBatchId = poBatchId;
  }

  public String getPoBatchId()
  {
    return poBatchId;
  }

  public void setCurrencyCode(String currencyCode)
  {
    this.currencyCode = currencyCode;
  }

  public String getCurrencyCode()
  {
    return currencyCode;
  }

  public void setOrgId(String orgId)
  {
    this.orgId = orgId;
  }

  public String getOrgId()
  {
    return orgId;
  }

  public void setAgentId(String agentId)
  {
    this.agentId = agentId;
  }

  public String getAgentId()
  {
    return agentId;
  }

  public void setVendorId(String vendorId)
  {
    this.vendorId = vendorId;
  }

  public String getVendorId()
  {
    return vendorId;
  }

  public void setVendorSiteId(String vendorSiteId)
  {
    this.vendorSiteId = vendorSiteId;
  }

  public String getVendorSiteId()
  {
    return vendorSiteId;
  }

  public void setBillToLocationId(String billToLocationId)
  {
    this.billToLocationId = billToLocationId;
  }

  public String getBillToLocationId()
  {
    return billToLocationId;
  }

  public void setRevisionNumber(String revisionNumber)
  {
    this.revisionNumber = revisionNumber;
  }

  public String getRevisionNumber()
  {
    return revisionNumber;
  }

  public void setInterfaceHeaderId(String interfaceHeaderId)
  {
    this.interfaceHeaderId = interfaceHeaderId;
  }

  public String getInterfaceHeaderId()
  {
    return interfaceHeaderId;
  }

  public void setCipAcePoNumber(String cipAcePoNumber)
  {
    this.cipAcePoNumber = cipAcePoNumber;
  }

  public String getCipAcePoNumber()
  {
    return cipAcePoNumber;
  }

  public void setOraclePoNumber(String oraclePoNumber)
  {
    this.oraclePoNumber = oraclePoNumber;
  }

  public String getOraclePoNumber()
  {
    return oraclePoNumber;
  }

  public void setRate(BigDecimal rate)
  {
    this.rate = rate;
  }

  public BigDecimal getRate()
  {
    return rate;
  }

  public void setOrgName(String orgName)
  {
    this.orgName = orgName;
  }

  public String getOrgName()
  {
    return orgName;
  }

  public boolean isImportFailed()
  {
    return importFailed;
  }

  public void setOraclePoHeaderId(String oraclePoHeaderId)
  {
    this.oraclePoHeaderId = oraclePoHeaderId;
  }

  public String getOraclePoHeaderId()
  {
    return oraclePoHeaderId;
  }

  public void setCipAceDatabaseName(String cipAceDatabaseName)
  {
    this.cipAceDatabaseName = cipAceDatabaseName;
  }

  public String getCipAceDatabaseName()
  {
    return cipAceDatabaseName;
  }

  public void setOracleDatabaseName(String oracleDatabaseName)
  {
    this.oracleDatabaseName = oracleDatabaseName;
  }

  public String getOracleDatabaseName()
  {
    return oracleDatabaseName;
  }

  public void setUserName(String userName)
  {
    this.userName = userName;
  }

  public String getUserName()
  {
    return userName;
  }

  public void setCipAcePoAutoId(String cipAcePoAutoId)
  {
    this.cipAcePoAutoId = cipAcePoAutoId;
  }

  public String getCipAcePoAutoId()
  {
    return cipAcePoAutoId;
  }

  public void setProjectId(String projectId)
  {
    this.projectId = projectId;
  }

  public String getProjectId()
  {
    return projectId;
  }

  public void setProjectName(String projectName)
  {
    this.projectName = projectName;
  }

  public String getProjectName()
  {
    return projectName;
  }

  public void setShipToOrganizationName(String shipToOrganizationName)
  {
    this.shipToOrganizationName = shipToOrganizationName;
  }

  public String getShipToOrganizationName()
  {
    return shipToOrganizationName;
  }

  public void setShipToOrganizationCode(String shipToOrganizationCode)
  {
    this.shipToOrganizationCode = shipToOrganizationCode;
  }

  public String getShipToOrganizationCode()
  {
    return shipToOrganizationCode;
  }

  public void setShipToLocationId(String shipToLocationId)
  {
    this.shipToLocationId = shipToLocationId;
  }

  public String getShipToLocationId()
  {
    return shipToLocationId;
  }

  public void setGlCompanyCode(String glCompanyCode)
  {
    this.glCompanyCode = glCompanyCode;
  }

  public String getGlCompanyCode()
  {
    return glCompanyCode;
  }

  public void setGlProfitCenterCode(String glProfitCenterCode)
  {
    this.glProfitCenterCode = glProfitCenterCode;
  }

  public String getGlProfitCenterCode()
  {
    return glProfitCenterCode;
  }

  public void setGlLocationCode(String glLocationCode)
  {
    this.glLocationCode = glLocationCode;
  }

  public String getGlLocationCode()
  {
    return glLocationCode;
  }

  public void setGlCostCenterCode(String glCostCenterCode)
  {
    this.glCostCenterCode = glCostCenterCode;
  }

  public String getGlCostCenterCode()
  {
    return glCostCenterCode;
  }

  public void setGlAccountNumber(String glAccountNumber)
  {
    this.glAccountNumber = glAccountNumber;
  }

  public String getGlAccountNumber()
  {
    return glAccountNumber;
  }

  public void setGlCategoryCode(String glCategoryCode)
  {
    this.glCategoryCode = glCategoryCode;
  }

  public String getGlCategoryCode()
  {
    return glCategoryCode;
  }

  public void setGlDepartmentCode(String glDepartmentCode)
  {
    this.glDepartmentCode = glDepartmentCode;
  }

  public String getGlDepartmentCode()
  {
    return glDepartmentCode;
  }

  public void setGlLocal(String glLocal)
  {
    this.glLocal = glLocal;
  }

  public String getGlLocal()
  {
    return glLocal;
  }
}
