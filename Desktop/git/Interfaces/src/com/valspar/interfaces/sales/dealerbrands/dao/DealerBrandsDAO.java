package com.valspar.interfaces.sales.dealerbrands.dao;

import com.sforce.soap.enterprise.sobject.Account;
import com.sforce.soap.enterprise.sobject.RecordType;
import com.sforce.soap.enterprise.sobject.User;
import com.valspar.interfaces.sales.common.SFDCCommonDAO;
import com.valspar.interfaces.common.beans.ConnectionAccessBean;
import com.valspar.interfaces.common.enums.DataSource;
import com.valspar.interfaces.common.utils.CommonUtility;
import com.valspar.interfaces.common.utils.JDBCUtil;
import com.valspar.interfaces.sales.dealerbrands.beans.DealerBrandsInputNotificationBean;
import java.util.ArrayList;
import java.util.List;
import oracle.jdbc.OracleConnection;
import oracle.jdbc.OraclePreparedStatement;
import oracle.jdbc.OracleResultSet;
import org.apache.log4j.Logger;

public class DealerBrandsDAO
{
  private static Logger log4jLogger = Logger.getLogger(DealerBrandsDAO.class);

  public DealerBrandsDAO()
  {
  }

  public static void dealerBrandsAccounts(DealerBrandsInputNotificationBean dealerBrandsInputNotificationBean)
  {
    OracleConnection connDealerBrands = (OracleConnection) ConnectionAccessBean.getConnection(DataSource.DEALERBRANDS);
    OraclePreparedStatement pst = null;
    OracleResultSet rs = null;
    List<Account> accountList = new ArrayList<Account>();

    try
    {
      StringBuilder sb = new StringBuilder();
      sb.append(" select distinct nvl(dcusnm,' ') as dcusnm,trim(a.deal#)as deal#,nvl(dmadd1,' ')as dmadd1,nvl(dcity,' ') as dcity,nvl(dstcde,' ') as dstcde,");
      sb.append(" nvl(dcntry,' ') as dcntry,nvl(dpostl,' ')as dpostl, nvl(dspstl,' ') as dspstl,nvl(dsadd1,' ') as dsadd1,nvl(dscity,' ')as dscity,");
      sb.append(" nvl(dsstcd,' ') as dsstcd,nvl(dscntr,' ') as dscntr,nvl(dphone,' ') as dphone,nvl(dfax,' ') as dfax,");
      sb.append(" nvl(dweb,' ') as dweb,nvl(b.email,' ') as email from dealer.dlrlistnew a  ");
      sb.append(" LEFT OUTER join dealer.repemail b ON trim(a.dslsnr)=trim(b.repnbr) WHERE  nvl(ddelcd,' ')<>'D'   ");
      sb.append(" and upper(dcusnm) not like '%ACE %' and upper(dcusnm) not like '%LOWE''S%' ");
      sb.append(" and a.deal# not in  (select deal# from dealer.dlrcoop where trim(dlrcoop.deal#)=trim(a.deal#) and (trim(dlrcoop.dcoop)='A1' or trim(dlrcoop.dcoop)='L1')) ");
      sb.append(" and dkyact in('R','M') group by dcusnm, a.deal#, dmadd1, dcity, dstcde, ");
      sb.append(" dcntry,dpostl,dspstl,dsadd1,dscity,dsstcd,dscntr,dphone,dfax,dweb,email ");
      pst = (OraclePreparedStatement) connDealerBrands.prepareStatement(sb.toString());
      rs = (OracleResultSet) pst.executeQuery();
      RecordType type = new RecordType();
      type.setName("Manual Account");
      int errorcount=0;
      
      while (rs.next())
      {
        Account account = new Account();
        account.setStore_Code__c(rs.getString("deal#"));
        account.setName(rs.getString("dcusnm"));
        account.setStore_Code__c(rs.getString("deal#"));
        account.setLegacy_Account_ID_Number__c(rs.getString("deal#"));
        account.setRecordTypeId("012E00000000Gz6IAE");
        account.setBusiness_Unit__c("Consumer DB");
        account.setAccount_Currency__c("USD");
        account.setBillingStreet(rs.getString("dmadd1"));
        account.setBillingCity(rs.getString("dcity"));
        account.setBillingState(rs.getString("dstcde"));
        account.setBillingPostalCode(rs.getString("dpostl"));
        account.setBillingCountry(rs.getString("dcntry"));
        account.setShippingStreet(rs.getString("dsadd1"));
        account.setShippingCity(rs.getString("dscity"));
        account.setShippingState(rs.getString("dsstcd"));
        account.setShippingPostalCode(rs.getString("dspstl"));
        account.setShippingCountry(rs.getString("dscntr"));
        String phone = rs.getString("dphone");
        String fax = rs.getString("dfax");
        String shippingcountry = rs.getString("dscntr");
        if (phone != null && shippingcountry != null)
        {
          phone = CommonUtility.convertPhoneNumberToCountryFormat(shippingcountry, phone);
        }
        account.setPhone(phone);
        if (fax != null && shippingcountry != null)
        {
          fax = CommonUtility.convertPhoneNumberToCountryFormat(shippingcountry, fax);
        }
        account.setFax(fax);
        account.setWebsite(rs.getString("dweb"));
        User user = new User();
        if (rs.getString("email").equals(" "))
        {
         dealerBrandsInputNotificationBean.getErrorAccountList().add(account);
         errorcount++;
        }
        else
        {
          user.setUsername(rs.getString("email"));
          account.setOwner(user);
        }
      
        accountList.add(account);
      }
      dealerBrandsInputNotificationBean.setRowCount(accountList.size());
      dealerBrandsInputNotificationBean.setErrorCount(errorcount);
      SFDCCommonDAO.upsertRecordsByID(accountList.toArray(new Account[accountList.size()]), "Legacy_Account_ID_Number__c");
    }
    catch (Exception e)
    {
      log4jLogger.error(e);
    }
    finally
    {
      JDBCUtil.close(pst, rs);
      JDBCUtil.close(connDealerBrands);
    }
  }
}
