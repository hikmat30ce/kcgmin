package com.valspar.interfaces.purchasing.cipace.dao;

import com.valspar.interfaces.common.beans.*;
import com.valspar.interfaces.common.enums.DataSource;
import com.valspar.interfaces.common.utils.*;
import com.valspar.interfaces.purchasing.cipace.beans.*;
import java.io.*;
import java.sql.*;
import java.util.*;
import oracle.jdbc.*;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.mail.EmailAttachment;
import org.apache.log4j.Logger;

public final class OracleDAO
{
  private static Logger log4jLogger = Logger.getLogger(OracleDAO.class);
  private static Map<String, OracleConnection> connections = new HashMap<String, OracleConnection>();
  private static String APPS_USERNAME = "CIPACE";

  private OracleDAO()
  {
  }

  public static void initialize(Map<String, OracleConnection> connections)
  {
    OracleDAO.connections = connections;
  }

  private static void debug(String message)
  {
    log4jLogger.info(message);
  }

  public static String lookupItemCategoryId(Connection conn, String itemCategoryName)
  {
    StringBuilder sb = new StringBuilder();
    sb.append("SELECT mcs.category_id ");
    sb.append(" FROM mtl_categories_vl mcs, ");
    sb.append("      mtl_category_set_valid_cats mcsvc ");
    sb.append("WHERE mcs.category_id = mcsvc.category_id ");
    sb.append("  and upper(mcs.description) = upper(?) ");
    sb.append("  AND mcsvc.category_set_id = ");
    sb.append("            (SELECT   category_set_id ");
    sb.append("             FROM     mtl_default_category_sets ");
    sb.append("             WHERE    functional_area_id = 2) "); // purchasing, hardcoded ID per PDOI code
    sb.append("  AND sysdate < nvl(mcs.disable_date, sysdate+1) ");
    sb.append("  AND mcs.enabled_flag = 'Y' ");

    return ValsparLookUps.queryForSingleValueLeaveConnectionOpen(conn, sb.toString(), itemCategoryName);
  }

  public static String lookupOrgName(Connection conn, String orgId)
  {
    return ValsparLookUps.queryForSingleValueLeaveConnectionOpen(conn, "select name from hr_all_organization_units where organization_id = ?", orgId);
  }

  private static boolean stagePurchaseOrder(PoHeadersInterfaceBean poBean, DatabaseContextBean context)
  {
    boolean success = false;

    OracleConnection conn = (OracleConnection) context.getConnection();
    String oracleDbName = ConnectionUtility.buildDatabaseName(conn);

    poBean.setPoBatchId(context.getPoBatchId());

    String agentId = ValsparLookUps.queryForSingleValueLeaveConnectionOpen(conn, "select employee_id from fnd_user where upper(user_name) = upper(?)", poBean.getUserName());

    if (StringUtils.isEmpty(agentId))
    {
      log4jLogger.error("Error - could not find a user in " + oracleDbName + " with username " + poBean.getUserName() + ".  CIPAce PO# " + poBean.getCipAcePoNumber());
      poBean.addFatalErrorMessage("Could not find a user in " + oracleDbName + " with username " + poBean.getUserName());
      return false;
    }

    poBean.setAgentId(agentId);

    String shipToLocationId = ValsparLookUps.queryForSingleValueLeaveConnectionOpen(conn, "select ship_to_location_id from hr_locations_all where upper(location_code) = upper(?)", poBean.getShipToOrganizationCode());

    if (StringUtils.isEmpty(shipToLocationId))
    {
      log4jLogger.error("Error - could not find a location in " + oracleDbName + " with code " + poBean.getShipToOrganizationCode() + ".  CIPAce PO# " + poBean.getCipAcePoNumber());
      poBean.addFatalErrorMessage("Could not find a location in " + oracleDbName + " with code " + poBean.getShipToOrganizationCode());
      return false;
    }

    poBean.setShipToLocationId(shipToLocationId);

    for (PoLinesInterfaceBean line: poBean.getLines())
    {
      if (StringUtils.isNotEmpty(line.getItemDescription()) && line.getItemDescription().length() > 240)
      {
        log4jLogger.error("Error - The description for PO line " + line.getLineNumber() + " is too long (line description must be 240 characters or less).  Please recreate the PO with less than 240 characters in the line description field.  CIPAce PO# " + poBean.getCipAcePoNumber());
        poBean.addFatalErrorMessage("The description for PO line " + line.getLineNumber() + " is too long (line description must be 240 characters or less).  Please recreate the PO with less than 240 characters in the line description field.");
        return false;
      }
    }

    StringBuilder sb = new StringBuilder();
    sb.append("select nvl(pvsa.bill_to_location_id, fspa.bill_to_location_id) ");
    sb.append("from po_vendor_sites_all pvsa ");
    sb.append("inner join financials_system_params_all fspa ");
    sb.append("   on pvsa.org_id = fspa.org_id ");
    sb.append("where pvsa.vendor_id = ? ");
    sb.append("  and pvsa.vendor_site_id = ? ");

    poBean.setBillToLocationId(ValsparLookUps.queryForSingleValueLeaveConnectionOpen(conn, sb.toString(), poBean.getVendorId(), poBean.getVendorSiteId()));

    for (PoLinesInterfaceBean poLineBean: poBean.getLines())
    {
      poLineBean.setItemCategoryId(lookupItemCategoryId(conn, poLineBean.getItemCategory()));
    }

    success = insertPurchaseOrderInterfaceHeader(conn, poBean);

    if (success)
    {
      success = insertPurchaseOrderInterfaceLines(conn, poBean);
    }

    if (success)
    {
      success = insertPurchaseOrderInterfaceDistributions(conn, poBean);
    }

    return success;
  }

  public static Map<String, DatabaseContextBean> stagePurchaseOrdersForImport(List<PurchaseOrderBean> purchaseOrders)
  {
    Map<String, DatabaseContextBean> contextMap = new HashMap<String, DatabaseContextBean>();

    for (PurchaseOrderBean poBean: purchaseOrders)
    {
      OracleConnection conn = null;
      String cipAceDbName = poBean.getCipAceDatabaseName();
      String orgId = poBean.getOrgId();

      debug("");
      debug("Processing PO " + poBean.getCipAcePoNumber() + " for database " + cipAceDbName + "/Org ID " + orgId + ". Project " + poBean.getProjectId() + " - " + poBean.getProjectName() + "...");

      conn = connections.get(cipAceDbName);

      if (conn == null)
      {
        log4jLogger.error("Error - could not find an 11i database connection for database named " + cipAceDbName + ".  CIPAce PO# " + poBean.getCipAcePoNumber());
        poBean.addFatalErrorMessage("Could not find an 11i database named '" + cipAceDbName + "'");
        continue;
      }

      String oracleDbName = ConnectionUtility.buildDatabaseName(conn);
      poBean.setOracleDatabaseName(oracleDbName);
      String orgName = lookupOrgName(conn, poBean.getOrgId());

      if (StringUtils.isEmpty(orgName))
      {
        log4jLogger.error("Error - could not find an 11i organization in " + oracleDbName + " with ID " + orgId + ".  CIPAce PO# " + poBean.getCipAcePoNumber());
        poBean.addFatalErrorMessage("Could not find an 11i organization in " + oracleDbName + " with ID " + orgId);
        continue;
      }

      poBean.setOrgName(orgName);

      String contextKey = oracleDbName + "|" + orgName;
      debug("    Org " + orgName);

      if (poBean.getLines().isEmpty())
      {
        log4jLogger.error("Error - PO has no lines!  CIPAce PO# " + poBean.getCipAcePoNumber());
        poBean.addFatalErrorMessage("Purchase Order has no lines");
        continue;
      }

      try
      {
        DatabaseContextBean context = contextMap.get(contextKey);

        if (context == null)
        {
          context = new DatabaseContextBean();
          contextMap.put(contextKey, context);

          context.setConnection(conn);
          conn.setAutoCommit(false);
          context.initialize(orgId, orgName, APPS_USERNAME);
        }

        boolean success = stagePurchaseOrder(poBean, context);

        if (success)
        {
          conn.commit();
          context.getPurchaseOrders().add(poBean);
        }
        else
        {
          JDBCUtil.rollBack(conn);
        }
      }
      catch (Exception e)
      {
        log4jLogger.error("CIPAce PO# " + poBean.getCipAcePoNumber(), e);
        poBean.addFatalErrorMessage("Error creating purchase order in 11i: " + e);

        if (conn != null)
        {
          JDBCUtil.rollBack(conn);
        }
      }
    }

    return contextMap;
  }

