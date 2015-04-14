package com.valspar.interfaces.purchasing.cipace.dao;

import com.valspar.interfaces.common.utils.*;
import com.valspar.interfaces.purchasing.cipace.beans.*;
import com.valspar.interfaces.purchasing.cipace.enums.PoFetchMode;
import java.math.BigDecimal;
import java.sql.*;
import java.util.*;
import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;

public final class CIPAceDAO
{
  private static Logger log4jLogger = Logger.getLogger(CIPAceDAO.class);
  private static Connection connection = null;

  private CIPAceDAO()
  {
  }

  public static void initialize(Connection connection)
  {
    CIPAceDAO.setConnection(connection);
  }

  private static void debug(String message)
  {
    log4jLogger.info(message);
  }

  public static List<PurchaseOrderBean> fetchPurchaseOrders(PoFetchMode poFetchMode)
  {
    return fetchPurchaseOrders(poFetchMode, null);
  }

  private static PurchaseOrderBean fetchPurchaseOrder(String cipAcePurchaseOrderNumber)
  {
    List<PurchaseOrderBean> pos = fetchPurchaseOrders(null, cipAcePurchaseOrderNumber);

    if (pos.size() == 1)
    {
      return pos.get(0);
    }
    else
    {
      return null;
    }
  }

  private static List<PurchaseOrderBean> fetchPurchaseOrders(PoFetchMode poFetchMode, String cipAcePurchaseOrderNumber)
  {
    CallableStatement cst = null;
    ResultSet rs = null;
    List<PurchaseOrderBean> beans = new ArrayList<PurchaseOrderBean>();

    try
    {
      /*
       *
      CREATE PROCEDURE [dbo].[CIP_SearchPurchaseOrders_Valspar]
            @PurchaseOrderName NVARCHAR(100) = '' ,
            @PurchaseOrderNumber NVARCHAR(255) = '' ,
            @VendorAutoID INT = 0 ,
            @OraclePONo NVARCHAR(500) = '' ,
            @PurchaseOrderStatusID INT = 0 ,
            @OrderDateFrom DATETIME = NULL ,
            @OrderDateTo DATETIME = NULL ,
            @UpdateDateFrom DATETIME = NULL ,
            @UpdateDateTo DATETIME = NULL ,
            @OraclePoLinked NVARCHAR(1) = ''
       */

      cst = connection.prepareCall("{call dbo.CIP_SearchPurchaseOrders_Valspar(?,?,?)}");

      String poNumber = "";
      String poStatusId = "";
      String oraclePoLinked = "";

      if (StringUtils.isNotEmpty(cipAcePurchaseOrderNumber))
      {
        poNumber = cipAcePurchaseOrderNumber;
      }

      if (PoFetchMode.LINKED.equals(poFetchMode))
      {
        poStatusId = getPurchaseOrderStatusId("PO Generated in 11i");
        oraclePoLinked = "Y";
      }
      else if (PoFetchMode.NOT_LINKED.equals(poFetchMode))
      {
        poStatusId = getPurchaseOrderStatusId("Ready to Submit to 11i");
        oraclePoLinked = "N";
      }

      cst.setString("@PurchaseOrderNumber", poNumber);
      cst.setString("@PurchaseOrderStatusID", poStatusId);
      cst.setString("@OraclePoLinked", oraclePoLinked);

      rs = cst.executeQuery();

      while (rs.next())
      {
        PurchaseOrderBean bean = new PurchaseOrderBean();
        bean.setProjectId(rs.getString("ProjectID"));
        bean.setProjectName(rs.getString("ProjectName"));
        bean.setCipAcePoAutoId(rs.getString("PurchaseOrderAutoID"));
        bean.setCipAcePoNumber(rs.getString("PurchaseOrderNumber"));
        bean.setRevisionNumber("0");
        bean.setCipAceDatabaseName(rs.getString("DatabaseName"));
        bean.setShipToOrganizationCode(rs.getString("ShipToOrganizationCode"));
        bean.setShipToOrganizationName(rs.getString("ShipToOrganizationName"));
        bean.setCurrencyCode(rs.getString("CurrencyCode"));
        bean.setOrgId(rs.getString("OrgId"));
        bean.setVendorId(rs.getString("VendorId"));
        bean.setVendorSiteId(rs.getString("VendorSiteId"));
        bean.setGlCompanyCode(rs.getString("co_code"));
        bean.setGlProfitCenterCode(rs.getString("profit_center"));
        bean.setGlLocationCode(rs.getString("location"));
        bean.setGlCostCenterCode(rs.getString("cost_center"));
        bean.setGlAccountNumber(rs.getString("account"));
        bean.setGlCategoryCode(rs.getString("category"));
        bean.setGlDepartmentCode(rs.getString("dept"));
        bean.setOraclePoNumber(rs.getString("cstm_OraclePONo"));
        bean.setUserName(rs.getString("TriggerUserLoginName"));
        bean.setLines(fetchPurchaseOrderLines(rs.getString("PurchaseOrderAutoID")));

        beans.add(bean);
      }
    }
    catch (Exception e)
    {
      log4jLogger.error(e);
    }
    finally
    {
      JDBCUtil.close(cst, rs);
    }

    return beans;
  }

