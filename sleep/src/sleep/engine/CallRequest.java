package sleep.engine;

import sleep.engine.*;
import sleep.runtime.*;

import java.util.*;
import sleep.interfaces.*;
import sleep.engine.types.*;
import sleep.bridges.SleepClosure;

/** this class encapsulates a function call request. sleep has so many reasons, places, and ways to call functions.
    this class helps to avoid duplicate code and manage the complexity of Sleep's myriad of profiling, tracing, and error reporting
    options. */
public abstract class CallRequest
{
   protected ScriptEnvironment environment;
   protected int               lineNumber;

   /** initialize a new call request */
   public CallRequest(ScriptEnvironment e, int lineNo)
   {
      environment = e;
      lineNumber  = lineNo;
   }

   /** returns the script environment... pHEAR */
   protected ScriptEnvironment getScriptEnvironment()
   {
      return environment;      
   }

   /** returns the line number this function call is occuring from */
   public int getLineNumber()
   {
      return lineNumber;
   }

   /** return the name of the function (for use in profiler statistics) */
   public abstract String getFunctionName();

   /** return the description of this current stack frame in the event of an exception */
   public abstract String getFrameDescription();

   /** execute the function call contained here */
   protected abstract Scalar execute();

   /** return a string view of this function call for trace messages; arguments are captured as comma separated descriptions of all args */
   protected abstract String formatCall(String args);

   /** return true if debug trace is enabled.  override this to add/change criteria for trace activiation */
   public boolean isDebug()
   {
      return (getScriptEnvironment().getScriptInstance().getDebugFlags() & ScriptInstance.DEBUG_TRACE_CALLS) == ScriptInstance.DEBUG_TRACE_CALLS;
   }

   /** actually execute the function call */
   public void CallFunction()
   {
      Scalar temp = null;
      ScriptEnvironment e = getScriptEnvironment();
      int mark = getScriptEnvironment().markFrame();

      if (isDebug())
      {
         if (e.getScriptInstance().isProfileOnly())
         {
             try
             {
                long stat = System.currentTimeMillis();
                temp = execute();
                stat = System.currentTimeMillis() - stat;
                e.getScriptInstance().collect(getFunctionName(), getLineNumber(), stat);
             }
             catch (RuntimeException rex)
             {
                if (rex.getCause() == null || ! (  (java.lang.reflect.InvocationTargetException.class).isInstance(rex.getCause())  ))
                {
                   /* swallow invocation target exceptions please */

                   e.cleanFrame(mark);
                   e.KillFrame();
                   throw(rex);
                }
             }
         }
         else
         {
             String args = SleepUtils.describe(e.getCurrentFrame());

             try
             {
                long stat = System.currentTimeMillis();
                temp = execute();
                stat = System.currentTimeMillis() - stat;
                e.getScriptInstance().collect(getFunctionName(), getLineNumber(), stat);

                if (e.isThrownValue())
                {
                   e.getScriptInstance().fireWarning(formatCall(args) + " - FAILED!", getLineNumber(), true);
                }
                else if (e.isPassControl())
                {
                   e.getScriptInstance().fireWarning(formatCall(args) + " ... " + SleepUtils.describe(temp), getLineNumber(), true);
                }
                else if (SleepUtils.isEmptyScalar(temp))
                {
                   e.getScriptInstance().fireWarning(formatCall(args), getLineNumber(), true);
                }
                else
                {
                   e.getScriptInstance().fireWarning(formatCall(args) + " = " + SleepUtils.describe(temp), getLineNumber(), true);
                }
             }
             catch (RuntimeException rex)
             {
                e.getScriptInstance().fireWarning(formatCall(args) + " - FAILED!", getLineNumber(), true);

                if (rex.getCause() == null || ! (  (java.lang.reflect.InvocationTargetException.class).isInstance(rex.getCause())  ))
                {
                   /* swallow invocation target exceptions please */

                   e.cleanFrame(mark);
                   e.KillFrame();
                   throw(rex);
                }
             }
         }
      }
      else
      {
         try
         {
             temp = execute();
         }
         catch (RuntimeException rex)
         {
             if (rex.getCause() == null || ! (  (java.lang.reflect.InvocationTargetException.class).isInstance(rex.getCause())  ))
             {
                 /* swallow invocation target exceptions please */

                e.cleanFrame(mark);
                e.KillFrame();
                throw(rex);
             }
         }
      }

      if (e.isThrownValue())
      {
         e.getScriptInstance().recordStackFrame(getFrameDescription(), getLineNumber());
      }

      if (temp == null)
        temp = SleepUtils.getEmptyScalar();

      e.cleanFrame(mark);
      e.FrameResult(temp);
   }