  public static Map<String, DatabaseContextBean> stagePOChangeOrdersForImport(List<POChangeOrderBean> poChangeOrders)
  {
    Map<String, DatabaseContextBean> contextMap = new HashMap<String, DatabaseContextBean>();

    for (POChangeOrderBean poChangeOrderBean: poChangeOrders)
    {
      OracleConnection conn = null;
      String cipAceDbName = poChangeOrderBean.getCipAceDatabaseName();
      String orgId = poChangeOrderBean.getOrgId();

      debug("");
      debug("Processing Change Order for PO " + poChangeOrderBean.getCipAcePoNumber() + " line " + poChangeOrderBean.getPoLineNumber() + " for database " + cipAceDbName + "/Org ID " + orgId + "...");

      conn = connections.get(cipAceDbName);

      if (conn == null)
      {
        log4jLogger.error("Error - could not find an 11i database connection for database named " + cipAceDbName + ".  CIPAce Change order for PO# " + poChangeOrderBean.getCipAcePoNumber() + " line " + poChangeOrderBean.getPoLineNumber());
        poChangeOrderBean.addFatalErrorMessage("Could not find an 11i database named '" + cipAceDbName + "'");
        continue;
      }

      String oracleDbName = ConnectionUtility.buildDatabaseName(conn);
      poChangeOrderBean.setOracleDatabaseName(oracleDbName);
      String orgName = lookupOrgName(conn, poChangeOrderBean.getOrgId());

      if (StringUtils.isEmpty(orgName))
      {
        log4jLogger.error("Error - could not find an 11i organization in " + oracleDbName + " with ID " + orgId + ".  CIPAce Change order for PO# " + poChangeOrderBean.getCipAcePoNumber() + " line " + poChangeOrderBean.getPoLineNumber());
        poChangeOrderBean.addFatalErrorMessage("Could not find an 11i organization in " + oracleDbName + " with ID " + orgId);
        continue;
      }

      poChangeOrderBean.setOrgName(orgName);

      String contextKey = oracleDbName + "|" + orgName;
      debug("    Org " + orgName);

      try
      {
        DatabaseContextBean context = contextMap.get(contextKey);

        if (context == null)
        {
          context = new DatabaseContextBean();
          contextMap.put(contextKey, context);

          context.setConnection(conn);
          conn.setAutoCommit(false);
          context.initialize(orgId, orgName, APPS_USERNAME);
        }

        OracleAppsUtility.appsInitialize(conn, context.getUserId(), context.getResponsibilityId(), context.getApplicationId());

        // We can't find a good way to update a PO line (without putting the PO into a status of 'requires reapproval'), so
        // for now if any change is requested, we cancel the PO.
        debug("    Cancelling PO# " + poChangeOrderBean.getOraclePoNumber() + " in Oracle...");
        boolean success = cancelPo(conn, poChangeOrderBean); // Will commit, doesn't have to though.

        if (success && !poChangeOrderBean.isCancel())
        {
          // If the PO line quantity or price is just being adjusted and not fully canceled, then recreate the PO with the new line quantity & price

          debug("    Rebuilding and staging PO with updated quantity or unit price...");
          poChangeOrderBean.compositeFieldsFromPurchaseOrder();

          success = stagePurchaseOrder(poChangeOrderBean, context);

          if (success)
          {
            conn.commit();
            context.getPurchaseOrders().add(poChangeOrderBean);
          }
          else
          {
            JDBCUtil.rollBack(conn);
          }
        }
      }
      catch (Exception e)
      {
        log4jLogger.error("CIPAce Change order for PO# " + poChangeOrderBean.getCipAcePoNumber() + " line " + poChangeOrderBean.getPoLineNumber(), e);
        poChangeOrderBean.addFatalErrorMessage("Error creating purchase order in 11i: " + e);

        if (conn != null)
        {
          JDBCUtil.rollBack(conn);
        }
      }
    }

    return contextMap;
  }

  private static boolean insertPurchaseOrderInterfaceHeader(OracleConnection conn, PoHeadersInterfaceBean poBean)
  {
    OraclePreparedStatement pst = null;

    try
    {
      poBean.setInterfaceHeaderId(ValsparLookUps.queryForSingleValueLeaveConnectionOpen(conn, "SELECT PO_HEADERS_INTERFACE_S.NEXTVAL FROM dual"));

      StringBuilder sb = new StringBuilder();

      sb.append("INSERT INTO PO_HEADERS_INTERFACE ( ");
      sb.append("   INTERFACE_HEADER_ID,  ");
      sb.append("   ACTION, ");
      sb.append("   ORG_ID, ");
      sb.append("   AGENT_ID, ");
      sb.append("   DOCUMENT_TYPE_CODE, ");
      sb.append("   APPROVAL_STATUS, ");
      sb.append("   REVISION_NUM, ");
      sb.append("   VENDOR_ID, ");
      sb.append("   VENDOR_SITE_ID, ");
      sb.append("   VENDOR_DOC_NUM, ");
      sb.append("   SHIP_TO_LOCATION_ID, ");
      sb.append("   BILL_TO_LOCATION_ID, ");
      sb.append("   CURRENCY_CODE, ");
      sb.append("   RATE, ");
      sb.append("   COMMENTS, ");
      sb.append("   ATTRIBUTE2, ");
      sb.append("   BATCH_ID) ");
      sb.append("VALUES ( ");
      sb.append("   :INTERFACE_HEADER_ID, ");
      sb.append("   :ACTION, ");
      sb.append("   :ORG_ID, ");
      sb.append("   :AGENT_ID, ");
      sb.append("   :DOCUMENT_TYPE_CODE, ");
      sb.append("   :APPROVAL_STATUS, ");
      sb.append("   :REVISION_NUM,  ");
      sb.append("   :VENDOR_ID,  ");
      sb.append("   :VENDOR_SITE_ID,  ");
      sb.append("   :VENDOR_DOC_NUM,  ");
      sb.append("   :SHIP_TO_LOCATION_ID,  ");
      sb.append("   :BILL_TO_LOCATION_ID,  ");
      sb.append("   :CURRENCY_CODE,  ");
      sb.append("   :RATE, ");
      sb.append("   :COMMENTS, ");
      sb.append("   :ATTRIBUTE2, ");
      sb.append("   :BATCH_ID) ");

      pst = (OraclePreparedStatement) conn.prepareStatement(sb.toString());
      pst.setStringAtName("INTERFACE_HEADER_ID", poBean.getInterfaceHeaderId());
      pst.setStringAtName("ACTION", poBean.getAction());
      pst.setStringAtName("ORG_ID", poBean.getOrgId());
      pst.setStringAtName("AGENT_ID", poBean.getAgentId());
      pst.setStringAtName("DOCUMENT_TYPE_CODE", "STANDARD");
      pst.setStringAtName("APPROVAL_STATUS", "APPROVED");
      pst.setStringAtName("REVISION_NUM", poBean.getRevisionNumber());
      pst.setStringAtName("VENDOR_ID", poBean.getVendorId());
      pst.setStringAtName("VENDOR_SITE_ID", poBean.getVendorSiteId());
      pst.setStringAtName("VENDOR_DOC_NUM", poBean.getCipAcePoNumber());
      pst.setStringAtName("SHIP_TO_LOCATION_ID", poBean.getShipToLocationId());
      pst.setStringAtName("BILL_TO_LOCATION_ID", poBean.getBillToLocationId());
      pst.setStringAtName("CURRENCY_CODE", poBean.getCurrencyCode());
      pst.setBigDecimalAtName("RATE", poBean.getRate());
      pst.setStringAtName("COMMENTS", poBean.getComments());
      pst.setStringAtName("ATTRIBUTE2", poBean.getPoBatchId());
      pst.setStringAtName("BATCH_ID", poBean.getPoBatchId());
      pst.execute();
      return true;
    }
    catch (Exception e)
    {
      log4jLogger.error(e);
      poBean.addFatalErrorMessage("Error creating PO header interface record in 11i: " + e);
      return false;
    }
    finally
    {
      JDBCUtil.close(pst);
    }
  }

