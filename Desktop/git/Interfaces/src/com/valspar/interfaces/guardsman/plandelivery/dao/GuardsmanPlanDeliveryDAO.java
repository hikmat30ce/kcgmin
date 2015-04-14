package com.valspar.interfaces.guardsman.plandelivery.dao;

import com.valspar.interfaces.common.beans.ConnectionAccessBean;
import com.valspar.interfaces.common.enums.DataSource;
import com.valspar.interfaces.common.utils.JDBCUtil;
import com.valspar.interfaces.guardsman.plandelivery.beans.PlanBean;
import com.valspar.interfaces.guardsman.plandelivery.enums.PlanActionTaken;
import java.sql.Types;
import java.util.*;
import oracle.jdbc.*;
import org.apache.log4j.Logger;

public final class GuardsmanPlanDeliveryDAO
{
  private static Logger log4jLogger = Logger.getLogger(GuardsmanPlanDeliveryDAO.class);

  private GuardsmanPlanDeliveryDAO()
  {
  }

  public static List<PlanBean> fetchPlans()
  {
    OracleConnection conn = null;
    OraclePreparedStatement pst = null;
    OracleResultSet rs = null;

    List<PlanBean> plans = new ArrayList<PlanBean>();

    try
    {
      conn = (OracleConnection) ConnectionAccessBean.getConnection(DataSource.GUARDSMAN);

      StringBuilder sb = new StringBuilder();

      // standard plans
      sb.append("SELECT   sam_con_sa.sa_type_id, sam_con_sa.con_sa_id, sam_con.con_id, ");
      sb.append("         sam_con.last_name, sam_con.first_name, ");
      sb.append("         NVL (sam_lu_language.language_code, 'ENG') AS lang, ");
      sb.append("         sam_sa_type.description plan_name, sam_con.email, sam_sa_type.sa_id, ");
      sb.append("         sam_rtlr_addr.erp_rtlr_no, sam_con_sa.fpp_print_dt, sam_sa_out_xref.print_plan_id, ");
      sb.append("         sam_con_sa.reprint_flg, sam_rtlr_ftp.plan_emailing_flg, ");
      sb.append("         sam_con_sa.ueta_notify_dt, sam_con_sa.ueta_accept_dt, ");
      sb.append("         sam_con_sa.ueta_decline_dt, trunc(sysdate) - trunc(sam_con_sa.ueta_notify_dt) days_since_ueta_notified ");
      sb.append("FROM sam_con_sa ");
      sb.append("INNER JOIN sam_con               ON sam_con_sa.con_id = sam_con.con_id ");
      sb.append("INNER JOIN sam_rtlr_addr         ON sam_con_sa.rtlr_addr_id = sam_rtlr_addr.rtlr_addr_id ");
      sb.append("LEFT OUTER JOIN sam_lu_language  ON sam_con.language_id = sam_lu_language.language_id ");
      sb.append("INNER JOIN sam_sa_type           ON sam_con_sa.sa_type_id = sam_sa_type.sa_type_id ");
      sb.append("INNER JOIN sam_rtlr_erp          ON sam_rtlr_addr.erp_rtlr_no = sam_rtlr_erp.erp_rtlr_no ");
      sb.append("INNER JOIN sam_rtlr_ftp          ON sam_rtlr_erp.rtlr_ftp_id = sam_rtlr_ftp.rtlr_ftp_id ");
      sb.append("INNER JOIN sam_fpp_erp_sa        ON sam_con_sa.sa_type_id = sam_fpp_erp_sa.sa_type_id AND sam_rtlr_erp.erp_rtlr_no = sam_fpp_erp_sa.erp_rtlr_no ");
      sb.append("LEFT OUTER JOIN sam_sa_out_xref  ON sam_sa_type.sa_id = sam_sa_out_xref.sa_id AND NVL(sam_lu_language.language_code, 'ENG') = sam_sa_out_xref.language ");
      sb.append("WHERE (select count(*) ");
      sb.append("       from sam_con_addr ");
      sb.append("       where con_id = sam_con.con_id ");
      sb.append("         and type in ('Ship To', 'Home') ");
      sb.append("         and status = 'Active') > 0 ");
      sb.append("  AND sam_sa_type.coverage_type <> 'X' ");
      sb.append("  AND sam_con_sa.sa_status = 'Active' ");
      sb.append("  AND sam_con_sa.fpp_print_dt IS NULL ");
      sb.append("  AND (sam_con_sa.logged_uid = 'POS' OR sam_con_sa.origination = 'REG') ");
      sb.append("  and sam_rtlr_ftp.plan_printing_flg = 'Y' ");

      sb.append("UNION ");

      // Reprints
      sb.append("SELECT   sam_con_sa.sa_type_id, sam_con_sa.con_sa_id, sam_con.con_id, ");
      sb.append("         sam_con.last_name, sam_con.first_name, ");
      sb.append("         NVL (sam_lu_language.language_code, 'ENG') AS lang, ");
      sb.append("         sam_sa_type.description plan_name, sam_con.email, sam_sa_type.sa_id, ");
      sb.append("         sam_rtlr_addr.erp_rtlr_no, sam_con_sa.fpp_print_dt, sam_sa_out_xref.print_plan_id, ");
      sb.append("         sam_con_sa.reprint_flg, 'Y' plan_emailing_flg, ");
      sb.append("         sam_con_sa.ueta_notify_dt, sam_con_sa.ueta_accept_dt, ");
      sb.append("         sam_con_sa.ueta_decline_dt, trunc(sysdate) - trunc(sam_con_sa.ueta_notify_dt) days_since_ueta_notified ");
      sb.append("FROM sam_con_sa ");
      sb.append("INNER JOIN sam_con               ON sam_con_sa.con_id = sam_con.con_id ");
      sb.append("INNER JOIN sam_sa_type           ON sam_con_sa.sa_type_id = sam_sa_type.sa_type_id ");
      sb.append("INNER JOIN sam_rtlr_addr         ON sam_con_sa.rtlr_addr_id = sam_rtlr_addr.rtlr_addr_id ");
      sb.append("LEFT OUTER JOIN sam_lu_language  ON sam_con.language_id = sam_lu_language.language_id ");
      sb.append("LEFT OUTER JOIN sam_sa_out_xref  ON sam_sa_type.sa_id = sam_sa_out_xref.sa_id AND NVL(sam_lu_language.language_code, 'ENG') = sam_sa_out_xref.language ");
      sb.append("WHERE (select count(*) ");
      sb.append("       from sam_con_addr ");
      sb.append("       where con_id = sam_con.con_id ");
      sb.append("         and type in ('Ship To', 'Home') ");
      sb.append("         and status = 'Active') > 0 ");
      sb.append("  AND sam_con_sa.sa_status = 'Active' ");
      sb.append("  AND sam_con_sa.reprint_flg = 'Y' ");
      sb.append("  AND sam_con_sa.fpp_reprint_dt IS NULL ");

      sb.append("UNION ");

      // Non-POS plans
      sb.append("SELECT   sam_con_sa.sa_type_id, sam_con_sa.con_sa_id, sam_con.con_id, ");
      sb.append("         sam_con.last_name, sam_con.first_name, ");
      sb.append("         NVL (sam_lu_language.language_code, 'ENG') AS lang, ");
      sb.append("         sam_sa_type.description plan_name, sam_con.email, sam_sa_type.sa_id, ");
      sb.append("         sam_rtlr_addr.erp_rtlr_no, sam_con_sa.fpp_print_dt, sam_sa_out_xref.print_plan_id, ");
      sb.append("         sam_con_sa.reprint_flg, 'Y' plan_emailing_flg, ");
      sb.append("         sam_con_sa.ueta_notify_dt, sam_con_sa.ueta_accept_dt, ");
      sb.append("         sam_con_sa.ueta_decline_dt, trunc(sysdate) - trunc(sam_con_sa.ueta_notify_dt) days_since_ueta_notified ");
      sb.append("FROM sam_con_sa ");
      sb.append("INNER JOIN sam_con               ON sam_con_sa.con_id = sam_con.con_id ");
      sb.append("INNER JOIN sam_sa_type           ON sam_con_sa.sa_type_id = sam_sa_type.sa_type_id ");
      sb.append("INNER JOIN sam_rtlr_addr         ON sam_con_sa.rtlr_addr_id = sam_rtlr_addr.rtlr_addr_id ");
      sb.append("LEFT OUTER JOIN sam_lu_language  ON sam_con.language_id = sam_lu_language.language_id ");
      sb.append("INNER JOIN sam_sa_out_xref       ON sam_sa_type.sa_id = sam_sa_out_xref.sa_id AND NVL(sam_lu_language.language_code, 'ENG') = sam_sa_out_xref.language ");
      sb.append("WHERE (select count(*) ");
      sb.append("       from sam_con_addr ");
      sb.append("       where con_id = sam_con.con_id ");
      sb.append("         and type in ('Ship To', 'Home') ");
      sb.append("         and status = 'Active') > 0 ");
      sb.append("  AND sam_sa_type.coverage_type <> 'X' ");
      sb.append("  AND (sam_con_sa.logged_uid != 'POS' OR sam_con_sa.origination != 'REG') ");
      sb.append("  AND sam_sa_type.print_on_creation = 'Y' ");
      sb.append("  AND sam_con_sa.sa_status = 'Active' ");
      sb.append("  AND sam_con_sa.fpp_print_dt IS NULL ");

      sb.append("ORDER BY con_sa_id, con_id "); //erp_rtlr_no

      pst = (OraclePreparedStatement) conn.prepareStatement(sb.toString());

      rs = (OracleResultSet) pst.executeQuery();

      while (rs.next())
      {
        PlanBean bean = new PlanBean();
        bean.setSaTypeId(rs.getString("sa_type_id"));
        bean.setConSaId(rs.getString("con_sa_id"));
        bean.setConId(rs.getString("con_id"));
        bean.getConsumer().setLastName(rs.getString("last_name"));
        bean.getConsumer().setFirstName(rs.getString("first_name"));
        bean.setLanguageCode(rs.getString("lang"));
        bean.setPlanName(rs.getString("plan_name"));
        bean.getConsumer().setEmail(rs.getString("email"));
        bean.setSaId(rs.getString("sa_id"));
        bean.setErpRetailerNo(rs.getString("erp_rtlr_no"));
        bean.setPrintDate(rs.getTimestamp("fpp_print_dt"));
        bean.setPrintPlanId(rs.getString("print_plan_id"));
        bean.setReprintFlag(rs.getString("reprint_flg"));
        bean.setPlanEmailingFlag(rs.getString("plan_emailing_flg"));
        bean.setUetaNotifyDate(rs.getTimestamp("ueta_notify_dt"));
        bean.setUetaAccepted(rs.getTimestamp("ueta_accept_dt") != null);
        bean.setUetaDeclined(rs.getTimestamp("ueta_decline_dt") != null);
        bean.setDaysSinceUetaNotified(rs.getLong("days_since_ueta_notified"));

        plans.add(bean);
      }
    }
    catch (Exception e)
    {
      log4jLogger.error(e);
    }
    finally
    {
      JDBCUtil.close(pst, rs);
      JDBCUtil.close(conn);
    }
    return plans;
  }

