<html>
  <head>
    <style type="text/css">
      <#include "css/notification_styles.css">
    </style>
  </head>

  <body>
<p>The following plans could not be generated for emailing:</p>

<#list errors?keys as plan>
<#assign result = errors?values[plan_index]>

<table>
<tr>
  <td>Plan Name:</td>
  <td><b>${plan.planName!}</b></td>
</tr>
<tr>
  <td>Language Code:</td>
  <td><b>${plan.languageCode!}</b></td>
</tr>
<tr>
  <td>Error Message:</td>
  <td><b>${result.errorMessage!}</b></td>
</tr>
<tr>
  <td>ERP Retailer #:</td>
  <td><b>${plan.erpRetailerNo!}</b></td>
</tr>
<tr>
  <td>CON-SA-ID:</td>
  <td><b>${plan.conSaId!}</b></td>
</tr>
<tr>
  <td>Consumer Name:</td>
  <td><b>${(plan.consumer.fullName)!}</b></td>
</tr>
<tr>
  <td>Consumer Address:</td>
  <td><b>${plan.combinedStreetAddress!}</b></td>
</tr>
<tr>
  <td>Consumer City, State, Zip:</td>
  <td><b>${plan.city!} ${plan.state!} ${plan.postalCode!}</b></td>
</tr>
</table>
<hr/>
</#list>
  </body>
</html>