  private static boolean insertPurchaseOrderInterfaceLines(OracleConnection conn, PoHeadersInterfaceBean poBean)
  {
    OraclePreparedStatement pst = null;

    try
    {
      StringBuilder sb = new StringBuilder();

      sb.append("INSERT INTO PO_LINES_INTERFACE ( ");
      sb.append("   INTERFACE_LINE_ID, ");
      sb.append("   INTERFACE_HEADER_ID, ");
      sb.append("   ACTION, ");
      sb.append("   SHIPMENT_TYPE, ");
      //sb.append("   CATEGORY, "); // Don't populate this or PDOI will try to infer the category_id which will fail
      sb.append("   CATEGORY_ID, ");
      sb.append("   ITEM_DESCRIPTION, ");
      sb.append("   UNIT_OF_MEASURE, ");
      sb.append("   QUANTITY, ");
      sb.append("   UNIT_PRICE, ");
      sb.append("   PROMISED_DATE, ");
      sb.append("   LINE_NUM) ");
      sb.append("VALUES ( ");
      sb.append("   :INTERFACE_LINE_ID, ");
      sb.append("   :INTERFACE_HEADER_ID, ");
      sb.append("   :ACTION, ");
      sb.append("   :SHIPMENT_TYPE, ");
      //sb.append("   :CATEGORY, ");
      sb.append("   :CATEGORY_ID, ");
      sb.append("   :ITEM_DESCRIPTION, ");
      sb.append("   :UNIT_OF_MEASURE,  ");
      sb.append("   :QUANTITY,  ");
      sb.append("   :UNIT_PRICE,  ");
      sb.append("   :PROMISED_DATE,  ");
      sb.append("   :LINE_NUM) ");

      pst = (OraclePreparedStatement) conn.prepareStatement(sb.toString());

      for (PoLinesInterfaceBean line: poBean.getLines())
      {
        line.setInterfaceLineId(ValsparLookUps.queryForSingleValueLeaveConnectionOpen(conn, "SELECT PO_LINES_INTERFACE_S.NEXTVAL FROM dual"));

        pst.setStringAtName("INTERFACE_LINE_ID", line.getInterfaceLineId());
        pst.setStringAtName("INTERFACE_HEADER_ID", poBean.getInterfaceHeaderId());
        pst.setStringAtName("ACTION", line.getAction());
        pst.setStringAtName("SHIPMENT_TYPE", "STANDARD");
        //pst.setStringAtName("CATEGORY", line.getItemCategory());
        pst.setStringAtName("CATEGORY_ID", line.getItemCategoryId());
        pst.setStringAtName("ITEM_DESCRIPTION", line.getItemDescription());
        pst.setStringAtName("UNIT_OF_MEASURE", line.getUom());
        pst.setBigDecimalAtName("QUANTITY", line.getQuantity());
        pst.setBigDecimalAtName("UNIT_PRICE", line.getUnitPrice());
        pst.setDATEAtName("PROMISED_DATE", JDBCUtil.getDATE(line.getPromisedDate()));
        pst.setIntAtName("LINE_NUM", line.getLineNumber());
        pst.execute();
      }
      return true;
    }
    catch (Exception e)
    {
      log4jLogger.error(e);
      poBean.addFatalErrorMessage("Error creating PO line interface record in 11i: " + e);
      return false;
    }
    finally
    {
      JDBCUtil.close(pst);
    }
  }

  private static String lookupGlAccountId(OracleConnection conn, PoHeadersInterfaceBean poBean)
  {
    StringBuilder sb = new StringBuilder();
    sb.append("SELECT code_combination_id ");
    sb.append("FROM gl_code_combinations ");
    sb.append("WHERE segment1 = ? ");
    sb.append("  AND segment2 = ? ");
    sb.append("  AND segment3 = ? ");
    sb.append("  AND segment4 = ? ");
    sb.append("  AND segment5 = ? ");
    sb.append("  AND segment6 = ? ");
    sb.append("  AND segment7 = ? ");
    sb.append("  AND nvl(segment8, ' ') = nvl(?, ' ') ");

    return ValsparLookUps.queryForSingleValueLeaveConnectionOpen(conn, sb.toString(), poBean.getGlCompanyCode(), poBean.getGlProfitCenterCode(), poBean.getGlLocationCode(), poBean.getGlCostCenterCode(), poBean.getGlAccountNumber(), poBean.getGlCategoryCode(), poBean.getGlDepartmentCode(), poBean.getGlLocal());
  }

