/*
   SLEEP - Simple Language for Environment Extension Purposes
 .----------------------------.
 | sleep.engine.atoms.Foreach |_______________________________________________
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

public class Foreach extends Step
{
   public Block  source;
   public String value;
   public Block  code;
 
   public Foreach (Block _source, String _value, Block _code)
   {
       source = _source;
       value  = _value;
       code   = _code;
   }

   public String toString(String prefix)
   {
      StringBuffer temp = new StringBuffer();

      temp.append(prefix);
      temp.append("[Foreach]: " + value + "\n");
      temp.append(prefix);

      temp.append("  [Source Data]: \n");      
      temp.append(source.toString(prefix+"      "));

      temp.append(prefix);
      temp.append("  [Code to execute]: \n");      
      temp.append(code.toString(prefix+"      "));

      return temp.toString();
   }

   public Scalar evaluate(ScriptEnvironment e)
   {
      Stack env = e.getEnvironmentStack();   

      Variable venv = e.getScriptVariables().getScalarLevel(value, e.getScriptInstance());

      if (venv == null)
      {
         venv = e.getScriptVariables().getGlobalVariables();
      }

      source.evaluate(e);
 
      Scalar src = (Scalar)env.pop();

      if (src.getArray() == null)
      {
         e.getScriptInstance().fireWarning("Attempted to use foreach on non-array: '" + src + "'", getLineNumber());
         return null;
      }

      Iterator i = src.getArray().scalarIterator();

      while (i.hasNext())
      {
         Scalar out = (Scalar)i.next();

         venv.putScalar(value, out);

         code.evaluate(e);

         if (e.isBreak())
         {
            break;
         }

         if (e.isReturn())
         {
            return e.getReturnValue();
         }
      }

      e.flagBreak(false);
      env.clear();
//      env.push(valueHolder);
      return null;
   }
}



