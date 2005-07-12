/*
   SLEEP - Simple Language for Environment Extension Purposes
 .----------------------------.
 | sleep.bridges.BasicStrings |_______________________________________________
 |                                                                            |
   Author: Raphael Mudge (raffi@hick.org)
           http://www.hick.org/~raffi/

   Description:
      provides some of the basic parsing facilities.

   Documentation:

   Changelog:

   * This software is distributed under the artistic license, see license.txt
     for more information. *

 |____________________________________________________________________________|
 */

package sleep.bridges;
 
import java.util.*;

import sleep.engine.*;
import sleep.interfaces.*;
import sleep.runtime.*;

import sleep.parser.ParserConfig;

/** provides basic string parsing facilities */
public class BasicStrings implements Loadable
{
    static 
    {
        // if an operator followed by an expression could be mistaken for
        // a function call, then we need to register the operator as a keyword.
        // :)
        ParserConfig.addKeyword("x");
        ParserConfig.addKeyword("eq");
        ParserConfig.addKeyword("ne");
        ParserConfig.addKeyword("lt");
        ParserConfig.addKeyword("gt");
        ParserConfig.addKeyword("isin");
        ParserConfig.addKeyword("iswm");
    }

    public boolean scriptUnloaded(ScriptInstance aScript)
    {
        return true;
    }

    public boolean scriptLoaded (ScriptInstance aScript)
    {
        Hashtable temp = aScript.getScriptEnvironment().getEnvironment();

        // functions
        temp.put("&left",   new func_left());
        temp.put("&right",  new func_right());

        temp.put("&charAt",  new func_charAt());
        temp.put("&uc",      new func_uc());
        temp.put("&lc",      new func_lc());
        temp.put("&substr",  new func_substr());
        temp.put("&indexOf", new func_indexOf());
        temp.put("&strlen",  new func_strlen());
        temp.put("&strrep",  new func_strrep());

        temp.put("&tr",      new func_tr());

        temp.put("&asc",     new func_asc());
        temp.put("&chr",     new func_chr());

        temp.put("&sort",    new func_sort());
        temp.put("&sorta",    new func_sorta());
        temp.put("&sortn",    new func_sortn());
        temp.put("&sortd",    new func_sortd());

        // predicates
        temp.put("eq", new pred_eq());
        temp.put("ne", new pred_ne());
        temp.put("lt", new pred_lt());
        temp.put("gt", new pred_gt());

        temp.put("-isletter", new pred_isletter());
        temp.put("-isnumber", new pred_isnumber());

        temp.put("isin", new pred_isin());
        temp.put("iswm", new pred_iswm());  // I couldn't resist >)

        // operators
        temp.put(".", new oper_concat());
        temp.put("x", new oper_multiply());
        temp.put("cmp", new oper_compare());
        temp.put("<=>", new oper_spaceship());

        return true;
    }

    private static class pred_eq implements Predicate
    {
        public boolean decide(String n, ScriptInstance i, Stack l)
        {
           String b = BridgeUtilities.getString(l, "");
           String a = BridgeUtilities.getString(l, "");

           return a.equals(b);
        }
    }

    private static class pred_isin implements Predicate
    {
        public boolean decide(String n, ScriptInstance i, Stack l)
        {
           String b = BridgeUtilities.getString(l, "");
           String a = BridgeUtilities.getString(l, "");

           return b.indexOf(a) > -1;
        }
    }

    private static class pred_isletter implements Predicate
    {
        public boolean decide(String n, ScriptInstance i, Stack l)
        {
           String check = l.pop().toString();

           if (check.length() <= 0)
              return false;

           for (int x = 0; x < check.length(); x++)
           {
              if (! Character.isLetter(check.charAt(x)) )
              {
                 return false;
              }
           }

           return true;
        }
    }

    private static class pred_isnumber implements Predicate
    {
        public boolean decide(String n, ScriptInstance i, Stack l)
        {
           String check = l.pop().toString();
 
           if (check.length() <= 0)
              return false;

           if (check.indexOf('.') > -1 && check.indexOf('.') != check.lastIndexOf('.'))
              return false;

           for (int x = 0; x < check.length(); x++)
           {
              if (! Character.isDigit(check.charAt(x)) && (check.charAt(x) != '.' || (x+1) >= check.length()) )
              {
                 return false;
              }
           }

           return true;
        }
    }

    private static class pred_ne implements Predicate
    {
        public boolean decide(String n, ScriptInstance i, Stack l)
        {
           String b = BridgeUtilities.getString(l, "");
           String a = BridgeUtilities.getString(l, "");

           return ! a.equals(b);
        }
    }

    private static class pred_gt implements Predicate
    {
        public boolean decide(String n, ScriptInstance i, Stack l)
        {
           String b = BridgeUtilities.getString(l, "");
           String a = BridgeUtilities.getString(l, "");

           return a.compareTo(b) > 0;
        }
    }

    private static class pred_lt implements Predicate
    {
        public boolean decide(String n, ScriptInstance i, Stack l)
        {
           String b = BridgeUtilities.getString(l, "");
           String a = BridgeUtilities.getString(l, "");

           return a.compareTo(b) < 0;
        }
    }

