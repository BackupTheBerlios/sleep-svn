/*
   SLEEP - Simple Language for Environment Extension Purposes
 .---------------------------.
 | sleep.bridges.RegexBridge |_______________________________________________
 |                                                                            |
   Author: Raphael Mudge (raffi@hick.org)
           http://www.hick.org/~raffi/

   Description:
      bridges java's regex API into sleep...

   Documentation:

   Changelog:
      1.8.04 - initially created.

   * This software is distributed under the artistic license, see license.txt
     for more information. *

 |____________________________________________________________________________|
 */

package sleep.bridges;
 
import java.util.*;
import java.util.regex.*;

import sleep.engine.*;
import sleep.engine.types.*;

import sleep.interfaces.*;
import sleep.runtime.*;

import sleep.parser.ParserConfig;

/** Provides a bridge between Java's regex API and sleep.  Rock on */
public class RegexBridge implements Loadable
{
    private static Map patternCache = Collections.synchronizedMap(new HashMap());
 
    static
    {
       ParserConfig.addKeyword("ismatch");
       ParserConfig.addKeyword("hasmatch");
    }

    private static Pattern getPattern(String pattern)
    {
       if (patternCache.containsKey(pattern))
       {
          return (Pattern)patternCache.get(pattern);  
       }

       if (patternCache.size() > 1024)
       {
          patternCache.clear(); /* ensure the pattern cache is flushed once in awhile */
       }
     
       Pattern temp = Pattern.compile(pattern);
       patternCache.put(pattern, temp);

       return temp;
    }

    public boolean scriptUnloaded(ScriptInstance aScript)
    {
        return true;
    }

    public boolean scriptLoaded (ScriptInstance aScript)
    {
        Hashtable temp = aScript.getScriptEnvironment().getEnvironment();

        isMatch matcher = new isMatch();

        // predicates
        temp.put("ismatch", matcher);
        temp.put("hasmatch", matcher);

        // functions
        temp.put("&matched", matcher);
        temp.put("&split", new split());
        temp.put("&join",  new join());
        temp.put("&matches", new getMatches());
        temp.put("&replace", new rreplace());

        return true;
    }

    private static class isMatch implements Predicate, Function
    {
       public boolean decide(String n, ScriptInstance i, Stack l)
       {
          String b = ((Scalar)l.pop()).toString(); // PATTERN
          String a = ((Scalar)l.pop()).toString(); // TEXT TO MATCH AGAINST

          ScriptEnvironment env = i.getScriptEnvironment();
          Matcher matcher;

          Pattern pattern = RegexBridge.getPattern(b);
          boolean rv;

          if (n.equals("hasmatch"))
          {
              matcher = (Matcher)env.getContextMetadata(a + b);

              if (matcher != null)
              {
                 env.setContextMetadata("matcher", matcher);
              }
              else
              {
                 matcher = pattern.matcher(a);
                 env.setContextMetadata(a + b, matcher);
                 env.setContextMetadata("matcher", matcher);
              }
              rv = matcher.find();
          }
          else
          {
              matcher = pattern.matcher(a);
              env.setContextMetadata("matcher", matcher);
              rv =  matcher.matches();
          }

          if (!rv) 
          {
             matcher  = null;           
             env.setContextMetadata("matcher", null);
             env.setContextMetadata(a + b, null);
          }

          return rv;
       }

       public Scalar evaluate(String n, ScriptInstance i, Stack l)
       {
          Scalar value = SleepUtils.getArrayScalar();            

          ScriptEnvironment env = i.getScriptEnvironment();
          Matcher matcher = (Matcher)env.getContextMetadata("matcher");

          if (matcher != null)
          {
             int count = matcher.groupCount();  

             for (int x = 1; x <= count; x++)
             {
                value.getArray().push(SleepUtils.getScalar(matcher.group(x)));
             }
          }

          return value;
       }
    }

    private static class getMatches implements Function
    {
       public Scalar evaluate(String n, ScriptInstance i, Stack l)
       {
          String a = ((Scalar)l.pop()).toString();
          String b = ((Scalar)l.pop()).toString();
          int    c = BridgeUtilities.getInt(l, -1);
          int    d = BridgeUtilities.getInt(l, c);

          Pattern pattern = RegexBridge.getPattern(b);
          Matcher matcher = pattern.matcher(a);
   
          Scalar value = SleepUtils.getArrayScalar();            

          int temp = 0;

          while (matcher.find())
          {
             int    count = matcher.groupCount();  

             if (temp == c) { value = SleepUtils.getArrayScalar(); }

             for (int x = 1; x <= count; x++)
             {
                value.getArray().push(SleepUtils.getScalar(matcher.group(x)));
             }

             if (temp == d) { return value; }

             temp++;
          }

          return value;
       }
    }

    private static class split implements Function
    {
       public Scalar evaluate(String n, ScriptInstance i, Stack l)
       {
          String a = ((Scalar)l.pop()).toString();
          String b = ((Scalar)l.pop()).toString();

          Pattern pattern  = RegexBridge.getPattern(a);

          String results[] = pattern.split(b);
          
          ArrayContainer value = new ArrayContainer();
          for (int x = 0; x < results.length; x++)
          {
             value.push(SleepUtils.getScalar(results[x]));
          }

          return SleepUtils.getArrayScalar(value);
       }
    }

    private static class join implements Function
    {
       public Scalar evaluate(String n, ScriptInstance script, Stack l)
       {
          String      a = ((Scalar)l.pop()).toString();
          Iterator    i = BridgeUtilities.getIterator(l, script);

          StringBuffer result = new StringBuffer();


          if (i.hasNext())
          {
             result.append(i.next().toString());
          }

          while (i.hasNext())
          {
             result.append(a);
             result.append(i.next().toString());
          }

          return SleepUtils.getScalar(result.toString());
       }
    }

    private static class rreplace implements Function
    {
       public Scalar evaluate(String n, ScriptInstance script, Stack l)
       {
          String a = BridgeUtilities.getString(l, ""); // current
          String b = BridgeUtilities.getString(l, ""); // old
          String c = BridgeUtilities.getString(l, ""); // new
          int    d = BridgeUtilities.getInt(l, -1);

          StringBuffer rv = new StringBuffer();

          Pattern pattern = RegexBridge.getPattern(b);
          Matcher matcher = pattern.matcher(a);
       
          int matches = 0;

          while (matcher.find() && matches != d)
          {
             matcher.appendReplacement(rv, c);
             matches++;
          }

          matcher.appendTail(rv);

          return SleepUtils.getScalar(rv.toString());
       }
    }
}
