package com.valspar.interfaces.wercs.quickfds.program;

import com.valspar.interfaces.common.BaseInterface;
import com.valspar.interfaces.common.beans.ConnectionAccessBean;
import com.valspar.interfaces.common.enums.DataSource;
import com.valspar.interfaces.common.utils.JDBCUtil;
import oracle.jdbc.*;
import org.apache.log4j.Logger;

public class QuickFdsInterface extends BaseInterface
{
  Logger log4jLogger = Logger.getLogger(QuickFdsInterface.class);

  public void execute()
  {
    OracleConnection conn = null;
    OraclePreparedStatement pst = null;

    try
    {
      conn = (OracleConnection) ConnectionAccessBean.getConnection(DataSource.WERCS);

      StringBuilder sb = new StringBuilder();
      sb.append("INSERT INTO VCA_MSDS_REQUEST_QUEUE (");
      sb.append("     request_id, ");
      sb.append("     request_type, ");
      sb.append("     extract_from, ");
      sb.append("     email, ");
      sb.append("     delivery_method, ");
      sb.append("     status, ");
      sb.append("     added_by, ");
      sb.append("     date_added, ");
      sb.append("     publish_date) ");
      sb.append("VALUES (");
      sb.append("     VCA_MSDS_REQUEST_QUEUE_SEQ.NEXTVAL, ");
      sb.append("     'QuickFDS', ");
      sb.append("     'Valapps', ");
      sb.append("     'isreg@valspar.com', ");
      sb.append("     'FTP', ");
      sb.append("     '0', ");
      sb.append("     'QUARTZ', ");
      sb.append("     SYSDATE, ");
      sb.append("     TRUNC (SYSDATE - 1)) ");

      pst = (OraclePreparedStatement)conn.prepareStatement(sb.toString());
      pst.executeUpdate();
    }
    catch (Exception e)
    {
      log4jLogger.error(e);
    }
    finally
    {
      JDBCUtil.close(pst);
      JDBCUtil.close(conn);
    }
  }

}
