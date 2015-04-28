<html>
<head>
    <style type="text/css">
      <#include "css/notification_styles.css">
    </style>
</head>
<body>
<table>
<tr>
  <th>Batch ID</th>
  <th>Lines Processed</th>
  <th>Lines Succeeded</th>
  <th>Lines Erred</th>
  <th>Start Date</th>
  <th>End Date</th>
  <th>Duration</th>
</tr>
<tr>
  <td>${payrollInputNotificationBean.batchId}</td>
  <td>${payrollInputNotificationBean.rowCount}</td>
  <td>${payrollInputNotificationBean.rowCount-payrollInputNotificationBean.errorCount}</td>
  <td>${payrollInputNotificationBean.errorCount}</td>
  <td>${payrollInputNotificationBean.startDate?time}</td>
  <td>${payrollInputNotificationBean.endDate?time}</td>
  <td>${payrollInputNotificationBean.duration}</td>
</tr>
</table>
<br>

<#if payrollInputErrorBeanList?has_content>
<table>
<tr>
  <th>Employee ID</th>
  <th>Earning Code</th>
  <th>Hours</th>
  <th>Shift</th>
  <th>Dollars</th>
  <th>Error Message</th>
</tr>
<#list payrollInputErrorBeanList as payrollInputBean> 
<#if !payrollInputBean.validFormat>
<tr class="invalidformat">
<#else>
<tr class="apierror">
</#if>
  <td>
   <#if payrollInputBean.employeeId?has_content>
    ${payrollInputBean.employeeId}
   </#if>
  </td>
  <td>
  <#if payrollInputBean.earningCode?has_content>
  ${payrollInputBean.earningCode}
   </#if></td>
  <td>
  <#if payrollInputBean.hours?has_content>
  ${payrollInputBean.hours}
   </#if></td>
  <td>
  <#if payrollInputBean.shift?has_content>
  ${payrollInputBean.shift}
   </#if></td>
  <td>
  <#if payrollInputBean.dollars?has_content>
  ${payrollInputBean.dollars}
   </#if></td>
  <td>
  <#if payrollInputBean.errorMessage?has_content>
  ${payrollInputBean.errorMessage}
  </#if></td>
</tr>
</#list> 
</table>
</#if>

</body>
</html>


