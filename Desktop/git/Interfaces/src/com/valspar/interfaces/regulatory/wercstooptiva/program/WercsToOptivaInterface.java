package com.valspar.interfaces.regulatory.wercstooptiva.program;

import com.valspar.interfaces.common.*;
import com.valspar.interfaces.common.beans.ConnectionAccessBean;
import com.valspar.interfaces.common.enums.DataSource;
import com.valspar.interfaces.common.servlets.PropertiesServlet;
import com.valspar.interfaces.common.utils.*;
import com.valspar.interfaces.regulatory.wercstooptiva.beans.*;
import java.io.*;
import java.sql.*;
import java.text.DecimalFormat;
import java.util.*;
import oracle.jdbc.OracleConnection;
import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.jdom.*;
import org.jdom.output.XMLOutputter;

public class WercsToOptivaInterface extends BaseInterface
{
  OracleConnection regulatoryConn = null;
  private static Logger log4jLogger = Logger.getLogger(WercsToOptivaInterface.class);

  HashMap productList = new HashMap();
  HashMap idMap = new HashMap();
  ArrayList requiredDataCodes = new ArrayList();
  private static final int maxFileItems = 300;
  private int fileNumber = 0;

  public WercsToOptivaInterface()
  {
  }

  public void execute()
  {
    try
    {
      regulatoryConn = (OracleConnection) ConnectionAccessBean.getConnection(DataSource.REGULATORY);
      buildRequiredDataCodes();
      ArrayList formulaList = buildFormulaBeans();

      log4jLogger.info("Formula List Size is " + formulaList.size());
      if (requiredDataCodes.size() > 0)
      {
        checkRequiredDataCodes(formulaList);
      }
      if (formulaList.size() > 0)
      {
        populateFormulaIngredients(formulaList);
        ArrayList<ArrayList> formulaListList = breakupList(formulaList, maxFileItems);
        for (ArrayList subFormulaList: formulaListList)
        {
          buildFormulaXMLFile(subFormulaList);
        }
        ArrayList itemBeanList = buildItemBeans();
        ArrayList<ArrayList> itemBeansListList = breakupList(itemBeanList, maxFileItems);
        for (ArrayList subItemBeans: itemBeansListList)
        {
          buildItemXMLFile(subItemBeans);
        }
        markAsComplete();
      }
      else
      {
        log4jLogger.info("There were no valid products to pick up in VCA_OPTIVA_QUEUE today.");
      }
    }
    catch (Exception e)
    {
      log4jLogger.error(e);
    }
    finally
    {
      JDBCUtil.close(regulatoryConn);
    }
  }

  private ArrayList<ArrayList> breakupList(ArrayList ar, int maxFileItems)
  {
    ArrayList<ArrayList> lar = new ArrayList<ArrayList>();
    ArrayList arItem = null;
    int count = 0;

    Iterator i = ar.listIterator();
    while (i.hasNext())
    {
      if ((count % maxFileItems) == 0)
      {
        arItem = new ArrayList();
        lar.add(arItem);
      }
      arItem.add(i.next());
      count++;
      i.remove();
    }

    return lar;
  }

  public void buildRequiredDataCodes()
  {
    Statement st = null;
    ResultSet rs = null;
    try
    {
      StringBuffer sql = new StringBuffer();
      sql.append("SELECT WERCS_DATA_CODE,SOURCE_TABLE FROM VA_WERCS_OPTIVA_MAPPING WHERE REQUIRED = 'Y' AND ACTIVE = 'Y'");
      st = regulatoryConn.createStatement();
      rs = st.executeQuery(sql.toString());
      while (rs.next())
      {
        DataCodeBean db = new DataCodeBean();
        db.setWercsDataCode(rs.getString(1));
        db.setTable(rs.getString(2));
        requiredDataCodes.add(db);
      }
    }
    catch (Exception e)
    {
      log4jLogger.error(e);
    }
    finally
    {
      JDBCUtil.close(st, rs);
    }
  }

  public void checkRequiredDataCodes(ArrayList ar)
  {
    log4jLogger.info("Checking that all products have required data codes");

    Iterator i = ar.iterator();
    while (i.hasNext())
    {
      StringBuffer error = new StringBuffer();

      FormulaBean fb = (FormulaBean) i.next();
      HashMap hm = getDataCodeValues(requiredDataCodes, fb.getProduct());
      Set s = hm.keySet();
      Iterator it2 = s.iterator();
      while (it2.hasNext())
      {
        String key = (String) it2.next();
        DataCodeBean db = (DataCodeBean) hm.get(key);
        if (!db.isDataCodeFound())
        {
          error.append(key);
          error.append(",");
        }
      }
      if (error.length() > 0)
      {
        log4jLogger.error(fb.getProduct() + " will not be added because it is missing the following data codes " + error.toString().substring(0, error.length() - 1));
        i.remove();
      }
    }
  }

