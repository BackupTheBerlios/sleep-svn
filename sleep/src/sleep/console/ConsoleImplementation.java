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
 * <p>The ConsoleImplementation is the "engine" behind the sleep console.  To use the sleep console in your application use 
 * the following steps:</p>
 * 1. Instantiate the console implementation
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
 * <p>When embedding the console reusing the object of an already quitted console is not
 * only allowed but it is also recommended.  When a user quits the console with the quit command
 * the console proxy is set to a dummy console that does not output anything.  To restart
 * a quitted console just set the appropriate proxy again and call the <code>rppl()</code> method.</P>
 *
 * @see sleep.console.ConsoleProxy
 * @see sleep.runtime.ScriptLoader
 * @see sleep.interfaces.Variable
 */
public class ConsoleImplementation implements RuntimeWarningWatcher, Loadable, ConsoleProxy
{
   /** the *active* script... */
   private ScriptInstance script; 

   /** the user installed console proxy, defining all input/output for the console */
   private ConsoleProxy myProxy; 

   /** the script environment with all of the installed functions, predicates, and environments */
   private Hashtable        sharedEnvironment; 

   /** the shared variable container for all scripts, assuming variables are being shared */
   private Variable         sharedVariables; 

   /** the script loader */
   private ScriptLoader     loader; 

   /** Creates an implementation of the sleep console.  The implementation created by this constructor is isolated from your 
       applications environment.  Any scripts loaded via this console will have only the default bridges.  */
   public ConsoleImplementation()
   {
      this(new Hashtable(), new DefaultVariable(), new ScriptLoader());
   }

   /** Creates an implementation of the sleep console that shares what your application is already using.  Any of the 
     * parameters can be null. 
     *
     * <p><font color="red"><b>Warning!</b></font> If you choose to use the Sleep console in your application with this constructor,
     * be aware that even if you don't specify a set of variables or an environment for scripts to share that they will all end up 
     * sharing something as the sleep console will create and install its own environment or variables if you don't specify 
     * something.</p>
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
      loader            = _loader;
      loader.addSpecificBridge(this);

      setProxy(this);
   }

   /** Returns the current console proxy being used */
   public ConsoleProxy getProxy()
   {
      return myProxy;
   }

   /** Sets up the implementation of the consoles input/output facilities */
   public void setProxy(ConsoleProxy p)
   {
      myProxy = p;
   }

   /** Dummy implementation, does nothing really. */
   public void consolePrint(String m) { }

   /** Dummy implementation, always returns null. */
   public String consoleReadln() { return null; }

   /** Dummy implementation, does nothing. */
   public void consolePrintln(Object m) { }

   private boolean interact = true; // are we in interact mode?

   /** starts the console */
   public void rppl() throws IOException
   {
       getProxy().consolePrintln(">> Welcome to the Sleep Scripting Language");

       interact = false;

       String input;
       StringBuffer code = new StringBuffer();

       while (true)
       {
          if (!interact)
             getProxy().consolePrint("> ");

          input = getProxy().consoleReadln();

          if (interact)
          {
             if (input == null || input.equals("done"))
             {
                interact = false;
             }
             else if (input.equals("."))
             { 
                eval(code.toString());
                code = new StringBuffer();                                
             }
             else
             {
                code.append(input + "\n");
             }
          }
          else if (input != null)
          {
             String command, args, filter;
             if (input.indexOf(' ') > -1)
             {
                command = input.substring(0, input.indexOf(' '));
                args    = input.substring(command.length() + 1, input.length());
             }
             else
             {
                command = input;
                args    = null;
             }

             if (command.equals("env"))
             {
                if (args != null && args.indexOf(' ') > -1)
                {
                   filter = args.substring(args.indexOf(' ') + 1, args.length());
                   args   = args.substring(0, args.indexOf(' '));
                }
                else
                {
                   filter = null;
                }

                env(args, filter);
             }
             else if (command.equals("help"))
             {
                help();
             }
             else if (command.equals("interact"))
             {
                interact();
             }
             else if (command.equals("list"))
             {
                list();
             }
             else if (command.equals("load") && args != null)
             {
                load(args);
             }
             else if (command.equals("tree") && (args != null || script != null))
             {
                tree(args);
             }
             else if (command.equals("unload") && args != null)
             {
                unload(args);
             }
             else if (command.equals("x") && args != null)
             {
                eval("println(" + args + ");");
             }
             else if (command.equals("quit") || command.equals("exit") || command.equals("done"))
             {
                getProxy().consolePrintln("Good bye!");
                setProxy(this);
                break;
             }
             else if (command.trim().length() > 0)
             {
                getProxy().consolePrintln("Command '"+command+"' not understood.  Type 'help' if you need it");
             } 
          }
          else
          {
             getProxy().consolePrintln("Good bye!");
             setProxy(this);
             break;
          }
      }

      interact = true;
   }

