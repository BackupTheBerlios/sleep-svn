package sleep.console;

import java.io.*;
import java.util.*;

import sleep.interfaces.*;

import sleep.engine.*;
import sleep.error.*;
import sleep.parser.*;
import sleep.runtime.*;

import sleep.bridges.*;

/**
 * The ConsoleImplementation is the "engine" behind the sleep console.  To use the sleep console in your application use 
 * the following steps:
 * <br>
 * <br>1. Instantiate the console implementation
 * <br>
 * <br><code>ConsoleImplementation console;</code>
 * <br><code>console = new ConsoleImplementation(environment, variables, loader);</code>
 * <br>
 * <br>2. Install your implementation of sleep.console.ConsoleProxy into the console
 * <br>
 * <br><code>console.setProxy(new MyConsoleProxy());</code>
 * <br>
 * <br>3. Start the Read, Parse, Print Loop in the console
 * <br>
 * <br><code>console.rppl(); // starts the console</code>
 * 
 * @see sleep.console.ConsoleProxy
 * @see sleep.runtime.ScriptLoader
 * @see sleep.interfaces.Variable
 */
public class ConsoleImplementation implements RuntimeWarningWatcher, Loadable
{
   public static final int DEFAULT    =  0;
   public static final int INPUT_CODE =  1;
   public static final int QUIT       = -1;
   public static final int INTERACT   =  2;

   /** the mode the console is currently in, DEFAULT, INPUT_CODE, or QUIT */
   protected int mode                 = QUIT; 

   /** the *active* script... */
   protected ScriptInstance script; 

   /** the user installed console proxy, defining all input/output for the console */
   protected ConsoleProxy myProxy; 

   /** the script environment with all of the installed functions, predicates, and environments */
   protected Hashtable        sharedEnvironment; 

   /** the shared variable container for all scripts, assuming variables are being shared */
   protected Variable         sharedVariables; 

   /** the script loader */
   protected ScriptLoader     scriptLoader; 

   /** current code loaded into the console so far */
   protected StringBuffer     code; 

   /** Creates an implementation of the sleep console.  The implementation created by this constructor is isolated from your 
       applications environment.  Any scripts loaded via this console will have only the default bridges.  */
   public ConsoleImplementation()
   {
      this(new Hashtable(), new DefaultVariable(), new ScriptLoader());
   }

   /** Creates an implementation of the sleep console that shares what your application is already using.  Any of the 
     * parameters can be null. 
     *
     * @param _sharedEnvironment the environment contains all of the bridges (functions, predicates, and environments)
     * @param _sharedVariables the Variable class is a container for Scalar variables with global, local, and script specific scope
     * @param _loader the Script Loader is a container for managing all of the currently loaded scripts
     */
   public ConsoleImplementation(Hashtable _sharedEnvironment, Variable _sharedVariables, ScriptLoader _loader)
   {
      if (_sharedEnvironment == null)
         _sharedEnvironment = new Hashtable();

      if (_sharedVariables == null)
         _sharedVariables = new DefaultVariable();

      if (_loader == null)
         _loader = new ScriptLoader();

      sharedEnvironment = _sharedEnvironment;
      sharedVariables   = _sharedVariables;
      scriptLoader      = _loader;
      scriptLoader.addSpecificBridge(this);
   }

   public ConsoleProxy getProxy()
   {
      return myProxy;
   }

   /** Sets up the implementation of the consoles input/output facilities */
   public void setProxy(ConsoleProxy p)
   {
      myProxy = p;
   }

   public void append(String text)
   {
      code.append(text);
   }

   public String getCode()
   {
      return code.toString();
   }

   public void clear()
   {
      code = new StringBuffer();
   }

   public int getMode() 
   {
      return mode;
   }

   public boolean canContinue()
   {
      return (getMode() != QUIT);
   }

   protected int lineCount = 0;
   protected boolean showLines = true;

   public void setMode(int m)
   {
      mode = m;
      lineCount = 1;
   }

   public void prompt()
   {
      switch (getMode())
      {
         case DEFAULT:
           getProxy().consolePrint("Enter Command> ");
           break;
         case INPUT_CODE:
         case INTERACT:
           showLineCount();
           break;
         default:
           getProxy().consolePrint("? ");
      }
   }

   public void quickExecute(String command)
   {
      setMode(QUIT);
      handleCommand(command);
   }

