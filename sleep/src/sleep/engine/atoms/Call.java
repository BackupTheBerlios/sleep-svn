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

public class Call extends Step
{
   String function;
 
   public Call(String f)
   {
      function = f;
   }

   public String toString()
   {
      return "[Function Call]: "+function+"\n";
   }
  
   // Pre Condition:
   //  arguments on the current stack (to allow stack to be passed0
   //
   // Post Condition:
   //  current frame will be dissolved and return value will be placed on parent frame


   public Scalar evaluate(ScriptEnvironment e)
   {
      Scalar temp = null;
      Function callme = e.getFunction(function);

      if (callme != null)
      {
          temp = callme.evaluate(function, e.getScriptInstance(), e.getCurrentFrame());
          e.clearReturn();
      }
      else
      {
          e.getScriptInstance().fireWarning("Attempted to call non-existent function " + function, getLineNumber());
          temp = SleepUtils.getEmptyScalar();
      }

      e.FrameResult(temp);

      return null;
   }
}