  public void buildFormulaXMLFile(ArrayList ar)
  {

    Document doc = new Document();

    Element root = new Element(Constants.FSXML);
    doc.setRootElement(root);

    Iterator it = ar.iterator();
    while (it.hasNext())
    {
      FormulaBean bean = (FormulaBean) it.next();
      if (bean.getCostClass() == null || !bean.getCostClass().equalsIgnoreCase("C") && !bean.isComponentBased())
      {

        Element formula = new Element(Constants.FORMULA);
        formula.setAttribute(Constants.MAXCOL, Constants.THIRTYSEVEN);
        formula.setAttribute(Constants.KEYCOUNT, Constants.TWO);
        formula.setAttribute(Constants.FMT, Constants.A);
        formula.setAttribute(Constants.DTLCODES, "HEADER\\INGR\\ST\\PER\\");
        formula.setAttribute(Constants.OBJECTKEY, bean.getProduct() + "\\0001");

        Element keyCode = new Element(Constants.KEYCODE);
        keyCode.setText(bean.getProduct());
        formula.addContent(keyCode);

        Element keyCode2 = new Element(Constants.KEYCODE2);
        keyCode2.setText("0001");
        formula.addContent(keyCode2);

        Element description = new Element(Constants.DESCRIPTION);
        CDATA descData = new CDATA(bean.getAliasName());
        description.addContent(descData);
        formula.addContent(description);

        Element uomCode = new Element(Constants.UOMCODE);
        uomCode.setText(Constants.LB);
        formula.addContent(uomCode);

        Element itemCode = new Element(Constants.ITEMCODE);
        itemCode.setText(bean.getProduct());
        formula.addContent(itemCode);

        Element yield = new Element(Constants.YIELD);
        yield.setText(Constants.ONEHUNDRED);
        formula.addContent(yield);

        Element yieldPct = new Element(Constants.YIELDPCT);
        yieldPct.setText(Constants.ONEHUNDRED);
        formula.addContent(yieldPct);

        Element phantomInd = new Element(Constants.PHANTOMIND);
        phantomInd.setText(Constants.ZERO);
        formula.addContent(phantomInd);

        Element primaryFormulaInd = new Element(Constants.PRIMARYFORMULAIND);
        primaryFormulaInd.setText(Constants.ONE);
        formula.addContent(primaryFormulaInd);

        Element processYield = new Element(Constants.PROCESSYIELD);
        processYield.setText(Constants.ONEHUNDRED);
        formula.addContent(processYield);

        Element statusInd = new Element(Constants.STATUSIND);
        statusInd.setText("402");
        formula.addContent(statusInd);

        Element approvalCode = new Element(Constants.APPROVALCODE);
        approvalCode.setText(Constants.APPROVED);
        formula.addContent(approvalCode);

        Element formulatorCode = new Element(Constants.FORMULATORCODE);
        formulatorCode.setText(Constants.JCS);
        formula.addContent(formulatorCode);

        Element yieldCalcInd = new Element(Constants.YIELDCALCIND);
        yieldCalcInd.setText(Constants.ONE);
        formula.addContent(yieldCalcInd);

        Element eClass = new Element(Constants.CLASS);
        eClass.setText(Constants.J);
        formula.addContent(eClass);

        Element typeInd = new Element(Constants.TYPEIND);
        typeInd.setText(Constants.ONE);
        formula.addContent(typeInd);

        Iterator it2 = bean.getIngredients().iterator();
        while (it2.hasNext())
        {
          IngredientBean ib = (IngredientBean) it2.next();

          Element ingrRow = new Element(Constants.INGRROW);
          ingrRow.setAttribute(Constants.DETAIL, Constants.ONE);
          ingrRow.setAttribute(Constants.FMT, Constants.A);

          Element lineId = new Element(Constants.LINEID);
          lineId.setText(ib.getLineId());

          Element itemCodeIng = new Element(Constants.ITEMCODE);
          itemCodeIng.setText(ib.getProduct());

          Element quantity = new Element(Constants.QUANTITY);
          quantity.setText(ib.getPercent());

          Element uomCodeIng = new Element(Constants.UOMCODE);
          uomCodeIng.setText(Constants.LB);

          Element materialPct = new Element(Constants.MATERIALPCT);
          materialPct.setText(Constants.ZERO);

          Element componentId = new Element(Constants.COMPONENTIND);
          componentId.setText(Constants.EIGHT);

          ingrRow.addContent(lineId);
          ingrRow.addContent(itemCodeIng);
          ingrRow.addContent(quantity);
          ingrRow.addContent(uomCodeIng);
          ingrRow.addContent(materialPct);
          ingrRow.addContent(componentId);
          formula.addContent(ingrRow);
        }

        Element strow = new Element(Constants.STROW);
        strow.setAttribute(Constants.DETAIL, Constants.ONE);
        strow.setAttribute(Constants.FMT, Constants.A);

        Element sClass = new Element(Constants.CLASS);
        sClass.setText(Constants.VALSPAR);

        strow.addContent(sClass);
        formula.addContent(strow);

        Element strow2 = new Element(Constants.STROW);
        strow2.setAttribute(Constants.DETAIL, Constants.ONE);
        strow2.setAttribute(Constants.FMT, Constants.A);

        Element sClass2 = new Element(Constants.CLASS);
        sClass2.setText(Constants.RAW);

        strow2.addContent(sClass2);
        formula.addContent(strow2);

        Element perrow = new Element(Constants.PERROW);
        perrow.setAttribute(Constants.DETAIL, Constants.ONE);
        perrow.setAttribute(Constants.FMT, Constants.A);

        Element objectSymbol = new Element(Constants.OBJECTSYMBOL);
        objectSymbol.setText("FORMULA");

        Element ownerSecurity = new Element(Constants.OWNERSECURITY);
        ownerSecurity.setText(Constants.SEVEN);

        Element groupSecurity = new Element(Constants.GROUPSECURITY);
        groupSecurity.setText(Constants.THREE);

        Element roleSecurity = new Element(Constants.ROLESECURITY);
        roleSecurity.setText(Constants.ONE);

        Element ownerCode = new Element(Constants.OWNERCODE);
        ownerCode.setText(Constants.CONV);

        Element groupCode = new Element(Constants.GROUPCODE);
        groupCode.setText(Constants.VALSPAR);

        perrow.addContent(objectSymbol);
        perrow.addContent(ownerSecurity);
        perrow.addContent(groupSecurity);
        perrow.addContent(roleSecurity);
        perrow.addContent(ownerCode);
        perrow.addContent(groupCode);
        formula.addContent(perrow);

        Element statusRow = new Element(Constants.STATUSROW);
        statusRow.setAttribute(Constants.DETAIL, Constants.ONE);
        statusRow.setAttribute(Constants.FMT, Constants.A);

        Element keyCode3 = new Element(Constants.KEYCODE);
        keyCode3.setText("STATUSIND");

        Element value3 = new Element(Constants.VALUE);
        value3.setText("402");

        statusRow.addContent(keyCode3);
        statusRow.addContent(value3);
        formula.addContent(statusRow);

        Element statusRow2 = new Element(Constants.STATUSROW);
        statusRow2.setAttribute(Constants.DETAIL, Constants.ONE);
        statusRow2.setAttribute(Constants.FMT, Constants.A);

        Element keyCode4 = new Element(Constants.KEYCODE);
        keyCode4.setText("APPROVALCODE");

        Element value4 = new Element(Constants.VALUE);
        value4.setText(Constants.APPROVED);

        statusRow2.addContent(keyCode4);
        statusRow2.addContent(value4);
        formula.addContent(statusRow2);

        root.addContent(formula);
      }
    }

    try
    {
      StringBuffer fileName = new StringBuffer();
      fileName.append(PropertiesServlet.getProperty("wercstoptiva.directory"));
      fileName.append("OptivaFormulas");
      fileName.append(CommonUtility.getFormattedDate("MMdyyyyhhmm"));
      fileName.append("_");
      fileName.append(fileNumber++);
      fileName.append(".xml");

      File f = new File(fileName.toString());
      FileOutputStream fos = new FileOutputStream(f);
      BufferedOutputStream bos = new BufferedOutputStream(fos);

      XMLOutputter xo = new XMLOutputter();
      xo.setNewlines(true);
      xo.setIndent(true);
      xo.output(doc, bos);
      bos.close();
      fos.close();
    }
    catch (Exception e)
    {
      log4jLogger.error(e);
    }
  }