  private static boolean insertPurchaseOrderInterfaceDistributions(OracleConnection conn, PoHeadersInterfaceBean poBean)
  {
    OraclePreparedStatement pst = null;

    try
    {
      StringBuilder sb = new StringBuilder();

      sb.append("INSERT INTO PO_DISTRIBUTIONS_INTERFACE ( ");
      sb.append("   INTERFACE_HEADER_ID, ");
      sb.append("   INTERFACE_LINE_ID, ");
      sb.append("   INTERFACE_DISTRIBUTION_ID, ");
      sb.append("   QUANTITY_ORDERED, ");
      sb.append("   ACCRUAL_ACCOUNT_ID, ");
      sb.append("   CHARGE_ACCOUNT_ID,  ");
      sb.append("   DELIVER_TO_LOCATION_ID, ");
      sb.append("   DESTINATION_ORGANIZATION_ID, ");
      sb.append("   DESTINATION_TYPE_CODE, ");
      sb.append("   ORG_ID) ");
      sb.append("VALUES ( ");
      sb.append("   :INTERFACE_HEADER_ID, ");
      sb.append("   :INTERFACE_LINE_ID, ");
      sb.append("   PO_DISTRIBUTIONS_INTERFACE_S.NEXTVAL, ");
      sb.append("   :QUANTITY_ORDERED, ");
      sb.append("   :ACCRUAL_ACCOUNT_ID, ");
      sb.append("   :CHARGE_ACCOUNT_ID, ");
      sb.append("   :DELIVER_TO_LOCATION_ID, ");
      sb.append("   :DESTINATION_ORGANIZATION_ID, ");
      sb.append("   :DESTINATION_TYPE_CODE, ");
      sb.append("   :ORG_ID) ");

      pst = (OraclePreparedStatement) conn.prepareStatement(sb.toString());

      for (PoLinesInterfaceBean line: poBean.getLines())
      {
        /* Need to do this ?

          GMI_RESERVATION_UTIL.get_OPM_account (v_dest_org_id => p_po_line_rec.dest_org_id,
                                                v_apps_item_id => p_po_line_rec.item_id,
                                                v_vendor_site_id => p_po_line_rec.vendor_site_id,
                                                x_cc_id => v_charge_account_id,
                                                x_ac_id => v_accrual_account_id);
         */
        String chargeAccountId = lookupGlAccountId(conn, poBean);

        if (StringUtils.isEmpty(chargeAccountId))
        {
          log4jLogger.error("  **VALIDATION ERROR** PO " + poBean.getCipAcePoNumber() + ".  Could not find GL code combination for Company Code=" + poBean.getGlCompanyCode() + ", profit center=" + poBean.getGlProfitCenterCode() + ", location=" + poBean.getGlLocationCode() + ", cost center=" + poBean.getGlCostCenterCode() + ", account=" + poBean.getGlAccountNumber() + ", category=" + poBean.getGlCategoryCode() + ", dept=" + poBean.getGlDepartmentCode() + ", local=" + poBean.getGlLocal());
          poBean.addFatalErrorMessage("GL Account String is invalid for " + poBean.getOracleDatabaseName());
          return false;
        }

        String destOrgId = ValsparLookUps.queryForSingleValueLeaveConnectionOpen(conn, "select organization_id from hr_all_organization_units where location_id = ?", poBean.getShipToLocationId());

        pst.setStringAtName("INTERFACE_HEADER_ID", poBean.getInterfaceHeaderId());
        pst.setStringAtName("INTERFACE_LINE_ID", line.getInterfaceLineId());
        pst.setBigDecimalAtName("QUANTITY_ORDERED", line.getQuantity());
        // No accrual account for you!
        pst.setStringAtName("ACCRUAL_ACCOUNT_ID", "");
        pst.setStringAtName("CHARGE_ACCOUNT_ID", chargeAccountId);
        pst.setStringAtName("DELIVER_TO_LOCATION_ID", poBean.getShipToLocationId());
        pst.setStringAtName("DESTINATION_ORGANIZATION_ID", destOrgId);
        // No destination type code since this is a service not an item (no item ID)
        //pst.setStringAtName("DESTINATION_TYPE_CODE", "INVENTORY");
        pst.setStringAtName("DESTINATION_TYPE_CODE", "");
        pst.setStringAtName("ORG_ID", poBean.getOrgId());
        pst.execute();
      }
      return true;
    }
    catch (Exception e)
    {
      log4jLogger.error(e);
      poBean.addFatalErrorMessage("Error creating PO distribution interface record in 11i: " + e);
      return false;
    }
    finally
    {
      JDBCUtil.close(pst);
    }
  }

  private static void logContextFailure(DatabaseContextBean context)
  {
    String nonFatalErrorMessage = "An 11i database error occurred while importing this purchase order in " + ConnectionUtility.buildDatabaseName((OracleConnection) context.getConnection()) + ". Will try again shortly.";

    for (PoHeadersInterfaceBean poBean: context.getPurchaseOrders())
    {
      if (!poBean.isImportFailed())
      {
        poBean.addNonFatalErrorMessage(nonFatalErrorMessage);
      }
    }
  }

  public static boolean importPurchaseOrders(DatabaseContextBean context)
  {
    OracleConnection conn = (OracleConnection) context.getConnection();
    String poBatchId = context.getPoBatchId();

    try
    {
      debug("");
      debug("Submitting concurrent request to import POs in " + ConnectionUtility.buildDatabaseName(conn) + "/" + context.getOrgName() + " (Batch ID=" + poBatchId + ")...");
      int requestId = OracleAppsUtility.submitConcurrentRequest(conn, "PO", "POXPOPDOI", null, null, false, new String[] { null, "STANDARD", null, "N", null, "APPROVED", null, poBatchId, null, null });

      if (requestId <= 0)
      {
        String errorMessage = OracleAppsUtility.getLastErrorMessage(conn);
        log4jLogger.error("Failed to submit request to process POs in 11i, returned request ID was " + requestId + ".  Error message is: " + errorMessage);
        logContextFailure(context);
        return false;
      }
      else
      {
        debug("Waiting for request " + requestId + " to complete...");
        if (!OracleAppsUtility.waitForConcurrentRequest(conn, requestId, 5))
        {
          log4jLogger.error("request ID " + requestId + " did not complete successfully.");
          logContextFailure(context);
          return false;
        }
        debug("Request ID " + requestId + " completed successfully.  Checking for import errors...");
        if (!populateImportErrors(conn, requestId, context.getPurchaseOrders()))
        {
          log4jLogger.error("Unable to process import errors.");
          logContextFailure(context);
          return false;
        }
        debug("Fetching the 11i IDs for any imported POs...");
        if (!populate11iIds(conn, requestId, context.getPurchaseOrders()))
        {
          log4jLogger.error("Unable to get 11i IDs for imported POs");
          logContextFailure(context);
          return false;
        }

        return true;
      }
    }
    catch (Exception e)
    {
      log4jLogger.error(e);
      logContextFailure(context);
      return false;
    }
  }

  private static String lookupFriendlyErrorMessage(String errorCode)
  {
    StringBuilder sb = new StringBuilder();
    sb.append("select keyfield4 ");
    sb.append("from vca_lookups ");
    sb.append("where application = 'Valspar Custom Application' ");
    sb.append("  and keyfield1 = 'CIPAce' ");
    sb.append("  and keyfield2 = 'Error Message Replacement' ");
    sb.append("  and keyfield3 = ? ");

    return ValsparLookUps.queryForSingleValue(DataSource.NORTHAMERICAN, sb.toString(), errorCode);
  }

  private static boolean populateImportErrors(OracleConnection conn, int requestId, List<PoHeadersInterfaceBean> purchaseOrders)
  {
    OraclePreparedStatement pst = null;
    OracleResultSet rs = null;

    try
    {
      StringBuilder sb = new StringBuilder();

      sb.append("select distinct ");
      sb.append("       hdr.vendor_doc_num, ");
      sb.append("       err.interface_header_id, ");
      sb.append("       case (err.table_name) ");
      sb.append("         when 'PO_HEADERS_INTERFACE' then 'Header' ");
      sb.append("         when 'PO_LINES_INTERFACE' then 'Line ' || line.line_num ");
      sb.append("         when 'PO_DISTRIBUTIONS_INTERFACE' then 'Distribution for Line ' || line.line_num ");
      sb.append("       end location, ");
      sb.append("       case (err.table_name) ");
      sb.append("         when 'PO_HEADERS_INTERFACE' then 'INTERFACE_HEADER_ID=' || err.interface_header_id ");
      sb.append("         when 'PO_LINES_INTERFACE' then 'INTERFACE_LINE_ID=' || err.interface_line_id ");
      sb.append("         when 'PO_DISTRIBUTIONS_INTERFACE' then 'INTERFACE_DISTRIBUTION_ID=' || err.interface_distribution_id ");
      sb.append("       end id_text, ");
      sb.append("       err.table_name, ");
      sb.append("       err.column_name, ");
      sb.append("       err.error_message, ");
      sb.append("       err.error_message_name, ");
      sb.append("       err.interface_header_id, ");
      sb.append("       err.interface_line_id, ");
      sb.append("       err.interface_distribution_id ");
      sb.append("from po_interface_errors err ");
      sb.append("inner join po_headers_interface hdr ");
      sb.append("  on err.interface_header_id = hdr.interface_header_id ");
      sb.append("left outer join po_lines_interface line ");
      sb.append("  on err.interface_line_id = line.interface_line_id ");
      sb.append("left outer join po_distributions_interface dist ");
      sb.append("  on err.interface_distribution_id = dist.interface_distribution_id ");
      sb.append("where err.request_id = :REQUEST_ID  ");

      pst = (OraclePreparedStatement) conn.prepareStatement(sb.toString());
      pst.setIntAtName("REQUEST_ID", requestId);

      rs = (OracleResultSet) pst.executeQuery();

      while (rs.next())
      {
        String errorCode = rs.getString("error_message_name");

        String interfaceHeaderId = rs.getString("interface_header_id");
        for (PoHeadersInterfaceBean poBean: purchaseOrders)
        {
          if (StringUtils.equalsIgnoreCase(poBean.getInterfaceHeaderId(), interfaceHeaderId))
          {
            String message = rs.getString("error_message");
            String location = rs.getString("location");
            String friendlyErrorMessage = lookupFriendlyErrorMessage(errorCode);

            if (StringUtils.isNotEmpty(friendlyErrorMessage))
            {
              poBean.addFatalErrorMessage(friendlyErrorMessage);
            }
            else
            {
              poBean.addFatalErrorMessage(message + " (" + location + ")");
            }

            log4jLogger.error("PO " + poBean.getCipAcePoNumber() + " Failed Import. Message=" + message + ".  Error Code=" + errorCode);
            log4jLogger.error("    Location=" + location);
            log4jLogger.error("    Table.Column=" + rs.getString("table_name") + "." + rs.getString("column_name"));
            log4jLogger.error("    " + rs.getString("id_text"));
            break;
          }
        }
      }
      return true;
    }
    catch (Exception e)
    {
      log4jLogger.error(e);
      return false;
    }
    finally
    {
      JDBCUtil.close(pst, rs);
    }
  }

