<#assign datetimeformat="MM/dd/yyyy 'at' HH:mm:ss">
<html>
<head>
</head>
<body>
   <#if exception??>
    <#if orderStagingBean??>
     Delivery Number: ${orderStagingBean.getDeliveryNumber()!}
     <P/>
     Trans Id: ${orderStagingBean.getTransId()!}
     <P/>
    </#if>
   </#if>
   <#if orderStagingBean??>
   <#if orderStagingBean.getReturnMessage()??>
     <B>CLX Error Message:</B>
     <BR/>
     ${orderStagingBean.getReturnMessage()!}  
   </#if>
   </#if>
   <#if exception??>
     <P/>
     <B>Java Error Message:</B>
     ${exception.getMessage()!}  
   </#if>
   <#if message??>
     <P/>
     <B>Error Message:</B>
     ${message!}
   </#if>
</body>
</html>