  private static List<PurchaseOrderLineBean> fetchPurchaseOrderLines(String purchaseOrderAutoId)
  {
    CallableStatement cst = null;
    ResultSet rs = null;
    List<PurchaseOrderLineBean> lines = new ArrayList<PurchaseOrderLineBean>();

    try
    {
      /*
       * CREATE  PROCEDURE [dbo].[CIP_GetPOLineItems_Valspar]
            @PurchaseOrderAutoID INT = 0 ,
            @PONumber NVARCHAR(255) = '' ,
            @LineItemNumber NVARCHAR(100) = ''
       */

      cst = connection.prepareCall("{call dbo.CIP_GetPOLineItems_Valspar(?)}");
      cst.setString("@PurchaseOrderAutoID", purchaseOrderAutoId);

      rs = cst.executeQuery();

      while (rs.next())
      {
        PurchaseOrderLineBean bean = new PurchaseOrderLineBean();
        bean.setCipAcePoLineAutoId(rs.getString("PurchaseOrderLineItemAutoID"));
        bean.setLineNumber(rs.getInt("LineItemNumber"));
        bean.setUnitPrice(rs.getBigDecimal("LatestUnitPrice"));
        bean.setQuantity(rs.getBigDecimal("LatestQuantity"));
        bean.setPromisedDate(rs.getTimestamp("NeedByDate"));
        bean.setItemCategory(rs.getString("ItemCategory"));
        bean.setUom(rs.getString("UnitShortName"));
        bean.setItemDescription(rs.getString("ItemDescription"));

        lines.add(bean);
      }
    }
    catch (Exception e)
    {
      log4jLogger.error(e);
    }
    finally
    {
      JDBCUtil.close(cst, rs);
    }

    return lines;
  }

  public static List<POChangeOrderBean> fetchPOChangeOrders()
  {
    CallableStatement cst = null;
    ResultSet rs = null;
    List<POChangeOrderBean> beans = new ArrayList<POChangeOrderBean>();

    try
    {
      /*
       *
      CREATE PROCEDURE [dbo].[CIP_GetChangeOrders_Valspar]
          @PurchaseOrderLineItemAutoID INT = 0 ,
          @StatusID INT = 0 ,
          @CreateOnFrom DATETIME = NULL ,
          @CreateOnTo DATETIME = NULL
       */

      cst = connection.prepareCall("{call dbo.CIP_GetChangeOrders_Valspar(?)}");
      String poChangeOrderStatusName = "Ready to Submit to 11i";

      cst.setString("@StatusID", getPOChangeOrderStatusId(poChangeOrderStatusName));
      //cst.setString("@StatusID", "82");

      rs = cst.executeQuery();

      while (rs.next())
      {
        POChangeOrderBean bean = new POChangeOrderBean();
        bean.setPoChangeOrderAutoId(rs.getString("POChangeOrderAutoID"));
        bean.setAuthorizedBy(rs.getString("AuthorizedBy"));
        bean.setChangedDate(rs.getTimestamp("ChangedDate"));
        bean.setStatusId(rs.getString("StatusID"));
        bean.setChangedQuantity(rs.getBigDecimal("ChangedQuantity"));
        bean.setChangedUnitPrice(rs.getBigDecimal("ChangedUnitPrice"));
        bean.setCreatedOn(rs.getTimestamp("CreatedOn"));
        bean.setDescription(rs.getString("Description"));
        bean.setLatestAmount(rs.getBigDecimal("LatestAmount"));
        bean.setLatestQuantity(rs.getBigDecimal("LatestQuantity"));
        bean.setLatestUnitPrice(rs.getBigDecimal("LatestUnitPrice"));
        bean.setPurchaseOrderLineItemAutoId(rs.getString("PurchaseOrderLineItemAutoID"));
        bean.setVariance(rs.getBigDecimal("Variance"));
        bean.setCipAceDatabaseName(rs.getString("DatabaseName"));
        bean.setOrgId(rs.getString("OrgId"));
        bean.setCipAcePoNumber(rs.getString("PurchaseOrderNumber"));
        bean.setOraclePoNumber(rs.getString("cstm_OraclePONo"));
        bean.setPoLineNumber(rs.getInt("LineItemNumber"));

        // We will also probably need these fields if we need to send an updated PO PDF
        //bean.setProjectId(rs.getString("ProjectID"));
        //bean.setProjectName(rs.getString("ProjectName"));
        //bean.setCipAcePoAutoId(rs.getString("PurchaseOrderAutoID"));
        //bean.setUserName(rs.getString("TriggerUserLoginName"));

        PurchaseOrderBean purchaseOrder = fetchPurchaseOrder(bean.getCipAcePoNumber());

        if (purchaseOrder == null)
        {
          log4jLogger.error("ERROR, could not find CIPAce purchase order # " + bean.getCipAcePoNumber() + " for Change Order Auto ID=" + bean.getPoChangeOrderAutoId());
          continue;
        }

        bean.setPurchaseOrder(purchaseOrder);

        beans.add(bean);
      }
    }
    catch (Exception e)
    {
      log4jLogger.error(e);
    }
    finally
    {
      JDBCUtil.close(cst, rs);
    }

    return beans;
  }

  private static String getPurchaseOrderStatusId(String poStatusName)
  {
    CallableStatement cst = null;
    ResultSet rs = null;
    String statusId = null;

    try
    {
      /*
       * CREATE  PROCEDURE [dbo].[CIP_GetPOStatuses_Valspar]
            @PurchaseOrderStatusName NVARCHAR(100) = ''
       */

      cst = connection.prepareCall("{call dbo.CIP_GetPOStatuses_Valspar(?)}");
      cst.setString("@PurchaseOrderStatusName", poStatusName);

      rs = cst.executeQuery();

      if (rs.next())
      {
        statusId = rs.getString("PuchaseOrderStatusID");
      }
    }
    catch (Exception e)
    {
      log4jLogger.error(e);
    }
    finally
    {
      JDBCUtil.close(cst, rs);
    }

    return statusId;
  }

