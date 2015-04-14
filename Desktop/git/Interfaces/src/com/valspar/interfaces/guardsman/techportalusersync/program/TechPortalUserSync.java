package com.valspar.interfaces.guardsman.techportalusersync.program;

import com.valspar.interfaces.common.BaseInterface;
import com.valspar.interfaces.guardsman.techportalusersync.beans.TechUserBean;
import com.valspar.interfaces.guardsman.techportalusersync.dao.TechPortalDAO;
import com.valspar.interfaces.guardsman.techportalusersync.utility.OIDUtility;
import java.util.List;
import org.apache.log4j.Logger;

public class TechPortalUserSync extends BaseInterface
{
  private static Logger log4jLogger = Logger.getLogger(TechPortalUserSync.class);

  public void execute()
  {
    List<TechUserBean> techPortalUsers = TechPortalDAO.getTechPortalUsers();

    for (TechUserBean techPortalUser : techPortalUsers)
    {
      log4jLogger.info("");
      log4jLogger.info("User: " + techPortalUser.getFullName() + (techPortalUser.isExpired() ? " (password expired)" : "") + "...");
      OIDUtility.syncUser(techPortalUser);
    }
  }
}
