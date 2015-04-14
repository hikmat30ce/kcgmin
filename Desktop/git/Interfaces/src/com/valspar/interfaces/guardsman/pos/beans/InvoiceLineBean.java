package com.valspar.interfaces.guardsman.pos.beans;

public class InvoiceLineBean implements Comparable
{
  //String samConSaId;
  String pricingMethod;
  String pricingDescription;
  String sales;
  String returns;
  String total;
  String storeNo;
  String storeName;
  String storeAddr;
  String storeCity;
  String itemSku;
  String erpRtlrDseq;
  

  public InvoiceLineBean()
  {
  }

  /*public String getSamConSaId()
  {
    return samConSaId;
  }

  public void setSamConSaId(String samConSaId)
  {
    this.samConSaId = samConSaId;
  }*/

  public String getPricingMethod()
  {
    return pricingMethod;
  }

  public void setPricingMethod(String pricingMethod)
  {
    this.pricingMethod = pricingMethod;
  }

  public String getPricingDescription()
  {
    return pricingDescription;
  }

  public void setPricingDescription(String pricingDescription)
  {
    this.pricingDescription = pricingDescription;
  }

  public String getSales()
  {
    return sales;
  }

  public void setSales(String sales)
  {
    this.sales = sales;
  }

  public String getReturns()
  {
    return returns;
  }

  public void setReturns(String returns)
  {
    this.returns = returns;
  }

  public String getTotal()
  {
    return total;
  }

  public void setTotal(String total)
  {
    this.total = total;
  }

  public void setStoreNo(String storeNo)
  {
    this.storeNo = storeNo;
  }

  public String getStoreNo()
  {
    return storeNo;
  }

  public void setStoreName(String storeName)
  {
    this.storeName = storeName;
  }

  public String getStoreName()
  {
    return storeName;
  }

  public void setStoreAddr(String storeAddr)
  {
    this.storeAddr = storeAddr;
  }

  public String getStoreAddr()
  {
    return storeAddr;
  }

  public void setStoreCity(String storeCity)
  {
    this.storeCity = storeCity;
  }

  public String getStoreCity()
  {
    return storeCity;
  }

  public int compareTo(Object o)
  {
    InvoiceLineBean other = (InvoiceLineBean)o;
     
    return (storeNo.compareTo(other.storeNo));
  }

  public void setItemSku(String itemSku)
  {
    this.itemSku = itemSku;
  }

  public String getItemSku()
  {
    return itemSku;
  }

  public void setErpRtlrDseq(String erpRtlrDseq)
  {
    this.erpRtlrDseq = erpRtlrDseq;
  }

  public String getErpRtlrDseq()
  {
    return erpRtlrDseq;
  }
}
