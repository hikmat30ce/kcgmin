package com.valspar.interfaces.regulatory.dot.beans;

import java.util.*;

public class ProductBean
{
  private String productNumber;
  private String shipMethod;
  private String iataCode;
  private String hazQty2;
  private String hazQty3;
  private String limitedQuantity;
  private String imdgPage;
  private String bulk;
  private boolean rqFlag;
  private boolean bulkFlag;
  private String shippingName;
  private String subsidaryRisk;
  private String ergCode;
  private String hazIngr1;
  private String hazIngr2;
  private String hazIngr3;
  private String hazLabel1;
  private String hazLabel2;
  private String hazLabel3;
  private ArrayList itemDbList;
  private String transCount;
  private String deleteMark;
  private String adrNumber;
  private String mfagNo;
  private String emsNo;
  private String tunnelCode;
  private String hazQty1;
  private String unNumSeq;
  private String zLabel;
  private HashMap dataCodes = new HashMap();
  private String hazardClass;
  private String unNumber;
  private String packingGroup;
  private String euUnNumber;
  private String euPackingGroup;
  private String viscosityException;
  private String container;
  private String id;
  private String euHazIngr1;
  private String euHazIngr2;
  private String euHazQty1;
  private String euHazQty2;


  public void setProductNumber(String productNumber)
  {
    this.productNumber = productNumber;
  }
  public String getProductNumber()
  {
    return productNumber;
  }
  public String getProductExtension()
  {
    if (container != null)
      return container;
    else if (productNumber.indexOf('.') > 0)
      return productNumber.substring(productNumber.lastIndexOf('.')+1, productNumber.length());
    else
      return null;
  }
  public void setShipMethod(String shipMethod)
  {
    this.shipMethod = shipMethod;
  }
  public String getShipMethod()
  {
    return shipMethod;
  }
  public void setIataCode(String iataCode)
  {
    this.iataCode = iataCode;
  }
  public String getIataCode()
  {
    return iataCode;
  }
  public void setHazQty2(String hazQty2)
  {
    this.hazQty2 = hazQty2;
  }
  public String getHazQty2()
  {
    return hazQty2;
  }
  public void setHazQty3(String hazQty3)
  {
    this.hazQty3 = hazQty3;
  }
  public String getHazQty3()
  {
    return hazQty3;
  }
  public void setLimitedQuantity(String limitedQuantity)
  {
    this.limitedQuantity = limitedQuantity;
  }
  public String getLimitedQuantity()
  {
    return limitedQuantity;
  }
  public void setImdgPage(String imdgPage)
  {
    this.imdgPage = imdgPage;
  }
  public String getImdgPage()
  {
    return imdgPage;
  }
  public void setBulk(String bulk)
  {
    this.bulk = bulk;
  }
  public String getBulk()
  {
    return bulk;
  }
  public void setRqFlag(boolean rqFlag)
  {
    this.rqFlag = rqFlag;
  }
  public boolean isRqFlag()
  {
    return rqFlag;
  }
  public void setBulkFlag(boolean bulkFlag)
  {
    this.bulkFlag = bulkFlag;
  }
  public boolean isBulkFlag()
  {
    return bulkFlag;
  }
  public void setShippingName(String shippingName)
  {
    this.shippingName = shippingName;
  }
  public String getShippingName()
  {
    return shippingName;
  }
  public void setSubsidaryRisk(String subsidaryRisk)
  {
    this.subsidaryRisk = subsidaryRisk;
  }
  public String getSubsidaryRisk()
  {
    return subsidaryRisk;
  }
  public void setErgCode(String ergCode)
  {
    this.ergCode = ergCode;
  }
  public String getErgCode()
  {
    return ergCode;
  }
  public void setHazLabel1(String hazLabel1)
  {
    this.hazLabel1 = hazLabel1;
  }
  public String getHazLabel1()
  {
    return hazLabel1;
  }
  public void setHazLabel2(String hazLabel2)
  {
    this.hazLabel2 = hazLabel2;
  }
  public String getHazLabel2()
  {
    return hazLabel2;
  }
  public void setHazLabel3(String hazLabel3)
  {
    this.hazLabel3 = hazLabel3;
  }
  public String getHazLabel3()
  {
    return hazLabel3;
  }
  public void setItemDbList(java.util.ArrayList itemDbList)
  {
    this.itemDbList = itemDbList;
  }
  public java.util.ArrayList getItemDbList()
  {
    return itemDbList;
  }
  public void setTransCount(String transCount)
  {
    this.transCount = transCount;
  }
  public String getTransCount()
  {
    return transCount;
  }
  public void setDeleteMark(String deleteMark)
  {
    this.deleteMark = deleteMark;
  }
  public String getDeleteMark()
  {
    return deleteMark;
  }
  public void setAdrNumber(String adrNumber)
  {
    this.adrNumber = adrNumber;
  }
  public String getAdrNumber()
  {
    return adrNumber;
  }
  public void setMfagNo(String mfagNo)
  {
    this.mfagNo = mfagNo;
  }
  public String getMfagNo()
  {
    return mfagNo;
  }
  public void setEmsNo(String emsNo)
  {
    this.emsNo = emsNo;
  }
  public String getEmsNo()
  {
    return emsNo;
  }
  public void setHazQty1(String hazQty1)
  {
    this.hazQty1 = hazQty1;
  }
  public String getHazQty1()
  {
    return hazQty1;
  }
  public void setUnNumSeq(String unNumSeq)
  {
    this.unNumSeq = unNumSeq;
  }
  public String getUnNumSeq()
  {
    return unNumSeq;
  }