  public void buildItemXMLFile(ArrayList ar)
  {

    Document doc = new Document();
    Element root = new Element(Constants.FSXML);
    doc.setRootElement(root);

    log4jLogger.info("Start Creating Item XML file");
    Iterator it = ar.iterator();
    while (it.hasNext())
    {
      ItemBean ib = (ItemBean) it.next();

      Element item = new Element(Constants.ITEM);
      item.setAttribute(Constants.MAXCOL, Constants.THIRTYONE);
      item.setAttribute(Constants.KEYCOUNT, Constants.ONE);
      item.setAttribute(Constants.FMT, Constants.A);
      item.setAttribute(Constants.DTLCODES, "HEADER\\TP0\\TP1\\ST\\PER\\STATUS\\");
      item.setAttribute(Constants.OBJECTKEY, ib.getProduct());

      Element keyCode = new Element(Constants.KEYCODE);
      keyCode.setText(ib.getProduct());
      item.addContent(keyCode);

      Element description = new Element(Constants.DESCRIPTION);
      CDATA descData = new CDATA(ib.getAliasName());
      description.addContent(descData);
      item.addContent(description);

      Element uomCode = new Element(Constants.UOMCODE);
      uomCode.setText(Constants.LB);
      item.addContent(uomCode);

      Element calcInd = new Element(Constants.CALCIND);
      calcInd.setText(Constants.ZERO);
      item.addContent(calcInd);

      //    Element statusInd = new Element(STATUSIND);
      //    statusInd.setText(ib.getStatusInd());
      //    item.addContent(statusInd);

      Element scaleInd = new Element(Constants.SCALEIND);
      scaleInd.setText(Constants.ZERO);
      item.addContent(scaleInd);

      Element componentInd = new Element(Constants.COMPONENTIND);
      componentInd.setText(Constants.EIGHT);
      item.addContent(componentInd);

      if (ib.getRmgDesc() != null)
      {
        Element aliasCode8 = new Element(Constants.ALIASCODE);
        aliasCode8.setText(ib.getRmgDesc());
        item.addContent(aliasCode8);
      }

      if (ib.getBusgp() != null)
      {
        Element supplier = new Element(Constants.SUPPLIER);
        supplier.setText(ib.getBusgp());
        item.addContent(supplier);
      }

      if (ib.getCas() != null)
      {
        Element cas = new Element(Constants.CAS);
        cas.setText(ib.getCas());
        item.addContent(cas);
      }

      if (ib.getChemName() != null)
      {
        Element chemName = new Element("commcode");
        //chemName.setText(ib.getChemName());
        CDATA chemDesc = new CDATA(ib.getChemName());
        chemName.addContent(chemDesc);
        item.addContent(chemName);
      }

      Set st = ib.getTp0row().keySet();
      Iterator it2 = st.iterator();
      while (it2.hasNext())
      {
        String key = (String) it2.next();
        DataCodeBean db = (DataCodeBean) ib.getTp0row().get(key);

        Element tp0row = new Element(Constants.TP0ROW);
        tp0row.setAttribute(Constants.DETAIL, Constants.ONE);
        tp0row.setAttribute(Constants.FMT, Constants.A);

        Element lineId = new Element(Constants.LINEID);
        lineId.setText(Constants.ZERO);

        Element keyCode2 = new Element(Constants.KEYCODE);
        keyCode2.setText(db.getOptivaDataCode());

        Element value = new Element(Constants.VALUE);
        if (db.getValue() != null)
        {
          value.setText(db.getValue());
        }
        else
        {
          value.setText(db.getDefaultValue());
        }
        tp0row.addContent(lineId);
        tp0row.addContent(keyCode2);
        tp0row.addContent(value);
        item.addContent(tp0row);
      }

      Set st2 = ib.getTp1row().keySet();
      Iterator it3 = st2.iterator();
      while (it3.hasNext())
      {
        String key = (String) it3.next();
        DataCodeBean db = (DataCodeBean) ib.getTp1row().get(key);

        Element tp1row = new Element(Constants.TP1ROW);
        tp1row.setAttribute(Constants.DETAIL, Constants.ONE);
        tp1row.setAttribute(Constants.FMT, Constants.A);

        Element lineId1 = new Element(Constants.LINEID);
        lineId1.setText(Constants.ZERO);

        Element keyCode1 = new Element(Constants.KEYCODE);
        keyCode1.setText(db.getOptivaDataCode());

        Element value1 = new Element(Constants.VALUE);
        if (db.getValue() != null)
        {
          value1.setText(db.getValue());
        }
        else
        {
          value1.setText(db.getDefaultValue());
        }

        tp1row.addContent(lineId1);
        tp1row.addContent(keyCode1);
        tp1row.addContent(value1);
        item.addContent(tp1row);
      }

      Element strow2 = new Element(Constants.STROW);
      strow2.setAttribute(Constants.DETAIL, Constants.ONE);
      strow2.setAttribute(Constants.FMT, Constants.A);

      Element sClass2 = new Element(Constants.CLASS);
      sClass2.setText(Constants.RAW);

      strow2.addContent(sClass2);
      item.addContent(strow2);

      Element strow = new Element(Constants.STROW);
      strow.setAttribute(Constants.DETAIL, Constants.ONE);
      strow.setAttribute(Constants.FMT, Constants.A);

      Element sClass = new Element(Constants.CLASS);
      sClass.setText(Constants.VALSPAR);

      strow.addContent(sClass);
      item.addContent(strow);

      Set st3 = ib.getStrow().keySet();
      Iterator it4 = st3.iterator();
      while (it4.hasNext())
      {
        String key = (String) it4.next();
        DataCodeBean db = (DataCodeBean) ib.getStrow().get(key);

        Element strow3 = new Element(Constants.STROW);
        strow3.setAttribute(Constants.DETAIL, Constants.ONE);
        strow3.setAttribute(Constants.FMT, Constants.A);

        Element sClass3 = new Element(Constants.CLASS);
        if (db.getValue() != null)
        {
          sClass3.setText(db.getValue());
          strow3.addContent(sClass3);
          item.addContent(strow3);
        }
      }

      Element perrow = new Element(Constants.PERROW);
      perrow.setAttribute(Constants.DETAIL, Constants.ONE);
      perrow.setAttribute(Constants.FMT, Constants.A);

      Element objectSymbol = new Element(Constants.OBJECTSYMBOL);
      objectSymbol.setText("ITEM");

      Element ownerSecurity = new Element(Constants.OWNERSECURITY);
      ownerSecurity.setText(Constants.SEVEN);

      Element groupSecurity = new Element(Constants.GROUPSECURITY);
      groupSecurity.setText(Constants.THREE);

      Element roleSecurity = new Element(Constants.ROLESECURITY);
      roleSecurity.setText(Constants.ONE);

      Element ownerCode = new Element(Constants.OWNERCODE);
      ownerCode.setText(Constants.CONV);

      Element groupCode = new Element(Constants.GROUPCODE);
      groupCode.setText(Constants.VALSPAR);

      perrow.addContent(objectSymbol);
      perrow.addContent(ownerSecurity);
      perrow.addContent(groupSecurity);
      perrow.addContent(roleSecurity);
      perrow.addContent(ownerCode);
      perrow.addContent(groupCode);
      item.addContent(perrow);

      Element enableInd = new Element(Constants.ENABLEIND);
      enableInd.setText(Constants.ONE);

      Element statusRow = new Element(Constants.STATUSROW);
      statusRow.setAttribute(Constants.DETAIL, Constants.ONE);
      statusRow.setAttribute(Constants.FMT, Constants.A);

      Element keyCode4 = new Element(Constants.KEYCODE);
      keyCode4.setText("STATUSIND");

      Element value3 = new Element(Constants.VALUE);
      value3.setText(ib.getStatusInd());

      statusRow.addContent(keyCode4);
      statusRow.addContent(value3);
      statusRow.addContent(enableInd);
      item.addContent(statusRow);

      root.addContent(item);
    }

    try
    {
      StringBuffer fileName = new StringBuffer();
      fileName.append(PropertiesServlet.getProperty("wercstoptiva.directory"));
      fileName.append("OptivaItems");
      fileName.append(CommonUtility.getFormattedDate("MMdyyyyhhmm"));
      fileName.append("_");
      fileName.append(fileNumber++);
      fileName.append(".xml");

      File f = new File(fileName.toString());
      FileOutputStream fos = new FileOutputStream(f);
      BufferedOutputStream bos = new BufferedOutputStream(fos);

      XMLOutputter xo = new XMLOutputter();
      xo.setNewlines(true);
      xo.setIndent(true);
      xo.output(doc, bos);
      bos.close();
      fos.close();
    }
    catch (Exception e)
    {
      log4jLogger.error(e);
    }
    log4jLogger.info("End of Creating Item XML file");
  }

