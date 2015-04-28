package com.valspar.interfaces.common;

public interface Constants
{
  // General
  public String EMPTY_STRING      = " ";
  public String MINUS_NINTYNINE   = "-99";
  public String MINUS_ONE         = "-1";
  public String ZERO              = "0";
  public String ONE               = "1";
  public String TWO               = "2";
  public String THREE             = "3";
  public String FOUR              = "4";
  public String FIVE              = "5";
  public String SIX               = "6";
  public String SEVEN             = "7";
  public String EIGHT             = "8";
  public String NINE              = "9";
  public String TEN               = "10";
  public String THIRTYONE         = "31";
  public String THIRTYFOUR        = "34";
  public String THIRTYSEVEN       = "37";
  public String ONEHUNDRED        = "100";
  public String A                 = "A";
  public String J                 = "J";
  public String S                 = "S";
  public String I                 = "I";
  public String F                 = "F";
  public String VAL_FORMAT        = "VAL";
  public String LB                = "LB";
  public String WERCS_USER        = "WERC";
  public String WERCS             = "WERCS";
  public String OPTIVA_USER       = "OPTV";
  public String DOTCALC           = "DOTCALC";
  public String NOT_IN_WERCS_CODE = "-99";
  public String DUMMY             = "DUMMY";
  public String DUMMYBUYOUT       = "DUMMYBUYOUT";
  public String PRODUCTION        = "PRODUCTION";
  public String PP                = "PP";
  public String P                 = "P";
  public String Y                 = "Y";
  public String FALSE             = "false";
  public String TRUE              = "true";
  public String ERROR             = "ERROR";
  public String SUCCESS           = "SUCCESS";
  public String APPROVED          = "APPROVED";

  // Paths
  public String REG_DATA_PATH = "/data/regulatory/";

  // Ship Methods
  public String LAND    = "LAND";
  public String AIR     = "AIR";
  public String WATR    = "WATR";
  public String NONLAND = "NONLAND";

  // Subformats
  public String CPDS_SUBFORMAT = "PDS";

  // Languages
  public String EN_LANG = "EN";

  // Business Groups
  public String CONSUMER_BUSGP = "CONS";

  // Formula Class
  public String WOOD_FORMULA_CLASS = "W";
  public String W    = "W";
  public String WS   = "WS";

  // Databases
  public String REGP = "REGP";
  public String RSUP = "RSUP";
  public String RDEV = "RDEV";
  public String RPRJ = "RPRJ";
  public String PROD = "PROD";
  public String CSUP = "CSUP";
  public String OPRD = "OPRD";
  public String TSUP = "TSUP";
  public String EURO = "EURO";
  public String ESUP = "ESUP";
  public String ASIA = "ASIA";
  public String ASUP = "ASUP";
  public String NAPR = "NAPR";
  public String NADV = "NADV";
  public String NACV = "NACV";
  public String NACF = "NACF";
  public String NASP = "NASP";

