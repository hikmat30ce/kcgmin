package com.valspar.interfaces.common.utils;

import java.util.*;
import com.valspar.interfaces.regulatory.optivatowercs.beans.*;

public class Conversion
{
  public Conversion()
  {
  }

  public static String [][] hashMapToArray(HashMap hm)
  {
    String [][] objArray = new String[hm.size()][2];
    Set s = hm.keySet();
    Iterator i = s.iterator();
    int counter = 0;
    while (i.hasNext())
    {
      objArray[counter][0] = (String)i.next();
      objArray[counter][1] = (String)hm.get(objArray[counter][0]);
      ++counter;
    }
    return objArray;
  }

  public static String [][] arrayListToArray(ArrayList ar)
  {
    String [][] objArray = new String[ar.size()][3];
    Iterator i = ar.iterator();
    int counter = 0;
    while (i.hasNext())
    {
      ComponentBean compBean = (ComponentBean)i.next();
      objArray[counter][0] = compBean.getComponentId();
      objArray[counter][1] = compBean.getPercent();
      objArray[counter][2] = compBean.getDescription();
      ++counter;
    }
    return objArray;
  }
}