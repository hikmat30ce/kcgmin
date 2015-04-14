<#assign datetimeformat="MM/dd/yyyy 'at' HH:mm:ss">

<html>
<head>
    <style type="text/css">
      <#include "css/notification_styles.css">
    </style>
</head>
<body>
<#list fileArray as file>
${erpDirectory}${file.name}<br>
</#list>

</body>
</html>