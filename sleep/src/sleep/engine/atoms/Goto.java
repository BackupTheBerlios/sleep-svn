/*
   SLEEP - Simple Language for Environment Extension Purposes
 .-------------------------.
 | sleep.engine.atoms.Goto |__________________________________________________
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

public class Goto extends Step
{
   public Block iftrue;
   public Block iffalse;
   public Check start;
 
   public String name;
   public boolean isLoop;

   public Goto (Check s)
   {
      start = s;
   }

   public String toString(String prefix)
   {
      StringBuffer temp = new StringBuffer();
      temp.append(prefix);
      temp.append("[Goto]:  (loop: "+isLoop+")      \n");
      temp.append(prefix);
      temp.append("  [Condition]: \n");      
      temp.append(start.toString(prefix+"      "));
     
      if (iftrue != null)
      {
         temp.append(prefix); 
         temp.append("  [If true]:   \n");      
         temp.append(iftrue.toString(prefix+"      "));
      }

      if (iffalse != null)
      {
         temp.append(prefix); 
         temp.append("  [If False]:   \n");      
         temp.append(iffalse.toString(prefix+"      "));
      }

      return temp.toString();
   }

   public void setLoop(boolean l)
   {
      isLoop = l;
   }

   public void setChoices(Block t, Block f)
   {
      iftrue = t;
      iffalse = f;
   }

   public Scalar evaluate(ScriptEnvironment e)
   {
      Stack env = e.getEnvironmentStack();   

      Scalar temp = null;

      if (start.check(e))
      {
          temp = iftrue.evaluate(e);
      }
      else if (iffalse != null)
      {
          temp = iffalse.evaluate(e);
      }

      while (!e.isReturn() && isLoop && start.check(e))
      {
          temp = iftrue.evaluate(e);
          env.clear();
      }

      if (isLoop && e.getFlowControlRequest() == ScriptEnvironment.FLOW_CONTROL_BREAK)
      {
          e.clearReturn();
          env.clear();
      }

      return temp;
   }
}



