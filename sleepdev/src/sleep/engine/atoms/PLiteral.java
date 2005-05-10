/*
   SLEEP - Simple Language for Environment Extension Purposes
 .-------------------------.
 | sleep.engine.atoms.PLiteral |__________________________________________________
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

public class PLiteral extends Step
{
   String[] fragments;
   Block[] code;
   Block[] align;

   public String toString(String prefix)
   {
      StringBuffer temp = new StringBuffer();
      temp.append(prefix);
      temp.append("[Parsed Literal]  \n");
      for (int x = 0; x < fragments.length; x++)
      {
          temp.append(prefix);
          temp.append("   [Element]: ");
          temp.append(fragments[x]);
          temp.append("\n");

          if (x < code.length)
          {
             temp.append(prefix);
             temp.append("   [Access variable]\n");
             temp.append(code[x].toString(prefix+"      "));

             if (align[x] != null)
             { 
                temp.append(prefix);
                temp.append("      [Align variable]\n");
                temp.append(align[x].toString(prefix+"         ")); 
             }
          }
      }
      return temp.toString();
   }

   public String toString()
   {
       return toString("");
   }

   public PLiteral(String[] frag, Block[] c, Block[] a)
   {
       fragments = frag;
       code = c;
       align = a;
   }

   public Scalar evaluate(ScriptEnvironment e)
   {
      Stack env = e.getEnvironmentStack();

      StringBuffer value = new StringBuffer();
      for (int x = 0; x < fragments.length; x++) 
      {
          value.append(fragments[x]);
          if (x < code.length)
          {
             code[x].evaluate(e);
             if (align[x] != null)
             {
                String temp = ((Scalar)env.pop()).getValue().toString();
                align[x].evaluate(e);
                int al = ((Scalar)env.pop()).getValue().intValue();
                if (temp != null)
                {
                   for (int z = 0 - temp.length(); z > al; z--)
                   {
                      value.append(" ");
                   }
                   value.append(temp);
                   for (int y = temp.length(); y < al; y++)
                   {
                      value.append(" ");
                   }
                }
             }
             else
             {
                value = value.append(((Scalar)env.pop()).getValue().toString());
             }
          }
      }

      Scalar rv = SleepUtils.getScalar(value.toString());

      env.push(rv);
      return rv;
   }
}



