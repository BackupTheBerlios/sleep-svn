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
   public Block increment;

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

      if (increment != null)
      {
         temp.append(prefix); 
         temp.append("  [Increment]:   \n");      
         temp.append(increment.toString(prefix+"      "));
      }

      return temp.toString();
   }

   public void setLoop(boolean l, Block i)
   {
      isLoop    = l;
      increment = i;
   }

   public void setChoices(Block t, Block f)
   {
      iftrue = t;
      iffalse = f;
   }

   public Scalar evaluate(ScriptEnvironment e)
   {
      Scalar temp = null;

      if (start.check(e))
      {
          iftrue.evaluate(e);
      }
      else if (iffalse != null)
      {
          iffalse.evaluate(e);
      }

      if (isLoop)
      {
          if (e.getFlowControlRequest() == ScriptEnvironment.FLOW_CONTROL_CONTINUE)
          {
             e.clearReturn();
          }

          if (increment != null)
          {
             increment.evaluate(e);
          }

          while (!e.isReturn() && start.check(e))
          {
             iftrue.evaluate(e);

             if (e.getFlowControlRequest() == ScriptEnvironment.FLOW_CONTROL_CONTINUE)
             {
                e.clearReturn();
             }

             if (increment != null)
             {
                increment.evaluate(e);
             }
         }

         if (e.getFlowControlRequest() == ScriptEnvironment.FLOW_CONTROL_BREAK)
         {
             e.clearReturn();
         }
      }

      return null;
   }
}