  private static String getPOChangeOrderStatusId(String poChangeOrderStatusName)
  {
    CallableStatement cst = null;
    ResultSet rs = null;
    String statusId = null;

    try
    {
      /*
       * CREATE  PROCEDURE [dbo].[CIP_GetAllCOStatus_Valspar]
       */

      cst = connection.prepareCall("{call dbo.CIP_GetAllCOStatus_Valspar()}");
      //cst.setString("@PurchaseOrderStatusName", poChangeOrderStatusName);

      rs = cst.executeQuery();

      while (rs.next())
      {
        String statusName = rs.getString("StatusName");

        if (StringUtils.equalsIgnoreCase(poChangeOrderStatusName, statusName))
        {
          statusId = rs.getString("ChangeOrderStatusID");
          break;
        }
      }
    }
    catch (Exception e)
    {
      log4jLogger.error(e);
    }
    finally
    {
      JDBCUtil.close(cst, rs);
    }

    return statusId;
  }

  public static boolean updatePurchaseOrders(List<PurchaseOrderBean> purchaseOrders)
  {
    for (PurchaseOrderBean poBean: purchaseOrders)
    {
      if (!updatePurchaseOrder(poBean))
      {
        return false;
      }
    }

    return true;
  }

  private static boolean updatePurchaseOrder(PoHeadersInterfaceBean poBean)
  {
    CallableStatement cst = null;

    try
    {
      /*
        CREATE  PROCEDURE [dbo].[CIP_UpdatePurchaseOrder_Valspar]
            @LoginUser NVARCHAR(20) = 'Oracle11iUser' ,
            @PurchaseOrderAutoID INT ,
            @PurchaseOrderStatusID INT ,
            @POApprovedDate DATETIME ,
            @OraclePONo NVARCHAR(500) ,
            @SubmissionErrMsg NVARCHAR(500)
     */
      cst = connection.prepareCall("{call dbo.CIP_UpdatePurchaseOrder_Valspar(?,?,?,?,?)}");

      String statusIdSuccess = getPurchaseOrderStatusId("PO Generated in 11i");
      String statusIdFail = getPurchaseOrderStatusId("Submission Fail");

      if (poBean.isImportFailed())
      {
        debug("  Updating CIPAce PO - Fail (PurchaseOrderAutoID=" + poBean.getCipAcePoAutoId() + ")");
        cst.clearParameters();
        cst.setString("@PurchaseOrderAutoID", poBean.getCipAcePoAutoId());
        cst.setString("@PurchaseOrderStatusID", statusIdFail);
        cst.setTimestamp("@POApprovedDate", null);
        cst.setString("@OraclePONo", "");
        cst.setString("@SubmissionErrMsg", poBean.getSubmissionErrorMessage());
        cst.execute();
      }
      else if (poBean.isImportSuccessful())
      {
        debug("  Updating CIPAce PO - Success (PurchaseOrderAutoID=" + poBean.getCipAcePoAutoId() + ") with Oracle PO# " + poBean.getOraclePoNumber());
        cst.clearParameters();
        cst.setString("@PurchaseOrderAutoID", poBean.getCipAcePoAutoId());
        cst.setString("@PurchaseOrderStatusID", statusIdSuccess);
        cst.setTimestamp("@POApprovedDate", getNow());
        cst.setString("@OraclePONo", poBean.getOraclePoNumber());
        cst.setString("@SubmissionErrMsg", poBean.getSubmissionErrorMessage());
        cst.execute();
      }
      else if (poBean.hasErrorMessages())
      {
        debug("  Updating CIPAce PO - Non Fatal Errors, will try again (PurchaseOrderAutoID=" + poBean.getCipAcePoAutoId() + ")");
        cst.clearParameters();
        cst.setString("@PurchaseOrderAutoID", poBean.getCipAcePoAutoId());
        cst.setString("@PurchaseOrderStatusID", "-1");
        cst.setTimestamp("@POApprovedDate", null);
        cst.setString("@OraclePONo", "");
        cst.setString("@SubmissionErrMsg", poBean.getSubmissionErrorMessage());
        cst.execute();
      }

      return true;
    }
    catch (Exception e)
    {
      log4jLogger.error("PurchaseOrderAutoID=" + poBean.getCipAcePoAutoId(), e);
      return false;
    }
    finally
    {
      JDBCUtil.close(cst);
    }
  }

  public static boolean updatePurchaseOrderChangeOrders(List<POChangeOrderBean> poChangeOrders)
  {
    for (POChangeOrderBean coBean: poChangeOrders)
    {
      if (!updatePurchaseOrderChangeOrder(coBean))
      {
        return false;
      }

      if (StringUtils.isNotEmpty(coBean.getCipAcePoAutoId()) && !coBean.isCancel())
      {
        updatePurchaseOrder(coBean);
      }
    }

    return true;
  }

