package com.valspar.interfaces.guardsman.pos.beans;

public class ErrorBean
{
  String errorMsg;
  String record;
  String validationStep;
  String transID;
  String conLastName;
  String conFirstName;
  String qty;
  String pricingMethod;
  String pricingCode;
  String pos_file_name;
  String rtlr_erp_no;

  public void setErrorMsg(String errorMsg)
  {
    this.errorMsg = errorMsg;
  }

  public String getErrorMsg()
  {
    return errorMsg;
  }

  public void setRecord(String record)
  {
    this.record = record;
  }

  public String getRecord()
  {
    return record;
  }

  public void setValidationStep(String validationStep)
  {
    this.validationStep = validationStep;
  }

  public String getValidationStep()
  {
    return validationStep;
  }

  public void setConLastName(String conLastName)
  {
    this.conLastName = conLastName;
  }

  public String getConLastName()
  {
    return conLastName;
  }

  public void setConFirstName(String conFirstName)
  {
    this.conFirstName = conFirstName;
  }

  public String getConFirstName()
  {
    return conFirstName;
  }

  public void setQty(String qty)
  {
    this.qty = qty;
  }

  public String getQty()
  {
    return qty;
  }

  public void setPricingMethod(String pricingMethod)
  {
    this.pricingMethod = pricingMethod;
  }

  public String getPricingMethod()
  {
    return pricingMethod;
  }

  public void setTransID(String transID)
  {
    this.transID = transID;
  }

  public String getTransID()
  {
    return transID;
  }

  public void setPricingCode(String pricingCode)
  {
    this.pricingCode = pricingCode;
  }

  public String getPricingCode()
  {
    return pricingCode;
  }

  public void setPos_file_name(String pos_file_name)
  {
    this.pos_file_name = pos_file_name;
  }

  public String getPos_file_name()
  {
    return pos_file_name;
  }

  public void setRtlr_erp_no(String rtlr_erp_no)
  {
    this.rtlr_erp_no = rtlr_erp_no;
  }

  public String getRtlr_erp_no()
  {
    return rtlr_erp_no;
  }
}
