<#assign datetimeformat="MM/dd/yyyy 'at' HH:mm:ss">
<html>
<head>
</head>
<body>
<table>
<TR><TD> The Product Sync Started at ${productInputNotificationBean.startDate?time}</td></TR>
<TR><TD> The Product Sync Ended at ${productInputNotificationBean.endDate?time}</td><TR>
<TR><TD> The overall interface processing duration was ${productInputNotificationBean.getDuration()}</td><TR>
<TR><TD> Total records processed ${productInputNotificationBean.getRowCount()}</td><TR>
</table>
<table>
<tr>
  <td>Records in error ${productInputNotificationBean.getErrorCount()}</td>
 </tr>
</table>
</body>
</html>