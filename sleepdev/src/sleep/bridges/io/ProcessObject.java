package sleep.bridges.io;

import java.io.*;
import sleep.runtime.ScriptEnvironment;

public class ProcessObject extends IOObject
{
   protected Process process;

   public void open(String command, ScriptEnvironment env)
   {
      try
      {
         if (command.indexOf(' ') > -1)
         {
            String args;
            args    = command.substring(command.indexOf(' '), command.length());
            command = command.substring(0, command.indexOf(' ')).replace('/', File.separatorChar);
            command = command + args;
         }

         process = Runtime.getRuntime().exec(command);

         openRead(process.getInputStream());
         openWrite(process.getOutputStream());
      }
      catch (Exception ex)
      {
         env.flagError(ex.toString());
      }
   }

   public void close()
   {
      super.close();
      process.destroy();
   }
}


