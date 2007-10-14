package sleep.engine;

import sleep.engine.*;
import sleep.runtime.*;

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

   /** return a string view of this function call; arguments are captured as comma separated descriptions of all args */
   protected abstract String formatCall(String args);

   /** return true if debug trace is enabled.  override this to add/change criteria for trace activiation */
   public boolean isDebug()
   {
      return (getScriptEnvironment().getScriptInstance().getDebugFlags() & ScriptInstance.DEBUG_TRACE_CALLS) == ScriptInstance.DEBUG_TRACE_CALLS;
   }

   /** actually execute the function call */
   public void CallFunction()
   {
      Scalar temp;
      ScriptEnvironment e = getScriptEnvironment();
      int mark = getScriptEnvironment().markFrame();

      if (isDebug())
      {
         if (e.getScriptInstance().isProfileOnly())
         {
             long stat = System.currentTimeMillis();
             temp = execute();
             stat = System.currentTimeMillis() - stat;
             e.getScriptInstance().collect(getFunctionName(), getLineNumber(), stat);
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
                e.cleanFrame(mark);
                e.KillFrame();
                e.getScriptInstance().fireWarning(formatCall(args) + " - FAILED!", getLineNumber(), true);
                throw(rex);
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
             e.cleanFrame(mark);
             e.KillFrame();
             throw(rex);
         }
      }

      if (e.isThrownValue())
      {
         e.getScriptInstance().recordStackFrame(getFrameDescription(), getLineNumber());
      }

      e.cleanFrame(mark);
      e.FrameResult(temp);
   }
}