  public HashMap getDataCodeValues(ArrayList dataCodes)
  {

    HashMap hm = new HashMap();
    StringBuffer sql = new StringBuffer();
    Statement stmt = null;
    ResultSet rs = null;
    try
    {
      DataCodeBean db = null;

      ListIterator it = dataCodes.listIterator();
      while (it.hasNext())
      {
        db = (DataCodeBean) it.next();
        if (db.getTable().equalsIgnoreCase("t_prod_data"))
        {
          sql.append("select f_data_code,f_data ");
          sql.append("from t_prod_data ");
          sql.append("where f_data_code = '");
          sql.append(db.getWercsDataCode());
          sql.append("' ");
          sql.append("and f_product = '");
          sql.append(db.getProduct());
          sql.append("' ");
        }
        if (db.getTable().equalsIgnoreCase("t_prod_text"))
        {
          sql.append("select a.f_text_code, b.f_text_line ");
          sql.append("from t_prod_text a, t_text_details b ");
          sql.append("where a.f_text_code = b.f_text_code ");
          sql.append("and   a.f_text_code = '");
          sql.append(db.getWercsDataCode());
          sql.append("' ");
          sql.append("and f_product = '");
          sql.append(db.getProduct());
          sql.append("' ");
        }
        if (db.getTable().equalsIgnoreCase("t_comp_data"))
        {
          sql.append("select f_data_code,f_data ");
          sql.append("from t_comp_data ");
          sql.append("where f_data_code = '");
          sql.append(db.getWercsDataCode());
          sql.append("' ");
          sql.append("and f_component_id = '");
          sql.append(db.getProduct());
          sql.append("'");
        }
        if (db.getTable().equalsIgnoreCase("t_comp_text"))
        {
          sql.append("select a.f_text_code, b.f_text_line ");
          sql.append("from t_comp_text a, t_text_details b ");
          sql.append("where a.f_text_code = b.f_text_code ");
          sql.append("and   a.f_text_code = '");
          sql.append(db.getWercsDataCode());
          sql.append("' ");
          sql.append("and f_component_id = '");
          sql.append(db.getProduct());
          sql.append("' ");
        }

        hm.put(db.getWercsDataCode(), db.clone());

        if (dataCodes.size() > 1 && it.nextIndex() < dataCodes.size())
        {
          sql.append(" union ");
        }
      }

      stmt = regulatoryConn.createStatement();
      rs = stmt.executeQuery(sql.toString());

      while (rs.next())
      {
        String wercsDataCode = rs.getString(1);
        DataCodeBean dcb = (DataCodeBean) hm.get(wercsDataCode);
        dcb.setDataCodeFound(true);
        if (dcb.isReturnActualValue())
        {
          dcb.setValue(rs.getString(2));
        }
        else
        {
          dcb.setValue(dcb.getDefaultPositiveValue());
        }
      }
    }
    catch (Exception e)
    {
      log4jLogger.error("SQL=" + sql.toString(), e);
    }
    finally
    {
      JDBCUtil.close(stmt, rs);
    }
    return hm;
  }

