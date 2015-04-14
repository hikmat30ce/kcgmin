package com.valspar.interfaces.guardsman.pos.utility;

import java.io.*;

public class ShellUtility
{
  private ShellUtility()
  {
  }

  public static String runScript(String... command) throws Exception
  {
    ProcessBuilder pb = new ProcessBuilder(command);
    pb.redirectErrorStream(true);
    Process process = pb.start();

    InputStreamReader in = new InputStreamReader(process.getInputStream());
    BufferedReader br = new BufferedReader(in);

    String line = null;
    StringBuilder sb = new StringBuilder();

    while ((line = br.readLine()) != null)
    {
      sb.append(line);
    }
    br.close();
    in.close();

    process.waitFor();

    return sb.toString();
  }
}
