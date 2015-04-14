package com.valspar.interfaces.guardsman.pos.beans;

import com.valspar.interfaces.guardsman.pos.utility.FindErpRtlrNo;
import java.util.ArrayList;

public class SalesReceiptBean
{
  ArrayList srHeaders = new ArrayList();
  ArrayList errors = new ArrayList();
  ArrayList conSAs = new ArrayList();

  String address1;
  String address2;
  String city;
  String controlNo;
  String firstName;
  String invoiceNo;
  String lastName;
  String phoneHome;
  String phoneWork;
  String postalCode;
  String pricingMethod;
  String retailerNo;
  String retailerBillTo;
  String saNo;
  String saleDt;
  String state;
  String storeNo;
  String transId;
  String saAmt;
  String samConId;
  String samSrId;
  String samAddrId;
  String samPhoneHomeId;
  String samPhoneWorkId;
  String samRtlrAddrId;
  String samSaTypeId;
  boolean hasSale;
  boolean hasReturn;
  boolean hasUpdate;
  boolean hasCancel;
  String samEliteConSAId;
  String tab18SAType;
  String tab18ItemQty;
  String rtlrCountry;
  String saTypeId;
  String language;
  String serialNo;
  boolean hasClaim;
  String email;

  public SalesReceiptBean()
  {
  }

  public ArrayList getSrHeaders()
  {
    return srHeaders;
  }

  public ArrayList getErrors()
  {
    return errors;
  }

  public ArrayList getConSAs()
  {
    return conSAs;
  }

  public void addSrHeader(SrHeaderBean inSrHeaderBean)
  {
    this.getSrHeaders().add(inSrHeaderBean);
  }

  public String getAddress1()
  {
    return address1;
  }

  public void setAddress1(String address1)
  {
    this.address1 = address1;
  }

  public String getAddress2()
  {
    return address2;
  }

  public void setAddress2(String address2)
  {
    this.address2 = address2;
  }

  public String getCity()
  {
    return city;
  }

  public void setCity(String city)
  {
    this.city = city;
  }

  public String getControlNo()
  {
    return controlNo;
  }

  public void setControlNo(String controlNo)
  {
    this.controlNo = controlNo;
  }

  public String getFirstName()
  {
    return firstName;
  }

  public void setFirstName(String firstName)
  {
    this.firstName = firstName;
  }

  public String getInvoiceNo()
  {
    return invoiceNo;
  }

  public void setInvoiceNo(String invoiceNo)
  {
    this.invoiceNo = invoiceNo;
  }

  public String getLastName()
  {
    return lastName;
  }

  public void setLastName(String lastName)
  {
    this.lastName = lastName;
  }

  public String getPhoneHome()
  {
    return phoneHome;
  }

  public void setPhoneHome(String phoneHome)
  {
    this.phoneHome = phoneHome;
  }

  public String getPhoneWork()
  {
    return phoneWork;
  }

  public void setPhoneWork(String phoneWork)
  {
    this.phoneWork = phoneWork;
  }

  public String getPostalCode()
  {
    return postalCode;
  }

  public void setPostalCode(String postalCode)
  {
    this.postalCode = postalCode;
  }

  public String getPricingMethod()
  {
    return pricingMethod;
  }

  public void setPricingMethod(String pricingMethod)
  {
    this.pricingMethod = pricingMethod;
  }

  public String getRetailerNo()
  {
    return retailerNo;
  }

  public void setRetailerNo(String retailerId)
  {
    this.retailerNo = FindErpRtlrNo.findErpRtlrNo(retailerId);
    if (storeNo != null)
    {
      setRetailerBillTo();
    }
  }

  public String getSaNo()
  {
    return saNo;
  }

  public void setSaNo(String saNo)
  {
    this.saNo = saNo;
  }

  public String getSaleDt()
  {
    return saleDt;
  }

  public void setSaleDt(String saleDt)
  {
    this.saleDt = saleDt;
  }

  public String getState()
  {
    return state;
  }

  public void setState(String state)
  {
    this.state = state;
  }

  public String getStoreNo()
  {
    return storeNo;
  }

  public void setStoreNo(String storeNo)
  {
    this.storeNo = storeNo;
    if (retailerNo != null)
    {
      setRetailerBillTo();
    }
  }

