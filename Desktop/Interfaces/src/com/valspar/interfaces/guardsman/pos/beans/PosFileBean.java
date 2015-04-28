package com.valspar.interfaces.guardsman.pos.beans;

import java.util.ArrayList;
import java.util.HashMap;
import oracle.jdbc.*;

public class PosFileBean
{
  String fileName;
  String invoicePath;
  String errorPath;
  ArrayList salesReceiptMap = new ArrayList();
  ArrayList errors = new ArrayList();
  OracleConnection connection;
  HashMap retailerMap = new HashMap();
  HashMap uniqueRetailerMap = new HashMap();
  HashMap erpErrorEmailMap = new HashMap();
  HashMap erpInvoiceEmailMap = new HashMap();
  ArrayList validSaTypes = new ArrayList();
  int srLastId;
  int srNextId;
  int srItemLastId;
  int srItemNextId;
  int conLastId;
  int conNextId;
  int conPhoneLastId;
  int conPhoneNextId;
  int conSALastId;
  int conSANextId;
  int conAddrLastId;
  int conAddrNextId;
  HashMap stateIdMap = new HashMap();
  int updateSrItemNextId;
  int updateSrItemLastId;
  int returnSrItemNextId;
  int returnSrItemLastId;
  ArrayList adminMessages = new ArrayList();
  String emailAddr;
  boolean retailerActive;
  String retailerName;
  String fileFormat;
  String invoiceEmailAddr;
  ArrayList invoiceItems = new ArrayList();
  boolean autoInvoice;
  HashMap pricingCodeMap = new HashMap();
  boolean printPlans;
  boolean manualReturns;
  boolean invoiceByStore;
  ArrayList manualRtnItems = new ArrayList();
  ArrayList planPrintingItems = new ArrayList();
  boolean fileHeader;
  String  fileRunDt;
  String  fileExtractStartDt;
  String  fileExtractEndDt;
  String  posFhId;
  String  rtlrFtpId;
  String  autoInvoiceToJba;
  String  tallyDisc;
  boolean sendInvoiceDetailRpt;

  public PosFileBean()
  {

  }

  public String getFileName()
  {
    return fileName;
  }

  public void setFileName(String fileName)
  {
    this.fileName = fileName;
  }

  public ArrayList getSalesReceipts()
  {
    return salesReceiptMap;
  }

  public void setSalesReceipts(ArrayList salesReceipts)
  {
    this.salesReceiptMap = salesReceipts;
  }

  public ArrayList getErrors()
  {
    return errors;
  }

  public void setErrors(ArrayList errors)
  {
    this.errors = errors;
  }

  public OracleConnection getConnection()
  {
    return connection;
  }

  public void setConnection(OracleConnection connection)
  {
    this.connection = connection;
  }

  public HashMap getRetailerMap()
  {
    return retailerMap;
  }

  public void setRetailerMap(HashMap retailerMap)
  {
    this.retailerMap = retailerMap;
  }

  public HashMap getUniqueRetailerMap()
  {
    return uniqueRetailerMap;
  }

  public void setUniqueRetailerMap(HashMap uniqueRetailerMap)
  {
    this.uniqueRetailerMap = uniqueRetailerMap;
  }

  public ArrayList getValidSaTypes()
  {
    return validSaTypes;
  }

  public void setValidSaTypes(ArrayList validSaTypes)
  {
    this.validSaTypes = validSaTypes;
  }

  public int getSrLastId()
  {
    return srLastId;
  }

  public void setSrLastId(int srLastId)
  {
    this.srLastId = srLastId;
  }

  public int getSrNextId()
  {
    return srNextId;
  }

  public void setSrNextId(int srNextId)
  {
    this.srNextId = srNextId;
  }

  public int getSrItemLastId()
  {
    return srItemLastId;
  }

  public void setSrItemLastId(int srItemLastId)
  {
    this.srItemLastId = srItemLastId;
  }

  public int getSrItemNextId()
  {
    return srItemNextId;
  }

  public void setSrItemNextId(int srItemNextId)
  {
    this.srItemNextId = srItemNextId;
  }