  // Datacodes
  public String BUSGP   = "BUSGP";
  public String FRMCT   = "FRMCT";
  public String FRMCLS  = "FRMCLS";//being replaced by FRMCT
  public String UND     = "UND";
  public String TRUN    = "TRUN";//being replaced by UND
  public String SEC     = "SEC";
  public String PGD     = "PGD";
  public String TRPKG   = "TRPKG";//being replaced by PGD
  public String IMPT    = "IMPT";
  public String DMPT    = "DMPT";
  public String TRMPOL  = "TRMPOL";//being replaced by DMPT and IMPT
  public String TRMING  = "TRMING";
  public String TRMING2 = "TRMING2";
  public String TRNBUN  = "TRNBUN";
  public String FPF     = "FPF";
  public String FPC     = "FPC";
  public String FLASHPT = "FLASHPT";//being replaced by FPF
  public String FLASHC  = "FLASHC";//being replaced by FPC
  public String UNI     = "UNI";
  public String TRAUN   = "TRAUN";//being replaced by UNI
  public String PGI     = "PGI";
  public String TRAPK   = "TRAPK";//being replaced by PGI
  public String TRPASS  = "TRPASS";
  public String TRCARG  = "TRCARG";
  public String UNM     = "UNM";
  public String TRWUN   = "TRWUN";//being replaced by UNM
  public String PGM     = "PGM";
  public String TRWPK   = "TRWPK";//being relaced by PGM
  public String TRLNG1  = "TRLNG1";
  public String TRLNG2  = "TRLNG2";
  public String TRANG1  = "TRANG1";
  public String TRANG2  = "TRANG2";
  public String TRWNG1  = "TRWNG1";
  public String TRWNG2  = "TRWNG2";
  public String RQ      = "RQ";
  public String RQQTY   = "RQQTY";
  public String FRSETCD = "FRSETCD";
  public String FRMCODE = "FRMCODE";
  public String FVER    = "FVER";
  public String VERSION = "VERSION";//being replaced by FVER
  public String EXTN    = "EXTN";
  public String VOLDENS = "VOLDENS";
  public String RUL66A  = "RUL66A";
  public String RUL66B  = "RUL66B";
  public String RUL66C  = "RUL66C";
  public String RUL66T  = "RUL66T";
  public String TCADMS  = "TCADMS";
  public String TLEADS  = "TLEADS";
  public String TMERCS  = "TMERCS";
  public String THEXS   = "THEXS";
  public String THVYMTL = "THVYMTL";
  public String VAPORPR = "VAPORPR";
  public String VAPTMP  = "VAPTMP";
  public String VAPTMPC = "VAPTMPC";
  public String VAPORDE = "VAPORDE";
  public String BOILPT  = "BOILPT";
  public String BOILPTC = "BOILPTC";
  public String EVAPRTE = "EVAPRTE";
  public String LLEADSM = "LLEADSM";
  public String EXPTOT  = "EXPTOT";
  public String EXPHGH  = "EXPHGH";
  public String EXPLOW  = "EXPLOW";
  public String TRUNL   = "TRUNL";
  public String TRUNA   = "TRUNA";
  public String TRUNW   = "TRUNW";
  public String TRPSN   = "TRPSN";
  public String TRHAZ   = "TRHAZ";
  public String TRSUB   = "TRSUB";
  public String TRAPS   = "TRAPS";
  public String TRAHZ   = "TRAHZ";
  public String TRASB   = "TRASB";
  public String TRWPS   = "TRWPS";
  public String TRWHZ   = "TRWHZ";
  public String TRWSB   = "TRWSB";

  // EU Datacodes
  public String TRUNUK  = "TRUNUK";
  public String TRIAPSN = "TRIAPSN";
  public String TRIAUN  = "TRIAUN";//being replaced by UNI
  public String HCI     = "HCI";
  public String TRIACL  = "TRIACL";//being replaced by HCI
  public String TRIASR  = "TRIASR";
  public String TRIAPG  = "TRIAPG";
  public String TRIMPSN = "TRIMPSN";
  public String TRIMUN  = "TRIMUN";//being replaced by UNM
  public String HCM     = "HCM";
  public String TRIMCL  = "TRIMCL";//being replaced by HCM
  public String TRIMSR  = "TRIMSR";
  public String TRIMPG  = "TRIMPG";
  public String TRMPL   = "TRMPL";
  public String TRADPSN = "TRADPSN";
  public String HCD     = "HCD";
  public String HCA     = "HCA";
  public String TRADCL  = "TRADCL";//being replaced with HCA
  public String TRADSR  = "TRADSR";
  public String PGA     = "PGA";
  public String TRADPCK = "TRADPCK";//being replaced with PGA
  public String TRING1  = "TRING1";
  public String TRING2  = "TRING2";
  public String VOCGL   = "VOCGL";
  public String TRADSP  = "TRADSP";
  public String ENVHAZA = "ENVHAZA";
  public String ENVHAZB = "ENVHAZB";
  public String MARPOL  = "MARPOL";
  public String DENSITY = "DENSITY"; //being replaced by DENSLB
  public String DENSLB  = "DENSLB";
  public String DENKGL  = "DENKGL";
  public String DENSKG  = "DENSKG";//being replaced by DENKGL
  public String UNA     = "UNA";
  public String TRADUN  = "TRADUN";//being replaced by UNA
  public String VOCPCT  = "VOCPCT";
  public String TND1    = "TND1";
  public String TRADEH1 = "TRADEH1";//being replaced by TND1
  public String TND2    = "TND2";
  public String TRADEH2 = "TRADEH2";//being replaced by TND2
  public String TRIMEH1 = "TRIMEH1";//being replaced by TND1
  public String TRIMEH2 = "TRIMEH2";//being replaced by TND2
  public String TRIAEH1 = "TRIAEH1";//being replaced by TND1
  public String TRIAEH2 = "TRIAEH2";//being replaced by TND2
  public String MIR     = "MIR";
  public String TRCA    = "TRCA";
  public String PSND    = "PSND";
  public String PSNA    = "PSNA";
  public String PSNI    = "PSNI";
  public String PSNM    = "PSNM";
  public String ALQ     = "ALQ";
  public String ITLQ    = "ITLQ";
  public String IMLQ    = "IMLQ";  
  public String SCD     = "SCD";
  public String SCA     = "SCA";
  public String SCI     = "SCI";
  public String SCM     = "SCM";
  public String DERGN    = "DERGN";
  public String DOTHL1  = "DOTHL1";
  public String ADRHL1  = "ADRHL1"; 
  public String IATAHL1 = "IATAHL1";
  public String IMDGHL1 = "IMDGHL1";
  public String DOTHL2  = "DOTHL2";
  public String ADRHL2  = "ADRHL2";
  public String IATAHL2 = "IATAHL2";
  public String IMDGHL2 = "IMDGHL2";
  public String DOTHL3  = "DOTHL3";
  public String ADRHL3  = "ADRHL3";
  public String IATAHL3 = "IATAHL3";
  public String IMDGHL3 = "IMDGHL3";
  public String EMS     = "EMS";
  

