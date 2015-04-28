package com.valspar.interfaces.sales.common;

import com.sforce.soap.enterprise.*;
import com.sforce.soap.enterprise.Error;
import com.sforce.soap.enterprise.sobject.*;
import com.sforce.ws.bind.XMLizable;
import com.valspar.interfaces.common.beans.ConnectionAccessBean;
import java.lang.reflect.*;
import org.apache.commons.lang3.*;
import org.apache.log4j.Logger;

public class SFDCCommonDAO
{
  private static Logger log4jLogger = Logger.getLogger(SFDCCommonDAO.class);

  private SFDCCommonDAO()
  {
  }

  public static String checkForErrors(XMLizable result)
  {
    StringBuilder sb = new StringBuilder();

    if (result instanceof SaveResult)
    {
      if (!((SaveResult) result).isSuccess())
      {
        for (Error errorMessage: ((SaveResult) result).getErrors())
        {
          sb.append(errorMessage.getMessage());
          log4jLogger.error(errorMessage.getMessage());
        }
      }
    }
    else if (result instanceof UpsertResult)
    {
      if (!((UpsertResult) result).isSuccess())
      {
        for (Error errorMessage: ((UpsertResult) result).getErrors())
        {
          sb.append(errorMessage.getMessage());
          log4jLogger.error(errorMessage.getMessage());
        }
      }
    }
    return sb.toString();

  }

  public static String createRecords(SObject[] records)
  {
    StringBuilder sb = new StringBuilder();
    int startingPosition = 0;
    int endingPosition = 200;
    do
    {
      SObject[] subarray = ArrayUtils.subarray(records, startingPosition, endingPosition);
      sb.append(batchCreateRecords(subarray));
      startingPosition = endingPosition + 1;
      endingPosition += 200;
    }
    while (startingPosition <= records.length);

    return sb.toString();
  }

  public static String batchCreateRecords(SObject[] records)
  {
    EnterpriseConnection conn = ConnectionAccessBean.getSFDCConnection();
    SaveResult saveResult = null;

    try
    {
      SaveResult[] saveResults = conn.create(records);

      if (saveResults.length > 0)
      {
        saveResult = saveResults[0];
      }
    }
    catch (Exception e)
    {
      log4jLogger.error(e);
    }
    return SFDCCommonDAO.checkForErrors(saveResult);
  }

  public static String updateRecords(EnterpriseConnection conn, SObject[] records)
  {
    StringBuilder sb = new StringBuilder();
    int startingPosition = 0;
    int endingPosition = 200;
    do
    {
      SObject[] subarray = ArrayUtils.subarray(records, startingPosition, endingPosition);
      sb.append(batchUpdateRecords(conn,subarray));
      startingPosition = endingPosition + 1;
      endingPosition += 200;
    }
    while (startingPosition <= records.length);

    return sb.toString();
  }
  
  private static String batchUpdateRecords(EnterpriseConnection conn, SObject[] records)
  {
    SaveResult result = null;
    try
    {
      SaveResult[] saveResults = conn.update(records);

      if (saveResults.length > 0)
      {
        result = saveResults[0];
      }
    }
    catch (Exception e)
    {
      log4jLogger.error(e);
    }

    return SFDCCommonDAO.checkForErrors(result);
  }

  public static String upsertRecordsByID(SObject[] records, String id)
  {
    StringBuilder sb = new StringBuilder();
    int startingPosition = 0;
    int endingPosition = 200;
    do
    {
      SObject[] subarray = ArrayUtils.subarray(records, startingPosition, endingPosition);
      sb.append(batchUpsertRecordsByID(subarray, id));
      startingPosition = endingPosition + 1;
      endingPosition += 200;
    }
    while (startingPosition <= records.length+200);

    return sb.toString();
  }

  private static String batchUpsertRecordsByID(SObject[] records, String id)
  {
    EnterpriseConnection conn = ConnectionAccessBean.getSFDCConnection();
    UpsertResult upsertResult = null;
    try
    {

      UpsertResult[] upsertResults = conn.upsert(id, records);

      if (upsertResults.length > 0)
      {
        upsertResult = upsertResults[0];
      }
    }
    catch (Exception e)
    {
      log4jLogger.error(e);
    }
    return SFDCCommonDAO.checkForErrors(upsertResult);
  }

  public static Object[] executeQuery(String query, Class c)
  {
    EnterpriseConnection conn = ConnectionAccessBean.getSFDCConnection();
    Object[] objArray = null;

    QueryResult queryResult = null;
    try
    {
      queryResult = conn.query(query);
      objArray = (Object[]) Array.newInstance(c, queryResult.getRecords().length);

      for (int i = 0; i < queryResult.getRecords().length; i++)
      {
        objArray[i] = queryResult.getRecords()[i];
      }
    }
    catch (Exception e)
    {
      log4jLogger.error(e);
    }
    return objArray;
  }
}