  public int getConLastId()
  {
    return conLastId;
  }

  public void setConLastId(int conLastId)
  {
    this.conLastId = conLastId;
  }

  public int getConNextId()
  {
    return conNextId;
  }

  public void setConNextId(int conNextId)
  {
    this.conNextId = conNextId;
  }

  public int getConPhoneLastId()
  {
    return conPhoneLastId;
  }

  public void setConPhoneLastId(int conPhoneLastId)
  {
    this.conPhoneLastId = conPhoneLastId;
  }

  public int getConPhoneNextId()
  {
    return conPhoneNextId;
  }

  public void setConPhoneNextId(int conPhoneNextId)
  {
    this.conPhoneNextId = conPhoneNextId;
  }

  public int getConSALastId()
  {
    return conSALastId;
  }

  public void setConSALastId(int conSALastId)
  {
    this.conSALastId = conSALastId;
  }

  public int getConSANextId()
  {
    return conSANextId;
  }

  public void setConSANextId(int conSANextId)
  {
    this.conSANextId = conSANextId;
  }

  public int getConAddrLastId()
  {
    return conAddrLastId;
  }

  public void setConAddrLastId(int conAddrLastId)
  {
    this.conAddrLastId = conAddrLastId;
  }

  public int getConAddrNextId()
  {
    return conAddrNextId;
  }

  public void setConAddrNextId(int conAddrNextId)
  {
    this.conAddrNextId = conAddrNextId;
  }

  public HashMap getStateIdMap()
  {
    return stateIdMap;
  }

  public void setStateIdMap(HashMap stateIdMap)
  {
    this.stateIdMap = stateIdMap;
  }

  public int getUpdateSrItemNextId()
  {
    return updateSrItemNextId;
  }

  public void setUpdateSrItemNextId(int updateSrItemNextId)
  {
    this.updateSrItemNextId = updateSrItemNextId;
  }

  public int getUpdateSrItemLastId()
  {
    return updateSrItemLastId;
  }

  public void setUpdateSrItemLastId(int updateSrItemLastId)
  {
    this.updateSrItemLastId = updateSrItemLastId;
  }

  public int getReturnSrItemNextId()
  {
    return returnSrItemNextId;
  }

  public void setReturnSrItemNextId(int returnSrItemNextId)
  {
    this.returnSrItemNextId = returnSrItemNextId;
  }

  public int getReturnSrItemLastId()
  {
    return returnSrItemLastId;
  }

  public void setReturnSrItemLastId(int returnSrItemLastId)
  {
    this.returnSrItemLastId = returnSrItemLastId;
  }

  public ArrayList getAdminMessages()
  {
    return adminMessages;
  }

  public void setAdminMessages(ArrayList adminMessages)
  {
    this.adminMessages = adminMessages;
  }

  public String getEmailAddr()
  {
    return emailAddr;
  }

  public void setEmailAddr(String emailAddr)
  {
    this.emailAddr = emailAddr;
  }

  public boolean isRetailerActive()
  {
    return retailerActive;
  }

  public void setRetailerActive(boolean retailerActive)
  {
    this.retailerActive = retailerActive;
  }

  public String getRetailerName()
  {
    return retailerName;
  }

  public void setRetailerName(String retailerName)
  {
    this.retailerName = retailerName;
  }

  public String getFileFormat()
  {
    return fileFormat;
  }

  public void setFileFormat(String fileFormat)
  {
    this.fileFormat = fileFormat;
  }

  public String getInvoiceEmailAddr()
  {
    return invoiceEmailAddr;
  }

  public void setInvoiceEmailAddr(String invoiceEmailAddr)
  {
    this.invoiceEmailAddr = invoiceEmailAddr;
  }

  public ArrayList getInvoiceItems()
  {
    return invoiceItems;
  }

  public void setInvoiceItems(ArrayList invoiceItems)
  {
    this.invoiceItems = invoiceItems;
  }

  public boolean isAutoInvoice()
  {
    return autoInvoice;
  }

  public void setAutoInvoice(boolean autoInvoice)
  {
    this.autoInvoice = autoInvoice;
  }

