<#assign datetimeformat="MM/dd/yyyy 'at' HH:mm:ss">
<html>
<head>
</head>
<body>
   <#if exception??>
    <#if shipmentStatusStagingBean??>
     Delivery/Transfer Batch: ${shipmentStatusStagingBean.getDeliveryOrTransferBatch()!}
     <P/>
     Trans Id: ${shipmentStatusStagingBean.getTransId()!}
     <P/>
    </#if>
   </#if>
   <#if shipmentStatusStagingBean??>
   <#if shipmentStatusStagingBean.getReturnMessage()??>
     <B>CLX Error Message:</B>
     <BR/>
     ${shipmentStatusStagingBean.getReturnMessage()!}  
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