  private static boolean updatePurchaseOrderChangeOrder(POChangeOrderBean coBean)
  {
    CallableStatement cst = null;

    try
    {
      /*
        CREATE  PROCEDURE [dbo].[CIP_UpdateChangeOrder_Valspar]
            @LoginUser NVARCHAR(20) = 'Oracle11iUser' ,
            @POChangeOrderAutoID INT ,
            @POChangeOrderStatusID INT ,
            @SubmissionErrMsg NVARCHAR(500)
     */
      cst = connection.prepareCall("{call dbo.CIP_UpdateChangeOrder_Valspar(?,?,?)}");

      String statusIdSuccess = getPOChangeOrderStatusId("PO Change Order Generated in 11i");
      String statusIdFail = getPOChangeOrderStatusId("Submission Fail");

      if (coBean.isImportFailed())
      {
        debug("  Updating CIPAce PO Change Order - Fail (POChangeOrderAutoID=" + coBean.getPoChangeOrderAutoId() + ")");
        cst.clearParameters();
        cst.setString("@POChangeOrderAutoID", coBean.getPoChangeOrderAutoId());
        cst.setString("@POChangeOrderStatusID", statusIdFail);
        cst.setString("@SubmissionErrMsg", coBean.getSubmissionErrorMessage());
        cst.execute();
      }
      else if (coBean.isImportSuccessful())
      {
        debug("  Updating CIPAce PO Change Order - Success (POChangeOrderAutoID=" + coBean.getPoChangeOrderAutoId() + ")");
        cst.clearParameters();
        cst.setString("@POChangeOrderAutoID", coBean.getPoChangeOrderAutoId());
        cst.setString("@POChangeOrderStatusID", statusIdSuccess);
        cst.setString("@SubmissionErrMsg", coBean.getSubmissionErrorMessage());
        cst.execute();
      }
      else if (coBean.hasErrorMessages())
      {
        debug("  Updating CIPAce PO Change Order - Non Fatal Errors, will try again (POChangeOrderAutoID=" + coBean.getPoChangeOrderAutoId() + ")");
        cst.clearParameters();
        cst.setString("@POChangeOrderAutoID", coBean.getPoChangeOrderAutoId());
        cst.setString("@POChangeOrderStatusID", "-1");
        cst.setString("@SubmissionErrMsg", coBean.getSubmissionErrorMessage());
        cst.execute();
      }

      return true;
    }
    catch (Exception e)
    {
      log4jLogger.error("POChangeOrderAutoID=" + coBean.getPoChangeOrderAutoId(), e);
      return false;
    }
    finally
    {
      JDBCUtil.close(cst);
    }
  }

  public static boolean updatePurchaseOrderLines(List<PurchaseOrderBean> purchaseOrders)
  {
    for (PurchaseOrderBean poBean: purchaseOrders)
    {
      for (PurchaseOrderLineBean poLineBean: poBean.getLines())
      {
        debug("  " + poBean.getOracleDatabaseName() + " PO " + poBean.getOraclePoNumber() + " (" + poBean.getOrgName() + ")/CIPAce PO " + poBean.getCipAcePoNumber() + ", Line " + poLineBean.getLineNumber());
        debug("    Updating PO Line received quantity to " + poLineBean.getReceivedQuantity().toPlainString() + " " + poLineBean.getUom() + " (PoLineAutoID=" + poLineBean.getCipAcePoLineAutoId() + ")...");
        updatePurchaseOrderLine(poLineBean);

        ActualExpenseBean actualExpense = searchActualExpense(poLineBean.getCipAcePoLineAutoId());

        if (actualExpense == null)
        {
          if (!BigDecimal.ZERO.equals(poLineBean.getInvoicedAmount()))
          {
            debug("    Inserting actual expense (invoiced dollars/quantity) = $" + poLineBean.getInvoicedAmount() + ", " + poLineBean.getInvoicedQuantity().toPlainString() + " " + poLineBean.getUom() + " (POLineAutoID=" + poLineBean.getCipAcePoLineAutoId() + ")...");
            addActualExpense(poLineBean);
          }
        }
        else
        {
          poLineBean.setProjectExpenseAutoId(actualExpense.getProjectExpenseAutoId());
          if (StringUtils.isEmpty(poLineBean.getInvoiceCurrencyCode()))
          {
            // If there is an actual expense in CIP but none found in 11i then we want to
            // update the CIP one to zero, but the 11i currency code won't be available
            // (since no invoices found), so just use the CIP one, since the value is zero anyway
            poLineBean.setInvoiceCurrencyCode(actualExpense.getCurrencyCode());
          }
          debug("    Updating actual expense (invoiced dollars/quantity) = $" + poLineBean.getInvoicedAmount() + ", " + poLineBean.getInvoicedQuantity().toPlainString() + " " + poLineBean.getUom() + " (POLineAutoID=" + poLineBean.getCipAcePoLineAutoId() + ", Project Expense ID=" + poLineBean.getProjectExpenseAutoId() + ")...");
          updateActualExpense(poLineBean);
        }
      }
    }

    return true;
  }

