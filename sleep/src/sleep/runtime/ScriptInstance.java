/*
   SLEEP - Simple Language for Environment Extension Purposes
 .------------------------------.
 | sleep.runtime.ScriptInstance |_____________________________________________
 |                                                                            |
   Author: Raphael Mudge (rsmudge@mtu.edu)
           http://www.csl.mtu.edu/~rsmudge/

   Description: 
     A class for the purpose of representing an instance of a loaded script
     ready for functions to be called against, ready to be run itself, and 
     ready to have values from its environment extracted.

   Documentation:

   Changelog:

   * This software is distributed under the artistic license, see license.txt
     for more information. *

 |____________________________________________________________________________|
 */

package sleep.runtime;

import sleep.runtime.Scalar;
import sleep.engine.Block;

import sleep.bridges.BasicNumbers;
import sleep.bridges.BasicStrings;
import sleep.bridges.BasicIO;
import sleep.bridges.BasicUtilities;
import sleep.bridges.DefaultEnvironment;
import sleep.bridges.DefaultVariable;
import sleep.bridges.RegexBridge;

import sleep.interfaces.*;
import sleep.error.*;

import sleep.parser.Parser;
import sleep.parser.ParserUtilities;

import java.util.*;
import java.io.*;

/** Every piece of information related to a loaded script.  This includes the scripts runtime environment, code in compiled 
  * form, variable information, and listeners for runtime issues.
  */
public class ScriptInstance implements Serializable, Runnable
{
    /** the name of this script */
    protected String  name  = "Script";

    /** true by default, indicates wether or not the script is loaded.  Once unloaded this variable must be flagged to false so
        the bridges know data related to this script is stale */
    protected boolean loaded; 

    /** A list of listeners watching for a runtime error */
    protected LinkedList watchers = new LinkedList(); 

    /** The script environment which contains all of the runtime info for a script */
    protected ScriptEnvironment environment;

    /** The script variables which contains all of the variable information for a script */
    protected ScriptVariables   variables;

    /** The compiled sleep code for this script, the ScriptLoader will set this value upon loading a script. */
    protected Block             script;

    /** debug should be absolutely quiet, never fire any runtime warnings */
    public static final int DEBUG_NONE          = 0;

    /** fire runtime warnings for all critical flow interrupting errors */
    public static final int DEBUG_SHOW_ERRORS   = 1;

    /** fire runtime warnings for anything flagged for retrieval with checkError() */
    public static final int DEBUG_SHOW_WARNINGS = 2;

    /** fire runtime warning whenever an undeclared variable is fired */
    public static final int DEBUG_REQUIRE_STRICT = 4;

    /** fire a runtime warning describing each function call */
    public static final int DEBUG_TRACE_CALLS    = 8;

    /** forces function call tracing to occur (for the sake of profiling a script) but supresses
        all runtime warnings as a result of the tracing */
    public static final int DEBUG_TRACE_PROFILE_ONLY  = 8 | 16;

    /** track all of the flagged debug options for this script (set to DEBUG_SHOW_ERRORS by default) */
    protected int debug = DEBUG_SHOW_ERRORS;

    /** set the debug flags for this script */
    public void setDebugFlags(int options)
    {
        debug = options;
    }

    /** retrieve the debug flags for this script */
    public int getDebugFlags()  
    {
        return debug;
    }

    /** Constructs a script instance, if the parameter is null a default implementation will be used.
        By specifying the same shared Hashtable container for all scripts, such scripts can be made to
        environment information */
    public ScriptInstance(Hashtable environmentToShare)
    {
        this((Variable)null, environmentToShare);
    }

    /** Constructs a script instance, if either of the parameters are null a default implementation will be used.
        By specifying the same shared Variable and Hashtable containers for all scripts, scripts can be made to
        share variables and environment information */
    public ScriptInstance(Variable varContainerToUse, Hashtable environmentToShare)
    {
        if (environmentToShare == null)
        {
           environmentToShare = new Hashtable();
        }

        if (varContainerToUse == null)
        {
           variables = new ScriptVariables();
        }
        else
        {
           variables = new ScriptVariables(varContainerToUse);
        }

        environment = new ScriptEnvironment(environmentToShare, this);

        loaded = true;
    }

    /** Install a block as the compiled script code */ 
    public void installBlock(Block _script)
    {
        script = _script;
    }

    /** Constructs a new script instance */
    public ScriptInstance()
    {
        this((Variable)null, (Hashtable)null);
    }

    /** Returns this scripts runtime environment */
    public ScriptEnvironment getScriptEnvironment()
    {
        return environment;
    }

    /** Sets the variable container to be used by this script */
    public void setScriptVariables(ScriptVariables v)
    {
        variables = v;
    }

    /** Returns the variable container used by this script */
    public ScriptVariables getScriptVariables()
    {
        return variables;
    }
    
    /** Returns the name of this script (typically a full pathname) as a String */
    public String getName()
    {
        return name;
    }

    /** Sets the name of this script */
    public void setName(String sn)
    {
        name = sn;
    }

    /** Executes this script, should be done first thing once a script is loaded */
    public Scalar runScript()
    {
        script.evaluate(getScriptEnvironment());
        Scalar temp = getScriptEnvironment().getReturnValue();

        getScriptEnvironment().clearReturn();
        return temp;
    }
 
    /** A container for a profile statistic about a sleep function */
    public static class ProfilerStatistic implements Comparable
    {
        /** the name of the function call */
        public String functionName;

