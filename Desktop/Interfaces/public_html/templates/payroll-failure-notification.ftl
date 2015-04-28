<html>
<head>
    <style type="text/css">
      <#include "css/notification_styles.css">
    </style>
</head>
<body>
<table>
<tr><td>File Name:</td><td>${payrollInputNotificationBean.fileName}</td></tr>
<tr><td>Start Date:</td><td>${payrollInputNotificationBean.startDate?time}</td></tr>
</table>
<br>

${exception}


</body>
</html>


