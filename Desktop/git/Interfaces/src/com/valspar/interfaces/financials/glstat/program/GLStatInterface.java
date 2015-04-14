package com.valspar.interfaces.financials.glstat.program;

import com.valspar.interfaces.common.BaseInterface;
import com.valspar.interfaces.common.beans.ConnectionAccessBean;
import com.valspar.interfaces.common.enums.DataSource;
import com.valspar.interfaces.common.utils.*;
import com.valspar.interfaces.financials.glstat.beans.*;
import java.sql.*;
import java.util.*;
import oracle.jdbc.*;
import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;

public class GLStatInterface extends BaseInterface
{
  private static Logger log4jLogger = Logger.getLogger(GLStatInterface.class);
  boolean loadAnalyticsData;
  boolean import11iStats;
  boolean loadAnalyticsDataSuccess;

  public GLStatInterface()
  {
  }

  public void execute()
  {
    String jndiName = getParameterValue("jndiName");
    String loadAnalyticsData = getParameterValue("loadAnalyticsData");
    String import11iStats = getParameterValue("import11iStats");
    if (StringUtils.equalsIgnoreCase(loadAnalyticsData, "true"))
    {
      this.setLoadAnalyticsData(true);
    }
    else
    {
      this.setLoadAnalyticsData(false);
    }

    if (StringUtils.equalsIgnoreCase(import11iStats, "true"))
    {
      this.setImport11iStats(true);
    }
    else
    {
      this.setImport11iStats(false);
    }
    startInterface(jndiName);
  }

  public void startInterface(String jndiName)
  {
    DataSource toDataSource = CommonUtility.getDataSourceByJndiName(jndiName);
    this.setLoadAnalyticsDataSuccess(true);

    if (toDataSource != null)
    {
      if (this.isLoadAnalyticsData())
      {
        if (analyticsDailyLoadComplete(toDataSource))
        {
          loadGLInterfaceTable(buildGLStatBeans(toDataSource), toDataSource);
        }
        else
        {
          this.setLoadAnalyticsDataSuccess(false);
          log4jLogger.error("GLStatInterface cannot run because the Analytics load did not complete.");
        }
      }
      if (this.isLoadAnalyticsDataSuccess() && this.isImport11iStats())
      {
        importStatsTo11i(toDataSource);
      }
    }
    else
    {
      log4jLogger.error("GLStatInterface cannot run because the jndiName parameter is not valid.");
    }

    this.getEmailMessages().addAll(fetchErrorMessages());
  }

  public void importStatsTo11i(DataSource datasource)
  {
    OracleCallableStatement cst = null;
    OracleConnection toERPConn = (OracleConnection) ConnectionAccessBean.getConnection(datasource);
    StringBuilder sb = new StringBuilder();
    sb.append("declare ");
    sb.append("  l_return     VARCHAR2 (100); ");
    sb.append("begin ");
    sb.append("  l_return := vca_gl_pkg.main ('VCA_STAT_IMPORT_POST'); ");
    sb.append("  ? := l_return; ");
    sb.append("end; ");

    try
    {
      cst = (OracleCallableStatement) toERPConn.prepareCall(sb.toString());
      cst.registerOutParameter(1, Types.VARCHAR);
      log4jLogger.info("running VCA_STAT_IMPORT_POST");
      cst.execute();
      String returnStatus = String.valueOf(cst.getString(1));
      if (StringUtils.equalsIgnoreCase(returnStatus, "success"))
      {
        log4jLogger.info("VCA_STAT_IMPORT_POST ran successfully");
      }
      else
      {
        log4jLogger.error("VCA_STAT_IMPORT_POST outcome: " + returnStatus);
      }
    }
    catch (Exception e)
    {
      log4jLogger.error("VCA_STAT_IMPORT_POST outcome: " + e);
    }
    finally
    {
      JDBCUtil.close(cst);
      JDBCUtil.close(toERPConn);
    }
  }

