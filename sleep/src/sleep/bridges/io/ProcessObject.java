package sleep.bridges.io;

import java.io.*;
import sleep.runtime.*;

public class ProcessObject extends IOObject
{
   protected Process process;

   /** returns the Process object used by this IO implementation */
   public Object getSource()
   {
      return process;
   }

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

   public Scalar wait(ScriptEnvironment env, long timeout)
   {
      if (getThread() != null && getThread().isAlive())
      {
         super.wait(env, timeout);
      }

      try
      {
         process.waitFor();
         return SleepUtils.getScalar(process.waitFor());
      }
      catch (Exception ex)
      {
         env.flagError("wait for process failed: " + ex);
      }

      return SleepUtils.getEmptyScalar();
   }

   public void close()
   {
      super.close();
      process.destroy();
   }
}


