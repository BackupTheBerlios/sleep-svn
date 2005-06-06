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
   public String key;
 
   public Foreach (Block _source, String _value, Block _code)
   {
       this(_source, null, _value, _code);
   }

   public Foreach (Block _source, String _key, String _value, Block _code)
   {
       source = _source;
       key    = _key;
       value  = _value;
       code   = _code;
   }

   public String toString(String prefix)
   {
      StringBuffer temp = new StringBuffer();

      temp.append(prefix);
      temp.append("[Foreach]: " + key + " => " + value + "\n");
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
      Iterator i;

      if (src.getHash() != null)
      {
         i = src.getHash().keys().scalarIterator();  
      }
      else if (src.getArray() != null)
      {
         i = src.getArray().scalarIterator();
      }
      else
      {
         e.getScriptInstance().fireWarning("Attempted to use foreach on non-array: '" + src + "'", getLineNumber());
         return null;
      }

      int x = 0;
      while (i.hasNext())
      {
         Scalar out = (Scalar)i.next();

         if (key != null)
         {
            if (src.getHash() != null)
            {
               venv.putScalar(key, out);
               venv.putScalar(value, src.getHash().getAt(out));
            }
            else
            {
               venv.putScalar(key, SleepUtils.getScalar(x));
               venv.putScalar(value, out);
            }
         }
         else
         {
            venv.putScalar(value, out);
         }

         code.evaluate(e);

         if (e.isBreak())
         {
            break;
         }

         if (e.isReturn())
         {
            return e.getReturnValue();
         }

         x++;
      }

      e.flagBreak(false);
      env.clear();
//      env.push(valueHolder);
      return null;
   }
}



