package com.valspar.interfaces.common.enums;

public enum InterfaceEnum
{
  ALLOCATED_PURCHASES("AllocatedPurchasesInterface"),
  CIP_ACE("CIPAceInterface"),
  CLEAN_UP("CleanUpInterface"),
  DEA("DeaInterface"),
  DENSITY_SYNC("DensitySyncInterface"),
  DEALER_BRANDS("DealerBrandsInterface"),
  DOT("DotInterface"),
  DOT_CALC("DotCalcInterface"),
  EOQ_PRODUCTION_USAGE("EOQProductionUsageInterface"),
  EXTRACT_FILLS("ExtractFillsInterface"),
  EXPIRE_VALIDITY_RULES("ExpireValidityRulesInterface"),
  GL_STAT("GLStatInterface"),
  LOCKOUTS("LockoutsInterface"),
  LOWES("LowesInterface"),
  MSDS_REQUEST("MsdsRequestInterface"),
  OBSOLETE_FORMULAS("ObsoleteFormulasInterface"),
  OPTIVA_COSTING("OptivaCostingInterface"),
  OPTIVA_TO_DROMONT("OptivaToDromontInterface"),
  OPTIVA_TO_WERCS_ETL("OptivaToWercsETLInterface"),
  RAW_MATERIALS("RawMaterialsInterface"),
  TSCA("TscaInterface"),
  WERCS_ORDERS("WercsOrdersInterface"),
  WERCS_TO_OPTIVA("WercsToOptivaInterface");
  
  private String interfaceName;

  InterfaceEnum(String name)
  {
    this.setInterfaceName(name);
  }

  public void setInterfaceName(String interfaceName)
  {
    this.interfaceName = interfaceName;
  }

  public String getInterfaceName()
  {
    return interfaceName;
  }
}
