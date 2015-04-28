<#assign datetimeformat="MM/dd/yyyy 'at' HH:mm:ss">
<html>
<head>
</head>
<body>
<table>
<TR><TD> The Lowes Account Sync Interface Started at ${lowesInputNotificationBean.startDate?time}</td></TR>
<TR><TD> The Lowes Account Sync Ended at ${lowesInputNotificationBean.endDate?time}</td><TR>
<TR><TD> The overall interface processing duration was ${lowesInputNotificationBean.getDuration()}</td><TR>
<TR><TD> Total records processed ${lowesInputNotificationBean.getRowCount()}</td><TR>
<#if lowesInputNotificationBean.getMessage()?has_content>
<TR><TD> ${lowesInputNotificationBean.getMessage()}</td><TR>
</#if>
</table>
<table>
<tr>
  <td>Records in error ${lowesInputNotificationBean.getErrorCount()}</td>
 </tr>
</table>
<#if lowesInputNotificationBean.getErrorAccountList()?has_content>
<table border=1>
<tr>Records without an email are below</tr>
<tr>
  <th>Name</th>
  <th>Store Code</th>
  <th>City</th>
  <th>State</th>
 </tr>
<#list lowesInputNotificationBean.getErrorAccountList() as loweserror> 
 <TR>
  <td>
   <#if loweserror.getName()?has_content>
   ${loweserror.getName()}
    </#if>
  </td>
  <td>
   <#if loweserror.getStore_Code__c()?has_content>
     ${loweserror.getStore_Code__c()}  
   </#if>
   </td>
    <td>
    <#if loweserror.getBillingCity()?has_content>
     ${loweserror.getBillingCity()}
   </#if>
   </td>
   <td>
     <#if loweserror.getBillingState()?has_content>
     ${loweserror.getBillingState()}
   </#if>
   </td>
  </TR> 
 </#list> 
</table>
</#if>
</body>
</html>