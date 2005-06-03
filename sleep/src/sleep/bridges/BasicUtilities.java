/*
   SLEEP - Simple Language for Environment Extension Purposes
 .------------------------------.
 | sleep.bridges.BasicUtilities |_____________________________________________
 |                                                                            |
   Author: Raphael Mudge (raffi@hick.org)
           http://www.hick.org/~raffi/

   Description:
      provides some of the basic built in utility functions.

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

import java.io.File;
import java.net.URL;
import java.net.URLClassLoader;

import sleep.parser.Parser;
import sleep.error.YourCodeSucksException;

/** implementation of basic utility functions */
public class BasicUtilities implements Function, Loadable, Predicate
{
    public boolean scriptUnloaded (ScriptInstance i)
    {
        return true;
    }

    public boolean scriptLoaded (ScriptInstance i)
    {
        Hashtable temp = i.getScriptEnvironment().getEnvironment();
        //
        // functions
        //

        // array & hashtable related
        temp.put("&array",   new array());    // &keys(%hash) = @array
        temp.put("&hash",   new hash());      // &keys(%hash) = @array
        temp.put("&keys",  this);      // &keys(%hash) = @array
        temp.put("&size",  this);      // &size(@array) = <int>
        temp.put("&push",  this);      // &push(@array, $value) = $scalar
        temp.put("&pop",   this);      // &pop(@array) = $scalar
        temp.put("&add",   this);      // &pop(@array) = $scalar
        temp.put("&clear", this);
        temp.put("&subarray", this);
        temp.put("&copy",  new copy());
        temp.put("&map",    new map());

        temp.put("&remove", this);     // not safe within foreach loops (since they use an iterator, and remove throws an exception)
        temp.put("-istrue", this);    // predicate -istrue <Scalar>, determine wether or not the scalar is null or not.
        temp.put("-isarray", this);   
        temp.put("-ishash",  this); 

        SetScope scopeFunctions = new SetScope();

        temp.put("&local",    scopeFunctions);
        temp.put("&this",     scopeFunctions);
        temp.put("&module",   scopeFunctions);

        temp.put("&reverse",  new reverse());      // @array2 = &reverse(@array) 
        temp.put("&removeAt", new removeAt());   // not safe within foreach loops yada yada yada...
        temp.put("&shift",    new shift());   // not safe within foreach loops yada yada yada...

        temp.put("&systemProperties",    new systemProperties());
        temp.put("&use",    new f_use());
        temp.put("&checkError",    new checkError());

        // closure / function handle type stuff
        temp.put("&lambda",    new lambda());
        temp.put("&function",  new function());
        temp.put("&compile_closure",    new compile_closure());
        temp.put("&eval",     new eval());
        temp.put("&expr",     new expr());


        temp.put("=>",       new HashKeyValueOp());

        return true;
    }

    private static class HashKeyValueOp implements Operator
    {
        public Scalar operate(String name, ScriptInstance script, Stack locals)
        {
            Scalar identifier = (Scalar)locals.pop();
            Scalar value      = (Scalar)locals.pop();

            return SleepUtils.getScalar(new KeyValuePair(identifier, value));
        }
    }

    public boolean decide(String predName, ScriptInstance anInstance, Stack terms)
    {
       Scalar value = (Scalar)terms.pop();
 
       // Times when a scalar is considered true:
       // - its value is not equal to 0
       // - its not null (string value is not "")
       //
       // Scalar - String intValue
       //   0       "0"      0         - false
       //  null     ""       0         - false
       //  "blah"   "blah"   0         - true
       //  "3"      "3"      3         - true
       //   
       if (predName.equals("-istrue"))
          return value.getValue().toString().length() != 0 && !("0".equals(value.getValue().toString()));

       if (predName.equals("-isarray"))
          return value.getArray() != null;

       if (predName.equals("-ishash"))
          return value.getHash() != null;

       return false;
    }

    private static class checkError implements Function
    {
       public Scalar evaluate(String n, ScriptInstance si, Stack l)
       {
          Scalar value = BridgeUtilities.getScalar(l);

          String temp  = si.getScriptEnvironment().checkError();

          if (temp == null)
              return SleepUtils.getEmptyScalar();

          value.setValue(SleepUtils.getScalar(temp));           
          return value;
       }
    }

    private static class f_use implements Function
    {
       private HashMap bridges = new HashMap();

