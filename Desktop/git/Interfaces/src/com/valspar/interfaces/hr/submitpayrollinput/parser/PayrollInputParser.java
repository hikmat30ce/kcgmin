package com.valspar.interfaces.hr.submitpayrollinput.parser;

import com.valspar.interfaces.hr.submitpayrollinput.beans.PayrollInputBean;
import java.util.*;
import org.apache.log4j.*;

public abstract class PayrollInputParser
{
  private static Logger log4jLogger = Logger.getLogger(PayrollInputParser.class);
  private List<String[]> readerList;
  private String referenceID;
  
  public void parse(String[] inputLine, PayrollInputBean payrollInputBean, List<PayrollInputBean> payrollInputBeanErrorList)
  {
  }

  public void removeUTFBOM()
  {
    if (this.getReaderList() != null && !this.getReaderList().isEmpty())
    {
      String [] firstInputLine = this.getReaderList().get(0);
      if (firstInputLine != null && firstInputLine.length != 0 && firstInputLine[0].contains("\uFEFF"))
      {
        firstInputLine[0] = firstInputLine[0].replace("\uFEFF", "");
        log4jLogger.info("PayrollInputParser.removeUTFBOM() removed \uFEFF");
      }
    }
  }

  public void setReaderList(List<String[]> readerList)
  {
    this.readerList = readerList;
  }

  public List<String[]> getReaderList()
  {
    return readerList;
  }

  public void setReferenceID(String referenceID)
  {
    this.referenceID = referenceID;
  }

  public String getReferenceID()
  {
    return referenceID;
  }
}
