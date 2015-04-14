<html>
  <head>
    <style type="text/css">
      <#include "css/notification_styles.css">
    </style>
  </head>

  <body>
    <h2>Please find your purchase order attached</h2>

    <table style="margin-left: 10px;">
      <tr>
        <th class='rightalign'>CIP Ace Project: </th>
        <td>${po.projectId!} - ${po.projectName}</td>
      </tr>
      <tr>
        <th class='rightalign'>CIP Ace PO#: </th>
        <td>${po.cipAcePoNumber!}</td>
      </tr>
      <tr>
        <th class='rightalign'>11i Database: </th>
        <td>${po.oracleDatabaseName!}</td>
      </tr>
      <tr>
        <th class='rightalign'>11i PO#: </th>
        <td>${po.oraclePoNumber!} - (${po.orgName!})</td>
      </tr>
    </table>
  </body>
</html>
