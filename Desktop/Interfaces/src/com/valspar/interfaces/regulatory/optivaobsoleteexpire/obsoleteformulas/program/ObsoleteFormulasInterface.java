package com.valspar.interfaces.regulatory.optivaobsoleteexpire.obsoleteformulas.program;

import com.valspar.interfaces.common.BaseInterface;
import com.valspar.interfaces.common.enums.DataSource;
import com.valspar.interfaces.regulatory.optivaobsoleteexpire.common.beans.*;
import com.valspar.interfaces.regulatory.optivaobsoleteexpire.common.dao.OptivaObsoleteExpireDAO;
import java.util.ArrayList;
import org.apache.log4j.Logger;

public class ObsoleteFormulasInterface extends BaseInterface
{
  private static Logger log4jLogger = Logger.getLogger(ObsoleteFormulasInterface.class);

  public ObsoleteFormulasInterface()
  {
  }

  public void execute()
  {
    String recordsToProcess = getParameterValue("recordsToProcess");

    try
    {
      OptivaObsoleteExpireDAO.setNorthAmericaNextPeriodStartDate(OptivaObsoleteExpireDAO.getNextPeriodStartDate(DataSource.NORTHAMERICAN));
      OptivaObsoleteExpireDAO.setEmeaiNextPeriodStartDate(OptivaObsoleteExpireDAO.getNextPeriodStartDate(DataSource.EMEAI));
      OptivaObsoleteExpireDAO.setAsiapacNextPeriodStartDate(OptivaObsoleteExpireDAO.getNextPeriodStartDate(DataSource.ASIAPAC));
      log4jLogger.info("Number of Records to Process: " + recordsToProcess);
      ArrayList<ItemBean> ar = OptivaObsoleteExpireDAO.buildItemBeans(recordsToProcess);
      if (!ar.isEmpty())
      {
        for (ItemBean iBean: ar)
        {          
          log4jLogger.info("Obsoleting formula: " + iBean.getFormulaId());
          OptivaObsoleteExpireDAO.updateOptivaFormula(iBean.getFormulaId());

          if (iBean.isNorthAmerica() && !iBean.getNorthAmericaValidityRules().isEmpty())
          {
            log4jLogger.info("Expiring North American validity rules...");
            for (ValidityRuleBean vrBean: iBean.getNorthAmericaValidityRules())
            {
              OptivaObsoleteExpireDAO.update11iValidityRule(DataSource.NORTHAMERICAN, vrBean.getRuleId(), vrBean.getOrgnCode());
            }
          }

          if (iBean.isEmeai() && !iBean.getEmeaiValidityRules().isEmpty())
          {
            log4jLogger.info("Expiring EMEAI validity rules...");
            for (ValidityRuleBean vrBean: iBean.getEmeaiValidityRules())
            {
              OptivaObsoleteExpireDAO.update11iValidityRule(DataSource.EMEAI, vrBean.getRuleId(), vrBean.getOrgnCode());
            }
          }

          if (iBean.isAsiapac() && !iBean.getAsiapacValidityRules().isEmpty())
          {
            log4jLogger.info("Expiring AsiaPac validity rules...");
            for (ValidityRuleBean vrBean: iBean.getAsiapacValidityRules())
            {
              OptivaObsoleteExpireDAO.update11iValidityRule(DataSource.ASIAPAC, vrBean.getRuleId(), vrBean.getOrgnCode());
            }
          }

          if (iBean.isBulk())
          {
            log4jLogger.info("Adding PRDSTAT3 to WERCS product...");
            OptivaObsoleteExpireDAO.updateWercs(iBean.getItemNumber());
          }
        }
      }
      else
      {
        log4jLogger.info("No records to process");
      }
    }
    catch (Exception e)
    {
      log4jLogger.error(e);
    }
  }
}
