package com.valspar.interfaces.regulatory.optivaobsoleteexpire.common.beans;

import java.util.ArrayList;

public class ItemBean
{
  private String itemNumber;
  private String formulaId;
  private Boolean northAmerica = false;
  private Boolean emeai = false;
  private Boolean asiapac = false;
  private Boolean bulk = false;
  private ArrayList<ValidityRuleBean> northAmericaValidityRules;
  private ArrayList<ValidityRuleBean> emeaiValidityRules;
  private ArrayList<ValidityRuleBean> asiapacValidityRules;
  
  public ItemBean()
  {
  }
  
  public void setItemNumber(String itemNumber)
  {
    this.itemNumber = itemNumber;
  }

  public String getItemNumber()
  {
    return itemNumber;
  }

  public void setNorthAmerica(Boolean northAmerica)
  {
    this.northAmerica = northAmerica;
  }

  public Boolean isNorthAmerica()
  {
    return northAmerica;
  }

  public void setEmeai(Boolean emeai)
  {
    this.emeai = emeai;
  }

  public Boolean isEmeai()
  {
    return emeai;
  }

  public void setAsiapac(Boolean asiapac)
  {
    this.asiapac = asiapac;
  }

  public Boolean isAsiapac()
  {
    return asiapac;
  }

  public void setBulk(Boolean bulk)
  {
    this.bulk = bulk;
  }

  public Boolean isBulk()
  {
    return bulk;
  }

  public void setNorthAmericaValidityRules(ArrayList<ValidityRuleBean> northAmericaValidityRules)
  {
    this.northAmericaValidityRules = northAmericaValidityRules;
  }

  public ArrayList<ValidityRuleBean> getNorthAmericaValidityRules()
  {
    return northAmericaValidityRules;
  }

  public void setEmeaiValidityRules(ArrayList<ValidityRuleBean> emeaiValidityRules)
  {
    this.emeaiValidityRules = emeaiValidityRules;
  }

  public ArrayList<ValidityRuleBean> getEmeaiValidityRules()
  {
    return emeaiValidityRules;
  }

  public void setAsiapacValidityRules(ArrayList<ValidityRuleBean> asiapacValidityRules)
  {
    this.asiapacValidityRules = asiapacValidityRules;
  }

  public ArrayList<ValidityRuleBean> getAsiapacValidityRules()
  {
    return asiapacValidityRules;
  }

  public void setFormulaId(String formulaId)
  {
    this.formulaId = formulaId;
  }

  public String getFormulaId()
  {
    return formulaId;
  }
}
