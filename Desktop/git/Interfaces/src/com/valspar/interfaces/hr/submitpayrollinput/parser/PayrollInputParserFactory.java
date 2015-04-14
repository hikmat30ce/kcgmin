package com.valspar.interfaces.hr.submitpayrollinput.parser;

import java.util.*;
import org.apache.commons.lang3.*;

public class PayrollInputParserFactory
{
  public PayrollInputParserFactory()
  {
  }
  
  public static PayrollInputParser getPayrollInputParser(String fileName, List<String[]> readerList)
  {
    PayrollInputParser payrollInputParser = null;
    if (StringUtils.startsWithIgnoreCase(fileName, "Canada"))
    {
      payrollInputParser = new CanadaParser();
    }
    else
    {
      payrollInputParser =  new USAParser();
    }
    payrollInputParser.setReaderList(readerList);
    payrollInputParser.removeUTFBOM();
    return payrollInputParser;
  }
}