  public HashMap getDataCodeValues(ArrayList dataCodes, String product)
  {

    HashMap hm = new HashMap();
    StringBuffer sql = new StringBuffer();
    Statement stmt = null;
    ResultSet rs = null;
    try
    {
      DataCodeBean db = null;

      ListIterator it = dataCodes.listIterator();
      while (it.hasNext())
      {
        db = (DataCodeBean) it.next();
        if (db.getTable().equalsIgnoreCase("t_prod_data"))
        {
          sql.append("select f_data_code,f_data ");
          sql.append("from t_prod_data ");
          sql.append("where f_data_code = '");
          sql.append(db.getWercsDataCode());
          sql.append("' ");
          sql.append("and f_product = '");
          sql.append(product);
          sql.append("' ");
        }
        if (db.getTable().equalsIgnoreCase("t_prod_text"))
        {
          sql.append("select a.f_text_code, b.f_text_line ");
          sql.append("from t_prod_text a, t_text_details b ");
          sql.append("where a.f_text_code = b.f_text_code ");
          sql.append("and   a.f_text_code = '");
          sql.append(db.getWercsDataCode());
          sql.append("' ");
          sql.append("and f_product = '");
          sql.append(product);
          sql.append("' ");
        }
        if (db.getTable().equalsIgnoreCase("t_comp_data"))
        {
          sql.append("select f_data_code,f_data ");
          sql.append("from t_comp_data ");
          sql.append("where f_data_code = '");
          sql.append(db.getWercsDataCode());
          sql.append("' ");
          sql.append("and f_component_id = '");
          sql.append(product);
          sql.append("'");
        }
        if (db.getTable().equalsIgnoreCase("t_comp_text"))
        {
          sql.append("select a.f_text_code, b.f_text_line ");
          sql.append("from t_comp_text a, t_text_details b ");
          sql.append("where a.f_text_code = b.f_text_code ");
          sql.append("and   a.f_text_code = '");
          sql.append(db.getWercsDataCode());
          sql.append("' ");
          sql.append("and f_component_id = '");
          sql.append(product);
          sql.append("' ");
        }

        hm.put(db.getWercsDataCode(), db.clone());

        if (dataCodes.size() > 1 && it.nextIndex() < dataCodes.size())
        {
          sql.append(" union ");
        }
      }

      stmt = regulatoryConn.createStatement();
      rs = stmt.executeQuery(sql.toString());

      while (rs.next())
      {
        String wercsDataCode = rs.getString(1);
        DataCodeBean dcb = (DataCodeBean) hm.get(wercsDataCode);
        dcb.setDataCodeFound(true);
        if (dcb.isReturnActualValue())
        {
          dcb.setValue(rs.getString(2));
        }
        else
        {
          dcb.setValue(dcb.getDefaultPositiveValue());
        }
      }
    }
    catch (Exception e)
    {
      log4jLogger.error("SQL=" + sql.toString(), e);
    }
    finally
    {
      JDBCUtil.close(stmt, rs);
    }
    return hm;
  }