  // DotInterface Data Codes //TODO these can go away after we upgrade
  String[] LAND_DATA_CODES = {TRUN,TRADUN,SEC,TRPKG,TRMPOL,TRMING,TRMING2,TRNBUN,FLASHPT,FLASHC,TRADPCK,TRLNG1,TRLNG2,TRADCL,VOCGL,TRADSP,ENVHAZA,ENVHAZB,MARPOL,VOCPCT,TRADEH1,TRADEH2,MIR};
  String[] AIR_DATA_CODES  = {TRAUN,TRUNUK,SEC,TRAPK,TRMPOL,TRMING,TRMING2,TRPASS,TRCARG,TRNBUN,FLASHPT,FLASHC,TRIAUN,TRIAPG,TRANG1,TRANG2,TRIACL,VOCGL,ENVHAZA,ENVHAZB,MARPOL,VOCPCT,TRIAEH1,TRIAEH2,MIR};
  String[] WATR_DATA_CODES = {TRWUN,TRUNUK,SEC,TRWPK,TRMPOL,TRMING,TRMING2,TRNBUN,FLASHPT,FLASHC,TRIMUN,TRIMPG,TRMPL,TRWNG1,TRWNG2,TRIMCL,VOCGL,ENVHAZA,ENVHAZB,MARPOL,VOCPCT,TRIMEH1,TRIMEH2,MIR};
  
  // TransporationInterface Data Codes
 /* String[] TRANS_LAND_DATA_CODES = {DOTHL1,ADRHL1,DOTHL2,ADRHL2,DOTHL3,ADRHL3,UND,UNA,SEC,PGD,SCD,SCA,TRMING,TRMING2,TRPASS,TRCARG,TRNBUN,FPF,FPC,PGA,DERGN,TRLNG1,TRLNG2,HCA,HCD,VOCGL,TRADSP,ENVHAZA,ENVHAZB,MARPOL,VOCPCT,TND1,TND2,MIR,TRCA,ALQ};
  String[] TRANS_AIR_DATA_CODES  = {IATAHL1,IATAHL2,IATAHL3,UNI,TRUNUK,SEC,PGI,SCI,TRMING,TRMING2,TRPASS,TRCARG,TRNBUN,FPF,FPC,UNI,PGI,TRANG1,TRANG2,HCI,VOCGL,ENVHAZA,ENVHAZB,MARPOL,VOCPCT,TND1,TND2,MIR,ITLQ};
  String[] TRANS_WATR_DATA_CODES = {EMS,IMDGHL1,IMDGHL2,IMDGHL3,UNM,TRUNUK,SEC,PGM,SCM,TRMING,TRMING2,TRPASS,TRCARG,TRNBUN,FPF,FPC,TRIMUN,PGM,TRMPL,TRWNG1,TRWNG2,HCM,VOCGL,ENVHAZA,ENVHAZB,MARPOL,VOCPCT,TND1,TND2,MIR,TRCA,IMLQ};
 */
  String[] TRANS_LAND_DATA_CODES = {UND,UNA,SEC,PGD,SCD,SCA,TRMING,TRMING2,TRPASS,TRCARG,TRNBUN,FPF,FPC,PGA,DERGN,TRLNG1,TRLNG2,HCA,HCD,VOCGL,TRADSP,ENVHAZA,ENVHAZB,MARPOL,VOCPCT,TND1,TND2,MIR,TRCA,ALQ};
  String[] TRANS_AIR_DATA_CODES  = {UNI,TRUNUK,SEC,PGI,SCI,TRMING,TRMING2,TRPASS,TRCARG,TRNBUN,FPF,FPC,UNI,PGI,TRANG1,TRANG2,HCI,VOCGL,ENVHAZA,ENVHAZB,MARPOL,VOCPCT,TND1,TND2,MIR,ITLQ};
  String[] TRANS_WATR_DATA_CODES = {EMS,UNM,TRUNUK,SEC,PGM,SCM,TRMING,TRMING2,TRPASS,TRCARG,TRNBUN,FPF,FPC,TRIMUN,PGM,TRMPL,TRWNG1,TRWNG2,HCM,VOCGL,ENVHAZA,ENVHAZB,MARPOL,VOCPCT,TND1,TND2,MIR,TRCA,IMLQ};
   
