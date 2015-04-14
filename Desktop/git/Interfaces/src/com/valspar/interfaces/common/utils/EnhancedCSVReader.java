package com.valspar.interfaces.common.utils;

import au.com.bytecode.opencsv.CSVReader;
import com.valspar.interfaces.common.exceptions.SpreadsheetReadingFatalException;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import org.apache.commons.lang.StringUtils;

public class EnhancedCSVReader extends CSVReader
{
  private Map<String, Integer> columnHeaderToIndexMap;
  private boolean throwExceptionForColumnNotFound;
  private String[] currentRow;

  public EnhancedCSVReader(java.io.Reader reader, char c, char c1, char c2, int i, boolean b, boolean b1)
  {
    super(reader, c, c1, c2, i, b, b1);
  }

  public EnhancedCSVReader(java.io.Reader reader, char c, char c1, char c2, int i, boolean b)
  {
    super(reader, c, c1, c2, i, b);
  }

  public EnhancedCSVReader(java.io.Reader reader, char c, char c1, char c2, int i)
  {
    super(reader, c, c1, c2, i);
  }

  public EnhancedCSVReader(java.io.Reader reader, char c, char c1, int i)
  {
    super(reader, c, c1, i);
  }

  public EnhancedCSVReader(java.io.Reader reader, char c, char c1, char c2)
  {
    super(reader, c, c1, c2);
  }

  public EnhancedCSVReader(java.io.Reader reader, char c, char c1, boolean b)
  {
    super(reader, c, c1, b);
  }

  public EnhancedCSVReader(java.io.Reader reader, char c, char c1)
  {
    super(reader, c, c1);
  }

  public EnhancedCSVReader(java.io.Reader reader, char c)
  {
    super(reader, c);
  }

  public EnhancedCSVReader(java.io.Reader reader)
  {
    super(reader);
  }

  private int getColumnIndex(String columnHeader) throws SpreadsheetReadingFatalException
  {
    if (columnHeaderToIndexMap == null)
    {
      throw new SpreadsheetReadingFatalException("You must call readColumnHeaders() on the first line of the spreadsheet first");
    }

    String columnHeaderKey = StringUtils.lowerCase(StringUtils.trim(columnHeader));
    Integer columnIndex = columnHeaderToIndexMap.get(columnHeaderKey);

    if (columnIndex == null && throwExceptionForColumnNotFound)
    {
      throw new SpreadsheetReadingFatalException("Column '" + columnHeader + "' not found!");
    }

    return columnIndex;
  }

  public void readColumnHeaders() throws IOException
  {
    readColumnHeaders(readNext());
  }

  public void readColumnHeaders(String[] headers) throws IOException
  {
    columnHeaderToIndexMap = new HashMap<String, Integer>();

    for (int i = 0; i < headers.length; i++)
    {
      String header = StringUtils.lowerCase(StringUtils.trim(headers[i]));
      if (StringUtils.isNotEmpty(header))
      {
        columnHeaderToIndexMap.put(header, i);
      }
    }
  }

  public boolean next() throws IOException
  {
    currentRow = readNext();
    return currentRow != null;
  }

  public String[] getCurrentRow()
  {
    return currentRow;
  }

  public String getString(String columnHeader) throws SpreadsheetReadingFatalException
  {
    int columnIndex = getColumnIndex(columnHeader);
    return currentRow[columnIndex];
  }

  public void setThrowExceptionForColumnNotFound(boolean throwExceptionForColumnNotFound)
  {
    this.throwExceptionForColumnNotFound = throwExceptionForColumnNotFound;
  }

  public boolean isThrowExceptionForColumnNotFound()
  {
    return throwExceptionForColumnNotFound;
  }
  }
