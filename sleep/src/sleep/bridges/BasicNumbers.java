/*
   SLEEP - Simple Language for Environment Extension Purposes
 .----------------------------.
 | sleep.bridges.BasicNumbers |_______________________________________________
 |                                                                            |
   Author: Raphael Mudge (raffi@hick.org)
           http://www.hick.org/~raffi/

   Description:
      provides some of the basic number crunching functionality.

   Documentation:

   Changelog:

   * This software is distributed under the artistic license, see license.txt
     for more information. *

 |____________________________________________________________________________|
 */

package sleep.bridges;
 
import java.util.*;

import sleep.engine.*;
import sleep.engine.types.*;

import sleep.interfaces.*;
import sleep.runtime.*;

import java.math.*;

/** provides some of the basic number crunching functionality */
public class BasicNumbers implements Predicate, Operator, Loadable, Function
{
    public static Class TYPE_LONG;
    public static Class TYPE_INT;
    public static Class TYPE_DOUBLE;
  
    public BasicNumbers()
    {
       try
       {
          TYPE_LONG   = Class.forName("sleep.engine.types.LongValue");
          TYPE_INT    = Class.forName("sleep.engine.types.IntValue");
          TYPE_DOUBLE = Class.forName("sleep.engine.types.DoubleValue");
       }
       catch (Exception ex)
       {
          ex.printStackTrace();
       }
    }

    public boolean scriptUnloaded(ScriptInstance aScript)
    {
       return true;
    }

    public boolean scriptLoaded(ScriptInstance aScript)
    {
       Hashtable temp = aScript.getScriptEnvironment().getEnvironment();

       // math ops..

       String funcs[] = new String[] { "&abs", "&acos", "&asin", "&atan", "&atan2", "&ceil", "&cos", "&log", "&round", 
                                       "&sin", "&sqrt", "&tan", "&radians", "&degrees", "&exp", "&floor" };

       for (int x = 0; x < funcs.length; x++)
       {
          temp.put(funcs[x], this);
       }

       // functions
       temp.put("&double", new convert_double());
       temp.put("&int",    new convert_int());
       temp.put("&uint",    new convert_uint());
       temp.put("&long",   new convert_long());

       temp.put("&parseNumber",   new parseNumber());
       temp.put("&formatNumber",   new formatNumber());

       // basic operators
       temp.put("+", this);
       temp.put("-", this);
       temp.put("/", this);
       temp.put("*", this);
       temp.put("**", this); // exponentation
       temp.put("%", this);

       temp.put("<<", this);
       temp.put(">>", this);
       temp.put("&", this);
       temp.put("|", this);
       temp.put("^", this);
       temp.put("&not", new not());
 
       // predicates
       temp.put("==", this);
       temp.put("!=", this);
       temp.put("<=", this);
       temp.put(">=", this);
       temp.put("<",  this);
       temp.put(">",  this);
       temp.put("is", this);

       // functions
       temp.put("&rand", new rand());

       return true;
    }

    private static class parseNumber implements Function
    {
       public Scalar evaluate(String name, ScriptInstance si, Stack args)
       {
          String number = BridgeUtilities.getString(args, "0");
          int    radix  = BridgeUtilities.getInt(args, 10);

          BigInteger temp = new BigInteger(number, radix);
          return SleepUtils.getScalar(temp.longValue());
       }
    }

