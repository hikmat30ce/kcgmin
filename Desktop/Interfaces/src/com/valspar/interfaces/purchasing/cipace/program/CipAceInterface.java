package com.valspar.interfaces.purchasing.cipace.program;

import com.valspar.interfaces.common.BaseInterface;
import com.valspar.interfaces.common.beans.ConnectionAccessBean;
import com.valspar.interfaces.common.enums.DataSource;
import com.valspar.interfaces.common.utils.*;
import com.valspar.interfaces.purchasing.cipace.beans.*;
import com.valspar.interfaces.purchasing.cipace.dao.*;
import com.valspar.interfaces.purchasing.cipace.enums.PoFetchMode;
import java.sql.Connection;
import java.util.*;
import oracle.jdbc.OracleConnection;
import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;

public class CipAceInterface extends BaseInterface
{
  private static Logger log4jLogger = Logger.getLogger(CipAceInterface.class);

  /* References:

     Concurrent Program: Automated PO Upload Process - Valspar
     Short Name: VAUTOPOUPLOAD
     Execution File Name: apps.vca_po_upload_process_pkg.run_po_upload_process
     Create PO Package: apps.vca_po_upload_create_po_pkg.create_po

     Modeled after: VCA_CREATE_PO_PKG

     Oracle's PDOI (Purchasing Docs Open Interface) is: po_docs_interface_sv5.process_po_headers_interface
   */

  public CipAceInterface()
  {
  }

  public void execute()
  {
    Connection cipAceConn = null;
    OracleConnection northAmericanConn = null;
    OracleConnection emeaiConn = null;
    OracleConnection asiaPacConn = null;
    try
    {
      cipAceConn = ConnectionAccessBean.getConnection(DataSource.CIPACE);

      northAmericanConn = (OracleConnection) ConnectionAccessBean.getConnection(DataSource.NORTHAMERICAN);
      emeaiConn = (OracleConnection) ConnectionAccessBean.getConnection(DataSource.EMEAI);
      asiaPacConn = (OracleConnection) ConnectionAccessBean.getConnection(DataSource.ASIAPAC);

      Map<String, OracleConnection> connections = new HashMap<String, OracleConnection>();
      connections.put(DataSource.NORTHAMERICAN.getAnalyticsDataSource(), northAmericanConn);
      connections.put(DataSource.EMEAI.getAnalyticsDataSource(), emeaiConn);
      connections.put(DataSource.ASIAPAC.getAnalyticsDataSource(), asiaPacConn);
      CIPAceDAO.initialize(cipAceConn);
      OracleDAO.initialize(connections);

      /*
      OracleDAO.fakeStuff();

      if (1 + 1 == 2)
        return;
*/
      debug("");
      debug("----------------------------------------------------------------------------------------------------------------------------------------------------------------------");
      debug("START Transfer new purchase orders to 11i");
      debug("----------------------------------------------------------------------------------------------------------------------------------------------------------------------");
      debug("");
      transferNewPurchaseOrdersTo11i();
      debug("DONE Transferring new purchase orders to 11i");

      debug("");
      debug("----------------------------------------------------------------------------------------------------------------------------------------------------------------------");
      debug("START Update Purchase Order Activity (Invoices/Receipts) in CIPACE");
      debug("----------------------------------------------------------------------------------------------------------------------------------------------------------------------");
      debug("");
      updatePurchaseOrderActivityInCipAce();
      debug("DONE Updating Purchase Order Activity (Invoices/Receipts) in CIPACE");

      debug("");
      debug("----------------------------------------------------------------------------------------------------------------------------------------------------------------------");
      debug("START Execute CIPAce PO Change Orders in 11i");
      debug("----------------------------------------------------------------------------------------------------------------------------------------------------------------------");
      debug("");
      executeCIPAceChangeOrdersIn11i();
      debug("DONE Executing CIPAce PO Change Orders in 11i");

      debug("");
      debug("----------------------------------------------------------------------------------------------------------------------------------------------------------------------");
      debug("START Sync CIPAce Vendors & Sites from 11i");
      debug("----------------------------------------------------------------------------------------------------------------------------------------------------------------------");
      debug("");
      syncVendors(connections);
      debug("DONE Sync CIPAce Vendors & Sites from 11i");

      debug("");
      debug("----------------------------------------------------------------------------------------------------------------------------------------------------------------------");
      debug("START Sync CIPAce Currency Exchange Rates from 11i");
      debug("----------------------------------------------------------------------------------------------------------------------------------------------------------------------");
      debug("");
      syncCurrencyExchangeRates(connections);
      debug("DONE Sync CIPAce Currency Exchange Rates from 11i");
    }
    catch (Exception e)
    {
      log4jLogger.error(e);
    }
    finally
    {
      JDBCUtil.close(cipAceConn);
      JDBCUtil.close(northAmericanConn);
      JDBCUtil.close(emeaiConn);
      JDBCUtil.close(asiaPacConn);
    }
  }

