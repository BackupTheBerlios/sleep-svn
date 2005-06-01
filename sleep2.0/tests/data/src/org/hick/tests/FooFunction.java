package org.hick.tests;

import sleep.interfaces.*;
import sleep.runtime.*;

import java.util.*;

public class FooFunction implements Function
{
   private int calls = 0;

   public Scalar evaluate(String name, ScriptInstance script, Stack locals)
   {
      System.out.println("Foo has been called with args: " + locals);
      calls++;
 
      return SleepUtils.getScalar(calls);
   }
}
