package com.valspar.interfaces.common.api;

import com.valspar.workday.humanresources.Human_ResourcesPort;
import com.valspar.workday.humanresources.Human_ResourcesService;
import com.valspar.workday.humanresources.types.*;
import java.io.*;
import java.math.*;
import java.net.*;
import java.util.*;
import javax.xml.datatype.*;
import javax.xml.namespace.*;
import org.apache.log4j.*;

public class HumanResourcesAPI extends BaseAPI
{
  private Human_ResourcesPort humanResourcesPort;
  private List<String> employeeIDList;
  private static Logger log4jLogger = Logger.getLogger(HumanResourcesAPI.class);

  private Human_ResourcesPort getHumanResourcesPort()
  {
    if(humanResourcesPort == null)
    {
      try
      {
        String wsdlURL = "https://" + this.getWorkdayServer() + "/ccx/service/"+this.getTenant()+"/Human_Resources/v23.0?wsdl";
        Human_ResourcesService humanResourcesService = new Human_ResourcesService(new URL(wsdlURL), new QName("urn:com.workday/bsvc/Human_Resources", "Human_ResourcesService"));
        humanResourcesPort = humanResourcesService.getHuman_Resources(getSecurityFeatures());
        bindCredentials(humanResourcesPort,wsdlURL);
      }
      catch (Exception e)
      {
        log4jLogger.error("Exception in HumanResourcesAPI.getHumanResourcesPort() " + e);
      }
      return humanResourcesPort;
    }
    else
    {
      return humanResourcesPort;
    }
  }
  
  
  public List<WorkerType> getAllActiveWorkers()
  {
    List<WorkerType> workerTypeList = new ArrayList<WorkerType>();
    try
    {
      WorkerRequestCriteriaType workerRequestCriteriaType = new WorkerRequestCriteriaType();
      workerRequestCriteriaType.setExcludeInactiveWorkers(true);

      GetWorkersRequestType getWorkersRequestType = new GetWorkersRequestType();
      getWorkersRequestType.setVersion("v23.0");
      getWorkersRequestType.setRequestCriteria(workerRequestCriteriaType);

      WorkerResponseGroupType workerResponseGroupType = new WorkerResponseGroupType();
      workerResponseGroupType.setIncludeCompensation(false);
      workerResponseGroupType.setIncludeOrganizations(true);
      workerResponseGroupType.setIncludeRoles(false);
      workerResponseGroupType.setIncludePhoto(true);
      workerResponseGroupType.setIncludeEmploymentInformation(true);
      workerResponseGroupType.setIncludePersonalInformation(true);
      workerResponseGroupType.setIncludeManagementChainData(true);
      getWorkersRequestType.setResponseGroup(workerResponseGroupType);
      workerResponseGroupType.setExcludeLocationHierarchies(false);
      
      if(this.getEmployeeIDList() != null && !this.getEmployeeIDList().isEmpty())
      {
        WorkerRequestReferencesType workerRequestReferencesType = new WorkerRequestReferencesType();      
        for (String employeeID: this.getEmployeeIDList())
        { 
          WorkerObjectType workerObjectType = new WorkerObjectType();
          
          List<WorkerObjectIDType> workerObjectIDTypeList = workerObjectType.getID();
          WorkerObjectIDType workerObjectIDType = new WorkerObjectIDType();
          workerObjectIDType.setType("Employee_ID");
          workerObjectIDType.setValue(employeeID);
          workerObjectIDTypeList.add(workerObjectIDType);
          
          List criteriaList = workerRequestReferencesType.getWorkerReference();
          criteriaList.add(workerObjectType);
        }
        getWorkersRequestType.setRequestReferences(workerRequestReferencesType);
      }

      GetWorkersResponseType getWorkersResponseType = getHumanResourcesPort().getWorkers(getWorkersRequestType);
      ResponseResultsType responseResultsType = getWorkersResponseType.getResponseResults();

      ResponseFilterType responseFilterType = new ResponseFilterType();
      GregorianCalendar gregorianCalendar = new GregorianCalendar();
      XMLGregorianCalendar xmlGregorianCalendar = DatatypeFactory.newInstance().newXMLGregorianCalendar(gregorianCalendar);
      responseFilterType.setAsOfEntryDateTime(xmlGregorianCalendar);
      getWorkersRequestType.setResponseFilter(responseFilterType);

      int totalNumberOfPages = responseResultsType.getTotalPages().intValue();

      for (int i = 1; i <= totalNumberOfPages; i++)
      {
        responseFilterType.setPage(new BigDecimal(i));
        GetWorkersResponseType wrt = getHumanResourcesPort().getWorkers(getWorkersRequestType);
        workerTypeList.addAll(wrt.getResponseData().getWorker());
      }
    }
    catch (Exception e)
    {
      log4jLogger.error("Exception in HumanResourcesAPI.getAllActiveWorkers() " + e);
    }
    return workerTypeList;
  }

