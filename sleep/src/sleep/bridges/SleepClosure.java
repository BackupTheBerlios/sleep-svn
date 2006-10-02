/*
   SLEEP - Simple Language for Environment Extension Purposes
 .----------------------------.
 | sleep.bridges.SleepClosure |_______________________________________________
 |                                                                            |
   Author: Raphael Mudge (raffi@hick.org)
           http://www.hick.org/~raffi/

   Description:
        Implementation of a Sleep Closure.

   Documentation:

   Changelog:

   * This software is distributed under the artistic license, see license.txt
     for more information. *

 |____________________________________________________________________________|
 */

package sleep.bridges;
 
import java.util.*;
import java.io.*;

import sleep.engine.*;
import sleep.engine.types.*;
import sleep.interfaces.*;
import sleep.runtime.*;

/** The Sleep Closure class.  This class represents a Function object that is also a self contained closure */
public class SleepClosure implements Function
{
    private class ClosureIterator implements Iterator
    {
       protected Scalar            current;
       protected Stack             locals = new Stack();

       public boolean hasNext()
       {
          current = callClosure("eval", null, locals);
          return !SleepUtils.isEmptyScalar(current);
       }

       public Object next()
       {
          return current;
       }

       public void remove()
       {
       }
    }

    public Iterator scalarIterator()
    {
       return new ClosureIterator();
    }

    public static Class CLOSURE_CLASS;

    static
    {
       try
       {
          CLOSURE_CLASS = Class.forName("sleep.bridges.SleepClosure");
       }
       catch (Exception ex) { }
    }

    /** the block of code associated with this sleep closure */
    Block                code;

    /** the owning script associated with this sleep closure */
    ScriptInstance      owner;

    /** the saved context of this closure */
    Stack             context;

    /** the meta data for this closure context */
    HashMap          metadata; 

    /** the closure variables referenced by this closure */
    Variable         variables;

    /** saves the top level context */
    private void saveToplevelContext(Stack _context, Variable localLevel)
    {
       if (!_context.isEmpty())
       {
          _context.push(localLevel); /* push the local vars on to the top of the context stack,
                                        this better be popped before use!!! */
          context.push(_context);
       }
    }

    /** returns the top most context stack... */
    private Stack getToplevelContext()
    {
       if (context.isEmpty())
       {
          return new Stack();
       }
       return (Stack)context.pop();
    }

    public String toString()
    {
       return "&closure" + hashCode();
    }

    /** Creates a new Sleep Closure, with a brand new set of internal variables.  Don't be afraid, you can call this constructor from your code. */
    public SleepClosure(ScriptInstance si, Block _code)
    {
       this(si, _code, si.getScriptVariables().getGlobalVariables().createInternalVariableContainer());
    }
  
    /** Creates a new Sleep Closure that uses the specified variable container for its internal variables */
    public SleepClosure(ScriptInstance si, Block _code, Variable _var)
    {
       code      = _code;
       owner     = si;
       context   = new Stack();
       metadata  = new HashMap();

       _var.putScalar("$this", SleepUtils.getScalar(this));
       setVariables(_var);
    }

    /** Returns the owning script instance */
    public ScriptInstance getOwner()
    {
       return owner;
    }

    /** Returns the runnable block of code associated with this closure */
    public Block getRunnableCode()
    {
       return code;
    }

    /** Returns the variable container for this closures */
    public Variable getVariables()
    {
       return variables;
    }

    /** Sets the variable environment for this closure */
    public void setVariables(Variable _variables)
    {
       variables = _variables; 
    }

    /** "Safely" calls this closure.  Use this if you are evaluating this closure from your own code. 

        @param message the message to pass to this closure (available as $0)
        @param the calling script instance (null value assumes same as owner)
        @param the local data as a stack object (available as $1 .. $n)

        @return the scalar returned by this closure
     */
    public Scalar callClosure(String message, ScriptInstance si, Stack locals)
    {
       if (si == null)
           si = getOwner();

       if (locals == null)
           locals = new Stack();

       Scalar temp = evaluate(message, si, locals);
       si.getScriptEnvironment().clearReturn();
       return temp;
    }

    /** Evaluates the closure, use callClosure instead. */
    public Scalar evaluate(String message, ScriptInstance si, Stack locals)
    {
       ScriptVariables   vars = si.getScriptVariables();
       ScriptEnvironment env  = si.getScriptEnvironment();

       Variable          localLevel;

       Scalar temp; // return value of subroutine.

       synchronized (vars)
       {
          Stack toplevel = getToplevelContext();
          env.loadContext(toplevel, metadata);

          vars.pushClosureLevel(getVariables()); 

          if (toplevel.isEmpty()) /* a normal closure call */
          {
             vars.pushLocalLevel();
             localLevel = vars.getLocalVariables();
          }
          else /* restoring from a coroutine */
          {
             localLevel = (Variable)toplevel.pop();
             vars.pushLocalLevel(localLevel);
          }

          vars.setScalarLevel("$0", SleepUtils.getScalar(message), localLevel);

          //
          // setup the parameters from the stack based since this is
          // the default function environment.
          //
          int name = 1;
          while (!locals.isEmpty())
          {
             Scalar lvar = (Scalar)locals.pop();

             if (lvar.getValue() != null && lvar.getValue().getClass() == ObjectValue.class && lvar.getValue().objectValue() != null && lvar.getValue().objectValue().getClass() == KeyValuePair.class)
             {
                KeyValuePair kvp = (KeyValuePair)lvar.getValue().objectValue();

                if (kvp.getKey().toString().charAt(0) != '$')
                {
                   throw new IllegalArgumentException("unreachable named parameter: " + kvp.getKey());
                }
                else
                {
                   vars.setScalarLevel(kvp.getKey().toString(), kvp.getValue(), localLevel);
                }
             } 
             else
             {
                vars.setScalarLevel("$"+name, lvar, localLevel);
                name++;
             }
          }

          vars.setScalarLevel("@_", SleepUtils.getArrayScalar(new ArgumentArray(name, localLevel)), localLevel);

          //
          // call the function, save the scalar that was returned. 
          //
          if (toplevel.isEmpty())
          {
             temp = code.evaluate(env);
          }
          else
          {
             temp = env.evaluateOldContext();
          }

          saveToplevelContext(env.saveContext(), localLevel);

          vars.popLocalLevel();
          vars.popClosureLevel();
       }

       return temp;
    }
}

