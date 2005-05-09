/*
   SLEEP - Simple Language for Environment Extension Purposes
 .---------------------------.
 | sleep.interfaces.Function |________________________________________________
 |                                                                            |
   Author: Raphael Mudge (rsmudge@mtu.edu)
           http://www.csl.mtu.edu/~rsmudge/
 
   Description: An interface for a class that is adding a new function to the
     SLEEP language.
 
   Documentation: 
 
   * This software is distributed under the artistic license, see license.txt
     for more information. *
 
 |____________________________________________________________________________|
 */

package sleep.interfaces;
 
import sleep.runtime.ScriptInstance;
import sleep.runtime.Scalar;

import java.util.Stack;

/**
 * <p>A function bridge is used to define a built-in function.  Once a function bridge is installed into the script 
 * environment, it can be called from user created scripts.</p>
 * 
 * <p>An example of a function bridge:</p>
 * 
 * <pre>
 * public class MyAddFunction implements Function
 * {
 *    public Scalar evaluate(String name, ScriptInstance script, Stack arguments) 
 *    {
 *       if (name.equals("&add"))
 *       {
 *          int a = BridgeUtilities.getInt(arguments, 0);  
 *          int b = BridgeUtilities.getInt(arguments, 0); 
 *  
 *          return SleepUtils.getScalar(a + b); 
 *       }
 * 
 *       return SleepUtils.getEmptyScalar();
 *    }
 * }
 * </pre>
 * 
 * <p>To install a function into a script environment:</p>
 * 
 * <pre>
 * ScriptInstance script;           // assume
 * 
 * Function  myFunctionBridge = new MyAddFunction();
 * Hashtable environment      = script.getScriptEnvironment().getEnvironment();
 * 
 * environment.put("&add", myFunctionBridge);
 * </pre>
 * 
 * <p>In the above code snippet the script environment is extracted from the ScriptInstance object script.  The function 
 * name is the key with the instance of our Function bridge as the value.   The function name must begin with & ampersand 
 * for sleep to know it is a function.</p>
 * 
 * <p>Once a function bridge is installed into the script environment.  The installed function(s) can be called as normal
 * sleep functions i.e.</p>
 * 
 * <code>$var = add(3, 4); # value of $var is now 7</code>
 * 
 * @see sleep.bridges.BridgeUtilities
 * @see sleep.runtime.ScriptInstance
 * @see sleep.runtime.ScriptUtils
 */
public interface Function
{
   /**
    * evaluate a function and return the resulting scalar.
    *
    * @param functionName the function being called.
    * @param anInstance an instance of the script calling this function.
    * @param passedInLocals a stack containing the locals passed to this function.  The locals are Scalar values passed in reverse order i.e. [arg n, arg n-1, ..., arg 1, arg 0]
    *
    * @return an instance of Scalar containing the return value of this function.
    */
   public Scalar evaluate(String functionName, ScriptInstance anInstance, Stack passedInLocals);
}