  public ArrayList buildFormulaBeans()
  {

    log4jLogger.info("Starting to build formula beans");

    ArrayList ar = new ArrayList();
    StringBuffer sql = new StringBuffer();
    Statement stmt = null;
    ResultSet rs = null;
    try
    {
      sql.append("SELECT B.ID, A.F_PRODUCT, A.F_ALIAS_NAME, D.F_DATA, ");
      sql.append("CASE WHEN A.F_PRODUCT LIKE 'C%-%' THEN 'YES' END COMP_BASED ");
      sql.append("FROM T_PRODUCT_ALIAS_NAMES A, VCA_OPTIVA_QUEUE B, T_PROD_DATA D ");
      sql.append("WHERE A.F_ALIAS = B.PRODUCT ");
      sql.append("AND B.PRODUCT = D.F_PRODUCT (+) ");
      sql.append("AND D.F_DATA_CODE (+) = 'CSTCL' ");
      sql.append("AND B.DATE_PROCESSED IS NULL ");
      sql.append("AND B.COMMENTS IS NULL ");

      stmt = regulatoryConn.createStatement();
      rs = stmt.executeQuery(sql.toString());

      while (rs.next())
      {
        if (isCostClass01(rs.getString(2)))
        {
          FormulaBean formula = new FormulaBean();
          formula.setId(rs.getString(1));
          formula.setProduct(rs.getString(2));
          formula.setAliasName(rs.getString(3));
          formula.setCostClass(rs.getString(4));
          if (rs.getString(5) != null && rs.getString(5).equalsIgnoreCase("YES"))
          {
            formula.setComponentBased(true);
          }
          ar.add(formula);
        }
        else
        {
          updateVcaOptivaQueue(rs.getString(1), "Updated " + rs.getString(2) + " to complete, but not adding it to the Formula List because it does not have COSTCL01 on it.");
          log4jLogger.error("Updated " + rs.getString(1) + " to complete, but not adding it to the Formula List because it does not have COSTCL01 on it.");
        }
      }
      log4jLogger.info("Done building formula beans there were " + ar.size() + " that were built.");
    }
    catch (Exception e)
    {
      log4jLogger.error(e);
    }
    finally
    {
      JDBCUtil.close(stmt, rs);
    }
    return ar;
  }

