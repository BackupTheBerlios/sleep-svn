/*
   SLEEP - Simple Language for Environment Extension Purposes
 .---------------------------------.
 | sleep.engine.atoms.ObjectAccess |__________________________________________
 |                                                                            |
   Author: Raphael Mudge (rsmudge@mtu.edu)
           http://www.csl.mtu.edu/~rsmudge/

   Description: This class contains an implementation of an atomic Step for
     the sleep scripting.  

   Documentation:

   Changelog:

   * This software is distributed under the artistic license, see license.txt
     for more information. *

 |____________________________________________________________________________|
 */

package sleep.engine.atoms;

import java.util.*;
import sleep.interfaces.*;
import sleep.engine.*;
import sleep.runtime.*;

import sleep.bridges.SleepClosure;

import java.lang.reflect.*;

public class ObjectAccess extends Step
{
   protected String name;
   protected Class  classRef;

   public ObjectAccess(String _name, Class _classRef)
   {
      name     = _name;
      classRef = _classRef;
   }

   public String toString()
   {
      return "[Object Access]: "+classRef+"#"+name+"\n";
   }

   private static class ClosureCallRequest extends CallRequest
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

   //
   // Pre Condition:
   //   object we're accessing is top item on current frame
   //   arguments consist of the rest of the current frame...
   //
   // Post Condition:
   //   current frame is dissolved
   //   result is top item on parent frame

   public Scalar evaluate(ScriptEnvironment e)
   {
      int mark = e.markFrame();
      Scalar result = SleepUtils.getEmptyScalar();

      Object accessMe = null;
      Class  theClass = null;

      Scalar scalar   = null;

      if (classRef == null)
      {
         scalar    = (Scalar)e.getCurrentFrame().pop();
         accessMe  = scalar.objectValue();

         if (accessMe == null)
         {
            e.getScriptInstance().fireWarning("Attempted to call a non-static method on a null reference", getLineNumber());
            e.cleanFrame(mark);
            e.KillFrame();

            e.getCurrentFrame().push(SleepUtils.getEmptyScalar());

            return null;
         }

         theClass  = accessMe.getClass();
      }
      else
      {
         theClass   = classRef;
      }
      
      //
      // check if this is a closure, if it is, try to invoke stuff on it instead
      //

      if (scalar != null && SleepUtils.isFunctionScalar(scalar))
      {
         ClosureCallRequest request = new ClosureCallRequest(e, getLineNumber(), scalar, name);
         request.CallFunction();
         return null;
      }

      //
      // now we know we're not dealing with a closure; so before we go on the name field has to be non-null.
      //

      if (name == null)
      {
         e.getScriptInstance().fireWarning("Attempted to query an object with no method/field", getLineNumber());
         e.cleanFrame(mark);
         e.KillFrame();
         e.getCurrentFrame().push(result);

         return null;
      }

      boolean isTrace   = (e.getScriptInstance().getDebugFlags() & ScriptInstance.DEBUG_TRACE_CALLS) == ScriptInstance.DEBUG_TRACE_CALLS;

      //
      // try to invoke stuff on the object...
      //

      Method theMethod    = null;
      Object[] parameters = null;

      try
      {
         theMethod  = ObjectUtilities.findMethod(theClass, name, e.getCurrentFrame());

         if (theMethod != null && (classRef == null || (theMethod.getModifiers() & Modifier.STATIC) == Modifier.STATIC))
         {  
            try
            {
               theMethod.setAccessible(true);
            }
            catch (Exception ex) { }

            if (isTrace)
            {
               if (e.getScriptInstance().isProfileOnly())
               {
                  long stat = System.currentTimeMillis();

                  parameters = ObjectUtilities.buildArgumentArray(theMethod.getParameterTypes(), e.getCurrentFrame(), e.getScriptInstance());
                  result = ObjectUtilities.BuildScalar(true, theMethod.invoke(accessMe, parameters));

                  stat = System.currentTimeMillis() - stat;
                  e.getScriptInstance().collect(theMethod.toString(), getLineNumber(), stat);
               }
               else
               {
                  String args = SleepUtils.describe(e.getCurrentFrame());

                  if (args.length() > 0) { args = ": " + args; }

                  parameters = ObjectUtilities.buildArgumentArray(theMethod.getParameterTypes(), e.getCurrentFrame(), e.getScriptInstance());

                  /* construct the actual trace message */

                  StringBuffer trace = new StringBuffer("[");

                  if (scalar == null)
                  {
                     trace.append(theClass.getName() + " " + name + args + "]");
                  }
                  else if (Proxy.isProxyClass(theClass))
                  {
                     trace.append(theClass.getName() + " " + name + args + "]");
                  }
                  else
                  {
                     trace.append(SleepUtils.describe(scalar) + " " + name + args + "]");
                  }

                  try
                  {
                     long stat = System.currentTimeMillis();
                     result = ObjectUtilities.BuildScalar(true, theMethod.invoke(accessMe, parameters));
                     stat = System.currentTimeMillis() - stat;
                     e.getScriptInstance().collect(theMethod.toString(), getLineNumber(), stat);

                     if (!SleepUtils.isEmptyScalar(result))
                     {
                        trace.append(" = " + SleepUtils.describe(result));
                     }

                     e.getScriptInstance().fireWarning(trace.toString(), getLineNumber(), true); 
                  }
                  catch (InvocationTargetException ite)
                  {
                     ObjectUtilities.handleExceptionFromJava(ite.getCause(), e, theMethod + "", getLineNumber());
                     trace.append(" - FAILED!");
                     e.getScriptInstance().fireWarning(trace.toString(), getLineNumber(), true); 
                  }
                  catch (RuntimeException rex)
                  {
                     trace.append(" - FAILED!");
                     e.getScriptInstance().fireWarning(trace.toString(), getLineNumber(), true); 
                     throw(rex);
                  }
               }
            }
            else
            {
               parameters = ObjectUtilities.buildArgumentArray(theMethod.getParameterTypes(), e.getCurrentFrame(), e.getScriptInstance());
               result = ObjectUtilities.BuildScalar(true, theMethod.invoke(accessMe, parameters));
            }
         }
         else
         {
            Field aField = theClass.getField(name);

            if (aField != null)
            {
               try
               {
                  aField.setAccessible(true);
               }
               catch (Exception ex) { }

               result = ObjectUtilities.BuildScalar(true, aField.get(accessMe));
            }
            else
            {
               result = SleepUtils.getEmptyScalar();
            }
         }
      }
      catch (InvocationTargetException ite)
      {
         ObjectUtilities.handleExceptionFromJava(ite.getCause(), e, theMethod + "", getLineNumber());
      }
      catch (IllegalArgumentException aex)
      {
         aex.printStackTrace();

         e.getScriptInstance().fireWarning(ObjectUtilities.buildArgumentErrorMessage(theClass, name, theMethod.getParameterTypes(), 
                                     parameters), getLineNumber());
      }
      catch (NoSuchFieldException fex)
      {
         if (!e.getCurrentFrame().isEmpty())
         {
            e.getScriptInstance().fireWarning("there is no method that matches " + name + "("+SleepUtils.describe(e.getCurrentFrame()) + ") in " + theClass.getName(), getLineNumber());
         }
         else
         {
            e.getScriptInstance().fireWarning("no field/method named " + name + " in " + theClass, getLineNumber());
         }
      }
      catch (IllegalAccessException iax)
      {
         e.getScriptInstance().fireWarning("cannot access " + name + " in " + theClass + ": " + iax.getMessage(), getLineNumber());
      }

      e.cleanFrame(mark);
      e.FrameResult(result);
      return null;
   }
}