       public Scalar evaluate(String n, ScriptInstance si, Stack l)
       {
          File   parent;
          String className;

          if (l.size() == 2)
          {
             parent    = BridgeUtilities.getFile(l);
             className = BridgeUtilities.getString(l, "");
          }
          else
          {
             File a    = BridgeUtilities.getFile(l);
             parent    = a.getParentFile();
             className = a.getName();
          }

          Class bridge;

          try
          {
             if (parent != null)
             {
                URLClassLoader loader = new URLClassLoader(new URL[] { parent.toURL() });
                bridge = Class.forName(className, true, loader);
             }
             else
             {
                bridge = Class.forName(className);
             }

             Loadable temp;

             if (bridges.get(bridge) == null)
             {
                temp = (Loadable)bridge.newInstance();
                bridges.put(bridge, temp);
             }
             else
             {
                temp = (Loadable)bridges.get(bridge);
             }

             temp.scriptLoaded(si);
          }
          catch (Exception ex)
          {
             si.getScriptEnvironment().flagError(ex.toString());
          }

          return SleepUtils.getEmptyScalar();
       }
    }

    private static class array implements Function
    {
       public Scalar evaluate(String n, ScriptInstance si, Stack l)
       {
          Scalar value = SleepUtils.getArrayScalar();
           
          while (!l.isEmpty())
          {
             value.getArray().push(BridgeUtilities.getScalar(l));
          }

          return value;
       }
    }

    private static class function implements Function
    {
       public Scalar evaluate(String n, ScriptInstance si, Stack l)
       {
          String temp = BridgeUtilities.getString(l, "");
          return SleepUtils.getScalar(si.getScriptEnvironment().getFunction(temp));
       }
    }

    private static class hash implements Function
    {
       public Scalar evaluate(String n, ScriptInstance si, Stack l)
       {
          Scalar value = SleepUtils.getHashScalar();
           
          while (!l.isEmpty())
          {
             KeyValuePair kvp = BridgeUtilities.getKeyValuePair(l);
 
             Scalar blah = value.getHash().getAt(kvp.getKey());
             blah.setValue(kvp.getValue());
          }

          return value;
       }
    }

    private static class lambda implements Function
    {
       public Scalar evaluate(String n, ScriptInstance si, Stack l)
       {
          SleepClosure value;
 
          SleepClosure temp = BridgeUtilities.getFunction(l, si);           
          value = new SleepClosure(si, temp.getRunnableCode());
           
          Variable vars = value.getVariables();

          while (!l.isEmpty())
          {
             KeyValuePair kvp = BridgeUtilities.getKeyValuePair(l);
              vars.putScalar(kvp.getKey().toString(), kvp.getValue());
          }

          return SleepUtils.getScalar(value);
       }
    }

    private static class map implements Function
    {
       public Scalar evaluate(String n, ScriptInstance si, Stack l)
       {
          SleepClosure temp  = BridgeUtilities.getFunction(l, si);           
          ScalarArray  value = BridgeUtilities.getArray(l); 

          Scalar       rv    = SleepUtils.getArrayScalar();
          Stack        locals = new Stack();

          Iterator i = value.scalarIterator();
          while (i.hasNext())
          {
             locals.push(i.next());
             rv.getArray().push(temp.callClosure("eval", si, locals));
             locals.clear();
          }

          return rv;
       }
    }

    private static class copy implements Function
    {
       public Scalar evaluate(String n, ScriptInstance si, Stack l)
       {
          Scalar      value = SleepUtils.getArrayScalar();
          ScalarArray temp  = BridgeUtilities.getArray(l);           

          Iterator i = temp.scalarIterator();
          while (i.hasNext())
          {
             value.getArray().push(SleepUtils.getScalar((Scalar)i.next()));
          }

          return value;
       }
    }

    private static class removeAt implements Function
    {
       public Scalar evaluate(String n, ScriptInstance si, Stack l)
       {
          ScalarArray value = BridgeUtilities.getArray(l);
          return value.remove(BridgeUtilities.getInt(l, 0));
       }
    }

    private static class shift implements Function
    {
       public Scalar evaluate(String n, ScriptInstance si, Stack l)
       {
          ScalarArray value = BridgeUtilities.getArray(l);
          return value.remove(0);
       }
    }

    private static class reverse implements Function
    {
       public Scalar evaluate(String n, ScriptInstance si, Stack l)
       {
          ScalarArray temp  = BridgeUtilities.getArray(l);
          Scalar value = SleepUtils.getArrayScalar();

          Iterator i = temp.scalarIterator();
          while (i.hasNext())
          {
             value.getArray().add(SleepUtils.getScalar((Scalar)i.next()), 0);
          }

          return value;
       }          
    }

