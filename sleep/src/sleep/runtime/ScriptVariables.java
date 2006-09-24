/*
   SLEEP - Simple Language for Environment Extension Purposes
 .-------------------------------.
 | sleep.runtime.ScriptVariables |____________________________________________
 |                                                                            |
   Author: Raphael Mudge (rsmudge@mtu.edu)
           http://www.csl.mtu.edu/~rsmudge/

   Description: 
      A class for managing variable scopes for sleep.  

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

import sleep.bridges.*;
import sleep.interfaces.*;

import sleep.parser.Parser;
import sleep.parser.ParserUtilities;

import java.util.Hashtable;
import java.util.Stack;
import java.util.LinkedList;
import java.util.WeakHashMap;

/** Maintains variables and variable scopes for a script instance.  If you want to change the way variables are handled do not 
  * override this class.  This class handles all accessing of variables through an object that implements the Variable 
  * interface.  
  *
  * <p><b>Set/Get a Variable without Parsing</b></p>
  * 
  * <code>script.getScriptVariables().putScalar("$var", SleepUtils.getScalar("value"));</code>
  * 
  * <p>The ScriptVariables object is the entry point for installing variables into
  * a script's runtime environment.  The above example illustrates how to set a
  * variable named $var to a specified Scalar value.</p>
  *
  * <code>Scalar value  = script.getScriptVariables().getScalar("$var");</code>
  *
  * <p>The code above illustrates how to retrieve a Scalar named $var from a 
  * script instance object.</p>
  *
  * <p>Sleep has 3 levels of scope.  They are (in order of precedence):</p>
  * <li>Local   - discarded after use</li>
  * <li>Closure - specific to the current executing closure</li>
  * <li>Global  - global to all scripts sharing this script variables instance</li>
  * 
  * @see sleep.runtime.Scalar
  * @see sleep.runtime.ScriptInstance
  * @see sleep.interfaces.Variable
  */
public class ScriptVariables implements Serializable
{
    Variable    global;   /* global variables */
    LinkedList  closure;  /* closure specific variables :) */
    LinkedList  locals;   /* local variables */
    WeakHashMap cscopes;  /* closure scope storage */

    /** Initializes this ScriptVariables container using a DefaultVariable object for default variable storage */
    public ScriptVariables()
    {
        this(new DefaultVariable());
    }

    /** Initializes this class with your version of variable storage */
    public ScriptVariables(Variable aVariableClass)
    {
       global   = aVariableClass;
       closure  = new LinkedList();
       locals   = new LinkedList();
       cscopes  = new WeakHashMap();

       pushLocalLevel();
    }

    /** puts a scalar into the global scope */ 
    public void putScalar(String key, Scalar value)
    {
       global.putScalar(key, value);
    }

    /** retrieves a scalar */
    public Scalar getScalar(String key)
    {
       return getScalar(key, null);
    }

    /** retrieves the appropriate Variable container that has the specified key.  Precedence is in the order of the current
        local variable container, the script specific container, and then the global container */
    public Variable getScalarLevel(String key, ScriptInstance i)
    {
       Variable temp;

       //
       // check local variables for an occurence of our variable
       //
       temp = getLocalVariables();
       if (temp != null && temp.scalarExists(key))
       {
          return temp; 
       }

       //
       // check closure specific variables for an occurence of our variable
       //
       temp = getClosureVariables();
       if (temp != null && temp.scalarExists(key))
       {
          return temp;
       }

       //
       // check the global variables
       //
       temp = getGlobalVariables();
       if (temp.scalarExists(key))
       {
          return temp;
       }

       return null;
    }

    /** Returns the specified scalar, looking at each scope in order.  It is worth noting that only one local variable level is    
        qeuried.  If a variable is not local, the previous local scope is not checked.  */
    public Scalar getScalar(String key, ScriptInstance i)
    {     
       Variable temp = getScalarLevel(key, i);

       if (temp != null)
          return temp.getScalar(key);

       return null;
    }

    /** Puts the specified scalar in a specific scope
      * @param level the Variable container from the scope we want to store this scalar in.
      */
    public void setScalarLevel(String key, Scalar value, Variable level)
    {
       level.putScalar(key, value);
    }

    /** returns the current local variable scope */
    public Variable getLocalVariables()
    {
       return (Variable)locals.getFirst();
    }

    /** returns the current closure variable scope */
    public Variable getClosureVariables()
    {
       if (closure.size() == 0)
           return null;

       return (Variable)closure.getFirst();
    }

    /** returns the global variable scope */
    public Variable getGlobalVariables()
    {
       return global;
    }

    /** returns the closure level variables for this specific script environment */
    public Variable getClosureVariables(SleepClosure closure)
    {
       Object temp = cscopes.get(closure);
       if (temp == null)
       {
          temp = global.createInternalVariableContainer();
          cscopes.put(closure, temp);
       }

       return (Variable)temp;       
    }

    /** returns the closure level variables for this specific script environment */
    public void setClosureVariables(SleepClosure closure, Variable variables)
    {
       cscopes.put(closure, variables);
    }

    /** makes the specified closure variable scope active, once the closure has executed this should be popped */
    public void pushClosureLevel(SleepClosure level)
    {
       closure.addFirst(getClosureVariables(level));
    }

    /** discards the current closure variable scope */
    public void popClosureLevel()
    {
       closure.removeFirst();
    }

    /** makes the specified variable container active for the local scope.  once the code that is using this has finished, it really should be popped. */
    public void pushLocalLevel(Variable localVariables)
    {
       locals.addFirst(localVariables);
    }

    /** starts a new local variable scope.  once the code that is using this has finished, it should be popped */
    public void pushLocalLevel()
    {
       locals.addFirst(global.createLocalVariableContainer());
    }

    /** discards the current local variable scope, making the previous local scope the current local scope again */
    public void popLocalLevel()
    {
       locals.removeFirst();
    }
}
