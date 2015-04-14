package com.valspar.interfaces.regulatory.optivaobsoleteexpire.expirevalidityrules.program;

import com.valspar.interfaces.common.BaseInterface;
import com.valspar.interfaces.common.enums.DataSource;
import com.valspar.interfaces.regulatory.optivaobsoleteexpire.common.beans.ValidityRuleBean;
import com.valspar.interfaces.regulatory.optivaobsoleteexpire.common.dao.OptivaObsoleteExpireDAO;
import java.util.ArrayList;
import org.apache.log4j.Logger;

public class ExpireValidityRulesInterface extends BaseInterface
{
  private static Logger log4jLogger = Logger.getLogger(ExpireValidityRulesInterface.class);

  public ExpireValidityRulesInterface()
  {
  }

  public void execute()
  {
    try
    {
      ArrayList<ValidityRuleBean> northAmericaRules = OptivaObsoleteExpireDAO.buildValidityRuleBeans(DataSource.NORTHAMERICAN);

      if (!northAmericaRules.isEmpty())
      {
        OptivaObsoleteExpireDAO.setNorthAmericaNextPeriodStartDate(OptivaObsoleteExpireDAO.getNextPeriodStartDate(DataSource.NORTHAMERICAN));
        for (ValidityRuleBean vrBean: northAmericaRules)
        {
          log4jLogger.info("Expiring validity rule: " + vrBean.getRuleId() + " org code: " + vrBean.getOrgnCode());
          OptivaObsoleteExpireDAO.update11iValidityRule(DataSource.NORTHAMERICAN, vrBean.getRuleId(), vrBean.getOrgnCode());
        }
      }
    }
    catch (Exception e)
    {
      log4jLogger.error(e);
    }
  }
}
