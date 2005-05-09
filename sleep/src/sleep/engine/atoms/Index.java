/*
   SLEEP - Simple Language for Environment Extension Purposes
 .-------------------------.
 | sleep.engine.atoms.GetArray |__________________________________________________
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

public class Index extends Step
{
   String value; /* the name of the original data structure we are accessing, important for creating a new ds if we have to */
   Block index;  

   public String toString(String prefix)
   {
      StringBuffer temp = new StringBuffer();

      temp.append(prefix);
      temp.append("[SCALAR INDEX]: "+value+"\n");

      if (index != null)
      {
         temp.append(prefix);
         temp.append("   [INDEX]:     \n");

         temp.append(prefix);
         temp.append(index.toString("      "));
      }

      return temp.toString();
   }

   public Index(String v, Block i)
   {
      value = v;
      index = i;
   }

   public Scalar evaluate(ScriptEnvironment e)
   {
      Stack  env       = e.getEnvironmentStack();
      Scalar pos, rv = null;

      Scalar structure = (Scalar)env.pop();

      if (SleepUtils.isEmptyScalar(structure))
      {
          if (value.charAt(0) == '@')
          {
             structure.setValue(SleepUtils.getArrayScalar());
          }
          else if (value.charAt(0) == '%')
          {
             structure.setValue(SleepUtils.getHashScalar());
          }
      }

      index.evaluate(e);
      pos = (Scalar)(env.pop());

      if (structure.getArray() != null) { rv = structure.getArray().getAt(pos.getValue().intValue()); }
      else if (structure.getHash() != null) { rv = structure.getHash().getAt(pos); }
      else { rv = SleepUtils.getEmptyScalar(); } // always return an empty scalar if we are not referencing a hash or an array

      return (Scalar)(env.push(rv));
   }
}



