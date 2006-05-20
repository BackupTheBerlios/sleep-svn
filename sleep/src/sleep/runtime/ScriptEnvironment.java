/*
   SLEEP - Simple Language for Environment Extension Purposes
 .---------------------------------.
 | sleep.runtime.ScriptEnvironment |__________________________________________
 |                                                                            |
   Author: Raphael Mudge (rsmudge@mtu.edu)
           http://www.csl.mtu.edu/~rsmudge/

   Description: 
     A class for the purpose of representing a script environment.  Everything
     was hickeldy pickeldy up until this was created.

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
import sleep.bridges.BasicUtilities;
import sleep.bridges.DefaultEnvironment;

import sleep.interfaces.*;

import sleep.parser.Parser;
import sleep.parser.ParserUtilities;

import java.util.*;
import java.util.Hashtable;
import java.util.Stack;

import sleep.runtime.SleepUtils;

import sleep.error.YourCodeSucksException;

/**
 * <p>This class contains methods for accessing the data stack, return value of a function, and the environment hashtable 
 * for a script.  In sleep each ScriptInstance has a ScriptEnvironment object associated with it.  Most of the functions in 
 * this class are used internally by sleep.</p>
 *
 * <p>For the developers purposes, this class is your gateway into the runtime environment of a script.</p>
 * 
 * <p>If you use the evaluate methods to evaluate a snippet of code, they will be evaluated as if they were part of the 
 * script file that this ScriptEnvironment represents.</p>
 * 
 * <p>The Hashtable environment contains references for all of the loaded bridges this script has access to.  Every 
 * function, predicate, and operator is specified in the environment hashtable.  To force scripts to share this information 
 * use setEnvironment(Hashtable) and pass the same instance of Hashtable that other scripts are using.</p>
 *
 * <p>This class is instantiated by sleep.runtime.ScriptInstance.</p>
 *
 * @see sleep.runtime.ScriptLoader
 * @see sleep.runtime.ScriptInstance
 */
public class ScriptEnvironment implements Serializable
{
    /** the script instance that this is the environment for */
    protected ScriptInstance  self;  

    /** the runtime data stack for this environment */
    protected Stack           environmentStack;

    /** the environment hashtable that contains all of the functions, predicates, operators, and "environment keywords" this 
        script has access to. */
    protected Hashtable       environment;

    /** Not recommended that you instantiate a script environment in this way */
    public ScriptEnvironment()
    {
       self        = null;
       environment = null;
       environmentStack = new Stack();
    }

    /** Instantiate a new script environment with the specified environment (can be shared), and the specified ScriptInstance */
    public ScriptEnvironment(Hashtable env, ScriptInstance myscript)
    {
       self        = myscript;
       environment = env;
       environmentStack = new Stack();
    }

    /** returns a reference to the script associated with this environment */
    public ScriptInstance getScriptInstance()
    {
       return self;
    }

    /** stored error message... */
    protected String errorMessage = null;

    /** A utility for bridge writers to flag an error.  flags an error that script writers can then check for with checkError().  
        Currently used by the IO bridge openf, exec, and connect functions.  Major errors should bubble up as exceptions.  Small 
        stuff like being unable to open a certain file should be flagged this way. */
    public void flagError(String message)
    {
       errorMessage = message;
//       getScriptVariables().putScalar("$!", SleepUtils.getScalar(message));
    }

    /** once an error is checked using this function, it is cleared, the orignal error message is returned as well */
    public String checkError()
    {
       String temp  = errorMessage;
       errorMessage = null;
       return temp;
    }

    /** returns the variable manager for this script */
    public ScriptVariables getScriptVariables()
    {
       return getScriptInstance().getScriptVariables();
    }

    /** returns a scalar from this scripts environment */
    public Scalar getScalar(String key)
    {
       return getScriptVariables().getScalar(key, getScriptInstance());
    }

    /** puts a scalar into this scripts environment */
    public void putScalar(String key, Scalar value)
    {
       getScriptVariables().putScalar(key, value);
    }
 
    public Function getFunction(String func)
    {
       return (Function)(getEnvironment().get(func));
    }

    public Environment getFunctionEnvironment(String env)
    {
       return (Environment)(getEnvironment().get(env));
    }

