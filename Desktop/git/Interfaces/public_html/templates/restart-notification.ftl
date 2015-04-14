<#assign datetimeformat="MM/dd/yyyy 'at' HH:mm:ss">

<html>
<head>
    <style type="text/css">
      <#include "css/notification_styles.css">
    </style>
</head>
<body>
${webserver} Has Been Restarted<br>
<span class="bold">Time of Restart was ${restartTime?string(datetimeformat)} </span>
<br><br>
<table>
<tr>
  <th>System Key</th>
  <th>System Value</th>
</tr>

<#list properties?keys as key> 
<tr>
  <td>${key}</td>
  <td>${properties[key]}</td>
</tr>
</#list> 

</body>
</html>