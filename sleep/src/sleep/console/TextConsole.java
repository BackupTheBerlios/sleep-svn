package sleep.console;

import java.io.*;
import java.util.*;

import sleep.engine.*;
import sleep.parser.*;
import sleep.runtime.*;
import sleep.error.*;

/** Default implementation of the console proxy class.  Provides a STDIN/STDOUT implementation of the sleep console. */
public class TextConsole implements ConsoleProxy
{
   public static void main(String args[])
   {
      ScriptLoader loader = new ScriptLoader();

      ConsoleImplementation temp = new ConsoleImplementation(null, null, loader);
      temp.setProxy(new TextConsole());

      if (args.length > 0)
      {
         boolean check = false;
         boolean ast   = false;
         int     start = 0;

         if (args[0].equals("-version") || args[0].equals("--version") || args[0].equals("-v"))
         {
             System.out.println(SleepUtils.SLEEP_VERSION + " (" + SleepUtils.SLEEP_RELEASE + ")");
             return;
         } 
         else if (args[0].equals("-help") || args[0].equals("--help") || args[0].equals("-h"))
         {
             System.out.println(SleepUtils.SLEEP_VERSION + " (" + SleepUtils.SLEEP_RELEASE + ")");
             System.out.println("Usage: java [properties] -jar sleep.jar [options] [-|file]");
             System.out.println("       properties:");
             System.out.println("         -Dsleep.debug=<debug level>");
             System.out.println("         -Dsleep.classpath=<path to locate 3rd party jars from>");
             System.out.println("       options:");
             System.out.println("         -a --ast       display the abstract syntax tree of the specified file");
             System.out.println("         -c --check     check the syntax of the specified file");
             System.out.println("         -v --version   display version information");
             System.out.println("         -h --help      display this help message");
             System.out.println("       file:");
             System.out.println("         specify a '-' to read script from STDIN");
             return;
         }
         else if (args[0].equals("--check") || args[0].equals("-c"))
         {
             start = 1;
             check = true;
         }
         else if (args[0].equals("--ast") || args[0].equals("-a"))
         {
             start = 1;
             ast   = true;
         }
         
         //
         // put all of our command line arguments into an array scalar
         //

         Scalar array = SleepUtils.getArrayScalar();
         for (int x = start + 1; x < args.length; x++)
         {
            array.getArray().push(SleepUtils.getScalar(args[x]));
         }

         try
         {
            ScriptInstance script;
            if (args[start].equals("-"))
            {
                script = loader.loadScript("STDIN", System.in);
            }
            else
            {
                script = loader.loadScript(args[start]);     // load the script, parse it, etc.
            }
            script.getScriptVariables().putScalar("@ARGV", array);  // set @ARGV to be our array of command line arguments

            if (System.getProperty("sleep.debug") != null)
            {
               script.setDebugFlags(Integer.parseInt(System.getProperty("sleep.debug")));
            }

            if (check)
            {
               System.out.println(args[start] + " syntax OK");    
            }
            else if (ast)
            {
               System.out.println(script.getRunnableBlock());
            } 
            else
            {
               script.runScript();                                     // run the script...
            }
         }
         catch (YourCodeSucksException yex)
         {
            // deal with all of our syntax errors, I'm using the console as a convienence
            temp.processScriptErrors(yex);
         }
         catch (Exception ex)
         {
            ex.printStackTrace();
         }
      }
      else
      {
         try
         {
            temp.rppl();
         }
         catch (Exception ex)
         {
            ex.printStackTrace();
         }
      }
   }

   public void consolePrint(String message)
   {
      System.out.print(message);
   }

   public void consolePrintln(Object message)
   {
      System.out.println(message.toString());
   }

   public String consoleReadln()
   {
      try
      {
         return in.readLine();
      }
      catch (Exception ex)
      {
         ex.printStackTrace();
         return null;
      }
   }
   
   BufferedReader in;

   public TextConsole()
   {
      in = new BufferedReader(new InputStreamReader(System.in));
   }
}
