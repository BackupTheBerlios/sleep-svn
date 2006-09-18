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
      boolean isTrace   = (e.getScriptInstance().getDebugFlags() & ScriptInstance.DEBUG_TRACE_CALLS) == ScriptInstance.DEBUG_TRACE_CALLS;

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
         Function func = SleepUtils.getFunctionFromScalar(scalar, e.getScriptInstance());

         if (isTrace)
         {
            String args = SleepUtils.describe(e.getCurrentFrame());

            /* construct the actual trace message */

            StringBuffer trace = new StringBuffer("[" + SleepUtils.describe(scalar));
           
            if (name != null && name.length() > 0)
            {
               trace.append(" " + name);
            }

            if (args.length() > 0)
            {
               trace.append(": " + args + "]");
            }
            else
            {
               trace.append("]");
            }

            try
            {
               result = func.evaluate(name, e.getScriptInstance(), e.getCurrentFrame());

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
            result = func.evaluate(name, e.getScriptInstance(), e.getCurrentFrame());
         }         

         e.clearReturn(); // this has to be done or else bad things will happen when the closure returns stuff
         // ^-- evaluate, is this really necessary or are closures smart enough to clear the return themselves? // RSM

         e.FrameResult(result);
         return null;
      }

      if (name == null)
      {
         e.getScriptInstance().fireWarning("Attempted to query an object with no method/field", getLineNumber());
         e.KillFrame();
         e.getCurrentFrame().push(result);

         return null;
      }

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
               String args = SleepUtils.describe(e.getCurrentFrame());

               if (args.length() > 0) { args = ": " + args; }

               parameters = ObjectUtilities.buildArgumentArray(theMethod.getParameterTypes(), e.getCurrentFrame(), e.getScriptInstance());

               /* construct the actual trace message */

               StringBuffer trace = new StringBuffer("[");

               if (scalar == null)
               {
                  trace.append(theClass.getName() + " " + name + args + "]");
               }
               else
               {
                  trace.append(SleepUtils.describe(scalar) + " " + name + args + "]");
               }

               try
               {
                  result = ObjectUtilities.BuildScalar(true, theMethod.invoke(accessMe, parameters));

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

      e.FrameResult(result);
      return null;
   }
}
