package com.valspar.interfaces.sales.sampleopportunity.program;

import com.valspar.interfaces.common.BaseInterface;
import com.valspar.interfaces.sales.sampleopportunity.dao.SampleOpportunityDAO;
import org.apache.log4j.Logger;

public class SampleOpportunityInterface extends BaseInterface
{
  private static Logger log4jLogger = Logger.getLogger(SampleOpportunityInterface.class);

  public SampleOpportunityInterface()
  {
  }

  public void execute()
  {
    log4jLogger.info("Sample Opportunity interface starting ");
    SampleOpportunityDAO.createSamples();
    log4jLogger.info("Sample Opportunity interface ended ");
  }
}