        /** the total number of ticks consumed by this function call */
        public long ticks = 0;

        /** the total number of times this function has been called */
        public long calls = 0;

        /** used to compare this statistic to other statistics for the sake of sorting */
        public int compareTo(Object o)
        {
           return (int)(((ProfilerStatistic)o).ticks - ticks);
        }

        /** returns a string in the form of (total time used in seconds)s (total calls made) @(line number) (function description) */ 
        public String toString()
        {
           return (ticks / 1000.0) + "s " + calls + " " + functionName;
        }
    }

    /** this function is used internally by the sleep interpreter to collect profiler statistics
        when DEBUG_TRACE_CALLS or DEBUG_TRACE_PROFILE_ONLY is enabled */
    public void collect(String function, int lineNo, long ticks)
    {
       Map statistics = (Map)getScriptEnvironment().getEnvironment().get("%statistics%");

       if (statistics == null) 
       {
          statistics = new HashMap();
          getScriptEnvironment().getEnvironment().put("%statistics%", statistics);
       }

       ProfilerStatistic stats = (ProfilerStatistic)statistics.get(function);

       if (stats == null)
       {
          stats = new ProfilerStatistic();
          stats.functionName = function;

          statistics.put(function, stats);
       }

       stats.ticks += ticks;
       stats.calls ++;
    }

    /** a quick way to check if we are profiling and not tracing the script steps */
    public boolean isProfileOnly()
    {
       return (getDebugFlags() & DEBUG_TRACE_PROFILE_ONLY) == DEBUG_TRACE_PROFILE_ONLY;
    }

    /** Returns a sorted (in order of total ticks used) list of function call statistics for this
        script environment.  The list contains ScriptInstance.ProfileStatistic objects. 
        Note!!! For Sleep to provide profiler statistics, DEBUG_TRACE_CALLS or DEBUG_TRACE_PROFILE_ONLY must be enabled! */
    public List getProfilerStatistics()
    {
        Map statistics = (Map)getScriptEnvironment().getEnvironment().get("%statistics%");

        if (statistics != null)
        {
           List values = new LinkedList(statistics.values());
           Collections.sort(values);

           return values;
        }
        else
        {
           return new LinkedList();
        }
    }

    /** Dumps the profiler statistics to the specified stream */
    public void printProfileStatistics(OutputStream out)
    {
        PrintWriter pout = new PrintWriter(out, true);

        Iterator i = getProfilerStatistics().iterator();
        while (i.hasNext())
        {
           String temp = i.next().toString();
           pout.println(temp);
        }
    }

    /** Creates a forked script instance.  This does not work like fork in an operating system.  Variables are not copied, period.
        The idea is to create a fork that shares the same environment as this script instance. */
    public ScriptInstance fork()
    {
        ScriptInstance si = new ScriptInstance(variables.getGlobalVariables().createInternalVariableContainer(), environment.getEnvironment());
        si.setName("fork of " + getName());
        si.setDebugFlags(getDebugFlags());
        si.watchers = watchers;
 
        return si;
    }

    /** Executes this script, same as runScript() just here for Runnable compatability */
    public void run()
    {
        Scalar temp = runScript();

        if (parent != null)
        {
           parent.setToken(temp);
        }
    }

    protected sleep.bridges.io.IOObject parent = null;
    
    /** Sets up the parent of this script (in case it is being run via &amp;fork()).  When this script returns a value, the return value will be passed to the parent IOObject to allow retrieval with the &amp;wait function. */
    public void setParent(sleep.bridges.io.IOObject p)
    {
        parent = p;
    }

    /** Returns the compiled form of this script */
    public Block getRunnableBlock()
    {
        return script;
    }

    /** Calls a subroutine/built-in function using this scripts */
    public Scalar callFunction(String funcName, Stack parameters)
    {
       Function myfunction = getScriptEnvironment().getFunction(funcName);

       if (myfunction == null)
       {
          return null;
       }

       Scalar evil = myfunction.evaluate(funcName, this, parameters);
       getScriptEnvironment().clearReturn();

       return evil;
    }

    /** Flag this script as unloaded */
    public void setUnloaded()
    {
       loaded = false;
    }

    /** Returns wether or not this script is loaded.  If it is unloaded it should be removed from data structures and
        its modifications to the environment should be ignored */
    public boolean isLoaded()
    {
       return loaded;
    }

    /** Register a runtime warning watcher listener.  If an error occurs while the script is running these listeners will
        be notified */
    public void addWarningWatcher(RuntimeWarningWatcher w)
    {
       watchers.add(w);
    }

    /** Removes a runtime warning watcher listener */
    public void removeWarningWatcher(RuntimeWarningWatcher w)
    {
       watchers.remove(w);
    }

    /** Fire a runtime script warning */
    public void fireWarning(String message, int line)
    {
       fireWarning(message, line, false);
    }

    /** Fire a runtime script warning */
    public void fireWarning(String message, int line, boolean isTrace)
    {
       if (debug != DEBUG_NONE && (!isTrace || (getDebugFlags() & DEBUG_TRACE_PROFILE_ONLY) != DEBUG_TRACE_PROFILE_ONLY))
       {
          ScriptWarning temp = new ScriptWarning(this, message, line, isTrace);

          Iterator i = watchers.iterator();
          while (i.hasNext())
          {
             ((RuntimeWarningWatcher)i.next()).processScriptWarning(temp);
          }
       }
    }
}