  // TransporationInterface Text Codes
  String[] TRANS_LAND_TEXT_CODES = {DMPT,IMPT,PSND,PSNA};
  String[] TRANS_AIR_TEXT_CODES  = {DMPT,IMPT,PSNI};
  String[] TRANS_WATR_TEXT_CODES = {DMPT,IMPT,PSNM};


  // Text Codes
  public String TRDSC003 = "TRDSC003";
  public String TRDSC    = "TRDSC";
  public String PYSCL017 = "PYSCL017";
  public String PYSCL016 = "PYSCL016";
  public String PYSCL014 = "PYSCL014";
  public String PHYSST02 = "PHYSST02";
  public String PHYSST01 = "PHYSST01";
  public String PYSCL    = "PYSCL";
  public String PHYSST   = "PHYSST";
  public String CORREV   = "CORREV";
  public String CORREV03 = "CORREV03";
  public String WHMCLS   = "WHMCLS";
  public String WHMCLS13 = "WHMCLS13";
  public String REGION   = "REGION";

  // Set Codes
  public String USA      = "USA";
  public String CAN      = "CAN";

  // Country Codes
  public String US = "US";

  // State Codes
  public String MN = "MN";
  public String MA = "MA";
  public String CA = "CA";
  public String NJ = "NJ";
  public String PA = "PA";

  // RTF Codes
  public String RTF_RETURN_CHAR = " \\par ";

  // Inventory Types
  public String FG = "FG";
  public String RM = "RM";

  // Special Extensions
  public String CO = "CO";

  //Codes for WercsToOptiva
  public String APPROVALCODE      = "approvalcode";
  public String FSXML             = "fsxml";
  public String FORMULA           = "formula";
  public String MAXCOL            = "maxcol";
  public String KEYCOUNT          = "keycount";
  public String FMT               = "fmt";
  public String DTLCODES          = "dtlcodes";
  public String OBJECTKEY         = "objectkey";
  public String KEYCODE           = "keycode";
  public String KEYCODE2          = "keycode2";
  public String DESCRIPTION       = "description";
  public String UOMCODE           = "uomcode";
  public String ITEMCODE          = "itemcode";
  public String YIELD             = "yield";
  public String PHANTOMIND        = "phantomind";
  public String PRIMARYFORMULAIND = "primaryformulaind";
  public String PROCESSYIELD      = "processyield";
  public String FORMULATORCODE    = "formulatorcode";
  public String YIELDCALCIND      = "yieldcalcind";
  public String CLASS             = "class";
  public String DETAIL            = "detail";
  public String LINEID            = "lineid";
  public String QUANTITY          = "quantity";
  public String MATERIALPCT       = "materialpct";
  public String COMPONENTIND      = "componentind";
  public String ALIASCODE         = "aliascode8";
  public String TYPEIND           = "typeind";
  public String INGRROW           = "ingrrow";
  public String STROW             = "strow";
  public String VALSPAR           = "VALSPAR";
  public String RAW               = "RAW";
  public String PERROW            = "perrow";
  public String OBJECTSYMBOL      = "objectsymbol";
  public String OWNERSECURITY     = "ownersecurity";
  public String GROUPSECURITY     = "groupsecurity";
  public String ROLESECURITY      = "rolesecurity";
  public String OWNERCODE         = "ownercode";
  public String CONV              = "CONV";
  public String GROUPCODE         = "groupcode";
  public String JCS               = "JCS";
  public String ITEM              = "item";
  public String CALCIND           = "calcind";
  public String STATUSIND         = "statusind";
  public String SCALEIND          = "scaleind";
  public String SUPPLIER          = "supplier";
  public String CAS               = "cas";
  public String TP0ROW            = "tp0row";
  public String TP1ROW            = "tp1row";
  public String TP3ROW            = "tp3row";
  public String VALUE             = "value";
  public String STATUSROW         = "statusrow";
  public String BULK              = "BULK";
  public String INPUT             = "INPUT";
  public String YIELDPCT          = "yieldpct";
  public String ENABLEIND         = "enableind";
}
