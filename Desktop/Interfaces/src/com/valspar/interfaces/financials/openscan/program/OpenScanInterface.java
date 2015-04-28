package com.valspar.interfaces.financials.openscan.program;

import com.valspar.interfaces.common.BaseInterface;
import com.valspar.interfaces.common.beans.ConnectionAccessBean;
import com.valspar.interfaces.common.enums.DataSource;
import com.valspar.interfaces.common.servlets.PropertiesServlet;
import com.valspar.interfaces.common.utils.*;
import com.valspar.interfaces.financials.openscan.beans.HSBCImageFileBean;
import java.io.File;
import java.sql.ResultSet;
import java.text.SimpleDateFormat;
import java.util.*;
import oracle.jdbc.*;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.net.ftp.FTPFile;
import org.apache.log4j.Logger;

public class OpenScanInterface extends BaseInterface
{
  private static Logger log4jLogger = Logger.getLogger(OpenScanInterface.class);
  private static final String ediFTPServer = PropertiesServlet.getProperty("openscan.ediFTPServer");
  private static final String localRootDirectory = PropertiesServlet.getProperty("openscan.localRootDirectory");
  private static final String openScanFTPServer = PropertiesServlet.getProperty("openscan.openScanFTPServer");
  private static final String openScanFTPUser = PropertiesServlet.getProperty("openscan.openScanFTPUser");
  private static final String openScanFTPPassword = PropertiesServlet.getProperty("openscan.openScanFTPPassword");
  private static final String autoAppsPassword = PropertiesServlet.getProperty("autoapps.password");
  private static final String erpDirectory = PropertiesServlet.getProperty("openscan.erpDirectory");

  public OpenScanInterface()
  {
  }

  public void execute()
  {
    String interfaceName = getParameterValue("interfaceName");

    log4jLogger.info("OpenScanInterface started for " + interfaceName);
    if (StringUtils.equalsIgnoreCase(interfaceName, "boaImageFiles"))
    {
      boaImageFiles();
    }
    else if (StringUtils.equalsIgnoreCase(interfaceName, "hsbcFiles"))
    {
      hsbcFiles();
    }
    else if (StringUtils.equalsIgnoreCase(interfaceName, "boa820File"))
    {
      boa820File();
    }
    else if (StringUtils.equalsIgnoreCase(interfaceName, "validationFile"))
    {
      validationFile();
    }
    else if (StringUtils.equalsIgnoreCase(interfaceName, "bai2File"))
    {
      bai2File();
    }
    else if (StringUtils.equalsIgnoreCase(interfaceName, "iclFile"))
    {
      iclFile();
    }
    else
    {
      log4jLogger.error("OpenScanInterface.startInterface(): Interface Name does not exist: " + interfaceName);
    }
    log4jLogger.info("OpenScanInterface finished for " + interfaceName);
  }

  private void iclFile()
  {
    String openScanICLRoot = PropertiesServlet.getProperty("openscan.openScanICLRoot");
    String iclDirectory = PropertiesServlet.getProperty("openscan.iclDirectory");

    FTPFile[] fileArray = FtpUtility.listFiles(openScanFTPServer, "openscanftp", openScanFTPPassword, openScanICLRoot, null);
    for (int i = 0; i < fileArray.length; i++)
    {
      FTPFile file = fileArray[i];
      FtpUtility.retrieveFile(openScanFTPServer, "openscanftp", openScanFTPPassword, openScanICLRoot + file.getName(), localRootDirectory + file.getName(), true);
      FtpUtility.sendFileOrDirectory(ediFTPServer, "autoapps", autoAppsPassword, localRootDirectory + file.getName(), iclDirectory, null);
    }
  }

  private void bai2File()
  {
    String openScanBAI2Root = PropertiesServlet.getProperty("openscan.openScanBAI2Root");
    String erpFTPServer = PropertiesServlet.getProperty("openscan.erpFTPServer");

    FTPFile[] fileArray = FtpUtility.listFiles(openScanFTPServer, "openscanftp", openScanFTPPassword, openScanBAI2Root, null);
    for (int i = 0; i < fileArray.length; i++)
    {
      FTPFile file = fileArray[i];
      log4jLogger.info("OpenScanInterface.bai2File() - Processing file " + file.getName());
      FtpUtility.retrieveFile(openScanFTPServer, "openscanftp", openScanFTPPassword, openScanBAI2Root + file.getName(), localRootDirectory + file.getName(), true);
      FtpUtility.sendFileOrDirectory(erpFTPServer, "autoapps", autoAppsPassword, localRootDirectory + file.getName(), erpDirectory, null);
    }
    if (fileArray.length > 0)
    {
      Map<String, Object> rootMap = new HashMap<String, Object>();
      rootMap.put("fileArray", fileArray);
      rootMap.put("erpDirectory", erpDirectory);
      NotificationUtility.sendNotification("cash_apps@valspar.com", "Open Scan BAI2 Files", "bai2File-notification.ftl", rootMap, null);
    }
  }

