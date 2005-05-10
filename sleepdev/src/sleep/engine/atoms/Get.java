/*
   SLEEP - Simple Language for Environment Extension Purposes
 .-------------------------.
 | sleep.engine.atoms.Get |__________________________________________________
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

public class Get extends Step
{
   String value;
  
   public Get(String v)
   {
      value = v;
   }

   public String toString()
   {
      return "[Scalar]: "+value+"\n";
   }

   public Scalar evaluate(ScriptEnvironment e)
   {
      Stack env = e.getEnvironmentStack();

      Scalar structure = e.getScalar(value);

      if (structure != null)
      {
         env.push(structure);
         return structure;
      }

      if (value.charAt(0) == '@')
         structure = SleepUtils.getArrayScalar();
      else if (value.charAt(0) == '%')
         structure = SleepUtils.getHashScalar();
      else
         structure = SleepUtils.getEmptyScalar();

      e.putScalar(value, structure);
      env.push(structure);

      return structure;
   }
}