  public static boolean populate11iIds(OracleConnection conn, int requestId, List<PoHeadersInterfaceBean> purchaseOrders)
  {
    OraclePreparedStatement pst = null;
    OracleResultSet rs = null;
    boolean success = true;

    try
    {
      StringBuilder sb = new StringBuilder();

      sb.append("select po_header_id, segment1 ");
      sb.append("from po_headers_all pha ");
      sb.append("where pha.request_id = :REQUEST_ID  ");
      sb.append("  and vendor_order_num = :VENDOR_ORDER_NUM ");
      sb.append("  and document_creation_method = 'PDOI' ");

      pst = (OraclePreparedStatement) conn.prepareStatement(sb.toString());

      for (PoHeadersInterfaceBean poBean: purchaseOrders)
      {
        if (!poBean.isImportFailed())
        {
          pst.clearParameters();
          pst.setIntAtName("REQUEST_ID", requestId);
          pst.setStringAtName("VENDOR_ORDER_NUM", poBean.getCipAcePoNumber());

          rs = (OracleResultSet) pst.executeQuery();

          if (rs.next())
          {
            poBean.setOraclePoHeaderId(rs.getString("po_header_id"));
            poBean.setOraclePoNumber(rs.getString("segment1"));
          }
          else
          {
            String error = "Could not find a record in po_headers_all with request_id=" + requestId + " and vendor_order_num=" + poBean.getCipAcePoNumber();
            log4jLogger.error(error);
            poBean.addFatalErrorMessage(error);
          }

          JDBCUtil.close(rs);
        }
      }
    }
    catch (Exception e)
    {
      log4jLogger.error(e);
      success = false;
    }
    finally
    {
      JDBCUtil.close(pst, rs);
    }

    return success;
  }

  public static void sendDocumentsToBuyers(DatabaseContextBean context)
  {
    OracleConnection conn = (OracleConnection) context.getConnection();

    for (PoHeadersInterfaceBean poBean: context.getPurchaseOrders())
    {
      if (poBean.isImportSuccessful())
      {
        debug("");
        debug("Submitting concurrent request to generate PDF for PO " + poBean.getOraclePoNumber() + " in database " + ConnectionUtility.buildDatabaseName(conn) + "/" + context.getOrgName() + "...");
        generateAndSendPoPdf(conn, poBean);
      }
    }
  }

  public static boolean fakeStuff()
  {
    OracleConnection conn = null;
    File pdf = null;
    PoHeadersInterfaceBean poBean = new PurchaseOrderBean();
    poBean.setProjectId("100216");
    poBean.setProjectName("Team Test project 12_5_2013");
    poBean.setCipAcePoAutoId("101");
    poBean.setCipAcePoNumber("PO00000030");
    poBean.setRevisionNumber("0");
    poBean.setCipAceDatabaseName("NAPR");
    poBean.setShipToLocationId("500729");
    poBean.setShipToOrganizationCode("H18: CONS PAINT DROP - CHICAGO");
    poBean.setCurrencyCode("USD");
    poBean.setOrgId("165");
    poBean.setVendorId("33838");
    poBean.setVendorSiteId("64026");
    poBean.setOraclePoNumber("1471257");
    poBean.setUserName("SMLMIN");
    poBean.setOrgName("VALSPAR OPERATING UNIT");
    poBean.setOracleDatabaseName("NASP");
    try
    {
      conn = (OracleConnection) ConnectionAccessBean.getConnection(DataSource.NORTHAMERICAN);
      pdf = fetchPdfDocument(conn, poBean);

      if (pdf == null)
      {
        String errorMessage = "Unable to fetch PDF document from fnd_lobs.";
        log4jLogger.error(errorMessage);
        poBean.addNonFatalErrorMessage(errorMessage);
        return false;
      }
    }
    catch (Exception e)
    {
      log4jLogger.error(e);
    }
    finally
    {
      JDBCUtil.close(conn);
    }

    return sendPdf(poBean, pdf);
  }

  private static boolean generateAndSendPoPdf(OracleConnection conn, PoHeadersInterfaceBean poBean)
  {
    try
    {
      int requestId = OracleAppsUtility.submitConcurrentRequest(conn, "PO", "POXPOPDF", null, null, false, new String[] { "R", null, null, null, null, null, null, null, null, "N", null, null, null, null, null, null, "View", "N", "Y", "N", poBean.getOraclePoHeaderId(), poBean.getRevisionNumber(), "APPROVED", "STANDARD", null, null });

      if (requestId <= 0)
      {
        String errorMessage = OracleAppsUtility.getLastErrorMessage(conn);
        String fullErrorMessage = "Failed to submit request to generate PO PDF in 11i, returned request ID was " + requestId + ".  Error message is: " + errorMessage;
        log4jLogger.error(fullErrorMessage);
        poBean.addNonFatalErrorMessage(fullErrorMessage);
        return false;
      }
      else
      {
        debug("Waiting for request " + requestId + " to complete...");
        if (!OracleAppsUtility.waitForConcurrentRequest(conn, requestId, 1))
        {
          String errorMessage = "request ID " + requestId + " did not complete successfully.";
          log4jLogger.error(errorMessage);
          poBean.addNonFatalErrorMessage(errorMessage);
          return false;
        }
        debug("Request ID " + requestId + " completed successfully.  Fetching PDF from fnd_lobs...");
        File pdf = fetchPdfDocument(conn, poBean);

        if (pdf == null)
        {
          String errorMessage = "Unable to fetch PDF document from fnd_lobs.";
          log4jLogger.error(errorMessage);
          poBean.addNonFatalErrorMessage(errorMessage);
          return false;
        }

        return sendPdf(poBean, pdf);
      }
    }
    catch (Exception e)
    {
      String errorMessage = "Error generating/fetching/sending PDF for PO " + poBean.getOraclePoNumber();
      log4jLogger.error(e);
      poBean.addNonFatalErrorMessage(errorMessage);
      return false;
    }
  }