  public void debug(String message)
  {
    log4jLogger.info(message);
  }

  private void transferNewPurchaseOrdersTo11i()
  {
    debug("Looking for CIPAce Purchase Orders that are ready to submit to 11i...");

    List<PurchaseOrderBean> purchaseOrders = CIPAceDAO.fetchPurchaseOrders(PoFetchMode.NOT_LINKED);

    debug("CIPAce purchase order query finished, count = " + purchaseOrders.size());

    if (!purchaseOrders.isEmpty())
    {
      debug("Start Validate & Import PO's into 11i...");
      Map<String, DatabaseContextBean> contextMap = OracleDAO.stagePurchaseOrdersForImport(purchaseOrders);

      for (Map.Entry<String, DatabaseContextBean> entry: contextMap.entrySet())
      {
        DatabaseContextBean context = entry.getValue();

        if (!context.getPurchaseOrders().isEmpty())
        {
          try
          {
            OracleAppsUtility.appsInitialize((OracleConnection) context.getConnection(), context.getUserId(), context.getResponsibilityId(), context.getApplicationId());
            if (!OracleDAO.importPurchaseOrders(context))
            {
              // Do nothing for now, any specific errors we want to handle will be
              // logged inside importPurchaseOrders()
            }

            OracleDAO.sendDocumentsToBuyers(context);
          }
          catch (Exception e)
          {
            log4jLogger.error(e);
          }
        }
      }

      debug("Updating CIPAce...");
      CIPAceDAO.updatePurchaseOrders(purchaseOrders);
    }
  }

  private void updatePurchaseOrderActivityInCipAce()
  {
    debug("Looking for linked PO's in CIPAce...");
    List<PurchaseOrderBean> purchaseOrders = CIPAceDAO.fetchPurchaseOrders(PoFetchMode.LINKED);

    debug("CIPAce purchase order query finished, count = " + purchaseOrders.size());

    if (!purchaseOrders.isEmpty())
    {
      debug("Querying PO activity (receipts & billing) from 11i...");
      if (!OracleDAO.populatePurchaseOrderActivity(purchaseOrders))
      {
        return;
      }
      debug("Updating CIPAce PO Lines with 11i invoice spend & receipts...");
      if (!CIPAceDAO.updatePurchaseOrderLines(purchaseOrders))
      {
        return;
      }
    }
  }

  private void executeCIPAceChangeOrdersIn11i()
  {
    debug("Looking for CIPAce PO Change Orders that are ready to submit to 11i...");

    List<POChangeOrderBean> poChangeOrders = CIPAceDAO.fetchPOChangeOrders();

    debug("CIPAce PO change order query finished, count = " + poChangeOrders.size());

    if (!poChangeOrders.isEmpty())
    {
      /*
      debug("Start Validate & Execute PO Change Orders in 11i...");
      Map<String, DatabaseContextBean> contextMap = OracleDAO.stagePOChangeOrdersForImport(poChangeOrders);

      for (Map.Entry<String, DatabaseContextBean> entry: contextMap.entrySet())
      {
        DatabaseContextBean context = entry.getValue();

        if (!context.getPurchaseOrders().isEmpty())
        {
          ConnectionBean cb = context.getConnectionBean();

          try
          {
            OracleAppsUtility.appsInitialize((OracleConnection) cb.getConnection(), context.getUserId(), context.getResponsibilityId(), context.getApplicationId());
            if (!OracleDAO.importPurchaseOrders(context))
            {
              // Do nothing for now, any specific errors we want to handle will be
              // logged inside importPurchaseOrderChangeOrders()
            }

            OracleDAO.sendDocumentsToBuyers(context);
          }
          catch (Exception e)
          {
            log4jLogger.error(e);
          }
        }
      }
      */

      debug("Skipping all 11i processing for change orders...");

      debug("Updating CIPAce...");
      CIPAceDAO.updatePurchaseOrderChangeOrders(poChangeOrders);
    }
  }

