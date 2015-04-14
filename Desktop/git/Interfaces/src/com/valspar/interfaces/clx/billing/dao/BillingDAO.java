package com.valspar.interfaces.clx.billing.dao;

import com.valspar.interfaces.clx.billing.beans.BillingBean;
import com.valspar.interfaces.common.enums.DataSource;
import com.valspar.interfaces.common.hibernate.HibernateUtil;
import com.valspar.interfaces.common.utils.*;
import java.io.*;
import java.util.*;
import org.hibernate.*;
import java.math.BigDecimal;
import org.apache.commons.lang3.StringUtils;
import org.apache.log4j.Logger;

public class BillingDAO
{
  private static Logger log4jLogger = Logger.getLogger(BillingDAO.class);

  public BillingDAO()
  {
  }

  public static List<BillingBean> buildBillingBeanList(String pathAndFileName, String orgFileName, String user)
  {
    List<BillingBean> billingBeanList = new ArrayList<BillingBean>();
    EnhancedCSVReader enhancedCSVReader = null;
    BufferedReader br = null;
    BufferedInputStream bis = null;
    try
    {
      bis = new BufferedInputStream(new FileInputStream(pathAndFileName));
      br = new BufferedReader(new InputStreamReader(bis));
      enhancedCSVReader = new EnhancedCSVReader(br, '|');
      enhancedCSVReader.setThrowExceptionForColumnNotFound(true);
      BigDecimal userId = new BigDecimal(user);
      while (enhancedCSVReader.next())
      {
        String[] currentRow = enhancedCSVReader.getCurrentRow();
        BillingBean billingBean = new BillingBean();
        billingBean.setDirection(currentRow[0]);
        billingBean.setTmsShipmentId(currentRow[1]);
        billingBean.setFreightAmount(new BigDecimal(currentRow[2]));
        billingBean.setCurrency(currentRow[3]);
        billingBean.setShipFromWhse(currentRow[4]);
        billingBean.setShipToWhse(currentRow[5]);
        billingBean.setCustVendNumber(currentRow[6]);
        billingBean.setCustVendSiteNumber(currentRow[7]);
        billingBean.setCustVendName(currentRow[8]);
        billingBean.setShipmentNumber(currentRow[9]);
        billingBean.setOrigName(currentRow[10]);
        billingBean.setOrigCity(currentRow[11]);
        billingBean.setOrigState(currentRow[12]);
        billingBean.setOrigZip(currentRow[13]);
        billingBean.setDestName(currentRow[14]);
        billingBean.setDestCity(currentRow[15]);
        billingBean.setDestState(currentRow[16]);
        billingBean.setDestZip(currentRow[17]);
        billingBean.setGlAccount(currentRow[18]);
        billingBean.setGlSource(currentRow[19]);
        billingBean.setDocumentNumber(currentRow[20]);
        billingBean.setScac(currentRow[21]);
        billingBean.setProNumber(currentRow[22]);
        billingBean.setMasterTrip(currentRow[23]);
        if (StringUtils.isNotEmpty(currentRow[24]))
        {
          billingBean.setDateShipped(CommonUtility.getFormattedDate("MM/dd/yyyy", currentRow[24]));
        }
        if (StringUtils.isNotEmpty(currentRow[25]))
        {
          billingBean.setDateDelivered(CommonUtility.getFormattedDate("MM/dd/yyyy",currentRow[25]));
        }
        billingBean.setServiceType(currentRow[26]);
        if (StringUtils.isNotEmpty(currentRow[27]))
        {
          billingBean.setTransactionDate(CommonUtility.getFormattedDate("MM/dd/yyyy",currentRow[27]));
        }
        if (StringUtils.isNotEmpty(currentRow[28]))
        {
          billingBean.setTaxReference1(currentRow[28]);
          billingBean.setTaxReference1Type("GST");
        }
        if (StringUtils.isNotEmpty(currentRow[29]))
        {
          billingBean.setTaxReference2(currentRow[29]);
          billingBean.setTaxReference2Type("HST");
        }
        if (StringUtils.isNotEmpty(currentRow[30]))
        {
          billingBean.setTaxReference3(currentRow[30]);
          billingBean.setTaxReference3Type("PST");
        }
        if (StringUtils.isNotEmpty(currentRow[31]))
        {
          billingBean.setTaxReference4(currentRow[31]);
          billingBean.setTaxReference4Type("QST");
        }
        billingBean.setCreationDate(CommonUtility.getFormattedDate("dd-MM-yyyy HH:mm:ss", new Date()));
        billingBean.setLastUpdateDate(CommonUtility.getFormattedDate("dd-MM-yyyy HH:mm:ss", new Date()));
        billingBean.setCreatedBy(userId);
        billingBean.setLastUpdatedBy(userId);
        billingBean.setFileName(orgFileName);
        billingBeanList.add(billingBean);
      }
    }
    catch (Exception e)
    {
      log4jLogger.error(e);
    }
    finally
    {
      try
      {
        bis.close();
        br.close();
        enhancedCSVReader.close();
      }
      catch (Exception e)
      {
        log4jLogger.error(e);
      }
    }
    return billingBeanList;
  }

  public static void saveBillingBeanList(List<BillingBean> billingBeanList, DataSource ds) throws Exception
  {
    int recCount = 0;
    Session session = HibernateUtil.getHibernateSession(ds);
    Transaction transaction = session.beginTransaction();
    try
    {
      for (BillingBean billingBean: billingBeanList)
      {
        session.save(billingBean);
        if (++recCount % 20 == 0)
        {
          session.flush();
          session.clear();
        }
      }
      transaction.commit();
    }
    catch (Exception e)
    {
      transaction.rollback();
      log4jLogger.error(e);
    }
    finally
    {
      session.close();
    }
  }
}
