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

public class Assign extends Step
{
   Block variable;
    
   public Assign(Block var)
   {
      variable = var;
   }

   public String toString(String prefix)
   {
      StringBuffer temp = new StringBuffer();

      temp.append(prefix);
      temp.append("[Assign]:\n");
     
      temp.append(prefix);
      temp.append(variable.toString("   "));

      return temp.toString();
   }

   public Scalar evaluate(ScriptEnvironment e)
   {
      Stack env = e.getEnvironmentStack();
      variable.evaluate(e);
 
      Scalar putv  = (Scalar)env.pop();
      Scalar value = (Scalar)env.pop();

      putv.setValue(value);    

      return putv;
   }
}



