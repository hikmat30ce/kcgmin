<html>
  <head>
    <title>Interfaces</title>
    <style type="text/css">
      <#include "css/notification_styles.css">
    </style>
  </head>

  <body>
    <CENTER> 
      <H2>Interfaces Control Panel</H2>
      <FORM ACTION='/interfaces/StartUpServlet' METHOD='POST'>
        <H3>Control Options</H3>
        <INPUT TYPE='SUBMIT' NAME='REFRESH_SCHEDULER' VALUE='Refresh Scheduler'/> 
        <br> 
  
        <H3>Run An Interface</H3>
        Interface: <select name='JOB_KEY'>
          <#list interfaces as interface>
            <option value='${interface.jobKey}' 
              <#if interface.jobKey == jobKey!>selected</#if>
            >${interface.jobKey}</option>
          </#list>
        </select>
        <INPUT TYPE='SUBMIT' NAME='RUN_INTERFACE' VALUE='Run Interface'> 
      </FORM> 

<#if message?has_content>
      <h3>Message:</h3>
      <p>${message!}</p>
</#if>

      <table>
        <tr valign='top'>
          <td>
            <H2>Data Sources</H2>
            <table>
              <tr class='bold'>
                <td>Data Source</td>
                <td>Database</td>
                <td>Status</td>
              </tr>
      
              <#list dbStatusBeans as dbStatusBean>
                <tr>
                  <td>${dbStatusBean.dataSourceName}</td>
                  <td>${dbStatusBean.databaseName}</td>
                  <td>
                    <#if dbStatusBean.imageSource?has_content>
                      <img src='images/${dbStatusBean.imageSource}'/>
                    </#if>
                  </td>
                </tr>
              </#list>
            </table>
          </td>
          <td style='width: 50px'/>
          <td>
            <h2>Running Jobs</h2>
            <table>
              <tr class='bold'>
                <td>Job Key</td>
                <td>Start Time</td>
              </tr>
      
              <#list runningJobs as runningJob>
                <tr>
                  <td>${runningJob.jobDetail.key.name}</td>
                  <td>${runningJob.fireTime?datetime}</td>
                </tr>
              </#list>
            </table>
          </td>
        </tr>
      </table>
    </CENTER> 
  </body>
</html>
