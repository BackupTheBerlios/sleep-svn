/*
   SLEEP - Simple Language for Environment Extension Purposes
 .-----------------------.
 | sleep.bridges.BasicIO |____________________________________________________
 |                                                                            |
   Author: Raphael Mudge (raffi@hick.org)
           http://www.hick.org/~raffi/

   Description:
       Implementation of the subroutine concept.

   Documentation:

   Changelog:

   * This software is distributed under the artistic license, see license.txt
     for more information. *

 |____________________________________________________________________________|
 */

package sleep.bridges;
 
import java.util.*;
import java.io.*;

import sleep.engine.*;
import sleep.interfaces.*;
import sleep.runtime.*;

public class ArgumentArray extends sleep.engine.types.ArrayContainer
{
       private int      sz;
       private Variable store;

       public ArgumentArray(int size, Variable varStore)
       {
          sz     = size - 1;
          store  = varStore;
          values = null;
       }

       private void check()
       {
          if (values == null)
          {
             values = new Stack();
             for (int x = 0; x < sz; x++)
             {
                values.push(store.getScalar("$"+(x+1)));
             }
          }
       }

       public Scalar   pop() { check(); return super.pop(); }
       public Scalar   push(Scalar value) { check(); return super.push(value); }
       public int      size() { return sz; }
       public Scalar   getAt(int index) { check(); return super.getAt(index); }
       public Iterator scalarIterator() { check(); return super.scalarIterator(); }
       public Scalar   add(Scalar value, int index) { check(); return super.add(value, index); }
       public void     remove(Scalar value) { check(); super.remove(value); }
       public Scalar   remove(int index) { check(); return super.remove(index); }
       public void     sort(Comparator compare) { check(); super.sort(compare); }
       public String   toString() { check(); return super.toString(); }
}