  public static void populateAddresses(List<PlanBean> plans)
  {
    OracleConnection conn = null;
    OraclePreparedStatement pst = null;
    OracleResultSet rs = null;

    try
    {
      conn = (OracleConnection) ConnectionAccessBean.getConnection(DataSource.GUARDSMAN);

      StringBuilder sb = new StringBuilder();

      sb.append("select a.con_addr_id, a.address1, a.address2, a.city, s.state, a.postal_code ");
      sb.append("from sam_con_addr a ");
      sb.append("left outer join sam_lu_state s ");
      sb.append("  on a.state_id = s.state_id ");
      sb.append("where a.con_id = :con_id ");
      sb.append("  and a.type in ('Ship To', 'Home') ");
      sb.append("  and a.status = 'Active' ");
      sb.append("order by case a.type when 'Ship To' then 1 ");
      sb.append("                     when 'Home' then 2 end ");

      pst = (OraclePreparedStatement) conn.prepareStatement(sb.toString());

      for (PlanBean plan: plans)
      {
        pst.clearParameters();
        pst.setStringAtName("con_id", plan.getConId());
        rs = (OracleResultSet) pst.executeQuery();

        if (rs.next())
        {
          plan.setConAddrId(rs.getString("con_addr_id"));
          plan.setAddress1(rs.getString("address1"));
          plan.setAddress2(rs.getString("address2"));
          plan.setCity(rs.getString("city"));
          plan.setState(rs.getString("state"));
          plan.setPostalCode(rs.getString("postal_code"));
        }

        JDBCUtil.close(rs);
      }
    }
    catch (Exception e)
    {
      log4jLogger.error(e);
    }
    finally
    {
      JDBCUtil.close(pst, rs);
      JDBCUtil.close(conn);
    }
  }