  public String toString()
  {
    StringBuffer sb = new StringBuffer();

    Set s = getDataCodes().keySet();
    Iterator i = s.iterator();
    while (i.hasNext())
    {
      String key = (String)i.next();
      String value = (String)getDataCodes().get(key);
      sb.append(key);
      sb.append(" = ");
      sb.append(value);
      sb.append("\n");
    }

    sb.append("getProductNumber() = ");
    sb.append(getProductNumber());
    sb.append("\n");

    sb.append("getAdrNumber() = ");
    sb.append(getAdrNumber());
    sb.append("\n");

    sb.append("getBulk() = ");
    sb.append(getBulk());
    sb.append("\n");

    sb.append("getDeleteMark() = ");
    sb.append(getDeleteMark());
    sb.append("\n");

    sb.append("getEmsNo() = ");
    sb.append(getEmsNo());
    sb.append("\n");

    sb.append("getTunnelCode() = ");
    sb.append(getTunnelCode());
    sb.append("\n");

    sb.append("getErgCode() = ");
    sb.append(getErgCode());
    sb.append("\n");

    sb.append("getHazardClass() = ");
    sb.append(getHazardClass());
    sb.append("\n");

    sb.append("getHazIngr1() = ");
    sb.append(getHazIngr1());
    sb.append("\n");

    sb.append("getHazIngr2() = ");
    sb.append(getHazIngr2());
    sb.append("\n");

    sb.append("getHazIngr3() = ");
    sb.append(getHazIngr3());
    sb.append("\n");

    sb.append("getHazLabel1() = ");
    sb.append(getHazLabel1());
    sb.append("\n");

    sb.append("getHazLabel2() = ");
    sb.append(getHazLabel2());
    sb.append("\n");

    sb.append("getHazLabel3() = ");
    sb.append(getHazLabel3());
    sb.append("\n");

    sb.append("getHazQty1() = ");
    sb.append(getHazQty1());
    sb.append("\n");

    sb.append("getHazQty2() = ");
    sb.append(getHazQty2());
    sb.append("\n");

    sb.append("getHazQty3() = ");
    sb.append(getHazQty3());
    sb.append("\n");

    sb.append("getIataCode() = ");
    sb.append(getIataCode());
    sb.append("\n");

    sb.append("getImdgPage() = ");
    sb.append(getImdgPage());
    sb.append("\n");

    sb.append("getItemDbList() = ");
    sb.append(getItemDbList());
    sb.append("\n");

    sb.append("getLimitedQuantity() = ");
    sb.append(getLimitedQuantity());
    sb.append("\n");

    sb.append("getMfagNo() = ");
    sb.append(getMfagNo());
    sb.append("\n");

    sb.append("getPackingGroup() = ");
    sb.append(getPackingGroup());
    sb.append("\n");

    sb.append("getProductExtension() = ");
    sb.append(getProductExtension());
    sb.append("\n");

    sb.append("getProductNumber() = ");
    sb.append(getProductNumber());
    sb.append("\n");

    sb.append("getShipMethod() = ");
    sb.append(getShipMethod());
    sb.append("\n");

    sb.append("getShippingName() = ");
    sb.append(getShippingName());
    sb.append("\n");

    sb.append("getSubsidaryRisk() = ");
    sb.append(getSubsidaryRisk());
    sb.append("\n");

    sb.append("getTransCount() = ");
    sb.append(getTransCount());
    sb.append("\n");

    sb.append("getUnNumber() = ");
    sb.append(getUnNumber());
    sb.append("\n");

    sb.append("getUnNumSeq() = ");
    sb.append(getUnNumSeq());
    sb.append("\n");

    sb.append("getZLabel() = ");
    sb.append(getZLabel());
    sb.append("\n");

    sb.append("isBulkFlag() = ");
    sb.append(isBulkFlag());
    sb.append("\n");

    sb.append("isRqFlag() = ");
    sb.append(isRqFlag());
    sb.append("\n\n");

    return sb.toString();
  }

