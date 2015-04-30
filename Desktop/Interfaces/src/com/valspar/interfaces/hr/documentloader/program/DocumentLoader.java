package com.valspar.interfaces.hr.documentloader.program;

import com.valspar.interfaces.common.BaseInterface;
import com.valspar.interfaces.common.api.StaffingAPI;
import java.io.File;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
import org.apache.log4j.Logger;

public class DocumentLoader extends BaseInterface
{
  private static Logger log4jLogger = Logger.getLogger(DocumentLoader.class);
  
  public DocumentLoader()
  {
  }
  public void execute()
  {
    try
    {
      log4jLogger.info("Running DocumentLoader");
      
      StaffingAPI staffingAPI = new StaffingAPI();      
      for(File file: FileUtils.listFiles(new File("C:\\data\\noncompete"), null, false))
      {
        log4jLogger.info("Processing "+file.getName());
        staffingAPI.putWorkerDocument(FilenameUtils.removeExtension(file.getName()), file.getName(), "Non-Compete Agreement", FileUtils.readFileToByteArray(file));      
      }      
      log4jLogger.info("Ending DocumentLoader");
    }
    catch(Exception e)
    {
      log4jLogger.error(e);
    }
  }
}