  public void setStoreNo(String retailerId, String storeNumber)
  {
    if (retailerNo == null)
    {
      setRetailerNo(retailerId);
    }
    if (retailerNo.equals("2301820"))
    {
      this.storeNo = retailerId + '-' + storeNumber;
    }
    else
    {
      this.storeNo = storeNumber;
    }
    setRetailerBillTo();
  }

  public void setRetailerBillTo()
  {
    retailerBillTo = FindErpRtlrNo.findBillTo(retailerNo, storeNo);
  }

  public String getRetailerBillTo()
  {
    return retailerBillTo;
  }

  public String getTransId()
  {
    return transId;
  }

  public void setTransId(String transId)
  {
    this.transId = transId;
  }

  public String getSaAmt()
  {
    return saAmt;
  }

  public void setSaAmt(String saAmt)
  {
    this.saAmt = saAmt;
  }

  public String getSamConId()
  {
    return samConId;
  }

  public void setSamConId(String samConId)
  {
    this.samConId = samConId;
  }

  public String getSamSrId()
  {
    return samSrId;
  }

  public void setSamSrId(String samSrId)
  {
    this.samSrId = samSrId;
  }

  public String getSamAddrId()
  {
    return samAddrId;
  }

  public void setSamAddrId(String samAddrId)
  {
    this.samAddrId = samAddrId;
  }

  public String getSamPhoneHomeId()
  {
    return samPhoneHomeId;
  }

  public void setSamPhoneHomeId(String samPhoneHomeId)
  {
    this.samPhoneHomeId = samPhoneHomeId;
  }

  public String getSamPhoneWorkId()
  {
    return samPhoneWorkId;
  }

  public void setSamPhoneWorkId(String samPhoneWorkId)
  {
    this.samPhoneWorkId = samPhoneWorkId;
  }

  public String getSamRtlrAddrId()
  {
    return samRtlrAddrId;
  }

  public void setSamRtlrAddrId(String samRtlrAddrId)
  {
    this.samRtlrAddrId = samRtlrAddrId;
  }

  public String getSamSaTypeId()
  {
    return samSaTypeId;
  }

  public void setSamSaTypeId(String samSaTypeId)
  {
    this.samSaTypeId = samSaTypeId;
  }

  public boolean isHasSale()
  {
    return hasSale;
  }

  public void setHasSale(boolean hasSale)
  {
    this.hasSale = hasSale;
  }

  public boolean isHasReturn()
  {
    return hasReturn;
  }

  public void setHasReturn(boolean hasReturn)
  {
    this.hasReturn = hasReturn;
  }

  public boolean isHasUpdate()
  {
    return hasUpdate;
  }

  public void setHasUpdate(boolean hasUpdate)
  {
    this.hasUpdate = hasUpdate;
  }

  public boolean isHasCancel()
  {
    return hasCancel;
  }

  public void setHasCancel(boolean hasCancel)
  {
    this.hasCancel = hasCancel;
  }

  public String getSamEliteConSAId()
  {
    return samEliteConSAId;
  }

  public void setSamEliteConSAId(String samEliteConSAId)
  {
    this.samEliteConSAId = samEliteConSAId;
  }

  public String getTab18SAType()
  {
    return tab18SAType;
  }

  public void setTab18SAType(String tabSAType)
  {
    this.tab18SAType = tabSAType;
  }

  public String getTab18ItemQty()
  {
    return tab18ItemQty;
  }

  public void setTab18ItemQty(String tab19ItemQty)
  {
    this.tab18ItemQty = tab19ItemQty;
  }

  public String getRtlrCountry()
  {
    return rtlrCountry;
  }

  public void setRtlrCountry(String rtlrCountry)
  {
    this.rtlrCountry = rtlrCountry;
  }

  public void setSaTypeId(String saTypeId)
  {
    this.saTypeId = saTypeId;
  }

  public String getSaTypeId()
  {
    return saTypeId;
  }

  public void setLanguage(String language)
  {
    this.language = language;
  }

  public String getLanguage()
  {
    return language;
  }

  public void setSerialNo(String serialNo)
  {
    this.serialNo = serialNo;
  }

  public String getSerialNo()
  {
    return serialNo;
  }

  public void setHasClaim(boolean hasClaim)
  {
    this.hasClaim = hasClaim;
  }

  public boolean isHasClaim()
  {
    return hasClaim;
  }

  public void setEmail(String email)
  {
    this.email = email;
  }

  public String getEmail()
  {
    return email;
  }
}
