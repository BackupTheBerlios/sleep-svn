/*
   SLEEP - Simple Language for Environment Extension Purposes
 .--------------------------.
 | sleep.engine.atoms.Check |_________________________________________________
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
import java.io.Serializable;
import sleep.runtime.*;

public class Check implements Serializable
{
   private Check   iftrue;
   private Check   iffalse;
   private Block   setup;
   private boolean negate;

   public String name; 

   public String toString(String prefix)
   {     
       StringBuffer temp = new StringBuffer();
       temp.append(prefix);
       temp.append("[Predicate]: ");
           temp.append("name->");
           temp.append(name);
           temp.append("  negated->");
           temp.append(negate);
           temp.append("\n");
       temp.append(prefix);
       temp.append("   ");
       temp.append("[Setup]: \n");
       temp.append(setup.toString(prefix+"      "));

       if (iftrue != null)
       {
          temp.append(prefix);
          temp.append("   [AND]: \n");
          temp.append(iftrue.toString(prefix+"      "));
       }
      
       if (iffalse != null)
       {
          temp.append(prefix);
          temp.append("   [OR]: \n");
          temp.append(iffalse.toString(prefix+"      "));
       }

       return temp.toString();
   }

   public String toString()
   {
       return toString("");
   }

   public Check(String n, Block s)
   {
      if (n.charAt(0) == '!' && n.length() > 2) // negation operator - we don't apply it though for like != or any other 2 character operator
      {
         name   = n.substring(1, n.length());
         negate = true;
      }
      else
      {
         name = n;
         negate = false;
      }
      setup = s;

      iftrue = null;
      iffalse = null;
   }

   public void setChoices(Check t, Check f)
   {
      if (t != null)
      {
         iftrue = t;
      }

      if (f != null)
      {
         iffalse = f;
      }
   }

   private int hint = -1;

   public void setInfo(int _hint)
   {
      hint = _hint;
   }

   public boolean check(ScriptEnvironment env)
   {
      setup.evaluate(env);
      Predicate choice = env.getPredicate(name);
 
      boolean temp;

      if (choice == null)
      {
         env.getScriptInstance().fireWarning("Attempted to use non-existent predicate: " + name, hint);
         temp = false;
      }
      else
      {
         temp = choice.decide(name, env.getScriptInstance(), env.getCurrentFrame());
      }

      env.KillFrame();

      if (negate) { temp = !temp; }

      if (temp)
      {
         if (iftrue != null)
         {
            return iftrue.check(env);
         }
         return true;
      }
      else
      {
         if (iffalse != null)
         {
            return iffalse.check(env);
         }
         return false;
      }
   }
}