  private void validationFile()
  {
    OracleConnection conn = (OracleConnection) ConnectionAccessBean.getConnection(DataSource.NORTHAMERICAN);
    OracleStatement stmt = null;
    ResultSet rs = null;
    try
    {
      String openScanValidationRoot = PropertiesServlet.getProperty("openscan.openScanValiationRoot");

      StringBuilder sb = new StringBuilder();
      sb.append("SELECT         aps.trx_number, ");
      sb.append("               aps.amount_due_original, ");
      sb.append("               aps.amount_due_remaining, ");
      sb.append("               decode (round((select sum(a.extended_amount * b.discount_percent) / 100 ");
      sb.append("                    from apps.ra_customer_trx_lines_all a, apps.ra_terms_lines_discounts b ");
      sb.append("                    where a.customer_trx_id = aps.customer_trx_id ");
      sb.append("                    and b.term_id = ct.term_id),2),null,'0.00', ");
      sb.append("                    (round((select sum(a.extended_amount * b.discount_percent) / 100 ");
      sb.append("                    from apps.ra_customer_trx_lines_all a, apps.ra_terms_lines_discounts b ");
      sb.append("                    where a.customer_trx_id = aps.customer_trx_id ");
      sb.append("                    and b.term_id = ct.term_id),2))) discount_amt, ");
      sb.append("               '0.00' freight_amt, ");
      sb.append("               (select sum(extended_amount) from apps.ra_customer_trx_lines_all ");
      sb.append("               where customer_trx_id = ct.customer_trx_id ");
      sb.append("               and line_type = 'TAX') tax_amt, ");
      sb.append("               TO_CHAR (aps.trx_date, 'YYYYMMDD') bill_date, ");
      sb.append("               ct.purchase_order PO_number, ");
      sb.append("               ct.interface_header_attribute1 sales_order, ");
      sb.append("               '0.00' Statement_amount, ");
      sb.append("               '' Parent_customer, ");
      sb.append("               rc.customer_number customer_location, ");
      sb.append("               '' customer_name, ");
      sb.append("               '' address1, ");
      sb.append("               '' address2, ");
      sb.append("               '' city, ");
      sb.append("               '' state, ");
      sb.append("               '' zip, ");
      sb.append("               '0.00' custom_money1, ");
      sb.append("               '0.00' custom_money2, ");
      sb.append("               '' country, ");
      sb.append("               trim(SUBSTR (hou.name, 1, 3)) division, ");
      sb.append("               aps.invoice_currency_code invoice_currency, ");  
      sb.append("               ct.interface_header_attribute3 BOL ");
      sb.append("          FROM apps.ra_customers rc, ");
      sb.append("               apps.ar_payment_schedules_all aps, ");
      sb.append("               apps.hr_operating_units hou, ");
      sb.append("               apps.ra_customer_trx_all ct ");
      sb.append("         WHERE     aps.customer_id = rc.customer_id ");
      sb.append("               and aps.customer_trx_id = ct.customer_trx_id (+) ");
      sb.append("               AND aps.org_id = hou.organization_id ");
      sb.append("               AND aps.status = 'OP' ");
      sb.append("               and aps.customer_site_use_id is null ");
      sb.append("          UNION ALL ");
      sb.append("SELECT         aps.trx_number, ");
      sb.append("               aps.amount_due_original, ");
      sb.append("               aps.amount_due_remaining, ");
      sb.append("               decode (round((select sum(a.extended_amount * b.discount_percent) / 100 ");
      sb.append("                    from apps.ra_customer_trx_lines_all a, apps.ra_terms_lines_discounts b ");
      sb.append("                    where a.customer_trx_id = aps.customer_trx_id ");
      sb.append("                    and b.term_id = ct.term_id),2),null,'0.00', ");
      sb.append("                    (round((select sum(a.extended_amount * b.discount_percent) / 100 ");
      sb.append("                    from apps.ra_customer_trx_lines_all a, apps.ra_terms_lines_discounts b ");
      sb.append("                    where a.customer_trx_id = aps.customer_trx_id ");
      sb.append("                    and b.term_id = ct.term_id),2))) discount_amt, ");
      sb.append("               '0.00' freight_amt, ");
      sb.append("               (select sum(extended_amount) from apps.ra_customer_trx_lines_all ");
      sb.append("               where customer_trx_id = ct.customer_trx_id ");
      sb.append("               and line_type = 'TAX') tax_amt, ");
      sb.append("               TO_CHAR (aps.trx_date, 'YYYYMMDD') bill_date, ");
      sb.append("               ct.purchase_order PO_number, ");
      sb.append("               ct.interface_header_attribute1 sales_order, ");
      sb.append("               '0.00' Statement_amount, ");
      sb.append("               rsua.location Parent_customer, ");
      sb.append("               rc.customer_number customer_location, ");
      sb.append("               raa.address1 customer_name, ");
      sb.append("               raa.address2 address1, ");
      sb.append("               raa.address3 address2, ");
      sb.append("               raa.city city, ");
      sb.append("               raa.state state, ");
      sb.append("               raa.postal_code zip, ");
      sb.append("               '0.00' custom_money1, ");
      sb.append("               '0.00' custom_money2, ");
      sb.append("               raa.country country, ");
      sb.append("               trim(SUBSTR (hou.name, 1, 3)) division, ");
      sb.append("               aps.invoice_currency_code invoice_currency, ");  
      sb.append("               ct.interface_header_attribute3 BOL ");
      sb.append("          FROM apps.ra_customers rc, ");
      sb.append("               apps.ra_addresses_all raa, ");
      sb.append("               apps.ra_site_uses_all rsua, ");
      sb.append("               apps.ar_payment_schedules_all aps, ");
      sb.append("               apps.hr_operating_units hou, ");
      sb.append("               apps.ra_customer_trx_all ct ");
      sb.append("         WHERE     aps.customer_id = rc.customer_id ");
      sb.append("               AND raa.customer_id = aps.customer_id ");
      sb.append("               AND raa.address_id = rsua.address_id ");
      sb.append("               and aps.customer_trx_id = ct.customer_trx_id (+) ");
      sb.append("               AND aps.org_id = hou.organization_id ");
      sb.append("               AND rsua.site_use_id = aps.customer_site_use_id ");
      sb.append("               AND rsua.site_use_code = 'BILL_TO' ");
      sb.append("               AND aps.status = 'OP' ");
      sb.append("      ORDER BY 22, ");
      sb.append("               11, ");
      sb.append("               1 ");

      stmt = (OracleStatement) conn.createStatement();
      rs = stmt.executeQuery(sb.toString());
      FlatFileUtility flatFileUtility = new FlatFileUtility(".txt", null, "\r\n");
      String modifiedDate = new SimpleDateFormat("MMddyyyy").format(new java.util.Date());
      flatFileUtility.setCustomFilename("VAL_" + modifiedDate);

      while (rs.next())
      {
        StringBuilder data = new StringBuilder();
        for (int i = 1; i < 25; i++)
        {
          if (rs.getString(i) != null)
          {
            data.append(rs.getString(i));
          }
          data.append("|");
        }
        flatFileUtility.writeLine(StringUtils.removeEnd(data.toString(), "|"));
      }

      String validationFile = flatFileUtility.getFileWritePath();
      log4jLogger.info("OpenScanInterface.validationFile() - Processing file " + validationFile);
      flatFileUtility.close();
      File file = new File(validationFile);
      FtpUtility.sendFileOrDirectory(openScanFTPServer, openScanFTPUser, openScanFTPPassword, validationFile, openScanValidationRoot, null);
      FtpUtility.rename(openScanFTPServer, openScanFTPUser, openScanFTPPassword, openScanValidationRoot + "/" + file.getName(), openScanValidationRoot + "/" + file.getName() + ".ready");
    }
    catch (Exception e)
    {
      log4jLogger.error(e);
    }
    finally
    {
      JDBCUtil.close(stmt, rs);
      JDBCUtil.close(conn);
    }
  }

