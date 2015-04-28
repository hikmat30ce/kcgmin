package com.valspar.interfaces.common.test;

import com.valspar.interfaces.common.BaseInterface;
import com.valspar.interfaces.common.enums.DataSource;
import com.valspar.interfaces.common.hibernate.HibernateUtil;
import com.valspar.interfaces.common.utils.DataTransferUtility;
import org.apache.log4j.Logger;
import org.hibernate.*;

public class TestInterface extends BaseInterface
{
  private static Logger log4jLogger = Logger.getLogger(TestInterface.class);

  public void execute()
  {
    Session sourceSession = null;

    try
    {
      sourceSession = HibernateUtil.getHibernateSession(DataSource.NORTHAMERICAN);

      StringBuilder sb = new StringBuilder();
      sb.append("select vendor_id, ");
      sb.append("       vendor_name, ");
      sb.append("       segment1 vendor_code, ");
      sb.append("       vendor_type_lookup_code ");
      sb.append("from po_vendors v ");
      sb.append("where vendor_name like :VENDOR_NAME_FILTER ");

      SQLQuery query = sourceSession.createSQLQuery(sb.toString());
      query.setParameter("VENDOR_NAME_FILTER", "%");

      DataTransferUtility.streamTransfer(query, DataSource.MIDDLEWARE, VendorBean.class);
    }
    catch (Exception e)
    {
      log4jLogger.error(e);
    }
    finally
    {
      HibernateUtil.closeHibernateSession(sourceSession);
    }
  }
}
