package com.valspar.interfaces.purchasing.allocatedpurchases.threads;

import com.valspar.interfaces.common.beans.*;
import com.valspar.interfaces.common.enums.DataSource;
import com.valspar.interfaces.common.utils.*;
import com.valspar.interfaces.common.utils.InterfaceThreadManager;
import com.valspar.interfaces.purchasing.allocatedpurchases.beans.BatchBean;
import oracle.jdbc.*;
import org.apache.log4j.Logger;

public class BatchesBuilderThread extends BaseBuilderThread
{
  private static Logger log4jLogger = Logger.getLogger(BatchesBuilderThread.class);
  private DataSource dataSource;

  public BatchesBuilderThread(InterfaceInfoBean interfaceinfo, DataSource dataSource)
  {
    setInterfaceInfo(interfaceinfo);
    this.dataSource = dataSource;
  }

  public void run()
  {
    InterfaceThreadManager.addInterfaceThread(getInterfaceInfo());

    try
    {
      transferBatches();
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

  private void transferBatches()
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

      sb.append("INSERT INTO ANNUAL_BATCHES ( ");
      sb.append("   PLANT_CODE, RM_ITEM_NO, RM_ITEM_DESC,  ");
      sb.append("   RM_ITEM_UM, RM_QTY, ");
      sb.append("   PRODUCT_ITEM_NO, PRODUCT_ITEM_DESC, ");
      sb.append("   FORMULA_CLASS_CODE, FORMULA_CLASS_DESC,  ");
      sb.append("   PRODUCT_COST_CLASS_CODE, PRODUCT_COST_CLASS_DESC, ");
      sb.append("   PRODUCT_GL_CLASS_CODE, PRODUCT_GL_CLASS_DESC, ");
      sb.append("   PRODUCT_SALES_CLASS_CODE, PRODUCT_SALES_CLASS_DESC, ");
      sb.append("   MAJOR_CUSTOMER, SOURCE_INSTANCE)  ");
      sb.append("VALUES ( ");
      sb.append("   :PLANT_CODE, :RM_ITEM_NO, :RM_ITEM_DESC,  ");
      sb.append("   :RM_ITEM_UM, :RM_QTY, ");
      sb.append("   :PRODUCT_ITEM_NO, :PRODUCT_ITEM_DESC, ");
      sb.append("   :FORMULA_CLASS_CODE, :FORMULA_CLASS_DESC,  ");
      sb.append("   :PRODUCT_COST_CLASS_CODE, :PRODUCT_COST_CLASS_DESC, ");
      sb.append("   :PRODUCT_GL_CLASS_CODE, :PRODUCT_GL_CLASS_DESC, ");
      sb.append("   :PRODUCT_SALES_CLASS_CODE, :PRODUCT_SALES_CLASS_DESC, ");
      sb.append("   :MAJOR_CUSTOMER, :SOURCE_INSTANCE)  ");

      targetConn = (OracleConnection) ConnectionAccessBean.getConnection(DataSource.RMINDEX);
      targetPst = (OraclePreparedStatement) targetConn.prepareStatement(sb.toString());

      sb = new StringBuilder();
      sb.append(" WITH all_periods AS  ");
      sb.append("          (select gp.period_set_name, gp.period_name, gp.start_date, gp.end_date,  ");
      sb.append("                  rank() over (order by gp.start_date) rnum  ");
      sb.append("           from gl_periods gp  ");
      sb.append("           where gp.period_set_name in ('NOV-OCT 4-4-5', 'OCT-SEP')  ");
      sb.append("             and gp.period_name not like 'END%'  ");
      sb.append("             and gp.period_name not like 'BEG%'  ");
      sb.append("             order by gp.start_date),  ");
      sb.append("      current_period_rnum AS  ");
      sb.append("          (select rnum, start_date, end_date   ");
      sb.append("           from all_periods  ");
      sb.append("           where trunc(sysdate) between start_date and end_date), ");
      sb.append("      categories_v1 AS ");
      sb.append("          (select mic.inventory_item_id, ");
      sb.append("                  mic.organization_id, ");
      sb.append("                  msib.segment1 item_no, ");
      sb.append("                  mcs.category_set_name category_name, ");
      sb.append("                  mc.segment1 category_value, ");
      sb.append("                  mc.description category_value_desc ");
      sb.append("           from mtl_item_categories mic ");
      sb.append("           inner join mtl_system_items_b msib on mic.inventory_item_id = msib.inventory_item_id and mic.organization_id = msib.organization_id ");
      sb.append("           inner join mtl_category_sets mcs on mic.category_set_id = mcs.category_set_id ");
      sb.append("           inner join mtl_categories mc on mic.category_id = mc.category_id), ");
      sb.append("      item_master_org as ");
      sb.append("          (select vca_common_api.get_item_master_org org_value ");
      sb.append("           from dual) ");
      sb.append("  SELECT gbh.plant_code, ");
      sb.append("         iim_rm.item_no rm_item_no, ");
      sb.append("         iim_rm.item_desc1 rm_item_desc, ");
      sb.append("         iim_rm.item_um rm_item_um, ");
      sb.append("         sum(case when gmd_rm.item_um = iim_rm.item_um then gmd_rm.actual_qty  ");
      sb.append("                  else gmicuom.uom_conversion(iim_rm.item_id, null, gmd_rm.actual_qty, gmd_rm.item_um, iim_rm.item_um, 0)  ");
      sb.append("             end) rm_qty, ");
      sb.append("         iim_product.item_no product_item_no, ");
      sb.append("         iim_product.item_desc1 product_item_desc, ");
      sb.append("         ffm.formula_class, ");
      sb.append("         gfc.formula_class_desc, ");
      sb.append("         iim_product.itemcost_class, ");
      sb.append("         itemcost_class_category.category_value_desc itemcost_class_desc, ");
      sb.append("         iim_product.gl_class, ");
      sb.append("         gl_class_category.category_value_desc gl_class_desc, ");
      sb.append("         iim_product.sales_class, ");
      sb.append("         sales_class_category.category_value_desc sales_class_desc, ");
      sb.append("         major_customer_category.category_value major_customer, ");
      sb.append("         null oracle_source  ");
      sb.append("    FROM gme_batch_header gbh  ");
      sb.append("    CROSS JOIN item_master_org ");
      sb.append("    INNER JOIN gme_material_details gmd_product ");
      sb.append("        ON gbh.batch_id = gmd_product.batch_id  ");
      sb.append("        and gmd_product.line_type = 1  ");
      sb.append("        and gmd_product.actual_qty > 0  ");
      sb.append("    INNER JOIN ic_item_mst iim_product ");
      sb.append("        ON iim_product.item_id = gmd_product.item_id ");
      sb.append("    INNER JOIN gme_material_details gmd_rm ");
      sb.append("        ON gmd_rm.batch_id = gbh.batch_id  ");
      sb.append("        and gmd_rm.line_type = -1  ");
      sb.append("        and gmd_rm.actual_qty > 0  ");
      sb.append("    INNER JOIN ic_item_mst iim_rm ");
      sb.append("        ON iim_rm.item_id = gmd_rm.item_id  ");
      sb.append("    INNER JOIN mtl_system_items_b msi_product ");
      sb.append("        ON msi_product.segment1 = iim_product.item_no  ");
      sb.append("        and msi_product.organization_id = item_master_org.org_value ");
      sb.append("    INNER JOIN fm_form_mst ffm ");
      sb.append("        ON ffm.formula_id = gbh.formula_id ");
      sb.append("        and ffm.formula_no not in ('FGSL-OUTPUT') ");
      sb.append("        and ffm.formula_no not like 'RMX___M.110%' ");
      sb.append("    LEFT OUTER JOIN gmd_formula_class_tl gfc ");
      sb.append("        ON ffm.formula_class = gfc.formula_class ");
      sb.append("        and gfc.language = 'US' ");
      sb.append("    LEFT OUTER JOIN categories_v1 gl_class_category ");
      sb.append("        ON gl_class_category.inventory_item_id = msi_product.inventory_item_id  ");
      sb.append("        and gl_class_category.organization_id  = msi_product.organization_id  ");
      sb.append("        and gl_class_category.category_name    = 'GL CLASS' ");
      sb.append("    LEFT OUTER JOIN categories_v1 sales_class_category ");
      sb.append("        ON sales_class_category.inventory_item_id = msi_product.inventory_item_id  ");
      sb.append("        and sales_class_category.organization_id  = msi_product.organization_id  ");
      sb.append("        and sales_class_category.category_name    = 'SALES CLASS' ");
      sb.append("    LEFT OUTER JOIN categories_v1 major_customer_category ");
      sb.append("        ON major_customer_category.inventory_item_id = msi_product.inventory_item_id  ");
      sb.append("        and major_customer_category.organization_id  = msi_product.organization_id  ");
      sb.append("        and major_customer_category.category_name    = 'MAJOR CUSTOMER' ");
      sb.append("    LEFT OUTER JOIN categories_v1 itemcost_class_category ");
      sb.append("        ON itemcost_class_category.inventory_item_id = msi_product.inventory_item_id  ");
      sb.append("        and itemcost_class_category.organization_id  = msi_product.organization_id  ");
      sb.append("        and itemcost_class_category.category_name    = 'ITEMCOST CLASS' ");
      sb.append("  WHERE gbh.batch_status = 4  ");
      sb.append("    and gbh.actual_cmplt_date between (select all_periods.start_date   ");
      sb.append("                                         from all_periods, current_period_rnum  ");
      sb.append("                                        where (current_period_rnum.rnum-12) = all_periods.rnum)  ");
      sb.append("                                  and (select all_periods.end_date   ");
      sb.append("                                         from all_periods, current_period_rnum  ");
      sb.append("                                        where (current_period_rnum.rnum-1) = all_periods.rnum) ");
      sb.append("    and iim_rm.item_no != iim_product.item_no ");
      sb.append("  GROUP BY gbh.plant_code, ");
      sb.append("           iim_rm.item_no, ");
      sb.append("           iim_rm.item_desc1, ");
      sb.append("           iim_rm.item_um, ");
      sb.append("           iim_product.item_no, ");
      sb.append("           iim_product.item_desc1, ");
      sb.append("           ffm.formula_class, ");
      sb.append("           gfc.formula_class_desc, ");
      sb.append("           iim_product.itemcost_class, ");
      sb.append("           iim_product.gl_class, ");
      sb.append("           iim_product.sales_class, ");
      sb.append("           itemcost_class_category.category_value_desc, ");
      sb.append("           gl_class_category.category_value_desc, ");
      sb.append("           sales_class_category.category_value_desc, ");
      sb.append("           major_customer_category.category_value ");

      sourceConn = (OracleConnection) ConnectionAccessBean.getConnection(dataSource);
      sourcePst = (OraclePreparedStatement) sourceConn.prepareStatement(sb.toString());

      log4jLogger.info("BATCHES - Running annual batches query against " + dataSource.getDataSourceLabel() + "...");
      rs = (OracleResultSet) sourcePst.executeQuery();
      log4jLogger.info("BATCHES - Query finished.  Streaming data from " + dataSource.getDataSourceLabel() + " to " + DataSource.RMINDEX.getDataSourceLabel() + "...");

      while (rs.next())
      {
        BatchBean bean = new BatchBean();
        bean.setPlantCode(rs.getString("plant_code"));
        bean.setRmItemNumber(rs.getString("rm_item_no"));
        bean.setRmItemDescription(rs.getString("rm_item_desc"));
        bean.setRmItemUom(rs.getString("rm_item_um"));
        bean.setRmQuantity(rs.getString("rm_qty"));
        bean.setProductItemNumber(rs.getString("product_item_no"));
        bean.setProductItemDescription(rs.getString("product_item_desc"));
        bean.setFormulaClassCode(rs.getString("formula_class"));
        bean.setFormulaClassDesc(rs.getString("formula_class_desc"));
        bean.setProductCostClassCode(rs.getString("itemcost_class"));
        bean.setProductCostClassDesc(rs.getString("itemcost_class_desc"));
        bean.setProductGlClassCode(rs.getString("gl_class"));
        bean.setProductGlClassDesc(rs.getString("gl_class_desc"));
        bean.setProductSalesClassCode(rs.getString("sales_class"));
        bean.setProductSalesClassDesc(rs.getString("sales_class_desc"));
        bean.setProductMajorCustomer(rs.getString("major_customer"));
        bean.setOracleSource(dataSource.getAnalyticsDataSource());

        targetPst.setStringAtName("PLANT_CODE", bean.getPlantCode());
        targetPst.setStringAtName("RM_ITEM_NO", bean.getRmItemNumber());
        targetPst.setStringAtName("RM_ITEM_DESC", bean.getRmItemDescription());
        targetPst.setStringAtName("RM_ITEM_UM", bean.getRmItemUom());
        targetPst.setStringAtName("RM_QTY", bean.getRmQuantity());
        targetPst.setStringAtName("PRODUCT_ITEM_NO", bean.getProductItemNumber());
        targetPst.setStringAtName("PRODUCT_ITEM_DESC", bean.getProductItemDescription());
        targetPst.setStringAtName("FORMULA_CLASS_CODE", bean.getFormulaClassCode());
        targetPst.setStringAtName("FORMULA_CLASS_DESC", bean.getFormulaClassDesc());
        targetPst.setStringAtName("PRODUCT_COST_CLASS_CODE", bean.getProductCostClassCode());
        targetPst.setStringAtName("PRODUCT_COST_CLASS_DESC", bean.getProductCostClassDesc());
        targetPst.setStringAtName("PRODUCT_GL_CLASS_CODE", bean.getProductGlClassCode());
        targetPst.setStringAtName("PRODUCT_GL_CLASS_DESC", bean.getProductGlClassDesc());
        targetPst.setStringAtName("PRODUCT_SALES_CLASS_CODE", bean.getProductSalesClassCode());
        targetPst.setStringAtName("PRODUCT_SALES_CLASS_DESC", bean.getProductSalesClassDesc());
        targetPst.setStringAtName("MAJOR_CUSTOMER", bean.getProductMajorCustomer());
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
      log4jLogger.error("BATCHES (" + dataSource.getDataSourceLabel() + ")", e);
      setError(e);
    }
    finally
    {
      JDBCUtil.close(sourcePst, rs);
      JDBCUtil.close(targetPst);
      JDBCUtil.close(sourceConn);
      JDBCUtil.close(targetConn);
    }

    log4jLogger.info("BATCHES - Done transferring batch data from " + dataSource.getDataSourceLabel() + ", count = " + rec);
  }
}
