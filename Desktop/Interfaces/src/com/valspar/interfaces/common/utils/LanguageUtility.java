package com.valspar.interfaces.common.utils;

import com.valspar.interfaces.common.beans.ConnectionAccessBean;
import com.valspar.interfaces.common.enums.DataSource;
import com.valspar.interfaces.wercs.common.beans.WercsLanguageBean;
import java.sql.Statement;
import java.util.*;
import oracle.jdbc.*;
import org.apache.log4j.Logger;

public class LanguageUtility
{
  private static Logger log4jLogger = Logger.getLogger(LanguageUtility.class);
  private static Map<String, WercsLanguageBean> languages;

  static
  {
    populateLanguages();
  }

  public static void populateLanguages()
  {
    // http://www.science.co.il/Language/Character-sets.asp

    OracleConnection conn = (OracleConnection)ConnectionAccessBean.getConnection(DataSource.WERCS);
    OraclePreparedStatement pst = null;
    OracleResultSet rs = null;

    languages = new HashMap<String, WercsLanguageBean>();

    try
    {
      StringBuilder sb = new StringBuilder();
      sb.append("select language, database_charset_name, windows_charset_name ");
      sb.append("from vca_languages ");

      pst = (OraclePreparedStatement) conn.prepareStatement(sb.toString());
      rs = (OracleResultSet) pst.executeQuery();

      while (rs.next())
      {
        WercsLanguageBean bean = new WercsLanguageBean();
        bean.setLanguage(rs.getString("language"));
        bean.setDatabaseCharsetName(rs.getString("database_charset_name"));
        bean.setWindowsCharsetName(rs.getString("windows_charset_name"));

        languages.put(bean.getLanguage(), bean);
      }
    }
    catch (Exception e)
    {
      log4jLogger.error(e);
    }
    finally
    {
      JDBCUtil.close(pst, rs);
      JDBCUtil.close(conn);
    }
  }

  public static String updateWercsDescriptionTranslation(String alias, String productDescription, String language, OracleConnection conn)
  {
    String returnValue = "success";
    String encodedTranslation = convertDataFromWindowsEncoding(productDescription, language);
    Statement stmt = null;
    try
    {
      StringBuffer sql = new StringBuffer();
      sql.append("MERGE INTO t_alias_language_names a ");
      sql.append("USING (SELECT '" + alias + "' AS F_ALIAS FROM DUAL) b ");
      sql.append("ON (a.F_ALIAS = b.F_ALIAS and a.F_LANGUAGE = '");
      sql.append(language);
      sql.append("') ");
      sql.append("WHEN MATCHED THEN ");
      sql.append("UPDATE ");
      sql.append("SET f_alias_name = '" + encodedTranslation + "', ");
      sql.append("f_user_id = 'WERCS', ");
      sql.append("f_date_stamp = sysdate ");
      sql.append("WHEN NOT MATCHED THEN ");
      sql.append("INSERT (f_language, f_alias, f_alias_name, f_user_id, f_date_stamp) ");
      sql.append("VALUES ('" + language + "', '" + alias + "', '" + encodedTranslation + "', 'WERCS', sysdate) ");

      stmt = conn.createStatement();
      stmt.executeUpdate(sql.toString());
    }
    catch (Exception e)
    {
      log4jLogger.error("Record with error - Alias: " + alias + "  Language: " + language, e);
      returnValue = "failure";
    }
    finally
    {
      JDBCUtil.close(stmt);
    }
    return returnValue;
  }

  public static String convertDataFromWindowsEncoding(String data, String language)
  {
    if ("SN".equals(language))
    {
      // temporary bridge for Slovenian that changed language codes between 5x and 6x
      language = "SL";
    }

    WercsLanguageBean languageBean = languages.get(language);

    if (languageBean == null)
    {
      throw new RuntimeException("language " + language + " is not supported!");
    }

    if (languageBean.needsConversion())
    {
      return languageBean.convertFromWindowsEncoding(data);
    }
    else
    {
      return data;
    }
  }

  public static String convertDataToWindowsEncoding(String data, String language)
  {
    if ("SN".equals(language))
    {
      // temporary bridge for Slovenian that changed language codes between 5x and 6x
      language = "SL";
    }

    WercsLanguageBean languageBean = languages.get(language);

    if (languageBean == null)
    {
      throw new RuntimeException("language " + language + " is not supported!");
    }

    if (languageBean.needsConversion())
    {
      return languageBean.convertToWindowsEncoding(data);
    }
    else
    {
      return data;
    }
  }
}