  private static boolean updateServiceAgreement(PlanBean plan)
  {
    StringBuilder sb = new StringBuilder();
    OracleConnection conn = null;
    OraclePreparedStatement pst = null;

    try
    {
      if (plan.getActionTaken() == PlanActionTaken.UETA_EMAILED)
      {
        sb.append("update sam_con_sa set ");
        sb.append("   ueta_notify_dt = sysdate, ");
        sb.append("   ueta_unique_id = con_id || con_sa_id ");
        sb.append("where con_sa_id = :con_sa_id ");
      }
      else if (plan.getActionTaken() == PlanActionTaken.PDF_EMAILED || plan.getActionTaken() == PlanActionTaken.PRINTED)
      {
        if (plan.getPrintDate() == null)
        {
          sb.append("update sam_con_sa set ");
          sb.append("   fpp_print_dt = sysdate ");
          sb.append("where con_sa_id = :con_sa_id ");
        }
        else
        {
          sb.append("update sam_con_sa set ");
          sb.append("   reprint_flg = 'N', ");
          sb.append("   fpp_reprint_dt = sysdate ");
          sb.append("where con_sa_id = :con_sa_id ");
        }
      }

      conn = (OracleConnection) ConnectionAccessBean.getConnection(DataSource.GUARDSMAN);
      pst = (OraclePreparedStatement) conn.prepareStatement(sb.toString());
      pst.setStringAtName("con_sa_id", plan.getConSaId());

      int recordsUpdated = pst.executeUpdate();

      if (recordsUpdated != 1)
      {
        log4jLogger.error("Expected to update 1 service agreement, actually updated " + recordsUpdated + ", con_sa_id: " + plan.getConSaId());
        return false;
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
      JDBCUtil.close(pst);
      JDBCUtil.close(conn);
    }
  }

  private static boolean updateConsumer(PlanBean plan)
  {
    StringBuilder sb = new StringBuilder();
    OracleConnection conn = null;
    OraclePreparedStatement pst = null;

    try
    {
      if (plan.getActionTaken() == PlanActionTaken.PRINTED)
      {
        sb.append("update sam_con set ");
        sb.append("   email = null ");
        sb.append("where con_id = :con_id ");

        conn = (OracleConnection) ConnectionAccessBean.getConnection(DataSource.GUARDSMAN);
        pst = (OraclePreparedStatement) conn.prepareStatement(sb.toString());
        pst.setStringAtName("con_id", plan.getConId());

        int recordsUpdated = pst.executeUpdate();

        if (recordsUpdated != 1)
        {
          log4jLogger.error("Expected to update 1 consumer record, actually updated " + recordsUpdated + ", con_id: " + plan.getConId());
          return false;
        }
      }
    }
    catch (Exception e)
    {
      log4jLogger.error(e);
      return false;
    }
    finally
    {
      JDBCUtil.close(pst);
      JDBCUtil.close(conn);
    }

    return true;
  }

  private static String getNextDiaryId()
  {
    String nextId = null;

    OracleConnection conn = null;
    OracleCallableStatement cst = null;

    try
    {
      conn = (OracleConnection) ConnectionAccessBean.getConnection(DataSource.GUARDSMAN);

      StringBuilder sb = new StringBuilder();
      sb.append("declare ");
      sb.append("  sequence_value number; ");
      sb.append("begin ");
      sb.append("  update counter ");
      sb.append("  set last_one = last_one + 1 ");
      sb.append("  where item = 'sam_con_sa_diary.con_sa_diary_id' ");
      sb.append("  returning last_one into sequence_value; ");

      sb.append("  ? := sequence_value; ");
      sb.append("end; ");

      cst = (OracleCallableStatement) conn.prepareCall(sb.toString());
      cst.registerOutParameter(1, Types.INTEGER);

      synchronized (log4jLogger)
      {
        cst.execute();
        nextId = String.valueOf(cst.getInt(1));
      }
    }
    catch (Exception e)
    {
      log4jLogger.error(e);
    }
    finally
    {
      JDBCUtil.close(cst);
      JDBCUtil.close(conn);
    }

    return nextId;
  }

  private static boolean insertDiaryEntry(PlanBean plan)
  {
    OracleConnection conn = null;
    OraclePreparedStatement pst = null;

    try
    {
      String conSaDiaryId = getNextDiaryId();

      String notes = null;

      switch (plan.getActionTaken())
      {
        case UETA_EMAILED:
          notes = "FPP UETA Notice Emailed to Consumer";
          break;
        case PDF_EMAILED:
          notes = "FPP Emailed to Consumer";
          break;
        case PRINTED:
          notes = "FPP Sent to Consumer";
          break;
      }

      StringBuilder sb = new StringBuilder();
      sb.append("insert into sam_con_sa_diary( ");
      sb.append("     con_sa_diary_id, con_sa_id, logged_dt, logged_uid, notes) ");
      sb.append("values (");
      sb.append("     :con_sa_diary_id, :con_sa_id, sysdate, 'SAMS', :notes) ");

      conn = (OracleConnection) ConnectionAccessBean.getConnection(DataSource.GUARDSMAN);
      pst = (OraclePreparedStatement) conn.prepareStatement(sb.toString());
      pst.setStringAtName("con_sa_diary_id", conSaDiaryId);
      pst.setStringAtName("con_sa_id", plan.getConSaId());
      pst.setStringAtName("notes", notes);
      pst.executeUpdate();

      return true;
    }
    catch (Exception e)
    {
      log4jLogger.error(e);
      return false;
    }
    finally
    {
      JDBCUtil.close(pst);
      JDBCUtil.close(conn);
    }
  }

  public static void updateApplix(PlanBean plan)
  {
    updateServiceAgreement(plan);

    updateConsumer(plan);

    // Write diary entry to service agreement for what action was taken
    insertDiaryEntry(plan);
  }

  public static boolean createOverstockASN()
  {
    OracleConnection conn = null;
    OracleCallableStatement cst = null;

    try
    {
      conn = (OracleConnection) ConnectionAccessBean.getConnection(DataSource.GUARDSMAN);
      cst = (OracleCallableStatement) conn.prepareCall("{call create_overstock_asn}");
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
      JDBCUtil.close(conn);
    }
  }
}
