package com.valspar.interfaces.sales.sampleopportunity.dao;

import com.sforce.soap.enterprise.sobject.Functionality_by_BU__c;
import com.sforce.soap.enterprise.sobject.Opportunity;
import com.sforce.soap.enterprise.sobject.RecordType;
import com.sforce.soap.enterprise.sobject.Sample__c;
import com.valspar.interfaces.common.beans.ConnectionAccessBean;
import com.valspar.interfaces.common.enums.DataSource;
import com.valspar.interfaces.common.utils.*;
import com.valspar.interfaces.sales.common.SFDCCommonDAO;
import com.valspar.interfaces.sales.sampleopportunity.beans.SampleParameterBean;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;
import java.text.SimpleDateFormat;
import java.util.*;
import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;

public class SampleOpportunityDAO
{
  private static Logger log4jLogger = Logger.getLogger(SampleOpportunityDAO.class);

  public SampleOpportunityDAO()
  {
  }

  public static void createSamples()
  {
    List<Sample__c> sampleList = new ArrayList<Sample__c>();
    List<Functionality_by_BU__c> businesUnitList = buildBusinesList();
    for (Functionality_by_BU__c businesUnit: businesUnitList)
    {
      List<SampleParameterBean> sampleParameterList = buildSampleParameterList(businesUnit);
      for (SampleParameterBean sampleParameterBean: sampleParameterList)
      {
        buildSamplesList(sampleParameterBean, sampleList);
      }
    }
    if(!sampleList.isEmpty())
    {
      SFDCCommonDAO.createRecords(sampleList.toArray(new Sample__c[sampleList.size()]));
    }
  }

  private static List<Functionality_by_BU__c> buildBusinesList()
  {
    StringBuilder sb = new StringBuilder();
    sb.append("select id, name, sample_record_type__C, Days_for_Sample_Sync__c ");
    sb.append("from functionality_by_bu__c where populate_sample_data__c = true ");
    Functionality_by_BU__c[] businesUnitArray = (Functionality_by_BU__c[]) SFDCCommonDAO.executeQuery(sb.toString(), Functionality_by_BU__c.class);
    List<Functionality_by_BU__c> businesUnitList = new ArrayList<Functionality_by_BU__c>(Arrays.asList(businesUnitArray));
    return businesUnitList;
  }
  
  private static RecordType[] getRecordType(String recordTypeName)
  {
    StringBuilder sb = new StringBuilder();
    sb.append("select id, name from RecordType where name = ");
    sb.append(CommonUtility.toVarchar(recordTypeName));
    return (RecordType[]) SFDCCommonDAO.executeQuery(sb.toString(), RecordType.class);
  }

  private static List<SampleParameterBean> buildSampleParameterList(Functionality_by_BU__c businessUnit)
  {
    StringBuilder sb = new StringBuilder();
    sb.append("select id, ownerId, closedate, lwr_new_product_code__c, opportunity.account.merged_shiptos__c  ");
    sb.append("from opportunity ");
    sb.append("where business_unit__c = ");
    sb.append(CommonUtility.toVarchar(businessUnit.getName()));
    sb.append(" and lwr_new_product_code__c <> '' ");
    sb.append("and opportunity.account.merged_shiptos__c <> '' ");
    sb.append("and (CloseDate = LAST_N_DAYS:");
    sb.append((businessUnit.getDays_for_Sample_Sync__c()).intValue());
    sb.append(" or CloseDate >TODAY) ");
    sb.append(" order by opportunity.account.merged_shiptos__c, lwr_new_product_code__c ");
    
    RecordType[] recordType = getRecordType(businessUnit.getSample_Record_Type__c());

    Opportunity[] opportunityArray = (Opportunity[]) SFDCCommonDAO.executeQuery(sb.toString(), Opportunity.class);
    List<Opportunity> opportunityList = new ArrayList<Opportunity>(Arrays.asList(opportunityArray));
    List<SampleParameterBean> sampleParametList = new ArrayList<SampleParameterBean>();
    for (Opportunity opportunity: opportunityList)
    {
      String mergedShipTo = opportunity.getAccount().getMerged_ShipTos__c();
      if (mergedShipTo.contains(" ; "))
      {
        for (String shipTo: StringUtils.split(mergedShipTo, " ; "))
        {
          SampleParameterBean sampleParameterBean = new SampleParameterBean();
          sampleParameterBean.setId(opportunity.getId());
          sampleParameterBean.setCloseDate(getAnalyticsDateFormat(opportunity.getCloseDate()));
          sampleParameterBean.setProduct(opportunity.getLWR_New_Product_Code__c());
          sampleParameterBean.setOwnerId(opportunity.getOwnerId());
          sampleParameterBean.setShipTo(shipTo);
          if(recordType != null && recordType.length >0)
          {
            sampleParameterBean.setRecordType(recordType[0].getName());
            sampleParameterBean.setRecordTypeId(recordType[0].getId());
          }
          sampleParametList.add(sampleParameterBean);
        }
      }
      else
      {
        SampleParameterBean sampleParameterBean = new SampleParameterBean();
        sampleParameterBean.setId(opportunity.getId());
        sampleParameterBean.setCloseDate(getAnalyticsDateFormat(opportunity.getCloseDate()));
        sampleParameterBean.setProduct(opportunity.getLWR_New_Product_Code__c());
        sampleParameterBean.setOwnerId(opportunity.getOwnerId());
        sampleParameterBean.setShipTo(mergedShipTo);
        if(recordType != null && recordType.length >0)
        {
          sampleParameterBean.setRecordType(recordType[0].getName());
          sampleParameterBean.setRecordTypeId(recordType[0].getId());
        }
        sampleParametList.add(sampleParameterBean);
      }
    }
    return sampleParametList;
  }