  private static ActualExpenseBean searchActualExpense(String purchaseOrderLineItemAutoId)
  {
    CallableStatement cst = null;
    ResultSet rs = null;
    ActualExpenseBean bean = null;

    try
    {
      /*
       * CREATE  PROCEDURE [dbo].[CIP_SearchActualExpense_Valspar]
              @PurchaseOrderAutoID INT = 0 ,
              @PurchaseOrderNumber NVARCHAR(255) = '' ,
              @PurchaseOrderLineItemAutoID INT = 0 ,
              @LineItemNumber NVARCHAR(100) = '' ,
              @ItemCategory NVARCHAR(100) = '' ,
              @ItemDescription NVARCHAR(500) = '' ,
              @NeedByDate DATETIME = ''
       */
      cst = connection.prepareCall("{call dbo.CIP_SearchActualExpense_Valspar(?)}");
      cst.setString("@PurchaseOrderLineItemAutoID", purchaseOrderLineItemAutoId);

      rs = cst.executeQuery();

      if (rs.next())
      {
        bean = new ActualExpenseBean();
        bean.setProjectExpenseAutoId(rs.getString("ProjectExpenseAutoID"));
        bean.setCurrencyCode(rs.getString("CurrencyCode"));
        bean.setQuantity(rs.getBigDecimal("Quantity"));
        bean.setUnitPrice(rs.getBigDecimal("UnitPrice"));
        bean.setAmount(rs.getBigDecimal("Amount"));
      }
    }
    catch (Exception e)
    {
      log4jLogger.error(e);
    }
    finally
    {
      JDBCUtil.close(cst, rs);
    }

    return bean;
  }

  private static boolean addActualExpense(PurchaseOrderLineBean poLineBean)
  {
    CallableStatement cst = null;

    try
    {
      /*
       *
        CREATE  PROCEDURE [dbo].[CIP_AddActualExpense_Valspar]
            @LoginUser NVARCHAR(20) = 'Oracle11iUser',
            @Amount MONEY ,
            @CostDate DATETIME ,
            @Notes NVARCHAR(MAX) = '' ,
            @Quantity DECIMAL ,
            @UnitPrice MONEY ,
            @PaymentReferenceCode NVARCHAR(100) = '' ,
            @UnitShortName NVARCHAR(50) = '',
            @UpdateDate DATETIME ,
            @PurchaseOrderLineItemAutoID INT ,
            @CurrencyCode NVARCHAR(3) ,
            @ProjectExpneseAutoID INT OUTPUT
      */
      cst = connection.prepareCall("{call dbo.CIP_AddActualExpense_Valspar(?,?,?,?,?,?,?,?,?)}");

      cst.setBigDecimal("@Amount", poLineBean.getInvoicedAmount());
      cst.setTimestamp("@CostDate", getNow());
      //cst.setString("@Notes", null);
      cst.setBigDecimal("@Quantity", poLineBean.getInvoicedQuantity());
      cst.setBigDecimal("@UnitPrice", poLineBean.getInvoicedUnitPrice());
      //cst.setString("@PaymentReferenceCode", null);
      cst.setString("@UnitShortName", poLineBean.getUom());
      cst.setString("@UpdateDate", null);
      cst.setString("@PurchaseOrderLineItemAutoID", poLineBean.getCipAcePoLineAutoId());
      cst.setString("@CurrencyCode", poLineBean.getInvoiceCurrencyCode());
      cst.registerOutParameter("@ProjectExpneseAutoID", Types.INTEGER);
      cst.execute();

      String projectExpenseAutoId = cst.getString("@ProjectExpneseAutoID");

      if (StringUtils.isEmpty(projectExpenseAutoId) || StringUtils.equalsIgnoreCase(projectExpenseAutoId, "0"))
      {
        throw new SQLException("CIPAce stored procedure CIP_AddActualExpense_Valspar returned " + projectExpenseAutoId + " for ProjectExpneseAutoID");
      }
      poLineBean.setProjectExpenseAutoId(projectExpenseAutoId);
      return true;
    }
    catch (Exception e)
    {
      log4jLogger.error(e);
      return false;
    }
    finally
    {
      JDBCUtil.close(cst);
    }
  }

  private static boolean updateActualExpense(PurchaseOrderLineBean poLineBean)
  {
    CallableStatement cst = null;

    try
    {
      /*
       *
        CREATE  PROCEDURE [dbo].[CIP_UpdateActualExpense_Valspar]
            @LoginUser NVARCHAR(20) = 'Oracle11iUser' ,
            @ProjectExpneseAutoID INT ,
            @Amount MONEY ,
            @CostDate DATETIME ,
            @Notes NVARCHAR(MAX) = '' ,
            @Quantity DECIMAL ,
            @UnitPrice MONEY ,
            @PaymentReferenceCode NVARCHAR(100)='' ,
            @UnitShortName NVARCHAR(50),
            @UpdateDate DATETIME ,
            @CurrencyCode NVARCHAR(3)
      */

      cst = connection.prepareCall("{call dbo.CIP_UpdateActualExpense_Valspar(?,?,?,?,?,?,?)}");

      cst.setString("@ProjectExpneseAutoID", poLineBean.getProjectExpenseAutoId());
      cst.setBigDecimal("@Amount", poLineBean.getInvoicedAmount());
      //cst.setTimestamp("@CostDate", getNow());
      //cst.setString("@Notes", null);
      cst.setBigDecimal("@Quantity", poLineBean.getInvoicedQuantity());
      cst.setBigDecimal("@UnitPrice", poLineBean.getInvoicedUnitPrice());
      //cst.setString("@PaymentReferenceCode", null);
      cst.setString("@UnitShortName", poLineBean.getUom());
      cst.setString("@UpdateDate", null);
      cst.setString("@CurrencyCode", poLineBean.getInvoiceCurrencyCode());
      cst.execute();

      return true;
    }
    catch (Exception e)
    {
      log4jLogger.error(e);
      return false;
    }
    finally
    {
      JDBCUtil.close(cst);
    }
  }

  private static Timestamp getNow()
  {
    return new java.sql.Timestamp((new java.util.Date()).getTime());
  }