   private void help()
   {
       getProxy().consolePrintln("env [functions/other] [regex filter]");
       getProxy().consolePrintln("   dumps the shared environment, filters output with specified regex");
       getProxy().consolePrintln("help");
       getProxy().consolePrintln("   displays this message");
       getProxy().consolePrintln("interact");
       getProxy().consolePrintln("   enters the console into interactive mode.");
       getProxy().consolePrintln("list");
       getProxy().consolePrintln("   lists all of the currently loaded scripts");
       getProxy().consolePrintln("load <file>");
       getProxy().consolePrintln("   loads a script file into the script loader");
       getProxy().consolePrintln("unload <file>");
       getProxy().consolePrintln("   unloads a script file from the script loader");
       getProxy().consolePrintln("tree [key]");
       getProxy().consolePrintln("   displays the Abstract Syntax Tree for the specified key");
       getProxy().consolePrintln("quit");
       getProxy().consolePrintln("   stops the console");
       getProxy().consolePrintln("x <expression>");
       getProxy().consolePrintln("   evaluates a sleep expression and displays the value");

   }

   private void load(String file)
   {
       try
       {
          ScriptInstance script = loader.loadScript(file, sharedEnvironment);
          script.runScript();
       }
       catch (YourCodeSucksException yex)
       {
          processScriptErrors(yex);
       }
       catch (Exception ex)
       {
          getProxy().consolePrintln("Could not load script " + file + ": " + ex.getMessage());
       }
   }

   private String getFullScript(String name)
   {
       if (loader.getScriptsByKey().containsKey(name))
       {
          return name;
       }

       Iterator i = loader.getScripts().iterator();
       while (i.hasNext())
       {
          ScriptInstance script = (ScriptInstance)i.next();
          File temp = new File(script.getName());
 
          if (temp.getName().equals(name))
          {
             return temp.getAbsolutePath();
          }
       }

       return name;
   }

   private void unload(String file)
   {
       try
       {
          loader.unloadScript(getFullScript(file));
       }
       catch (Exception ex)
       {
          getProxy().consolePrintln("Could not unloaded script " + file + ": " + ex.getMessage());
       }
   }

   private void list()
   {
       Iterator i = loader.getScripts().iterator();
       while (i.hasNext())
       {
          ScriptInstance temp = (ScriptInstance)i.next();
          getProxy().consolePrintln(temp.getName());
       }
   }

   private void env(String type, String filter)
   {
       Iterator i = sharedEnvironment.keySet().iterator();
       while (i.hasNext())
       {
          Object temp = i.next();
          
          if ( (type == null) || 
               (type.equals("functions") && temp.toString().charAt(0) == '&') ||
               (type.equals("other") && temp.toString().charAt(0) != '&') 
             )
          {
             if (filter == null || java.util.regex.Pattern.matches(".*?" + filter + ".*", sharedEnvironment.get(temp).toString()))
             {
                getProxy().consolePrintln(align(temp.toString(), 20) + " => " + sharedEnvironment.get(temp));
             }
          }
       }
   }

   private String align(String text, int to)
   {
       StringBuffer temp = new StringBuffer(text);
       while (temp.length() < to)
       {
          temp.append(" ");
       }

       return temp.toString();
   }

   private void tree(String item)
   {
       if (item == null)
       {
          getProxy().consolePrintln(script.getRunnableBlock().toString());
       }
       else if (item.charAt(0) == '&' || item.charAt(0) == '$')
       {
          if (sharedEnvironment != null && sharedEnvironment.get(item) instanceof SleepClosure)
          {
             SleepClosure temp = (SleepClosure)sharedEnvironment.get(item);
             getProxy().consolePrintln(temp.getRunnableCode());
          }
          else
          {
             getProxy().consolePrintln("Could not find code block "+item+" to print tree of");
          }
       }
       else
       {
          HashMap temp = loader.getScriptsByKey();

          if (temp.get(getFullScript(item)) != null)
          {
             getProxy().consolePrintln(((ScriptInstance)temp.get(getFullScript(item))).getRunnableBlock());
          }
          else
          {
             getProxy().consolePrintln("Could not find script "+item+" to print tree of");
          }
       }
   }

   private void interact()
   {
       interact = true;
       getProxy().consolePrintln(">> Welcome to interactive mode.");
       getProxy().consolePrintln("Type your code and then '.' on a line by itself to execute the code.");
       getProxy().consolePrintln("Type Ctrl+D or 'done' on a line by itself to leave interactive mode.");
   }

   private void eval (String expression)
   {
       try
       {
          Block parsed = SleepUtils.ParseCode(expression.toString());
          script = loader.loadScript("<interact mode>", parsed, sharedEnvironment);
          script.runScript();
       }
       catch (YourCodeSucksException yex)
       {
          processScriptErrors(yex);
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
      if (! script.getName().equals("<interact mode>") && !interact)
         getProxy().consolePrintln(script.getName() + " loaded successfully.");

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
