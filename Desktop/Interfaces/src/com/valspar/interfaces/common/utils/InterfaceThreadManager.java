package com.valspar.interfaces.common.utils;

import com.valspar.interfaces.common.beans.InterfaceInfoBean;
import java.util.HashMap;

public final class InterfaceThreadManager
{
  private static HashMap<Long, InterfaceInfoBean> activeInterfaceThreads = new HashMap<Long, InterfaceInfoBean>();

  private InterfaceThreadManager()
  {
  }

  public static boolean isActiveInterface()
  {
    return getActiveInterfaceThreads().containsKey(Thread.currentThread().getId());
  }

  public static InterfaceInfoBean getActiveInterfaceInfo()
  {
    return getActiveInterfaceThreads().get(Thread.currentThread().getId());
  }

  public static void addInterfaceThread(InterfaceInfoBean interfaceInfo)
  {
    getActiveInterfaceThreads().put(Thread.currentThread().getId(), interfaceInfo);
  }

  public static void removeInterfaceThread()
  {
    getActiveInterfaceThreads().remove(Thread.currentThread().getId());
  }

  private static HashMap<Long, InterfaceInfoBean> getActiveInterfaceThreads()
  {
    return activeInterfaceThreads;
  }
}