  private boolean isCostClass01(String product)
  {
    StringBuffer sql = new StringBuffer();
    boolean returnValue = false;
    Statement stmt = null;
    ResultSet rs = null;
    try
    {
      sql.append("SELECT COUNT(*) ");
      sql.append("FROM   T_PROD_TEXT ");
      sql.append("WHERE  F_PRODUCT = '");
      sql.append(product);
      sql.append("' AND F_TEXT_CODE in ('COSTCL01','COSTCL06') ");

      stmt = regulatoryConn.createStatement();
      rs = stmt.executeQuery(sql.toString());
      if (rs.next())
      {
        if (rs.getInt(1) == 0)
          returnValue = false;
        else
          returnValue = true;
      }
    }
    catch (Exception e)
    {
      log4jLogger.error(e);
    }
    finally
    {
      JDBCUtil.close(stmt, rs);
    }
    return returnValue;
  }

  public ArrayList buildItemBeans()
  {
    log4jLogger.info("Starting to build item beans");
    ArrayList ar = new ArrayList();
    Set s = productList.keySet();
    Iterator it = s.iterator();
    while (it.hasNext())
    {
      Statement stmt = null;
      ResultSet rs = null;
      try
      {
        String product = (String) it.next();

        StringBuffer sql = new StringBuffer();
        sql.append("SELECT A.F_ALIAS_NAME, B.F_CAS_NUMBER, GET_WERCS_DATA('");
        sql.append(product);
        sql.append("','BUSGP'), B.F_CHEM_NAME, C.F_DATA_CODE, GET_WERCS_DATA('");
        sql.append(product);
        sql.append("','RMGDESC')");
        sql.append("FROM T_PRODUCT_ALIAS_NAMES A , T_COMPONENTS B, T_COMP_DATA C ");
        sql.append("WHERE A.F_ALIAS = A.F_PRODUCT ");
        sql.append("AND   A.F_PRODUCT = B.F_COMPONENT_ID(+) ");
        sql.append("AND   A.F_PRODUCT = C.F_COMPONENT_ID(+) ");
        sql.append("AND C.F_DATA_CODE (+) = 'CMPTYP' "); //Added to pull up criteria for hidden CAS#
        sql.append("AND C.F_DATA(+) = 'RESIN' "); //Added to pull up criteria for hidden CAS#
        sql.append("AND A.F_PRODUCT = '");
        sql.append(product);
        sql.append("' ");

        stmt = regulatoryConn.createStatement();
        rs = stmt.executeQuery(sql.toString());

        while (rs.next())
        {
          String casNumber = rs.getString(2);
          String resinChk = rs.getString(5);
          ItemBean item = new ItemBean();
          item.setProduct(product);
          item.setAliasName(rs.getString(1));

          if (StringUtils.isEmpty(resinChk))
          {
            item.setCas(casNumber);
          }
          else
          {
            if (casNumber.equalsIgnoreCase("unknown"))
            {
              item.setCas("Proprietary Resin");
            }
            else
              item.setCas("Valspar Proprietary Resin");
          }

          item.setBusgp(rs.getString(3));
          item.setChemName(rs.getString(4));
          item.setRmgDesc(rs.getString(6));
          item.getTp0row().putAll(buildDataCodeBeans(product, Constants.TP0ROW));
          item.getTp1row().putAll(buildDataCodeBeans(product, Constants.TP1ROW));
          item.getStrow().putAll(buildDataCodeBeans(product, Constants.STROW));
          ar.add(item);
        }
      }
      catch (Exception e)
      {
        log4jLogger.error(e);
      }
      finally
      {
        JDBCUtil.close(stmt, rs);
      }
    }

    log4jLogger.info("Done building item beans there were " + ar.size() + " that were built.");

    return ar;
  }