    public PredicateEnvironment getPredicateEnvironment(String env)
    {
       return (PredicateEnvironment)(getEnvironment().get(env));
    }

    public FilterEnvironment getFilterEnvironment(String env)
    {
       return (FilterEnvironment)(getEnvironment().get(env));
    }

    public Predicate getPredicate(String name)
    {
       return (Predicate)(getEnvironment().get(name));
    }

    public Operator getOperator(String oper)
    {
       return (Operator)(getEnvironment().get(oper));
    }

    /**
     * Returns the environment for this script.
     * The environment has the following formats for keys:
     * &amp;[keyname] - a sleep function
     * -[keyname] - assumed to be a unary predicate
     * [keyname]  - assumed to be an environment binding, predicate, or operator
     */
    public Hashtable getEnvironment()
    {
       return environment;
    }

    /** Sets the environment Hashtable this script is to use.  Sharing an instance of this Hashtable allows scripts to share 
        common environment data like functions, subroutines, etc.   Also useful for bridge writers as their information can be
        stored in this hashtable as well */
    public void setEnvironment(Hashtable h)
    {
       environment = h;
    }
 
    /** returns the environment stack used for temporary calculations and such. */
    public Stack getEnvironmentStack()
    {
       return environmentStack;
    }

    public String toString()
    {
       StringBuffer temp = new StringBuffer();
       temp.append("ScriptInstance -- " + getScriptInstance());
       temp.append("Misc Environment:\n");
       temp.append(getEnvironment().toString()); 
       temp.append("\nEnvironment Stack:\n");
       temp.append(getEnvironmentStack().toString());
       temp.append("Return Stuff: " + rv); 

       return temp.toString();
    }

    //
    // ******** Flow Control **********
    //

    /** currently no flow contrl change has been requested */
    public static final int FLOW_CONTROL_NONE     = 0;

    /** request a return from the current function */
    public static final int FLOW_CONTROL_RETURN   = 1;

    /** request a break out of the current loop */
    public static final int FLOW_CONTROL_BREAK    = 2;

    /** adding a continue keyword as people keep demanding it */
    public static final int FLOW_CONTROL_CONTINUE = 3;
    
    protected Scalar rv      = null;
    protected int    request = 0;

    public Scalar getReturnValue()
    {
       return rv;
    }

    public boolean isReturn()
    {
       return request != FLOW_CONTROL_NONE;
    }

    public int getFlowControlRequest()
    {
       return request;
    }

    public void flagReturn(Scalar value, int type_of_flow)
    {
       if (value == null) { value = SleepUtils.getEmptyScalar(); }
       rv      = value;
       request = type_of_flow;
    }

    public void clearReturn()
    {
       request = FLOW_CONTROL_NONE;
       rv      = null;
    }

    //
    // stuff related to frame management
    //
    protected ArrayList frames = new ArrayList(10);
    protected int       findex = -1;

    public Stack getCurrentFrame()
    {
       return (Stack)frames.get(findex);    
    }

    public void KillFrame()
    {
       getCurrentFrame().clear();
       findex--;
    }
    
    public void CreateFrame()
    {
       if ((findex + 1) >= frames.size())
       {
          frames.add(new Stack());
       } 

       findex++;
    }

    /** evaluate a full blown statement... probably best to just load a script at this point */
    public Scalar evaluateStatement(String code) throws YourCodeSucksException
    {
       return SleepUtils.runCode(SleepUtils.ParseCode(code), this);
    }

    /** evaluates a predicate condition */
    public boolean evaluatePredicate(String code) throws YourCodeSucksException
    {
       code = "if (" + code + ") { return 1; } else { return $null; }";
       return (SleepUtils.runCode(SleepUtils.ParseCode(code), this).intValue() == 1);
    }

    /** evaluates an expression */
    public Scalar evaluateExpression(String code) throws YourCodeSucksException
    {
       code = "return (" + code + ");";
       return SleepUtils.runCode(SleepUtils.ParseCode(code), this);
    }

    /** evaluates the passed in code as if it was a sleep parsed literal */
    public Scalar evaluateParsedLiteral(String code) throws YourCodeSucksException
    {
       code = "return \"" + code + "\";";
       return SleepUtils.runCode(SleepUtils.ParseCode(code), this);
    }
}