  private void boa820File()
  {
    String boa820Directory = PropertiesServlet.getProperty("openscan.boa820Directory");
    String openScanBOA820Root = PropertiesServlet.getProperty("openscan.openScanBOA820Root");

    FTPFile[] fileArray = FtpUtility.listFiles(ediFTPServer, "autoapps", autoAppsPassword, boa820Directory, null);
    for (int i = 0; i < fileArray.length; i++)
    {
      FTPFile file = fileArray[i];
      log4jLogger.info("OpenScanInterface.boa820File() - Processing file " + file.getName());
      FtpUtility.retrieveFile(ediFTPServer, "autoapps", autoAppsPassword, boa820Directory + file.getName(), localRootDirectory + file.getName(), true);
      FtpUtility.sendFileOrDirectory(openScanFTPServer, openScanFTPUser, openScanFTPPassword, localRootDirectory + file.getName(), openScanBOA820Root, null);
      FtpUtility.rename(openScanFTPServer, openScanFTPUser, openScanFTPPassword, openScanBOA820Root + "/" + file.getName(), openScanBOA820Root + "/" + file.getName() + ".ready");
    }
  }

  private void hsbcFiles()
  {
    String hsbcLockBoxDirectory = PropertiesServlet.getProperty("openscan.hsbcLockBoxDirectory");

    Map<String, HSBCImageFileBean> hsbcImageFileBeanMap = new HashMap<String, HSBCImageFileBean>();

    FTPFile[] fileArray = FtpUtility.listFiles(ediFTPServer, "autoapps", autoAppsPassword, hsbcLockBoxDirectory, null);
    for (int i = 0; i < fileArray.length; i++)
    {
      FTPFile file = fileArray[i];
      log4jLogger.info("OpenScanInterface.hsbcFiles() - Processing file " + file.getName());
      HSBCImageFileBean hsbcImageFileBean = new HSBCImageFileBean(file.getName());

      if (hsbcImageFileBean.getHsbcLocalRootDirectory() != null)
      {
        new File(hsbcImageFileBean.getHsbcLocalRootDirectory()).mkdir();
        FtpUtility.retrieveFile(ediFTPServer, "autoapps", autoAppsPassword, hsbcLockBoxDirectory + file.getName(), hsbcImageFileBean.getHsbcLocalRootDirectory() + File.separator + file.getName(), true);

        if (hsbcImageFileBean.getHsbcLocalRootDirectory() != null)
        {
          if (new File(hsbcImageFileBean.getHsbcLocalRootDirectory()).list().length == 9)
          {
            hsbcImageFileBeanMap.put(hsbcImageFileBean.getHsbcLocalRootDirectory(), hsbcImageFileBean);
          }
        }
      }
    }

    for (Map.Entry<String, HSBCImageFileBean> entry: hsbcImageFileBeanMap.entrySet())
    {
      HSBCImageFileBean hsbcImageFileBean = entry.getValue();
      log4jLogger.info("OpenScanInterface.hsbcFiles() - hsbcImageFileBean.getHsbcLocalRootDirectory() = " + hsbcImageFileBean.getHsbcLocalRootDirectory() + ", hsbcImageFileBean.getHsbcFTPRootDirectory() = " + hsbcImageFileBean.getHsbcFTPRootDirectory());
      FtpUtility.sendFileOrDirectory(openScanFTPServer, openScanFTPUser, openScanFTPPassword, hsbcImageFileBean.getHsbcLocalRootDirectory(), hsbcImageFileBean.getHsbcFTPRootDirectory(), null);
      FtpUtility.rename(openScanFTPServer, openScanFTPUser, openScanFTPPassword, hsbcImageFileBean.getHsbcFullFTPRootDirectory(), hsbcImageFileBean.getHsbcFullFTPRootDirectory() + ".ready");
    }
  }

