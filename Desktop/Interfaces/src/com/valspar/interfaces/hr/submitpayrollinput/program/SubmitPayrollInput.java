package com.valspar.interfaces.hr.submitpayrollinput.program;

import au.com.bytecode.opencsv.CSVReader;
import com.sun.jersey.api.client.Client;
import com.sun.jersey.api.client.ClientResponse;
import com.sun.jersey.api.client.WebResource;
import com.sun.jersey.api.client.config.ClientConfig;
import com.sun.jersey.api.client.config.DefaultClientConfig;
import com.sun.jersey.api.client.filter.HTTPBasicAuthFilter;
import com.sun.jersey.api.json.JSONConfiguration;
import com.valspar.interfaces.common.BaseInterface;
import com.valspar.interfaces.common.api.PayrollAPI;
import com.valspar.interfaces.common.servlets.PropertiesServlet;
import com.valspar.interfaces.common.utils.NotificationUtility;
import com.valspar.interfaces.hr.submitpayrollinput.beans.IntCurrentPayPeriodBean;
import com.valspar.interfaces.hr.submitpayrollinput.beans.PayrollInputBean;
import com.valspar.interfaces.hr.submitpayrollinput.beans.PayrollInputFactory;
import com.valspar.interfaces.hr.submitpayrollinput.beans.PayrollInputNotificationBean;
import com.valspar.interfaces.hr.submitpayrollinput.parser.PayrollInputParser;
import com.valspar.interfaces.hr.submitpayrollinput.parser.PayrollInputParserFactory;
import com.valspar.workday.payroll.Validation_FaultMsg;
import com.valspar.workday.payroll.types.CustomWorktag02ObjectIDType;
import com.valspar.workday.payroll.types.CustomWorktag02ObjectType;
import com.valspar.workday.payroll.types.CustomWorktag03ObjectIDType;
import com.valspar.workday.payroll.types.CustomWorktag03ObjectType;
import com.valspar.workday.payroll.types.EarningAllObjectIDType;
import com.valspar.workday.payroll.types.EarningAllObjectType;
import com.valspar.workday.payroll.types.PayrollInputObjectType;
import com.valspar.workday.payroll.types.PayrollInputWorktagsDataType;
import com.valspar.workday.payroll.types.RunCategoryObjectIDType;
import com.valspar.workday.payroll.types.RunCategoryObjectType;
import com.valspar.workday.payroll.types.SubmitPayrollInputDataType;
import com.valspar.workday.payroll.types.SubmitPayrollInputRequestType;
import com.valspar.workday.payroll.types.SubmitPayrollInputResponseType;
import com.valspar.workday.payroll.types.WorkerObjectIDType;
import com.valspar.workday.payroll.types.WorkerObjectType;
import java.io.*;
import java.math.BigDecimal;
import java.text.*;
import java.util.*;
import org.apache.commons.lang3.*;
import org.apache.log4j.*;
import com.valspar.workday.payroll.types.*;
import com.valspar.workday.reader.WorkdayReader;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.ws.rs.core.MediaType;
import org.codehaus.jackson.JsonNode;
import org.codehaus.jackson.map.DeserializationConfig;
import org.codehaus.jackson.map.ObjectMapper;
import org.codehaus.jackson.type.TypeReference;

public class SubmitPayrollInput extends BaseInterface
{
  private static Logger log4jLogger = Logger.getLogger(SubmitPayrollInput.class);

  public SubmitPayrollInput()
  {
  }

  public void execute()
  {
    try
    {
      log4jLogger.info("SubmitPayrollInput.execute() started...");
      File[] files = new File(PropertiesServlet.getProperty("workday.payrollinputdirectory")).listFiles();

      if (files != null && files.length == 1)
      {
        this.setDeleteLogFile(true);
      }
      else
      {
        for (File file: files)
        {
          if (file.isFile())
          {
            log4jLogger.info("Calling process payroll inputs for " + file.getCanonicalPath());
            processPayrollInputs(file);
          }
        }
      }
    }
    catch (Exception e)
    {
      log4jLogger.error(e);
    }
  }

  public static File archiveFile(File file)
  {
    File archivedFile = null;
    try
    {
      DateFormat dateFormat = new SimpleDateFormat("M-dd-yyyy_k-m-s");
      StringBuilder sb = new StringBuilder();
      sb.append(PropertiesServlet.getProperty("workday.payrollinputdirectory"));
      sb.append(File.separator);
      sb.append("archive");
      sb.append(File.separator);
      sb.append(StringUtils.substringAfterLast(file.getCanonicalPath(), File.separator));
      sb.append("_processed_on_");
      sb.append(dateFormat.format(new Date()));
      archivedFile = new File(sb.toString());
      file.renameTo(archivedFile);
    }
    catch (Exception e)
    {
      log4jLogger.error(e);
    }
    return archivedFile;
  }