  public void setZLabel(String zLabel)
  {
    this.zLabel = zLabel;
  }
  public String getZLabel()
  {
    return zLabel;
  }
  public void setDataCodes(java.util.HashMap dataCodes)
  {
    this.dataCodes = dataCodes;
  }
  public java.util.HashMap getDataCodes()
  {
    return dataCodes;
  }
  public void setHazardClass(String hazardClass)
  {
    this.hazardClass = hazardClass;
  }
  public String getHazardClass()
  {
    return hazardClass;
  }
  public String getData(String fDataCode)
  {
    Object o = getDataCodes().get(fDataCode);
    if(o != null)
      return (String)o;
    else
      return null;
  }
  public void setData(String fDataCode, String fData)
  {
    getDataCodes().put(fDataCode, fData);
  }
  public void setUnNumber(String unNumber)
  {
    this.unNumber = unNumber;
  }
  public String getUnNumber()
  {
    return unNumber;
  }
  public void setPackingGroup(String packingGroup)
  {
    this.packingGroup = packingGroup;
  }
  public String getPackingGroup()
  {
    return packingGroup;
  }
  public void setEuUnNumber(String euUnNumber)
  {
    this.euUnNumber = euUnNumber;
  }
  public String getEuUnNumber()
  {
    return euUnNumber;
  }
  public void setEuPackingGroup(String euPackingGroup)
  {
    this.euPackingGroup = euPackingGroup;
  }
  public String getEuPackingGroup()
  {
    return euPackingGroup;
  }

  public String getViscosityException()
  {
    return viscosityException;
  }

  public void setViscosityException(String newViscosityException)
  {
    viscosityException = newViscosityException;
  }

  public String getHazIngr1()
  {
    return hazIngr1;
  }

  public void setHazIngr1(String newHazIngr1)
  {
    hazIngr1 = newHazIngr1;
  }

  public String getHazIngr2()
  {
    return hazIngr2;
  }

  public void setHazIngr2(String newHazIngr2)
  {
    hazIngr2 = newHazIngr2;
  }

  public String getHazIngr3()
  {
    return hazIngr3;
  }

  public void setHazIngr3(String newHazIngr3)
  {
    hazIngr3 = newHazIngr3;
  }

  public String getContainer()
  {
    return container;
  }

  public void setContainer(String container)
  {
    this.container = container;
  }

  public String getId()
  {
    return id;
  }

  public void setId(String id)
  {
    this.id = id;
  }

  public void setEuHazIngr1(String euHazIngr1)
  {
    this.euHazIngr1 = euHazIngr1;
  }

  public String getEuHazIngr1()
  {
    return euHazIngr1;
  }

  public void setEuHazIngr2(String euHazIngr2)
  {
    this.euHazIngr2 = euHazIngr2;
  }

  public String getEuHazIngr2()
  {
    return euHazIngr2;
  }

  public void setEuHazQty1(String euHazQty1)
  {
    this.euHazQty1 = euHazQty1;
  }

  public String getEuHazQty1()
  {
    return euHazQty1;
  }

  public void setEuHazQty2(String euHazQty2)
  {
    this.euHazQty2 = euHazQty2;
  }

  public String getEuHazQty2()
  {
    return euHazQty2;
  }

  public void setTunnelCode(String tunnelCode)
  {
    this.tunnelCode = tunnelCode;
  }

  public String getTunnelCode()
  {
    return tunnelCode;
  }
}
