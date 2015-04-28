package com.valspar.interfaces.purchasing.allocatedpurchases.threads;

import com.valspar.interfaces.common.beans.InterfaceInfoBean;
import commonj.work.Work;

public abstract class BaseBuilderThread implements Work
{
  private InterfaceInfoBean interfaceInfo;
  private Exception error;

  public BaseBuilderThread()
  {
    super();
  }

  public void release()
  {
  }

  public boolean isDaemon()
  {
    return false;
  }

  public void setError(Exception error)
  {
    this.error = error;
  }

  public Exception getError()
  {
    return error;
  }

  public void setInterfaceInfo(InterfaceInfoBean interfaceInfo)
  {
    this.interfaceInfo = interfaceInfo;
  }

  public InterfaceInfoBean getInterfaceInfo()
  {
    return interfaceInfo;
  }
}