  private static File fetchPdfDocument(OracleConnection conn, PoHeadersInterfaceBean poBean)
  {
    OraclePreparedStatement pst = null;
    OracleResultSet rs = null;
    InputStream is = null;
    FileOutputStream fos = null;
    File pdf = null;

    try
    {
      StringBuilder sb = new StringBuilder();
      sb.append("select file_name, file_data ");
      sb.append("from fnd_lobs ");
      sb.append("where file_id = (select file_id ");
      sb.append("                from (select * ");
      sb.append("                      from (select file_id, file_name ");
      sb.append("                            from fnd_lobs ");
      sb.append("                            order by file_id desc) ");
      sb.append("                      where rownum < 100) ");
      sb.append("                where file_name like :file_name_pattern) ");

      pst = (OraclePreparedStatement) conn.prepareStatement(sb.toString());
      pst.setStringAtName("file_name_pattern", poBean.getPoPdfFilenamePattern());

      rs = (OracleResultSet) pst.executeQuery();

      if (rs.next())
      {
        String filename = rs.getString("file_name");
        pdf = new File(CommonUtility.getDataDirectoryPath() + filename);
        fos = new FileOutputStream(pdf);

        byte[] buffer = new byte[1024];
        is = rs.getBinaryStream("file_data");
        int len;
        while ((len = is.read(buffer)) > 0)
        {
          fos.write(buffer, 0, len);
        }

        fos.flush();
      }
      else
      {
        log4jLogger.error("Could not find PDF record in fnd_lobs for PO " + poBean.getOraclePoNumber() + ", was looking for file_name LIKE " + poBean.getPoPdfFilenamePattern());
        return null;
      }

      return pdf;
    }
    catch (Exception e)
    {
      log4jLogger.error(e);
      return null;
    }
    finally
    {
      JDBCUtil.close(pst, rs);
      CommonUtility.close(is);
      CommonUtility.close(fos);
    }
  }

  private static boolean sendPdf(PoHeadersInterfaceBean poBean, File pdf)
  {
    SimpleUserBean buyer = ADUtility.buildSimpleUserBeanFromUserName(poBean.getUserName());
    if (buyer == null)
    {
      String errorMessage = "Unable to find a user named " + poBean.getUserName() + " in Active Directory!  Cannot email PDF of purchase order";
      log4jLogger.error(errorMessage);
      poBean.addNonFatalErrorMessage(errorMessage);
      return false;
    }

    SimpleUserBean from = new SimpleUserBean();
    from.setFullName("CIPAce");
    from.setEmail("cipace@valspar.com");

    StringBuilder subject = new StringBuilder();
    subject.append(poBean.getOracleDatabaseName());
    subject.append(" Purchase Order ");
    subject.append(poBean.getOraclePoNumber());
    subject.append(" (");
    subject.append(poBean.getOrgName());
    subject.append(")");

    Map<String, Object> values = new HashMap<String, Object>();
    values.put("po", poBean);

    List<EmailAttachment> attachments = new ArrayList<EmailAttachment>();
    EmailAttachment attachment = new EmailAttachment();
    attachment.setPath(pdf.getPath());
    attachment.setName(pdf.getName());
    attachments.add(attachment);

    debug("Emailing " + pdf.getName() + " to " + buyer.getFullNameAndEmail() + "...");

    if (!NotificationUtility.sendNotification(buyer, from, subject.toString(), "cipace-purchase_order_generated.ftl", values, attachments))
    {
      String errorMessage = "Unable to email PDF to the buyer";
      log4jLogger.error(errorMessage);
      poBean.addNonFatalErrorMessage(errorMessage);
      return false;
    }

    return true;
  }

  public static boolean populatePurchaseOrderActivity(List<PurchaseOrderBean> purchaseOrders)
  {
    for (PurchaseOrderBean poBean: purchaseOrders)
    {
      OracleConnection conn = connections.get(poBean.getCipAceDatabaseName());
      poBean.setOracleDatabaseName(ConnectionUtility.buildDatabaseName(conn));
      poBean.setOrgName(lookupOrgName(conn, poBean.getOrgId()));

      if (!populatePurchaseOrderLinesSpend(conn, poBean))
      {
        return false;
      }
      if (!populatePurchaseOrderLinesReceipts(conn, poBean))
      {
        return false;
      }
    }

    return true;
  }

  public static boolean populatePurchaseOrderLinesSpend(OracleConnection conn, PurchaseOrderBean poBean)
  {
    OraclePreparedStatement pst = null;
    OracleResultSet rs = null;

    StringBuilder sb = new StringBuilder();
    sb.append("select nvl(sum(aida.amount), 0) line_invoiced_amount, ");
    sb.append("       nvl(sum(aida.quantity_invoiced), 0) line_invoiced_qty, ");
    sb.append("       nvl(sum(aida.amount) / nullif(sum(nvl(aida.quantity_invoiced, 0)), 0), 0) line_invoice_unit_price, ");
    sb.append("       aia.payment_currency_code ");
    sb.append("from po_headers_all pha ");
    sb.append("inner join po_lines_all pla ");
    sb.append("  on pha.po_header_id = pla.po_header_id ");
    sb.append("  and pla.line_num = :LINE_NUMBER ");
    sb.append("inner join po_distributions_all pda ");
    sb.append("  on pla.po_line_id = pda.po_line_id ");
    sb.append("left outer join ap_invoice_distributions_all aida ");
    sb.append("  on pda.po_distribution_id = aida.po_distribution_id ");
    sb.append("  and aida.reversal_flag is null  ");
    sb.append("left outer join ap_invoices_all aia ");
    sb.append("  on aida.invoice_id = aia.invoice_id ");
    sb.append("where pha.org_id = :ORG_ID ");
    sb.append("  and pha.segment1 = :ORACLE_PO_NUMBER ");
    sb.append("  and pha.vendor_order_num = :CIPACE_PO_NUMBER ");
    sb.append("group by aia.payment_currency_code ");

    try
    {
      pst = (OraclePreparedStatement) conn.prepareStatement(sb.toString());

      for (PurchaseOrderLineBean poLineBean: poBean.getLines())
      {
        pst.clearParameters();
        pst.setIntAtName("LINE_NUMBER", poLineBean.getLineNumber());
        pst.setStringAtName("ORG_ID", poBean.getOrgId());
        pst.setStringAtName("ORACLE_PO_NUMBER", poBean.getOraclePoNumber());
        pst.setStringAtName("CIPACE_PO_NUMBER", poBean.getCipAcePoNumber());

        rs = (OracleResultSet) pst.executeQuery();

        if (rs.next())
        {
          poLineBean.setInvoicedAmount(rs.getBigDecimal("line_invoiced_amount"));
          poLineBean.setInvoicedQuantity(rs.getBigDecimal("line_invoiced_qty"));
          poLineBean.setInvoicedUnitPrice(rs.getBigDecimal("line_invoice_unit_price"));
          poLineBean.setInvoiceCurrencyCode(rs.getString("payment_currency_code"));
        }

        JDBCUtil.close(rs);
      }

      return true;
    }
    catch (Exception e)
    {
      log4jLogger.error(e);
      return false;
    }
    finally
    {
      JDBCUtil.close(pst, rs);
    }
  }