  private static boolean updatePurchaseOrderLine(PurchaseOrderLineBean poLineBean)
  {
    CallableStatement cst = null;

    try
    {
      /*
       * CREATE  PROCEDURE [dbo].[CIP_UpdatePOLineItem_Valspar]
            @LoginUser NVARCHAR(20)='Oracle11iUser' ,
            @PurchaseOrderLineItemAutoID INT ,
            @ReceiveQuantity DECIMAL = 0 ,
            @ReceiveTimeStamp DATETIME = NULL
       */

      cst = connection.prepareCall("{call dbo.CIP_UpdatePOLineItem_Valspar(?,?,?)}");

      cst.setString("@PurchaseOrderLineItemAutoID", poLineBean.getCipAcePoLineAutoId());
      cst.setBigDecimal("@ReceiveQuantity", poLineBean.getReceivedQuantity());
      cst.setTimestamp("@ReceiveTimeStamp", getNow());
      cst.execute();

      return true;
    }
    catch (Exception e)
    {
      log4jLogger.error(e);
      return false;
    }
    finally
    {
      JDBCUtil.close(cst);
    }
  }

  public static Map<String, VendorSiteBean> fetchVendorSitesByRegion(String oracleDatabaseName, int cipRegionId) throws Exception
  {
    CallableStatement cst = null;
    ResultSet rs = null;
    Map<String, VendorSiteBean> beans = new HashMap<String, VendorSiteBean>();

    try
    {
      /*
      CREATE  PROCEDURE [dbo].[CIP_SearchVendors_Valspar] (
            @VendorAutoID INT = 0 ,
            @RegionID INT = 0 ,
            @VendorSiteID NVARCHAR(100) = '' ,
            @OrgID INT = 0
     */

      cst =connection.prepareCall("{call dbo.CIP_SearchVendors_Valspar(?)}");
      cst.setInt("@RegionID", cipRegionId);

      rs = cst.executeQuery();

      while (rs.next())
      {
        VendorSiteBean bean = new VendorSiteBean();
        bean.setVendorAutoId(rs.getString("VendorAutoID"));
        bean.setAddressLine(rs.getString("AddressLine"));
        bean.setZipCode(rs.getString("Zip"));
        bean.setBillTo(rs.getString("BillTo"));
        bean.setBillToLocationId(rs.getString("BillToLocation"));
        bean.setCity(rs.getString("City"));
        bean.setCountry(rs.getString("Country"));
        bean.setOperatingUnit(rs.getString("OperatingUnit"));
        bean.setOrgId(rs.getString("OrgID"));
        bean.setProvince(rs.getString("Province"));
        bean.setOracleDatabaseName(oracleDatabaseName);
        bean.setCipRegionId(rs.getString("RegionID"));
        bean.setSiteBillToLocation(rs.getString("SiteBillToLocation"));
        bean.setSiteBillToLocationId(rs.getString("SiteBillToLocationID"));
        bean.setState(rs.getString("STATE"));
        bean.setVendorId(rs.getString("VendorID"));
        bean.setName(rs.getString("NAME"));
        bean.setVendorSiteCode(rs.getString("Site"));
        bean.setVendorSiteId(rs.getString("VendorSiteID"));
        bean.setVendorType(rs.getString("VendorType"));
        bean.setActive(rs.getInt("IsActive") == 1);

        beans.put(bean.getVendorSiteId(), bean);
      }
    }
    finally
    {
      JDBCUtil.close(cst, rs);
    }

    return beans;
  }

  public static boolean insertVendor(VendorSiteBean vendorSiteBean)
  {
    CallableStatement cst = null;

    try
    {
      /*
       * CREATE  PROCEDURE [dbo].[CIP_AddVendor_Valspar]
              @LoginUser NVARCHAR(20) = 'Oracle11iUser' ,
              @AddressLine NVARCHAR(100) = '' ,
              @ZIP NVARCHAR(100) = '' ,
              @BillTo NVARCHAR(100) = '' ,
              @BillToLocationID NVARCHAR(100) = '' ,
              @City NVARCHAR(100) = '' ,
              @Country NVARCHAR(100) = '' ,
              @OperatingUnit NVARCHAR(100) = '' ,
              @OrgID INT ,
              @Province NVARCHAR(100) = '' ,
              @RegionID INT ,
              @SiteBillToLocation NVARCHAR(100) = '' ,
              @SiteBillToLocationID NVARCHAR(100) = '' ,
              @State NVARCHAR(100) = '' ,
              @VendorID NVARCHAR(100) ,
              @Name NVARCHAR(200) ,
              @VendorSiteID NVARCHAR(100) ,
              @VendorType NVARCHAR(100) = '' ,
              @Site NVARCHAR(100) ,
              @IsActive BIT = 1,
              @VendorAutoID INT OUTPUT
       */
      cst = connection.prepareCall("{call dbo.CIP_AddVendor_Valspar(?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)}");

      //cst.setString("@LoginUser", cipAceUserId);
      cst.setString("@AddressLine", vendorSiteBean.getAddressLine());
      cst.setString("@ZIP", vendorSiteBean.getZipCode());
      cst.setString("@BillTo", vendorSiteBean.getBillTo());
      cst.setString("@BillToLocationID", vendorSiteBean.getBillToLocationId());
      cst.setString("@City", vendorSiteBean.getCity());
      cst.setString("@Country", vendorSiteBean.getCountry());
      cst.setString("@OperatingUnit", vendorSiteBean.getOperatingUnit());
      cst.setInt("@OrgID", Integer.valueOf(vendorSiteBean.getOrgId()));
      cst.setString("@Province", vendorSiteBean.getProvince());
      cst.setInt("@RegionID", Integer.valueOf(vendorSiteBean.getCipRegionId()));
      cst.setString("@SiteBillToLocation", vendorSiteBean.getSiteBillToLocation());
      cst.setString("@SiteBillToLocationID", vendorSiteBean.getSiteBillToLocationId());
      cst.setString("@State", vendorSiteBean.getState());
      cst.setString("@VendorID", vendorSiteBean.getVendorId());
      cst.setString("@Name", vendorSiteBean.getName());
      cst.setString("@VendorSiteID", vendorSiteBean.getVendorSiteId());
      cst.setString("@VendorType", vendorSiteBean.getVendorType());
      cst.setString("@Site", vendorSiteBean.getVendorSiteCode());
      cst.setInt("@IsActive", vendorSiteBean.isActive()? 1: 0);
      cst.registerOutParameter("@VendorAutoID", Types.INTEGER);
      cst.execute();

      String vendorAutoId = cst.getString("@VendorAutoID");

      if (StringUtils.isEmpty(vendorAutoId) || StringUtils.equalsIgnoreCase(vendorAutoId, "0"))
      {
        throw new SQLException("CIPAce stored procedure CIP_AddVendor_Valspar returned " + vendorAutoId + " for vendorAutoID");
      }
      vendorSiteBean.setVendorAutoId(vendorAutoId);
      return true;
    }
    catch (Exception e)
    {
      log4jLogger.error(vendorSiteBean.getOracleDatabaseName() + " vendor [" + vendorSiteBean.getName() + "] (Vendor ID=" + vendorSiteBean.getVendorId() + ") site [" + vendorSiteBean.getVendorSiteCode() + "] (Vendor Site ID=" + vendorSiteBean.getVendorSiteId() + "), CIPAce VendorAutoID=" + vendorSiteBean.getVendorAutoId(), e);
      return false;
    }
    finally
    {
      JDBCUtil.close(cst);
    }
  }