    private static class SetScope implements Function
    {
       public Scalar evaluate(String n, ScriptInstance i, Stack l)
       {
          Variable level = null;

          if (n.equals("&local")) { level = i.getScriptVariables().getLocalVariables(); }
          else if (n.equals("&this")) { level = i.getScriptVariables().getClosureVariables(); }
          else if (n.equals("&module")) { level = i.getScriptVariables().getInternalVariables(i); }

          String temp = l.pop().toString();

          if (level == null)
              return SleepUtils.getEmptyScalar(); 

          String vars[] = temp.split(" "); 
          for (int x = 0; x < vars.length; x++)
          {
             if (level.scalarExists(vars[x]))
             {
                // do nothing...
             }
             else if (vars[x].charAt(0) == '$')
             {
                i.getScriptVariables().setScalarLevel(vars[x], SleepUtils.getEmptyScalar(), level);
             }
             else if (vars[x].charAt(0) == '@')
             {
                i.getScriptVariables().setScalarLevel(vars[x], SleepUtils.getArrayScalar(), level);
             }
             else if (vars[x].charAt(0) == '%')
             {
                i.getScriptVariables().setScalarLevel(vars[x], SleepUtils.getHashScalar(), level);
             }
          }

          return SleepUtils.getEmptyScalar();
       }
    }

    private static class systemProperties implements Function
    {
       public Scalar evaluate(String n, ScriptInstance i, Stack l)
       {
          return SleepUtils.getHashWrapper(System.getProperties());
       }
    }

    private static class eval implements Function
    {
       public Scalar evaluate(String n, ScriptInstance i, Stack l)
       {
          String code  = l.pop().toString();

          try 
          {
             Scalar temp = SleepUtils.getScalar(i.getScriptEnvironment().evaluateStatement(code));
             return temp;
          }
          catch (YourCodeSucksException ex)
          {
             i.getScriptEnvironment().flagError(ex.getMessage());
             return SleepUtils.getEmptyScalar();
          }
       }
    }

    private static class compile_closure implements Function
    {
       public Scalar evaluate(String n, ScriptInstance i, Stack l)
       {
          String code  = l.pop().toString();

          try 
          {
             SleepClosure value = new SleepClosure(i, SleepUtils.ParseCode(code));
             Scalar       temp  = SleepUtils.getScalar(value);

             Variable      vars = value.getVariables();

             while (!l.isEmpty())
             {
                KeyValuePair kvp = BridgeUtilities.getKeyValuePair(l);
                vars.putScalar(kvp.getKey().toString(), kvp.getValue());
             }

             return temp;
          }
          catch (YourCodeSucksException ex)
          {
             i.getScriptEnvironment().flagError(ex.getMessage());
             return SleepUtils.getEmptyScalar();
          }
       }
    }

    private static class expr implements Function
    {
       public Scalar evaluate(String n, ScriptInstance i, Stack l)
       {
          String code  = l.pop().toString();

          try 
          {
             Scalar temp = SleepUtils.getScalar(i.getScriptEnvironment().evaluateExpression(code));
             return temp;
          }
          catch (YourCodeSucksException ex)
          {
             i.getScriptEnvironment().flagError(ex.getMessage());
             return SleepUtils.getEmptyScalar();
          }
       }
    }

    public Scalar evaluate(String n, ScriptInstance i, Stack l)
    {
       Scalar value = BridgeUtilities.getScalar(l);

       if (n.equals("&push"))
       {
          return value.getArray().push(SleepUtils.getScalar((Scalar)l.pop()));
       }
       else if (n.equals("&add"))
       {
          Scalar item = (Scalar)l.pop();

          int index    = 0;

          if (!l.isEmpty())
          {
             index = BridgeUtilities.getInt(l);  
          }
          else
          {
             index = value.getArray().size();
          }

          return value.getArray().add(item, index);
       }
       else if (n.equals("&pop"))
       {
          return value.getArray().pop();
       }
       else if (n.equals("&size")) // &size(@array)
       {
          if (value.getArray() != null)
             return SleepUtils.getScalar(value.getArray().size());
       }
       else if (n.equals("&clear"))
       {
          if (value.getArray() != null)
          {
             value.setValue(SleepUtils.getArrayScalar());
          }
          else if (value.getHash() != null)
          {
             value.setValue(SleepUtils.getHashScalar());
          }
          else
          {
             value.setValue(SleepUtils.getEmptyScalar());
          }
       }
       else if (n.equals("&subarray"))
       {
          if (value.getArray() != null)
          {
             int begin = BridgeUtilities.getInt(l, 0);
             int end   = BridgeUtilities.getInt(l, value.getArray().size());

             Scalar rv = SleepUtils.getArrayScalar();
             while (begin < end)
             {
                rv.getArray().push(value.getArray().getAt(begin));
                begin++;
             }

             return rv;
          }
       }
       else if (n.equals("&remove"))
       {
          Scalar scalar = (Scalar)l.pop();

          if (value.getArray() != null)
          {
             value.getArray().remove(scalar);
          }
          else if (value.getHash() != null)
          {
             value.getHash().remove(scalar);
          }
       }
       else if (n.equals("&keys")) // &keys(%hash)
       {
          if (value.getHash() != null)
          {
             Scalar temp = SleepUtils.getEmptyScalar();
             temp.setValue(value.getHash().keys());
             return temp;
          }
       }

       return SleepUtils.getEmptyScalar();
   }
}