   public void handleCommand(String inputstr)
   {
      if (inputstr == null)
      {
         setMode(QUIT);
         return;
      }

      String tempa[] = inputstr.split(" ");
      String command = tempa[0];
      String parms   = "";

      if (tempa.length == 2)
         parms = tempa[1];

      if (command.equals("load") && parms.length() > 0)
      {
         try
         {
            script = scriptLoader.loadScript(parms, sharedEnvironment);
            
            long timed = System.currentTimeMillis();
            script.runScript();
            timed = System.currentTimeMillis() - timed; 

            if (getMode() != QUIT)
               getProxy().consolePrintln("Executed script " + script.getName() + " in " + timed + "ms");
         }
         catch (YourCodeSucksException sucksEx)
         {
            processScriptErrors(sucksEx);
         }
         catch (Exception ex)
         {
            getProxy().consolePrintln(ex.toString());
            ex.printStackTrace();
         }
      }
      else if (command.equals("serialize"))
      {
         try
         {
            script = scriptLoader.loadScript(parms, sharedEnvironment);
            scriptLoader.saveSerialized(script);
            getProxy().consolePrintln("Serialized script to " + parms + ".bin");
         }
         catch (YourCodeSucksException sucksEx2)
         {
            processScriptErrors(sucksEx2);
         }
         catch (Exception ex)
         {
            getProxy().consolePrintln(ex.toString());
            ex.printStackTrace();
         }
      }
      else if (command.equals("bload"))
      {
         try
         {
            for (int x = 0; x < 1; x++)
            {
            long timed = System.currentTimeMillis();
            script = scriptLoader.loadScript(parms, sharedEnvironment);
            getProxy().consolePrintln("Non-serialized loaded in: " + (System.currentTimeMillis() - timed) + "ms");

            timed = System.currentTimeMillis();
            scriptLoader.saveSerialized(script);
            getProxy().consolePrintln("Serialization took: " + (System.currentTimeMillis() - timed) + "ms");

            timed = System.currentTimeMillis();
            File tempf = new File(parms + ".bin");
            script = scriptLoader.loadSerialized(tempf.getName(), new FileInputStream(tempf), sharedEnvironment);
            getProxy().consolePrintln("Serialized loaded in: " + (System.currentTimeMillis() - timed) + "ms");
            getProxy().consolePrintln("---");
            }
         }
         catch (YourCodeSucksException sucksEx2)
         {
            processScriptErrors(sucksEx2);
         }
         catch (Exception ex)
         {
            getProxy().consolePrintln(ex.toString());
            ex.printStackTrace();
         }
      }
      else if (command.equals("unload"))
      {
         scriptLoader.unloadScript(parms);
      }
      else if (command.equals("help"))
      {
         getProxy().consolePrintln("clear dump help interact load tree run - not up to date :)");
      }
      else if (command.equals("interact"))
      {
         getProxy().consolePrintln("Keep entering code hit ^D when your done, use . to parse and run code in buffer");
         setMode(INTERACT);
         clear();
      }
      else if (command.equals("clear"))
      {
         sharedEnvironment.clear();
         sharedVariables   = new DefaultVariable();
      }
      else if (command.equals("env"))
      {
         Enumeration en = script.getScriptEnvironment().getEnvironment().keys();
         while (en.hasMoreElements())
         {
            String key = (String)en.nextElement();
            getProxy().consolePrintln(key + " = " + script.getScriptEnvironment().getEnvironment().get(key).toString());
         }
      }
      else if (command.equals("run"))
      {
         script.runScript();
      }
      else if (command.equals("load"))
      {
         getProxy().consolePrintln("Keep entering code hit ^D when your done");
         setMode(INPUT_CODE);
         showLineCount();
         clear();
      }
      else if (command.equals("tree"))
      {
         getProxy().consolePrintln(script.getRunnableBlock());  
      }
      else if (command.equals("quit") || command.equals("done"))
      {
         getProxy().consolePrintln("Good bye!");
         setMode(QUIT);
      }
      else if (!command.equals(""))
      {
         getProxy().consolePrintln("Unknown command! Type 'help' if you need it.");
      }
   }

   private void showLineCount()
   {
       getProxy().consolePrint(lineCount+": ");
       lineCount++;
   }

   /** starts the console */
   public void rppl() throws IOException
   {
       setMode(DEFAULT);
 
       StringBuffer code = new StringBuffer();
       String text;

       while (canContinue())
       {
          prompt();
          text = getProxy().consoleReadln();

          switch (getMode())
          {
             case DEFAULT: 
                handleCommand(text);
                break;
             case INTERACT:
                if (text != null && !text.equals("."))
                {
                   append(text);                
                   append("\n");
                }
                else
                {
                  try
                  {
                     script = scriptLoader.loadScript("input", getCode(), sharedEnvironment);
                     script.runScript();
                  }
                  catch (YourCodeSucksException yex)
                  {
                     processScriptErrors(yex);
                  }
                  catch (Exception ex)
                  {
                     ex.printStackTrace();
                  }

                  lineCount = 1;
                  clear();
                }

              

                if (text == null)
                {
                   setMode(DEFAULT);
                }
                break;
             case INPUT_CODE:
                if (text != null)
                {
                   append(text);                
                   append("\n");
                   showLineCount();
                }
                else
                {
                  try
                  {
                     script = scriptLoader.loadScript("input", getCode(), sharedEnvironment);
                  }
                  catch (YourCodeSucksException yex)
                  {
                     processScriptErrors(yex);
                  }
                  catch (Exception ex)
                  {
                     ex.printStackTrace();
                  }

                  setMode(DEFAULT);
                }
                break;
             default:
          }
       }
   }

   /** a convienence method that formats and writes each syntax error to the proxy output */
   public void processScriptErrors(YourCodeSucksException ex)
   {
      LinkedList errors = ex.getErrors();
      Iterator i = errors.iterator();
      while (i.hasNext())
      {
         SyntaxError anError = (SyntaxError)i.next();
         getProxy().consolePrintln("Error: " + anError.getDescription() + " at line " + anError.getLineNumber());
         getProxy().consolePrintln("       " + anError.getCodeSnippet());
                         
         if (anError.getMarker() != null)
           getProxy().consolePrintln("       " + anError.getMarker());
      }
   }

   public void processScriptWarning(ScriptWarning warning)
   {
      getProxy().consolePrintln("Warning: " + warning.getMessage() + " at line " + warning.getLineNumber());
   }     

   public boolean scriptLoaded(ScriptInstance script)
   {
      if (getMode() != QUIT)
         getProxy().consolePrintln(script.getName() + " loaded successfully.");

//      sleep.bridges.SwingBridge swing = new sleep.bridges.SwingBridge();
//      swing.scriptLoaded(script);

      script.addWarningWatcher(this);
      script.setScriptVariables(new ScriptVariables(sharedVariables));
      return true;
   }

   public boolean scriptUnloaded(ScriptInstance script)
   {
      getProxy().consolePrintln(script.getName() + " has been unloaded");
      return true;
   }
}