  public HashMap buildDataCodeBeans(String product, String tagName)
  {

    ArrayList ar = new ArrayList();
    HashMap hm = new HashMap();
    Statement stmt = null;
    ResultSet rs = null;

    try
    {
      StringBuffer sql = new StringBuffer();
      sql.append("SELECT optiva_data_code, wercs_data_code, default_value, source_table, ");
      sql.append("return_actual_value, default_positive_value, ");
      sql.append("(select 'Y' from t_components where f_component_id = '");
      sql.append(product);
      sql.append("' and rownum = 1) as component ");
      sql.append("FROM VA_WERCS_OPTIVA_MAPPING WHERE TAG_NAME = '");
      sql.append(tagName);
      sql.append("' AND ACTIVE = 'Y'");

      stmt = regulatoryConn.createStatement();
      rs = stmt.executeQuery(sql.toString());

      while (rs.next())
      {
        String componentFlag = rs.getString(7);
        if (rs.getString(1).equalsIgnoreCase("DENS"))
        {
          if ((componentFlag != null && rs.getString(2).equalsIgnoreCase("CMPDENS")) || (componentFlag == null && rs.getString(2).equalsIgnoreCase("DENSITY")))
          {
            DataCodeBean db = new DataCodeBean();
            db.setProduct(product);
            db.setOptivaDataCode(rs.getString(1));
            db.setWercsDataCode(rs.getString(2));
            db.setDefaultValue(rs.getString(3));
            db.setTable(rs.getString(4));
            String actualValue = rs.getString(5);
            if (actualValue != null && actualValue.equalsIgnoreCase("Y"))
            {
              db.setReturnActualValue(true);
            }
            db.setDefaultPositiveValue(rs.getString(6));
            ar.add(db);
          }
        }
        else
        {
          DataCodeBean db = new DataCodeBean();
          db.setProduct(product);
          db.setOptivaDataCode(rs.getString(1));
          db.setWercsDataCode(rs.getString(2));
          db.setDefaultValue(rs.getString(3));
          db.setTable(rs.getString(4));
          String actualValue = rs.getString(5);
          if (actualValue != null && actualValue.equalsIgnoreCase("Y"))
          {
            db.setReturnActualValue(true);
          }
          db.setDefaultPositiveValue(rs.getString(6));
          ar.add(db);
        }
      }
      hm = getDataCodeValues(ar);
    }
    catch (Exception e)
    {
      log4jLogger.error(e);
    }
    finally
    {
      JDBCUtil.close(stmt, rs);
    }
    return hm;
  }

  public ArrayList populateFormulaIngredients(ArrayList ar)
  {
    DecimalFormat df = (DecimalFormat) DecimalFormat.getInstance();
    df.setMaximumFractionDigits(0);
    df.applyPattern("000");

    StringBuffer sql = new StringBuffer();
    sql.append("SELECT A.F_COMPONENT_ID, A.F_PERCENT ");
    sql.append("FROM T_PROD_COMP A ");
    sql.append("WHERE F_PRODUCT = ? ORDER BY F_PERCENT ");

    Iterator it = ar.iterator();
    while (it.hasNext())
    {
      PreparedStatement pst = null;
      ResultSet rs = null;
      try
      {
        FormulaBean formula = (FormulaBean) it.next();
        pst = regulatoryConn.prepareStatement(sql.toString());
        pst.setString(1, formula.getProduct());
        productList.put(formula.getProduct(), Constants.BULK);
        idMap.put(formula.getId(), null);

        rs = pst.executeQuery();

        int lineCounter = 0;
        while (rs.next())
        {
          lineCounter++;
          IngredientBean ingredient = new IngredientBean();
          ingredient.setProduct(rs.getString(1));
          productList.put(rs.getString(1), Constants.INPUT);
          ingredient.setPercent(rs.getString(2));
          ingredient.setLineId(df.format(lineCounter));
          formula.getIngredients().add(ingredient);
        }
      }
      catch (Exception e)
      {
        log4jLogger.error(e);
      }
      finally
      {
        JDBCUtil.close(pst, rs);
      }
    }

    return ar;
  }

  public void markAsComplete()
  {
    log4jLogger.info("Updating VCA_OPTIVA_QUEUE with completed bulks");
    try
    {
      Set st = idMap.keySet();
      Iterator it = st.iterator();
      while (it.hasNext())
      {
        updateVcaOptivaQueue((String) it.next(), null);
      }
      log4jLogger.info("Done updating VCA_OPTIVA_QUEUE with completed bulks.");
    }
    catch (Exception e)
    {
      log4jLogger.error(e);
    }
  }

  private void updateVcaOptivaQueue(String id, String comments)
  {
    Statement stmt = null;
    try
    {
      stmt = regulatoryConn.createStatement();
      StringBuffer sql = new StringBuffer();
      sql.append("UPDATE VCA_OPTIVA_QUEUE SET ");
      if (comments == null)
        sql.append("DATE_PROCESSED = SYSDATE");
      else
      {
        sql.append("COMMENTS = '");
        sql.append(comments);
        sql.append("'");
      }
      sql.append(" WHERE ID = ");
      sql.append(id);

      stmt.executeUpdate(sql.toString());
    }
    catch (Exception e)
    {
      log4jLogger.error(e);
    }
    finally
    {
      JDBCUtil.close(stmt);
    }
  }
}
