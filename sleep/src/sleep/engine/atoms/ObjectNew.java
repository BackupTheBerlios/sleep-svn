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

   public Scalar evaluate(ScriptEnvironment e)
   {
      Stack  env    = e.getEnvironmentStack();
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

            parameters = ObjectUtilities.buildArgumentArray(theConstructor.getParameterTypes(), e.getCurrentFrame(), e.getScriptInstance());
            result = ObjectUtilities.BuildScalar(false, theConstructor.newInstance(parameters));
         }
         else
         {
            e.getScriptInstance().fireWarning("no " + e.getCurrentFrame().size() + " argument constructor exists for class " + name.getName(), getLineNumber());
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

      e.KillFrame();
      env.push(result);
      return result;
   }
}
