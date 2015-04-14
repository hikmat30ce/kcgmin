package com.valspar.interfaces.regulatory.tsca.program;

import com.valspar.interfaces.common.BaseInterface;
import com.valspar.interfaces.common.beans.ConnectionAccessBean;
import com.valspar.interfaces.common.enums.DataSource;
import com.valspar.interfaces.common.utils.*;
import java.sql.*;
import oracle.jdbc.OracleConnection;
import org.apache.log4j.Logger;

public class TscaInterface extends BaseInterface
{
  private static Logger log4jLogger = Logger.getLogger(TscaInterface.class);

  OracleConnection regulatoryConn = null;
  //  Declare all variables to a space at the beginning
  String va_tsca_dsl_id = " "; // va_tsca_dsl.id
  String country_shipped_to = " "; // va_tsca_dsl.country_shipped_to
  String alias = " "; // va_tsca_dsl.alias
  String date_requested = " "; // va_tsca_dsl.date_requested
  String export_date = " "; //Export Date is date_requested +1, as per user request
  String company_name = " "; // t_plant_defaults.f_company_name
  String address = " "; // t_plant_defaults.f_address
  String city = " "; // t_plant_defaults.f_city
  String state = " "; // t_plant_defaults.f_state
  String zip = " "; // t_plant_defaults.f_zip_code
  String trade_name = " "; // t_products.f_product_name
  String component_id = " "; // t_prod_comp.f_component_id
  String cas_number = " "; // t_prod_comp.f_cas_number
  String chem_name = " "; // t_prod_comp.f_chem_name
  String chem_percent = " "; //t_prod_comp.f_percent
  String product_id = " "; //t_prod_comp.f_product
  String data_code = " "; // t_comp_data.f_data_code
  String section = " "; // t_comp_data.f_data
  String letter = " "; // t_text_details.f_text_line

  //  Indexes for search and replace algorithm
  int index = 0;
  int last_index = 0;
  int letter_count = 1;

  //  Hold area for the search and replace algorithm / Holds updated letter after each search
  StringBuffer buff = new StringBuffer(0);
  //  Search strings(EYE CATCHERS) for the letter stored in T_TEXT_DETAILS table
  final String search[] = new String[11];
  //  Define replace String array
  final String replace[] = new String[11];  

  public TscaInterface()
  {
  }