    public Scalar evaluate(String name, ScriptInstance si, Stack args)
    {
       if (name.equals("&abs")) { return SleepUtils.getScalar(Math.abs(BridgeUtilities.getDouble(args, 0.0))); }
       else if (name.equals("&acos")) { return SleepUtils.getScalar(Math.acos(BridgeUtilities.getDouble(args, 0.0))); }
       else if (name.equals("&asin")) { return SleepUtils.getScalar(Math.asin(BridgeUtilities.getDouble(args, 0.0))); }
       else if (name.equals("&atan")) { return SleepUtils.getScalar(Math.atan(BridgeUtilities.getDouble(args, 0.0))); }
       else if (name.equals("&atan2")) { return SleepUtils.getScalar(Math.atan2(BridgeUtilities.getDouble(args, 0.0), BridgeUtilities.getDouble(args, 0.0))); }
       else if (name.equals("&ceil")) { return SleepUtils.getScalar(Math.ceil(BridgeUtilities.getDouble(args, 0.0))); }
       else if (name.equals("&floor")) { return SleepUtils.getScalar(Math.floor(BridgeUtilities.getDouble(args, 0.0))); }
       else if (name.equals("&cos")) { return SleepUtils.getScalar(Math.cos(BridgeUtilities.getDouble(args, 0.0))); }
       else if (name.equals("&log") && args.size() == 1) { return SleepUtils.getScalar(Math.log(BridgeUtilities.getDouble(args, 0.0))); }
       else if (name.equals("&log") && args.size() == 2) { return SleepUtils.getScalar(Math.log(BridgeUtilities.getDouble(args, 0.0)) / Math.log(BridgeUtilities.getDouble(args, 0.0))); }
       else if (name.equals("&round")) { return SleepUtils.getScalar(Math.round(BridgeUtilities.getDouble(args, 0.0))); }
       else if (name.equals("&sin")) { return SleepUtils.getScalar(Math.sin(BridgeUtilities.getDouble(args, 0.0))); }
       else if (name.equals("&sqrt")) { return SleepUtils.getScalar(Math.sqrt(BridgeUtilities.getDouble(args, 0.0))); }
       else if (name.equals("&tan")) { return SleepUtils.getScalar(Math.tan(BridgeUtilities.getDouble(args, 0.0))); }
       else if (name.equals("&radians")) { return SleepUtils.getScalar(Math.toRadians(BridgeUtilities.getDouble(args, 0.0))); }
       else if (name.equals("&degrees")) { return SleepUtils.getScalar(Math.toDegrees(BridgeUtilities.getDouble(args, 0.0))); }
       else if (name.equals("&exp")) { return SleepUtils.getScalar(Math.exp(BridgeUtilities.getDouble(args, 0.0))); }

       return SleepUtils.getEmptyScalar();
    }
    
    private static class formatNumber implements Function
    {
       public Scalar evaluate(String name, ScriptInstance si, Stack args)
       {
          String number = BridgeUtilities.getString(args, "0");

          int from = 10, to = 10;

          if (args.size() == 2)
          {
             from = BridgeUtilities.getInt(args, 10);
          }

          to = BridgeUtilities.getInt(args, 10);

          BigInteger temp = new BigInteger(number, from);
          return SleepUtils.getScalar(temp.toString(to));
       }
    }
    
    private static class not implements Function
    {
       public Scalar evaluate(String name, ScriptInstance si, Stack args)
       {
           ScalarType sa = ((Scalar)args.pop()).getValue();

           if (sa.getClass() == TYPE_INT)
               return SleepUtils.getScalar(~ sa.intValue());

           return SleepUtils.getScalar(~ sa.longValue());
       }
    }

    private static class rand implements Function
    {
       public Scalar evaluate(String name, ScriptInstance si, Stack args)
       {
          if (! args.isEmpty())
          {
             int to = BridgeUtilities.getInt(args);
             return SleepUtils.getScalar((int)(Math.random() * to));
          }
          
          return SleepUtils.getScalar(Math.random());
       }
    }

    private static class convert_double implements Function
    {
       public Scalar evaluate(String name, ScriptInstance si, Stack args)
       {          Scalar temp = BridgeUtilities.getScalar(args);
          return SleepUtils.getScalar(temp.doubleValue());
       }
    }

    private static class convert_int implements Function
    {
       public Scalar evaluate(String name, ScriptInstance si, Stack args)
       {
          Scalar temp = BridgeUtilities.getScalar(args);
          return SleepUtils.getScalar(temp.intValue());
       }
    }

    private static class convert_uint implements Function
    {
       public Scalar evaluate(String name, ScriptInstance si, Stack args)
       {
          int temp = BridgeUtilities.getInt(args, 0);
          long templ = 0x00000000FFFFFFFFL & temp;
          return SleepUtils.getScalar(templ);
       }
    }

    private static class convert_long implements Function
    {
       public Scalar evaluate(String name, ScriptInstance si, Stack args)
       {
          Scalar temp = BridgeUtilities.getScalar(args);
          return SleepUtils.getScalar(temp.longValue());
       }
    }

