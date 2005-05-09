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

   public Scalar evaluate(ScriptEnvironment e)
   {
      Stack  env    = e.getEnvironmentStack();
      Scalar result = SleepUtils.getEmptyScalar();

      Object accessMe = null;
      Class  theClass = null;

      Scalar scalar   = null;

      if (classRef == null)
      {
         scalar    = (Scalar)env.pop();
         accessMe  = scalar.objectValue();
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

         result = func.evaluate(name, e.getScriptInstance(), e.getCurrentFrame());
         e.clearReturn(); // this has to be done or else bad things will happen when the closure returns stuff

         env.push(result);
         e.KillFrame();
         return result;
      }

      if (name == null)
      {
         e.getScriptInstance().fireWarning("Attempted to query an object with no method/field", getLineNumber());
         env.push(result);
         e.KillFrame();
         return result;
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
            parameters = ObjectUtilities.buildArgumentArray(theMethod.getParameterTypes(), e.getCurrentFrame(), e.getScriptInstance());
            result = ObjectUtilities.BuildScalar(true, theMethod.invoke(accessMe, parameters));
         }
         else
         {
            Field aField = theClass.getField(name);

            result = ObjectUtilities.BuildScalar(true, aField.get(accessMe));
         }
      }
      catch (InvocationTargetException ite)
      {
         Throwable yex = ite;

         while (yex.getCause() != null)
         {
            yex = yex.getCause();
         }

         e.flagError((yex.getMessage() == null) ? yex.toString() : yex.getMessage());
         e.getScriptInstance().fireWarning((yex.getMessage() == null) ? yex.toString() : yex.getMessage(), getLineNumber());
      }
      catch (IllegalArgumentException aex)
      {
         e.getScriptInstance().fireWarning(ObjectUtilities.buildArgumentErrorMessage(theClass, name, theMethod.getParameterTypes(), 
                                     parameters), getLineNumber());
      }
      catch (NoSuchFieldException fex)
      {
         e.getScriptInstance().fireWarning("no field/method named " + name + " in " + theClass, getLineNumber());
      }
      catch (Exception ex)
      {
         e.getScriptInstance().fireWarning(ex.toString() + " " + ex.getMessage(), getLineNumber());
         ex.printStackTrace();
      }

      e.KillFrame();
      env.push(result);
      return result;
   }
}
