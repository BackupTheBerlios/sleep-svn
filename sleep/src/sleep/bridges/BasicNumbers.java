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
       temp.put("&double", this);
       temp.put("&int", this);
       temp.put("&uint", this);
       temp.put("&long", this);

       temp.put("&parseNumber",  this);
       temp.put("&formatNumber", this);

       // basic operators
       temp.put("+", this);
       temp.put("-", this);
       temp.put("/", this);
       temp.put("*", this);
       temp.put("**", this); // exponentation

       /* why "% "?  we had an amibiguity with %() to initialize hash literals and n % (expr) 
          for normal math ops.  the initial parser in the case of mod will preserve one bit of
          whitespace to try to prevent mass hysteria and confusion to the parser for determining
          wether an op is being used or a hash literal is being initialized */
       temp.put("% ", this);

       temp.put("<<", this);
       temp.put(">>", this);
       temp.put("&", this);
       temp.put("|", this);
       temp.put("^", this);
       temp.put("&not", this);
 
       // predicates
       temp.put("==", this);
       temp.put("!=", this);
       temp.put("<=", this);
       temp.put(">=", this);
       temp.put("<",  this);
       temp.put(">",  this);
       temp.put("is", this);

       // functions
       temp.put("&rand", this);
       temp.put("&srand", this);

       return true;
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
       else if (name.equals("&not")) {
           ScalarType sa = ((Scalar)args.pop()).getActualValue(); /* we already assume this is a number */

           if (sa.getClass() == IntValue.class)
               return SleepUtils.getScalar(~ sa.intValue());

           return SleepUtils.getScalar(~ sa.longValue());
       }
       else if (name.equals("&long"))
       {
          Scalar temp = BridgeUtilities.getScalar(args);
          return SleepUtils.getScalar(temp.longValue());
       }
       else if (name.equals("&double"))
       {
          Scalar temp = BridgeUtilities.getScalar(args);
          return SleepUtils.getScalar(temp.doubleValue());
       }
       else if (name.equals("&int"))
       {
          Scalar temp = BridgeUtilities.getScalar(args);
          return SleepUtils.getScalar(temp.intValue());
       }
       else if (name.equals("&uint"))
       {
          int temp = BridgeUtilities.getInt(args, 0);
          long templ = 0x00000000FFFFFFFFL & temp;
          return SleepUtils.getScalar(templ);
       }
       else if (name.equals("&parseNumber"))
       {
          String number = BridgeUtilities.getString(args, "0");
          int    radix  = BridgeUtilities.getInt(args, 10);

          BigInteger temp = new BigInteger(number, radix);
          return SleepUtils.getScalar(temp.longValue());
       }
       else if (name.equals("&formatNumber"))
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
       else if (name.equals("&srand"))
       {
          long seed = BridgeUtilities.getLong(args);
          si.getScriptEnvironment().getEnvironment().put("%RANDOM%", new Random(seed));
       }
       else if (name.equals("&rand"))
       {
          if (si.getScriptEnvironment().getEnvironment().get("%RANDOM%") == null) 
          { 
             si.getScriptEnvironment().getEnvironment().put("%RANDOM%", new Random()); 
          }
          Random r = (Random)si.getScriptEnvironment().getEnvironment().get("%RANDOM%");

          if (! args.isEmpty())
          {
             Scalar temp = (Scalar)args.pop();

             if (temp.getArray() != null)
             {
                int potential = r.nextInt(temp.getArray().size());
                return temp.getArray().getAt(potential);
             }
             else
             {
                return SleepUtils.getScalar(r.nextInt(temp.intValue()));
             }
          }
          
          return SleepUtils.getScalar(r.nextDouble());
       }

       return SleepUtils.getEmptyScalar();
    }

    public boolean decide(String n, ScriptInstance i, Stack l)
    {
       Stack env = i.getScriptEnvironment().getEnvironmentStack();
       Scalar vb = (Scalar)l.pop();
       Scalar va = (Scalar)l.pop();

       if (n.equals("is"))
          return va.objectValue() == vb.objectValue(); /* could be anything! */

       ScalarType sb = vb.getActualValue();
       ScalarType sa = va.getActualValue();

       if (sa.getClass() == DoubleValue.class || sb.getClass() == DoubleValue.class)
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
       else if (sa.getClass() == LongValue.class || sb.getClass() == LongValue.class)
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
       ScalarType left  = ((Scalar)locals.pop()).getActualValue();
       ScalarType right = ((Scalar)locals.pop()).getActualValue();

       if ((right.getClass() == DoubleValue.class || left.getClass() == DoubleValue.class) && !(o.equals(">>") || o.equals("<<") || o.equals("&") || o.equals("|") || o.equals("^")))
       {
          double a = left.doubleValue();
          double b = right.doubleValue();

          if (o.equals("+")) { return SleepUtils.getScalar(a + b); }
          if (o.equals("-")) { return SleepUtils.getScalar(a - b); }
          if (o.equals("*")) { return SleepUtils.getScalar(a * b); }
          if (o.equals("/")) { return SleepUtils.getScalar(a / b); }
          if (o.equals("% ")) { return SleepUtils.getScalar(a % b); }
          if (o.equals("**")) { return SleepUtils.getScalar(Math.pow((double)a, (double)b)); }
       }
       else if (right.getClass() == LongValue.class || left.getClass() == LongValue.class)
       {
          long a = left.longValue();
          long b = right.longValue();

          if (o.equals("+")) { return SleepUtils.getScalar(a + b); }
          if (o.equals("-")) { return SleepUtils.getScalar(a - b); }
          if (o.equals("*")) { return SleepUtils.getScalar(a * b); }
          if (o.equals("/")) { return SleepUtils.getScalar(a / b); }
          if (o.equals("% ")) { return SleepUtils.getScalar(a % b); }
          if (o.equals("**")) { return SleepUtils.getScalar(Math.pow((double)a, (double)b)); }
          if (o.equals(">>"))  { return SleepUtils.getScalar(a >> b); }
          if (o.equals("<<"))  { return SleepUtils.getScalar(a << b); }
          if (o.equals("&"))  { return SleepUtils.getScalar(a & b); }
          if (o.equals("|"))  { return SleepUtils.getScalar(a | b); }
          if (o.equals("^"))  { return SleepUtils.getScalar(a ^ b); }
       }
       else
       {
          int a = left.intValue();
          int b = right.intValue();

          if (o.equals("+")) { return SleepUtils.getScalar(a + b); }
          if (o.equals("-")) { return SleepUtils.getScalar(a - b); }
          if (o.equals("*")) { return SleepUtils.getScalar(a * b); }
          if (o.equals("/")) { return SleepUtils.getScalar(a / b); }
          if (o.equals("% ")) { return SleepUtils.getScalar(a % b); }
          if (o.equals("**")) { return SleepUtils.getScalar(Math.pow((double)a, (double)b)); }
          if (o.equals(">>"))  { return SleepUtils.getScalar(a >> b); }
          if (o.equals("<<"))  { return SleepUtils.getScalar(a << b); }
          if (o.equals("&"))  { return SleepUtils.getScalar(a & b); }
          if (o.equals("|"))  { return SleepUtils.getScalar(a | b); }
          if (o.equals("^"))  { return SleepUtils.getScalar(a ^ b); }
       }

       return SleepUtils.getEmptyScalar();
    }
}
