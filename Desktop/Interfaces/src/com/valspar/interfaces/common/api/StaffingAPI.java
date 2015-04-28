package com.valspar.interfaces.common.api;

import com.valspar.workday.staffing.StaffingPort;
import com.valspar.workday.staffing.StaffingService;
import com.valspar.workday.staffing.types.DocumentCategoryAllObjectIDType;
import com.valspar.workday.staffing.types.DocumentCategoryAllObjectType;
import com.valspar.workday.staffing.types.PutWorkerDocumentRequestType;
import com.valspar.workday.staffing.types.WorkerDocumentDataType;
import com.valspar.workday.staffing.types.WorkerObjectIDType;
import com.valspar.workday.staffing.types.WorkerObjectType;
import java.net.URL;
import javax.xml.namespace.QName;
import org.apache.log4j.Logger;

public class StaffingAPI extends BaseAPI
{
  private StaffingPort staffingPort;
  private static Logger log4jLogger = Logger.getLogger(StaffingAPI.class);

  public StaffingAPI()
  {
  }

  public void putWorkerDocument(String employeeID,String fileName,String documentCategeory, byte[] documentBytes)
  {
    try
    {
      PutWorkerDocumentRequestType putWorkerDocumentRequestType = new PutWorkerDocumentRequestType();
      putWorkerDocumentRequestType.setVersion("v23.0");
      putWorkerDocumentRequestType.setAddOnly(true);
      WorkerDocumentDataType workerDocumentDataType = new WorkerDocumentDataType();

      WorkerObjectType workerObjectType = new WorkerObjectType();
      WorkerObjectIDType idType = new WorkerObjectIDType();
      idType.setType("Employee_ID");
      idType.setValue(employeeID);
      workerObjectType.getID().add(idType);

      workerDocumentDataType.setWorkerReference(workerObjectType);
      workerDocumentDataType.setFile(documentBytes);
      workerDocumentDataType.setFilename(fileName);

      DocumentCategoryAllObjectType documentCategoryAllObjectType = new DocumentCategoryAllObjectType();
      DocumentCategoryAllObjectIDType documentCategoryAllObjectIDType = new DocumentCategoryAllObjectIDType();
      documentCategoryAllObjectIDType.setType("Document_Category_ID");
      documentCategoryAllObjectIDType.setValue(documentCategeory);
      documentCategoryAllObjectType.getID().add(documentCategoryAllObjectIDType);

      workerDocumentDataType.setDocumentCategoryReference(documentCategoryAllObjectType);
     
      putWorkerDocumentRequestType.setWorkerDocumentData(workerDocumentDataType);
      getStaffingPort().putWorkerDocument(putWorkerDocumentRequestType);
    }
    catch (Exception e)
    {
      log4jLogger.error("Exception in StaffingAPI.putWorkerDocument() " + e);
    }

  }

  private StaffingPort getStaffingPort()
  {
    if (staffingPort == null)
    {
      try
      {
        String wsdlURL = "https://" + this.getWorkdayServer() + "/ccx/service/" + this.getTenant() + "/Staffing/v23.0?wsdl";
        StaffingService staffingService = new StaffingService(new URL(wsdlURL), new QName("urn:com.workday/bsvc/Staffing", "StaffingService"));
        staffingPort = staffingService.getStaffing(getSecurityFeatures());
        bindCredentials(staffingPort,wsdlURL);
      }
      catch (Exception e)
      {
        log4jLogger.error("Exception in StaffingAPI.getStaffingPort() " + e);
      }
      return staffingPort;
    }
    else
    {
      return staffingPort;
    }
  }
}
