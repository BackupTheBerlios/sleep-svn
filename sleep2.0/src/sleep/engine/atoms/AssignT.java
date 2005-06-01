/*
   SLEEP - Simple Language for Environment Extension Purposes
 .-------------------------.
 | sleep.engine.atoms.Assign |__________________________________________________
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

public class AssignT extends Step
{
   public String toString(String prefix)
   {
      StringBuffer temp = new StringBuffer();

      temp.append(prefix);
      temp.append("[AssignT]:\n");
     
      return temp.toString();
   }

   public Scalar evaluate(ScriptEnvironment e)
   {
      Stack env = e.getEnvironmentStack();

      Scalar   scalar = (Scalar)env.pop();

      Scalar   putv;
      Scalar   value;
      Stack    variables = e.getCurrentFrame();

      if (scalar.getArray() == null)
      {
         Iterator i = variables.iterator();
         while (i.hasNext())
         {
            ((Scalar)i.next()).setValue(scalar.getValue()); // copying of value or ref handled by Scalar class
         }          
         e.KillFrame();
         return SleepUtils.getEmptyScalar();
      }

      Iterator values = scalar.getArray().scalarIterator();
      Iterator putvs  = variables.iterator();

      while (putvs.hasNext())
      {
         putv = (Scalar)putvs.next();

         if (values.hasNext())
         {
            value = (Scalar)values.next();
         }
         else
         {
            value = SleepUtils.getEmptyScalar();
         }
 
         putv.setValue(value);
      }

      e.KillFrame();
      return SleepUtils.getEmptyScalar();
   }
}