   private static void buildSamplesList(SampleParameterBean sampleParameterBean, List<Sample__c> sampleList )
  {
    Connection analyticsConn = ConnectionAccessBean.getConnection(DataSource.ANALYTICS);
    Statement st = null;
    ResultSet rs = null;
    try
    {
      StringBuilder sb = new StringBuilder();
      sb.append("SELECT \"Customer Ship To Location\".\"Ship-To Customer Number\" ship_to_cust_no, ");
      sb.append(" \"Customer Ship To Location\".\"Ship To Customer GL Class Name\" gl_class, ");
      sb.append(" \"- Product Attributes\".\"Product Number\" product_no, ");
      sb.append(" \"- Product Attributes\".Container container, ");
      sb.append(" \"- Product Attributes\".\"Product Description\" product_desc, ");
      sb.append(" \"- Sales Attributes\".\"Sales Class Code and Sales Class Description\" sales_class, ");
      sb.append(" \"- Other Attributes\".\"Product Category\" product_category, ");
      sb.append(" \"- Other Attributes\".\"Product Type\" product_type, ");
      sb.append(" \"Pick Line Details\".\"Sales Order Number\" order_no, ");
      sb.append(" \"Pick Line Details\".\"Sales Order Line Number\" line_no, ");
      sb.append(" \"Pick Line Details\".\"Lot Number\" lot_no, ");
      sb.append(" \"Pick Line Details\".\"Sales UOM Code\" uom, ");
      sb.append(" Time.\"Invoice Actual Ship Date\" ship_date, ");
      sb.append(" ROUND (\"Fact - Sales Invoice Lines\".\"Invoiced List Amount\" / \"Fact - Sales Pick Lines\".\"Shipped Quantity\", 5) unit_price, ");
      sb.append(" \"Fact - Sales Pick Lines\".\"Shipped Quantity\" shipped_qty, ");
      sb.append(" \"Fact - Sales Invoice Lines\".\"Invoiced List Amount\" extended_Value ");
      sb.append(" FROM \"Sales - Pick Lines\" ");
      sb.append("WHERE \"- Product Attributes\".\"Product Number\" =  ");
      sb.append(CommonUtility.toVarchar(sampleParameterBean.getProduct()));
      sb.append(" AND \"Customer Ship To Location\".\"Ship-To Customer Number\" = ");
      sb.append(CommonUtility.toVarchar(sampleParameterBean.getShipTo()));
      sb.append(" AND \"Fact - Sales Pick Lines\".\"Shipped Quantity\" <> 0 ");
      sb.append("AND Time.\"Invoice Actual Ship Date\" <= TIMESTAMP  ");
      sb.append(CommonUtility.toVarchar(sampleParameterBean.getCloseDate())); 

      st = analyticsConn.createStatement();
      rs = st.executeQuery(sb.toString());

      while (rs.next())
      {
        if (!isOrderLineExist(rs.getString("order_no"), rs.getString("line_no"), rs.getString("lot_no")))
        {
          Sample__c sample = new Sample__c();
          sample.setOpportunity__c(sampleParameterBean.getId());
          sample.setOwnerId(sampleParameterBean.getOwnerId());
          sample.setCust_GL_Class__c(rs.getString("gl_class"));
          sample.setOrder_Number__c(rs.getString("order_no"));
          sample.setOrder_Line_Number__c(rs.getString("line_no"));
          sample.setProduct_Code__c(rs.getString("product_no"));
          sample.setQty_Shipped__c(rs.getDouble("shipped_qty"));
          sample.setUOM__c(rs.getString("uom"));
          if (rs.getDate("ship_date") != null)
          {
            Calendar shipDate = Calendar.getInstance();
            shipDate.setTime(rs.getDate("ship_date"));
            sample.setShipped_Date__c(shipDate);
          }
          sample.setLot_Number__c(rs.getString("lot_no"));
          sample.setProduct_Sales_Class__c(rs.getString("sales_class"));
          sample.setUnit_Price__c(rs.getDouble("unit_price"));
          sample.setExtended_Value__c(rs.getDouble("extended_Value"));
          sample.setItem_Description__c(rs.getString("product_desc"));
          sample.setContainer__c(rs.getString("container"));
          sample.setProduct_Category11i__c(rs.getString("product_category"));
          sample.setProduct_Type_11i__c(rs.getString("product_type"));
          sample.setRecordTypeId(sampleParameterBean.getRecordTypeId());
          sampleList.add(sample);
        }
      }
    }
    catch (Exception e)
    {
      log4jLogger.error(e);
    }
    finally
    {
      JDBCUtil.close(st, rs);
      JDBCUtil.close(analyticsConn);
    }
  }

  private static boolean isOrderLineExist(String orderNo, String lineNo, String lotNo)
  {
    StringBuilder sb = new StringBuilder();
    sb.append("select order_number__c from sample__c where order_number__c = "); //'5680837'
    sb.append(CommonUtility.toVarchar(orderNo));
    sb.append(" and order_line_number__c = "); //'2'
    sb.append(CommonUtility.toVarchar(lineNo));
    sb.append(" and Lot_Number__c = "); //'2'
    sb.append(CommonUtility.toVarchar(lotNo));
    sb.append(" limit 1 ");
    Sample__c[] existSampleLine = ((Sample__c[]) SFDCCommonDAO.executeQuery(sb.toString(), Sample__c.class));
    if(existSampleLine != null && existSampleLine.length >0 )
    {
      return true;
    }
    else
    {
      return false;
    }
  }
  
  private static String getAnalyticsDateFormat(Calendar cal)
  {
    Date d1 = cal.getTime();             
    SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
    return sdf.format(d1) + " 00:00:00";            
  }
}
