package com.valspar.interfaces.sales.lowes.dao;

import com.sforce.soap.enterprise.sobject.*;
import com.valspar.interfaces.sales.common.SFDCCommonDAO;
import com.valspar.interfaces.sales.lowes.beans.LowesInputNotificationBean;
import com.valspar.interfaces.common.beans.ConnectionAccessBean;
import com.valspar.interfaces.common.enums.DataSource;
import com.valspar.interfaces.common.utils.JDBCUtil;
import java.util.*;
import oracle.jdbc.*;
import org.apache.log4j.Logger;

public class LowesDAO
{
  private static Logger log4jLogger = Logger.getLogger(LowesDAO.class);

  public LowesDAO()
  {
  }

  private static Map<String, String> buildLowesAccountMap()
  {
    Map<String, String> lowesMap = new HashMap<String, String>();
    Account[] accountArray = (Account[]) SFDCCommonDAO.executeQuery("SELECT ID,Legacy_Account_ID_Number__c FROM Account where Business_Unit__c='Consumer Lowes' ", Account.class);
    for (int i = 0; i < accountArray.length; i++)
    {
      lowesMap.put(accountArray[i].getLegacy_Account_ID_Number__c(), accountArray[i].getId());
    }
    return lowesMap;
  }

  public static void loadAccountsAndContacts(LowesInputNotificationBean lowesInputNotificationBean)
  {
    Map<String, String> lowesAccountIDs = buildLowesAccountMap();
    OracleConnection lowesDealersConn = (OracleConnection) ConnectionAccessBean.getConnection(DataSource.LOWESBIDS);
    OraclePreparedStatement pst = null;
    OracleResultSet rs = null;
    List<Account> accountList = new ArrayList<Account>();
    List<Contact> contactList = new ArrayList<Contact>();

    try
    {
      StringBuilder sb = new StringBuilder();
      sb.append(" select s.open_date,s.store_name,trim(s.store_id)as store_id,s.store_nbr,s.store_name,s.store_mgr,s.address,s.city,s.state,s.zip,s.country_code,s.store_phone,");
      sb.append(" s.manu_lname,s.store_fax,substr(store_mgr, instr(store_mgr,',', 1)+1) as store_mgr_first_name, ");
      sb.append(" nvl(substr(store_mgr, 1 ,instr(store_mgr, ',', 1, 1)-1),'-') as store_mgr_last_name,f.region, ");
      sb.append(" e.last_name||', '||e.first_name as terr_mgr,e.work_email as tm_email ");
      sb.append(" from lowes_dw.store_info s, lowes_dw.field_info f, lowes_dw.employee_info e ");
      sb.append(" where s.field_info_id = f.field_info_id ");
      sb.append(" and s.terr_mgr_id = e.emp_id   ");
      sb.append(" and s.store_name is not null ");
    
      pst = (OraclePreparedStatement) lowesDealersConn.prepareStatement(sb.toString());
      rs = (OracleResultSet) pst.executeQuery();
      int errorcount = 0;
      while (rs.next())
      {
        Account account = new Account();
        account.setName(rs.getString("store_name"));
        account.setLegacy_Account_ID_Number__c(rs.getString("store_id"));
        Calendar calStart = Calendar.getInstance();
        if (rs.getDate("open_date") != null)
        {
          calStart.setTime(rs.getDate("open_date"));
          account.setOpen_Date__c(calStart);
        }
        account.setStore_Code__c(rs.getString("store_nbr"));
        account.setBusiness_Unit__c("Consumer Lowes");
        account.setName(rs.getString("store_name"));
        account.setBillingStreet(rs.getString("address"));
        account.setBillingCity(rs.getString("city"));
        account.setBillingState(rs.getString("State"));
        account.setBillingPostalCode(rs.getString("zip"));
        account.setShippingCountry(rs.getString("country_code"));
        account.setPhone(rs.getString("store_phone"));
        account.setGeo_Region__c(rs.getString("region"));
        account.setShip_From_Whse__c(rs.getString("manu_lname"));
        account.setTerritory_Manager_Name__c(rs.getString("terr_mgr"));
        account.setAccount_Type__c("Lowes Store");
        account.setFax(rs.getString("store_fax"));
        User user = new User();
        if (rs.getString("tm_email") == null)
        {
          lowesInputNotificationBean.getErrorAccountList().add(account);
          errorcount++;
        }
        else
        {
          user.setUsername(rs.getString("tm_email"));
        }
        account.setOwner(user);
        accountList.add(account);
        Contact contact = new Contact();
        contact.setOwner(user);
        contact.setLegacy_Contact_ID_Number__c(account.getLegacy_Account_ID_Number__c());
        contact.setFirstName(rs.getString("store_mgr_first_name"));
        contact.setLastName(rs.getString("store_mgr_last_name"));
        contact.setAccountId(lowesAccountIDs.get(account.getLegacy_Account_ID_Number__c()));
        contact.setContact_Type__c("Store Mgr");
        contact.setMailingStreet(rs.getString("address"));
        contact.setMailingCity(rs.getString("city"));
        contact.setMailingState(rs.getString("state"));
        contact.setMailingCountry(rs.getString("country_code"));
        contact.setMailingPostalCode(rs.getString("zip"));
        contact.setBusiness_Unit__c("Consumer Lowes");
        contact.setContact_Type__c("employee");
        contact.setPhone(rs.getString("store_phone"));
        contact.setFax(rs.getString("store_fax"));
        contactList.add(contact);
      }
      lowesInputNotificationBean.setRowCount(accountList.size() + contactList.size());
      lowesInputNotificationBean.setErrorCount(errorcount);
      SFDCCommonDAO.upsertRecordsByID(accountList.toArray(new Account[accountList.size()]), "Legacy_Account_ID_Number__c");
      SFDCCommonDAO.upsertRecordsByID(contactList.toArray(new Contact[contactList.size()]), "Legacy_Contact_ID_Number__c");
    }
    catch (Exception e)
    {
      lowesInputNotificationBean.getMessage().append("There was an error: \n" +  e.toString());
      log4jLogger.error(e);
    }
    finally
    {
      JDBCUtil.close(pst, rs);
      JDBCUtil.close(lowesDealersConn);
    }
  }
}
