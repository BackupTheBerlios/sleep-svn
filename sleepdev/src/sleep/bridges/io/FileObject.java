package sleep.bridges.io;

import java.io.*;
import sleep.bridges.BridgeUtilities;
import sleep.runtime.ScriptEnvironment;

public class FileObject extends IOObject
{
   protected File file;

   /** opens a file and references it to this file object.  the descriptor parameter is a filename */
   public void open(String descriptor, ScriptEnvironment env)
   {
      try
      {
         if (descriptor.charAt(0) == '>' && descriptor.charAt(1) == '>')
         {
            file = new File(descriptor.substring(2, descriptor.length()).trim().replace('/', File.separatorChar));
            openWrite(new FileOutputStream(file, true));
         }
         else if (descriptor.charAt(0) == '>')
         {
            file = new File(descriptor.substring(1, descriptor.length()).trim().replace('/', File.separatorChar));
            openWrite(new FileOutputStream(file, false));
         }
         else
         {
            file = new File(descriptor.replace('/', File.separatorChar));
            openRead(new FileInputStream(file));
         }
      }
      catch (Exception ex)
      {
         env.flagError(ex.toString());
      }
   }
}
