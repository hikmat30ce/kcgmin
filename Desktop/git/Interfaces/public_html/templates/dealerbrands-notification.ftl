<#assign datetimeformat="MM/dd/yyyy 'at' HH:mm:ss">
<html>
<head>
</head>
<body>
<table>
<tr>
  <td>The DealerBrands Account Sync Interface Started at ${dealerBrandsInputNotificationBean.startDate?time}</td>
 </tr>
<tr>
  <td>The DealerBrands Account Sync Ended at ${dealerBrandsInputNotificationBean.endDate?time}</td>
 </tr>
<tr>
  <td>The overall interface processing duration was ${dealerBrandsInputNotificationBean.getDuration()}</td>
 </tr>
<tr>
  <td>The total accounts processed were ${dealerBrandsInputNotificationBean.getRowCount()}</td>
 </tr>
<tr>
  <td>The total accounts in error were ${dealerBrandsInputNotificationBean.getErrorCount()}</td>
</tr>
</table>
<#if dealerBrandsInputNotificationBean.getErrorAccountList()?has_content>
<table border=1>
<tr>Records without a valid rep email are below</tr>
<tr>
  <th>Name</th>
  <th>Store Code</th>
  <th>City</th>
  <th>State</th>
 </tr>
<#list dealerBrandsInputNotificationBean.getErrorAccountList() as dealerBrandserror> 
 <TR>
  <td>
   <#if dealerBrandserror.getName()?has_content>
   ${dealerBrandserror.getName()}
    </#if>
  </td>
  <td>
   <#if dealerBrandserror.getStore_Code__c()?has_content>
     ${dealerBrandserror.getStore_Code__c()}  
   </#if>
   </td>
    <td>
    <#if dealerBrandserror.getBillingCity()?has_content>
     ${dealerBrandserror.getBillingCity()}
   </#if>
   </td>
   <td>
     <#if dealerBrandserror.getBillingState()?has_content>
     ${dealerBrandserror.getBillingState()}
   </#if>
   </td>
  </TR> 
 </#list> 
</table>
</#if>
</body>
</html>