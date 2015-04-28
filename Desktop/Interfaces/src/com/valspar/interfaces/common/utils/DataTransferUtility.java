package com.valspar.interfaces.common.utils;

import com.valspar.interfaces.common.enums.DataSource;
import com.valspar.interfaces.common.hibernate.HibernateUtil;
import java.sql.Statement;
import java.util.Iterator;
import org.hibernate.*;

public final class DataTransferUtility
{
  private DataTransferUtility()
  {
  }

  public static void streamTransfer(SQLQuery query, DataSource targetDataSource, Class rowBeanClass)
  {
    Session targetSession = null;

    try
    {
      int i=0;
      System.out.println("Opening Target Hibernate Session");
      targetSession = HibernateUtil.getHibernateSessionAndBeginTransaction(targetDataSource);

      query.addEntity(rowBeanClass);

      ScrollableResults scrollableResults = query.setCacheMode(CacheMode.IGNORE).scroll(ScrollMode.FORWARD_ONLY);

      while (scrollableResults.next())
      {
        Object row = scrollableResults.get(0);
        targetSession.save(row);

        if (++i % 20 == 0)
        {
          targetSession.flush();
          targetSession.clear();
        }
      }
    }
    finally
    {
      HibernateUtil.closeHibernateSessionAndCommitTransaction(targetSession);
    }
    
    System.out.println("Closed Target Hibernate Session");
  }
}