  private void boaImageFiles()
  {
    String boaLockBoxDirectory = PropertiesServlet.getProperty("openscan.boaLockBoxDirectory");
    String openScanBOARoot = PropertiesServlet.getProperty("openscan.openScanBOARoot");

    FTPFile[] fileArray = FtpUtility.listFiles(ediFTPServer, "autoapps", autoAppsPassword, boaLockBoxDirectory, null);
    for (int i = 0; i < fileArray.length; i++)
    {
      FTPFile file = fileArray[i];
      if (StringUtils.endsWith(file.getName(), ".zip"))
      {
        log4jLogger.info("OpenScanInterface.boaImageFiles() - Processing file " + file.getName());
        String zipFileNameWithoutExtension = file.getName().replace(".zip", "");
        FtpUtility.retrieveFile(ediFTPServer, "autoapps", autoAppsPassword, boaLockBoxDirectory + file.getName(), localRootDirectory + file.getName(), true);
        ZipUtility.unZipFile(localRootDirectory + file.getName(), localRootDirectory + zipFileNameWithoutExtension);
        FtpUtility.sendFileOrDirectory(openScanFTPServer, openScanFTPUser, openScanFTPPassword, localRootDirectory + zipFileNameWithoutExtension, openScanBOARoot, null);
        FtpUtility.rename(openScanFTPServer, openScanFTPUser, openScanFTPPassword, openScanBOARoot + "/" + zipFileNameWithoutExtension, openScanBOARoot + "/" + zipFileNameWithoutExtension + ".ready");
      }
    }
  }
}
