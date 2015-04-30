package com.valspar.interfaces.wercs.common.beans;

import java.nio.*;
import java.nio.charset.*;
import org.apache.commons.lang3.StringUtils;
import org.apache.log4j.Logger;

public class WercsLanguageBean
{
  private static Logger log4jLogger = Logger.getLogger(WercsLanguageBean.class);

  private String language;
  private String databaseCharsetName;
  private String windowsCharsetName;

  public boolean needsConversion()
  {
    return StringUtils.isNotEmpty(databaseCharsetName) && StringUtils.isNotEmpty(windowsCharsetName);
  }

  public String convertToWindowsEncoding(String data)
  {
    try
    {
      Charset databaseCharset = Charset.forName(databaseCharsetName);
      CharsetEncoder databaseEncoder = databaseCharset.newEncoder();

      Charset windowsCharset = Charset.forName(windowsCharsetName);
      CharsetDecoder windowsDecoder = windowsCharset.newDecoder();

      ByteBuffer dbByteBuf = databaseEncoder.encode(CharBuffer.wrap(data));
      CharBuffer winCharBuf = windowsDecoder.decode(dbByteBuf);

      return winCharBuf.toString();
    }
    catch (Exception e)
    {
      log4jLogger.error("Data=" + data, e);
      return "INVALID TRANSLATION";
    }
  }

  public String convertFromWindowsEncoding(String data)
  {
    try
    {
      Charset databaseCharset = Charset.forName(databaseCharsetName);
      CharsetDecoder databaseDecoder = databaseCharset.newDecoder();

      Charset windowsCharset = Charset.forName(windowsCharsetName);
      CharsetEncoder windowsEncoder = windowsCharset.newEncoder();

      ByteBuffer winByteBuf = windowsEncoder.encode(CharBuffer.wrap(data));
      CharBuffer dbCharBuf = databaseDecoder.decode(winByteBuf);

      return dbCharBuf.toString();
    }
    catch (Exception e)
    {
      log4jLogger.error("Data=" + data, e);
      return "INVALID TRANSLATION";
    }
  }

  public void setLanguage(String language)
  {
    this.language = language;
  }

  public String getLanguage()
  {
    return language;
  }

  public void setDatabaseCharsetName(String databaseCharsetName)
  {
    this.databaseCharsetName = databaseCharsetName;
  }

  public String getDatabaseCharsetName()
  {
    return databaseCharsetName;
  }

  public void setWindowsCharsetName(String windowsCharsetName)
  {
    this.windowsCharsetName = windowsCharsetName;
  }

  public String getWindowsCharsetName()
  {
    return windowsCharsetName;
  }
}
