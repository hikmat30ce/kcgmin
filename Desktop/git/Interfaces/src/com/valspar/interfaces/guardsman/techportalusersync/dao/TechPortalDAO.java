package com.valspar.interfaces.guardsman.techportalusersync.dao;

import com.valspar.interfaces.common.beans.ConnectionAccessBean;
import com.valspar.interfaces.common.utils.JDBCUtil;
import com.valspar.interfaces.guardsman.techportalusersync.beans.TechUserBean;
import java.sql.*;
import java.util.*;
import java.util.Date;
import org.apache.commons.lang3.StringUtils;
import org.apache.log4j.Logger;

public final class TechPortalDAO
{
  private static Logger log4jLogger = Logger.getLogger(TechPortalDAO.class);

  private TechPortalDAO()
  {
  }

  public static List<TechUserBean> getTechPortalUsers()
  {
    Connection conn = null;
    PreparedStatement pst = null;
    ResultSet rs = null;

    List<TechUserBean> users = new ArrayList<TechUserBean>();

    try
    {
      StringBuilder sb = new StringBuilder();

      sb.append("select w.username, ifnull(w.name, w.username || ' company') full_name, ");
      sb.append("       w.password, w.pwdate ");
      sb.append("from webuser w ");
      sb.append("where w.username is not null ");
      sb.append("order by w.username ");

      conn = ConnectionAccessBean.getSasConnection();

      pst = conn.prepareStatement(sb.toString());

      rs = pst.executeQuery();
      Date now = new Date();

      while (rs.next())
      {
        TechUserBean bean = new TechUserBean();
        bean.setUserName(rs.getString("username"));
        bean.setFullName(rs.getString("full_name"));
        bean.setPassword(rs.getString("password"));
        bean.setEmail("not.provided@valspar.com");
        bean.setExpired(rs.getTimestamp("pwdate").compareTo(now) < 0);
        bean.setFirstName(StringUtils.substringBefore(bean.getFullName(), " "));
        bean.setLastName(StringUtils.substringAfter(bean.getFullName(), " "));
        
        if (StringUtils.isEmpty(bean.getLastName()))
        {
          bean.setLastName("N/A");
        }
        users.add(bean);
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

    return users;
  }
}
