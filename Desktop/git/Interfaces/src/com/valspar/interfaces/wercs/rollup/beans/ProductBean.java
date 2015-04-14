package com.valspar.interfaces.wercs.rollup.beans;

import com.valspar.interfaces.wercs.common.beans.BaseProductBean;
import java.math.BigDecimal;
import java.util.ArrayList;

public class ProductBean extends BaseProductBean
{  
  private String rollupItemId;
  private BigDecimal level;
  private boolean intermediate; 
  private boolean obsolete;
  private ArrayList<MsdsBean> msdsList = new ArrayList<MsdsBean>();
  
  public ProductBean()
  {
  }

  public void setMsdsList(ArrayList<MsdsBean> msdsList)
  {
    this.msdsList = msdsList;
  }

  public ArrayList<MsdsBean> getMsdsList()
  {
    return msdsList;
  }

  public void setRollupItemId(String rollupItemId)
  {
    this.rollupItemId = rollupItemId;
  }

  public String getRollupItemId()
  {
    return rollupItemId;
  }

  public void setLevel(BigDecimal level)
  {
    this.level = level;
  }

  public BigDecimal getLevel()
  {
    return level;
  }

  public void setIntermediate(boolean intermediate)
  {
    this.intermediate = intermediate;
  }

  public boolean isIntermediate()
  {
    return intermediate;
  }

  public void setObsolete(boolean obsolete)
  {
    this.obsolete = obsolete;
  }

  public boolean isObsolete()
  {
    return obsolete;
  }
}