    private static class pred_iswm implements Predicate
    {
        public boolean decide(String name, ScriptInstance script, Stack locals)
        {
           String b = locals.pop().toString();
           String a = locals.pop().toString();

           if ((a.length() == 0 || b.length() == 0) && a.length() != b.length())
              return false;

           int aptr = 0, bptr = 0, cptr;

           while (aptr < a.length())
           {
              if (a.charAt(aptr) == '*')
              {
                 boolean greedy = ((aptr + 1) < a.length() && a.charAt(aptr + 1) == '*');

                 while (a.charAt(aptr) == '*')
                 {
                    aptr++;
                    if (aptr == a.length())
                    {
                       return true;
                    }
                 }

                 for (cptr = aptr; cptr < a.length() && a.charAt(cptr) != '?' && a.charAt(cptr) != '*'; cptr++);

                 if (greedy)
                    cptr = b.lastIndexOf(a.substring(aptr, cptr)); 
                 else
                    cptr = b.indexOf(a.substring(aptr, cptr), bptr); 


                 if (cptr == -1 || cptr < bptr) // < - require 0 or more chars, <= - requires 1 or more chars
                 {
                    return false;
                 }

                 bptr = cptr;
              }
              else if (a.charAt(aptr) == '\\')
              {
                 aptr++;

                 if (aptr < a.length() && a.charAt(aptr) != b.charAt(bptr))
                    return false;
              }
              else if (a.charAt(aptr) != '?' && a.charAt(aptr) != b.charAt(bptr))
              {
                 return false;
              }

              aptr++;
              bptr++;
           }

           return (bptr == b.length());
        }
    }

    private static class func_left implements Function
    {
        public Scalar evaluate(String n, ScriptInstance i, Stack l)
        {
           String temp  = l.pop().toString();
           int    value = ((Scalar)l.pop()).intValue();

           if (value >= temp.length()) { return SleepUtils.getScalar(temp); }

           return SleepUtils.getScalar(temp.substring(0, value));
        }
    }

    private static class func_tr implements Function
    {
        public Scalar evaluate(String n, ScriptInstance i, Stack l)
        {
           String old       = BridgeUtilities.getString(l, "");
           String pattern   = BridgeUtilities.getString(l, "");
           String mapper    = BridgeUtilities.getString(l, "");
           String optstr    = BridgeUtilities.getString(l, "");

           int options = 0; 

           if (optstr.indexOf('c') > -1) { options = options | Transliteration.OPTION_COMPLEMENT; }
           if (optstr.indexOf('d') > -1) { options = options | Transliteration.OPTION_DELETE; }
           if (optstr.indexOf('s') > -1) { options = options | Transliteration.OPTION_SQUEEZE; }

           Transliteration temp = Transliteration.compile(pattern, mapper, options);

           return SleepUtils.getScalar(temp.translate(old));
        }
    }

    private static class func_right implements Function
    {
        public Scalar evaluate(String n, ScriptInstance i, Stack l)
        {
           String temp  = l.pop().toString();
           int    value = ((Scalar)l.pop()).intValue();

           if (value >= temp.length()) { return SleepUtils.getScalar(temp); }

           return SleepUtils.getScalar(temp.substring(temp.length() - value, temp.length()));
        }
    }

    private static class func_asc implements Function
    {
        public Scalar evaluate(String n, ScriptInstance i, Stack l)
        {
           return SleepUtils.getScalar((int)l.pop().toString().charAt(0));
        }
    }

    private static class func_chr implements Function
    {
        public Scalar evaluate(String n, ScriptInstance i, Stack l)
        {
           return SleepUtils.getScalar(((char)BridgeUtilities.getInt(l))+"");
        }
    }


    private static class func_uc implements Function
    {
        public Scalar evaluate(String n, ScriptInstance i, Stack l)
        {
           return SleepUtils.getScalar(l.pop().toString().toUpperCase());
        }
    }

    private static class func_lc implements Function
    {
        public Scalar evaluate(String n, ScriptInstance i, Stack l)
        {
           return SleepUtils.getScalar(l.pop().toString().toLowerCase());
        }
    }

    private static class func_strlen implements Function
    {
        public Scalar evaluate(String n, ScriptInstance i, Stack l)
        {
           return SleepUtils.getScalar(l.pop().toString().length());
        }
    }

    private static class func_strrep implements Function
    {
        public Scalar evaluate(String n, ScriptInstance i, Stack l)
        {
           StringBuffer work    = new StringBuffer(BridgeUtilities.getString(l, ""));

           while (!l.isEmpty())
           {
              String       oldstr  = BridgeUtilities.getString(l, "");
              String       newstr  = BridgeUtilities.getString(l, "");
 
              if (oldstr.length() == 0) { continue; }

              int x      = 0;
              int oldlen = oldstr.length();
              int newlen = newstr.length();

              while ((x = work.indexOf(oldstr, x)) > -1)
              {
                 work.replace(x, x + oldlen, newstr);
                 x += newstr.length();
              }
           }

           return SleepUtils.getScalar(work.toString());
        }
    }

