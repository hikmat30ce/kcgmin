package com.valspar.interfaces.regulatory.wercsorders.beans;

import com.valspar.interfaces.common.utils.*;
import java.util.*;
import org.apache.log4j.Logger;

public class OrderBean implements Cloneable
{
  private static Logger log4jLogger = Logger.getLogger(OrderBean.class);

  private String plant;
  private String customerId;
  private String productGroup;
  private String format;
  private String language;
  private String city;
  private String shipToState;
  private String zip;
  private String attentionLine;
  private String shipToCountry;
  private String numCopies;
  private String processedFlag;
  private String custOrder;
  private String quantity;
  private String uom;
  private String container;
  private String standardWeight;
  private String msdsType;
  private String destination;
  private String alias;
  private String publishedAlias;
  private String product;
  private String errorCode;
  private String shipFromCountry;
  private ArrayList addressBean;
  private String businessGroup;
  private String orderType;
  private String db;
  private String inventoryType;
  private String substitutions;
  private String alwaysSend;
  private String shipAddress;

  public String getPlant()
  {
    return CommonUtility.nvl(plant);
  }
  public void setPlant(String plant)
  {
    this.plant = plant;
  }
  public void setCustomerId(String customerId)
  {
    this.customerId = customerId;
  }
  public String getCustomerId()
  {
    return CommonUtility.nvl(customerId);
  }
  public void setProductGroup(String productGroup)
  {
    this.productGroup = productGroup;
  }
  public String getProductGroup()
  {
    return getPublishedAlias();
  }
  public void setFormat(String format)
  {
    this.format = format;
  }
  public String getFormat()
  {
    return format;
  }
  public void setLanguage(String language)
  {
    this.language = language;
  }
  public String getLanguage()
  {
    return language;
  }
  public void setAddressBean(ArrayList addressBean)
  {
    this.addressBean = addressBean;
  }
  public ArrayList getAddressBean()
  {
    return addressBean;
  }
  public void setCity(String city)
  {
    this.city = city;
  }
  public String getCity()
  {
    return CommonUtility.nvl(city);
  }
  public void setShipToState(String shipToState)
  {
    this.shipToState = shipToState;
  }
  public String getShipToState()
  {
    return CommonUtility.nvl(shipToState);
  }
  public void setZip(String zip)
  {
    this.zip = zip;
  }
  public String getZip()
  {
    return CommonUtility.nvl(zip);
  }
  public void setAttentionLine(String attentionLine)
  {
    this.attentionLine = attentionLine;
  }
  public String getAttentionLine()
  {
    return CommonUtility.nvl(attentionLine);
  }
  public void setShipToCountry(String shipToCountry)
  {
    this.shipToCountry = shipToCountry;
  }
  public String getShipToCountry()
  {
    return CommonUtility.nvl(shipToCountry);
  }
  public void setNumCopies(String numCopies)
  {
    this.numCopies = numCopies;
  }
  public String getNumCopies()
  {
    return numCopies;
  }
  public void setProcessedFlag(String processedFlag)
  {
    this.processedFlag = processedFlag;
  }
  public String getProcessedFlag()
  {
    return processedFlag;
  }
  public void setCustOrder(String custOrder)
  {
    this.custOrder = custOrder;
  }
  public String getCustOrder()
  {
    return custOrder;
  }
  public void setQuantity(String quantity)
  {
    this.quantity = quantity;
  }
  public String getQuantity()
  {
    return quantity;
  }
  public void setUom(String uom)
  {
    this.uom = uom;
  }
  public String getUom()
  {
    return uom;
  }
  public void setContainer(String container)
  {
    this.container = container;
  }
  public String getContainer()
  {
    return container;
  }
  public void setStandardWeight(String standardWeight)
  {
    this.standardWeight = standardWeight;
  }
  public String getStandardWeight()
  {
    return standardWeight;
  }
  public void setMsdsType(String msdsType)
  {
    this.msdsType = msdsType;
  }
  public String getMsdsType()
  {
    return msdsType;
  }
  public void setDestination(String destination)
  {
    this.destination = destination;
  }
  public String getDestination()
  {
    return destination;
  }
  public void setAlias(String alias)
  {
    this.alias = alias;
  }
  public String getAlias()
  {
    return alias;
  }
  public void setProduct(String product)
  {
    this.product = product;
  }
  public String getProduct()
  {
    return CommonUtility.nvl(product);
  }
  public void setErrorCode(String errorCode)
  {
    this.errorCode = errorCode;
  }
  public String getErrorCode()
  {
    return errorCode;
  }
  public void setShipFromCountry(String shipFromCountry)
  {
    this.shipFromCountry = shipFromCountry;
  }
  public String getShipFromCountry()
  {
    return CommonUtility.nvl(shipFromCountry);
  }

  public Object clone()
  {
    try
    {
      return super.clone();
    }
    catch (CloneNotSupportedException e)
    {
      log4jLogger.error(e);
    }
    return null;
  }
  public void setBusinessGroup(String businessGroup)
  {
    this.businessGroup = businessGroup;
  }
  public String getBusinessGroup()
  {
    return CommonUtility.nvl(businessGroup);
  }
  public void setOrderType(String orderType)
  {
    this.orderType = orderType;
  }
  public String getOrderType()
  {
    return orderType;
  }
  public void setDb(String db)
  {
    this.db = db;
  }
  public String getDb()
  {
    return db;
  }
  public void setInventoryType(String inventoryType)
  {
    this.inventoryType = inventoryType;
  }
  public String getInventoryType()
  {
    return CommonUtility.nvl(inventoryType);
  }

  public String getSubstitutions()
  {
    return substitutions;
  }

  public void setSubstitutions(String newSubstitutions)
  {
    substitutions = newSubstitutions;
  }

  public String getAlwaysSend()
  {
    return alwaysSend;
  }

  public void setAlwaysSend(String newAlwaysSend)
  {
    alwaysSend = newAlwaysSend;
  }

  public String getPublishedAlias()
  {
    return publishedAlias;
  }

  public void setPublishedAlias(String newPublishedAlias)
  {
    publishedAlias = newPublishedAlias;
  }

  public void setShipAddress(String shipAddress)
  {
    this.shipAddress = shipAddress;
  }

  public String getShipAddress()
  {
    return shipAddress;
  }
}
