/*
   SLEEP - Simple Language for Environment Extension Purposes
 .-------------------------.
 | sleep.engine.atoms.Operate |__________________________________________________
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

public class Operate extends Step
{
   String oper;

   public Operate(String o)
   {
       oper = o;
   }

   public String toString()
   {
       return "[Operator]: "+oper+"\n";
   }

   public Scalar evaluate(ScriptEnvironment e)
   {
      Stack env = e.getEnvironmentStack();   

      Operator callme = e.getOperator(oper);

      if (callme != null)
      {
         Scalar temp = callme.operate(oper, e.getScriptInstance(), e.getCurrentFrame());
         env.push(temp);
         e.KillFrame();
         return temp;
      }
      else
      {
         e.getScriptInstance().fireWarning("Attempting to use non-existent operator: '" + oper + "'", getLineNumber());
      }
      e.KillFrame();

      return SleepUtils.getEmptyScalar();
   }
}



