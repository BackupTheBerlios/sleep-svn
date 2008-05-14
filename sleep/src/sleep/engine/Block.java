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

import sleep.interfaces.Function;
import sleep.runtime.*;
import sleep.bridges.SleepClosure;

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
    /** our first step in this block */
    protected Step  first;

    /** our last step in this block */
    protected Step  last;

    /** an identifier/tag/whatever identifying the source of this block (i.e. somescript.sl) */
    protected String source = "unknown";   

    public Block(String _src)
    {
       source = _src;
    }

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

    /** Returns the source identifier for this block */
    public String getSource()
    {
       return source;
    }

    /** Returns an approximated line number for the steps in this block object...  returns -1 if no code is in this block (unlikely) */
    public int getApproximateLineNumber()
    {
       if (first != null)
          return first.getLineNumber();

       return -1;
    }

    /** return the highest line number associated with this block */
    public int getHighLineNumber()
    {
       int high = 0;
 
       int m;
       Step temp = first;
       while (temp != null)
       {
          m = temp.getHighLineNumber();

          if (m > high)
              high = m;

          temp = temp.next;
       }

       return high;
    }

    /** return the lowest line number associated with this block */
    public int getLowLineNumber()
    {
       int low = Integer.MAX_VALUE;
 
       int m;
       Step temp = first;
       while (temp != null)
       {
          m = temp.getLowLineNumber();

          if (m < low)
              low = m;

          temp = temp.next;
       }

       return low;
    }

    /** Returns an approximate range of line numbers for the steps in this block object.  Useful for formatting error messages in script warnings and such. */
    public String getApproximateLineRange()
    {
       int low  = getLowLineNumber();
       int high = getHighLineNumber();

       if (low == high)
          return low + "";

       return low + "-" + high;
    }

    /** Returns a string representation of where in the source code this block originated from */
    public String getSourceLocation()
    {
       return (new File(source).getName()) + ":" + getApproximateLineRange();
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

    /** handle an exception raised by a script / java object */
    private void handleException(ScriptEnvironment environment)
    {
        if (environment.isResponsible(this))
        {
           Block doit = environment.getExceptionHandler();
           doit.evaluate(environment);
           environment.getScriptInstance().clearStackTrace();
        }
    }

    /** clean up the environment */
    private void cleanupEnvironment(ScriptEnvironment environment)
    {
        /* pop source information from the stack */
        environment.popSource();

        /* remove exception handlers associated with this block (we obviously returned within one */
        if (environment.isResponsible(this))
        {
           environment.popExceptionContext();
        }
    }

    /** evaluates this block of code.  please note that if the block has a return statement and the method clearReturn() is not 
        called on the corresponding script environment chaos will ensue.  use SleepUtils.runCode() to safely execute a block of
        code.  don't call this method yourself.  okay? */
    public Scalar evaluate(ScriptEnvironment environment, Step start)
    {
        if (environment.isReturn())
        {
           if (environment.isThrownValue())
           {
              environment.pushSource(source); /* may not be necessary, but then again, maybe they are... used for stack traces */
              handleException(environment);
              cleanupEnvironment(environment);
           }

           return environment.getReturnValue();
        }

        environment.pushSource(source);

        Step temp = start;
        while (temp != null)
        {
           try
           {
              temp.evaluate(environment);
           }
           catch (Exception ex)
           {
              if (ex instanceof IllegalArgumentException)
              {
                 environment.getScriptInstance().fireWarning(ex.getMessage(), temp.getLineNumber());
              }
              else if (ex instanceof IndexOutOfBoundsException)
              {
                 environment.getScriptInstance().fireWarning("attempted an invalid index", temp.getLineNumber());
              }
              else if (ex instanceof ClassCastException)
              {
                 environment.getScriptInstance().fireWarning("attempted an invalid cast: " + ex.getMessage(), temp.getLineNumber());
              }
              else if (ex instanceof NullPointerException)
              {
                 environment.getScriptInstance().fireWarning("null value error", temp.getLineNumber());
                 ex.printStackTrace();
              }
              else if (ex instanceof RuntimeException)
              {
                 if (ex.getMessage() == null)
                 {
                    environment.getScriptInstance().fireWarning("internal error - " + ex.getClass(), temp.getLineNumber());
//                    ex.printStackTrace(System.err);
                 }
                 else
                 {
                    environment.getScriptInstance().fireWarning(ex.getMessage(), temp.getLineNumber());
                 }
              }
              else
              {
                 environment.getScriptInstance().fireWarning(ex.toString(), temp.getLineNumber());
//                 ex.printStackTrace(System.err);
              }

              cleanupEnvironment(environment);
              return SleepUtils.getEmptyScalar();
           } 

           while (environment.isReturn())
           {
              if (environment.isYield())
              {
                 if (temp instanceof sleep.engine.atoms.Goto)
                 {
                    environment.addToContext(this, temp);
                 }
                 else
                 {
                    environment.addToContext(this, temp.next);
                 }
              }

              if (environment.isCallCC())
              {
                 environment.getCurrentFrame().push(source);
                 environment.getCurrentFrame().push(new Integer(temp.getLineNumber()));
              }

              if (environment.isThrownValue())
              {
                 if (!environment.isExceptionHandlerInstalled())
                 {
                    /* if no handler is installed we will fire a warning and then flag a return of $null so at least the
                       current function fails for not installing a handler */

                    if (!SleepUtils.isEmptyScalar(environment.getReturnValue()))
                    {
                       environment.getScriptInstance().fireWarning("Uncaught exception: " + environment.getExceptionMessage(), temp.getLineNumber());

                       /* the empty throw will cause the current script environment to essentially "exit" */
                       environment.flagReturn(null, ScriptEnvironment.FLOW_CONTROL_THROW);
                    }
                 }
                 else
                 {
                    if (!SleepUtils.isEmptyScalar(environment.getReturnValue())) /* an empty return value means we're exiting */
                    {
                       handleException(environment);
                    }
                 }

                 cleanupEnvironment(environment);
                 return environment.getReturnValue(); /* we do this because the exception will get cleared and after that
                                                         there may be a return value */
              }
              else if (environment.isDebugInterrupt())
              {
                 environment.getScriptInstance().fireWarning(environment.getDebugString(), temp.getLineNumber());
                 /** get debug string clears the debug interrupt! */
              }
              else
              {
                 cleanupEnvironment(environment);
                 return environment.getReturnValue();
              }
           }

           temp = temp.next;
        }

        cleanupEnvironment(environment);
        return SleepUtils.getEmptyScalar(); 
    }
}