  public static void processPayrollInputs(File file)
  {

    PayrollInputNotificationBean payrollInputNotificationBean = new PayrollInputNotificationBean();
    try
    {
      Thread.sleep(5 * 1000);
      String originalFileName = StringUtils.substringAfterLast(file.getCanonicalPath(), File.separator);
      File archivedFile = archiveFile(file);
      payrollInputNotificationBean.setFileName(originalFileName);

      CSVReader reader = new CSVReader(new FileReader(archivedFile.getCanonicalPath()));
      List<PayrollInputBean> payrollInputBeanErrorList = new ArrayList<PayrollInputBean>();
      List<PayrollInputBean> payrollInputBeanList = new ArrayList<PayrollInputBean>();
      PayrollInputParser payrollInputParser = PayrollInputParserFactory.getPayrollInputParser(originalFileName, reader.readAll());
      IntCurrentPayPeriodBean intCurrentPayPeriodBean = buildIntCurrentPayPeriodBean(payrollInputParser.getReferenceID());

      SubmitPayrollInputRequestType submitPayrollInputRequestType = new SubmitPayrollInputRequestType();

      for(String [] inputLine: payrollInputParser.getReaderList())
      {
        PayrollInputBean payrollInputBean = PayrollInputFactory.getPayrollInputBean(originalFileName);
      
        payrollInputBean.setIntCurrentPayPeriodBean(intCurrentPayPeriodBean);
      
        payrollInputParser.parse(inputLine, payrollInputBean, payrollInputBeanErrorList);

        if (payrollInputBean.isValidFormat())
        {
          buildPayrollInput(submitPayrollInputRequestType, payrollInputBean, originalFileName);
          payrollInputBeanList.add(payrollInputBean);
        }
      }
      payrollInputNotificationBean.setRowCount(payrollInputParser.getReaderList().size());

      PayrollAPI payrollAPI = new PayrollAPI();

      if (!submitPayrollInputRequestType.getPayrollInputData().isEmpty())
      {
        callWorkDayAPI(payrollAPI, submitPayrollInputRequestType, payrollInputBeanErrorList, payrollInputBeanList, 0, payrollInputNotificationBean);
      }
      else
      {
        log4jLogger.error("There were " + payrollInputBeanErrorList.size() + " errors in the file and 0 successful rows.  We cannot call callWorkDayAPI() since there is no valid data.");
        payrollInputNotificationBean.setEndDate(new Date());
        payrollInputNotificationBean.setErrorCount(payrollInputBeanErrorList.size());
        NotificationUtility.sendPayrollNoticticationEmail(payrollInputNotificationBean, payrollInputBeanErrorList);
      }
      reader.close();
    }
    catch (Exception e)
    {
      log4jLogger.error(e);
      NotificationUtility.sendPayrollNoticticationFailureEmail(payrollInputNotificationBean, e);
    }
  }