  public void execute()
  {
    try
    {
      log4jLogger.info("Application Started");
      regulatoryConn = (OracleConnection) ConnectionAccessBean.getConnection(DataSource.REGULATORY);
      log4jLogger.info("Connected to " + ConnectionUtility.buildDatabaseName(regulatoryConn));
      log4jLogger.info("Selecting TSCA Items");
      if (!processTSCA())
      {
        log4jLogger.error("Error processing TSCA Items");
      }
      updateDateRange();
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

  public boolean processTSCA()
  {
    Statement tsca_query_stmt = null;
    ResultSet tsca_query_rs = null;
    try
    {
      StringBuffer tsca_query = new StringBuffer(0);
      tsca_query.append("SELECT a.id, nvl(c.f_country_name, ' '), ");
      tsca_query.append("a.alias, to_char(a.date_requested, 'DD-MON-YYYY'), ");
      tsca_query.append("to_char(to_date(a.date_requested)+1, 'DD-MON-YYYY'), ");
      tsca_query.append("nvl(b.f_company_name, ' '), nvl(b.f_address, ' '), ");
      tsca_query.append("nvl(b.f_city, ' '), nvl(b.f_state, ' '), nvl(b.f_zip_code, ' '), ");
      tsca_query.append("nvl(d.f_product_name, ' '), ");
      tsca_query.append("e.f_component_id, e.f_cas_number, nvl(e.f_chem_name, ' '), ");
      tsca_query.append("f.f_data_code, nvl(f.f_data, ' '), ");
      tsca_query.append("e.f_percent, ");
      tsca_query.append("e.f_product ");
      tsca_query.append("FROM t_product_alias_names g, ");
      tsca_query.append("t_comp_data f, ");
      tsca_query.append("t_prod_comp e, ");
      tsca_query.append("t_products  d, ");
      tsca_query.append("t_countries c, ");
      tsca_query.append("t_plant_defaults b, ");
      tsca_query.append("va_tsca_dsl a ");
      tsca_query.append("WHERE  (f.f_data_code = 'TSCA12A' ");
      tsca_query.append("     OR f.f_data_code = 'TSCA12B' ) ");
      tsca_query.append("AND    a.alias = g.f_alias ");
      tsca_query.append("AND    f.f_component_id = e.f_component_id ");
      tsca_query.append("AND    g.f_product = e.f_product ");
      tsca_query.append("AND    g.f_product = d.f_product ");
      tsca_query.append("AND    a.country_shipped_to = c.f_country_code ");
      tsca_query.append("AND    a.plant = b.f_plant ");
      tsca_query.append("AND    a.tsca_processed = 0 ");
      tsca_query.append("AND    trunc(a.date_requested) >= ");
      tsca_query.append("(SELECT NVL(trunc(START_DATE), trunc(SYSDATE)-1) START_DATE ");
      tsca_query.append(" FROM va_reg_interfaces_run_env ");
      tsca_query.append(" where interface_id = (select interface_id ");
      tsca_query.append(" from va_reg_interfaces where interface_name = 'TscaInterface') ");
      tsca_query.append(") ");
      tsca_query_stmt = regulatoryConn.createStatement();
      tsca_query_rs = tsca_query_stmt.executeQuery(tsca_query.toString());
      while (tsca_query_rs.next())
      {
        va_tsca_dsl_id = tsca_query_rs.getString(1); // va_tsca_dsl.id
        country_shipped_to = tsca_query_rs.getString(2); // va_tsca_dsl.country_shipped_to
        alias = tsca_query_rs.getString(3); // va_tsca_dsl.alias
        date_requested = tsca_query_rs.getString(4); // va_tsca_dsl.date_requested
        export_date = tsca_query_rs.getString(5); //Export Date = Date Requested +1
        company_name = tsca_query_rs.getString(6); // t_plant_defaults.f_company_name
        address = tsca_query_rs.getString(7); // t_plant_defaults.f_address
        city = tsca_query_rs.getString(8); // t_plant_defaults.f_city
        state = tsca_query_rs.getString(9); // t_plant_defaults.f_state
        zip = tsca_query_rs.getString(10); // t_plant_defaults.f_zip_code
        trade_name = tsca_query_rs.getString(11); // t_products.f_product_name
        component_id = tsca_query_rs.getString(12); // t_prod_comp.f_component_id
        cas_number = tsca_query_rs.getString(13); // t_prod_comp.f_cas_number
        chem_name = tsca_query_rs.getString(14); // t_prod_comp.f_chem_name
        data_code = tsca_query_rs.getString(15); // t_comp_data.f_data_code
        section = tsca_query_rs.getString(16); // t_comp_data.f_data
        chem_percent = tsca_query_rs.getString(17); //t_prod_comp.f_percent
        product_id = tsca_query_rs.getString(18); //t_prod_comp.f_product
        log4jLogger.info("New Component: " + component_id + ", Data Code: " + data_code);
        letter_count = 1;

        //If it is Section 5(a) formula, check to see if a Letter has been sent
        if (data_code.equals("TSCA12A"))
        {
          PreparedStatement tsca12a_stmt = null;
          ResultSet tsca12a_rs = null;
          try
          {
            StringBuffer tsca12a_query = new StringBuffer();
            tsca12a_query.append("SELECT count(*) ");
            tsca12a_query.append("FROM va_doc_hist ");
            tsca12a_query.append("WHERE  cas = ? ");
            tsca12a_query.append("AND country_shipped_to = ? ");
            tsca12a_query.append("AND    to_char(notification_date, 'YYYY') = to_char(sysdate,'YYYY') ");

            tsca12a_stmt = regulatoryConn.prepareStatement(tsca12a_query.toString());
            tsca12a_stmt.setString(1, cas_number);
            tsca12a_stmt.setString(2, country_shipped_to);
            tsca12a_rs = tsca12a_stmt.executeQuery();
            if (tsca12a_rs.next())
              letter_count = tsca12a_rs.getInt(1);
          }
          catch (Exception e)
          {
            log4jLogger.error("Error checking if a letter has been sent for chemical/country combination for TSCA12A for current year", e);
          }
          finally
          {
            JDBCUtil.close(tsca12a_stmt, tsca12a_rs);
          }
        }
        //If it is Section 5(b) formula, check to see if a Letter has been sent
        else if (data_code.equals("TSCA12B"))
        {
          PreparedStatement tsca12b_stmt = null;
          ResultSet tsca12b_rs = null;
          try
          {
            StringBuffer tsca12b_query = new StringBuffer();
            tsca12b_query.append("SELECT count(*) ");
            tsca12b_query.append("FROM va_doc_hist ");
            tsca12b_query.append("WHERE  cas = ? ");
            tsca12b_query.append("AND country_shipped_to = ? ");

            tsca12b_stmt = regulatoryConn.prepareStatement(tsca12b_query.toString());
            tsca12b_stmt.setString(1, cas_number);
            tsca12b_stmt.setString(2, country_shipped_to);
            tsca12b_rs = tsca12b_stmt.executeQuery();
            if (tsca12b_rs.next())
              letter_count = tsca12b_rs.getInt(1);
          }
          catch (Exception e)
          {
            log4jLogger.error("Error checking if a letter has been sent for TSCA12B for chemical/country combination", e);
          }
          finally
          {
            JDBCUtil.close(tsca12b_stmt, tsca12b_rs);
          }
        }

        if (letter_count == 0)
        {
          PreparedStatement tsca1Car_pstmt = null;
          ResultSet tsca1Car_rs = null;
          try
          {
            StringBuffer carcin_query = new StringBuffer();
            carcin_query.append("select a.f_component_id ");
            carcin_query.append("from t_prod_comp a, t_comp_data b ");
            carcin_query.append("where a.f_component_id = b.f_component_id ");
            carcin_query.append("and a.f_component_id = ? ");
            carcin_query.append("and a.f_product = ? ");
            carcin_query.append("and b.f_data_code ");
            carcin_query.append(" in('IARC2B','IARC2A','IARC2B','OSHCRC','OSHPOS','NTPCARC','NTPEVID')");

            tsca1Car_pstmt = regulatoryConn.prepareStatement(carcin_query.toString());
            tsca1Car_pstmt.setString(1, component_id);
            tsca1Car_pstmt.setString(2, product_id);
            tsca1Car_rs = tsca1Car_pstmt.executeQuery();

            if (tsca1Car_rs.next())
            {
              if (Double.parseDouble(chem_percent) >= 0.1)
              {
                print_letter();
              }

            }
            else if (Double.parseDouble(chem_percent) >= 1.0)
            {
              print_letter();
            }
          }
          catch (Exception e)
          {
            log4jLogger.error("Error While Checking for Carcinogen Chemical", e);
          }
          finally
          {
            JDBCUtil.close(tsca1Car_pstmt, tsca1Car_rs);
          }
        }
        updateTSCA();
      }
    }
    catch (Exception e)
    {
      log4jLogger.error(e);
      return false;
    }
    finally
    {
      JDBCUtil.close(tsca_query_stmt, tsca_query_rs);
    }
    return true;
  }

  public void print_letter()
  {
    PreparedStatement print_letter_pstmt = null;
    ResultSet print_letter_rs = null;
    try
    {
      StringBuffer query = new StringBuffer();
      query.append("SELECT f_text_line ");
      query.append("FROM   t_text_details ");
      query.append("WHERE  f_text_key  = 'ENVALLTR01' ");
      query.append("AND    f_text_code = 'VALLTR01' ");

      print_letter_pstmt = regulatoryConn.prepareStatement(query.toString());
      print_letter_rs = print_letter_pstmt.executeQuery();
      if (print_letter_rs.next())
        letter = print_letter_rs.getString(1); 
    }
    catch (Exception e)
    {
      log4jLogger.error("Error getting the letter from " + ConnectionUtility.buildDatabaseName(regulatoryConn), e);
    }
    finally
    {
      JDBCUtil.close(print_letter_pstmt, print_letter_rs);
    }

    buff.setLength(0); 
    search[0] = "%CAS%";
    search[1] = "%CHEM NAME%";
    search[2] = "%TRADE NAME%";
    search[3] = "%PRODUCT%";
    search[4] = "%COMPANY NAME%";
    search[5] = "%ADDRESS1%";
    search[6] = "%ADDRESS2%";
    search[7] = "%COUNTRY%";
    search[8] = "%DATE%";
    search[9] = "%TSCA12A%";

    replace[0] = cas_number;
    replace[1] = chem_name;
    replace[2] = trade_name;
    replace[3] = alias;
    replace[4] = company_name;
    replace[5] = address;
    replace[6] = city + ", " + state + " " + zip;
    replace[7] = country_shipped_to;
    replace[8] = export_date;
    replace[9] = section;
   
    for (int x = 0; x < 10; x++)
    {
      index = 0;
      last_index = 0;
      buff.setLength(0); 
      while (index != -1)
      {
        // Find the search string depending on which array element currently on
        index = letter.indexOf(search[x], last_index);
        if (index < 0)
          break;
        //  Append everything read up to the current search string
        buff.append(letter.substring(last_index, index));
        //  Replace current search string with the current replace string
        buff.append(replace[x]);
        index += search[x].length();
        last_index = index;
      }
      //  Append the rest of the letter to the end
      buff.append(letter.substring(last_index, letter.length()));
      letter = "";
      letter = buff.toString();
    }
    log4jLogger.info("  Letter Sent, component: " + component_id);
    PreparedStatement insert_into_va_doc_hist_pstmt = null;
    try
    { 
      StringBuffer query_insert = new StringBuffer();
      query_insert.append("INSERT INTO VA_DOC_HIST ");
      query_insert.append("VALUES (VA_DOC_HIST_SEQ.NEXTVAL, 'tsca12bprog', SYSDATE, ");
      query_insert.append("?, ?, ?, ?, TO_DATE(?,'DD-MON-YYYY'), ?, ?,  ");
      query_insert.append("SYSDATE, SYSDATE, 'TSCA', 'TSCA', 0 )");
     
      insert_into_va_doc_hist_pstmt = regulatoryConn.prepareStatement(query_insert.toString());
      insert_into_va_doc_hist_pstmt.setString(1, cas_number);
      insert_into_va_doc_hist_pstmt.setString(2, component_id);
      insert_into_va_doc_hist_pstmt.setString(3, chem_name);
      insert_into_va_doc_hist_pstmt.setString(4, country_shipped_to);
      insert_into_va_doc_hist_pstmt.setString(5, date_requested);
      insert_into_va_doc_hist_pstmt.setString(6, "Not Used Anymore");
      insert_into_va_doc_hist_pstmt.setString(7, section);
      insert_into_va_doc_hist_pstmt.executeUpdate();
      log4jLogger.info("  Insert into VA_DOC_HIST done, component: " + component_id);
    }
    catch (Exception e)
    {
      log4jLogger.error("Error inserting into VA_DOC_HIST", e);
    }
    finally
    {
      JDBCUtil.close(insert_into_va_doc_hist_pstmt);
    }

    try
    {
      EmailBean.emailMessage("TSCA12B Export Notification Letter for Cas Number: " + cas_number + " Chem Name: " + chem_name + " Component Id: " + component_id + " Section: " + section, letter, getEmail());
    }
    catch (Exception e)
    {
      log4jLogger.error("Error sending e-mail", e);
    }
  }

  private String getEmail()
  {
    Statement stmt = null;
    ResultSet rs = null;
    String emailAddress = "";
    try
    {
      StringBuffer query = new StringBuffer();
      query.append("select notification_email ");
      query.append("from va_reg_interfaces ");
      query.append("where interface_name = 'TscaInterface'");
      stmt = regulatoryConn.createStatement();
      rs = stmt.executeQuery(query.toString());

      if (rs.next())
      {
        emailAddress = rs.getString(1);
      }
    }
    catch (Exception e)
    {
      log4jLogger.error("Error finding Email Address", e);
    }
    finally
    {
      JDBCUtil.close(stmt, rs);
    }
    return emailAddress;
  }

  private void updateTSCA()
  {
    PreparedStatement update_tsca_pstmt = null;
    try
    {
      StringBuffer query = new StringBuffer();
      query.append("UPDATE va_tsca_dsl ");
      query.append("SET tsca_processed = 1 ");
      query.append("WHERE  id = ? ");

      update_tsca_pstmt = regulatoryConn.prepareStatement(query.toString());
      update_tsca_pstmt.setString(1, va_tsca_dsl_id);
      update_tsca_pstmt.executeUpdate();
      log4jLogger.info("  setting tsca_processed = 1 for id = " + va_tsca_dsl_id);
    }
    catch (SQLException e)
    {
      log4jLogger.error(e);
    }
    finally
    {
      JDBCUtil.close(update_tsca_pstmt);
    }
  }

  public void updateDateRange()
  {
    Statement stmt = null;
    try
    {
      StringBuffer sql = new StringBuffer();
      sql.append("update va_reg_interfaces_run_env ");
      sql.append("set start_date = NVL(END_DATE, SYSDATE), ");
      sql.append("end_date = null ");
      sql.append("where interface_id = (select interface_id ");
      sql.append("from va_reg_interfaces where interface_name = 'TscaInterface') ");
      stmt = regulatoryConn.createStatement();
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