   /** execute a closure with all of the trimmings. */
   public static class ClosureCallRequest extends CallRequest
   {
      protected String name;
      protected Scalar scalar;

      public ClosureCallRequest(ScriptEnvironment e, int lineNo, Scalar _scalar, String _name)
      {
         super(e, lineNo);
         scalar = _scalar;
         name   = _name;
      }

      public String getFunctionName()
      {
         return ((SleepClosure)scalar.objectValue()).toStringGeneric();
      }

      public String getFrameDescription()
      {
         return scalar.toString();
      }

      public String formatCall(String args)
      {
         StringBuffer buffer = new StringBuffer("[" + SleepUtils.describe(scalar));

         if (name != null && name.length() > 0)
         {
            buffer.append(" " + name);
         }

         if (args.length() > 0)
         {
            buffer.append(": " + args);
         }

         buffer.append("]");

         return buffer.toString();
      }

      protected Scalar execute()
      {
         Function func = SleepUtils.getFunctionFromScalar(scalar, getScriptEnvironment().getScriptInstance());

         Scalar result;
         result = func.evaluate(name, getScriptEnvironment().getScriptInstance(), getScriptEnvironment().getCurrentFrame());
         getScriptEnvironment().clearReturn();
         return result;
      }
   }

   /** execute a function with all of the debug, trace, etc.. support */
   public static class FunctionCallRequest extends CallRequest
   {
      protected String function;
      protected Function callme;

      public FunctionCallRequest(ScriptEnvironment e, int lineNo, String functionName, Function f)
      {
         super(e, lineNo);
         function = functionName;
         callme   = f;
      }

      public String getFunctionName()
      {
         return function;
      }

      public String getFrameDescription()    
      {
         return function + "()";
      }

      public String formatCall(String args) 
      {
         return function + "(" + args + ")";
      }

      public boolean isDebug()
      {
         return super.isDebug() && !function.equals("&@") && !function.equals("&%");
      }

      protected Scalar execute()
      {
         Scalar temp = callme.evaluate(function, getScriptEnvironment().getScriptInstance(), getScriptEnvironment().getCurrentFrame());
         getScriptEnvironment().clearReturn();
         return temp;
      }
   }

   /** execute a block of code inline with all the profiling, tracing, and other support */
   public static class InlineCallRequest extends CallRequest
   {
      protected String function;
      protected Block  inline;

      public InlineCallRequest(ScriptEnvironment e, int lineNo, String functionName, Block i)
      {
         super(e, lineNo);
         function = functionName;
         inline   = i;
      }

      public String getFunctionName()
      {
         return "<inline> " + function;
      }

      public String getFrameDescription()    
      {
         return "<inline> " + function + "()";
      }

      protected String formatCall(String args) 
      {
         return "<inline> " + function + "(" + args + ")";
      }

      protected Scalar execute()
      {
         ScriptVariables vars = getScriptEnvironment().getScriptVariables();
         synchronized (vars)
         {
            Variable localLevel = vars.getLocalVariables();
            sleep.bridges.BridgeUtilities.initLocalScope(vars, localLevel, getScriptEnvironment().getCurrentFrame());
            return inline.evaluate(getScriptEnvironment());
         }
      }
   }
}