  public byte[] getEmployeeImage(String employeeId)
  {
    EmployeeImageGetType employeeImageGetType = new EmployeeImageGetType();
    employeeImageGetType.setVersion("v23.0");

    EmployeeReferenceType employeeReferenceType = new EmployeeReferenceType();
    employeeImageGetType.getEmployeeReference().add(employeeReferenceType);

    IDType idType = new IDType();
    idType.setSystemID("WD-EMPLID");
    idType.setValue(employeeId);

    ExternalIntegrationIDReferenceDataType externalIntegrationIDReferenceDataType = new ExternalIntegrationIDReferenceDataType();
    externalIntegrationIDReferenceDataType.setDescriptor("?");
    externalIntegrationIDReferenceDataType.setID(idType);
    employeeReferenceType.setIntegrationIDReference(externalIntegrationIDReferenceDataType);

    byte[] employeeImage = null;
    try
    {
      EmployeeImageType employeeImageType = getHumanResourcesPort().getEmployeeImage(employeeImageGetType);
      employeeImage = employeeImageType.getEmployeeImageData().getImage();
    }
    catch (Exception e)
    {
      log4jLogger.error("Exception in HumanResourcesAPI.getEmployeeImage() " + e);
    }
    return employeeImage;
  }

  public void writeImageEmployeeImageToPath(String employeeId, String outputFilePath, byte[] image)
  {
    try
    {
      BufferedOutputStream bos = new BufferedOutputStream(new FileOutputStream(outputFilePath + employeeId + ".jpg"));
      bos.write(image);
      bos.flush();
      bos.close();
    }
    catch (Exception e)
    {
      log4jLogger.error("Exception in HumanResourcesAPI.writeImageEmployeeImageToPath() " + e);
    }
  }
    
  public WorkerDataType getWorker(String employeeID)
  {
    WorkerResponseGroupType workerResponseGroupType = new WorkerResponseGroupType();
    workerResponseGroupType.setIncludeCompensation(false);
    workerResponseGroupType.setIncludeOrganizations(false);
    workerResponseGroupType.setIncludeRoles(false);
    workerResponseGroupType.setIncludePhoto(false);
    workerResponseGroupType.setIncludeEmploymentInformation(true);
    workerResponseGroupType.setIncludePersonalInformation(true);
    workerResponseGroupType.setIncludeManagementChainData(true);
    GetWorkersRequestType getWorkersRequestType = new GetWorkersRequestType();
    getWorkersRequestType.setResponseGroup(workerResponseGroupType);
    
    WorkerDataType workerDataType = null;
    try
    {
      WorkerRequestReferencesType workerRequestReferencesType = new WorkerRequestReferencesType();
      WorkerObjectType workerObjectType = new WorkerObjectType();
      WorkerObjectIDType idType = new WorkerObjectIDType();
      idType.setType("Employee_ID");
      idType.setValue(employeeID);
      workerObjectType.getID().add(idType);
      workerRequestReferencesType.getWorkerReference().add(workerObjectType);      
      getWorkersRequestType.setRequestReferences(workerRequestReferencesType);
      GetWorkersResponseType getWorkersReponseType = getHumanResourcesPort().getWorkers(getWorkersRequestType);
      WorkerType workerType = getWorkersReponseType.getResponseData().getWorker().get(0);
      if(workerType != null)
      {
        workerDataType = workerType.getWorkerData();
      }
    }
    catch(Exception e)
    {
      log4jLogger.error("Exception in HumanResourcesAPI.getWorker() " + e);
    }
    return workerDataType;
  }
  
