package com.valspar.interfaces.common.interfaces;

import commonj.work.Work;
import java.util.Date;

public interface ValsparWork extends Work
{
  public String getProcessingStatusMessage();

  public void setStartTime(Date startTime);
  public Date getStartTime();

  public int getElapsedMinutes();
}