  public static IntCurrentPayPeriodBean buildIntCurrentPayPeriodBean(String referenceID)
  {
    IntCurrentPayPeriodBean intCurrentPayPeriodBean = null;
    try
    {
      ClientConfig clientConfig = new DefaultClientConfig();
      clientConfig.getFeatures().put(JSONConfiguration.FEATURE_POJO_MAPPING, Boolean.TRUE);
      Client client = Client.create(clientConfig);
      client.addFilter(new HTTPBasicAuthFilter(PropertiesServlet.getProperty("workday.username"), WorkdayReader.readProperty(PropertiesServlet.getProperty("workday.apikey"))));
      
      WebResource webResource = client.resource("https://" + PropertiesServlet.getProperty("workday.server") + "/ccx/service/customreport2/" + PropertiesServlet.getProperty("workday.tenant") + "/integrationOwner/Int_Current_Pay_Period?format=json");
      webResource = webResource.queryParam("referenceID", referenceID);

      WebResource.Builder request = webResource.getRequestBuilder(); 
      request.accept(MediaType.APPLICATION_JSON_TYPE);
      request.type(MediaType.APPLICATION_JSON_TYPE);

      ClientResponse response = request.get(ClientResponse.class);

      if (response.getStatus() != 200)
      {
        log4jLogger.info("SubmitPayrollInput.buildIntCurrentPayPeriodBean() - Failed : HTTP error code : " + response.getStatus());
      }
      else
      {
        ObjectMapper mapper = new ObjectMapper();
        mapper.configure(DeserializationConfig.Feature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        JsonNode node = mapper.readTree(response.getEntity(String.class)).get("Report_Entry");

        List<IntCurrentPayPeriodBean> intCurrentPayPeriodBeanList = (List<IntCurrentPayPeriodBean>) mapper.readValue(node.traverse(), new TypeReference<List<IntCurrentPayPeriodBean>>(){});
        
        if (!intCurrentPayPeriodBeanList.isEmpty())
        {
          intCurrentPayPeriodBean = intCurrentPayPeriodBeanList.get(0);
        }
        
        response.close();
      }
    }
    catch (Exception e)
    {
      log4jLogger.error(e);
    }
    return intCurrentPayPeriodBean;
  }

  public static void buildPayrollInput(SubmitPayrollInputRequestType submitPayrollInputRequestType, PayrollInputBean payrollInputBean, String fileName)
  {
    SubmitPayrollInputDataType submitPayrollInputDataType = new SubmitPayrollInputDataType();
    DateFormat dateFormat = new SimpleDateFormat("yy-MM-dd_");
    submitPayrollInputDataType.setBatchID(dateFormat.format(new Date()) + fileName);

    submitPayrollInputDataType.setStartDate(payrollInputBean.getIntCurrentPayPeriodBean().getPeriodStartDate());
    submitPayrollInputDataType.setEndDate(payrollInputBean.getIntCurrentPayPeriodBean().getPeriodEndDate());

    RunCategoryObjectType runCategoryObjectType = new RunCategoryObjectType();
    RunCategoryObjectIDType runCategoryObjectIDType = new RunCategoryObjectIDType();
    runCategoryObjectIDType.setType("Run_Category_ID");
    runCategoryObjectIDType.setValue(payrollInputBean.getRunCategory());
    runCategoryObjectType.getID().add(runCategoryObjectIDType);
    submitPayrollInputDataType.getRunCategoryReference().add(runCategoryObjectType);

    WorkerObjectType workerObjectType = new WorkerObjectType();
    WorkerObjectIDType workerObjectIDType = new WorkerObjectIDType();
    workerObjectIDType.setType("Employee_ID");
    workerObjectIDType.setValue(payrollInputBean.getEmployeeId());
    workerObjectType.getID().add(workerObjectIDType);
    submitPayrollInputDataType.setWorkerReference(workerObjectType);

    EarningAllObjectType earningAllObjectType = new EarningAllObjectType();
    EarningAllObjectIDType earningAllObjectIDType = new EarningAllObjectIDType();
    earningAllObjectIDType.setType("Earning_Code");
    earningAllObjectIDType.setValue(payrollInputBean.getEarningCode());
    earningAllObjectType.getID().add(earningAllObjectIDType);
    submitPayrollInputDataType.setEarningReference(earningAllObjectType);

    if (payrollInputBean.getShift().equalsIgnoreCase("E"))
    {
      submitPayrollInputDataType.setAmount(new BigDecimal(payrollInputBean.getDollars()));
    }

    PayrollInputWorktagsDataType payrollInputWorktagsDataType = new PayrollInputWorktagsDataType();
    if (StringUtils.isNotEmpty(payrollInputBean.getCostCenterID()))
    {
      CustomWorktag03ObjectType customWorktag03ObjectType = new CustomWorktag03ObjectType();
      CustomWorktag03ObjectIDType customWorktag03ObjectIDType = new CustomWorktag03ObjectIDType();
      customWorktag03ObjectIDType.setType("Custom_Worktag_3_ID");
      customWorktag03ObjectIDType.setValue(payrollInputBean.getCostCenterID());
      customWorktag03ObjectType.getID().add(customWorktag03ObjectIDType);
      payrollInputWorktagsDataType.setCustomWorktag3Reference(customWorktag03ObjectType);
      submitPayrollInputDataType.setWorktagData(payrollInputWorktagsDataType);
    }
    if (StringUtils.isNotEmpty(payrollInputBean.getShift()))
    {
      CustomWorktag02ObjectType customWorktag02ObjectType = new CustomWorktag02ObjectType();
      CustomWorktag02ObjectIDType customWorktag02ObjectIDType = new CustomWorktag02ObjectIDType();
      customWorktag02ObjectIDType.setType("Custom_Worktag_2_ID");
      customWorktag02ObjectIDType.setValue(payrollInputBean.getShift());
      customWorktag02ObjectType.getID().add(customWorktag02ObjectIDType);
      payrollInputWorktagsDataType.setCustomWorktag2Reference(customWorktag02ObjectType);
      submitPayrollInputDataType.setWorktagData(payrollInputWorktagsDataType);
    }
    if (!StringUtils.equalsIgnoreCase(payrollInputBean.getShift(), "E"))
    {
      submitPayrollInputDataType.getAdditionalInputDetailsData().add(createAdditionalInputDetailsType("W_HRSU", payrollInputBean.getHours()));

      if (payrollInputBean.getShift() != null && payrollInputBean.getShift().startsWith("R"))
      {
        submitPayrollInputDataType.getAdditionalInputDetailsData().add(createAdditionalInputDetailsType("W_RATE", payrollInputBean.getDollars()));
      }
    }
    submitPayrollInputRequestType.getPayrollInputData().add(submitPayrollInputDataType);
  }

  public static void callWorkDayAPI(PayrollAPI payrollAPI, SubmitPayrollInputRequestType submitPayrollInputRequestType, List<PayrollInputBean> payrollInputBeanErrorList, List<PayrollInputBean> payrollInputBeanList, int errorCount, PayrollInputNotificationBean payrollInputNotificationBean)
  {
    try
    {
      SubmitPayrollInputResponseType submitPayrollInputResponseType = payrollAPI.submitPayroll(submitPayrollInputRequestType);
      List<PayrollInputObjectType> reponseList = submitPayrollInputResponseType.getPayrollInputReference();
      log4jLogger.info("SubmitPayrollInput completed successfully for " + payrollInputNotificationBean.getFileName() + ". " + reponseList.size() + " row(s) were processed.");
      payrollInputNotificationBean.setEndDate(new Date());
      payrollInputNotificationBean.setErrorCount(payrollInputBeanErrorList.size());
      NotificationUtility.sendPayrollNoticticationEmail(payrollInputNotificationBean, payrollInputBeanErrorList);
    }
    catch (Validation_FaultMsg v)
    {
      try
      {
        log4jLogger.error("Validation Error Found - Current API Error Count " + errorCount);

        String xpath = v.getFaultInfo().getValidationError().get(0).getXpath();
        Matcher matcher = Pattern.compile("(?<=\\[)\\d+(?=\\])").matcher(xpath);
        matcher.find();
        matcher.find();
        int errorPosition = Integer.parseInt(matcher.group()) - 1;
        log4jLogger.error("Validation Error Found - Current Error Position " + errorPosition);
        submitPayrollInputRequestType.getPayrollInputData().remove(errorPosition);

        PayrollInputBean payrollInputBean = payrollInputBeanList.get(errorPosition + errorCount);
        if (!v.getFaultInfo().getValidationError().isEmpty())
        {
          ValidationErrorType ve = v.getFaultInfo().getValidationError().get(0);
          payrollInputBean.setErrorMessage(ve.getDetailMessage());
          log4jLogger.error("Validation Error Found - " + ve.getDetailMessage());
        }
        payrollInputBeanErrorList.add(payrollInputBean);
        errorCount++;

        if (!submitPayrollInputRequestType.getPayrollInputData().isEmpty())
        {
          callWorkDayAPI(payrollAPI, submitPayrollInputRequestType, payrollInputBeanErrorList, payrollInputBeanList, errorCount, payrollInputNotificationBean);
        }
        else
        {
          log4jLogger.error("There were " + payrollInputBeanErrorList.size() + " errors in the file and 0 successful rows.  We cannot call callWorkDayAPI() since there is no valid data.");
          payrollInputNotificationBean.setEndDate(new Date());
          payrollInputNotificationBean.setErrorCount(payrollInputBeanErrorList.size());
          NotificationUtility.sendPayrollNoticticationEmail(payrollInputNotificationBean, payrollInputBeanErrorList);
        }
      }
      catch (Exception e1)
      {
        log4jLogger.error(e1);
        NotificationUtility.sendPayrollNoticticationFailureEmail(payrollInputNotificationBean, e1);
      }
    }
    catch (Exception e)
    {
      log4jLogger.error(e);
      NotificationUtility.sendPayrollNoticticationFailureEmail(payrollInputNotificationBean, e);
    }
  }

  public static AdditionalInputDetailsType createAdditionalInputDetailsType(String code, String value)
  {
    AdditionalInputDetailsType additionalInputDetailsType = new AdditionalInputDetailsType();
    RelatedCalculationAllObjectType relatedCalculationAllObjectType = new RelatedCalculationAllObjectType();
    RelatedCalculationAllObjectIDType relatedCalculationAllObjectIDType = new RelatedCalculationAllObjectIDType();
    relatedCalculationAllObjectIDType.setType("Workday_Related_Calculation_ID");
    relatedCalculationAllObjectIDType.setValue(code);
    additionalInputDetailsType.setInputValue(new BigDecimal(value));
    relatedCalculationAllObjectType.getID().add(relatedCalculationAllObjectIDType);
    additionalInputDetailsType.setRelatedCalculationReference(relatedCalculationAllObjectType);

    return additionalInputDetailsType;
  }
}