  public static boolean populatePurchaseOrderLinesReceipts(OracleConnection conn, PurchaseOrderBean poBean)
  {
    OraclePreparedStatement pst = null;
    OracleResultSet rs = null;

    StringBuilder sb = new StringBuilder();
    sb.append("select pha.segment1, sum(nvl(rt.quantity, 0)) received_quantity ");
    sb.append("from po_headers_all pha ");
    sb.append("inner join po_lines_all pla ");
    sb.append("  on pha.po_header_id = pla.po_header_id ");
    sb.append("  and pla.line_num = :LINE_NUMBER ");
    sb.append("left outer join rcv_transactions rt ");
    sb.append("  on pha.po_header_id = rt.po_header_id ");
    sb.append("  and pla.po_line_id = rt.po_line_id ");
    sb.append("  and rt.destination_type_code = 'RECEIVING' ");
    sb.append("where pha.org_id = :ORG_ID ");
    sb.append("  and pha.segment1 = :ORACLE_PO_NUMBER ");
    sb.append("  and pha.vendor_order_num = :CIPACE_PO_NUMBER ");
    sb.append("group by pha.segment1 ");

    try
    {
      pst = (OraclePreparedStatement) conn.prepareStatement(sb.toString());

      for (PurchaseOrderLineBean poLineBean: poBean.getLines())
      {
        pst.clearParameters();
        pst.setIntAtName("LINE_NUMBER", poLineBean.getLineNumber());
        pst.setStringAtName("ORG_ID", poBean.getOrgId());
        pst.setStringAtName("ORACLE_PO_NUMBER", poBean.getOraclePoNumber());
        pst.setStringAtName("CIPACE_PO_NUMBER", poBean.getCipAcePoNumber());

        rs = (OracleResultSet) pst.executeQuery();

        if (rs.next())
        {
          poLineBean.setReceivedQuantity(rs.getBigDecimal("received_quantity"));
        }

        JDBCUtil.close(rs);
      }

      return true;
    }
    catch (Exception e)
    {
      log4jLogger.error(e);
      return false;
    }
    finally
    {
      JDBCUtil.close(pst, rs);
    }
  }
  /*
  private static boolean updatePoLine(OracleConnection conn, POChangeOrderBean poChangeOrderBean)
  {
    OracleCallableStatement cst = null;
    boolean success = false;

    try
    {
      StringBuilder sb = new StringBuilder();
      sb.append("{call PO_CHANGE_API1_S.update_po (");
      sb.append("         :X_PO_NUMBER, ");
      sb.append("         :X_RELEASE_NUMBER, ");
      sb.append("         :X_REVISION_NUMBER, ");
      sb.append("         :X_LINE_NUMBER, ");
      sb.append("         :X_SHIPMENT_NUMBER, ");
      sb.append("         :NEW_QUANTITY, ");
      sb.append("         :NEW_PRICE, ");
      sb.append("         :NEW_PROMISED_DATE, ");
      sb.append("         :LAUNCH_APPROVALS_FLAG, ");
      sb.append("         :UPDATE_SOURCE, ");
      sb.append("         :VERSION, ");
      sb.append("         :X_OVERRIDE_DATE, ");
      sb.append("         :X_API_ERRORS, ");
      sb.append("         :p_BUYER_NAME)}");

      cst = (OracleCallableStatement) conn.prepareCall(sb.toString());
      cst.setStringAtName("X_PO_NUMBER", poChangeOrderBean.getOraclePoNumber());
      cst.setStringAtName("X_RELEASE_NUMBER", null);
      cst.setStringAtName("X_REVISION_NUMBER", poChangeOrderBean.getRevisionNumber());
      cst.setIntAtName("X_LINE_NUMBER", poChangeOrderBean.getPoLineNumber());
      cst.setStringAtName("X_SHIPMENT_NUMBER", null);
      cst.setBigDecimalAtName("NEW_QUANTITY", poChangeOrderBean.getLatestQuantity());
      cst.setBigDecimalAtName("NEW_PRICE", poChangeOrderBean.getLatestUnitPrice());
      cst.setDATEAtName("NEW_PROMISED_DATE", null);
      cst.setStringAtName("LAUNCH_APPROVALS_FLAG", "N");
      cst.setStringAtName("UPDATE_SOURCE", null);
      cst.setStringAtName("VERSION", "1.0");
      cst.setDATEAtName("X_OVERRIDE_DATE", null);
      cst.setStringAtName("X_API_ERRORS", "");
      cst.setStringAtName("p_BUYER_NAME", null);
      cst.execute();

      String returnStatus = cst.getString(4);

      success = StringUtils.equals(returnStatus, "S");

      if (!success)
      {
        // TO DO get the 11i error message from fnd_msg_pub.get
        poChangeOrderBean.addFatalErrorMessage("Error cancelling PO Line in 11i (11i did not return an error message)");
      }
    }
    catch (Exception e)
    {
      success = false;
      log4jLogger.error(e);
      poChangeOrderBean.addFatalErrorMessage("Error cancelling PO Line in 11i: " + e);
    }
    finally
    {
      JDBCUtil.close(cst);
    }

    return success;
  }
*/

  private static boolean cancelPo(OracleConnection conn, POChangeOrderBean poChangeOrderBean)
  {
    OracleCallableStatement cst = null;
    boolean success = false;

    try
    {
      StringBuilder sb = new StringBuilder();
      sb.append("{call PO_Document_Control_PUB.control_document (");
      sb.append("         :p_api_version, ");
      sb.append("         :p_init_msg_list, ");
      sb.append("         :p_commit, ");
      sb.append("         :x_return_status, ");
      sb.append("         :p_doc_type, ");
      sb.append("         :p_doc_subtype, ");
      sb.append("         :p_doc_id, ");
      sb.append("         :p_doc_num, ");
      sb.append("         :p_release_id, ");
      sb.append("         :p_release_num, ");
      sb.append("         :p_doc_line_id, ");
      sb.append("         :p_doc_line_num, ");
      sb.append("         :p_doc_line_loc_id, ");
      sb.append("         :p_doc_shipment_num, ");
      sb.append("         :p_action, ");
      sb.append("         :p_action_date, ");
      sb.append("         :p_cancel_reason, ");
      sb.append("         :p_cancel_reqs_flag, ");
      sb.append("         :p_print_flag, ");
      sb.append("         :p_note_to_vendor, ");
      sb.append("         :p_use_gldate)}");

      cst = (OracleCallableStatement) conn.prepareCall(sb.toString());
      cst.setStringAtName("p_api_version", "1");
      cst.setStringAtName("p_init_msg_list", "T");
      cst.setStringAtName("p_commit", "T");
      cst.registerOutParameter(4, OracleTypes.VARCHAR);
      cst.setStringAtName("p_doc_type", "PO");
      cst.setStringAtName("p_doc_subtype", "STANDARD");
      cst.setStringAtName("p_doc_id", null);
      cst.setStringAtName("p_doc_num", poChangeOrderBean.getOraclePoNumber());
      cst.setStringAtName("p_release_id", null);
      cst.setStringAtName("p_release_num", null);
      cst.setStringAtName("p_doc_line_id", null);
      //cst.setIntAtName("p_doc_line_num", poChangeOrderBean.getPoLineNumber());
      cst.setStringAtName("p_doc_line_num", null);
      cst.setStringAtName("p_doc_line_loc_id", null);
      cst.setStringAtName("p_doc_shipment_num", null);
      cst.setStringAtName("p_action", "CANCEL");
      cst.setStringAtName("p_action_date", null);
      cst.setStringAtName("p_cancel_reason", null);
      cst.setStringAtName("p_cancel_reqs_flag", null);
      cst.setStringAtName("p_print_flag", null);
      cst.setStringAtName("p_note_to_vendor", null);
      cst.setStringAtName("p_use_gldate", null);
      cst.execute();

      String returnStatus = cst.getString(4);

      success = StringUtils.equals(returnStatus, "S");

      if (!success)
      {
        String errorMessage = getLastOracleErrorMessage(conn);

        if (StringUtils.isEmpty(errorMessage))
        {
          errorMessage = "[11i API did not return an error message (Has the PO been received at all?)]";
        }
        debug("      Unable to cancel PO in 11i: " + errorMessage);
        poChangeOrderBean.addFatalErrorMessage("Unable to cancel PO in 11i: " + errorMessage);
      }
    }
    catch (Exception e)
    {
      success = false;
      log4jLogger.error("PO# " + poChangeOrderBean.getOraclePoNumber() + ", Org " + poChangeOrderBean.getOrgName(), e);
      poChangeOrderBean.addFatalErrorMessage("Error cancelling PO in 11i: " + e);
    }
    finally
    {
      JDBCUtil.close(cst);
    }

    return success;
  }