    public boolean decide(String n, ScriptInstance i, Stack l)
    {
       Stack env = i.getScriptEnvironment().getEnvironmentStack();
       ScalarType sb = ((Scalar)l.pop()).getValue();
       ScalarType sa = ((Scalar)l.pop()).getValue();

       if (n.equals("is"))
          return sa.objectValue() == sb.objectValue();

       if (sa.getClass() == TYPE_DOUBLE || sb.getClass() == TYPE_DOUBLE)
       {
          double a = sa.doubleValue();
          double b = sb.doubleValue();

          if (n.equals("==")) { return a == b; }
          if (n.equals("!=")) { return a != b; }
          if (n.equals("<=")) { return a <= b; }
          if (n.equals(">=")) { return a >= b; }
          if (n.equals("<"))  { return a <  b; }
          if (n.equals(">"))  { return a >  b; }
       }
       else if (sa.getClass() == TYPE_LONG || sb.getClass() == TYPE_LONG)
       {
          long a = sa.longValue();
          long b = sb.longValue();

          if (n.equals("==")) { return a == b; }
          if (n.equals("!=")) { return a != b; }
          if (n.equals("<=")) { return a <= b; }
          if (n.equals(">=")) { return a >= b; }
          if (n.equals("<"))  { return a <  b; }
          if (n.equals(">"))  { return a >  b; }
       }
       else
       {
          int a = sa.intValue();
          int b = sb.intValue();

          if (n.equals("==")) { return a == b; }
          if (n.equals("!=")) { return a != b; }
          if (n.equals("<=")) { return a <= b; }
          if (n.equals(">=")) { return a >= b; }
          if (n.equals("<"))  { return a <  b; }
          if (n.equals(">"))  { return a >  b; }
       }

       return false;
    }

    public Scalar operate(String o, ScriptInstance i, Stack locals)
    {
       ScalarType left  = ((Scalar)locals.pop()).getValue();
       ScalarType right = ((Scalar)locals.pop()).getValue();

       if ((right.getClass() == TYPE_DOUBLE || left.getClass() == TYPE_DOUBLE) && !(o.equals(">>") || o.equals("<<") || o.equals("&") || o.equals("|") || o.equals("^")))
       {
          double a = left.doubleValue();
          double b = right.doubleValue();

          if (o.equals("+")) { return SleepUtils.getScalar(a + b); }
          if (o.equals("-")) { return SleepUtils.getScalar(a - b); }
          if (o.equals("*")) { return SleepUtils.getScalar(a * b); }
          if (o.equals("/")) { return SleepUtils.getScalar(a / b); }
          if (o.equals("%")) { return SleepUtils.getScalar(a % b); }
          if (o.equals("**")) { return SleepUtils.getScalar(Math.pow((double)a, (double)b)); }
       }
       else if (right.getClass() == TYPE_LONG || left.getClass() == TYPE_LONG)
       {
          long a = left.longValue();
          long b = right.longValue();

          if (o.equals("+")) { return SleepUtils.getScalar(a + b); }
          if (o.equals("-")) { return SleepUtils.getScalar(a - b); }
          if (o.equals("*")) { return SleepUtils.getScalar(a * b); }
          if (o.equals("/")) { return SleepUtils.getScalar(a / b); }
          if (o.equals("%")) { return SleepUtils.getScalar(a % b); }
          if (o.equals("**")) { return SleepUtils.getScalar(Math.pow((double)a, (double)b)); }
          if (o.equals(">>"))  { return SleepUtils.getScalar(a >> b); }
          if (o.equals("<<"))  { return SleepUtils.getScalar(a << b); }
          if (o.equals("&"))  { return SleepUtils.getScalar(a & b); }
          if (o.equals("|"))  { return SleepUtils.getScalar(a | b); }
          if (o.equals("^"))  { return SleepUtils.getScalar(a & b); }
       }
       else
       {
          int a = left.intValue();
          int b = right.intValue();

          if (o.equals("+")) { return SleepUtils.getScalar(a + b); }
          if (o.equals("-")) { return SleepUtils.getScalar(a - b); }
          if (o.equals("*")) { return SleepUtils.getScalar(a * b); }
          if (o.equals("/")) { return SleepUtils.getScalar(a / b); }
          if (o.equals("%")) { return SleepUtils.getScalar(a % b); }
          if (o.equals("**")) { return SleepUtils.getScalar(Math.pow((double)a, (double)b)); }
          if (o.equals(">>"))  { return SleepUtils.getScalar(a >> b); }
          if (o.equals("<<"))  { return SleepUtils.getScalar(a << b); }
          if (o.equals("&"))  { return SleepUtils.getScalar(a & b); }
          if (o.equals("|"))  { return SleepUtils.getScalar(a | b); }
          if (o.equals("^"))  { return SleepUtils.getScalar(a & b); }
       }

       return SleepUtils.getEmptyScalar();
    }
}
