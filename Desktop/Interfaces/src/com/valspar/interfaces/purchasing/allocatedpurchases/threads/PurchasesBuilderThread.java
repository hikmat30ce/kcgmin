package com.valspar.interfaces.purchasing.allocatedpurchases.threads;

import com.valspar.interfaces.common.beans.*;
import com.valspar.interfaces.common.enums.DataSource;
import com.valspar.interfaces.common.utils.*;
import com.valspar.interfaces.common.utils.InterfaceThreadManager;
import com.valspar.interfaces.purchasing.allocatedpurchases.beans.PurchaseBean;
import oracle.jdbc.*;
import org.apache.log4j.Logger;

public class PurchasesBuilderThread extends BaseBuilderThread
{
  private static Logger log4jLogger = Logger.getLogger(PurchasesBuilderThread.class);
  private DataSource dataSource;

  public PurchasesBuilderThread(InterfaceInfoBean interfaceinfo, DataSource dataSource)
  {
    setInterfaceInfo(interfaceinfo);
    this.dataSource = dataSource;
  }

  public void run()
  {
    InterfaceThreadManager.addInterfaceThread(getInterfaceInfo());

    try
    {
      transferPurchases();
    }
    catch (Exception e)
    {
      log4jLogger.error(e);
    }
    finally
    {
      InterfaceThreadManager.removeInterfaceThread();
    }
  }