    private static class func_substr implements Function
    {
        public Scalar evaluate(String n, ScriptInstance i, Stack l)
        {
           String value = l.pop().toString();
           int    start = BridgeUtilities.getInt(l);
           int    stop  = BridgeUtilities.getInt(l, value.length());
          
           return SleepUtils.getScalar(value.substring(start, stop));
        }
    }

    private static class func_indexOf implements Function
    {
        public Scalar evaluate(String n, ScriptInstance i, Stack l)
        {
           String value = l.pop().toString();
           String item  = l.pop().toString();
          
           return SleepUtils.getScalar(value.indexOf(item));
        }
    }

    private static class func_charAt implements Function
    {
        public Scalar evaluate(String n, ScriptInstance i, Stack l)
        {
           String value = l.pop().toString();
           int    start = BridgeUtilities.getInt(l);
          
           return SleepUtils.getScalar(value.charAt(start) + "");
        }
    }

    private static class func_sort implements Function
    {
        public Scalar evaluate(String n, ScriptInstance i, Stack l)
        {
           Function     my_func   = BridgeUtilities.getFunction(l, i);
           ScalarArray  array     = BridgeUtilities.getWorkableArray(l);

           if (my_func == null)
           {
              return SleepUtils.getArrayScalar();
           }

           array.sort(new CompareFunction(my_func, i));
           return SleepUtils.getArrayScalar(array);
        }
    }

    private static class func_sorta implements Function
    {
        public Scalar evaluate(String n, ScriptInstance i, Stack l)
        {
           ScalarArray  array     = BridgeUtilities.getWorkableArray(l);

           array.sort(new CompareStrings());
           return SleepUtils.getArrayScalar(array);
        }
    }

    private static class func_sortn implements Function
    {
        public Scalar evaluate(String n, ScriptInstance i, Stack l)
        {
           ScalarArray  array     = BridgeUtilities.getWorkableArray(l);

           array.sort(new CompareNumbers());
           return SleepUtils.getArrayScalar(array);
        }
    }

    private static class func_sortd implements Function
    {
        public Scalar evaluate(String n, ScriptInstance i, Stack l)
        {
           ScalarArray  array     = BridgeUtilities.getWorkableArray(l);

           array.sort(new CompareDoubles());
           return SleepUtils.getArrayScalar(array);
        }
    }

    private static class CompareFunction implements Comparator
    {
        protected Function       func;
        protected ScriptInstance script;
        protected Stack          locals;

        public CompareFunction(Function _func, ScriptInstance _script)
        {
           func     = _func;
           script   = _script;
           locals   = new Stack();
        }

        public int compare(Object a, Object b)
        {
           locals.push(b);
           locals.push(a);

           Scalar temp = SleepUtils.runCode(func, "&sort", script, locals);

           return temp.intValue();
        }
    }

    private static class CompareNumbers implements Comparator
    {
        public int compare(Object a, Object b)
        {
           long aa = ((Scalar)a).longValue();
           long bb = ((Scalar)b).longValue();

           return (int)(aa - bb);
        }
    }

    private static class CompareDoubles implements Comparator
    {
        public int compare(Object a, Object b)
        {
           double aa = ((Scalar)a).doubleValue();
           double bb = ((Scalar)b).doubleValue();

           if (aa == bb)
              return 0;

           if (aa < bb)
              return -1;

           return 1;
        }
    }

    private static class CompareStrings implements Comparator
    {
        public int compare(Object a, Object b)
        {
           return a.toString().compareTo(b.toString());
        }
    }

    private static class oper_concat implements Operator
    {
        public Scalar operate(String o, ScriptInstance i, Stack l)
        {
           Scalar left = (Scalar)(l.pop());
           Scalar right = (Scalar)(l.pop());

           if (o.equals("."))
           {
              return SleepUtils.getScalar(left.toString() + right.toString());
           }
 
           return null;
        }
    }

    private static class oper_multiply implements Operator
    {
        public Scalar operate(String o, ScriptInstance i, Stack l)
        {
           Scalar left = (Scalar)(l.pop());
           Scalar right = (Scalar)(l.pop());

           String str = left.toString();
           int    num = right.intValue();

           StringBuffer value = new StringBuffer();
         
           for (int x = 0; x < num; x++)
           {
              value.append(str);
           }

           return SleepUtils.getScalar(value);
        }
    }

    private static class oper_compare implements Operator
    {
        public Scalar operate(String o, ScriptInstance i, Stack l)
        {
           Scalar left = (Scalar)(l.pop());
           Scalar right = (Scalar)(l.pop());

           return SleepUtils.getScalar(left.toString().compareTo(right.toString()));
        }
    }

    private static class oper_spaceship implements Operator
    {
        public Scalar operate(String o, ScriptInstance i, Stack l)
        {
           double left  = BridgeUtilities.getDouble(l, 0); 
           double right = BridgeUtilities.getDouble(l, 0); 
        
           return SleepUtils.getScalar(left - right);
        }
    }

}
