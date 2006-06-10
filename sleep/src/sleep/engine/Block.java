/*
   SLEEP - Simple Language for Environment Extension Purposes
 .---------------------.
 | sleep.engine.Block  |______________________________________________________
 |                                                                            |
   Author: Raphael Mudge (rsmudge@mtu.edu)
           http://www.hick.org/~raffi/
 
   Description: This Block class is meant to serve as a container for a Block
     of parsed and ready to run SLEEP code.  Its main responsibilities are to:
     1. store parsed code
     2. know how to run the stored parsed code
     3. return a value when the executing code says to. 

   Documentation: 
 
   * This software is distributed under the artistic license, see license.txt
     for more information. *
 
 |____________________________________________________________________________|
 */

package sleep.engine;

import java.util.*;
import java.io.*;

import sleep.runtime.*;

/**
 * <p>A Block is the fundamental unit of parsed and ready to execute sleep code.</p>
 * 
 * <p>To execute a block of code:</p>
 * <pre>
 * ScriptInstance    script; // asume
 * Block             code;   // assume
 * 
 * ScriptEnvironment env   = script.getEnvironment();  
 * Scalar            value = SleepUtils.runCode(code, env);
 * </pre>
 * 
 * <p>The variable value would contain the return value after the block was executed.  It is recommended blocks only be run 
 * using {@linkplain sleep.runtime.SleepUtils#runCode(Block, ScriptEnvironment) SleepUtils.runCode()} as there is a little 
 * bit of synchronization and cleanup that has to be done prior to and after executing a block of code.</p>
 * 
 * @see sleep.runtime.Scalar
 * @see sleep.runtime.ScriptEnvironment
 * @see sleep.runtime.ScriptInstance
 */
public class Block implements Serializable
{
    protected Step  first;
    protected Step  last;

    public String toString(String prefix)
    { 
       StringBuffer tempz = new StringBuffer();
       Step temp = first;
       while (temp != null)
       {
          tempz.append(temp.toString(prefix));
          temp = temp.next;
       }
       return tempz.toString();
    }

    /** Returns a string representation of the Abstract Syntax Tree (AST).  An AST represents how the sleep parser interpreted a script string */
    public String toString()
    {
       return toString("");
    }

    /** Returns an approximated line number for the steps in this block object...  returns -1 if no code is in this block (unlikely) */
    public int getApproximateLineNumber()
    {
       if (first != null)
          return first.getLineNumber();

       return -1;
    }

    /** Returns an approximate range of line numbers for the steps in this block object.  Useful for formatting error messages in script warnings and such. */
    public String getApproximateLineRange()
    {
       int high = 0;
       int low  = Integer.MAX_VALUE;
 
       int n;
       Step temp = first;
       while (temp != null)
       {
          n = temp.getLineNumber();
          if (n < low)
              low = n;

          if (n > high)
              high = n;

          temp = temp.next;
       }

       if (low == high)
          return low + "";

       return low + "-" + high;
    }

    public void add(Step n)
    {
       if (first == null)
       {
          first = n;
       }
       else
       {
          last.next = n;
       }

       last = n;
    }

    /** evaluates this block of code.  please note that if the block has a return statement and the method clearReturn() is not 
        called on the corresponding script environment chaos will ensue.  use SleepUtils.runCode() to safely execute a block of
        code.  don't call this method yourself.  okay? */
    public Scalar evaluate(ScriptEnvironment environment)
    {
        return evaluate(environment, first);
    }

    /** evaluates this block of code.  please note that if the block has a return statement and the method clearReturn() is not 
        called on the corresponding script environment chaos will ensue.  use SleepUtils.runCode() to safely execute a block of
        code.  don't call this method yourself.  okay? */
    public Scalar evaluate(ScriptEnvironment environment, Step start)
    {
        if (environment.isReturn())
        {
           return environment.getReturnValue();
        }

        Step temp = start;
        while (temp != null)
        {
           try
           {
              Scalar value = temp.evaluate(environment);
           }
           catch (IllegalArgumentException aex)
           {
              environment.getScriptInstance().fireWarning(aex.getMessage(), temp.getLineNumber());
              return SleepUtils.getEmptyScalar();
           }
           catch (RuntimeException rex)
           {
              environment.getScriptInstance().fireWarning(rex.getMessage(), temp.getLineNumber());
              return SleepUtils.getEmptyScalar();
           }
           catch (Exception ex)
           {
              environment.getScriptInstance().fireWarning(ex.toString(), temp.getLineNumber());
              ex.printStackTrace(System.out);

              return SleepUtils.getEmptyScalar();
           }

           if (environment.isReturn())
           {
              if (environment.getFlowControlRequest() == ScriptEnvironment.FLOW_CONTROL_YIELD)
              {
                 if (temp instanceof sleep.engine.atoms.Return)
                 {
                    environment.addToContext(this, temp.next);
                 }
                 else
                 {
                    environment.addToContext(this, temp);
                 }
              }

              return environment.getReturnValue();
           }

           temp = temp.next;
        }

        return SleepUtils.getEmptyScalar(); 
    }
}
