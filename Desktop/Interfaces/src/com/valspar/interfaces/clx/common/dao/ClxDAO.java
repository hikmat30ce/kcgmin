package com.valspar.interfaces.clx.common.dao;

import com.valspar.interfaces.clx.common.beans.*;
import com.valspar.interfaces.common.beans.ConnectionAccessBean;
import com.valspar.interfaces.common.utils.*;
import java.util.List;
import oracle.jdbc.*;
import org.apache.log4j.Logger;

public class ClxDAO
{
  private static Logger log4jLogger = Logger.getLogger(ClxDAO.class);

  public static void insertAuditXML(String senderIdKey, String rowId, String transId, String xmlMessage, String interfaceName)
  {
    log4jLogger.info("ClxDAO.insertAuditXML() - Inserting XML into audit table... ");
    OracleConnection conn = (OracleConnection) ConnectionAccessBean.findConnection(senderIdKey);
    OraclePreparedStatement pst = null;

    try
    {
      StringBuilder sb = new StringBuilder();
      sb.append("INSERT INTO VALSPAR.VCA_CLX_XML_AUDIT ");
      sb.append("( ROW_ID, ");
      sb.append("  TRANS_ID, ");
      sb.append("  XML_MESSAGE, ");
      sb.append("  INTERFACE_NAME, ");
      sb.append("  CREATION_DATE) ");
      sb.append("VALUES( ");
      sb.append(":ROW_ID, ");
      sb.append(":TRANS_ID, ");
      sb.append(":XML_MESSAGE, ");
      sb.append(":INTERFACE_NAME, ");
      sb.append("SYSDATE) ");

      pst = (OraclePreparedStatement) conn.prepareStatement(sb.toString());
      pst.setStringAtName("ROW_ID", rowId);
      pst.setStringAtName("TRANS_ID", transId);
      pst.setStringForClobAtName("XML_MESSAGE", xmlMessage);
      pst.setStringAtName("INTERFACE_NAME", interfaceName);
      pst.executeUpdate();
    }
    catch (Exception e)
    {
      log4jLogger.error(e);
      log4jLogger.error("SenderIdKey: " + senderIdKey + "RowId: " + rowId + "TransId: " + transId + "XML Message: " + xmlMessage + "InterfaceName: " + interfaceName);
    }
    finally
    {
      JDBCUtil.close(pst);
      JDBCUtil.close(conn);
    }
  }
  
  public static String updateStagingTableForList(List<? extends StagingBean> stagingBeanList)
  {
    log4jLogger.info("ClxDAO.updateStagingTableForList() - Updating staging table status... ");
    String returnMessage = null;
    StagingBean stageBean = stagingBeanList.get(0);
    OracleConnection conn = (OracleConnection) ConnectionAccessBean.findConnection(stageBean.getSenderIdKey());
    OraclePreparedStatement pst = null;
    String erpUserId = OracleAppsUtility.getERPUserId(conn, "TMS");
    try
    {
      StringBuilder sb = new StringBuilder();
      sb.append("UPDATE ");
      sb.append(stageBean.getStagingTableName());
      sb.append("   SET STATUS = :STATUS, ");
      sb.append("       LAST_UPDATED_BY = :ERP_USER_ID, ");
      sb.append("       LAST_UPDATE_DATE = SYSDATE ");
      sb.append(" WHERE ROWID = :ROW_ID ");

      pst = (OraclePreparedStatement) conn.prepareStatement(sb.toString());
      for (StagingBean stagingBean: stagingBeanList)
      {
        pst.setStringAtName("STATUS", stagingBean.getStatus());
        pst.setStringAtName("ROW_ID", stagingBean.getTransId());
        pst.setStringAtName("ERP_USER_ID", erpUserId);
        pst.addBatch();
      }
      pst.executeBatch();
    }
    catch (Exception e)
    {
      returnMessage = "ERROR - ClxDAO.updateStagingTableForList() " + e;
      log4jLogger.error(returnMessage);
    }
    finally
    {
      JDBCUtil.close(pst);
      JDBCUtil.close(conn);
    }
    return returnMessage;
  }
  
  public static String updateStagingTableForBean(StagingBean stagingBean)
  {
    log4jLogger.info("ClxDAO.updateStagingTableForBean() - Updating staging table... ");
    String returnMessage = null;
    OracleConnection conn = (OracleConnection) ConnectionAccessBean.findConnection(stagingBean.getSenderIdKey());
    OraclePreparedStatement pst = null;
    String erpUserId = OracleAppsUtility.getERPUserId(conn, "TMS");

    try
    {
      StringBuilder sb = new StringBuilder();
      sb.append("UPDATE ");
      sb.append(stagingBean.getStagingTableName());
      sb.append("   SET STATUS = :STATUS, ");
      sb.append("       LAST_UPDATED_BY = :ERP_USER_ID, ");
      sb.append("       LAST_UPDATE_DATE = SYSDATE, ");
      sb.append("       ERROR_MESSAGE = SUBSTR(:ERROR_MESSAGE, 1, 2000) ");
      sb.append(" WHERE ROWID = :ROW_ID ");

      pst = (OraclePreparedStatement) conn.prepareStatement(sb.toString());
      pst.setStringAtName("ROW_ID", stagingBean.getTransId());
      pst.setStringAtName("STATUS", stagingBean.getInterfaceStatusCode());
      pst.setStringAtName("ERP_USER_ID", erpUserId);
      pst.setStringAtName("ERROR_MESSAGE", stagingBean.getReturnMessage());
      pst.executeUpdate();
    }
    catch (Exception e)
    {
      returnMessage = "ERROR - ClxDAO.updateStagingTableForBean() " + e;
      log4jLogger.error(returnMessage);
    }
    finally
    {
      JDBCUtil.close(pst);
      JDBCUtil.close(conn);
    }
    return returnMessage;
  }
}