  public static String getLastOracleErrorMessage(OracleConnection conn)
  {
    // -3 = FND_MSG_PUB.G_LAST for which message
    //  F = FND_API.G_FALSE for whether to encode the message

    return ValsparLookUps.queryForSingleValueLeaveConnectionOpen(conn, "select FND_MSG_PUB.GET ( -3, 'F') from dual");
  }

  public static List<VendorSiteBean> fetchOracleVendorSites(OracleConnection conn, String oracleDatabaseName, String cipRegionId) throws Exception
  {
    OraclePreparedStatement pst = null;
    OracleResultSet rs = null;
    List<VendorSiteBean> beans = new ArrayList<VendorSiteBean>();

    try
    {
      StringBuilder sb = new StringBuilder();
      sb.append("select substr(pvsa.address_line1,1,100) address_line1, ");
      sb.append("       pvsa.zip, ");
      sb.append("       substr(vend_hla.description,1,100) hdr_bill_to_location, ");
      sb.append("       pv.bill_to_location_id hdr_bill_to_location_id, ");
      sb.append("       pvsa.city, ");
      sb.append("       pvsa.country, ");
      sb.append("       substr(haou.name,1,100) operating_unit, ");
      sb.append("       pvsa.org_id, ");
      sb.append("       substr(pvsa.province,1,100) province, ");
      sb.append("       substr(site_hla.description,1,100) site_bill_to_location, ");
      sb.append("       pvsa.bill_to_location_id site_bill_to_location_id, ");
      sb.append("       substr(pvsa.state,1,100) state, ");
      sb.append("       pv.vendor_id, ");
      sb.append("       substr(pv.vendor_name,1,100) vendor_name, ");
      sb.append("       pvsa.vendor_site_code, ");
      sb.append("       pvsa.vendor_site_id, ");
      sb.append("       pv.vendor_type_lookup_code ");
      sb.append("from po_vendors pv ");
      sb.append("inner join po_vendor_sites_all pvsa ");
      sb.append("  on pv.vendor_id = pvsa.vendor_id ");
      sb.append("  and nvl(pvsa.inactive_date, sysdate + 1) >= sysdate ");
      sb.append("  and pvsa.purchasing_site_flag = 'Y' ");
      sb.append("inner join hr_all_organization_units haou ");
      sb.append("  on pvsa.org_id = haou.organization_id ");
      sb.append("  and nvl(haou.date_to, sysdate + 1) >= sysdate ");
      sb.append("left outer join hr_locations_all vend_hla ");
      sb.append("  on pv.bill_to_location_id = vend_hla.location_id ");
      sb.append("left outer join hr_locations_all site_hla ");
      sb.append("  on pvsa.bill_to_location_id = site_hla.location_id ");
      sb.append("where nvl(pv.end_date_active, sysdate + 1) >= sysdate ");
      sb.append("order by lower(pv.vendor_name), lower(pvsa.vendor_site_code) ");

      pst = (OraclePreparedStatement) conn.prepareStatement(sb.toString());
      rs = (OracleResultSet) pst.executeQuery();

      while (rs.next())
      {
        VendorSiteBean bean = new VendorSiteBean();
        bean.setOracleDatabaseName(oracleDatabaseName);
        bean.setAddressLine(rs.getString("address_line1"));
        bean.setZipCode(rs.getString("zip"));
        bean.setBillTo(rs.getString("hdr_bill_to_location"));
        bean.setBillToLocationId(rs.getString("hdr_bill_to_location_id"));
        bean.setCity(rs.getString("city"));
        bean.setCountry(rs.getString("country"));
        bean.setOperatingUnit(rs.getString("operating_unit"));
        bean.setOrgId(rs.getString("org_id"));
        bean.setProvince(rs.getString("province"));
        bean.setCipRegionId(cipRegionId);
        bean.setSiteBillToLocation(rs.getString("site_bill_to_location"));
        bean.setSiteBillToLocationId(rs.getString("site_bill_to_location_id"));
        bean.setState(rs.getString("state"));
        bean.setVendorId(rs.getString("vendor_id"));
        bean.setName(rs.getString("vendor_name"));
        bean.setVendorSiteCode(rs.getString("vendor_site_code"));
        bean.setVendorSiteId(rs.getString("vendor_site_id"));
        bean.setVendorType(rs.getString("vendor_type_lookup_code"));
        bean.setActive(true);

        beans.add(bean);
      }
    }
    finally
    {
      JDBCUtil.close(pst, rs);
    }

    return beans;
  }

  public static List<CurrencyExchangeRateBean> fetchOracleCurrencyExchangeRates(OracleConnection conn, String periodName) throws Exception
  {
    OraclePreparedStatement pst = null;
    OracleResultSet rs = null;

    List<CurrencyExchangeRateBean> beans = new ArrayList<CurrencyExchangeRateBean>();

    try
    {
      StringBuilder sb = new StringBuilder();
      sb.append("select distinct ");
      sb.append("       b.currency_code from_currency_code, ");
      sb.append("       r.to_currency_code, ");
      sb.append("       r.eop_rate, ");
      sb.append("       to_char(gps.end_date, 'MON-YYYY') note ");
      sb.append("from gl_translation_rates r ");
      sb.append("inner join gl_sets_of_books b ");
      sb.append("  on r.set_of_books_id = b.set_of_books_id ");
      sb.append("inner join gl_period_statuses gps ");
      sb.append("  on gps.period_name = r.period_name ");
      sb.append("where r.period_name = :PERIOD_NAME ");
      sb.append("  and r.to_currency_code = 'USD' ");

      pst = (OraclePreparedStatement) conn.prepareStatement(sb.toString());
      pst.setStringAtName("PERIOD_NAME", periodName);

      rs = (OracleResultSet) pst.executeQuery();

      while (rs.next())
      {
        CurrencyExchangeRateBean bean = new CurrencyExchangeRateBean();
        bean.setFromCurrencyCode(rs.getString("from_currency_code"));
        bean.setToCurrencyCode(rs.getString("to_currency_code"));
        bean.setRate(rs.getBigDecimal("eop_rate"));
        bean.setPeriodName(periodName);
        bean.setNote(rs.getString("note"));
        beans.add(bean);
      }
    }
    finally
    {
      JDBCUtil.close(pst, rs);
    }

    return beans;
  }

  public static String lookupFiscalPeriodName(Connection conn)
  {
    StringBuilder sb = new StringBuilder();
    sb.append("select distinct first_value(gps.period_name) over (order by gps.start_date desc) ");
    sb.append("from gl_period_statuses gps ");
    sb.append("inner join gl_sets_of_books sob ");
    sb.append("  on gps.set_of_books_id = sob.set_of_books_id ");
    sb.append("  and sob.name not like '%STAT%' ");
    sb.append("where gps.application_id = 101 ");
    sb.append("  and gps.adjustment_period_flag = 'N' ");
    sb.append("  and sysdate > gps.end_date ");

    return ValsparLookUps.queryForSingleValueLeaveConnectionOpen(conn, sb.toString());
  }

  public static void setConnections(Map<String, OracleConnection> connections)
  {
    OracleDAO.connections = connections;
  }

  public static Map<String, OracleConnection> getConnections()
  {
    return connections;
  }
}
