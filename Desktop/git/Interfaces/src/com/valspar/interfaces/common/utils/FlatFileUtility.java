package com.valspar.interfaces.common.utils;

import java.io.*;
import java.nio.charset.Charset;
import java.util.*;
import org.apache.commons.lang3.*;
import org.apache.log4j.Logger;

public class FlatFileUtility extends ReportUtility
{
  private static Logger log4jLogger = Logger.getLogger(FlatFileUtility.class);
  private BufferedWriter bufferedWriter;
  private String fieldDelimiter;
  private String endOfLineSequence;
  private boolean zipFile;
  private boolean reportEmpty;
  private boolean csvEscapeAllValues;
  private boolean forceNumberFormat;
  private boolean initialized;

  public FlatFileUtility(String fileExtension, String delimeter, String endOfLineSequence)
  {
    setFieldDelimiter(delimeter);
    setEndOfLineSequence(endOfLineSequence);
    setFileExtension(fileExtension);
  }

  private void init() throws IOException
  {
    buildFileName(getFileExtension());
    setBufferedWriter(new BufferedWriter(new OutputStreamWriter(new FileOutputStream(this.getFileWritePath()), Charset.forName("UTF-8"))));
  }

  private void checkInit()
  {
    if (!initialized)
    {
      try
      {
        init();
      }
      catch (Exception e)
      {
        log4jLogger.error(e);
      }

      initialized = true;
    }
  }

  public void writeLine(List<String> fieldList)
  {
    try
    {
      checkInit();

      StringBuilder sb = new StringBuilder();
      int i = 0;
      for (String field: fieldList)
      {
        if (isCsvEscapeAllValues())
        {
          if (forceNumberFormat && CommonUtility.isNumeric(field))
          {
            sb.append("=\"");
            sb.append(CommonUtility.nvl(field, ""));
            sb.append("\"");
          }
          else
          {
            sb.append(CommonUtility.nvl(StringEscapeUtils.escapeCsv(field), ""));
          }
        }
        else
        {
          sb.append(CommonUtility.nvl(field, ""));
        }
        if (i < fieldList.size() - 1)
        {
          sb.append(getFieldDelimiter());
        }
        i++;
      }
      sb.append(getEndOfLineSequence());
      getBufferedWriter().write(sb.toString());
    }
    catch (Exception e)
    {
      log4jLogger.error(e);
    }
  }

  public void writeLine(String... fields)
  {
    writeLine(Arrays.asList(fields));
  }

  public void writeUTF8Bom()
  {
    try
    {
      checkInit();
      getBufferedWriter().write("\uFEFF");
    }
    catch (Exception e)
    {
      log4jLogger.error(e);
    }
  }

  public void close()
  {
    try
    {
      checkInit();
      getBufferedWriter().flush();
      getBufferedWriter().close();
    }
    catch (Exception e)
    {
      log4jLogger.error(e);
    }
  }

  public void setBufferedWriter(BufferedWriter bufferedWriter)
  {
    this.bufferedWriter = bufferedWriter;
  }

  public BufferedWriter getBufferedWriter()
  {
    return bufferedWriter;
  }

  public void setFieldDelimiter(String fieldDelimiter)
  {
    this.fieldDelimiter = fieldDelimiter;
  }

  public String getFieldDelimiter()
  {
    return fieldDelimiter;
  }

  public void setEndOfLineSequence(String endOfLineSequence)
  {
    this.endOfLineSequence = endOfLineSequence;
  }

  public String getEndOfLineSequence()
  {
    return endOfLineSequence;
  }

  public void setZipFile(boolean zipFile)
  {
    this.zipFile = zipFile;
  }

  public boolean isZipFile()
  {
    return zipFile;
  }

  public void setReportEmpty(boolean reportEmpty)
  {
    this.reportEmpty = reportEmpty;
  }

  public boolean isReportEmpty()
  {
    return reportEmpty;
  }

  public void setCsvEscapeAllValues(boolean csvEscapeAllValues)
  {
    this.csvEscapeAllValues = csvEscapeAllValues;
  }

  public boolean isCsvEscapeAllValues()
  {
    return csvEscapeAllValues;
  }

  public void setForceNumberFormat(boolean forceNumberFormat)
  {
    this.forceNumberFormat = forceNumberFormat;
  }

  public boolean isForceNumberFormat()
  {
    return forceNumberFormat;
  }
}