  public static boolean updateVendor(VendorSiteBean vendorSiteBean)
  {
    CallableStatement cst = null;

    try
    {
      /*
       * CREATE  PROCEDURE [dbo].[CIP_UpdateVendor_Valspar]
              @LoginUser NVARCHAR(20) ,
              @VendorAutoID INT ,
              @AddressLine NVARCHAR(100) = '' ,
              @ZIP NVARCHAR(100) = '' ,
              @BillTo NVARCHAR(100) = '' ,
              @BillToLocationID NVARCHAR(100) = '' ,
              @City NVARCHAR(100) = '' ,
              @Country NVARCHAR(100) = '' ,
              @OperatingUnit NVARCHAR(100) = '' ,
              @OrgID INT ,
              @Province NVARCHAR(100) = '' ,
              @RegionID INT ,
              @SiteBillToLocation NVARCHAR(100) = '' ,
              @SiteBillToLocationID NVARCHAR(100) = '' ,
              @State NVARCHAR(100) = '' ,
              @VendorID NVARCHAR(100),
              @Name NVARCHAR(200) ,
              @VendorSiteID NVARCHAR(100) ,
              @VendorType NVARCHAR(100) = '',
              @Site NVARCHAR(100),
              @IsActive BIT
       */

      cst = connection.prepareCall("{call dbo.CIP_UpdateVendor_Valspar(?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)}");

      cst.setString("@LoginUser", "Oracle11iUser");
      cst.setInt("@VendorAutoID", Integer.valueOf(vendorSiteBean.getVendorAutoId()));
      cst.setString("@AddressLine", vendorSiteBean.getAddressLine());
      cst.setString("@ZIP", vendorSiteBean.getZipCode());
      cst.setString("@BillTo", vendorSiteBean.getBillTo());
      cst.setString("@BillToLocationID", vendorSiteBean.getBillToLocationId());
      cst.setString("@City", vendorSiteBean.getCity());
      cst.setString("@Country", vendorSiteBean.getCountry());
      cst.setString("@OperatingUnit", vendorSiteBean.getOperatingUnit());
      cst.setInt("@OrgID", Integer.valueOf(vendorSiteBean.getOrgId()));
      cst.setString("@Province", vendorSiteBean.getProvince());
      cst.setInt("@RegionID", Integer.valueOf(vendorSiteBean.getCipRegionId()));
      cst.setString("@SiteBillToLocation", vendorSiteBean.getSiteBillToLocation());
      cst.setString("@SiteBillToLocationID", vendorSiteBean.getSiteBillToLocationId());
      cst.setString("@State", vendorSiteBean.getState());
      cst.setString("@VendorID", vendorSiteBean.getVendorId());
      cst.setString("@Name", vendorSiteBean.getName());
      cst.setString("@VendorSiteID", vendorSiteBean.getVendorSiteId());
      cst.setString("@VendorType", vendorSiteBean.getVendorType());
      cst.setString("@Site", vendorSiteBean.getVendorSiteCode());
      cst.setInt("@IsActive", vendorSiteBean.isActive()? 1: 0);
      cst.execute();

      return true;
    }
    catch (Exception e)
    {
      log4jLogger.error(vendorSiteBean.getOracleDatabaseName() + " vendor [" + vendorSiteBean.getName() + "] (Vendor ID=" + vendorSiteBean.getVendorId() + ") site [" + vendorSiteBean.getVendorSiteCode() + "] (Vendor Site ID=" + vendorSiteBean.getVendorSiteId() + "), CIPAce VendorAutoID=" + vendorSiteBean.getVendorAutoId(), e);
      return false;
    }
    finally
    {
      JDBCUtil.close(cst);
    }
  }

