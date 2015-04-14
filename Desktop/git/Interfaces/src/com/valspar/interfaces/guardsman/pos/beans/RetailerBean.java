package com.valspar.interfaces.guardsman.pos.beans;

import com.valspar.interfaces.guardsman.pos.utility.FindErpRtlrNo;

public class RetailerBean
{
  String retailerNo;
  String storeNo;
  String samRtlrAddrId;
  String rtlrCountry;

  public RetailerBean()
  {
  }

  public String getRetailerNo()
  {
    return retailerNo;
  }

  public void setRetailerNo(String retailerNo)
  {
    this.retailerNo = FindErpRtlrNo.findErpRtlrNo(retailerNo);
  }

  public String getStoreNo()
  {
    return storeNo;
  }

  public void setStoreNo(String storeNo)
  {
    this.storeNo = storeNo;
  }

  public String getSamRtlrAddrId()
  {
    return samRtlrAddrId;
  }

  public void setSamRtlrAddrId(String samRtlrAddrId)
  {
    this.samRtlrAddrId = samRtlrAddrId;
  }

  public String getRtlrCountry()
  {
    return rtlrCountry;
  }

  public void setRtlrCountry(String rtlrCountry)
  {
    this.rtlrCountry = rtlrCountry;
  }
}
