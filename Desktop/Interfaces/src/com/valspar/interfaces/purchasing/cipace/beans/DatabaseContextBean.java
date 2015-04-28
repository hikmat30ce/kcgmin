package com.valspar.interfaces.purchasing.cipace.beans;

import com.valspar.interfaces.common.utils.*;
import java.sql.Connection;
import java.util.*;
import oracle.jdbc.*;

public class DatabaseContextBean
{
  private String orgId;
  private String orgName;
  private String userId;
  private String responsibilityId;
  private String applicationId;
  private Connection connection;
  private String poBatchId;
  private String orgSetOfBooksId;
  private String orgCurrencyCode;
  private List<PoHeadersInterfaceBean> purchaseOrders = new ArrayList<PoHeadersInterfaceBean>();

  public void initialize(String orgId, String orgName, String username) throws Exception
  {
    this.orgId = orgId;
    this.orgName = orgName;

    OraclePreparedStatement pst = null;
    OracleResultSet rs = null;

    try
    {
      StringBuilder sb = new StringBuilder();
      sb.append("select distinct ");
      sb.append("       trim(substr(hou.name, 1, 3)) org_code, ");
      sb.append("       pv.profile_option_value org_id, ");
      sb.append("       fr.responsibility_name, ");
      sb.append("       first_value(fr.responsibility_id) over (partition by pv.profile_option_value order by fr.responsibility_name desc) responsibility_id, ");
      sb.append("       first_value(fr.application_id) over (partition by pv.profile_option_value order by fr.responsibility_name desc) application_id ");
      sb.append("from apps.fnd_responsibility_vl fr ");
      sb.append("inner join fnd_profile_options_vl pvl ");
      sb.append("    on pvl.user_profile_option_name = 'MO: Operating Unit' ");
      sb.append("inner join fnd_profile_option_values pv ");
      sb.append("    on pv.level_value = fr.responsibility_id ");
      sb.append("    and pv.level_id = 10003 ");
      sb.append("    and pv.profile_option_id = pvl.profile_option_id ");
      sb.append("    and pv.profile_option_value = :ORG_ID ");
      sb.append("inner join hr_operating_units hou ");
      sb.append("    on to_char(hou.organization_id) = pv.profile_option_value ");
      sb.append("    and upper(hou.name) not like '%STAT%' ");
      sb.append("where upper(fr.responsibility_name) like '% OPM ALL' ");

      pst = (OraclePreparedStatement) connection.prepareStatement(sb.toString());
      pst.setStringAtName("ORG_ID", orgId);

      rs = (OracleResultSet) pst.executeQuery();

      if (rs.next())
      {
        responsibilityId = rs.getString("responsibility_id");
        applicationId = rs.getString("application_id");
      }

      poBatchId = ValsparLookUps.queryForSingleValueLeaveConnectionOpen(connection, "SELECT VCA_PDOI_BATCH_ID_SEQ.NEXTVAL FROM dual");
      userId = ValsparLookUps.queryForSingleValueLeaveConnectionOpen(connection, "select user_id from fnd_user where upper(user_name) = ?", username);
      orgSetOfBooksId = ValsparLookUps.queryForSingleValueLeaveConnectionOpen(connection, "select set_of_books_id from FINANCIALS_SYSTEM_PARAMS_ALL where org_id = ? ", orgId);
      orgCurrencyCode = ValsparLookUps.queryForSingleValueLeaveConnectionOpen(connection, "select currency_code from gl_sets_of_books where set_of_books_id = ?", orgSetOfBooksId);
    }
    finally
    {
      JDBCUtil.close(pst, rs);
    }
  }

  public String getUserId()
  {
    return userId;
  }

  public String getPoBatchId()
  {
    return poBatchId;
  }

  public String getResponsibilityId()
  {
    return responsibilityId;
  }

  public String getApplicationId()
  {
    return applicationId;
  }

  public void setPurchaseOrders(List<PoHeadersInterfaceBean> purchaseOrders)
  {
    this.purchaseOrders = purchaseOrders;
  }

  public List<PoHeadersInterfaceBean> getPurchaseOrders()
  {
    return purchaseOrders;
  }

  public String getOrgId()
  {
    return orgId;
  }

  public String getOrgName()
  {
    return orgName;
  }

  public String getOrgCurrencyCode()
  {
    return orgCurrencyCode;
  }

  public String getOrgSetOfBooksId()
  {
    return orgSetOfBooksId;
  }

  public void setConnection(Connection connection)
  {
    this.connection = connection;
  }

  public Connection getConnection()
  {
    return connection;
  }
}