  public static Map<String, String> getCurrencyMap() throws Exception
  {
    CallableStatement cst = null;
    ResultSet rs = null;
    Map<String, String> currencyMap = new HashMap<String, String>();

    try
    {
      /*
      CREATE  PROCEDURE [dbo].[CIP_GetAllCurrencies_Valspar]
      */

      cst = connection.prepareCall("{call dbo.CIP_GetAllCurrencies_Valspar()}");

      rs = cst.executeQuery();

      while (rs.next())
      {
        currencyMap.put(rs.getString("CurrencyCode"), rs.getString("CurrencyGuid"));
      }
    }
    catch (Exception e)
    {
      log4jLogger.error(e);
    }
    finally
    {
      JDBCUtil.close(cst, rs);
    }

    return currencyMap;
  }

  public static String getCurrencyExchangeRateGuid(CurrencyExchangeRateBean oracleExchangeRateBean)
  {
    CallableStatement cst = null;
    ResultSet rs = null;
    String currencyExchangeRateGuid = null;

    try
    {
      /*
      CREATE  PROCEDURE [dbo].[CIP_SearchCurrencyExchangeRates_Valspar]
            @FromCurrencyCode NVARCHAR(MAX) = '' ,
            @ToCurrencyCode NVARCHAR(MAX) = '' ,
            @EffectiveDate DATETIME = NULL ,
            @Note NVARCHAR(MAX) = ''
      */

      cst = connection.prepareCall("{call dbo.CIP_SearchCurrencyExchangeRates_Valspar(?,?,?)}");
      cst.setString("@FromCurrencyCode", oracleExchangeRateBean.getFromCurrencyCode());
      cst.setString("@ToCurrencyCode", oracleExchangeRateBean.getToCurrencyCode());
      cst.setString("@Note", oracleExchangeRateBean.getNote());

      rs = cst.executeQuery();

      if (rs.next())
      {
        currencyExchangeRateGuid = rs.getString("CurrencyExchangeRateGuid");
      }
    }
    catch (Exception e)
    {
      log4jLogger.error(e);
    }
    finally
    {
      JDBCUtil.close(cst, rs);
    }

    return currencyExchangeRateGuid;
  }

  public static boolean insertCurrencyExchangeRate(CurrencyExchangeRateBean currencyExchangeRateBean)
  {
    CallableStatement cst = null;

    try
    {
      /*
       * CREATE  PROCEDURE [dbo].[CIP_AddCurrencyExchangeRate_Valspar]
            @LoginUser NVARCHAR(20) = 'Oracle11iUser' ,
            @FromCurrencyGuid UNIQUEIDENTIFIER ,
            @ToCurrencyGuid UNIQUEIDENTIFIER ,
            @EffectiveDate DATETIME ,
            @ExchangeRate DECIMAL ,
            @Note NVARCHAR(MAX) = NULL ,
            @CurrencyExchageRateGuid UNIQUEIDENTIFIER OUTPUT
       */

      cst = connection.prepareCall("{call dbo.CIP_AddCurrencyExchangeRate_Valspar(?,?,?,?,?,?)}");

      //cst.setString("@LoginUser", cipAceUserId);
      cst.setString("@FromCurrencyGuid", currencyExchangeRateBean.getFromCurrencyGuid());
      cst.setString("@ToCurrencyGuid", currencyExchangeRateBean.getToCurrencyGuid());
      cst.setTimestamp("@EffectiveDate", getNow());
      cst.setBigDecimal("@ExchangeRate", currencyExchangeRateBean.getRate());
      cst.setString("@Note", currencyExchangeRateBean.getNote());
      cst.registerOutParameter("@CurrencyExchageRateGuid", Types.VARCHAR);
      cst.execute();

      String currencyExchageRateGuid = cst.getString("@CurrencyExchageRateGuid");

      if (StringUtils.isEmpty(currencyExchageRateGuid) || StringUtils.equalsIgnoreCase(currencyExchageRateGuid, "0"))
      {
        throw new SQLException("CIPAce stored procedure CIP_AddCurrencyExchangeRate_Valspar returned " + currencyExchageRateGuid + " for CurrencyExchageRateGuid");
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
      JDBCUtil.close(cst);
    }
  }

  public static String lookupRegionId(String databaseName)
  {
    CallableStatement cst = null;
    ResultSet rs = null;
    String regionId = null;

    try
    {
      /*
       * CREATE  PROCEDURE [dbo].[CIP_GetAllRegions_Valspar]
       */

      cst = connection.prepareCall("{call dbo.CIP_GetAllRegions_Valspar()}");

      rs = cst.executeQuery();

      String instanceCode = StringUtils.substring(databaseName, 0, 2);

      while (rs.next())
      {
        String regionDatabaseName = rs.getString("cstm_database");
        String regionInstanceCode = StringUtils.substring(regionDatabaseName, 0, 2);

        if (StringUtils.equalsIgnoreCase(instanceCode, regionInstanceCode))
        {
          regionId = rs.getString("regionID");
          break;
        }
      }
    }
    catch (Exception e)
    {
      log4jLogger.error(e);
    }
    finally
    {
      JDBCUtil.close(cst, rs);
    }

    return regionId;
  }

  public static void setConnection(Connection connection)
  {
    CIPAceDAO.connection = connection;
  }

  public static Connection getConnection()
  {
    return connection;
  }
}