  private void transferPurchases()
  {
    OracleConnection sourceConn = null;
    OracleConnection targetConn = null;
    OraclePreparedStatement sourcePst = null;
    OraclePreparedStatement targetPst = null;
    OracleResultSet rs = null;
    long rec = 0L;

    try
    {
      StringBuilder sb = new StringBuilder();

      sb.append("INSERT INTO ANNUAL_PURCHASES ( ");
      sb.append("   PERIOD_NAME, ITEM_NO, ITEM_DESC, ");
      sb.append("   WHSE_CODE, WHSE_NAME, WHSE_ORG_ID, ");
      sb.append("   ORGN_CODE, ORGN_NAME, CO_CODE, ");
      sb.append("   CO_NAME, QUANTITY, UOM, ");
      sb.append("   UNIT_PRICE, SAC_COST, CURRENCY_CODE, ");
      sb.append("   VENDOR_TYPE, ");
      sb.append("   COST_CLASS_CODE, COST_CLASS_DESC, ");
      sb.append("   GL_CLASS_CODE, GL_CLASS_DESC, ");
      sb.append("   SALES_CLASS_CODE, SALES_CLASS_DESC, ");
      sb.append("   MAJOR_CUSTOMER, SOURCE_INSTANCE) ");
      sb.append("VALUES ( ");
      sb.append("   :PERIOD_NAME, :ITEM_NO, :ITEM_DESC, ");
      sb.append("   :WHSE_CODE, :WHSE_NAME, :WHSE_ORG_ID, ");
      sb.append("   :ORGN_CODE, :ORGN_NAME, :CO_CODE, ");
      sb.append("   :CO_NAME, :QUANTITY, :UOM, ");
      sb.append("   :UNIT_PRICE, :SAC_COST, :CURRENCY_CODE, ");
      sb.append("   :VENDOR_TYPE, ");
      sb.append("   :COST_CLASS_CODE, :COST_CLASS_DESC, ");
      sb.append("   :GL_CLASS_CODE, :GL_CLASS_DESC, ");
      sb.append("   :SALES_CLASS_CODE, :SALES_CLASS_DESC, ");
      sb.append("   :MAJOR_CUSTOMER, :SOURCE_INSTANCE) ");

      targetConn = (OracleConnection) ConnectionAccessBean.getConnection(DataSource.RMINDEX);
      targetPst = (OraclePreparedStatement) targetConn.prepareStatement(sb.toString());

      sb = new StringBuilder();
      sb.append(" with all_periods AS ");
      sb.append("            (select gp.period_set_name, gp.period_name, gp.start_date, gp.end_date,  ");
      sb.append("                    rank() over (order by gp.start_date) rnum  ");
      sb.append("             from gl_periods gp ");
      sb.append("             where gp.period_set_name in ('NOV-OCT 4-4-5', 'OCT-SEP') ");
      sb.append("               and gp.period_name not like 'END%' ");
      sb.append("               and gp.period_name not like 'BEG%' ");
      sb.append("             order by gp.start_date), ");
      sb.append("      current_period_rnum AS ");
      sb.append("              (select rnum, start_date, end_date ");
      sb.append("                 from all_periods ");
      sb.append("                where trunc(sysdate) between start_date and end_date), ");
      sb.append("      categories_v1 AS ");
      sb.append("          (select mic.inventory_item_id, ");
      sb.append("                  mic.organization_id, ");
      sb.append("                  msib.segment1 item_no, ");
      sb.append("                  mcs.category_set_name category_name, ");
      sb.append("                  mc.segment1 category_value, ");
      sb.append("                  mc_kfv.concatenated_segments category_value_concatenated, ");
      sb.append("                  mc.description category_value_desc ");
      sb.append("           from mtl_item_categories mic ");
      sb.append("           inner join mtl_system_items_b msib on mic.inventory_item_id = msib.inventory_item_id and mic.organization_id = msib.organization_id ");
      sb.append("           inner join mtl_category_sets mcs on mic.category_set_id = mcs.category_set_id ");
      sb.append("           inner join mtl_categories mc on mic.category_id = mc.category_id ");
      sb.append("           inner join mtl_categories_kfv mc_kfv on mc.row_id = mc_kfv.row_id), ");
      sb.append("      item_master_org as ");
      sb.append("          (select vca_common_api.get_item_master_org org_value ");
      sb.append("           from dual) ");
      sb.append(" select main.period_name, ");
      sb.append("        main.item_no, ");
      sb.append("        main.item_description, ");
      sb.append("        main.whse_code, ");
      sb.append("        main.whse_name, ");
      sb.append("        main.whse_org_id, ");
      sb.append("        main.orgn_code, ");
      sb.append("        main.orgn_name, ");
      sb.append("        main.co_code, ");
      sb.append("        main.co_name, ");
      sb.append("        sum(main.quantity) quantity, ");
      sb.append("        main.uom, ");
      sb.append("        sum(main.unit_price * main.quantity) / sum(main.quantity) unit_price, ");
      sb.append("        main.std_actual_cost, ");
      sb.append("        main.currency_code, ");
      sb.append("        main.vendor_type, ");
      sb.append("        main.itemcost_class, ");
      sb.append("        itemcost_class_category.category_value_desc itemcost_class_desc, ");
      sb.append("        main.gl_class, ");
      sb.append("        gl_class_category.category_value_desc gl_class_desc, ");
      sb.append("        main.sales_class, ");
      sb.append("        sales_class_category.category_value_desc sales_class_desc, ");
      sb.append("        major_customer_category.category_value major_customer ");
      sb.append(" from ( ");
      sb.append("      select period_name, ");
      sb.append("             inventory_item_id, ");
      sb.append("             organization_id, ");
      sb.append("             item_no, ");
      sb.append("             item_description, ");
      sb.append("             whse_code, ");
      sb.append("             whse_name, ");
      sb.append("             whse_org_id, ");
      sb.append("             orgn_code, ");
      sb.append("             orgn_name, ");
      sb.append("             co_code, ");
      sb.append("             co_name, ");
      sb.append("             case when unit_of_measure = primary_unit_of_measure then quantity ");
      sb.append("                  else gmicuom.uom_conversion(item_id, null, quantity, unit_of_measure, primary_unit_of_measure, 0) ");
      sb.append("             end quantity, ");
      sb.append("             primary_unit_of_measure uom, ");
      sb.append("             case when unit_of_measure = primary_unit_of_measure then nvl(next_invoice_unit_price, po_unit_price) ");
      sb.append("                  when nvl(next_invoice_unit_price, po_unit_price) = 0 then 0 ");
      sb.append("                  else 1/gmicuom.uom_conversion(item_id, null, 1/nvl(next_invoice_unit_price, po_unit_price), unit_of_measure, primary_unit_of_measure, 0) ");
      sb.append("             end * conversion_rate unit_price, ");
      sb.append("             sac_cost * conversion_rate std_actual_cost, ");
      sb.append("             conversion_currency_code currency_code, ");
      sb.append("             case when vendor_type_lookup_code in ('INTERNAL', 'INTERCOMPANY') or vendor_category = 'INTERNAL' then 'INTERNAL' ");
      sb.append("                  else 'EXTERNAL' ");
      sb.append("             end vendor_type, ");
      sb.append("             itemcost_class, ");
      sb.append("             gl_class, ");
      sb.append("             sales_class ");
      sb.append("      from ( ");
      sb.append("            select rt.transaction_id, ");
      sb.append("                   pha.segment1 po_number, ");
      sb.append("                   pha.org_id, ");
      sb.append("                   pla.po_line_id, ");
      sb.append("                   rt.transaction_date receipt_date, ");
      sb.append("                   msi.inventory_item_id, ");
      sb.append("                   msi.organization_id, ");
      sb.append("                   msi.segment1 item_no, ");
      sb.append("                   msi.description item_description, ");
      sb.append("                   iwm.whse_code, ");
      sb.append("                   iwm.whse_name, ");
      sb.append("                   iwm.mtl_organization_id whse_org_id, ");
      sb.append("                   iwm.orgn_code, ");
      sb.append("                   som_org.orgn_name, ");
      sb.append("                   som_org.co_code, ");
      sb.append("                   som_co.orgn_name co_name, ");
      sb.append("                   RT.quantity, ");
      sb.append("                   rt.unit_of_measure, ");
      sb.append("                   rt.primary_unit_of_measure, ");
      sb.append("                   rt.po_unit_price, ");
      sb.append("                   (select distinct ");
      sb.append("                           first_value(aida.amount / nullif(aida.quantity_invoiced, 0)) over (order by aida.creation_date) ");
      sb.append("                    from ap_invoice_distributions_all aida ");
      sb.append("                    where pda.po_distribution_id = aida.po_distribution_id ");
      sb.append("                      and aida.creation_date >= rt.transaction_date ");
      sb.append("                      and aida.reversal_flag is null) next_invoice_unit_price, ");
      sb.append("                    vca_common_api.convert_currency( ");
      sb.append("                          gpm.base_currency_code,rt.currency_code, ");
      sb.append("                          vca_common_api.get_item_cost_for_cost_type( ");
      sb.append("                                     iim.item_id,iwm.whse_code,iwm.orgn_code,trunc(rt.transaction_date),'SAC'), ");
      sb.append("                          rt.transaction_date,'CORP') sac_cost, ");
      sb.append("                   rt.currency_code, ");
      sb.append("                   glp.period_name, ");
      sb.append("                   iim.item_id, ");
      sb.append("                   glr.conversion_rate, ");
      sb.append("                   glr.to_currency conversion_currency_code, ");
      sb.append("                   pv.vendor_type_lookup_code, ");
      sb.append("                   pv.attribute10 vendor_category, ");
      sb.append("                   iim.itemcost_class, ");
      sb.append("                   iim.gl_class, ");
      sb.append("                   iim.sales_class ");
      sb.append("              from apps.po_headers_all pha, ");
      sb.append("                   apps.po_lines_all pla, ");
      sb.append("                   apps.rcv_transactions rt, ");
      sb.append("                   apps.mtl_system_items msi, ");
      sb.append("                   apps.po_distributions_all pda, ");
      sb.append("                   apps.po_vendors pv, ");
      sb.append("                   apps.gl_periods glp, ");
      sb.append("                   apps.ic_item_mst_b iim, ");
      sb.append("                   apps.gl_daily_rates glr, ");
      sb.append("                   apps.ic_whse_mst iwm, ");
      sb.append("                   apps.sy_orgn_mst som_org, ");
      sb.append("                   apps.sy_orgn_mst som_co, ");
      sb.append("                   apps.gl_plcy_mst gpm, ");
      sb.append("                   apps.hr_all_organization_units haou, ");
      sb.append("                   item_master_org ");
      sb.append("             where pha.po_header_id = pla.po_header_id ");
      sb.append("               and pha.po_header_id = rt.po_header_id ");
      sb.append("               and pla.item_id = msi.inventory_item_id ");
      sb.append("               and msi.organization_id = item_master_org.org_value ");
      sb.append("               and pla.po_line_id = rt.po_line_id ");
      sb.append("               and rt.organization_id = iwm.mtl_organization_id ");
      sb.append("               and rt.destination_type_code = 'RECEIVING' ");
      sb.append("               and rt.transaction_date between (select all_periods.start_date  ");
      sb.append("                                                from all_periods, current_period_rnum ");
      sb.append("                                                where (current_period_rnum.rnum-12) = all_periods.rnum) ");
      sb.append("                                           and (select all_periods.end_date  ");
      sb.append("                                                from all_periods, current_period_rnum ");
      sb.append("                                                where (current_period_rnum.rnum-1) = all_periods.rnum) ");
      sb.append("               and pla.po_line_id = pda.po_line_id ");
      sb.append("               and pha.vendor_id = pv.vendor_id ");

      if (dataSource == DataSource.NORTHAMERICAN)
      {
        sb.append("             and (pv.vendor_type_lookup_code in ('DIRECT', 'DISTRIBUTOR','RAW MATERIAL','INTERNAL','RAW MATERIAL INTL') ");
        sb.append("                  OR (pv.attribute10 = 'INTERNAL' AND pv.attribute11 = 'INTERCOMPANY') ");
        sb.append("                 )");
        sb.append("             and glp.period_set_name = 'NOV-OCT 4-4-5' ");
      }
      else
      {
        sb.append("             and pv.vendor_type_lookup_code in ('DIRECT', 'RM_DISTRIBUTOR','RAW MATERIAL','INTERCOMPANY') ");
        sb.append("             and glp.period_set_name = 'OCT-SEP' ");
      }
      sb.append("               and glp.period_name not like 'BEG%' ");
      sb.append("               and glp.period_name not like 'END%' ");
      sb.append("               and trunc(rt.transaction_date) between glp.start_date and glp.end_date ");
      sb.append("               and msi.segment1 = iim.item_no ");
      sb.append("               and iim.noninv_ind = 0 ");
      sb.append("               and rt.currency_code = glr.from_currency ");
      sb.append("               and trunc(rt.transaction_date) = glr.conversion_date ");
      sb.append("               and glr.conversion_type = '1000' ");

      if (dataSource == DataSource.EMEAI)
      {
        sb.append("             and glr.to_currency = 'EUR' ");
      }
      else
      {
        sb.append("             and glr.to_currency = 'USD' ");
      }
      sb.append("               and som_org.orgn_code = iwm.orgn_code ");
      sb.append("               and som_co.orgn_code = som_org.co_code ");
      sb.append("               and gpm.co_code = som_org.co_code ");
      sb.append("               and rt.organization_id = haou.organization_id ");
      sb.append("               and nvl(haou.date_to, sysdate + 1) > sysdate ");
      sb.append("               and nvl(haou.type, ' ') not in ('RMC', 'CONSIGN') ");
      sb.append("             ) ");
      sb.append("       ) main, ");
      sb.append("       categories_v1 gl_class_category, ");
      sb.append("       categories_v1 sales_class_category, ");
      sb.append("       categories_v1 major_customer_category, ");
      sb.append("       categories_v1 itemcost_class_category, ");
      sb.append("       categories_v1 po_item_category ");
      sb.append("WHERE gl_class_category.inventory_item_id (+) = main.inventory_item_id  ");
      sb.append("  and gl_class_category.organization_id   (+) = main.organization_id  ");
      sb.append("  and gl_class_category.category_name     (+) = 'GL CLASS' ");
      sb.append("  and sales_class_category.inventory_item_id (+) = main.inventory_item_id  ");
      sb.append("  and sales_class_category.organization_id   (+) = main.organization_id  ");
      sb.append("  and sales_class_category.category_name     (+) = 'SALES CLASS' ");
      sb.append("  and major_customer_category.inventory_item_id (+) = main.inventory_item_id  ");
      sb.append("  and major_customer_category.organization_id   (+) = main.organization_id  ");
      sb.append("  and major_customer_category.category_name     (+) = 'MAJOR CUSTOMER' ");
      sb.append("  and itemcost_class_category.inventory_item_id (+) = main.inventory_item_id  ");
      sb.append("  and itemcost_class_category.organization_id   (+) = main.organization_id  ");
      sb.append("  and itemcost_class_category.category_name     (+) = 'ITEMCOST CLASS' ");
      sb.append("  and po_item_category.inventory_item_id (+) = main.inventory_item_id  ");
      sb.append("  and po_item_category.organization_id   (+) = main.organization_id  ");
      sb.append("  and po_item_category.category_name     (+) = 'PO ITEM CATEGORY' ");
      sb.append("  and nvl(po_item_category.category_value_concatenated, ' ') not in ('LABELS') ");
      sb.append("GROUP BY main.period_name, ");
      sb.append("         main.item_no, ");
      sb.append("         main.item_description, ");
      sb.append("         main.whse_code, ");
      sb.append("         main.whse_name, ");
      sb.append("         main.whse_org_id, ");
      sb.append("         main.orgn_code, ");
      sb.append("         main.orgn_name, ");
      sb.append("         main.co_code, ");
      sb.append("         main.co_name, ");
      sb.append("         main.uom, ");
      sb.append("         main.currency_code, ");
      sb.append("         main.vendor_type, ");
      sb.append("         main.std_actual_cost, ");
      sb.append("         main.itemcost_class, ");
      sb.append("         itemcost_class_category.category_value_desc, ");
      sb.append("         main.gl_class, ");
      sb.append("         gl_class_category.category_value_desc, ");
      sb.append("         main.sales_class, ");
      sb.append("         sales_class_category.category_value_desc, ");
      sb.append("         major_customer_category.category_value ");
      sb.append("having sum(main.quantity) != 0 ");

      sourceConn = (OracleConnection) ConnectionAccessBean.getConnection(dataSource);
      sourcePst = (OraclePreparedStatement) sourceConn.prepareStatement(sb.toString());

      log4jLogger.info("PURCHASES - Running purchases query against " + dataSource.getDataSourceLabel() + "...");
      rs = (OracleResultSet) sourcePst.executeQuery();
      log4jLogger.info("PURCHASES - Query finished.  Streaming data from " + dataSource.getDataSourceLabel() + " to " + DataSource.RMINDEX.getDataSourceLabel() + "...");

      while (rs.next())
      {
        PurchaseBean bean = new PurchaseBean();
        bean.setPeriodName(rs.getString("period_name"));
        bean.setItemNumber(rs.getString("item_no"));
        bean.setItemDescription(rs.getString("item_description"));
        bean.setWarehouseCode(rs.getString("whse_code"));
        bean.setWarehouseName(rs.getString("whse_name"));
        bean.setWarehouseOrgId(rs.getString("whse_org_id"));
        bean.setOrganizationCode(rs.getString("orgn_code"));
        bean.setOrganizationName(rs.getString("orgn_name"));
        bean.setCompanyCode(rs.getString("co_code"));
        bean.setCompanyName(rs.getString("co_name"));
        bean.setQuantity(rs.getString("quantity"));
        bean.setUom(rs.getString("uom"));
        bean.setUnitPrice(rs.getString("unit_price"));
        bean.setSacCost(rs.getString("std_actual_cost"));
        bean.setCurrencyCode(rs.getString("currency_code"));
        bean.setVendorType(rs.getString("vendor_type"));
        bean.setCostClassCode(rs.getString("itemcost_class"));
        bean.setCostClassDesc(rs.getString("itemcost_class_desc"));
        bean.setGlClassCode(rs.getString("gl_class"));
        bean.setGlClassDesc(rs.getString("gl_class_desc"));
        bean.setSalesClassCode(rs.getString("sales_class"));
        bean.setSalesClassDesc(rs.getString("sales_class_desc"));
        bean.setMajorCustomer(rs.getString("major_customer"));
        bean.setOracleSource(dataSource.getAnalyticsDataSource());

        targetPst.setStringAtName("PERIOD_NAME", bean.getPeriodName());
        targetPst.setStringAtName("ITEM_NO", bean.getItemNumber());
        targetPst.setStringAtName("ITEM_DESC", bean.getItemDescription());
        targetPst.setStringAtName("WHSE_CODE", bean.getWarehouseCode());
        targetPst.setStringAtName("WHSE_NAME", bean.getWarehouseName());
        targetPst.setStringAtName("WHSE_ORG_ID", bean.getWarehouseOrgId());
        targetPst.setStringAtName("ORGN_CODE", bean.getOrganizationCode());
        targetPst.setStringAtName("ORGN_NAME", bean.getOrganizationName());
        targetPst.setStringAtName("CO_CODE", bean.getCompanyCode());
        targetPst.setStringAtName("CO_NAME", bean.getCompanyName());
        targetPst.setStringAtName("QUANTITY", bean.getQuantity());
        targetPst.setStringAtName("UOM", bean.getUom());
        targetPst.setStringAtName("UNIT_PRICE", bean.getUnitPrice());
        targetPst.setStringAtName("SAC_COST", bean.getSacCost());
        targetPst.setStringAtName("CURRENCY_CODE", bean.getCurrencyCode());
        targetPst.setStringAtName("VENDOR_TYPE", bean.getVendorType());
        targetPst.setStringAtName("COST_CLASS_CODE", bean.getCostClassCode());
        targetPst.setStringAtName("COST_CLASS_DESC", bean.getCostClassDesc());
        targetPst.setStringAtName("GL_CLASS_CODE", bean.getGlClassCode());
        targetPst.setStringAtName("GL_CLASS_DESC", bean.getGlClassDesc());
        targetPst.setStringAtName("SALES_CLASS_CODE", bean.getSalesClassCode());
        targetPst.setStringAtName("SALES_CLASS_DESC", bean.getSalesClassDesc());
        targetPst.setStringAtName("MAJOR_CUSTOMER", bean.getMajorCustomer());
        targetPst.setStringAtName("SOURCE_INSTANCE", bean.getOracleSource());
        targetPst.addBatch();

        rec += 1L;

        if (rec % 1000L == 0L)
        {
          targetPst.executeBatch();
        }
      }

      targetPst.executeBatch();
    }
    catch (Exception e)
    {
      log4jLogger.error("PURCHASES (" + dataSource.getDataSourceLabel() + ")", e);
      setError(e);
    }
    finally
    {
      JDBCUtil.close(sourcePst, rs);
      JDBCUtil.close(targetPst);
      JDBCUtil.close(sourceConn);
      JDBCUtil.close(targetConn);
    }

    log4jLogger.info("PURCHASES - Done transferring purchase data from " + dataSource.getDataSourceLabel() + ", count = " + rec);
  }
}
