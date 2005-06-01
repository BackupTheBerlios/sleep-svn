/*
   SLEEP - Simple Language for Environment Extension Purposes
 .----------------------------.
 | sleep.interfaces.Evalation |_______________________________________________
 |                                                                            |
   Author: Raphael Mudge (rsmudge@mtu.edu)
           http://www.csl.mtu.edu/~rsmudge/
 
   Description: An interface for a class that defines a environment for user
     defined functions.   

   Documentation: 
 
   * This software is distributed under the artistic license, see license.txt
     for more information. *
 
 |____________________________________________________________________________|
 */

package sleep.interfaces;
 
import sleep.runtime.ScriptInstance;
import sleep.runtime.Scalar;

/**
 * <p>A Sleep evaluation is a way to define how a `back quoted` string should work.  In Perl any text inside of `back quotes` is
 * fevaluated for embedded $scalar values and then executed as a shell command.  The output of the executed command is collected 
 * into a perl array and returned as the resulting value of the `back quote` expression.</p>
 *
 * <p>While executing commands in this way might be a useful abstraction, it seems more fun to allow you, the application
 * developer to define what this syntax should do.</p>
 *
 * <p>The following is an implementation of perl-like backquote behavior for Sleep:</p>
 * 
 * <pre> import sleep.interfaces.Evaluation;
 *
 * import sleep.runtime.Scalar;
 * import sleep.runtime.ScriptInstance;
 * import sleep.runtime.SleepUtils;
 *
 * import java.io.*;
 *
 * public class PerlLike implements Evaluation
 * {
 *    public Scalar evaluateString(ScriptInstance script, String value)
 *    {
 *       Scalar rv = SleepUtils.getArrayScalar();
 *
 *       try
 *       {
 *          // execute our process and setup a reader for it 
 * 
 *          Process proc  = Runtime.getRuntime().exec(value);
 *          BufferedReader reader = new BufferedReader(new InputStreamReader(proc.getInputStream()));
 *
 *          // read each line from the process output, stuff it into our scalar array rv
 *
 *          String text = null;
 *          while ((text = reader.readLine()) != null)
 *          {
 *             rv.getArray().push(SleepUtils.getScalar(text));
 *          }
 *       }
 *       catch (IOException ex)
 *       {
 *          script.getScriptEnvironment().flagError(ex.toString());
 *       }
 *
 *       return rv;
 *    }
 * }</pre>
 *
 * <p>To install the perl-like backquote evaluator into the script environment:</p>
 * <pre>
 * public boolean scriptLoaded(ScriptInstance script)
 * {
 *    Evaluation perlStuff = new PerlLike();
 * 
 *    Hashtable environment = script.getScriptEnvironment().getEnvironment();
 *    environment.put("%BACKQUOTE%", perlStuff);
 *
 *    return true;
 * }
 * </pre>
 */
public interface Evaluation
{
   /**
    * Evaluate the specified string value.
    *
    * @param typeKeyword the keyword for the function. (i.e. sub)
    * @param functionName the function name (i.e. add)
    * @param functionBody the compiled body of the function (i.e. code to add 2 numbers)
    */
   public abstract Scalar evaluateString(ScriptInstance si, String value);
}
