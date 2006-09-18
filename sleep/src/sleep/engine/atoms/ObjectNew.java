/*
   SLEEP - Simple Language for Environment Extension Purposes
 .-------------------------.
 | sleep.engine.atoms.Call |__________________________________________________
 |                                                                            |
   Author: Raphael Mudge (rsmudge@mtu.edu)
           http://www.csl.mtu.edu/~rsmudge/

   Description: This class contains an implementation of an atomic Step for
     the sleep scripting.  

   Documentation:

   Changelog:
   11/17/2002 - this class was refactored out of Step and put in its own file.

   * This software is distributed under the artistic license, see license.txt
     for more information. *

 |____________________________________________________________________________|
 */

package sleep.engine.atoms;

import java.util.*;
import sleep.interfaces.*;
import sleep.engine.*;
import sleep.runtime.*;

import java.lang.reflect.*;

public class ObjectNew extends Step
{
   protected Class name;

   public ObjectNew(Class _name)
   {
      name = _name;
   }

   public String toString()
   {
      return "[Object New]: "+name+"\n";
   }

   //
   // Pre Condition:
   //   arguments are on the current frame
   //
   // Post Condition:
   //   current frame dissolved
   //   new object is placed on parent frame

   public Scalar evaluate(ScriptEnvironment e)
   {
      boolean isTrace = (e.getScriptInstance().getDebugFlags() & ScriptInstance.DEBUG_TRACE_CALLS) == ScriptInstance.DEBUG_TRACE_CALLS;

      Scalar result = SleepUtils.getEmptyScalar();

      Object[]    parameters     = null;
      Constructor theConstructor = null;

      try
      {
         theConstructor  = ObjectUtilities.findConstructor(name, e.getCurrentFrame());

         if (theConstructor != null)
         {  
            try
            {
               theConstructor.setAccessible(true);
            }
            catch (Exception ex) { }

            if (isTrace)
            {
               String args = SleepUtils.describe(e.getCurrentFrame());

               parameters = ObjectUtilities.buildArgumentArray(theConstructor.getParameterTypes(), e.getCurrentFrame(), e.getScriptInstance());

               StringBuffer trace = new StringBuffer("[new " + name.getName());

               if (args.length() > 0)
               {
                  trace.append(": " + args);
               }

               trace.append("]");

               try
               {
                  result = ObjectUtilities.BuildScalar(false, theConstructor.newInstance(parameters));

                  if (!SleepUtils.isEmptyScalar(result))
                  {
                     trace.append(" = " + SleepUtils.describe(result));
                  }

                  e.getScriptInstance().fireWarning(trace.toString(), getLineNumber(), true);
               }
               catch (RuntimeException rex)
               {
                  trace.append(" - FAILED!");
                  e.getScriptInstance().fireWarning(trace.toString(), getLineNumber(), true);
                  throw(rex);
               }
            }
            else
            {
               parameters = ObjectUtilities.buildArgumentArray(theConstructor.getParameterTypes(), e.getCurrentFrame(), e.getScriptInstance());
               result = ObjectUtilities.BuildScalar(false, theConstructor.newInstance(parameters));
            }
         }
         else
         {
            e.getScriptInstance().fireWarning("no constructor matching "+name.getName()+"(" + SleepUtils.describe(e.getCurrentFrame()) + ")", getLineNumber());
         }
      }
      catch (InvocationTargetException ite)
      {
         Throwable yex = ite;
 
         while (yex.getCause() != null)
         {
            yex = yex.getCause();
         }

         e.flagError(yex.toString());
         e.getScriptInstance().fireWarning(yex.toString(), getLineNumber());
      }
      catch (IllegalArgumentException aex)
      {
         e.getScriptInstance().fireWarning(ObjectUtilities.buildArgumentErrorMessage(name, name.getName(), theConstructor.getParameterTypes(),
                                     parameters), getLineNumber());
      }
      catch (InstantiationException iex)
      {
         e.flagError("unable to instantiate abstract class " + name.getName());
         e.getScriptInstance().fireWarning("unable to instantiate abstract class " + name.getName(), getLineNumber());
      }
      catch (Exception iax)
      {
         e.flagError(iax.toString());
         e.getScriptInstance().fireWarning(iax.toString(), getLineNumber());
      }

      e.FrameResult(result);
      return null;
   }
}