  private boolean analyticsDailyLoadComplete(DataSource datasource)
  {
    Connection analyticsConn = null;
    PreparedStatement pst = null;
    ResultSet rs = null;
    boolean loadComplete = false;
    String analyticsRefreshDBColumn = StringUtils.left(datasource.getAnalyticsDataSource(), 2);

    try
    {
      analyticsConn = ConnectionAccessBean.getConnection(DataSource.ANALYTICS);
      StringBuilder sb = new StringBuilder();
      sb.append("SELECT  Time.\"Fiscal Year\" saw_0 ");
      sb.append("FROM \"Financials - Statistics\" ");
      sb.append("WHERE Time.\"Fiscal Year\" IS NULL ");
      sb.append("AND VALUEOF(VAL_");
      sb.append(analyticsRefreshDBColumn);
      sb.append("PR_INCR_LOAD_COMPLETE) = 'Y'");

      pst = analyticsConn.prepareStatement(sb.toString());
      rs = pst.executeQuery();

      if (rs.next())
      {
        loadComplete = true;
      }
    }
    catch (Exception e)
    {
      log4jLogger.error(e);
    }
    finally
    {
      JDBCUtil.close(pst, rs);
      JDBCUtil.close(analyticsConn);
    }

    return loadComplete;

  }

  public String findAutosysUser(DataSource datasource)
  {
    PreparedStatement pst = null;
    ResultSet rs = null;
    String autosysUserId = null;
    OracleConnection conn = null;
    try
    {
      conn = (OracleConnection) ConnectionAccessBean.getConnection(datasource);
      pst = conn.prepareStatement("select user_id from fnd_user where user_name = 'AUTOSYS'");
      rs = pst.executeQuery();

      if (rs.next())
      {
        autosysUserId = rs.getString(1);
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

    return autosysUserId;
  }

  public ArrayList<GLAccountBean> buildGLAccounts()
  {
    Statement st = null;
    ResultSet rs = null;
    ArrayList<GLAccountBean> resultList = new ArrayList<GLAccountBean>();
    Connection analyticsConn = null;

    try
    {
      analyticsConn = ConnectionAccessBean.getConnection(DataSource.ANALYTICS);
      StringBuffer sql = new StringBuffer();
      sql.append("SELECT \"Statistic Extract Driver Table\".\"GL Segment 5 Value\" gl_account_no, ");
      sql.append(" \"Statistic Extract Driver Table\".\"Analytics Folder Name\" folder_name, ");
      sql.append(" \"Statistic Extract Driver Table\".\"Statistic Data Column Name\" column_name ");
      sql.append("FROM \"Financials - Statistics\" ");
      sql.append("WHERE \"Statistic Extract Driver Table\".\"GL Segment 5 Value\" = ");
      sql.append("SUBSTRING(\"Statistic Extract Driver Table\".\"Statistic Data Column Name\" FROM 1 FOR 7) ");

      st = analyticsConn.createStatement();
      rs = st.executeQuery(sql.toString());

      while (rs.next())
      {
        GLAccountBean glBean = new GLAccountBean();
        glBean.setGlAccount(rs.getString("gl_account_no"));
        glBean.setFolderName(rs.getString("folder_name"));
        glBean.setColumnName(rs.getString("column_name"));

        resultList.add(glBean);
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

    return resultList;
  }

  public boolean validGLAccount(GLStatBean bean, DataSource datasource)
  {
    if (validGlSegment("PPP", bean.getProfitCenter(), datasource) && validGlSegment("LOC", bean.getLocation(), datasource) && validGlSegment("COST", bean.getCostCenter(), datasource))
    {
      return true;
    }

    return false;
  }

  public boolean validGlSegment(String segmentName, String flexValue, DataSource datasource)
  {
    OracleConnection conn = null;
    Statement stmt = null;
    ResultSet rs = null;

    boolean validGlSegment = true;

    try
    {
      conn = (OracleConnection) ConnectionAccessBean.getConnection(datasource);
      StringBuffer sql = new StringBuffer();
      sql.append("select distinct v.flex_value ");
      sql.append("from fnd_flex_values_vl v, ");
      sql.append("     fnd_id_flex_segments seg ");
      sql.append("where seg.flex_value_set_id = v.flex_value_set_id ");
      sql.append("and seg.segment_name = '");
      sql.append(segmentName);
      sql.append("' and v.flex_value = '");
      sql.append(flexValue);
      sql.append("'");

      stmt = conn.createStatement();
      rs = stmt.executeQuery(sql.toString());

      if (!rs.next())
      {
        validGlSegment = false;
        log4jLogger.info("Invalid segment found - segment_name: " + segmentName + " flex_value: " + flexValue);
      }
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

    return validGlSegment;
  }

  public void populateGLBean(GLAccountBean bean, String autosysUserId, ArrayList<GLStatBean> resultList, DataSource datasource)
  {
    Statement st = null;
    ResultSet rs = null;
    Connection analyticsConn = null;
    StringBuilder sql = new StringBuilder();
    
    String analyticsFiscalCalendar;//Can  not use the analyticsFiscalCalendar setup in DataSource enums.  
                                   //The TIME.FiscalCalendar is different depending on the folder being queried.
    if (StringUtils.equalsIgnoreCase(datasource.getAnalyticsDataSource(), "NAPR"))
    {
      analyticsFiscalCalendar = "NorthAmerica";
    }
    else 
    {
      analyticsFiscalCalendar = "International";
    }

    try
    {
      analyticsConn = ConnectionAccessBean.getConnection(DataSource.ANALYTICS);
      sql.append("SELECT  \"Time\".\"Fiscal Month End Date\" accounting_date,");
      sql.append("         \"Time\".\"User Independent Fiscal Period Name\" period_name,");
      sql.append("         \"" + bean.getFolderName() + "\".\"Set of Books ID\" set_of_books_id,");
      sql.append("         \"" + bean.getFolderName() + "\".\"GL Company Code Segment 1\" company_code,");
      sql.append("         \"" + bean.getFolderName() + "\".\"GL Profit Center Segment 2\" profit_center,");
      sql.append("         \"" + bean.getFolderName() + "\".\"GL Location Segment 3\" location,");
      sql.append("         \"" + bean.getFolderName() + "\".\"GL Cost Center Segment 4\" cost_center,");
      sql.append("         '" + bean.getGlAccount() + "' account, ");
      sql.append("         \"" + bean.getFolderName() + "\".\"Currency Code\" currency_code,");
      sql.append("         \"" + bean.getFolderName() + "\".\"GL Account Category Segment 6\" segment6,");
      sql.append("         \"" + bean.getFolderName() + "\".\"GL Segment 7\" segment7, ");
      sql.append("         \"" + bean.getFolderName() + "\".\"" + bean.getColumnName() + "\" entered_dr, ");
      sql.append("         UPPER (CAST (CURRENT_DATE AS CHAR)) today ");
      sql.append(" FROM \"Financials - Statistics\" ");
      sql.append("WHERE (\"" + bean.getFolderName() + "\".\"Oracle 11i Source\" = '" + datasource.getAnalyticsDataSource() + "') ");
      sql.append("AND (TIME.\"Fiscal Calendar\" = '" + analyticsFiscalCalendar + "') ");
      sql.append("AND (\"Time Current Period Codes\".\"User Independent Current Fiscal Month Code\" = 'Previous') ");
      sql.append("AND \"" + bean.getFolderName() + "\".\"" + bean.getColumnName() + "\" <> 0 ");

      st = analyticsConn.createStatement();
      rs = st.executeQuery(sql.toString());

      while (rs.next())
      {
        GLStatBean glBean = new GLStatBean();
        glBean.setStatus("NEW");
        String accountingDate = rs.getString("accounting_date").substring(0, 10);
        glBean.setAccountingDate(accountingDate);
        glBean.setSetOfBooksID(rs.getString("set_of_books_id"));
        glBean.setPeriodName(rs.getString("period_name"));
        glBean.setCurrencyCode(rs.getString("currency_code"));
        glBean.setDateCreated(rs.getString("today"));
        glBean.setCreatedBy(autosysUserId);
        glBean.setActualFlag("A");
        glBean.setUserJECategoryName("Statistics");
        glBean.setUserJESourceName("Analytics");
        glBean.setCompanyCode(StringUtils.leftPad(rs.getString("company_code"), 4, "0"));
        if (StringUtils.isAlpha(rs.getString("profit_center")))
        {
          glBean.setProfitCenter("0000");
        }
        else
        {
          glBean.setProfitCenter(StringUtils.leftPad(rs.getString("profit_center"), 4, "0"));
        }
        glBean.setLocation(StringUtils.leftPad(rs.getString("location"), 4, "0"));
        glBean.setCostCenter(StringUtils.leftPad(rs.getString("cost_center"), 4, "0"));
        glBean.setAccount(StringUtils.leftPad(rs.getString("account"), 7, "0"));
        glBean.setSegment6(StringUtils.leftPad(rs.getString("segment6"), 4, "0"));
        glBean.setSegment7(StringUtils.leftPad(rs.getString("segment7"), 3, "0"));
        if (datasource.getDataSourceLabel().equalsIgnoreCase("EMEAI") || datasource.getDataSourceLabel().equalsIgnoreCase("Asia-Pac"))
        {
          glBean.setSegment8("000000");
        }
        glBean.setEnteredDR(rs.getString("entered_dr"));
        glBean.setReference4(rs.getString("today"));
        StringBuilder sb = new StringBuilder();
        sb.append(rs.getString("company_code")); //segment1
        sb.append("-");
        sb.append(rs.getString("profit_center")); //segment2
        sb.append("-");
        sb.append(rs.getString("location")); //segment3
        sb.append("-");
        sb.append(rs.getString("account")); //segment5
        sb.append(" ");
        sb.append(rs.getString("currency_code"));
        glBean.setReference10(sb.toString());

        resultList.add(glBean);
      }
    }
    catch (Exception e)
    {
      log4jLogger.error(e);
      //log4jLogger.error("Analytics query: " + sql.toString());
    }
    finally
    {
      JDBCUtil.close(st, rs);
      JDBCUtil.close(analyticsConn);
    }

  }

  public ArrayList<GLStatBean> buildGLStatBeans(DataSource datasource)
  {
    log4jLogger.info("Building GL Stat records from Analytics; datasource: " + datasource.getDataSourceLabel());
    ArrayList<GLStatBean> resultList = new ArrayList<GLStatBean>();
    List<GLAccountBean> GLAccounts = buildGLAccounts();
    String autosysUserId = findAutosysUser(datasource);

    for (GLAccountBean bean: GLAccounts)
    {
      populateGLBean(bean, autosysUserId, resultList, datasource);
    }
    return resultList;

  }

  public void loadGLInterfaceTable(ArrayList<GLStatBean> glStatBeanList, DataSource datasource)
  {
    log4jLogger.info("Loading GL Stat records to " + datasource.getDataSourceLabel());
    OracleConnection conn = null;
    OraclePreparedStatement pstmt = null;
    String autosysUserId = findAutosysUser(datasource);

    try
    {
      conn = (OracleConnection) ConnectionAccessBean.getConnection(datasource);
      StringBuilder sql = new StringBuilder();
      sql.append("INSERT INTO GL.GL_INTERFACE (");
      sql.append("STATUS,SET_OF_BOOKS_ID,ACCOUNTING_DATE,CURRENCY_CODE,DATE_CREATED,");
      sql.append("CREATED_BY,ACTUAL_FLAG,USER_JE_CATEGORY_NAME,USER_JE_SOURCE_NAME,");
      sql.append("SEGMENT1,SEGMENT2,SEGMENT3,SEGMENT4,SEGMENT5,SEGMENT6,SEGMENT7,");
      sql.append("ENTERED_DR,ENTERED_CR,REFERENCE4,PERIOD_NAME,REFERENCE10 ");
      if (datasource.getDataSourceLabel().equalsIgnoreCase("EMEAI") || datasource.getDataSourceLabel().equalsIgnoreCase("Asia-Pac"))
      {
        sql.append(",SEGMENT8 ) ");
      }
      else
      {
        sql.append(" ) ");
      }
      sql.append("VALUES (");
      sql.append(":STATUS,:SET_OF_BOOKS_ID,to_date(:ACCOUNTING_DATE,'YYYY-MM-DD'),:CURRENCY_CODE, :DATE_CREATED,");
      sql.append(":CREATED_BY,:ACTUAL_FLAG,:USER_JE_CATEGORY_NAME,:USER_JE_SOURCE_NAME,");
      sql.append(":SEGMENT1,:SEGMENT2,:SEGMENT3,:SEGMENT4,:SEGMENT5,:SEGMENT6,:SEGMENT7,");
      sql.append(":ENTERED_DR,:ENTERED_CR,:REFERENCE4,:PERIOD_NAME,:REFERENCE10 ");
      if (datasource.getDataSourceLabel().equalsIgnoreCase("EMEAI") || datasource.getDataSourceLabel().equalsIgnoreCase("Asia-Pac"))
      {
        sql.append(",:SEGMENT8 ) ");
      }
      else
      {
        sql.append(" ) ");
      }

      pstmt = (OraclePreparedStatement) conn.prepareStatement(sql.toString());

      for (GLStatBean bean: glStatBeanList)
      {
        pstmt.clearParameters();
        pstmt.setString(1, bean.getStatus());
        pstmt.setString(2, bean.getSetOfBooksID());
        pstmt.setString(3, bean.getAccountingDate());
        pstmt.setString(4, bean.getCurrencyCode());
        pstmt.setString(5, bean.getDateCreated());
        pstmt.setString(6, autosysUserId); //created_by
        pstmt.setString(7, bean.getActualFlag());
        pstmt.setString(8, bean.getUserJECategoryName());
        pstmt.setString(9, bean.getUserJESourceName());
        pstmt.setString(10, bean.getCompanyCode()); //segment1
        if (validGLAccount(bean, datasource))
        {
          pstmt.setString(11, bean.getProfitCenter()); //segment2
          pstmt.setString(12, bean.getLocation()); //segment3
          pstmt.setString(13, bean.getCostCenter()); //segment4
          pstmt.setString(14, bean.getAccount()); //segment5
          pstmt.setString(15, bean.getSegment6()); //segment6  defaults to 9999
          pstmt.setString(16, bean.getSegment7()); //segment7  defaults to 000
        }
        else
        {
          pstmt.setString(11, "1700"); //segment2
          pstmt.setString(12, "0000"); //segment3
          pstmt.setString(13, "0000"); //segment4
          pstmt.setString(14, "9999999"); //segment5
          pstmt.setString(15, "9999"); //segment6
          pstmt.setString(16, "000"); //segment7
        }
        pstmt.setString(17, bean.getEnteredDR());
        pstmt.setString(18, bean.getEnteredCR());
        pstmt.setString(19, bean.getReference4()); //dd-mon-rr
        pstmt.setString(20, bean.getPeriodName());
        pstmt.setString(21, bean.getReference10()); //concat company,location,cost_center,account,currency_code
        if (datasource.getDataSourceLabel().equalsIgnoreCase("EMEAI") || datasource.getDataSourceLabel().equalsIgnoreCase("Asia-Pac"))
        {
          pstmt.setString(22, bean.getSegment8()); //segment8  defaults to 000000
        }

        pstmt.executeUpdate();
      }
      log4jLogger.info("GL STAT Interface process loaded " + glStatBeanList.size() + " records into " + datasource.getDataSourceLabel());
    }
    catch (Exception e)
    {
      this.setLoadAnalyticsDataSuccess(false);
      log4jLogger.error(e);
    }
    finally
    {
      JDBCUtil.close(pstmt);
      JDBCUtil.close(conn);
    }
  }

  public void setLoadAnalyticsData(boolean loadAnalyticsData)
  {
    this.loadAnalyticsData = loadAnalyticsData;
  }

  public boolean isLoadAnalyticsData()
  {
    return loadAnalyticsData;
  }

  public void setImport11iStats(boolean import11iStats)
  {
    this.import11iStats = import11iStats;
  }

  public boolean isImport11iStats()
  {
    return import11iStats;
  }

  public void setLoadAnalyticsDataSuccess(boolean loadAnalyticsDataSuccess)
  {
    this.loadAnalyticsDataSuccess = loadAnalyticsDataSuccess;
  }

  public boolean isLoadAnalyticsDataSuccess()
  {
    return loadAnalyticsDataSuccess;
  }
}