  public HashMap getPricingCodeMap()
  {
    return pricingCodeMap;
  }

  public void setPricingCodeMap(HashMap pricingCodeMap)
  {
    this.pricingCodeMap = pricingCodeMap;
  }


  public boolean isPrintPlans()
  {
    return printPlans;
  }

  public void setPrintPlans(boolean printPlans)
  {
    this.printPlans = printPlans;
  }

  public void setManualReturns(boolean manualReturns)
  {
    this.manualReturns = manualReturns;
  }

  public boolean isManualReturns()
  {
    return manualReturns;
  }

  public void setManualRtnItems(ArrayList manualRtnItems)
  {
    this.manualRtnItems = manualRtnItems;
  }

  public ArrayList getManualRtnItems()
  {
    return manualRtnItems;
  }

  public void setPlanPrintingItems(ArrayList planPrintingItems)
  {
    this.planPrintingItems = planPrintingItems;
  }

  public ArrayList getPlanPrintingItems()
  {
    return planPrintingItems;
  }

  public void setInvoiceByStore(boolean invoiceByStore)
  {
    this.invoiceByStore = invoiceByStore;
  }

  public boolean isInvoiceByStore()
  {
    return invoiceByStore;
  }

  public void setFileRunDt(String fileRunDt)
  {
    this.fileRunDt = fileRunDt;
  }

  public String getFileRunDt()
  {
    return fileRunDt;
  }

  public void setFileExtractStartDt(String fileExtractStartDt)
  {
    this.fileExtractStartDt = fileExtractStartDt;
  }

  public String getFileExtractStartDt()
  {
    return fileExtractStartDt;
  }

  public void setFileExtractEndDt(String fileExtractEndDt)
  {
    this.fileExtractEndDt = fileExtractEndDt;
  }

  public String getFileExtractEndDt()
  {
    return fileExtractEndDt;
  }

  public void setPosFhId(String posFhId)
  {
    this.posFhId = posFhId;
  }

  public String getPosFhId()
  {
    return posFhId;
  }

  public void setFileHeader(boolean fileHeader)
  {
    this.fileHeader = fileHeader;
  }

  public boolean isFileHeader()
  {
    return fileHeader;
  }

  public void setRtlrFtpId(String rtlrFtpId)
  {
    this.rtlrFtpId = rtlrFtpId;
  }

  public String getRtlrFtpId()
  {
    return rtlrFtpId;
  }

  public void setAutoInvoiceToJba(String autoInvoiceToJba)
  {
    this.autoInvoiceToJba = autoInvoiceToJba;
  }

  public String getAutoInvoiceToJba()
  {
    return autoInvoiceToJba;
  }

  public void setTallyDisc(String tallyDisc)
  {
    this.tallyDisc = tallyDisc;
  }

  public String getTallyDisc()
  {
    return tallyDisc;
  }

  public HashMap getErpErrorEmailMap()
  {
    return erpErrorEmailMap;
  }

  public void setErpErrorEmailMap(HashMap erpErrorEmailMap)
  {
    this.erpErrorEmailMap = erpErrorEmailMap;
  }

  public HashMap getErpInvoiceEmailMap()
  {
    return erpInvoiceEmailMap;
  }

  public void setErpInvoiceEmailMap(HashMap erpInvoiceEmailMap)
  {
    this.erpInvoiceEmailMap = erpInvoiceEmailMap;
  }

  public void setSendInvoiceDetailRpt(boolean sendInvoiceDetailRpt)
  {
    this.sendInvoiceDetailRpt = sendInvoiceDetailRpt;
  }

  public boolean isSendInvoiceDetailRpt()
  {
    return sendInvoiceDetailRpt;
  }

  public void setInvoicePath(String invoicePath)
  {
    this.invoicePath = invoicePath;
  }

  public String getInvoicePath()
  {
    return invoicePath;
  }

  public void setErrorPath(String errorPath)
  {
    this.errorPath = errorPath;
  }

  public String getErrorPath()
  {
    return errorPath;
  }
}