  public void updateEmployeePersonalInfo(EmployeePersonalInfoUpdateType employeePersonalInfoUpdateType) throws Exception
  {
    getHumanResourcesPort().updateEmployeePersonalInfo(employeePersonalInfoUpdateType);
  }
  
  public String updateEmailAddress(String employeeID, String emailAddress)
  {
    try
    {
      MaintainContactInformationForPersonEventRequestType maintainContactInformationForPersonEventRequestType = new MaintainContactInformationForPersonEventRequestType();
      maintainContactInformationForPersonEventRequestType.setAddOnly(false);
      maintainContactInformationForPersonEventRequestType.setVersion("v23.0");

      ContactInformationForPersonEventDataType contactInformationForPersonEventDataType = new ContactInformationForPersonEventDataType();
      GregorianCalendar gregorianCalendar = new GregorianCalendar();
      XMLGregorianCalendar xmlGregorianCalendar = DatatypeFactory.newInstance().newXMLGregorianCalendar(gregorianCalendar);

      contactInformationForPersonEventDataType.setEffectiveDate(xmlGregorianCalendar);

      WorkerObjectType workerObjectType = new WorkerObjectType();
      WorkerObjectIDType idType = new WorkerObjectIDType();
      idType.setType("Employee_ID");
      idType.setValue(employeeID);
      workerObjectType.getID().add(idType);
     
      contactInformationForPersonEventDataType.setWorkerReference(workerObjectType);
      
      
      ContactInformationDataType contactInformationDataType = new ContactInformationDataType();

      EmailAddressInformationDataType emailAddressInformationDataType = new EmailAddressInformationDataType();
      emailAddressInformationDataType.setEmailAddress(emailAddress);
      
      CommunicationUsageTypeDataType communicationUsageTypeDataType = new CommunicationUsageTypeDataType();
      communicationUsageTypeDataType.setPrimary(true);
      CommunicationUsageTypeObjectType communicationUsageTypeObjectType = new CommunicationUsageTypeObjectType();
     
      CommunicationUsageTypeObjectIDType communicationUsageTypeObjectIDType = new CommunicationUsageTypeObjectIDType();
      communicationUsageTypeObjectIDType.setType("Communication_Usage_Type_ID");
      communicationUsageTypeObjectIDType.setValue("WORK");
      communicationUsageTypeObjectType.getID().add(communicationUsageTypeObjectIDType);
      communicationUsageTypeDataType.setTypeReference(communicationUsageTypeObjectType);

      CommunicationMethodUsageInformationDataType communicationMethodUsageInformationDataType = new CommunicationMethodUsageInformationDataType();
      communicationMethodUsageInformationDataType.setPublic(true);
      communicationMethodUsageInformationDataType.getTypeData().add(communicationUsageTypeDataType);

      emailAddressInformationDataType.getUsageData().add(communicationMethodUsageInformationDataType);

      contactInformationDataType.getEmailAddressData().add(emailAddressInformationDataType);
      contactInformationForPersonEventDataType.setWorkerContactInformationData(contactInformationDataType);
        
      maintainContactInformationForPersonEventRequestType.setMaintainContactInformationData(contactInformationForPersonEventDataType);
      
      getHumanResourcesPort().maintainContactInformation(maintainContactInformationForPersonEventRequestType);
      
    }
    catch (Exception e)
    {
      log4jLogger.error("Exception in HumanResourcesAPI.updateEmailAddress() " + e);
      return e.getMessage();
    }
    return null;
  }

  public void setHumanResourcesPort(Human_ResourcesPort humanResourcesPort)
  {
    this.humanResourcesPort = humanResourcesPort;
  }

  public void setEmployeeIDList(List<String> employeeIDList)
  {
    this.employeeIDList = employeeIDList;
  }

  public List<String> getEmployeeIDList()
  {
    return employeeIDList;
  }
}