  private void syncVendors(Map<String, OracleConnection> connections)
  {
    try
    {
      for (Map.Entry<String, OracleConnection> entry: connections.entrySet())
      {
        String dbName = ConnectionUtility.buildDatabaseName(entry.getValue());
        String regionId = CIPAceDAO.lookupRegionId(dbName);

        debug("Querying active 11i vendor sites in " + dbName + "...");
        List<VendorSiteBean> oracleVendorSites = OracleDAO.fetchOracleVendorSites(entry.getValue(), dbName, regionId);
        debug("..." + oracleVendorSites.size() + " Oracle sites found.");

        debug("Querying CIPAce vendor sites in region " + regionId + "...");
        Map<String, VendorSiteBean> cipAceVendorSites = CIPAceDAO.fetchVendorSitesByRegion(dbName, Integer.valueOf(regionId));
        debug("..." + cipAceVendorSites.size() + " CIPAce sites found.");

        for (VendorSiteBean oracleVendorSiteBean: oracleVendorSites)
        {
          VendorSiteBean cipAceVendorSiteBean = cipAceVendorSites.remove(oracleVendorSiteBean.getVendorSiteId());

          if (cipAceVendorSiteBean == null)
          {
            debug("Inserting " + dbName + " vendor " + oracleVendorSiteBean.getName() + " site " + oracleVendorSiteBean.getVendorSiteCode() + " / " + oracleVendorSiteBean.getOperatingUnit());
            CIPAceDAO.insertVendor(oracleVendorSiteBean);
          }
          else
          {
            oracleVendorSiteBean.setVendorAutoId(cipAceVendorSiteBean.getVendorAutoId());

            if (!oracleVendorSiteBean.equals(cipAceVendorSiteBean))
            {
              debug("Updating " + dbName + " vendor  " + oracleVendorSiteBean.getName() + " site " + oracleVendorSiteBean.getVendorSiteCode() + " / " + oracleVendorSiteBean.getOperatingUnit());

              CIPAceDAO.updateVendor(oracleVendorSiteBean);
            }
            else
            {
              //debug("  Matched in CIPAce, no changes!");
            }
          }
        }

        for (VendorSiteBean cipAceVendorSite: cipAceVendorSites.values())
        {
          if (cipAceVendorSite.isActive())
          {
            debug("Inactivating CIPAce vendor " + cipAceVendorSite.getName() + " site " + cipAceVendorSite.getVendorSiteCode() + " / " + cipAceVendorSite.getOperatingUnit());
            cipAceVendorSite.setActive(false);
            CIPAceDAO.updateVendor(cipAceVendorSite);
          }
        }
      }
    }
    catch (Exception e)
    {
      log4jLogger.error(e);
    }
  }

  private void syncCurrencyExchangeRates(Map<String, OracleConnection> connections)
  {
    try
    {
      Map<String, String> cipAceCurrencyMap = CIPAceDAO.getCurrencyMap();

      for (Map.Entry<String, OracleConnection> entry: connections.entrySet())
      {
        String dbName = ConnectionUtility.buildDatabaseName(entry.getValue());

        String periodName = OracleDAO.lookupFiscalPeriodName(entry.getValue());

        debug("Querying currency exchange rates in " + dbName + " for " + periodName + "...");
        List<CurrencyExchangeRateBean> oracleCurrencyExchangeRates = OracleDAO.fetchOracleCurrencyExchangeRates(entry.getValue(), periodName);
        debug("..." + oracleCurrencyExchangeRates.size() + " Oracle currency exchanges rates found.");

        for (CurrencyExchangeRateBean oracleCurrencyExchangeRateBean: oracleCurrencyExchangeRates)
        {
          //debug("Querying CIPAce currency exchange rate...");
          String currencyExchangeRateGuid = CIPAceDAO.getCurrencyExchangeRateGuid(oracleCurrencyExchangeRateBean);

          if (StringUtils.isEmpty(currencyExchangeRateGuid))
          {
            debug("Inserting CIPAce " + oracleCurrencyExchangeRateBean.getFromCurrencyCode() + " -> " + oracleCurrencyExchangeRateBean.getToCurrencyCode() + " exchange rate for " + oracleCurrencyExchangeRateBean.getNote() + " (GL period " + oracleCurrencyExchangeRateBean.getPeriodName() + ")");
            oracleCurrencyExchangeRateBean.setFromCurrencyGuid(cipAceCurrencyMap.get(oracleCurrencyExchangeRateBean.getFromCurrencyCode()));
            oracleCurrencyExchangeRateBean.setToCurrencyGuid(cipAceCurrencyMap.get(oracleCurrencyExchangeRateBean.getToCurrencyCode()));
            CIPAceDAO.insertCurrencyExchangeRate(oracleCurrencyExchangeRateBean);
          }
        }
      }
    }
    catch (Exception e)
    {
      log4jLogger.error(e);
    }
  }
}
