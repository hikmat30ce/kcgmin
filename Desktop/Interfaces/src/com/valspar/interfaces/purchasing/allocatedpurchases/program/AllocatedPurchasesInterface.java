package com.valspar.interfaces.purchasing.allocatedpurchases.program;

import com.valspar.interfaces.common.BaseInterface;
import com.valspar.interfaces.common.beans.ConnectionAccessBean;
import com.valspar.interfaces.common.enums.DataSource;
import com.valspar.interfaces.common.utils.*;
import com.valspar.interfaces.purchasing.allocatedpurchases.threads.*;
import java.util.*;
import oracle.jdbc.*;
import org.apache.log4j.Logger;

public class AllocatedPurchasesInterface extends BaseInterface
{
  private static Logger log4jLogger = Logger.getLogger(AllocatedPurchasesInterface.class);

  public AllocatedPurchasesInterface()
  {
  }

  public void execute()
  {
    try
    {
      truncateTable(DataSource.RMINDEX, "ANNUAL_PURCHASES");
      truncateTable(DataSource.RMINDEX, "ANNUAL_BATCHES");
      truncateTable(DataSource.RMINDEX, "ANNUAL_SALES");

      List<BaseBuilderThread> workers = new ArrayList<BaseBuilderThread>();
      for (DataSource datasource: CommonUtility.getERPDataSourceList())
      {
        workers.add(new PurchasesBuilderThread(getInterfaceInfo(), datasource));
      }
      for (DataSource datasource: CommonUtility.getERPDataSourceList())
      {
        workers.add(new BatchesBuilderThread(getInterfaceInfo(), datasource));
      }
      for (DataSource datasource: CommonUtility.getERPDataSourceList())
      {
        workers.add(new SalesBuilderThread(getInterfaceInfo(), datasource));
      }
    /*  workers.add(new PurchasesBuilderThread(getInterfaceInfo(), DataSource.NORTHAMERICAN));
      workers.add(new PurchasesBuilderThread(getInterfaceInfo(), DataSource.EMEAI));
      workers.add(new PurchasesBuilderThread(getInterfaceInfo(), DataSource.ASIAPAC));

      workers.add(new BatchesBuilderThread(getInterfaceInfo(), DataSource.NORTHAMERICAN));
      workers.add(new BatchesBuilderThread(getInterfaceInfo(), DataSource.EMEAI));
      workers.add(new BatchesBuilderThread(getInterfaceInfo(), DataSource.ASIAPAC));

      workers.add(new SalesBuilderThread(getInterfaceInfo(), DataSource.NORTHAMERICAN));
      workers.add(new SalesBuilderThread(getInterfaceInfo(), DataSource.EMEAI));
      workers.add(new SalesBuilderThread(getInterfaceInfo(), DataSource.ASIAPAC));*/

      try
      {
        WorkManagerUtility.runParallelAndWait(workers);

        boolean success = true;

        for (BaseBuilderThread thread : workers)
        {
          if (thread.getError() != null)
          {
            success = false;
            break;
          }
        }

        if (success)
        {
          rebuildAllocatedPurchases();
        }
        else
        {
          log4jLogger.info("Errors were detected in at least one builder thread, so **NOT*** submitting job to rebuild the allocated purchases table.");          
        }
      }
      catch (Exception e)
      {
        log4jLogger.error(e);
      }
    }
    catch (Exception e)
    {
      log4jLogger.error(e);
    }
  }

  private void truncateTable(DataSource datasource, String tableName)
  {
    OracleConnection conn = null;
    OraclePreparedStatement pst = null;

    try
    {
      log4jLogger.info("Truncating table " + tableName + " ...");
      conn = (OracleConnection) ConnectionAccessBean.getConnection(datasource);
      pst = (OraclePreparedStatement) conn.prepareStatement("TRUNCATE TABLE " + tableName);
      pst.execute();
    }
    catch (Exception e)
    {
      log4jLogger.error("Table=" + tableName, e);
    }
    finally
    {
      JDBCUtil.close(pst);
      JDBCUtil.close(conn);
    }
  }

  public void rebuildAllocatedPurchases()
  {
    OracleConnection conn = null;
    OracleCallableStatement cst = null;

    try
    {
      conn = (OracleConnection) ConnectionAccessBean.getConnection(DataSource.RMINDEX);
      cst = (OracleCallableStatement) conn.prepareCall("{call ANNUAL_PROCESS_PKG.REBUILD_ALLOCATED_PURCHASES}");

      log4jLogger.info("Submitting Job to Rebuild Allocated Purchases table...");
      cst.execute();
      log4jLogger.info("Job submitted to rebuild allocated purchases table");
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
  }
}
