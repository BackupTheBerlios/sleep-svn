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

import java.io.Serializable;

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

import java.util.Hashtable;
import java.util.Stack;
import java.util.LinkedList;
import java.util.Iterator;

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
 
    /** Creates a forked script instance.  This does not work like fork in an operating system.  Variables are not copied, period.
        The idea is to create a fork that shares the same environment as this script instance. */
    public ScriptInstance fork()
    {
        ScriptInstance si = new ScriptInstance(variables.getGlobalVariables().createInternalVariableContainer(), environment.getEnvironment());
        si.setName("fork of " + getName());

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
       ScriptWarning temp = new ScriptWarning(this, message, line);
 
       Iterator i = watchers.iterator();
       while (i.hasNext())
       {
          ((RuntimeWarningWatcher)i.next()).processScriptWarning(temp);
       }
    }
}


