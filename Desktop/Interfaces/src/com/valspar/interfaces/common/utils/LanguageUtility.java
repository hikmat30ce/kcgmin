package com.valspar.interfaces.common.utils;

import java.nio.ByteBuffer;
import java.nio.charset.Charset;
import java.nio.charset.CharsetDecoder;
import java.nio.charset.CharsetEncoder;
import java.nio.CharBuffer;
import java.sql.Statement;
import oracle.jdbc.OracleConnection;
import org.apache.log4j.Logger;

public class LanguageUtility
{
  private static Logger log4jLogger = Logger.getLogger(LanguageUtility.class);

  public LanguageUtility()
  {
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
  
  public static String convertDataToWindowsEncoding(String data, String language) 
  {
    String result = "";
    try
    {
      if (language != null && language.equalsIgnoreCase("EL")) //Greek
      {
        Charset databaseCharset = Charset.forName("windows-1252");
        CharsetEncoder databaseEncoder = databaseCharset.newEncoder();
        Charset windowsCharset = Charset.forName("windows-1253");
        CharsetDecoder windowsDecoder = windowsCharset.newDecoder();
        ByteBuffer dbByteBuf = databaseEncoder.encode(CharBuffer.wrap(data));
        CharBuffer dbCharBuf = windowsDecoder.decode(dbByteBuf);
        result = dbCharBuf.toString();
      }
      else if (language != null && language.equalsIgnoreCase("RU") || language != null && language.equalsIgnoreCase("BG"))
      //Russian,Bulgarian
      {
        Charset databaseCharset = Charset.forName("windows-1252");
        CharsetEncoder databaseEncoder = databaseCharset.newEncoder();
        Charset windowsCharset = Charset.forName("windows-1251");
        CharsetDecoder windowsDecoder = windowsCharset.newDecoder();
        ByteBuffer dbByteBuf = databaseEncoder.encode(CharBuffer.wrap(data));
        CharBuffer dbCharBuf = windowsDecoder.decode(dbByteBuf);
        result = dbCharBuf.toString();
      }
      else if (language != null && language.equalsIgnoreCase("CN")) //Chinese Simplified
      {
        Charset databaseCharset = Charset.forName("windows-1252");
        CharsetEncoder databaseEncoder = databaseCharset.newEncoder();
        Charset windowsCharset = Charset.forName("GBK");
        CharsetDecoder windowsDecoder = windowsCharset.newDecoder();
        ByteBuffer dbByteBuf = databaseEncoder.encode(CharBuffer.wrap(data));
        CharBuffer dbCharBuf = windowsDecoder.decode(dbByteBuf);
        result = dbCharBuf.toString();
      }
      else if (language != null && language.equalsIgnoreCase("KO")) //Korean
      {
        Charset databaseCharset = Charset.forName("windows-1252");
        CharsetEncoder databaseEncoder = databaseCharset.newEncoder();
        Charset windowsCharset = Charset.forName("EUC-KR");
        CharsetDecoder windowsDecoder = windowsCharset.newDecoder();
        ByteBuffer dbByteBuf = databaseEncoder.encode(CharBuffer.wrap(data));
        CharBuffer dbCharBuf = windowsDecoder.decode(dbByteBuf);
        result = dbCharBuf.toString();
      }
      else if (language != null && language.equalsIgnoreCase("LT") || language != null && language.equalsIgnoreCase("LV") || language != null && language.equalsIgnoreCase("ET")) //Lithuanian,Latvian,Estonian
      {
        Charset databaseCharset = Charset.forName("windows-1252");
        CharsetEncoder databaseEncoder = databaseCharset.newEncoder();
        Charset windowsCharset = Charset.forName("windows-1257");
        CharsetDecoder windowsDecoder = windowsCharset.newDecoder();
        ByteBuffer dbByteBuf = databaseEncoder.encode(CharBuffer.wrap(data));
        CharBuffer dbCharBuf = windowsDecoder.decode(dbByteBuf);
        result = dbCharBuf.toString();
      }
      else if (language != null && language.equalsIgnoreCase("SN") || language != null && language.equalsIgnoreCase("RO") || language != null && language.equalsIgnoreCase("CR") || language != null && language.equalsIgnoreCase("CS") || language != null && language.equalsIgnoreCase("PL") || language != null && language.equalsIgnoreCase("HU")) //Slovenian,Romanian,Croatian,Czech,Polish,Hungarian
      {
        Charset databaseCharset = Charset.forName("windows-1252");
        CharsetEncoder databaseEncoder = databaseCharset.newEncoder();
        Charset windowsCharset = Charset.forName("windows-1250");
        CharsetDecoder windowsDecoder = windowsCharset.newDecoder();
        ByteBuffer dbByteBuf = databaseEncoder.encode(CharBuffer.wrap(data));
        CharBuffer dbCharBuf = windowsDecoder.decode(dbByteBuf);
        result = dbCharBuf.toString();
      }
      else if (language != null && language.equalsIgnoreCase("TA")) //Chinese Traditional
      {
        Charset databaseCharset = Charset.forName("windows-1252");
        CharsetEncoder databaseEncoder = databaseCharset.newEncoder();
        Charset windowsCharset = Charset.forName("Big5-HKSCS");
        CharsetDecoder windowsDecoder = windowsCharset.newDecoder();
        ByteBuffer dbByteBuf = databaseEncoder.encode(CharBuffer.wrap(data));
        CharBuffer dbCharBuf = windowsDecoder.decode(dbByteBuf);
        result = dbCharBuf.toString();
      }
      else if (language != null && language.equalsIgnoreCase("TH")) //Thailand
      {
        Charset databaseCharset = Charset.forName("windows-1252");
        CharsetEncoder databaseEncoder = databaseCharset.newEncoder();
        Charset windowsCharset = Charset.forName("TIS-620");
        CharsetDecoder windowsDecoder = windowsCharset.newDecoder();
        ByteBuffer dbByteBuf = databaseEncoder.encode(CharBuffer.wrap(data));
        CharBuffer dbCharBuf = windowsDecoder.decode(dbByteBuf);
        result = dbCharBuf.toString();
      }
      else if (language != null && language.equalsIgnoreCase("TR")) //Turkish
      {
        Charset databaseCharset = Charset.forName("windows-1252");
        CharsetEncoder databaseEncoder = databaseCharset.newEncoder();
        Charset windowsCharset = Charset.forName("windows-1254");
        CharsetDecoder windowsDecoder = windowsCharset.newDecoder();
        ByteBuffer dbByteBuf = databaseEncoder.encode(CharBuffer.wrap(data));
        CharBuffer dbCharBuf = windowsDecoder.decode(dbByteBuf);
        result = dbCharBuf.toString();
      }
      else if (language != null && language.equalsIgnoreCase("AR")) //ARABIC
      {
        Charset databaseCharset = Charset.forName("windows-1252");
        CharsetEncoder databaseEncoder = databaseCharset.newEncoder();
        Charset windowsCharset = Charset.forName("windows-1256");
        CharsetDecoder windowsDecoder = windowsCharset.newDecoder();
        ByteBuffer dbByteBuf = databaseEncoder.encode(CharBuffer.wrap(data));
        CharBuffer dbCharBuf = windowsDecoder.decode(dbByteBuf);
        result = dbCharBuf.toString();
      }
      else if (language != null && language.equalsIgnoreCase("HE")) //HEBREW
      {
        Charset databaseCharset = Charset.forName("windows-1252");
        CharsetEncoder databaseEncoder = databaseCharset.newEncoder();
        Charset windowsCharset = Charset.forName("windows-1255");
        CharsetDecoder windowsDecoder = windowsCharset.newDecoder();
        ByteBuffer dbByteBuf = databaseEncoder.encode(CharBuffer.wrap(data));
        CharBuffer dbCharBuf = windowsDecoder.decode(dbByteBuf);
        result = dbCharBuf.toString();
      }
      else if (language != null && language.equalsIgnoreCase("JP")) //Japanese
      {
        Charset databaseCharset = Charset.forName("X-ORACLE-WE8MSWIN1252");
        CharsetEncoder databaseEncoder = databaseCharset.newEncoder();
        Charset windowsCharset = Charset.forName("Shift_JIS");
        CharsetDecoder windowsDecoder = windowsCharset.newDecoder();
        ByteBuffer dbByteBuf = databaseEncoder.encode(CharBuffer.wrap(data));
        CharBuffer dbCharBuf = windowsDecoder.decode(dbByteBuf);
        result = dbCharBuf.toString();
      }
      else
      {
        result = data;
      }
    }
    catch (Exception e)
    {
      result = "INVALID TRANSLATION";
    }
    return result;
  }

  public static String convertDataFromWindowsEncoding(String data, String language) 
  {
    String result = "";
    try
    {
      if (language != null && language.equalsIgnoreCase("EL")) //Greek
      {
        Charset databaseCharset = Charset.forName("windows-1252");
        CharsetDecoder databaseDecoder = databaseCharset.newDecoder();
        Charset windowsCharset = Charset.forName("windows-1253");
        CharsetEncoder windowsEncoder = windowsCharset.newEncoder();
        ByteBuffer dbByteBuf = windowsEncoder.encode(CharBuffer.wrap(data));
        CharBuffer dbCharBuf = databaseDecoder.decode(dbByteBuf);
        result = dbCharBuf.toString();
      }
      else if (language != null && language.equalsIgnoreCase("RU") || language != null && language.equalsIgnoreCase("BG"))
      //Russian,Bulgarian
      {
        Charset databaseCharset = Charset.forName("windows-1252");
        CharsetDecoder databaseDecoder = databaseCharset.newDecoder();
        Charset windowsCharset = Charset.forName("windows-1251");
        CharsetEncoder windowsEncoder = windowsCharset.newEncoder();
        ByteBuffer dbByteBuf = windowsEncoder.encode(CharBuffer.wrap(data));
        CharBuffer dbCharBuf = databaseDecoder.decode(dbByteBuf);
        result = dbCharBuf.toString();
      }
      else if (language != null && language.equalsIgnoreCase("CN")) //Chinese Simplified
      {
        Charset databaseCharset = Charset.forName("windows-1252");
        CharsetDecoder databaseDecoder = databaseCharset.newDecoder();
        Charset windowsCharset = Charset.forName("GBK");
        CharsetEncoder windowsEncoder = windowsCharset.newEncoder();
        ByteBuffer dbByteBuf = windowsEncoder.encode(CharBuffer.wrap(data));
        CharBuffer dbCharBuf = databaseDecoder.decode(dbByteBuf);
        result = dbCharBuf.toString();
      }
      else if (language != null && language.equalsIgnoreCase("KO")) //Korean
      {
        Charset databaseCharset = Charset.forName("windows-1252");
        CharsetDecoder databaseDecoder = databaseCharset.newDecoder();
        Charset windowsCharset = Charset.forName("EUC-KR");
        CharsetEncoder windowsEncoder = windowsCharset.newEncoder();
        ByteBuffer dbByteBuf = windowsEncoder.encode(CharBuffer.wrap(data));
        CharBuffer dbCharBuf = databaseDecoder.decode(dbByteBuf);
        result = dbCharBuf.toString();
      }
      else if (language != null && language.equalsIgnoreCase("LT") || language != null && language.equalsIgnoreCase("LV") || language != null && language.equalsIgnoreCase("ET")) //Lithuanian,Latvian,Estonian
      {
        Charset databaseCharset = Charset.forName("windows-1252");
        CharsetDecoder databaseDecoder = databaseCharset.newDecoder();
        Charset windowsCharset = Charset.forName("windows-1257");
        CharsetEncoder windowsEncoder = windowsCharset.newEncoder();
        ByteBuffer dbByteBuf = windowsEncoder.encode(CharBuffer.wrap(data));
        CharBuffer dbCharBuf = databaseDecoder.decode(dbByteBuf);
        result = dbCharBuf.toString();
      }
      else if (language != null && language.equalsIgnoreCase("SN") || language != null && language.equalsIgnoreCase("RO") || language != null && language.equalsIgnoreCase("CR") || language != null && language.equalsIgnoreCase("CS") || language != null && language.equalsIgnoreCase("PL") || language != null && language.equalsIgnoreCase("HU")) //Slovenian,Romanian,Croatian,Czech,Polish,Hungarian
      {
        Charset databaseCharset = Charset.forName("windows-1252");
        CharsetDecoder databaseDecoder = databaseCharset.newDecoder();
        Charset windowsCharset = Charset.forName("windows-1250");
        CharsetEncoder windowsEncoder = windowsCharset.newEncoder();
        ByteBuffer dbByteBuf = windowsEncoder.encode(CharBuffer.wrap(data));
        CharBuffer dbCharBuf = databaseDecoder.decode(dbByteBuf);
        result = dbCharBuf.toString();
      }
      else if (language != null && language.equalsIgnoreCase("TA")) //Chinese Traditional
      {
        Charset databaseCharset = Charset.forName("windows-1252");
        CharsetDecoder databaseDecoder = databaseCharset.newDecoder();
        Charset windowsCharset = Charset.forName("Big5-HKSCS");
        CharsetEncoder windowsEncoder = windowsCharset.newEncoder();
        ByteBuffer dbByteBuf = windowsEncoder.encode(CharBuffer.wrap(data));
        CharBuffer dbCharBuf = databaseDecoder.decode(dbByteBuf);
        result = dbCharBuf.toString();
      }
      else if (language != null && language.equalsIgnoreCase("TH")) //Thailand
      {
        Charset databaseCharset = Charset.forName("windows-1252");
        CharsetDecoder databaseDecoder = databaseCharset.newDecoder();
        Charset windowsCharset = Charset.forName("TIS-620");
        CharsetEncoder windowsEncoder = windowsCharset.newEncoder();
        ByteBuffer dbByteBuf = windowsEncoder.encode(CharBuffer.wrap(data));
        CharBuffer dbCharBuf = databaseDecoder.decode(dbByteBuf);
        result = dbCharBuf.toString();
      }
      else if (language != null && language.equalsIgnoreCase("TR")) //Turkish
      {
        Charset databaseCharset = Charset.forName("windows-1252");
        CharsetDecoder databaseDecoder = databaseCharset.newDecoder();
        Charset windowsCharset = Charset.forName("windows-1254");
        CharsetEncoder windowsEncoder = windowsCharset.newEncoder();
        ByteBuffer dbByteBuf = windowsEncoder.encode(CharBuffer.wrap(data));
        CharBuffer dbCharBuf = databaseDecoder.decode(dbByteBuf);
        result = dbCharBuf.toString();
      }
      else if (language != null && language.equalsIgnoreCase("AR")) //ARABIC
      {
        Charset databaseCharset = Charset.forName("windows-1252");
        CharsetDecoder databaseDecoder = databaseCharset.newDecoder();
        Charset windowsCharset = Charset.forName("windows-1256");
        CharsetEncoder windowsEncoder = windowsCharset.newEncoder();
        ByteBuffer dbByteBuf = windowsEncoder.encode(CharBuffer.wrap(data));
        CharBuffer dbCharBuf = databaseDecoder.decode(dbByteBuf);
        result = dbCharBuf.toString();
      }
      else if (language != null && language.equalsIgnoreCase("HE")) //HEBREW
      {
        Charset databaseCharset = Charset.forName("windows-1252");
        CharsetDecoder databaseDecoder = databaseCharset.newDecoder();
        Charset windowsCharset = Charset.forName("windows-1255");
        CharsetEncoder windowsEncoder = windowsCharset.newEncoder();
        ByteBuffer dbByteBuf = windowsEncoder.encode(CharBuffer.wrap(data));
        CharBuffer dbCharBuf = databaseDecoder.decode(dbByteBuf);
        result = dbCharBuf.toString();
      }
      else if (language != null && language.equalsIgnoreCase("JP")) //Japanese
      {
        Charset databaseCharset = Charset.forName("X-ORACLE-WE8MSWIN1252");
        CharsetDecoder databaseDecoder = databaseCharset.newDecoder();
        Charset windowsCharset = Charset.forName("Shift_JIS");
        CharsetEncoder windowsEncoder = windowsCharset.newEncoder();
        ByteBuffer dbByteBuf = windowsEncoder.encode(CharBuffer.wrap(data));
        CharBuffer dbCharBuf = databaseDecoder.decode(dbByteBuf);
        result = dbCharBuf.toString();
      }
      else
      {
        result = data;
      }
    }
    catch (Exception e)
    {
      result = "INVALID TRANSLATION";
    }
    return result;
  }
}

